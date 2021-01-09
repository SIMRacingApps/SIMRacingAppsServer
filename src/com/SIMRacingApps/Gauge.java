package com.SIMRacingApps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.SIMRacingApps.Data;
import com.SIMRacingApps.SIMPlugin.SIMPluginException;

/**
 * The Gauge class is used to monitor anything on the car that has a value, or has a value that can be set on the next pit stop.
 *
 * It will have 3 possible values, Current, Next, Historical.
 * A flag for each of the values so you know when they get updated.
 * <pre>
 *    Current    = (real-time, pit stall)
 *    Next       = (fixed, real-time, pit stall)
 *    Historical = The value of Current stored by lap when measured in the pit stall.
 * </pre>
 * A Change Flag to indicate you want to apply the Next value at the next valid change point (real-time,pit stall).
 * Changing the Next value will automatically set the Dirty Flag and the Change Flag if not equal Current.
 * <p>
 * It will have Capacity Minimum,Maximum and Increment values that will constrain what you can change the Next value to.
 * It will have Minimum and Maximum values that a gauge would display.
 * It will have Major and Minor Increments that can be used by a gauge for display.
 * <p>
 * A gauge doesn't have to be on the car, it could be in the pits (such as a tire pressure or temperature gauge).
 * A gauge could represent an object that gets replaced when pitting or when reset
 * that may not output a value (such as the wind-shield tear-off).
 * <p>
 * A gauge can have several attributes to allow clients to build adaptable software representations of a gauge.
 * It has min and max values on the gauge. 
 * It has min and max values(capacity) that it can have.
 * It has an increment that all values will be rounded to when changing it.
 * <p>
 * A gauge can also have "State", which is defined range(s) of values. 
 * Standard States are "NOTAVAILABLE", "OFF", "ERROR" and "NORMAL".
 * Clients can choose to display different colors, blink, or other visual effects based on the state.
 * Many gauges also have additional states. 
 * See {@link com.SIMRacingApps.Gauge.Type} for each gauges documentation for the states it supports.
 * @author Jeffrey Gilliam
 * @since 1.4
 * @copyright Copyright (C) 2015 - 2021 Jeffrey Gilliam
 * @license Apache License 2.0
 * 
 */

public class Gauge {
    /**
     * This defines all the possible gauge types.
     * Gauges have predefined states based on ranges of values.
     * All gauges support the standard states of
     * "NOTAVAILABLE" (This gauge doesn't exist in this car) 
     * "OFF" (The gauge is turned off), 
     * "NORMAL" (The gauge is on and outputting values)
     * "ERROR" (There's a problem reading the value of this gauge).
     * "NOTAVAILABLE" (The gauge is not available on this car)
     * Gauges can also define other states that the range can change based on the car and track.
     * <p>
     * For example, if you want to turn the Tachometer red when it's time to shift, 
     * then look for the state, "SHIFT" instead of a hard coded value of RPMs.
     * You can also make it blink when the state is "CRITICAL" to indicate it has red-lined. 
     */
    public static class Type {
        //NOTE: Cannot put any slashes in these names so not to confuse the data parser
        
        /** A generic gauge. Use this one when you want to set all the values, ranges, etc. manually
         * It is also the gauge that is returned when an unknown gauge is requested. 
         */
        public static final String GENERIC         = "Generic";
        
        /**
         * The fuel level. This could be real-time, estimated, or may not be possible to see until you pit. 
         * That depends on the SIM.
         * <p>
         * States:
         * <ul>
         * <li>WARNING - indicates it is time to think about pitting</li>
         * <li>CRITICAL - indicates it is time to pit immediately</li>
         * </ul>
         */
        public static final String FUELLEVEL       = "FuelLevel";

        /**
         * The fuel pressure. 
         * <p>
         * States:
         * <ul>
         * <li>WARNING - indicates it is time to think about pitting</li>
         * <li>CRITICAL - indicates it is time to pit immediately</li>
         * </ul>
         */
        public static final String FUELPRESSURE    = "FuelPressure";
        
        /**
         * The gear your transmission is currently in.
         * While all other gauges return a decimal, this gauge returns a string stated as follows:
         * <p>
         * States:
         * <ul>
         * <li>R - Reverse</li>
         * <li>N - Neutral</li>
         * <li>1-x - Gear</li>
         * </ul>
         */
        public static final String GEAR            = "Gear";

        /**
         * The oil level. 
         * <p>
         * States:
         * <ul>
         * <li>WARNING - indicates you should probably pit soon, you are leaking oil.</li>
         * <li>CRITICAL - indicates the oil is about gone, pit for repairs now.</li>
         * </ul>
         */
        public static final String OILLEVEL        = "OilLevel";

        /**
         * The oil pressure. 
         * <p>
         * States:
         * <ul>
         * <li>WARNING - indicates you should probably pit soon, you are leaking oil.</li>
         * <li>CRITICAL - indicates the oil is about gone, pit for repairs now.</li>
         * </ul>
         */
        public static final String OILPRESSURE     = "OilPressure";
        
        /**
         * The oil temperature. 
         * <p>
         * States:
         * <ul>
         * <li>WARNING - indicates you need to keep an eye on it.</li>
         * <li>CRITICAL - indicates it is at a dangerous level and you should get some air to radiator.</li>
         * </ul>
         */
        public static final String OILTEMP         = "OilTemp";
        
        /**
         * The speed of the car.
         * <p>
         * States:
         * <ul>
         * <li>APPROACHINGLIMIT - you are approaching the pit road speed limit</li>
         * <li>LIMIT            - You are near the pit road speed limit</li>
         * <li>OVERLIMIT        - You are over the pit road speed limit</li>
         * <li>WAYOVERLIMIT     - You are way over the pit road speed limit</li>
         * </ul>
         */
        public static final String SPEEDOMETER     = "Speedometer";

        /**
         * Power Steering Assist.
         */
        public static final String POWERSTEERINGASSIST  = "PowerSteeringAssist";

        /**
         * The Revolutions Per Minute (RPM).
         * The pit road states could be based on the speedometer or RPMs in 2nd gear.
         * The client can set that as an option.
         * <p>
         * States:
         * <ul>
         * <li>APPROACHINGLIMIT - you are approaching the pit road speed limit</li>
         * <li>LIMIT            - You are near the pit road speed limit</li>
         * <li>OVERLIMIT        - You are over the pit road speed limit</li>
         * <li>WAYOVERLIMIT     - You are way over the pit road speed limit</li>
         * <li>SHIFTLIGHTS      - indicates the shift lights should be turned on</li>
         * <li>SHIFT            - indicates it is time to shift to the next gear</li>
         * <li>CRITICAL         - indicates it has red-lined</li>
         * </ul>
         */
        public static final String TACHOMETER      = "Tachometer";
        
        /**
         * The rate of acceleration expressed as the rate of RPMs over time.
         */
        public static final String ACCELOMETER     = "Accelometer";
        
        /**
         * The percentage of tape currently on the front grill.
         */
        public static final String TAPE            = "Tape";
        
        /**
         * The battery voltage.
         */
        public static final String VOLTAGE         = "Voltage";

        /**
         * The water pressure. 
         * <p>
         * States:
         * <ul>
         * <li>WARNING - indicates you should probably pit soon, you are leaking water.</li>
         * <li>CRITICAL - indicates the water is about gone, pit for repairs now.</li>
         * </ul>
         */
        public static final String WATERLEVEL      = "WaterLevel";

        /**
         * The water pressure. 
         * <p>
         * States:
         * <ul>
         * <li>WARNING - indicates you should probably pit soon, you are leaking water.</li>
         * <li>CRITICAL - indicates the water is about gone, pit for repairs now.</li>
         * </ul>
         */
        public static final String WATERPRESSURE   = "WaterPressure";

        /**
         * The water temperature. 
         * <p>
         * States:
         * <ul>
         * <li>WARNING - indicates you need to keep an eye on it.</li>
         * <li>CRITICAL - indicates it is at a dangerous level and you should get some air to radiator.</li>
         * </ul>
         */
        public static final String WATERTEMP       = "WaterTemp";

        /**
         * The amount of right rear track bar adjustment that can be made during a pit stop.
         * Measured as a delta of Wedge.
         */
        public static final String RRWEDGEADJUSTMENT = "RRWedgeAdjustment";
        
        /**
         * The amount of left rear track bar adjustment that can be made during a pit stop.
         * Measured as a delta of Wedge.
         */
        public static final String LRWEDGEADJUSTMENT = "LRWedgeAdjustment";
        
        /**
         * The amount of front wing angle.
         */
        public static final String FRONTWING         = "FrontWing";
        public static final String REARWING          = "RearWing";
        
        /**
         * Controls the wind shield tear off for the next pit stop. 
         */
        public static final String WINDSHIELDTEAROFF = "WindshieldTearoff";

        /**
         * The amount of brake applied as a percentage.
         */
        public static final String BRAKE           = "Brake";
        
        /**
         * The amount of brake applied as pressure.
         */
        public static final String BRAKEPRESSURE     = "BrakePressure";
        
        /**
         * The amount of throttle applied as a percentage.
         */
        public static final String THROTTLE        = "Throttle";
        
        /**
         * The amount of how much the clutch is engaged as a percentage.
         * Note, the clutch is 100% engaged when the pedal is up ad goes towards 0% as you press the pedal to disengage it.
         */
        public static final String CLUTCH          = "Clutch";
        
        /**
         * The position of the steering wheel where straight up is -90deg.
         */
        public static final String STEERING        = "Steering";

        /**
         * The type of compound the tire is made out of.
         * Possible values are blank (Unknown), S (Soft), M (Medium), H (Hard)
         */
        public static final String TIRECOMPOUND  = "TireCompound";
        
        /**
         * The amount of cold pressure in the LF tire. 
         */
        public static final String TIREPRESSURELF  = "TirePressureLF";

        /**
         * The amount of cold pressure in the LR tire. 
         */
        public static final String TIREPRESSURELR  = "TirePressureLR";

        /**
         * The amount of cold pressure in the RF tire. 
         */
        public static final String TIREPRESSURERF  = "TirePressureRF";

        /**
         * The amount of cold pressure in the RR tire. 
         */
        public static final String TIREPRESSURERR  = "TirePressureRR";

        /**
         * The temperature of the LF tire's Left Side (Outside).
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIRETEMPLFL     = "TireTempLFL";

        /**
         * The temperature of the LF tire's Middle.
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIRETEMPLFM     = "TireTempLFM";

        /**
         * The temperature of the LF tire's Right Side (Inside).
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIRETEMPLFR     = "TireTempLFR";

        /**
         * The temperature of the LR tire's Left Side (Outside).
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIRETEMPLRL     = "TireTempLRL";

        /**
         * The temperature of the LR tire's Middle.
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIRETEMPLRM     = "TireTempLRM";

        /**
         * The temperature of the LR tire's Right Side (Inside).
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIRETEMPLRR     = "TireTempLRR";

        /**
         * The temperature of the RF tire's Left Side (Inside).
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIRETEMPRFL     = "TireTempRFL";

        /**
         * The temperature of the RF tire's Middle.
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIRETEMPRFM     = "TireTempRFM";

        /**
         * The temperature of the RF tire's Right Side (Outside).
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIRETEMPRFR     = "TireTempRFR";

        /**
         * The temperature of the RR tire's Left Side (Inside).
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIRETEMPRRL     = "TireTempRRL";

        /**
         * The temperature of the RR tire's Middle.
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIRETEMPRRM     = "TireTempRRM";

        /**
         * The temperature of the RR tire's Right Side (Outside).
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIRETEMPRRR     = "TireTempRRR";


        /**
         * The remaining wear of the LF tire's Left Side (Inside) as a percentage.
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIREWEARLFL     = "TireWearLFL";

        /**
         * The remaining wear of the LF tire's Middle as a percentage.
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIREWEARLFM     = "TireWearLFM";

        /**
         * The remaining wear of the LF tire's Right Side (Inside) as a percentage.
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIREWEARLFR     = "TireWearLFR";

        /**
         * The remaining wear of the LR tire's Left Side (Inside)as a percentage.
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIREWEARLRL     = "TireWearLRL";

        /**
         * The remaining wear of the LR tire's Middle as a percentage.
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIREWEARLRM     = "TireWearLRM";

        /**
         * The remaining wear of the LR tire's Right Side (Inside) as a percentage.
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIREWEARLRR     = "TireWearLRR";

        /**
         * The remaining wear of the RF tire's Left Side (Inside) as a percentage.
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIREWEARRFL     = "TireWearRFL";

        /**
         * The remaining wear of the RF tire's Middle as a percentage.
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIREWEARRFM     = "TireWearRFM";

        /**
         * The remaining wear of the RF tire's Right Side (Outside) as a percentage.
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIREWEARRFR     = "TireWearRFR";

        /**
         * The remaining wear of the RR tire's Left Side (Inside) as a percentage.
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIREWEARRRL     = "TireWearRRL";

        /**
         * The remaining wear of the RR tire's Middle as a percentage.
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIREWEARRRM     = "TireWearRRM";

        /**
         * The remaining wear of the RR tire's Right Side (Outside) as a percentage.
         * The reading is taken after the tire is changed during a pit stop. 
         */
        public static final String TIREWEARRRR     = "TireWearRRR";

        /**
         * Front Flap
         */
        public static final String FRONTFLAP = "FrontFlap";
        
        /**
         * The number of Fast Repairs you have left
         * 
         */
        public static final String FASTREPAIRS = "FastRepairs";
        
        /*
         * In-Car Adjustable Gauges
         */
        public static final String ABS                  = "ABS";
        public static final String ANTIROLLFRONT        = "AntiRollFront";
        public static final String ANTIROLLREAR         = "AntiRollRear";
        public static final String BOOSTLEVEL           = "BoostLevel";
        public static final String BRAKEBIASADJUSTMENT  = "BrakeBiasAdjustment";
        public static final String DIFFENTRY            = "DiffEntry";
        public static final String DIFFEXIT             = "DiffExit";
        public static final String DIFFMIDDLE           = "DiffMiddle";
        public static final String DIFFPRELOAD          = "DiffPreload";
        public static final String DISABLEFUELCUT       = "DisableFuelCut";
        public static final String ENGINEBRAKING        = "EngineBraking";
        public static final String ENGINEPOWER          = "EnginePower";
        public static final String FULLCOURSEYELLOWMODE = "FullCourseYellowMode";
        public static final String FUELCUTPOSITION      = "FuelCutPosition";
        public static final String FUELMIXTURE          = "FuelMixture";
        public static final String HYSBOOSTHOLD         = "HYSBoostHold";
        public static final String HYSDISABLEBOOSTHOLD  = "HYSDisableBoostHold";
        public static final String HYSCHARGE            = "HYSCharge";
        public static final String HYSDEPLOYMENT        = "HYSDeployment";
        public static final String HYSDEPLOYMODE        = "HYSDeployMode";
        public static final String HYSDEPLOYTRIM        = "HYSDeployTrim";
        public static final String HYSREGENGAIN         = "HYSRegenGain";
        public static final String INLAPMODE            = "InLapMode";
        public static final String LAUNCHRPM            = "LaunchRPM";
        public static final String LOWFUELACCEPT        = "LowFuelAccept";
        public static final String PEAKBRAKEBIAS        = "PeakBrakeBias";
        public static final String PITSPEEDLIMITER      = "PitSpeedLimiter";
        public static final String RFBRAKECONNECTED     = "RFBrakeConnected";
        public static final String STARTER              = "Starter";
        public static final String THROTTLESHAPE        = "ThrottleShape";
        public static final String TOPWING              = "TopWing";
        public static final String TRACTIONCONTROL      = "TractionControl";
        public static final String TRACTIONCONTROLFRONT = "TractionControlFront";
        public static final String TRACTIONCONTROLREAR  = "TractionControlRear";
        public static final String WEIGHTJACKERRIGHT    = "WeightJackerRight";
        public static final String WEIGHTJACKERLEFT     = "WeightJackerLeft";
    }

    /**
     * This class is for managing a state that the gauge can be in.
     * A State has a starting and ending range, what the State is named, 
     * and if the value should be transformed to a different value.
     *
     */
    private class StateRange {
        /**
         * The starting value of the range, inclusive.
         */
        public Data start;
        /**
         * The ending value of the range, exclusive.
         */
        public Data end;
        /**
         * The name of the state.
         */
        public String state;
        /**
         * An optional value that will be returned instead of the original value.
         * The value does not have to be a number, it can be a string. 
         */
        public Data value = null;
        /**
         * A constructor to be used when defining a state.
         * @param state The name of the state.
         * @param start The starting value.
         * @param end   The ending value.
         * @param UOM   The UOM of these values.
         */
        public StateRange(String state, double start, double end, String UOM) {
            this.state = state;
            this.start = new Data("",start,UOM);
            this.end   = new Data("",end,UOM);
        }
        /**
         * A constructor to be used when defining a state that transforms the return value.
         * @param state The name of the state.
         * @param start The starting value.
         * @param end   The ending value.
         * @param UOM   The UOM of these values.
         * @param value The new value.
         */
        public StateRange(String state, double start, double end, String UOM, Data value) {
            this.state = state;
            this.start = new Data("",start,UOM);
            this.end   = new Data("",end,UOM);
            this.value = value;
        }
    }

    protected Car m_car;
    protected String m_carIdentifier;
    protected String m_type;
    protected String m_UOM;
    protected String m_imperial;    //what to return if measurement system is imperial
    protected String m_metric;      //what to return if measurement system is metric
    protected String m_measurementSystem = "";  //either METRIC or IMPERIAL or none to leave alone
    protected String m_name = "";
    protected String m_typeName = "";
    protected boolean m_stateAscending;
    protected double m_multiplier;
    protected Data m_minimum;
    protected Data m_maximum;
    protected Data m_majorIncrement;
    protected Data m_minorIncrement;
//    protected Data m_RPMPitRoadSpeed;
    protected Data m_capacityMinimum;
    protected Data m_capacityMaximum;
    protected Data m_capacityIncrement;
    protected boolean m_isFixed;
    protected boolean m_isChangable;
    protected boolean m_changeFlag;
    protected boolean m_isDirty;
    protected boolean m_onResetChange;
    protected Map<String,TreeMap<Double,StateRange>> m_states = null;
    protected String m_reader;
    protected int m_lapChanged;
    
    @SuppressWarnings("unused")
    private Gauge() {}

    /**
     * Constructor for the Gauge class.
     * Some gauges change depending on the track.
     * 
     * The UOM will be set my the SIM to either what is naturally displayed on this car at this track.
     * Or, it could let the user switch between Imperial and Metric.
     * 
     * @param type The type of gauge as defined by {@link com.SIMRacingApps.Gauge.Type}
     * @param car The car to associate this gauge with.
     * @param track The track the car will be running on.
     * @param simGaugesBefore A map that contains gauge data from the SIM to be applied first. The files can then override those values if needed.
     * @param simGaugesAfter A map that contains gauge data from the SIM to be applied after the files are processed to override any values in them.
     */
    public Gauge(
          String type
        , Car car
        , Track track
        , Map<String, Map<String, Map<String, Object>>> simGaugesBefore
        , Map<String, Map<String, Map<String, Object>>> simGaugesAfter
    ) {
        this.m_car = car;
        this.m_carIdentifier = "I" + m_car.getId().getString();
        this.m_type = type;
        this.m_UOM = "";
        this.m_stateAscending = true;
        this.m_multiplier = 1.0;
        this.m_isFixed = false;
        this.m_isChangable = false;
        this.m_changeFlag = false;
        this.m_isDirty = false;
        this.m_onResetChange = false;
        this.m_minimum = new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/Minimum",0.0,"",Data.State.NORMAL);
        this.m_maximum = new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/Maximum",100.0,"",Data.State.NORMAL);
        this.m_majorIncrement = new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/MajorIncrement",10.0,"",Data.State.NORMAL);
        this.m_minorIncrement = new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/MinorIncrement",2.0,"",Data.State.NORMAL);
//        this.m_RPMPitRoadSpeed = new Data(0.0,Data.State.NORMAL);
        this.m_capacityMinimum = new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/CapacityMinimum",0.0,"",Data.State.NORMAL);
        this.m_capacityMaximum = new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/CapacityMaximum",100.0,"",Data.State.NORMAL);
        this.m_capacityIncrement = new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/CapacityIncrement",1.0,"",Data.State.NORMAL);
        this.m_states = new HashMap<String,TreeMap<Double,StateRange>>();
        this.m_reader = "";
        this.m_lapChanged = 1;
        
        //now read the json profiles, gauges passed in first, then default, then overrides from the SIM
        ArrayList<Map<String, Map<String, Map<String, Object>>>> gaugesList = new ArrayList<Map<String, Map<String, Map<String, Object>>>>();
        gaugesList.add(simGaugesBefore);
        gaugesList.add(car._getDefaultGauges());
        gaugesList.add(car._getSIMGauges());
        gaugesList.add(simGaugesAfter);
        
        for (int i=0; i < gaugesList.size(); i++) {
            Map<String, Map<String, Map<String, Object>>> gauges = gaugesList.get(i);
            
            if (gauges != null && gauges.get(type) != null) {
                
                Map<String, Map<String, Object>> gauge = gauges.get(type);
                __loadGauge(gauge,"default","");
                __loadGauge(gauge,track.getName().getString(),"");
                
                //now load gear specific gauges if they exist
                //If they do exist in the .json file, then there has to be an entry for every gear position
                if (type.equalsIgnoreCase(Gauge.Type.TACHOMETER)) {
                    String [] gears = {"R","N","1","2","3","4","5","6","7","8"};
                    for (String gear : gears) {
                        __loadGauge(gauge,"default","-"+gear);
                        __loadGauge(gauge,track.getName().getString(),"-"+gear);
                    
                        String [] powers = {"1","2","3","4","5","6","7","8"};
                        for (String power : powers) {
                            __loadGauge(gauge,"default","-"+gear+"-"+power);
                            __loadGauge(gauge,track.getName().getString(),"-"+gear+"-"+power);
                        }
                    }
                }
            }
        }
        
	    //if the .json profile does not specify the Imperial or Metric UOMs
        //then use the default from the Data class
        if (this.m_UOM.equalsIgnoreCase("kg") || this.m_UOM.equalsIgnoreCase("lb")) {
            m_metric   = "kg"; 
            m_imperial = "lb";
        }
        else {
            if (m_imperial == null)
                this.m_imperial = new Data("",0,this.m_UOM).convertUOM("IMPERIAL").getUOM();
            if (m_metric == null)
                this.m_metric = new Data("",0,this.m_UOM).convertUOM("METRIC").getUOM();
        }
    }

    /**
      * Returns the type of gauge as defined by {@link com.SIMRacingApps.Gauge.Type}
      * 
      * <p>PATH = {@link #getType() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/Type}
      * 
      * @return the type of gauge
      */
    public Data getType() { return new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/Type",m_type,"",Data.State.NORMAL); }

    /**
      * Returns the untranslated name to display on the gauge. (e.g. WATER)
      * 
      * <p>PATH = {@link #getName() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/Name}
      * @return The name of the gauge.
      */
    public Data getName() { return new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/Name",m_name,"",Data.State.NORMAL); }

    /**
      * Returns farther explanation of the gauge (i.e. TEMP)
      * 
      * <p>PATH = {@link #getTypeName() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/TypeName}
      * 
      * @return A additional explanation of the gauge.
      */
    public Data getTypeName() { return new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/TypeName",m_typeName,"",Data.State.NORMAL); }
    
    /*
     * Returns the Default Unit of Measure for this gauge (i.e. "NATIVE")
     * 
     * <p>PATH = {@link #getDefaultUOM() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/DefaultUOM}
     * 
     * @return The Unit of Measure.
     */
//   public Data getDefaultUOM() { return new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/DefaultUOM","","",Data.State.NORMAL); }

    /**
     * Returns the Unit of Measure for this gauge (i.e. "F") based on the users locale.
     * This may not be the same as the internal UOM that is what all the values are in.
     * All the values are converted to this UOM before returning them.
     * 
     * <p>PATH = {@link #getUOM() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/UOM}
     * 
     * @return The Unit of Measure.
     */
    public Data getUOM() { return new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/UOM",new Data("",0.0,m_UOM).convertUOM(_getGaugeUOM(m_measurementSystem)).getUOM(),"",Data.State.NORMAL); }
   
    /**
      * Return a multiplier to be used when calculating the major and minor tick marks.
      * For example: the tachometer returns values in RPMs ranging from 0 to 11,000. The multiplier could be set to .001, so that
      * the gauge major tick marks go from 0 to 11.
      * 
      * <p>PATH = {@link #getMultiplier() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/Multiplier}
      * 
      * @return The multiplier value.
      */
    public Data getMultiplier() { return new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/Multiplier",m_multiplier,"double",Data.State.NORMAL); }
    
    /**
      * Returns the minimum value of where this gauge should start with the first tick mark (i.e. 0)
      * Note: The value would be after the multiplier has been applied.
      *
      * <p>PATH = {@link #getMinimum(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/Minimum/(UOM)}
      * 
      * @param UOM (Optional) The UOM to convert the value to, defaults to the gauge's UOM.
      *
      * @return The minimum value
      */
    public Data getMinimum(String UOM) { return _getReturnValue(m_minimum,UOM); }
    public Data getMinimum()           { return getMinimum(m_measurementSystem); }
    
    /**
      * Returns the maximum value of where this gauge should end with the last tick mark (i.e. 300).
      * Note: The value would be after the multiplier has been applied.
      *
      * <p>PATH = {@link #getMaximum(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/Maximum/(UOM)}
      * 
      * @param UOM (Optional) The UOM to convert the value to, defaults to the gauge's UOM.
      *
      * @return The maximum value
      */
    public Data getMaximum(String UOM) { return _getReturnValue(m_maximum,UOM); }
    public Data getMaximum()           { return getMaximum(m_measurementSystem); }
    
    /**
      * Returns how often to show major tick marks between the minimum and the maximum.
      * Note: The value would be after the multiplier has been applied.
      *
      * <p>PATH = {@link #getMajorIncrement(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/MajorIncrement/(UOM)}
      * 
      * @param UOM (Optional) The UOM to convert the value to, defaults to the gauge's UOM.
      * @return The major increment value
      */
    public Data getMajorIncrement(String UOM) { 
        Data d = m_majorIncrement.convertUOM(_getGaugeUOM(UOM));
        d.setValue(Math.abs(d.getDouble()));
        return d;
    }
    public Data getMajorIncrement()           { return getMajorIncrement(m_measurementSystem); }

    /**
      * Returns how often to show minor tick marks between the minimum and the maximum.
      * Note: The value would be after the multiplier has been applied.
      *
      * <p>PATH = {@link #getMinorIncrement(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/MinorIncrement/(UOM)}
      * 
      * @param UOM (Optional) The UOM to convert the value to, defaults to the gauge's UOM.
      * @return The minor increment value
      */
    public Data getMinorIncrement(String UOM) { 
        Data d = m_minorIncrement.convertUOM(_getGaugeUOM(UOM));
        d.setValue(Math.abs(d.getDouble()));
        return d;
    }
    public Data getMinorIncrement()           { return getMinorIncrement(m_measurementSystem); }

    /**
      * Returns the maximum number that this gauge will accept when calling {@link com.SIMRacingApps.Gauge#setValueNext(double,String)}
      * or {@link com.SIMRacingApps.Gauge#incrementValueNext(String)}
      * 
      * <p>PATH = {@link #getCapacityMaximum(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/CapacityMaximum/(UOM)}
      * 
      * @param UOM (Optional) The UOM to convert the value to, defaults to the gauge's UOM.
      * @return The maximum capacity.
      */
    public Data getCapacityMaximum(String UOM) { return _getReturnValue(m_capacityMaximum,UOM); }
    public Data getCapacityMaximum()           { return getCapacityMaximum(m_measurementSystem); }

    /**
      * Returns the minimum number that this gauge will accept when calling {@link com.SIMRacingApps.Gauge#setValueNext(double,String)}
      * or {@link com.SIMRacingApps.Gauge#decrementValueNext(String)}
      * 
      * <p>PATH = {@link #getCapacityMinimum(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/CapacityMinimum/(UOM)}
      * 
      * @param UOM (Optional) The UOM to convert the value to, defaults to the gauge's UOM.
      * @return The minimum capacity.
      */
    public Data getCapacityMinimum(String UOM) { return _getReturnValue(m_capacityMinimum,UOM); }
    public Data getCapacityMinimum()           { return getCapacityMinimum(m_measurementSystem); }

    /**
      * Returns the increment value that the gauge uses when you call {@link com.SIMRacingApps.Gauge#incrementValueNext(String)} 
      * or {@link com.SIMRacingApps.Gauge#decrementValueNext(String)}.
      * Also, all values passed to {@link com.SIMRacingApps.Gauge#setValueNext(double,String)} are rounded up to the the
      * closest multiple of this value.
      * 
      * <p>PATH = {@link #getCapacityIncrement(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/CapacityIncrement/(UOM)}
      * 
      * @param UOM (Optional) The UOM to convert the value to, defaults to the gauge's UOM.
      * @return The incremental capacity.
      */
    public Data getCapacityIncrement(String UOM) { 
        Data d = m_capacityIncrement.convertUOM(_getGaugeUOM(UOM));
        d.setValue(Math.abs(d.getDouble()));
        return d;
    }
    public Data getCapacityIncrement()           { return getCapacityIncrement(m_measurementSystem); }

    /**
      * Returns false if this gauge's next value can be changed via {@link com.SIMRacingApps.Gauge#setValueNext(double,String)}, 
      * {@link com.SIMRacingApps.Gauge#incrementValueNext(String)} or {@link com.SIMRacingApps.Gauge#decrementValueNext(String)}.
      * A client could use this value to grey out or hide the buttons for changing it.
      * 
      * <p>PATH = {@link #getIsFixed() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/IsFixed}
      * 
      * @return true if gauge is fixed, false of not.
      */
    public Data getIsFixed() { return new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/IsFixed",m_isFixed,"boolean",Data.State.NORMAL);}
    
    /**
      * Returns true if this gauge represents an object that can be replaced/changed at the next pit stop (i.e. Tires, Tearoff usually)
      * 
      * <p>PATH = {@link #getIsChangeable() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/IsChangeable}
      * 
      * @return true if gauge is changeable, false of not.
      */
    public Data getIsChangeable() { return new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/IsChangeable",m_isChangable,"boolean",Data.State.NORMAL);}

    /**
     * Returns true if this gauge is changed on a reset. 
     * 
     * <p>PATH = {@link #getOnResetChange() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/OnResetChange}
     * 
     * @return true if gauge is changed on a reset, false of not.
     */
   public Data getOnResetChange() { return new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/OnResetChange",m_onResetChange,"boolean",Data.State.NORMAL);}
   
    /*
     * Rounds the value to the nearest increment and keeps between the min and max inclusively.
     */
    protected double _roundToIncrement(double value,String UOM) {
        
        double d = value;
        double increment = this.m_capacityIncrement.convertUOM(_getGaugeUOM(UOM)).getDouble();
        double minimum   = this.m_capacityMinimum.convertUOM(_getGaugeUOM(UOM)).getDouble(); 
        double maximum   = this.m_capacityMaximum.convertUOM(_getGaugeUOM(UOM)).getDouble();
        
        double floored_d = Math.floor(d / increment) * increment; //floor it to the closest increment

        if ((floored_d + (increment/2.0)) <= d)
            d = floored_d + increment;
        else
            d = floored_d;

        //don't let it round down below the max capacity, if it was originally above the max capacity
        if (d < maximum && value >= maximum)
            d = maximum;
        
        //by the same rule if it rounds below the min capacity
        if (d < minimum && value >= minimum)
            d = minimum;
        
        return d;
    }

    /*
     * Rounds the value up to the nearest capacity increment if not at an increment value already
     * and keeps between the min and max inclusively.
     */
    protected double _roundUpToIncrement(double value,String UOM) {
        double d = value;
        double increment = this.m_capacityIncrement.convertUOM(_getGaugeUOM(UOM)).getDouble();
        double minimum   = this.m_capacityMinimum.convertUOM(_getGaugeUOM(UOM)).getDouble(); 
        double maximum   = this.m_capacityMaximum.convertUOM(_getGaugeUOM(UOM)).getDouble();
        
        double floored_d = Math.floor(d / increment) * increment; //floor it to the closest increment

        //now add an increment if we are below the requested value
        if ((floored_d + 0.001) < d)
            d = floored_d + increment;
        else
            d = floored_d;

        //don't let it round down below the max capacity, if it was originally above the max capacity
        if (d < maximum && value >= maximum)
            d = maximum;
        
        //by the same rule if it rounds below the min capacity
        if (d < minimum && value >= minimum)
            d = minimum;
        
        return d;
    }
    
    /**
     * Returns Y if the next value of the gauge is different from the current value.
     * 
     * <p>PATH = {@link #getIsDirty() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/IsDirty}
     * 
     * @return Y if dirty, N if not.
     */
    public Data getIsDirty() { return new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/IsDirty",m_isDirty,"boolean",Data.State.NORMAL); }

    /**
     * Returns the current value of the gauge. 
     * 
     * <p>PATH = {@link #getValueCurrent(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/ValueCurrent/(UOM)}
     * 
     * @param UOM (Optional) The unit of measure to return, default to the gauges UOM.
     * @return The current value.
     */
    public Data getValueCurrent(String UOM) { return _getReturnValue(new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/ValueCurrent",0.0,"",Data.State.NOTAVAILABLE),""); }
    public Data getValueCurrent()           { return getValueCurrent(m_measurementSystem); }

    /**
     * Returns the lap when the object for this gauge was changed.
     * 
     * <p>PATH = {@link #getLapChanged() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/LapChanged}
     * 
     * @return The lap it was changed.
     */
    public Data getLapChanged() { return new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/LapChanged",m_lapChanged,"lap",Data.State.NORMAL); }

    /**
     * Returns the number of times this gauge was used.
     * 
     * <p>PATH = {@link #getCount() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/Count} 1.2
     * 
     * @since 1.2
     * @return The number of times this gauge was changed.
     */
    public Data getCount() { return new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/Count",1,"",Data.State.NOTAVAILABLE); }
    
    /**
     * Returns the number of times this gauge can be changed.
     * By default, a gauge will return the NOTAVAILABLE status unless a specific gauge overrides it.
     * 
     * <p>PATH = {@link #getMaxCount() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/MaxCount} 1.13
     * 
     * @since 1.13
     * @return The number of times this gauge can be changed.
     */
    public Data getMaxCount() { return new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/MaxCount",999,"",Data.State.NOTAVAILABLE); }
    
    /**
     * Returns the number of laps since the object for this gauge was changed.
     * By default, a gauge will return the NOTAVAILABLE status unless a specific gauge overrides it.
     * 
     * <p>PATH = {@link #getLaps(int) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/Laps/(CURRENTLAP)}
     * 
     * @param currentLap (Optional) The lap you are currently on, defaults to current lap.
     * @return The number of laps.
     */
    public Data getLaps(int currentLap)    { return new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/Laps",currentLap - getLapChanged().getInteger() > 0 ? currentLap - getLapChanged().getInteger() : 0,"lap",Data.State.NORMAL); }
    public Data getLaps(String currentLap) { return getLaps(Integer.parseInt(currentLap)); }
    public Data getLaps()                  { return getLaps(1); }

    /**
     * Returns the historical value of the gauge taken at the time it was changed.
     * 
     * <p>PATH = {@link #getValueHistorical(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/ValueHistorical/(UOM)}
     * 
     * @param UOM (Optional) The unit of measure to return it in, defaults to the gauges UOM.
     * @return    The historical value.
     */
    public Data getValueHistorical(String UOM) { return _getReturnValue(new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/ValueHistorical",0.0,"",Data.State.NOTAVAILABLE),""); }
    public Data getValueHistorical()           { return getValueHistorical(m_measurementSystem); }

    /**
     * Returns the number of laps since the last change.
     * 
     * <p>PATH = {@link #getLapsHistorical() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/LapsHistorical}
     * 
     * @return The lap number of laps since the last change.
     */
    public Data getLapsHistorical() { return new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/LapHistorical",0,"lap",Data.State.NOTAVAILABLE); }

    /**
     * Returns the value of the next value that the object will be at the next pit stop.
     * By default it returns the current value.
     *
     * <p>PATH = {@link #getValueNext(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/ValueNext/(UOM)}
     * 
     * @param UOM The unit of value to return.
     * @return The value if the next value.
     */
    public Data getValueNext(String UOM) { return getValueCurrent(); }
    public Data getValueNext()           { return getValueNext(m_measurementSystem); }

    /**
     * Returns Y if the object for this gauge is flagged for changed on the next pit stop.
     * 
     * <p>PATH = {@link #getChangeFlag() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/ChangeFlag}
     * 
     * @return Y if flagged for changed, N if not.
     */
    public Data getChangeFlag() { return new Data("Car/"+m_carIdentifier+"/Gauge/"+m_type+"/ChangeFlag",m_changeFlag,"boolean",Data.State.NORMAL); }

    /**
     * Sets the change flag for this gauge and all the gauges grouped with it.
     * By default, this will not all the change flag to be true.
     * Therefore, each gauge that can be changed, must override this and set the flag.
     * 
     * <p>PATH = {@link #setChangeFlag(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/setChangeFlag/(FLAG)}
     * 
     * @param flag Y or N to change or not to change
     * @return     The change flag
     */
    public Data setChangeFlag(boolean flag) { return getChangeFlag(); }
    public Data setChangeFlag(String flag)  { return setChangeFlag(new Data("",flag).getBoolean()); }

    /**
     * Decrements the next value, to be applied at the next pit stop, according to the value set by {@link com.SIMRacingApps.Gauge#_setCapacityIncrement(double,String)}. 
     * If the gauge is fixed, then only the change flag will be set and the next value will not be changed. 
     * The change flag is set to Y even if the new value and the old value are the same.
     * 
     * <p>PATH = {@link #decrementValueNext(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/decrementValueNext/(UOM)}
     * 
     * @param UOM (Optional) The unit of measure to return the new value in. Default gauge's UOM.
     * @return    The new value.
     */
    public Data decrementValueNext(String UOM) { return getValueNext(UOM); }
    public Data decrementValueNext()           { return decrementValueNext(m_measurementSystem); }

    /**
     * Increments the next value, to be applied at the next pit stop, according to the value set by {@link com.SIMRacingApps.Gauge#_setCapacityIncrement(double,String)}. 
     * If the gauge is fixed, then only the change flag will be set and the next value will not be changed. 
     * If increments above the maximum capacity, then it sets it to the maximum capacity. 
     * The change flag is set to Y even if the new value and the old value are the same.
     * 
     * <p>PATH = {@link #incrementValueNext(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/incrementValueNext/(UOM)}
     * 
     * @param UOM (Optional) The unit of measure to return the new value in. Default to gauge's UOM.
     * @return    The new value.
     */
    public Data incrementValueNext(String UOM) { return getValueNext(UOM); }
    public Data incrementValueNext()           { return incrementValueNext(m_measurementSystem); }
    
    /**
     * Sets the next value to be applied at the next pit stop.
     * If the gauge is fixed, then only the change flag will be set and the next value will not be changed. 
     * If below the minimum capacity, then it sets it to the minimum capacity.
     * If above the maximum capacity, then it sets it to the maximum capacity.
     * The value is rounded to the nearest increment as defined by {@link com.SIMRacingApps.Gauge#_setCapacityIncrement(double,String)}.
     * The change flag is set to Y even if the new value and the old value are the same.
     * 
     * <p>PATH = {@link #setValueNext(double, String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/setValueNext/(VALUE)/(UOM)}
     * 
     * @param value (Optional) The value to be set. Default 0. 
     * @param UOM   (Optional) The unit of measure the new measure to be set is in. Also affects return value. Default to gauge's UOM.
     * @return      The new value, possibly adjusted.
     */
    public Data setValueNext(double value, String UOM) { return getValueNext(UOM); }
    public Data setValueNext(String value, String UOM) { return setValueNext(Double.parseDouble(value),UOM); }
    public Data setValueNext(String value)             { return setValueNext(value,m_measurementSystem); }
    public Data setValueNext()                         { return setValueNext("0"); }

    /**
     * Adds all the states from the passed in gauge to this gauge.
     * @param gauge The gauge to copy the states from
     */
    public void _addStateRange(Gauge gauge) {
        Iterator<Entry<String, TreeMap<Double, StateRange>>> stateNamesItr = gauge.m_states.entrySet().iterator();
        while (stateNamesItr.hasNext()) {
            Entry<String, TreeMap<Double, StateRange>> stateNames = stateNamesItr.next();
            String stateName = stateNames.getKey();
            TreeMap<Double, StateRange> states = stateNames.getValue();
            Iterator<Entry<Double, StateRange>> itr = states.entrySet().iterator();
            while (itr.hasNext()) {
                StateRange state = itr.next().getValue();
                _removeStateRange(stateName,state.state);
                _addStateRange(stateName,state.state,state.start.getDouble(),state.end.getDouble(),state.start.getUOM(),state.value);
            }
        }
    }
    
    /*
     * Add a new state range to this gauge. 
     * The values should be in the same UOM that getInternalUOM() returns.
     * @param name  The name of the state.
     * @param start The starting value, inclusive.
     * @param end   The ending value, exclusive.
     */
    public void _addStateRange(String stateName, String name,double start,double end,String UOM) {
        _removeStateRange(stateName,name);

        TreeMap<Double,StateRange> states = m_states.get(stateName);

        if (states == null)
            m_states.put(stateName, states = new TreeMap<Double,StateRange>());
        
        states.put(start, new StateRange(name.toUpperCase(),start,end,UOM));
    }

    /*
     * Add a new state range to this gauge that transforms the value.
     * The values should be in the same UOM that getInternalUOM() returns.
     * @param name  The name of the state.
     * @param start The starting value, inclusive.
     * @param end   The ending value, exclusive.
     * @param d     The new value
     */
    public void _addStateRange(String stateName,String name,double start,double end, String UOM, Data d) {
        _removeStateRange(stateName,name);

        TreeMap<Double,StateRange> states = m_states.get(stateName);

        if (states == null)
            m_states.put(stateName, states = new TreeMap<Double,StateRange>());
        
        states.put(start, new StateRange(name.toUpperCase(),start,end,UOM,d));
        
    }

    /*
     * Removes a state.
     * 
     * @param name The name of the state.
     */
    public void _removeStateRange(String stateName, String name) {
        
        TreeMap<Double,StateRange> states = m_states.get(stateName);
        
        if (states != null) {
            Iterator<Entry<Double,StateRange>> itr = states.entrySet().iterator();
            //remove the name if it exists
            while (itr.hasNext()) {
                StateRange range = itr.next().getValue();
                if (range.state.equals(name.toUpperCase()))
                    itr.remove();
            }
        }
    }
    
    /*
     * Returns the UOM for this gauge to display
     * @return The UOM
     */
    protected String _getGaugeUOM(String UOM) {
        return  UOM.equalsIgnoreCase("METRIC") ? m_metric : UOM.equalsIgnoreCase("IMPERIAL") ? m_imperial : UOM;
    }
    
    /*
     * Prepares a value to be returned. 
     * It converts to the request UOM, sets the state and state percentage
     * @return A data value prepared for the user.
     */
    protected Data _getReturnValue(Data d,String UOM,String gear, String power) {
        //use the imperial and metric UOM from the json file instead of the global one.
        //this allows each car/gauge to decide what UOM to use.
        //was mainly done for the quart gauges. The global one would return gallons.
        Data r = d.convertUOM(_getGaugeUOM(UOM));

        TreeMap<Double, StateRange> states = null;
        if (!gear.isEmpty() && !power.isEmpty()) 
            states = m_states.get("-"+gear+"-"+power);
        if (!gear.isEmpty() && states == null)
            states = m_states.get("-"+gear);
        if (states == null)
            states = m_states.get("");

        if (states != null 
           //if state has not already been set, the look it up
        && (  d.getState().equalsIgnoreCase(Data.State.NORMAL)
           || d.getState().equalsIgnoreCase(Data.State.ERROR)
           || d.getState().equalsIgnoreCase(Data.State.NOTAVAILABLE)
           || d.getState().equalsIgnoreCase(Data.State.OFF)
           )
        ) {
            //now translate the value if provided
            Iterator<Entry<Double,StateRange>> itr = m_stateAscending 
                                                   ? states.entrySet().iterator()
                                                   : states.descendingMap().entrySet().iterator();
    
            //pick the one with the highest start if the ranges overlap.
            //all ranges overlap NORMAL
            double v = r.convertUOM(this.m_UOM).getDouble(); //must do the compares of the states in Gauges UOM 
            while (itr.hasNext()) {
                StateRange range = itr.next().getValue();
                //if the value is within the range 
                if ((v >= range.start.convertUOM(m_UOM).getDouble() && v < range.end.convertUOM(m_UOM).getDouble())) {
                    //only if the original state was NORMAL, change the state
                    if (d.getState().equalsIgnoreCase(Data.State.NORMAL)) {
                        r.setState(range.state);
                        r.setStatePercent(((v - range.start.convertUOM(m_UOM).getDouble()) / (range.end.convertUOM(m_UOM).getDouble() - range.start.convertUOM(m_UOM).getDouble())) * 100.0);
                    }
                    if (range.value != null)
                        r.setValue(range.value.getValue(),range.value.getUOM());
                }
            }
        }
        
        return r;
    }
    protected Data _getReturnValue(Data d,String UOM) { return _getReturnValue(d,UOM,"",""); }
    
    //This setters allows the gauges to be modified by the SIM after the JSON files have been loaded
    public Data _setMinimum(double d, String uom) { return m_minimum.setValue(d,uom); }
    public Data _setMaximum(double d, String uom) { return m_maximum.setValue(d,uom); }
    public Data _setMajorIncrement(double d, String uom) { return m_majorIncrement.setValue(d,uom); }
    public Data _setMinorIncrement(double d, String uom) { return m_minorIncrement.setValue(d,uom); }
    public Data _setCapacityMinimum(double d, String uom) { return m_capacityMinimum.setValue(d,uom); }
    public Data _setCapacityMaximum(double d, String uom) { return m_capacityMaximum.setValue(d,uom); }
    public Data _setCapacityIncrement(double d, String uom) { return m_capacityIncrement.setValue(d,uom); }
    public Data _setIsFixed(boolean b) { m_isFixed = b; return getIsFixed(); }
    public Data _setIsChangable(boolean b) { m_isChangable = b; return getIsChangeable(); }
    public Data _setIsDirty(boolean b) {  m_isDirty = b; return getIsDirty(); }
    public Data _setOnResetChange(boolean b) { m_onResetChange = b; return getOnResetChange(); }
    
    /****************************************/
    /****************************************/
    /*********** PRIVATE ********************/
    /****************************************/
    /****************************************/
    
    private void __loadGauge(Map<String, Map<String,Object>> gaugemap,String trackName,String stateName) {
        String s;
        Double d;
        Boolean b;
        
        Map<String,Object> trackmap = gaugemap.get(trackName+stateName);
    
        if (trackmap != null) {
            if ((s = (String)trackmap.get("Name"))              != null) m_name = s;
            if ((s = (String)trackmap.get("TypeName"))          != null) m_typeName = s;
            if ((s = (String)trackmap.get("UOM"))               != null) m_UOM = s;
            if ((s = (String)trackmap.get("imperial"))          != null) m_imperial = s;
            if ((s = (String)trackmap.get("metric"))            != null) m_metric = s;
            if ((b = (Boolean)trackmap.get("StateAscending"))   != null) m_stateAscending = b;
            if ((d = (Double)trackmap.get("Multiplier"))        != null) m_multiplier = d;
            if ((d = (Double)trackmap.get("Minimum"))           != null) _setMinimum(d,m_UOM);
            if ((d = (Double)trackmap.get("Maximum"))           != null) _setMaximum(d,m_UOM);
            if ((d = (Double)trackmap.get("MajorIncrement"))    != null) _setMajorIncrement(d,m_UOM);
            if ((d = (Double)trackmap.get("MinorIncrement"))    != null) _setMinorIncrement(d,m_UOM);
//            if ((d = (Double)trackmap.get("RPMPitRoadSpeed"))   != null) m_RPMPitRoadSpeed.setValue(d,m_UOM);
            if ((d = (Double)trackmap.get("CapacityMinimum"))   != null) _setCapacityMinimum(d,m_UOM);
            if ((d = (Double)trackmap.get("CapacityMaximum"))   != null) _setCapacityMaximum(d,m_UOM);
            if ((d = (Double)trackmap.get("CapacityIncrement")) != null) _setCapacityIncrement(d,m_UOM);
            if ((b = (Boolean)trackmap.get("IsFixed"))          != null) _setIsFixed(b);
            if ((b = (Boolean)trackmap.get("IsChangable"))      != null) _setIsChangable(b);
            if ((b = (Boolean)trackmap.get("OnResetChange"))    != null) _setOnResetChange(b);
            if ((s = (String)trackmap.get("Reader"))            != null) m_reader = s;
    
            @SuppressWarnings("unchecked")
            Map<String,Map<String,Object>> states = (Map<String,Map<String,Object>>)trackmap.get("States");
            if (states != null) {
                Iterator<Entry<String, Map<String, Object>>> itr = states.entrySet().iterator();
                while (itr.hasNext()) {
                    Entry<String, Map<String,Object>> state = itr.next();
                    if (state.getValue().get("Value") != null) {
                        _addStateRange(
                            stateName,
                            state.getKey(),
                            (Double)state.getValue().get("Start"),
                            (Double)state.getValue().get("End"),
                            m_UOM,
                            new Data((String)state.getValue().get("Name"),state.getValue().get("Value"),"",Data.State.NORMAL)
                        );
                    }
                    else {
                        _addStateRange(
                                stateName,
                                state.getKey(),
                                (Double)state.getValue().get("Start"),
                                (Double)state.getValue().get("End"),
                                m_UOM
                        );
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        String s = "{}";
        try {
            //cheat and let the plugin serialize this.
            s = this.m_car.m_SIMPlugin.getData("Car/"+m_carIdentifier+"/Gauge/"+m_type).getString();
        } catch (SIMPluginException e) {}
        return s;
    }
}

