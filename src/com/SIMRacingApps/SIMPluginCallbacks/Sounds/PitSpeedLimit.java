package com.SIMRacingApps.SIMPluginCallbacks.Sounds;

import java.util.Map;
import com.SIMRacingApps.Car;
import com.SIMRacingApps.Data;
import com.SIMRacingApps.SIMPlugin;
import com.SIMRacingApps.SIMPlugin.SIMPluginException;
import com.SIMRacingApps.Server;
import com.SIMRacingApps.SIMPluginCallbacks.SIMPluginCallback;
import com.SIMRacingApps.Util.Sound;

/**
 * This plug-in will beep if you are speeding on pit road.
 * You can control its behavior with the following variables.
 * <pre>
 *    pit-speed-limit-device = A Sound Device
 *    pit-speed-limit-volume = 100.0
 *    pit-speed-limit-clip = com/SIMRacingApps/SIMPluginCallbacks/Sounds/Clips/speeding_beep.wav
 *    pit-speed-limit-rate = 200
 *    pit-speed-limit-rate-excessive = 100
 *    pit-speed-limit-replay = false
 *    
 * #Also these global settings will be used if the specific settings above are not set.
 *    sound-device = A Sound Device Name
 *    sound-volume = 100.0
 * </pre>
 * Setting the volume zero(0) will disable the sounds.
 * The volume is a percentage from 0.0 to 1.0.
 *
 * @author Jeffrey Gilliam
 * @since 1.0
 * @copyright Copyright (C) 2015 - 2019 Jeffrey Gilliam
 * @license Apache License 2.0
 */
public class PitSpeedLimit extends SIMPluginCallback {

    private final Sound m_clip;
    private final String m_device;
    private final Integer m_rate;
    private final Integer m_rate_excessive;
    private final Boolean m_replay;
    
    private Double m_volume;
    
    /**
	 * Constructor. 
	 * This plug-in watches for your distance from you pit stall and counts you down.
	 *
	 * @param SIMPlugin An instance of the current SIM.
	 * @throws SIMPluginException If there's a problem constructing your plug-in.
	 */	
    public PitSpeedLimit(SIMPlugin SIMPlugin) throws SIMPluginException {
		super(SIMPlugin,"Sounds.PitSpeedLimit");
		
		m_device              = Server.getArg("pit-speed-limit-device");
		m_volume              = Server.getArg("pit-speed-limit-volume", -1.0);
        m_rate                = Server.getArg("pit-speed-limit-rate", 300);
        m_rate_excessive      = Server.getArg("pit-speed-limit-excessive", 200);
        m_replay              = Server.getArg("pit-speed-limit-replay",false);
        
		String defaultFile    = "com/SIMRacingApps/SIMPluginCallbacks/Sounds/Clips/speeding_beep.wav";
		String soundFile      = Server.getArg("pit-speed-limit-clip",defaultFile);
		
        Sound clip = new Sound(m_device,soundFile);
        if (!clip.getErrorMessage().isEmpty())
            clip = new Sound(m_device,defaultFile);
        
        clip.setVolume(m_volume);
        clip.setMinTimeBetweenPlays(m_rate);
        m_clip = clip;
        
        Subscribe("Car/REFERENCE/Status");
        Subscribe("Car/REFERENCE/Gauge/Speedometer/ValueCurrent");
        Subscribe("Session/IsReplay");
	}
	
	/**
	 * Called when the SIMPlugin is destroyed.
	 * It's always best to call super.destroy() for future compatibility.
	 */
    public void destroy() {
		super.destroy();
		
        synchronized (m_clip) {
		    m_clip.close();
        }
	}
	
    /**
     * Gets the volume as a percentage.
     * 
     * <p>PATH = {@link #getVolume() /SIMPluginCallback/Sounds/PitSpeedLimit/Volume}
     * 
     * @return The new volume percentage in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getVolume() {
        double volume = 0.0;
        //get the volume that the clip says, not m_volume as it could be -1
        if (m_clip != null)
            synchronized (m_clip) {
                volume = m_clip.getVolume();
            }
        return new Data("", volume,"%",Data.State.NORMAL);
    }
    
    /**
     * Sets the volume as a percentage. Range 0.0 to 100.0
     * 
     * <p>PATH = {@link #setVolume(double) /SIMPluginCallback/Sounds/PitSpeedLimit/setVolume/(PERCENTAGE)}
     * 
     * @param percentage The new volume percentage, between 0.0 and 100.0.
     * @return The new volume percentage in a {@link com.SIMRacingApps.Data} container.
     */
    public Data setVolume(double percentage) {

        if (m_clip != null)
            synchronized (m_clip) {
    		    m_clip.setVolume(percentage);
            }
        synchronized (m_volume) {
            m_volume = percentage;
        }
        return getVolume();
    }
    public Data setVolume(String percentage) {
        try {
            return setVolume(Double.parseDouble(percentage));
        }
        catch (NumberFormatException e) {}
        return getVolume();
    }
    
    @Override 
    public boolean ProcessData(SIMPlugin SIMPlugin, Map<String,Data> data) {
        if (data.isEmpty())
            return true;
        
        synchronized (m_clip) {
            String status = data.get("Car/REFERENCE/Status").getString();
            boolean replay = data.get("Session/IsReplay").getBoolean();
            
            //should we be playing in replay mode?
            if (!m_replay && replay)
                return true;
            
            if (status.equals(Car.Status.APPROACHINGPITS)
            ||  status.equals(Car.Status.ONPITROAD) 
            ||  status.equals(Car.Status.ENTERINGPITSTALL)
            ||  status.equals(Car.Status.INPITSTALL)
            ) {
                String state = data.get("Car/REFERENCE/Gauge/Speedometer/ValueCurrent").getState();
                
                if (state.equals("OVERLIMIT")) {
                    m_clip.setMinTimeBetweenPlays(m_rate);
                    m_clip.play();
                }
                if (state.equals("WAYOVERLIMIT")) {
                    m_clip.setMinTimeBetweenPlays(m_rate_excessive);
                    m_clip.play();
                }
            }
        }
        return true;
    }
}