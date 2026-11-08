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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import de.symeda.sormas.api.sample.SampleDto;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.utils.AbstractForm.BeanFieldGroup;
import de.symeda.sormas.ui.utils.InvalidValueException;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;

/**
 * Ticket 2b-01 (sweep 2b-001, WF03 step 2): after a refused Save the empty required fields show the error state, as
 * Vaadin 7 did. Vaadin 7 (8.14.3) painted a field's validation error each time it painted the field
 * ({@code AbstractField.getErrorMessage()}): while validation is visible and {@code validate()} fails with a message. An
 * empty required field got the error style without the error indicator ({@code shouldHideErrors()}), and only with a
 * non-empty required error ({@code InvalidValueException.isInvisible()}). Forms that call
 * {@code hideValidationUntilNextCommit()} turn validation visible in their pre-commit handler.
 * <p>
 * The tests attach the fields to a UI and run its before-client-response callbacks, where the end of a request runs
 * them ({@link #endOfRequest()}); test-only use of Flow internals (UI internals, state tree) to play the request.
 */
public class RequiredFieldFeedbackTest {

	private UI ui;

	@BeforeEach
	public void setUp() {
		ui = new UI();
		// test only: a session that counts as locked, so the UI runs its before-client-response callbacks
		VaadinService service = Mockito.mock(VaadinService.class);
		Mockito.when(service.getDeploymentConfiguration()).thenReturn(Mockito.mock(DeploymentConfiguration.class));
		VaadinSession session = new VaadinSession(service) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean hasLock() {
				return true;
			}

			@Override
			public void checkHasLock() {
			}
		};
		ui.getInternals().setSession(session);
		UI.setCurrent(ui);
	}

	@AfterEach
	public void tearDown() {
		UI.setCurrent(null);
	}

	private static void assertNoMessage(String errorMessage) {
		assertTrue(errorMessage == null || errorMessage.isEmpty(), errorMessage);
	}

	private void endOfRequest() {
		ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
	}

	private static SormasFieldGroupFieldFactory factory() {
		return new SormasFieldGroupFieldFactory(new FieldVisibilityCheckers(), UiFieldAccessCheckers.getNoop());
	}

	private TextField requiredText(String requiredError) {
		TextField field = factory().createField(String.class, TextField.class);
		field.setRequired(true);
		field.setRequiredError(requiredError);
		ui.add(field);
		return field;
	}

	private DateField requiredDate(String requiredError) {
		DateField field = factory().createField(Date.class, DateField.class);
		field.setRequired(true);
		field.setRequiredError(requiredError);
		ui.add(field);
		return field;
	}

	private static BeanFieldGroup<SampleDto> group() {
		BeanFieldGroup<SampleDto> group = new BeanFieldGroup<>(SampleDto.class);
		group.setFieldFactory(factory());
		return group;
	}

	/**
	 * What {@code AbstractEditForm.hideValidationUntilNextCommit()} and its pre-commit handler do.
	 */
	private static void hideValidationUntilNextCommit(BeanFieldGroup<?> group) {
		group.getFields().forEach(f -> f.setValidationVisible(false));
		group.addCommitHandler(new BeanFieldGroup.CommitHandler() {

			private static final long serialVersionUID = 1L;

			@Override
			public void preCommit(BeanFieldGroup.CommitEvent commitEvent) {
				group.getFields().forEach(f -> f.setValidationVisible(true));
			}

			@Override
			public void postCommit(BeanFieldGroup.CommitEvent commitEvent) {
			}
		});
	}

	/**
	 * WF03 step 2 (a create form): no error state on open; a refused Save marks the empty required fields, without a
	 * message under the field; filling one clears its mark, the others stay.
	 */
	@Test
	public void refusedSaveMarksEmptyRequiredFields() {
		TextField fieldSampleId = requiredText("Field sample ID");
		DateField receivedDate = requiredDate("Date sample received at lab");
		BeanFieldGroup<SampleDto> group = group();
		group.bind(fieldSampleId, SampleDto.FIELD_SAMPLE_ID);
		group.bind(receivedDate, SampleDto.RECEIVED_DATE);
		hideValidationUntilNextCommit(group);
		SampleDto sample = new SampleDto();
		group.setItemDataSource(sample);
		endOfRequest();
		assertFalse(fieldSampleId.isInvalid());
		assertFalse(receivedDate.isInvalid());

		// typing and emptying again before the first Save shows nothing yet
		fieldSampleId.setValue("x");
		fieldSampleId.setValue("");
		endOfRequest();
		assertFalse(fieldSampleId.isInvalid());

		assertThrows(InvalidValueException.class, group::commit);
		endOfRequest();
		assertTrue(fieldSampleId.isInvalid());
		assertTrue(receivedDate.isInvalid());
		assertNoMessage(fieldSampleId.getErrorMessage());
		assertNoMessage(receivedDate.getErrorMessage());
		assertNull(sample.getFieldSampleID());

		fieldSampleId.setValue("S-1");
		endOfRequest();
		assertFalse(fieldSampleId.isInvalid());
		assertTrue(receivedDate.isInvalid());

		// emptied again after a Save: marked again at once, as V7 repainted it
		fieldSampleId.setValue("");
		assertTrue(fieldSampleId.isInvalid());
	}

	/**
	 * A pre-commit handler that refuses the commit (AbstractEditForm: an invalid custom field) stops the commit before
	 * the bound fields are validated; Vaadin 7 still painted their errors afterwards.
	 */
	@Test
	public void refusedPreCommitMarksBoundFields() {
		TextField fieldSampleId = requiredText("Field sample ID");
		BeanFieldGroup<SampleDto> group = group();
		group.bind(fieldSampleId, SampleDto.FIELD_SAMPLE_ID);
		hideValidationUntilNextCommit(group);
		group.addCommitHandler(new BeanFieldGroup.CommitHandler() {

			private static final long serialVersionUID = 1L;

			@Override
			public void preCommit(BeanFieldGroup.CommitEvent commitEvent) {
				throw new InvalidValueException("Place of stay");
			}

			@Override
			public void postCommit(BeanFieldGroup.CommitEvent commitEvent) {
			}
		});
		group.setItemDataSource(new SampleDto());
		endOfRequest();
		assertFalse(fieldSampleId.isInvalid());

		InvalidValueException e = assertThrows(InvalidValueException.class, group::commit);
		assertEquals("Place of stay", e.getMessage());
		endOfRequest();
		assertTrue(fieldSampleId.isInvalid());
	}

	/**
	 * A form that does not hide validation (e.g. the sample create form): Vaadin 7 marked its empty required fields when
	 * it showed the form.
	 */
	@Test
	public void visibleValidationMarksOnOpen() {
		DateField receivedDate = requiredDate("Date sample received at lab");
		BeanFieldGroup<SampleDto> group = group();
		group.bind(receivedDate, SampleDto.RECEIVED_DATE);
		group.setItemDataSource(new SampleDto());
		endOfRequest();
		assertTrue(receivedDate.isInvalid());
		assertNoMessage(receivedDate.getErrorMessage());

		// no longer required: the mark goes (V7 painted the field again)
		receivedDate.setRequired(false);
		endOfRequest();
		assertFalse(receivedDate.isInvalid());
	}

	/**
	 * Cases V7 did not mark: an empty required error (an invisible exception), a read-only field, hidden validation.
	 */
	@Test
	public void notMarked() {
		TextField noRequiredError = requiredText("");
		DateField readOnly = requiredDate("Date sample received at lab");
		readOnly.setReadOnly(true);
		BeanFieldGroup<SampleDto> group = group();
		group.bind(noRequiredError, SampleDto.FIELD_SAMPLE_ID);
		group.bind(readOnly, SampleDto.RECEIVED_DATE);
		group.setItemDataSource(new SampleDto());
		endOfRequest();
		assertThrows(InvalidValueException.class, group::commit);
		endOfRequest();
		assertFalse(noRequiredError.isInvalid());
		assertFalse(readOnly.isInvalid());

		TextField hidden = requiredText("Comment");
		hidden.setValidationVisible(false);
		BeanFieldGroup<SampleDto> other = group();
		other.bind(hidden, SampleDto.COMMENT);
		other.setItemDataSource(new SampleDto());
		assertThrows(InvalidValueException.class, other::commit);
		endOfRequest();
		assertFalse(hidden.isInvalid());
	}

	/**
	 * An error a form sets itself (e.g. a duplicate external token, ValidationUtils) is not cleared by the repaint of a
	 * valid field; a validator error shows its message, as before.
	 */
	@Test
	public void ownErrorKeptAndValidatorMessageShown() {
		TextField fieldSampleId = requiredText("Field sample ID");
		BeanFieldGroup<SampleDto> group = group();
		group.bind(fieldSampleId, SampleDto.FIELD_SAMPLE_ID);
		SampleDto sample = new SampleDto();
		sample.setFieldSampleID("S-1");
		group.setItemDataSource(sample);
		fieldSampleId.setErrorMessage("duplicate");
		fieldSampleId.setInvalid(true);
		endOfRequest();
		assertTrue(fieldSampleId.isInvalid());
		assertEquals("duplicate", fieldSampleId.getErrorMessage());

		fieldSampleId.addValidator(value -> {
			if ("bad".equals(value)) {
				throw new Field.InvalidValueException("not bad");
			}
		});
		fieldSampleId.setValue("bad");
		assertTrue(fieldSampleId.isInvalid());
		assertEquals("not bad", fieldSampleId.getErrorMessage());
	}
}
