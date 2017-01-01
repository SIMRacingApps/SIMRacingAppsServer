package com.SIMRacingApps.SIMPluginCallbacks.MSPEC;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.SIMRacingApps.Car;
import com.SIMRacingApps.Data;
import com.SIMRacingApps.SIMPlugin;
import com.SIMRacingApps.SIMPlugin.SIMPluginException;
import com.SIMRacingApps.Server;
import com.SIMRacingApps.Windows;
import com.SIMRacingApps.Windows.Handle;
import com.SIMRacingApps.SIMPluginCallbacks.SIMPluginCallback;

/**
 * This class implements the interface to the MSPEC Shift Light.
 * <p>
 * Each light must have a unique port assigned to it in the settings.
 * For example: If your light is on COM3, then use
 * <pre> 
 *     mspec-rpm = 3
 * </pre>
 * 
 * By default the light will come on when speeding. You can control
 * this with the "mspec-speeding = true or false" variable.
 * 
 * This document was used as the reference to this implementation.
 * <a href="http://www.silabs.com/Support%20Documents/TechnicalDocs/an197.pdf"></a>
 * @author Jeffrey Gilliam
 * @since 1.0
 * @copyright Copyright (C) 2015 - 2017 Jeffrey Gilliam
 * @license Apache License 2.0
 */
public class ShiftLight extends SIMPluginCallback {

    private int     NONE                        = 0;
    private int     RTS                         = 1;
    private int     DTR                         = 2;
    private long    CAUTIONBLINKDURATION        = 30000L;
    private long    m_blinkRateLimiter          = 500;
    private long    m_blinkRate                 = 300;
    private long    m_blinkRateCritical         = 100;
    private Map<String,_ShiftLight> m_lights    = new HashMap<String,_ShiftLight>();
    
    public static String[] types = {"eflag","rpm","red","green","blue","yellow","redflag","greenflag","yellowflag"};

    public ShiftLight(SIMPlugin SIMPlugin) throws SIMPluginException {
        super(SIMPlugin,"MSPEC.ShiftLight");
        Map<Integer,String> usedPorts = new HashMap<Integer,String>();
        for (String type : types) {
            int commPort = Server.getArg("mspec-"+type, 0);
            if (commPort > 0) {
                if (usedPorts.containsKey(commPort)) {
                    throw new SIMPluginException("MSPEC: Cannot assign the same comm port to multiple lights");
                }
                try {
                    m_lights.put(type, new _ShiftLight(commPort,type));
                    usedPorts.put(commPort, type);
                    Server.logger().info("MSPEC: Connected "+type+" light to comm port "+Integer.toString(commPort));
                } catch (SIMPluginException e) {
                    //don't throw this because it will stop the SIM
                    //just log it and continue
                    Server.logger().warning(e.getMessage());
                }
            }
        }
        Subscribe("Car/REFERENCE/PitSpeedLimit");
        Subscribe("Car/REFERENCE/Gauge/Tachometer/ValueCurrent");
        Subscribe("Car/REFERENCE/Gauge/Speedometer/ValueCurrent");
        Subscribe("Car/REFERENCE/Status");
        Subscribe("Car/REFERENCE/Messages");
        Subscribe("Car/REFERENCE/IsYellowFlag");
        Subscribe("Car/REFERENCE/IsBlueFlag");
        Subscribe("Session/IsGreenFlag");
        Subscribe("Session/IsCautionFlag");
    }

    @Override
    public void destroy() {
        Iterator<Entry<String, _ShiftLight>> itr = m_lights.entrySet().iterator();
        while (itr.hasNext()) {
            Entry<String, _ShiftLight> entry = itr.next();
            entry.getValue().disconnect();
            itr.remove();
        }
    }
    
    @Override 
    public boolean ProcessData(SIMPlugin SIMPlugin, Map<String,Data> data) {
        
        Iterator<Entry<String, _ShiftLight>> itr = m_lights.entrySet().iterator();
        while (itr.hasNext()) {
            Entry<String, _ShiftLight> entry = itr.next();
            if (data.isEmpty())
                entry.getValue().disconnect();  //have the light disconnect when the SIM isn't running
            else
                entry.getValue().onDataVersionChanged(data);
        }
        
        return true;
    }
    
    private class _ShiftLight {
        private int                            m_commPort          = 0;
        private String                         m_function          = "rpm";
        private Handle                         m_handle            = null;
        private boolean                        m_prevState         = false;
        private int                            m_prevLight         = NONE;
        private long                           m_blinkStart        = 0L;
        private boolean                        m_blinkState        = false;
        private long                           m_startTime         = Long.MAX_VALUE - CAUTIONBLINKDURATION;
    
        /**
         * Constructor to initiate a connection to the MSPEC Shift Light when connected to the same machine as the SIMRacingApps server.
         * @param SIMPlugin The current SIM plug-in.
         * @param commPort The comm port number the light is assigned to.
         * @param function The function of the light. (rpm,green,blue,yellow)
         * @throws SIMPluginException If there is a problem connecting to the light.
         */
        public _ShiftLight(int commPort,String function) throws SIMPlugin.SIMPluginException {
            m_commPort  = commPort;
            m_function  = function.toLowerCase();
            m_handle = Windows.openCommPort(m_commPort);
            if (m_handle == null) {
                throw new SIMPlugin.SIMPluginException("MSPEC: Error connecting "+m_function+" light to comm port "+Integer.toString(m_commPort)+", "+Windows.getLastErrorMessage());
            }
        }
    
        /**
         * Called to disconnect from the comm port
         */
        public void disconnect() {
            if (m_handle != null) {
                Server.logger().info("disconnect()");
                Windows.setCommPortRTS(m_handle,true);  //try and turn it off before we close it
                Windows.closeHandle(m_handle);
                m_handle = null;
            }
        }
        
        private void _light(boolean state, int light) {
            if (m_prevState != state || m_prevLight != light) {
                if (state) {
                    if (Server.logger().getLevel().intValue() >= Level.FINER.intValue())
                        Server.logger().finer("MSPEC: Turning On the "+m_function+" light("+String.format("%d", light));
                    
                    if ((light & RTS) > 0) {
                        if (!Windows.setCommPortRTS(m_handle,true)) {
                            Server.logger().warning("MSPEC: Error turning on the "+m_function+" light. " + Windows.getLastErrorMessage());
                            disconnect();
                        }
                    }
                    else {
                        if (!Windows.setCommPortRTS(m_handle,false)) {
                            Server.logger().warning("MSPEC: Error turning off the "+m_function+" light(RTS). " + Windows.getLastErrorMessage());
                            disconnect();
                        }
                    }
                
                    if ((light & DTR) > 0) {
                        if (!Windows.setCommPortDTR(m_handle,true)) {
                            Server.logger().warning("MSPEC: Error turning on the "+m_function+" light. " + Windows.getLastErrorMessage());
                            disconnect();
                        }
                    }
                    else {
                        if (!Windows.setCommPortDTR(m_handle,false)) {
                            Server.logger().warning("MSPEC: Error turning off the "+m_function+" light(DTR). " + Windows.getLastErrorMessage());
                            disconnect();
                        }
                    }
                }
                else {
                    if (Server.logger().getLevel().intValue() >= Level.FINER.intValue())
                        Server.logger().finer("MSPEC: Turning Off the "+m_function+" light");
                    if (!Windows.setCommPortRTS(m_handle,false)) {
                        Server.logger().warning("MSPEC: Error turning off the "+m_function+" light(RTS). " + Windows.getLastErrorMessage());
                        disconnect();
                    }
                    if (!Windows.setCommPortDTR(m_handle,false)) {
                        Server.logger().warning("MSPEC: Error turning off the "+m_function+" light(DTR). " + Windows.getLastErrorMessage());
                        disconnect();
                    }
                }
                m_prevState = state;
                m_prevLight = light;
            }
        }
        
        private void _lightSwitch(boolean state,int light) {
            _light(state,light);
            m_blinkStart = 0L;
        }
        
        private void _lightBlink(long rate,int light) {
            if (System.currentTimeMillis() > (m_blinkStart + rate)) {
                if (m_blinkState)
                    _light(false,NONE);
                else
                    _light(true,light);
                
                m_blinkState = !m_blinkState;
                m_blinkStart = System.currentTimeMillis();
            }
        }
        
        public void onDataVersionChanged(Map<String,Data> data) {
        
            try {
                if (m_handle == null) {
                    Server.logger().info("MSPEC: Connecting "+m_function+" light to comm port "+Integer.toString(m_commPort));
                    m_handle = Windows.openCommPort(m_commPort);
                    if (m_handle == null) {
                        Server.logger().warning("MSPEC: Error connecting to comm port "+Integer.toString(m_commPort)+", "+Windows.getLastErrorMessage());
                        return;
                    }
                }
    
                if (m_function.equals("rpm") || m_function.equals("red")) {
//                    Data limit            = data.get("Car/REFERENCE/PitSpeedLimit");
                    Data tach             = data.get("Car/REFERENCE/Gauge/Tachometer/ValueCurrent"); 
                    Data speed            = data.get("Car/REFERENCE/Gauge/Speedometer/ValueCurrent");
                    String status         = data.get("Car/REFERENCE/Status").getString();
                    boolean pitLimiter    = data.get("Car/REFERENCE/Messages").getString().contains(";PITSPEEDLIMITER;");
                    boolean pitRoadActive = Server.getArg("mspec-speeding",true) && status.contains("PIT") && !status.equals(Car.Status.LEAVINGPITS);

                    if (pitLimiter) {
                        _lightBlink(m_blinkRateLimiter,RTS);
                    }
                    else
                    if (pitRoadActive && speed.getState().equalsIgnoreCase("OVERLIMIT")) {
                        _lightSwitch(true,RTS);
                    }
                    else
                    if (pitRoadActive && speed.getState().equalsIgnoreCase("WAYOVERLIMIT")) {
                        //_lightSwitch(true,RTS);
                        _lightBlink(m_blinkRateCritical,RTS);
                    }
                    else
                    if (tach.getState().equalsIgnoreCase("SHIFT")) {
                        _lightSwitch(true,RTS);
                    }
                    else {
                        if (tach.getState().equalsIgnoreCase("SHIFTBLINK")) {
                            _lightBlink(m_blinkRate,RTS);
                        }
                        else
                        if (tach.getState().equalsIgnoreCase("CRITICAL")) {
                            _lightBlink(m_blinkRateCritical,RTS);
                        }
                        else {
                            _lightSwitch(false,NONE);
                        }
                    }
                }
                else
                if (m_function.equals("green") || m_function.equals("greenflag")) {
                    if (data.get("Session/IsGreenFlag").getBoolean()) {
                        _lightSwitch(true,RTS);
                    }
                    else {
                        _lightSwitch(false,NONE);
                    }
                }
                else
                if (m_function.equals("blue") || m_function.equals("blueflag")) {
                    if (data.get("Car/REFERENCE/IsBlueFlag").getBoolean()) {
                        _lightSwitch(true,RTS);
                    }
                    else {
                        _lightSwitch(false,NONE);
                    }
                }
                else
                if (m_function.equals("yellow") || m_function.equals("yellowflag")) {
                    boolean yellowFlag = data.get("Car/REFERENCE/IsYellowFlag").getBoolean()
                                      || data.get("Session/IsCautionFlag").getBoolean();
                                
                    if (yellowFlag) {
                        if (m_startTime == 0L)
                            m_startTime = System.currentTimeMillis();
                            
                        if (System.currentTimeMillis() <= (m_startTime + CAUTIONBLINKDURATION))
                            _lightBlink(m_blinkRate,RTS);
                        else
                            _lightSwitch(true,RTS);
                    }
                    else {
                        _lightSwitch(false,NONE);
                        m_startTime = 0L;
                    }
                }
                else
                if (m_function.equals("eflag")) {
                    boolean yellowFlag = data.get("Car/REFERENCE/IsYellowFlag").getBoolean()
                                      || data.get("Session/IsCautionFlag").getBoolean();
                                
                    if (yellowFlag) {
                        if (m_startTime == 0L)
                            m_startTime = System.currentTimeMillis();
                            
                        if (System.currentTimeMillis() <= (m_startTime + CAUTIONBLINKDURATION))
                            _lightBlink(m_blinkRate,RTS);
                        else
                            _lightSwitch(true,RTS);
                    }
                    else
                    if (data.get("Session/IsGreenFlag").getBoolean()) {
                        _lightSwitch(true,DTR);
                        m_startTime = 0L;
                    }
                    else
                    if (data.get("Car/REFERENCE/IsBlueFlag").getBoolean()) {
                        _lightSwitch(true,RTS | DTR);
                        m_startTime = 0L;
                    }
                    else {
                        _lightSwitch(false,NONE);
                        m_startTime = 0L;
                    }
                }
            }
            catch (Exception e)
            {
                Server.logStackTrace(Level.SEVERE,"MSPEC: Unknown Exception caught: "+e.getMessage(),e);
                disconnect();
            }
        }
    }
}
