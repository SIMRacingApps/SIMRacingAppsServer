package com.SIMRacingApps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.SIMRacingApps.Data;

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
 * Standard States are "OFF", "ERROR" and "NORMAL".
 * Clients can choose to display different colors, blink, or other visual effects based on the state.
 * Many gauges also have additional states. 
 * See {@link com.SIMRacingApps.Gauge.Type} for each gauges documentation for the states it supports.
 * @author Jeffrey Gilliam
 * @since 1.0
 * @copyright Copyright (C) 2015 - 2016 Jeffrey Gilliam
 * @license Apache License 2.0
 * 
 */

public class Gauge {
    

    /**
     * This defines when the SIMValue.get() function is called what it will return when.
     * They are used internally to by the SIM to indicate what the SIM reader will return.
     */
    public enum SIMValueTypes {
        /** return is unknown*/
        Unknown,             
        /** returns the real-time value the car is currently outputting */
        ForCar,              
        /** returns the value the car is currently outputting, but setupValue should be set to zero after pitting*/
        ForCarZeroOnPit,     
        /** this value is not updated until you pit. (ie. Tire Temps,Wear)*/
        AfterPit,            
        /** returns the current setup value that will be applied to the car when you pit*/
        ForSetup,            
        /** value is applied to the car and the setup at the same time (ie. brake bias)*/
        ForCarAndSetup,      
        /** SIM doesn't return a value and it should be zero on pit (ie. windshield tearoff)*/
        ZeroOnPit            
    };

    /**
     * This defines all the possible gauge types.
     * Gauges have predefined states based on ranges of values.
     * All gauges support the standard states of 
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
         * The brake bias adjustment value that is set real-time while driving.
         * It is the percentage applied to the brake bias as an adjustment, usually +/-3%.
         * Use the isFixed value to determine if this value can be adjusted while driving.
         */
        public static final String BRAKEBIASADJUSTMENT = "BrakeBiasAdjustment";
        
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
        
        /**
         * The amount of rear wing angle.
         */
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
         * The traction control
         */
        public static final String TRACTIONCONTROL = "TractionControl";
        
        /**
         * The Automatic Braking System (ABS) 
         */
        public static final String ABS = "ABS";
        
        /**
         * The Anti Roll Bar Front
         */
        public static final String ANTIROLLFRONT = "AntiRollFront";
        
        /**
         * The Anti Roll Bar Rear
         */
        public static final String ANTIROLLREAR = "AntiRollRear";
        
        /**
         * The Fuel Mixture
         */
        public static final String FUELMIXTURE = "FuelMixture";
        
        /**
         * Throttle Shaping
         */
        public static final String THROTTLESHAPE = "ThrottleShape";
        
        /**
         * Power Steering Assist
         */
        public static final String POWERSTEERINGASSIST = "PowerSteeringAssist";
        
        /**
         * Engine Power
         */
        public static final String ENGINEPOWER = "EnginePower";
        
        /**
         * The number of clicks on the Weight Jacker Right
         */
        public static final String WEIGHTJACKERRIGHT = "WeightJackerRight";
        
        /**
         * The number of clicks on the Weight Jacker Left
         */
        public static final String WEIGHTJACKERLEFT = "WeightJackerLeft";

        /**
         * Front Flap
         */
        public static final String FRONTFLAP = "FrontFlap";
        
        /**
         * Engine Braking
         */
        public static final String ENGINEBRAKING = "EngineBraking";
        
        /**
         * Diff, Entry, Middle, Exit
         */
        public static final String DIFFENTRY  = "DiffEntry";
        public static final String DIFFMIDDLE = "DiffMiddle";
        public static final String DIFFEXIT   = "DiffExit";
        
        /**
         * The number of Fast Repairs you have left
         * 
         */
        public static final String FASTREPAIRS = "FastRepairs";
        
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
        public double start;
        /**
         * The ending value of the range, exclusive.
         */
        public double end;
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
         */
        public StateRange(String state, double start, double end) {
            this.state = state;
            this.start = start;
            this.end   = end;
        }
        /**
         * A constructor to be used when defining a state that transforms the return value.
         * @param state The name of the state.
         * @param start The starting value.
         * @param end   The ending value.
         * @param value The new value.
         */
        public StateRange(String state, double start, double end, Data value) {
            this.state = state;
            this.start = start;
            this.end   = end;
            this.value = value;
        }
    }

    private String m_car;
    private TreeMap<Double,StateRange> m_states = null;
    
    private boolean m_state_ascending = false;
    public boolean getStateAscending() { return m_state_ascending; }
    public void setStateAscending(boolean state_ascending) { m_state_ascending = state_ascending; } 
    
    private String m_type;                
    private String m_name;
    private String m_typename;            
    private String m_uom;
    private String m_defaultUOM;
    private double m_multiplier;          //Adjusts value for gauge major increments
    private double m_minimum;             
    private double m_maximum;
    private double m_majorIncrement;      
    private double m_minorIncrement;
    private double m_capacityMaximum;     
    private double m_capacityMinimum;
    private double m_capacityIncrement;   
    private int m_currentLap;               //the current lap update each tick by calling updateCurrentLap()
    protected int    m_lapChanged;          //the lap when this gauge was changed because we pitted
    protected int  m_count;
//  protected int    m_lapChangedPrevious;  //the previous value of lapChanged whenever you have reset or pitted

//  private boolean m_isInCar;            //returns true if this gauge is a real gauge on the real car or it's virtual from the SIM
    private boolean m_isFixed;              
    private boolean m_isChangeable;          
    private boolean m_onResetChange;
    private boolean m_isDebug;              //returns true if this value setup value cannot be changed
    private boolean m_isSentToSIM;        //returns true if the value has been sent to the SIM
    
    private Data SIMValue;
    private Data m_SIMValue = null;
    private SIMValueTypes SIMValueType;

    //delay initialization so derived classes can change the defaults first
    private Data value = null;
    private Data beforePitValue = null;
    private Data afterPitValue = null;
    private Data afterPitLaps = null;
    private Data setupValue = null;
    private boolean m_changeFlag = false;
    private boolean m_firstreset = true;
    private Map<String,Gauge> m_group = new HashMap<String,Gauge>();


    /**
      * Returns the type of gauge as defined by {@link com.SIMRacingApps.Gauge.Type}
      * 
      * <p>PATH = {@link #getType() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/Type}
      * 
      * @return the type of gauge
      */
    public Data getType()                 { return new Data("Car/"+m_car+"/Gauge/"+m_type+"/Type",m_type,"String",Data.State.NORMAL); }

    /**
      * Returns the untranslated name of the gauge. (e.g. WATER)
      * 
      * <p>PATH = {@link #getName() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/Name}
      * @return The name of the gauge.
      */
    public Data getName()                 { return new Data("Car/"+m_car+"/Gauge/"+m_type+"/Name",m_name,"String",Data.State.NORMAL); }
    /**
      * Sets the Name of the gauge.
      * See {@link com.SIMRacingApps.Gauge#getName()}
      * @param name The name of the gauge.
      */
    public void setName(String name)      { m_name = name; }

    /**
      * Returns farther explanation of the gauge (i.e. TEMP)
      * 
      * <p>PATH = {@link #getTypeName() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/TypeName}
      * 
      * @return A additional explanation of the gauge.
      */
    public Data getTypeName()             { return new Data("Car/"+m_car+"/Gauge/"+m_type+"/TypeName",m_typename,"String",Data.State.NORMAL); }
    
    /**
      * Sets the Type Name of the gauge.
      * See {@link com.SIMRacingApps.Gauge#getTypeName()}
      * @param typename The typename of the gauge.
      */
    public void setTypeName(String typename){ m_typename = typename;}

    /**
     * Returns the Default Unit of Measure for this gauge (i.e. "NATIVE")
     * 
     * <p>PATH = {@link #getDefaultUOM() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/DefaultUOM}
     * 
     * @return The Unit of Measure.
     */
   public Data getDefaultUOM()            { return new Data("Car/"+m_car+"/Gauge/"+m_type+"/DefaultUOM",m_defaultUOM,"String",Data.State.NORMAL); }

   /**
    * Sets the default unit of measure.
    * See {@link com.SIMRacingApps.Gauge#getDefaultUOM()}
    * @param uom The unit of measure.
    */
   public void setDefaultUOM(String uom)  { m_defaultUOM = uom; }
   
    /**
      * Returns the Unit of Measure for this gauge (i.e. "F")
      * 
      * <p>PATH = {@link #getUOM() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/UOM}
      * 
      * @return The Unit of Measure.
      */
    public Data getUOM()                  {
        Data withUOM = new Data("",0.0,m_uom).convertUOM(m_defaultUOM);
        return new Data("Car/"+m_car+"/Gauge/"+m_type+"/UOM",withUOM.getUOM(),"String",Data.State.NORMAL); 
    }
    
    /**
      * Sets the unit of measure.
      * See {@link com.SIMRacingApps.Gauge#getUOM()}
      * @param uom The unit of measure.
      */
    public void setUOM(String uom)        {
        if (m_uom.equalsIgnoreCase(uom))
            return;
        
        m_minimum           = new Data("",m_minimum,          m_uom).addConversion(SIMValue).convertUOM(uom).getDouble();
        m_maximum           = new Data("",m_maximum,          m_uom).addConversion(SIMValue).convertUOM(uom).getDouble();
        m_majorIncrement    = new Data("",m_majorIncrement,   m_uom).addConversion(SIMValue).convertUOM(uom).getDouble();
        m_minorIncrement    = new Data("",m_minorIncrement,   m_uom).addConversion(SIMValue).convertUOM(uom).getDouble();
        m_capacityMaximum   = new Data("",m_capacityMaximum,  m_uom).addConversion(SIMValue).convertUOM(uom).getDouble();
        m_capacityMinimum   = new Data("",m_capacityMinimum,  m_uom).addConversion(SIMValue).convertUOM(uom).getDouble();
        m_capacityIncrement = new Data("",m_capacityIncrement,m_uom).addConversion(SIMValue).convertUOM(uom).getDouble();
        
        TreeMap<Double,StateRange> states = new TreeMap<Double,StateRange>();

        Iterator<Entry<Double, StateRange>> itr = m_states.entrySet().iterator();
        while (itr.hasNext()) {
            StateRange state = itr.next().getValue();
            StateRange newState = new StateRange(
                                    state.state,
                                    new Data("",state.start,m_uom).addConversion(SIMValue).convertUOM(uom).getDouble(),
                                    new Data("",state.end,m_uom).addConversion(SIMValue).convertUOM(uom).getDouble()
                                  );
            if (state.value != null)
                newState.value = state.value.addConversion(SIMValue).convertUOM(uom);
        }
        m_states = states;
        m_uom = uom;
    }

    /**
      * Return a multiplier to be used when calculating the major and minor tick marks.
      * For example: the tachometer returns values in RPMs ranging from 0 to 11,000. The multiplier could be set to .001, so that
      * the gauge major tick marks go from 0 to 11.
      * 
      * <p>PATH = {@link #getMultiplier() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/Multiplier}
      * 
      * @return The multiplier value.
      */
    public Data getMultiplier()           { return new Data("Car/"+m_car+"/Gauge/"+m_type+"/Multiplier",m_multiplier,"double",Data.State.NORMAL); }
    /**
      * Sets the multiplier for this gauge to use when calculating major and minor tick marks.
      * See {@link com.SIMRacingApps.Gauge#getMultiplier()}
      * @param multiplier The multiplier.
      */
    public void setMultiplier(double multiplier) { m_multiplier = multiplier; }

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
    public Data getMinimum(String UOM)    { 
        Data d = new Data("Car/"+m_car+"/Gauge/"+m_type+"/Minimum",m_minimum,m_uom,Data.State.NORMAL);
        d.addConversion(SIMValue);
        d = d.convertUOM(UOM);
        //now loop through the states and see if the value should be overridden with a constant.
        Double v = d.getDouble();
        Iterator<Entry<Double,StateRange>> itr = m_states.entrySet().iterator();

        //pick the one with the highest start if the ranges overlap.
        //all ranges overlap NORMAL
        while (itr.hasNext()) {
            StateRange range = itr.next().getValue();
            if (v >= range.start && v < range.end)
                if (range.value != null)
                    d  = range.value;
        }
        return d;
    }
    public Data getMinimum()              { return getMinimum(m_defaultUOM); }
    
    /**
      * Sets the minimum value for the first tick mark.
      * See {@link com.SIMRacingApps.Gauge#getMinimum(String)}
      * @param minimum The minimum value.
      * @param UOM (Optional) The Unit of Measure of this value. Defaults to the gauges UOM.
      */
    public void setMinimum(double minimum, String UOM) {
        Data d= new Data("",minimum,UOM);
        d.addConversion(SIMValue);
        m_minimum = d.convertUOM(m_uom).getDouble();
    }
    public void setMinimum(double minimum) { setMinimum(minimum,m_uom); }

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
    public Data getMaximum(String UOM)    { 
        Data d = new Data("Car/"+m_car+"/Gauge/"+m_type+"/Maximum",m_maximum,m_uom,Data.State.NORMAL);
        d.addConversion(SIMValue);
        d = d.convertUOM(UOM); 
        //now loop through the states and see if the value should be overridden with a constant.
        Double v = d.getDouble();
        Iterator<Entry<Double,StateRange>> itr = m_states.entrySet().iterator();

        //pick the one with the highest start if the ranges overlap.
        //all ranges overlap NORMAL
        while (itr.hasNext()) {
            StateRange range = itr.next().getValue();
            if (v >= range.start && v < range.end)
                if (range.value != null)
                    d  = range.value;
        }
        
        return d;
    }
    public Data getMaximum()              { return getMaximum(m_defaultUOM); }
    
    /**
      * Sets the maximum value for the last tick mark.
      * See {@link com.SIMRacingApps.Gauge#getMaximum(String)}
      * @param maximum The maximum value.
      * @param UOM (Optional) The Unit of Measure of this value. Defaults to the gauges UOM.
      */
    public void setMaximum(double maximum, String UOM) {
        Data d = new Data("",maximum,UOM);
        d.addConversion(SIMValue);
        m_maximum = d.convertUOM(m_uom).getDouble();
    }
    public void setMaximum(double maximum) { setMaximum(maximum,m_uom); }

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
        Data d = new Data("Car/"+m_car+"/Gauge/"+m_type+"/MajorIncrement",m_majorIncrement,m_uom,Data.State.NORMAL);
        d.addConversion(SIMValue);
        d = d.convertUOM(UOM);
        d.setValue(Math.abs(d.getDouble()));
        return d;
    }
    public Data getMajorIncrement()       { return getMajorIncrement(m_defaultUOM); }

    /**
      * Sets the major increment value..
      * See {@link com.SIMRacingApps.Gauge#getMajorIncrement(String)}
      * @param majorIncrement The major increment value
      * @param UOM (Optional) The Unit of Measure of this value. Defaults to the gauges UOM.
      */
    public void setMajorIncrement(double majorIncrement, String UOM) {
        Data d = new Data("",majorIncrement,UOM);
        d.addConversion(SIMValue);
        m_majorIncrement = Math.abs(d.convertUOM(m_uom).getDouble()); 
    }
    public void setMajorIncrement(double majorIncrement) { setMajorIncrement(majorIncrement,m_uom); }

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
        Data d = new Data("Car/"+m_car+"/Gauge/"+m_type+"/MinorIncrement",m_minorIncrement,m_uom,Data.State.NORMAL); 
        d.addConversion(SIMValue);
        d = d.convertUOM(UOM);
        d.setValue(Math.abs(d.getDouble()));
        return d;
    }
    public Data getMinorIncrement()       { return getMinorIncrement(m_defaultUOM); }

    /**
      * Sets the minor increment value..
      * See {@link com.SIMRacingApps.Gauge#getMinorIncrement(String)}
      * @param minorIncrement The minor increment value
      * @param UOM (Optional) The Unit of Measure of this value. Defaults to the gauges UOM.
      */
    public void setMinorIncrement(double minorIncrement, String UOM) {
        Data d = new Data("",minorIncrement,UOM);
        d.addConversion(SIMValue);
        m_minorIncrement = Math.abs(d.convertUOM(m_uom).getDouble());
    }
    public void setMinorIncrement(double minorIncrement) { setMinorIncrement(minorIncrement,m_uom); }

    
    /**
      * Returns the maximum number that this gauge will accept when calling {@link com.SIMRacingApps.Gauge#setValueNext(Double,String)}
      * or {@link com.SIMRacingApps.Gauge#incrementValueNext(String)}
      * 
      * <p>PATH = {@link #getCapacityMaximum(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/CapacityMaximum/(UOM)}
      * 
      * @param UOM (Optional) The UOM to convert the value to, defaults to the gauge's UOM.
      * @return The maximum capacity.
      */
    public Data getCapacityMaximum(String UOM) { 
        Data d = new Data("Car/"+m_car+"/Gauge/"+m_type+"/CapacityMaximum",m_capacityMaximum,m_uom,Data.State.NORMAL); 
        d.addConversion(SIMValue);
        d = d.convertUOM(UOM); 
        //now loop through the states and see if the value should be overridden with a constant.
        Double v = d.getDouble();
        Iterator<Entry<Double,StateRange>> itr = m_states.entrySet().iterator();

        //pick the one with the highest start if the ranges overlap.
        //all ranges overlap NORMAL
        while (itr.hasNext()) {
            StateRange range = itr.next().getValue();
            if (v >= range.start && v < range.end)
                if (range.value != null)
                    d  = range.value;
        }
        return d;
    }
    public Data getCapacityMaximum()      { return getCapacityMaximum(m_defaultUOM); }

    /**
      * Sets the maximum capacity value.
      * See {@link com.SIMRacingApps.Gauge#getCapacityMaximum(String)}
      * @param capacityMaximum The maximum capacity value
      * @param UOM (Optional) The Unit of Measure of this value. Defaults to the gauges UOM.
      */
    public void setCapacityMaximum(double capacityMaximum, String UOM) {
        Data d = new Data("",capacityMaximum,UOM);
        d.addConversion(SIMValue);
        m_capacityMaximum = d.convertUOM(m_uom).getDouble(); 
    }
    public void setCapacityMaximum(double capacityMaximum) { setCapacityMaximum(capacityMaximum,m_uom); }

    /**
      * Returns the minimum number that this gauge will accept when calling {@link com.SIMRacingApps.Gauge#setValueNext(Double,String)}
      * or {@link com.SIMRacingApps.Gauge#decrementValueNext(String)}
      * 
      * <p>PATH = {@link #getCapacityMinimum(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/CapacityMinimum/(UOM)}
      * 
      * @param UOM (Optional) The UOM to convert the value to, defaults to the gauge's UOM.
      * @return The minimum capacity.
      */
    public Data getCapacityMinimum(String UOM){ 
        Data d = new Data("Car/"+m_car+"/Gauge/"+m_type+"/CapacityMinimum",m_capacityMinimum,m_uom,Data.State.NORMAL);
        d.addConversion(SIMValue);
        d = d.convertUOM(UOM); 
        //now loop through the states and see if the value should be overridden with a constant.
        Double v = d.getDouble();
        Iterator<Entry<Double,StateRange>> itr = m_states.entrySet().iterator();

        //pick the one with the highest start if the ranges overlap.
        //all ranges overlap NORMAL
        while (itr.hasNext()) {
            StateRange range = itr.next().getValue();
            if (v >= range.start && v < range.end)
                if (range.value != null)
                    d  = range.value;
        }
        return d;
    }
    public Data getCapacityMinimum()      { return getCapacityMinimum(m_defaultUOM); }

    /**
      * Sets the minimum capacity value..
      * See {@link com.SIMRacingApps.Gauge#getCapacityMinimum(String)}
      * @param capacityMinimum The minimum capacity value
      * @param UOM (Optional) The Unit of Measure of this value. Defaults to the gauges UOM.
      */
    public void setCapacityMinimum(double capacityMinimum, String UOM) {
        Data d = new Data("",capacityMinimum,UOM);
        d.addConversion(SIMValue);
        m_capacityMinimum = d.convertUOM(m_uom).getDouble(); 
    }
    public void setCapacityMinimum(double capacityMinimum) { setCapacityMinimum(capacityMinimum,m_uom); }

    /**
      * Returns the increment value that the gauge uses when you call {@link com.SIMRacingApps.Gauge#incrementValueNext(String)} 
      * or {@link com.SIMRacingApps.Gauge#decrementValueNext(String)}.
      * Also, all values passed to {@link com.SIMRacingApps.Gauge#setValueNext(Double,String)} are rounded up to the the
      * closest multiple of this value.
      * 
      * <p>PATH = {@link #getCapacityIncrement(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/CapacityIncrement/(UOM)}
      * 
      * @param UOM (Optional) The UOM to convert the value to, defaults to the gauge's UOM.
      * @return The incremental capacity.
      */
    public Data getCapacityIncrement(String UOM){ 
        Data d = new Data("Car/"+m_car+"/Gauge/"+m_type+"/CapacityIncrement",m_capacityIncrement,m_uom,Data.State.NORMAL);
        d.addConversion(SIMValue);
        d = d.convertUOM(UOM);
        d.setValue(Math.abs(d.getDouble()));
        return d;
    }
    public Data getCapacityIncrement()    { return getCapacityIncrement(m_defaultUOM); }

    /**
      * Sets the increment capacity value..
      * See {@link com.SIMRacingApps.Gauge#getCapacityIncrement(String)}
      * @param capacityIncrement The incremental capacity value
      * @param UOM (Optional) The Unit of Measure of this value. Defaults to the gauges UOM.
      */
    public void setCapacityIncrement(double capacityIncrement,String UOM) {
        Data d = new Data("",capacityIncrement,UOM); 
        d.addConversion(SIMValue);
        m_capacityIncrement = Math.abs(d.convertUOM(m_uom).getDouble()); 
    }
    public void setCapacityIncrement(double capacityIncrement) { setCapacityIncrement(capacityIncrement,""); }

    /**
      * Updates the gauge with the current lap so functions that need the current lap can calculate their values.
      * Obviously, this should be called when the current lap changes for the car this gauge is attached to.
      * @param currentLap The current lap.
      */
    public void updateCurrentLap(int currentLap) { m_currentLap = currentLap; }
    
//    public Data isInCar()                 { return new Data("CarGaugeIsInCar/"+m_type,m_isInCar ? true : false,"boolean",Data.State.NORMAL); }
//    public void setIsInCar(boolean isInCar) { m_isInCar = isInCar; }

    /**
      * Returns false if this gauge's next value can be changed via {@link com.SIMRacingApps.Gauge#setValueNext(Double,String)}, 
      * {@link com.SIMRacingApps.Gauge#incrementValueNext(String)} or {@link com.SIMRacingApps.Gauge#decrementValueNext(String)}.
      * A client could use this value to grey out or hide the buttons for changing it.
      * 
      * <p>PATH = {@link #getIsFixed() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/IsFixed}
      * 
      * @return true if gauge is fixed, false of not.
      */
    public Data getIsFixed() {              return new Data("Car/"+m_car+"/Gauge/"+m_type+"/IsFixed",m_isFixed,"boolean");}
    
    /**
      * Sets the gauge's fixed attribute to indicate if the next value can be changed.
      * See {@link com.SIMRacingApps.Gauge#getIsFixed()}
      * @param isFixed true if fixed, false if not.
      */
    public void setIsFixed(boolean isFixed) { m_isFixed = isFixed; }

    /**
      * Returns true if this gauge represents an object that can be replaced/changed at the next pit stop (i.e. Tires, Tearoff usually)
      * 
      * <p>PATH = {@link #getIsChangeable() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/IsChangeable}
      * 
      * @return true if gauge is changeable, false of not.
      */
    public Data getIsChangeable() {          return new Data("Car/"+m_car+"/Gauge/"+m_type+"/IsChangeable",m_isChangeable,"boolean");}

    /**
      * Sets the gauge's changeable attribute.
      * See {@link com.SIMRacingApps.Gauge#getIsChangeable()}
      * @param isChangeable true if changeable, false if not.
      */
    public void setIsChangable(boolean isChangeable) { m_isChangeable = isChangeable; }

    /**
      * Returns true if this gauge is changed on a reset.
      * 
      * <p>PATH = {@link #getOnResetChange() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/OnResetChange}
      * 
      * @return true if gauge is changed upon reset, false of not.
      */
    public Data getOnResetChange() {              return new Data("Car/"+m_car+"/Gauge/"+m_type+"/OnResetChange",m_onResetChange,"boolean");}
    /**
      * Sets the gauge's on reset change attribute.
      * See {@link com.SIMRacingApps.Gauge#setOnResetChange(boolean)}
      * @param onResetChange true if changed on reset, false if not.
      */
    public void setOnResetChange(boolean onResetChange) { m_onResetChange = onResetChange; }

    /**
     * Returns Y if this gauge is outputting debug information.
     * 
     * <p>PATH = {@link #getIsDebug() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/IsDebug}
     * 
     * @return true if gauge is in debug mode, false of not.
     */
    public Data getIsDebug() {              return new Data("Car/"+m_car+"/Gauge/"+m_type+"/IsDebug",m_isDebug,"boolean");}
    /**
     * Sets the debug flag for this gauge
     * See {@link com.SIMRacingApps.Gauge#getIsDebug()}
     * 
     * <p>PATH = {@link #setIsDebug(boolean) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/setIsDebug/(ISDEBUG)}
     * 
     * @param isDebug true to enable debugging
     * @return true if gauge is changed upon reset, false of not.
     */
    public Data setIsDebug(boolean isDebug) { m_isDebug = isDebug; return getIsDebug(); }
    public Data setIsDebug(String isDebug) { return setIsDebug(new Data("",isDebug).getBoolean()); }
    public Data setIsDebug() { return setIsDebug(true); }

    public boolean _getIsSentToSIM()      { return m_isSentToSIM; }
    public void _setIsSentToSIM(boolean isSentToSIM) { 
        m_isSentToSIM = isSentToSIM; 
        //now set the flag on the whole group
        Iterator<Entry<String,Gauge>> itr = m_group.entrySet().iterator();
        while (itr.hasNext()) {
            Gauge g = itr.next().getValue();
            g._setIsSentToSIM(isSentToSIM);
        }
    }

    @SuppressWarnings("unused")
    private Gauge() {}

    /**
     * Constructor for the Gauge class.
     * It takes 2 required parameters that defines the car identifier and the type of gauge.
     * 
     * @param car  The car identifier as defined by {@link com.SIMRacingApps.Session#getCar(String)}
     * @param type The type of gauge as defined by {@link com.SIMRacingApps.Gauge.Type}
     */
    public Gauge(String car, String type) {
        this.m_car = car;
        this.m_type = type;
        m_name = "unknown";
        m_typename = "unknown";
        m_uom = "";
        m_defaultUOM = "NATIVE";
        m_multiplier = 1.0;
        m_minimum = 0.0;
        m_maximum = 100.0;
        m_majorIncrement = 10.0;
        m_minorIncrement = 2.0;
        m_capacityMinimum = m_minimum;
        m_capacityMaximum = m_maximum;
        m_capacityIncrement = m_minorIncrement;
        m_states = new TreeMap<Double,StateRange>();
        m_lapChanged = 1;
        m_count = 1;

//        m_isInCar = false;
        m_isFixed = true;
        m_isChangeable = false;
        m_isSentToSIM = false;

        //the default is all false so no values will be read until the subclass initializes these
        SIMValue           = null;
        SIMValueType       = SIMValueTypes.Unknown;
    }

    /**
     * Rounds the value to the nearest capacity increment.
     * @param value The value to round.
     * @return      The rounded value.
     */
    private double _roundToIncrement(double value) {
        
        //TODO: add a flag to indicate gauges that must round up. For now hardcoded for fuel level
        if (this.m_type.equals(Gauge.Type.FUELLEVEL))
            return _roundUpToIncrement(value);
        
        double d = value;
        double floored_d = Math.floor(d / m_capacityIncrement) * m_capacityIncrement; //floor it to the closest increment

        if ((floored_d + (m_capacityIncrement/2.0)) <= d)
            d = floored_d + m_capacityIncrement;
        else
            d = floored_d;

        //don't let it round down below the max capacity, if it was originally above the max capacity
        if (d < m_capacityMaximum && value >= m_capacityMaximum)
            d = m_capacityMaximum;
        
        //by the same rule if it rounds below the min capacity
        if (d < m_capacityMinimum && value >= m_capacityMinimum)
            d = m_capacityMinimum;
        
        return d;
    }

    /**
     * Rounds the value up to the nearest capacity increment if not at an increment value already.
     * @param value The value to round.
     * @return      The rounded value.
     */
    private double _roundUpToIncrement(double value) {
        double d = value;
        double floored_d = Math.floor(d / m_capacityIncrement) * m_capacityIncrement; //floor it to the closest increment

        //now add an increment if we are below the requested value
        if ((floored_d + 0.001) < d)
            d = floored_d + m_capacityIncrement;
        else
            d = floored_d;

        //don't let it round down below the max capacity, if it was originally above the max capacity
        if (d < m_capacityMaximum && value >= m_capacityMaximum)
            d = m_capacityMaximum;
        
        //by the same rule if it rounds below the min capacity
        if (d < m_capacityMinimum && value >= m_capacityMinimum)
            d = m_capacityMinimum;
        
        return d;
    }
    
    private boolean _isDirty() {
        if (value == null || setupValue == null)
            return false;

        double a = _roundToIncrement(value.getDouble());
        double b = _roundToIncrement(setupValue.getDouble());

        return a != b;
    }

    public String toString() {
        String s =
          "{\n"
        + "    \"CarId\":             \"" + m_car + "\",\n"
        + "    \"Name\":              \"" + m_name + "\",\n"
        + "    \"TypeName\":          \"" + m_typename + "\",\n"
        + "    \"UOM\":               \"" + m_uom + "\",\n"
        + "    \"DefaultUOM\":        \"" + m_defaultUOM + "\",\n"
        + "    \"Multiplier\":        " + m_multiplier + ",\n"
        + "    \"Minimum\":           " + m_minimum + ",\n"
        + "    \"Maximum\":           " + m_maximum + ",\n"
        + "    \"MajorIncrement\":    " + m_majorIncrement + ",\n"
        + "    \"MinorIncrement\":    " + m_minorIncrement + ",\n"
        + "    \"CapacityMinimum\":   " + m_capacityMinimum + ",\n"
        + "    \"CapacityMaximum\":   " + m_capacityMaximum + ",\n"
        + "    \"CapacityIncrement\": " + m_capacityIncrement + ",\n"
        + "    \"IsFixed\":           \"" + (m_isFixed ? "true" : "false") + "\",\n"
        + "    \"IsChangable\":       \"" + (m_isChangeable ? "true" : "false") + "\",\n"
        + "    \"OnResetChange\":     \"" + (m_onResetChange ? "true" : "false") + "\",\n"
        + "    \"LapChanged\":        " + m_lapChanged + ",\n"
        + "    \"Count\":             " + m_count + ",\n"
        + "    \"Reader\":            \"" + (SIMValue == null ? "null" : SIMValue.getClass().getName()) + "\",\n"
        + "    \"ValueCurrent\":        " + (value == null ? new Data(m_name,0.0,m_uom).toString(m_name) : value.toString(value.getName())) + ",\n"
        + "    \"ValueNext\":           " + (setupValue == null ? new Data(m_name,0.0,m_uom).toString(m_name) : setupValue.toString(setupValue.getName())) + ",\n"
        + "    \"ValueHistorical\":     " + (afterPitValue == null ? new Data(m_name,0.0,m_uom).toString(m_name) : afterPitValue.toString(afterPitValue.getName())) + ",\n";

        s += "    \"States\": {\n";
        Iterator<Entry<Double, StateRange>> itr = m_states.entrySet().iterator();
        while (itr.hasNext()) {
            StateRange state = itr.next().getValue();
            s +=
          "        \"" + state.state + "\": { \"Start\": " + (Double.isNaN(state.start) ? 0.0 : state.start) + ", \"End\": " + (Double.isNaN(state.end) ? 0.0 : state.end);
            if (state.value != null)
                s += ", \"Value\": \"" + state.value.getValueFormatted() + "\"";
            s += " }" + (itr.hasNext() ? "," : "") + "\n";
        }
        s += "    }\n";
        s += "}";
        return s;
    }

    protected void _initValues() {
        Data simValue = SIMValue != null ? SIMValue.convertUOM(m_uom) : new Data("Car/"+m_car+"/Gauge/"+m_type+"/ValueCurrent",0.0,m_uom,Data.State.NOTAVAILABLE);

        if (value == null) {

//if (m_car.isME() && m_type.equals(Type.TACHOMETER))
//    value=value;

            //if we can get the setup value or after pit value, initialize it with that
            if (SIMValueType == SIMValueTypes.ForSetup
            ||  SIMValueType == SIMValueTypes.ForCarAndSetup
            ||  SIMValueType == SIMValueTypes.AfterPit
            ) {
                value = new Data("Car/"+m_car+"/Gauge/"+this.m_type+"/ValueCurrent",simValue.getDouble(),simValue.getUOM(),simValue.getState());
            }
            else {
                value = new Data("Car/"+m_car+"/Gauge/"+this.m_type+"/ValueCurrent",0.0,m_uom,simValue.getState());
            }
            value.addConversion(simValue);
        }

//if (m_car.isME() && m_type.equals(Type.TACHOMETER))
//    value=value;

        //now update the value if it's from the car real-time
        if (SIMValueType == SIMValueTypes.ForCar
        ||  SIMValueType == SIMValueTypes.ForCarZeroOnPit
        ||  SIMValueType == SIMValueTypes.ForCarAndSetup
        ) {
            value = new Data("Car/"+m_car+"/Gauge/"+this.m_type+"/ValueCurrent",simValue.getDouble(),simValue.getUOM(),simValue.getState());
            value.addConversion(simValue);
        }

        if (setupValue == null) {

            if (SIMValueType == SIMValueTypes.ForSetup
            ||  SIMValueType == SIMValueTypes.ForCarAndSetup
            ) {
                setupValue = new Data("Car/"+m_car+"/Gauge/"+this.m_type+"/ValueNext",simValue.getDouble(),simValue.getUOM(),simValue.getState());
                setupValue.addConversion(simValue);
            }
            else {
                double d = 0.0;
                if (SIMValueType != SIMValueTypes.ForCarZeroOnPit) {
                    //the setup value should start with the value unless the SIM returns the setup value instead of the real-time value
                    //round it off to the closest increment
                    d = _roundToIncrement(value.getDouble());
                }
                setupValue = new Data("Car/"+m_car+"/Gauge/"+this.m_type+"/ValueNext",d,value.getUOM(),value.getState());
                setupValue.addConversion(simValue);
            }
        }

        //now update the setup value if the sim is returning it
        if (SIMValueType == SIMValueTypes.ForSetup
        ||  SIMValueType == SIMValueTypes.ForCarAndSetup
        ) {
            setupValue = new Data("Car/"+m_car+"/Gauge/"+this.m_type+"/ValueNext",simValue.getDouble(),simValue.getUOM(),simValue.getState());
            setupValue.addConversion(simValue);
        }

        //This after pit values should default to zero until we pit or reset
        if (afterPitValue == null) {
            afterPitValue = new Data("Car/"+m_car+"/Gauge/"+this.m_type+"/ValueHistorical",0.0,m_uom,simValue.getState());
            afterPitValue.addConversion(simValue);
        }
        if (afterPitLaps == null) {
            afterPitLaps = new Data("Car/"+m_car+"/Gauge/"+this.m_type+"/LapsHistorical",0.0,"lap",simValue.getState());
        }

        //if the value is for the setup only, then set the change flag if it's dirty
        if (SIMValueType == SIMValueTypes.ForSetup)
            m_changeFlag = _isDirty();
    }

    /**
     * Returns Y if the next value of the gauge is different from the current value.
     * 
     * <p>PATH = {@link #getIsDirty() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/IsDirty}
     * 
     * @return Y if dirty, N if not.
     */
    public Data getIsDirty() {
        if (value == null || setupValue == null)
            return new Data("Car/"+m_car+"/Gauge/"+m_type+"/IsDirty",false,"boolean");

        _initValues();
        return new Data("Car/"+m_car+"/Gauge/"+m_type+"/IsDirty",_isDirty() ? true : false,"boolean");
    }

    /**
     * Returns the current value of the gauge. 
     * 
     * <p>PATH = {@link #getValueCurrent(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/ValueCurrent/(UOM)}
     * 
     * @param UOM (Optional) The unit of measure to return, default to the gauges UOM.
     * @return The current value.
     */
    public Data getValueCurrent(String UOM) {
        Data d;
        
//if (m_car.isME() && m_type.equals(Type.TACHOMETER))
//    value=value;

        if (value == null || setupValue == null)
            d = new Data("Car/"+m_car+"/Gauge/"+m_type+"/ValueCurrent",0.0,m_uom,Data.State.NOTAVAILABLE);
        else {
            _initValues();
            d = new Data(value).convertUOM(UOM); //make a copy to return
            
            //This is just for testing
            double newvalue = Server.getArg("gauge."+m_type+".value", 0.0);
            if (newvalue != 0.0) {
                newvalue += d.getDouble();
                d.setValue(Math.min(Math.max(newvalue,this.m_capacityMinimum),this.m_capacityMaximum));
            }
        }

        //now loop through the states and see if the value should be overridden with a constant.
        Double v = d.getDouble();
        Iterator<Entry<Double,StateRange>> itr = m_states.entrySet().iterator();

        //pick the one with the highest start if the ranges overlap.
        //all ranges overlap NORMAL
        while (itr.hasNext()) {
            StateRange range = itr.next().getValue();
            if (v >= range.start && v < range.end)
                if (range.value != null)
                    d  = range.value;
        }

        //now update the state. TODO: should I delegate the state logic to the Data class?
        d.setState(getState());
        d.setStatePercent(getStatePercent() * 100.0);
        d = d.convertUOM(UOM);
//        d.add("LapChangedPrevious",m_lapChangedPrevious,"lap");
        d.add("LapChanged",m_lapChanged,"lap");
        d.add("Count",m_count,"");
        return d;
    }
    public Data getValueCurrent() { return getValueCurrent(m_defaultUOM); }

    /**
     * Returns the lap when the object for this gauge was changed.
     * 
     * <p>PATH = {@link #getLapChanged() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/LapChanged}
     * 
     * @return The lap it was changed.
     */
    public Data getLapChanged() {
        //_initValues();
        return new Data("Car/"+m_car+"/Gauge/"+m_type+"/LapChanged",m_lapChanged,"lap",Data.State.NORMAL);
    }

    /**
     * Returns the number of times this gauge was changed.
     * The count starts at one, so the initial reading counts.
     * 
     * <p>PATH = {@link #getCount() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/Count} 1.2
     * 
     * @since 1.2
     * @return The number of times this gauge was changed.
     */
    public Data getCount() {
        //_initValues();
        return new Data("Car/"+m_car+"/Gauge/"+m_type+"/Count",m_count,"",Data.State.NORMAL);
    }
    
    /**
     * Returns the number of laps since the object for this gauge was changed.
     * 
     * <p>PATH = {@link #getLaps(int) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/Laps/(CURRENTLAP)}
     * 
     * @param currentLap (Optional) The lap you are currently on, defaults to current lap.
     * @return The number of laps.
     */
    public Data getLaps(int currentLap) {
        //_initValues();
        return new Data("Car/"+m_car+"/Gauge/"+m_type+"/Laps",currentLap - m_lapChanged > 0 ? currentLap - m_lapChanged : 0,"lap",Data.State.NORMAL);
    }
    public Data getLaps(String currentLap) {
        return getLaps(Integer.parseInt(currentLap));
    }
    public Data getLaps() {
        return getLaps(m_currentLap);
    }

    /**
     * Returns the State Name that matches the range based on the current value.
     * @return The State Name.
     */
    protected String getState() {
        if (value == null || setupValue == null)
            return Data.State.NOTAVAILABLE;

        _initValues();
        String state = value.getState(); //default to the value's state.

//if (this.m_type.equalsIgnoreCase(Type.TACHOMETER) && value.getDouble() > 8500.0)
//    value = value;

        if (!state.equals(Data.State.OFF) 
        &&  !state.equals(Data.State.ERROR)
        &&  !state.equals(Data.State.NOTAVAILABLE)
        ) {
            Iterator<Entry<Double,StateRange>> itr = m_state_ascending 
                                                   ? m_states.entrySet().iterator()
                                                   : m_states.descendingMap().entrySet().iterator();

            //pick the one with the highest start if the ranges overlap.
            //all ranges overlap NORMAL
            Double v = value.getDouble();
            while (itr.hasNext()) {
                StateRange range = itr.next().getValue();
                //if the value is within the range 
                //or the reader has already set this state so it will override less important values
                if ((v >= range.start && v < range.end) || state.equals(range.state))
                    state = range.state;
            }
        }

        return state;
    }

    /**
     * Returns the state range of the requested state.
     * 
     * <p>PATH = {@link #getStateRange(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/StateRange/(STATE)}
     * 
     * @param state (Optional) The state to return. Default CRITICAL state.
     * @return The requested state as an JSON string { state: "xxx", start: -1.0, end: -1.0 }
     */
    public Data getStateRange(String state) {
        Data d = new Data("Car/"+m_car+"/Gauge/"+m_type+"/StateRange/"+state.toUpperCase(),new StateRange(state.toUpperCase(),-1.0,-1.0),m_uom,Data.State.ERROR);
        Iterator<Entry<Double,StateRange>> itr = m_state_ascending 
                ? m_states.entrySet().iterator()
                : m_states.descendingMap().entrySet().iterator();

        while (itr.hasNext()) {
            StateRange range = itr.next().getValue();
            if (range.state.equalsIgnoreCase(state))
                d.setValue(range,"",Data.State.NORMAL);
        }
        return d;
    }
    public Data getStateRange() {
        return getStateRange("CRITICAL");
    }
    
    /**
     * Returns all the states.
     * 
     * <p>PATH = {@link #getStates() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/States}
     * 
     * @return The states as an JSON string ["name": { state: "xxx", start: -1.0, end: -1.0 }]
     */
    public Data getStates() {
        ArrayList<StateRange> a = new ArrayList<StateRange>();
        Data d = new Data("Car/"+m_car+"/Gauge/"+m_type+"/States",a,"ArrayList<StateRange>",Data.State.ERROR);
        Iterator<Entry<Double,StateRange>> itr = m_state_ascending 
                ? m_states.entrySet().iterator()
                : m_states.descendingMap().entrySet().iterator();

        while (itr.hasNext()) {
            Entry<Double, StateRange> state = itr.next();
            a.add(state.getValue());
        }
        d.setValue(a,"ArrayList<StateRange>",Data.State.NORMAL);
        return d;
    }
    
    /**
     * Adds all the states from the passed in gauge to this gauge.
     * @param gauge The gauge to copy the states from
     */
    public void addStateRange(Gauge gauge) {
        Iterator<Entry<Double, StateRange>> itr = gauge.m_states.entrySet().iterator();
        while (itr.hasNext()) {
            StateRange state = itr.next().getValue();
            removeStateRange(state.state);
            addStateRange(state.state,state.start,state.end,state.value);
        }
    }
    
    /**
     * Add a new state range to this gauge.
     * @param name  The name of the state.
     * @param start The starting value, inclusive.
     * @param end   The ending value, exclusive.
     */
    public void addStateRange(String name,double start,double end) {
        removeStateRange(name);

        m_states.put(start, new StateRange(name.toUpperCase(),start,end));
    }

    /**
     * Add a new state range to this gauge that transforms the value.
     * @param name  The name of the state.
     * @param start The starting value, inclusive.
     * @param end   The ending value, exclusive.
     * @param d     The new value
     */
    public void addStateRange(String name,double start,double end, Data d) {
        removeStateRange(name);

        m_states.put(start, new StateRange(name.toUpperCase(),start,end,d));
    }

    /**
     * Removes a state.
     * 
     * @param name The name of the state.
     */
    public void removeStateRange(String name) {
        Iterator<Entry<Double,StateRange>> itr = m_states.entrySet().iterator();
        //remove the name if it exists
        while (itr.hasNext()) {
            StateRange range = itr.next().getValue();
            if (range.state.equals(name.toUpperCase()))
                itr.remove();
        }
    }
    
    /**
     * Returns a percentage that represents where the current value falls between the starting and ending value.
     * @return The state percentage.
     */
    protected Double getStatePercent() {
        if (value == null || setupValue == null)
            return 0.0;

        _initValues();
        double d = value.getStatePercent(); //default to the value's percentage.
        String state = value.getState();

        //if the value.State is not OFF or ERROR, then lookup the state
        if (!state.equals(Data.State.OFF) 
        &&  !state.equals(Data.State.ERROR)
        &&  !state.equals(Data.State.NOTAVAILABLE)
        ) {
            Iterator<Entry<Double,StateRange>> itr = m_states.entrySet().iterator();

            while (itr.hasNext()) {
                StateRange range = itr.next().getValue();
                Double v = value.getDouble();
                if (v >= range.start && v < range.end)
                    d = (v - range.start) / (range.end - range.start);
            }
        }

        return d;
    }

    /**
     * Returns the historical value of the gauge taken at the time it was changed.
     * 
     * <p>PATH = {@link #getValueHistorical(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/ValueHistorical/(UOM)}
     * 
     * @param UOM (Optional) The unit of measure to return it in, defaults to the gauges UOM.
     * @return    The historical value.
     */
    public Data getValueHistorical(String UOM) {
        if (value == null || setupValue == null) {
            Data d = new Data("Car/"+m_car+"/Gauge/"+m_type+"/ValueHistorical",0.0,m_uom,Data.State.NOTAVAILABLE);
            d.addConversion(SIMValue);
            return d.convertUOM(UOM); 
        }
        _initValues();
        Data d = new Data(afterPitValue);
        return d.convertUOM(UOM);
    }
    public Data getValueHistorical() { return getValueHistorical(m_defaultUOM); }

    /**
     * Returns the lap the historical value was taken.
     * 
     * <p>PATH = {@link #getLapsHistorical() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/LapsHistorical}
     * 
     * @return The lap the historical value was taken.
     */
    public Data getLapsHistorical() {
        if (value == null || setupValue == null)
            return new Data("Car/"+m_car+"/Gauge/"+m_type+"/LapHistorical",0,"lap",Data.State.NOTAVAILABLE);

        _initValues();
        Data d = new Data(afterPitLaps);
        return d;
    }

    /**
     * Returns the value of the next value that the object will be at the next pit stop.
     * 
     * <p>PATH = {@link #getValueNext(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/ValueNext/(UOM)}
     * 
     * @param UOM The unit of value to return.
     * @return The value if the next value.
     */
    public Data getValueNext(String UOM) {
//        _initValues();
//        Data d = new Data("Car/"+m_car+"/Gauge/"+m_type+"/ValueNext",setupValue.getDouble(),m_uom);
//        return d;
        Data d;
        if (value == null || setupValue == null)
            d = new Data("Car/"+m_car+"/Gauge/"+m_type+"/ValueNext",0.0,m_uom,Data.State.NOTAVAILABLE);
        else {
            _initValues();
            d = new Data(setupValue); //make a copy to return
        }

        //now loop through the states and see if the value should be overridden with a constant.
        Double v = d.getDouble();
        Iterator<Entry<Double,StateRange>> itr = m_states.entrySet().iterator();

        //pick the one with the highest start if the ranges overlap.
        //all ranges overlap NORMAL
        while (itr.hasNext()) {
            StateRange range = itr.next().getValue();
            if (v >= range.start && v < range.end)
                if (range.value != null)
                    d = range.value;
        }

        d.addConversion(SIMValue);
        return d.convertUOM(UOM);
    }
    public Data getValueNext() { return getValueNext(m_defaultUOM); }

    /**
     * Add another gauge to this gauge to group them together.
     * When the change flag is set this gauge, the entire group will also get called.
     * @param gauge An instance of another gauge.
     */
    public void addGroup(Gauge gauge) {
        m_group.put(gauge.getType().getString(), gauge);
    }

    /**
     * Returns Y if the object for this gauge is flagged for changed on the next pit stop.
     * 
     * <p>PATH = {@link #getChangeFlag() /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/ChangeFlag}
     * 
     * @return Y if flagged for changed, N if not.
     */
    public Data getChangeFlag() {
        //_initValues();
        return new Data("Car/"+m_car+"/Gauge/"+m_type+"/ChangeFlag",m_changeFlag ? true : false,"boolean");
    }

    /**
     * Sets the change flag for this gauge and all the gauges grouped with it.
     * 
     * <p>PATH = {@link #setChangeFlag(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/setChangeFlag/(FLAG)}
     * 
     * @param flag Y or N to change or not to change
     * @return     The change flag
     */
    public Data setChangeFlag(boolean flag) {
        //_initValues();
        if (m_isChangeable) {
            m_changeFlag = flag;
            m_isSentToSIM = false;

            //now set the flag on the whole group
            Iterator<Entry<String,Gauge>> itr = m_group.entrySet().iterator();
            while (itr.hasNext()) {
                Gauge g = itr.next().getValue();
                g.setChangeFlag(flag);
            }
        }
        return getChangeFlag();
    }
    public Data setChangeFlag(String flag) { return setChangeFlag(new Data("",flag).getBoolean()); }

    /**
     * Decrements the next value, to be applied at the next pit stop, according to the value set by {@link com.SIMRacingApps.Gauge#setCapacityIncrement(double)}. 
     * If the gauge is fixed, then only the change flag will be set and the next value will not be changed. 
     * The change flag is set to Y even if the new value and the old value are the same.
     * 
     * <p>PATH = {@link #decrementValueNext(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/decrementValueNext/(UOM)}
     * 
     * @param UOM (Optional) The unit of measure to return the new value in. Default gauge's UOM.
     * @return    The new value.
     */
    public Data decrementValueNext(String UOM) {
        if (value == null || setupValue == null || !m_isChangeable)
            return getValueNext(UOM);

        _initValues();
        if (!m_isFixed) {
            double d = setupValue.getDouble();
            d -= m_capacityIncrement;
            if (d < m_capacityMinimum)
                d = m_capacityMinimum;
            setupValue.setValue(d);
        }
        setChangeFlag(true);
        m_isSentToSIM = false;
        return getValueNext(UOM);
    }
    public Data decrementValueNext() { return decrementValueNext(m_defaultUOM); }

    /**
     * Increments the next value, to be applied at the next pit stop, according to the value set by {@link com.SIMRacingApps.Gauge#setCapacityIncrement(double)}. 
     * If the gauge is fixed, then only the change flag will be set and the next value will not be changed. 
     * If increments above the maximum capacity, then it sets it to the maximum capacity. 
     * The change flag is set to Y even if the new value and the old value are the same.
     * 
     * <p>PATH = {@link #incrementValueNext(String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/incrementValueNext/(UOM)}
     * 
     * @param UOM (Optional) The unit of measure to return the new value in. Default to gauge's UOM.
     * @return    The new value.
     */
    public Data incrementValueNext(String UOM) {
        if (value == null || setupValue == null || !m_isChangeable)
            return getValueNext(UOM);

        _initValues();
        if (!m_isFixed) {
            double d = setupValue.getDouble();
            d += m_capacityIncrement;
            if (d > m_capacityMaximum)
                d = m_capacityMaximum;
            setupValue.setValue(d);
        }
        setChangeFlag(true);
        m_isSentToSIM = false;
        return getValueNext(UOM);
    }
    public Data incrementValueNext() { return incrementValueNext(m_defaultUOM); }
    
    /**
     * Sets the next value to be applied at the next pit stop.
     * If the gauge is fixed, then only the change flag will be set and the next value will not be changed. 
     * If below the minimum capacity, then it sets it to the minimum capacity.
     * If above the maximum capacity, then it sets it to the maximum capacity.
     * The value is rounded to the nearest increment as defined by {@link com.SIMRacingApps.Gauge#setCapacityIncrement(double)}.
     * The change flag is set to Y even if the new value and the old value are the same.
     * 
     * <p>PATH = {@link #setValueNext(Double, String) /Car/(CARIDENTIFIER)/Gauge/(GAUGETYPE)/setValueNext/(VALUE)/(UOM)}
     * 
     * @param value (Optional) The value to be set. Default 0. 
     * @param UOM   (Optional) The unit of measure the new measure to be set is in. Also affects return value. Default to gauge's UOM.
     * @return      The new value, possibly adjusted.
     */
    public Data setValueNext(Double value, String UOM) {
        if (value == null || setupValue == null || !m_isChangeable)
            return getValueNext(UOM);

        _initValues();
        if (!m_isFixed) {
            Data d = new Data("",value,UOM);
            d.addConversion(SIMValue);
            double u = d.convertUOM(m_uom).getDouble();
            double v = _roundToIncrement(u);

            if (v < m_capacityMinimum)
                v = m_capacityMinimum;
            else
            if (v > m_capacityMaximum)
                v = m_capacityMaximum;
            setupValue.setValue(v);
        }
        setChangeFlag(true);
        m_isSentToSIM = false;
        return  getValueNext(UOM);
    }
    public Data setValueNext(String value, String UOM) {
        return setValueNext(Double.parseDouble(value),UOM);
    }
    public Data setValueNext(String value) {
        return setValueNext(value,"");
    }
    public Data setValueNext() {
        return setValueNext("0");
    }

    /**
     * Resets the gauge to it's default values.
     * The app_ini_autoResetPitBox flag determines if the change flag will be set to Y or N to match it.
     * @param lap The lap the reset occurred on.
     * @param app_ini_autoResetPitBox 1 to set the change flag to Y, 0 to not.
     * @param app_ini_autoResetFastRepair 1 to set the change flag to Y, 0 to not.
     */
    public void reset(int lap,int app_ini_autoResetPitBox, int app_ini_autoResetFastRepair) {
        boolean firsttime = value == null ? true : false;
        _initValues();
        Data simValue = SIMValue != null ? SIMValue.convertUOM(m_uom) : new Data("",0.0);

        if (m_isDebug)
            Server.logger().finer(String.format("%s.reset(%s) on lap %d, Current value=%f %s, Next value=%f %s, SIM value=%f %s"
                , this.getClass().getName()
                , m_type
                , lap
                , value.getDouble()
                , value.getUOM()
                , setupValue.getDouble()
                , setupValue.getUOM()
                , simValue.getDouble()
                , simValue.getUOM()
            ));

        if (!firsttime || app_ini_autoResetPitBox == 1) {
            if ((!m_firstreset && m_onResetChange) || app_ini_autoResetPitBox == 1) {
                if (!m_type.equals(Type.FASTREPAIRS)) //we should never automatically select this to be changed.
                    setChangeFlag(true);   //call this so groups will get called together
                else
                if (app_ini_autoResetFastRepair == 1)
                    setChangeFlag(true);   //call this so groups will get called together

                if (app_ini_autoResetPitBox == 1) {
                    //on auto reset fill fuel to max
                    if (m_type.equals(Type.FUELLEVEL)) //TODO: make a flag for gauges that need filling on reset.
                        setupValue.setValue(this.m_capacityMaximum);
                }
        
                //TODO: Need flag that says on reset, change setup value to the current sim value.
                if (m_type.equals(Type.TIREPRESSURELF)
                ||  m_type.equals(Type.TIREPRESSURELR)
                ||  m_type.equals(Type.TIREPRESSURERR)
                ||  m_type.equals(Type.TIREPRESSURERF)
                ||  m_type.equals(Type.TAPE)
                ||  m_type.equals(Type.RRWEDGEADJUSTMENT)
                ||  m_type.equals(Type.LRWEDGEADJUSTMENT)
                ||  m_type.equals(Type.FRONTWING)
                ||  m_type.equals(Type.REARWING)
                ||  m_type.equals(Type.ANTIROLLFRONT)
                ||  m_type.equals(Type.ANTIROLLREAR)
                ||  m_type.equals(Type.FUELMIXTURE)
                ||  m_type.equals(Type.THROTTLESHAPE)
                ||  m_type.equals(Type.TRACTIONCONTROL)
                ||  m_type.equals(Type.WEIGHTJACKERLEFT)                
                ||  m_type.equals(Type.WEIGHTJACKERRIGHT)
                ||  m_type.equals(Type.FRONTFLAP)
                ||  m_type.equals(Type.ENGINEBRAKING)
                ||  m_type.equals(Type.DIFFENTRY)
                ||  m_type.equals(Type.DIFFMIDDLE)
                ||  m_type.equals(Type.DIFFEXIT)
                ) {
                    setupValue.setValue(simValue.getDouble());
                }
                
                m_isSentToSIM = true; //The SIM knows it already changed it. This will prevent it from sending the command to change it again.
            }
        }
        
        m_firstreset = false;

        beforePitting(lap);
        
//        //now do the whole group
//        Iterator<Entry<String,Gauge>> itr = m_group.entrySet().iterator();
//        while (itr.hasNext()) {
//            Gauge g = itr.next().getValue();
//            g.reset(lap,app_ini_autoResetPitBox);
//        }

    }

    /**
     * This method should be called before pitting to allow the current value to be saved historically before they are changed.
     * @param lap The lap pitted.
     */
    public void beforePitting(int lap) {
        boolean firsttime = value == null ? true : false;
        _initValues();
        Data simValue = SIMValue != null ? SIMValue.convertUOM(m_uom) : new Data("",0.0);

        if (!m_changeFlag) {
            if (m_isDebug)
                Server.logger().finer(String.format("%s.beforePitting(%s) on lap %d, Current value=%f %s, Next value=%f %s, SIM value=%f %s"
                    , this.getClass().getName()
                    , m_type
                    , lap
                    , value.getDouble()
                    , value.getUOM()
                    , setupValue.getDouble()
                    , setupValue.getUOM()
                    , simValue.getDouble()
                    , simValue.getUOM()
                ));
        }
        else {
            if (firsttime) {
                //change where we start recording, but do record what we can't see.
                m_lapChanged = lap;
            }
            else
            if (lap - m_lapChanged > 0) {

                //TODO: Save history of the changes, for now at least save the last one
                if (SIMValueType == SIMValueTypes.ForCar
                ||  SIMValueType == SIMValueTypes.ForCarAndSetup
                ||  SIMValueType == SIMValueTypes.ForCarZeroOnPit
                ) {
                    beforePitValue = new Data(simValue);

                    if (m_isDebug)
                        Server.logger().finer(String.format("%s.beforePitting(%s). Saving Laps=%d, value=%f %s as Historical"
                            , this.getClass().getName()
                            , m_type
                            , lap - m_lapChanged
                            , beforePitValue.getDouble()
                            , beforePitValue.getUOM()
                        ));
                }
            }
        }
//        //now do the whole group
//        Iterator<Entry<String,Gauge>> itr = m_group.entrySet().iterator();
//        while (itr.hasNext()) {
//            Gauge g = itr.next().getValue();
//            g.beforePitting(lap);
//        }
    }

    /**
     * Call this method to get the gauge to take a reading and store it internally.
     */
    public void takeReading() {
        _initValues();
        m_SIMValue = SIMValue != null ? SIMValue.convertUOM(m_uom) : new Data("",0.0);
        //now do the whole group
        Iterator<Entry<String,Gauge>> itr = m_group.entrySet().iterator();
        while (itr.hasNext()) {
            Gauge g = itr.next().getValue();
            g.takeReading();
        }
    }

    /**
     * Call this method after pitting to allow saving historical values that can only be read after you pit.
     * @param lap The lap you pitted on.
     */
    public void afterPitting(int lap) {
        boolean firsttime = value == null ? true : false;
        _initValues();
        if (m_SIMValue == null)
            takeReading();

        if (!m_changeFlag) {
            if (m_isDebug)
                Server.logger().finer(String.format("%s.afterPitting(%s) on lap %d, Current value=%f %s, Next value=%f %s, SIM value=%f %s"
                    , this.getClass().getName()
                    , m_type
                    , lap
                    , value.getDouble()
                    , value.getUOM()
                    , setupValue.getDouble()
                    , setupValue.getUOM()
                    , m_SIMValue.getDouble()
                    , m_SIMValue.getUOM()
                ));
        }
        else {
            if (firsttime) {
                //change where we start recording, but do record what we can't see.
                m_lapChanged = lap;
                if (m_isDebug)
                    Server.logger().finer(String.format("%s.afterPitting(%s). First time, initializing m_lapChanged to %d"
                        , this.getClass().getName()
                        , m_type
                        , m_lapChanged
                    ));
            }
            else
            if (lap - m_lapChanged > 0) {

                //TODO: Save history of the changes, for now at least save the last one
                if (beforePitValue != null
                || SIMValueType == SIMValueTypes.AfterPit
                ) {
                    afterPitLaps.setValue(lap - m_lapChanged);
                    afterPitValue = beforePitValue != null ? beforePitValue : m_SIMValue;
                    beforePitValue = null;
                    m_lapChanged = lap;
                    m_count++;

                    if (m_isDebug)
                        Server.logger().finer(String.format("%s.afterPitting(%s). Saving Laps=%d, value=%f %s as Historical"
                            , this.getClass().getName()
                            , m_type
                            , afterPitLaps.getInteger()
                            , afterPitValue.getDouble()
                            , afterPitValue.getUOM()
                        ));
                }
                else {
                    if (m_isDebug)
                        Server.logger().finer(String.format("%s.afterPitting(%s). with laps, nothing recorded, m_lapChanged=%d, lap=%d"
                                , this.getClass().getName()
                                , m_type
                                , m_lapChanged
                                , lap
                            ));
                }
            }
            else {
                if (m_isDebug)
                    Server.logger().finer(String.format("%s.afterPitting(%s). no laps recorded, m_lapChanged=%d, lap=%d"
                        , this.getClass().getName()
                        , m_type
                        , m_lapChanged
                        , lap
                    ));
            }

            //Update the value with the setup value if the SIM is not giving it to us
            if (SIMValueType != SIMValueTypes.ForCar
            &&  SIMValueType != SIMValueTypes.ForCarZeroOnPit
            ) {
                value = new Data(setupValue);
            }

        //Reset the new Setup Value
            //Need to set the setupValue to it's initial value
            //fuel = 0.0, tearoff=0.0, tirepressure=nochange, wedge=nochange, brakebias=nochange, tape=nochange
            if (SIMValueType == SIMValueTypes.ForSetup
            ||  SIMValueType == SIMValueTypes.ForCarAndSetup
            ) {
                setupValue.setValue(m_SIMValue);
            }
            else
            if (SIMValueType == SIMValueTypes.ZeroOnPit
            ||  SIMValueType == SIMValueTypes.ForCarZeroOnPit
            ) {
                setupValue.setValue(0.0);
            }

            m_changeFlag = false;
            m_isSentToSIM = false;
        }

        m_SIMValue = null;
        
        //now do the whole group
        Iterator<Entry<String,Gauge>> itr = m_group.entrySet().iterator();
        while (itr.hasNext()) {
            Gauge g = itr.next().getValue();
            g.afterPitting(lap);
        }
        
    }

    /**
     * Used internally to set the reader the gauge will use to get values from the SIM.
     * @param d The data object that will return the value when getValue() is called on it.
     * @param t The type of reader it is as defined by {@link com.SIMRacingApps.Gauge#SIMValueType} of what the reader will return.
     */
    public void setSIMValue(Data d, SIMValueTypes t ) {
        SIMValue = d;
        SIMValueType = t;
    }
}

