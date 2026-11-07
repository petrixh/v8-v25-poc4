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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.PropertyChangeDeniedException;

import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.sample.SampleDto;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.utils.AbstractForm.BeanFieldGroup;
import de.symeda.sormas.ui.utils.AbstractForm.BeanFieldGroup.FieldGroupInvalidValueException;
import de.symeda.sormas.ui.utils.InvalidValueException;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;

/**
 * Ticket 2a-15 (data loss): typed text a date field cannot parse makes a form commit fail, writes nothing and keeps the
 * stored value, as in Vaadin 7 ({@code com.vaadin.v7.ui.DateField.changeVariables} and {@code validate()}, 8.14.3: the
 * field value becomes {@code null}, but {@code validate()} throws {@code UnparsableDateString} with
 * "Date format not recognized" before any other check). Before the fix the empty value was written. Unparsable text in a
 * number field fails the commit in the conversion, as in V7.
 */
public class UnparsableInputCommitTest {

	private static final ZoneId ZONE = ZoneId.systemDefault();

	private static Date date(LocalDateTime dateTime) {
		return Date.from(dateTime.atZone(ZONE).toInstant());
	}

	private static SormasFieldGroupFieldFactory factory() {
		return new SormasFieldGroupFieldFactory(new FieldVisibilityCheckers(), UiFieldAccessCheckers.getNoop());
	}

	private static DateField dateField() {
		return factory().createField(Date.class, DateField.class);
	}

	private static <T> BeanFieldGroup<T> group(Class<T> beanType) {
		BeanFieldGroup<T> group = new BeanFieldGroup<>(beanType);
		group.setFieldFactory(factory());
		return group;
	}

	/**
	 * What the browser sends for typed text the date picker cannot parse: the input text, an empty value (a client-side
	 * change) and an {@code unparsable-change} event. Test-only use of Flow's node features to play the client.
	 */
	private static void userTypesUnparsable(DateField field, String text) {
		DatePicker picker = field.getDatePicker();
		picker.getElement().setProperty("_inputElementValue", text);
		try {
			picker.getElement().getNode().getFeature(ElementPropertyMap.class).deferredUpdateFromClient("value", "").run();
		} catch (PropertyChangeDeniedException e) {
			throw new IllegalStateException(e);
		}
		picker.getElement()
			.getNode()
			.getFeature(ElementListenerMap.class)
			.fireEvent(new DomEvent(picker.getElement(), "unparsable-change", JacksonUtils.createObjectNode()));
	}

	/**
	 * A day picked or typed correctly in the browser.
	 */
	private static void userPicks(DateField field, LocalDate day) {
		DatePicker picker = field.getDatePicker();
		LocalDate old = picker.getValue();
		picker.getElement().setProperty("_inputElementValue", day != null ? day.toString() : "");
		picker.setValue(day);
		ComponentUtil.fireEvent(picker, new ComponentValueChangeEvent<>(picker, picker, old, true));
	}

	private static SampleDto sampleReceived(Date receivedDate) {
		SampleDto sample = new SampleDto();
		sample.setReceived(true);
		sample.setReceivedDate(receivedDate);
		return sample;
	}

	private static String onlyCauseMessage(InvalidValueException e) {
		assertTrue(e instanceof FieldGroupInvalidValueException);
		assertEquals(1, e.getCauses().length);
		return e.getCauses()[0].getMessage();
	}

	/**
	 * The finding (sample, "Date sample received at lab"): unparsable text, Save twice. Both commits fail with the V7
	 * message and the stored date stays; the field's value is empty meanwhile, as in V7.
	 */
	@Test
	public void unparsableDateBlocksCommitAndKeepsStoredValue() {
		DateField receivedDate = dateField();
		BeanFieldGroup<SampleDto> group = group(SampleDto.class);
		group.bind(receivedDate, SampleDto.RECEIVED_DATE);
		Date stored = date(LocalDateTime.of(2026, 10, 3, 14, 5));
		SampleDto sample = sampleReceived(stored);
		group.setItemDataSource(sample);

		userTypesUnparsable(receivedDate, "10/3/2026x");
		assertNull(receivedDate.getValue());
		assertEquals(DateField.DEFAULT_PARSE_ERROR_MESSAGE, receivedDate.getUnparsableInputError());
		assertFalse(receivedDate.isValid());
		assertFalse(group.isValid());

		for (int save = 1; save <= 2; save++) {
			InvalidValueException e = assertThrows(InvalidValueException.class, group::commit);
			assertEquals("Date format not recognized", onlyCauseMessage(e));
			assertSame(stored, sample.getReceivedDate());
		}
		// the field shows the error, as V7 marked it
		assertTrue(receivedDate.isInvalid());
		assertEquals("Date format not recognized", receivedDate.getErrorMessage());
	}

	/**
	 * Corrected input saves; discard brings the stored value back and ends the error.
	 */
	@Test
	public void correctedDateSavesAndDiscardRestores() {
		DateField receivedDate = dateField();
		BeanFieldGroup<SampleDto> group = group(SampleDto.class);
		group.bind(receivedDate, SampleDto.RECEIVED_DATE);
		Date stored = date(LocalDateTime.of(2026, 10, 3, 14, 5));
		SampleDto sample = sampleReceived(stored);
		group.setItemDataSource(sample);

		userTypesUnparsable(receivedDate, "x");
		group.discard();
		assertNull(receivedDate.getUnparsableInputError());
		assertEquals(stored, receivedDate.getValue());
		assertTrue(group.isValid());

		userTypesUnparsable(receivedDate, "x");
		assertThrows(InvalidValueException.class, group::commit);
		userPicks(receivedDate, LocalDate.of(2026, 10, 4));
		assertNull(receivedDate.getUnparsableInputError());
		group.commit();
		// a day typed into an emptied field is the start of that day (1b-14)
		assertEquals(date(LocalDateTime.of(2026, 10, 4, 0, 0)), sample.getReceivedDate());
	}

	/**
	 * An empty field with unparsable text (no value change at all) blocks the commit too; clearing the text ends it and
	 * the empty value is written.
	 */
	@Test
	public void unparsableTextInEmptyField() {
		DateField receivedDate = dateField();
		BeanFieldGroup<SampleDto> group = group(SampleDto.class);
		group.bind(receivedDate, SampleDto.RECEIVED_DATE);
		SampleDto sample = sampleReceived(null);
		group.setItemDataSource(sample);

		userTypesUnparsable(receivedDate, "x");
		assertEquals("Date format not recognized", onlyCauseMessage(assertThrows(InvalidValueException.class, group::commit)));

		userPicks(receivedDate, null);
		assertNull(receivedDate.getUnparsableInputError());
		group.commit();
		assertNull(sample.getReceivedDate());
	}

	/**
	 * On a required field the parse error comes first (V7 order) and is shown, though the value is empty.
	 */
	@Test
	public void requiredFieldReportsParseError() {
		DateField receivedDate = dateField();
		receivedDate.setRequired(true);
		receivedDate.setRequiredError("required");
		BeanFieldGroup<SampleDto> group = group(SampleDto.class);
		group.bind(receivedDate, SampleDto.RECEIVED_DATE);
		Date stored = date(LocalDateTime.of(2026, 10, 3, 14, 5));
		SampleDto sample = sampleReceived(stored);
		group.setItemDataSource(sample);

		userTypesUnparsable(receivedDate, "x");
		InvalidValueException e = assertThrows(InvalidValueException.class, group::commit);
		assertEquals("Date format not recognized", onlyCauseMessage(e));
		assertTrue(receivedDate.isInvalid());
		assertSame(stored, sample.getReceivedDate());
		InvalidValueException direct = assertThrows(InvalidValueException.class, receivedDate::validateValue);
		assertEquals("Date format not recognized", direct.getMessage());

		receivedDate.setParseErrorMessage("own message");
		assertEquals("own message", onlyCauseMessage(assertThrows(InvalidValueException.class, group::commit)));
	}

	/**
	 * As the V7 popup date field, the calendar opens from its button only, not on typing: an overlay opened by typing
	 * took the next click (e.g. on Save) only to close itself.
	 */
	@Test
	public void calendarDoesNotOpenOnTyping() {
		assertFalse(dateField().getDatePicker().isAutoOpen());
		assertFalse(new PopupDateField().getDatePicker().isAutoOpen());
	}

	/**
	 * A date-time field (date part unparsable): the outer field does not look into its date part, as in V7, where
	 * {@code DateTimeField} (a V7 {@code CustomField}) did not validate its inner date field either. Its date part fails
	 * on its own.
	 */
	@Test
	public void dateTimeFieldDatePartOwnState() {
		DateField datePart = new PopupDateField();
		userTypesUnparsable(datePart, "x");
		assertFalse(datePart.isValid());
	}

	/**
	 * Number field: unparsable text fails the commit in the conversion (unchanged behaviour, checked here so it stays).
	 */
	@Test
	public void unparsableNumberBlocksCommit() {
		TextField approximateAge = factory().createField(Integer.class, TextField.class);
		BeanFieldGroup<PersonDto> group = group(PersonDto.class);
		group.bind(approximateAge, PersonDto.APPROXIMATE_AGE);
		PersonDto person = PersonDto.build();
		person.setApproximateAge(42);
		group.setItemDataSource(person);
		assertEquals("42", approximateAge.getValue());

		approximateAge.setValue("4x2");
		assertThrows(InvalidValueException.class, group::commit);
		assertEquals(Integer.valueOf(42), person.getApproximateAge());

		approximateAge.setValue("43");
		group.commit();
		assertEquals(Integer.valueOf(43), person.getApproximateAge());
	}
}
