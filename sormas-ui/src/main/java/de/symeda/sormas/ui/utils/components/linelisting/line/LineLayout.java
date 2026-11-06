package de.symeda.sormas.ui.utils.components.linelisting.line;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.shared.Registration;

public class LineLayout extends HorizontalLayout {

	public Registration addDeleteLineListener(DeleteLineListener deleteLineListener) {
		return addListener(DeleteLineEvent.class, deleteLineListener);
	}
}
