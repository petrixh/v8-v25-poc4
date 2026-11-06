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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Locale;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.converter.StringToBigDecimalConverter;
import com.vaadin.flow.data.converter.StringToBigIntegerConverter;
import com.vaadin.flow.data.converter.StringToBooleanConverter;
import com.vaadin.flow.data.converter.StringToDoubleConverter;
import com.vaadin.flow.data.converter.StringToFloatConverter;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.converter.StringToLongConverter;

import de.symeda.sormas.api.utils.HasCaption;

/**
 * Default converters between a field value and a bound property, as the V7 session converter factory did. Flow has no
 * session converter factory: the form binding asks for them (see
 * {@link de.symeda.sormas.ui.utils.fields.FieldBindingConverter}).
 */
@SuppressWarnings("serial")
public final class SormasDefaultConverterFactory {

	private static final DateConverter DATE_CONVERTER = new DateConverter();

	/**
	 * V7 default conversion error message.
	 */
	private static String conversionError(Class<?> modelType) {
		return "Could not convert value to " + modelType.getSimpleName();
	}

	/**
	 * Converter from a text field value to the model type, or {@code null} if there is none.
	 */
	@SuppressWarnings({
		"unchecked",
		"rawtypes" })
	public static Converter<String, ?> createStringConverter(Class<?> sourceType) {
		if (Enum.class.isAssignableFrom(sourceType)) {
			return new StringToEnumConverter((Class<? extends Enum>) sourceType);
		} else if (HasCaption.class.isAssignableFrom(sourceType)) {
			return new StringToAnythingConverter<HasCaption>((Class<HasCaption>) sourceType) {

				@Override
				public String convertToPresentation(HasCaption value, ValueContext context) {
					return value != null ? value.buildCaption() : null;
				}
			};
		} else if (Integer.class == sourceType || int.class == sourceType) {
			return new StringToIntegerConverter(conversionError(sourceType));
		} else if (Long.class == sourceType || long.class == sourceType) {
			return new StringToLongConverter(conversionError(sourceType));
		} else if (Double.class == sourceType || double.class == sourceType) {
			return new StringToDoubleConverter(conversionError(sourceType));
		} else if (Float.class == sourceType || float.class == sourceType) {
			return new StringToFloatConverter(conversionError(sourceType));
		} else if (BigDecimal.class == sourceType) {
			return new StringToBigDecimalConverter(conversionError(sourceType));
		} else if (BigInteger.class == sourceType) {
			return new StringToBigIntegerConverter(conversionError(sourceType));
		} else if (Boolean.class == sourceType || boolean.class == sourceType) {
			return new StringToBooleanConverter(conversionError(sourceType));
		}
		return null;
	}

	public static Converter<Date, ?> createDateConverter(Class<?> sourceType) {
		if (Date.class == sourceType) {
			return DATE_CONVERTER;
		}
		return null;
	}

	/**
	 * V7 {@code StringToEnumConverter} with the SORMAS presentation ({@link #enumToString}).
	 */
	@SuppressWarnings({
		"rawtypes",
		"unchecked" })
	private static final class StringToEnumConverter implements Converter<String, Enum> {

		private final Class<? extends Enum> enumType;

		private StringToEnumConverter(Class<? extends Enum> enumType) {
			this.enumType = enumType;
		}

		@Override
		public Result<Enum> convertToModel(String value, ValueContext context) {
			if (value == null || value.trim().isEmpty()) {
				return Result.ok(null);
			}
			Locale locale = context.getLocale().orElse(null);
			String trimmed = value.trim();
			for (Enum constant : enumType.getEnumConstants()) {
				if (enumToString(constant, locale).equals(trimmed)) {
					return Result.ok(constant);
				}
			}
			try {
				return Result.ok(Enum.valueOf(enumType, trimmed.toUpperCase(Locale.ROOT).replace(" ", "_")));
			} catch (IllegalArgumentException e) {
				return Result.error(conversionError(enumType));
			}
		}

		@Override
		public String convertToPresentation(Enum value, ValueContext context) {
			if (value == null) {
				return null;
			}
			return SormasDefaultConverterFactory.enumToString(value, context.getLocale().orElse(null));
		}
	}

	public static String enumToString(Enum<?> value, Locale locale) {
		if (locale == null) {
			locale = Locale.getDefault();
		}

		String enumString = value.toString();
		// we don't want to have this part of Vaadin magic
//        if (enumString.equals(value.name())) {
//            // FOO -> Foo
//            // FOO_BAR -> Foo bar
//            // _FOO -> _foo
//            String result = enumString.substring(0, 1).toUpperCase(locale);
//            result += enumString.substring(1).toLowerCase(locale).replace('_',
//                    ' ');
//            return result;
//        } else 
		{
			return enumString;
		}
	}
}
