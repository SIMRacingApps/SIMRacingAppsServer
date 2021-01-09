package com.SIMRacingApps.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * The State class track any state oriented content.
 * It time stamps each state change with a time you give it, so that you can get the amount of time you were in a particular state.
 * It also keeps track of the previous state, so even after changing the state, you can see the state you were in before the change. 
 * @author Jeffrey Gilliam
 * @since 1.0
 * @copyright Copyright (C) 2015 - 2021 Jeffrey Gilliam
 * @license Apache License 2.0
 */
public class State {

    private String m_state;
    private Map<String,Double> m_stateStartingTime;
    private String m_prevState;
    private Map<String,Double> m_prevStartingTime;
    private Map<String,Double> m_prevTime;

    @SuppressWarnings("unused")
    private State() {}

    /**
     * Constructor for creating an initial state and time.
     * 
     * @param state The initial state.
     * @param time The time to track this state against.
     */
    public State(String state, double time) {
        m_state                 = new String(state);
        m_stateStartingTime     = new HashMap<String,Double>();
        m_prevState             = "";
        m_prevStartingTime      = new HashMap<String,Double>();
        m_prevTime              = new HashMap<String,Double>();

        m_prevTime.put(m_prevState, 0.0);
        m_prevStartingTime.put(m_prevState, 0.0);
        m_stateStartingTime.put(state, time);
    }

    /**
     * Copy constructor for creating a duplicate a state
     * 
     * @param state The state to duplicate
     */
    public State(State state) {
        m_state             = state.m_state;
        m_stateStartingTime = state.m_stateStartingTime;
        m_prevState         = state.m_prevState;
        m_prevStartingTime  = new HashMap<String,Double>(state.m_prevStartingTime);
        m_prevTime          = new HashMap<String,Double>(state.m_prevTime);
    }
    
    /**
     * Returns the current state.
     * @return The current state.
     */
    public String getState() {
        return new String(m_state);
    }

    /**
     * Returns the amount of time you have been in the current state.
     * @param current The current time.
     * @return The current time minus the starting time of the state.
     */
    public double getTime(double current) {
        return current - m_stateStartingTime.get(m_state);
    }

    /**
     * Returns the starting time of the current state.
     * @return The starting time of the current state.
     */
    public double getStartingTime() {
        if (m_stateStartingTime.containsKey(m_state))
            return m_stateStartingTime.get(m_state);
        return 0.0;
    }

    /**
     * Returns the starting time of the specified state.
     * @param state The state to lookup.
     * @return The starting time, 0.0 if state is not found.
     */
    public double getStartingTime(String state) {
        if (m_stateStartingTime.containsKey(state))
            return m_stateStartingTime.get(state);
        return 0.0;
    }

    /**
     * Returns the amount of time you were in the specified state. 
     * @param state The state to lookup.
     * @param current The current time.
     * @return The amount of time.
     */
    public double getTime(String state, double current) {
    	//if it's the current state calculate the time so far, else return the time of the previous state
    	if (state.equals(m_state)) {
    		if (m_stateStartingTime.containsKey(state))
        		return current - m_stateStartingTime.get(state);
    	}
        else {
        	if (m_prevTime.containsKey(state))
        		return m_prevTime.get(state);
        }
        return 0.0;
    }

    /**
     * Changes to the specified state.
     * <p>
     * NOTE: If called subsequently with the same state, the starting time is only recorded the first time.
     * @param state The state to change to.
     * @param time The time to record as the starting time you changed to this state
     */
    public void setState(String state, double time) {
        if (!this.equals(state)) {
            m_prevState = m_state;
            m_prevStartingTime.put(m_prevState, m_stateStartingTime.get(m_state));
            m_prevTime.put(m_prevState, time - m_stateStartingTime.get(m_state));

            m_state = new String(state);
            m_stateStartingTime.put(m_state, time);
        }
    }
    public void setState(int state, double time) {
        setState(Integer.toString(state), time);
    }
    public void setState(long state, double time) {
        setState(Long.toString(state), time);
    }
    public void setState(boolean state, double time) {
        setState(Boolean.toString(state), time);
    }

    /**
     * Changes to the specified state using the starting time of the last time the state was started.
     * @param state The state to change to.
     */
    public void setState(State state) {
    	this.setState(state.m_state,state.m_stateStartingTime.get(state.m_state));
    }

    /**
     * Returns true if the current state equals the specified state.
     * @param state The state to compare to.
     * @return true if equal
     */
    public boolean equals(String state) {
        return m_state.equals(state);
    }

    /**
     * Returns true if the current state equals the specified state object.
     * @param state The state to compare to.
     * @return true if equal
     */
    public boolean equals(State state) {
        return m_state.equals(state.getState());
    }
    public boolean equals(int state) {
        return equals(Integer.toString(state));
    }
    public boolean equals(long state) {
        return equals(Long.toString(state));
    }
    public boolean equals(boolean state) {
        return equals(Boolean.toString(state));
    }

    /**
     * Returns the previous state.
     * @return The pervious state.
     */
    public String getPrevState() {
        return new String(m_prevState);
    }

    /**
     * Returns the starting time of the specified state.
     * @param state The state to lookup.
     * @return The starting time.
     */
    public double getPrevStartingTime(String state) {
        if (m_prevStartingTime.containsKey(state))
            return m_prevStartingTime.get(state);
        return 0.0;
    }
    
    /**
     * Returns the starting time of the current state.
     * @return The starting time.
     */
    public double getPrevStartingTime() {
        return m_prevStartingTime.get(m_state);
    }


    /**
     * Returns the amount of time you were in the previous state.
     * @return The amount of time.
     */
    public double getPrevTime() {
        return m_prevTime.get(m_prevState);
    }

    /**
     * Returns true if the previous state equals the specified state.
     * @param state The state to compare to.
     * @return true if equal
     */
    public boolean equalsPrevious(String state) {
        return m_prevState.equals(state);
    }

    /**
     * Returns true if the previous state equals the specified state object.
     * @param state The state to compare to.
     * @return true if equal
     */
    public boolean equalsPrevious(State state) {
        return m_prevState.equals(state.getState());
    }

    /**
     * Returns a printable string of the current and previous states.
     */
    @Override
    public String toString() {
    	return String.format("%s,%f,%s,%f",m_state,m_stateStartingTime.get(m_state),m_prevState,m_prevTime.get(m_prevState));
    }
}
