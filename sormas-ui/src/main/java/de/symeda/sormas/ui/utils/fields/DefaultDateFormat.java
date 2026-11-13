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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.datepicker.DatePicker;

/**
 * The display format a Vaadin 7 or Vaadin 8 date field had when the code set none ({@code setDateFormat} never called):
 * the short date pattern of the component's locale, which is the browser's locale in SORMAS ({@code SormasUI} sets the UI
 * locale from the request), e.g. {@code M/d/yy} for en-US and {@code dd.MM.yy} for de-DE. Not the user's language: the
 * fields that follow the user's language set {@code DateFormatHelper.getDateFormatPattern()} explicitly.
 * <p>
 * A Flow date picker without a format shows the browser's {@code toLocaleDateString} format instead, with a four-digit
 * year ({@code 11/8/2026} where Vaadin 7 showed {@code 11/8/26}). {@link #applyOnAttach(DatePicker)} gives such a picker the
 * Vaadin 7/8 format.
 * <p>
 * Parsing as in Vaadin 7/8 (GWT {@code DateTimeFormat}): a four-digit year is accepted too, and a two-digit year falls
 * within 80 years before and 20 years after today.
 */
public final class DefaultDateFormat {

	/**
	 * Flow chooses the century of a two-digit year within 50 years of the reference date's year (from 50 years before to
	 * 49 after); 30 years back gives the GWT window of 80 years before and 20 years after today.
	 */
	static final int REFERENCE_YEARS_BACK = 30;

	/**
	 * For pickers with a format of their own: 39 years back gives 89 years before to 10 years after this year, the
	 * century correction of the old app's date fields ({@code DateHelper.toCorrectCentury}, see
	 * {@link #ownFormatTwoDigitYears}).
	 */
	static final int OWN_FORMAT_REFERENCE_YEARS_BACK = 39;

	private DefaultDateFormat() {
	}

	/**
	 * The Vaadin 7/8 default pattern for the locale: {@code com.vaadin.server.LocaleService} sent the short date pattern,
	 * and the client ({@code VTextualDate.cleanFormat}) removed unsupported letters and leading or trailing delimiters.
	 */
	public static String pattern(Locale locale) {
		DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale != null ? locale : Locale.getDefault());
		String pattern = dateFormat instanceof SimpleDateFormat ? ((SimpleDateFormat) dateFormat).toPattern() : "yyyy-MM-dd";
		pattern = pattern.replaceAll("[GzZwWkK]", "").trim();
		while (pattern.startsWith("/") || pattern.startsWith(".") || pattern.startsWith(",")) {
			pattern = pattern.substring(1);
		}
		while (pattern.endsWith("/") || pattern.endsWith(".") || pattern.endsWith(",")) {
			pattern = pattern.substring(0, pattern.length() - 1);
		}
		return pattern.trim();
	}

	/**
	 * The picker settings for the locale's default pattern: display in that pattern, parse it and its four-digit-year
	 * variant, two-digit years as in Vaadin 7/8.
	 */
	public static DatePicker.DatePickerI18n i18n(Locale locale, LocalDate today) {
		return configure(new DatePicker.DatePickerI18n(), locale, today);
	}

	private static DatePicker.DatePickerI18n configure(DatePicker.DatePickerI18n i18n, Locale locale, LocalDate today) {
		String pattern = pattern(locale);
		if (pattern.contains("yy") && !pattern.contains("yyy")) {
			i18n.setDateFormats(pattern, pattern.replace("yy", "yyyy"));
		} else {
			i18n.setDateFormat(pattern);
		}
		i18n.setReferenceDate(today.minusYears(REFERENCE_YEARS_BACK));
		return i18n;
	}

	/**
	 * Two-digit years in a picker with a format of its own (e.g. {@code DateFormatHelper.getDateFormatPattern()},
	 * {@code M/d/yyyy}), as the old app's form date fields read them: the client took {@code 70} as the year 70 and the
	 * date converter ({@code DateConverter}, {@code DateHelper.toCorrectCentury}) moved it to the century that puts it at
	 * most 10 years after and 89 years before this year (2026: {@code 37} to {@code 99} are 1937 to 1999, {@code 00}
	 * to {@code 36} are 2000 to 2036). Flow's default reads {@code 70} as 2070 (50 years either side of today). Flow
	 * applies the reference date to the two-digit variant of a {@code yyyy} format too. Returns {@code i18n} (migration
	 * ticket 3-07, CR04).
	 */
	public static DatePicker.DatePickerI18n ownFormatTwoDigitYears(DatePicker.DatePickerI18n i18n, LocalDate today) {
		i18n.setReferenceDate(today.minusYears(OWN_FORMAT_REFERENCE_YEARS_BACK));
		return i18n;
	}

	/**
	 * When the picker is attached and has no date format of its own, gives it the default format of the locale of the UI
	 * it is attached to (see the class comment; no caller sets a locale on these pickers). A format set later replaces it.
	 * Also gives it the calendar language of that locale ({@link DatePickerLocale}), with or without a format of its own,
	 * and with a format of its own the two-digit years of {@link #ownFormatTwoDigitYears}.
	 */
	public static void applyOnAttach(DatePicker picker) {
		picker.addAttachListener(e -> {
			DatePicker.DatePickerI18n current = picker.getI18n();
			DatePicker.DatePickerI18n i18n = current != null ? current : new DatePicker.DatePickerI18n();
			List<String> formats = i18n.getDateFormats();
			if (formats == null || formats.isEmpty()) {
				configure(i18n, e.getUI().getLocale(), LocalDate.now());
			} else {
				ownFormatTwoDigitYears(i18n, LocalDate.now());
			}
			picker.setI18n(DatePickerLocale.apply(i18n, e.getUI().getLocale()));
		});
	}
}
