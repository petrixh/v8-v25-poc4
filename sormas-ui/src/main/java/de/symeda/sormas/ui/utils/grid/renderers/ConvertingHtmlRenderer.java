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
package de.symeda.sormas.ui.utils.grid.renderers;

import java.util.Locale;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

import de.symeda.sormas.ui.utils.SormasDefaultConverterFactory;

/**
 * HTML renderer for columns that came from Vaadin 7 grids. A Vaadin 7 grid converted each value to a String with the
 * session's converter factory before its HTML renderer saw it (numbers in the UI's locale, enums and captions as SORMAS
 * shows them). This renderer does the same conversion with {@link SormasDefaultConverterFactory}, then calls
 * {@link #encodeString(String)}.
 */
public abstract class ConvertingHtmlRenderer extends ValueRenderer<Object> {

	private static final long serialVersionUID = 1L;

	protected ConvertingHtmlRenderer() {
		super("");
	}

	@Override
	public final String encode(Object value) {
		return encodeString(toPresentation(value));
	}

	/**
	 * @param value
	 *            the value as a String, as the Vaadin 7 converter gave it ({@code null} for {@code null})
	 * @return the HTML to show, {@code null} for an empty cell
	 */
	protected abstract String encodeString(String value);

	@Override
	public boolean isHtml() {
		return true;
	}

	/**
	 * The Vaadin 7 grid's presentation of a model value: converted with the SORMAS default converters where one exists,
	 * else {@code toString()}.
	 */
	@SuppressWarnings({
		"unchecked",
		"rawtypes" })
	public static String toPresentation(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof String) {
			return (String) value;
		}
		Converter converter = SormasDefaultConverterFactory.createStringConverter(value.getClass());
		if (converter != null) {
			UI ui = UI.getCurrent();
			Locale locale = ui != null ? ui.getLocale() : Locale.getDefault();
			Object presentation = converter.convertToPresentation(value, new ValueContext(locale));
			return presentation != null ? presentation.toString() : null;
		}
		return value.toString();
	}
}
