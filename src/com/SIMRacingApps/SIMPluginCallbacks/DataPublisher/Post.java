package com.SIMRacingApps.SIMPluginCallbacks.DataPublisher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import com.owlike.genson.*;
import com.SIMRacingApps.Data;
import com.SIMRacingApps.SIMPlugin;
import com.SIMRacingApps.Server;
import com.SIMRacingApps.SIMPlugin.SIMPluginException;
import com.SIMRacingApps.SIMPluginCallbacks.SIMPluginCallback;

/**
 * TODO: Pull documentation from http://issues.simracingapps.com/71
 *
 * @author    Jeffrey Gilliam
 * @since     1.2
 * @copyright Copyright (C) 2015 - 2016 Jeffrey Gilliam
 * @license   Apache License 2.0
 */
public class Post extends SIMPluginCallback {

    private Genson m_genson; //synchronize on this for all member variables
	private Long m_lastUpdate = 0L;
	private Properties m_variables;
	private String m_version;
	private String m_session = "";
	private String m_date = "1970-01-01 00:00:00";
    private SimpleDateFormat m_dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private Map<String,Object> m_results = new HashMap<String,Object>();

    /**
	 * Constructor. 
	 * Use the constructor to initialize your plug-in.
	 *
	 * @param SIMPlugin An instance of the current SIM.
	 * @throws SIMPluginException If there's a problem constructing your plug-in.
	 */	
    public Post(SIMPlugin SIMPlugin) throws SIMPluginException {
		super(SIMPlugin,"DataPublisher.Post");
		
		m_genson = new Genson();
	    m_dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		
        InputStream in = null;
        try {
            Properties version = new Properties();
            in = this.getClass().getClassLoader().getResourceAsStream("com/SIMRacingApps/version.properties");
            version.load(in);
            in.close();
            m_version = String.format("%s.%s Build: %s",(String)version.get("major"),(String)version.get("minor"),(String)version.get("build"));
        } catch (IOException e) {
        }
        finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                }
        }
        
        in = null;
        try {
            m_variables = new Properties();
            in = this.getClass().getClassLoader().getResourceAsStream("com/SIMRacingApps/SIMPluginCallbacks/DataPublisher/DataToPublish.properties");
            m_variables.load(in);
            in.close();
        } catch (IOException e) {
        }
        finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                }
        }
        Iterator<Object> itr = m_variables.keySet().iterator();

        //These are required
        Subscribe("SIMName");
        Subscribe("Version");
        Subscribe("Session/Id");
        Subscribe("Session/LeagueId");
        
        //these the user can override with their own properties file.
        while (itr.hasNext()) {
            String key = itr.next().toString();
            String format = m_variables.getProperty(key);
            Subscribe(key);
            Server.logger().finer(String.format("Publishing: '%s' formatted as '%s'",key,format));
        }
	}
	
	/**
	 * Called when the SIMPlugin is destroyed.
	 * It's always best to call super.destroy() for future compatibility.
	 */
    @Override
    public void destroy() {
		super.destroy();
		
		//TODO: Add code to clean up your plug-in here
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
        
        //if data is empty, then the SIM is not running
        if (data.isEmpty()) {
            return true;
        }
                    
        //read the data and do something with it. You are in your own thread now.
        Data leagueId = data.get("Session/LeagueId");

        int interval = Server.getArg(String.format("datapublisher-post-interval-%s",leagueId.getString()), 5000);

        if (m_lastUpdate + interval <= System.currentTimeMillis()) {
    
            String publish_URL = Server.getArg(String.format("datapublisher-post-url-%s",leagueId.getString()),"");
            
            if (!publish_URL.isEmpty()) {
                
                try {
                    URL url = new URL(publish_URL);
                    HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                    urlc.setRequestProperty("Referer", "http://SIMRacingApps.com/");
                    urlc.setRequestMethod("POST");
                    urlc.setDoInput(true);
                    urlc.setDoOutput(true);
                    urlc.setUseCaches(false);
                    
                    Map<String,Object> output_map   = new HashMap<String,Object>();
                    //now load up the data
                    Iterator<Object> itr = m_variables.keySet().iterator();
                    
                    while (itr.hasNext()) {
                        String key = itr.next().toString();
                        String format = m_variables.getProperty(key);
                        Data d = data.get(key);
                        if (d != null) {
                            output_map.put(key, d.getStringFormatted(format));
                        }
                    }
                    
                    //add sim name, version and time stamp
                    output_map.put("SIMName",           data.get("SIMName").getString());
                    output_map.put("SIMVersion",        data.get("Version").getString());
                    output_map.put("Session/Id",        data.get("Session/Id").getString());
                    output_map.put("Session/LeagueId",  data.get("Session/LeagueId").getString());
                    output_map.put("Version",           m_version);
                    output_map.put("PostDateGMT",       m_dateFormat.format(System.currentTimeMillis()));
                    
                    if (!((String)output_map.get("Session/Id")).equals(m_session)) {
                        m_session = (String) output_map.get("Session/Id");
                        m_date    = (String) output_map.get("PostDateGMT");
                    }
                    
                    output_map.put("StartDateGMT", m_date);

                    //post it
                    OutputStream os = urlc.getOutputStream();
                    if (os != null) {
                        String packet = m_genson.serialize(output_map);
                        
                        os.write(packet.getBytes());
                        os.flush();
                        os.close();
                        
                        InputStream is = urlc.getInputStream();
                        
                        if (is != null) {
            
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
    
                            try {
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    out.write(buffer,0,bytesRead);
                                }
                            }
                            finally {
                                is.close();
                            }
                            
                            //Cache this away for the API routes to read it
                            String result = out.toString();
                            try {
                                @SuppressWarnings("unchecked")
                                Map<String,Object> results = m_genson.deserialize(result, Map.class);
                                if (results != null) {
                                    synchronized (m_genson) {
                                        m_results = results;
                                    }
                                }
                            }
                            catch (Exception e) {}
                            Server.logger().finer(String.format("DataPublish.Post Server Returned %s",result));
                        }
                    }
                }
                catch (IOException e) {
                    Server.logStackTrace(e);
                }
            }
            
            m_lastUpdate = System.currentTimeMillis();
        }
        
        return true;
    }
    /**
     * Gets the max value for a tire.
     * 
     * <p>PATH = {@link #getMaxTires() /SIMPluginCallback/DataPublisher/Post/MaxTires}
     * 
     * @return The max number of tires in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getMaxTires() {
        Data d = new Data ("DataPublisher/Post/MaxTires",99);
        synchronized (m_genson) {
            Long maxTires = (Long) m_results.get("MaxTires");
            if (maxTires != null) {
                try {
                    int max = maxTires.intValue();
                    d.setValue(max,"",Data.State.NORMAL);
                }
                catch (NumberFormatException e) {
                    
                }
            }
        }
        return d;
    }
}