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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker;

import de.symeda.sormas.api.caze.NewCaseDateType;
import de.symeda.sormas.api.utils.DateFilterOption;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.EpiWeekAndDateFilterComponent;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;

/**
 * Ticket 2b-08 (sweep 2b-003b, AD1, WF57 step 2): a date field the code gives no format shows the Vaadin 7/8 default, the
 * short date pattern of the UI locale (en-US {@code 11/8/26}), not the Flow picker's four-digit year ({@code 11/8/2026}).
 * Fields with a format of their own keep it.
 */
public class DefaultDateFormatTest {

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

	private static List<String> formats(DatePicker picker) {
		return picker.getI18n() != null ? picker.getI18n().getDateFormats() : null;
	}

	@Test
	public void testPatternIsTheShortDatePatternOfTheLocale() {
		assertEquals("M/d/yy", DefaultDateFormat.pattern(Locale.US));
		assertEquals("dd.MM.yy", DefaultDateFormat.pattern(Locale.GERMANY));
		assertEquals("dd.MM.yy", DefaultDateFormat.pattern(Locale.GERMAN));
	}

	@Test
	public void testI18nParsesTwoAndFourDigitYearsWithTheGwtCentury() {
		DatePicker.DatePickerI18n i18n = DefaultDateFormat.i18n(Locale.US, LocalDate.of(2026, 11, 8));
		assertEquals(Arrays.asList("M/d/yy", "M/d/yyyy"), i18n.getDateFormats());
		// two-digit years fall within 1946-11-08 .. 2046-11-07 as with GWT (80 years back, 20 ahead)
		assertEquals(LocalDate.of(1996, 11, 8), i18n.getReferenceDate());
	}

	@Test
	public void testDashboardCustomPeriodDateFieldsUseTheDefaultPattern() {
		EpiWeekAndDateFilterComponent<NewCaseDateType> filter = new EpiWeekAndDateFilterComponent<>(true, true, "info", null);
		ui.add(filter);
		filter.getDateFilterOptionFilter().setValue(DateFilterOption.DATE);

		assertEquals(Arrays.asList("M/d/yy", "M/d/yyyy"), formats(filter.getDateFromFilter().getDatePicker()));
		assertEquals(Arrays.asList("M/d/yy", "M/d/yyyy"), formats(filter.getDateToFilter().getDatePicker()));
		assertEquals(null, filter.getDateFromFilter().getDateFormat());
	}

	@Test
	public void testShimFieldWithoutFormatFollowsTheUiLocale() {
		ui.setLocale(Locale.GERMANY);
		PopupDateField field = new PopupDateField();
		ui.add(field);
		assertEquals(Arrays.asList("dd.MM.yy", "dd.MM.yyyy"), formats(field.getDatePicker()));
	}

	@Test
	public void testFieldWithOwnFormatKeepsIt() {
		DateField factoryField = new SormasFieldGroupFieldFactory(new FieldVisibilityCheckers(), UiFieldAccessCheckers.getNoop())
			.createField(Date.class, DateField.class);
		ui.add(factoryField);
		assertEquals(Arrays.asList(DateFormatHelper.getDateFormatPattern()), formats(factoryField.getDatePicker()));

		DateField explicit = new DateField();
		explicit.setDateFormat("dd/MM/yyyy");
		ui.add(explicit);
		assertEquals(Arrays.asList("dd/MM/yyyy"), formats(explicit.getDatePicker()));
	}

	@Test
	public void testFormatSetAfterAttachReplacesTheDefault() {
		DateField field = new DateField();
		ui.add(field);
		field.setDateFormat("M/d/yyyy");
		assertEquals(Arrays.asList("M/d/yyyy"), formats(field.getDatePicker()));
	}

	@Test
	public void testRawPickerReplacingAVaadin8DateField() {
		DatePicker picker = new DatePicker();
		DefaultDateFormat.applyOnAttach(picker);
		ui.add(picker);
		assertEquals(Arrays.asList("M/d/yy", "M/d/yyyy"), formats(picker));

		DatePicker withFormat = new DatePicker();
		withFormat.setI18n(new DatePicker.DatePickerI18n().setDateFormat("yyyy-MM-dd"));
		DefaultDateFormat.applyOnAttach(withFormat);
		ui.add(withFormat);
		assertEquals(Arrays.asList("yyyy-MM-dd"), formats(withFormat));
	}
}
