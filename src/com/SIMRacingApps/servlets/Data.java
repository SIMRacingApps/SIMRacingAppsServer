package com.SIMRacingApps.servlets;

import static com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE;

import java.io.*;
//import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Level;

import com.owlike.genson.*;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;



//import javax.management.AttributeNotFoundException;
//import javax.management.InstanceNotFoundException;
//import javax.management.MBeanException;
//import javax.management.MBeanServer;
//import javax.management.MalformedObjectNameException;
//import javax.management.ObjectName;
//import javax.management.Query;
//import javax.management.ReflectionException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.SIMRacingApps.SIMPlugin.SIMPluginException;
import com.SIMRacingApps.Server;
import com.SIMRacingApps.Windows;
import com.SIMRacingApps.Util.FindFile;
import com.SIMRacingApps.Util.URLBroadcastThread;

/**
 * This class implements the "/Data" interface for the HTTP protocol.
 * It accepts the POST and GET methods for requesting and receiving data from the SIM.
 * <p>
 * There are 2 ways to use these methods. 
 * One is Subscribe and Publish and the other is ReST(<a href="https://en.wikipedia.org/wiki/Representational_state_transfer" target="_blank">Representational State Transfer</a>).
 * <p>
 * <b>Subscribe and Publish</b>
 * <p>
 * This process uses the POST method to subscribe to the data to publish. 
 * The GET method is then used to poll the server for the published changes.
 * Through other HTTP interfaces, you can also use 
 * {@link com.SIMRacingApps.servlets.DataSocket Web Socket Polling},
 * {@link com.SIMRacingApps.servlets.DataStreaming Web Socket Streaming}, 
 * and {@link com.SIMRacingApps.servlets.DataEvent Web Events}
 * to receive the published changes.
 * <p>
 * Both POST and GET must have a parameter called "sessionid", in the URL query string, with a value that will be used to identify who the subscriptions below to.
 * Depending on the client, you may want to disable caching by adding a counter parameter that is incremented on every call to GET.
 * To help out, the response headers are also set to disable caching. For example: http://localhost/SIMRacingApps/Data?sessionid=1234567890.
 * I would recommend using a value that will not conflict with other clients using the server at the same time, like time of day down to the millisecond.
 * <p>
 * The body of the POST method must be formatted as a JSON string according to the following structure. 
 * This same structure will be updated with the values and returned to you when you call GET.
 * It is important to note, that only changes are returned by GET for performance reasons.
 * Changes means, that if any of the "names" associated with an "id" changes, then all of the "names" for that "id" are returned.
 * Also, when the Interval has passed, all "names" with a shorter or equal Interval will also be returned so they will all be in sync with the SIM.
 * <pre>
 * {
 *   "(id)":  {  "(name)":  { "Name": "(datapath)",  "Format": "(formatstring)",  "UOM": "(requesteduom)",  "Interval": (milliseconds) }
 *          ,    "(name2)": { "Name": "(datapath2)", "Format": "(formatstring2)", "UOM": "(requesteduom2)", "Interval": (milliseconds) }  //optional
 *          ,... //followed with as many as you need
 *        }
 *  ,"(id2)": {  "(name)":  { "Name": "(datapath)",  "Format": "(formatstring)",  "UOM": "(requesteduom)",  "Interval": (milliseconds) }  //optional
 *          ,    "(name2)": { "Name": "(datapath2)", "Format": "(formatstring2)", "UOM": "(requesteduom2)", "Interval": (milliseconds) }  //optional
 *          ,... //followed with as many as you need
 *        }
 *  ,...
 * }
 * </pre>
 * <dl>
 *      <dt>id</dt>             <dd>An identifier to be used as you see fit. It is only used by the server to group names together.</dd>
 *      <dt>name</dt>           <dd>Also an identifier to be used as you see fit.</dd>
 *      <dt>datapath</dt>       <dd>A path to the SIM data you are subscribing to. For example: "/Car/REFERENCE/Description".</dd>
 *      <dt>formatstring</dt>   <dd>A format string using the syntax of {@link java.util.Formatter}.</dd>
 *      <dt>requesteduom</dt>   <dd>The unit of measure you would like the result in. Note: If it cannot be converted, the SIM's UOM is returned.</dd>
 *      <dt>milliseconds</dt>   <dd>How often this particular should check for changes. Slowing these down can help performance.</dd>
 * </dl>
 * POST also supports the following parameters in the query string:
 * <ul>
 *   <li> startingversion=(startingVersion) - a SIM specific counter of where to start playing the recorded file.</li>
 *   <li> endingversion=(endingVersion) - A SIM specific counter of where to stop playing the record file.</li>
 *   <li> play=(filename) - Plays the previously recorded file.</li>
 *   <li> record=(filename) - Records the current session to a file. Saves the file in SRA_DIR/webapps/SIMRacingApps/recordings.</li>
 *   <li> stop - stops playing or stops recording.</li>
 * </ul>
 * The GET method should be called at least as often as the smallest Interval of all the names in the subscribed data structure.
 * It returns the changed data in the very same format as you subscribed to with the additional attributes added.
 * For details about these attributes, see {@link com.SIMRacingApps.Data}.
 * Also, the call to POST will return this data as well. So, you want to parse the response body for both.
 * Failure to call GET within 10 seconds of the last call will assume the client has died and delete the session cache.
 * This should not be a problem as most practical applications will need to call GET multiple times per second.
 * To recover, the client can call POST again.
 * <ul>
 *      <li>ValueFormatted</li>
 *      <li>Value</li>
 *      <li>Type</li>
 *      <li>UOMAbbr</li>
 *      <li>UOMDescr</li>
 *      <li>State</li>
 *      <li>StatePercent</li>
 * </ul> 
 * <P>
 * <b>ReST</b>
 * <p>
 * With ReST you do not call POST, because you can only specify one data path in the URL.
 * Instead, you call GET with the data path appended to the URL after "/Data". 
 * For example: http://localhost/SIMRacingApps/Data/Car/REFERENCE/Description.
 * Because of this, ReST does not perform as well as Subscribe/Publish for larger data sets. 
 * It also does not guarantee the data will be in sync between the calls to GET for multiple values.
 * Therefore, it usefulness is degraded to when Subscribe/Publish is not needed, such as, one time or infrequent calls.
 * By default, this returns a JSON string formatted "pretty" because I see it primarily being used from the browser ad-hoc.
 * If the data returned refers to a structure, then the ValueFormatted may actually be a JSON string. 
 * For example: http://localhost/SIMRacingApps/Data/Car/REFERENCE, will return all the data available for that car in JSON format.
 * You can specify the following parameters in the query string to change result returned.
 * <ul>
 *   <li> lang=(lang), to the data be localized to this language instead of the servers default.</li>
 *   <li> format=(format), a format using {@link java.util.Formatter} syntax.</li>
 *   <li> uom=(uom), the unit of measure to return the result in.</li>
 *   <li> output=(one of the following. See {@link com.SIMRacingApps.Data} for details).
 *        <ul>
 *           <li>ValueFormatted,Formatted - Returns the Value formatted with the "format" string and translated to "lang".</li>
 *           <li>Value - Returns the raw value.</li>
 *           <li>State - Returns the state.</li>
 *           <li>StatePercent - Returns the percentage of the Value of the State that it is in.</li>
 *           <li>Lang - Returns the language/locale of the request.</li>
 *           <li>Uom - Returns the raw unit of measure code.</li>
 *           <li>UOMAbbr - Returns the short name of the UOM.</li>
 *           <li>UOMDescr - Returns the long name of the UOM, pluralized.</li>
 *           <li>JSON - Returns the entire Data object as a JSON string.</li>
 *           <li>Pretty - The default. Returns the entire Data object as a JSON string, but formatted for readability.</li>
 *        </ul> 
 * </ul>
 * <p>
 * <b>What data paths are available?</b>
 * <p>
 * The paths that are available is determined by searching the following classes for methods that returns a "{@link com.SIMRacingApps.Data}" object
 * and either takes no arguments or all of the arguments are strings. See each class for available paths. 
 * <ul>
 *      <li>{@link com.SIMRacingApps.SIMPlugin}</li>
 *      <li>{@link com.SIMRacingApps.Session}</li>
 *      <li>{@link com.SIMRacingApps.Track}</li>
 *      <li>{@link com.SIMRacingApps.Car}</li>
 *      <li>{@link com.SIMRacingApps.Gauge}</li>
 *      <li>{@link com.SIMRacingApps.TeamSpeak}</li>
 * </ul>
 * @author Jeffrey Gilliam
 * @copyright Copyright (C) 2015 - 2020 Jeffrey Gilliam
 * @since 1.0
 * @license Apache License 2.0
 */
@WebServlet(description = "SIMRacingApps Data Access for JSON", urlPatterns = { "/Data", "/Data/*" }, loadOnStartup=1)
public class Data extends HttpServlet {
    private static final long serialVersionUID = 1L;

    String ip = "";
    
    /**
     * Default Constructor.
     * @see HttpServlet#HttpServlet()
     */
    public Data() {
        super();
    }

    /**
     * Initializes the servlet by getting the IP address and version information. 
     * It displays this information in the log and in the title bar of the server window.
     * The loadOnStartup option is enabled, so this gets called as soon as the server starts up.
     * <p>
     * @param config The Servlet's configuration information.
     * @throws ServletException Thrown so the server can log the error.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        Server.logger().info("init() called");
        super.init(config);
        DataService.start();
    }

    /**
     * Gets called when the container destroys this object
     */
    public void destroy() {
        Server.logger().info("distroy() called");
        DataService.stop();
    }

    private void parseParameters(HttpServletRequest request) {
        //Start a thread to start pumping data to the registered data events
        Map<String,String[]> params = request.getParameterMap();

        if (request.getParameter("stop") != null) {
            DataService.stop();
            return;
        }

        String startingVersion[] = params.get("startingversion");
        if (startingVersion != null && startingVersion.length > 0) {
            DataService.setStartingVersion(startingVersion[0]);
        }

        String endingVersion[] = params.get("endingversion");
        if (endingVersion != null && endingVersion.length > 0) {
            DataService.setEndingVersion(endingVersion[0]);
        }

        String play[] = params.get("play");
        if (play != null && play.length > 0 && !play[0].equals("")) {
            String playfile = play[0];
            DataService.setPlay(playfile);
        }

        String record[] = params.get("record");
        if (record != null && record.length > 0 && !record[0].equals("")) {

            String recordfile = record[0];
            DataService.setRecord(recordfile);
        }

//        startService();
    }

    @SuppressWarnings("deprecation")
    private Genson gensonPretty = new Genson.Builder().useIndentation(true).create(); //this is really slow.
    private Genson genson = new Genson();

    private void process_request(Map<String,Map<String,Map<String,Object>>> data, HttpServletRequest request, HttpServletResponse response) throws IOException {

    	parseParameters(request);

        //establish session in cache if needed
        String sessionid = request.getParameter("sessionid");
        if (sessionid == null)
            sessionid = "";

        //if we have data, call with the sessionid so it can register
        //else get the session by id and call the version of getJSON(Session) to retrieve it real-time instead of using the cache.
        StringBuffer s = DataService.getJSON(sessionid,data);

        //add these headers to try and prevent the various browsers from caching this data
        response.addHeader("Expires", "Sat, 01 Mar 2014 00:00:00 GMT");
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Cache-Control", "no-cache, must-revalidate");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(s);
        out.flush();
    }
    /**
     * The doGet method gets called when a HTTP GET request comes in.
     * If there are any elements after "/Data" in the URL, then they are used to lookup the data and return it.
     * If there are not any elements, then it looks for a "sessionid" to use to look up the data in the session cache.
     * <p>
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     * @param request The request information
     * @param response The response object.
     * @throws ServletException If there is a Servlet Exception
     * @throws IOException If there is an IO Exception 
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String lang = request.getParameter("lang");
        StringBuffer json = new StringBuffer("{");
        String data = request.getParameter("data");
        String path = data != null ? ("/"+data) : request.getPathInfo();

        //see if the user passed the parameters as REST and make the call to get the data now
        if (path != null) {
            com.SIMRacingApps.Data d = null;

            String s[] = path.split("[;]");
            for (int i=0; i < s.length; i++) {
                com.SIMRacingApps.Data d2 = null;
                try {
                     parseParameters(request);
    
                     if (s[i].equalsIgnoreCase("/IP")) {
                         d2 = new com.SIMRacingApps.Data("IP",ip,"",com.SIMRacingApps.Data.State.NORMAL);
                     }
                     else
                     if (s[i].toUpperCase().startsWith("/SETPLAY")) {
                         String[] play = s[i].split("[/]");
                         String playfile = play[play.length-1];
                         DataService.setPlay(playfile);
                         d2 = new com.SIMRacingApps.Data("/Play",playfile,"",com.SIMRacingApps.Data.State.NORMAL);
                     }
                     else
                     if (s[i].toUpperCase().startsWith("/SETRECORD")) {
                         String[] record = s[i].split("[/]");
                         if (record.length > 2) {
                             String recordfile = record[record.length-1];
                             DataService.setRecord(recordfile);
                         }
                         else
                             DataService.setRecord("");
                         
                         d2 = DataService.getRecord();
                     }
                     else {
                    	 d2 = DataService.getData(s[i]);
                     }
                     
                     if (d2 != null) {
                         if (d == null) {
                             d2.setName(s[i]);
                             d = d2;
                         }
                         else
                             d.add(d);
                     }
    
                } catch (SIMPluginException e) {
                    Server.logStackTrace(Level.FINE, "SIMPluginException",e);
                    d = new com.SIMRacingApps.Data(s[i],"SIMPluginException caught in /Data: " + e.getLocalizedMessage(),"",com.SIMRacingApps.Data.State.ERROR);
                }
            }
            
            //add these headers to try and prevent the various browsers from caching this data
            response.addHeader("Expires", "Sat, 01 Mar 2014 00:00:00 GMT");
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Cache-Control", "no-cache, must-revalidate");

            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();

            if (d != null) {
//                if (d.getState().equals("ERROR")) {
//                    out.print(d.getValue());
//                }
//                else {

                    if (request.getParameter("lang") != null)
                        d.setLang(request.getParameter("lang"));
                    if (request.getParameter("format") != null)
                        d.setFormat(request.getParameter("format"));
                    if (request.getParameter("uom") != null)
                        d = d.convertUOM(request.getParameter("uom"));
                    
                    String property;
                    if ((property = request.getParameter("output")) != null) {
                        if (property.equalsIgnoreCase("Value")) {
                            if (d.getString().startsWith("{") && d.getString().endsWith("}")) {
                                response.setContentType("application/json");
                                out.print(d.getString().replaceAll("[\\\\]([^\\x00-\\x7F])", "$1"));
                            }
                            else {
                                out.print(d.getValue());
                            }
                        }
                        else
                        if (property.equalsIgnoreCase("State"))
                            out.print(d.getState());
                        else
                        if (property.equalsIgnoreCase("StatePercent"))
                            out.print(d.getStatePercent());
                        else
                        if (property.equalsIgnoreCase("Lang"))
                            out.print(d.getLang());
                        else
                        if (property.equalsIgnoreCase("Uom"))
                            out.print(d.getUOM());
                        else
                        if (property.equalsIgnoreCase("UOMAbbr"))
                            out.print(d.getUOMAbbr());
                        else
                        if (property.equalsIgnoreCase("UOMDesc"))
                            out.print(d.getUOMDesc());
                        else
                        if (property.equalsIgnoreCase("json")) {
                            response.setContentType("application/json");
                            out.print(d.toString(d.getName()).replaceAll("[\\\\]([^\\x00-\\x7F])", "$1"));
                        }
                        else
                        if (property.equalsIgnoreCase("ValueFormatted") || property.equalsIgnoreCase("Formatted")) {
                            if (d.getString().startsWith("{") && d.getString().endsWith("}"))
                                response.setContentType("application/json");
                            out.print(d.getValueFormatted().replaceAll("[\\\\]([^\\x00-\\x7F])", "$1"));
                        }
                        else {
                           //default is pretty, if output is not recognized.
                           response.setContentType("application/json");
                           //to work around a bug in Genson where \x causes problems deserializing, strip them out
                           String s2 = d.toString(d.getName()).replaceAll("[\\\\]([^\\x00-\\x7F])", "?");
                           out.print(gensonPretty.serialize(genson.deserialize(s2,Map.class)).replaceAll("[\\\\]([^\\x00-\\x7F])", "$1"));
                        }
                    }
                    else {
//                        if (d.getString().startsWith("{") && d.getString().endsWith("}"))
//                            response.setContentType("application/json");
//                        out.print(d.getValueFormatted().replaceAll("[\\\\]([^\\x00-\\x7F])", "$1"));
                        response.setContentType("application/json");
                        //to work around a bug in Genson where \x causes problems deserializing, strip them out
                        String s2 = d.toString(d.getName()).replaceAll("[\\\\]([^\\x00-\\x7F])", "?");
                        out.print(gensonPretty.serialize(genson.deserialize(s2,Map.class)).replaceAll("[\\\\]([^\\x00-\\x7F])", "$1"));
                    }
//                }
            }
            else {
                out.print("");
            }

            out.flush();
            return;
        }
        else
        if (data != null) {
            String sessionid = request.getParameter("sessionid");
            if (sessionid == null)
                sessionid = "";

            DataService.deleteSession(sessionid);
            json.append("\"");
            json.append(data);
            json.append("\": {");

            String s[] = data.split("[;]");
            for (int i=0; i < s.length; i++) {
                if (i != 0) json.append(String.format("%n,"));
                json.append("\"");
                json.append(s[i]);
                json.append("\": { \"Name\": \"");
                json.append(s[i]);
                json.append("\"");

                if (lang != null && !lang.equals("")) {
                    json.append(", \"Lang\": \"");
                    json.append(lang);
                    json.append("\"");
                }
                json.append("}");
            }

            json.append("}");
        }
        json.append("}");

        try {
            @SuppressWarnings("unchecked")
            Map<String,Map<String,Map<String,Object>>> d = genson.deserialize(json.toString(), Map.class);

            try {
                process_request(d,request,response);
            }
            catch (Exception e) {
                Server.logStackTrace(Level.WARNING, "Exception",e);
            }
        }
        catch (Exception e) {
            Server.logStackTrace(Level.WARNING, "Exception",e);
        }

    }

    /**
     * The doPost method gets call when a HTTP POST message comes in.
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     * @param request The request information
     * @param response The response object.
     * @throws ServletException If there is a Servlet Exception
     * @throws IOException If there is an IO Exception 
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        @SuppressWarnings("unchecked")
        Map<String,Map<String,Map<String,Object>>> d = genson.deserialize(request.getReader(), Map.class);

        String sessionid = request.getParameter("sessionid");
        if (sessionid == null)
            sessionid = "";

        if (Server.isLogLevelFinest())
            Server.logger().finest(String.format("doPost(%s): Input = %s",sessionid,genson.serialize(d)));
        
        DataService.deleteSession(sessionid);
        
        process_request(d,request,response);

    }
}
