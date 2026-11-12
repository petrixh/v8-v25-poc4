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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.datepicker.DatePicker;

import de.symeda.sormas.api.event.EventDto;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.utils.AbstractForm.BeanFieldGroup;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;

/**
 * Ticket 1b-14: as in Vaadin 7 ({@code com.vaadin.v7.ui.DateField.changeVariables}, {@code Resolution.DAY}, 8.14.3), a
 * day picked or typed by the user replaces only year, month and day of the current value; the time of day is kept.
 * Without a current value the result is the start of the day. Untouched fields write the stored object back.
 */
public class DateFieldKeepsTimeOfDayTest {

	private static final ZoneId ZONE = ZoneId.systemDefault();

	private static Date date(LocalDateTime dateTime) {
		return Date.from(dateTime.atZone(ZONE).toInstant());
	}

	private static Timestamp stored(LocalDateTime dateTime, int nanos) {
		Timestamp timestamp = new Timestamp(date(dateTime).getTime());
		timestamp.setNanos(nanos);
		return timestamp;
	}

	private static DateField factoryField() {
		return new SormasFieldGroupFieldFactory(new FieldVisibilityCheckers(), UiFieldAccessCheckers.getNoop())
			.createField(Date.class, DateField.class);
	}

	/**
	 * A user change in the browser: the picker gets the new day and fires a client-side value change.
	 */
	private static void userPicks(DateField field, LocalDate day) {
		DatePicker picker = field.getDatePicker();
		LocalDate old = picker.getValue();
		picker.setValue(day);
		ComponentUtil.fireEvent(picker, new ComponentValueChangeEvent<>(picker, picker, old, true));
	}

	private static BeanFieldGroup<EventDto> group(DateField reportDate) {
		BeanFieldGroup<EventDto> group = new BeanFieldGroup<>(EventDto.class);
		group.setFieldFactory(new SormasFieldGroupFieldFactory(new FieldVisibilityCheckers(), UiFieldAccessCheckers.getNoop()));
		group.bind(reportDate, EventDto.REPORT_DATE_TIME);
		return group;
	}

	/**
	 * The finding: a stored report date with a time of day ({@code 11:00:17.25}) edited to another day keeps that time
	 * of day through the form commit (the new app wrote midnight before).
	 */
	@Test
	public void editedDayKeepsStoredTimeOfDay() {
		DateField reportDate = factoryField();
		BeanFieldGroup<EventDto> group = group(reportDate);
		EventDto event = EventDto.build();
		event.setReportDateTime(stored(LocalDateTime.of(2026, 10, 26, 11, 0, 17), 250_000_000));
		group.setItemDataSource(event);

		userPicks(reportDate, LocalDate.of(2026, 10, 24));
		assertTrue(group.isModified());
		group.commit();

		assertEquals(date(LocalDateTime.of(2026, 10, 24, 11, 0, 17, 250_000_000)), event.getReportDateTime());
	}

	/**
	 * Untouched: the stored object is written back unchanged (V7 kept the stored object too).
	 */
	@Test
	public void untouchedWritesStoredObject() {
		DateField reportDate = factoryField();
		BeanFieldGroup<EventDto> group = group(reportDate);
		Timestamp storedValue = stored(LocalDateTime.of(2026, 10, 26, 11, 0, 17), 250_000_000);
		EventDto event = EventDto.build();
		event.setReportDateTime(storedValue);
		group.setItemDataSource(event);

		assertFalse(group.isModified());
		group.commit();
		assertSame(storedValue, event.getReportDateTime());
	}

	/**
	 * No current value: the start of the picked day. Cleared: {@code null}; picked again after clearing: start of day.
	 */
	@Test
	public void withoutCurrentValueStartOfDay() {
		DateField field = factoryField();
		userPicks(field, LocalDate.of(2026, 10, 24));
		assertEquals(date(LocalDateTime.of(2026, 10, 24, 0, 0)), field.getValue());

		field.setValue(date(LocalDateTime.of(2026, 10, 24, 9, 30, 5)));
		userPicks(field, null);
		assertNull(field.getValue());
		userPicks(field, LocalDate.of(2026, 10, 20));
		assertEquals(date(LocalDateTime.of(2026, 10, 20, 0, 0)), field.getValue());
	}

	/**
	 * Successive edits keep the time of day; year, month and day are all replaced (no roll-over from the old day, e.g.
	 * the 31st of a month moved to February).
	 */
	@Test
	public void successiveEditsAndMonthEnds() {
		DateField field = factoryField();
		field.setValue(date(LocalDateTime.of(2026, 1, 31, 13, 44, 55, 999_000_000)));

		userPicks(field, LocalDate.of(2025, 2, 28));
		assertEquals(date(LocalDateTime.of(2025, 2, 28, 13, 44, 55, 999_000_000)), field.getValue());

		userPicks(field, LocalDate.of(2026, 12, 31));
		assertEquals(date(LocalDateTime.of(2026, 12, 31, 13, 44, 55, 999_000_000)), field.getValue());
	}
}
