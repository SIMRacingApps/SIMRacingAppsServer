package com.SIMRacingApps;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Level;

import com.SIMRacingApps.Util.FindFile;
import com.owlike.genson.*;
import com.owlike.genson.stream.JsonStreamException;

/**
 * A container for all values returned from all Simulators.
 * <p> 
 * It contains the following access get/set methods. Each set method returns "this", so you can chain them.
 * <ul>
 * <li><b>Name</b> - The name of the value.</li> 
 * <li><b>Value</b> - The actual value untranslated</li> 
 * <li><b>UOM</b> - Unit of Measure.</li> 
 * <li><b>UOMAbbr</b> - The translated short abbreviation name for the UOM.</li> 
 * <li><b>UOMDescr</b> - The translated long name for the UOM. Singular and Plural rules are applied.</li> 
 * <li><b>Format</b> - A java format string using rules defined by {@link java.lang.String#format(Locale, String, Object...)}.</li> 
 * <li><b>FormattedValue</b> - The value after it has been formated and translated.</li> 
 * <li><b>Lang</b> - The locale used to translate and format the value returned in FormattedValue.</li> 
 * <li><b>State</b> - A String value indicating the Value is within the range of a defined State.</li> 
 * <li><b>StatePercent</b> - The percentage the value is between the range of the State.</li> 
 * <li><b>Type</b> - The type the Value is as defined by {@link com.SIMRacingApps.Data.Type}.</li> 
 * <li><b>isDirty</b> - Compares new value to current, returns true if different. Initially true.</li> 
 *</ul>
 * Multiple values can be added by name. 
 * The first named value added is called the default name.
 * All of the access methods take "name" as an argument, but also there's an overloaded version that doesn't take a name and uses the default name.
 * @author Jeffrey Gilliam
 * @since 1.0
 * @copyright Copyright (C) 2015 - 2016 Jeffrey Gilliam
 * @license Apache License 2.0
 */

public class Data extends Object {

//    private Genson gensonPretty = new Genson.Builder().useIndentation(true).create(); //this is really slow.
    private static Genson genson = new Genson();

    /** Defines the Type the Value is */
    public enum Type {
        UNKNOWN,BOOLEAN,STRING,DOUBLE,INTEGER,LONG,FLOAT,ARRAY
    };

    /**
     * Enumerates the States that are available, standard. Users can add their own states as well.
     */
    public static class State {
        /** NOTAVAILABLE, the value is not available in this configuration */
        public static final String NOTAVAILABLE      = "NOTAVAILABLE";
        /** OFF, not under power, not outputting data */
        public static final String OFF               = "OFF";
        /** NORMAL, under power and outputting data */
        public static final String NORMAL            = "NORMAL";
        /** WARNING, not defined by default, but allows user to define the range */ 
        public static final String WARNING           = "WARNING";
        /** CRITICAL, not defined by default, but allows user to define the range */ 
        public static final String CRITICAL          = "CRITICAL";
        /** ERROR, Data cannot be read, there is an error message in the Value */
        public static final String ERROR             = "ERROR";
    }

    /**
     * Internal container for the data. It holds only one named value. 
     */
    protected class cData {
        public String    Name;
        public Object    Value;
        public String    UOM;
        public String    requestedUOM;
        public Type      Type;
        public Type      ArrayType;
        public String    Format;
        public String    State;
        public Double    StatePercent;
        public String    Lang;
        public String    ValueFormatted;
        public Long      Interval;
        public Long      TimeStamp;
        public Locale    locale;
        public cData(String pName,Object pValue,String pUOM,Type pType) {
            Name           = new String(pName == null ? "" : pName);
            Value          = pValue == null ? "" : pValue;
            UOM            = new String(pUOM != null ? pUOM : "");
            requestedUOM   = "";
            Type           = pType;
            Format         = "";
            State          = Data.State.OFF;
            StatePercent   = new Double(0);
            Lang           = System.getProperty("user.language");
            ValueFormatted = pValue != null ? pValue.toString() : "";
            Interval       = 0L;
            TimeStamp      = 0L;
            setLocale();
        }
        public cData( cData data ) {
            Name            = new String(data.Name);
            UOM             = new String(data.UOM);
            requestedUOM    = new String(data.requestedUOM);
            Type            = data.Type;
            Format          = new String(data.Format);
            State           = new String(data.State);
            StatePercent    = new Double(data.StatePercent);
            Lang            = new String(data.Lang);
            ValueFormatted  = new String(data.ValueFormatted);
            Interval        = new Long(data.Interval);
            TimeStamp       = new Long(data.TimeStamp);
            locale          = (Locale)data.locale.clone();
            switch (Type) {
            case BOOLEAN: Value = new Boolean((Boolean)data.Value); break;
            case STRING:  Value = new String((String)data.Value); break;
            case DOUBLE:  Value = new Double((Double)data.Value); break;
            case INTEGER: Value = new Integer((Integer)data.Value); break;
            case LONG:    Value = new Long((Long)data.Value); break;
            case FLOAT:   Value = new Float((Float)data.Value); break;
            case ARRAY:   @SuppressWarnings("unchecked")
                          ArrayList<Object> s = (ArrayList<Object>)data.Value;
                          ArrayList<Object> d = new ArrayList<Object>();
                          for (int i=0; i < s.size(); i++) {
                              switch (m_getType(s.get(i))) {
                                  case BOOLEAN: d.add(new Boolean((Boolean)s.get(i))); break;
                                  case STRING:  d.add(new String((String)s.get(i))); break;
                                  case DOUBLE:  d.add(new Double((Double)s.get(i))); break;
                                  case INTEGER: d.add(new Integer((Integer)s.get(i))); break;
                                  case LONG:    d.add(new Long((Long)s.get(i))); break;
                                  case FLOAT:   d.add(new Float((Float)s.get(i))); break;
                                  default:      d.add(s.get(i));
                              }
                          }
                          Value = d;
                          break;
            default:      Value = data.Value; //TODO: Need to clone this but type is not known
            }
        }
        
        /**
         * Sets the Locale based on Lang. Looks for the format {language}_{country}.
         */
        public void setLocale() {
            String[] s = Lang.split("[_-]");
            Locale l = null;
            if (s.length > 1)
                    l = new Locale(s[0],s[1].toUpperCase());
            else
            if (s.length == 1)
                    l = new Locale(s[0]);
            else
                    l = new Locale(Lang);
            locale = l;
        }
        
        /**
         * Returns true if the value is considered empty.
         * Numerics are considered empty if the value is Not A Number or zero for integers.
         * @return true if empty, false if not.
         */
        public boolean isEmpty() {
            if (Value == null)
                return true;
            
            switch (Type) {
            case BOOLEAN: return Value == null;
            case STRING:  return ((String)Value).isEmpty();
            case DOUBLE:  return ((Double)Value).isNaN();
            case INTEGER: return ((Integer)Value).equals(0);
            case LONG:    return ((Long)Value).equals(0L);
            case FLOAT:   return ((Float)Value).isNaN();
            default:      break;
            }
            
            return false;
        }
    }

    /**
     * Detects the type of the value.
     * @param value The value to detect.
     * @return The Type as defined by {@link com.SIMRacingApps.Data.Type}.
     */
    private Type m_getType(Object value) {
        if (value == null || value instanceof String) {
            return Type.STRING;
        }
        else
        if (value instanceof Boolean) {
            return Type.BOOLEAN;
        }
        else
        if (value instanceof Double) {
            return Type.DOUBLE;
        }
        else
        if (value instanceof Integer) {
            return Type.INTEGER;
        }
        else
        if (value instanceof Long) {
            return Type.LONG;
        }
        else
        if (value instanceof Float) {
            return Type.FLOAT;
        }
        else
        if (value instanceof ArrayList) {
            return Type.ARRAY;
        }
//        else {
//            System.err.printf("SIMData() value type of %s is unknown\n",value.getClass());
//        }
        return Type.UNKNOWN;
    }

    private Boolean m_dirty = true;
    private String m_id;
    private String m_defaultname;
    private Map<String,Map<String,Double>> m_localUnits = null; //don't allocate this until you need it
    protected Map<String,cData> m_data;

    /**
     * Internal method that adds a new named value.
     * @param name  The name of the value.
     * @param value The value.
     * @param UOM   The Unit of Measure.
     * @return      The name that was added.
     */
    private String m_add(String name,Object value,String UOM) {
        if (m_data == null)
            m_data = new HashMap<String,cData>();
        m_data.put(name, new cData(name,value,UOM,m_getType(value)));
        m_dirty = true;
        return name;
    }

    /**
     * Internal method to make a copy of another Data object.
     * @param data The Data object to copy
     */
    private void m_copy(Data data) {
        m_dirty = new Boolean(data.m_dirty);
        m_id    = new String(data.m_id);
        m_defaultname = new String(data.m_defaultname);
        data.getValue(m_defaultname); //Just for subclasses to update before we copy them
        m_data = new HashMap<String,cData>();
        for (String name : data.m_data.keySet()) {
            m_data.put(name,new cData(data.m_data.get(name)));
        }
        if (data.m_localUnits == null) {
            m_localUnits = null;
        }
        else {
            m_localUnits = new HashMap<String,Map<String,Double>>();
            for (String fromUOM : data.m_localUnits.keySet()) {
                Map<String,Double> conversions = new HashMap<String,Double>();
                
                for (String toUOM : data.m_localUnits.get(fromUOM).keySet()) {
                    conversions.put(toUOM,data.m_localUnits.get(fromUOM).get(toUOM));
                }
                m_localUnits.put(fromUOM, conversions);
            }
        }
    }
    
    /**
     * Constructor(s). Each argument is optional, except name which is required.
     * @param name  The name of the value. Becomes the default name.
     * @param value (Optional), the value. Default is "".
     * @param UOM   (Optional), the Unit of Measure of the value. Default is "".
     * @param state (Optional), the initial State of the value. Default is "OFF".
     */
    public Data(String name,Object value,String UOM,String state)   { m_id = m_defaultname = m_add(name, value, UOM); setState(state); }
    public Data(String name,Object value,String UOM)                { m_id = m_defaultname = m_add(name, value, UOM);}
    public Data(String name,Object value)                           { m_id = m_defaultname = m_add(name, value, "");}
    public Data(String name)                                        { m_id = m_defaultname = m_add(name, "", "");}

    /**
     * Copy Constructor.
     * @param data The Data instance to copy.
     */
    public Data(Data data)                                          { m_copy(data); }
    
    @SuppressWarnings("unused")
    /**
     * Constructor with no name. This is not allowed, so it is made private to give you an error.
     */
    private Data()                                                  {}

    /**
     * Adds a new named value to this container Each argument is optional, except name which is required.
     * @param name  The name of the value. Becomes the default name.
     * @param value (Optional), the value. Default is "".
     * @param UOM   (Optional), the Unit of Measure of the value. Default is "".
     */
    public void add(String name,Object value,String UOM)            { m_add(name, value, UOM);}
    public void add(String name,Object value)                       { m_add(name, value, "");}
    public void add(String name)                                    { m_add(name, "", "");}

    /**
     * Adds all the named values from another Data instance.
     * @param d The Data instance to copy from.
     */
    public void add(Data d)                                         { for (String name : d.m_data.keySet()) {
                                                                        m_add(name,d.getValue(name),d.getUOM(name));
                                                                        setFormat(name,d.getFormat(name));
                                                                        setLang(name, d.getLang(name));
                                                                        setState(name, d.getState(name));
                                                                        setStatePercent(name,d.getStatePercent(name));
                                                                      }
                                                                      setId(d.getId());
                                                                    }

    /**
     * Copies all the named values from another Data instance as if it was cloning it.
     * This differs from the copy constructor by the fact that a new object is not created.
     * @param d The Data instance to copy from.
     */
    public void set(Data d)                                         { m_copy(d); }
    
    /**
     * Removes a named value from the container.
     * @param name The name of the value.
     */
    public void remove(String name)                                 { m_data.remove(name); m_dirty=true; }

    /**
     * Add a unit of measure conversion directly to this object.
     * Useful for non-generic conversions like, weight to volume.
     * This can also me used to override the generic conversions as it is checked first.
     * @param fromUOM The from unit of measure
     * @param toUOM   The to unit of measure
     * @param factor  The conversion factor to convert from to to.
     * @return this
     */
    public Data addConversion(String fromUOM, String toUOM, double factor) {
        if (m_localUnits == null)
            m_localUnits = new HashMap<String,Map<String,Double>>();
            
        Map<String,Double> conversions = m_localUnits.get(fromUOM.toUpperCase());
        if (conversions == null)
            m_localUnits.put(fromUOM.toUpperCase(),conversions = new HashMap<String,Double>());
        conversions.put(toUOM.toUpperCase(), factor);
        return this;
    }
    
    /**
      * Add all the units from another data object.
      * Existing conversions will be overridden.
      * @param data The data object to copy conversions from
      * @return this
      */
    public Data addConversion(Data data) {
        if (data != null && data.m_localUnits != null) {
            if (m_localUnits == null)
                m_localUnits = new HashMap<String,Map<String,Double>>();
                
            for (String fromUOM : data.m_localUnits.keySet()) {
                Map<String,Double> conversions = m_localUnits.get(fromUOM);
                if (conversions == null)
                    conversions = new HashMap<String,Double>();
                
                for (String toUOM : data.m_localUnits.get(fromUOM).keySet()) {
                    conversions.put(toUOM,data.m_localUnits.get(fromUOM).get(toUOM));
                }
                m_localUnits.put(fromUOM, conversions);
            }
        }
        return this;
    }
    
    /**
     * Returns true if the Data has changed.
     * It is up to you to call setDirty(false), to detect changes.
     * Initially, a Data instance is dirty.
     * @return true if dirty
     */
    public boolean  isDirty()                                       { return m_dirty; }
    /**
     * Sets the dirty flag.
     * @param dirty The dirty flag.
     * @return A reference to this.
     */
    public Data     setDirty(Boolean dirty)                         { m_dirty = dirty; return this; }
    
    /**
     * Returns true if the value is older than the specified interval.
     * @param name (Optional), the name of the value.
     * @return true if data is old, false if not.
     */
    public boolean  isDataStale(String name)                        { return getTimeStamp(name) + getInterval(name) <= System.currentTimeMillis(); }
    public boolean  isDataStale()                                   { return isDataStale(m_defaultname); }
    
    /**
     * Returns true if the value is considered empty.
     * Numerics are considered empty if the value is Not A Number or zero for integers.
     * @param name (Optional), the name of the value.
     * @return true if empty
     */
    public boolean  isEmpty(String name)                            { return m_data.get(name) == null ? true : m_data.get(name).isEmpty(); }
    public boolean  isEmpty()                                       { return isEmpty(m_defaultname); }

    /**
     * Returns the Id of this instance. This is not the default name, it is a separate attribute.
     * @return The Id.
     */
    public String   getId()                                         { return m_id; }
    /**
     * Sets the Id for this instance.
     * @param id The Id.
     * @return A reference to this.
     */
    public Data     setId(String id)                                { m_id = id;  /* m_dirty=true; */ return this; }

    /**
     * Returns an array of all of the names for the named values that are in this container.
     * @return An Array of names.
     */
    public String[] getNames()                                      { String[] a = m_data.keySet().toArray(new String[0]); return a;}
    
    /**
     * Returns the default name.
     * @return The default name.
     */
    public String   getName()                                       { return m_defaultname;}

    /**
     * Sets or updates the default name.
     * @param name The new name
     * @return A reference to this.
     */
    public Data setName(String name)                                {
                                                                        if (!m_defaultname.equals(name)) {
                                                                          cData d = m_data.get(m_defaultname);
                                                                          if (d != null) {
                                                                              d.Name = name;
                                                                              m_data.remove(m_defaultname);
                                                                              m_data.put(name, d);
                                                                          }
                                                                          m_defaultname = name;
                                                                          m_dirty=true;
                                                                        }
                                                                        return this;
                                                                    }

    /**
     * Returns the value as an Object.
     * @param name (Optional), the name of the value.
     * @return The value.
     */
    public Object   getValue(String name)                           { return m_data.get(name) == null ? null : m_data.get(name).Value;}
    public Object   getValue()                                      { return getValue(m_defaultname);}

    /**
     * Sets the value for the object.
     * If the value is different from the current value, then the dirty flag is set to true.
     * @param name  (Optional), The name of the value.
     * @param value The value
     * @return      A reference to this.
     */
    public Data setValue(String name, Object value)                 { if (m_data.get(name) == null)
                                                                        add(name,value);
                                                                      else {
                                                                        if (!m_data.get(name).Value.equals(value)) {
                                                                            m_data.get(name).Value = value;
                                                                            m_data.get(name).Type = m_getType(value);
                                                                            m_dirty = true;
                                                                        }
                                                                      }
                                                                      return this;
                                                                    }
    public Data setValue(Object value)                              { setValue(m_defaultname,value); return this;}
    
    /**
     * As well as the standard {@link com.SIMRacingApps.Data#setValue(String, Object)}, 
     * I have overloaded it to take value and UOM together as well as optionally, 
     * State and Percentage when setting for the default name.
     * @param value The value.  
     * @param UOM   The unit of measure.
     * @param state (Optionally), The State.
     * @param pct   (Optionally), The state percentage
     * @return      A reference to this.
     */
    public Data setValue(Object value,String UOM,String state,Double pct) { setValue(m_defaultname,value); setUOM(m_defaultname,UOM); setState(m_defaultname,state); setStatePercent(m_defaultname,pct); return this;}
    public Data setValue(Object value,String UOM,String state)      { setValue(m_defaultname,value); setUOM(m_defaultname,UOM); setState(m_defaultname,state); return this;}
    //the (Object,String) version is here to cause a conflict with (String(name),Object) 
    //when passing a String as the value or you will not be calling the right one.
    //This will force the user to use one of the previous 2 versions since a string type doesn't need a UOM anyway
    public Data setValue(Object value,String UOM)                   { setValue(m_defaultname,value); setUOM(m_defaultname,UOM);  return this;}
    
    
    /**
     * Returns the unit of measure.
     * @param name (Optional), the name of the value.
     * @return The unit of measure
     */
    public String   getUOM(String name)                             { return m_data.get(name) == null ? "" : (m_data.get(name).UOM == null ? "" : m_data.get(name).UOM);}
    public String   getUOM()                                        { return getUOM(m_defaultname);}

    /**
     * Sets the unit of measure.
     * NOTE: It does not convert the existing value to this unit of measure
     * @param name The name of the value
     * @param UOM  The unit of measure
     * @return A reference to this.
     */
    public Data setUOM(String name, String UOM)                     { if (m_data.get(name) == null)
                                                                        add(name,0.0,UOM);
                                                                      else {
                                                                        if (UOM != null && !m_data.get(name).UOM.equals(UOM)) {
                                                                            m_data.get(name).UOM = new String(UOM);
                                                                            m_dirty = true;
                                                                        }
                                                                      }
                                                                      return this;
                                                                    }
    public Data setUOM(String UOM)                                  { setUOM(m_defaultname,UOM); return this;}

    /**
     * Returns the Requested Unit of Measure.
     * This is a separate attribute from the UOM so that you can track what unit of measure you want, without having to store it somewhere else.
     * @param name (Optional), The name of the value.
     * @return The requested unit of measure
     */
    public String   getRequestedUOM(String name)                    { return m_data.get(name) == null ? null : m_data.get(name).requestedUOM;}
    public String   getRequestedUOM()                               { return getRequestedUOM(m_defaultname);}
    
    /**
     * Sets the Requested Unit of Measure.
     * @param name (Optional), The name of the value.
     * @param UOM The unit of measure.
     * @return A reference to this.
     */
    public Data setRequestedUOM(String name, String UOM)            { if (m_data.get(name) == null) {
                                                                        add(name,0.0,UOM);
                                                                        m_data.get(name).requestedUOM = new String(UOM);
                                                                      }
                                                                      else {
                                                                        if (UOM != null && !m_data.get(name).requestedUOM.equals(UOM)) {
                                                                            m_data.get(name).requestedUOM = new String(UOM);
                                                                            m_dirty = true;
                                                                        }
                                                                      }
                                                                      return this;
                                                                    }
    public Data setRequestedUOM(String UOM)                         { setRequestedUOM(m_defaultname,UOM); return this;}
    
    /**
     * Returns a translated abbreviation of the unit of measure.
     * @param name (Optional), The name of the value.
     * @return The unit of measure abbreviation.
     */
    public String   getUOMAbbr(String name)                         {
                                                                        if (m_data.get(name) == null) return null;
                                                                        String estimated = getUOM(name).startsWith("~") ? "~" : "";
                                                                        String UOM = estimated.isEmpty() ? getUOM(name) : getUOM(name).substring(1);
                                                                        
                                                                        if (_units().containsKey(UOM.toUpperCase())) {
                                                                            @SuppressWarnings("unchecked")
                                                                            String x = ((Map<String,String>)_units().get(UOM.toUpperCase())).get("Abbr");
                                                                            if (x != null) {
                                                                                return estimated + x;
                                                                            }
                                                                        }
                                                                        return estimated + m_data.get(name).UOM;
                                                                    }
    public String    getUOMAbbr()                                   { return getUOMAbbr(m_defaultname);}
    
    /**
     * Returns a translated long description of the unit of measure.
     * It will reflect singular and plural version depending on the value.
     * @param name (Optional), The name of the value.
     * @return The unit of measure description.
     */
    public String    getUOMDesc(String name)                        {
                                                                        if (m_data.get(name) == null) return null;
                                                                        String UOMDesc = getUOM(name);
                                                                        String estimated = getUOM(name).startsWith("~") ? "~" : "";
                                                                        String UOM = estimated.isEmpty() ? getUOM(name) : getUOM(name).substring(1);
                                                                        
                                                                        if (_units().containsKey(UOM.toUpperCase())) {
                                                                            String[] uoms = getUOM(name).split("/");
                                                                            if (getDouble(name) == 1.0) {
                                                                                @SuppressWarnings("unchecked")
                                                                                String x = ((Map<String,String>)_units().get(UOM.toUpperCase())).get("Singular");
                                                                                if (x != null) {
                                                                                    UOMDesc = estimated + x;
                                                                                }
                                                                            }
                                                                            else {
                                                                                @SuppressWarnings("unchecked")
                                                                                String x = ((Map<String,String>)_units().get(UOM.toUpperCase())).get("Plural");
                                                                                if (x != null) {
                                                                                    UOMDesc = estimated + x;
                                                                                }
                                                                            }

                                                                            if (uoms.length > 1) {
                                                                                @SuppressWarnings("unchecked")
                                                                                String x = ((Map<String,String>)_units().get(UOM.toUpperCase())).get("Singular");
                                                                                if (x != null) {
                                                                                    UOMDesc += "/"+estimated + x;
                                                                                }
                                                                            }
                                                                        }
                                                                        return UOMDesc;
                                                                    }
    public String    getUOMDesc()                                   { return getUOMDesc(m_defaultname);}
    
    /**
     * Returns the Type as defined by {@link com.SIMRacingApps.Data.Type}.
     * @param name (Optional), The name of the value.
     * @return The Type.
     */
    public Type      getType(String name)                           { return m_data.get(name) == null ? null : m_data.get(name).Type;}
    public Type      getType()                                      { return getType(m_defaultname);}
    
    /**
     * Returns the format string.
     * @param name (Optional), The name of the value.
     * @return The format string.
     */
    public String    getFormat(String name)                         { return m_data.get(name) == null ? null : m_data.get(name).Format;}
    public String    getFormat()                                    { return getFormat(m_defaultname);}

    /**
     * Sets the format using the same syntax as {@link java.lang.String#format(Locale, String, Object...)}.
     * @param name   (Optionally), The name of the value.
     * @param format The format string.
     * @return       A reference to this.
     */
    public Data setFormat(String name, String format)               { if (m_data.get(name) == null) add(name,0.0); else {m_data.get(name).Format = new String(format);} return this;}
    public Data setFormat(String format)                            { setFormat(m_defaultname,format); return this;}
    
    /**
     * Returns the current State as defined by {@link com.SIMRacingApps.Data.State}.
     * Note: Other states can be added, it is just a string.
     * @param name (Optional), The name of the value.
     * @return     The State
     */
    public String    getState(String name)                          { return m_data.get(name) == null ? "" : m_data.get(name).State;}
    public String    getState()                                     { return getState(m_defaultname);}

    /**
     * Sets the State.
     * @param name  (Optionally), The name of the value.
     * @param state The State
     * @return      A reference to this.
     */
    public Data setState(String name, String state)                 { if (m_data.get(name) == null) add(name,0.0); else {m_data.get(name).State = new String(state);} return this;}
    public Data setState(String state)                              { setState(m_defaultname,state); return this;}
    
    /**
     * Returns the percentage of where the current value lies within the range of the State.
     * @param name (Optional), The name of the value.
     * @return The percentage of the State.
     */
    public double    getStatePercent(String name)                   { return m_data.get(name) == null ? null : m_data.get(name).StatePercent;}
    public double    getStatePercent()                              { return getStatePercent(m_defaultname);}
    
    /**
     * Sets the State Percentage.
     * Note: You have to calculate this percentage and call this function to set it. 
     * No ranges are stored within this object.
     * Should be a value that is between 0.0 and 100.0, but doesn't have to be. 
     * @param name         (Optionally), The name of the value.
     * @param statepercent The State Percentage
     * @return             A reference to this.
     */
    public Data setStatePercent(String name, Double statepercent)   { if (m_data.get(name) == null) add(name,0.0); else {m_data.get(name).StatePercent = statepercent;} return this;}
    public Data setStatePercent(Double statepercent)                { setStatePercent(m_defaultname,statepercent); return this;}
    
    /**
     * Returns the language that this container will use for translations and formatting.
     * @param name (Optional), The name of the value.
     * @return The language
     */
    public String    getLang(String name)                           { return m_data.get(name) == null ? null : m_data.get(name).Lang;}
    public String    getLang()                                      { return getLang(m_defaultname);}

    /**
     * Sets the language to be used for translations and formatting of this value.
     * This must be a Java supported language.
     * 
     * @param name (Optional), The name of the value.
     * @param lang The language formatted as {language}_{country}.
     * @return     A reference to this.
     */
    public Data setLang(String name, String lang)                   {
                                                                        if (m_data.get(name) == null)
                                                                          add(name,0.0);
                                                                        else {
                                                                          if (lang != null) {
                                                                              m_data.get(name).Lang = new String(lang);
                                                                              m_data.get(name).setLocale();
                                                                          }
                                                                        }
                                                                        return this;
                                                                    }
    public Data setLang(String lang)                                { setLang(m_defaultname,lang); return this;}

    
    /**
     * Returns the interval in milliseconds used by {@link com.SIMRacingApps.Data#isDataStale(String)}.
     * @param name (Optional), The name of the value.
     * @return The interval in milliseconds.
     */
    public long      getInterval(String name)                       { return m_data.get(name) == null ? null : m_data.get(name).Interval;}
    public long      getInterval()                                  { return getInterval(m_defaultname);}
    
    /**
     * Sets the interval in milliseconds used by {@link com.SIMRacingApps.Data#isDataStale(String)}.
     * @param name (Optional), The name of the value.
     * @param interval milliseconds
     * @return A reference to this.
     */
    public Data setInterval(String name, Long interval)             { if (m_data.get(name) == null)
                                                                        add(name);
                                                                      else {
                                                                        if (interval != null && !m_data.get(name).Interval.equals(interval)) {
                                                                            m_data.get(name).Interval = interval;
                                                                            m_dirty = true;
                                                                        }
                                                                      }
                                                                      return this;
                                                                    }
    public Data setInterval(Long value)                             { setInterval(m_defaultname,value); return this;}
    
    /**
     * Returns the time stamp that was set by {@link com.SIMRacingApps.Data#updateTimeStamp(String)}.
     * @param name (Optional), The name of the value.
     * @return The time stamp in milliseconds.
     */
    public long      getTimeStamp(String name)                      { return m_data.get(name) == null ? null : m_data.get(name).TimeStamp;}
    public long      getTimeStamp()                                 { return getTimeStamp(m_defaultname);}

    /**
     * Updates the time stamp to the current time in milliseconds
     * @param name (Optional), The name of the value.
     * @return A reference to this.
     */
    public Data updateTimeStamp(String name)                        { if (m_data.get(name) != null)
                                                                        m_data.get(name).TimeStamp = System.currentTimeMillis();
                                                                      return this;
                                                                    }
    public Data updateTimeStamp()                                   { return updateTimeStamp(m_defaultname);}


    /**
     * Returns the value formatted using the format string and translated.
     * @param name The name of the value.
     * @return     The formatted value.
     */
    public String getValueFormatted(String name)                    { return m_data.get(name).Format.equals("") ? getString(name) : getStringFormatted(name,m_data.get(name).Format); }
    public String getValueFormatted()                               { return getValueFormatted(m_defaultname); }

    /**
     * Returns the value as a string. Numbers are converted to a string by calling the toString() method.
     * Therefore, it is not formatted. Use getValueFormatted(), which always returns a string.
     * @param name (Optional), The name of the value.
     * @return     The value as a String.
     */
    public String getString(String name)                            {
                                                                        Object o = getValue(name);
                                                                        if (o != null)
                                                                            return new String(o.toString());
                                                                        return "";
                                                                    }
    public String getString()                                       { return getString(m_defaultname); }

    /**
     * Returns the value as a String, translated.
     * Note: This is yet to be done.
     * @param name The name of the value.
     * @return     The value as a string, translated.
     */
    public String getStringTranslated(String name)                  {
                                                                        //TODO: Add translation lookup
                                                                        return getString(name);
                                                                    }
    public String getStringTranslated()                             { return getStringTranslated( m_defaultname ); }

    /**
     * Returns the unit of measure abbreviation as a String, translated.
     * Note: This is yet to be done.
     * @param name The name of the value.
     * @return     The value as a string, translated.
     */
    public String getUOMAbbrTranslated(String name)                 {
                                                                        //TODO: Add translation lookup
                                                                        return getUOMAbbr(name);
                                                                    }
    public String getUOMAbbrTranslated() { return getUOMAbbrTranslated( m_defaultname ); }

    /**
     * Returns the unit of measure long description as a String, translated.
     * Note: This is yet to be done.
     * @param name The name of the value.
     * @return     The value as a string, translated.
     */
    public String getUOMDescTranslated(String name)                 {
                                                                        //TODO: Add translation lookup
                                                                        return getUOMDesc(name);
                                                                    }
    public String getUOMDescTranslated() { return getUOMDescTranslated( m_defaultname ); }

    /**
     * Returns the value formatted using the passed in format instead of the internal one.
     * The format syntax is defined by {@link java.lang.String#format(Locale, String, Object...)}.
     * If there is an error with the format syntax, that error is returned instead of the formatted value.
     * 
     * @param name   (Optional), The name of the value.
     * @param format The format string. If blank, a default appropriate for the Type is used.
     * @return       The formatted string.
     */
    public String getStringFormatted(String name, String format) {
        String s = getString(name);
        Locale l = m_data.get(name).locale;

//        if (!format.equals("")) {
            try {
                //support for date/time formatting based on UOM being "s" or "s/s" or "sec"
                if (getType(name) != null)
                    if ((!format.isEmpty() && (format.contains("t") || format.contains("T")))
                    && (  this.getUOM().equalsIgnoreCase("s")
                       || this.getUOM().equalsIgnoreCase("s/s")
                       || this.getUOM().equalsIgnoreCase("sec")
                       || this.getUOM().equalsIgnoreCase("lap")
                       )
                    ){
                        if (this.getUOM().equalsIgnoreCase("lap"))
                            s = String.format(l, !format.equals("") ? format : "%d", getInteger(name),           getUOMAbbrTranslated(name),  getUOMDescTranslated(name));
                        else {
                            Double d = getDouble(name) * 1000.0;
                            s = String.format(l, format, d.longValue(),  getUOMAbbrTranslated(name),  getUOMDescTranslated(name));
                        }
                    }
                    else {
                        switch (getType(name)) {
                        case BOOLEAN: s = getBoolean(name) ? "true" : "false"; break;
                        case STRING:  s = String.format(l, !format.equals("") ? format : "%s", getStringTranslated(name),  getUOMAbbrTranslated(name),  getUOMDescTranslated(name)); break;
                        case DOUBLE:  s = String.format(l, !format.equals("") ? format : "%f", getDouble(name),            getUOMAbbrTranslated(name),  getUOMDescTranslated(name)); break;
                        case INTEGER: s = String.format(l, !format.equals("") ? format : "%d", getInteger(name),           getUOMAbbrTranslated(name),  getUOMDescTranslated(name)); break;
                        case LONG:    s = String.format(l, !format.equals("") ? format : "%d", getLong(name),              getUOMAbbrTranslated(name),  getUOMDescTranslated(name)); break;
                        case FLOAT:   s = String.format(l, !format.equals("") ? format : "%f", getFloat(name),             getUOMAbbrTranslated(name),  getUOMDescTranslated(name)); break;
                        default:      s = getStringTranslated(name); break;
                        }
                    }
            }
            catch (MissingFormatArgumentException ex) {
                s = "FORMATERROR("+format+";"+ex.getMessage()+")";
            }
            catch (IllegalFormatConversionException ex) {
                s = "FORMATERROR("+format+";"+ex.getMessage()+")";
            }
            catch (UnknownFormatConversionException ex) {
                s = "FORMATERROR("+format+";"+ex.getMessage()+")";
            }
            catch (Exception ex) {
                s = "FORMATERROR("+format+";"+ex.getMessage()+")";
            }
//        }
        return s;
    }
    public String getStringFormatted(String format) { return getStringFormatted(m_defaultname,format); }

    /**
     * Returns the value as an array of boolean. 
     * If a String fails to parse to a boolean, false is returned.
     * It is a copy of the array, so modifying it will not modify the original.
     * You can modify it my doing a setValue() call with the modified array.
     * @param name (Optional), The name of the value.
     * @return     The value as a double.
     */
    @SuppressWarnings("unchecked")
    public ArrayList<Boolean> getBooleanArray(String name) {
        ArrayList<Boolean> d = new ArrayList<Boolean>(); 
        if (getType(name) != null) {
            boolean b = false;
        
            switch (getType(name)) {
            case BOOLEAN:b = (Boolean)getValue(name); d.add(b); break; 
            case STRING: if (getValue(name).toString().equalsIgnoreCase("Y")) b = true;
                         if (getValue(name).toString().equalsIgnoreCase("YES")) b = true;
                         if (getValue(name).toString().equalsIgnoreCase("T")) b = true;
                         if (getValue(name).toString().equalsIgnoreCase("TRUE")) b = true;
                         if (getValue(name).toString().equals("1")) b = true;
                         d.add(b);
                         break;
            case INTEGER:b = (Integer)getValue(name) != 0; break;
            case DOUBLE: b = ((Double)getValue(name)) != 0.0; d.add(b); break;
            case LONG:   b = ((Long)getValue(name)) != 0L;    d.add(b); break;
            case FLOAT:  b = ((Float)getValue(name)) != 0.0F; d.add(b); break;
            case ARRAY:  ArrayList<Object> a = (ArrayList<Object>)getValue(name);
                         for (int i=0; i < a.size(); i++) {
                             b = false;
                             switch (m_getType(a.get(0))) {
                                 case BOOLEAN:d.add((Boolean)a.get(i)); break;
                                 case STRING: if (((String)a.get(i)).equalsIgnoreCase("Y")) b = true;
                                              if (((String)a.get(i)).equalsIgnoreCase("YES")) b = true;
                                              if (((String)a.get(i)).equalsIgnoreCase("T")) b = true;
                                              if (((String)a.get(i)).equalsIgnoreCase("TRUE")) b = true;
                                              if (((String)a.get(i)).equals("1")) b = true;
                                              d.add(b);
                                              break;
                                 case INTEGER:b = ((Integer)a.get(i)) != 0;    d.add(b); break;
                                 case DOUBLE: b = ((Double)a.get(i)) != 0.0; d.add(b); break;
                                 case LONG:   b = ((Long)a.get(i)) != 0L;    d.add(b); break;
                                 case FLOAT:  b = ((Float)a.get(i)) != 0.0F; d.add(b); break;
                                 default:     break;
                             }
                             d.add(b);
                         }
                         break;
            default:     break;
            }
        }
        return d;
    }
    public ArrayList<Boolean> getBooleanArray()        { return getBooleanArray(m_defaultname); }
    
    /**
     * Returns the value as an array of doubles. 
     * If a String fails to parse to a double, Double.NaN is returned.
     * It is a copy of the array, so modifying it will not modify the original.
     * You can modify it my doing a setValue() call with the modified array.
     * @param name (Optional), The name of the value.
     * @return     The value as a double.
     */
    @SuppressWarnings("unchecked")
    public ArrayList<Double> getDoubleArray(String name) {
        ArrayList<Double> d = new ArrayList<Double>(); 
        if (getType(name) != null) {
            double v = Double.NaN;
        
            switch (getType(name)) {
            case BOOLEAN:v = (Boolean)getValue(name) ? 1.0 : 0.0; d.add(v); break; 
            case STRING:
                         try {
                             v = Double.parseDouble((String)getValue(name));
                         }
                         catch (NumberFormatException e) {}
                         d.add(v);
                         break;
            case INTEGER:v = ((Integer)getValue(name)).doubleValue(); d.add(v); break;
            case LONG:   v = ((Long)getValue(name)).doubleValue();    d.add(v); break;
            case FLOAT:  v = ((Float)getValue(name)).doubleValue();   d.add(v); break;
            case DOUBLE: v = (Double)getValue(name);                  d.add(v); break;
            case ARRAY:  ArrayList<Object> a = (ArrayList<Object>)getValue(name);
                         for (int i=0; i < a.size(); i++) {
                             v = Double.NaN;
                             switch (m_getType(a.get(0))) {
                                 case BOOLEAN:v = (Boolean)a.get(i) ? 1.0 : 0.0; d.add(v); break;
                                 case STRING: try {
                                                  v = Double.parseDouble((String)a.get(i));
                                              }
                                              catch (NumberFormatException e) {}
                                              break;
                                 case INTEGER:v = ((Integer)a.get(i)).doubleValue(); break;
                                 case LONG:   v = ((Long)a.get(i)).doubleValue(); break;
                                 case FLOAT:  v = ((Float)a.get(i)).doubleValue(); break;
                                 case DOUBLE: v = (Double)a.get(i); break;
                                 default:     break;
                             }
                             d.add(v);
                         }
                         break;
            default:     break;
            }
        }
        return d;
    }
    public ArrayList<Double> getDoubleArray()        { return getDoubleArray(m_defaultname); }

    /**
     * Returns the value as a boolean. For Strings, this means it cannot be blank, but if must be one of (Y,YES,T,TRUE).
     * All other values will return false.
     * For numeric values, any non-zero value returns true.
     * @param name (Optional), The name of the value.
     * @return     true or false
     */
    public boolean getBoolean(String name) {
        Boolean b = false;
        if (getType(name) != null)
            switch (getType(name)) {
            case BOOLEAN:b = (Boolean)getValue(name); break;
            case INTEGER:b = (Integer)getValue(name) != 0; break;
            case STRING: if (getValue(name).toString().equalsIgnoreCase("Y")) b = true;
                         if (getValue(name).toString().equalsIgnoreCase("YES")) b = true;
                         if (getValue(name).toString().equalsIgnoreCase("T")) b = true;
                         if (getValue(name).toString().equalsIgnoreCase("TRUE")) b = true;
                         if (getValue(name).toString().equals("1")) b = true;
                         break;
            case DOUBLE: b = ((Double)getValue(name)) != 0.0; break;
            case LONG:   b = ((Long)getValue(name)) != 0L; break;
            case FLOAT:  b = ((Float)getValue(name)) != 0.0F; break;
            default:     break;
            }
        return b;
    }
    public boolean getBoolean()      { return getBoolean(m_defaultname); }

    /**
     * Returns the value as a double. If a String fails to parse to a double, Double.NaN is returned.
     * @param name (Optional), The name of the value.
     * @return     The value as a double.
     */
    public double getDouble(String name) {
        double d = 0.0;
        if (getType(name) != null)
            switch (getType(name)) {
            case BOOLEAN:d = (Boolean)getValue(name) ? 1.0 : 0.0; break;
            case STRING: if (getValue(name).toString().equals("")) return d;
                         try {
                        	 d = Double.parseDouble(getValue(name).toString());
                         }
                         catch (NumberFormatException e) {
                        	 d = Double.NaN;
                         }
                         break;
            case INTEGER:d = ((Integer)getValue(name)).doubleValue(); break;
            case LONG:   d = ((Long)getValue(name)).doubleValue(); break;
            case FLOAT:  d = ((Float)getValue(name)).doubleValue(); break;
            case DOUBLE: d = (Double)getValue(name); break;
            default:     break;
            }
        return d;
    }
    public double getDouble()        { return getDouble(m_defaultname); }

    /**
     * Returns the value as a float. If a String fails to parse to a float, Float.NaN is returned.
     * @param name (Optional), The name of the value.
     * @return     The value as a float.
     */
    public Float getFloat(String name) {
        float d = 0.0f;
        if (getType(name) != null)
            switch (getType(name)) {
            case BOOLEAN:d = (Boolean)getValue(name) ? 1.0f : 0.0f; break;
            case STRING: if (getValue(name).toString().equals("")) return d;
                         try {
                        	 d = Float.parseFloat(getValue(name).toString());
                         }
    			         catch (NumberFormatException e) {
    			       	 	 d = Float.NaN;
    			         }
                         break;
            case INTEGER:d = ((Integer)getValue(name)).floatValue(); break;
            case LONG:   d = ((Long)getValue(name)).floatValue(); break;
            case FLOAT:  d = (Float)getValue(name); break;
            case DOUBLE: d = ((Double)getValue(name)).floatValue(); break;
            default:     break;
            }
        return d;
    }
    public float getFloat()          { return getFloat(m_defaultname); }

    /**
     * Returns the value as an integer. If a String fails to parse to an integer, zero(0) is returned.
     * If the value was a decimal, it is not rounded, it is floored.
     * @param name (Optional), The name of the value.
     * @return     The value as an integer.
     */
    public int getInteger(String name) {
        int i = 0;
        if (getType(name) != null)
            switch (getType(name)) {
            case BOOLEAN:i = (Boolean)getValue(name) ? 1 : 0; break;
            case INTEGER:i = (Integer)getValue(name); break;
            case STRING: if (getValue(name).toString().equals("")) return i;
                         try {
                        	 i = Integer.parseInt(getValue(name).toString());
                         }
    			         catch (NumberFormatException e) {
    			       	 	 i = 0;
    			         }
                         break;
            case DOUBLE: i = ((Double)getValue(name)).intValue(); break;
            case LONG:   i = ((Long)getValue(name)).intValue(); break;
            case FLOAT:  i = ((Float)getValue(name)).intValue(); break;
            default:     break;
            }
        return i;
    }
    public int getInteger()          { return getInteger(m_defaultname); }


    /**
     * Returns the value as a long. If a String fails to parse to a long, zero(0L) is returned.
     * If the value was a decimal, it is not rounded, it is floored.
     * @param name (Optional), The name of the value.
     * @return     The value as an integer.
     */
    public Long getLong(String name) {
        long l = (long)0;
        if (getType(name) != null)
            switch (getType(name)) {
            case BOOLEAN:l = (Boolean)getValue(name) ? 1L : 0L; break;
            case INTEGER:l = ((Integer)getValue(name)).longValue(); break;
            case LONG:   l = (Long)getValue(name); break;
            case STRING: if (getValue(name).toString().equals("")) return l;
                         try {
                        	 l = Long.parseLong(getValue(name).toString());
                         }
    			         catch (NumberFormatException e) {
    			       	 	 l = 0L;
    			         }
                         break;
            case DOUBLE: l = ((Double)getValue(name)).longValue(); break;
            case FLOAT:  l = ((Float)getValue(name)).longValue(); break;
            default:     break;
            }
        return l;
    }
    public long getLong()            { return getLong(m_defaultname); }

    @SuppressWarnings("unchecked")
    /**
     * Converts the value in the existing instance to the requested unit of measure and returns a new instance.
     * If the conversion cannot be made, then the new instance is a copy of the current instance with no conversion.
     * @param name (Optional), The name of the value.
     * @param UOM  The unit of measure.
     * @return     A new Data instance converted to UOM.
     */
    public Data convertUOM(String name,String UOM) {
        
        //TODO: For arrays, need to convert each element
        Data d = new Data(name,getValue(name),getUOM(name));    // Create one with just this value in it to return
        d.setFormat(name,getFormat(name));
        d.setLang(name, getLang(name));
        d.setState(name, getState(name));
        d.setStatePercent(name,getStatePercent(name));
        d.m_data.get(name).Interval = getInterval();
        d.m_data.get(name).TimeStamp = getTimeStamp();
        d.setId(getId());
        d.addConversion(this);

        String estimated = UOM.startsWith("~") || getUOM(name).startsWith("~") ? "~" : "";
        
        if (UOM == null || UOM.isEmpty() || UOM.toUpperCase().equals(getUOM(name).toUpperCase()))
            return d;

        String toUOM   = UOM.startsWith("~") ? UOM.substring(1) : UOM;
        String fromUOM = getUOM(name).startsWith("~") ? getUOM(name).substring(1) : getUOM(name);
        
        if (toUOM.toUpperCase().equals("NATIVE")) {
            if (this.getLang(name).endsWith("_US")  //TODO: According to Google, the US is the only country still using Imperial
            ||  this.getLang(name).equals("en")     //assume US if no country specified for English
            )  
                toUOM = "IMPERIAL";
            else
                toUOM = "METRIC";
        }

        if (toUOM.equalsIgnoreCase("METRIC")) {
            if (_units().containsKey(getUOM(name).toUpperCase())) {
                toUOM = ((Map<String,String>)_units().get(fromUOM.toUpperCase())).get("Metric");
            }
        }
        else
        if (toUOM.equalsIgnoreCase("IMPERIAL")) {
            if (_units().containsKey(getUOM(name).toUpperCase())) {
                toUOM = ((Map<String,String>)_units().get(fromUOM.toUpperCase())).get("Imperial");
            }
        }
        
        if (toUOM.toUpperCase().equals(fromUOM.toUpperCase()))
            return d;
        
        if (fromUOM.toUpperCase().equals("C") && toUOM.toUpperCase().equals("F")) {
            d.setValue( (getDouble(name) * 9) / 5 + 32 );
            d.setUOM(estimated + toUOM);
        }
        else
        if (getUOM(name).toUpperCase().equals("F") && toUOM.toUpperCase().equals("C")) {
            d.setValue( ((getDouble(name) - 32) * 5) / 9 );
            d.setUOM(estimated + toUOM);
        }
        else {

            Map<String,Double> from;
            
            if (m_localUnits != null) {
                from = m_localUnits.get(fromUOM.toUpperCase());
                if (from != null) {
                    Double factor = from.get(toUOM.toUpperCase());
                    if (factor != null) {
                        d.setValue( getDouble(name) * factor );
                        d.setUOM(estimated + toUOM);
                        return d;
                    }
                }
            }
            
            Map<String, Object> unit = (Map<String, Object>) _units().get(fromUOM.toUpperCase());
            
            if (unit != null) {
                from = (Map<String, Double>) unit.get("Conversions");
                if (from != null) {
                    Double factor = from.get(toUOM.toUpperCase());
                    if (factor != null) {
                        d.setValue( getDouble(name) * factor );
                        d.setUOM(estimated + toUOM);
                        return d;
                    }
                }
            }

            //try the reverse
            if (m_localUnits != null) {
                from = m_localUnits.get(toUOM.toUpperCase());
                if (from != null) {
                    Double factor = from.get(fromUOM.toUpperCase());
                    if (factor != null) {
                        d.setValue( getDouble(name) / factor);
                        d.setUOM(estimated + toUOM);
                        return d;
                    }
                }
            }
            
            unit = (Map<String, Object>) _units().get(toUOM.toUpperCase());
            if (unit != null) {
                from = (Map<String, Double>) unit.get("Conversions");
                if (from != null) {
                    Double factor = from.get(fromUOM.toUpperCase());
                    if (factor != null) {
                        d.setValue( getDouble(name) / factor);
                        d.setUOM(estimated + toUOM);
                        return d;
                    }
                }
            }
            
            //See if the UOM is a combo and split it if it is
            String fromuom[] = fromUOM.split("/");
            String touom[]   = toUOM.split("/");

            if (fromuom.length > 1 && touom.length > 1) {
                Data from1to1 = new Data(d.getName(), d.getValue(),estimated + fromuom[0]).convertUOM(estimated + touom[0]);
                Data v        = new Data(from1to1.getName(), from1to1.getValue(), estimated + touom[1]);
                Data from2to2 = v.convertUOM(estimated + fromuom[1]);
                d.setValue(from2to2.getValue());
                d.setUOM(from1to1.getUOM()+"/"+v.getUOM());
            }
        }
        return d;
    }
    public Data convertUOM(String UOM)    { return convertUOM(m_defaultname,UOM); }
    /**
     * Compares this instance to another Data instance. 
     * If this instance contains multiple named values, then each one is compared in a non-predictable order.
     * Only the values are compared based on the Type.
     * If Type is UNKNOWN, then only equals() is used to return 0 if equal, 1 if not.
     * An attempt is made to convert them to the same unit of measure to do the comparison.
     * @param data The Data instance to compare to.
     * @param name (Optional) The specific named value to compare.
     * @return -1 if this instance is less than, +1 if this instance is greater than, and 0 if they are equal.
     */
    @SuppressWarnings("unchecked")
    public int compare(Data data, String name) {
        Data d = data.convertUOM(getUOM(name));  //convert them to the same UOM if possible
        int i = 0;
        if (data.m_data.get(name) == null) return 1;
        switch (getType(name)) {
        case BOOLEAN: if (!getBoolean(name) && d.getBoolean()) i = -1;
                      else
                      if (getBoolean(name) == d.getBoolean()) i = 0;
                      else i = 1;
                      break;
        case STRING:  i = getString(name).compareTo(d.getString(name)); break;
        case DOUBLE:  i = getDouble(name)  < d.getDouble(name)  ? -1 : (getDouble(name)  > d.getDouble(name)  ? 1 : 0); break;
        case INTEGER: i = getInteger(name) < d.getInteger(name) ? -1 : (getInteger(name) > d.getInteger(name) ? 1 : 0); break;
        case LONG:    i = getLong(name)    < d.getLong(name)    ? -1 : (getLong(name)    > d.getLong(name)    ? 1 : 0); break;
        case FLOAT:   i = getFloat(name)   < d.getFloat(name)   ? -1 : (getFloat(name)   > d.getFloat(name)   ? 1 : 0); break;
        case ARRAY:   i = (((ArrayList<Object>)d.getValue(name)).size() - ((ArrayList<Object>)getValue(name)).size());
                          
                      for (int idx=0; i == 0 && idx < ((ArrayList<Object>)getValue(name)).size(); idx++) {
                          Data d1 = new Data("",((ArrayList<Object>)getValue(name)).get(idx));
                          Data d2 = new Data("",((ArrayList<Object>)d.getValue(name)).get(idx));
                          i = d1.compare(d2);
                      }
                      
                      break;
        default:      i = getValue().equals(d.getValue()) ? 0 : 1; break;
        }
        //if they are not equal, return the result, even if the units of measures are not the same
        if (i != 0) return i;
        //Otherwise, how do we best compare 2 different units of measures? which is smaller, 1 mile or 1 gallon.
        i = getUOM(name).compareTo(data.getUOM(name)); //compare to the original UOM to see if it is different. Else 0L and 0GAL will be equal.
        if (i != 0) return i;
        i = getState(name).compareTo(d.getState(name));
        if (i != 0) return i;
        i = Double.compare(getStatePercent(name),d.getStatePercent(name));
        if (i != 0) return i;
//            if (isDirty() && !data.isDirty())
//                return -1;
//            if (!isDirty() && data.isDirty())
//                return 1;
        //TODO: Test other changes, like format, lang? Or do I expect these to stay constant.
        return i;
    }

    public int compare(Data data) {
        int i = 0;
        for (String name : m_data.keySet()) {
            if ((i = compare(data,name)) != 0)
                return i;
        }
        return i;
    }
    
    //Now override equals() for all the data types and perform the appropriate comparison.
    public boolean equals(Data pData) {
        return compare(pData) == 0 ? true : false;
    }
    public boolean equals(String name, double d) {
        return ((Double)getDouble(name)).equals(d);
    }
    public boolean equals(double d) {
        return equals(m_defaultname,d);
    }
    public boolean equals(String name, boolean b) {
        return ((Boolean)getBoolean(name)).equals(b);
    }
    public boolean equals(boolean b) {
        return equals(m_defaultname,b);
    }
    public boolean equals(String name, float f) {
        return ((Float)getFloat(name)).equals(f);
    }
    public boolean equals(float f) {
        return equals(m_defaultname,f);
    }
    public boolean equals(String name, int i) {
        return ((Integer)getInteger(name)).equals(i);
    }
    public boolean equals(int i) {
        return equals(m_defaultname,i);
    }
    public boolean equals(String name, long l) {
        return ((Long)getLong(name)).equals(l);
    }
    public boolean equals(long l) {
        return equals(m_defaultname,l);
    }
    public boolean equals(String name, String s) {
        return getString(name).equals(s);
    }
    public boolean equals(String s) {
        return equals(m_defaultname,s);
    }
    public boolean equalsIgnoreCase(String name, String s) {
        return getString(name).equals(s);
    }
    public boolean equalsIgnoreCase(String s) {
        return equalsIgnoreCase(m_defaultname,s);
    }

    /**
     * Returns the contents of this instances' named value as a JSON formatted string.
     * The format will be as follows:
     * <pre>
     * {
     *     "Name": "{Name}",
     *     "Type": "{Type}",
     *     "Value": "{Value}",
     *     "Format": "{Format}",
     *     "UOM": "{UOM}",
     *     "UOMAbbr": "{UOMAbbr}",
     *     "UOMDescr": "{UOMDescr}",
     *     "Lang": "{Lang}",
     *     "State": "{State}",
     *     "StatePercent": "{StatePercent}",
     *     "ValueFormatted": "{ValueFormatted}"
     * }
     * </pre>
     * @param name The name of the value.
     * @return     A JSON formatted String.
     */
    public String toString(String name) {

        StringBuffer s = new StringBuffer("");
        
        if (getType(name) != null) {
        
            s.append("{");
            
            m_data.get(name).ValueFormatted = getValueFormatted(name); //format the value and save it back into our object
    
            s.append("\"Name\": \""); s.append(name); s.append("\"");
            s.append(",\"Type\": \""); s.append(getType(name).toString()); s.append("\"");
    
            s.append(", \"Value\": ");
    
            switch (getType(name)) {
            case STRING:    
                            String d = getString(name);
                            if (d.replace("\n","").matches("[{].*[:].*[}]")) {    //if string is a JSON object, leave it be
                                s.append(d); //.replace("\"","\\\""));
                            }
                            else {
                                s.append("\""); s.append(d.replace("\\", "\\\\").replace("\"","\\\"")); s.append("\"");
                            }
                            break;
            case ARRAY:
                            String a = genson.serialize(getValue(name));
                            s.append(a);
                            break;
            case UNKNOWN:   
                            String u = genson.serialize(getValue(name));
                            s.append(u);
                            break;
            default:        
                            String b = getString(name);
                            if (b.equals("NaN")) {
                                s.append("\""); s.append(b); s.append("\"");
                            }
                            else
                                s.append(b);
            }
    
            s.append(",\"Format\": \""); s.append(getFormat(name).replace("\\", "\\\\").replace("\"","\\\"")); s.append("\"");
            s.append(",\"UOM\": \""); s.append(getUOM(name).replace("\\", "\\\\").replace("\"","\\\"")); s.append("\"");
            s.append(",\"UOMAbbr\": \""); s.append(getUOMAbbr(name).replace("\\", "\\\\").replace("\"","\\\"")); s.append("\"");
            s.append(",\"UOMDesc\": \""); s.append(getUOMDesc(name).replace("\\", "\\\\").replace("\"","\\\"")); s.append("\"");
            s.append(",\"Lang\": \""); s.append(getLang(name).replace("\\", "\\\\").replace("\"","\\\"")); s.append("\"");
            s.append(",\"State\": \""); s.append(getState(name).replace("\\", "\\\\").replace("\"","\\\"")); s.append("\"");
            s.append(",\"StatePercent\": \""); s.append(Double.toString(getStatePercent(name))); s.append("\"");
            s.append(",\"ValueFormatted\": ");
    
            if (m_data.get(name).ValueFormatted.matches("[{].*[:].*[}]")) {    //if string is a JSON object, leave it be
                s.append(m_data.get(name).ValueFormatted); //.replace("\"","\\\""));
            }
            else {
                s.append("\""); s.append(m_data.get(name).ValueFormatted.replace("\\", "\\\\").replace("\"","\\\"")); s.append("\"");
            }
            s.append("}");
        }    
        return s.toString();
    }

    /**
     * Returns the contents of all the named values of this instance as a JSON formatted string.
     * Each name will be used as the key to the attributes for that name.
     * The format will be as follows:
     * <pre>
     * {
     *     "__Id": "{m_id}",
     *     "__DefaultName": "{m_defaultname}",
     *     "{Name1}": {
     *         "Name": "{Name1.Name}",
     *         "Type": "{Name1.Type}",
     *         "Value": "{Name1.Value}",
     *         "Format": "{Name1.Format}",
     *         "UOM": "{Name1.UOM}",
     *         "UOMAbbr": "{Name1.UOMAbbr}",
     *         "UOMDescr": "{Name1.UOMDescr}",
     *         "Lang": "{Name1.Lang}",
     *         "State": "{Name1.State}",
     *         "StatePercent": "{Name1.StatePercent}",
     *         "ValueFormatted": "{Name1.ValueFormatted}"
     *     }
     *     , "{NameX}": {
     *          "Name": "{NameX.Name}",
     *          ...
     *     }
     * }
     * </pre>
     * @return     A JSON formatted String.
     */
    public String toString() {
        //if (m_data.keySet().size() <= 1 && m_id.equals(m_defaultname)) return toString(m_defaultname);

        StringBuffer s = new StringBuffer("{");
        //add in the Id and the DefaltName. Prepend with 2 underscores to reduce the possibility of a conflict with a real name
        s.append("  \"__Id\": \"" + m_id + "\"");
        s.append(", \"__DefaultName\": \"" + m_defaultname + "\"");
        for (String name : m_data.keySet()) {
            String s2 = toString(name);
            if (s2.length() > 0)
                s.append(", \"" + name + "\":" + s2);
        }
        s.append("}");
        
        return s.toString();
    }

    private static Map<String,Object> m_json = null;
    private static Map<String,Object> m_units = new HashMap<String,Object>();
    @SuppressWarnings("unchecked")
    /*
     * Returns a Map to the Units of Measure
     * Loads the Unit of Measures from the Data.json file. 
     * The format of that file must be as follows.
     * <pre>
     * {
     *  "Units": {
     *      "{UNIT}": {      
     *          "Abbr":     "{unit}",         
     *          "Singular": "{A Unit}",                  
     *          "Plural":   "{Units}", 
     *          "Conversions": {
     *              "Unit2": 2.0,
     *              "Unit...": 3.0
     *          } 
     *      }
     *   }
     *  }
     *</pre>
     */
    private static Map<String, Object> _units() {
        
        if (m_json == null) {
            String filepath = "com/SIMRacingApps/Data.json";
            
            try {
                //InputStream is = this.getClass().getClassLoader().getResourceAsStream(filepath);
                InputStream is = new FindFile(filepath).getInputStream();
                if (is == null) {
                    Server.logger().warning(String.format("_loadUnits(%s) cannot open",filepath));
                    return m_units;
                }
                InputStreamReader in = new InputStreamReader( is );
                try {
                    m_json = genson.deserialize(in, Map.class);
                    in.close();
                } catch (IOException e) {
                    try {
                        in.close();
                    } catch (IOException e1) {
                    }
                    Server.logStackTrace(Level.SEVERE,"IOException",e);
                    m_json = new HashMap<String,Object>();
                } catch (JsonStreamException e) {
                    try {
                        in.close();
                    } catch (IOException e1) {
                    }
                    Server.logStackTrace(Level.SEVERE,"JsonStreamException",e);
                    m_json = new HashMap<String,Object>();
                }
                
                if (!m_json.containsKey("Units"))
                    m_json.put("Units",new HashMap<String,Object>());
                
                m_units = (Map<String, Object>) m_json.get("Units");
            }
            catch (FileNotFoundException e) {
                Server.logger().warning(String.format("_loadUnits(%s) cannot open",filepath));
                return m_units;
            }
        }
        return m_units;
    }
}
