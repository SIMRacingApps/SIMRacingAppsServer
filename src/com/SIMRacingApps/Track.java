 package com.SIMRacingApps;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.owlike.genson.*;
import com.owlike.genson.stream.JsonStreamException;
import com.SIMRacingApps.SIMPlugin;
import com.SIMRacingApps.Data;
import com.SIMRacingApps.Util.FindFile;
import com.SIMRacingApps.Util.TimezoneMapper;

/**
 * This class contains information about a track.
 * A track map contains points in percentage that maps to the Latitude and Longitude of the location on the track.
 * This can be used by SIMs that do not output the actual Lat/Long.
 * The JSON files can be found in the @{link com.SIMRacingApps.Tracks} for the different configurations.
 * https://en.wikipedia.org/wiki/List_of_NASCAR_tracks
 * @author Jeffrey Gilliam
 * @since 1.0
 * @copyright Copyright (C) 2015 - 2022 Jeffrey Gilliam
 * @license Apache License 2.0
 *
 */
public class Track {

    /**
     * An enumeration of the track types returned by the SIM.
     */
    public final static class Type {
        public static final String UNKNOWN        = "unknown";
        public static final String LONG_OVAL      = "long oval";
        public static final String MEDIUM_OVAL    = "medium oval";
        public static final String SHORT_OVAL     = "short oval";
        public static final String ROAD_COURSE    = "road course";
        public static final String SUPER_SPEEDWAY = "super speedway";
        //TODO: Need to find all of the track types returned by iRacing
    }
    
    /**
     * An enumeration of the track category returned by the SIM.
     */
    
    public final static class Category {
        public static final String UNKNOWN        = "UNKNOWN";
        public static final String OVAL           = "Oval";
        public static final String ROAD           = "Road";
        //TODO: Need to find all of the track categories returned by iRacing
    }

    protected transient SIMPlugin SIMPlugin = null;
    private Genson genson = new Genson();
    private String m_name = "";
//    private double m_north;
    private Map<String,Object> m_trackmap = null;
    private Map<String,ArrayList<Map<String,Double>>> m_trackpaths = new HashMap<String,ArrayList<Map<String,Double>>>();
    private long m_lastLoadTime = 0L;
    private Locale m_us = new Locale("us"); //used to override formatting for key lookups

//    protected String m_distanceUOM, m_tempUOM, m_speedUOM;

    /**
     * Class constructor to use when you have a SIMPlugin
     * @param SIMPlugin An instance of a specific SIM SIMPlugin.
     */
    public Track(SIMPlugin SIMPlugin) {
        this.SIMPlugin = SIMPlugin;
    }

    /**
     * Reads the JSON file for this track.
     */
    @SuppressWarnings("unchecked")
    protected boolean _loadTrack() {
        if (SIMPlugin == null || System.currentTimeMillis() < (m_lastLoadTime + 3000L)) 
            return false;

        String trackname = getName().getString();
        
        if (trackname.isEmpty())
            trackname = "default";

        if (m_trackmap != null && trackname.equals(m_name)) //if the track hasn't changed, just return
            return false;

        m_name = "unknown";
//        m_distanceUOM = "UOM";
//        m_tempUOM = "UOM";
//        m_speedUOM = "UOM";
//        m_north = -90.0;
        m_trackmap = null;
        m_trackpaths = new HashMap<String,ArrayList<Map<String,Double>>>();
                
//        String description   = getDescription().getString();
//        String country       = getCountry().getString();
//        Data pitspeedlimit   = getPitSpeedLimit();
//
        InputStream is = null;

//this code was to convert the old YAML files to JSON. They have all been converted.
//        {
//            String trackpathyaml = "com/SIMRacingApps/Tracks/" + trackname.replace(' ', '_') + ".yaml";
//            is = SIMPlugin.class.getClassLoader().getResourceAsStream(trackpathyaml);
//            if (is != null) {
//                Yaml y = new Yaml();
//                Object ydata = y.load(is);
//                //convert to percentages
//                ArrayList<Map<String,Object>> a;
//                Map<String,Object> p;
//                a = ((Map<String,ArrayList<Map<String,Object>>>)ydata).get("OnTrack");
//                for (int i=0; i < a.size(); i++) {
//                    p = a.get(i);
//                    p.put("x",(Integer)p.get("x") / 640.0);
//                    p.put("y",(Integer)p.get("y") / 384.0);
//                }
//                a = ((Map<String,ArrayList<Map<String,Object>>>)ydata).get("OnPitRoad");
//                for (int i=0; i < a.size(); i++) {
//                    p = a.get(i);
//                    p.put("x",(Integer)p.get("x") / 640.0);
//                    p.put("y",(Integer)p.get("y") / 384.0);
//                }
//                try {
//                    String j = genson.serialize(ydata);
//                    System.err.printf("---ymal to json---%n%s%n---%n",j);
//                    is.close();
//                } catch (TransformationException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }

        String trackpath = "com/SIMRacingApps/Tracks/" + Server.getArg(trackname.replace(' ', '_'),trackname.replace(' ', '_')) + ".json";
        Server.logger().info(String.format("Loading Track Map %s", trackpath));
        
        try {
            is = new FindFile(trackpath).getInputStream();
        } catch (FileNotFoundException e) {
            String s = trackpath;
            trackpath = "com/SIMRacingApps/Tracks/default.json";
            Server.logger().info(String.format("cannot open %s, Loading default Track %s", s,trackpath));
            try {
                is = new FindFile(trackpath).getInputStream();
            } catch (FileNotFoundException e1) {}            
        }

        if (is != null) {
            InputStreamReader in = new InputStreamReader( is );
            try {
                if (in != null) {
                    m_trackmap = genson.deserialize(in, Map.class);
                    m_lastLoadTime = System.currentTimeMillis();
//                    m_distanceUOM = (String) m_trackmap.get("DistanceUOM");
//                    m_tempUOM = (String) m_trackmap.get("TempUOM");
//                    m_speedUOM = (String) m_trackmap.get("SpeedUOM");
//                    if (m_trackmap.get("North") != null)
//                        m_north = (Double)m_trackmap.get("North");
//                    SIMPlugin.getTrackType().getString();
                    m_name  = trackname;
                }
            } catch (JsonStreamException e) {
                Server.logStackTrace(Level.SEVERE,"JsonStreamException",e);
            } catch (JsonBindingException e) {
                Server.logStackTrace(Level.SEVERE,"JsonBindingException",e);
            }
            finally {
                try {
                    if (in != null) in.close();
                    if (is != null) is.close();
                    is = null;
                } catch (IOException e) {}
            }
        }

        if (m_trackmap != null && m_trackmap.containsKey("GPX")) {
            
            //now see if there is a GPX file and load it.
            Map<String,String> GPX = (Map<String,String>)m_trackmap.get("GPX");
            
            Iterator<Entry<String, String>> itr = GPX.entrySet().iterator();
            while (itr.hasNext()) {
                Entry<String, String> GPXEntry = itr.next();
                //String path = "com/SIMRacingApps/Tracks/" + trackname.replace(' ', '_') + "-" + GPXEntry.getKey() + ".gpx";
                String path = "com/SIMRacingApps/Tracks/" + GPXEntry.getValue();
                Server.logger().info(String.format("Loading GPX Lat/Lon from %s", path));
                try {
                    is = new FindFile(path).getInputStream();
                    class Rec {
                        public double lat,lon,distance,bearing,percent=0.0;
                        String name;
                        Rec(double lat,double lon,double distance,double bearing) {
                            this.lat      = lat;
                            this.lon      = lon;
                            this.distance = distance;
                            this.bearing  = bearing;
                        }
                    }
                    
                    ArrayList<Double> mergePoints = new ArrayList<Double>();
                    
                    try {
                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                        DocumentBuilder db = dbf.newDocumentBuilder();
                        
                        Document dom            = db.parse(is);
                        
                        Element docEle = dom.getDocumentElement();
                        //look for way points
                        NodeList nl = docEle.getElementsByTagName("wpt");
                        if(nl != null && nl.getLength() > 0) {
                            for(int i = 0 ; i < nl.getLength();i++) {

                                Element el = (Element)nl.item(i);
                                double lat = Double.parseDouble(el.getAttribute("lat"));
                                double lon = Double.parseDouble(el.getAttribute("lon"));
                                NodeList rtept_taglist = el.getElementsByTagName("name");
                                if (rtept_taglist != null) {
                                    String name = ((Element)rtept_taglist.item(0)).getTextContent();
                                    if (name.equalsIgnoreCase("Center")) {
                                        m_trackmap.put("Latitude", lat);
                                        m_trackmap.put("Longitude", lon);
                                        Server.logger().info(String.format(m_us,"Center = %f,%f",lat,lon));
                                    }
                                }
                            }
                        }
                        
                        nl = docEle.getElementsByTagName("rtept");
                        if(nl != null && nl.getLength() > 0) {
                            ArrayList<Rec> records  = new ArrayList<Rec>();
                            Rec prevRec = new Rec(0.0,0.0,0.0,270.0);
                            Map<String,Map<String,Double>> percentageMap = new HashMap<String,Map<String,Double>>(); 
                            
                            double totalDistance    = 0.0;
                            for(int i = 0 ; i < nl.getLength();i++) {

                                Element el = (Element)nl.item(i);
                                double lat = Double.parseDouble(el.getAttribute("lat"));
                                double lon = Double.parseDouble(el.getAttribute("lon"));
                                double distance = (i == 0 ? 0.0 : _distance(prevRec.lat,prevRec.lon,lat,lon));
                                records.add(prevRec = new Rec(lat,lon,distance,_bearing(prevRec.lat,prevRec.lon,lat,lon) + 270.0));
                                NodeList rtept_taglist = el.getElementsByTagName("name");
                                if (rtept_taglist != null) {
                                    prevRec.name = ((Element)rtept_taglist.item(0)).getTextContent();
                                }
                                
                                totalDistance += distance;
                            }
                        
                            double cumlativeDistance = 0.0;
                            double prevPercent = 0.0;
                            prevRec = new Rec(0.0,0.0,0.0,270.0);
                            
                            //most tracks are setup for 100% from start to finish
                            //nurburg tourist doesn't start and end at the same place
                            //it is 109.2% long
                            double maxPercent = m_trackmap.containsKey("MaxPercent") 
                                              ? (double)m_trackmap.get("MaxPercent") / 100.0
                                              : 1.0;
                            
                            for (int i=0; i < records.size(); i++) {
                                Rec rec = records.get(i);
                                cumlativeDistance += records.get(i).distance;
                                rec.percent = (cumlativeDistance / totalDistance) * maxPercent;
                                
                                while (i > 0 
                                &&     prevPercent + .001 < rec.percent
                                &&     !String.format(m_us,"%.1f", (prevPercent + .001) * 100.0).equals(String.format(m_us,"%.1f", rec.percent * 100.0))
                                ) {
                                    double percent = prevPercent + 0.001; //This is the next precent we need to write out.
                                    double percentDiff = (percent - prevRec.percent) / (rec.percent - prevRec.percent);
                                    
                                    double latDiff = (rec.lat - prevRec.lat) * percentDiff;
                                    double lonDiff = (rec.lon - prevRec.lon) * percentDiff;
                                    
                                    Map<String,Double> percentageEntry = new HashMap<String,Double>();
                                    percentageEntry.put("Latitude",  prevRec.lat + latDiff);
                                    percentageEntry.put("Longitude", prevRec.lon + lonDiff);
                                    percentageEntry.put("Bearing",rec.bearing);
                                    percentageMap.put(String.format(m_us,"%.1f", percent * 100.0), percentageEntry);
                                    
                                    String s = String.format(m_us,"%s, \"%.1f\": { \"Latitude\": %-16.12f, \"Longitude\": %-16.12f, \"Bearing\": %-16.12f }",
                                            GPXEntry.getKey(), 
                                            percent * 100.0,
                                            prevRec.lat + latDiff,
                                            prevRec.lon + lonDiff,
                                            rec.bearing);
                                    
                                    Server.logger().finer(s);
                                    prevPercent = percent;
                                }
                
                                if (i == 0 || !String.format(m_us,"%.1f", prevPercent * 100.0).equals(String.format(m_us,"%.1f", rec.percent * 100.0))) {
                                        
                                    Map<String,Double> percentageEntry = new HashMap<String,Double>();
                                    percentageEntry.put("Latitude",  rec.lat);
                                    percentageEntry.put("Longitude", rec.lon);
                                    percentageEntry.put("Bearing",rec.bearing + 270.0);
                                    percentageMap.put(String.format(m_us,"%.1f", rec.percent * 100.0), percentageEntry);
                                    
                                    if (rec.name.equalsIgnoreCase("MergePoint")) {
                                        //TODO: Do I Override the MergePoint or only add it if missing. ONTRACK or ONPITROAD?
                                        if (GPXEntry.getKey().equals("ONTRACK")) {
                                            mergePoints.add(rec.percent * 100.0);
                                            //m_trackmap.put("MergePoint", rec.percent * 100.0);
                                            Server.logger().info(String.format(m_us,"%s.MergePoint = %f",GPXEntry.getKey(),rec.percent * 100.0));
                                        }
                                    }
                                    
                                    String s = String.format(m_us,"%s, \"%.1f\": { \"Latitude\": %-16.12f, \"Longitude\": %-16.12f, \"Distance\": \"%-16.12fm / %-16.12fm\", \"Bearing\": %-16.12f, \"Name\": \"%s\" }",
                                               GPXEntry.getKey(),
                                               rec.percent * 100.0,
                                               rec.lat,
                                               rec.lon,
                                               rec.distance,
                                               totalDistance,
                                               rec.bearing,
                                               rec.name);
                                    
                                    Server.logger().finer(s);
                                    prevRec     = rec;
                                    prevPercent = rec.percent;
                                }
                            }
                            
                            if (!percentageMap.isEmpty())
                                m_trackmap.put(GPXEntry.getKey(),percentageMap);
                        }
                        
                        if (!mergePoints.isEmpty()) {
                            m_trackmap.put("MergePoint", mergePoints);
                        }
                    }
                    catch (IOException | ParserConfigurationException | SAXException e) {
                        Server.logStackTrace(Level.SEVERE,e);
                    }
                } catch (FileNotFoundException e1) {
                    Server.logger().warning(String.format("Track: GPX file, %s, not found",path));
                }
                finally {
                    try {
                        if (is != null) is.close();
                    } catch (IOException e) {}
                    is = null;
                }
            }
        }
        
        return true;
    }

    public double _maxPercentage() {
        return m_trackmap.containsKey("MaxPercent") 
             ? (double)m_trackmap.get("MaxPercent") / 100.0
             : 1.0;
    }
    
    //http://www.movable-type.co.uk/scripts/latlong.html
    private static double _distance(double lat1, double lon1, double lat2, double lon2) {
        //System.err.printf("lat1=%f,lon1=%f,lat2=%f,lon2=%f%n" , lat1,lon1,lat2,lon2);
        double R = 6371000.0; //meters around the earth
        double a = Math.sin(_deg2rad(lat2-lat1)/2) * Math.sin(_deg2rad(lat2-lat1)/2)
                 + Math.cos(_deg2rad(lat1))        * Math.cos(_deg2rad(lat2))
                 * Math.sin(_deg2rad(lon2-lon1)/2) * Math.sin(_deg2rad(lon2-lon1)/2)
                 ;
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0-a));
        double dist = R * c;
        return dist;
    }
    
    private static double _bearing(double lat1, double lon1, double lat2, double lon2) {
        double y = Math.sin(_deg2rad(lon2-lon1)) * Math.cos(_deg2rad(lat2));
        double x = Math.cos(_deg2rad(lat1)) * Math.sin(_deg2rad(lat2)) -
                   Math.sin(_deg2rad(lat1)) * Math.cos(_deg2rad(lat2)) * Math.cos(_deg2rad(lon2-lon1));
        double brng = Math.atan2(y,x);
        return _rad2deg(brng);
    }
    
    private static double _deg2rad(double deg) {
        return deg * (Math.PI / 180.0);
    }

    private static double _rad2deg(double rad) {
        return (rad / Math.PI) * 180.0;
    }
    
    /**
     * Forces a reload of the track's JSON files. 
     * Normally the JSON files are only read once and cached.
     */
    public void reload() { m_name="x"; _loadTrack(); }

    /**
     * Returns the data map of the track as a Map&lt;String,Object&gt;.
     * The OnTrack and OnPitRoad percentages will be from 0.0 to 100.0 with each one tenth of a percent filled in between. 
     * The format of the map is defined in JSON as:
     * <pre>
     * {
     *     "Latitude":    35.351669,        //center to rotate on
     *     "Longitude":   -80.682891,       //center to rotate on
     *     "Resolution":  1.9,              //meters per pixed @ 800x480
     *     "MergePoint":  38.5,             //percentage from finish line where you are safe to merge from pit road to the track
     *     "MaxPercentage": 100.0,          //Normalize the percentage for distance according to this
     *     "DistanceUOM": "mile",           //the unit of measure to show distance in
     *     "TempUOM":     "F",              //the unit of measure to show temperature in
     *     "SpeedUOM":    "mph",            //the unit of measure to show speed in
     *     "North":       166.0,            //degrees to rotate map so north points in that direction
     *     "FinishLine":  270.0,            //degrees to rotate the finish line, post rotation of the map. Initially zero degrees.
     *     "ONTRACK": {
     *         "0.0":   { "Latitude": 29.18888888,  "Longitude": -81.072 },
     *         "0.1":   { "Latitude": 29.18888888,  "Longitude": -81.072 },
     *         "0.2":   { "Latitude": 29.18888888,  "Longitude": -81.072 },
     *         "0.3":   { "Latitude": 29.18888888,  "Longitude": -81.072 },
     *         ...
     *         "100.0": { "Latitude": 29.18888888,  "Longitude": -81.072 }
     *     },
     *     "ONPITROAD": {
     *         "0.0":   { "Latitude": 29.18888888,  "Longitude": -81.072 },
     *         "0.1":   { "Latitude": 29.18888888,  "Longitude": -81.072 },
     *         "0.2":   { "Latitude": 29.18888888,  "Longitude": -81.072 },
     *         "0.3":   { "Latitude": 29.18888888,  "Longitude": -81.072 },
     *         ...
     *         "100.0": { "Latitude": 29.18888888,  "Longitude": -81.072 }
     *     }
     * }
     * </pre>
     * 
     * @return A Data object containing the JSON file contents of this track defined as a Map&lt;String,Object&gt; in a {@link com.SIMRacingApps.Data} container..
     */
    public Map<String,Object> getMap()                                 { /*Object*/                              _loadTrack();return m_trackmap; }

    /**
     * Returns the merging point as a percentage of where you are allowed to merge onto the track after when leaving the pits.
     * <p>
     * Some tracks, like Bristol, have multiple merge points because it has 2 pit lanes. 
     * So the relativeTo argument should be either your pit stall location or your current location if you are exiting the pits.
     * 
     * @param relativeTo The point, as a percentage, to use to find the next merge point after. Range 0.0 to 100.0.
     * @return The merge point.
     */
    public double _getMergePoint(double relativeTo) {
        _loadTrack();
        if (m_trackmap == null) return 0.0;
        Object mergePoint = m_trackmap.get("MergePoint");
        
        if (mergePoint != null) {
            if (mergePoint instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<Double> mergePoints = (ArrayList<Double>)mergePoint;
                for (int i=0; i < mergePoints.size(); i++) {
                    if (mergePoints.get(i) >= relativeTo)
                        return mergePoints.get(i);
                }
                return mergePoints.get(0);
            }
            return (double)mergePoint;
        }
        return 0.0;
    }
//    
//    /**
//     * Returns a Data object containing the location of the car on the track and in the image as percentages respectively.
//     * 
//     * <p>
//     * The primary name in the object will be Track/CarLocation. Secondarily, Track/CarLocationX and Track/CarLocationY are available.
//     * 
//     * <p>PATH = {@link #getCarLocation(String) /Track/CarLocation/(carIdentifier)}
//     * 
//     * @param carIdentifier - The car identifier name as defined by {@link com.SIMRacingApps.Session#getCar(String)}
//     * 
//     * @return The location of the car in a {@link com.SIMRacingApps.Data} container.
//     */
//    public Data getCarLocation(String carIdentifier) {
//        _loadTrack();
//
//        Double x = -1.0, y = -1.0;
//
//        //look this up in the json data
//        String status = SIMPlugin.getSession().getCar(carIdentifier).getStatus().getString();
//        double carPercentage = SIMPlugin.getSession().getCar(carIdentifier).getLap(iRacingCar.LapType.COMPLETEDPERCENT).getDouble() / 100.0;
//
//        //check to see if the car is out on the track
//        if (carPercentage >= 0.0
//        && !status.equals(Car.Status.INVALID)
//        && !status.equals(Car.Status.INGARAGE)
//        ) {
//            Boolean isOnPitRoad = !(status.equals(Car.Status.ONTRACK) || status.equals(Car.Status.OFFTRACK));
//            @SuppressWarnings("unchecked")
//            ArrayList<Map<String,Double>> tracklocations = (ArrayList<Map<String,Double>>) (isOnPitRoad ? m_trackmap.get("OnPitRoad") : m_trackmap.get("OnTrack"));
//            double prevPercentage = 1.00;
//            double prevX = -1.0;
//            double prevY = -1.0;
//
//            for (int i=0; i < tracklocations.size(); i++) {
//                Map<String,Double> location = tracklocations.get(i);
//                double markPercentage = location.get("percentage");
//                if (carPercentage >= markPercentage) {
//                    double diffPercentage = (carPercentage - markPercentage) / (prevPercentage - markPercentage);
//                    x = location.get("x") + (diffPercentage * (prevX - location.get("x")));
//                    y = location.get("y") + (diffPercentage * (prevY - location.get("y")));
//                    break;
//                }
//                prevPercentage = markPercentage;
//                prevX = location.get("x");
//                prevY = location.get("y");
//            }
//        }
//
//        Data d = new Data("Track/CarLocation",carPercentage,"%");
//        d.add("Track/CarLocationX",x * 100.0,"%");
//        d.add("Track/CarLocationY",y * 100.0,"%");
//        return d;
//    }
//    public Data getCarLocation() {
//        return getCarLocation("REFERENCE");
//    }

    /**
     * Returns the bearing of the requested position based on percentage traveled from the start/finish line.
     * Can return the coordinate based on 2 locations, ONTRACK and ONPITROAD.
     * You can get the bearing at the start/finish line by passing in ONTRACK with a percentage of zero.
     * <p>
     * Zero degrees is north, 90 is east, 180 is south and 270 is west.
     * <p>
     * If a SIM can return the exact bearing, then it should override this functions and return them.
     * Otherwise, the percentage is used to approximate the position.
     * 
     * <p>PATH = {@link #getBearing(String, Double, String) /Track/Bearing/(LOCATION)/(PERCENTAGE)/(UOM)}
     *
     * @param location (Optional), The location of where you want the percentage to apply to, ONTRACK, or ONPITROAD. 
     *        Defaults to ONTRACK.
     * @param percentage (Optional), The percentage traveled from the start/finish line. 
     *        Range 0.0 to 100.0. 
     *        Defaults to 0.0.
     * @param UOM (optional), The UOM to return the Latitude in. 
     *        Defaults to degrees.
     * 
     * @return The bearing.
     */
    public    Data    getBearing(String location, Double percentage, String UOM) { /*Double*/
        _loadTrack();
        Double bearing = 0.0; //default to true north
        if (!(percentage < 0.0) && m_trackmap != null) {
            @SuppressWarnings("unchecked")
            Map<String,Map<String,Double>> map = (Map<String, Map<String, Double>>) m_trackmap.get(location.toUpperCase());
            if (map != null) {
                Map<String,Double> latlng = map.get(String.format(m_us,"%.1f",percentage));
                if (latlng != null) {
                    bearing = latlng.get("Bearing");
                    if (bearing == null) {
                        //get the previous location behind us
                        Map<String,Double> prevlatlng = map.get(String.format(m_us,"%.1f",percentage - 0.1));
                        if (percentage < 0.1) {
                            latlng = map.get("100.0");
                            prevlatlng = map.get("99.9");
                        }
                        if (prevlatlng != null)
                            bearing = _bearing(prevlatlng.get("Latitude"),prevlatlng.get("Longitude"),latlng.get("Latitude"),latlng.get("Longitude"));
                    }
                }
                else
                    Server.logger().fine(String.format(m_us,"TrackMap(%s) missing %.1f",location,percentage));
            }
        }

        return new Data("Track/Bearing/"+location,bearing,"deg",Data.State.NORMAL).convertUOM(UOM); 
    }
    public    Data    getBearing(String location, String percentage, String UOM) { return getBearing(location,Double.parseDouble(percentage),UOM); }
    public    Data    getBearing(String location, String percentage) { return getBearing(location,Double.parseDouble(percentage),"deg"); }
    public    Data    getBearing(String location) { return getBearing(location,"0.0"); }
    public    Data    getBearing() { return getBearing("ONTRACK"); }

    /**
     * Returns the track category as defined by {@link com.SIMRacingApps.Track.Category}
     * 
     * <p>PATH = {@link #getCategory() /Track/Category}
     * 
     * @return The category in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getCategory()                      { /*String*/                              return new Data("Track/Category",Category.UNKNOWN,"",Data.State.NOTAVAILABLE); }

    /**
     * Returns the city of where the track is as reported by the SIM SIMPlugin.
     * 
     * <p>PATH = {@link #getCity() /Track/City}
     * 
     * @return The city in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getCity()                          { /*String*/                              return new Data("Track/City","{City}","",Data.State.NOTAVAILABLE); }
    
    /**
     * Returns the track configuration as reported by the SIM SIMPlugin.
     * 
     * <p>PATH = {@link #getConfiguration() /Track/Configuration}
     * 
     * @return The configuration in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getConfiguration()                 { /*String*/                              return new Data("Track/Configuration","{Configuration}","",Data.State.NOTAVAILABLE); }
    
    /**
     * Returns the country code of where the track is as reported by the SIM SIMPlugin.
     * 
     * <p>PATH = {@link #getCountry() /Track/Country}
     * 
     * @return The country code in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getCountry()                       { /*String*/                              return new Data("Track/Country","{Country}","",Data.State.NOTAVAILABLE); }
    
    /**
     * Returns the description of the track. 
     * This is a value that can be displayed to the user.
     * 
     * <p>PATH = {@link #getDescription() /Track/Description}
     * 
     * @return The description of the track in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getDescription()                   { /*String*/                              return new Data("Track/Description","{TrackDescription}","",Data.State.NOTAVAILABLE); }

    /**
     * Returns the degrees to rotate the finish line.
     * Defaults to zero degrees for no rotation.
     * Should be the angle you want to match the already rotated map based on North angle.
     * In other words, it does not rotate with the map.
     * 
     * <p>PATH = {@link #getFinishLineRotation(String) /Track/getFinishLineRotation/(UOM)}
     * 
     * @param UOM (Optional), The unit of measure to return, defaults to "deg".
     * 
     * @return The degrees to Rotate in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getFinishLineRotation(String UOM) { /*double*/
        _loadTrack();
        Double rotate = m_trackmap != null ? (Double)m_trackmap.get("FinishLine") : null;
        return new Data("Track/FinishLineRotation",rotate == null ? 0.0 : rotate,"deg",rotate == null ? Data.State.NOTAVAILABLE : Data.State.NORMAL).convertUOM(UOM); 
    }
    public    Data    getFinishLineRotation() { return getFinishLineRotation(""); }
    
    /**
     * Returns the path to the track image specified by the "type" of image. 
     * The image will be located on the classpath.
     * You can also use the this.class.getClassLoader().getResourceAsStream(imagepath) to have Java
     * find the file anywhere in the classpath.
     * <p>
     * The supported types are:
     * <ul>
     *    <li>SAT = Satellite</li>
     *    <li>MAp = A Map Drawing</li>
     * </ul>
     * <p>
     * For example: com/SIMRacingApps/Tracks/charlotte_sat.png
     * 
     * <p>PATH = {@link #getImage() /Track/Image/(type)}
     * 
     * @param type The type of image, "sat" or "map". Defaults to "sat".
     * @return The image path in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getImage(String type) { /*image*/
        _loadTrack();
        String image = null;
        @SuppressWarnings("unchecked")
        Map<String,String> m_images = m_trackmap != null ? (Map<String, String>) m_trackmap.get("Images") : null;
        if (m_images != null) {
            image = (String)m_images.get(type.toUpperCase());
        }
        return new Data("Track/Image",image == null || image.isEmpty() ? "" : "com/SIMRacingApps/Tracks/"+image,"",image == null ? Data.State.NOTAVAILABLE : Data.State.NORMAL); 
    }
    public    Data    getImage() { return getImage("sat"); }

    /**
     * Returns the Latitude of the requested position based on percentage traveled from the start/finish line.
     * Can return the coordinate based on 3 locations, CENTER, ONTRACK and ONPITROAD.
     * You can get the start/finish line by passing in ONTRACK with a percentage of 100.
     * <p>
     * If a SIM can return the exact coordinates, then it should override this functions and return them.
     * Otherwise, the percentage is used to approximate the position.
     * 
     * <p>PATH = {@link #getLatitude(String, Double, String) /Track/Latitude/(LOCATION)/(PERCENTAGE)/(UOM)}
     *
     * @param location (Optional), The location of where you want the percentage to apply to, CENTER, ONTRACK, or ONPITROAD. 
     *        Defaults to CENTER.
     * @param percentage (Optional), The percentage traveled from the start/finish line. 
     *        Range 0.0 to 100.0 or MaxPercentage. 
     *        Defaults to 0.0.
     * @param UOM (optional), The UOM to return the Latitude in. 
     *        Defaults to degrees.
     * 
     * @return The Latitude.
     */
    public    Data    getLatitude(String location, Double percentage, String UOM) { /*Double*/
        _loadTrack();
        Double lat = null;
        if (m_trackmap != null) {
            if (location.equalsIgnoreCase("CENTER")) {
                lat = (Double)m_trackmap.get("Latitude");
            }
            else {
                if (!(percentage < 0.0)) {
                    @SuppressWarnings("unchecked")
                    Map<String,Map<String,Double>> map = (Map<String, Map<String, Double>>) m_trackmap.get(location.toUpperCase());
                    if (map != null) {
                        Map<String,Double> latlng = map.get(String.format(m_us,"%.1f",percentage));
                        if (latlng != null)
                            lat = latlng.get("Latitude");
                        else
                            Server.logger().fine(String.format(m_us,"TrackMap(%s) missing %.1f",location,percentage));
                    }
                }
            }
        }

        return new Data("Track/Latitude",lat == null ? 0.0 : lat,"deg",lat == null ? Data.State.NOTAVAILABLE : Data.State.NORMAL).convertUOM(UOM); 
    }
    public    Data    getLatitude(String location, String percentage, String UOM) { return getLatitude(location,Double.parseDouble(percentage),UOM); }
    public    Data    getLatitude(String location, String percentage) { return getLatitude(location,Double.parseDouble(percentage),"deg"); }
    public    Data    getLatitude(String location) { return getLatitude(location,"0.0"); }
    public    Data    getLatitude() { return getLatitude("CENTER"); }

    /**
     * Returns the length of the track using the unit of measure the distance of this track is normally reported in.
     * This is defined in the JSON file.
     * If not defined in the JSON file, then it will default to "mile" in the United States and "km" everywhere else.
     * You can also pass in a UOM as an optional argument.
     * 
     * <p>PATH = {@link #getLength(String) /Track/Length/(UOM)}
     * 
     * @param UOM (Optional), The UOM to return the track length in. Default to track's UOM, which is based on the country it's in.
     * 
     * @return The length of the track in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getLength(String UOM) { /*double*/
        _loadTrack();
        return new Data("Track/Length",0.0,UOM,Data.State.NORMAL).convertUOM(UOM); 
    }
    public    Data    getLength()                        { 
        _loadTrack();
        String distanceUOM = m_trackmap != null ? (String)m_trackmap.get("DistanceUOM") : null;
        
        //if not defined in the JSON file, then make some assumptions
        if (distanceUOM == null || distanceUOM.equals("UOM") || distanceUOM.isEmpty()) {
            if (getCountry().getString().equalsIgnoreCase("US")
            ||  getCountry().getString().equalsIgnoreCase("USA")
            //TODO: Get list of countries that publish their tracks in miles.
            )
                distanceUOM = "mile";
            else
                distanceUOM = "km";
        }
        return getLength(distanceUOM); 
    }
    
    /**
     * Returns the Longitude of the requested position based on percentage traveled from the start/finish line.
     * Can return the coordinate based on 3 locations, CENTER, ONTRACK and ONPITROAD.
     * You can get the start/finish line by passing in ONTRACK with a percentage of 100.0.
     * <p>
     * If a SIM can return the exact coordinates, then it should override this functions and return them.
     * Otherwise, the percentage is used to approximate the position.
     * 
     * <p>PATH = {@link #getLongitude(String, Double, String) /Track/Longitude/(LOCATION)/(PERCENTAGE)/(UOM)}
     *
     * @param location (Optional), The location of where you want the percentage to apply to, CENTER, ONTRACK, or ONPITROAD. 
     *        Defaults to CENTER.
     * @param percentage (Optional), The percentage traveled from the start/finish line. 
     *        Range 0.0 to 100.0 or MaxPercentage. 
     *        Defaults to 0.0.
     * @param UOM (optional), The UOM to return the Longitude in. 
     *        Defaults to degrees.
     * 
     * @return The Longitude.
     */
    public    Data    getLongitude(String location, Double percentage, String UOM) { /*Double*/
        _loadTrack();
        Double lng = null;
        if (m_trackmap != null) {
            if (location.equalsIgnoreCase("CENTER")) {
                lng = (Double)m_trackmap.get("Longitude");
            }
            else {
                if (!(percentage < 0.0)) {
                    @SuppressWarnings("unchecked")
                    Map<String,Map<String,Double>> map = (Map<String, Map<String, Double>>) m_trackmap.get(location.toUpperCase());
                    if (map != null) {
                        Map<String,Double> latlng = map.get(String.format(m_us,"%.1f",percentage));
                        if (latlng != null)
                            lng = latlng.get("Longitude");
                        else
                            Server.logger().fine(String.format(m_us,"TrackMap(%s) missing %.1f",location,percentage));
                    }
                }
            }
        }
        
        return new Data("Track/Longitude",lng == null ? 0.0 : lng,"deg",lng == null ? Data.State.NOTAVAILABLE : Data.State.NORMAL).convertUOM(UOM); 
    }
    public    Data    getLongitude(String location, String percentage, String UOM) { return getLongitude(location,Double.parseDouble(percentage),UOM); }
    public    Data    getLongitude(String location, String percentage) { return getLongitude(location,Double.parseDouble(percentage),"deg"); }
    public    Data    getLongitude(String location) { return getLongitude(location,"0.0"); }
    public    Data    getLongitude() { return getLongitude("CENTER"); }
    

    /**
     * Returns the name of the track as reported by the SIM.
     * This should include a specific configuration.
     * Clients should make no assumptions about the format of this name. Use it to display only.
     * <p>
     * For example: atlanta_qualoval
     * 
     * <p>PATH = {@link #getName() /Track/Name}
     * 
     * @return The name of the track in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getName()                          { /*String*/                              return new Data("Track/Name","default","",Data.State.NOTAVAILABLE); }

    /**
     * Returns the degrees of where North points, where zero degrees is at 3 o'clock and rotates clockwise.
     * If North is not defined in the track.json file, then it defaults to 270deg, straight up at 12 o'clock.
     * 
     * <p>PATH = {@link #getNorth(String) /Track/North/(UOM)}
     * 
     * @param UOM (Optional), The unit of measure to return the direction of north in, defaults to "deg".
     * 
     * @return The degrees to North in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getNorth(String UOM) { /*double*/
        _loadTrack();
        Double north = 270.0;
        if (m_trackmap != null) {
            Object o = m_trackmap.get("North");
            
            if (o instanceof Double)
                north = (Double)o;
            else
            if (o instanceof Long)
                north = ((Long)o).doubleValue();
        }
        
        return new Data("Track/North",north,"deg",m_trackmap == null ? Data.State.NOTAVAILABLE : Data.State.NORMAL).convertUOM(UOM); 
    }
    public    Data    getNorth()                         { return getNorth(""); }
    
    /**
     * Returns the path of the track's configuration.
     * The format follows how SVG defines a path with a Start, MoveTo, End.
     * If needed, there could be more than one segment if it is not continuous.
     * <p>
     * The results are returned in the following format. Type values are: -1.0 move to, 0.0 line to. 
     * NOTE: I use to use the close Type, but not all Start/Finish lines are at the same place on all tracks.
     * <pre>
     * [
     *     { "Type": -1.0,  "Lat": 0.0, "Lon": 0.0 },
     *     { "Type":  0.0,  "Lat": 0.0, "Lon": 0.0 },
     * ]
     * </pre>
     * <p>PATH = {@link #getName() /Track/Path}
     * 
     * @param location (Optional) The location of the track to return the path to. Can be ONTRACK, ONPITROAD. Defaults to ONTRACK.
     * @param UOM (Optional) The unit of measure to return the path in, default to the track's UOM.
     * 
     * @return The path of the track in a {@link com.SIMRacingApps.Data} container as ArrayList&lt;Map&lt;String,Double&gt;&gt;.
     */
    public    Data    getPath(String location, String UOM) {

        _loadTrack();
        
        ArrayList<Map<String,Double>> path     = new ArrayList<Map<String,Double>>();
        
        if (m_trackmap != null) {
            //see if it's been cached and use that
            if (m_trackpaths.containsKey(location+"/"+UOM))
                path = m_trackpaths.get(location+"/"+UOM);
            else {
                ArrayList<Map<String,Double>> segments = new ArrayList<Map<String,Double>>();
                
                Map<String,Double> segment             = null;
                
                for (int i=1; i == 1 || segment != null ;i++) {
                    @SuppressWarnings("unchecked")
                    Map<String,Map<String,Double>> map = (Map<String, Map<String, Double>>) m_trackmap.get(location.toUpperCase());
                    if (map != null) {
                        segment = map.get(String.format(m_us,"Segment-%d",i));
                        if (segment != null) {
                            segments.add(segment);
                        }
                    }
                }
                
                //if no segments found and the location is for the track, default to 0.0 to 100.0
                if (segments.size() == 0 && location.toUpperCase().equals("ONTRACK")) {
                    segment = new HashMap<String,Double>();
                    segment.put("Start", 0.0);
                    segment.put("End",   this._maxPercentage() * 100.0);
                    segments.add(segment);
                }
                
                Iterator<Map<String, Double>> itr = segments.iterator();
                while (itr.hasNext()) {
                    segment = itr.next();
                    int count = 0;
                    
                    for (double d = segment.get("Start"); ; d += 0.1) {
                        Data lat = getLatitude(location,d,UOM);
                        Data lon = getLongitude(location,d,UOM);
                        
                        Map<String,Double> point = new HashMap<String,Double>();
                        
                        point.put("Lat", lat.getDouble());
                        point.put("Lon", lon.getDouble());
                        
                        if (count > 0) {
                            point.put("Type",  0.0);
                        }
                        else {
                            point.put("Type", -1.0);
                        }
                        path.add(point);
                        
                        //protect from infinite loop. There should never be more than 6000.
                        if (++count > 6000)
                            break;
                        
                        if (Math.round(d*10.0) == Math.round(segment.get("End")*10.0))
                            break;
                        
                        if (Math.round(d*10.0) == 6000.0)
                            d = 0.0 - 0.1;
                    }

//Removed as all tracks are not connected start and end. Example drag strips or Mount Washington.
//                    if (count > 0) {  //if has a start add an end
//                        Map<String,Double> point = new HashMap<String,Double>();
//                        
//                        point.put("Lat",  0.0);
//                        point.put("Lon",  0.0);
//                        point.put("Type", 1.0 );
//                        path.add(point);
//                    }
                }
                
                m_trackpaths.put(location+"/"+UOM, path);   //put it in the cache
            }
        }
        
        return new Data("Track/Path/"+location, path, UOM,m_trackmap == null ? Data.State.NOTAVAILABLE : Data.State.NORMAL);
    }
    public    Data    getPath(String location)           { return getPath(location,""); }
    public    Data    getPath()                          { return getPath("ONTRACK"); }
    
    /**
     * Returns the Pit Speed Limit for this session as reported by the SIM SIMPlugin.
     * 
     * <p>PATH = {@link #getPitSpeedLimit(String) /Track/PitSpeedLimit/(UOM)}
     * 
     * @param UOM (Optional) The unit of measure to return the speed limit in, default to the track's UOM.
     * 
     * @return The Pit Speed Limit
     */
    public    Data    getPitSpeedLimit(String UOM) { /*double*/
        _loadTrack();
        
        String speedUOM = m_trackmap != null ? (String)m_trackmap.get("SpeedUOM") : null;
        if (speedUOM == null || speedUOM.equals("UOM") || speedUOM.isEmpty()) {
            if (getCountry().getString().equalsIgnoreCase("US")
            ||  getCountry().getString().equalsIgnoreCase("USA")
            //TODO: Get list of countries that publish their speed in mph. UK?
            )
                speedUOM = "mph";
            else
                speedUOM = "kph";
        }
        return new Data("TrackPitSpeedLimit",0.0,speedUOM,m_trackmap == null ? Data.State.NOTAVAILABLE : Data.State.NORMAL).convertUOM(UOM); 
    }
    public    Data    getPitSpeedLimit()                 { return getPitSpeedLimit(""); }
    
    /**
     * Returns the resolution to be used with GPS mapping software for displaying the track on a map.
     * The resolution is the meters per pixel based on the standard map size of 800x480.
     * Defaults to 1.9 if the JSON file does not define it.
     * 
     * @param UOM (Optional) The unit of measure to return the resolution in, defaults to meters.
     * @return The Resolution in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getResolution(String UOM) {
        _loadTrack();
        Double resolution = m_trackmap != null ? (Double)m_trackmap.get("Resolution") : null;
        return (new Data("Track/Resolution",resolution == null ? 1.9 : resolution,"m",m_trackmap == null ? Data.State.NOTAVAILABLE : Data.State.NORMAL)).convertUOM(UOM);
    }
    public    Data    getResolution() { return getResolution(""); }

    /**
     * Returns the current temperature of the track.
     * The location of this temperature is SIM specific. 
     * Should not be used to understand where grip is best on the track.
     * In most cases you will only want to display it.
     * 
     * <p>PATH = {@link #getTemp(String) /Track/Temp/(UOM)}
     * 
     * @param UOM (Optional) The unit of measure to convert the temperature in, default to track's UOM.
     * 
     * @return The temperature in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getTemp(String UOM) { /*double*/
        _loadTrack(); 
        String tempUOM = m_trackmap != null ? (String)m_trackmap.get("TempUOM") : null;
        
        if (tempUOM == null || tempUOM.equals("UOM") || tempUOM.isEmpty()) {
            if (getCountry().getString().equalsIgnoreCase("US")
            ||  getCountry().getString().equalsIgnoreCase("USA")
            //TODO: Get list of countries that publish their tracks in F.
            )
                tempUOM = "F";
            else
                tempUOM = "C";
        }

        return new Data("Track/Temp",0.0,tempUOM,m_trackmap == null ? Data.State.NOTAVAILABLE : Data.State.NORMAL).convertUOM(UOM); 
    }
    public    Data    getTemp()                   { return getTemp(""); }
    
    /**
     * Returns the time zone of the track's actual location in long format (i.e. America/New_York).
     * The State is set to the short format (i.e. EST).
     * If no track is loaded, it returns the users current time zone.
     * 
     * @return The time zone of the track in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getTimeZone() {
        _loadTrack();
        
        TimeZone tz = TimeZone.getDefault();
        String timezone = tz.getID();
        String state = Data.State.NOTAVAILABLE;
        
        if (SIMPlugin.isConnected() && m_trackmap != null) {
            timezone = (String)m_trackmap.get("TimeZone"); //allow track profile to override mapper
            if (timezone == null || !timezone.isEmpty())
                timezone = TimezoneMapper.latLngToTimezoneString(this.getLatitude().getDouble(), this.getLongitude().getDouble());
    
            tz = TimeZone.getTimeZone(timezone);
            timezone = tz.getID();
            state = Data.State.NORMAL;
        }

        return new Data("Track/Timezone",timezone,"",state);
    }
    
    /**
     * Returns the track's type as defined by {@link com.SIMRacingApps.Track.Type}
     * 
     * <p>PATH = {@link #getType() /Track/Type}
     * 
     * @return The type in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getType()                          { /*TrackType*/                           return new Data("Track/Type","{TrackType}","",Data.State.NOTAVAILABLE); }

    /**
     * Returns the fog level as a percentage.
     * 
     * <p>PATH = {@link #getWeatherFogLevel() /Track/WeatherFogLevel}
     * 
     * @return The fog level in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getWeatherFogLevel()               { /*double*/                              return new Data("Track/WeatherFogLevel",0.0,"%",Data.State.NOTAVAILABLE); }

    /**
     * Returns the relative humidity as a percentage.
     * 
     * <p>PATH = {@link #getWeatherRelativeHumidity() /Track/WeatherRelativeHumidity}
     * 
     * @return The relative humidity in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getWeatherRelativeHumidity()       { /*double*/                              return new Data("TrackWeatherRelativeHumidity",0.0,"%",Data.State.NOTAVAILABLE); }

    /**
     * Returns the condition of the skies, like "Partly Cloudy".
     * 
     * <p>PATH = {@link #getWeatherSkies() /Track/WeatherSkies}
     * 
     * @return The sky condition in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getWeatherSkies()                  { /*String*/                              return new Data("Track/WeatherSkies","{Skies}",Data.State.NOTAVAILABLE); }

    /**
     * Returns the current ambient temperature at the track.
     * 
     * <p>PATH = {@link #getWeatherTemp(String) /Track/WeatherTemp/(UOM)}
     * 
     * @param UOM (Optional) The unit of measure to convert the temperature in, default to track's UOM.
     * 
     * @return The temperature in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getWeatherTemp(String UOM) { /*double*/
        Data temp = getTemp();  //To get the track's UOM.
        
        return new Data("Track/WeatherTemp",0.0,temp.getUOM(),Data.State.NOTAVAILABLE).convertUOM(UOM); 
    }
    public    Data    getWeatherTemp()                   { return getWeatherTemp(""); }
    
    /**
     * Returns the wind direction, where East is zero degrees at 3 o'clock.
     * If the UOM is "TEXT", then returns one of the following: N, NE, W, SW, S, SE, E, NE.
     * 
     * <p>PATH = {@link #getWeatherWindDirection(String) /Track/WeatherWindDirection/(UOM)}
     * 
     * @param UOM (Optional) The unit of measure to return the wind direction in, defaults to "deg".
     * 
     * @return The wind direction in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getWeatherWindDirection(String UOM){ /*Double*/
        return new Data("Track/WeatherWindDirection",
                        UOM.equalsIgnoreCase("TEXT") ? "E" : 0.0,
                        UOM.equalsIgnoreCase("TEXT") ? "" : "deg",m_trackmap == null ? Data.State.NOTAVAILABLE : Data.State.NORMAL
               ).convertUOM(UOM); 
    }
    public    Data    getWeatherWindDirection()          { return getWeatherWindDirection(""); }
    
    /**
     * Returns the wind speed.
     * 
     * <p>PATH = {@link #getWeatherWindSpeed(String) /Track/WeatherWindSpeed/(UOM)}
     * 
     * @param UOM (Optional) The unit of measure to return the wind speed in, defaults to track's UOM for Pit Road Speed.
     * 
     * @return The wind speed in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getWeatherWindSpeed(String UOM) { /*double*/
        Data speed = getPitSpeedLimit();    //to get the UOM
        return new Data("Track/WeatherWindSpeed",0.0,speed.getUOM(),m_trackmap == null ? Data.State.NOTAVAILABLE : Data.State.NORMAL).convertUOM(UOM); 
    }
    
    public    Data    getWeatherWindSpeed()              { return getWeatherWindSpeed(""); }

}
