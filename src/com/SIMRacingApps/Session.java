package com.SIMRacingApps;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.SIMRacingApps.SIMPlugin;
import com.SIMRacingApps.Data;
import com.SIMRacingApps.Data.State;

/**
 * The class defines access to session level data.
 * There can be different types of sessions, like Practice, Qualifying, Race.
 * The session types are defined by {@link com.SIMRacingApps.Session.Type}.
 * <p>
 * Each session contains a reference to the {@link com.SIMRacingApps.Track}. 
 * The track's instance can be obtained by calling {@link com.SIMRacingApps.Session#getTrack()}.
 * <p>
 * Each session contains references to all the cars, both in the session and not.
 * An instance to a can be obtained by calling {@link com.SIMRacingApps.Session#getCar(String)}, 
 * where the String defines which car to return. If omitted, it returns the Reference Car.
 * <p>
 * The session also controls what the reference car is. The default reference car is "ME", but clients can change it.
 * The Reference Car impacts how some lookups for the car are calculated, such as the relative lookups, 
 * which means relative to the Reference Car. 
 * NOTE: When it's changed, all connected clients will assume the new reference car.
 * @author Jeffrey Gilliam
 * @since 1.0
 * @copyright Copyright (C) 2015 - 2023 Jeffrey Gilliam
 * @license Apache License 2.0
 */
public class Session {

    /**
     * This class enumerates the values returned by {@link com.SIMRacingApps.Session#getType()}
     * <ul>
     * <li>UNKNOWN</li>
     * <li>OPEN_QUALIFY</li>
     * <li>LONE_QUALIFY</li>
     * <li>OFFLINE</li>
     * <li>OFFLINE_TESTING</li>
     * <li>PRACTICE</li>
     * <li>RACE</li>
     * </ul>
     */
    public final static class Type {
        public final static String UNKNOWN          = "UNKNOWN";
        public final static String OPEN_QUALIFY     = "OPEN QUALIFY";
        public final static String LONE_QUALIFY     = "LONE QUALIFY";
        public final static String OFFLINE          = "OFFLINE";
        public final static String OFFLINE_TESTING  = "OFFLINE TESTING";
        public final static String PRACTICE         = "PRACTICE";
        public final static String RACE             = "RACE";
    }

    /**
     * This class enumerates the values return by {@link com.SIMRacingApps.Session#getStatus()}
     * <ul>
     * <li>UNKNOWN</li>
     * <li>ENGINES_STARTED</li>
     * <li>GREEN</li>
     * <li>CAUTION</li>
     * <li>RED</li>
     * <li>FINISHED</li>
     * </ul>
     */
    public final static class Status {
        public final static String UNKNOWN          = "UNKNOWN";
        public final static String ENGINES_STARTED  = "ENGINES_STARTED"; 
        public final static String GREEN            = "GREEN"; 
        public final static String CAUTION          = "CAUTION";
        public final static String RED              = "RED";
        public final static String FINISHED         = "FINISHED"; 
    }
    
    /**
     * This class enumerates the values return by {@link com.SIMRacingApps.Session#getRestart()}
     * <ul>
     * <li>DOUBLEFILE</li>
     * <li>SINGLEFILE</li>
     * </ul>
     */
    public final static class RestartMethod {
        public final static String DOUBLEFILE       = "DOUBLEFILE";
        public final static String SINGLEFILE       = "SINGLEFILE"; 
    }
    
    /** number of laps to return if the session has unlimited laps */
    public static final int UNLIMITEDLAPS = 9999;

    private String m_referenceCar = "ME";
    private boolean m_hideApps = false;

    private SIMPlugin m_SIMPlugin;
    private Car defaultCar = null;
    private Track defaultTrack = null;

    protected String _getShortTimeZone(Date d, String longTimeZone) {
        TimeZone tz = TimeZone.getTimeZone(longTimeZone);
        String name = tz.getDisplayName(tz.inDaylightTime(d), TimeZone.SHORT);
        return name;
    }

    protected String _getTimeZoneOffset(Date d, String longTimeZone) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(longTimeZone));
        cal.setTime(d);
        int zone_offset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);
        String offset = String.format("%s%02d%02d",
                        zone_offset >= 0 ? "+" : "-",
                        Math.abs(zone_offset) / 1000 / 60 / 60,
                        (Math.abs(zone_offset) / 1000) % 60
                        );
        return offset;
    }
    
    public Session(SIMPlugin SIMPlugin) {
        m_SIMPlugin = SIMPlugin;
    }

    /**
     * Returns the string of keys strokes to send to the SIM to 
     * issue the command from the group.
     * 
     * Each SIM has to override this method and implement it.
     * @param group The group to get the commands from (i.e. ADMIN_COMMANDS, RADIO_COMMANDS)
     * @param command The command within the group to retrieve (i.e. BLACK, ADD)
     * @return The keys to send to the SIM using the syntax defined by {@link com.SIMRacingApps.Util.SendKeys}
     */
    public String getSendKeys(String group, String command) {
        return "";
    }
    
    /**
     * This class enumerates the various car identifiers.
     * Dynamic identifiers are the ones where additional information is appended,
     * such as, the position, the number, etc.
     * 
     * This is provided as a convenience to prevent type-os.
     * See {@link com.SIMRacingApps.Session#getCar(String)}
     */
    public final static class CarIdentifiers {
        /** The current driver */
        public static final String ME               = "ME";
        /** The current reference car. The reference car can be set to any other car identifier. See {@link com.SIMRacingApps.Session#setReferenceCar(String)} */
        public static final String REFERENCE        = "REFERENCE";
        /** The Pace or Safety Car */
        public static final String PACECAR          = "PACECAR";
        /** The Pit Stall Location of the REFERENCE CAR */
        public static final String PITSTALL         = "PITSTALL";
        /** The cat that is currently transmitting on the radio */
        public static final String TRANSMITTING     = "TRANSMITTING";
        /** The Leader of the race */
        public static final String LEADER           = "LEADER";
        /** The car with the fastest time in this session */
        public static final String BEST             = "BEST";
        /** the car with the fastest time of the previous lap. Cars more than 2 laps down are not considered */
        public static final String FASTEST          = "FASTEST";
        /** the car that is crashing. Only valid during replay */
        public static final String CRASHES          = "CRASHES";
        /** the car that is most exciting. Only valid during replay */
        public static final String EXCITING         = "EXCITING";
        
        //The following are a prefix and must be appended with more information on what to return.
        
        /** Id, followed by the internal id that the SIM uses */
        public static final String ID_PREFIX        = "I";
        /** The Leader of the class */
        public static final String LEADER_PREFIX    = "LEADER";
        /** Position, followed by the position starting at 1 */
        public static final String POSITION_PREFIX  = "P";
        /** Position of your class, followed by the position starting at 1 */
        public static final String POSITIONCLASS_PREFIX  = "PC";
        /** Relative, followed by the number of cars from the reference car. Can be positive or negative */
        public static final String RELATIVE_PREFIX  = "R";
        /** Relative in the reference car's class, followed by the number of cars from the reference car. Can be positive or negative */
        public static final String RELATIVE_CLASS_PREFIX  = "RC";
        /** Relative by location (OnTrack or OnPitRoad), followed by the number of cars from the reference car. Can be positive or negative */
        public static final String RELATIVE_LOCATION_REPFIX = "RL";
        /** Relative by location (OnTrack or OnPitRoad) for the reference car's class, followed by the number of cars from the reference car. Can be positive or negative */
        public static final String RELATIVE_LOCATION_CLASS_REPFIX = "RLC";
        /** Relative by Position, followed by the number of positions from the reference car. Can be positive or negative */
        public static final String RELATIVE_POSITION_PREFIX = "RP";
        /** Relative by Position in Class, followed by the number of positions in the same class as the reference car. Can be positive or negative */
        public static final String RELATIVE_POSITION_CLASS_PREFIX = "RPC";
        /** The car using the following number */
        public static final String CARNUMBER_PREFIX  = "";
    }
    
    /**
     * Returns the name of the current camera.
     * 
     * <p>PATH = {@link #getCamera() /Session/Camera}
     * 
     * @since 1.3
     * @return The name of the current camera in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getCamera() {
        return new Data("Session/Camera","",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Returns the name of what the camera is focused on.
     * 
     * <p>PATH = {@link #getCameraFocus() /Session/CameraFocus}
     * 
     * @since 1.3
     * @return The name of what the current camera is focused on in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getCameraFocus() {
        return new Data("Session/CameraFocus","","",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Returns an array of the camera names for this session.
     * The camera names are SIM specific and it is not recommended that you assume what 
     * the names will be.
     * 
     * <p>PATH = {@link #getCameras() /Session/Cameras}
     * 
     * @since 1.3
     * @return The camera names array in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getCameras() {
        ArrayList<String> a = new ArrayList<String>();
        return new Data("Session/Cameras",a,"String",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Returns an instance of a Car object as defined by the "car" argument. 
     * SIM implementors should override this method and return a SIM specific car object.
     * @param carIndentifier A string representing the car you want. Values accepted are:
     * <dl>
     * <dt>ME</dt><dd>My Car/Driver information</dd>
     * <dt>REFERENCE, R0, RL0, RP0 RPC0</dt><dd>Defaults to ME, but can be set by the user to any car</dd>
     * <dt>PACE, PACECAR, SAFETY, SAFETYCAR, P0, PC0</dt><dd>The pace car</dd>
     * <dt>TRANSMITTING</dt><dd>The car that is currently talking in SIM or TeamSpeak</dd>
     * <dt>LEADER, P1</dt><dd>The leader</dd>
     * <dt>LEADERCLASS, PC1</dt><dd>The leader of your class</dd>
     * <dt>LEADERxxx</dt><dd>The leader if the same class as xxx</dd>
     * <dt>BEST</dt><dd>The car with the best lap for the entire session</dd>
     * <dt>FASTEST</dt><dd>The car with the fastest last lap</dd>
     * <dt>Ixx</dt><dd> The Car Id xx. Used internally to retrieve cars by Id.</dd>
     * <dt>xxx, Nxxx</dt><dd>The car with this car number</dd>
     * <dt>Pxx</dt><dd>The Car/Driver at position xx. Note: Position 0 should be the pace car in sessions that have a pace car</dd>
     * <dt>PCxx</dt><dd>The Car/Driver at position xx in your class. Note: Position 0 should be the pace car in sessions that have a pace car</dd>
     * <dt>R-/+xx</dt><dd>The Car/Driver relative to REFERENCE by location in the session. Example: R-1 driver behind, R+1 driver ahead</dd>
     * <dt>RC-/+xx</dt><dd>The Car/Driver relative to REFERENCE by location in the session for the same class as the reference car. Example: RC-1 driver behind in class, RC+1 driver ahead in class</dd>
     * <dt>RL-/+xx</dt><dd>The Car/Driver relative to REFERENCE by location segregated by On Pit Road and On Track. Example: RL-1 driver behind, RL+1 driver ahead</dd>
     * <dt>RLC-/+xx</dt><dd>The Car/Driver relative to REFERENCE by location in the same class as the reference car segregated by On Pit Road and On Track. Example: RLC-1 driver behind in class, RLC+1 driver ahead in class</dd>
     * <dt>RP-/+xx</dt><dd>The Car/Driver relative to REFERENCE by position. Example: RP-1 driver behind, RP+1 driver ahead</dd>
     * <dt>RPCxx</dt><dd>The Car/Driver at position xx by relative position by class. Example: RPC-1 driver behind, RPC+1 driver ahead in same class</dd>
     * </dl>
     *            
     * @return An instance of {@link com.SIMRacingApps.Car}
     */
    public    Car     getCar(String carIndentifier)     { /*Car*/                                 return defaultCar != null ? defaultCar : (defaultCar = new Car(m_SIMPlugin)); }
    
    /**
     * Returns the actual number of cars registered for this session.
     * It does not count the pace car or ghost cars.
     * 
     * <p>PATH = {@link #getCars() /Session/Cars}
     * 
     * @return The number of cars in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getCars()                        { /*int*/                                 return new Data("Session/Cars",0,"",Data.State.NOTAVAILABLE); }

    /**
     * Returns the number of caution laps.
     * 
     * <p>PATH = {@link #getCautionLaps() /Session/CautionLaps}
     * 
     * @return The number of caution laps in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getCautionLaps()                 { /*int*/                                 return new Data("Session/CautionLaps",0,"lap",Data.State.NOTAVAILABLE); }
    
    /**
     * Returns the number of cautions as seen by the leader.
     * 
     * <p>PATH = {@link #getCautions() /Session/Cautions}
     * 
     * @return The number of cautions in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getCautions()                    { /*int*/                                 return new Data("Session/Cautions",0,"integer",Data.State.NOTAVAILABLE); }

    /**
     * Returns an array of the class names sorted fastest to slowest.
     * 
     * <p>PATH = {@link #getClassNames() /Session/ClassNames}
     * 
     * @return The class names array in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getClassNames() {
        ArrayList<String> a = new ArrayList<String>();
        return new Data("/Session/ClassNames",a,"String",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Returns a SIM specific string indicating the version of the data just returned.
     * Do not make any assumptions of how this is formatted. Assume it's just a printable string.
     * 
     * <p>PATH = {@link #getDataVersion() /Session/DataVersion}
     * 
     * @return The version in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getDataVersion()                 { /*Long*/                                return new Data("Session/DataVersion",System.currentTimeMillis(),"",Data.State.NOTAVAILABLE); }

    /**
     * Returns the difference between 2 cars as either laps or seconds (if on the same lap).
     * If the 2 cars are the same car, then returns the last lap time or the total session time if the checkered flag is out.
     * <p>
     * This sign of the result will be car2 is behind car1, 
     * where car2 is the REFERENCE car and car1 is the LEADER, 
     * it will return seconds/laps behind as a negative number.
     * 
     * <p>PATH = {@link #getDiffCars(String, String) /Session/DiffCars/(CARIDENTIFIER1)/(CARIDENTIFIER2)}
     * 
     * @param carIdentifier1 Car as defined by {@link com.SIMRacingApps.Session#getCar(String)}, default LEADERxxx.
     * @param carIdentifier2 Car as defined by {@link com.SIMRacingApps.Session#getCar(String)}, default REFERENCE.
     * 
     * @return The difference in seconds or laps in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getDiffCars(String carIdentifier1, String carIdentifier2) {
        if (carIdentifier1 == null || carIdentifier2 == null)
            return new Data("Session/DiffCars/null",0.0,"s",Data.State.NOTAVAILABLE);
        
        Data seconds        = new Data("Session/DiffCars/"+carIdentifier1+"/"+carIdentifier2,0.0,"s",Data.State.NOTAVAILABLE);
        String cartoproject = carIdentifier2;
        Car _car2           = getCar(carIdentifier2);
        Car _car1           = getCar(carIdentifier1);

        if (_car2 == null
        ||  _car1 == null
        ||  (_car2.getId().getInteger() == -1 && !carIdentifier2.equalsIgnoreCase("PITSTALL"))
        ||  (_car1.getId().getInteger() == -1 && !carIdentifier1.equalsIgnoreCase("PITSTALL"))
        )
            return new Data("Session/DiffCars"+carIdentifier1+"/"+carIdentifier2,0.0,"s",Data.State.NOTAVAILABLE);

        if (carIdentifier1.equalsIgnoreCase("PITSTALL")) {
            if (!_car1.getPitLocation().getState().equals(Data.State.NORMAL))
                return new Data("Session/DiffCars"+carIdentifier1+"/"+carIdentifier2,0.0,"s",Data.State.NOTAVAILABLE);
            else
                _car1 = _car1;
        }
        
        if (carIdentifier2.equalsIgnoreCase("PITSTALL")) {
            if (!_car2.getPitLocation().getState().equals(Data.State.NORMAL))
                return new Data("Session/DiffCars"+carIdentifier1+"/"+carIdentifier2,0.0,"s",Data.State.NOTAVAILABLE);
            else
                _car2 = _car2;
        }
        
        Data sessionlap = m_SIMPlugin.getSession().getLap();

        if ((m_SIMPlugin.getSession().getType().getString().equalsIgnoreCase("RACE") && sessionlap.getInteger() > 1)
        ||   carIdentifier2.equalsIgnoreCase("PITSTALL")
        ||   carIdentifier1.equalsIgnoreCase("PITSTALL")
        ) {
            Data lap2;
            Data lap1;
            Data pct2 = _car2.getLap(Car.LapType.COMPLETEDPERCENT);
            Data pct1 = _car1.getLap(Car.LapType.COMPLETEDPERCENT);
            double car1Speed = _car1._getGauge(Gauge.Type.SPEEDOMETER).getValueCurrent("mph").getDouble();
            double car2Speed = _car2._getGauge(Gauge.Type.SPEEDOMETER).getValueCurrent("mph").getDouble();

            if (m_SIMPlugin.getSession().getIsCheckeredFlag().getBoolean()) {
                pct1.setValue(0.0);
                pct2.setValue(0.0);
            }

            //keep the pitstall ahead of the car being diff'ed
            if (carIdentifier1.equalsIgnoreCase("PITSTALL")) {
                if (pct2.getDouble() > pct1.getDouble()) {
                    lap2 = new Data("CarLap",0,"lap");
                    lap1 = new Data("CarLap",1,"lap");
                }
                else {
                    lap2 = new Data("CarLap",0,"lap");
                    lap1 = new Data("CarLap",0,"lap");
                }
            }
            else
            if (carIdentifier2.equalsIgnoreCase("PITSTALL")) {
                if (pct1.getDouble() > pct2.getDouble()) {
                    lap2 = new Data("CarLap",1,"lap");
                    lap1 = new Data("CarLap",0,"lap");
                }
                else {
                    lap2 = new Data("CarLap",0,"lap");
                    lap1 = new Data("CarLap",0,"lap");
                }
                cartoproject = carIdentifier1;
            }
            else {
                lap2 = _car2.getLap(Car.LapType.COMPLETED);
                lap1 = _car1.getLap(Car.LapType.COMPLETED);
                
                //use the fastest car 
                if (car1Speed > car2Speed)
                    cartoproject = carIdentifier1;
            }

            double distanceBetween = (lap2.getDouble() + (pct2.getDouble()/100.0)) - (lap1.getDouble() + (pct1.getDouble()/100.0));
            double s = distanceBetween * m_SIMPlugin.getSession().getCar(cartoproject).getLapTimeProjected().getDouble();

            //if refcar is the car, then just show the last time or the total session time if the race has finished.
            if (_car2.getIsEqual(carIdentifier1).getBoolean()) {
                if (m_SIMPlugin.getSession().getIsCheckeredFlag().getBoolean()) {
                    //the time is based on when the reference car starts the race.
                    seconds.setValue(_car2.getLapTime(Car.LapType.FINISHLINE).getDouble() - _car2.getLapTime(Car.LapType.RACESTART).getDouble());
                }
                else {
                    seconds.setValue(_car2.getLapTime(Car.LapType.SESSIONLAST).getDouble());
                }
            }
            else
            if (distanceBetween >= 1.0) {
                seconds.setValue((int)Math.floor(distanceBetween));
                seconds.setUOM("lap");
            }
            else
            if (distanceBetween <= -1.0) {
                seconds.setValue((int)Math.ceil(distanceBetween));
                seconds.setUOM("lap");
            }
            else {
                if (m_SIMPlugin.getSession().getIsCheckeredFlag().getBoolean()) {
                    //the time is base on when the reference car starts the race
                    double car1time = _car1.getLapTime(Car.LapType.FINISHLINE).getDouble() - _car1.getLapTime(Car.LapType.RACESTART).getDouble();
                    double car2time = _car2.getLapTime(Car.LapType.FINISHLINE).getDouble() - _car1.getLapTime(Car.LapType.RACESTART).getDouble();
                    seconds.setValue(car1time - car2time);
                }
                else
                    seconds.setValue(s);
            }
        }
        else
        if (m_SIMPlugin.getSession().getType().getString().equalsIgnoreCase("RACE") && sessionlap.getInteger() <= 1) {
            Data qualtime1 = _car1.getLapTime(Car.LapType.QUALIFYING);
            Data qualtime2 = _car2.getLapTime(Car.LapType.QUALIFYING);
            if (_car1.getIsEqual(carIdentifier2).getBoolean())
                seconds.setValue(qualtime2.getDouble());
            else
            if (qualtime2.getDouble() > 0.0)
                seconds.setValue(qualtime1.getDouble() - qualtime2.getDouble());
            else {
                seconds.setValue("");
                seconds.setUOM("");
            }
        }
        else {
            Data best1 = _car1.getLapTime(Car.LapType.SESSIONBEST);
            Data best2 = _car2.getLapTime(Car.LapType.SESSIONBEST);
            if (_car1.getIsEqual(carIdentifier2).getBoolean())
                seconds.setValue(best2.getDouble());
            else
                seconds.setValue(best1.getDouble() - best2.getDouble());
        }

        seconds.setState(State.NORMAL);
        return seconds;
    }
    public    Data    getDiffCars(String carIdentifier)        { /*double(seconds),int(lap)*/            return getDiffCars("LEADER"+carIdentifier,carIdentifier); }
    public    Data    getDiffCars()                            { /*double(seconds),int(lap)*/            return getDiffCars("LEADERREFERENCE","REFERENCE"); }
    
    /**
     * Calculates the difference between the 2 cars based on their physical distance from each other.
     * This differs from getDiffCars() by the fact it uses the way that car1 and car2 are named to determine position.
     * 
     * <p>PATH = {@link #getDiffCarsRelative(String, String) /Session/DiffCarsRelative/(CARIDENTIFIER1)/(CARIDENTIFIER2)}
     * 
     * @param carIdentifier1 Car as defined by {@link com.SIMRacingApps.Session#getCar(String)}
     * @param carIdentifier2 Car as defined by {@link com.SIMRacingApps.Session#getCar(String)}
     * 
     * @return The difference in seconds in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getDiffCarsRelative(String carIdentifier1, String carIdentifier2) {
        Data seconds = new Data("Session/DiffCarsRelative/"+carIdentifier1+"/"+carIdentifier2,0.0,"s");
        Car _car2         = getCar(carIdentifier2);
        Car _car1         = getCar(carIdentifier1);
//boolean logit = false;        

        if ((_car2.getId().getInteger() == -1 && !carIdentifier2.equalsIgnoreCase("PITSTALL"))
        ||  (_car1.getId().getInteger() == -1 && !carIdentifier1.equalsIgnoreCase("PITSTALL"))
        )
            return new Data("Session/DiffCarsRelative/"+carIdentifier1+"/"+carIdentifier2,"","",Data.State.NOTAVAILABLE);

        Data pct2 = _car2.getLap(Car.LapType.COMPLETEDPERCENT);
        Data pct1 = _car1.getLap(Car.LapType.COMPLETEDPERCENT);

        try {
            int position = 0;
            int refposition = 0;

            if (getCar("REFERENCE").getId().getInteger() == _car2.getId().getInteger() || carIdentifier2.startsWith("LEADER")) {
//            if (car.equalsIgnoreCase("ME") || car.equalsIgnoreCase("REFERENCE")) {
                position = 0;
            }
            else
            if (carIdentifier2.startsWith("RPC") || carIdentifier2.startsWith("RLC")) {
                position = Integer.parseInt(carIdentifier2.substring(3));
            }
            else
            if (carIdentifier2.startsWith("RL") || carIdentifier2.startsWith("RP") || carIdentifier2.startsWith("RC") || carIdentifier2.startsWith("PC")) {
                position = Integer.parseInt(carIdentifier2.substring(2));
            }
            else
            if (carIdentifier2.startsWith("R") || carIdentifier2.startsWith("P") || carIdentifier2.startsWith("I")) {
                position = Integer.parseInt(carIdentifier2.substring(1));
            }

            if (getCar("REFERENCE").getId().getInteger() == _car1.getId().getInteger() || carIdentifier1.startsWith("LEADER")) {
//            if (refcar.equalsIgnoreCase("ME") || refcar.equalsIgnoreCase("REFERENCE")) {
                refposition = 0;
//if (position == 1) {  
//    logit = true;
//}
            }
            else
            if (carIdentifier1.startsWith("RPC") || carIdentifier1.startsWith("RLC")) {
                refposition = Integer.parseInt(carIdentifier1.substring(3));
            }
            else
            if (carIdentifier1.startsWith("RL") || carIdentifier1.startsWith("RP") || carIdentifier1.startsWith("RC") || carIdentifier1.startsWith("PC")) {
                refposition = Integer.parseInt(carIdentifier1.substring(2));
            }
            else
            if (carIdentifier1.startsWith("R") || carIdentifier1.startsWith("P") || carIdentifier1.startsWith("I")) {
                refposition = Integer.parseInt(carIdentifier1.substring(1));
            }

            Data carspeed; //if car is ahead of reference, the use reference speed. If behind, use car's speed.
            double distanceBetween = 0.0;
            if (position < refposition) {
                if (pct2.getDouble() > pct1.getDouble())
                    distanceBetween = (1.0 - (((pct2.getDouble()/100.0)) - ((pct1.getDouble()/100.0)))) * -1.0;
                else
                    distanceBetween = (((pct1.getDouble()/100.0)) - ((pct2.getDouble()/100.0))) * -1.0;
                carspeed = _car2._getGauge(Gauge.Type.SPEEDOMETER).getValueCurrent("mph");
                if (Math.floor(carspeed.getDouble()) <= 0.0)
                    carspeed = _car1._getGauge(Gauge.Type.SPEEDOMETER).getValueCurrent("mph");
            }
            else {
                if (pct2.getDouble() < pct1.getDouble())
                    distanceBetween = (1.0 - (((pct1.getDouble()/100.0)) - ((pct2.getDouble()/100.0))));
                else
                    distanceBetween = (((pct2.getDouble()/100.0)) - ((pct1.getDouble()/100.0)));
                carspeed = _car1._getGauge(Gauge.Type.SPEEDOMETER).getValueCurrent("mph");
                if (Math.floor(carspeed.getDouble()) <= 0.0)
                    carspeed = _car2._getGauge(Gauge.Type.SPEEDOMETER).getValueCurrent("mph");
            }

            //use the car's speed if you can get it. The unit of measure for speed and track length need to match.
//            if (Math.floor(carspeed.getDouble()) > 0.0) {
//                double tracklength = m_SIMPlugin.getSession().getTrack().getLength("mile").getDouble();
//                seconds.setValue((((tracklength * distanceBetween) / carspeed.getDouble()) * 60 * 60));
//            }
//            else 
            {
                double projected = _car1.getLapTimeProjected().getDouble();
                //if car1 is not moving, see if car2 is.
                if (projected <= 0.0) {
                    projected = _car2.getLapTimeProjected().getDouble();
                }
                double s = distanceBetween * projected;
                seconds.setValue(s);
            }
            seconds.setState(State.NORMAL);
        }
        catch (NumberFormatException e) {}

        return seconds;
    }
    public    Data    getDiffCarsRelative(String carIdentifier)        { /*double(seconds),int(lap)*/            return getDiffCarsRelative("LEADER"+carIdentifier,carIdentifier); }
    public    Data    getDiffCarsRelative()                            { /*double(seconds),int(lap)*/            return getDiffCarsRelative("LEADERREFERENCE","REFERENCE"); }
    
    /**
     * Returns a SIM specific ID for the current session.
     * 
     * <p>PATH = {@link #getId() /Session/Id} 1.2
     *
     * @since 1.2
     * @return The Id in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getId()                                          { /*string*/                                 return new Data("Session/Id","","",Data.State.NOTAVAILABLE); } 
    
    /**
     * Returns the total number of incidents allowed for this session.
     * If the session does not specify them, such as, practice, then 9999 is returned.
     * 
     * Different SIMs may measure these in different units of measure. The default is "x".
     * 
     * <p>PATH = {@link #getIncidentLimit() /Session/IncidentLimit}
     *
     * @return The incident limit in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIncidentLimit()                              { /*int*/                                  return new Data("Session/IncidentLimit",0,"x",Data.State.NOTAVAILABLE); } 

    /**
     * Returns true if the client should hide the App windows.
     * 
     * <p>PATH = {@link #getIsAppsHidden() /Session/IsAppsHidden}
     * 
     * @return true if the Apps should be hidden {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIsAppsHidden()                { /*Boolean*/                             return new Data("Session/IsAppsHidden",m_hideApps,"boolean",Data.State.NORMAL); }
    
    /**
     * Returns true if the garage screen is visible so apps/widgets can hide it.
     * 
     * <p>PATH = {@link #getIsGarageVisible() /Session/IsGarageVisible}
     * 
     * @return true if the garage screen is visible in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIsGarageVisible()                 { /*Boolean*/                             return new Data("Session/IsGarageVisible",false,"boolean",Data.State.NOTAVAILABLE); }

    /**
     * Returns true if the green flag is waving.
     * 
     * <p>PATH = {@link #getIsGreenFlag() /Session/IsGreenFlag}
     * 
     * @return true if the green flag is out in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIsGreenFlag()                 { /*Boolean*/                             return new Data("Session/IsGreenFlag",false,"boolean",Data.State.NOTAVAILABLE); }

    /**
     * Returns true if the caution flag is waving.
     * 
     * <p>PATH = {@link #getIsCautionFlag() /Session/IsCautionFlag}
     * 
     * @return true if the caution flag is out in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIsCautionFlag()               { /*Boolean*/                             return new Data("Session/IsCautionFlag",false,"boolean",Data.State.NOTAVAILABLE); }

    /**
     * Returns true if the checkered flag is waving.
     * 
     * <p>PATH = {@link #getIsCheckeredFlag() /Session/IsCheckeredFlag}
     * 
     * @return true if the checkered flag is out in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIsCheckeredFlag()             { /*Boolean*/                             return new Data("Session/IsCheckeredFlag",false,"boolean",Data.State.NOTAVAILABLE); }

    /**
     * Returns true if the crossed flag is waving. This generally means the leader has passed the halfway point.
     * 
     * <p>PATH = {@link #getIsCrossedFlag() /Session/IsCrossedFlag}
     * 
     * @return true if the crossed flag is out in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIsCrossedFlag()               { /*Boolean*/                             return new Data("Session/IsCrossedFlag",false,"boolean",Data.State.NOTAVAILABLE); }

    /**
     * Returns true if the white flag is waving.
     * 
     * <p>PATH = {@link #getIsWhiteFlag() /Session/IsWhiteFlag}
     * 
     * @return true if the white flag is out in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIsWhiteFlag()                 { /*Boolean*/                             return new Data("Session/IsWhiteFlag",false,"boolean",Data.State.NOTAVAILABLE); }

    /**
     * Returns true if the red flag is waving.
     * 
     * <p>PATH = {@link #getIsRedFlag() /Session/IsRedFlag}
     * 
     * @return true if the green flag is out in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIsRedFlag()                   { /*Boolean*/                             return new Data("Session/IsRedFlag",false,"boolean",Data.State.NOTAVAILABLE); }
    
    /**
     * Returns true if pit road is open.
     * 
     * <p>PATH = {@link #getIsPitRoadOpen() /Session/IsPitRoadOpen}
     * 
     * @return true if the pits are open in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIsPitRoadOpen()               { /*Boolean*/                             return new Data("Session/IsPitRoadOpen",true,"boolean",Data.State.NOTAVAILABLE); }

    /**
     * Returns true if the session is in replay mode.
     * Otherwise, false which assumes all data is live (real-time).
     * 
     * <p>PATH = {@link #getIsReplay() /Session/IsReplay}
     * 
     * @return true if the green flag is out in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIsReplay()                    { /*Boolean*/                             return new Data("Session/IsReplay",false,"boolean",Data.State.NOTAVAILABLE); }
    
    /**
     * Returns true if the session is in replay mode and has not been rewound to a time in the past.
     * Otherwise, false which assumes the replay is not live and is running in the past.
     * 
     * NOTE: If getIsReplay() returns false, then the status of this is set to OFF and the value returned is false. So check the status to be NORMAL if in replay mode.
     * 
     * <p>PATH = {@link #getIsReplayLive() /Session/IsReplayLive}
     * 
     * @return true if the green flag is out in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIsReplayLive()                    { /*Boolean*/                         return new Data("Session/IsReplayLive",false,"boolean",Data.State.NOTAVAILABLE); }

    /**
     * Returns the current lap of the race or REFERENCE car if in a different type of session.
     * <p>
     * Current lap is defined as the lap you are about to complete. 
     * Meaning that, if you're on lap 1, you just took the green flag.
     * 
     * <p>PATH = {@link #getLap() /Session/Lap}
     * 
     * @return The current lap in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getLap()                         { /*int*/                                 return new Data("Session/Lap",0,"lap",Data.State.NOTAVAILABLE); }
    
    /**
     * Returns the total number of laps in the specified session.
     * If the session does not specify them, such as, practice, then 9999 is returned or estimated based on time remaining.
     * Sometimes, sessions are timed and it is not defined.
     * In this case, the laps will be estimated based on the leaders average lap time.
     * If the specified session does not exist, then the State will be set to OFF and zero returned.
     * 
     * <p>PATH = {@link #getLaps() /Session/Laps/(SESSIONTYPE)}
     *
     * @param sessionType The session type, {@link com.SIMRacingApps.Session.Type}, to get the laps from. Defaults to current session.
     * @return The number of laps in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getLaps(String sessionType)      { /*int*/                                 return new Data("Session/Laps",0,"lap",Data.State.NOTAVAILABLE); }
    public    Data    getLaps()                        { /*int*/                                 return getLaps(""); } 

    /**
     * Returns the number of laps to go in sessions that define a total number of laps
     * See {@link com.SIMRacingApps.Session#getLaps()}
     * 
     * <p>PATH = {@link #getLapsToGo() /Session/LapsToGo}
     * 
     * @return The number of laps to go in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getLapsToGo()                    { /*int*/                                 return new Data("Session/LapsToGo",0,"lap",Data.State.NOTAVAILABLE); }

    /**
     * Returns a SIM specific League ID for the current session.
     * 
     * <p>PATH = {@link #getLeagueId() /Session/LeagueId} 1.2
     *
     * @since 1.2
     * @return The League Id in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getLeagueId()                     { /*string*/                                 return new Data("Session/LeagueId","","",Data.State.NOTAVAILABLE); } 

    /**
     * This class enumerates the various messages that a SIM can raise that applies to all cars.
     * See {@link com.SIMRacingApps.Session#getMessages()}
     */
    public final static class Message {
        public static final String DISCONNECTED     = "DISCONNECTED";
        public static final String DEBRIS           = "DEBRIS";
        public static final String ONELAPTOGREEN    = "ONELAPTOGREEN";
        public static final String GREENHELD        = "GREENHELD";
        public static final String TENTOGO          = "TENTOGO";
        public static final String FIVETOGO         = "FIVETOGO";
        public static final String TWOTOGO          = "TWOTOGO";
        public static final String STARTREADY       = "STARTREADY";
        public static final String STARTSET         = "STARTSET";
        public static final String STARTGO          = "STARTGO";
    }
    
    /**
     * Returns a string of a semicolon separate list of messages that are currently active.
     * It is possible to have multiple messages.
     * Each message will have a semicolon before and after it, even at the begin and end of the string.
     *
     * Possible message values are determined by {@link com.SIMRacingApps.Session.Message}.
     * You can display as is, or use it as an index to the the translation for a particular language.
     * <p>
     * For example: ";ONELAPTOGREEN;"
     * 
     * <p>PATH = {@link #getMessages() /Session/Messages}
     * 
     * @return A list of messages in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getMessages()                       { /*String*/                              return new Data("Session/Messages",";DISCONNECTED;","",Data.State.NORMAL); }
    
    /**
     * Returns the name of the session. This name is SIM dependent and should not be used for decision making, only display.
     * 
     * <p>PATH = {@link #getName(String) /Session/Name/(session)}
     * 
     * @param session (optional) The session identifier. Defaults to current session.
     * @return A list of messages in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getName(String session)            { /*String*/                              return new Data("Session/Name","UNKNOWN","",Data.State.NORMAL); }
    public    Data    getName()                          { /*String*/                              return getName(""); }

    /**
     * Returns the number of car classes.
     * 
     * See {@link com.SIMRacingApps.Session#getNumberOfCarClasses()}
     * 
     * <p>PATH = {@link #getNumberOfCarClasses() /Session/NumberOfCarClasses}
     * 
     * @return The number of laps to go in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getNumberOfCarClasses()          { /*rgb*/                                 return new Data("Session/NumberOfCarClasses",1,"",Data.State.NORMAL); }

    /**
     * Returns the number of radio channels that is scannable by you.
     * 
     * <p>PATH = {@link #getRadioChannels() /Session/RadioChannels}
     * 
     * @return The number of radio channels available in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getRadioChannels()                         { /*int*/         return new Data("Session/RadioChannels",0,"",Data.State.NOTAVAILABLE); }

    /**
     * Returns the active radio channel.
     * 
     * <p>PATH = {@link #getRadioChannelActive() /Session/RadioChannelActive}
     * 
     * @return The active channel in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getRadioChannelActive()                    { /*int*/         return new Data("Session/RadioChannelActive",-1,"",Data.State.NOTAVAILABLE); }
    
    /**
     * Returns if the radio channel can be deleted.
     * 
     * <p>PATH = {@link #getRadioChannelIsDeleteable(int) /Session/RadioChannelIsDeletable/(CHANNELNUMBER)}
     * 
     * @param channel (Optional) The channel number. Defaults to the active channel.
     * @return Y if deleteable, N if not in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getRadioChannelIsDeleteable(int channel)    { /*boolean*/     return new Data("Session/RadioChannelIsDeletable",false,"boolean",Data.State.NOTAVAILABLE); }
    public    Data    getRadioChannelIsDeleteable(String channel) { /*boolean*/     return getRadioChannelIsDeleteable(Integer.parseInt(channel)); }
    public    Data    getRadioChannelIsDeleteable()               { /*boolean*/     return getRadioChannelIsDeleteable(getRadioChannelActive().getInteger()); }

    /**
     * Returns if the radio channel can only be listened to.
     * N means that you can also transmit on this channel.
     * 
     * <p>PATH = {@link #getRadioChannelIsListenOnly(int) /Session/RadioChannelIsListenOnly/(CHANNELNUMBER)}
     * 
     * @param channel (Optional) The channel number. Defaults to the active channel.
     * @return Y if listen only, N if you can also transmit in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getRadioChannelIsListenOnly(int channel)   { /*boolean*/     return new Data("Session/RadioChannelIsListenOnly",true,"boolean",Data.State.NOTAVAILABLE); }
    public    Data    getRadioChannelIsListenOnly(String channel){ /*boolean*/     return getRadioChannelIsListenOnly(Integer.parseInt(channel)); }
    public    Data    getRadioChannelIsListenOnly()              { /*boolean*/     return getRadioChannelIsListenOnly(getRadioChannelActive().getInteger()); }

    /**
     * Returns if the radio channel can be muted.
     * 
     * <p>PATH = {@link #getRadioChannelIsMutable(int) /Session/RadioChannelIsMutable/(CHANNELNUMBER)}
     * 
     * @param channel (Optional) The channel number. Default to the active channel.
     * @return Y if muteable, N if not in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getRadioChannelIsMutable(int channel)      { /*boolean*/     return new Data("Session/RadioChannelIsMutable",false,"boolean",Data.State.NOTAVAILABLE); }
    public    Data    getRadioChannelIsMutable(String channel)   { /*boolean*/     return getRadioChannelIsMutable(Integer.parseInt(channel)); }
    public    Data    getRadioChannelIsMutable()                 { /*boolean*/     return getRadioChannelIsMutable(getRadioChannelActive().getInteger()); }

    /**
     * Returns if the radio channel is currently muted.
     * 
     * <p>PATH = {@link #getRadioChannelIsMuted(int) /Session/RadioChannelIsMuted/(CHANNELNUMBER)}
     * 
     * @param channel (Optional) The channel number. Defaults to the active channel.
     * @return Y if muted, N if not in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getRadioChannelIsMuted(int channel)        { /*boolean*/     return new Data("Session/RadioChannelIsMuted",false,"boolean",Data.State.NOTAVAILABLE); }
    public    Data    getRadioChannelIsMuted(String channel)     { /*boolean*/     return getRadioChannelIsMuted(Integer.parseInt(channel)); }
    public    Data    getRadioChannelIsMuted()                   { /*boolean*/     return getRadioChannelIsMuted(getRadioChannelActive().getInteger()); }

    /**
     * Returns if the radio channel can be scanned.
     * 
     * <p>PATH = {@link #getRadioChannelIsScanable(int) /Session/RadioChannelIsScanable/(CHANNELNUMBER)}
     * 
     * @param channel (Optional) The channel number. Defaults to the active channel.
     * @return Y if scanable, N if not in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getRadioChannelIsScanable(int channel)     { /*boolean*/     return new Data("Session/RadioChannelIsScanable",true,"boolean",Data.State.NOTAVAILABLE); }
    public    Data    getRadioChannelIsScanable(String channel)  { /*boolean*/     return getRadioChannelIsScanable(Integer.parseInt(channel)); }
    public    Data    getRadioChannelIsScanable()                { /*boolean*/     return getRadioChannelIsScanable(getRadioChannelActive().getInteger()); }

    /**
     * Returns the radio channel name.
     * 
     * <p>PATH = {@link #getRadioChannelName(int) /Session/RadioChannelName/(CHANNELNUMBER)}
     * 
     * @param channel (Optional) The channel number. Defaults to the active channel.
     * @return name of the radio channel in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getRadioChannelName(int channel)           { /*string*/      return new Data("Session/RadioChannelName","","",Data.State.NOTAVAILABLE); }
    public    Data    getRadioChannelName(String channel)        { /*string*/      return getRadioChannelName(Integer.parseInt(channel)); }
    public    Data    getRadioChannelName()                      { /*boolean*/     return getRadioChannelName(getRadioChannelActive().getInteger()); }

    /**
     * Returns the radio's scanning state
     * 
     * <p>PATH = {@link #getRadioScan() /Session/RadioScan}
     * 
     * @return Y if radio is scanning, N if not in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getRadioScan()                             { /*boolean*/      return new Data("Session/RadioScan",false,"boolean",Data.State.NOTAVAILABLE); }

    /**
     * Makes the specified radio channel the active channel to transmit on.
     * If you cannot transmit on this channel, it does not change the active channel.
     * 
     * <p>PATH = {@link #setRadioChannel(int) /Session/setRadioChannel/(CHANNELNUMBER)}
     * 
     * @param channel The channel number.
     * @return the active channel number in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    setRadioChannel(int channel)               { /*string*/      return new Data("Session/setRadioChannel",-1,"",Data.State.NOTAVAILABLE); }
    public    Data    setRadioChannel(String channel)            { /*string*/      return setRadioChannel(Integer.parseInt(channel)); }

    /**
     * Deletes the specified radio channel.
     * 
     * <p>PATH = {@link #setRadioChannelDelete(int) /Session/setRadioChannelDelete/(CHANNELNUMBER)}
     * 
     * @param channel The channel number.
     * @return the channel number deleted in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    setRadioChannelDelete(int channel)         { /*string*/      return new Data("Session/setRadioChannelDelete",channel,"",Data.State.NOTAVAILABLE); }
    public    Data    setRadioChannelDelete(String channel)      { /*string*/      return setRadioChannelDelete(Integer.parseInt(channel)); }

    /**
     * Mutes or Unmutes the specified radio channel.
     * 
     * <p>PATH = {@link #setRadioChannelMute(int,boolean) /Session/setRadioChannelMute/(CHANNELNUMBER)/(Y/N)}
     * 
     * @param channel (Optional) The channel number. Defaults to the active channel.
     * @param flag (Optional) Y to mute, N to unmute. Default is Y.
     * @return the channel number muted in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    setRadioChannelMute(int channel,boolean flag)    { /*string*/     return new Data("Session/setRadioChannelMute/"+Integer.toString(channel),flag,"",Data.State.NOTAVAILABLE); }
    public    Data    setRadioChannelMute(String channel, String flag) { /*string*/     return setRadioChannelMute(Integer.parseInt(channel),(new Data("",flag)).getBoolean()); }
    public    Data    setRadioChannelMute(String channel)              { /*string*/     return setRadioChannelMute(Integer.parseInt(channel),true); }
    public    Data    setRadioChannelMute()                            { /*string*/     return setRadioChannelMute(getRadioChannelActive().getInteger(),true); }

    /**
     * Makes the specified radio channel the active channel to transmit on.
     * If the channel does not exist, an attempt to create the channel is done.
     * If the channel already exists, and you can transmit on it, it will join that channel and make it the active channel.
     * 
     * <p>PATH = {@link #setRadioChannelName(String) /Session/setRadioChannelName/(CHANNELNAME)}
     * 
     * @param channelName The channel name, limited to 15 characters.
     * @return the channel name in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    setRadioChannelName(String channelName)    { /*string*/      return new Data("Session/setRadioChannelName",channelName,"",Data.State.NOTAVAILABLE); }

    
    /**
     * Turns the scanning of the channels on and off, such that, you are only listening on the active channel.
     * Some channels may not turn off based on the SIM, such as, RACECONTROL or ADMIN channels.
     * 
     * <p>PATH = {@link #setRadioScan(boolean) /Session/setRadioScan/(Y/N)}
     * 
     * @param flag Y to enable scanning, N to disable scanning
     * @return flag in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    setRadioScan(boolean flag)                 { /*boolean*/     return new Data("Session/setRadioScanable",flag,"boolean",Data.State.NOTAVAILABLE); }
    public    Data    setRadioScan(String flag)                  { /*boolean*/     return setRadioScan((new Data("",flag)).getBoolean());}

    
    /**
     * Returns the state of the current replay position.
     * <dl>
     * <dt>&gt;</dt><dd>Playing at Normal Speed</dd>
     * <dt>&lt;&lt; {speed}x</dt><dd>Rewinding</dd>
     * <dt>&gt;&gt; {speed}x</dt><dd>Fast Forwarding</dd>
     * <dt>||</dt><dd>Paused</dd>
     * </dl>
     * 
     * <p>PATH = {@link #getReplay() /Session/Replay}
     * 
     * @since 1.3
     * @return A string representing the replay state in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getReplay()                                { /*String*/      return new Data("Session/Replay","","String",Data.State.NOTAVAILABLE); }

    /**
     * Returns the type of restart, double or single file.
     * 
     * <p>PATH = {@link #getRestart() /Session/Restart}
     * 
     * @since 1.20
     * @return A string representing the restart type in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getRestart()                                { /*String*/      return new Data("Session/Restart","","String",Data.State.NOTAVAILABLE); }
    
    /**
     * Tells the replay system what to do. 
     * Note, some SIMs require you to get out of the car first. 
     * <dl>
     * <dt>PLAY, &gt;</dt><dd>Start Playing at Normal Speed</dd>
     * <dt>REWIND, RW, &lt;, &lt;&lt;</dt><dd>Start Rewinding. Multiple presses will go faster.</dd>
     * <dt>FASTFORWARD, FF, &gt;&gt;</dt><dd>Start Fast Forwarding. Multiple presses will go faster.</dd>
     * <dt>PAUSE, ||</dt><dd>Pause the replay</dd>
     * <dt>SLOWMOTION, SM, |&gt;</dt><dd>Slow down the replay play back speed in the current direction. Multiple presses will go slower.</dd>
     * </dl>
     * <p>PATH = {@link #setReplay(String) /Session/setReplay/(COMMAND)}
     * 
     * @since 1.3
     * @param command The command to execute. Defaults to PLAY.
     * @return The speed the replay is currently playing at in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    setReplay(String command)                  { /*String*/      return new Data("Session/setReplay",command,"String",Data.State.NOTAVAILABLE); }

    /**
     * Tells the replay system what position to start playing at. Here are the available commands.
     * <dl>
     * <dt>BEGINNING, START</dt><dd>Start Playing at the beginning</dd>
     * <dt>ENDING, END</dt><dd>Go to end of replay. If in a session, should take you back to live.</dd>
     * <dt>NEXTFRAME, NEXT</dt><dd>Go to the next frame</dd>
     * <dt>PREVFRAME, PREV</dt><dd>Go to the previous frame</dd>
     * <dt>NEXTLAP</dt><dd>Go to the next lap</dd>
     * <dt>PREVLAP</dt><dd>Go to the previous lap</dd>
     * <dt>NEXTCRASH</dt><dd>Go to the next crash</dd>
     * <dt>PREVCRASH</dt><dd>Go to the previous crash</dd>
     * <dt>NEXTSESSION</dt><dd>Go to the next session, Practice, Qualifying, Race</dd>
     * <dt>PREVSESSION</dt><dd>Go to the previous session, Practice, Qualifying, Race</dd>
     * <dt>FORWARD15</dt><dd>Go forward 15 seconds</dd>
     * <dt>BACKWARD15</dt><dd>Go backwards 15 seconds</dd>
     * <dt>FORWARD30</dt><dd>Go forward 30 seconds</dd>
     * <dt>BACKWARD30</dt><dd>Go backwards 30 seconds</dd>
     * <dt>FORWARD60</dt><dd>Go forward 60 seconds</dd>
     * <dt>BACKWARD80</dt><dd>Go backwards 60 seconds</dd>
     * </dl>
     * <p>PATH = {@link #setReplayPosition(String) /Session/setReplayPosition/(COMMAND)}
     * 
     * @since 1.3
     * @param command The position to go to.
     * @return The speed the replay is currently playing at in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    setReplayPosition(String command)          { /*String*/      return new Data("Session/setReplayPosition",command,"String",Data.State.NOTAVAILABLE); }
    
    /**
     * Tells the system what type of restart you want.
     * <dl>
     * <dt>DOUBLEFILE</dt><dd>Restart Double File</dd>
     * <dt>SINGLEFILE</dt><dd>Restart Single File</dd>
     * </dl>
     * <p>PATH = {@link #setRestart(String) /Session/setRestart/(COMMAND)}
     * 
     * @since 1.20
     * @param command The restart method.
     * @return The restart method in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    setRestart(String command)          { /*String*/      return new Data("Session/setRestart",command,"String",Data.State.NOTAVAILABLE); }

    /**
     * Returns if the SIM has own pit count down spotter enabled.
     * 
     * <p>PATH = {@link #getSpotterPitCountDown() /Session/SpotterPitCountDown}
     * 
     * @return Y if SIM is counting down, N if not in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getSpotterPitCountDown()                { /*boolean*/     return new Data("Session/SpotterPitCountDown",false,"boolean",Data.State.NOTAVAILABLE);  }
    
    /**
     * Returns the time the current session started as the number of seconds since Jan 1, 1970 UTC.
     * It may be adjusted by the SIM to reflect a virtual date/time being simulated.
     * But, that will be in UTC as well. The offset of the time zone is in the State in the format of +/-HHMM.
     * 
     * You can use the com.SIMRacingApps.Track.getTimeZone() to display the track's time zone,
     * but to convert to the time zone of the track use the offset returned in the Data.State field.
     * This will account for daylight savings time.
     * 
     * <p>PATH = {@link #getStartTime() /Session/StartTime}
     * 
     * @return The start time in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getStartTime() {
        Data d = new Data("Session/StartTime",(double)(new Date()).getTime()/1000L,"s"); 
        d.setState(this._getTimeZoneOffset(new Date(), TimeZone.getDefault().getID()));
        return d;
    }

    /**
     * Returns the status of the session as one of the following defined in 
     * {@link com.SIMRacingApps.Session.Status}.
     * <ul>
     * <li>UNKNOWN</li>
     * <li>ENGINES_STARTED</li>
     * <li>GREEN</li>
     * <li>CAUTION</li>
     * <li>RED</li>
     * <li>FINISHED</li>
     * </ul>
     * <p>PATH = {@link #getStatus() /Session/Status} 1.12
     * 
     * @since 1.12
     * @return The status in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getStatus() {
        Data d = new Data("Session/Status",Status.UNKNOWN,"",Data.State.NOTAVAILABLE); 
        return d;
    }
    
    /**
     * Returns a String that is SIM specific that represents the strength of the field.
     * This implies a SIM has some sort of ranking system.
     * Do not assume a specific format of this string. It is for display purposes only.
     * 
     * <p>PATH = {@link #getStrengthOfField() /Session/StrengthOfField}
     * 
     * @return The strength of the field in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getStrengthOfField()             { /*String*/                              return new Data("Session/StrengthOfField","",Data.State.NOTAVAILABLE); }

    /**
     * Returns the time of the current session as the number of seconds since Jan 1, 1970 UTC.
     * It may be adjusted by the SIM to reflect a virtual date/time being simulated. 
     * But, that will be in UTC as well. The offset of the time zone is in the State in the format of +/-HHMM.
     * 
     * You can use the com.SIMRacingApps.Track.getTimeZone() to display the track's time zone,
     * but to convert to the time zone of the track use the offset returned in the Data.State field.
     * This will account for daylight savings time.
     * 
     * <p>PATH = {@link #getTime() /Session/Time}
     * 
     * @return The start time in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getTime() { 
        double startTime = getStartTime().getDouble() + getTimeElapsed().getDouble();
        Data d = new Data("Session/Time",startTime,"s");
        
        d.setState((getTrack() == null 
                ? this._getTimeZoneOffset(new Date(Math.round(startTime)*1000L), TimeZone.getDefault().getID())
                : this._getTimeZoneOffset(new Date(Math.round(startTime)*1000L), getTrack().getTimeZone().getString())));

        return d;
    }
    
    /**
     * Returns the time of the current session in the time zone of the track using the following format.
     * 
     * YYYYMMDDHHMMSS
     * 
     * It may be adjusted by the SIM to reflect a virtual date/time being simulated. 
     * 
     * You can use the com.SIMRacingApps.Track.getTimeZone() to display the track's time zone.
     * 
     * <p>PATH = {@link #getTimeString() /Session/TimeString}
     * 
     * @return The start time in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getTimeString() {
        Data d = new Data("Session/TimeString","","");
        Data t = getTime();
        long seconds = t.getLong();
        String tz = t.getState();

        Date dt = new Date(seconds*1000L);
        
        //displaying this date on IST timezone
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        String track_tz = this.m_SIMPlugin.getSession().getTrack().getTimeZone().getString();
        df.setTimeZone(TimeZone.getTimeZone(track_tz));
        String tracktime = df.format(dt);

        d.setValue(tracktime);
        d.setState(tz);
        return d;
    }
    
    /**
     * Returns the number of seconds since the current session started.
     * 
     * <p>PATH = {@link #getTimeElapsed() /Session/TimeElapsed}
     * 
     * @return The number of seconds in current session in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getTimeElapsed()                 { /*double*/                              return new Data("Session/TimeElapsed",0.0,"s",Data.State.NOTAVAILABLE); }

    /**
     * If the current session has a time restriction, this returns the number of seconds remaining.
     * If the session is laps bound and the laps to go is not estimated, then the time remaining will be estimated.
     * 
     * If there is no time restriction, then the time is estimated based on the leader's average lap times.
     * The UOM will be prefixed with a tilde(~) to indicate it is an approximate time.
     * 
     * <p>PATH = {@link #getTimeRemaining() /Session/TimeRemaining}
     * 
     * @return The number of seconds remaining in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getTimeRemaining()               { /*double*/                              return new Data("Session/TimeRemaining",0.0,"s",Data.State.NOTAVAILABLE); }

    /**
     * Returns an instance to the Track in this session
     * Each SIM will have to provide an override that does not return a null
     * @return An instance of {@link com.SIMRacingApps.Track}
     */
    public    Track   getTrack()                       { /*Track*/                               return defaultTrack != null ? defaultTrack : (defaultTrack = new Track(m_SIMPlugin)); }
    
    /**
     * Returns the type of the current session as defined by {@link com.SIMRacingApps.Session.Type}
     * 
     * <p>PATH = {@link #getType() /Session/Type}
     * 
     * @return The session type in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getType()                        { /*String*/                              return new Data("Session/Type",Type.UNKNOWN,"",Data.State.NOTAVAILABLE); }

    /**
     * Returns the reference car identifier as defined by {@link com.SIMRacingApps.Session#getCar(String)}. 
     * The default reference car is "ME", but can be changed by calling {@link com.SIMRacingApps.Session#setReferenceCar(String)}
     * 
     * <p>PATH = {@link #getReferenceCar() /Session/ReferenceCar}
     * 
     * @return The reference car identifier in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getReferenceCar()                { /*car*/                                 return new Data("Session/ReferenceCar",getIsReplay().getBoolean() ? m_referenceCar : (m_referenceCar = "ME"),"",Data.State.NORMAL); }

    /**
     * Advance to next Session. 
     * This will tell the SIM go to the next session, practice to qualify to grid to end of race.
     * This command is SIM specific and requires special privileges to succeed.
     * 
     * <p>PATH = {@link #setAdvanceFlag() /Session/setAdvanceFlag}
     * 
     * @return The string sent to SIM in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setAdvanceFlag() {
        return new Data("Session/setAdvanceFlag","","String",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Sets the flag returned by getIsAppsHidden().
     * 
     * <p>PATH = {@link #setAppsHidden() /Session/setAppsHidden}
     * 
     * @param isHidden true to hide the apps, false to unhide them
     * @return The apps hidden state in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setAppsHidden(boolean isHidden) {
        m_hideApps = isHidden;
        return getIsAppsHidden();
    }
    public    Data setAppsHidden(String isHidden) {
        return setAppsHidden(new Data("",isHidden).getBoolean());
    }
    public    Data setAppsHidden() {
        return setAppsHidden(true);
    }
    
    /**
     * Changes the camera.
     * 
     * This is very SIM specific. Some SIMs can do this, others cannot.
     * The name of the camera is very SIM specific. 
     * Some SIMs limit the camera you can choose based on if you are driving or spectating or watching a replay.
     * So, if the SIM doesn't allow the change, then this function will not really know. Therefore, there I cannot detect an error condition.
     * 
     * <p>PATH = {@link #setCamera(String,String,String) /Session/setCamera/(CAMERA)/(FOCUSON)/(CARIDENTIFIER)}
     *
     * @since 1.3
     * @param cameraName The name of the camera. Can be the string CURRENT or blank to use the currently selected camera.
     * @param focusOn (Optional) What to focus on. Valid values are (CRASHES,LEADER,EXCITING,DRIVER). Default is DRIVER.
     * @param carIdentifier (Optional) A car identifier as defined by {@link com.SIMRacingApps.Session#getCar(String)}. Default is REFERENCE.
     * @return The camera name in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setCamera(String cameraName,String focusOn,String carIdentifier) {
        return new Data("Session/setCamera/"+cameraName+"/"+focusOn+"/"+carIdentifier,"","String",Data.State.NOTAVAILABLE);
    }
    public    Data setCamera(String cameraName,String focusOn) {
        return setCamera(cameraName,focusOn,"REFERENCE");
    }
    public    Data setCamera(String cameraName) {
        return setCamera(cameraName,getCameraFocus().getString());
    }
    
    /**
     * Changes the focus of the current camera.
     * 
     * This is very SIM specific. Some SIMs can do this, others cannot.
     * The name of the camera is very SIM specific. 
     * Some SIMs limit the camera you can choose based on if you are driving or spectating or watching a replay.
     * So, if the SIM doesn't allow the change, then this function will not really know. Therefore, there I cannot detect an error condition.
     * 
     * <p>PATH = {@link #setCameraFocus(String,String) /Session/setCamera/(FOCUSON)/(CARIDENTIFIER)}
     *
     * @param focusOn What to focus on. Valid values are (CRASHES,LEADER,EXCITING,DRIVER). Default is DRIVER.
     * @param carIdentifier (Optional) A car identifier as defined by {@link com.SIMRacingApps.Session#getCar(String)}. Default is REFERENCE.
     * @return The camera name in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setCameraFocus(String focusOn,String carIdentifier) {
        return setCamera(getCamera().getString(),focusOn,carIdentifier);
    }
    public    Data setCameraFocus(String focusOn) {
        return setCameraFocus(focusOn,"REFERENCE");
    }
    
    /**
     * Sends the text string as a chat message to all drivers.
     * Each SIM must override this method and provide this functionality.
     *
     * <p>PATH = {@link #setChat(String) /Session/setChat/(TEXT)}
     *
     *@param text The text to send to the chat window
     *@return The text string sent to the drivers  in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setChat(String text) {
        return new Data("Session/setChat",text,"String",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Sends the text string as a chat message to the last driver to send you a private message.
     * Each SIM must override this method and provide this functionality.
     *
     * <p>PATH = {@link #setChatReply(String) /Session/setChatReply/(TEXT)}
     *
     *@param text The text to send to the chat window
     *@return The text string sent to the drivers  in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setChatReply(String text) {
        return new Data("Session/setChatReply",text,"String",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Set Chat on or off for all drivers. 
     * This command is SIM specific and requires special privileges to succeed.
     * 
     * <p>PATH = {@link #setChatFlag(boolean) /Session/setChatFlag/(Y/N)}
     * 
     * @param onOffFlag Y/N to turn chat on or off for all drivers.
     * @return The string sent to SIM in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setChatFlag(boolean onOffFlag) {
        return new Data("Session/setChatFlag","","String",Data.State.NOTAVAILABLE);
    }
    public    Data setChatFlag(String onOffFlag) {
        return setChatFlag((new Data("",onOffFlag)).getBoolean());
    }
    
    /**
     * Sets the reference car.
     * 
     * <p>PATH = {@link #setReferenceCar(String) /Session/setReferenceCar/(CARIDENTIFIER)}
     * 
     * @param carIdentifier A car identifier as defined by {@link com.SIMRacingApps.Session#getCar(String)}.
     *            You cannot set the reference car to "REFERENCE".
     *            Default to "ME".
     * @return The reference car identifier in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    setReferenceCar(String carIdentifier)      {         
        if (!carIdentifier.equalsIgnoreCase("REFERENCE"))
            if (carIdentifier.isEmpty())
                m_referenceCar = "ME";
            else
                m_referenceCar = carIdentifier;
        return getReferenceCar();
    }
    public    Data    setReferenceCar()                { /*void*/                                return setReferenceCar("ME");}

    /**
     * Set Full Course Caution Flag on. You cannot turn it off once it is issued. 
     * This command is SIM specific and requires special privileges to succeed.
     * 
     * <p>PATH = {@link #setCautionFlag() /Session/setCautionFlag}
     * 
     * @return The string sent to SIM in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setCautionFlag() {
        return new Data("Session/setCautionFlag","","String",Data.State.NOTAVAILABLE);
    }

    /**
     * Sets the number of caution laps. Default is 2 laps. 
     * This command is SIM specific and requires special privileges to succeed.
     * 
     * <p>PATH = {@link #setCautionLaps(int) /Session/setCautionLaps/(LAPS)}
     * 
     * @param laps The number of laps for the caution.
     * @return The string sent to SIM in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setCautionLaps(int laps) {
        return new Data("Session/setCautionLaps/"+Integer.toString(laps),"","String",Data.State.NOTAVAILABLE);
    }
    public    Data setCautionLaps(String laps) {
        Data d = new Data("Session/setCautionLaps/"+laps,"","String",Data.State.NOTAVAILABLE);
        try {
            d = setCautionLaps(Integer.parseInt(laps));
        } catch (NumberFormatException e) {}
        return d;
    }
    public    Data setCautionLaps() {
        return setCautionLaps("2");
    }

    /**
     * Add or subtract laps to the current caution laps. Default is to add 1 lap.
     * This command is SIM specific and requires special privileges to succeed.
     * 
     * <p>PATH = {@link #setCautionLapsAdjust(int) /Session/setCautionLapsAdjust/(LAPS)}
     * 
     * @param laps +/- the number of laps to add to the current caution laps.
     * @return The string sent to SIM in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setCautionLapsAdjust(int laps) {
        return new Data("Session/setCautionLapsAdjust/"+Integer.toString(laps),"","String",Data.State.NOTAVAILABLE);
    }
    public    Data setCautionLapsAdjust(String laps) {
        Data d = new Data("Session/setCautionLapsAdjust/"+laps,"","String",Data.State.NOTAVAILABLE);
        try {
            d = setCautionLapsAdjust(Integer.parseInt(laps));
        } catch (NumberFormatException e) {}
        return d;
    }
    public    Data setCautionLapsAdjust() {
        return setCautionLapsAdjust("1");
    }

    /**
     * Sets the Pits to be closed. 
     * This command is SIM specific and requires special privileges to succeed.
     * 
     * <p>PATH = {@link #setPitClose() /Session/setPitClose}
     * 
     * @return The string sent to SIM in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setPitClose() {
        return new Data("Session/setPitClose","","String",Data.State.NOTAVAILABLE);
    }

    /**
     * Sets the Pits to be open. 
     * This command is SIM specific and requires special privileges to succeed.
     * 
     * <p>PATH = {@link #setPitOpen() /Session/setPitOpen}
     * 
     * @return The string sent to SIM in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setPitOpen() {
        return new Data("Session/setPitOpen","","String",Data.State.NOTAVAILABLE);
    }

    /**
     * Tell the SIM to reload the paint files for all cars.
     * If SIM does not support this capability, then the NOTAVAILABLE state will be returned.
     * 
     * <p>PATH = {@link #setReloadPaint() /Session/setReloadPaint}
     * 
     * @return The string sent to SIM in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setReloadPaint() {
        return new Data("Session/setReloadPaint","","String",Data.State.NOTAVAILABLE);
    }

}
