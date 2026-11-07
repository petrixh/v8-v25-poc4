package de.symeda.sormas.ui.utils.components.expandablebutton;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

public class ExpandableButton extends HorizontalLayout {

	private final Button expandableButton;

	public ExpandableButton(String caption) {
		expandableButton = ButtonHelper.createIconButton(caption, VaadinIcon.PLUS_CIRCLE, null, CssStyles.BUTTON_PRIMARY);
		add(expandableButton);
	}

	public ExpandableButton expand(ComponentEventListener<ClickEvent<Button>> listener) {
		expandableButton.addClickListener(listener);
		return this;
	}
}
