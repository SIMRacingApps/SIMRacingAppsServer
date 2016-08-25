package com.SIMRacingApps.SIMPluginCallbacks.Sounds;

import java.util.Map;
import com.SIMRacingApps.Data;
import com.SIMRacingApps.SIMPlugin;
import com.SIMRacingApps.SIMPlugin.SIMPluginException;
import com.SIMRacingApps.Server;
import com.SIMRacingApps.SIMPluginCallbacks.SIMPluginCallback;
import com.SIMRacingApps.Util.Sound;

/**
 * This plug-in will beep when it is time to shift.
 * You can control its behavior with the following variables.
 * <pre>
 *    shift-device = A Sound Device
 *    shift-volume = 1.0
 *    shift-clip = com/SIMRacingApps/SIMPluginCallbacks/Sounds/Clips/shift_beep.wav
 *    
 * #Also these global settings will be used if the specific settings above are not set.
 *    sound-device = A Sound Device Name
 *    sound-volume = 1.0
 * </pre>
 * Setting the volume zero(0) will disable the sounds.
 * The volume is a percentage from 0.0 to 1.0.
 *
 * @author Jeffrey Gilliam
 * @since 1.0
 * @copyright Copyright (C) 2015 - 2016 Jeffrey Gilliam
 * @license Apache License 2.0
 */
public class Shift extends SIMPluginCallback {

    private final Sound m_clip;
    private final String m_device;
    
    private Double m_volume;
    
    /**
	 * Constructor. 
	 * This plug-in watches for your distance from you pit stall and counts you down.
	 *
	 * @param SIMPlugin An instance of the current SIM.
	 * @throws SIMPluginException If there's a problem constructing your plug-in.
	 */	
    public Shift(SIMPlugin SIMPlugin) throws SIMPluginException {
		super(SIMPlugin,"Sounds.Shift");
		
		m_device              = Server.getArg("shift-device");
		m_volume              = Server.getArg("shift-volume", -1.0);
        
		String defaultFile    = "com/SIMRacingApps/SIMPluginCallbacks/Sounds/Clips/shift_beep.wav";
		String soundFile      = Server.getArg("shift-clip",defaultFile);
		
        Sound clip = new Sound(m_device,soundFile);
        if (!clip.getErrorMessage().isEmpty())
            clip = new Sound(m_device,defaultFile);
        
        clip.setVolume(m_volume * 100.0);
        clip.setMinTimeBetweenPlays(500);
        m_clip = clip;
        
        Subscribe("Car/REFERENCE/Gauge/Tachometer/ValueCurrent");
        Subscribe("Car/REFERENCE/Gauge/Gear/ValueCurrent");
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
     * <p>PATH = {@link #getVolume() /SIMPluginCallback/Sounds/Shift/Volume}
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
        return new Data("", volume * 100.0,"%",Data.State.NORMAL);
    }
    
    /**
     * Sets the volume as a percentage. Range 0.0 to 100.0
     * 
     * <p>PATH = {@link #setVolume(double) /SIMPluginCallback/Sounds/Shift/setVolume/(PERCENTAGE)}
     * 
     * @param percentage The new volume percentage, between 0.0 and 100.0.
     * @return The new volume percentage in a {@link com.SIMRacingApps.Data} container.
     */
    public Data setVolume(double percentage) {

        if (m_clip != null)
            synchronized (m_clip) {
    		    m_clip.setVolume(percentage / 100.0);
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
    
    //These variables are only used by the background thread.
    private String m_gear = "";
    
    @Override 
    public boolean ProcessData(SIMPlugin SIMPlugin, Map<String,Data> data) {
        if (data.isEmpty())
            return true;
        
        synchronized (m_clip) {
            String gear  = data.get("Car/REFERENCE/Gauge/Gear/ValueCurrent").getString();
            String state = data.get("Car/REFERENCE/Gauge/Tachometer/ValueCurrent").getState();

            if (state.equals("SHIFT") && !gear.equals(m_gear) && !gear.equals("N")) {
                m_clip.play();
                m_gear = gear;
            }
        }
        return true;
    }
}