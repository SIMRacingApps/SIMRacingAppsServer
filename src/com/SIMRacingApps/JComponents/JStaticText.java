package com.SIMRacingApps.JComponents;

import javax.swing.Icon;
import javax.swing.JLabel;

import java.awt.Font;
import java.awt.Color;

public class JStaticText extends JLabel {
	
	private static final long serialVersionUID = -8062607308220952896L;
	public static final int FONTSIZE = 14;
	public static final String FONTNAME = "Arial";
	public static final int FONTWEIGHT = Font.PLAIN;
	public static final Color FOREGROUND = new Color(255,215,0);  //Gold
	
	public JStaticText() {
		super();
		setForeground(FOREGROUND);
		setFont(new Font(FONTNAME, FONTWEIGHT, FONTSIZE));
	}

	public JStaticText(String text) {
		super(text);
		setForeground(FOREGROUND);
		setFont(new Font(FONTNAME, FONTWEIGHT, FONTSIZE));
	}

	public JStaticText(Icon arg0) {
		super(arg0);
		setForeground(FOREGROUND);
		setFont(new Font(FONTNAME, FONTWEIGHT, FONTSIZE));
	}

	public JStaticText(String arg0, int arg1) {
		super(arg0, arg1);
		setForeground(FOREGROUND);
		setFont(new Font(FONTNAME, FONTWEIGHT, FONTSIZE));
	}

	public JStaticText(Icon arg0, int arg1) {
		super(arg0, arg1);
		setForeground(FOREGROUND);
		setFont(new Font(FONTNAME, FONTWEIGHT, FONTSIZE));
	}

	public JStaticText(String arg0, Icon arg1, int arg2) {
		super(arg0, arg1, arg2);
		setForeground(FOREGROUND);
		setFont(new Font(FONTNAME, FONTWEIGHT, FONTSIZE));
	}

}
