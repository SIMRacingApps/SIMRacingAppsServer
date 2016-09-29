package com.SIMRacingApps.SIMPluginCallbacks.Template;

import java.util.Map;
import com.SIMRacingApps.Data;
import com.SIMRacingApps.SIMPlugin;
import com.SIMRacingApps.SIMPlugin.SIMPluginException;
import com.SIMRacingApps.SIMPluginCallbacks.SIMPluginCallback;

/**
 * TODO: Describe your plug-in here.
 *
 * @author     TODO: Add your name here
 * @since 1.0  TODO: Update the first Server version your plugin was released for
 * @copyright  TODO: Put your copyright here
 * @license Apache License 2.0
 */
public class MyCallback extends SIMPluginCallback {

	//TODO: Add your own member variables here

    /**
	 * Constructor. 
	 * Use the constructor to initialize your plug-in.
	 *
	 * @param SIMPlugin An instance of the current SIM.
	 * @throws SIMPluginException If there's a problem constructing your plug-in.
	 */	
    public MyCallback(SIMPlugin SIMPlugin) throws SIMPluginException {
		super(SIMPlugin,"Template.MyCallback");
		
		//TODO: Add your initialization code here
		//Call Subscribe one for each path you want to get data for.

		Subscribe("Car/REFERENCE/Status"); //This is just an example. 
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
            
            //TODO: Put any code you want to do while the SIM is not running here
            
            
            return true;
        }
                    
        //TODO: read the data and do something with it. You are in your own thread now.
        
        Data status = data.get("Car/REFERENCE/Status"); //This is just and example
        
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