package com.SIMRacingApps.servlets;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.Set;

import com.SIMRacingApps.SIMPlugin;
import com.SIMRacingApps.SIMPlugin.SIMPluginException;
import com.SIMRacingApps.Data;
import com.SIMRacingApps.Data.State;
import com.SIMRacingApps.Server;

/**
 * This class implements the Data Caching Service for the servlets. 
 * It starts up a background thread that waits for events from the SIM, where it updates the cache with any new values.
 * <p>
 * It is implemented as a singleton, where the all you have to do is call the static method {@link DataService#getJSON(String)} with the sessionid.
 * This method returns a JSON formatted string with all the data that has changed since the last call.
 * <p>
 * Other configuration options are available and must be called before your first call to getJSON(). 
 * @author Jeffrey Gilliam
 * @copyright Copyright (C) 2015 - 2020 Jeffrey Gilliam
 * @since 1.0
 * @license Apache License 2.0
 */
public class DataService {

//    private static final long m_lostPacketThreshold = 10L;
//    private static final long OLDCACHETIME = 10000L;
    private static volatile boolean m_stayalive = true;
    private static volatile Object dt_lock = new Object();
    private static volatile Thread dt = null;
    private static volatile SIMPlugin m_SIMPlugin = null;
    private static final Map<String/*sessionid*/,Map<String/*dataid*/,Map<String/*datapath*/,com.SIMRacingApps.Data>>> m_sessionCache = new HashMap<String,Map<String,Map<String,com.SIMRacingApps.Data>>>();
    private static volatile String m_playfile = "";
    private static volatile String m_recordfile = "";
    private static volatile String m_startingVersion = "";
    private static volatile String m_endingVersion = "";
    private static volatile String m_SIMName = "iRacing";

    public static void stop() {
        m_recordfile = "";
        if (dt != null) {
            m_stayalive = false;

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Sets the name of the SIM to connect to. By default, it is set to iRacing.
     * If a different SIM is running, it will stop it.
     * @param SIMName The name of the SIM. Defaults to iRacing.
     */
    public static void setSIMname(String SIMName) {
        if (!m_SIMName.equals(SIMName)) {
            stop();
        }
        m_SIMName = SIMName;
        start();
    }
    
    /**
     * If playing back a recorded file, this sets the position to start playing it at.
     * @param startingVersion The SIM specific version to start at.
     */
    public static void setStartingVersion(String startingVersion) {
        m_startingVersion = startingVersion;
        if (m_SIMPlugin != null)
            synchronized (m_SIMPlugin) {
                m_SIMPlugin.setStartingVersion(m_startingVersion);
            }
    }

    /**
     * If playing back a recorded file, this sets the position to stop playing it at.
     * @param endingVersion The SIM specific version to end at.
     */
    public static void setEndingVersion(String endingVersion) {
        m_endingVersion = endingVersion;
        if (m_SIMPlugin != null)
            synchronized (m_SIMPlugin) {
                m_SIMPlugin.setEndingVersion(m_endingVersion);
            }
    }
    
    /**
     * Sets the file to use to play back a recorded session.
     * @param playfile The filename to use.
     */
    public static void setPlay(String playfile) {
        if (!m_playfile.equals(playfile)) {
            m_playfile = playfile;
            if (m_SIMPlugin != null) {
                synchronized (m_SIMPlugin) {
                    m_SIMPlugin.setPlay(playfile);
                }
                
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
            }
        }
    }
    
    public static Data getRecord() {
        if (m_SIMPlugin != null)
            synchronized (m_SIMPlugin) {
                return m_SIMPlugin.getRecord();
            }
        return new Data("record","");
    }
    
    /**
     * Sets the file to record the session to. 
     * @param recordfile The file to record to. Can be null or blank to stop recording.
     */
    public static void setRecord(String recordfile) {
        if (!m_recordfile.equals(recordfile)) {
            m_recordfile = recordfile;
            if (m_SIMPlugin != null)
                synchronized (m_SIMPlugin) {
                    String recordingExtension = m_SIMName;
                    
                    if (m_recordfile != null && !m_recordfile.isEmpty()) {
                        //start a new file each time this thread starts
                        Calendar c = new GregorianCalendar();
                        String date = String.format("%1$tY-%1$tm-%1$td_%1$tH-%1$tM-%1$tS", c);
                        m_SIMPlugin.setRecord(m_recordfile+"_"+date+"."+recordingExtension);
                    }
                    else {
                        m_SIMPlugin.setRecord("");
                    }
                }
        }
    }

    /**
     * Used by the servlets to delete a session from the cache.
     * @param sessionid The session to delete.
     */
    public static void deleteSession(String sessionid) {
        //if this session already called GET, then remove it's cache so we can reload.
        synchronized (m_sessionCache) {    //lock it, so thread will not be changing this while we query it.
            if (!sessionid.isEmpty() && m_sessionCache.containsKey(sessionid)) {
                Server.logger().info(String.format("Received delete request for existing session %s, Deleting Cache.", sessionid));
                synchronized (m_sessionCache.get(sessionid)) {
                    m_sessionCache.remove(sessionid);
                }
            }
        }
    }
    
    /**
     * Used by the servlets to return a data from the SIM directly, bypassing the cache.
     * @param datapath The path to the data.
     * @return The result as a {@link com.SIMRacingApps.Data} object.
     * @throws SIMPluginException If there's an error during the call.
     */
    public static com.SIMRacingApps.Data getData(String datapath) throws SIMPluginException {
        start();
        
        //put this in a sync block until I can get the SIMPlugins thread safe
        if (m_SIMPlugin != null) {
            synchronized (m_SIMPlugin) {
                return m_SIMPlugin.getData(datapath);
            }
        }
        return new Data(datapath,"NO Connection to SIM","",com.SIMRacingApps.Data.State.ERROR);
    }

    /**
     * This method is called to subscribe to the data and return the results as a JSON string. 
     * If data is null, then only the results are returned.
     * @param sessionid The session identifier.
     * @param data The data to subscribe to.
     * @return A JSON String with the results.
     */
    public static StringBuffer getJSON(String sessionid, Map<String,Map<String,Map<String,Object>>> data) {
        start();
        
        Map<String,Map<String,com.SIMRacingApps.Data>> session = null;
        
        if (data != null) {
            Set<Entry<String,Map<String,Map<String,Object>>>> dataids = data.entrySet();
    
            if (!dataids.isEmpty()) {
//                boolean needtowait = false;
                
                synchronized (m_sessionCache) {
//                    if (!m_sessionCache.containsKey(sessionid)) {
                        //add a new session
                        if (!sessionid.isEmpty())
                            Server.logger().info(String.format("Adding new SessionId=%s", sessionid));
                        
                        session = new HashMap<String,Map<String,com.SIMRacingApps.Data>>();
//                        Map<String,com.SIMRacingApps.Data> vars = new HashMap<String,com.SIMRacingApps.Data>();
//                        vars.put("timestamp", new com.SIMRacingApps.Data("timestamp",System.currentTimeMillis()));
//                        session.put("__vars__", vars);
                        m_sessionCache.put(sessionid, session);
            
                        //see if any sessions should be deleted
//                        Iterator<Entry<String, Map<String, Map<String, Data>>>> iter = m_sessionCache.entrySet().iterator();
//                        while (iter.hasNext()) {
//                            Entry<String, Map<String, Map<String, Data>>> entry = iter.next();
//                            
//                            //do not remove it if we are not updating it with the global thread.
//                            //This allows session threads to use it and delete it when they are done with it.
//                            if (entry.getValue().get("__vars__").get("nocacheupdate") == null
//                            &&  entry.getValue().get("__vars__").get("timestamp").getLong() < (System.currentTimeMillis() - OLDCACHETIME)
//                            ) {
//                                Server.logger().info(String.format("Removing Old SessionId=%s, last updated on %s", entry.getKey(), entry.getValue().toString()));
//                                m_sessionCache.remove(entry.getKey());
//                                iter.remove();
//                            }
//                        }
//                    }
//                    else {
//                        session = m_sessionCache.get(sessionid);
//                    }
                }
            
                if (session != null) {
                    //now lock on this particular session
                    synchronized (session) {
                        for (Entry<String,Map<String,Map<String,Object>>> dataid : dataids) {
                            String id    = dataid.getKey();
            
                            //See if it is in data map
                            if ((session.get(id)) == null) {
                                Map<String,com.SIMRacingApps.Data> datamap = new HashMap<String,com.SIMRacingApps.Data>();
            
                                for (Entry<String,Map<String,Object>> datakey : dataid.getValue().entrySet()) {
                                    String name    = datakey.getKey();
            
                                    //create a new one
                                    String dataname = (String)data.get(id).get(name).get("Name");
                                    com.SIMRacingApps.Data d = new com.SIMRacingApps.Data(dataname);
            
                                    Map<String,Object> value = datakey.getValue();
            
                                    d.setId(name);          //this Id is not the web id, it's the name
                                    d.setRequestedUOM((String)value.get("UOM"));
                                    d.setLang((String)value.get("Lang"));
                                    if (value.get("Format") != null)
                                        d.setFormat((String)value.get("Format"));
                                    Object o = value.get("Interval");
            
                                    if (o instanceof Integer) {
                                        d.setInterval(new Long((Integer)value.get("Interval")));
                                    }
                                    else
                                    if (o instanceof Long) {
                                        d.setInterval((Long)value.get("Interval"));
                                    }
                                    else
                                    if (o instanceof String) {
                                        try {
                                            d.setInterval(Long.parseLong((String)value.get("Interval")));
                                        }
                                        catch (NumberFormatException e2) {}
                                    }
            //                            try {
            //                                d.setInterval((Long)value.get("Interval"));
            //                            }
            //                            catch (ClassCastException e) {
            //                                try {
            //                                    d.setInterval(Long.parseLong((String)value.get("Interval")));
            //                                }
            //                                catch (NumberFormatException e2) {
            //                                }
            //                            }
            
                                    datamap.put(name, d);
//                                    needtowait = true;
                                }
                                session.put(id,datamap);  //this is the web Id
                            }
                        }
                    }
                }
                
//                if (needtowait) {
//                    try {
//                        //give the thread a little time to update the new attributes.
//                        //This sleep will only happen the first time they are requested.
//                        Thread.sleep(16*5);
//                    } catch (InterruptedException e) {
//                    }
//                }
            }
        }
        
        synchronized (m_sessionCache) {
             session = m_sessionCache.get(sessionid);
        }
        
        if (session != null) {
            //lock the session while we are updating it.
            synchronized (session) {
//                session.get("__vars__").get("timestamp").setValue(System.currentTimeMillis());    //update the session time

                return getJSON(session);
            }
        }

        return new StringBuffer("{}");
    }
    
    /**
     * Returns the results as a JSON String.
     * @param sessionid The session identifier
     * @return The results.
     */
    public static StringBuffer getJSON(String sessionid) {
        return getJSON(sessionid,null);
    }

    private static StringBuffer getJSON(Map<String,Map<String,com.SIMRacingApps.Data>> session) {
        if (m_SIMPlugin == null)
            return new StringBuffer("{}");
        
        updateSession(m_SIMPlugin,session);
        
        StringBuffer s = new StringBuffer("{");
        
        for (Entry<String, Map<String, com.SIMRacingApps.Data>> dataid : session.entrySet()) {
            String id    = dataid.getKey();

//            if (!id.equals("__vars__")) {
                //this code finds out if this id has changed
                Boolean changed = false;
                for (Entry<String, com.SIMRacingApps.Data> datakey : dataid.getValue().entrySet()) {
                    String name    = datakey.getKey();
                    if (session.get(id).get(name).isDirty()) {
                        changed = true;
                        break;
                    }
                }

                if (changed) {
                    if (s.length() > 1) s.append(",");

                    s.append("\"");
                    s.append( id );
                    s.append( "\": {" );

                    int count=0;

                    for (Entry<String, com.SIMRacingApps.Data> datakey : dataid.getValue().entrySet()) {
                        String name    = datakey.getKey();

                        //com.SIMRacingApps.Data obj = request.getValue();
                        String dataname= datakey.getValue().getName();

                        //make a copy of it
                        com.SIMRacingApps.Data d = new com.SIMRacingApps.Data(session.get(id).get(name));

                        //make the cache clean
                        session.get(id).get(name).setDirty(false);

//see if we need to convert the copy's UOM to the requested UOM.
//we don't convert in the cache so it can be faster. So the cache will always be in the default UOM of the SIM.
//                            String uom     = datakey.getValue().getUOM();
//                            if (uom != null && !uom.equals("") && !d.getUOM().equals(uom))
//                                d = d.convertUOM(uom);
//                            String lang    = datakey.getValue().getLang();
//                            if (lang != null && !lang.equals(""))
//                                d.setLang(lang);

                        String format  = (String)datakey.getValue().getFormat();
                        if (format != null && !format.equals(""))
                            d.setFormat(format);

                        if (count++ > 0)
                            s.append(",");

                        //If the name and the dataname are the same, need to force it out as toString() does not.
                        if (name.equals(dataname)) {
                            s.append( "\"" );
                            s.append( name );
                            s.append( "\":" );
                        }

//                            String x = d.toString();
                        s.append( d.toString(d.getName()) );  //force it to only stringify the default name

                    }

                    s.append("}\n");
                }
//            }
        }
        
        s.append("}");
        return s;
    }
    
//    public static Map<String,Map<String,com.SIMRacingApps.Data>> getSession(String sessionid) {
//        Map<String, Map<String, Data>> session;
//        synchronized (m_sessionCache) {
//            session = m_sessionCache.get(sessionid);
//            //add a nocacheupdate variable so it will not get cleaned up or cached
//            if (session.get("__vars__").get("nocacheupdate") == null)
//                session.get("__vars__").put("nocacheupdate", new Data("NoCacheUpdate",true));
//       }
//
//       updateSession(m_SIMPlugin,session);
//       
//       return session;
//    }

    public static com.SIMRacingApps.Data getSIMName() throws SIMPluginException {
        start();
        
        //put this in a sync block until I can get the SIMPlugins thread safe
        if (m_SIMPlugin != null) {
            synchronized (m_SIMPlugin) {
                return m_SIMPlugin.getSIMName();
            }
        }
        return new Data("SIMName","NO Connection to SIM","",com.SIMRacingApps.Data.State.ERROR);
    }
    
    public static com.SIMRacingApps.Data getSIMVersion() throws SIMPluginException {
        start();
        
        //put this in a sync block until I can get the SIMPlugins thread safe
        if (m_SIMPlugin != null) {
            synchronized (m_SIMPlugin) {
                return m_SIMPlugin.getVersion();
            }
        }
        return new Data("SIMVersion","NO Connection to SIM","",com.SIMRacingApps.Data.State.ERROR);
    }
    
    private static int updateSession(SIMPlugin SIMPlugin, Map<String, Map<String, Data>> session) {
        int count = 0;
        //now lock each session because all the values for a given session must be in sync with the SIMs data
        synchronized (session) {
            
            //find the highest interval that's ready to update
            long highest_interval = 0;
            for ( Entry<String,Map<String,com.SIMRacingApps.Data>> id : session.entrySet()) {
//                if (!id.getKey().equals("__vars__")) {
                    Iterator<Entry<String,com.SIMRacingApps.Data>> iter = id.getValue().entrySet().iterator();
                    while (iter.hasNext()) {
                        com.SIMRacingApps.Data data = iter.next().getValue();
                        //check if it's time to update
                        if (data.isDataStale(data.getName())) {
                            if (highest_interval < data.getInterval())
                                highest_interval = data.getInterval();
                        }
                    }
//                }
            }
            
            //if nothing needs updating return
            if (highest_interval == 0)
                return count;

            synchronized (SIMPlugin) {
                for ( Entry<String,Map<String,com.SIMRacingApps.Data>> id : session.entrySet()) {
//                    if (!id.getKey().equals("__vars__")) {
                        Iterator<Entry<String,com.SIMRacingApps.Data>> iter = id.getValue().entrySet().iterator();
    
                        while (iter.hasNext()) {
                            Entry<String,com.SIMRacingApps.Data> data_entry = iter.next();
                            com.SIMRacingApps.Data data = data_entry.getValue();
    
                            try {
    
                            //check if it's time to update
//replaced this check with a check against the highest interval so that all values are in sync
//without this, some values would update and others not, causing weird results in the clients.
//Note: It's up to the clients to understand how to set their intervals to keep the values in sync.
//                            if (data.isDataStale(data.getName())) {
                                if (data.getInterval() <= highest_interval) {
    
                                    com.SIMRacingApps.Data d;
                                    d = SIMPlugin.getData(data.getName());

                                    if (d != null) {
                                        //carry over values from the users request
                                        d.setId(data_entry.getKey());
                                        d.setLang(data.getLang());
                                        d.setInterval(data.getInterval());
                                        d.setFormat(data.getFormat());
                                        d = d.convertUOM(data.getRequestedUOM());
                                        d.setRequestedUOM(data.getRequestedUOM());
    
                                        //don't keep calling SET operations
                                        //remove it from the cache and return the value
                                        if (d.getBoolean("SET")) {
                                            //remove it from the cache
                                            iter.remove();
                                        }
                                        d.remove("SET"); //Don't return this to the client.
    
                                        //now compare it with our cached value and if different update the cache
                                        //if we just copied it straight over without checking, the dirty flag would not be set correctly.
    //                                        if (d.compare(id.getValue().get(data_entry.getKey())) != 0) {
                                        if (data.compare(d,data_entry.getKey()) != 0) {
                                            data.set(d);
                                            count++;
                                        }
                                    }
    
                                    //update the timestamp even if we didn't update the data
                                    //this way, less frequent changing data will not poll the
                                    //server faster than it's interval.
                                    data_entry.getValue().updateTimeStamp(data_entry.getValue().getName());
                                }
                            }
                            catch (SIMPluginException e) {
                                Server.logStackTrace(Level.FINE, "while calling SIMPlugin.getData(): "+e.getMessage(),e);
                                data.setValue(e.getMessage());
                                data.setState(State.ERROR);
                            }
                            catch (Exception e) {
                                Server.logStackTrace(Level.WARNING, "while calling SIMPlugin.getData(): "+e.getMessage(),e);
                                data.setValue(e.getMessage());
                                data.setState(State.ERROR);
                            }
                        }
//                    }
                }
            }
        }
        return count;
    }
    
    /**
     * starts the background thread to wait for events from the SIM.
     */
    public static void start() {
        synchronized (dt_lock) {
            if (dt == null) {
                m_stayalive = true;
                dt = new Thread( new Runnable() {
//                    private Integer m_ips = 0;
//                    private long lastDataVersion = -1L;
                     //override the global logger and get one for this thread
                    
                    public void run() {
                        Server.logger().info("start() is running...");
    
                        String recordingExtension = m_SIMName;
                        try {
                            m_SIMPlugin = SIMPlugin.createSIMPlugin(recordingExtension);
                        } catch (Exception e1) {
                            Server.logStackTrace(Level.SEVERE, "while creating SIMPlugin: "+e1.getMessage(),e1);
                            return;
                        }
    
                        synchronized (m_SIMPlugin) {
                            if (m_startingVersion != null) {
                                m_SIMPlugin.setStartingVersion(m_startingVersion);
                            }
                            if (m_endingVersion != null) {
                                m_SIMPlugin.setEndingVersion(m_endingVersion);
                            }
                            if (m_playfile != null && !m_playfile.equals(""))
                                m_SIMPlugin.setPlay(m_playfile);
        
                            if (m_recordfile != null && !m_recordfile.equals("")) {
                                //start a new file each time this thread starts
                                Calendar c = new GregorianCalendar();
                                String date = String.format("%1$tY-%1$tm-%1$td_%1$tH-%1$tM-%1$tS", c);
                                m_SIMPlugin.setRecord(m_recordfile+"_"+date+"."+recordingExtension);
                            }
                            else {
                                m_SIMPlugin.setRecord("");
                            }

                            //This is a list of built in callbacks to automatically load.
                            //To unload a callback, start it with an exclamation point.
                            //Other callbacks specified by the user is appended.       
                            //keep this list in sync with the list in the default.setttings.txt file.                     
                            String[] callbacksToLoad = ("MSPEC.ShiftLight;Sounds.PitCountDown;Sounds.PitSpeedLimit;Sounds.Shift;DataPublisher.Post;LIFX.Flags;"+Server.getArg("simplugins","")).split(";");
                            
                            for (int i=0; i < callbacksToLoad.length; i++) {
                                String callback = callbacksToLoad[i].trim();
                                boolean okToLoad = Server.getArg(
                                                    callback.replaceAll("[.]", "-"),
                                                    Server.getArg(
                                                        callback,
                                                           callback.equals("DataPublisher.Post")
                                                        || callback.equals("LIFX.Flags")
                                                        ? false 
                                                        : true
                                                    )
                                                  );
                                
                                for (int j=0; j < callbacksToLoad.length; j++) {
                                    String callback2 = callbacksToLoad[j].trim();
                                    if (callback2.startsWith("!") && callback2.substring(1).equals(callback))
                                        okToLoad = false;
                                }
                                
                                //if callback starts with an exclamation point, then that means do not load it
                                //this way the built in callbacks can be turned off.
                                if (!Server.getArg("safemode", false) && okToLoad && !callback.startsWith("!"))
                                    m_SIMPlugin.addCallback(callback);
                            }
                        }
    
                        //we will provide a callback that simply watches the m_stayalive variable
                        //otherwise, do nothing. As each client requests data, they will be in their own thread.
                        //They will synchronize on the plugin and get the data they need.
                        m_SIMPlugin.run(new SIMPlugin.Callback() {
                            public boolean DataReady(SIMPlugin SIMPlugin, Integer ips) {
                                if (!m_stayalive)
                                    return false;
                                return true;
                            }

                            public boolean Waiting(SIMPlugin SIMPlugin) {
                                if (!m_stayalive)
                                    return false;
                                return true;
                            }
                        });
                        
//                        m_SIMPlugin.run(new SIMPlugin.Callback() {
//                            private boolean _processdata(SIMPlugin SIMPlugin, Integer ips) {
//    
//                                //!!!!Time spent in this routine can impact the IPS. Try to spend less than 16 ms to not drop packets.
//    
//                                StringBuffer times = new StringBuffer();
//    
//                                if (!m_stayalive)
//                                    return false;
//                                
//                                //create a copy of the sessions that need updating so we can release the lock on m_sessionCache while each session is updating
//                                Map<String, Map<String, Map<String, Data>>> sessions = new HashMap<String,Map<String,Map<String,com.SIMRacingApps.Data>>>();
//                                
//                                synchronized (m_sessionCache) {
//                                    for (Entry<String,Map<String,Map<String,com.SIMRacingApps.Data>>> session : m_sessionCache.entrySet()) {
//                                        
//                                        //if flagged to not update or it's too old 
//                                        //then don't grab a copy of it.
//                                        if (session.getValue().get("__vars__").get("nocacheupdate") == null
//                                        &&  session.getValue().get("__vars__").get("timestamp").getLong() >= (System.currentTimeMillis() - OLDCACHETIME)
//                                        )
//                                            sessions.put(session.getKey(),session.getValue());
//                                    }
//                                }
//                                
//                                String dataVersion = "";
//                                
//                                //TODO: get a unique list of data names so not to call getData() more than once for the same name
//                                //      but update all the occurrences in the map
//                                long allsessions_starttime = System.currentTimeMillis();
//                                long allsessions_count     = 0L;
//    
//                                for (Entry<String, Map<String, Map<String, Data>>> session_entry : sessions.entrySet()) {
//                                    
//                                    Map<String, Map<String, Data>> session = session_entry.getValue();
//                                    
//                                    long session_starttime = System.currentTimeMillis();
//                                    long session_count = 0L;
//                                    
//                                    dataVersion = SIMPlugin.getSession().getDataVersion().getString();
//                                    String v[] = dataVersion.split("[-]");
//                                    long currentDataVersion = v.length > 1 ? Long.parseLong(v[1]) : lastDataVersion + 1L; 
//                                    
//                                    if (lastDataVersion > -1L && currentDataVersion > (lastDataVersion+1L+m_lostPacketThreshold)) {
//                                        Server.logger().fine(String.format("PacketLoss above threshold(%d) at %s: last: %d, lost: %d",m_lostPacketThreshold,dataVersion,lastDataVersion,currentDataVersion - lastDataVersion - 1L));
//                                    }
//                                    
//                                    lastDataVersion = currentDataVersion;
//                                    
//                                    session_count += updateSession(SIMPlugin,session);
//                                    
//                                    long session_endtime = System.currentTimeMillis();
//                                    if (session_endtime - session_starttime > 32) {
//                                        times.append(String.format("\nsession %s took %d milliseconds, count = %d\n",session_entry.getKey(),session_endtime - session_starttime,session_count));
//                                    }
//                                    allsessions_count += session_count;
//                                }
//                                
//                                long allsessions_endtime = System.currentTimeMillis();
//                                if (allsessions_endtime - allsessions_starttime > (m_sessionCache.size() * 16 + 16)) {
//                                //if (allsessions_endtime - allsessions_starttime > 60) {
//                                    times.append(String.format("\n%d sessions took %d milliseconds, count = %d\n",m_sessionCache.size(), allsessions_endtime - allsessions_starttime, allsessions_count));
//                                }
//                                
////                                if (times.length() > 0)
////                                    Server.logger().finest(String.format("DataVersion=%s - %s"
////                                        , dataVersion
////                                        , times.toString()
////                                    ));
//    
//                                if (m_ips != ips && ips < 8) {
//                                    Server.logger().info(String.format("IPS(%d) dropped below threshold(8), SessionVersion = %s, count = %d%s", ips, dataVersion, allsessions_count, times.toString()));
//                                    m_ips = ips;
//                                }
//                                else
//                                if (m_ips != ips && ips < 15) {
//                                    Server.logger().fine(String.format("IPS(%d) dropped below threshold(15), SessionVersion = %s, count = %d%s", ips, dataVersion, allsessions_count, times.toString()));
//                                    m_ips = ips;
//                                }
//                                else
//                                if (m_ips != ips && ips < 30) {
//                                    Server.logger().finer(String.format("IPS(%d) dropped below threshold(30), SessionVersion = %s, count = %d%s", ips, dataVersion, allsessions_count, times.toString()));
//                                    m_ips = ips;
//                                }
//                                else
//                                if (m_ips != ips && ips < 50) {
//                                    Server.logger().finest(String.format("IPS(%d) dropped below threshold(50), SessionVersion = %s, count = %d%s", ips, dataVersion, allsessions_count, times.toString()));
//                                    m_ips = ips;
//                                }
//    
//                                return true;
//                            }
//                            
//                            public boolean DataReady(SIMPlugin SIMPlugin, Integer ips) {
//                                return _processdata(SIMPlugin,ips);
//                            }
//    
//                            public boolean Waiting(SIMPlugin SIMPlugin) {
//                                if (!m_stayalive)
//                                    return false;
//    //I decided not to sleep here and let it cycle as fast as it could
//    //just in case a SIM wants to provide data even when it's waiting for it to run.
//    //                                System.err.printf("%d - Waiting for %s to connect...\r",counter++, SIMPlugin.getClass().getName());
//    //                                try {
//    //                                    Thread.sleep(1000);
//    //                                } catch (InterruptedException e) {
//    //                                    e.printStackTrace();
//    //                                    System.err.printf("%d - Waiting sleep interupted for %s...\r",counter++, SIMPlugin.getClass().getName());
//    //                                }
//    //so calling _processdata() here will allow default values or otherwise to be sent to the client real-time while waiting.                                
//                                return _processdata(SIMPlugin,1);
//                                //return true;
//                            }
//                        });
    
                        Server.logger().info("start() is exiting...");
                        if (m_SIMPlugin != null) {
                            synchronized (m_SIMPlugin) {
                                m_SIMPlugin.close();
                                m_SIMPlugin = null;
                            }
                        }
                        synchronized (dt_lock) {
                            dt = null;
                        }
                    }
                });
    
                dt.setPriority(Thread.MAX_PRIORITY);
                
                dt.setName("Servlet.DataService."+m_SIMName);
                dt.start();
    
                //give the tread time to start processing data
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
