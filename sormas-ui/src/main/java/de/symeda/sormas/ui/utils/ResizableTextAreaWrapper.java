/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.ui.utils;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.utils.fields.AbstractTextField;
import de.symeda.sormas.ui.utils.fields.TextArea;

public class ResizableTextAreaWrapper<T extends AbstractTextField> implements FieldWrapper<T> {

	// XXX: make this configurable
	private static final int MAX_ROWS = 30;
	private static final int MIN_ROWS = 4;

	private final boolean withMaxLength;
	private T textField;
	private String caption;
	private Span labelField;

	public ResizableTextAreaWrapper() {
		this(true);
	}

	public ResizableTextAreaWrapper(boolean withMaxLength) {
		this.withMaxLength = withMaxLength;
	}

	@Override
	public Component wrap(T textField, String caption) {
		return wrap(textField, caption, true);
	}

	@Override
	public Component wrap(T textField, String caption, boolean withMargin) {

		this.textField = textField;
		this.caption = caption;

		VerticalLayout layout = new VerticalLayout();
		layout.setSpacing(false);
		layout.setPadding(false);
		layout.setWidthFull();
		if (withMargin) {
			layout.addClassName(CssStyles.FIELD_WRAPPER);
		}

		textField.setWidthFull();
		textField.addStyleName(CssStyles.RESIZABLE);
		textField.setNullRepresentation("");
		// was setTextChangeTimeout(200) with a text change listener: the value follows typing after 200 ms
		if (textField instanceof HasValueChangeMode) {
			((HasValueChangeMode) textField).setValueChangeMode(ValueChangeMode.TIMEOUT);
			((HasValueChangeMode) textField).setValueChangeTimeout(200);
		}

		// Collect validators first to avoid ConcurrentModificationException
		List<Validator<? super String>> maxLengthValidators =
			textField.getValidators().stream().filter(v -> v instanceof MaxLengthValidator).collect(Collectors.toList());

		if (withMaxLength) {
			maxLengthValidators.stream().findFirst().map(v -> ((MaxLengthValidator) v).getMaxLength()).ifPresent(textField::setMaxLength);

			labelField = new Span(buildLabelMessage(textField.getValue(), textField, caption));
			labelField.setId(textField.getId().orElse(null) + "_label");
			labelField.setWidthFull();
			labelField.addClassNames(CssStyles.ALIGN_RIGHT, CssStyles.FIELD_EXTRA_INFO, CssStyles.LABEL_ITALIC);
		} else {
			maxLengthValidators.forEach(textField::removeValidator);
		}

		textField.addValueChangeListener(e -> {
			updateTextfieldAppearance();
		});

		layout.add(textField.asComponent());
		if (withMaxLength) {
			layout.add(labelField);
		}
		return layout;
	}

	private void updateTextfieldAppearance() {
		if (withMaxLength) {
			// XXX: notify user if text is not valid (e.g. too long)
			labelField.setText(buildLabelMessage(textField.getValue(), textField, caption));
		}
		adjustRows(textField);
	}

	private String buildLabelMessage(String text, T textField, String caption) {
		return String.format(I18nProperties.getCaption(caption), Strings.nullToEmpty(text).length(), getMaxLength(textField));
	}

	private static int getMaxLength(AbstractTextField textField) {
		if (textField instanceof com.vaadin.flow.component.textfield.TextArea) {
			return ((com.vaadin.flow.component.textfield.TextArea) textField).getMaxLength();
		} else if (textField instanceof com.vaadin.flow.component.textfield.TextField) {
			return ((com.vaadin.flow.component.textfield.TextField) textField).getMaxLength();
		}
		return -1;
	}

	/**
	 * Set number of rows in textareas to the number of lines of text + 1.
	 * Min: {@link #MIN_ROWS}, Max: {@link #MAX_ROWS}
	 */
	private void adjustRows(T textField) {
		if (textField instanceof TextArea) {
			((TextArea) textField)
				.setRows(Math.min(MAX_ROWS, Math.max(CharMatcher.is('\n').countIn(Strings.nullToEmpty(textField.getValue())) + 1, MIN_ROWS)));
		}
	}
}
