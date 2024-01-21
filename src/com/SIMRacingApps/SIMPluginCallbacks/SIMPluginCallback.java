package com.SIMRacingApps.SIMPluginCallbacks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.SIMRacingApps.Data;
import com.SIMRacingApps.SIMPlugin;
import com.SIMRacingApps.SIMPlugin.Callback;
import com.SIMRacingApps.SIMPlugin.SIMPluginException;
import com.SIMRacingApps.Server;

/**
 * This class is the abstract base class for all SIMPluginCallback classes.
 * By implementing this class, you can hook into the event loop and get data from the SIM.
 *
 * Here are the specifics of how to develop your plug-in for the SIMRacingApps Server.
 * 
 * Create a new class com.SIMRacingApps.SIMCallbacks.{YourNameOrCompany}.{YourPluginClass} 
 * in the com.SIMRacingApps.SIMCallbacks.{YourNameOrCompany} package that extends SIMPluginCallback.
 * There is a Template plug-in folder in the SIMPluginCallbacks folder that you can copy and rename.
 *
 * NOTE: Each subclass can define methods that return Data objects and if a  
 * Data Url maps to a method on the class, it will call it.
 *
 * Example: /SIMRacingApps/SIMPluginCallback/Sounds/PitCountDown/setVolume/50
 * will call the setVolume method and pass it 50.
 *
 * If you know Java, you will probably know how to compile your source code. 
 * But just in case you don't, here's an example how. 
 * Of course, your specific server version may be different.
 * <pre>
 *    {pathToJDK}\bin\javac -cp "%USERPROFILE%"\Documents\SIMRacingApps\SIMRacingAppsServer_1.0_Build_BETA-2016.01.09.exe" *.java
 * </pre>
 * For testing, put the .class file(s) generated in the "Documents\SIMRacingApps\com\SIMRacingApps\SIMPluginCallbacks\{YourNameOrCompany}" folder.
 * Add the plug-in classes to the settings.txt file's "simplugins" entry. Each plug-in should be separated by a semicolon.
 * The SIMRacingApps Server will load your plug-in upon startup of the server.
 * <pre>
 * settings.txt
 *    simplugins = YourNameOrCompany.YourPluginClass;Name1.DoSomething;Name2.DoneIt
 * </pre>
 *
 * When testing, if the plug-in is already built into the SIMRacingAppsServer, 
 * you will have to alter the classpath to look for your new .class files to load before the built in ones.
 * To do that, do not run the SIMRacingAppsServer.exe directly. 
 * Instead, use the following example command, on how to use it as a library and put your stuff first in the path.
 *<pre>
 *   {pathToJRE}\bin\java -cp "%USERPROFILE%"\Documents\SIMRacingApps\com\SIMRacingApps\SIMPluginCallbacks\{YourNameOrCompany}:%USERPROFILE%"\Documents\SIMRacingApps\SIMRacingAppsServer_1.0_Build_BETA-2016.01.09.exe" com.SIMRacingApps.Server
 *</pre> 
 *
 * For distribution, create a jar file with your .class file(s).
 * We are going to use a .sra extension instead of .jar for uploading to the SIMRacingApps Server through the menu. 
 * <pre>
 *  Example:
 *    jar cvf YourPlugin_v1.0.sra -C "%USERPROFILE%\Documents\SIMRacingApps" com/SIMRacingApps/SIMPluginCallbacks/{YourNameOrCompany}
 * </pre>
 * Distribute the .sra file with instructions to upload it to SIMRacingApps through the main menu.
 * Also provide instructions on how to enable your plug-in by adding it to the settings.txt-&gt;plugins variable.
 *
 * For distribution with a Java App that uses the SIMRacingAppsServer.exe as a jar file, just add the .sra file to the classpath.
 *
 * If you would like your plug-in distributed as part of SIMRacingApps, please send me a request to do so at support@simracingapps.com.
 *
 * @author Jeffrey Gilliam
 * @since 1.0
 * @copyright Copyright (C) 2015 - 2024 Jeffrey Gilliam
 * @license Apache License 2.0
 */
public class SIMPluginCallback implements Callback {

    private final ArrayList<String> m_dataPaths = new ArrayList<String>();
    private final BlockingQueue<Map<String, Data>> m_queue;
    private final Thread m_thread;

	@SuppressWarnings("unused")
    private SIMPluginCallback() {
        m_queue  = null;
        m_thread = null;
	}

    /**
	 * Constructor. Provides you with an instance of the SIM. 
	 * You will need to override this constructor with SIMPlugin as a parameter and call super(SIMPlugin).
	 *
	 * Use the constructor to initialize your plug-in.
	 *
	 * @param SIMPlugin An instance of the current SIM.
	 * @param name The name of the plug-in so you can find it's thread in a debugger and show it in the logger messages.
	 * @throws SIMPluginException If there's a problem constructing your plug-in.
	 */	
    public SIMPluginCallback(SIMPlugin SIMPlugin, String name) throws SIMPluginException {
        //Create the queue for the thread. Set the size small so the thread can't get too far behind real-time, 
        //yet allow for some concurrency with the puts and takes.
        
        m_queue = new ArrayBlockingQueue<Map<String, Data>>(2);
        m_thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    Map<String,Data> data;
                    while ((data = m_queue.take()) != null) { //wait for something to do
                        if (!ProcessData(SIMPlugin,data))
                            break;
                    }
                }
                catch (InterruptedException e) {
                    //Silently exit the thread
                }
            }
        });
        
        m_thread.setName("SIMPluginCallback."+name);
        m_thread.start();
    }
	
	/**
	 * Called when the SIMPlugin is destroyed.
	 * If you override this method, be sure to call super.destroy() first.
	 */
    public void destroy() {
        if (m_thread != null)
            m_thread.interrupt();
    }
	
    protected void Subscribe(String dataPath) {
        m_dataPaths.add(dataPath);
    }
    
	/**
	 * This method is called every time the SIM has new data available.
	 * By default this method push the data into the queue. You should not need to override this method.
	 * It will not block if the queue is full. I do this so your plug-in gets data that is very close to real-time.
	 * If it takes to look to process the queue, then you will simply get the next available data.
	 * 
	 * @param SIMPlugin An instance of the current SIM.
	 * @param ips Iterations per second.
	 * @return true to stay alive, false to stop the server.
	 */
	public boolean DataReady(SIMPlugin SIMPlugin, Integer ips) {
	    Map<String,Data> data = new HashMap<String,Data>();
	    for (int i=0; i < m_dataPaths.size(); i++) {
            try {
                data.put(m_dataPaths.get(i), SIMPlugin.getData(m_dataPaths.get(i)));
            } catch (SIMPluginException e) {
                Server.logStackTrace(e);
            }
	    }
	    m_queue.offer(data);
	    return true; 
	}
	
	/**
	 * Waiting() is called every second if the SIM is not providing any data.
	 *
	 * @param SIMPlugin An instance of the current SIM.
	 * @return true to stay alive, false to stop the server.
	 */
	public boolean Waiting(SIMPlugin SIMPlugin)   {
	    m_queue.offer(new HashMap<String,Data>());
	    return true; 
	}

    /**
     * ProcessData is called from within the dedicated thread for this plug-in
     * every time there is data available in the queue it pops it off the queue and passes it to this method.
     * 
     * A should override this method to implement your plug-in's functionality.
     * If this method takes too long to process the data in the queue, you could miss data that the SIM is sending.
     * Design accordingly. There's no way to even guarantee that you will get every update from the SIM.
     * 
     * At this point in the design, ProcessData will get called on the same frequency as the SIM (typically 16ms). 
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
     * @param SIMPlugin A reference to the SIMPlugin instance.
     * @param data The Data objects added to the queue.
     * @return true to keep the plug-in alive, false to kill it.
     */
	public boolean ProcessData(SIMPlugin SIMPlugin, Map<String,Data> data) { return true; }

}