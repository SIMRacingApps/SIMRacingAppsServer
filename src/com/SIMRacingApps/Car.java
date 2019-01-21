package com.SIMRacingApps;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.TreeMap;

import com.SIMRacingApps.Gauge;
import com.SIMRacingApps.SIMPlugin;
import com.SIMRacingApps.Data;
import com.SIMRacingApps.Data.State;
import com.SIMRacingApps.Session.CarIdentifiers;
import com.SIMRacingApps.SIMPluginCallbacks.SIMPluginCallback;
import com.SIMRacingApps.Util.FindFile;

/**
 * Provides all information about a car. Each car will have an ID assigned to it by the SIM.
 * Each SIM has to subclass Car and provide the SIM specific data for all methods that it has information for. 
 * @author Jeffrey Gilliam
 * @since 1.0
 * @copyright Copyright (C) 2015 - 2019 Jeffrey Gilliam
 * @license Apache License 2.0
 */
public class Car {

    public static String [] Tires = {"LF","LR","RF","RR"};

    private static final int DEFAULT_LAPS_FUEL_MILAGE       = 0;    //zero means use worse lap, average means average
    private static final double FUELLEVELNEEDED_BUFFER_LAPS = 0.0;  //number of laps to add to the remaining laps as a buffer. TODO: GWC could generate the need to add 2 or more laps
    protected SIMPlugin m_SIMPlugin                         = null;
    
    //just to make things a little easier, I will just make these protected instead of getters and setters
    //each derived class should update this as need to override the default behavior
    protected int m_id                                      = -1;
    protected String m_carIdentifier                        = "";
    protected String m_name                                 = "unknown";
    protected String m_description                          = "An Unknown Car";
    protected String m_mfrLogo                              = "com/SIMRacingApps/Cars/default.png";
    protected double m_pitRoadSpeedRPM                      = -1;
    protected String m_carPath                              = "com/SIMRacingApps/Car.json";
    protected Map<String,Gauge> m_gauges                    = new TreeMap<String,Gauge>();
   

    /**
     * Defines an enumerated String class for where the car is.
     * <p>
     * keep this in sync with SIMRacingApps.css
     */
    public static class Status {
        public static final String INVALID         = "INVALID";
        public static final String INGARAGE        = "INGARAGE";
        public static final String ENTERINGPITSTALL= "ENTERINGPITSTALL";
        public static final String INPITSTALL      = "INPITSTALL";
        public static final String EXITINGPITSTALL = "EXITINGPITSTALL";
        public static final String ONPITROAD       = "ONPITROAD";
        public static final String APPROACHINGPITS = "APPROACHINGPITS";
        public static final String LEAVINGPITS     = "LEAVINGPITS";
        public static final String ONTRACK         = "ONTRACK";
        public static final String OFFTRACK        = "OFFTRACK";
        public static final String TOWING          = "TOWING";
    }

    /**
     * Defines the messages the spotter can say.
     * 
     * @author Jeffrey Gilliam
     * @copyright Copyright (C) 2017 Jeffrey Gilliam
     * @since 1.5
     * @license Apache License 2.0
     */
    public static class SpotterMessages {
        public static final String OFF              = "";
        public static final String CLEAR            = "CLEAR";
        public static final String CARLEFT          = "CARLEFT";
        public static final String CARRIGHT         = "CARRIGHT";
        public static final String CARLEFTRIGHT     = "CARLEFTRIGHT";
        public static final String CARSLEFT         = "CARSLEFT";
        public static final String CARSRIGHT        = "CARSRIGHT";
    }

    /**
     * Defines and validates an enumerated String class of the different types of laps that can be returned by methods that take a LapType as an argument.
     */
    public static class LapType {

        /** The average lap time for green flag laps from the start of the race */
        public static final String AVERAGE         = "Average";
        /** The average lap time for green flag laps from your last pit stop */
        public static final String AVERAGESINCEPITTING = "AverageSincePitting";
        /** The best lap ever run in this car at the track ever */
        public static final String BEST            = "Best";
        /** The lap completed */
        public static final String COMPLETED       = "Completed";
        /** The percentage of the current lap that has been completed */
        public static final String COMPLETEDPERCENT= "CompletedPercent";
        /** The current lap */
        public static final String CURRENT         = "Current";
        /** The number of laps led*/
        public static final String LED             = "Led";
        /** The Optimal lap based on your BEST lap*/
        public static final String OPTIMAL         = "Optimal";
        /** The lap pitted*/
        public static final String PITTED          = "Pitted";
        /** The qualifying lap*/
        public static final String QUALIFYING      = "Qualifying";
        /** Snapshot at the finish line*/
        public static final String FINISHLINE      = "FinishLine";
        /** Snapshot at the finish line at the start of the race*/
        public static final String RACESTART       = "RaceStart";
        /** The average lap time for green flag laps */
        public static final String RUNNINGAVERAGE  = "RunningAverage";
        /** The average lap time for green flag laps since your last pit stop */
        public static final String RUNNINGAVERAGESINCEPITTING = "RunningAverageSincePitting";
        /** The best lap of this session*/
        public static final String SESSIONBEST     = "SessionBest";
        /** The last lap of this session*/
        public static final String SESSIONLAST     = "SessionLast";
        /** The Optimal lap based on your SESSIONBEST lap*/
        public static final String SESSIONOPTIMAL  = "SessionOptimal";
        /** The number of laps since pitting*/
        public static final String SINCEPITTING    = "SincePitting";
        /** The number of caution laps */
        public static final String CAUTION         = "Caution";
        /** The time remaining at the start finish */
        public static final String REMAININGFINISHLINE = "RemainingFinishLine";

        /**
         * Validates the argument "ref" is a valid LapType. Validation is not case sensitive, but the returned value is.
         * @param ref Lap Type to validate
         * @return The validated lap type or "Unknown"
         */
        public static String getReference(String ref) {
            String s = "Unknown";
            if (ref.equalsIgnoreCase(AVERAGE))
                s = AVERAGE;
            else
            if (ref.equalsIgnoreCase(AVERAGESINCEPITTING))
                s = AVERAGESINCEPITTING;
            else
            if (ref.equalsIgnoreCase(RUNNINGAVERAGE))
                s = RUNNINGAVERAGE;
            else
            if (ref.equalsIgnoreCase(RUNNINGAVERAGESINCEPITTING))
                s = RUNNINGAVERAGESINCEPITTING;
            else
            if (ref.equalsIgnoreCase(CURRENT))
                s = CURRENT;
            else
            if (ref.equalsIgnoreCase(SESSIONBEST))
                s = SESSIONBEST;
            else
            if (ref.equalsIgnoreCase(RACESTART))
                s = RACESTART;
            else
            if (ref.equalsIgnoreCase(FINISHLINE))
                s = FINISHLINE;
            else
            if (ref.equalsIgnoreCase(REMAININGFINISHLINE))
                s = REMAININGFINISHLINE;
            else
            if (ref.equalsIgnoreCase(COMPLETED))
                s = COMPLETED;
            else
            if (ref.equalsIgnoreCase(COMPLETEDPERCENT))
                s = COMPLETEDPERCENT;
            else
            if (ref.equalsIgnoreCase("PERCENTCOMPLETED"))
                s = COMPLETEDPERCENT;
            else
            if (ref.equalsIgnoreCase(PITTED))
                s = PITTED;
            else
            if (ref.equalsIgnoreCase(SINCEPITTING))
                s = SINCEPITTING;
            else
            if (ref.equalsIgnoreCase(LED))
                s = LED;
            else
            if (ref.equalsIgnoreCase(SESSIONLAST) || ref.equalsIgnoreCase("LAST"))
                s = SESSIONLAST;
            else
            if (ref.equalsIgnoreCase(SESSIONOPTIMAL))
                s = SESSIONOPTIMAL;
            else
            if (ref.equalsIgnoreCase(OPTIMAL))
                s = OPTIMAL;
            else
            if (ref.equalsIgnoreCase(QUALIFYING))
                s = QUALIFYING;
            else
            if (ref.equalsIgnoreCase(BEST))
                s = BEST;
            else
            if (ref.equalsIgnoreCase(CAUTION))
                s = CAUTION;
            else {
                Server.logger().warning(String.format("LapType(%s) not valid",ref));
            }
            return s;
        }
    }

//    /**
//     * This is an enumeration class for the Tires.
//     * <p>
//     * LF = Left Front
//     * <p>
//     * LR = Left Rear
//     * <p>
//     * RF = Right Front
//     * <p>
//     * RR = Right Rear
//     * <p>
//     * <p>
//     * TIRES[] = A array of the tires
//     *
//     */
//    public class Tire {
//
//        public final static String LF = "LF";
//        public final static String LR = "LR";
//        public final static String RF = "RF";
//        public final static String RR = "RR";
//        public final String[] TIRES = {LF,LR,RF,RR};
//
//    }
    
    /**
     * Class constructor
     * @param SIMPlugin An instance of SIMPlugin.
     */
    public Car(SIMPlugin SIMPlugin) {
        m_SIMPlugin = SIMPlugin;
        __loadCar();
    }

    /**
     * Class constructor to be used by the SIM specific version to establish the "id", "name" and "track" of the car.
     * @param SIMPlugin An instance of SIMPlugin.
     * @param id        A numeric value used by the SIM to identify a car
     * @param name      The name of the car as returned by the SIM
     * @param carPath   The relative path to the car's .json file provided by the SIM
     */
    public Car(SIMPlugin SIMPlugin, int id, String name, String carPath) {
        this.m_SIMPlugin = SIMPlugin;
        this.m_id        = id;
        this.m_name      = name;
        this.m_carPath   = carPath;
        this.m_carIdentifier = "I" + Integer.toString(m_id);
        
        __loadCar();
    }

    /**
     * A convenience function that returns true if the car is an instance of "ME".
     * @return true or false
     */
    public boolean isME() {
        return false;
    }

    /**
     * Returns true if the car instance is actually in the session as defined by the SIM.
     * @return true or false
     */
    public boolean isValid() { return m_id != -1; }

    /**
     * Returns the bearing of the car.
     * 
     * <p>PATH = {@link #getBearing(String) /Car/(CARIDENTIFIER)/Bearing/(UOM)}
     * 
     * @param UOM           (Optional) A unit of measure to return. Defaults to degrees.
     * @return The bearing of the car where 0 is 3 o'clock.
     */
    public Data getBearing(String UOM) {
        Data status = getStatus();
        String location = status.getString().contains("PIT") 
                        ? "ONPITROAD" 
                        : (status.getString().contains("TRACK") ? "ONTRACK" : "OFF");
        Data bearing = m_SIMPlugin.getSession().getTrack().getBearing(location,getLap(LapType.COMPLETEDPERCENT).getDouble(),UOM);
        bearing.setName("Car/"+m_carIdentifier+"/Bearing");
        return bearing;
    }
    public Data getBearing() { return getBearing(""); }

    /**
     * Returns the number of Cautions that this car has seen.
     * <p>
     * To get the number of caution laps seen by this car, use {@link com.SIMRacingApps.Car#getLap(String) getLap("CAUTION")}
     * 
     * <p>PATH = {@link #getCautions() /Car/(CARIDENTIFIER)/Cautions}
     * 
     * @return The number of cautions in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getCautions() {
        return new Data("Car/"+m_carIdentifier+"/Cautions",0,"integer",Data.State.NORMAL);
    }
    
    /**
     * Returns the color of the car's class if in a multi-class session
     * 
     * <p>PATH = {@link #getClassColor() /Car/(CARIDENTIFIER)/ClassColor}
     * 
     * @return The name of the class in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getClassColor() {
        return new Data("Car/"+m_carIdentifier+"/ClassColor",0,"rgb",Data.State.NORMAL);
    }
    
    /**
     * Returns the name of the car's class in a multicar session.
     * 
     * <p>PATH = {@link #getClassName() /Car/(CARIDENTIFIER)/ClassName}
     * 
     * @return The name of the class in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getClassName() {
        return new Data("Car/"+m_carIdentifier+"/ClassName","","String",Data.State.NORMAL);
    }
    
    /**
     * Returns the main color of the car as an RGB value.
     * 
     * <p>PATH = {@link #getColor() /Car/(CARIDENTIFIER)/Color}
     * 
     * @return The color of the car in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getColor() {
        return new Data("Car/"+m_carIdentifier+"/Color",0xdddddd,"RGB",Data.State.NORMAL);
    }

    /**
     * Returns the main color of the car number as an RGB value.
     * 
     * <p>PATH = {@link #getColorNumber() /Car/(CARIDENTIFIER)/ColorNumber}
     * 
     * @return The color of the car number in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getColorNumber() {
        return new Data("Car/"+m_carIdentifier+"/ColorNumber",0x000000,"RGB",Data.State.NORMAL);
    }

    /**
     * Returns the main color of the car number's background as an RGB value.
     * 
     * <p>PATH = {@link #getColorNumberBackground() /Car/(CARIDENTIFIER)/ColorNumberBackground}
     * 
     * @return The color of the car number's background in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getColorNumberBackground() {
        return new Data("Car/"+m_carIdentifier+"/ColorNumberBackground",0x333333,"RGB",Data.State.NORMAL);
    }

    /**
     * Returns the main color of the car number's outline as an RGB value.
     * 
     * <p>PATH = {@link #getColorNumberOutline() /Car/(CARIDENTIFIER)/ColorNumberOutline}
     * 
     * @return The color of the car number's outline in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getColorNumberOutline() {
        return new Data("Car/"+m_carIdentifier+"/ColorNumberOutline",0xff0000,"RGB",Data.State.NORMAL);
    }

    /**
     * Returns the printable description of the car.
     * 
     * <p>PATH = {@link #getDescription() /Car/(CARIDENTIFIER)/Description}
     * 
     * @return The description of the car in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getDescription() {
        return new Data("Car/"+m_carIdentifier+"/Description",m_description,"",Data.State.NORMAL);
    }
    
    /**
     * Returns the number of times the car has blinked out.
     * 
     * <p>PATH = {@link #getDiscontinuality() /Car/(CARIDENTIFIER)/Discontinuality}
     * 
     * @return The number of blinks in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getDiscontinuality() {
    	return new Data("Car/"+m_carIdentifier+"/Discontinuality","0","integer",Data.State.NORMAL);
    }

    /**
     * Returns the club name of the current driver of the car.
     * 
     * <p>PATH = {@link #getDriverClubName() /Car/(CARIDENTIFIER)/ClubName}
     * 
     * @return The club name in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getDriverClubName() {
        return new Data("Car/"+m_carIdentifier+"/DriverClubName","",Data.State.NORMAL);
    }

    /**
     * Returns the division name of the current driver of the car.
     * 
     * <p>PATH = {@link #getDriverDivisionName() /Car/(CARIDENTIFIER)/DriverDivisionName}
     * 
     * @return The division name in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getDriverDivisionName() {
        return new Data("Car/"+m_carIdentifier+"/DriverDivisionName","",Data.State.NORMAL);
    }

    /**
     * Returns the initials of the current driver of the car.
     * 
     * <p>PATH = {@link #getDriverInitials() /Car/(CARIDENTIFIER)/DriverInitials}
     * 
     * @return The initials in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getDriverInitials() {
        return new Data("Car/"+m_carIdentifier+"/DriverInitials","",Data.State.NORMAL);
    }

    /**
     * Returns the license color of the current driver of the car.
     * License colors are SIM specific, do not make an assumption that one color ranks higher than another.
     * They are intended to be presented to the user as a background or foreground color of an object.
     * 
     * <p>PATH = {@link #getDriverLicenseColor() /Car/(CARIDENTIFIER)/DriverLicenseColor}
     * 
     * @return The license color in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getDriverLicenseColor() {
        return new Data("Car/"+m_carIdentifier+"/DriverLicenseColor",0xFF0000/*red*/,"RGB",Data.State.NORMAL);
    }

    /**
     * Returns the text color that best overlays the license color of the current driver of the car when it is used as a background.
     * License colors are SIM specific, do not make an assumption that one color ranks higher than another.
     * They are intended to be presented to the user as a background or foreground color of an object.
     * 
     * <p>PATH = {@link #getDriverLicenseColorText() /Car/(CARIDENTIFIER)/Driver/LicenseColorText}
     * 
     * @return The license color in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getDriverLicenseColorText() {
        return new Data("Car/"+m_carIdentifier+"/DriverLicenseColorText",0xffffff/*white*/,"RGB",Data.State.NORMAL);
    }

    /**
     * Returns the name of the current driver of the car.
     * 
     * <p>PATH = {@link #getDriverName() /Car/(CARIDENTIFIER)/DriverName}
     * 
     * @param allowMapping true allows the user to map the name in settings to something else.
     * @return The name in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getDriverName(boolean allowMapping) {
        return new Data("Car/"+m_carIdentifier+"/DriverName","","text",Data.State.NORMAL);
    }
    public Data getDriverName(String allowName) { return getDriverName(new Data("",allowName).getBoolean()); }
    public Data getDriverName() { return getDriverName(true); }

    /**
     * Returns the name of the current driver of the car in a shortened format.
     * The actual format is up to each SIM and there's no set length.
     * It's just shorter than their whole name.
     * 
     * <p>PATH = {@link #getDriverNameShort() /Car/(CARIDENTIFIER)/DriverNameShort}
     * 
     * @param allowMapping true allows the user to map the name in settings to something else.
     * @return The drivers shortened name in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getDriverNameShort(boolean allowMapping) {
        return new Data("Car/"+m_carIdentifier+"/DriverNameShort","","text",Data.State.NORMAL);
    }
    public Data getDriverNameShort(String allowName) { return getDriverNameShort(new Data("",allowName).getBoolean()); }
    public Data getDriverNameShort() { return getDriverNameShort(true); }
    
    /**
     * Returns the rating of the current driver of the car.
     * Each SIM can define how a rating should be formed, so do not assume it is number.
     * Do not try to parse it to break it down, as a specific format is not guaranteed.
     * The rating is simply meant to be displayed.
     * 
     * <p>PATH = {@link #getDriverRating() /Car/(CARIDENTIFIER)/DriverRating}
     * 
     * @return The rating in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getDriverRating() {
        return new Data("Car/"+m_carIdentifier+"/DriverRating","0","rating");
    }

    /**
     * Returns the number of laps you can run on the existing fuel in the tank.
     * 
     * <p>PATH = {@link #getFuelLaps(int) /Car/(CARIDENTIFIER)/FuelLaps/(LAPSTOAVERAGE)}
     * 
     * @param lapsToAverage (Optional) The number of laps to average. This is passed to {@link com.SIMRacingApps.Car#getFuelLevelPerLap}. Default 0.
     * @return The number of laps in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getFuelLaps(int lapsToAverage) {
        Data fuel = _getGauge(Gauge.Type.FUELLEVEL).getValueCurrent().convertUOM("l");
        Data fuelperlap = getFuelLevelPerLap(lapsToAverage,"l");
        Data d;
        if (fuelperlap.getState().equals(Data.State.NORMAL) && fuelperlap.getDouble() > 0.0) {
            d = new Data("Car/"+m_carIdentifier+"/FuelLaps", fuel.getDouble() / fuelperlap.getDouble(),"lap",Data.State.NORMAL);
        }
        else
            d = new Data("Car/"+m_carIdentifier+"/FuelLaps", 0,"lap",Data.State.NOTAVAILABLE);

        return d;
    }

    public Data getFuelLaps(String sLapsToAverage) {
        return getFuelLaps(Integer.parseInt(sLapsToAverage));
    }
    public Data getFuelLaps() {
        return getFuelLaps(0);
    }
    
    /**
     * Returns the maximum number of laps you can go on a full fuel tank.
     * 
     * <p>PATH = {@link #getFuelLapsMaximum(int) /Car/(CARIDENTIFIER)/FuelLapsMaximum/(LAPSTOAVERAGE)}
     * 
     * @param lapsToAverage (Optional) The number of laps to average. This is passed to {@link com.SIMRacingApps.Car#getFuelLevelPerLap}. Default 0.
     * @return The number of laps in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getFuelLapsMaximum(int lapsToAverage) {
//        Data d = new Data("Car/"+m_carIdentifier+"/FuelLapsMaximum",0.0,"laps");
//        if (isME()) {
//            Data fuelperlap = getFuelLevelPerLap(lapsToAverage);
//            if (fuelperlap.getDouble() > 0.0) {
//                //floor instead of rounding because you can't make it to the next rounded up lap
//                d.setValue((int)Math.floor(getGauge(Gauge.Type.FUELLEVEL).getCapacityMaximum().getDouble() / fuelperlap.getDouble()),"lap");
//                d.setState(Data.State.NORMAL);
//            }
//        }
//        return d;
        Data fuel = _getGauge(Gauge.Type.FUELLEVEL).getCapacityMaximum().convertUOM("l");
        Data fuelperlap = getFuelLevelPerLap(lapsToAverage,"l");
        Data d;
        if (fuelperlap.getState().equals(Data.State.NORMAL) && fuelperlap.getDouble() > 0.0) {
            d = new Data("Car/"+m_carIdentifier+"/FuelLaps", fuel.getDouble() / fuelperlap.getDouble(),"lap",Data.State.NORMAL);
        }
        else
            d = new Data("Car/"+m_carIdentifier+"/FuelLaps", 0,"lap",Data.State.NOTAVAILABLE);

        return d;
    }
    public Data getFuelLapsMaximum(String lapsToAverage) {
        return getFuelLapsMaximum(Integer.parseInt(lapsToAverage));
    }
    public Data getFuelLapsMaximum() {
        return getFuelLapsMaximum(DEFAULT_LAPS_FUEL_MILAGE);
    }

    /**
     * Returns the fuel level the last time the car crossed the start/finish line.
     * 
     * <p>PATH = {@link #getFuelLevelAtStartFinish(String) /Car/(CARIDENTIFIER)/FuelLevelAtStartFinish/(UOM)}
     * 
     * @param UOM (Optional) A unit of measure to return. Set to null or a blank string to use SIM's default UOM. Default blank to use car's UOM.
     * @return The fuel level in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getFuelLevelAtStartFinish(String UOM) {
        return new Data("Car/"+m_carIdentifier+"/FuelLevelAtStartFinish",0.0,_getGauge(Gauge.Type.FUELLEVEL).getUOM().getString(),Data.State.NOTAVAILABLE).convertUOM(UOM);
    }
    public Data getFuelLevelAtStartFinish() { return getFuelLevelAtStartFinish(""); }

    /**
     * Returns the total amount of fuel needed to finish the race regardless of how much is in the fuel tank.
     * 
     * If the number of laps is not specified and you are in a RACE session, 
     * it will use your own laps to go.
     * 
     * <p>PATH = {@link #getFuelLevelToFinish(int, double, String) /Car/(CARIDENTIFIER)/FuelLevelToFinish/(LAPSTOAVERAGE)/(LAPS)/(UOM)}
     * 
     * @param lapsToAverage (Optional) The number of laps to average. This is passed to {@link com.SIMRacingApps.Car#getFuelLevelPerLap}. Default 0.
     * @param laps          (Optional) The number of laps to calculate for. Defaults to remaining minus percentage of current lap completed.
     * @param UOM           (Optional) A unit of measure to return. Set to null or a blank string to use Car's UOM.
     * @return The number of laps in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getFuelLevelToFinish(int lapsToAverage,double laps, String UOM) {
        Data fuelneeded = new Data("Car/"+m_carIdentifier+"/FuelLevelToFinish",0.0,_getGauge(Gauge.Type.FUELLEVEL).getUOM().getString(),Data.State.NOTAVAILABLE);
        if (isME()) {
            Data fuelperlap = getFuelLevelPerLap(lapsToAverage,"");

            double lapsRemaining = laps;
            
            //if number of laps is not specified, then calculate the remaining distance
            if (Double.isNaN(lapsRemaining) || lapsRemaining <= 0.0) {
//not going to use the leaders laps, just causes too much confusion.                
//                lapsRemaining = m_SIMPlugin.getSession().getType().getString().equalsIgnoreCase(Session.Type.RACE)
//                              ? m_SIMPlugin.getSession().getCar("LEADER").getLapsToGo().getDouble()
//                              : getLapsToGo().getDouble();
                lapsRemaining = getLapsToGo().getDouble();
                double percentComplete = /*m_SIMPlugin.getSession().getType().getString().equalsIgnoreCase(Session.Type.RACE)
                                       ? m_SIMPlugin.getSession().getCar("LEADER").getLap(LapType.COMPLETEDPERCENT).getDouble()
                                       : */getLap(LapType.COMPLETEDPERCENT).getDouble();
                
                //compensate for the leader cross the line before I have
                //If so, add the distance to get to the line.
                //otherwise, subtract the distance I am from the line
                if (getLap(LapType.COMPLETEDPERCENT).getDouble() > percentComplete) //if ahead of leader, relative wise
                    lapsRemaining += (1.0 - (getLap(LapType.COMPLETEDPERCENT).getDouble() / 100.0)); //then add distance to finish line
                else
                    lapsRemaining -= (getLap(LapType.COMPLETEDPERCENT).getDouble() / 100.0); //else subtract distance from finish line
            }
            
            if (lapsRemaining > 0.0 && fuelperlap.getDouble() > 0.0) {
                double fueltoadd = (fuelperlap.getDouble() * (lapsRemaining + FUELLEVELNEEDED_BUFFER_LAPS));

                fuelneeded.setValue(fueltoadd > 0.0 ? fueltoadd : 0.0);
                fuelneeded.setState(Data.State.NORMAL);
            }
        }
        return fuelneeded.convertUOM(UOM);
    }
    public Data getFuelLevelToFinish(String lapsToAverage, String lapsRemaining, String UOM) {
        return getFuelLevelToFinish(Integer.parseInt(lapsToAverage),Integer.parseInt(lapsRemaining),UOM);
    }
    public Data getFuelLevelToFinish(String lapsToAverage, String lapsRemaining) {
        return getFuelLevelToFinish(Integer.parseInt(lapsToAverage),Integer.parseInt(lapsRemaining),"");
    }
    public Data getFuelLevelToFinish(String lapsToAverage) {
        return getFuelLevelToFinish(Integer.parseInt(lapsToAverage),0,"");
    }
    public Data getFuelLevelToFinish() {
        return getFuelLevelToFinish(Integer.toString(DEFAULT_LAPS_FUEL_MILAGE));
    }
    

    /**
     * Returns the amount of fuel you need to add to complete the number of laps specified.
     * It calls {@link #getFuelLevelToFinish(int, double, String)}, then subtracts what is in your fuel tank.
     * The amount could be more than will fit in the fuel cell.
     * 
     * <p>PATH = {@link #getFuelLevelNeeded(int, double, String) /Car/(CARIDENTIFIER)/FuelLevelNeeded/(LAPSTOAVERAGE)/(LAPS)/(UOM)}
     * 
     * @param lapsToAverage (Optional) The number of laps to average. This is passed to {@link com.SIMRacingApps.Car#getFuelLevelPerLap}. Default 0.
     * @param laps          (Optional) The number of laps to calculate for. Defaults to laps remaining in the race minus current lap percent completed.
     * @param UOM           (Optional) A unit of measure to return. Set to null or a blank string to use SIM's default UOM.
     * @return              The number of laps in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getFuelLevelNeeded(int lapsToAverage,double laps,String UOM) {
        Data fuelneeded = new Data("Car/"+m_carIdentifier+"/FuelLevelNeeded",0.0,_getGauge(Gauge.Type.FUELLEVEL).getUOM().getString(),Data.State.NOTAVAILABLE);
        if (isME()) {

            Data fuelToFinish = getFuelLevelToFinish(lapsToAverage,laps,UOM);
            
            if (fuelToFinish.getDouble() > 0.0) {
                double currentFuelLevel = _getGauge(Gauge.Type.FUELLEVEL).getValueCurrent().getDouble();
                double fueltoadd = fuelToFinish.getDouble() - currentFuelLevel;
                fuelneeded.setValue(fueltoadd > 0.0 ? fueltoadd : 0.0);
                fuelneeded.setState(Data.State.NORMAL);
            }
            else
                fuelneeded.setState(Data.State.OFF);
        }
        return fuelneeded.convertUOM(UOM);
    }
    public Data getFuelLevelNeeded(String lapsToAverage, String laps, String UOM) {
        return getFuelLevelNeeded(Integer.parseInt(lapsToAverage),Integer.parseInt(laps),UOM);
    }
    public Data getFuelLevelNeeded(String lapsToAverage, String laps) {
        return getFuelLevelNeeded(Integer.parseInt(lapsToAverage),Integer.parseInt(laps),"");
    }
    public Data getFuelLevelNeeded(String lapsToAverage) {
        return getFuelLevelNeeded(Integer.parseInt(lapsToAverage),0.0,"");
    }
    public Data getFuelLevelNeeded() {
        return getFuelLevelNeeded(Integer.toString(DEFAULT_LAPS_FUEL_MILAGE));
    }

    /**
     * Returns the amount of fuel being consumed per lap.
     * 
     * <p>PATH = {@link #getFuelLevelPerLap(int, String) /Car/(CARIDENTIFIER)/FuelLevelPerLap/(LAPSTOAVERAGE)/(UOM)}
     * 
     * @param lapsToAverage (Optional) The number of previous non-caution laps to average. Zero indicates use the worst lap. Default 0.
     * @param UOM           (Optional) A unit of measure to return. Set to null or a blank string to use car's UOM.
     * @return The number of laps in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getFuelLevelPerLap(int lapsToAverage,String UOM) {
        return new Data("Car/"+m_carIdentifier+"/FuelPerLap",0.0,"l",Data.State.NORMAL).convertUOM(UOM);
    }
    public Data getFuelLevelPerLap(String lapsToAverage,String UOM) {
        return getFuelLevelPerLap(Integer.parseInt(lapsToAverage),UOM);
    }    
    public Data getFuelLevelPerLap(String lapsToAverage) {
        return getFuelLevelPerLap(Integer.parseInt(lapsToAverage),"");
    }    
    public Data getFuelLevelPerLap() {
        return getFuelLevelPerLap(DEFAULT_LAPS_FUEL_MILAGE,"");
    }    

    /**
     * Returns an instance of the Gauge Type specified for this car.
     * If the car does not contain a gauge of this type, it returns a generic gauge.
     * @param gaugeType One of the enumerated types defined by {@link com.SIMRacingApps.Gauge.Type}
     * @return          A Gauge defined by {@link com.SIMRacingApps.Gauge}
     */
    public Gauge _getGauge(String gaugeType) {
        Gauge gauge = m_gauges.get(gaugeType.toLowerCase());
        if (gauge != null)
            return gauge;
        
//        if (!gaugeType.toLowerCase().equals("generic"))
//            Server.logger().warning(String.format("Car._getGauge(%s) not a valid Gauge.Type, returning Generic", gaugeType));

        //TODO: Need to validate gaugeType is supported, else return a generic gauge.
        return new Gauge(
                gaugeType,
                this,
                m_SIMPlugin.getSession().getTrack(),
                null,null
        );
    }

    /**
     * Returns a Data object of the Gauge Type specified for this car as a json string.
     * If the car does not contain a gauge of this type, it returns a generic gauge.
     * 
     * <p>PATH = {@link #getGauge(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)}
     * 
     * @param gaugeType (Optional) One of the enumerated types defined by {@link com.SIMRacingApps.Gauge.Type}, default All.
     * @return          A Gauge defined by {@link com.SIMRacingApps.Gauge}
     */
    public Data getGauge(String gaugeType) {
        return new Data("Car/"+m_carIdentifier+"/Gauge/"+gaugeType,_getGauge(gaugeType).toString());
    }
    public Data getGauge() {
        int count = 0;
        StringBuffer json = new StringBuffer();

        json.append("{");
        Iterator<Entry<String,Gauge>> itr = m_gauges.entrySet().iterator();
        while (itr.hasNext()) {
            Gauge gauge = itr.next().getValue();
            if (count++ > 0)
                json.append(",");
            json.append("\"");
            json.append(gauge.getType().getString());
            json.append("\":");
            json.append(gauge.toString());
        }
        json.append("}");
        return new Data("Car/"+m_carIdentifier+"/Gauge",json.toString(),"JSON",Data.State.NORMAL);
    }
    
    private boolean m_shiftLightsLoaded = false;
    /**
     * Assigns a gauge instance to this car.
     * If one already exists, it is replaced
     * @param gauge A instance of a Gauge defined by {@link com.SIMRacingApps.Gauge}
     */
    protected void _setGauge(Gauge gauge) {
        
        if (gauge.getType().getString().equalsIgnoreCase(Gauge.Type.SPEEDOMETER))
        {
            //convert the track UOM to the gauges UOM
            //I used to floor it and round it, but that produced problems with accuracy.
            //This code cannot make any assumptions about the error the track code may return.
            //In my test file, the speed limit is a published 45mph, but the track code returns 44.7.
            //My point is, the track code should round it up or floor it, not this code.
            double PitRoadSpeedLimit = m_SIMPlugin.getSession().getTrack().getPitSpeedLimit(gauge.getUOM().getString()).getDouble();

            //double WayOverPitSpeed     = 1.10;
            double WayOverPitSpeed     = (PitRoadSpeedLimit + (gauge.getUOM().equals("mph") ? 15.0 : 25.0)) / PitRoadSpeedLimit;
            double OverPitSpeed        = (PitRoadSpeedLimit + (gauge.getUOM().equals("mph") ? 0.8  : 1.29)) / PitRoadSpeedLimit;
            double PitSpeed            = (PitRoadSpeedLimit - (gauge.getUOM().equals("mph") ? 0.5  : 0.8))  / PitRoadSpeedLimit;
            double ApproachingPitSpeed = PitSpeed - (7*.012) - (7*.006);

            gauge._addStateRange("","WAYOVERLIMIT",     PitRoadSpeedLimit * WayOverPitSpeed,     Double.MAX_VALUE,                    gauge.getUOM().getString());
            gauge._addStateRange("","OVERLIMIT",        PitRoadSpeedLimit * OverPitSpeed,        PitRoadSpeedLimit * WayOverPitSpeed, gauge.getUOM().getString());
            gauge._addStateRange("","LIMIT",            PitRoadSpeedLimit * PitSpeed,            PitRoadSpeedLimit * OverPitSpeed,    gauge.getUOM().getString());
            gauge._addStateRange("","APPROACHINGLIMIT", PitRoadSpeedLimit * ApproachingPitSpeed, PitRoadSpeedLimit * PitSpeed,        gauge.getUOM().getString());

        }

        if (!m_shiftLightsLoaded) {
            try {
                Gauge tachometer = _getGauge(Gauge.Type.TACHOMETER);
                Gauge gear  = _getGauge(Gauge.Type.GEAR);
                Gauge power = _getGauge(Gauge.Type.ENGINEPOWER);
                
                if (tachometer != null && gear != null && power != null) {
                    String car  = getName().getString().replace(" ", "_");
                    String track= m_SIMPlugin.getSession().getTrack().getName().getString().replace(" ", "_");
        
                    //allow the user to override the shift light RPM values
                    //example:
                    //
                    //stockcars_chevyss-ShiftLightStart = 6000
                    //stockcars_chevyss-ShiftLightShift = 7000
                    //stockcars_chevyss-ShiftLightBlink = 8000
                    
                    double DriverCarSLFirstRPM = Server.getArg(
                                                    String.format(              "%s-%s-ShiftLightStart-%s-%d", track,car,gear.getValueCurrent().getString(),power.getValueCurrent().getInteger()),
                                                    Server.getArg(String.format("%s-%s-ShiftLightStart-%s",    track,car,gear.getValueCurrent().getString()),
                                                    Server.getArg(String.format("%s-%s-ShiftLightStart",       track,car),
                                                    Server.getArg(String.format("%s-ShiftLightStart-%s-%d",          car,gear.getValueCurrent().getString(),power.getValueCurrent().getInteger()),
                                                    Server.getArg(String.format("%s-ShiftLightStart-%s",             car,gear.getValueCurrent().getString()),
                                                    Server.getArg(String.format("%s-ShiftLightStart",                car),      -1.0)
                                                 )))));
                    double DriverCarSLShiftRPM = Server.getArg(
                                                    String.format(              "%s-%s-ShiftLightShift-%s-%d", track,car,gear.getValueCurrent().getString(),power.getValueCurrent().getInteger()),
                                                    Server.getArg(String.format("%s-%s-ShiftLightShift-%s",    track,car,gear.getValueCurrent().getString()),
                                                    Server.getArg(String.format("%s-%s-ShiftLightShift",       track,car),
                                                    Server.getArg(String.format("%s-ShiftLightShift-%s-%d",          car,gear.getValueCurrent().getString(),power.getValueCurrent().getInteger()),
                                                    Server.getArg(String.format("%s-ShiftLightShift-%s",             car,gear.getValueCurrent().getString()),
                                                    Server.getArg(String.format("%s-ShiftLightShift",                car),      -1.0)
                                                 )))));
                    double DriverCarSLBlinkRPM = Server.getArg(
                                                    String.format(              "%s-%s-ShiftLightBlink-%s-%d", track,car,gear.getValueCurrent().getString(),power.getValueCurrent().getInteger()),
                                                    Server.getArg(String.format("%s-%s-ShiftLightBlink-%s",    track,car,gear.getValueCurrent().getString()),
                                                    Server.getArg(String.format("%s-%s-ShiftLightBlink",       track,car),
                                                    Server.getArg(String.format("%s-ShiftLightBlink-%s-%d",          car,gear.getValueCurrent().getString(),power.getValueCurrent().getInteger()),
                                                    Server.getArg(String.format("%s-ShiftLightBlink-%s",             car,gear.getValueCurrent().getString()),
                                                    Server.getArg(String.format("%s-ShiftLightBlink",                car),      -1.0)
                                                 )))));
                    
                    if (DriverCarSLFirstRPM > 0.0 && DriverCarSLShiftRPM > 0.0 && DriverCarSLBlinkRPM > 0.0) {
                        gauge._addStateRange("","SHIFTLIGHTS",            DriverCarSLFirstRPM,                  DriverCarSLShiftRPM, "rev/min");
                        gauge._addStateRange("","SHIFT",                  DriverCarSLShiftRPM,                  DriverCarSLBlinkRPM, "rev/min");
                        gauge._addStateRange("","SHIFTBLINK",             DriverCarSLBlinkRPM,                  999999.0,            "rev/min");
        
                        Server.logger().info(String.format("Shift Point from user: First=%.0f, Shift=%.0f, Blink=%.0f",
                                DriverCarSLFirstRPM,DriverCarSLShiftRPM,DriverCarSLBlinkRPM));
                    }
                }
            }
            catch (NumberFormatException e) {}
            m_shiftLightsLoaded = true; 
        }
        
        m_gauges.put(gauge.getType().getString().toLowerCase(), gauge);
    }

    /**
     * Returns if the SIM supports sending pit commands remotely.
     * Each SIM should override this method and return a Y, if it does.
     * The default is N.
     * 
     * <p>PATH = {@link #getHasAutomaticPitCommands() /Car/(CARIDENTIFIER)/HasAutomaticPitCommands}
     * 
     * @return true or false in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getHasAutomaticPitCommands() {
        return new Data("Car/"+m_carIdentifier+"/HasAutoMaticPitCommands",false,"Boolean",Data.State.NOTAVAILABLE);
    }

    /**
     * Returns the numeric id of this car.
     * 
     * <p>PATH = {@link #getId() /Car/(CARIDENTIFIER)/Id}
     * 
     * @return Id in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getId() {
        return new Data("Car/"+m_carIdentifier+"/Id",m_id,"id",Data.State.NOTAVAILABLE);
    }

    /**
     * Returns a URL to this car's image suitable for using in the "src" attribute of the "img" tag.
     * The beginning of the URL will be the name of the SIM. 
     * You will have to replace this with the [protocol://]host[:port] of where to locate the image for the SIM.
     * <p>
     * Using the SIMRacingApps Server, you can prefix "/SIMRacingApps/" + getImageUrl().getString() and call the server.   
     * Same for HTML5/JavaScript clients, just assign to the "src" attribute of the "img" tag.
     * 
     * <p>PATH = {@link #getImageUrl() /Car/(CARIDENTIFIER)/ImageUrl}
     * 
     * @return URL to image of this car
     */
    public Data getImageUrl() {
        return new Data("Car/"+m_carIdentifier+"/ImageUrl","Resource/com/SIMRacingApps/Car.png","URL",Data.State.NORMAL);
    }
    
    /**
     * Returns the number of incidents for the driver of this car.
     * 
     * Different SIMs may measure these in different units of measure. The default is "x".
     * 
     * <p>PATH = {@link #getIncidents() /Car/(CARIDENTIFIER)/Incidents}
     * 
     * @return The number of incidents for the driver of this car in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getIncidents() {
        Data d = new Data("Car/"+m_carIdentifier+"/Incidents",0,"x",Data.State.NOTAVAILABLE);
        return d;
    }
    
    /**
     * Returns the number of incidents for this car's team.
     * If this is not a team session, it returns zero with the state set to OFF.
     * 
     * Different SIMs may measure these in different units of measure. The default is "x".
     * 
     * <p>PATH = {@link #getIncidentsTeam() /Car/(CARIDENTIFIER)/IncidentsTeam}
     * 
     * @return The number of incidents for this car's team in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getIncidentsTeam() {
        Data d = new Data("Car/"+m_carIdentifier+"/IncidentsTeam",0,"x",Data.State.NOTAVAILABLE);
        return d;
    }
    
    /**
     * This returns (true) if the car is below the minimum speed.
     * The following variables can be set via the arguments if you do not agree with these defaults.
     * <pre>
     *     -minimum-speed-laps         = 3
     *     -minimum-speed-laps-leader  = 3
     *     -minimum-speed-percentage   = 110
     * </pre>
     * The minimum speed uses lap times and is calculated by averaging the last (minimum-speed-laps-leader) green flag laps 
     * of the leader, multiplied by (minimum-speed-percentage). 
     * For example: if the average is 50 seconds, the minimum would be 55 seconds. 
     * 
     * After the car has completed at least the (minimum-speed-laps) since the last pit stop,
     * and they are still on the track, each of the previous (minimum-speed-laps) are checked.
     * If any of them are below the maximum lap time, then false is returned to indicate the car is okay.
     * So, if all of them are above the maximum lap time, true is returned to indicate the car is too slow. 
     * 
     * NASCAR's minimum is the Poll Speed or whatever is appropriate at that point in the race, multiplied by 110%. 
     * So, to simulate "whatever is appropriate" automatically, the last (minimum-speed-laps-leader) laps of the leader is used.
     *
     * <p>PATH = {@link #getIsBelowMinimumSpeed /Car/(CARIDENTIFIER)/IsBelowMinimumSpeed}
     * 
     * @return true or false in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getIsBelowMinimumSpeed() {
        Data d = new Data("Car/"+m_carIdentifier+"/IsBelowMinimumSpeed",false,"boolean",Data.State.NORMAL);
        int lapPitted                        = getLap(LapType.PITTED).getInteger();
        int lapCurrent                       = getLap(LapType.CURRENT).getInteger();
        boolean useQualifyingTime            = this.m_SIMPlugin.getSession().getType().getString().equals(Session.Type.LONE_QUALIFY)
                                            || this.m_SIMPlugin.getSession().getType().getString().equals(Session.Type.OPEN_QUALIFY)
                                            || (this.m_SIMPlugin.getSession().getType().getString().equals(Session.Type.RACE) && this.m_SIMPlugin.getSession().getLap().getInteger() <= 1);
//useQualifyingTime = true;        
        //if not the leader of your class and you're on the track
        if (useQualifyingTime || (!getIsEqual("PC1").getBoolean() && getStatus().getString().contains("TRACK"))) {
            int laps                             = useQualifyingTime ? 1 : Server.getArg("minimum-speed-laps",3);
            int lapsLeader                       = useQualifyingTime ? 1 : Server.getArg("minimum-speed-laps-leader",3);
            double percentage                    = Server.getArg("minimum-speed-percentage",110.0) / 100.0;
            
//if (getIsEqual("22").getBoolean())        
//    laps = laps;
                             
            if ((useQualifyingTime && getId().getInteger() > -1) 
            || (   lapCurrent > (lapPitted + laps)
                && !m_SIMPlugin.getSession().getIsCautionFlag().getBoolean()
                && !m_SIMPlugin.getSession().getIsRedFlag().getBoolean()
                && getId().getInteger() > -1
               )
            ) {
                double timeLeader                    = 0.0;

                //get the leader for the class of this car
                Car leader                           = m_SIMPlugin.getSession().getCar(CarIdentifiers.LEADER_PREFIX + "I" + getId().getString());
                if (useQualifyingTime) {
                    
                    timeLeader = leader.getLapTime(Car.LapType.QUALIFYING).getDouble();
                    
                    double time = getLapTime(Car.LapType.QUALIFYING).getDouble();
                    
                    if (time > 0.0 && time <= (timeLeader * percentage))
                        return d;
                    
                    d.setValue(true);
                }
                else {
                    ArrayList<Double> lapTimesLeader     = leader.getLapTimes().getDoubleArray();
                    ArrayList<Boolean> lapsInvalidLeader = leader.getLapInvalidFlags().getBooleanArray();
                    ArrayList<Double> lapTimes           = getLapTimes().getDoubleArray();
                    ArrayList<Boolean> lapsInvalid       = getLapInvalidFlags().getBooleanArray();
                    int count;
                    int lap;
                    
                    //get the average lap time for the leader
                    for (lap = lapTimesLeader.size() - 1, count=0
                         ;
                            lap >= 0 
                         && count < lapsLeader 
                         && lap < lapsInvalidLeader.size()
                         ;
                         lap--
                    ) {
                        if (!lapsInvalidLeader.get(lap)
                        && lapTimesLeader.get(lap) > 0.0
                        ) {
                            timeLeader += lapTimesLeader.get(lap);
                            count++;
                        }
                    }
                
                    if (count == lapsLeader) {
                        timeLeader /= count;    //convert to an average.
    
                        //check the lap times against the valid laps that have occurred since pitting.
                        for (lap = lapTimes.size() - 1, count=0
                             ; 
                                lap >= 0 
                             && (lap + 1) > lapPitted
                             && count < laps
                             && lap < lapsInvalid.size() 
                             && !lapsInvalid.get(lap)
                             //&& lapTimes.get(lap) > 0.0
                             ; 
                             lap--
                        ) {
                            //if any lap is fast enough, then we won't flag them.
                            //laps zero and below are not good laps.
                            if (lapTimes.get(lap) > 0.0 && lapTimes.get(lap) <= (timeLeader * percentage))
                                return d;
                            count++;
                        }
                    
                        //if we have enough laps to flag them
                        if (count == laps) {
                            d.setValue(true);
                        }
                    }
                }
            }
        }
        
//if (getIsEqual("61").getBoolean())        
//    d.setValue(true);

        return d;
    }

    /**
     * Returns true if the black flag has been waved for this car.
     * This flag means come to the pits to serve a penality.
     * 
     * <p>PATH = {@link #getIsBlackFlag() /Car/(CARIDENTIFIER)/IsBlackFlag}
     * @return true or false in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getIsBlackFlag() {
        return new Data("Car/"+m_carIdentifier+"/IsBlackFlag",false,"boolean",Data.State.NORMAL);
    }
    
    /**
     * Returns true if the blue flag has been waved for this car.
     * This flag means move out of the racing grove to give way to the leaders.
     * 
     * <p>PATH = {@link #getIsBlueFlag() /Car/(CARIDENTIFIER)/IsBlueFlag}
     * @return true or false in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getIsBlueFlag() {
        return new Data("Car/"+m_carIdentifier+"/IsBlueFlag",false,"boolean",Data.State.NORMAL);
    }
    
    /**
     * Returns true if the disqualify flag has been waved for this car.
     * This means you must exist the race.
     * 
     * <p>PATH = {@link #getIsDisqualifyFlag() /Car/(CARIDENTIFIER)/IsDisqualifyFlag}
     * @return true or false in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getIsDisqualifyFlag() {
        return new Data("Car/"+m_carIdentifier+"/IsDisqualifyFlag",false,"boolean",Data.State.NORMAL);
    }
    
    /**
     * Returns true if are driving this car. 
     * The car could be on the track, but you are not in it.
     * In team sessions, it could be one of your teammates.
     * 
     * <p>PATH = {@link #getIsDriving /Car/(CARIDENTIFIER)/IsDriving}
     * 
     * @return true or false in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getIsDriving() {
        return new Data("Car/"+m_carIdentifier+"/IsDriving",false,"boolean",Data.State.NORMAL);
    }
    
    /**
     * Returns true if the 2 cars refer to the same car
     * 
     * <p>PATH = {@link #getIsEqual(String) /Car/(CARIDENTIFIER)/IsEqual/(CARIDENTIFIER)}
     * @param carIdentifier (Optional) Car as defined by {@link com.SIMRacingApps.Session#getCar(String)}. Default REFERENCE.
     * @return true or false in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getIsEqual(String carIdentifier) {
        Data d = new Data("Car/"+m_carIdentifier+"/IsEqual/"+carIdentifier,false,"boolean",Data.State.NORMAL);
        Car car1 = m_SIMPlugin.getSession().getCar(carIdentifier);
        if (car1 != null && car1.isValid() && this.isValid() && car1.getId().getInteger() == m_id)
            d.setValue(true,"boolean",Data.State.NORMAL);
        return d;
    }
    public Data getIsEqual() {
        return getIsEqual("REFERENCE");
    }

    /**
     * Returns true if the car is in a fixed setup session.
     * 
     * <p>PATH = {@link #getIsFixedSetup /Car/(CARIDENTIFIER)/IsFixedSetup}
     * 
     * @return true or false in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getIsFixedSetup() {
        return new Data("Car/"+m_carIdentifier+"/IsFixedSetup",false,"boolean",Data.State.NOTAVAILABLE);
    }

    /**
     * Returns true if the car is over the specified speed limit.
     * 
     * <p>PATH = {@link #getIsOverSpeedLimit() /Car/(CARIDENTIFIER)/IsOverSpeedLimit}
     * 
     * @return true or false in a {@link com.SIMRacingApps.Data} container.
     *         <dl>
     *         <dt>{@link com.SIMRacingApps.Data#getState}</dt>
     *         <dt>OVERLIMIT</dt><dd>Set if you will only get an End Of Line penalty for speeding</dd>
     *         <dt>WAYOVERLIMIT</dt><dd>Set if you will get a black flag for excessive speeding and have to serve a stop and go under green</dd>
     *         </dl>
     */
    public Data getIsOverSpeedLimit() {
        Data d = new Data("Car/"+m_carIdentifier+"/IsOverSpeedLimit",false,"boolean");
        String state = _getGauge(Gauge.Type.SPEEDOMETER).getValueCurrent().getState();
        if (state.equals("OVERLIMIT")
        ||  state.equals("WAYOVERLIMIT")
           ) d.setValue(true);

        d.setState(Data.State.NORMAL);
        return d;
//        return new Data("Car/"+m_carIdentifier+"/IsOverPitSpeedLimit",false,"boolean");
    }

    /**
     * Returns true if the car has the pit speed limiter on.
     * 
     * <p>PATH = {@link #getIsPitSpeedLimiter() /Car/(CARIDENTIFIER)/IsPitSpeedLimiter}
     * 
     * @return true or false in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getIsPitSpeedLimiter() {
        return new Data("Car/"+m_carIdentifier+"/IsPitSpeedLimiter",false,"boolean",Data.State.NOTAVAILABLE);
    }

    /**
     * Returns true if the car is the pace car.
     * 
     * <p>PATH = {@link #getIsPaceCar() /Car/(CARIDENTIFIER)/IsPaceCar}
     * 
     * @return true or false in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getIsPaceCar() {
        return new Data("Car/"+m_carIdentifier+"/IsPaceCar",false,"boolean",Data.State.NORMAL);
    }

    /**
     * Returns true if the car is a spectator.
     * 
     * <p>PATH = {@link #getIsSpectator() /Car/(CARIDENTIFIER)/IsSpectator}
     * 
     * @return true or false in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getIsSpectator() {
        return new Data("Car/"+m_carIdentifier+"/IsSpectator",false,"boolean",Data.State.NOTAVAILABLE);
    }

    /**
     * Returns true if the car is under the specified speed limit.
     * 
     * <p>PATH = {@link #getIsUnderSpeedLimit() /Car/(CARIDENTIFIER)/IsUnderSpeedLimit}
     * 
     * @return true or false in a {@link com.SIMRacingApps.Data} container.
     *         <dl>
     *         <dt>{@link com.SIMRacingApps.Data#getState}</dt>
     *         <dt>APPROACHINGLIMIT</dt><dd>Set if you just below the speed limit</dd>
     *         </dl>
     */
    public Data getIsUnderSpeedLimit() {
        Data d = new Data("Car/"+m_carIdentifier+"/IsUnderPitSpeedLimit",false,"boolean");
        String state = _getGauge(Gauge.Type.SPEEDOMETER).getValueCurrent().getState();
        if (state.equals("APPROACHINGLIMIT"))
            d.setValue(true);
        d.setState(Data.State.NORMAL);
        return d;
//      return new Data("Car/"+m_carIdentifier+"/IsUnderPitSpeedLimit",false,"boolean");
    }

    /**
     * Returns true if the yellow flag has been waved for this car.
     * This is not a full course caution flag. 
     * It is for a local issue near this car.
     * 
     * <p>PATH = {@link #getIsYellowFlag() /Car/(CARIDENTIFIER)/IsYellowFlag}
     * @return true or false in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getIsYellowFlag() {
        return new Data("Car/"+m_carIdentifier+"/IsYellowFlag",false,"boolean",Data.State.NORMAL);
    }
    
    /**
     * Returns the lap based on the specified reference type.
     * Subclasses should extract the validated reference value from Data to use in deciding the return value.
     * <p>
     * Supported Reference Types are: AVERAGE, AVERAGESINCEPITTING, BEST, CAUTION, COMPLETED, COMPLETEDPERCENT, CURRENT, LED, PITTED, QUALIFYING, SESSIONBEST, SESSIONLAST, SINCEPITTING
     * 
     * <p>PATH = {@link #getLap(String,int) /Car/(CARIDENTIFIER)/Lap/(LAPTYPE)/(LAPSTOAVERAGE)}
     * 
     * @param lapType (Optional) as defined by {@link com.SIMRacingApps.Car.LapType}. Default CURRENT.
     * @param lapsToAverage (Optional) as defined by {@link com.SIMRacingApps.Car.LapType}. Default 9999.
     * @return lap in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getLap(String lapType,int lapsToAverage) {
        String s = LapType.getReference(lapType);
        Data d = new Data("Car/"+m_carIdentifier+"/Lap/"+lapType+"/"+lapsToAverage,0,"lap",Data.State.NORMAL);
        d.add("reference",s);
        return d;
    }
    public Data getLap(String lapType,String lapsToAverage) {
        return getLap(lapType,Integer.parseInt(lapsToAverage));
    }
    public Data getLap(String lapType) {
        return getLap(lapType,9999);
    }
    public Data getLap() {
        return getLap(LapType.CURRENT);
    }

    /**
     * Returns the number of laps this car has remaining.
     * It includes the laps down.
     * If a time bound session, the the number of laps remaining are adjusted based on an average lap time.
     * If you want the number of laps remaining in the session, use the leaders laps to go.
     * 
     * <p>PATH = {@link #getLapsToGo() /Car/(CARIDENTIFIER)/LapsToGo}
     * 
     * @return The number of laps remaining in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getLapsToGo() {
        Data d = new Data("Car/"+m_carIdentifier+"/LapsToGo",0,"lap",Data.State.NOTAVAILABLE);
        return d;
    }
    
    /**
     * Returns the time of the lap based on the specified reference type.
     * Subclasses should extract the validated reference value from Data to use in deciding the return value.
     * <p>
     * Supported Reference Types are: AVERAGE, AVERAGESINCEPITTING, BEST, CURRENT, FINISHLINE, QUALIFYING, RACESTART, SESSIONBEST, SESSIONLAST
     * 
     * <p>PATH = {@link #getLapTime(String,int) /Car/(CARIDENTIFIER)/LapTime/(LAPTYPE)/(LAPSTOAVERAGE)}
     * 
     * @param lapType (Optional) as defined by {@link com.SIMRacingApps.Car.LapType}. Default CURRENT.
     * @param lapsToAverage (Optional) as defined by {@link com.SIMRacingApps.Car.LapType}. Default 9999.
     * @return time in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getLapTime(String lapType, int lapsToAverage) {
        String s = LapType.getReference(lapType);
        Data d = new Data("Car/"+m_carIdentifier+"/LapTime/"+lapType+(lapsToAverage > 0 ? "/"+lapsToAverage : ""),0.0,"s",Data.State.NOTAVAILABLE);
        d.add("reference",s);
        return d;
    }
    public Data getLapTime(String lapType,String lapsToAverage) {
        return getLapTime(lapType,Integer.parseInt(lapsToAverage));
    }
    public Data getLapTime(String lapType) {
        return getLapTime(lapType,9999);
    }
    public Data getLapTime() {
        return getLapTime(LapType.CURRENT);
    }

    /**
     * Returns the time delta of the lap based on the specified reference type.
     * Subclasses should extract the validated reference value from Data to use in deciding the return value.
     * <p>
     * Supported Reference Types are: BEST, OPTIMAL, SESSIONBEST, SESSIONLAST, SESSIONOPTIMAL
     * 
     * <p>PATH = {@link #getLapTimeDelta(String) /Car/(CARIDENTIFIER)/LapTimeDelta/(LAPTYPE)}
     * 
     * @param lapType (Optional) as defined by {@link com.SIMRacingApps.Car.LapType}. Default CURRENT.
     * @return time in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getLapTimeDelta(String lapType) {
        String s = LapType.getReference(lapType);
        Data d = new Data("Car/"+m_carIdentifier+"/LapTimeDelta/"+lapType,0.0,"s",Data.State.NOTAVAILABLE);
        d.add("reference",s);
        return d;
    }
    public Data getLapTimeDelta() {
        return getLapTimeDelta(LapType.CURRENT);
    }

    /**
     * Returns the time delta as a percentage of the lap based on the specified reference type.
     * Subclasses should extract the validated reference value from Data to use in deciding the return value.
     * <p>
     * Supported Reference Types are: BEST, OPTIMAL, SESSIONBEST, SESSIONLAST, SESSIONOPTIMAL
     * 
     * <p>PATH = {@link #getLapTimeDeltaPercent(String) /Car/(CARIDENTIFIER)/LapTimeDeltaPercent/(LAPTYPE)}
     * 
     * @param lapType as defined by {@link com.SIMRacingApps.Car.LapType}
     * @return time in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getLapTimeDeltaPercent(String lapType) {
        String s = LapType.getReference(lapType);
        Data d = new Data("Car/"+m_carIdentifier+"/LapTimeDeltaPercent/"+lapType,0.0,"s",Data.State.NOTAVAILABLE);
        d.add("reference",s);
        return d;
    }
    public Data getLapTimeDeltaPercent() {
        return getLapTimeDeltaPercent(LapType.CURRENT);
    }

    /**
     * Returns the time delta reference of the lap based on the specified reference type.
     * The reference is used to calculate the projected time of the current lap.
     * Subclasses should extract the validated reference value from Data to use in deciding the return value.
     * <p>
     * Supported Reference Types are: BEST, OPTIMAL, SESSIONBEST, SESSIONLAST, SESSIONOPTIMAL
     * 
     * <p>PATH = {@link #getLapTimeDeltaReference(String) /Car/(CARIDENTIFIER)/LapTimeDeltaReference/(LAPTYPE)}
     * 
     * @param lapType as defined by {@link com.SIMRacingApps.Car.LapType}. Default CURRENT.
     * @return time in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getLapTimeDeltaReference(String lapType) {
        String s = LapType.getReference(lapType);
        Data d = new Data("Car/"+m_carIdentifier+"/LapTimeDeltaReference/"+lapType,0.0,"s",Data.State.NOTAVAILABLE);
        d.add("reference",s);
        return d;
    }
    public Data getLapTimeDeltaReference() {
        return getLapTimeDeltaReference(LapType.CURRENT);
    }

    /**
     * Estimates what the current lap time will be based on your current speed against your previous lap time.
     * 
     * <p>PATH = {@link #getLapTimeProjected() /Car/(CARIDENTIFIER)/LapTimeProjected}
     * 
     * @return The Lap Time Estimate in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getLapTimeProjected() {
        double pct = getLap(Car.LapType.COMPLETEDPERCENT).getDouble() / 100.0;
        double timeProjection = 0.0;

        //get the track length and speed in the same unit of measure
        Data carspeed    = _getGauge(Gauge.Type.SPEEDOMETER).getValueCurrent().convertUOM("mph");

        //TODO: For better road course times, use segment times. Use percent left of the current segment + remaining segments LAST time
        double remainingTime = (getLapTime(Car.LapType.SESSIONLAST).getDouble() * (1.0 - pct));

        //while on pit road, use speed if we can get it, otherwise use last lap time
        //or of this is the pace car, use speed
        if ((remainingTime <= 0.0 || getStatus().getString().contains("PIT") || getIsPaceCar().getBoolean()) && Math.floor(carspeed.getDouble()) > 0.0) {
            Data tracklength = m_SIMPlugin.getSession().getTrack().getLength().convertUOM("mile");
            timeProjection = ( (tracklength.getDouble() / carspeed.getDouble()) * 60 * 60 );
        }
        else {
            if (remainingTime > 0.0)
                timeProjection  = remainingTime + getLapTime(Car.LapType.CURRENT).getDouble();
        }

        Data d = new Data("Car/"+m_carIdentifier+"/LapTimeProjected",timeProjection,"s");
        d.setState(State.NORMAL);
        return d;
    }

    /**
     * Returns the lap time of each lap in in an array
     * The array is indexed as zero based.
     * This means to get the time for lap 1, look at array[0].
     * 
     * <p>PATH = {@link #getLapTimes() /Car/(CARIDENTIFIER)/LapTimes}
     * 
     * @return The lap times array in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getLapTimes() {
        ArrayList<Double> a = new ArrayList<Double>();
        return new Data("Car/"+m_carIdentifier+"/LapTimes",a,"lap",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Returns the invalid status of each lap in in an array.
     * false means good green flag lap, true means under yellow, or pitted.
     * Mainly used to determine the good laps to use in averages.
     * 
     * The array is indexed as zero based.
     * This means to get the flag for lap 1, look at array[0].
     * 
     * <p>PATH = {@link #getLapInvalidFlags() /Car/(CARIDENTIFIER)/LapInvalidFlags}
     * 
     * @return The valid flags array in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getLapInvalidFlags() {
        ArrayList<Boolean> a = new ArrayList<Boolean>();
        return new Data("Car/"+m_carIdentifier+"/LapInvalidFlags",a,"boolean",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Returns the Latitude location of the car.
     * 
     * <p>PATH = {@link #getLatitude(String) /Car/(CARIDENTIFIER)/Latitude/(UOM)}
     * 
     * @param UOM           (Optional) A unit of measure to return. Defaults to degrees.
     * @return The  Location of the car
     */
    public Data getLatitude(String UOM) {
        Data status = getStatus();
        String location = status.getString().contains("PIT") 
                        ? "ONPITROAD" 
                        : (status.getString().contains("TRACK") ? "ONTRACK" : "OFF");
        Data lat = m_SIMPlugin.getSession().getTrack().getLatitude(location,getLap(LapType.COMPLETEDPERCENT).getDouble(),UOM);
        lat.setName("Car/"+m_carIdentifier+"/Latitude");
        return lat;
    }
    public Data getLatitude() { return getLatitude(""); }

    /**
     * Returns the Longitude location of the car.
     * 
     * <p>PATH = {@link #getLongitude(String) /Car/(CARIDENTIFIER)/Longitude/(UOM)}
     * 
     * @param UOM           (Optional) A unit of measure to return. Defaults to degrees.
     * @return The  Location of the car
     */
    public Data getLongitude(String UOM) {
        Data status = getStatus();
        String location = status.getString().contains("PIT") 
                        ? "ONPITROAD" 
                        : (status.getString().contains("TRACK") ? "ONTRACK" : "OFF");
        Data lng = m_SIMPlugin.getSession().getTrack().getLongitude(location,getLap(LapType.COMPLETEDPERCENT).getDouble(),UOM);
        lng.setName("Car/"+m_carIdentifier+"/Longitude");
        return lng;
    }
    public Data getLongitude() { return getLongitude(""); }

    /**
     * Returns the filename of the manufacturer's logo for this car.
     * The filename will be the complete relative path such that it can be found in the classpath.
     * 
     * <p>PATH = {@link #getManufacturerLogo() /Car/(CARIDENTIFIER)/ManufacturerLogo}
     * 
     * @return The filename of the manufacturer's logo.
     */
    public Data getManufacturerLogo() {
        return new Data("Car/"+m_carIdentifier+"/ManufacturerLogo",m_mfrLogo,"",Data.State.NORMAL);
    }
    
    /**
     * Returns the maximum number of tire sets for this session.
     * It first checks to see if the DataPublisher.Post plugin is loaded and publishing.
     * If so, it asked if the server has a max tires count.
     * 
     * The SIM could override this and return a number if the SIM supports it as well.
     * The SIM should check to see if the plugin returned a value first before overriding it.
     * 
     * <p>PATH = {@link #getMaxTires() /Car/(CARIDENTIFIER)/MaxTires} 1.2
     * 
     * @since 1.2
     * @return The maximum number of tire sets.
     */
    public Data getMaxTires() {
        Data d = new Data("Car/"+m_carIdentifier+"/MaxTires",99,Data.State.NOTAVAILABLE);
        SIMPluginCallback callback = this.m_SIMPlugin.getCallback("DataPublisher.Post");
        if (callback != null) {
            Data max = ((com.SIMRacingApps.SIMPluginCallbacks.DataPublisher.Post)callback).getMaxTires();
            d.setValue(max.getValue(),max.getUOM(),max.getState());
        }
        return d;
    }
    
    /**
     * Returns the merge point as a percentage from the start/finish line.
     * 
     * <p>PATH = {@link #getMergePoint() /Car/(CARIDENTIFIER)/MergePoint}
     * 
     * @return The merge point.
     */
    public Data getMergePoint() {
        return new Data("Car/"+m_carIdentifier+"/MergePoint",0.0,"%",Data.State.NOTAVAILABLE);
    }

    /**
     * This class enumerates the various messages that a SIM can raise that applies to a specific car.
     * See {@link com.SIMRacingApps.Car#getMessages()}
     */
    public final static class Message {
        public static final String REPAIR           = "REPAIR";
        public static final String PITSPEEDLIMITER  = "PITSPEEDLIMITER";
        public static final String TOWING           = "TOWING";
    }
    
    /**
     * Returns a string of a semicolon separate list of messages that are currently active for this car.
     * It is possible to have multiple messages.
     * Each message will have a semicolon before and after it, even at the begin and end of the string.
     *
     * Possible message values are determined by {@link com.SIMRacingApps.Car.Message}.
     * You can display as is, or use it as an index to the the translation for a particular language.
     * <p>
     * For example: ";REPAIR;"
     * 
     * <p>PATH = {@link #getMessages() /Car/(CARIDENTIFIER)/Messages}
     * 
     * @return A list of messages in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getMessages() {
        return new Data("Car/"+m_carIdentifier+"/Messages","","String",Data.State.NOTAVAILABLE);
    };    

    /**
     * Returns a name of the car as reported by the SIM.
     * 
     * <p>PATH = {@link #getName() /Car/(CARIDENTIFIER)/Name}
     * 
     * @return The name of the car in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getName() {
        return new Data("Car/"+m_carIdentifier+"/Name",m_name,"String",Data.State.NORMAL);
    }

    /**
     * Returns the number of the car.
     * It can be up to 3 characters where 0,00,000 are considered unique values. Don't treat it as numeric.
     * 
     * <p>PATH = {@link #getNumber() /Car/(CARIDENTIFIER)/Number}
     * 
     * @return The car number in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getNumber() {
        return new Data("Car/"+m_carIdentifier+"/Number","","String",Data.State.NOTAVAILABLE);
    }

    /**
     * Returns the font of the car's number.
     * 
     * <p>PATH = {@link #getNumberFont() /Car/(CARIDENTIFIER)/NumberFont}
     * 
     * @return The slant in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getNumberFont() {
        return new Data("Car/"+m_carIdentifier+"/NumberFont","Arial","String",Data.State.NOTAVAILABLE);
    }

    /**
     * Returns the slant of the car's number.
     * Can be "normal","left","right","forward","backwards".
     * 
     * <p>PATH = {@link #getNumberSlant() /Car/(CARIDENTIFIER)/NumberSlant}
     * 
     * @return The slant in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getNumberSlant() {
        return new Data("Car/"+m_carIdentifier+"/NumberSlant","backwards","String",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Returns the pit location as a percentage from the start/finish line.
     * 
     * <p>PATH = {@link #getPitLocation() /Car/(CARIDENTIFIER)/PitLocation}
     * 
     * @return The pit location. State is OFF if not known.
     */
    public Data getPitLocation() {
        return new Data("Car/"+m_carIdentifier+"/PitLocation",0.0,"%",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Returns the pit road speed limit in the car's unit of measure.
     * 
     * <p>PATH = {@link #getPitLocation() /Car/(CARIDENTIFIER)/PitSpeedLimit/(UOM)}
     * 
     * @return The pit road speed limit.
     * 
     * @param UOM (Optional) The unit of measure to return the result in. Default is the same UOM as the speedometer.
     * @return The pit road speed limit.
     */
    public Data getPitSpeedLimit(String UOM) {
        Data speedlimit = m_SIMPlugin.getSession().getTrack().getPitSpeedLimit(UOM);
        
        return speedlimit.convertUOM(UOM.isEmpty() ? _getGauge(Gauge.Type.SPEEDOMETER).getUOM().getString() : UOM);
    }
    public Data getPitSpeedLimit() { return getPitSpeedLimit(""); }
    
    /**
     * Returns the number of pit stops remaining based on fuel mileage.
     * It takes into consideration what you are currently have setup to add when you pit.
     * 
     * <p>PATH = {@link #getPitStopsRemaining(int) /Car/(CARIDENTIFIER)/PitStopsRemaining/{LAPSTOAVERAGE}}
     * 
     * @param lapstoaverage (Optional) The number of laps to average to get the fuel mileage. Defaults to 0, your worst lap. 
     * @return The number of pit stops remaining in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getPitStopsRemaining(int lapstoaverage) {
        Data d = new Data("Car/"+m_carIdentifier+"/PitStops",0,"",Data.State.NOTAVAILABLE);
        Data fuelLevelNeeded = getFuelLevelNeeded(lapstoaverage,0,"");
        
        if (fuelLevelNeeded.getState().equals(Data.State.NORMAL) && fuelLevelNeeded.getDouble() > 0.0) {
            Gauge fuellevel = _getGauge(Gauge.Type.FUELLEVEL);
            double maxfuel    = fuellevel.getCapacityMaximum(fuelLevelNeeded.getUOM()).getDouble();
            double adding     = Math.min(fuellevel.getValueNext(fuelLevelNeeded.getUOM()).getDouble(), maxfuel); //only what will fit
            
            int stops;
            //next can return a value, but if the user unchecked it, it's not going to add anything
            if (adding > 0.0 && fuellevel.getChangeFlag().getBoolean())
                //This will check how many stops will still be require based on what you will be adding.
                stops = (int)Math.ceil( Math.max(fuelLevelNeeded.getDouble() - adding, 0.0) / maxfuel ) + 1;
            else
                stops = (int)Math.ceil( Math.max(fuelLevelNeeded.getDouble(), 0.0) / maxfuel );
                
            d.setValue(stops);
        }
        
        d.setState(Data.State.NORMAL);
        return d;
    }
    public Data getPitStopsRemaining(String laps) { return getPitStopsRemaining(Integer.parseInt(laps)); }
    public Data getPitStopsRemaining() { return getPitStopsRemaining(0); }
    
    /**
     * Returns the amount of time the last pit stop took.
     * 
     * <p>PATH = {@link #getPitTime() /Car/(CARIDENTIFIER)/PitTime}
     * 
     * @return The pit stop time in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getPitTime() {
        return new Data("Car/"+m_carIdentifier+"/PitTime",0.0,"s",Data.State.NOTAVAILABLE);
    }

    /**
     * Returns the times this car pitted by lap.
     * 
     * <p>PATH = {@link #getPitTimes /Car/(CARIDENTIFIER)/PitTimes}
     * 
     * @return An array of times when the car pitted. Index by lap, zero based.
     */
    public Data getPitTimes() {
        ArrayList<Double> a = new ArrayList<Double>();
        return new Data("Car/"+m_carIdentifier+"/PitTimes",a,"s",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Returns the current position based on all cars in the session.
     * Positions start at 1, such that 1 is considered the leader.
     * 
     * <p>PATH = {@link #getPosition() /Car/(CARIDENTIFIER)/Position}
     * 
     * @return The position in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getPosition() {
        return new Data("Car/"+m_carIdentifier+"/Position",0,"integer",Data.State.NOTAVAILABLE);
    }

    /**
     * Returns the lowest position in the current session.
     * Positions start at 1, such that 1 is considered the leader.
     * 
     * <p>PATH = {@link #getPositionLowest() /Car/(CARIDENTIFIER)/PositionLowest}
     * 
     * @return The position in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getPositionLowest() {
        return new Data("Car/"+m_carIdentifier+"/PositionLowest",0,"integer",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Returns the highest position in the current session.
     * Positions start at 1, such that 1 is considered the leader.
     * 
     * <p>PATH = {@link #getPositionHighest() /Car/(CARIDENTIFIER)/PositionHighest}
     * 
     * @return The position in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getPositionHighest() {
        return new Data("Car/"+m_carIdentifier+"/PositionHighest",0,"integer",Data.State.NOTAVAILABLE);
    }

    /**
     * Returns the number of positions changed.
     * Positions start at 1, such that 1 is considered the leader.
     * 
     * <p>PATH = {@link #getPositionDelta() /Car/(CARIDENTIFIER)/PositionDelta}
     * 
     * @return The position delta in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getPositionDelta() {
        int position = getPosition().getInteger();
        @SuppressWarnings("unchecked")
        ArrayList<Integer> positions = (ArrayList<Integer>)getPositions().getValue();
        int delta = 0;
        //if my current positions has changed from the last lap, use it as the delta
        //otherwise diff the last 2 completed laps.
        if (positions.size() > 1) {
            if (position != positions.get(positions.size()-1))
                delta = positions.get(positions.size()-1) - position;
            else
                delta = positions.get(positions.size()-2) - positions.get(positions.size()-1);
        }
        else
        if (positions.size() > 0) {
            int qual = getPositionQualifying().getInteger();
            if (position != positions.get(positions.size()-1))
                delta = positions.get(positions.size()-1) - position;
            else
                delta = qual - positions.get(positions.size()-1);
        }            
        return new Data("Car/"+m_carIdentifier+"/PositionDelta",delta,"integer",Data.State.NORMAL);
    }
    
    /**
     * Returns the position at the end of each lap in in an array
     * Positions start at 1, such that 1 is considered the leader, but the array is indexed as zero based.
     * This means to get the position for lap 1, look at array[0].
     * 
     * <p>PATH = {@link #getPositions() /Car/(CARIDENTIFIER)/Positions}
     * 
     * @return The position in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getPositions() {
        ArrayList<Integer> a = new ArrayList<Integer>();
        return new Data("Car/"+m_carIdentifier+"/Positions",a,"integer",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Returns the class position at the end of each lap in in an array
     * Positions start at 1, such that 1 is considered the leader, but the array is indexed as zero based.
     * This means to get the position for lap 1, look at array[0].
     * 
     * <p>PATH = {@link #getPositionsClass() /Car/(CARIDENTIFIER)/PositionsClass}
     * 
     * @return The class position in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getPositionsClass() {
        ArrayList<Integer> a = new ArrayList<Integer>();
        return new Data("Car/"+m_carIdentifier+"/PositionsClass",a,"integer",Data.State.NOTAVAILABLE);
    }

    /**
     * Returns the current position based on the class the car is in.
     * Positions start at 1, such that 1 is considered the leader.
     * 
     * <p>PATH = {@link #getPositionClass() /Car/(CARIDENTIFIER)/PositionClass}
     * 
     * @return The position in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getPositionClass() {
        return new Data("Car/"+m_carIdentifier+"/PositionClass",0,"integer",Data.State.NOTAVAILABLE);
    }

    /**
     * Returns the lowest position in the current session for your class.
     * Positions start at 1, such that 1 is considered the leader.
     * 
     * <p>PATH = {@link #getPositionLowestClass() /Car/(CARIDENTIFIER)/PositionLowestClass}
     * 
     * @return The position in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getPositionLowestClass() {
        return new Data("Car/"+m_carIdentifier+"/PositionLowestClass",0,"integer",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Returns the highest position in the current session for your class.
     * Positions start at 1, such that 1 is considered the leader.
     * 
     * <p>PATH = {@link #getPositionHighestClass() /Car/(CARIDENTIFIER)/PositionHighestClass}
     * 
     * @return The position in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getPositionHighestClass() {
        return new Data("Car/"+m_carIdentifier+"/PositionHighestClass",0,"integer",Data.State.NOTAVAILABLE);
    }

    /**
     * Returns the number of positions changed for your class.
     * Positions start at 1, such that 1 is considered the leader.
     * 
     * <p>PATH = {@link #getPositionClassDelta() /Car/(CARIDENTIFIER)/PositionClassDelta}
     * 
     * @return The position delta of your class in a {@link com.SIMRacingApps.Data} container.
     */
    @SuppressWarnings("unchecked")
    public Data getPositionClassDelta() {
        int position = getPositionClass().getInteger();
        ArrayList<Integer> positions = (ArrayList<Integer>)getPositionsClass().getValue();
        int delta = 0;
        //if my current positions has changed from the last lap, use it as the delta
        //otherwise diff the last 2 completed laps.
        if (positions.size() > 1) {
            if (position != positions.get(positions.size()-1))
                delta = positions.get(positions.size()-1) - position;
            else
                delta = positions.get(positions.size()-2) - positions.get(positions.size()-1);
        }
        else
        if (positions.size() > 0) {
            int qual = getPositionClassQualifying().getInteger();
            if (position != positions.get(positions.size()-1))
                delta = positions.get(positions.size()-1) - position;
            else
                delta = qual - positions.get(positions.size()-1);
        }            
        return new Data("Car/"+m_carIdentifier+"/PositionClassDelta",delta,"integer",Data.State.NORMAL);
    }
    
    /**
     * Returns the qualifying position based on other cars in this session.
     * Positions start at 1, such that 1 is considered the leader.
     * 
     * <p>PATH = {@link #getPositionQualifying() /Car/(CARIDENTIFIER)/PositionQualifying}
     * 
     * @return The qualifying position in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getPositionQualifying() {
        return new Data("Car/"+m_carIdentifier+"/PositionQualifying",0,Data.State.NOTAVAILABLE);
    }

    /**
     * Returns the qualifying position based on other cars in this session for your class.
     * Positions start at 1, such that 1 is considered the leader.
     * 
     * <p>PATH = {@link #getPositionClassQualifying() /Car/(CARIDENTIFIER)/PositionClassQualifying}
     * 
     * @return The qualifying position in your class in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getPositionClassQualifying() {
        return new Data("Car/"+m_carIdentifier+"/PositionClassQualifying",0,Data.State.NOTAVAILABLE);
    }

    /**
     * Returns the radio channel number that this car is currently transmitting on.
     * 
     * <p>PATH = {@link #getRadioChannel() /Car/(CARIDENTIFIER)/RadioChannel}
     * 
     * @return The radio channel number in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getRadioChannel() {
        return new Data("Car/"+m_carIdentifier+"/RadioChannel",0,Data.State.NOTAVAILABLE);
    }

    /**
     * Returns the radio channel name that this car is currently transmitting on.
     * 
     * <p>PATH = {@link #getRadioChannelName() /Car/(CARIDENTIFIER)/RadioChannelName}
     * 
     * @return The radio channel in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getRadioChannelName() {
        return new Data("Car/"+m_carIdentifier+"/RadioChannelName","",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Returns the amount of time required to repair the car.
     * 
     * <p>PATH = {@link #getRepairTime() /Car/(CARIDENTIFIER)/RepairTime}
     * 
     * @return The repair time in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getRepairTime() {
        return new Data("Car/"+m_carIdentifier+"/RepairTime",0.0,"s",Data.State.NOTAVAILABLE);
    }

    /**
     * Returns the amount of time that is optional to repair the car.
     * 
     * <p>PATH = {@link #getRepairTimeOptional() /Car/(CARIDENTIFIER)/RepairTimeOptional}
     * 
     * @return The optional repair time in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getRepairTimeOptional() {
        return new Data("Car/"+m_carIdentifier+"/RepairTime",0.0,"s",Data.State.NOTAVAILABLE);
    }

    /**
     * Returns the RPM for pit road speed while in 2nd gear
     * 
     * <p>PATH = {@link #getRPMPitRoadSpeed /Car/(CARIDENTIFIER)/RPMPitRoadSpeed}
     * 
     * @return The RPM in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getRPMPitRoadSpeed() {
        return new Data("Car/"+m_carIdentifier+"/RPMPitRoadSpeed",m_pitRoadSpeedRPM,"rev/min",Data.State.NORMAL);
    }

    /**
     * Returns the current spotter message. 
     * 
     * Messages supported are.
     * <dl>
     * <dt>OFF</dt><dd>Spotter is not talking</dd>
     * <dt>CLEAR</dt><dd>no cars around us</dd>
     * <dt>CARLEFT</dt><dd>there is a car to our left</dd>
     * <dt>CARRIGHT</dt><dd>there is a car to our right</dd>
     * <dt>CARLEFTRIGHT</dt><dd>there are cars on each side</dd>
     * <dt>CARSLEFT</dt><dd>there are two or more cars to our left</dd>
     * <dt>CARSRIGHT</dt><dd>there are two or more cars to our right</dd>
     * </dl>
     * 
     * <p>PATH = {@link #getSpotterMessage /Car/(CARIDENTIFIER)/SpotterMessage}
     * 
     * @return The spotter message in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getSpotterMessage() {
        return new Data("Car/"+m_carIdentifier+"/SpotterMessage",SpotterMessages.OFF,"",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Returns the times this car crossed the start/finish by lap.
     * 
     * <p>PATH = {@link #getStartFinishTimes /Car/(CARIDENTIFIER)/StartFinishTimes}
     * 
     * @return An array of times when the car passed the start/finish. Index by lap, zero based.
     */
    public Data getStartFinishTimes() {
        ArrayList<Double> a = new ArrayList<Double>();
        return new Data("Car/"+m_carIdentifier+"/StartFinishTimes",a,"s",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Returns the team name if the current session has teams.
     * 
     * <p>PATH = {@link #getTeamName /Car/(CARIDENTIFIER)/TeamName}
     * 
     * @return The team name. Blank if session does not have teams 
     */
    public Data getTeamName() {
        return new Data("Car/"+m_carIdentifier+"/TeamName","","",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Sets this car/driver's admin status.
     * This command is SIM specific and requires special privileges to succeed.
     * 
     * <p>PATH = {@link #setAdminFlag(boolean) /Car/(CARIDENTIFIER)/setAdminFlag/(Y/N)}
     * 
     * @param onOffFlag The admin status.
     * @return The string sent to SIM in a {@link com.SIMRacingApps.Data} container.
     */
    public Data setAdminFlag(boolean onOffFlag) {
        return new Data("Car/"+m_carIdentifier+"/setAdminFlag","","String",Data.State.NOTAVAILABLE);
    }
    public    Data setAdminFlag(String onOffFlag) {
        return setAdminFlag((new Data("",onOffFlag)).getBoolean());
    }

    /**
     * Sets a black flag for this car/driver.
     * Can be number of seconds or laps. 
     * Defaults to zero seconds, stop and go.
     * This command is SIM specific and requires special privileges to succeed.
     * 
     * <p>PATH = {@link #setBlackFlag(int,String) /Car/(CARIDENTIFIER)/setBlackFlag/(SECONDS/LAPS)/(UOM)}
     * 
     * @param quantity The number of seconds or laps based on the UOM. Default to zero(0).
     * @param uom The unit of measure of the quantity. "s" for seconds, "lap" for laps. Defaults to "s". 
     * @return The string sent to SIM in a {@link com.SIMRacingApps.Data} container.
     */
    public Data setBlackFlag(int quantity,String uom) {
        return new Data("Car/"+m_carIdentifier+"/setBlackFlag","","String",Data.State.NOTAVAILABLE);
    }
    public    Data setBlackFlag(String quantity, String uom) {
        return setBlackFlag(Integer.parseInt(quantity),uom);
    }
    public    Data setBlackFlag(String quantity) {
        return setBlackFlag(Integer.parseInt(quantity),"s");
    }
    public    Data setBlackFlag() {
        return setBlackFlag(0,"s");
    }

    /**
     * Changes the camera to focus on this car.
     * 
     * This is very SIM specific. Some cameras can do this, others cannot.
     * The name of the group and camera is also very SIM specific. 
     * Some SIM limit the camera you can choose based on if you are driving or spectating or watch a replay.
     * So, if the SIM doesn't allow the change, then this function will not really know. Therefore, there is not an error condition.
     * 
     * <p>PATH = {@link #setCamera(String) /Car/(CARIDENTIFIER)/setCamera/CameraName}
     *
     *@param cameraName (Optional) The name of the camera. Default to current camera.
     *@return The group/camera name in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setCamera(String cameraName) {
        Data d = m_SIMPlugin.getSession().setCamera(cameraName,"DRIVER",""+m_carIdentifier);
        return new Data("Car/"+m_carIdentifier+"/setCamera",d.getString(),"String",d.getState());
    }
    public    Data setCamera() {
        return setCamera(m_SIMPlugin.getSession().getCamera().getString());
    }
    
    /**
     * Sends the text string as a chat message to this driver.
     * Each SIM must override this and provide this functionality.
     *
     * <p>PATH = {@link #setChat(String) /Car/(CARIDENTIFIER)/setChat/(TEXT)}
     *
     *@param text The string to send to the chat window
     *@return The text string sent to the driver  in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setChat(String text) {
        return new Data("Car/"+m_carIdentifier+"/setChat",text,"String",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Sets this car/driver's chat status.
     * This command is SIM specific and requires special privileges to succeed.
     * 
     * <p>PATH = {@link #setChatFlag(boolean) /Car/(CARIDENTIFIER)/setChatFlag/(Y/N)}
     * 
     * @param onOffFlag The chat status.
     * @return The string sent to SIM in a {@link com.SIMRacingApps.Data} container.
     */
    public Data setChatFlag(boolean onOffFlag) {
        return new Data("Car/"+m_carIdentifier+"/setChatFlag","","String",Data.State.NOTAVAILABLE);
    }
    public    Data setChatFlag(String onOffFlag) {
        return setChatFlag((new Data("",onOffFlag)).getBoolean());
    }


    /**
     * Disqualify this car but leave them in the session. 
     * This command is SIM specific and requires special privileges to succeed.
     * 
     * <p>PATH = {@link #setDisqualifyFlag() /Car/(CARIDENTIFIER)/setDisqualifyFlag}
     * 
     * @return The string sent to SIM in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setDisqualifyFlag() {
        return new Data("Car/"+m_carIdentifier+"/setDisqualifyFlag","","String",Data.State.NOTAVAILABLE);
    }

    /**
     * Send this car to the end of the pacing line. 
     * This command is SIM specific and requires special privileges to succeed.
     * 
     * <p>PATH = {@link #setEndOfLineFlag() /Car/(CARIDENTIFIER)/setEndOfLineFlag}
     * 
     * @return The string sent to SIM in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setEndOfLineFlag() {
        return new Data("Car/"+m_carIdentifier+"/setEndOfLineFlag","","String",Data.State.NOTAVAILABLE);
    }

    /**
     * Clear all penalties, DQ, EOL, Black Flags. 
     * This command is SIM specific and requires special privileges to succeed.
     * 
     * <p>PATH = {@link #setClearPenaltiesFlag() /Car/(CARIDENTIFIER)/setClearPenaltiesFlag}
     * 
     * @return The string sent to SIM in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setClearPenaltiesFlag() {
        return new Data("Car/"+m_carIdentifier+"/setClearPenaltiesFlag","","String",Data.State.NOTAVAILABLE);
    }

    /**
     * Remove the user from the session. 
     * This command is SIM specific and requires special privileges to succeed.
     * 
     * <p>PATH = {@link #setRemoveFlag() /Car/(CARIDENDIFIER)/setRemoveFlag}
     * 
     * @return The string sent to SIM in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setRemoveFlag() {
        return new Data("Car/"+m_carIdentifier+"/setRemoveFlag","","String",Data.State.NOTAVAILABLE);
    }

    /**
     * Wave this car around the pace car. 
     * This command is SIM specific and requires special privileges to succeed.
     * 
     * <p>PATH = {@link #setWaveAroundFlag() /Car/(CARIDENTIFIER)/setWaveAroundFlag}
     * 
     * @return The requested setting in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data setWaveAroundFlag() {
        return new Data("Car/"+m_carIdentifier+"/setWaveAroundFlag",false,"boolean",Data.State.NOTAVAILABLE);
    }

    /**
     * Sets the pit road speed as the RPMs in 2nd gear.
     * Calculates the states as a percentage of that number.
     * Designed to be called internally by the SIM code. 
     * But, I suppose it could be called externally by a client simulating a real Tachometer where they push a button while following the pace car at pit road speed.
     * 
     * <p>PATH = {@link #setRPMPitRoadSpeed(double) /Car/(CARIDENTIFIER)/setRPMPitRoadSpeed/(RPM)}
     * 
     * @param rpm The RPMs that is pit road speed.
     * @return The RPM in a {@link com.SIMRacingApps.Data} container.
     */
    public Data setRPMPitRoadSpeed(double rpm) {
        if (m_id != -1)
            if (Server.isLogLevelFine())
                Server.logger().fine(String.format("Car.setRPMPitRoadSpeed(%.0f) for (%d) - %s",rpm, m_id, m_name));
        
        m_pitRoadSpeedRPM = rpm;
        double range = 200.0;  //TODO: 200 rpm is just an estimate of 1 mph to calculate the top and bottom end of the ranges base on visual of iRacing's gauge

        Gauge gauge = _getGauge(Gauge.Type.TACHOMETER);

        //double WayOverPitSpeed     = 1.10;
        double WayOverPitSpeed     = (m_pitRoadSpeedRPM + (range * 15.0)) / m_pitRoadSpeedRPM;
        double OverPitSpeed        = (m_pitRoadSpeedRPM + range) / m_pitRoadSpeedRPM;
        double PitSpeed            = (m_pitRoadSpeedRPM - 0.0) / m_pitRoadSpeedRPM;
//        double ApproachingPitSpeed = PitSpeed - (7*.04) - (7*.02);
        double ApproachingPitSpeed = PitSpeed - (range / m_pitRoadSpeedRPM) - ((range / m_pitRoadSpeedRPM)*2);

        gauge._addStateRange("","WAYOVERLIMIT",     m_pitRoadSpeedRPM * WayOverPitSpeed,     Double.MAX_VALUE,                    "rev/min");
        gauge._addStateRange("","OVERLIMIT",        m_pitRoadSpeedRPM * OverPitSpeed,        m_pitRoadSpeedRPM * WayOverPitSpeed, "rev/min");
        gauge._addStateRange("","LIMIT",            m_pitRoadSpeedRPM * PitSpeed,            m_pitRoadSpeedRPM * OverPitSpeed,    "rev/min");
        gauge._addStateRange("","APPROACHINGLIMIT", m_pitRoadSpeedRPM * ApproachingPitSpeed, m_pitRoadSpeedRPM * PitSpeed,        "rev/min");
        return getRPMPitRoadSpeed();
    }

    /**
     * Returns the current status of the car as defined by {@link com.SIMRacingApps.Car.Status}
     * 
     * <p>PATH = {@link #getStatus() /Car/(CARIDENTIFIER)/Status}
     * 
     * @return The current status in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getStatus() {
        return new Data("Car/"+m_carIdentifier+"/Status",Car.Status.INVALID,"Car.Status",Data.State.NOTAVAILABLE);
    }
    
    /**
     * Returns the status of the car and the car identifier as one string.
     * 
     * <p>PATH = {@link #getStatusClass() /Car/(CARIDENTIFIER)/StatusClass}
     * 
     * @return status + "-" + identifier in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getStatusClass() {
        Data status    = getStatus();
        Data thiscar   = getId();
        Car  me        = m_SIMPlugin.getSession().getCar("ME");
        Car  leader    = m_SIMPlugin.getSession().getCar(CarIdentifiers.LEADER_PREFIX + "I" + getId().getString());
        Car  reference = m_SIMPlugin.getSession().getCar("REFERENCE");
    
        String s = "";
    
        if (m_name.equalsIgnoreCase("PITSTALL"))
            return new Data("Car/"+m_carIdentifier+"/StatusClass","INPITSTALL-PITSTALL");
    
        if (thiscar.getInteger() == -1)
            s = "";
        else
        if (getIsEqual("PACECAR").getBoolean())
            s = "-PACECAR";
        else
        if (getIsEqual("ME").getBoolean())
            s = "-ME";
        else
        if (getIsEqual(CarIdentifiers.LEADER_PREFIX + "I" + getId().getString()).getBoolean())
            s = "-LEADER";
        else
        if (getIsEqual("REFERENCE").getBoolean())
            s = "-REFERENCE";
        else 
        if (reference != null) {
            double trackposition_ref = (reference.isME() && me.getIsSpectator().getBoolean() && leader != null)
                                     ? leader.getLap(Car.LapType.COMPLETED).getDouble() + (leader.getLap(Car.LapType.COMPLETEDPERCENT).getDouble() / 100.0)
                                     : reference.getLap(Car.LapType.COMPLETED).getDouble() + (reference.getLap(Car.LapType.COMPLETEDPERCENT).getDouble() / 100.0);
            double trackposition     = getLap(Car.LapType.COMPLETED).getDouble()  + (getLap(Car.LapType.COMPLETEDPERCENT).getDouble()  / 100.0);
    
            if (m_SIMPlugin.getSession().getType().getString().equalsIgnoreCase("RACE") && (trackposition_ref > 1.0 || trackposition > 1.0)) {
                if ((trackposition - trackposition_ref) > .65)
                    s = "-LAPAHEAD";
                else
                if ((trackposition_ref - trackposition) > .65)
                    s = "-LAPBEHIND";
            }
        }
        
        return new Data("Car/"+m_carIdentifier+"/StatusClass",status.getString()+s,"",status.getState());
    }

    /**
     * Returns a semicolon(;) separated list of engine warnings. 
     * The string will contain a leading and trailing semicolon, such that, a contains ";REVLIMITER;" can return true if found in the string.
     * You can also split the string on semicolons and ignore the blank ones.
     * Here is a list of all the possible values.
     * <ul>
     * <li>ENGINESTALLED</li>
     * <li>FUELLEVELCRITICAL</li>
     * <li>FUELLEVELWARNING</li>
     * <li>FUELPRESSURECRITICAL</li>
     * <li>FUELPRESSUREWARNING</li>
     * <li>OILLEVELCRITICAL</li>
     * <li>OILLEVELWARNING</li>
     * <li>OILPRESSURECRITICAL</li>
     * <li>OILPRESSUREWARNING</li>
     * <li>OILTEMPCRITICAL</li>
     * <li>OILTEMPWARNING</li>
     * <li>PITSPEEDLIMITER</li>
     * <li>REVLIMITER</li>
     * <li>REPAIRSREQUIRED</li>
     * <li>VOLTAGECRITICAL</li>
     * <li>VOLTAGEWARNING</li>
     * <li>WATERLEVELCRITICAL</li>
     * <li>WATERLEVELWARNING</li>
     * <li>WATERPRESSURECRITICAL</li>
     * <li>WATERPRESSUREWARNING</li>
     * <li>WATERTEMPCRITICAL</li>
     * <li>WATERTEMPWARNING</li>
     * </ul> 
     * 
     * <p>PATH = {@link #getWarnings() /Car/(CARIDENTIFIER)/Warnings}
     * 
     * @return A list of warnings in a {@link com.SIMRacingApps.Data} container.
     */
    public Data getWarnings() {
        return new Data("Car/"+m_carIdentifier+"/Warnings","","text",Data.State.NOTAVAILABLE);
    }

    static private Map<String,Object> s_profiles = null;
    
    @SuppressWarnings("unchecked")
    private void __loadCar() {
        //initialize the definitions cache, if needed
        if (s_profiles == null)
            s_profiles = new HashMap<String,Object>();
        
        FindFile file = null;
        String filepath = "com/SIMRacingApps/Car.json";
        Map<String,Object> carMap = null;
        String description = null;
        String logo = null;
        
        //if the default car has not been loaded, 
        //then load it and cache it in the m_cars map
        carMap = (Map<String, Object>) s_profiles.get(filepath);
        if (carMap == null) {
            
            try {
                file = new FindFile(filepath);
                carMap = file.getJSON();
                s_profiles.put(filepath, carMap);
            }
            catch (FileNotFoundException e) {
                Server.logger().severe(String.format("(%s) cannot open",filepath));
                s_profiles.put(filepath, carMap = new HashMap<String, Object>() );
            }
        }
        
        description = (String) carMap.get("Description");
        logo        = (String) carMap.get("MfrLogo");

        //if the SIM's car has not been loaded, 
        //then load it and cache it in the m_cars map
        if (m_carPath != null && !m_carPath.isEmpty()) {
            
            carMap = (Map<String, Object>) s_profiles.get(m_carPath);
            if (carMap == null) {
                try {
                    file = new FindFile(m_carPath);
                    carMap = file.getJSON();
                    s_profiles.put(m_carPath, carMap);
                    description = (String) carMap.get("Description");
                    logo        = (String) carMap.get("MfrLogo");
                }
                catch (FileNotFoundException e) {
                    Server.logger().warning(String.format("(%s) not found",m_carPath));
                    s_profiles.put(m_carPath, carMap = new HashMap<String, Object>());
                }
            }
            if (carMap.containsKey("Description"))
                description = (String) carMap.get("Description");
            if (carMap.containsKey("MfrLogo"))
                logo        = (String) carMap.get("MfrLogo");
        }

        if (description != null) {
            m_description = description;
        }
        
        if (logo != null) {
            m_mfrLogo = logo;
        }
        
        if (m_id != -1)
            Server.logger().info(String.format("(%s) for id(%d) - %s (%s)",m_carPath,m_id,m_name,m_description));
        

        return;
    }

    /**
     * Returns a map to the default gauges found in the Car.json file.
     * 
     * @return A map of the gauges or null if not found
     */
    public Map<String,Map<String,Map<String,Object>>> _getDefaultGauges() {
        @SuppressWarnings("unchecked")
        Map<String,Map<String,Map<String,Map<String,Object>>>> map = (Map<String, Map<String,Map<String,Map<String,Object>>>>) s_profiles.get("com/SIMRacingApps/Car.json");
        return map.get("Gauges");
    }
    
    /**
     * Returns a map to the default gauges found in the SIM's .json file.
     * 
     * @return A map of the gauges or null if not found
     */
    public Map<String,Map<String,Map<String,Object>>> _getSIMGauges() {
        @SuppressWarnings("unchecked")
        Map<String,Map<String,Map<String,Map<String,Object>>>> map = (Map<String, Map<String,Map<String,Map<String,Object>>>>) s_profiles.get(m_carPath);
        return map.get("Gauges");
    }
    
    public void _dumpGauges() {
        Iterator<Entry<String,Gauge>> itr = m_gauges.entrySet().iterator();
        while (itr.hasNext()) {
            Gauge gauge = itr.next().getValue();
            Server.logger().fine(gauge.toString());
        }
    }

    /**
     * Each SIM that subclasses Car, should call this at the end of the constructor
     * to allow global functionality to be added to the car.
     * 
     * One, update the shift states from the settings so users can override them.
     */
    public void _postInitialization() {
        try {
            Gauge gauge = _getGauge(Gauge.Type.TACHOMETER);
            Gauge gear  = _getGauge(Gauge.Type.GEAR);
            Gauge power = _getGauge(Gauge.Type.ENGINEPOWER);
            String car  = getName().getString().replace(" ", "_");
            String track= m_SIMPlugin.getSession().getTrack().getName().getString().replace(" ", "_");

            //allow the user to override the shift light RPM values
            //example:
            //
            //stockcars_chevyss-ShiftLightStart = 6000
            //stockcars_chevyss-ShiftLightShift = 7000
            //stockcars_chevyss-ShiftLightBlink = 8000
            
            double DriverCarSLFirstRPM = Server.getArg(
                                            String.format(              "%s-%s-ShiftLightStart-%s-%d", track,car,gear.getValueCurrent().getString(),power.getValueCurrent().getInteger()),
                                            Server.getArg(String.format("%s-%s-ShiftLightStart-%s",    track,car,gear.getValueCurrent().getString()),
                                            Server.getArg(String.format("%s-%s-ShiftLightStart",       track,car),
                                            Server.getArg(String.format("%s-ShiftLightStart-%s-%d",          car,gear.getValueCurrent().getString(),power.getValueCurrent().getInteger()),
                                            Server.getArg(String.format("%s-ShiftLightStart-%s",             car,gear.getValueCurrent().getString()),
                                            Server.getArg(String.format("%s-ShiftLightStart",                car),      -1.0)
                                         )))));
            double DriverCarSLShiftRPM = Server.getArg(
                                            String.format(              "%s-%s-ShiftLightShift-%s-%d", track,car,gear.getValueCurrent().getString(),power.getValueCurrent().getInteger()),
                                            Server.getArg(String.format("%s-%s-ShiftLightShift-%s",    track,car,gear.getValueCurrent().getString()),
                                            Server.getArg(String.format("%s-%s-ShiftLightShift",       track,car),
                                            Server.getArg(String.format("%s-ShiftLightShift-%s-%d",          car,gear.getValueCurrent().getString(),power.getValueCurrent().getInteger()),
                                            Server.getArg(String.format("%s-ShiftLightShift-%s",             car,gear.getValueCurrent().getString()),
                                            Server.getArg(String.format("%s-ShiftLightShift",                car),      -1.0)
                                         )))));
            double DriverCarSLBlinkRPM = Server.getArg(
                                            String.format(              "%s-%s-ShiftLightBlink-%s-%d", track,car,gear.getValueCurrent().getString(),power.getValueCurrent().getInteger()),
                                            Server.getArg(String.format("%s-%s-ShiftLightBlink-%s",    track,car,gear.getValueCurrent().getString()),
                                            Server.getArg(String.format("%s-%s-ShiftLightBlink",       track,car),
                                            Server.getArg(String.format("%s-ShiftLightBlink-%s-%d",          car,gear.getValueCurrent().getString(),power.getValueCurrent().getInteger()),
                                            Server.getArg(String.format("%s-ShiftLightBlink-%s",             car,gear.getValueCurrent().getString()),
                                            Server.getArg(String.format("%s-ShiftLightBlink",                car),      -1.0)
                                         )))));
            
            if (DriverCarSLFirstRPM > 0.0 && DriverCarSLShiftRPM > 0.0 && DriverCarSLBlinkRPM > 0.0) {
                gauge._addStateRange("","SHIFTLIGHTS",            DriverCarSLFirstRPM,                  DriverCarSLShiftRPM, "rev/min");
                gauge._addStateRange("","SHIFT",                  DriverCarSLShiftRPM,                  DriverCarSLBlinkRPM, "rev/min");
                gauge._addStateRange("","SHIFTBLINK",             DriverCarSLBlinkRPM,                  999999.0,            "rev/min");

                Server.logger().info(String.format("Shift Point from user: First=%.0f, Shift=%.0f, Blink=%.0f",
                        DriverCarSLFirstRPM,DriverCarSLShiftRPM,DriverCarSLBlinkRPM));
            }
        }
        catch (NumberFormatException e) {}
    }
}
