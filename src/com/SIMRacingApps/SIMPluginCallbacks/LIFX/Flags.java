package com.SIMRacingApps.SIMPluginCallbacks.LIFX;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import com.SIMRacingApps.Data;
import com.SIMRacingApps.SIMPlugin;
import com.SIMRacingApps.Server;
import com.SIMRacingApps.SIMPlugin.SIMPluginException;
import com.SIMRacingApps.SIMPluginCallbacks.SIMPluginCallback;
import com.SIMRacingApps.Util.State;

/**
 * This plugin uses your LIFX Lights to represent the current flag that is waving
 * in the flagman stand.
 * <p>
 * This plugin requires the following entries in the settings.
 * <pre>
 * LIFX-Flags = Y
 * LIFX-token = YourToken
 * LIFX-flags-brightness = 1.0
 * </pre>
 * To obtain a token, go to https://cloud.lifx.com and login to your account.
 * Then click on your name in the upper right corner of the screen and select "settings".
 * Click on "Generate a new Token" and give it the name "SIMRacingApps" and click "Generate".
 * Copy the token it generates and paste it in the SIMRacingApps settings.
 * <p>
 * NOTE: You must have the light(s) you want SIMRacingApps to use
 * assigned to your cloud account using the LIFX app and assigned to the group "SRA-flags". 
 * There is an option in the app, under each light's settings, to add the light to the cloud and assign the group. 
 * This field is case sensitive, so type it exactly as you see here.
 * <p>
 *
 * @author    Jeffrey Gilliam
 * @since     1.4
 * @copyright Copyright (C) 2018 - 2018 Jeffrey Gilliam
 * @license   Apache License 2.0
 */
public class Flags extends SIMPluginCallback {

    private static class states {
        static final String unknown = "unknown";
        static final String initialize = "initialize";
        static final String off="off";
        static final String blueflag="blueflag";
        static final String greenflag="greenflag";
        static final String yellowflagblinking="yellowflagblinking";
        static final String yellowflag="yellowflag";
        static final String whiteflag="whiteflag";
        static final String redflag="redflag";
        static final String checkeredflag="checkeredflag";
    }
    
    String m_token = "";
    State m_state = new State(states.unknown,System.currentTimeMillis());
    double m_brightness = 0;
    long m_blinkDuration = 0;
    Map<String,String> m_api = new HashMap<String,String>();
    boolean m_testMode = false;
    
    /**
	 * Constructor. 
	 * Use the constructor to initialize your plug-in.
	 *
	 * @param SIMPlugin An instance of the current SIM.
	 * @throws SIMPluginException If there's a problem constructing your plug-in.
	 */	
    public Flags(SIMPlugin SIMPlugin) throws SIMPluginException {
		super(SIMPlugin,"LIFX.Flags");
		
		m_brightness    = Server.getArg("LIFX-flags-brightness",            1.0);
		m_token         = Server.getArg("LIFX-token",                       "");
        m_blinkDuration = Server.getArg("LIFX-flags-yellow-blink-seconds",  30) * 1000;
        m_testMode      = Server.getArg("LIFX-flags-testmode",              false);        
        
        m_api.put(states.initialize,            Server.getArg("LIFX-api-flags-initialize",        "POST https://api.lifx.com/v1/lights/group:SRA-flags/effects/pulse?color=yellow%20brightness:0&from_color=yellow%20brightness:{BRIGHTNESS}&period=0.5&cycles=3&power_on=true"));
        m_api.put(states.off,                   Server.getArg("LIFX-api-flags-off",               "PUT https://api.lifx.com/v1/lights/group:SRA-flags/state?power=on&color=white&brightness=0&duration=0"));
        m_api.put(states.yellowflagblinking,    Server.getArg("LIFX-api-flags-yellow-blinking",   "POST https://api.lifx.com/v1/lights/group:SRA-flags/effects/pulse?color=yellow%20brightness:0&from_color=yellow%20brightness:{BRIGHTNESS}&period=1.25&cycles=9999&power_on=true"));
        m_api.put(states.yellowflag,            Server.getArg("LIFX-api-flags-yellow",            "PUT https://api.lifx.com/v1/lights/group:SRA-flags/state?power=on&color=yellow&brightness={BRIGHTNESS}&duration=0"));
        m_api.put(states.blueflag,              Server.getArg("LIFX-api-flags-blue",              "PUT https://api.lifx.com/v1/lights/group:SRA-flags/state?power=on&color=blue&brightness={BRIGHTNESS}&duration=0"));
        m_api.put(states.greenflag,             Server.getArg("LIFX-api-flags-green",             "PUT https://api.lifx.com/v1/lights/group:SRA-flags/state?power=on&color=green&brightness={BRIGHTNESS}&duration=0"));
        m_api.put(states.whiteflag,             Server.getArg("LIFX-api-flags-white",             "PUT https://api.lifx.com/v1/lights/group:SRA-flags/state?power=on&color=white&brightness={BRIGHTNESS}&duration=0"));
        m_api.put(states.checkeredflag,         Server.getArg("LIFX-api-flags-checkered",         "POST https://api.lifx.com/v1/lights/group:SRA-flags/effects/pulse?color=white%20brightness:{BRIGHTNESS}&from_color=white%20brightness:0&period=0.5&cycles=9999&power_on=true"));
        m_api.put(states.redflag,               Server.getArg("LIFX-api-flags-red",               "PUT https://api.lifx.com/v1/lights/group:SRA-flags/state?power=on&color=red&brightness={BRIGHTNESS}&duration=0"));
		
		if (m_token.isEmpty()) 
		    Server.logger().warning("No LIFX-token found in settings. See http://github.com/SIMRacingApps/SIMRacingApps/wiki/LIFX-Lights-Setup.");
		
		//Show that the light is working
		_send(states.initialize);
		try { Thread.sleep(2000); } catch (InterruptedException e) {}
		
		//Call Subscribe one for each path you want to get data for.
		Subscribe("/Car/REFERENCE/IsYellowFlag"); 
        Subscribe("/Car/REFERENCE/IsBlueFlag");
        Subscribe("/Session/IsGreenFlag");
        Subscribe("/Session/IsCautionFlag");
        Subscribe("/Session/IsWhiteFlag");
        Subscribe("/Session/IsCheckeredFlag");
        Subscribe("/Session/IsRedFlag");
	}
	
	/**
	 * Called when the SIMPlugin is destroyed.
	 * It's always best to call super.destroy() for future compatibility.
	 */
    @Override
    public void destroy() {
		super.destroy();
		
		m_state.setState(states.unknown,System.currentTimeMillis());
		_send(states.off);
	}

    private void _send(String state) {
        if (!m_token.isEmpty()) {
            
            if (m_state.equals(state))
                return;
            
            String api = m_api.get(state);
            
            if (api == null)
                return;
            
            String api_parts[] = api.replace("{BRIGHTNESS}", Double.toString(m_brightness)).split(" "); 
            
            if (api_parts.length != 2)
                return;
            
            Server.logger().info(String.format("LIFX.Flags._send(%s): %s,%s",state,api_parts[0],api_parts[1]));
            
            try {
                URL url = new URL(api_parts[1]);
                HttpsURLConnection urlc = (HttpsURLConnection)url.openConnection();
                urlc.setRequestMethod(api_parts[0]);
                urlc.setRequestProperty("Accept","*/*");
                urlc.setRequestProperty("Authorization", String.format("Bearer %s",m_token));
                //urlc.setRequestProperty("accept-encoding","gzip, deflate");
                urlc.setRequestProperty("content-type","application/json");
                urlc.setRequestProperty("content-lenth","0");
    
                try {
                    urlc.connect();
                }
                catch (IOException e) {}
                
                
                BufferedReader in = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
                StringBuffer response = new StringBuffer();
                
                if (in != null) {
                    String inputLine;
    
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                }

                int responseCode = urlc.getResponseCode();
                
                //do not set the state unless it returns a good code
                if (responseCode == 200 || responseCode == 207) {
                    Server.logger().info(String.format("LIFX.Flags._send(%s): response code = %d, %s",state,responseCode,response.toString()));
                    m_state.setState(state,System.currentTimeMillis());
                }
                else {
                    Server.logger().warning(String.format("LIFX.Flags._send(%s): response code = %d, %s",state,responseCode,response.toString()));
                }
                
                return;
            }
            catch (Exception e) {
                Server.logStackTrace(e);
            }
        }
    }
	/**
	 * ProcessData is called from within the dedicated thread for this plug-in
	 * every time there is data available in the queue it pops it off the queue and passes it to this method.
	 * 
	 * At this point in the design, it will get called every 16ms. 
	 * At some point, I may change it to only get called if the subscribed to data changes.
	 * 
	 * NOTE: If you find you need to use the SIMPlugin argument, inclose it in a synchronized block. 
	 * You are in a separate thread, so this will block the main thread. Do not keep it locked very long.
     * <pre>
     *     //When calling the SIMPlugin from this method, you must synchronize on it first. It is not thread safe.
     *     Data d;
     *     synchronized (SIMPlugin) {
     *         //do not spend much time in here, we are blocking the main thread 
     *         //Get the data and get out of the synchronized block 
     *         d = SIMPlugin.getData("/Session/IsGreenFlag");
     *         d = SIMPlugin.getSession().getIsGreenFlag();
     *     }
     *     //do something with the data
     * </pre>
	 * 
	 * @param SIMPlugin A reference to the SIMPlugin instance.
	 * @param data The Data objects added to the queue.
	 * @return true to keep the plug-in alive, false to kill it.
	 */
    @Override
	public boolean ProcessData(SIMPlugin SIMPlugin, Map<String,Data> data) {

        boolean isYellow    = false;
        boolean isGreen     = false;
        boolean isBlue      = false;
        boolean isWhite     = false;
        boolean isRed       = false;
//        boolean isCheckered = false;
        long    currentTime = System.currentTimeMillis();
        
        if (m_testMode) {
            if (m_state.equals(states.off) && m_state.getTime(currentTime) > 10000L) {
                isYellow = true;
            }
            if ((m_state.equals(states.yellowflag) || m_state.equals(states.yellowflagblinking))) {
                if (m_state.getTime(currentTime) > 5000L)
                    isGreen = true;
                else
                    isYellow = true;
            }
            if (m_state.equals(states.greenflag)) {
                if (m_state.getTime(currentTime) > 5000L)
                    isBlue = true;
                else
                    isGreen = true;
            }
            if (m_state.equals(states.blueflag)) {
                if (m_state.getTime(currentTime) > 5000L)
                    isWhite = true;
                else
                    isBlue = true;
            }
            if (m_state.equals(states.whiteflag)) {
                if (m_state.getTime(currentTime) > 5000L)
                    isRed = true;
                else
                    isWhite = true;
            }
//            if (m_state.equals(states.checkeredflag)) {
//                if (m_state.getTime(currentTime) > 5000L)
//                    isRed = true;
//                else
//                    isCheckered = true;
//            }
            if (m_state.equals(states.redflag)) {
                if (m_state.getTime(currentTime) < 5000L)
                    isRed = true;
            }
        }
        else {
            //if data is empty, then the SIM is not running
            if (data.isEmpty()) {
                //turn the light(s) off
                if (!m_state.equals(states.off))
                    _send(states.off);
                return true;
            }
            
            isYellow = data.get("/Car/REFERENCE/IsYellowFlag").getBoolean() || data.get("/Session/IsCautionFlag").getBoolean();
            isBlue   = data.get("/Car/REFERENCE/IsBlueFlag").getBoolean();
            isGreen  = data.get("/Session/IsGreenFlag").getBoolean();
            isWhite  = data.get("/Session/IsWhiteFlag").getBoolean();
            isRed    = data.get("/Session/IsRedFlag").getBoolean();
            //isCheckered = data.get("Session/IsCheckeredFlag").getBoolean();
        }
                    
        if (isRed) {
            if (!m_state.equals(states.redflag)) {
                _send(states.redflag);
            }
        }
        else 
        if (isWhite) {
            if (!m_state.equals(states.whiteflag)) {
                _send(states.whiteflag);
            }
        }
        else 
        if (isYellow) {
            if (!m_state.equals(states.yellowflagblinking) && !m_state.equals(states.yellowflag)) {
                _send(states.yellowflagblinking);
            }
            else
            if (m_state.equals(states.yellowflagblinking)) { 
                if (m_state.getTime(currentTime) > m_blinkDuration) {
                    _send(states.yellowflag);
                }
            }
        }
        else
        if (isBlue) {
            if (!m_state.equals(states.blueflag)) {
                _send(states.blueflag);
            }
        }
        else
        if (isGreen) {
            if (!m_state.equals(states.greenflag)) {
                _send(states.greenflag);
            }
        }
        else
//        if (isCheckered) {
//            if (!m_state.equals(states.checkeredflag)) {
//                _send(states.checkeredflag);
//            }
//        }
//        else 
        {
            //if the light(s) are on, turn them off
            if (!m_state.equals(states.off))
                _send(states.off);
        }
        
        return true;
    }
    
    /**
     * Gets a value.
     * 
     * <p>PATH = {@link #get?() /SIMPluginCallback/{plugin}/{name}/?}
     * 
     * @return The ? in a {@link com.SIMRacingApps.Data} container.
     */
/*    
    public Data get?() {
        //TODO: add code to get the value to return. Remember to synchronize, this is in a different thread.
        synchronize (?) {
            return new Data("", ?,"",Data.State.NORMAL);
        }
    }
/**/    
}