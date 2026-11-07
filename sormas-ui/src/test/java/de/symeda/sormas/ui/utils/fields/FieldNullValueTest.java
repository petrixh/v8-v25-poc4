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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.person.Sex;
import de.symeda.sormas.api.sample.PathogenTestType;
import de.symeda.sormas.api.sample.SampleDto;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.utils.AbstractForm.BeanFieldGroup;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;

/**
 * Ticket 1b-12: a form writes back {@code null} for a field read from a {@code null} property that nobody touched, as
 * Vaadin 7 did (8.14.3 {@code AbstractField.setPropertyDataSource/commit}, {@code AbstractTextField.changeVariables}).
 * Text a user clears is written as {@code ""} (null setting not allowed), typed text as the text.
 */
public class FieldNullValueTest {

	private static <T> BeanFieldGroup<T> group(Class<T> beanType, boolean buffered) {
		BeanFieldGroup<T> group = new BeanFieldGroup<>(beanType);
		group.setFieldFactory(new SormasFieldGroupFieldFactory(new FieldVisibilityCheckers(), UiFieldAccessCheckers.getNoop()));
		group.setBuffered(buffered);
		return group;
	}

	private static TextField factoryTextField() {
		return new SormasFieldGroupFieldFactory(new FieldVisibilityCheckers(), UiFieldAccessCheckers.getNoop())
			.createField(String.class, TextField.class);
	}

	/**
	 * Person form round trip, the 1b-11 finding: only one field edited, the other empty text fields keep {@code null}.
	 */
	@Test
	public void untouchedTextFieldsWriteNull() {
		for (boolean buffered : new boolean[] {
			true,
			false }) {
			BeanFieldGroup<PersonDto> group = group(PersonDto.class, buffered);
			TextField externalId = factoryTextField();
			TextField externalToken = factoryTextField();
			TextField passportNumber = factoryTextField();
			TextArea additionalDetails = new TextArea();
			additionalDetails.setNullRepresentation("");
			group.bind(externalId, PersonDto.EXTERNAL_ID);
			group.bind(externalToken, PersonDto.EXTERNAL_TOKEN);
			group.bind(passportNumber, PersonDto.PASSPORT_NUMBER);
			group.bind(additionalDetails, PersonDto.ADDITIONAL_DETAILS);

			PersonDto person = PersonDto.build();
			person.setPassportNumber("P1");
			group.setItemDataSource(person);

			assertNull(externalId.getValue());
			assertTrue(externalId.isEmpty());
			assertEquals("", externalId.getElement().getProperty("value", ""), "shows the null representation");
			assertNull(additionalDetails.getValue());
			assertFalse(group.isModified(), "reading is not a change");

			// the user types into one field only
			externalToken.setModelValue("T1", true);
			group.commit();

			String mode = buffered ? "buffered" : "unbuffered";
			assertNull(person.getExternalId(), mode);
			assertEquals("T1", person.getExternalToken(), mode);
			assertEquals("P1", person.getPassportNumber(), mode);
			assertNull(person.getAdditionalDetails(), mode);
		}
	}

	/**
	 * V7 {@code changeVariables}: clearing a field that had a value writes {@code ""}; with null setting allowed it
	 * writes {@code null}. Entering the null representation into a {@code null} field keeps {@code null}, with no event.
	 */
	@Test
	public void clientTextFollowsV7Rule() {
		BeanFieldGroup<PersonDto> group = group(PersonDto.class, true);
		TextField cleared = factoryTextField();
		TextField clearedNullAllowed = factoryTextField();
		clearedNullAllowed.setNullSettingAllowed(true);
		TextField emptyStaysNull = factoryTextField();
		PasswordField typed = new PasswordField();
		typed.setNullRepresentation("");
		group.bind(cleared, PersonDto.EXTERNAL_ID);
		group.bind(clearedNullAllowed, PersonDto.EXTERNAL_TOKEN);
		group.bind(emptyStaysNull, PersonDto.PASSPORT_NUMBER);
		group.bind(typed, PersonDto.ADDITIONAL_DETAILS);

		PersonDto person = PersonDto.build();
		person.setExternalId("E1");
		person.setExternalToken("T1");
		group.setItemDataSource(person);

		List<Object> events = new ArrayList<>();
		emptyStaysNull.addValueChangeListener(e -> events.add(e.getValue()));

		cleared.setModelValue("", true);
		clearedNullAllowed.setModelValue("", true);
		emptyStaysNull.setModelValue("", true);
		typed.setModelValue("secret", true);
		assertEquals("", cleared.getValue());
		assertNull(clearedNullAllowed.getValue());
		assertNull(emptyStaysNull.getValue());
		assertEquals(Collections.emptyList(), events);

		group.commit();
		assertEquals("", person.getExternalId());
		assertNull(person.getExternalToken());
		assertNull(person.getPassportNumber());
		assertEquals("secret", person.getAdditionalDetails());

		// typed into a null field, then cleared: "" as in V7
		emptyStaysNull.setModelValue("x", true);
		emptyStaysNull.setModelValue("", true);
		assertEquals("", emptyStaysNull.getValue());
		assertEquals(Arrays.asList("x", ""), events);
	}

	/**
	 * Programmatic values as in V7: a new field is {@code ""} (V7 {@code clear()} in the constructor), {@code clear()}
	 * sets {@code ""}, {@code setValue(null)} sets {@code null} and fires an event although the field looks the same.
	 */
	@Test
	public void programmaticNullAndEmpty() {
		for (AbstractTextField field : Arrays.asList(new TextField(), new TextArea(), new PasswordField())) {
			String type = field.getClass().getSimpleName();
			assertEquals("", field.getValue(), type);
			List<Object> events = new ArrayList<>();
			field.addValueChangeListener(e -> events.add(String.valueOf(e.getValue())));

			field.setValue(null);
			assertNull(field.getValue(), type);
			assertTrue(field.isEmpty(), type);
			assertTrue(field.isValid(), type);
			field.setValue(null);
			field.setValue("a");
			field.clear();
			assertEquals("", field.getValue(), type);
			assertEquals(Arrays.asList("null", "a", ""), events, type);

			field.setNullRepresentation("-");
			field.setValue(null);
			assertNull(field.getValue(), type);
			assertEquals("-", field.getElement().getProperty("value", ""), type);
			field.setRequired(true);
			assertFalse(field.isValid(), "required and null: " + type);
		}
		assertNull(new TextField("caption", null).getValue());
		assertEquals("v", new TextField("caption", "v").getValue());
	}

	@Test
	public void richTextAreaKeepsNull() {
		BeanFieldGroup<PersonDto> group = group(PersonDto.class, true);
		SormasRichTextArea area = new SormasRichTextArea();
		area.setNullRepresentation("");
		group.bind(area, PersonDto.ADDITIONAL_DETAILS);
		PersonDto person = PersonDto.build();
		group.setItemDataSource(person);
		assertNull(area.getValue());
		assertTrue(area.isEmpty());
		group.commit();
		assertNull(person.getAdditionalDetails());
	}

	@Test
	public void clientRule() {
		TextField field = new TextField();
		assertNull(AbstractTextField.valueFromClient(field, "", true));
		assertEquals("", AbstractTextField.valueFromClient(field, "", false));
		assertEquals("a", AbstractTextField.valueFromClient(field, "a", true));
		field.setNullSettingAllowed(true);
		assertNull(AbstractTextField.valueFromClient(field, "", false));
		field.setNullRepresentation("-");
		assertEquals("", AbstractTextField.valueFromClient(field, "", true));
		assertNull(AbstractTextField.valueFromClient(field, "-", false));
	}

	/**
	 * The other field types of a person form keep an untouched {@code null} too: a number in a text field (default
	 * converter), a date, a combo box.
	 */
	@Test
	public void otherFieldTypesWriteNull() {
		BeanFieldGroup<PersonDto> group = group(PersonDto.class, true);
		TextField birthWeight = factoryTextField();
		DateField deathDate = new DateField();
		ComboBox sex = new ComboBox();
		sex.addItems(Arrays.asList(Sex.values()));
		group.bind(birthWeight, PersonDto.BIRTH_WEIGHT);
		group.bind(deathDate, PersonDto.DEATH_DATE);
		group.bind(sex, PersonDto.SEX);

		PersonDto person = PersonDto.build();
		group.setItemDataSource(person);
		assertNull(birthWeight.getValue());
		group.commit();
		assertNull(person.getBirthWeight());
		assertNull(person.getDeathDate());
		assertNull(person.getSex());

		birthWeight.setModelValue("3200", true);
		group.commit();
		assertEquals(Integer.valueOf(3200), person.getBirthWeight());
		birthWeight.setModelValue("", true);
		group.commit();
		assertNull(person.getBirthWeight(), "a cleared number is null (converter), as in V7");

		Date date = new Date(0);
		person.setDeathDate(date);
		group.setItemDataSource(person);
		group.commit();
		assertEquals(date, person.getDeathDate());
	}

	/**
	 * V7 multi select: read from a {@code null} set, {@code getValue()} is an empty set but the commit writes
	 * {@code null}; once the value changes the set is written ({@code clear()} and {@code setValue(null)} give an empty
	 * set). A {@code null} check box stays {@code null} (1b-05).
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void multiSelectAndCheckBoxWriteNull() {
		BeanFieldGroup<SampleDto> group = group(SampleDto.class, true);
		OptionGroup requestedTests = new OptionGroup();
		group.bind(requestedTests, SampleDto.REQUESTED_PATHOGEN_TESTS);
		requestedTests.setMultiSelect(true);
		requestedTests.addItems(Arrays.asList(PathogenTestType.values()));
		CheckBox testingRequested = new CheckBox();
		group.bind(testingRequested, SampleDto.PATHOGEN_TESTING_REQUESTED);

		SampleDto sample = new SampleDto();
		group.setItemDataSource(sample);
		assertEquals(Collections.emptySet(), requestedTests.getValue());
		group.commit();
		assertNull(sample.getRequestedPathogenTests());
		assertNull(sample.getPathogenTestingRequested());

		requestedTests.setValue(new LinkedHashSet<>(Collections.singleton(PathogenTestType.PCR_RT_PCR)));
		group.commit();
		assertEquals(Collections.singleton(PathogenTestType.PCR_RT_PCR), sample.getRequestedPathogenTests());

		requestedTests.clear();
		group.commit();
		assertEquals(Collections.emptySet(), sample.getRequestedPathogenTests());

		sample.setRequestedPathogenTests(null);
		group.setItemDataSource(sample);
		requestedTests.setValue(null);
		group.commit();
		assertEquals(Collections.emptySet(), sample.getRequestedPathogenTests(), "programmatic null is an empty set (V7)");

		sample.setRequestedPathogenTests(new LinkedHashSet<>(Collections.singleton(PathogenTestType.CULTURE)));
		group.setItemDataSource(sample);
		group.commit();
		assertEquals(Set.of(PathogenTestType.CULTURE), sample.getRequestedPathogenTests());
	}
}
