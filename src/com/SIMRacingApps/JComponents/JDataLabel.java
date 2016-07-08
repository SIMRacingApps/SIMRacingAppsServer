package com.SIMRacingApps.JComponents;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;

import javax.swing.Icon;
import javax.swing.JLabel;

import com.SIMRacingApps.SIMPluginAWTEventDispatcher;
import com.SIMRacingApps.Data;
import java.awt.Font;
import java.awt.Color;

import javax.swing.SwingConstants;

public class JDataLabel extends JLabel {
	
	private static final long serialVersionUID = -2976572227367944271L;
	public static final int FONTSIZE = 16;
	public static final String FONTNAME = "Courier New";
	public static final int FONTWEIGHT = Font.PLAIN;
	public static final Color FOREGROUND = Color.WHITE;
	
	private PropertyChangeListener m_listener = null;
	
	private String m_name = null;
	public String getName() { return m_name; }
	public void setName(String name) {
		if (m_listener != null) {
		    SIMPluginAWTEventDispatcher.getDispatcher().removePropertyChangeListener(m_name,m_listener);
		}
		m_name = name;
		SIMPluginAWTEventDispatcher.getDispatcher().addPropertyChangeListener(m_name,m_listener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				//if we have any listeners for "data", then let that listener update the text
				boolean founddatalistener = false;
				PropertyChangeListener[] listeners = getPropertyChangeListeners();
				for (int i = 0; i < listeners.length; i++) {
					if (listeners[i] instanceof PropertyChangeListenerProxy) {
						PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy)listeners[i];
		        	 	if (proxy.getPropertyName().equals("data")) { 
		        	 		founddatalistener = true;
		        	 		break;
		        	 	}
					}
				}
				
				Data d = (Data)evt.getNewValue();
				setData(d);
				
				if (!founddatalistener) {
					setText(d.getStringFormatted(m_format));
				}
			}
		});
	}
	
	private Data m_data = null;
	public Data getData() { return m_data; }
	public void setData(Data data) {
		Data olddata = m_data;
		m_data = data;
		firePropertyChange("data", olddata, data);
	}
	
	private String m_format = "%s";
	public String getFormat() { return m_format; }
	public void setFormat(String format) { m_format = format; } 
	
	public JDataLabel(String dataname) {
		super();
		setForeground(FOREGROUND);
		setFont(new Font(FONTNAME, FONTWEIGHT, FONTSIZE));
		setHorizontalAlignment(SwingConstants.LEADING);
		setName(dataname);
		if (dataname != null && !dataname.equals("")) setText("{"+dataname+"}");	//just a visual place holder for a GUI builder.
	}

	public JDataLabel(String dataname, String defaultvalue) {
		super(defaultvalue);
		setForeground(FOREGROUND);
		setFont(new Font(FONTNAME, FONTWEIGHT, FONTSIZE));
		setHorizontalAlignment(SwingConstants.LEADING);
		setName(dataname);
	}

	public JDataLabel(String dataname, Icon arg0) {
		super(arg0);
		setForeground(FOREGROUND);
		setFont(new Font(FONTNAME, FONTWEIGHT, FONTSIZE));
		setHorizontalAlignment(SwingConstants.LEADING);
		setName(dataname);
	}

	public JDataLabel(String dataname, String arg0, int arg1) {
		super(arg0, arg1);
		setForeground(FOREGROUND);
		setFont(new Font(FONTNAME, FONTWEIGHT, FONTSIZE));
		setHorizontalAlignment(SwingConstants.LEADING);
		setName(dataname);
	}

	public JDataLabel(String dataname, Icon arg0, int arg1) {
		super(arg0, arg1);
		setForeground(FOREGROUND);
		setFont(new Font(FONTNAME, FONTWEIGHT, FONTSIZE));
		setHorizontalAlignment(SwingConstants.LEADING);
		setName(dataname);
	}

	public JDataLabel(String dataname, String arg0, Icon arg1, int arg2) {
		super(arg0, arg1, arg2);
		setForeground(FOREGROUND);
		setFont(new Font(FONTNAME, FONTWEIGHT, FONTSIZE));
		setHorizontalAlignment(SwingConstants.LEADING);
		setName(dataname);
	}

//just some notes on resizing	
//http://java-sl.com/Scale_In_JEditorPane.html
//    public void paint(Graphics g, Shape allocation) {
//        Graphics2D g2d = (Graphics2D) g;
//        double zoomFactor = getZoomFactor();
//        AffineTransform old = g2d.getTransform();
//        g2d.scale(zoomFactor, zoomFactor);
//        super.paint(g2d, allocation);
//        g2d.setTransform(old);
//    }
//g2.translate(zoomX, zoomY);
//g2.scale(zoom, zoom);
//g2.translate(-zoomX, -zoomY);
//Where zoom variable is initialized with the value of 1 for normal size view, and zoomX and zoomY are the points where I want to zoom in center
//
//http://www.roseindia.net/answers/viewqa/Java-Beginners/21740-Java-Swing-code-for-zoom-in-and-out.html
}
