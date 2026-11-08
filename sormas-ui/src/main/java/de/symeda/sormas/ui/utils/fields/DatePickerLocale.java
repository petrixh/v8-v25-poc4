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

import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;

import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;

/**
 * The calendar language of a date picker as in Vaadin 7/8: the calendar pop-up followed the <b>locale of the field</b>,
 * which is the UI locale (the browser's, {@code SormasUI} sets it from the request) unless the code set one on the field
 * ({@code FormComponent.createDateField} sets the user's language). Vaadin 8's {@code LocaleService} sent the month and
 * weekday names of {@link DateFormatSymbols} and the first day of the week of {@link GregorianCalendar} for that locale;
 * so a German browser showed {@code Mo. Di. ...}, Monday first, and an English one {@code Sun Mon ...}, Sunday first,
 * whatever the user's language. A Flow date picker shows English names and Sunday first unless its
 * {@link DatePicker.DatePickerI18n} says otherwise.
 * <p>
 * The Flow pop-up also has "today" and "cancel" buttons (Vaadin 8 had none); their texts follow the same locale, from the
 * SORMAS strings of the matching language (English when SORMAS has no such language or no translation).
 * <p>
 * Only these texts are set; the date formats, reference date and error messages of the picker are kept.
 */
public final class DatePickerLocale {

	private DatePickerLocale() {
	}

	/**
	 * Sets month names, weekday names and their short forms, the first day of the week and the today and cancel texts
	 * of {@code locale} on {@code i18n} and returns it. Everything else in {@code i18n} is kept.
	 */
	public static DatePicker.DatePickerI18n apply(DatePicker.DatePickerI18n i18n, Locale locale) {

		Locale l = locale != null ? locale : Locale.getDefault();
		DateFormatSymbols symbols = new DateFormatSymbols(l);
		// DateFormatSymbols: months 0..11 (a 13th empty entry), weekdays 1..7 from Sunday; Flow wants the same order
		i18n.setMonthNames(Arrays.asList(symbols.getMonths()).subList(0, 12));
		i18n.setWeekdays(Arrays.asList(symbols.getWeekdays()).subList(1, 8));
		i18n.setWeekdaysShort(Arrays.asList(symbols.getShortWeekdays()).subList(1, 8));
		// Calendar: 1 = Sunday; Flow: 0 = Sunday
		i18n.setFirstDayOfWeek(new GregorianCalendar(l).getFirstDayOfWeek() - 1);
		Language language = Language.fromLocaleString(l.toString());
		i18n.setToday(I18nProperties.getString(language, Strings.today));
		i18n.setCancel(I18nProperties.getString(language, Strings.cancel));
		return i18n;
	}

	/**
	 * On every attach, applies the calendar language of the picker's locale (its own, else its UI's) to the picker's
	 * current i18n, so a format set before (e.g. {@link DefaultDateFormat}, {@code setDateFormat}) stays.
	 */
	public static void applyOnAttach(DatePicker picker) {
		picker.addAttachListener(e -> picker.setI18n(apply(orNew(picker.getI18n()), picker.getLocale())));
	}

	/**
	 * {@link #applyOnAttach(DatePicker)} for the date part of a date-time picker.
	 */
	public static void applyOnAttach(DateTimePicker picker) {
		picker.addAttachListener(e -> picker.setDatePickerI18n(apply(orNew(picker.getDatePickerI18n()), picker.getLocale())));
	}

	private static DatePicker.DatePickerI18n orNew(DatePicker.DatePickerI18n i18n) {
		return i18n != null ? i18n : new DatePicker.DatePickerI18n();
	}
}
