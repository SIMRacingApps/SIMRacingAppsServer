/**
 * 
 */
package com.SIMRacingApps;

import static com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.SIMRacingApps.SIMPlugin.SIMPluginException;
import com.SIMRacingApps.Util.FindFile;
import com.SIMRacingApps.Util.SendKeys;
import com.SIMRacingApps.Util.Sound;
import com.SIMRacingApps.Util.URLBroadcastThread;
import com.SIMRacingApps.servlets.ConsumerTester;
import com.SIMRacingApps.servlets.DataEvent;
import com.SIMRacingApps.servlets.DataService;
import com.SIMRacingApps.servlets.DataSocket;
import com.SIMRacingApps.servlets.DataStreaming;
import com.SIMRacingApps.servlets.ROOT;
import com.SIMRacingApps.servlets.SIMRacingApps;
import com.SIMRacingApps.servlets.iRacing;
import com.SIMRacingApps.servlets.listings;
import com.SIMRacingApps.servlets.Data;
import com.SIMRacingApps.servlets.settings;
import com.SIMRacingApps.servlets.sendkeys;
import com.SIMRacingApps.servlets.upload;
import com.SIMRacingApps.servlets.useroverrides;
import com.owlike.genson.Genson;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.management.OperatingSystemMXBean;

import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer; 

/**
 * This class defines the static main() method used to start the internal server.
 * 
 * @author Jeffrey Gilliam
 * @copyright Copyright (C) 2015 - 2023 Jeffrey Gilliam
 * @since 1.0
 * @license Apache License 2.0
 */
public class Server {

    private static Logger _logger = null;
    private static String m_log   = "SIMRacingApps";
    private static ConsoleHandler _console = null;
    private static FileHandler _file = null;
    private static int m_port          = 80;
    private static Map<String,String> m_args = new HashMap<String,String>();
    private static Genson m_genson = new Genson();
    private static Properties m_version = new Properties();
    private static int RESERVED_CORES = 2;    
    public static String getLog() {
        return m_log + "-0.log.txt";
    }
    
    public static void setLog(String logname) {
        m_log = logname;
    }
    
    public static Logger logger() {

        //if logger not initialized, setup a console handler
        if (_logger == null) {
            _logger = Logger.getLogger("com.SIMRacingApps");
            _logger.setLevel(Level.INFO);
            
            _console = new ConsoleHandler();
            _console.setFormatter(new LogFormatter());
            _console.setLevel(_logger.getLevel());
            _logger.addHandler(_console);
            _logger.setUseParentHandlers(false);
        }

        //if the level has been changed, then update handlers
        if (_logger.getLevel().intValue() != _console.getLevel().intValue())
            _console.setLevel(_logger.getLevel());
        if (_file != null && _logger.getLevel().intValue() != _file.getLevel().intValue())
            _file.setLevel(_logger.getLevel());
        
        //If user wants a log file, setup a file handler.
        if (!m_log.isEmpty() && _file == null) {
            try {
                new File(FindFile.getUserPath()[0]+"/logs").mkdirs();
                //make the files small enough to email
                String filename = FindFile.getUserPath()[0]+"\\logs\\"+m_log+"-%g.log.txt";
                int maxsize = 10000000;
                int maxfiles = 3;
                _file = new FileHandler(filename,maxsize,maxfiles);
                _file.setFormatter(new LogFormatter());
                _file.setLevel(_logger.getLevel());
                _logger.addHandler(_file);
                _logger.info(String.format("Logging to %s, max size: %dMB, max files = %d",filename,maxsize/1000000,maxfiles));
            } catch (SecurityException | IOException e) {
                Server.logStackTrace(e);
            }
        }
        
        return _logger;
    }

    private static class LogFormatter extends Formatter {

        /* (non-Javadoc)
         * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
         */
        @Override
        public synchronized String format(LogRecord record) {
            StackTraceElement[] trace = (new Exception()).getStackTrace();
            
            String className = "unknownClass";
            for (int caller=1; caller < trace.length; caller++) {
                if (!trace[caller].toString().startsWith("java.util.logging.")
                &&  !trace[caller].toString().startsWith("com.SIMRacingApps.Server.logStackTrace")
                ) {
                    className = trace[caller].toString();
                    break;
                }
            }
            
            return String.format("%s: %-7s: %s: %s[%s]%n", 
                    String.format("%1$tY%1$tm%1$td%1$tH%1$tM%1$tS.%1$tL", Calendar.getInstance()),
                    record.getLevel(), 
                    record.getMessage(),
                    className,
                    Thread.currentThread().getName()
                    );
        }
    }
    
    /**
     * 
     * @param level The Level to log. Defaults to SEVERE.
     * @param message A message you provide to be logged with the trace.
     * @param trace An array to the stack trace.
     */
    public static synchronized void logStackTrace(Level level, String message, StackTraceElement[] trace) {
        logger().log(level,"logStackTrace(Level,String,StackTraceElement[])");
        if (message == null) {
            logger().log(level,"logStackTrace(Level,String,StackTraceElement[]): message is null");
            return;
        }
        if (trace == null) {
            logger().log(level,"logStackTrace(Level,String,StackTraceElement[]): trace is null");
            return;
        }
        for (int index=0; index < trace.length; index++) {
            String className  = trace[index].getClassName();
            String methodName = trace[index].getMethodName();
            String fileName   = trace[index].getFileName();
            String s = String.format("   at %s.%s (%s:%d) called by",
                            className  == null ? "null" : className,
                            methodName == null ? "null" : methodName,
                            fileName   == null ? "null" : fileName,
                            trace[index].getLineNumber()
            );
            logger().log(level,s);
        }
    }
    
    /**
     * This method should be used to log the entire stack trace to the log file.
     * 
     * @param level (optional) The Level to log. Defaults to SEVERE.
     * @param message (optional) A message you provide to be logged with the trace.
     * @param exception The exception to get the trace from.
     */
    public static synchronized void logStackTrace(Level level, String message, Exception exception) {
        logger().log(level,"Exception in thread \"" + Thread.currentThread().getName() + "\" " + exception.getClass().getName() + ": " + exception.getLocalizedMessage() + " " + (message != null ? message : "[null message]"));
        StackTraceElement[] trace = exception.getStackTrace();
        logStackTrace(level,message,trace);
        if (message != null && !message.isEmpty())
            logger().log(level,"exception.getLocalizedMessage(): " + exception.getLocalizedMessage() + " " + message);
    }

    /**
     * This method should be used to log the entire stack trace to the log file.
     * 
     * @param level (optional) The Level to log. Defaults to SEVERE.
     * @param message (optional) A message you provide to be logged with the trace.
     * @param throwable The throwable obtained from an exception.
     */
    public static synchronized void logStackTrace(Level level, String message, Throwable throwable) {
        logger().log(level,"Exception in thread \"" + Thread.currentThread().getName() + "\" " + throwable.getClass().getName() + ": " + throwable.getLocalizedMessage() + " " + (message != null ? message : "[null message]"));
        StackTraceElement[] trace = throwable.getStackTrace();
        logStackTrace(level,message,trace);
        if (message != null && !message.isEmpty())
            logger().log(level,throwable.getLocalizedMessage() + " " + message);
    }
    
    public static void logStackTrace(Level level,Exception exception) {
        logStackTrace(level,"",exception);
    }
    public static void logStackTrace(Exception exception) {
        logStackTrace(Level.SEVERE,"",exception);
    }
    
    /**
     * Use these methods to test if the specified level will actually log anything.
     * I do this in time critical sections to prevent forming the string to be logged,
     * if it's not going to be logged anyway.
     * @return true if level will be logged.
     */
    public static boolean isLogLevelFinest() {
        return logger().getLevel().intValue() <= Level.FINEST.intValue();
    }
    public static boolean isLogLevelFiner() {
        return logger().getLevel().intValue() <= Level.FINER.intValue();
    }
    public static boolean isLogLevelFine() {
        return logger().getLevel().intValue() <= Level.FINE.intValue();
    }
    public static boolean isLogLevelInfo() {
        return logger().getLevel().intValue() <= Level.INFO.intValue();
    }
    public static boolean isLogLevelWarning() {
        return logger().getLevel().intValue() <= Level.WARNING.intValue();
    }
    public static boolean isLogLevelSevere() {
        return logger().getLevel().intValue() <= Level.SEVERE.intValue();
    }
    
    public static int getPort() {
        return m_port;
    }
    
    /**
     * Returns the value of the specified argument as a string. 
     * The string will be empty if arg not found.
     * It is not case sensitive, all args are stored in lower case.
     * @param arg The argument name
     * @return The value of the argument
     */
    public static String getArg(String arg) {
        if (m_args.containsKey(arg.toLowerCase()))
            return m_args.get(arg.toLowerCase()).trim();
        return "";
    }
    
    /**
     * Returns the value of the specified argument as a string. 
     * The defaultValue be returned if arg not found or blank.
     * It is not case sensitive, all args are stored in lower case.
     * All values are trimmed of leading and trailing spaces.
     * @param arg The argument name
     * @param defaultValue The value to use if argument not found
     * @return The value of the argument
     */
    public static String getArg(String arg,String defaultValue) {
        if (m_args.containsKey(arg.toLowerCase()))
            return m_args.get(arg.toLowerCase()).trim();
        return defaultValue;
    }
    
    /**
     * Returns the value of the specified argument as a boolean. 
     * The defaultValue will be returned if arg not found or blank.
     * It is not case sensitive, all args are stored in lower case.
     * @param arg The argument name
     * @param defaultValue The value to use if argument not found
     * @return The value of the argument
     */
    public static boolean getArg(String arg,boolean defaultValue) {
        String s = getArg(arg);
        if (s.isEmpty())
            return defaultValue;
        try {
            if (s.equalsIgnoreCase("f") || s.equalsIgnoreCase("false"))
                return false;
            if (s.equalsIgnoreCase("t") || s.equalsIgnoreCase("true"))
                return true;
            if (s.equalsIgnoreCase("n") || s.equalsIgnoreCase("no"))
                return false;
            if (s.equalsIgnoreCase("y") || s.equalsIgnoreCase("yes"))
                return true;
            if (s.equalsIgnoreCase("off"))
                return false;
            if (s.equalsIgnoreCase("on"))
                return true;
            int i = Integer.parseInt(s);
            if (i != 0)
                return true;
            else
                return false;
        }
        catch (NumberFormatException e) {}
        return defaultValue;
    }
    
    /**
     * Returns the value of the specified argument as a integer. 
     * The defaultValue will be returned if arg not found or blank.
     * It is not case sensitive, all args are stored in lower case.
     * @param arg The argument name
     * @param defaultValue The value to use if argument not found
     * @return The value of the argument
     */
    public static int getArg(String arg,int defaultValue) {
        String s = getArg(arg);
        if (s.isEmpty())
            return defaultValue;
        try {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException e) {}
        return defaultValue;
    }
    
    /**
     * Returns the value of the specified argument as a double. 
     * The defaultValue will be returned if arg not found or blank.
     * It is not case sensitive, all args are stored in lower case.
     * @param arg The argument name
     * @param defaultValue The value to use if argument not found
     * @return The value of the argument
     */
    public static double getArg(String arg,double defaultValue) {
        String s = getArg(arg);
        if (s.isEmpty())
            return defaultValue;
        try {
            return Double.parseDouble(s);
        }
        catch (NumberFormatException e) {}
        return defaultValue;
    }

    /**
     * This method sends all the arguments to the log.
     */
    public static void logArgs() {
        for (Iterator<Entry<String, String>> itr = m_args.entrySet().iterator(); itr.hasNext();) {
            Entry<String, String> entry = itr.next();
            //don't log the secrets
            if (entry.getKey().toLowerCase().endsWith("-apikey")
            ||  entry.getKey().toLowerCase().endsWith("-token")
            ||  entry.getKey().toLowerCase().endsWith("-password")
               )
                logger().info(String.format("Arg(%s) = %s",entry.getKey(),entry.getValue().length() > 0 ? "xxxxx" : ""));
            else
                logger().info(String.format("Arg(%s) = %s",entry.getKey(),entry.getValue()));
        }
    }

    /**
     * This routine will parse for all args an put them in a map to be recalled from anywhere via any of the getArg()s methods.
     * The args can also be specified in a settings.txt file found in the userPath.
     * Command line args override args in the settings file.
     * It should be called by any application implementing it's own Main function.
     * <p>
     * The following arguments are used immediately to enable some features:
     * <p>
     * -level {logLevel}, where logLevel is SEVERE,WARNING,INFO,FINE,FINER,FINEST. Defaults to INFO.
     * <p>
     * -log {filename}, creates a log file using the -level option in the -userpath directory.
     * <p>
     * -settings {settingsFilename} The name of the settings file to use. Defaults to settings.txt.
     * <p>
     * -userpath {userPath}, A path where where users can add their own widgets and apps.
     *                       It can be a list of directories separated by a semi-colon, just like the PATH environment variable. 
     *                       All paths must be absolute paths,
     *                       Defaults to "%USERPROFILE%\Documents\SIMRacingApps".
     * <p>
     * -sendkeysdelay {milliseconds}, changes the delay sendkeys uses. Default is 32ms
     * <p>
     * -ip {IpAddress}, When users are connected to more than one network, this option forces
     *                  the server to bind to the specified address instead of the first one.
     * 
     * @param args command line arguments
     */
    public static void parseArgs(String[] args) {
        // parse arguments
        String settings = "settings.txt";
        for (int i=0; i < args.length; i++) {
            try {
                String arg = "";
                String value = "";
                        
                if (args[i].startsWith("-") || args[i].startsWith("/")) {
                    arg = args[i].substring(1).toLowerCase();
                    value = "";
                    if ((i+1) < args.length)
                        value = args[++i];
//can't do this, or values can't be a negative number
//                    if (value.startsWith("-") || value.startsWith("/")) {
//                        value = "";
//                        i--;
//                    }
                    m_args.put(arg, value);
                }
                
                if (m_args.containsKey("log")) {
                    Server.setLog(m_args.get("log"));
                }
                
                if (arg.equals("settings") && !value.isEmpty()) {
                    settings = value;
                }
                else
                if ((arg.equals("userpath") || arg.equals("userdir")) && !value.isEmpty()) {
                    FindFile.setUserPath(value);
                    //make sure the user's first folder exists
                    new File(FindFile.getUserPath()[0]+"/storage").mkdirs();
                    new File(FindFile.getUserPath()[0]+"/favorites").mkdirs();
                }
            }
            catch (Exception e) {
                Server.logStackTrace(e);
                System.exit(1);
            }
        }
        
        InputStream is = null;
        try {
            Properties userProperties = new Properties();
            FindFile settingsFile = new FindFile(settings);
            Server.logger().info("Reading settings from: "+settingsFile.toString());
            userProperties.load(is = settingsFile.getInputStream());
            Iterator<Entry<Object, Object>> itr = userProperties.entrySet().iterator();
            while (itr.hasNext()) {
                Entry<Object, Object> entry = itr.next();
                String key = (String) entry.getKey();
                
                if (key.startsWith("-") || key.startsWith("/"))
                    key = key.substring(1);
                
                if (!m_args.containsKey(key.toLowerCase())) {
                    m_args.put(key.toLowerCase(), (String)entry.getValue());
                    if (m_args.containsKey("log")) {
                        Server.setLog(m_args.get("log"));
                    }
                }
            }
        } catch (IOException e) {}
        finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {}
        }
        
        if (m_args.containsKey("log")) {
            Server.setLog(m_args.get("log"));
        }

        //now load a {sim}.settings.txt file
        String sim = m_args.containsKey("sim") ? m_args.get("sim") : "";
        if (!sim.isEmpty()) {
            is = null;
            try {
                Properties userProperties = new Properties();
                FindFile settingsFile = new FindFile(sim+"."+settings);
                Server.logger().info("Reading SIM settings from: "+settingsFile.toString());
                
                userProperties.load(is = settingsFile.getInputStream());
                Iterator<Entry<Object, Object>> itr = userProperties.entrySet().iterator();
                while (itr.hasNext()) {
                    Entry<Object, Object> entry = itr.next();
                    String key = (String) entry.getKey();
                    
                    if (key.startsWith("-") || key.startsWith("/"))
                        key = key.substring(1);
                    
                    if (!m_args.containsKey(key.toLowerCase()))
                        m_args.put(key.toLowerCase(), (String)entry.getValue());
                }
            } catch (IOException e) {}
            finally {
                if (is != null)
                    try {
                        is.close();
                    } catch (IOException e) {}
            }
        }
        
        if (m_args.containsKey("log")) {
            Server.setLog(m_args.get("log"));
        }
        
        if (m_args.containsKey("level")) {
            try {
                Level l = Level.parse(m_args.get("level"));
                if (l != null) {
                    logger().setLevel(l);
                }
            } catch (IllegalArgumentException e) {
                logger().warning("level = " + m_args.get("level") + " is an invalid level");
            }
        }
        if (m_args.containsKey("sendkeysdelay")) {
            SendKeys.setDelay(Integer.parseInt(m_args.get("sendkeysdelay")));
            logger().info(String.format("sendkeys delay = %d",SendKeys.getDelay()));
        }
        
        logger().info(String.format("Logger.Level = %s",logger().getLevel().toString()));

        logArgs();
    }
    
    /**
     * This is the main static method that Java will used to startup the server.
     * The server will create an internal Jetty instance and register all the servlets.
     * <p>
     * The arguments are:
     * <p>
     * -sim {SIMName}, the name of the SIM to start for. Defaults to iRacing.
     * <p>
     * -port {portNumber}, defaults to 80.
     * <p>
     * -play {playFile}, a file to play that was previously recorded by SIMRacingApps or by the SIM.
     * <p>
     * -record {recordFile}, the filename to record this session to. It will be saved in the recordings folder.
     * <p>
     * -startingVersion {version}, when playing back a file, start at this data point.
     * <p>
     * -endingVersion {version}, when playing back a file, stop at this data point.
     * 
     * @param args command line arguments
     */
    //@SuppressWarnings("deprecation")
    private static Process electronProcess = null;
    private static Process browserProcess = null;
    
    public static String m_hostname = "";  //once the service starts up, it will populate this.

    private static org.eclipse.jetty.server.Server startServer(int port) throws Exception {
        //since my package is also named Server, I have to specify the entire path to Jetty
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(port);
        
        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.setResourceBase(System.getProperty("java.io.tmpdir"));
        server.setHandler(contextHandler);
        
        ServletHolder servlet;
        
        servlet = contextHandler.addServlet(ROOT.class,          "/");
        servlet = contextHandler.addServlet(SIMRacingApps.class, "/SIMRacingApps/*");
        servlet = contextHandler.addServlet(listings.class,      "/SIMRacingApps/listings");
        servlet = contextHandler.addServlet(iRacing.class,       "/SIMRacingApps/iRacing/*");
        servlet = contextHandler.addServlet(upload.class,        "/SIMRacingApps/upload");
        servlet = contextHandler.addServlet(settings.class,      "/SIMRacingApps/settings");
        servlet = contextHandler.addServlet(useroverrides.class, "/SIMRacingApps/useroverrides");
        servlet = contextHandler.addServlet(sendkeys.class,      "/SIMRacingApps/sendkeys/*");
        servlet = contextHandler.addServlet(sendkeys.class,      "/SIMRacingApps/SENDKEYS/*");
        servlet = contextHandler.addServlet(sendkeys.class,      "/SIMRacingApps/sendKeys/*");
        servlet = contextHandler.addServlet(sendkeys.class,      "/SIMRacingApps/SendKeys/*");
        servlet = contextHandler.addServlet(Data.class,          "/SIMRacingApps/Data/*");
        servlet.setInitOrder(0); //Jetty's way to load on startup
        servlet = contextHandler.addServlet(DataEvent.class,     "/SIMRacingApps/DataEvent");
        servlet = contextHandler.addServlet(ConsumerTester.class,"/SIMRacingApps/ConsumerTester");
    
        ServerContainer container = WebSocketServerContainerInitializer.configureContext(contextHandler); 
         
        // Add endpoint to server container 
        ServerEndpointConfig socketConfig = ServerEndpointConfig.Builder.create(DataSocket.class,"/SIMRacingApps/DataSocket").build(); 
        container.addEndpoint(socketConfig); 

        // Add endpoint to server container 
        ServerEndpointConfig streamingConfig = ServerEndpointConfig.Builder.create(DataStreaming.class,"/SIMRacingApps/DataStreaming").build(); 
        container.addEndpoint(streamingConfig); 
        
        server.start();
        return server;
    }
    
    public static void main(String[] args) {

        //look through the args and get the log file and userpath before doing anything else.
        String log = null;
        String userpath = null;
        
        for (int i=0; i < args.length; i++) {
            try {
                String arg = "";
                String value = "";
                        
                if (args[i].startsWith("-") || args[i].startsWith("/")) {
                    arg = args[i].substring(1).toLowerCase();
                    value = "";
                    if ((i+1) < args.length)
                        value = args[++i];
                    if (arg.equalsIgnoreCase("log") && !value.isEmpty()) {
                        log = value;
                    }
                    else
                    if ((arg.equals("userpath") || arg.equals("userdir")) && !value.isEmpty()) {
                        userpath = value;
                    }
                }
            }
            catch (Exception e) {
                Server.logStackTrace(e);
                System.exit(1);
            }
        }
        
        if (userpath != null)
            FindFile.setUserPath(userpath);
        if (log != null)
            Server.setLog(log);
        
        //make sure the user's folders exists
        //make sure the user's first folder exists
        new File(FindFile.getUserPath()[0]+"/storage").mkdirs();
        new File(FindFile.getUserPath()[0]+"/favorites").mkdirs();
        
        //see if the user has a settings.txt file and copy the default over if they don't
        FindFile default_settings = null;
        FindFile settings = null;
        try {
            default_settings = new FindFile("com/SIMRacingApps/default.settings.txt");
            settings         = new FindFile("settings.txt");
        }
        catch (FileNotFoundException e) {
            if (default_settings != null) {
                FindFile.copy(default_settings,new File(FindFile.getUserDocumentsPath()+"/SIMRacingApps/settings.txt"));
            }
        }
        if (default_settings != null) default_settings.close();
        default_settings = null;
        if (settings != null) settings.close();
        settings = null;
        
        parseArgs(args);
        
        Sound.loadMixers();     //load the sound mixers
        
        if (!getArg("port").isEmpty()) {
            m_port = Integer.parseInt(getArg("port"));
            logger().info(String.format("Port = %d",m_port));
        }

        if (!getArg("play").isEmpty()) {
            DataService.setPlay(getArg("play"));
        }

        if (!getArg("record").isEmpty()) {
            DataService.setRecord(getArg("record"));
        }

        if (!getArg("startingversion").isEmpty()) {
            DataService.setStartingVersion(getArg("startingversion"));
        }

        if (!getArg("endingversion").isEmpty()) {
            DataService.setEndingVersion(getArg("endingversion"));
        }

        if (!getArg("sim").isEmpty()) {
            DataService.setSIMname(getArg("sim"));
        }

        File tmpdir = new File(System.getProperty("java.io.tmpdir") + "SIMRacingApps");
        
        logger().info(String.format("com.SIMRacingApps.main() using tmpdir = %s",tmpdir));
        
        try {
            org.eclipse.jetty.server.Server server = null;
            
            try {
                server = startServer(m_port);
            } catch (BindException be) {
                try {
                    //if requested port is in use, try 8080
                    logger().warning(String.format("Port %s in use, trying 8080",m_port));
                    m_port = 8080;
                    server = startServer(m_port);
                } catch (BindException be2) {
                    //if 8080 is in use, try 5555
                    logger().warning(String.format("Port 8080 in use, trying 5555"));
                    m_port = 5555;
                    server = startServer(m_port);
                }
            }
            
            InputStream in;
            //see if the user want's to tell us what ip to bind to
            String ip = Server.getArg("ip", "" );
            try {

                  String hostname = Server.getArg("hostname",InetAddress.getLocalHost().getHostName());
                  Server.logger().info("Hostname = " + hostname);
                  InetAddress[] addresses = InetAddress.getAllByName(hostname);
                  for (InetAddress addr : addresses) {
                      String host = addr.getHostAddress();
                      if (addr instanceof Inet4Address) {
                          if (ip.isEmpty()) //this will use the first one found, but list them all in the log
                              ip = host;
                          Server.logger().info("Found Address = " + host);
                      }
                  }
            } 
            catch (UnknownHostException e) {
                  Server.logStackTrace(Level.WARNING, "while getting IP address",e);
            } 
                      
            synchronized (Server.m_hostname) {
                  Server.m_hostname = ip;
            }
            
            ip = "http://" + ip + ((Server.getPort() == 80) ? "" : ":" + Server.getPort()); 
            URLBroadcastThread.start(ip);
            
            if (isLogLevelFiner())
                server.dumpStdErr();

            try {
                String userPath = FindFile.getUserPath()[0];
                new File(userPath).mkdirs();
                //URLClassLoader loader = (URLClassLoader)Thread.currentThread().getContextClassLoader();
                URLClassLoader loader = (URLClassLoader)Thread.currentThread().getContextClassLoader();
                URL[] urls = loader.getURLs();
                String classpath = "";
                for (int i=0; i < urls.length; i++)
                    classpath = classpath.length() > 0 ? ";" + urls[i].toString() : urls[i].toString();
                    
                in = loader.getResourceAsStream("com/SIMRacingApps/version.properties");
                //in = new FileInputStream(config.getServletContext().getRealPath("") + "version.properties");
                m_version.load(in);
                in.close();
                Server.logger().fine(System.getProperties().toString());
                Double dGHz = -1.0;
                String processor = "Unknown";
                String OSInfo = "";
                try {
                    int iMHz = Advapi32Util.registryGetIntValue(HKEY_LOCAL_MACHINE, "HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0", "~MHz");
                    dGHz = iMHz / 1000.0;
                    processor = Advapi32Util.registryGetStringValue(HKEY_LOCAL_MACHINE, "HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0", "ProcessorNameString"); 
                }                
                catch (Exception e) {
                    //log any exceptions, but keep on going
                    Server.logStackTrace(e);
                }
                
                try {
                    OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    
    //In Java 10 access via reflection has been removed. Must call the methods directly.
                    OSInfo += String.format("%n%-35s %,d","OS TotalPhysicalMemorySize:", operatingSystemMXBean.getTotalPhysicalMemorySize());
                    OSInfo += String.format("%n%-35s %,d","OS FreePhysicalMemorySize:", operatingSystemMXBean.getFreePhysicalMemorySize());
    
    //                for (Method method : operatingSystemMXBean.getClass().getDeclaredMethods()) {
    //                    method.setAccessible(true);
    //                    if (method.getName().startsWith("get")
    //                        && Modifier.isPublic(method.getModifiers())) {
    //                            Object value;
    //                        try {
    //                            value = method.invoke(operatingSystemMXBean);
    //                        } catch (Exception e) {
    //                            value = e;
    //                        } // try
    //                        if (value instanceof Long || value instanceof Integer)
    //                            OSInfo += String.format("%n%-35s %,d","OS " + method.getName().substring(3) + ":",value);
    //                        else
    //                        if (value instanceof Double)
    //                            OSInfo += String.format("%n%-35s %,f","OS " + method.getName().substring(3) + ":",value);
    //                        else
    //                            OSInfo += String.format("%n%-35s %s","OS " + method.getName().substring(3) + ":",value.toString());
    //                    } // if
    //                  } // for
                }
                catch (Exception e) {
                    //log any exceptions, but keep on going
                    Server.logStackTrace(e);
                }
                
                Server.logger().info(
                      String.format("%n*************************************************************************************************")
                    + String.format("%n%s", (String)m_version.get("copyright"))
                    + String.format("%n%s", (String)m_version.get("license"))
                    + String.format("%n%s%s", ip,(String)m_version.get("copyrightUrl"))
                    + String.format("%n%s%s", ip,(String)m_version.get("licenseUrl"))
                    + String.format("%n%s%s", ip,(String)m_version.get("noticeUrl"))
                    + String.format("%n%n%-35s %s", "Your Server Address:", ip)
                    + String.format("%n%-35s %s.%s Build-%s","SIMRacingAppsServer Version:",(String)m_version.get("major"),(String)m_version.get("minor"),(String)m_version.get("build"))
                    + String.format("%n%-35s %s", DataService.getSIMName().getString() + " Plugin Version:", DataService.getSIMVersion().getString())
                    + String.format("%n%-35s %s", "Your Personal Folder:", userPath)
                    + String.format("%n%-35s %s by %s", "Java Runtime:", System.getProperty("java.runtime.version"),System.getProperty("java.vm.vendor"))
                    + String.format("%n%-35s %s", "Java VM Name:", System.getProperty("java.vm.name"))
                    + String.format("%n%-35s %s", "Java java.class.path:", System.getProperty("java.class.path"))
                    + String.format("%n%-35s %s", "Java main.thread.classpath:", classpath)
                    + String.format("%n%-35s %,d", "Java TotalMemory (bytes):", Runtime.getRuntime().totalMemory())
                    + String.format("%n%-35s %,d", "Java FreeMemory (bytes):", Runtime.getRuntime().freeMemory())
                    + String.format("%n%-35s %s", "Java MaxMemory (bytes):", Runtime.getRuntime().maxMemory() == Long.MAX_VALUE ? "no limit" : String.format("%,d",Runtime.getRuntime().maxMemory()) )
                    + String.format("%n%-35s %s", "OS Name:", System.getProperty("os.name"))
                    + String.format("%n%-35s %s", "OS Version:", System.getProperty("os.version"))
                    + String.format("%n%-35s %s", "OS Architecture:", System.getProperty("os.arch"))
                    + String.format("%n%-35s %s", "OS Processor:", processor)
                    + String.format("%n%-35s %d", "OS AvailableCores:", Runtime.getRuntime().availableProcessors())
                    + String.format("%n%-35s %d", "ARGS Reserved Cores:", Server.getArg("reserved-cores",RESERVED_CORES))
                    + String.format("%n%-35s %.2f GHz", "OS Processor Speed:", dGHz)
                    + OSInfo
                    + String.format("%n*************************************************************************************************")
                );
                Windows.setConsoleTitle(
                        "SIMRacingAppsServer "
                      + (String)m_version.getProperty("major")
                      + "."
                      + (String)m_version.getProperty("minor")
                      + " Build: "
                      + (String)m_version.getProperty("build")
                      + " @ " 
                      + ip);
            } catch (IOException | SIMPluginException e) {
                Server.logStackTrace(Level.WARNING, "while getting version",e);
            }

            //reserve the cores, but only if there enough to reserve
            int cores_to_reserve = Runtime.getRuntime().availableProcessors() > Server.getArg("reserved-cores",RESERVED_CORES) 
                                 ? Server.getArg("reserved-cores",RESERVED_CORES) 
                                 : 0;
            Windows.setAffinity(cores_to_reserve);

            //Check if electron can be installed. If so, check version and install it, if needed.
            //default to false here so existing users will not get a surprise.
            //But, in the default settings.txt, I will have it set to true for new users.
            
            if (getArg("electron-autoupdate",false) || getArg("electron-force-update",false)) {
 
                //Use the FindFile class to location the exe file
                FindFile classFromJar = new FindFile("com/SIMRacingApps/default.settings.txt");
                File jarFile = new File(URLDecoder.decode(classFromJar.getClass().getProtectionDomain().getCodeSource().getLocation().getPath(),StandardCharsets.UTF_8.name()));
                
                if (jarFile.isFile()) {
                    String jarVersion = "", installedJarVersion="";
                    
                    //get the version in the jar
                    InputStream is_p = classFromJar.getClass().getClassLoader().getResourceAsStream("electron-apps/package.json");
                    if (is_p != null) {
                        @SuppressWarnings("unchecked")
                        Map<String,Object> packageJson = (Map<String, Object>) m_genson.deserialize(is_p, Map.class);
                        jarVersion = (String) packageJson.get("version");
                        is_p.close();
                    }
                    
                    try {
                        File installedPackageJsonFile = new File(FindFile.getAppData()+"/electron-apps/package.json");
                        is_p = new FileInputStream(installedPackageJsonFile);
                        if (is_p != null) {
                            @SuppressWarnings("unchecked")
                            Map<String,Object> packageJson = (Map<String, Object>) m_genson.deserialize(is_p, Map.class);
                            installedJarVersion = (String) packageJson.get("version");
                            is_p.close();
                        }
                    }
                    catch (FileNotFoundException e) {}

                    logger().info("electron-apps: "+FindFile.getAppData()+"/electron-apps");
                    logger().info("Electron: Source Version = "+jarVersion+", Destination Version = "+installedJarVersion);

                    //if the versions are not equal, we need to install
                    if (!jarVersion.equals(installedJarVersion) || getArg("electron-force-update",false)) {
                        File exe = new File(getArg("electron-path",FindFile.getAppData()+"/electron-apps/electron/SIMRacingApps-electron.exe"));
                        
                        ProcessBuilder processBuilder = new ProcessBuilder("tasklist.exe");
                        Process process = processBuilder.start();
                        Scanner scanner = new Scanner(process.getInputStream(), "UTF-8");
                        scanner.useDelimiter("\\A");
                        String tasksList = scanner.hasNext() ? scanner.next() : "";
                        scanner.close();
                        boolean isRunning = tasksList.contains(exe.getName().substring(0, 25));  //the name gets truncated to 25 characters
                        
                        if (isRunning) {
                            logger().info("Electron: " + exe.getName() + " is running. Cannot update Electron");
                        }
                        else {
                            JarFile jar = new JarFile(jarFile);
                            String name = "";
                            logger().info("Electron: Installing from " + jarFile.toString());
                            
                            try {
                                Enumeration<JarEntry> entries = jar.entries();
                                while (entries.hasMoreElements()) {
                                    name = entries.nextElement().getName();
                                    if (name.startsWith("electron-apps/")) {
                                        if (name.endsWith("/")) {
                                            new File(FindFile.getAppData()+"/"+name).mkdirs();
                                        }
                                        else {
                                            is_p = classFromJar.getClass().getClassLoader().getResourceAsStream(name);
                                            if (is_p != null) {
                                                File dest = new File(FindFile.getAppData()+"/"+name);
                                                Server.logger().info("Electron: Installing "+dest.toString());
                                                FindFile.copy(is_p,new File(name),dest);
                                                is_p.close();;
                                            }
                                        }
                                    }
                                }
                            }
                            catch (Exception e) {
                                Server.logger().severe("Electron: Exception while installing: " + name + "\r\n" + e.getMessage());
                            }
                            finally {
                                jar.close();
                            }
                        }
                    }
                }
            }
            
            if (getArg("electron-autostart",false)) {
                try {
                    File exe = new File(getArg("electron-path",
                                        getArg("electron-apps",FindFile.getAppData()+"/electron-apps")+"/electron/SIMRacingApps-electron.exe"));
                    
                    ProcessBuilder processBuilder = new ProcessBuilder("tasklist.exe");
                    Process process = processBuilder.start();
                    Scanner scanner = new Scanner(process.getInputStream(), "UTF-8");
                    scanner.useDelimiter("\\A");
                    String tasksList = scanner.hasNext() ? scanner.next() : "";
                    scanner.close();
                    boolean isRunning = tasksList.contains(exe.getName().substring(0, 25));  //the name gets truncated to 25 characters
                    
                    if (isRunning) {
                        logger().info("Electron: " + exe.getName() + " is already running");
                    }
                    else
                    if (exe.canExecute()) {
                        File dir = new File(exe.getParent());
                        List<String> a = new ArrayList<String>();
                        String s;
                        
                        logger().info("Electron: Starting " + exe.toString());
                        a.add(exe.toString());
                        if (!(s = getArg("electron-options","")).isEmpty()) {
                            String[] sa = s.split(" ");
                            for (int i=0; i < sa.length; i++)
                                a.add(sa[i]);
                        }
                        
//                        if (getArg("electron-disable-gpu",true)) {
//                            a.add("--disable-gpu");  //prevents studdering on my machine
//                                                     //use to be require for transparency, that's no longer the case as of electron 1.6
//                        }
                        
                        a.add(getArg("electron-apps",FindFile.getAppData()+"/electron-apps"));
                        
                        if (!(s = getArg("electron-client-options","")).isEmpty()) {
                            String[] sa = s.split(" ");
                            for (int i=0; i < sa.length; i++)
                                a.add(sa[i]);
                        }
                        
                        //the host has to be the same computer, so force it.
                        a.add("-hostname");
                        a.add(Server.getArg("electron-hostname",m_hostname));
                        
                        //always pass the port in case Electron saved the wrong one
                        a.add("-port");
                        a.add(Integer.toString(m_port));

                        if (!getArg("electron-disable-gpu",true)) {
                            a.add("-enableHardwareAcceleration");
                        }

                        //force electron to the same language as the server.
                        s = System.getProperty("user.language")+"-"+System.getProperty("user.country");
                        a.add("-lang");
                        a.add(getArg("electron-lang",s.toLowerCase()));  //let the user override the system's language
                        
                        if (!(s = getArg("electron-configuration","")).isEmpty()) {
                            a.add("-configuration");
                            a.add(s);
                        }
                        
                        if (!getArg("electron-noclickthrough",true)) {
                            a.add("-noclickthrough");
                        }
                        
                        if (getArg("electron-startminimized",false)) {
                            a.add("-startminimized");
                        }
                        
                        if (getArg("electron-showappsontaskbar",false)) {
                            a.add("-showappsontaskbar");
                        }
                        
                        ProcessBuilder pb = new ProcessBuilder(a);
                        Map<String,String> env = pb.environment();
                        env.put("ELECTRON_NO_ATTACH_CONSOLE","true");
                        pb.directory(dir);
                        electronProcess = pb.start();
                        logger().info("Electron: Started with " + a.toString() );
                        //watch for output from Electron and write it to the log
                        while (electronProcess.isAlive()) {
                            BufferedReader stdin = new BufferedReader(
                                    new InputStreamReader(
                                            electronProcess.getInputStream()
                                    )
                            );
                            BufferedReader stderr = new BufferedReader(
                                    new InputStreamReader(
                                            electronProcess.getErrorStream()
                                    )
                            );
                            
                            String line;
                            while ((line = stdin.readLine()) != null)
                                logger().fine("Electron: "+line);
                            while ((line = stderr.readLine()) != null)
                                logger().warning("Electron: "+line);
                            Thread.sleep(1000);
                        }
                        if (getArg("electron-kills-server",true)) {
                            logger().info("Electron: Stopped" );
                            DataService.stop();
                            Thread.sleep(5000);
                            logger().info("Server: Exiting");
                            Thread.sleep(1000);
                            System.exit(0);
                        }
                    }
                    else {
                        logger().warning("Electron: " + exe.toString() + " not found");
                    }
                } catch (Exception e1) {
                  logStackTrace(Level.WARNING,e1);
                }
            }
            else
            if (getArg("browser-autostart",false) || !getArg("app-autostart-url","").isEmpty()) {
                File exe = new File(getArg("cmd-path",System.getenv("ComSpec")));
                
                if (exe.canExecute()) {
                    List<String> a = new ArrayList<String>();
                    String s;
                    
                    logger().info("Browser: Starting " + exe.toString());
                    //wait for the service to start up
                    while (true) {
                        synchronized (m_hostname) {
                            if (!m_hostname.isEmpty())
                                break;
                        }
                    }
                    
                    a.add(exe.toString());
                    a.add("/c");
                    a.add("start");
                    a.add("\"SIMRacingApps\"");
                    s = System.getProperty("user.language")+"-"+System.getProperty("user.country");
                    String url = "http://"+Server.getArg("browser-hostname",m_hostname)+":"+m_port + Server.getArg("app-autostart-url","?lang="+getArg("browser-lang",s.toLowerCase()));
                    a.add(url);
                    
                    ProcessBuilder pb = new ProcessBuilder(a);
                    browserProcess = pb.start();
                    logger().info("Browser: Started with " + a.toString() );
                    while (browserProcess.isAlive()) {
                        BufferedReader stdin = new BufferedReader(
                                new InputStreamReader(
                                        browserProcess.getInputStream()
                                )
                        );
                        BufferedReader stderr = new BufferedReader(
                                new InputStreamReader(
                                        browserProcess.getErrorStream()
                                )
                        );
                        
                        String line;
                        while ((line = stdin.readLine()) != null)
                            logger().fine("Browser: "+line);
                        while ((line = stderr.readLine()) != null)
                            logger().warning("Browser: "+line);
                        Thread.sleep(1000);
                    }
                }
            }
            
            //TODO: can't get this to work when the CMD window is closed
            Runtime.getRuntime().addShutdownHook(new Thread()
            {
                @Override
                public void run()
                {
                    if (electronProcess != null) {
                        electronProcess.destroy();
                    }
                }
            });
            
            server.join();
        } catch (BindException be) {
//            for (Thread t : Thread.getAllStackTraces().keySet()) {
//                //System.err.println(t.getName());
//                if (t.getName().startsWith("Servlet.DataService.")) t.stop();
//            }
            DataService.stop();
            logStackTrace(Level.SEVERE,"All Ports: "+Server.getArg("port","80")+",8080,5555",be);
            System.err.print("\nTo solve this problem, see https://github.com/SIMRacingApps/SIMRacingApps/wiki/Port-80-in-use-by-another-process\n\n");
            System.err.print("Press ENTER to exit...");
            try { System.in.read(); } catch (IOException e) {}
            System.exit(1);
        } catch (Exception e1) {
//            for (Thread t : Thread.getAllStackTraces().keySet()) {
//                System.err.println(t.getName());
//                if (t.getName().startsWith("Servlet.DataService.")) t.stop();
//            }
            DataService.stop();
            logStackTrace(Level.SEVERE,e1);
            System.err.print("Press ENTER to exit...");
            try { System.in.read(); } catch (IOException e) {}
            System.exit(1);
        }
        System.exit(0);
    }
}
