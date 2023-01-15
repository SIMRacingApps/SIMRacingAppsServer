package com.SIMRacingApps.servlets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.Properties;
import java.util.TreeMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.SIMRacingApps.Server;
import com.SIMRacingApps.Util.FindFile;
import com.owlike.genson.Genson;
import com.owlike.genson.JsonBindingException;
import com.owlike.genson.stream.JsonStreamException;

/**
 * This class implements the "/listings" service of the HTTP protocol.
 * It returns a structure with the version information, a list of the Apps and Widgets, and a list of the documentation.
 * All URLs are relative to the Servlet's Context.
 * Apps and Widgets can have multiple configurations separated by semicolons for name, description, icon, args.
 * Args is the main driver for the multiple configurations. Each one must have a unique name to track and save it's state.
 * Here is a sample of the structure it returns:
 * <pre>
 * {
 *  "version": {
 *      "licenseUrl": "/SIMRacingApps/LICENSE.TXT",
 *      "minor": "0",
 *      "license": "Apache License, Version 2.0, January 2004",
 *      "copyright": "Copyright (C) 2015 - 2023 Jeffrey Gilliam",
 *      "major": "1",
 *      "noticeUrl": "/SIMRacingApps/NOTICE.TXT",
 *      "build": "BETA-2015.06.29",
 *      "copyrightUrl": "/SIMRacingApps/COPYRIGHT.TXT",
 *      "built-by": "Jeffrey Gilliam",
 *      "releasenotes": "/SIMRacingApps/documentation/SIMRacingApps_ReleaseNotes.txt",
 *      "userpath": "C:\Users\Jeff\SIMRacingApps",
 *      "translations": {
 *          "COPYRIGHT":    "Copyright",
 *          "LICENSE":      "License",
 *          "NOTICE":       "NOTICE",
 *          "RELEASENOTES": "Release Notes",
 *          "VERSION":      "Version",
 *          "favorites":    "Favorites",
 *          "apps":         "Apps",
 *          "widgets":      "Widgets",
 *          "documentation":"Documentation",
 *          "installbefore":"To install or update an App, Widget or Patch, choose a File to upload, then click 'Upload.' Files will be uploaded into your user directory",
 *          "installafter": "which is searched first by the server."
 *      }
 *  },
 *  "headers": [
 *      "apps",
 *      "widgets",
 *      "documentation"
 *  ],
 *  "apps": [
 *      {
 *          "name": "CrewChief",
 *          "description": "Crew Chief",
 *          "doc": "JavaScriptDoc/CrewChief.html",
 *          "width": "800",
 *          "height": "480",
 *          "url": "apps/CrewChief?",
 *          "icon": "apps/CrewChief/icon.png",
 *          "args": "PitCommandsSIMController=false;PitCommandsSIMController=true"
 *      },
 *      {
 *      "name": "Dash-Gauges-Spek",
 *      "description": "Dash-Gauges-Spek",
 *      "doc": "JavaScriptDoc/Dash-Gauges-Spek.html",
 *      "width": "1280",
 *      "height": "768",
 *      "url": "apps/Dash-Gauges-Spek?",
 *      "icon": "apps/Dash-Gauges-Spek/icon.png",
 *      "args": ""
 *      }
 *  ],
 *  "widgets": [
 *      {
 *          "name": "AnalogGauge/Spek/FuelLevel",
 *          "description": "Analog Gauge / Spek / Fuel Level",
 *          "doc": "JavaScriptDoc/sra-analog-gauge-spek-fuel-level.html",
 *          "width": "480",
 *          "height": "480",
 *          "url": "apps/WidgetLoader?widget=AnalogGauge/Spek/FuelLevel&amp;",
 *          "icon": "widgets/AnalogGauge/Spek/FuelLevel/icon.png",
 *          "args": ""
 *      },
 *      {
 *          "name": "AnalogGauge/Spek/FuelPressure",
 *          "description": "Analog Gauge / Spek / Fuel Pressure",
 *          "doc": "JavaScriptDoc/sra-analog-gauge-spek-fuel-pressure.html",
 *          "width": "480",
 *          "height": "480",
 *          "url": "apps/WidgetLoader?widget=AnalogGauge/Spek/FuelPressure&amp;",
 *          "icon": "widgets/AnalogGauge/Spek/FuelPressure/icon.png",
 *          "args": ""
 *      }
 *  ],
 *  "documentation": [
 *      {
 *          "name": "SIMRacingApps - Apps, Widgets, and JavaScript API Reference",
 *          "description": "SIMRacingApps - Apps, Widgets, and JavaScript API Reference",
 *          "doc": "JavaScriptDoc/index.html",
 *          "url": "JavaScriptDoc/index.html",
 *          "width": 1000,
 *          "height": 700,
 *          "icon": "documentation/javascriptdoc.png",
 *          "args": ""
 *      },
 *      {
 *          "name": "SIMRacingApps - Java API Reference",
 *          "description": "SIMRacingApps - Java API Reference",
 *          "doc": "JavaDoc/index.html",
 *          "url": "JavaDoc/index.html",
 *          "width": 1000,
 *          "height": 700,
 *          "icon": "documentation/javadoc.png",
 *          "args": ""
 *      }
 *  ]
 * }
 * </pre>
 */
@WebServlet(description = "Returns a list of SIMRacingApps Apps and Widgets and other links", urlPatterns = { "/listings" }, loadOnStartup=0)
public class listings extends HttpServlet {
    private static final long serialVersionUID = 1373152651405594830L;

    Properties m_version = new Properties();
    Genson m_genson = new Genson();
    
    public listings() {
    }

    public void init(ServletConfig config) throws ServletException {
        Server.logger().info("init() called, Logger.Level = "+(Server.logger().getLevel() == null ? "INFO" : Server.logger().getLevel().toString()));
        super.init(config);
        InputStream in;
        try {
            in = this.getClass().getClassLoader().getResourceAsStream("com/SIMRacingApps/version.properties");
            m_version.load(in);
            in.close();
        } catch (IOException e) {
            Server.logStackTrace(Level.WARNING, "while reading version.properties",e);
        }
    }

    public void distroy() throws ServletException {
        Server.logger().info("distroy() called");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Server.logger().info(String.format("doGet() called"));

        String lang = "";
        
        if (request.getParameter("lang") != null) {
            lang = request.getParameter("lang");
            Server.logger().info("lang = "+lang);
        }
        
        //add these headers to try and prevent the various browsers from caching this data
        response.addHeader("Expires", "Sat, 01 Mar 2014 00:00:00 GMT");
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Cache-Control", "no-cache, must-revalidate");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("application/json");

        StringBuffer JSON = new StringBuffer("{");
        
        if (m_version != null) {
            try {
                //reload the file in case it changes while the server is running.
                //in = new FileInputStream(this.getServletContext().getRealPath("") + "version.properties");
                InputStream in = this.getClass().getClassLoader().getResourceAsStream("com/SIMRacingApps/version.properties");
                m_version.load(in);
                in.close();
                JSON.append("\"version\": {");
                Iterator<Entry<Object, Object>> itr = m_version.entrySet().iterator();
                int count = 0;
                while (itr.hasNext()) {
                    Entry<Object,Object> entry = itr.next();
                    if (count++ > 0)
                        JSON.append(",");
                    JSON.append("\"");
                    JSON.append(entry.getKey());
                    JSON.append("\": \"");
                    JSON.append(entry.getValue());
                    JSON.append("\"");
                }
                if (count++ > 0)
                    JSON.append(",");
                JSON.append("\"userpath\": \"" + String.join(";", FindFile.getUserPath()).replace("\\", "/") + "\"");
                
                FindFile file = null;
                try {
                    ArrayList<String> files = new ArrayList<String>();
                    files.add("nls/menu-text-"+lang+".properties"); 
                    String[] langs = lang.split("[-]");
                    if (langs.length > 0 && !langs[0].equals(lang)) {
                        files.add("nls/menu-text-"+langs[0]+".properties");
                    }
                    files.add("nls/menu-text-en-us.properties");
                    files.add("nls/menu-text-en.properties");
                    
                    file = FindFile.find(files);

                    Properties translations = new Properties();
                    translations.load(file.getInputStream());
                    JSON.append(",\"translations\": {");
                    itr = translations.entrySet().iterator();
                    count = 0;
                    while (itr.hasNext()) {
                        Entry<Object,Object> entry = itr.next();
                        if (count++ > 0)
                            JSON.append(",");
                        JSON.append("\"");
                        JSON.append(entry.getKey());
                        JSON.append("\": \"");
                        JSON.append(entry.getValue());
                        JSON.append("\"");
                    }
                    JSON.append("}\n");
                    
                    JSON.append("}\n");
                }
                catch (FileNotFoundException e) {
                }
                finally {
                    if (file != null)
                        file.close();
                }

            } catch (IOException e) {
                Server.logStackTrace(Level.WARNING, "while reading version.properties",e);
            }
        }
        
        JSON.append(",\"headers\": [\"favorites\",\"apps\",\"widgets\",\"documentation\"]\n");
        
        PrintWriter out = response.getWriter();
        
        Map<String,Map<String,Object>> favoritesList = new TreeMap<String,Map<String,Object>>();
        _loadListFavorites("favorites",favoritesList);
        JSON.append(",\"favorites\": [\n");
        JSON.append(_getJSON("favorites",favoritesList,lang));
        JSON.append("]\n");
        
        Map<String,Map<String,Object>> appsList = new TreeMap<String,Map<String,Object>>();
        _loadListUser("apps",appsList);
        _loadList("apps",appsList);
        JSON.append(",\"apps\": [\n");
        JSON.append(_getJSON("apps",appsList,lang));
        JSON.append("]\n");

        Map<String,Map<String,Object>> widgetsList = new TreeMap<String,Map<String,Object>>();
        _loadListUser("widgets",widgetsList);
        _loadList("widgets",widgetsList);
        JSON.append(",\"widgets\": [\n");
        JSON.append(_getJSON("widgets",widgetsList,lang));
        JSON.append("]\n");
        
        Map<String,Map<String,Object>> docsList = new TreeMap<String,Map<String,Object>>();
        JSON.append(",\"documentation\": [\n");
        JSON.append("    { \"name\": \"SIMRacingApps - Apps, Widgets, and JavaScript API Reference\", \"description\": \"\", \"doc\": \"JavaScriptDoc/index.html\", \"url\": \"JavaScriptDoc/index.html\", \"width\": 1000, \"height\": 700, \"icon\": \"documentation/javascriptdoc.png\", \"args\": \"\" }");
        JSON.append("   ,{ \"name\": \"SIMRacingApps - Java API Reference\", \"description\": \"\", \"doc\": \"JavaDoc/index.html\", \"url\": \"JavaDoc/index.html\", \"width\": 1000, \"height\": 700, \"icon\": \"documentation/javadoc.png\", \"args\": \"\" },");
        _loadListUser("documentation",docsList);
        _loadList("documentation",docsList);
        JSON.append(_getJSON("documentation",docsList,lang));
        JSON.append("   ,{ \"name\": \"SIMRacingApps - Release Notes\", \"description\": \"\", \"doc\": \"documentation/SIMRacingApps_ReleaseNotes.txt\", \"url\": \"documentation/SIMRacingApps_ReleaseNotes.txt\", \"width\": 1000, \"height\": 700, \"icon\": \"SRA-Logo.png\", \"args\": \"\" }");
        JSON.append("   ,{ \"name\": \"SIMRacingApps - Wiki\", \"description\": \"\", \"doc\": \"http://wiki.SIMRacingApps.com\", \"url\": \"http://wiki.SIMRacingApps.com\", \"width\": 1000, \"height\": 700, \"icon\": \"SRA-Logo.png\", \"args\": \"\" }");
        JSON.append("   ,{ \"name\": \"Browse SIM Raw Data\", \"description\": \"\", \"doc\": \"Data/SIM\", \"url\": \"Data/SIM\", \"width\": 1000, \"height\": 700, \"icon\": \"SRA-Logo.png\", \"args\": \"\" }");
        JSON.append("   ,{ \"name\": \"Settings\", \"description\": \"\", \"doc\": \"settings.html\", \"url\": \"settings.html\", \"width\": 800, \"height\": 800, \"icon\": \"settings-icon.png\", \"args\": \"\" }");
        JSON.append("   ,{ \"name\": \"useroverrides.css\", \"description\": \"\", \"doc\": \"useroverrides.html\", \"url\": \"useroverrides.html\", \"width\": 800, \"height\": 800, \"icon\": \"settings-icon.png\", \"args\": \"\" }");
        JSON.append("   ,{ \"name\": \"View Server Log\", \"description\": \"\", \"doc\": \"logs/"+Server.getLog()+"\", \"url\": \"logs/"+Server.getLog()+"\", \"width\": 1000, \"height\": 700, \"icon\": \"SRA-Logo.png\", \"args\": \"\" }");
        JSON.append("]\n");

        JSON.append("}");
        out.print(JSON.toString());
        out.flush();
        return;
    }

    private void _loadList(String folder,Map<String,Map<String,Object>> list) {
        try {
            File jarFile = new File(URLDecoder.decode(getClass().getProtectionDomain().getCodeSource().getLocation().getPath(),StandardCharsets.UTF_8.name()));

            if(jarFile.isFile()) {  // Run with JAR file
                JarFile jar = new JarFile(jarFile);
                Server.logger().finest("Looking in folder in jar: " + jarFile.toString());
                Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
                while(entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (!name.equals(folder+"/") 
                    &&  name.startsWith(folder + "/")
                    &&  name.endsWith("/")
                    ) { //filter according to the path
                        String p = name+"listing.json";
                        InputStream is_p = this.getClass().getClassLoader().getResourceAsStream(p);
                        if (is_p != null) {
                            Server.logger().finer("listing file in jar: " + p);
                            try {
                                @SuppressWarnings("unchecked")
                                Map<String,Object> listing = (Map<String, Object>) m_genson.deserialize(is_p, Map.class);
                                list.putIfAbsent(name.substring(0,name.length()-1).replace(folder+"/", ""),listing);
                            }
                            catch (JsonStreamException | JsonBindingException e) {
                                Server.logStackTrace(Level.SEVERE, "listing file in jar: " + p, e);
                            }
                            finally {
                                is_p.close();
                            }
                        }
                    }
                }
                jar.close();
            }
            else {
                //not in a jar, look in the classpath
                URL url = this.getClass().getClassLoader().getResource(folder);
                Server.logger().finest("Looking in folder in classpath: " + URLDecoder.decode(url.getPath(),"UTF-8"));
                InputStream is = this.getClass().getClassLoader().getResourceAsStream(folder);
                
                if (is != null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8));
                    
                    String l;
                    if (br != null) {
                        l = br.readLine();
                        while (l != null) {
                            String p = folder+"/"+l+"/listing.json";
                            InputStream is_p = this.getClass().getClassLoader().getResourceAsStream(p);
                            if (is_p != null) {
                                Server.logger().finer("listing file in classpath: " + p);
                                try {
                                    @SuppressWarnings("unchecked")
                                    Map<String,Object> listing = (Map<String, Object>) m_genson.deserialize(is_p, Map.class);
                                    //not take the first folder listed off of the name and leave the rest.
                                    String fa[] = folder.split("/");
                                    list.putIfAbsent((folder+"/"+l).substring(fa[0].length()+1),listing);
                                }
                                catch (JsonStreamException | JsonBindingException e) {
                                    Server.logStackTrace(Level.SEVERE, "listing file in classpath: " + p, e);
                                }
                                finally {
                                    is_p.close();
                                }
                            }
                            url = this.getClass().getClassLoader().getResource(folder+"/"+l);
                            if (new File(URLDecoder.decode(url.getPath(),StandardCharsets.UTF_8.name())).isDirectory())
                                _loadList(folder+"/"+l,list);
                            l = br.readLine();
                        }
                    }                
                    is.close();
                }
            }
            
        }
        catch (IOException e) {}
    }

    private void _loadListUser(String folder,Map<String,Map<String,Object>> list) {
        try {
            for (int i=0; i < FindFile.getUserPath().length; i++) {
                //now check users folder for additional apps and widgets.
                File userPath = new File(FindFile.getUserPath()[i]+"\\"+folder);
                if (userPath.isDirectory()) {
                    Server.logger().finest("Looking in folder: " + userPath.getAbsolutePath());
                    for (File l : userPath.listFiles()) {
                        String fa[] = folder.split("/");
                        if (l.isDirectory()) {
                            try {
                                String p = userPath.getAbsolutePath()+"/"+l.getName()+"/listing.json";
                                InputStream is_p = new FileInputStream(new File(p));
                                if (is_p != null) {
                                    Server.logger().info("listing file in userPath: " + p);
                                    try {
                                        @SuppressWarnings("unchecked")
                                        Map<String,Object> listing = (Map<String, Object>) m_genson.deserialize(is_p, Map.class);
                                        //not take the first folder listed off of the name and leave the rest.
                                        list.putIfAbsent((folder+"/"+l.getName()).substring(fa[0].length()+1),listing);
                                    }
                                    catch (JsonStreamException | JsonBindingException e) {
                                        Server.logStackTrace(Level.SEVERE, "listing file in userPath: " + p, e);
                                    }
                                    finally {
                                        is_p.close();
                                    }
                                }
                                else {
                                    _loadListUser(folder+"/"+l.getName(),list);
                                }
                            }
                            catch (FileNotFoundException e) {
                                _loadListUser(folder+"/"+l.getName(),list);
                            }
                        }
                    }
                }            
            }
        }
        catch (IOException e) {
            Server.logStackTrace(e);
        }
    }

    private void _loadListFavorites(String folder,Map<String,Map<String,Object>> list) {
        try {
            for (int i=0; i < FindFile.getUserPath().length; i++) {
                //now check users folder for additional apps and widgets.
                File userPath = new File(FindFile.getUserPath()[i]+"\\"+folder);
                if (userPath.isDirectory()) {
                    Server.logger().finest("Looking in folder: " + userPath.getAbsolutePath());
                    for (File l : userPath.listFiles()) {
                        String fa[] = folder.split("/");
                        if (!l.isDirectory()) {
                            if (l.getName().toLowerCase().endsWith(".json")) {
                                String p = userPath.getAbsolutePath()+"/"+l.getName();
                                InputStream is_p = new FileInputStream(new File(p));
                                if (is_p != null) {
                                    Server.logger().info("listing file in userFavorites: " + p);
                                    try {
                                        @SuppressWarnings("unchecked")
                                        Map<String,Object> listing = (Map<String, Object>) m_genson.deserialize(is_p, Map.class);
                                        //not take the first folder listed off of the name and leave the rest.
                                        list.putIfAbsent((folder+"/"+l.getName()).substring(fa[0].length()+1),listing);
                                    }
                                    catch (JsonStreamException | JsonBindingException e) {
                                        Server.logStackTrace(Level.SEVERE, "listing file in userPath: " + p, e);
                                    }
                                    finally {
                                        is_p.close();
                                    }
                                }
                            }
                        }
                    }
                }            
            }
        }
        catch (IOException e) {
            Server.logStackTrace(e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private StringBuffer _getJSON(String header, Map<String,Map<String,Object>> list, String lang) {
        StringBuffer JSON = new StringBuffer();
        int count = 0;
        
        Iterator<Entry<String, Map<String, Object>>> itr = list.entrySet().iterator();
        while (itr.hasNext()) {
            Entry<String, Map<String, Object>> file = itr.next();
            String app = file.getKey();
            Map<String, Object> listing = file.getValue();
            
            if (!app.equals("Template")
            &&  listing.get("name") != null
            &&  listing.get("description") != null
            &&  listing.get("doc") != null
            &&  listing.get("url") != null
            &&  listing.get("icon") != null
            &&  listing.get("args") != null
            &&  listing.get("height") != null
            &&  listing.get("width") != null
            ) {
                Object o = listing.get("name");
                ArrayList<Object> names;
                if (o instanceof ArrayList) 
                    names = (ArrayList<Object>) o;
                else {
                    String[] a = ((String)o).split(";");
                    names = new ArrayList<Object>();
                    for (int i=0; i < a.length; i++)
                        names.add(a[i]);
                }

                o = listing.get("description");
                //if lang is passed in, see if the description is translated
                if (lang != null && lang.length() > 0) {
                    String[] locale = lang.split("[-]");
                    Object desc = listing.get("description-"+lang);
                    if (desc == null && locale.length > 0)
                        desc = listing.get("description-"+locale[0]);
                    if (desc != null)
                        o = desc;
                }
                
                ArrayList<Object> descriptions;
                if (o instanceof ArrayList) 
                    descriptions = (ArrayList<Object>) o;
                else {
                    String[] a = ((String)o).split(";");
                    descriptions = new ArrayList<Object>();
                    for (int i=0; i < a.length; i++)
                        descriptions.add(a[i]);
                }
                
                o = listing.get("doc");
                ArrayList<Object> docs;
                if (o instanceof ArrayList) 
                    docs = (ArrayList<Object>) o;
                else {
                    String[] a = ((String)o).split(";");
                    docs = new ArrayList<Object>();
                    for (int i=0; i < a.length; i++)
                        docs.add(a[i]);
                }
                
                o = listing.get("url");
                ArrayList<Object> urls;
                if (o instanceof ArrayList) 
                    urls = (ArrayList<Object>) o;
                else {
                    String[] a = ((String)o).split(";");
                    urls = new ArrayList<Object>();
                    for (int i=0; i < a.length; i++)
                        urls.add(a[i]);
                }
                
                o = listing.get("icon");
                ArrayList<Object> icons;
                if (o instanceof ArrayList) 
                    icons = (ArrayList<Object>) o;
                else {
                    String[] a = ((String)o).split(";");
                    icons = new ArrayList<Object>();
                    for (int i=0; i < a.length; i++)
                        icons.add(a[i]);
                }
                
                o = listing.get("args");
                ArrayList<Object> args;
                if (o instanceof ArrayList) 
                    args = (ArrayList<Object>) o;
                else {
                    String[] a = ((String)o).split(";");
                    args = new ArrayList<Object>();
                    for (int i=0; i < a.length; i++)
                        args.add(a[i]);
                }
                
                o = listing.get("height");
                ArrayList<Object> heights;
                if (o instanceof ArrayList) 
                    heights = (ArrayList<Object>) o;
                else {
                    if (o instanceof Long) {
                        heights = new ArrayList<Object>();
                        heights.add(o);
                    }
                    else {
                        String[] a = ((String)o).split(";");
                        heights = new ArrayList<Object>();
                        for (int i=0; i < a.length; i++)
                            heights.add(Long.parseLong(a[i]));
                    }
                }
                
                o = listing.get("width");
                ArrayList<Object> widths;
                if (o instanceof ArrayList) 
                    widths = (ArrayList<Object>) o;
                else {
                    if (o instanceof Long) {
                        widths = new ArrayList<Object>();
                        widths.add(o);
                    }
                    else {
                        String[] a = ((String)o).split(";");
                        widths = new ArrayList<Object>();
                        for (int i=0; i < a.length; i++)
                            widths.add(Long.parseLong(a[i]));
                    }
                }
                
                
                for (int i=0; i < names.size(); i++) {
                    if (count++ > 0)
                        JSON.append("   ,");
                    else 
                        JSON.append("    ");
                    JSON.append("{ \"name\": \"");
                    JSON.append((String)names.get(i));
                    JSON.append("\"");
                    JSON.append(", \"description\": \"");
                    JSON.append((String)(i < descriptions.size() ? descriptions.get(i) : descriptions.get(descriptions.size()-1)));
                    JSON.append("\"");
                    JSON.append(", \"doc\": \"");
                    JSON.append((String)(i < docs.size() ? docs.get(i) : docs.get(docs.size()-1)));
                    JSON.append("\"");
                    JSON.append(", \"width\": \"");
                    JSON.append(String.format("%d",(i < widths.size() ? widths.get(i) : widths.get(widths.size()-1))));
                    JSON.append("\"");
                    JSON.append(", \"height\": \"");
                    JSON.append(String.format("%d",(i < heights.size() ? heights.get(i) : heights.get(heights.size()-1))));
                    JSON.append("\"");
                    JSON.append(", \"url\": \"");
                    JSON.append((String)(i < urls.size() ? urls.get(i) : urls.get(urls.size()-1)));
                    JSON.append("\"");
                    JSON.append(", \"icon\": \"");
                    JSON.append((String)(i < icons.size() ? icons.get(i) : icons.get(icons.size()-1)));
                    JSON.append("\"");
                    JSON.append(", \"args\": \"");
                    JSON.append((String)(i < args.size() ? args.get(i) : args.get(args.size()-1)));
                    JSON.append("\"");
                    
                    JSON.append(" }\n");
                }
            }
        }
        return JSON;
    }
}
