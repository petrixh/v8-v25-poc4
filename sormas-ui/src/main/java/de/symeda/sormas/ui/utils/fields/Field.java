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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasPlaceholder;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.shared.HasTooltip;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.shared.Registration;

/**
 * The SORMAS field contract on Vaadin Flow. It replaces the V7 {@code com.vaadin.v7.ui.Field}: every field the
 * {@link de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory} creates implements it, so forms keep working with
 * field ids, captions, required flags, validators and converters as before.
 * <p>
 * Implementations are Flow components that already implement {@link HasValue} (a Flow field subclass or a
 * {@link CustomField}); the value methods declared here are implemented by the Flow superclass. Validators and
 * converters are kept on the component ({@link ComponentUtil#setData}) and are applied by the form's
 * {@link com.vaadin.flow.data.binder.Binder} binding (see {@link #asBindingConverter(Class)} and
 * {@link #asBindingValidator()}), so they can be added or replaced after the field was bound, like in V7.
 *
 * @param <T>
 *            the value type of the field (the presentation type, as in V7)
 */
public interface Field<T> extends HasElement, HasEnabled, HasStyle, HasSize {

	String DATA_VALIDATORS = "sormas.validators";
	String DATA_CONVERTER = "sormas.converter";
	String DATA_REQUIRED_ERROR = "sormas.requiredError";
	String DATA_CONVERSION_ERROR = "sormas.conversionError";
	String DATA_VALIDATION_VISIBLE = "sormas.validationVisible";
	String DATA_INVALID_COMMITTED = "sormas.invalidCommitted";
	String DATA_DESCRIPTION = "sormas.description";
	String DATA_NULL_REPRESENTATION = "sormas.nullRepresentation";
	String DATA_NULL_SETTING_ALLOWED = "sormas.nullSettingAllowed";
	String DATA_MODIFIED = "sormas.modified";

	/**
	 * The label of a field whose Vaadin 8 caption was empty (see {@link #setCaption(String)}).
	 */
	String EMPTY_CAPTION_LABEL = "\u200b";

	// Value methods, implemented by the Flow HasValue superclass

	T getValue();

	void setValue(T value);

	T getEmptyValue();

	boolean isEmpty();

	void clear();

	boolean isReadOnly();

	void setReadOnly(boolean readOnly);

	void setRequiredIndicatorVisible(boolean requiredIndicatorVisible);

	boolean isRequiredIndicatorVisible();

	// Component methods, implemented by com.vaadin.flow.component.Component

	void setVisible(boolean visible);

	boolean isVisible();

	Optional<String> getId();

	void setId(String id);

	default Component asComponent() {
		return (Component) this;
	}

	@SuppressWarnings("unchecked")
	default HasValue<?, T> asHasValue() {
		return (HasValue<?, T>) this;
	}

	/**
	 * Value change listener on the field. The event is the Flow event: use {@code event.getValue()} where V7 code used
	 * {@code event.getProperty().getValue()}.
	 */
	@FunctionalInterface
	interface ValueChangeListener<T> extends HasValue.ValueChangeListener<ComponentValueChangeEvent<?, T>> {
	}

	@SuppressWarnings({
		"unchecked",
		"rawtypes" })
	default Registration addValueChangeListener(ValueChangeListener<T> listener) {
		return ((HasValue) this).addValueChangeListener(listener);
	}

	// V7 conveniences

	default String getCaption() {
		String label = this instanceof HasLabel ? ((HasLabel) this).getLabel() : getElement().getProperty("label");
		return EMPTY_CAPTION_LABEL.equals(label) ? "" : label;
	}

	/**
	 * Vaadin 8 rendered an empty caption ({@code ""}, unlike {@code null}) as an empty caption row, so the field lines up
	 * with captioned fields next to it (e.g. the month and day of a birth date under the year's caption). A Flow field
	 * hides an empty (or blank) label: a zero-width space keeps the row.
	 */
	default void setCaption(String caption) {
		String label = "".equals(caption) ? EMPTY_CAPTION_LABEL : caption;
		if (this instanceof HasLabel) {
			((HasLabel) this).setLabel(label);
		} else {
			getElement().setProperty("label", label);
		}
	}

	default String getDescription() {
		return (String) ComponentUtil.getData(asComponent(), DATA_DESCRIPTION);
	}

	/**
	 * V7 description: shown as the field's tooltip.
	 */
	default void setDescription(String description) {
		ComponentUtil.setData(asComponent(), DATA_DESCRIPTION, description);
		if (this instanceof HasTooltip) {
			((HasTooltip) this).setTooltipText(description);
		} else {
			getElement().setAttribute("title", description == null ? "" : description);
		}
	}

	default boolean isRequired() {
		return isRequiredIndicatorVisible();
	}

	/**
	 * V7 required: the indicator is shown and {@link #isValid()} (and the form's binding validator) fail on an empty
	 * value.
	 */
	default void setRequired(boolean required) {
		setRequiredIndicatorVisible(required);
	}

	default String getRequiredError() {
		return (String) ComponentUtil.getData(asComponent(), DATA_REQUIRED_ERROR);
	}

	default void setRequiredError(String requiredError) {
		ComponentUtil.setData(asComponent(), DATA_REQUIRED_ERROR, requiredError);
	}

	default String getConversionError() {
		return (String) ComponentUtil.getData(asComponent(), DATA_CONVERSION_ERROR);
	}

	default void setConversionError(String conversionError) {
		ComponentUtil.setData(asComponent(), DATA_CONVERSION_ERROR, conversionError);
	}

	default String getInputPrompt() {
		return this instanceof HasPlaceholder ? ((HasPlaceholder) this).getPlaceholder() : null;
	}

	default void setInputPrompt(String inputPrompt) {
		if (this instanceof HasPlaceholder) {
			((HasPlaceholder) this).setPlaceholder(inputPrompt);
		}
	}

	/**
	 * V7 {@code Buffered.isModified()} of a field bound in a form's field group
	 * ({@link de.symeda.sormas.ui.utils.AbstractForm.BeanFieldGroup}): the value changed since the group read, committed
	 * or discarded the bean, by the user or programmatically, also if it was changed back to the read value. Always
	 * {@code false} for a field that is not bound (V7: no data source).
	 */
	default boolean isModified() {
		return Boolean.TRUE.equals(ComponentUtil.getData(asComponent(), DATA_MODIFIED));
	}

	/**
	 * No-op: Flow fields always send their value to the server on change.
	 */
	default void setImmediate(boolean immediate) {
		// nothing to do
	}

	/**
	 * V7 null representation: the text a text field ({@link TextField}, {@link TextArea}, {@link PasswordField},
	 * {@link SormasRichTextArea}) shows when its value is {@code null}. Default {@code ""}, which every SORMAS text field
	 * set in V7 (the field factory and all direct uses); the V7 component default {@code "null"} is not copied.
	 * <p>
	 * As in V7, the value of these fields can be {@code null}: an untouched field read from a {@code null} property
	 * stays {@code null}, and so does the property on commit. Text the user enters that equals the null representation
	 * keeps a {@code null} value {@code null}; on a field with a value it is the new value ({@code ""}), unless
	 * {@link #isNullSettingAllowed() null setting is allowed}.
	 */
	default String getNullRepresentation() {
		String nullRepresentation = (String) ComponentUtil.getData(asComponent(), DATA_NULL_REPRESENTATION);
		return nullRepresentation != null ? nullRepresentation : "";
	}

	/**
	 * V7 null representation, see {@link #getNullRepresentation()}. Flow text fields do not accept a {@code null}
	 * value; the SORMAS text fields show this text instead. It has no effect on other fields.
	 */
	default void setNullRepresentation(String nullRepresentation) {
		ComponentUtil.setData(asComponent(), DATA_NULL_REPRESENTATION, nullRepresentation);
	}

	/**
	 * V7 {@code AbstractTextField.isNullSettingAllowed()}: whether text the user enters that equals the
	 * {@link #getNullRepresentation() null representation} sets the value to {@code null} also when the value was not
	 * {@code null}. Default {@code false}, as in V7; no SORMAS field changed it.
	 */
	default boolean isNullSettingAllowed() {
		return Boolean.TRUE.equals(ComponentUtil.getData(asComponent(), DATA_NULL_SETTING_ALLOWED));
	}

	/**
	 * V7 null setting, see {@link #isNullSettingAllowed()}. It has no effect on fields other than text fields.
	 */
	default void setNullSettingAllowed(boolean nullSettingAllowed) {
		ComponentUtil.setData(asComponent(), DATA_NULL_SETTING_ALLOWED, nullSettingAllowed ? Boolean.TRUE : null);
	}

	default void addStyleName(String styleName) {
		if (styleName != null) {
			for (String name : styleName.trim().split("\\s+")) {
				if (!name.isEmpty()) {
					addClassName(name);
				}
			}
		}
	}

	default void removeStyleName(String styleName) {
		if (styleName != null) {
			for (String name : styleName.trim().split("\\s+")) {
				if (!name.isEmpty()) {
					removeClassName(name);
				}
			}
		}
	}

	// Validators (V7 field validators, Flow Validator type)

	@SuppressWarnings("unchecked")
	default List<Validator<? super T>> getValidatorList() {
		List<Validator<? super T>> validators = (List<Validator<? super T>>) ComponentUtil.getData(asComponent(), DATA_VALIDATORS);
		if (validators == null) {
			validators = new ArrayList<>();
			ComponentUtil.setData(asComponent(), DATA_VALIDATORS, validators);
		}
		return validators;
	}

	/**
	 * Adds a validator. Like in V7 it validates the model value: the value after the field's converter, if one is set.
	 */
	@SuppressWarnings({
		"unchecked",
		"rawtypes" })
	default void addValidator(Validator validator) {
		getValidatorList().add(validator);
	}

	/**
	 * Adds a validator written like a V7 validator: it checks the model value and throws an
	 * {@link de.symeda.sormas.ui.utils.InvalidValueException} (e.g. {@link InvalidValueException}) if it is not valid.
	 * Lets one-argument lambdas ({@code value -> { if (..) throw new InvalidValueException(..); }}) compile.
	 */
	@SuppressWarnings("unchecked")
	default <V> void addValidator(ValueValidator<V> validator) {
		addValidator((Validator<Object>) (value, context) -> {
			try {
				validator.validate((V) value);
				return ValidationResult.ok();
			} catch (de.symeda.sormas.ui.utils.InvalidValueException e) {
				return ValidationResult.error(e.getMessage() != null ? e.getMessage() : "");
			}
		});
	}

	/**
	 * V7 {@code Validator} shape: validates a value by throwing.
	 */
	@FunctionalInterface
	interface ValueValidator<V> extends java.io.Serializable {

		void validate(V value) throws de.symeda.sormas.ui.utils.InvalidValueException;
	}

	default void removeValidator(Validator<?> validator) {
		getValidatorList().remove(validator);
	}

	default Collection<Validator<? super T>> getValidators() {
		return Collections.unmodifiableList(new ArrayList<>(getValidatorList()));
	}

	default void removeAllValidators() {
		getValidatorList().clear();
	}

	default boolean isValidationVisible() {
		return !Boolean.FALSE.equals(ComponentUtil.getData(asComponent(), DATA_VALIDATION_VISIBLE));
	}

	/**
	 * V7 validation visibility. The form's binding status handler shows errors only when this is true (default).
	 */
	default void setValidationVisible(boolean validationVisible) {
		ComponentUtil.setData(asComponent(), DATA_VALIDATION_VISIBLE, validationVisible);
	}

	default boolean isInvalidCommitted() {
		return Boolean.TRUE.equals(ComponentUtil.getData(asComponent(), DATA_INVALID_COMMITTED));
	}

	/**
	 * V7 invalid committed: the form writes the value to the bean even when a validator fails. Stored for the form
	 * base; it has no meaning on an unbound field.
	 */
	default void setInvalidCommitted(boolean invalidCommitted) {
		ComponentUtil.setData(asComponent(), DATA_INVALID_COMMITTED, invalidCommitted);
	}

	// Converter (V7 field converter, Flow Converter type)

	@SuppressWarnings("unchecked")
	default Converter<T, Object> getConverter() {
		return (Converter<T, Object>) ComponentUtil.getData(asComponent(), DATA_CONVERTER);
	}

	/**
	 * Sets the converter between the field value and the bound property, like the V7 field converter. Passing
	 * {@code null} removes it.
	 */
	@SuppressWarnings("rawtypes")
	default void setConverter(Converter converter) {
		ComponentUtil.setData(asComponent(), DATA_CONVERTER, converter);
	}

	default ValueContext createValueContext() {
		UI ui = UI.getCurrent();
		Locale locale = ui != null ? ui.getLocale() : Locale.getDefault();
		return new ValueContext(asComponent(), asHasValue(), locale);
	}

	/**
	 * The value converted with the field's converter (the V7 converted value). Without a converter, the value.
	 *
	 * @throws InvalidValueException
	 *             when the value cannot be converted
	 */
	default Object getConvertedValue() {
		Converter<T, Object> converter = getConverter();
		if (converter == null) {
			return getValue();
		}
		Result<Object> result = converter.convertToModel(getValue(), createValueContext());
		return result.getOrThrow(message -> new InvalidValueException(getConversionError() != null ? getConversionError() : message));
	}

	/**
	 * V7 {@code validate()}: unparsable input ({@link #getUnparsableInputError()}), required check, conversion and all
	 * validators. Named differently because Flow fields have a protected {@code validate()}.
	 *
	 * @throws InvalidValueException
	 *             with the first error message
	 */
	default void validateValue() {
		String unparsableInputError = getUnparsableInputError();
		if (unparsableInputError != null) {
			throw new InvalidValueException(unparsableInputError);
		}
		if (isRequired() && isEmpty()) {
			throw new InvalidValueException(getRequiredError() != null ? getRequiredError() : "");
		}
		Object modelValue = getConvertedValue();
		ValidationResult result = runValidators(modelValue, createValueContext());
		if (result.isError()) {
			throw new InvalidValueException(result.getErrorMessage());
		}
	}

	/**
	 * The error of input the field could not parse, or {@code null}. As in V7 ({@code DateField.validate()}, which threw
	 * {@code UnparsableDateString} before any other check), such input makes {@link #validateValue()} and the form's
	 * binding validator ({@link #asBindingValidator()}) fail first, also when the field's value is empty, so a form
	 * commit fails and writes nothing. Default: {@code null} (the field has no input of its own to parse; text fields
	 * with a converter fail in the conversion instead). Overridden by {@link DateField}.
	 */
	default String getUnparsableInputError() {
		return null;
	}

	/**
	 * V7 {@code isValid()}.
	 */
	default boolean isValid() {
		try {
			validateValue();
			return true;
		} catch (InvalidValueException e) {
			return false;
		}
	}

	@SuppressWarnings({
		"unchecked",
		"rawtypes" })
	default ValidationResult runValidators(Object modelValue, ValueContext context) {
		for (Validator validator : new ArrayList<>(getValidatorList())) {
			ValidationResult result = (ValidationResult) validator.apply(modelValue, context);
			if (result.isError()) {
				return result;
			}
		}
		return ValidationResult.ok();
	}

	/**
	 * Converter for the form's binding ({@code binder.forField(field).withConverter(field.asBindingConverter(type))}).
	 * It looks the field's converter up on every call, so {@link #setConverter} works after binding. Without an own
	 * converter it uses the {@link de.symeda.sormas.ui.utils.SormasDefaultConverterFactory} default for text fields
	 * and passes other values through.
	 */
	default Converter<T, Object> asBindingConverter(Class<?> modelType) {
		return new FieldBindingConverter<>(this, modelType);
	}

	/**
	 * Validator for the form's binding: unparsable input ({@link #getUnparsableInputError()}), required check, then the
	 * field's validators, looked up on every call.
	 */
	default Validator<Object> asBindingValidator() {
		return (value, context) -> {
			String unparsableInputError = getUnparsableInputError();
			if (unparsableInputError != null) {
				return ValidationResult.error(unparsableInputError);
			}
			if (isRequired() && isEmpty()) {
				return ValidationResult.error(getRequiredError() != null ? getRequiredError() : "");
			}
			return runValidators(value, context);
		};
	}

	/**
	 * V7 {@code Validator.InvalidValueException}. Extends the form layer's {@link de.symeda.sormas.ui.utils.InvalidValueException}
	 * (thrown by commits), so one {@code catch} covers both; {@code getHtmlMessage()} escapes the message as in V7.
	 */
	class InvalidValueException extends de.symeda.sormas.ui.utils.InvalidValueException {

		private static final long serialVersionUID = 1L;

		public InvalidValueException(String message) {
			super(message);
		}
	}

	/**
	 * V7 {@code Property.ReadOnlyException}: a read-only field refused a programmatic write of a different value (see
	 * {@link ReadOnlyWrites}). Like in V7 it is not caught; the error handler shows the "unexpected error" notification.
	 */
	class ReadOnlyException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public ReadOnlyException() {
			super();
		}
	}
}
