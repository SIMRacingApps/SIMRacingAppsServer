package com.SIMRacingApps.SIMPluginCallbacks.Sounds;

import java.util.ArrayList;
import java.util.Map;
import com.SIMRacingApps.Car;
import com.SIMRacingApps.Data;
import com.SIMRacingApps.SIMPlugin;
import com.SIMRacingApps.SIMPlugin.SIMPluginException;
import com.SIMRacingApps.Server;
import com.SIMRacingApps.SIMPluginCallbacks.SIMPluginCallback;
import com.SIMRacingApps.Util.Sound;

/**
 * This plug-in will count you down to your pit stall using audible sounds.
 * You can control its behavior with the following variables.
 * <pre>
 *    pit-count-down-device = A Sound Device
 *    pit-count-down-volume = 1.0
 *    pit-count-down-start  = 5
 *    pit-count-down-play10 = Y
 *    pit-count-down-play0  = Y
 *    pit-count-down-pattern = com/SIMRacingApps/SIMPluginCallbacks/Sounds/Clips/n%d.wav
 *        #some know patterns to spotter packs. Your version may vary
 *        C:\\Program Files (x86)\\iRacing\\sound\\spcc\\JJ Spotter Pack v6.51\\n%d.wav
 *        C:\\Program Files (x86)\\iRacing\\sound\\spcc\\Dale Jr Spotter Pack v2.4\\n%d_A1.wav
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
public class PitCountDown extends SIMPluginCallback {

    private final ArrayList<Sound> m_clips = new ArrayList<Sound>();
    private final boolean m_play10;
    private final boolean m_play0;
    private final String m_device;
    private final int m_startCount;
    
    private Double m_volume;
    
    /**
	 * Constructor. 
	 * This plug-in watches for your distance from you pit stall and counts you down.
	 *
	 * @param SIMPlugin An instance of the current SIM.
	 * @throws SIMPluginException If there's a problem constructing your plug-in.
	 */	
    public PitCountDown(SIMPlugin SIMPlugin) throws SIMPluginException {
		super(SIMPlugin,"Sounds.PitCountDown");
		
		m_device              = Server.getArg("pit-count-down-device");
		m_volume              = Server.getArg("pit-count-down-volume", -1.0);
		m_play10              = Server.getArg("pit-count-down-play10",true);
        m_play0               = Server.getArg("pit-count-down-play0",true);
		m_startCount          = (int)Math.min(10.0, Math.max(0.0, (double)Server.getArg("pit-count-down-start",5)));
        
		String defaultPattern = "com/SIMRacingApps/SIMPluginCallbacks/Sounds/Clips/n%d.wav";
		String soundPattern   = Server.getArg("pit-count-down-pattern",defaultPattern);
		
        for (int i=0; m_startCount > 0.0 && i <= 10; i++) {
            Sound clip = new Sound(m_device,String.format(soundPattern,i));
            if (!clip.getErrorMessage().isEmpty())
                clip = new Sound(m_device,String.format(defaultPattern,i));
            clip.setVolume(m_volume * 100.0);
            clip.setMinTimeBetweenPlays(15000);
            m_clips.add(clip);
        }
        
        Subscribe("Car/REFERENCE/Status");
        Subscribe("Session/DiffCars/REFERENCE/PITSTALL");
	}
	
	/**
	 * Called when the SIMPlugin is destroyed.
	 * It's always best to call super.destroy() for future compatibility.
	 */
    public void destroy() {
		super.destroy();
		
        synchronized (m_clips) {
		    for (int i=0; i < m_clips.size(); i++)
		        m_clips.get(i).close();
        }
	}
	
    /**
     * Gets the volume as a percentage.
     * 
     * <p>PATH = {@link #getVolume() /SIMPluginCallback/Sounds/PitCountDown/Volume}
     * 
     * @return The new volume percentage in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getVolume() {
        double volume = 0.0;
        //get the volume that the clip says, not m_volume as it could be -1
        synchronized (m_clips) {
            if (m_clips.size() > 0)
                volume = m_clips.get(0).getVolume();
        }
        return new Data("", volume * 100.0,"%",Data.State.NORMAL);
    }
    
    /**
     * Sets the volume as a percentage. Range 0.0 to 100.0
     * 
     * <p>PATH = {@link #setVolume(double) /SIMPluginCallback/Sounds/PitCountDown/setVolume/(PERCENTAGE)}
     * 
     * @param percentage The new volume percentage, between 0.0 and 100.0.
     * @return The new volume percentage in a {@link com.SIMRacingApps.Data} container.
     */
    public Data setVolume(double percentage) {
        synchronized (m_clips) {
    		for (int i=0; i < m_clips.size(); i++) {
    		    m_clips.get(i).setVolume(percentage / 100.0);
            }
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
    long m_lastTimePlayed = 0L;
    boolean m_beenOnTrack = false;
    
    @Override 
    public boolean ProcessData(SIMPlugin SIMPlugin, Map<String,Data> data) {
        if (data.isEmpty())
            return true;
        
        synchronized (m_clips) {
            String status = data.get("Car/REFERENCE/Status").getString();
            
            //set this flag to show we have left the pits
            if (status.equals(Car.Status.ONTRACK) || status.equals(Car.Status.APPROACHINGPITS))
                m_beenOnTrack = true;
                
            //upon existing the pit reset this flag so nothing will play unless we get on the track and pit again.
            if (status.equals(Car.Status.LEAVINGPITS) || status.equals(Car.Status.INVALID))
                m_beenOnTrack = false;
            
            if (m_beenOnTrack  //This is to stop it from playing when you start the race or qualifying from pit road
            && (status.equals(Car.Status.ONPITROAD) 
            ||  status.equals(Car.Status.ENTERINGPITSTALL)
            ||  status.equals(Car.Status.INPITSTALL))
            ) {
                double time = data.get("Session/DiffCars/REFERENCE/PITSTALL").getDouble();
                
                double duration = m_clips.get(10).getDuration(); 
                if (time > (10.0 + duration)) {
                    time -= (duration - 1.0);
                }
                
                int seconds = (int)Math.round(time);
                
                if (seconds >= 0 && seconds <= 10) {
                    if (seconds == 0 
                    ||  status.equals(Car.Status.ENTERINGPITSTALL)
                    ||  status.equals(Car.Status.INPITSTALL)
                    ) {
                        //stop any other clip that may be playing
                        for (int i=1; i <= 10; i++)
                            m_clips.get(i).stop();
                            
                        //don't play zero unless something else was recently played
                        //this prevents zero from playing when you drop into your pit from a reset.
                        if (m_play0 && (m_lastTimePlayed > (System.currentTimeMillis() - 5000L)))
                            m_clips.get(0).play();
                    }
                    else
                    if (seconds == 10 || seconds > m_startCount) {
                        if (m_play10) {
                            //stop any other clip that may be playing
                            for (int i=0; i < 10; i++)
                                m_clips.get(i).stop();
                                
                            m_clips.get(10).play();
                            m_lastTimePlayed = System.currentTimeMillis();
                        }
                    }
                    else {
                        //stop any other clip that may be playing
                        for (int i=0; i <= 10; i++)
                            if (i > seconds)
                                m_clips.get(i).stop();
                            
                        m_clips.get(seconds).play();
                        m_lastTimePlayed = System.currentTimeMillis();
                    }
                }
            }
        }
        return true;
    }
}