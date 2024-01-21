package com.SIMRacingApps.Widgets;

import java.awt.GridLayout;
import javax.swing.SwingConstants;
import com.SIMRacingApps.Data;
import com.SIMRacingApps.JComponents.*;
import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * This Widget displays the FPS value and the current data version.
 * 
 * @author Jeffrey Gilliam
 * @copyright Copyright (C) 2015 - 2024 Jeffrey Gilliam
 * @since 1.0
 * @license Apache License 2.0
 */
public class FPSWidget extends Widget {

	private static final long serialVersionUID = -4848976597322410923L;

	private Color m_foreground = Color.CYAN;
	private JDataLabel dtjlblfps;
	
	/**
	 * Returns the Foreground Color.
	 * @return The Foreground Color.
	 */
	public Color getForeground() {
		return m_foreground;
	}

	/**
	 * Sets the Foreground Color.
	 * @param foreground The foreground color.
	 */
	public void setForeground(Color foreground) {
		this.m_foreground = foreground;
		if (dtjlblfps != null) dtjlblfps.setForeground(m_foreground);
	}

	/**
	 * Constructor to create the FPS Widget.
	 */
	public FPSWidget() {
		setPreferredSize(new Dimension(100, 20));
		setLayout(new JScaledLayout(new GridLayout(1, 1, 0, 0)));
		
		dtjlblfps = new JDataLabel("FPS");
		dtjlblfps.addPropertyChangeListener("data",new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				Data d = (Data)evt.getNewValue();
				//I happen to know that the FPS works by adding the DataVersion. So, let's display it.
				//It's the only data component that does this.
				dtjlblfps.setText(d.getString() + "(" + d.getString("Session/DataVersion") + ")");
			}
		});
		dtjlblfps.setForeground(m_foreground);
		dtjlblfps.setHorizontalTextPosition(SwingConstants.LEADING);
		dtjlblfps.setText("{FPS}");
		add(dtjlblfps);

	}

}
