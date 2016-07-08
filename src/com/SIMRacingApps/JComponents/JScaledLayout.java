package com.SIMRacingApps.JComponents;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

public class JScaledLayout implements LayoutManager {

	LayoutManager m_manager = null;
	
	public JScaledLayout(LayoutManager manager) {
		m_manager = manager;
	}

	@Override
	public void addLayoutComponent(String arg0, Component arg1) {
		if (m_manager != null) m_manager.addLayoutComponent(arg0, arg1);
	}

	@Override
	public void layoutContainer(Container arg0) {
		if (arg0 instanceof JWidgetPanel && arg0.isPreferredSizeSet()) {
			((JWidgetPanel)arg0)._setXScaleFactor((double)arg0.getWidth()/arg0.getPreferredSize().width);
			((JWidgetPanel)arg0)._setYScaleFactor((double)arg0.getHeight()/arg0.getPreferredSize().height);
			((JWidgetPanel)arg0)._setFontSize(arg0.getFont().getSize());
			
			//OK, I assume the layouts are called from the inside out.
			//to calculate the total amount of scaling, I need to wait for all my parents to get laid out first
			//once I know the total scaling, then I need to call all children to scale their font size 
			//and in turn tell their children, etc, etc, etc.
			//maybe this can be done on the paint routine? I assume they will be painted in the right order that they're layered.
			//according to the original font size multiplied by the total scaling factor from the parent to that object. Stopping if it's not visible.
			//all objects must be derived from JWidgetPanel to be able to track this
			//if all this pans out, then if instanceof JLabel, change the font size or the icon size and repaint.
		}
		if (m_manager != null) m_manager.layoutContainer(arg0);
	}

	@Override
	public Dimension minimumLayoutSize(Container arg0) {
		return m_manager == null ? null : m_manager.minimumLayoutSize(arg0);
	}

	@Override
	public Dimension preferredLayoutSize(Container arg0) {
		return m_manager == null ? null : m_manager.preferredLayoutSize(arg0);
	}

	@Override
	public void removeLayoutComponent(Component arg0) {
		if (m_manager != null) m_manager.removeLayoutComponent(arg0);
	}
}
