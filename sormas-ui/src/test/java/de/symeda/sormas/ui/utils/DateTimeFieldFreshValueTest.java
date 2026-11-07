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
package de.symeda.sormas.ui.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.event.EventDto;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.utils.AbstractForm.BeanFieldGroup;
import de.symeda.sormas.ui.utils.fields.ComboBox;

/**
 * Ticket 1b-13: as in Vaadin 7 ({@code DateTimeField.getInternalValue()} on {@code 8f4df2c}), the date-time field builds
 * a new {@link Date} from its date and time parts on every read, also when untouched: start of the day plus hour and
 * minute, seconds and milliseconds 0, JVM time zone. A form commit therefore writes a {@code java.util.Date}, never the
 * stored {@link Timestamp}, and {@code EventFacadeEjb}'s {@code Objects.equals(stored, written)} sees a change on every
 * event save, as in the old app.
 */
public class DateTimeFieldFreshValueTest {

	private static final ZoneId ZONE = ZoneId.systemDefault();

	private static Date date(LocalDateTime dateTime) {
		return Date.from(dateTime.atZone(ZONE).toInstant());
	}

	private static Timestamp stored(LocalDateTime dateTime, int nanos) {
		Timestamp timestamp = new Timestamp(date(dateTime).getTime());
		timestamp.setNanos(nanos);
		return timestamp;
	}

	private static DateTimeField factoryField() {
		return new SormasFieldGroupFieldFactory(new FieldVisibilityCheckers(), UiFieldAccessCheckers.getNoop())
			.createField(Date.class, DateTimeField.class);
	}

	private static BeanFieldGroup<EventDto> group(DateTimeField startDate, DateTimeField endDate) {
		BeanFieldGroup<EventDto> group = new BeanFieldGroup<>(EventDto.class);
		group.setFieldFactory(new SormasFieldGroupFieldFactory(new FieldVisibilityCheckers(), UiFieldAccessCheckers.getNoop()));
		group.bind(startDate, EventDto.START_DATE);
		group.bind(endDate, EventDto.END_DATE);
		return group;
	}

	private static ComboBox timeField(DateTimeField field) {
		return (ComboBox) ((HorizontalLayout) field.getContent()).getComponentAt(1);
	}

	/**
	 * The event form case: the stored start date (a {@link Timestamp} with seconds and fractions) is read, nobody
	 * touches it, the form is saved. The written value is a new {@code java.util.Date} truncated to the minute.
	 */
	@Test
	public void untouchedFieldWritesFreshDateTruncatedToMinute() {
		DateTimeField startDate = factoryField();
		DateTimeField endDate = factoryField();
		startDate.getContent();
		endDate.getContent();
		BeanFieldGroup<EventDto> group = group(startDate, endDate);

		Timestamp storedStart = stored(LocalDateTime.of(2026, 3, 4, 10, 37, 23), 456_789_000);
		EventDto event = EventDto.build();
		event.setStartDate(storedStart);
		event.setEndDate(null);
		group.setItemDataSource(event);
		assertFalse(group.isModified(), "reading is not a change");

		Date expected = date(LocalDateTime.of(2026, 3, 4, 10, 37, 0));
		Date first = startDate.getValue();
		Date second = startDate.getValue();
		assertEquals(Date.class, first.getClass(), "a java.util.Date, not the stored Timestamp");
		assertNotSame(storedStart, first);
		assertNotSame(first, second, "a new object on every read");
		assertEquals(expected, first, "seconds and milliseconds dropped");
		assertEquals(expected, second);
		assertNull(endDate.getValue());

		group.commit();
		Date written = event.getStartDate();
		assertEquals(Date.class, written.getClass());
		assertEquals(expected.getTime(), written.getTime());
		assertNull(event.getEndDate());
		// EventFacadeEjb.hasVaccinationReferenceChanged: existing (stored) value first
		assertFalse(Objects.equals(storedStart, written), "the backend sees a change, as in the old app");
	}

	/**
	 * Same value written when the stored value is already a whole minute: equal by time, but still a new {@link Date}
	 * (so {@code Timestamp.equals} is false), as in Vaadin 7.
	 */
	@Test
	public void wholeMinuteStaysEqualInTime() {
		DateTimeField startDate = factoryField();
		DateTimeField endDate = factoryField();
		startDate.getContent();
		endDate.getContent();
		BeanFieldGroup<EventDto> group = group(startDate, endDate);

		Timestamp storedStart = stored(LocalDateTime.of(2026, 3, 4, 23, 45, 0), 0);
		EventDto event = EventDto.build();
		event.setStartDate(storedStart);
		group.setItemDataSource(event);
		group.commit();

		assertEquals(storedStart.getTime(), event.getStartDate().getTime());
		assertFalse(Objects.equals(storedStart, event.getStartDate()));
	}

	/**
	 * Vaadin 7: without a time the value is the start of the date part's day; a time chosen later is added to it.
	 */
	@Test
	public void timePartChangesAreBuiltFromParts() {
		DateTimeField field = factoryField();
		field.getContent();
		field.setValue(stored(LocalDateTime.of(2026, 3, 4, 10, 37, 23), 0));

		ComboBox time = timeField(field);
		time.setValue(null);
		assertEquals(date(LocalDateTime.of(2026, 3, 4, 0, 0)), field.getValue());

		time.setValue(8 * 60 + 15);
		assertEquals(date(LocalDateTime.of(2026, 3, 4, 8, 15)), field.getValue());

		field.setValue(null);
		assertNull(field.getValue());
	}

	/**
	 * Vaadin 7: before the content exists (field never attached), the field returns the stored object. Creating the
	 * content later shows the stored value in the parts.
	 */
	@Test
	public void beforeContentTheStoredValue() {
		DateTimeField field = factoryField();
		Timestamp storedValue = stored(LocalDateTime.of(2026, 3, 4, 10, 37, 23), 0);
		field.setValue(storedValue);
		assertSame(storedValue, field.getValue());

		field.getContent();
		assertNotNull(field.getValue());
		assertEquals(10 * 60 + 37, timeField(field).getValue());
		assertEquals(date(LocalDateTime.of(2026, 3, 4, 10, 37)), field.getValue());
	}
}
