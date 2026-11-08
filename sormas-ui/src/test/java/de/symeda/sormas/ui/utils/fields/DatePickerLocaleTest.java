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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;

/**
 * Ticket 2b-18 (sweep 2b-vm8, D1): the date picker pop-up shows the month and weekday names and the first day of the week
 * of the field's locale (the browser's, unless the field has its own), as Vaadin 7/8 did: German browser {@code Mo. Di.
 * ...}, Monday first. The format of the field stays as it was.
 */
public class DatePickerLocaleTest {

	private static final List<String> MONTHS_DE = Arrays.asList(
		"Januar",
		"Februar",
		"März",
		"April",
		"Mai",
		"Juni",
		"Juli",
		"August",
		"September",
		"Oktober",
		"November",
		"Dezember");
	private static final List<String> WEEKDAYS_SHORT_DE = Arrays.asList("So.", "Mo.", "Di.", "Mi.", "Do.", "Fr.", "Sa.");
	private static final List<String> WEEKDAYS_SHORT_EN = Arrays.asList("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat");

	private UI ui;

	@BeforeEach
	public void setUp() {
		ui = new UI();
		ui.setLocale(Locale.GERMANY);
		UI.setCurrent(ui);
	}

	@AfterEach
	public void tearDown() {
		UI.setCurrent(null);
	}

	private static void assertGerman(DatePicker.DatePickerI18n i18n) {
		assertEquals(MONTHS_DE, i18n.getMonthNames());
		assertEquals("Sonntag", i18n.getWeekdays().get(0));
		assertEquals("Montag", i18n.getWeekdays().get(1));
		assertEquals(WEEKDAYS_SHORT_DE, i18n.getWeekdaysShort());
		assertEquals(1, i18n.getFirstDayOfWeek());
		assertEquals("Heute", i18n.getToday());
		assertEquals("Abbrechen", i18n.getCancel());
	}

	private static void assertEnglish(DatePicker.DatePickerI18n i18n) {
		assertEquals("January", i18n.getMonthNames().get(0));
		assertEquals("December", i18n.getMonthNames().get(11));
		assertEquals(12, i18n.getMonthNames().size());
		assertEquals("Sunday", i18n.getWeekdays().get(0));
		assertEquals(WEEKDAYS_SHORT_EN, i18n.getWeekdaysShort());
		assertEquals(0, i18n.getFirstDayOfWeek());
		assertEquals("Today", i18n.getToday());
		assertEquals("Cancel", i18n.getCancel());
	}

	@Test
	public void testNamesFirstDayAndButtonsOfTheLocale() {
		assertGerman(DatePickerLocale.apply(new DatePicker.DatePickerI18n(), Locale.GERMANY));
		assertEnglish(DatePickerLocale.apply(new DatePicker.DatePickerI18n(), Locale.US));
	}

	@Test
	public void testFormatAndReferenceDateAreKept() {
		DatePicker.DatePickerI18n i18n = new DatePicker.DatePickerI18n().setDateFormats("dd.MM.yy", "dd.MM.yyyy");
		i18n.setReferenceDate(LocalDate.of(1996, 11, 8));
		DatePickerLocale.apply(i18n, Locale.GERMANY);
		assertEquals(Arrays.asList("dd.MM.yy", "dd.MM.yyyy"), i18n.getDateFormats());
		assertEquals(LocalDate.of(1996, 11, 8), i18n.getReferenceDate());
		assertGerman(i18n);
	}

	@Test
	public void testLocaleWithoutSormasLanguageGetsJdkNamesAndEnglishButtons() {
		DatePicker.DatePickerI18n i18n = DatePickerLocale.apply(new DatePicker.DatePickerI18n(), new Locale("sv", "SE"));
		assertEquals("januari", i18n.getMonthNames().get(0));
		assertEquals(1, i18n.getFirstDayOfWeek());
		assertEquals("Today", i18n.getToday());
		assertEquals("Cancel", i18n.getCancel());
	}

	@Test
	public void testShimFieldsFollowTheUiLocale() {
		PopupDateField noFormat = new PopupDateField();
		ui.add(noFormat);
		assertEquals(Arrays.asList("dd.MM.yy", "dd.MM.yyyy"), noFormat.getDatePicker().getI18n().getDateFormats());
		assertGerman(noFormat.getDatePicker().getI18n());

		// as the field factory and DateTimeField: own format, names of the UI locale
		DateField ownFormat = new DateField();
		ownFormat.setDateFormat("dd/MM/yyyy");
		ui.add(ownFormat);
		assertEquals(Arrays.asList("dd/MM/yyyy"), ownFormat.getDatePicker().getI18n().getDateFormats());
		assertGerman(ownFormat.getDatePicker().getI18n());

		// a format set after attach keeps the names
		ownFormat.setDateFormat("d.M.yyyy");
		assertEquals(Arrays.asList("d.M.yyyy"), ownFormat.getDatePicker().getI18n().getDateFormats());
		assertGerman(ownFormat.getDatePicker().getI18n());
	}

	@Test
	public void testEnglishUi() {
		ui.setLocale(Locale.US);
		DateField field = new DateField();
		ui.add(field);
		assertEnglish(field.getDatePicker().getI18n());
	}

	@Test
	public void testRawPickerReplacingAVaadin8DateField() {
		DatePicker picker = new DatePicker();
		DefaultDateFormat.applyOnAttach(picker);
		ui.add(picker);
		assertEquals(Arrays.asList("dd.MM.yy", "dd.MM.yyyy"), picker.getI18n().getDateFormats());
		assertGerman(picker.getI18n());
	}

	@Test
	public void testPickerWithItsOwnLocaleAndFormat() {
		// FormComponent.createDateField: the user's language as the field locale (Vaadin 8 setLocale), own format
		DatePicker picker = new DatePicker();
		picker.setLocale(Locale.ENGLISH);
		picker.setI18n(new DatePicker.DatePickerI18n().setDateFormat("MM/dd/yyyy"));
		DatePickerLocale.applyOnAttach(picker);
		ui.add(picker);
		assertEquals(Arrays.asList("MM/dd/yyyy"), picker.getI18n().getDateFormats());
		assertEquals("January", picker.getI18n().getMonthNames().get(0));
		assertEquals(WEEKDAYS_SHORT_EN, picker.getI18n().getWeekdaysShort());
	}

	@Test
	public void testDateTimePickerKeepsItsDateFormat() {
		// customizable date-time fields: a fresh i18n with the user's date format only (carried row 12)
		DateTimePicker picker = new DateTimePicker();
		picker.setDatePickerI18n(new DatePicker.DatePickerI18n().setDateFormat("dd.MM.yyyy"));
		DatePickerLocale.applyOnAttach(picker);
		ui.add(picker);
		assertEquals(Arrays.asList("dd.MM.yyyy"), picker.getDatePickerI18n().getDateFormats());
		assertGerman(picker.getDatePickerI18n());
	}
}
