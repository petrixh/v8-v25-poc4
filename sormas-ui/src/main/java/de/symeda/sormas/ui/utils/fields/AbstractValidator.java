/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2026 SORMAS Foundation gGmbH
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package de.symeda.sormas.ui.utils.fields;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * Flow {@link Validator} with the V7 {@code com.vaadin.v7.data.validator.AbstractValidator} contract: subclasses
 * implement {@link #isValidValue(Object)}, and the error message can be changed later ({@code {0}} is replaced by the
 * value, as in V7). Usable in a {@link com.vaadin.flow.data.binder.Binder} and with {@link Field#addValidator}.
 *
 * @param <T>
 *            the validated (model) value type
 */
public abstract class AbstractValidator<T> implements Validator<T> {

	private static final long serialVersionUID = 1L;

	private String errorMessage;

	protected AbstractValidator(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	protected abstract boolean isValidValue(T value);

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	/**
	 * V7 {@code getType()}. Not used on Flow; subclasses may override it.
	 */
	public Class<T> getType() {
		return null;
	}

	public boolean isValid(T value) {
		return isValidValue(value);
	}

	/**
	 * V7 {@code validate(value)}.
	 *
	 * @throws Field.InvalidValueException
	 *             if the value is not valid
	 */
	public void validate(T value) {
		if (!isValidValue(value)) {
			throw new Field.InvalidValueException(buildErrorMessage(value));
		}
	}

	protected String buildErrorMessage(T value) {
		String message = getErrorMessage();
		return message != null ? message.replace("{0}", String.valueOf(value)) : "";
	}

	@Override
	public ValidationResult apply(T value, ValueContext context) {
		return isValidValue(value) ? ValidationResult.ok() : ValidationResult.error(buildErrorMessage(value));
	}
}
