package com.SIMRacingApps.JComponents;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;

public class JScaledLayout2 implements LayoutManager2 {

	LayoutManager2 m_manager = null;
	
	public JScaledLayout2(LayoutManager2 manager) {
		m_manager = manager;
	}

	@Override
	public void addLayoutComponent(String arg0, Component arg1) {
		if (m_manager != null) m_manager.addLayoutComponent(arg0, arg1);
	}

	@Override
	public void layoutContainer(Container arg0) {
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

	@Override
	public void addLayoutComponent(Component arg0, Object arg1) {
		if (m_manager != null) m_manager.addLayoutComponent(arg0, arg1);
	}

	@Override
	public float getLayoutAlignmentX(Container arg0) {
		return m_manager == null ? 0 : m_manager.getLayoutAlignmentX(arg0);
	}

	@Override
	public float getLayoutAlignmentY(Container arg0) {
		return m_manager == null ? 0 : m_manager.getLayoutAlignmentY(arg0);
	}

	@Override
	public void invalidateLayout(Container arg0) {
		if (m_manager != null) m_manager.invalidateLayout(arg0);
	}

	@Override
	public Dimension maximumLayoutSize(Container arg0) {
		return m_manager == null ? null : m_manager.maximumLayoutSize(arg0);
	}
}
