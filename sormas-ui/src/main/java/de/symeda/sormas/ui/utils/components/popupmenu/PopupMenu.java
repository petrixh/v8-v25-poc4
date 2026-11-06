package de.symeda.sormas.ui.utils.components.popupmenu;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.PopupButton;

public class PopupMenu extends PopupButton {

	private final VerticalLayout menuLayout;

	public PopupMenu(String caption) {
		super(caption);

		setId("more");
		setIcon(VaadinIcon.ELLIPSIS_DOTS_V.create());

		menuLayout = new VerticalLayout();
		menuLayout.setSpacing(true);
		menuLayout.setPadding(true);
		menuLayout.addClassName(CssStyles.LAYOUT_MINIMAL);
		menuLayout.setWidth("250px");

		setContent(menuLayout);
	}

	public void addMenuEntry(Component menuEntry) {
		menuLayout.add(menuEntry);
	}

	public boolean hasMenuEntries() {
		return menuLayout.getComponentCount() > 0;
	}
}
