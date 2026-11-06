package de.symeda.sormas.ui.utils;

import java.util.Objects;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.validator.AbstractValidator;
import com.vaadin.flow.component.Component;

public class FieldVisibleAndNotEmptyValidator<T> extends AbstractValidator<T> {

	private static final long serialVersionUID = 7701149451271927686L;

	public FieldVisibleAndNotEmptyValidator(String errorMessage) {
		super(errorMessage);
	}

	@Override
	public ValidationResult apply(T value, ValueContext context) {

		Component component = context.getComponent().get();
		if (component.isVisible() && (value == null || Objects.equals(value, context.getHasValue().get().getEmptyValue()))) {
			return ValidationResult.error(getMessage(value));
		} else {
			return ValidationResult.ok();
		}
	}
}
