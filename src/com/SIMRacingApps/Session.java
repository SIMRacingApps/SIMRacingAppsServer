package com.SIMRacingApps;

import java.util.ArrayList;

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
 * @copyright Copyright (C) 2015 - 2016 Jeffrey Gilliam
 * @license Apache License 2.0
 */
public class Session {

    /**
     * This class enumerates the values returned by {@link com.SIMRacingApps.Session#getType()}
     * <ul>
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

    /** number of laps to return if the session has unlimited laps */
    public static final int UNLIMITEDLAPS = 9999;

    private String m_referenceCar = "ME";

    private SIMPlugin m_SIMPlugin;
    private Car defaultCar = null;
    
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
    public    Data    getCars()                        { /*int*/                                 return new Data("Session/Cars",0); }

    /**
     * Returns the number of caution laps.
     * 
     * <p>PATH = {@link #getCautionLaps() /Session/CautionLaps}
     * 
     * @return The number of caution laps in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getCautionLaps()                 { /*int*/                                 return new Data("Session/CautionLaps",0,"lap"); }
    
    /**
     * Returns the number of cautions as seen by the leader.
     * 
     * <p>PATH = {@link #getCautions() /Session/Cautions}
     * 
     * @return The number of cautions in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getCautions()                    { /*int*/                                 return new Data("Session/Cautions",0,"integer"); }

    /**
     * Returns an array of the class names sorted fastest to slowest.
     * 
     * <p>PATH = {@link #getClassNames() /Session/ClassNames}
     * 
     * @return The class names array in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getClassNames() {
        ArrayList<String> a = new ArrayList<String>();
        return new Data("/Session/ClassNames",a,"String");
    }
    
    /**
     * Returns a SIM specific string indicating the version of the data just returned.
     * Do not make any assumptions of how this is formatted. Assume it's just a printable string.
     * 
     * <p>PATH = {@link #getDataVersion() /Session/DataVersion}
     * 
     * @return The version in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getDataVersion()                 { /*Long*/                                return new Data("Session/DataVersion",System.currentTimeMillis()); }

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
            return new Data("Session/DiffCars/null",0.0,"s");
        
        Data seconds        = new Data("Session/DiffCars/"+carIdentifier1+"/"+carIdentifier2,0.0,"s");
        String cartoproject = carIdentifier2;
        Car _car2           = getCar(carIdentifier2);
        Car _car1           = getCar(carIdentifier1);

        if (_car2 == null
        ||  _car1 == null
        ||  (_car2.getId().getInteger() == -1 && !carIdentifier2.equalsIgnoreCase("PITSTALL"))
        ||  (_car1.getId().getInteger() == -1 && !carIdentifier1.equalsIgnoreCase("PITSTALL"))
        )
            return new Data("Session/DiffCars"+carIdentifier1+"/"+carIdentifier2,0.0,"s");

        if (carIdentifier1.equalsIgnoreCase("PITSTALL")) {
            if (!_car1.getPitLocation().getState().equals(Data.State.NORMAL))
                return new Data("Session/DiffCars"+carIdentifier1+"/"+carIdentifier2,0.0,"s");
            else
                _car1 = _car1;
        }
        
        if (carIdentifier2.equalsIgnoreCase("PITSTALL")) {
            if (!_car2.getPitLocation().getState().equals(Data.State.NORMAL))
                return new Data("Session/DiffCars"+carIdentifier1+"/"+carIdentifier2,0.0,"s");
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
                lap2 = _car2.getLap(Car.LapType.CURRENT);
                lap1 = _car1.getLap(Car.LapType.CURRENT);
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
            return new Data("Session/DiffCarsRelative/"+carIdentifier1+"/"+carIdentifier2,"");

        Data pct2 = _car2.getLap(Car.LapType.COMPLETEDPERCENT);
        Data pct1 = _car1.getLap(Car.LapType.COMPLETEDPERCENT);

        try {
            int position = 0;
            int refposition = 0;

            if (getCar("REFERENCE").getId().getInteger() == _car2.getId().getInteger()) {
//            if (car.equalsIgnoreCase("ME") || car.equalsIgnoreCase("REFERENCE")) {
                position = 0;
            }
            else
            if (carIdentifier2.startsWith("RPC") || carIdentifier2.startsWith("RLC")) {
                position = Integer.parseInt(carIdentifier2.substring(3));
            }
            else
            if (carIdentifier2.startsWith("RL") || carIdentifier2.startsWith("RP") || carIdentifier2.startsWith("RC")) {
                position = Integer.parseInt(carIdentifier2.substring(2));
            }
            else
            if (carIdentifier2.startsWith("R") || carIdentifier2.startsWith("P")) {
                position = Integer.parseInt(carIdentifier2.substring(1));
            }

            if (getCar("REFERENCE").getId().getInteger() == _car1.getId().getInteger()) {
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
            if (carIdentifier1.startsWith("RL") || carIdentifier1.startsWith("RP") || carIdentifier1.startsWith("RC")) {
                refposition = Integer.parseInt(carIdentifier1.substring(2));
            }
            else
            if (carIdentifier1.startsWith("R") || carIdentifier1.startsWith("P")) {
                refposition = Integer.parseInt(carIdentifier1.substring(1));
            }

            Data carspeed; //if car is ahead of reference, the use reference speed. If behind, use car's speed.
            double distanceBetween = 0.0;
            if (position < refposition) {
                if (pct2.getDouble() > pct1.getDouble())
                    distanceBetween = (1.0 - (((pct2.getDouble()/100.0)) - ((pct1.getDouble()/100.0)))) * -1.0;
                else
                    distanceBetween = (((pct1.getDouble()/100.0)) - ((pct2.getDouble()/100.0))) * -1.0;
                carspeed = _car2._getGauge(Gauge.Type.SPEEDOMETER).getValueCurrent().convertUOM("mph");
                if (Math.floor(carspeed.getDouble()) <= 0.0)
                    carspeed = _car1._getGauge(Gauge.Type.SPEEDOMETER).getValueCurrent().convertUOM("mph");
            }
            else {
                if (pct2.getDouble() < pct1.getDouble())
                    distanceBetween = (1.0 - (((pct1.getDouble()/100.0)) - ((pct2.getDouble()/100.0))));
                else
                    distanceBetween = (((pct2.getDouble()/100.0)) - ((pct1.getDouble()/100.0)));
                carspeed = _car1._getGauge(Gauge.Type.SPEEDOMETER).getValueCurrent().convertUOM("mph");
                if (Math.floor(carspeed.getDouble()) <= 0.0)
                    carspeed = _car2._getGauge(Gauge.Type.SPEEDOMETER).getValueCurrent().convertUOM("mph");
            }

            //use the car's speed if you can get it. The unit of measure for speed and track length need to match.
//            if (Math.floor(carspeed.getDouble()) > 0.0) {
//                Data tracklength = m_SIMPlugin.getSession().getTrack().getLength().convertUOM("mile");
//                seconds.setValue(((tracklength.getDouble() / carspeed.getDouble()) * 60 * 60) * distanceBetween );
//            }
//            else 
            {
//if (logit) {
//    if (pct1.getDouble() < 1.0) {
//        double x = pct1.getDouble();
//    }
//}
                double projected = _car1.getLapTimeProjected().getDouble();
                //if car1 is not moving, see if car2 is.
                if (projected <= 0.0) {
//if (logit) {
//    Server.logger().info("projected("+projected+") is <= 0, using car2");
//}
                    projected = _car2.getLapTimeProjected().getDouble();
                }
                double s = distanceBetween * projected;
//if (logit) {
//    Server.logger().info(String.format("car1(%s,%8.6f), car2(%s,%8.6f), distanceBetween(%8.6f), TimeProjected(%8.6f), seconds(%8.6f), current(%8.6f)",
//            carIdentifier1,pct1.getDouble()/100.0,carIdentifier2,pct1.getDouble()/100.0,distanceBetween,projected,s,_car1.getLapTime(Car.LapType.CURRENT).getDouble()));
//}
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
     * Returns the total number of incidents allowed for this session.
     * If the session does not specify them, such as, practice, then 9999 is returned.
     * 
     * Different SIMs may measure these in different units of measure. The default is "x".
     * 
     * <p>PATH = {@link #getIncidentLimit() /Session/IncidentLimit}
     *
     * @return The incident limit in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIncidentLimit()                              { /*int*/                                  return new Data("Session/IncidentLimit",0,"x",Data.State.OFF); } 

    /**
     * Returns 'Y' if the green flag is waving.
     * 
     * <p>PATH = {@link #getIsGreenFlag() /Session/IsGreenFlag}
     * 
     * @return 'Y' if the green flag is out in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIsGreenFlag()                 { /*Boolean*/                             return new Data("Session/IsGreenFlag",false,"boolean",Data.State.NORMAL); }

    /**
     * Returns 'Y' if the caution flag is waving.
     * 
     * <p>PATH = {@link #getIsCautionFlag() /Session/IsCautionFlag}
     * 
     * @return 'Y' if the caution flag is out in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIsCautionFlag()               { /*Boolean*/                             return new Data("Session/IsCautionFlag",false,"boolean",Data.State.NORMAL); }

    /**
     * Returns 'Y' if the checkered flag is waving.
     * 
     * <p>PATH = {@link #getIsCheckeredFlag() /Session/IsCheckeredFlag}
     * 
     * @return 'Y' if the checkered flag is out in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIsCheckeredFlag()             { /*Boolean*/                             return new Data("Session/IsCheckeredFlag",false,"boolean",Data.State.NORMAL); }

    /**
     * Returns 'Y' if the crossed flag is waving. This generally means the leader has passed the halfway point.
     * 
     * <p>PATH = {@link #getIsCrossedFlag() /Session/IsCrossedFlag}
     * 
     * @return 'Y' if the crossed flag is out in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIsCrossedFlag()               { /*Boolean*/                             return new Data("Session/IsCrossedFlag",false,"boolean",Data.State.NORMAL); }

    /**
     * Returns 'Y' if the white flag is waving.
     * 
     * <p>PATH = {@link #getIsWhiteFlag() /Session/IsWhiteFlag}
     * 
     * @return 'Y' if the white flag is out in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIsWhiteFlag()                 { /*Boolean*/                             return new Data("Session/IsWhiteFlag",false,"boolean",Data.State.NORMAL); }

    /**
     * Returns 'Y' if the red flag is waving.
     * 
     * <p>PATH = {@link #getIsRedFlag() /Session/IsRedFlag}
     * 
     * @return 'Y' if the green flag is out in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getIsRedFlag()                   { /*Boolean*/                             return new Data("Session/IsRedFlag",false,"boolean",Data.State.NORMAL); }
    
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
    public    Data    getLap()                         { /*int*/                                 return new Data("Session/Lap",0,"lap"); }
    
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
    public    Data    getLaps(String sessionType)      { /*int*/                                 return new Data("Session/Laps",0,"lap"); }
    public    Data    getLaps()                        { /*int*/                                 return getLaps(""); } 

    /**
     * Returns the number of laps to go in sessions that define a total number of laps
     * See {@link com.SIMRacingApps.Session#getLaps()}
     * 
     * <p>PATH = {@link #getLapsToGo() /Session/LapsToGo}
     * 
     * @return The number of laps to go in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getLapsToGo()                    { /*int*/                                 return new Data("Session/LapsToGo",0,"lap"); }

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
    public    Data    getRadioChannels()                         { /*int*/         return new Data("Session/RadioChannels",0); }

    /**
     * Returns the active radio channel.
     * 
     * <p>PATH = {@link #getRadioChannelActive() /Session/RadioChannelActive}
     * 
     * @return The active channel in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getRadioChannelActive()                    { /*int*/         return new Data("Session/RadioChannelActive",-1); }
    
    /**
     * Returns if the radio channel can be deleted.
     * 
     * <p>PATH = {@link #getRadioChannelIsDeleteable(int) /Session/RadioChannelIsDeletable/(CHANNELNUMBER)}
     * 
     * @param channel (Optional) The channel number. Defaults to the active channel.
     * @return Y if deleteable, N if not in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getRadioChannelIsDeleteable(int channel)    { /*boolean*/     return new Data("Session/RadioChannelIsDeletable",false,"boolean"); }
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
    public    Data    getRadioChannelIsListenOnly(int channel)   { /*boolean*/     return new Data("Session/RadioChannelIsListenOnly",true,"boolean"); }
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
    public    Data    getRadioChannelIsMutable(int channel)      { /*boolean*/     return new Data("Session/RadioChannelIsMutable",false,"boolean"); }
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
    public    Data    getRadioChannelIsMuted(int channel)        { /*boolean*/     return new Data("Session/RadioChannelIsMuted",false,"boolean"); }
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
    public    Data    getRadioChannelIsScanable(int channel)     { /*boolean*/     return new Data("Session/RadioChannelIsScanable",true,"boolean"); }
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
    public    Data    getRadioChannelName(int channel)           { /*string*/      return new Data("Session/RadioChannelName",""); }
    public    Data    getRadioChannelName(String channel)        { /*string*/      return getRadioChannelName(Integer.parseInt(channel)); }
    public    Data    getRadioChannelName()                      { /*boolean*/     return getRadioChannelName(getRadioChannelActive().getInteger()); }

    /**
     * Returns the radio's scanning state
     * 
     * <p>PATH = {@link #getRadioScan() /Session/RadioScan}
     * 
     * @return Y if radio is scanning, N if not in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getRadioScan()                             { /*boolean*/      return new Data("Session/RadioScan",false,"boolean"); }

    /**
     * Makes the specified radio channel the active channel to transmit on.
     * If you cannot transmit on this channel, it does not change the active channel.
     * 
     * <p>PATH = {@link #setRadioChannel(int) /Session/setRadioChannel/(CHANNELNUMBER)}
     * 
     * @param channel The channel number.
     * @return the active channel number in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    setRadioChannel(int channel)               { /*string*/      return new Data("Session/setRadioChannel",-1); }
    public    Data    setRadioChannel(String channel)            { /*string*/      return setRadioChannel(Integer.parseInt(channel)); }

    /**
     * Deletes the specified radio channel.
     * 
     * <p>PATH = {@link #setRadioChannelDelete(int) /Session/setRadioChannelDelete/(CHANNELNUMBER)}
     * 
     * @param channel The channel number.
     * @return the channel number deleted in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    setRadioChannelDelete(int channel)         { /*string*/      return new Data("Session/setRadioChannelDelete",channel); }
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
    public    Data    setRadioChannelMute(int channel,boolean flag)    { /*string*/     return new Data("Session/setRadioChannelMute/"+Integer.toString(channel),flag); }
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
    public    Data    setRadioChannelName(String channelName)    { /*string*/      return new Data("Session/setRadioChannelName",channelName); }

    
    /**
     * Turns the scanning of the channels on and off, such that, you are only listening on the active channel.
     * Some channels may not turn off based on the SIM, such as, RACECONTROL or ADMIN channels.
     * 
     * <p>PATH = {@link #setRadioScan(boolean) /Session/setRadioScan/(Y/N)}
     * 
     * @param flag Y to enable scanning, N to disable scanning
     * @return flag in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    setRadioScan(boolean flag)                 { /*boolean*/     return new Data("Session/setRadioScanable",flag,"boolean"); }
    public    Data    setRadioScan(String flag)                  { /*boolean*/     return setRadioScan((new Data("",flag)).getBoolean());}
    
    /**
     * Returns the time the current session started as the number of seconds since Jan 1, 1970.
     * 
     * <p>PATH = {@link #getStartTime() /Session/StartTime}
     * 
     * @return The start time in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getStartTime()                   { /*double*/                              return new Data("Session/StartTime",(double)System.currentTimeMillis()/1000.0,"s"); }

    /**
     * Returns a String that is SIM specific that represents the strength of the field.
     * This implies a SIM has some sort of ranking system.
     * Do not assume a specific format of this string. It is for display purposes only.
     * 
     * <p>PATH = {@link #getStrengthOfField() /Session/StrengthOfField}
     * 
     * @return The strength of the field in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getStrengthOfField()             { /*String*/                              return new Data("Session/StrengthOfField",""); }

    /**
     * Returns the number of seconds since the current session started.
     * 
     * <p>PATH = {@link #getTimeElapsed() /Session/TimeElapsed}
     * 
     * @return The number of seconds in current session in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getTimeElapsed()                 { /*double*/                              return new Data("Session/TimeElapsed",0.0,"s"); }

    /**
     * If the current session has a time restriction, this returns the number of seconds remaining.
     * 
     * If there is no time restriction, then the time is estimated based on the leader's average lap times.
     * The UOM will be prefixed with a tilde(~) to indicate it is an approximate time.
     * 
     * <p>PATH = {@link #getTimeRemaining() /Session/TimeRemaining}
     * 
     * @return The number of seconds remaining in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getTimeRemaining()               { /*double*/                              return new Data("Session/TimeRemaining",0.0,"s"); }

    /**
     * Returns an instance to the Track in this session
     * Each SIM will have to provide an override that does not return a null
     * @return An instance of {@link com.SIMRacingApps.Track}
     */
    public    Track   getTrack()                       { /*Track*/                               return null; }
    
    /**
     * Returns the type of the current session as defined by {@link com.SIMRacingApps.Session.Type}
     * 
     * <p>PATH = {@link #getType() /Session/Type}
     * 
     * @return The session type in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getType()                        { /*String*/                              return new Data("Session/Type",Type.UNKNOWN); }

    /**
     * Returns the reference car identifier as defined by {@link com.SIMRacingApps.Session#getCar(String)}. 
     * The default reference car is "ME", but can be changed by calling {@link com.SIMRacingApps.Session#setReferenceCar(String)}
     * 
     * <p>PATH = {@link #getReferenceCar() /Session/ReferenceCar}
     * 
     * @return The reference car identifier in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getReferenceCar()                { /*car*/                                 return new Data("Session/ReferenceCar",m_referenceCar,"",Data.State.NORMAL); }

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
        return new Data("Session/setAdvanceFlag","","String");
    }
    
    /**
     * Changes the camera.
     * 
     * This is very SIM specific. Some SIMs can do this, others cannot.
     * The name of the group and camera are very SIM specific. 
     * Some SIMs limit the group and camera you can choose based on if you are driving or spectating or watching a replay.
     * So, if the SIM doesn't allow the change, then this function will not really know. Therefore, there I cannot detect an error condition.
     * 
     * <p>PATH = {@link #setCamera(String,String,String) /Session/setCamera/(CARIDENTIFIER)/Group/Camera}
     *
     *@param carIdentifier (Optional) A car identifier as defined by {@link com.SIMRacingApps.Session#getCar(String)}. Default to "REFERENCE".
     *@param group (Optional) The name of the camera group to change to. Default use current group.
     *@param camera (Optional) The name of the camera. The group is required if the camera is not blank. Default to current camera in the current group.
     *@return The group/camera name in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setCamera(String carIdentifier, String group, String camera) {
        return new Data("Session/setCamera/"+carIdentifier+"/"+group+"/"+camera,group+"/"+camera,"String");
    }
    public    Data setCamera(String carIdentifier, String group) {
        return setCamera(carIdentifier,group,"");
    }
    public    Data setCamera(String carIdentifier) {
        return setCamera(carIdentifier,"","");
    }
    public    Data setCamera() {
        return setCamera("REFERENCE","","");
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
        return new Data("Session/setChat",text);
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
        return new Data("Session/setChatReply",text);
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
        return new Data("Session/setChatFlag","","String");
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
        return new Data("Session/setCautionFlag","","String");
    }
}
