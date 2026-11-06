package de.symeda.sormas.ui.utils.components.sidecomponent;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.ui.utils.CssStyles;

public class SideComponentLayout extends VerticalLayout {

	private SideComponent component;
	public SideComponentLayout(SideComponent sideComponent) {
		setPadding(false);
		setSpacing(false);
		sideComponent.addClassName(CssStyles.SIDE_COMPONENT);
		add(sideComponent);
		component = sideComponent;
	}

	public SideComponent getComponent() {
		return component;
	}
}
