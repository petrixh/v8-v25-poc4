package de.symeda.sormas.ui.utils.components;

import com.vaadin.flow.component.html.Span;

import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

/**
 * Was a full-width Vaadin 8 {@code Label}: a full-width {@link Span} whose text wraps.
 */
public class MultilineLabel extends Span {

	public MultilineLabel(String text) {
		this(text, ContentMode.TEXT);
	}

	public MultilineLabel(String text, ContentMode contentMode) {
		VaadinUiUtil.setLabelValue(this, text, contentMode);
		setWidthFull();
	}
}
