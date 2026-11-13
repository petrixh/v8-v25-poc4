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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;

import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;

/**
 * Ticket 3-07 (change review CR04): a two-digit year typed into a date field is read as in the old app.
 * <ul>
 * <li>Fields with a format of their own ({@code M/d/yyyy}, {@code dd.MM.yyyy}; form fields, date-time fields, filters):
 * the old app's form fields read {@code 70} as the year 70 and its date converter ({@link DateHelper#toCorrectCentury})
 * moved it to at most 10 years ahead and 89 back (3/14/70 is 1970, 1/1/45 is 1945, 3/14/30 is 2030).</li>
 * <li>Fields without a format (the browser's short date, {@code M/d/yy}): GWT's 80 years back to 20 ahead (2b-08).</li>
 * </ul>
 * The Flow date picker parses on the client: a two-digit year (also typed into a {@code yyyy} format) goes to the year
 * from 50 before to 49 after the year of its reference date (date-fns {@code normalizeTwoDigitYear}, modelled in
 * {@link #clientYear}). Without a reference date it is today, and 3/14/70 is read as 2070. The browser check of the
 * ticket types every two-digit year into both kinds of field and compares with the same rules.
 */
public class TwoDigitYearsTest {

	private UI ui;

	@BeforeEach
	public void setUp() {
		ui = new UI();
		ui.setLocale(Locale.US);
		UI.setCurrent(ui);
	}

	@AfterEach
	public void tearDown() {
		UI.setCurrent(null);
	}

	/**
	 * The client's year for a two-digit year: date-fns {@code normalizeTwoDigitYear(twoDigitYear, referenceYear)} for
	 * reference years after 50.
	 */
	static int clientYear(int twoDigitYear, LocalDate reference) {
		int rangeEnd = reference.getYear() + 50;
		int rangeEndCentury = rangeEnd / 100 * 100;
		return twoDigitYear + rangeEndCentury - (twoDigitYear >= rangeEnd % 100 ? 100 : 0);
	}

	/** The old app's form fields: the year as typed, corrected by {@link DateHelper#toCorrectCentury}. */
	private static int oldFormFieldYear(int twoDigitYear, LocalDate today) {
		Calendar typed = Calendar.getInstance();
		typed.clear();
		typed.set(twoDigitYear, Calendar.MARCH, 14);
		Date reference = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
		Calendar corrected = Calendar.getInstance();
		corrected.setTime(DateHelper.toCorrectCentury(typed.getTime(), reference));
		return corrected.get(Calendar.YEAR);
	}

	/** GWT {@code DateTimeFormat} for a {@code yy} pattern, outside the one ambiguous year: 80 years back. */
	private static int gwtYear(int twoDigitYear, LocalDate today) {
		int start = today.getYear() - 80;
		return twoDigitYear + start / 100 * 100 + (twoDigitYear < start % 100 ? 100 : 0);
	}

	private static void assertReference(int yearsBack, LocalDate before, DatePicker.DatePickerI18n i18n) {
		LocalDate after = LocalDate.now();
		LocalDate reference = i18n.getReferenceDate();
		assertTrue(
			reference != null && (reference.equals(before.minusYears(yearsBack)) || reference.equals(after.minusYears(yearsBack))),
			"reference date " + reference);
	}

	private static void assertOwnFormatRule(LocalDate before, DatePicker.DatePickerI18n i18n) {
		assertReference(DefaultDateFormat.OWN_FORMAT_REFERENCE_YEARS_BACK, before, i18n);
	}

	@Test
	public void testOwnFormatReadsAsTheOldFormFields() {
		LocalDate today = LocalDate.of(2026, 11, 13);
		LocalDate reference = DefaultDateFormat.ownFormatTwoDigitYears(new DatePicker.DatePickerI18n(), today).getReferenceDate();
		assertEquals(LocalDate.of(1987, 11, 13), reference);
		// the inputs of the ticket, as the old app showed them (3/14/70, 3/14/30, 1/1/45 on running copies)
		assertEquals(1970, clientYear(70, reference));
		assertEquals(2030, clientYear(30, reference));
		assertEquals(1945, clientYear(45, reference));
		assertEquals(2036, clientYear(36, reference));
		assertEquals(1937, clientYear(37, reference));
		// Flow's default (reference today): 2070 and 2045
		assertEquals(2070, clientYear(70, today));
		assertEquals(2045, clientYear(45, today));
		// every two-digit year, for this year and the coming decades
		for (int year = 2000; year <= 2088; year++) {
			LocalDate day = LocalDate.of(year, 6, 1);
			LocalDate ref = DefaultDateFormat.ownFormatTwoDigitYears(new DatePicker.DatePickerI18n(), day).getReferenceDate();
			for (int yy = 0; yy < 100; yy++) {
				assertEquals(oldFormFieldYear(yy, day), clientYear(yy, ref), "today " + day + ", typed " + yy);
			}
		}
	}

	@Test
	public void testDefaultFormatReadsAsGwt() {
		LocalDate today = LocalDate.of(2026, 11, 13);
		DatePicker.DatePickerI18n i18n = DefaultDateFormat.i18n(Locale.US, today);
		assertEquals(Arrays.asList("M/d/yy", "M/d/yyyy"), i18n.getDateFormats());
		assertEquals(LocalDate.of(1996, 11, 13), i18n.getReferenceDate());
		assertEquals(1970, clientYear(70, i18n.getReferenceDate()));
		assertEquals(2045, clientYear(45, i18n.getReferenceDate()));
		for (int yy = 0; yy < 100; yy++) {
			if (yy != 46) { // GWT's ambiguous year (1946 or 2046 by month and day); the client reads 1946
				assertEquals(gwtYear(yy, today), clientYear(yy, i18n.getReferenceDate()), "typed " + yy);
			}
		}
	}

	@Test
	public void testFieldOfTheFieldFactoryWithTheUsersFormat() {
		LocalDate today = LocalDate.now();
		DateField field = new SormasFieldGroupFieldFactory(new FieldVisibilityCheckers(), UiFieldAccessCheckers.getNoop())
			.createField(Date.class, DateField.class);
		ui.add(field);
		assertEquals(Arrays.asList(DateFormatHelper.getDateFormatPattern()), field.getDatePicker().getI18n().getDateFormats());
		assertOwnFormatRule(today, field.getDatePicker().getI18n());
	}

	@Test
	public void testFormatSetBeforeOrAfterAttach() {
		LocalDate today = LocalDate.now();
		DateField before = new DateField();
		before.setDateFormat("dd.MM.yyyy");
		ui.add(before);
		assertOwnFormatRule(today, before.getDatePicker().getI18n());

		// attached without a format (default format, GWT window), then given one
		DateField after = new DateField();
		ui.add(after);
		assertReference(DefaultDateFormat.REFERENCE_YEARS_BACK, today, after.getDatePicker().getI18n());
		after.setDateFormat("M/d/yyyy");
		assertEquals(Arrays.asList("M/d/yyyy"), after.getDatePicker().getI18n().getDateFormats());
		assertOwnFormatRule(today, after.getDatePicker().getI18n());
	}

	@Test
	public void testFieldWithoutFormatKeepsTheGwtWindow() {
		LocalDate today = LocalDate.now();
		PopupDateField field = new PopupDateField();
		ui.add(field);
		assertReference(DefaultDateFormat.REFERENCE_YEARS_BACK, today, field.getDatePicker().getI18n());

		DatePicker raw = new DatePicker();
		DefaultDateFormat.applyOnAttach(raw);
		ui.add(raw);
		assertReference(DefaultDateFormat.REFERENCE_YEARS_BACK, today, raw.getI18n());
	}

	@Test
	public void testRawPickerWithItsOwnFormat() {
		LocalDate today = LocalDate.now();
		DatePicker picker = new DatePicker();
		picker.setI18n(new DatePicker.DatePickerI18n().setDateFormat("dd/MM/yyyy"));
		DefaultDateFormat.applyOnAttach(picker);
		ui.add(picker);
		assertEquals(Arrays.asList("dd/MM/yyyy"), picker.getI18n().getDateFormats());
		assertOwnFormatRule(today, picker.getI18n());
	}

	@Test
	public void testPickersWithTheCalendarLanguageOnly() {
		LocalDate today = LocalDate.now();
		// customizable date fields, configuration default values
		DatePicker picker = new DatePicker();
		picker.setI18n(new DatePicker.DatePickerI18n().setDateFormat("M/d/yyyy"));
		DatePickerLocale.applyOnAttach(picker);
		ui.add(picker);
		assertEquals(Arrays.asList("M/d/yyyy"), picker.getI18n().getDateFormats());
		assertOwnFormatRule(today, picker.getI18n());

		// FormComponent.createDateField, quick immunization: own locale and format
		DatePicker own = new DatePicker();
		own.setLocale(Locale.GERMAN);
		own.setI18n(new DatePicker.DatePickerI18n().setDateFormat("dd.MM.yyyy"));
		DatePickerLocale.applyOwnLocaleOnAttach(own);
		ui.add(own);
		assertOwnFormatRule(today, own.getI18n());

		// customizable date-time fields: the date part
		DateTimePicker dateTime = new DateTimePicker();
		dateTime.setDatePickerI18n(new DatePicker.DatePickerI18n().setDateFormat("M/d/yyyy"));
		DatePickerLocale.applyOnAttach(dateTime);
		ui.add(dateTime);
		assertEquals(Arrays.asList("M/d/yyyy"), dateTime.getDatePickerI18n().getDateFormats());
		assertOwnFormatRule(today, dateTime.getDatePickerI18n());
	}
}
