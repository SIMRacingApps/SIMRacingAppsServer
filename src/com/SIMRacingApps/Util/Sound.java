package com.SIMRacingApps.Util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.SIMRacingApps.Server;

/**
 * This class allows you to load a sound file (WAV) and play it.
 * 
 * @author Jeffrey Gilliam
 * @copyright Copyright (C) 2015 Jeffrey Gilliam
 * @since 1.0
 * @license Apache License 2.0
 */
public class Sound {

    private Clip m_clip;
    private FindFile m_file;
    private AudioInputStream m_audioStream;
    private double m_length = 0.0;
    private String m_errorMessage = "";
    private long m_lastTimePlayed = 0L;
    private long m_minTimeBetweenPlays = 0L;
    private FloatControl m_volumeControl = null;
    private FloatControl m_balanceControl = null;
    private double m_volumePercentage = 1.0;
    static private double m_masterVolumePercentage = -1.0;
    static private Map<Sound,Sound> m_clips = new HashMap<Sound,Sound>();
    
    /**
     * Constructor. Creates a Sound object. 
     * If the device name is not found, it falls back to the default device.
     * The device name can be the mixer number, but be aware that could change if new sound devices are installed.
     * If the filename is not found, no exception is thrown, it just doesn't play anything.
     * 
     * @param deviceName The Sound device name.
     * @param filename The path to the audio file to play. Can be a resource in the classpath. 
     */
    public Sound(String deviceName, String filename) {
        Sound.loadMixers();
        try {
            m_clip = _getClip(deviceName);
            if (m_clip != null) {
                m_file = new FindFile(filename);
                m_audioStream = AudioSystem.getAudioInputStream(m_file.getBufferedInputStream());
                m_clip.open(m_audioStream);
                m_length = (double)m_clip.getMicrosecondLength() / 1000000.0;
                
                if(m_clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
                    m_volumeControl = (FloatControl) m_clip.getControl(FloatControl.Type.MASTER_GAIN);
                
                if(m_volumeControl == null && m_clip.isControlSupported(FloatControl.Type.VOLUME))
                    m_volumeControl = (FloatControl) m_clip.getControl(FloatControl.Type.VOLUME);
                
                if (m_masterVolumePercentage < 0.0)
                    m_masterVolumePercentage = Server.getArg("volume",Server.getArg("sound-volume", 1.0));
                
                setVolume(1.0); //will get constrained to the master level
                    
                if (m_clip.isControlSupported(FloatControl.Type.BALANCE))
                    m_balanceControl = (FloatControl) m_clip.getControl(FloatControl.Type.BALANCE);
                
                if (m_balanceControl == null && m_clip.isControlSupported(FloatControl.Type.PAN))
                    m_balanceControl = (FloatControl) m_clip.getControl(FloatControl.Type.PAN);
                
                synchronized (m_clips) {
                    m_clips.put(this, this);
                }
            }
        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            m_errorMessage = e.getMessage();
            Server.logStackTrace(e);
        }        
    }
    
    /**
     * Returns the error message. Returns blank if no error occured.
     * @return The error message.
     */
    public String getErrorMessage() {
        return m_errorMessage;
    }
    
    /**
     * Returns the duration of the clip in seconds down to the microsecond.
     * 
     * @return The duration of the clip in seconds.
     */
    public double getDuration() {
        return m_length;
    }
    
    /**
     * Returns the system time of the last time this clip was played.
     * Use to compare with System.currentTimeMillis().
     * 
     * @return The last time played as a long
     */
    public long getLastTimePlayed() {
        return m_lastTimePlayed;
    }
    
    /**
     * Returns the master volume percentage
     * 
     * @return The master volume as a percentage in the range between 0.0 to 1.0
     */
    static public double getMasterVolume() { return m_masterVolumePercentage; }
    
    /**
     * Sets the master volume percentage. The current volume is adjusted to compensate.
     * @param percentage The percentage in the range of 0.0 to 1.0
     */
    static public void   setMasterVolume(double percentage) {
        if (percentage >= 0.0 && percentage <= 1.0 && percentage != m_masterVolumePercentage) {
            m_masterVolumePercentage = percentage;
            //reset the volume on all clips to the new master
            synchronized (m_clips) {
                Iterator<Entry<Sound, Sound>> itr = m_clips.entrySet().iterator();
                while (itr.hasNext()) {
                    Sound clip = itr.next().getKey();
                    clip.setVolume(clip.m_volumePercentage);
                }
            }
        }
    }
    
    /**
     * Get the volume for this clip as a percentage of the volume range.
     * 
     * @return The volume as a percentage in the range between 0.0 to 1.0
     */
    public double getVolume() {
        if (m_volumeControl != null && m_masterVolumePercentage > 0) {
            float max = m_volumeControl.getMaximum();
            float min = m_volumeControl.getMinimum();
            float range = max - min;
            float value = m_volumeControl.getValue();
            if (range > 0)
                m_volumePercentage = ((value - min) / range) / m_masterVolumePercentage;
        }
        return m_volumePercentage;
    }
    
    /**
     * Set the volume for this clip as a percentage of the volume range.
     * 
     * @param percentage The percentage in the range of 0.0 to 1.0
     */
    public void setVolume(double percentage) {
        if (m_volumeControl != null && percentage >= 0.0 && percentage <= 1.0) {
            float max = m_volumeControl.getMaximum();
            float min = m_volumeControl.getMinimum();
            float range = max - min;
            float value = min + (((float)m_masterVolumePercentage * range) * (float)percentage);
            //Server.logger().fine("Sound.setVolume("+percentage+") = "+(((float)m_masterVolumePercentage) * (float)percentage));
            m_volumeControl.setValue( value );
            m_volumePercentage = percentage;
        }
    }
    
    /**
     * Sets the balance position as a percentage of left to right. .5 is centered.
     * 
     * Note: If the source file is mono, you cannot change the balance.
     * Convert it to stereo.
     * @param percentage The balance position as a percentage between 0.0 to 1.0.
     */
    public void setBalance(double percentage) {
        if (m_balanceControl != null && percentage >= 0.0 && percentage <= 1.0) {
            float max = m_balanceControl.getMaximum();
            float min = m_balanceControl.getMinimum();
            float range = max - min;
            float value = min + (range * (float)percentage);
            m_balanceControl.setValue( value );
        }
    }
    
    /**
     * Starts the sound playing in the background.
     */
    public void play() {
        if ((m_lastTimePlayed + m_minTimeBetweenPlays) <= System.currentTimeMillis()) {
            stop();
            if (m_clip != null) {
                Server.logger().fine("Playing "+m_file.getFileFound());
                setVolume(m_volumePercentage); //This will adjust the volume if the master has changed.
                m_clip.setFramePosition(0);
                m_clip.start();
                m_lastTimePlayed = System.currentTimeMillis();
            }
        }
    }
    
    /**
     * Sets the minimum time between plays in milliseconds
     * 
     * @param milliseconds Number of milliseconds to wait. Defaults to zero.
     */
    public void setMinTimeBetweenPlays(long milliseconds) {
        m_minTimeBetweenPlays = milliseconds;
    }
    
    /**
     * Plays the sound, count, times in the background.
     * To play it unlimited pass in Integer.MAX_VALUE.
     * @param count The number of times to play the sound.
     */
    public void loop(int count) {
        if (m_clip != null)
            m_clip.loop(count);
    }

    /**
     * Waits for the sound to stop playing, then returns.
     */
    public void drain() {
        if (m_clip != null) 
            m_clip.drain();
    }
    
    /**
     * Stops the currently playing sound.
     */
    public void stop() {
        if (m_clip != null && m_clip.isRunning())
            m_clip.stop();
    }
    
    /**
     * Closes the sound. It will not play again after calling this.
     */
    public void close() {
        if (m_file != null) {
            m_file.close();
            m_file = null;
        }
        if (m_clip != null) {
            m_clip.close();
            m_clip = null;
        }
        synchronized (m_clips) {
            if (m_clips.containsKey(this))
                m_clips.remove(this);
        }
    }
    
    @Override
    public void finalize() throws Throwable {
        close();
        super.finalize();
    }

    private Clip _getClip(String name) {
        Clip clip = null;
        //first try the name given to us
        Mixer mixer = m_clipMixers.get(name.toUpperCase().trim());
        if (mixer != null) {
            Line.Info clipInfo = new Line.Info(Clip.class);
            try {
                clip = (Clip) mixer.getLine(clipInfo);
            } catch (LineUnavailableException e) {}
        }
        
        //if there's no clip, try the global device
        if (clip == null) {
            mixer = m_clipMixers.get(Server.getArg("sound-device","").toUpperCase().trim());
            if (mixer != null) {
                Line.Info clipInfo = new Line.Info(Clip.class);
                try {
                    clip = (Clip) mixer.getLine(clipInfo);
                } catch (LineUnavailableException e) {}
            }
        }
        
        //if still no clip, try the system default device. It should always work unless there's no sound card.
        if (clip == null && Server.getArg("sound", true)) {
            try {
                clip = (Clip) AudioSystem.getClip();
                if (!name.isEmpty())
                    Server.logger().warning("Sound Device("+name+") not found, using default");
            } catch (LineUnavailableException e1) {
                Server.logStackTrace(e1);
            }
        }
        return clip;
    }
    
    private static Map<String,Mixer> m_clipMixers = null;
    
    public static void loadMixers() {
        if (m_clipMixers == null) {
            m_clipMixers = new HashMap<String,Mixer>();
            
            if (Server.getArg("sound", true)) {
                Mixer.Info[] mixersInfo = AudioSystem.getMixerInfo();
            
                Line.Info clipInfo   = new Line.Info(Clip.class);
                
                for (int i=0; i < mixersInfo.length; i++) {
                    Mixer mixer = AudioSystem.getMixer(mixersInfo[i]);
                    
                    if (mixer.isLineSupported(clipInfo)) {
                        m_clipMixers.put(mixersInfo[i].getName().toUpperCase(), mixer);
                        m_clipMixers.put(Integer.toString(i),mixer);
                        
                        Server.logger().info(String.format(
                                "Sound Device[%d] = [%s]", 
                                i, mixersInfo[i].getName()
                        ));
                    }
                }
            }
        }
    }

    //java -classpath SIMRacingAppsServer.exe com.SIMRacingApps.Util.Sound
    public static void main(String[] args) {

        Sound.loadMixers();

        Sound soundClip = new Sound(
//                "Primary Sound Driver",
                "Z300 (Turtle Beach Z300 with Dolby Headphone)",
//                "Realtek HD Audio 2nd output (2- Realtek High Definition Audio)",
//                "C:/Program Files (x86)/iRacingMembers/sound/spcc/JJ Spotter Pack v6.51/10togo.wav"
//                "C:/temp/10togo.wav"
                "com/SIMRacingApps/SIMPluginCallbacks/Sounds/Clips/n10.wav"
              );
        soundClip.setVolume(.9);
        soundClip.setBalance(0.0);
        Server.logger().info(String.format("Duration = %f",soundClip.getDuration()));
        Server.logger().info("before play() left");
        soundClip.play();
        Server.logger().info("after  play() left");
        try {
            Thread.sleep((long)((soundClip.getDuration() / 4.0) * 1000.0));
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Server.logger().info("before shifting balance to the right");
        soundClip.setBalance(.5); //when this switches is based on the buffer of your sound card.
        soundClip.drain();
        Server.logger().info("after  drain() left");
        Server.logger().info("before play() both");
        soundClip.play();
        Server.logger().info("after  play() both");
        soundClip.drain();
        Server.logger().info("after  drain() both");
        
        soundClip.setBalance(1.0);
        Server.logger().info("before play() right");
        soundClip.play();
        Server.logger().info("after  play() right");
        soundClip.drain();
        Server.logger().info("after  drain() right");
        soundClip.close();
        Server.logger().info("after  close()");
        
    }

}
