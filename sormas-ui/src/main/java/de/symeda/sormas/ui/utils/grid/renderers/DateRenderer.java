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

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import com.vaadin.flow.component.UI;

/**
 * Date renderer with the Vaadin 7/8 {@code DateRenderer} API: a {@link DateFormat} (e.g. from {@code DateFormatHelper} or
 * {@code DateHelper}) formats the value, or a {@link String#format} pattern in the UI's locale.
 */
public class DateRenderer extends ValueRenderer<Date> {

	private static final long serialVersionUID = 1L;

	private final DateFormat dateFormat;
	private final String formatString;

	public DateRenderer(DateFormat dateFormat) {
		this(dateFormat, "");
	}

	public DateRenderer(DateFormat dateFormat, String nullRepresentation) {
		super(nullRepresentation);
		this.dateFormat = dateFormat;
		this.formatString = null;
	}

	public DateRenderer(String formatString) {
		this(formatString, "");
	}

	public DateRenderer(String formatString, String nullRepresentation) {
		super(nullRepresentation);
		this.dateFormat = null;
		this.formatString = formatString;
	}

	@Override
	public String encode(Date value) {
		if (value == null) {
			return getNullRepresentation();
		}
		if (dateFormat != null) {
			return dateFormat.format(value);
		}
		UI ui = UI.getCurrent();
		return String.format(ui != null ? ui.getLocale() : Locale.getDefault(), formatString, value);
	}

	@Override
	public boolean isHtml() {
		return false;
	}
}
