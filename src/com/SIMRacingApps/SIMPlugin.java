package com.SIMRacingApps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.SIMRacingApps.Data.State;
import com.SIMRacingApps.SIMPluginCallbacks.SIMPluginCallback;
import com.SIMRacingApps.Util.FindFile;
import com.SIMRacingApps.Util.Sound;

/**
 * The SIMPlugin Class defines a generic interface for retrieving SIM data.
 * SIMulators should inherit this class and override the methods for which it can provide data for.
 * Each SIM should try to return the same set of enumerated values defined by the overridden method to allow clients to make hard coded decisions based on those.
 * It doesn't have to do this if the data is to simply be displayed.
 * I will have to admit, I based these on iRacing (my primary SIM), so other SIMs will have to conform.
 * <p>
 * To create a connector, you do not call "new SIMPlugin(String)" as this class should be considered abstract.
 * Instead call the static factor method {@link com.SIMRacingApps.SIMPlugin#createSIMPlugin(String) createSIMPlugin(String)},
 * where the String argument is the SIM's name. See below for supported SIMs.
 * <p>
 * Once a connector has been created, you then call {@link com.SIMRacingApps.SIMPlugin#run(Callback)} method
 * passing in an instance of a {@link com.SIMRacingApps.SIMPlugin.Callback Callback} Class 
 * that you must implement to get called when data is available. 
 * <p>
 * So, to make things a little easier, I have 2 implementations for you to use out of the box.
 * For Java Applications, see {@link com.SIMRacingApps.SIMPluginAWTEventDispatcher}. 
 * This dispatcher allows you to register standard PropertyListener's that will receive events when data they are listening to changes.
 * For Web Server Applications, I have implemented several J2EE Servlets for different protocols, including HTTP and Web Socket.
 * See {@link com.SIMRacingApps.servlets} for details.
 * <p>
 * The most important SIMPlugin method is {@link com.SIMRacingApps.SIMPlugin#getData(String) getData(String)}. 
 * This method takes a string that is formatted as a path to the data you want to get or change.
 * It breaks down this path and calls the corresponding method to retrieve the data.
 * The method the path maps to must return a {@link com.SIMRacingApps.Data Data} object and be overloaded to take String arguments.
 * In a pure Java Application, you can also get a reference to these objects through the SIMPlugin instance and call them directly.
 * Here are the main path entry points. See the documentation for each one for more details on the data they can provide.
 * <pre>
 *    //NOTE: The leading slash is optional
 *    
 *    {@link com.SIMRacingApps.SIMPlugin /(method)}/(args)
 *    {@link com.SIMRacingApps.TeamSpeak /TeamSpeak}/(method)/(args)
 *    {@link com.SIMRacingApps.Session /Session}/(method)/(args)
 *        {@link com.SIMRacingApps.Track /Session/Track}/(method)/(args)
 *        {@link com.SIMRacingApps.Car /Session/Car}/(car)/(method)/(args)
 *            {@link com.SIMRacingApps.Gauge /Session/Car/(car)/Gauge}/(gaugeType)/(method)/(args)
 *    
 *    //Also, the Car and Track can be accessed without the /Session prefix on the path as follows.
 *    
 *    {@link com.SIMRacingApps.Track /Track}/(method)/(args)
 *    {@link com.SIMRacingApps.Car /Car}/(car)/(method)/(args)
 *        {@link com.SIMRacingApps.Gauge /Car/(car)/Gauge}/(gaugeType)/(method)/(args)
 *        
 *    //For direct access to the SIM's data, put the SIM name as the first argument followed by the path.
 *    //Refer to the SIM's documentation for available paths. It is beyond the scope of these documents to do so.
 *    //If you find yourself needing to use this, consider implementing a generic method, 
 *    //so clients do not have to be SIM specific.
 *    
 *    {@link com.SIMRacingApps.SIMPlugins.iRacing.iRacingSIMPlugin /iRacing}/(args)
 *    
 *    //TODO: Put a link to all supported SIMs
 * </pre>
 * <ul>
 * <li>Data d = connector.getData("/Session/Car/LEADER/Lap/Completed");</li>
 * <li>Data d = connector.getData("/Car/LEADER/Lap/Completed");</li>
 * <li>//SIM specific, not recommended</li>
 * <li>Data d = connector.getData("/iRacing/SessionInfo/Sessions/1/ResultsPositions/0/LapsCompleted");</li>
 * <li>Data d = connector.getSIMData("SessionInfo","Sessions","1","ResultsPositions","0","LapsCompleted");</li>
 * </ul>
 * @author Jeffrey Gilliam
 * @since 1.0
 * @copyright Copyright (C) 2015 - 2017 Jeffrey Gilliam
 * @license Apache License 2.0
 */
public class SIMPlugin {

    private String m_SIMName = "Generic";

    /**
     * This class returns the exception message thrown by the SIMPlugin Class
     */
    public static class SIMPluginException extends Exception {
        private static final long serialVersionUID = -7420049319989282595L;

        public SIMPluginException(String s) {
            super(s);
        }
    }

    /**
     * This interface is used to call back to your implementation as data is received.
     * The methods are called in the same thread that the SIMPlugin.run(Callback) method was invoked in.
     * Each method should return true to continue to process data.
     * Returning false will cause the SIMPlugin.run(Callback) to return.
     */
    public interface Callback {
        /** 
         * Called when data is ready to be used.
         * Time spent in this call should be less than 16ms to prevent packet loss because this routine
         * is single threaded with the SIMs notification event.
         * 
         * @param SIMPlugin The SIM Plugin instance.
         * @param ips       The current iterations per second(IPS) value.
         * @return true to continue receiving data, false to stop
         * @throws SIMPluginException An problem occurred in the SIMPlugin 
         */
        public boolean DataReady(SIMPlugin SIMPlugin, Integer ips)  throws SIMPluginException;
        
        /** 
         * Called when the connector is not getting data from the SIM.
         * But, you are still able to call methods on the connector to get data anyway, 
         * it just might be stale data or what the SIM wants you to get when the SIM is not running.
         * 
         * @param SIMPlugin The SIM Plugin instance
         * @return true to continue receiving data, false to stop
         * @throws SIMPluginException If a problem occurred in the SIMPlugin 
         */
        public boolean Waiting(SIMPlugin SIMPlugin) throws SIMPluginException;
    }

    /**
     * A static factory method for creating a SIM specific connector and returning it.
     * <p>
     * For example: SIMPlugin c = SIMPlugin.createSIMPlugin("iRacing");
     * @param SIM The name of a SIM supported by a SIMPlugin class implementation in the com.SIMRacingApps.SIMPlugins package.
     * @return A SIMPlugin
     * @throws SIMPluginException If there's a problem creating a SIMPlugin instance for this SIM.
     */
    public static SIMPlugin createSIMPlugin(String SIM) throws SIMPluginException {

        String SIMClass = "com.SIMRacingApps.SIMPlugins."+SIM+"."+SIM+"SIMPlugin";
        SIMPlugin c = null;
        try {
            c = (SIMPlugin) Thread.currentThread().getContextClassLoader().loadClass(SIMClass).newInstance();
            c.m_SIMName = SIM;
            Server.logger().info(c.getVersion().getString());
        } 
        catch (InstantiationException e) {
            Server.logger().throwing("SIMPlugin","createSIMPlugin",e);
            throw new SIMPluginException("InstantiationException Caught("+SIMClass+"): " + e.getMessage());
        } 
        catch (IllegalAccessException e) {
            Server.logger().throwing("SIMPlugin","createSIMPlugin",e);
            throw new SIMPluginException("IllegalAccessException Caught("+SIMClass+"): " + e.getMessage());
        } catch (ClassNotFoundException e) {
            Server.logger().throwing("SIMPlugin","createSIMPlugin",e);
            throw new SIMPluginException("ClassNotFoundException Caught("+SIMClass+"): " + e.getMessage());
        }
        return c;
    }

    private TeamSpeak                       m_teamspeak = null;
    private ArrayList<SIMPluginCallback>    m_callbacks = new ArrayList<SIMPluginCallback>();
    private Map<String,SIMPluginCallback>   m_loadedCallbacks = new HashMap<String,SIMPluginCallback>();
    
    /**
     * Class constructor. Protected so it cannot be instantiated. Meant to be called by the SIM's constructor.
     * @throws SIMPluginException If the SIMPlugin cannot by created.
     * 
     */
    protected SIMPlugin() throws SIMPluginException {
        Boolean teamspeak = Server.getArg("teamspeak", true);
        if (teamspeak) {
            String teamspeakClient = Server.getArg("teamspeak-client", "localhost");
            m_teamspeak = new TeamSpeak(this,teamspeakClient);
            m_teamspeak.startListener();
        }
    }

    /**
     * Closes the connection to the SIM and kills any threads it may have spawned.
     */
    public void close() {
        //close the SIMPluginCallbacks
        for (int i=0; i < m_callbacks.size(); i++) {
            m_callbacks.get(i).destroy();
        }
        m_callbacks = new ArrayList<SIMPluginCallback>();
        
        if (m_teamspeak != null)
            m_teamspeak.disconnect();
        m_teamspeak = null;
    }

    /**
     * Creates a callback instance from the specified name 
     * @param name The callback class name relative to com.SIMRacingApps.SIMPluginCallbacks.
     * @return true if callback was loaded, false if not.
     */
    public boolean addCallback(String name) {
        if (name == null || name.isEmpty())
            return false;

        if (!m_loadedCallbacks.containsKey(name)) {
            String callbackClass = "com.SIMRacingApps.SIMPluginCallbacks."+name;
            SIMPluginCallback c = null;
            try {
                Class<?> cl = Thread.currentThread().getContextClassLoader().loadClass(callbackClass);
                c = (SIMPluginCallback) cl.getConstructor(SIMPlugin.class).newInstance(this);
                m_callbacks.add( c );
                m_loadedCallbacks.put( name, c );
                Server.logger().info("SIMPlugin Callback Loaded "+callbackClass);
            } 
            catch (InstantiationException e) {
                Server.logStackTrace(e);
                return false;
            } 
            catch (IllegalAccessException e) {
                Server.logStackTrace(e);
                return false;
            } 
            catch (ClassNotFoundException e) {
                Server.logStackTrace(e);
                return false;
            } catch (IllegalArgumentException e) {
                Server.logStackTrace(e);
                return false;
            } catch (InvocationTargetException e) {
                Server.logStackTrace(Level.WARNING,"",e.getCause());
                return false;
            } catch (NoSuchMethodException e) {
                Server.logStackTrace(e);
                return false;
            } catch (SecurityException e) {
                Server.logStackTrace(e);
                return false;
            }
        }
        return true;
    }
    
    /**
     * Returns an instance to a callback. 
     * If the callback is not loaded, it returns null.
     * @param name The callback name as it is named in the list of callbacks.
     * @return an instance to the callback or null if not loaded.
     */
    public SIMPluginCallback getCallback(String name) {
        return m_loadedCallbacks.get(name);
    }
    
    /**
     * Removes a callback. If it is not loaded, silently ignores the call. 
     * @param name The callback name as it is named in the list of callbacks.
     */
    public void removeCallback(String name) {
        SIMPluginCallback callback = m_loadedCallbacks.get(name);
        if (callback != null) {
            callback.destroy();
            m_loadedCallbacks.remove(name);
            Server.logger().info("SIMPlugin Callback ["+name+"] removed.");
        }
    }
    
    /**
     * Returns the name of the SIM this connector is for.
     * 
     * <p>PATH = {@link #getSIMName() /SIMName}
     * 
     * @return The SIM name in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getSIMName() {
        return new Data("SIMPluginName",m_SIMName,"string");
    }

    private String m_Play = "";
    /**
     * Returns the name of the currently playing recorded file.
     * @return The filename that is playing in a {@link com.SIMRacingApps.Data} container. 
     */
    public Data getPlay() {
        return new Data("Play",m_Play,"filename",Data.State.NORMAL);
    }
    
    /**
     * Sets the filename to be played back.
     * @param Filename The complete path to the file to play.
     * @return The filename  in a {@link com.SIMRacingApps.Data} container.
     */
    public Data setPlay(String Filename) {
        m_Play = Filename;
        return getPlay();
    }

    private String m_Record = "";
    
    /**
     * Returns the name of the file being recorded.
     * 
     * @return The filename in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getRecord() {
        return new Data("Record",m_Record.isEmpty() ? "" : new File(m_Record).getAbsolutePath(),"filepath",Data.State.NORMAL);
    }
    
    /**
     * Sets the filename to record this session to.
     * If it is not a fully qualified path to a file,
     * then it will be saved in the userpath/recordings folder.
     * 
     * @param Filename The complete path to the file to record to.
     * @return The filename in a {@link com.SIMRacingApps.Data} container.
     */
    public Data setRecord(String Filename) {
        if (Filename.length() >= 3 
        &&  Filename.substring(1,1).equals(":") 
        && (Filename.substring(2,1).equals("\\") || Filename.substring(2,1).equals("/"))
        ) {
            m_Record = Filename;
        }
        else
        if (!Filename.isEmpty() 
        && (Filename.substring(0,1).equals("\\") || Filename.substring(0,1).equals("/"))
        ) {
            m_Record = Filename;
        }
        else
        if (!Filename.isEmpty()) {
            m_Record = FindFile.getUserPath()[0] + "/recordings/" + Filename;
        }
        else {
            m_Record = Filename;
        }

        if (!m_Record.isEmpty()) {
            File file = new File(m_Record);
            File parent = new File(file.getParent());
            parent.mkdirs();
        }
        
        return getRecord();
    }

    private String m_startingversion = "";
    /**
     * Returns the Data Version of where the playing file should start.
     * 
     * @return The starting version in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getStartingVersion() {
        return new Data("StartingVersion",m_startingversion,"String",Data.State.NORMAL);
    }
    /**
     * Sets the Data Version of where the playing file should start.
     * This is passed to the SIM, so the format will be SIM specific.
     * 
     * @param startingversion The starting version in the format specified by the SIM.
     * 
     * @return The starting version in a {@link com.SIMRacingApps.Data} container.
     */
    public Data setStartingVersion(String startingversion) {
        m_startingversion = startingversion;
        return getStartingVersion();
    }

    private String m_endingversion = "";
    /**
     * Returns the Data Version of where the playing file should stop.
     * This is passed to the SIM, so the format will be SIM specific.
     * 
     * @return The ending version.
     */
    public Data getEndingVersion() {
        return new Data("EndingVersion",m_endingversion,"long",Data.State.NORMAL);
    }
    /**
     * Sets the Data Version of where the playing file should stop.
     * 
     * @param endingversion The ending version in the format specified by the SIM.
     * 
     * @return The ending version in a {@link com.SIMRacingApps.Data} container.
     */
    public Data setEndingVersion(String endingversion) {
        m_endingversion = endingversion;
        return getEndingVersion();
    }

    /**
     * Returns the value of arg from the settings.txt file.
     *
     * @param arg The name of the setting value to return.
     * @param defaultValue (Optional) The value to return if the arg is not defined. Defaults to empty string.
     * @return The value of arg or the defaultValue if not found
     */
    public Data getSetting(String arg,String defaultValue) {
        return new Data("/Setting/"+arg,Server.getArg(arg,defaultValue),"",Data.State.NORMAL);
    }
    public Data getSetting(String arg) { return getSetting(arg,""); }
    
    private boolean m_sync = true;
    /**
     * Returns the sync flag. The sync flag is used to slow down the playing file to real-time. 
     * Turn sync off, to read as fast as you can.
     * 
     * @return The sync flag
     */
    public Data getSync() {
        return new Data("Sync",m_sync,"boolean",Data.State.NORMAL);
    }
    /**
     * Sets the the sync flag. The sync flag is used to slow down the playing file to real-time. 
     * Turn sync off, to read as fast as you can.
     * 
     * @param sync Y or N
     * 
     * @return The sync flag in a {@link com.SIMRacingApps.Data} container.
     */
    public Data setSync(boolean sync) {
        m_sync = sync;
        return getSync();
    }
    public Data setSynce(String sync) { return setSync(new Data("",sync).getBoolean()); }
    
    /**
     * Returns the master volume setting as a percentage.
     * 
     * <p>PATH = {@link #getVolume() /Volume}
     * 
     * @return The master volume as a percentage in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getVolume() {
        return new Data("Volume",Sound.getMasterVolume() * 100.0,"%",Data.State.NORMAL);
    }
    
    /**
     * Returns the master volume setting as a percentage.
     *
     * <p>PATH = {@link #setVolume(double) /Volume/(PERCENTAGE)}
     * 
     * @param percentage The new volume as a percentage. Range 0.0 to 100.0.
     * @return The master volume as a percentage in a {@link com.SIMRacingApps.Data} container.
     */
    public Data setVolume(double percentage) {
        Sound.setMasterVolume(percentage / 100.0); 
        return getVolume();
    }
    public Data setVolume(String percentage) {
        return setVolume(Double.parseDouble(percentage));
    }
    
    /**
     * Returns a value from the SIM.
     * The path argument, is formatted like a URL with slashes as separators.
     * A leading slash is optional.
     * <p>
     * The first argument is tested to see if it matches a public method on this class that takes the same number of String arguments, then calls it. 
     * If it's a "get", the "get" is optional, but "set", "decrement", and "increment" are required.
     * If the first argument matches the name of the SIM, all the arguments are passed to the SIM version of {@link com.SIMRacingApps.SIMPlugin#getSIMData(String...)}.
     * It is not recommended to call the SIM version directly as this will tie your client to that SIM.
     * Although you will have to use it if the API is missing something you need, 
     * but consider enhancing the generic API instead.
     * <p>
     * For Examples see {@link com.SIMRacingApps.SIMPlugin}.
     * 
     * @param path The path to the requested value with arguments separated by slashes in a RESTfull syntax.
     * @return null if return value is void, else the value in a {@link com.SIMRacingApps.Data} container. 
     *         If the path was invalid, then it returns an error message in a {@link com.SIMRacingApps.Data} container with the State set to ERROR.
     * @throws SIMPluginException for other syntax or runtime errors
     */
    public Data getData(String path) throws SIMPluginException {
        return callMethod(this,path);
    }
    
    /**
     * 3 methods work together to poll the SIM for data (isActive, waitForDataReady, isConnected).
     * <p>
     * isActive() is called first and every time through the loop. It returns true if the SIM is ready to accept calls.
     * The SIM can return false for various reasons, with the default action would be to exit your program because of a fatal error.
     * It could also mean you have reached the end of a recorded file that you told it to play. In that case, you might want to simply start it over.
     * <p>
     * waitForDataReady() is called to see if the SIM has any data. It should return true when data is available from the SIM. 
     * A return of false means no data was available and you should try again. 
     * Each SIM should put any throttling in this implementation to be nice to other processes and prevent a tight loop.
     * <p>
     * isConnected() will return true if the SIM is running, false if not.
     * <p>
     * The following code illustrates how to use these methods.
     * <pre>
     * while (connector.isActive()) {
     *     if (connector.waitForDataReady()) {
     *         //call connector.getData(datapath);
     *     }
     *     else {
     *         if (!connector.isConnected()) {
     *             //handle SIM not running
     *             //call connector.getData(datapath); //is allowed here
     *         }
     *     }
     * }
     * </pre>
     * Simulators should override these abstract methods and implement them.
     * 
     * @return true or false
     */
    protected boolean isActive()               { return true; }
    protected boolean waitForDataReady()       { try {Thread.sleep(16);} catch (InterruptedException e) {} return false; }
    protected boolean isConnected()            { return false; }
    
    /**
     * Starts the run loop that notifies the caller when data is available through the Callback class.
     * Each caller must provide an instance Callback class to process the data.
     * Note: If you don't call this in a new Thread, it will not return until your Callback class tells it to exit.
     * @param callback (Optional) An instance a callback implementation to process the data
     */
    public void run(Callback callback)
    {
        double sessionstart = 0.0, prevsessiontime = 0.0;
        int  ips = 0;
        int  ipsnext = 0;
        long ipstime = 0L;
        ArrayList<Callback> callbacks = new ArrayList<Callback>();
        
        if (callback != null)
            callbacks.add(callback);
            
        //make a copy of the callbacks so we can add one if needed
        for (int i=0; i < m_callbacks.size(); i++) {
            callbacks.add(m_callbacks.get(i));
        }
            
        Server.logger().info("run() called");
        while (isActive()) {
            try {
                if (waitForDataReady()) {
                    for (int i=0; i < callbacks.size(); i++) {
                        synchronized (this) {
                            if (callbacks.get(i) != null && !callbacks.get(i).DataReady(this,ips)) {
                                Server.logger().info(callbacks.get(i).getClass().getName() + ".DataReady() is requesting run() to exit...");
                                close();
                                return;
                            }
                        }
                    }
                    double currenttime = System.currentTimeMillis()/1000.0;

                    //sync with the times in the data, else if it gets too fast, slow it down
                    if (!getPlay().getString().isEmpty() && getSync().getBoolean()) {
                        double sessiontime = getSession().getTimeElapsed().getDouble(); //number if seconds since the session began

                        if (sessionstart == 0.0
                        || sessiontime < prevsessiontime  //new session started, recalibrate
                        ) {
                        	//initialize to when the session began
                            sessionstart = currenttime - sessiontime;
                        }
                        else {
                            double sleeptime = (sessionstart + sessiontime) - currenttime;

//                                if (sleeptime > 0.016) {
                            if (sleeptime > 0.0) {
                                try {
                                    Thread.sleep(Math.round(sleeptime * 1000));
                                }
                                catch (InterruptedException e) {
                                }
                            }
                        }
                        prevsessiontime = sessiontime;
                    }

                    if (ipstime == System.currentTimeMillis()/1000)  //floor the time to seconds
                        ipsnext++;
                    else {
                        ipstime = System.currentTimeMillis()/1000;
                        if (ipsnext != ips && ipsnext < 8 && Server.getArg("log-ips",true)) {
                            Server.logger().info(String.format("IPS(%d) dropped below threshold(8), DataVersion=(%s)", ipsnext,this.getSession().getDataVersion().getString()));
                        }
//                        else
//                        if (ipsnext != ips && ipsnext < 25 && Server.getArg("log-ips",true)) {
//                            Server.logger().fine(String.format("IPS(%d) dropped below threshold(25), DataVersion=(%s)", ipsnext,this.getSession().getDataVersion().getString()));
//                        }
//                        else
//                        if (ipsnext != ips && ipsnext < 40 && Server.getArg("log-ips",true)) {
//                            Server.logger().finer(String.format("IPS(%d) dropped below threshold(40), DataVersion=(%s)", ipsnext,this.getSession().getDataVersion().getString()));
//                        }
                        else
                        if (ipsnext != ips && ipsnext < 25 && Server.getArg("log-ips",true)) {
                            if (Server.isLogLevelFinest())
                                Server.logger().finest(String.format("IPS(%d) dropped below threshold(25), DataVersion=(%s)", ipsnext,this.getSession().getDataVersion().getString()));
                        }
                        ips = ipsnext;
                        ipsnext = 0;
                    }
                }
                else
                if (!isConnected()) {
                    for (int i=0; i < callbacks.size(); i++) {
                        synchronized (this) {
                            if (callbacks.get(i) != null && !callbacks.get(i).Waiting(this)) {
                                Server.logger().info(callbacks.get(i).getClass().getName() + ".Waiting() is requesting run() to exit...");
                                close();
                                return;
                            }
                        }
                    }
                }
                
                //let teamspeak do it's updates
                if (m_teamspeak != null)
                    m_teamspeak.update();
                
            }
            catch (Exception e) {
                Server.logStackTrace(Level.SEVERE,"Exception",e);
                close();
                return;
            }
        }
        
        if (getPlay().getString().isEmpty())
            Server.logger().info("run() is exiting...");
        else
            Server.logger().info("Playback file EOF, run() is exiting...");
        close();
    }
    public void run() {
        run(null);
    }
    
//    /**
//     * Records a set command in the file being recorded.
//     * @param cmd String to record
//     */
//    public void    recordString(String cmd) { /*System.err.printf("%s.recordString(%s)\n", this.getClass().getName(), cmd);/**/ }

    private Session m_session = null;
    /**
     * Returns an instance to the SIM's session.
     * Each SIM should override this method and return a SIM specific session instance.
     * @return A session as defined by {@link com.SIMRacingApps.Session}
     */
    public Session getSession() { 
        if (m_session == null) {
            new Session(this);
        }
        return m_session;
    }

    private String m_version = null;
    /**
     * Returns the version of the SIMRacingApps Plugin as a String in a printable format.
     * 
     * <p>PATH = {@link #getVersion() /Version}
     * 
     * @return The Server Version for this SIM
     */
    public Data getVersion() {
        Data d = new Data("Version",String.format("%s Plugin Version: Unknown",getSIMName().getString()),"String");
        if (m_version == null) {
            InputStream in;
            Properties version = new Properties();
            try {
                in = this.getClass().getClassLoader().getResourceAsStream("com/SIMRacingApps/SIMPlugins/"+getSIMName().getString()+"/version.properties");
                if (in != null) {
                    //in = new FileInputStream(config.getServletContext().getRealPath("") + "version.properties");
                    version.load(in);
                    in.close();
                    m_version = String.format("%s Plugin Version: %s.%s Build-%s",
                                       getSIMName().getString(),
                                       (String)version.get("major"),
                                       (String)version.get("minor"),
                                       (String)version.get("build")
                                );
                }
            } catch (IOException e) {
                Server.logStackTrace(Level.SEVERE,"IOException",e);
            }
        }
        if (m_version != null)
            d.setValue(m_version,"String",Data.State.NORMAL);
        return d;
    }
    /**** Protected for inheritance *****/

    /**
     * This gets called when the path specifies a specific SIM should return the data.
     * Each SIM should override this and return the requested data
     * 
     * @param args An array of String arguments as specified by the SIM.
     * 
     * @return The value in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getSIMData(String ... args) { /*Data*/ return new Data(args[0],"{Data}"); }
  

/**** Private *****/
    
    private Map<String /*className*/,Map<String /*methodName*/,Map<Integer /*argCount*/,Method>>> m_dataMethods = 
            new HashMap<String,Map<String,Map<Integer,Method>>>();
//    private Map<String,Map<String,Map<Integer,Method>>> m_voidMethods = new HashMap<String,Map<String,Map<Integer,Method>>>(); /* <class name, <method name, <number of arguments, method>>> */

    /**
     * This method takes a path string, decodes it and calls the method on the specified class instance.
     * The first argument in the path should be the method name to call. If it starts with "get", the "get" will be optional.
     * The method must return a {@link com.SIMRacingApps.Data} instance and take all Strings as arguments.
     * The method will look for an overloaded version of the method that takes the same number of Strings that was passed in the path after the method to be called.
     * @param classInstance An instance of SIMPlugin, Session, Car, Gauge, Track, TeamSpeak
     * @param path A path in the form of a URL
     * @return The value in a {@link com.SIMRacingApps.Data} container.
     *         value.getBoolean("SET") will return true if the operation was a "set", "increment" or "decrement".
     * @throws SIMPluginException
     */
    private Data callMethod(Object classInstance, String path) throws SIMPluginException {
        String s[] = path.split("[/]"); 
        Data o = null;
        String name = null;
        ArrayList<String> args = new ArrayList<String>();
        String methodCalled = "";

        //copy non blank requests to the args array
        for (int index=0; index < s.length; index++) {
            if (name == null) {
                if (!s[index].trim().isEmpty()) {
                    name = s[index].trim().toUpperCase();
                }
            }
            else {
                if (!s[index].trim().isEmpty())
                    args.add(s[index].trim());
            }
        }

        String className    = classInstance.getClass().getName();
        
        // on first call for each class, copy the methods the user can call into maps for faster lookup
        if (!m_dataMethods.containsKey(className)) {
            
            Map<String,Map<Integer,Method>> methods = new HashMap<String,Map<Integer,Method>>();
            
            Method[] allMethods = classInstance.getClass().getMethods();
            
            for (Method method : allMethods) {
                String methodName = method.getName().toUpperCase();

                if (!method.getName().equals("getData")) { //it's not this method, getData(), or it would be recursive

                    //only register the methods with all string arguments
                    boolean isAllStrings = true;
                    java.lang.reflect.Type[] allParameters = method.getGenericParameterTypes();
                    for (java.lang.reflect.Type parameter : allParameters) {
                        //apparently getTypeName() wasn't introduced until Java 8
                        //if (!parameter.getTypeName().equals("java.lang.String"))
                        //this works on Java 7 and 8
                        if (!parameter.toString().equals("class java.lang.String"))
                            isAllStrings = false;
                    }

                    //if the method returns a Data object and all of its' arguments are strings
                    //save it in our map
                    if (isAllStrings
                    && method.getReturnType().getName().equals("com.SIMRacingApps.Data")
                    ) {
                        //strip the get off
                        if (methodName.startsWith("GET"))
                            methodName = methodName.substring(3);

                        //see if the method overload container exits and create one if it doesn't
                        if (!methods.containsKey(methodName))
                            methods.put(methodName, new HashMap<Integer,Method>());
                        
                        //now store the overloaded method, indexed by the number of string arguments.
                        methods.get(methodName).put(new Integer(allParameters.length), method);
                        if (Server.isLogLevelFinest())
                            Server.logger().finest(String.format("Adding method(%-30s): Data %s.%s(%d)", methodName,className,method.getName(),allParameters.length));
                    }
                    
                } //if not getData()
                
            } //for each method in the class
            
            m_dataMethods.put(className, methods);

        } //if class has not been read

        try {
            //if there is no name, then iterate through of all of the classes "get" methods and return them as a json string
            if (name == null) {
                int count = 0;
                StringBuffer json = new StringBuffer();
                
                json.append("{");
                Iterator<Entry<String, Map<Integer, Method>>>itr = m_dataMethods.get(className).entrySet().iterator();
                while (itr.hasNext()) {
                    Entry<String, Map<Integer, Method>> methods = itr.next();
                    
                    Method method = methods.getValue().get(0); //get the default method
                    if (method != null) {
                        if (method.getName().startsWith("get")) {
                            methodCalled = method.getName();
                            o = (Data) method.invoke(classInstance,args.toArray());
                            if (o != null) {
                                if (count++ > 0)
                                    json.append(",");
                                json.append("\"");
                                json.append(method.getName().substring(3));
                                json.append("\": ");
                                json.append(o.toString(o.getName()));   //only do the default name
                            }
                        }
                    }
                }
                json.append("}");
                o = new Data("json",json.toString(),"JSON",Data.State.NORMAL);
            }
            else {
                if (classInstance instanceof com.SIMRacingApps.SIMPlugin) {
                    if (name.equalsIgnoreCase("SIM") || name.equalsIgnoreCase(getSIMName().getString())) {
                        String [] a = new String[args.size()];
                        for (int i=0; i < args.size(); i++)
                            a[i] = args.get(i);
                        
                        o = this.getSIMData(a);
                    }
                    else
                    if (name.equalsIgnoreCase("SESSION")) {
                        
                        //Allow the user to use /Session/Car and /Car as well as /Session/Track and /Track interchangeably.
                        if (args.size() > 0 
                        && (    args.get(0).equalsIgnoreCase("CAR")
                           ||   args.get(0).equalsIgnoreCase("TRACK")
                           )
                        )
                            o = callMethod(this,String.join("/",args));
                        else
                            o = callMethod(getSession(),String.join("/",args));
                    }
                    else
                    if (name.equalsIgnoreCase("CAR")) {
                        //this first argument for a CAR needs to be the car identifier. If missing default to REFERENCE
                        String car = "REFERENCE";
                        if (args.size() > 0) {
                            car = args.get(0);
                            args.remove(0);
                        }
                        
                        Car c = getSession().getCar(car);
                        
                        //The requested car may not exist, so check it
                        if (c == null)
                            o = new Data(path,"ERROR: ("+path+") INVALID Car","String",State.ERROR);
                        else
                            o = callMethod(c,String.join("/",args));
                    }
                    else
                    if (name.equalsIgnoreCase("TRACK")) {
                        o = callMethod(getSession().getTrack(),String.join("/",args));
                    }
                    else
                    if (name.equalsIgnoreCase("TEAMSPEAK")) {
                        if (m_teamspeak != null)
                            o = callMethod(m_teamspeak,String.join("/",args));
                        else
                            o = new Data(path,"ERROR: ("+path+") TeamSpeak not loaded","String",State.ERROR);
                    }
                    else
                    if (name.equalsIgnoreCase("SIMPLUGINCALLBACK")) {
                        //search through the list of SIMPluginCallback(s) and see if it matches
                        //there is no way to know how deep the callback class path could be, so a starts with match is performed.
                        //Since this is a linear search, maybe it won't need to be optimized until there is enough callbacks to warrent it.
                        String callbackPath = String.join(".",args).toUpperCase();
                        Iterator<Entry<String, SIMPluginCallback>>itr = m_loadedCallbacks.entrySet().iterator();
                        while (itr.hasNext()) {
                            Entry<String, SIMPluginCallback> entry = itr.next();
                            //callbackPath = MSPEC.SHIFTLIGHT.STATE
                            //getKey() = MSPEC.ShiftLight
                            if (callbackPath.startsWith(entry.getKey().toUpperCase() + ".")) {
                                //remove the class name from the args, then call the method with the remaining args
                                for (int i=entry.getKey().split("[.]").length; i > 0; i--)
                                    args.remove(0); 
                                o = callMethod(entry.getValue(),String.join("/",args));
                                //o = entry.getValue().ProcessRequest(args);
                            }
                        }
                        
                        if (o == null)
                            o = new Data(path,"ERROR: ("+path+") Callback not loaded","String",State.ERROR);
                    }
                }
                else
                if (classInstance instanceof com.SIMRacingApps.Car) {
                    if (name.equalsIgnoreCase("GAUGE")) {
                        //this first argument for a Gauge needs to be the gauge type. Default to generic if nothing there. 
                        if (args.size() > 0) {
                            String gaugeType = args.get(0);
                            args.remove(0);
                            Gauge gauge = ((Car)classInstance)._getGauge(gaugeType);
                        
                            o = callMethod(gauge,String.join("/",args));
                        }
                        else {
                            o = ((Car)classInstance).getGauge();
                        }
                    }
                }
                
                if (o == null) {
                    if (m_dataMethods.containsKey(className)) {
                        if (m_dataMethods.get(className).containsKey(name)) {
                            Integer count = args.size();
                            if (m_dataMethods.get(className).get(name).containsKey(count)) {
                                Method method = m_dataMethods.get(className).get(name).get(count);
                                methodCalled = method.getName();
                                o = (Data) method.invoke(classInstance,args.toArray());
                                //now if it was a set, increment, or decrement operation, 
                                //let the caller know by setting the SET variable to true
                                if (name.startsWith("SET") || name.startsWith("INCREMENT") || name.startsWith("DECREMENT"))
                                    o.add("SET",true,"boolean");
                                else
                                    o.add("SET",false,"boolean");
                            }
                        }
                    }
                    else
                    if (name.equalsIgnoreCase("SIM") || name.equalsIgnoreCase(this.getSIMName().getString())) {
                        String [] argsArray = new String[args.size()];
    
                        for (int arg=0; arg < args.size(); arg++)
                            argsArray[arg] = args.get(arg);
                        o = getSIMData(argsArray);
                    }
                    else {
                        o = new Data(path,"ERROR: ("+path+") INVALID","String",State.ERROR);
                    }
                }
            }
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
            String error = String.format("SIMPlugin.getData(%s,%s).%s,  NumberFormatException: %s\n",className,path,methodCalled,e.toString());
            Server.logStackTrace(Level.WARNING,error,e);
            Server.logger().info(this.getSIMData().toString());
            SIMPluginException ce = new SIMPluginException(error);
            Server.logger().throwing("SIMPlugin", "callMethod: "+e.getMessage(), ce);
            throw ce;
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
            String error = String.format("SIMPlugin.getData(%s,%s).%s,  IllegalAccessException: %s\n",className,path,methodCalled,e.toString());
            Server.logStackTrace(Level.WARNING,error,e);
            Server.logger().info(this.getSIMData().toString());
            SIMPluginException ce = new SIMPluginException(error);
            Server.logger().throwing("SIMPlugin", "callMethod: "+e.getMessage(), ce);
            throw ce;
        }
        catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (t != null)
                t.printStackTrace();
            String error = String.format("SIMPlugin.getData(%s,%s).%s,  InvocationTargetException: %s\n",className,path,methodCalled,t == null ? "" : t.toString());
            Server.logStackTrace(Level.WARNING,error,t);
            Server.logger().info(this.getSIMData().toString());
            SIMPluginException ce = new SIMPluginException(error);
            Server.logger().throwing("SIMPlugin", "callMethod: "+t.getMessage(), ce);
            throw ce;
        }
        catch (Exception e) {
            e.printStackTrace();
            String error = String.format("SIMPlugin.getData(%s,%s).%s,  Exception: %s\n",className,path,methodCalled,e.toString());
            Server.logStackTrace(Level.WARNING,error,e);
            Server.logger().info(this.getSIMData().toString());
            SIMPluginException ce = new SIMPluginException(error);
            Server.logger().throwing("SIMPlugin", "callMethod: "+e.getMessage(), ce);
            throw ce;
        }

        if (o == null) {
            String error = String.format("ERROR: (%s) not valid",path);
            SIMPluginException ce = new SIMPluginException(error);
            Server.logStackTrace(Level.FINE, "SIMPlugin callMethod: "+error, ce);
            throw ce;
        }

        o.setName(path);  //put the requested name back in the object
        return o;

    }
}
