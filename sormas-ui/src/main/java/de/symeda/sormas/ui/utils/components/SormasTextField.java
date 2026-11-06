package de.symeda.sormas.ui.utils.components;

import com.vaadin.flow.shared.Registration;

import de.symeda.sormas.ui.utils.fields.Field;
import de.symeda.sormas.ui.utils.fields.TextField;

public class SormasTextField extends TextField {

	private static final long serialVersionUID = 1L;

	/**
	 * Was a V7 {@code Property.ValueChangeListener} registered directly on the text field. Flow fields have one value
	 * change listener type; the event is the Flow event ({@code event.getValue()}).
	 */
	public Registration addTextFieldValueChangeListener(Field.ValueChangeListener<String> listener) {
		return addValueChangeListener(listener);
	}
}
