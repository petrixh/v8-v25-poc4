package de.symeda.sormas.ui.utils.components.linelisting.line;

import com.vaadin.flow.component.ComponentEventListener;

@FunctionalInterface
public interface DeleteLineListener extends ComponentEventListener<DeleteLineEvent> {

	void deleteLine(DeleteLineEvent event);

	@Override
	default void onComponentEvent(DeleteLineEvent event) {
		deleteLine(event);
	}
}
