package com.SIMRacingApps;

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.logging.Level;
import com.SIMRacingApps.SIMPlugin.Callback;
import com.SIMRacingApps.Data;
import com.SIMRacingApps.SIMPlugin.SIMPluginException;

/**
 * This class is used by a Java AWT application to send data events to registered listeners. 
 * It uses a singleton pattern to create and manage the connection to the SIM.
 * The main routine must call createDispatcher() before anything else is done to establish the singleton instance for the specific SIM.
 * Then others may access the dispatcher using the {@link com.SIMRacingApps.SIMPluginAWTEventDispatcher#getDispatcher()} 
 * method to register listeners for the events.
 * Then, the startThread() method must be called to start the thread that sends the events to the listeners.
 * <p>
 * For example:
 * <pre>
 *  //create a SIMPlugin to the SIM and pass it to the createDispatcher().
 *  SIMPluginAWTEventDispatcher.createDispatcher(
 *      SIMPlugin.createSIMPlugin(m_SIM)
 *  );
 *          
 *  //process SIMPlugin arguments.
 *  SIMPluginAWTEventDispatcher.getDispatcher().getSIMPlugin().setPlay(m_play);
 *          
 *  //initialize your application, register all the listeners
 *  m_geardata = new JDataLabel("Car/REFERENCE/Gauge/Gear/ValueCurrent");
 *  m_geardata.addPropertyChangeListener("data",
 *      new PropertyChangeListener() {
 *          public void propertyChange(PropertyChangeEvent evt) {
 *              Data d = (Data) evt.getNewValue();
 *              // normally you would do some fancy formatting that the
 *              // setFormat() method cannot handle.
 *              m_geardata.setText("[" + d.getString() + "]");
 *          }
 *      });
 *  widgetPanel.add(m_geardata);
 *          
 *  //start the dispatcher thread running to place data into the event queue
 *  SIMPluginAWTEventDispatcher.getDispatcher().startThread();
 * </pre>  
 */
public class SIMPluginAWTEventDispatcher {

    /**
     * Any exceptions are caught and wrapped in this exception.
     * Most exceptions are fatal and you should just print them and exit.
     */
    public class SIMPluginAWTEventsException extends Exception {
        private static final long serialVersionUID = -1168295862012759604L;

        SIMPluginAWTEventsException(String s) {
            super(s);
        }
    }

    /**
     * Constructor used by the factory method {@link com.SIMRacingApps.SIMPluginAWTEventDispatcher#createDispatcher(SIMPlugin)}.
     * @param SIMPlugin A SIMPlugin.
     */
    private SIMPluginAWTEventDispatcher( SIMPlugin SIMPlugin ) { m_SIMPlugin = SIMPlugin; }

    private static SIMPluginAWTEventDispatcher m_dispatcher = null;
    
    /**
     * A static factory method that creates the dispatcher instance and returns it.
     * This must be called first so the other methods have access to the singleton.
     * @param SIMPlugin The SIMPlugin.
     * @return A reference to the dispatcher singleton. 
     */
    public static SIMPluginAWTEventDispatcher createDispatcher( SIMPlugin SIMPlugin ) {
        if (m_dispatcher == null) 
            m_dispatcher = new SIMPluginAWTEventDispatcher(SIMPlugin);
        return m_dispatcher;
    }
    
    /**
     * Returns the dispatcher singleton. Can return null if createDispatcher() has not been called.
     * @return A reference to the dispatcher singleton. 
     */
    public static SIMPluginAWTEventDispatcher getDispatcher() {
        return m_dispatcher; 
    }
    
    private SIMPlugin m_SIMPlugin = null;

    /**
     * Returns the SIMPlugin. Can return null if the createDipatcher() method has not been called.
     * @return The SIMPlugin.
     */
    public SIMPlugin getSIMPlugin() {
        return m_SIMPlugin;
    }

    
    private PropertyChangeSupport m_Pcs = new PropertyChangeSupport(this);

    /**
     * Since I cannot listen unless you give me the name, this method throws an exception.
     * I will leave these here for Bean editors that don't recognize the named ones.
     * @param listener The listener to receive the events.
     * @throws SIMPluginAWTEventsException A fatal exception.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) throws SIMPluginAWTEventsException {
        throw new SIMPluginAWTEventsException("Cannot use generic listeners with this object. Use the named versions addPropertyChangeListener(name,listener)");
    }
    /**
     * Since I cannot listen unless you give me the name, this method throws an exception.
     * I will leave these here for Bean editors that don't recognize the named ones.
     * @param listener The listener to receive the events.
     * @throws SIMPluginAWTEventsException A fatal exception.
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) throws SIMPluginAWTEventsException {
        throw new SIMPluginAWTEventsException("Cannot use generic listeners with this object. Use the named versions removePropertyChangeListener(name,listener)");
    }
    
    /**
     * Registers a listener to receive events for the named SIM value.
     * @param name     The name of the value.
     * @param listener The listener to receive the events.
     */
    public void addPropertyChangeListener(String name,PropertyChangeListener listener) {
        m_Pcs.addPropertyChangeListener(name,listener);
    }
    
    /**
     * Removes a listener that was already registered to receive events for the named SIM value.
     * @param name     The name of the value.
     * @param listener The listener to receive the events.
     */
    public void removePropertyChangeListener(String name,PropertyChangeListener listener) {
        m_Pcs.removePropertyChangeListener(name,listener);
    }

    private Map<String,Data> m_eventqueue;    //implemented as a map so if dispatcher is slower than the event queuer, only the latest value will be in there.

    /**
     * Starts the dispatcher thread.
     * This should be called from the main thread after all your listeners are registered, and only once.
     * It starts a background thread and then returns.
     * 
     * @param callback (Optional) A callback implementation to receive events.
     */
    public void startThread(Callback callback) {

        m_eventqueue = new HashMap<String,Data>();

        //create a Thread that retrieves the data and updates the event queue
        Thread t = new Thread( new Runnable() {
            

            public void run() {
                Server.logger().info("dispatcher is running");

                m_SIMPlugin.run(new Callback() {
                    Map<String,Data> oldvalues = new HashMap<String,Data>();
                    int previps = -1;

                    public boolean Waiting(SIMPlugin SIMPlugin) throws SIMPluginException {
                        if (callback != null)
                            if (!callback.Waiting(SIMPlugin))
                                return false;
                        return true; 
                    }

                    public boolean DataReady(SIMPlugin SIMPlugin, Integer ips) throws SIMPluginException {
                        if (callback != null)
                            if (!callback.DataReady(SIMPlugin, ips))
                                return false;
                        
                        synchronized (m_eventqueue) {
                            PropertyChangeListener[] listeners = m_Pcs.getPropertyChangeListeners();
                            for (int i = 0; i < listeners.length; i++) {
                                if (listeners[i] instanceof PropertyChangeListenerProxy) {
                                    PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy)listeners[i];
                                        String name = proxy.getPropertyName();
                                        Data data = new Data(name);

                                        String names[] = name.split(";");

                                        for (String m : names) {
                                            Data o = null;
                                            if (m.equalsIgnoreCase("FPS")) {
                                                o = new Data(m,ips);
                                                //to get a true FPS, need to really get the most volatile data from the SIMPlugin and only return when it changes
                                                o.add(SIMPlugin.getData("Session/DataVersion"));
                                                data.add(o);
                                            }
                                            else
                                            {
                                                o = SIMPlugin.getData(m);
                                                data.add(m,o.getValue(),o.getUOM());
                                            }
                                        }
                                        if (data != null && (oldvalues.get(name) == null || !data.equals(oldvalues.get(name)))) {
                                            oldvalues.put(name, data);
                                            m_eventqueue.put(name, data);
                                        }
                                }
                            }

                            if (!m_eventqueue.isEmpty()) {
                                //fire the events on the event dispatch queue
                                EventQueue.invokeLater(new Runnable() {
                                    public void run() {
                                        try {
                                            //I'm going to fire the events inside this block so the Dispatcher will block until I'm done.
                                            synchronized (m_eventqueue) {
                                                for (String s : m_eventqueue.keySet()) {
                                                    Data sd = m_eventqueue.get(s);

                                                    //if it is FPS all by itself, don't fire unless it has changed.
                                                    //if combined with other values, then it will fire on DataVersion as well.
                                                    //Don't combine to reduce the number of events being fired.
                                                    if (s.equalsIgnoreCase("FPS")) {
                                                        if (sd.getInteger("FPS") != previps) {
                                                            m_Pcs.firePropertyChange(s, new Data(sd.getName()), sd);
                                                            previps = sd.getInteger("FPS");
                                                        }
                                                    }
                                                    else {
                                                        m_Pcs.firePropertyChange(s, new Data(sd.getName()), sd);
                                                    }
                                                }

                                                m_eventqueue.clear();
                                            } //synchronized
                                        }
                                        catch (Exception e) {
                                            Server.logStackTrace(Level.SEVERE,"Exception",e);
                                        }
                                    }
                                });
                            }
                        } //synchronized
                        return true;
                    }
                });

                //System.exit(0);
            }
        });

        t.setName(String.format("%s dispatcher",this.getClass().getName()));
        t.start();
    }
    
    public void startThread() {
        startThread(null);
    }

}
