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
package de.symeda.sormas.ui.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.person.Sex;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.backend.TestDataCreator;
import de.symeda.sormas.ui.AbstractUiBeanTest;

/**
 * Ticket 2b-04 (sweep 2b-002 RC2): with a similar person in the database, the "Pick or create person" field selects
 * "select a person" while it builds its content, and that listener calls {@code selectBestMatch()}. Since the 1a-51
 * port {@code selectBestMatch()} builds the content itself, so building it again recursed until a
 * {@code StackOverflowError} (seen in the case import, WF48 step 4). The field must build once and select the one
 * match, as in Vaadin 8.
 */
public class PersonSelectionFieldBestMatchTest extends AbstractUiBeanTest {

	@Test
	public void buildsOnceAndSelectsTheOneMatch() throws Exception {
		TestDataCreator.RDCF rdcf = creator.createRDCF();
		UserDto user = creator.createSurveillanceSupervisor(rdcf);
		loginWith(user);
		PersonDto existing = creator.createPerson("Import01", "Csvperson", Sex.FEMALE, 1971, 1, 1);
		creator.createCase(user.toReference(), existing.toReference(), rdcf);

		PersonDto newPerson = PersonDto.build();
		newPerson.setFirstName("Import01");
		newPerson.setLastName("Csvperson");
		newPerson.setSex(Sex.FEMALE);
		newPerson.setBirthdateYYYY(1971);
		newPerson.setBirthdateMM(1);
		newPerson.setBirthdateDD(1);

		PersonSelectionField field = new PersonSelectionField(newPerson, "info");
		// the constructor's match check needs the UI's user provider; the backend search itself runs as the user
		Field hasMatches = PersonSelectionField.class.getDeclaredField("hasMatches");
		hasMatches.setAccessible(true);
		hasMatches.setBoolean(field, true);

		// as DataImporter.handlePersonSimilarity and PersonController.selectOrCreatePerson
		field.selectBestMatch();

		assertNotNull(field.getContent());
		assertSame(field.getContent(), field.mainLayout);
		assertNotNull(field.getValue(), "the one match is selected");
		assertEquals(existing.getUuid(), field.getValue().getUuid());
	}
}
