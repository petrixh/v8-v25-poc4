package de.symeda.sormas.ui.utils.components.linelisting.line;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

public class DeleteLineEvent extends ComponentEvent<Component> {

	public DeleteLineEvent(Component source) {
		super(source, false);
	}
}
