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

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

import de.symeda.sormas.ui.utils.SormasDefaultConverterFactory;

/**
 * The converter a form binding uses for a {@link Field}: the field's own converter (V7 {@code setConverter}), else the
 * {@link SormasDefaultConverterFactory} default between a text field and a non-text property (the V7 session converter
 * factory), else the value unchanged. The field's converter is looked up on every call.
 */
public class FieldBindingConverter<T> implements Converter<T, Object> {

	private static final long serialVersionUID = 1L;

	private final Field<T> field;
	private final Class<?> modelType;

	public FieldBindingConverter(Field<T> field, Class<?> modelType) {
		this.field = field;
		this.modelType = modelType;
	}

	@SuppressWarnings({
		"unchecked",
		"rawtypes" })
	private Converter<T, Object> getEffectiveConverter() {
		Converter<T, Object> converter = field.getConverter();
		if (converter == null && isTextField() && modelType != null && modelType != String.class && modelType != Object.class) {
			converter = (Converter) SormasDefaultConverterFactory.createStringConverter(modelType);
		}
		return converter;
	}

	private boolean isTextField() {
		return field.getEmptyValue() instanceof String;
	}

	@Override
	public Result<Object> convertToModel(T value, ValueContext context) {
		Converter<T, Object> converter = getEffectiveConverter();
		if (converter == null) {
			return Result.ok(value);
		}
		Result<Object> result = converter.convertToModel(value, context);
		String conversionError = field.getConversionError();
		if (conversionError != null && result.isError()) {
			return Result.error(conversionError);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T convertToPresentation(Object value, ValueContext context) {
		Converter<T, Object> converter = getEffectiveConverter();
		T presentation = converter != null ? converter.convertToPresentation(value, context) : (T) value;
		// Flow text fields do not accept null; the CheckBox keeps a null property as null (V7), shown unchecked
		return presentation == null && !(field instanceof CheckBox) ? field.getEmptyValue() : presentation;
	}
}
