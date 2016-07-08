package com.SIMRacingApps.JComponents;

//import java.awt.Dimension;

import javax.swing.JPanel;

public class JWidgetPanel extends JPanel {

	private static final long serialVersionUID = -3901392787891222313L;

	/**
	 * Create the panel.
	 */
	public JWidgetPanel() {
		setOpaque(false);
	}
	
	private Double m_xscalefactor = 1.0;	//This will get calculated by the JScaledLayout manager. It must wrap the original layout manager on every JWidetPanel
	private Double m_yscalefactor = 1.0;

	public Double _getXScaleFactor() {
		return m_xscalefactor;
	}
	public void _setXScaleFactor(Double m_xscalefactor) {
		this.m_xscalefactor = m_xscalefactor;
	}
	public Double _getYScaleFactor() {
		return m_yscalefactor;
	}
	public void _setYScaleFactor(Double m_yscalefactor) {
		this.m_yscalefactor = m_yscalefactor;
	}
	
	private Integer m_fontsize = null;

	public Integer _getFontSize() {
		return m_fontsize;
	}
	public void _setFontSize(Integer m_fontsize) {
		if (m_fontsize == null) this.m_fontsize = m_fontsize;
	}

//	Dimension m_designsize = null;
//	
//	public Dimension getDesignSize() {
//		return m_designsize;
//	}
//
//	public void setDesignSize(Dimension m_designsize) {
//		this.m_designsize = m_designsize;
//	}
//
//	private Double getXScalingFactor(Double factor) {
//		Double d = factor;
//		if (d == null) 
//			d = 1.0;
//		if (m_designsize != null)
//			d = this.getSize().getWidth() / m_designsize.getWidth();
//		if (this.getParent() != null && this.getParent() instanceof JWidgetPanel)
//			d *= getXScalingFactor(d);
//		return d;
//	}
//
//	private Double getYScalingFactor(Double factor) {
//		Double d = factor;
//		if (d == null) 
//			d = 1.0;
//		if (m_designsize != null)
//			d = this.getSize().getHeight() / m_designsize.getHeight();
//		if (this.getParent() != null && this.getParent() instanceof JWidgetPanel)
//			d *= getYScalingFactor(d);
//		return d;
//	}
	
//	public void setBounds(int x, int y, int width, int height) {
//		super.setBounds(x, y, width, height);
//		//now propagate my change in size down to all my children
//		for (int comp = 0; comp < this.getComponentCount(); comp++) {
//			Component c = getComponent(comp);
//			.setBounds(x, y, width, height);
//		}
//	}
}
