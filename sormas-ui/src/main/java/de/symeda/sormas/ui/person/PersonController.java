/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.ui.person;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseClassification;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.caze.CaseOutcome;
import de.symeda.sormas.api.event.EventParticipantSelectionDto;
import de.symeda.sormas.api.event.EventReferenceDto;
import de.symeda.sormas.api.externaljournal.ExternalJournalSyncResponseDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.person.CauseOfDeath;
import de.symeda.sormas.api.person.PersonContext;
import de.symeda.sormas.api.person.PersonCriteria;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.person.PersonFacade;
import de.symeda.sormas.api.person.PersonHelper;
import de.symeda.sormas.api.person.PersonIndexDto;
import de.symeda.sormas.api.person.PersonReferenceDto;
import de.symeda.sormas.api.person.PresentCondition;
import de.symeda.sormas.api.person.SimilarPersonDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.ValidationRuntimeException;
import de.symeda.sormas.ui.SormasUI;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.caze.CaseDataView;
import de.symeda.sormas.ui.events.eventParticipantMerge.PickLeadEventParticipant;
import de.symeda.sormas.ui.navigation.Navigator;
import de.symeda.sormas.ui.utils.AbstractView;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent;
import de.symeda.sormas.ui.utils.ConfirmationComponent;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.ViewMode;
import de.symeda.sormas.ui.utils.components.page.title.TitleLayout;
import de.symeda.sormas.ui.utils.components.page.title.TitleLayoutHelper;
import de.symeda.sormas.ui.utils.fields.Field;

public class PersonController {

	private PersonFacade personFacade = FacadeProvider.getPersonFacade();

	public PersonController() {
	}

	public void registerViews(Navigator navigator) {
		navigator.addView(PersonsView.VIEW_NAME, PersonsView.class);
		navigator.addView(PersonDataView.VIEW_NAME, PersonDataView.class);
		navigator.addView(CaseDataView.VIEW_NAME, CaseDataView.class);
	}

	public TitleLayout getPersonViewTitleLayout(PersonDto personDto) {
		final TitleLayout titleLayout = new TitleLayout();

		final String shortUuid = DataHelper.getShortUuid(personDto.getUuid());
		final StringBuilder mainRowText = TitleLayoutHelper.buildPersonString(personDto);
		mainRowText.append(mainRowText.length() > 0 ? " (" + shortUuid + ")" : shortUuid);
		titleLayout.addMainRow(mainRowText.toString());

		return titleLayout;
	}

	public void mergePersons(PersonIndexDto person1, PersonIndexDto person2) {
		final PersonGrid personGrid = new PersonGrid();
		personGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
		final List<PersonIndexDto> persons = Arrays.asList(person1, person2);
		personGrid.setFixDataProvider(persons);
		personGrid.setVisible(true);
		personGrid.setWidth(100, Unit.PERCENTAGE);

		final Dialog popupWindow = VaadinUiUtil.createPopupWindow();
		popupWindow.setWidth("1024px");
		popupWindow.setHeaderTitle(I18nProperties.getString(Strings.headingPickOrMergePerson));

		final VerticalLayout layout = new VerticalLayout();
		// was setMargin(true): the Vaadin 8 margin is the padding in Flow
		layout.setPadding(true);
		layout.add(VaadinUiUtil.createInfoComponent(I18nProperties.getString(Strings.infoPersonMergeDescription)));
		layout.add(personGrid);

		final ConfirmationComponent confirmationComponent = new ConfirmationComponent(false) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfirm() {
				PersonController.this.mergePersons(personGrid, persons, popupWindow, true);
			}

			@Override
			protected void onCancel() {
				PersonController.this.mergePersons(personGrid, persons, popupWindow, false);
			}
		};

		final Button mergeButton = confirmationComponent.getConfirmButton();
		mergeButton.setText(I18nProperties.getCaption(Captions.actionMerge));
		mergeButton.setEnabled(false);
		final Button pickButton = confirmationComponent.getCancelButton();
		pickButton.setText(I18nProperties.getCaption(Captions.actionPick));
		CssStyles.removeStyles(pickButton, CssStyles.BUTTON_LINK);
		CssStyles.style(pickButton, CssStyles.BUTTON_PRIMARY);
		pickButton.setEnabled(false);

		personGrid.addItemClickListener(itemClick -> {
			if (itemClick.getClickCount() <= 1) {
				boolean enabled = personGrid.getSelectedItems().isEmpty()
					|| !DataHelper.equal(itemClick.getItem(), personGrid.getSelectedItems().stream().findFirst().get());
				mergeButton.setEnabled(enabled);
				pickButton.setEnabled(enabled);
			}
		});

		confirmationComponent.addExtraButton(
			ButtonHelper
				.createButton(Strings.unsavedChanges_cancel, I18nProperties.getCaption(Captions.actionDiscard), null, CssStyles.BUTTON_PRIMARY),
			buttonEvent -> popupWindow.close());

		layout.add(confirmationComponent);
		layout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, confirmationComponent);
		layout.setWidth(100, Unit.PERCENTAGE);
		layout.setSpacing(true);
		popupWindow.add(layout);
		VaadinUiUtil.setClosable(popupWindow, false);

		popupWindow.open();
	}

	private void mergePersons(PersonGrid personGrid, List<PersonIndexDto> personIndexDtos, Dialog popupWindow, boolean mergeProperties) {

		final Set<PersonIndexDto> selectedItems = personGrid.getSelectedItems();
		final PersonIndexDto leadPerson = selectedItems.iterator().next();
		final PersonIndexDto otherPerson = personIndexDtos.stream().filter(p -> p.getUuid() != leadPerson.getUuid()).findFirst().get();

		final String confirmationMessage;

		if (FacadeProvider.getPersonFacade().isSharedOrReceived(otherPerson.getUuid())) {
			if (FacadeProvider.getPersonFacade().isSharedOrReceived(leadPerson.getUuid())) {
				confirmationMessage = I18nProperties.getString(Strings.infoPersonMergeConfirmationBothShared);
			} else {
				NotificationHelper.show(
					I18nProperties.getString(Strings.headingMergePersonError),
					I18nProperties.getString(Strings.infoPersonMergeSharedMustLead),
					Type.ERROR_MESSAGE,
					false);
				return;
			}
		} else {
			confirmationMessage = I18nProperties.getString(Strings.infoPersonMergeConfirmation);
		}

		String firstPersonUuid = personIndexDtos.get(0).getUuid();
		String secondPersonUuid = personIndexDtos.get(1).getUuid();

		List<EventParticipantSelectionDto> eventParticipantsWithSameEvent =
			FacadeProvider.getEventParticipantFacade().getEventParticipantsWithSameEvent(firstPersonUuid, secondPersonUuid);

		Set<String> eventsWithMoreDuplicateThanAllowed = eventParticipantsWithSameEvent.stream()
			.collect(Collectors.groupingBy(EventParticipantSelectionDto::getEvent, Collectors.counting()))
			.entrySet()
			.stream()
			.filter(entry -> entry.getValue() > 2)
			.map(Map.Entry::getKey)
			.map(EventReferenceDto::getUuid)
			.collect(Collectors.toSet());

		if (!eventsWithMoreDuplicateThanAllowed.isEmpty()) {
			VerticalLayout popUpContent = new VerticalLayout();
			popUpContent.add(
				VaadinUiUtil
					.createInfoComponent(I18nProperties.getString(Strings.infoPickorMergeEventParticipantDuplicateEventParticipantByPersonByEvent)));
			eventsWithMoreDuplicateThanAllowed.forEach(eventUuid -> {
				Span eventUuidLabel = new Span(eventUuid);
				popUpContent.add(eventUuidLabel);
			});

			Dialog popupMoreDuplicatesThanAllowedWindow = VaadinUiUtil
				.showPopupWindow(popUpContent, I18nProperties.getString(Strings.headingMergeDuplicateEventParticipantSamePersonSameEvent));

			ComponentEventListener<ClickEvent<Button>> cancelListener = clickEvent -> {
				popupMoreDuplicatesThanAllowedWindow.close();
			};

			popUpContent.setSpacing(false);

			Button cancelButton = ButtonHelper.createButton(Captions.actionCancel, cancelListener);
			popUpContent.add(cancelButton);
			popUpContent.setHorizontalComponentAlignment(FlexComponent.Alignment.END, cancelButton);

			return;
		}

		if (!eventParticipantsWithSameEvent.isEmpty()) {
			if (UiUtil.permitted(UserRight.EVENTPARTICIPANT_VIEW)) {
				selectLeadEventParticipantByEvent(
					eventParticipantsWithSameEvent,
					(selectedEventParticipants, mergeEventParticipantProperties) -> pickOrMergeConfirmationPopUp(
						popupWindow,
						mergeProperties,
						leadPerson,
						otherPerson,
						confirmationMessage,
						selectedEventParticipants,
						mergeEventParticipantProperties));
				return;
			} else {
				VaadinUiUtil.showSimplePopupWindow(
					I18nProperties.getString(Strings.headingMergePersonError),
					I18nProperties.getString(Strings.messagePersonMergeNoEventParticipantRights));
				return;
			}
		}

		pickOrMergeConfirmationPopUp(popupWindow, mergeProperties, leadPerson, otherPerson, confirmationMessage, new ArrayList<>(), false);
	}

	private Dialog pickOrMergeConfirmationPopUp(
		Dialog popupWindow,
		boolean mergePersonProperties,
		PersonIndexDto leadPerson,
		PersonIndexDto otherPerson,
		String confirmationMessage,
		List<String> selectedEventParticipantUuids,
		boolean mergeEventParticipantProperties) {
		Dialog mergeWindow = VaadinUiUtil.showConfirmationPopup(
			I18nProperties.getString(Strings.headingPickOrMergePersonConfirmation),
			VaadinUiUtil.createLabel(confirmationMessage, ContentMode.HTML),
			I18nProperties.getCaption(Captions.actionConfirm),
			I18nProperties.getCaption(Captions.actionDiscard),
			800,
			confirm -> {
				if (Boolean.TRUE.equals(confirm)) {
					FacadeProvider.getPersonFacade()
						.mergePerson(
							leadPerson.getUuid(),
							otherPerson.getUuid(),
							mergePersonProperties,
							selectedEventParticipantUuids,
							mergeEventParticipantProperties);
					popupWindow.close();
					SormasUI.refreshView();
				}
			});

		return mergeWindow;
	}

	private void selectLeadEventParticipantByEvent(
		List<EventParticipantSelectionDto> eventParticipantSelectionDtos,
		BiConsumer<List<String>, Boolean> callback) {

		PickLeadEventParticipant pickLeadEventParticipant = new PickLeadEventParticipant(eventParticipantSelectionDtos);

		final CommitDiscardWrapperComponent<PickLeadEventParticipant> component = new CommitDiscardWrapperComponent<>(pickLeadEventParticipant);

		component.getCommitButton().setText(I18nProperties.getCaption((Captions.actionPick)));

		final Button mergeButton = ButtonHelper.createButton(Captions.actionMerge);
		component.getButtonsPanel().add(mergeButton);
		CssStyles.removeStyles(mergeButton, CssStyles.BUTTON_LINK);
		CssStyles.style(mergeButton, CssStyles.BUTTON_PRIMARY);

		mergeButton.addClickListener(clickEvent -> {
			pickLeadEventParticipant.setPickOrMerge(PickLeadEventParticipant.PickOrMerge.MERGE);
			component.commit();
		});

		component.setPreCommitListener(preCommitCallback -> {
			List<String> pickedEventParticipants = component.getWrappedComponent().getValue();

			if (eventParticipantSelectionDtos.size() / 2 > pickedEventParticipants.size()) {
				VaadinUiUtil.showSimplePopupWindow(
					I18nProperties.getString(Strings.headingPickEventParticipantsIncompleteSelection),
					I18nProperties.getString(Strings.messagePickEventParticipantsIncompleteSelection));
			} else {
				preCommitCallback.run();
			}
		});

		component.addCommitListener(() -> {
			List<String> pickedEventParticipants = component.getWrappedComponent().getValue();
			callback.accept(pickedEventParticipants, pickLeadEventParticipant.getPickOrMerge() == PickLeadEventParticipant.PickOrMerge.MERGE);
		});

		component.setWidth(1500, Unit.PIXELS);

		VaadinUiUtil.showModalPopupWindow(component, I18nProperties.getString(Strings.headingPickEventParticipants));
	}

	public void selectOrCreatePerson(final PersonDto person, String infoText, Consumer<PersonReferenceDto> resultConsumer, boolean saveNewPerson) {
		selectOrCreatePerson(person, infoText, resultConsumer, null, saveNewPerson, null);
	}

	/**
	 * Provides a PersonSelectionField to be able to decide to pick an existing Person from the system
	 * or to create a new one with the given information.
	 * 
	 * @param person
	 *            Dto wich contains the information about the person who should be created.
	 * @param infoText
	 *            Information that is shown to the user.
	 * @param resultConsumer
	 *            Is the operation that is executed after this mehtod.
	 * @param saveNewPerson
	 *            Indicates if the new person should be saved.
	 * @param infoTextWithoutMatches
	 *            Information that should be shown to the user if the window is still shown
	 *            if there are no matches found by the system.
	 */
	public void selectOrCreatePerson(
		final PersonDto person,
		String infoText,
		Consumer<PersonReferenceDto> resultConsumer,
		Runnable discardCallback,
		boolean saveNewPerson,
		String infoTextWithoutMatches) {

		PersonSelectionField personSelect = new PersonSelectionField(person, infoText, infoTextWithoutMatches);
		personSelect.setWidth(1024, Unit.PIXELS);

		if (StringUtils.isNotBlank(infoTextWithoutMatches) || personSelect.hasMatches()) {
			// TODO add user right parameter
			final CommitDiscardWrapperComponent<PersonSelectionField> component =
				new CommitDiscardWrapperComponent<PersonSelectionField>(personSelect);
			if (!saveNewPerson) {
				component.getCommitButton().setText(I18nProperties.getCaption(Captions.actionConfirm));
			}
			component.addCommitListener(() -> {
				SimilarPersonDto selectedPerson = personSelect.getValue();
				if (selectedPerson != null) {
					if (resultConsumer != null) {
						resultConsumer.accept(selectedPerson.toReference());
					}
				} else {
					PersonDto savedPerson;
					if (saveNewPerson) {
						savedPerson = personFacade.save(person);
					} else {
						savedPerson = person;
					}
					resultConsumer.accept(savedPerson.toReference());
				}
			});

			component.addDiscardListener(() -> {
				if (discardCallback != null) {
					discardCallback.run();
				}
			});

			personSelect.setSelectionChangeCallback((commitAllowed) -> {
				component.getCommitButton().setEnabled(commitAllowed);
			});

			VaadinUiUtil.showModalPopupWindow(component, I18nProperties.getString(Strings.headingPickOrCreatePerson), true);
			personSelect.selectBestMatch();
		} else if (saveNewPerson) {
			// no duplicate persons found so save a new person
			PersonDto savedPerson = personFacade.save(person);
			resultConsumer.accept(savedPerson.toReference());
		} else {
			resultConsumer.accept(person.toReference());
		}
	}

	public CommitDiscardWrapperComponent<PersonEditForm> getPersonEditComponent(String personUuid, boolean isEditAllowed) {
		PersonDto personDto = personFacade.getByUuid(personUuid);

		PersonEditForm editForm =
			new PersonEditForm(UiUtil.permitted(isEditAllowed, UserRight.PERSON_EDIT), personDto.isPseudonymized(), personDto.isInJurisdiction());
		editForm.setValue(personDto);

		final CommitDiscardWrapperComponent<PersonEditForm> editView = new CommitDiscardWrapperComponent<>(editForm, editForm.getFieldGroup());

		editView.addCommitListener(() -> {
			if (!editForm.getFieldGroup().isModified()) {
				PersonDto dto = editForm.getValue();
				savePersonWithPersonConditionChanged(dto);
			}
		});

		return editView;
	}

	public CommitDiscardWrapperComponent<PersonEditForm> getPersonEditComponent(
		PersonContext personContext,
		PersonDto person,
		Disease disease,
		String diseaseDetails,
		UserRight editUserRight,
		final ViewMode viewMode) {

		PersonEditForm editForm =
			new PersonEditForm(personContext, disease, diseaseDetails, viewMode, person.isPseudonymized(), person.isInJurisdiction());
		editForm.setValue(person);

		final CommitDiscardWrapperComponent<PersonEditForm> editView =
			new CommitDiscardWrapperComponent<>(editForm, UiUtil.permitted(editUserRight), editForm.getFieldGroup());

		editView.addCommitListener(() -> {
			if (!editForm.getFieldGroup().isModified()) {
				PersonDto dto = editForm.getValue();
				savePersonWithPersonConditionChanged(dto);
			}
		});

		return editView;
	}

	public CommitDiscardWrapperComponent<PersonEditForm> getPersonEditComponent(
		PersonContext personContext,
		PersonDto person,
		Disease disease,
		String diseaseDetails,
		UserRight editUserRight,
		final ViewMode viewMode,
		boolean isEditAllowed) {

		PersonEditForm editForm = new PersonEditForm(
			personContext,
			disease,
			diseaseDetails,
			viewMode,
			person.isPseudonymized(),
			person.isInJurisdiction(),
			UiUtil.permitted(isEditAllowed, editUserRight));
		editForm.setValue(person);

		final CommitDiscardWrapperComponent<PersonEditForm> editView = new CommitDiscardWrapperComponent<>(editForm, editForm.getFieldGroup());

		editView.addCommitListener(() -> {
			if (!editForm.getFieldGroup().isModified()) {
				PersonDto dto = editForm.getValue();
				if (editForm.getWarningSimilarPersons() != null) {
					// the dialog's first child is its content (was Window.getContent())
					@SuppressWarnings("unchecked")
					CommitDiscardWrapperComponent<PersonSelectionGrid> content =
						(CommitDiscardWrapperComponent<PersonSelectionGrid>) editView.getWrappedComponent()
							.getWarningSimilarPersons()
							.getChildren()
							.findFirst()
							.orElse(null);
					content.getDiscardButton().setVisible(true);
					content.addCommitListener(() -> savePersonWithPersonConditionChanged(dto));
				} else {
					savePersonWithPersonConditionChanged(dto);
				}
			}
		});

		return editView;
	}

	private void savePersonWithPersonConditionChanged(PersonDto editedPerson) {

		CaseDataDto lastCase;
		if (editedPerson.getCauseOfDeathDisease() != null) {
			List<CaseDataDto> personCases = FacadeProvider.getCaseFacade()
				.getAllCasesOfPerson(editedPerson.getUuid())
				.stream()
				.filter(caseDataDto -> caseDataDto.getDisease().equals(editedPerson.getCauseOfDeathDisease()))
				.collect(Collectors.toList());
			lastCase = personCases.isEmpty() ? null : Collections.max(personCases, Comparator.comparing(CaseDataDto::getReportDate));
		} else {
			lastCase = null;
		}

		PresentCondition editedPresentCondition = editedPerson.getPresentCondition();
		if (lastCase != null
			&& (Arrays.asList(PresentCondition.BURIED, PresentCondition.DEAD).contains(editedPresentCondition))
			&& editedPerson.getCauseOfDeath().equals(CauseOfDeath.EPIDEMIC_DISEASE)) {

			PersonDto initialPerson = FacadeProvider.getPersonFacade().getByUuid(editedPerson.getUuid());
			PresentCondition initialPresentCondition = initialPerson.getPresentCondition();

			if (Arrays.asList(PresentCondition.BURIED, PresentCondition.DEAD).contains(initialPresentCondition)
				&& lastCase.getOutcome().equals(CaseOutcome.DECEASED)
				&& ((editedPerson.getDeathDate() == null && lastCase.getOutcomeDate() == null)
					|| (editedPerson.getDeathDate() != null && editedPerson.getDeathDate().equals(lastCase.getOutcomeDate())))) {

				savePerson(editedPerson);
				return;
			} else {
				VerticalLayout warningLayout = new VerticalLayout();
				warningLayout.setSpacing(false);
				CommitDiscardWrapperComponent<VerticalLayout> warningComponent = new CommitDiscardWrapperComponent<>(warningLayout);
				warningComponent.setWidth(100, Unit.PERCENTAGE);

				Dialog popupWindow = VaadinUiUtil.showPopupWindow(warningComponent, I18nProperties.getString(Strings.warning));
				Span infoLabel = new Span(I18nProperties.getString(Strings.messageChangingPersonPresentCondition));
				CssStyles.style(infoLabel, CssStyles.LABEL_LARGE, CssStyles.LABEL_WHITE_SPACE_NORMAL);
				warningLayout.add(infoLabel);

				// case information
				warningLayout.add(new Span(lastCase.buildCaption()));

				// confirmation message
				Span confirmationMessage = new Span(I18nProperties.getString(Strings.messageReviewChangesAndConfirm));
				confirmationMessage.addClassName(CssStyles.VSPACE_TOP_3);
				warningLayout.add(confirmationMessage);

				// changes listed
				HorizontalLayout changesLayout = new HorizontalLayout();
				changesLayout.setPadding(false);
				changesLayout.addClassNames(CssStyles.VSPACE_TOP_1, CssStyles.VSPACE_2);

				VerticalLayout fieldsLayout = new VerticalLayout();
				fieldsLayout.setPadding(false);
				Span fieldsLabel = new Span(I18nProperties.getCaption(Captions.confirmChangesField));
				fieldsLayout.add(fieldsLabel);
				fieldsLabel.addClassName(CssStyles.LABEL_SECONDARY);
				fieldsLayout.add(new Span(I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.OUTCOME)));
				fieldsLayout.add(new Span(I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.OUTCOME_DATE)));

				VerticalLayout arrowsLayout = new VerticalLayout();
				arrowsLayout.setPadding(false);
				arrowsLayout.add(new Span());
				arrowsLayout.add(VaadinIcon.ARROW_RIGHT.create());
				arrowsLayout.add(VaadinIcon.ARROW_RIGHT.create());

				VerticalLayout valuesLayout = new VerticalLayout();
				valuesLayout.setPadding(false);

				Span valuesLabel = new Span(I18nProperties.getCaption(Captions.confirmChangesValue));
				valuesLabel.addClassName(CssStyles.LABEL_SECONDARY);
				valuesLayout.add(valuesLabel);

				valuesLayout.add(new Span(CaseOutcome.DECEASED.toString()));
				valuesLayout.add(new Span(DateFormatHelper.formatDate(editedPerson.getDeathDate())));

				changesLayout.add(fieldsLayout);
				changesLayout.add(arrowsLayout);
				changesLayout.add(valuesLayout);
				warningLayout.add(changesLayout);

				// actions
				warningComponent.addCommitListener(() -> {
					savePerson(editedPerson);
					popupWindow.close();
				});

				warningComponent.addDiscardListener(() -> popupWindow.close());

				// popup configuration
				VaadinUiUtil.addCloseListener(popupWindow, e -> popupWindow.close());
				popupWindow.setWidth("600px");

				return;
			}
		}

		savePerson(editedPerson);
	}

	private void savePerson(PersonDto personDto) {
		DataHelper.Pair<CaseClassification, PersonDto> saveResult = personFacade.savePersonWithoutNotifyingExternalJournal(personDto);

		ExternalJournalSyncResponseDto responseDto = FacadeProvider.getExternalJournalFacade().notifyExternalJournal(saveResult.getElement1());
		String synchronizationMessage = getSynchronizationMessage(responseDto);

		CaseClassification newClassification = saveResult.getElement0();
		if (newClassification != null) {
			String personSavedMessage =
				String.format(I18nProperties.getString(Strings.messagePersonSavedClassificationChanged), newClassification.toString());
			String notificationMessage = String.format("%s.%s", personSavedMessage, synchronizationMessage);
			if (responseDto == null || (responseDto.isSuccess() && responseDto.getErrors().isEmpty())) {
				NotificationHelper.showNotification(notificationMessage, Type.WARNING_MESSAGE, -1);
			} else {
				VaadinUiUtil.showWarningPopup(notificationMessage);
			}
		} else {
			String personSavedMessage = I18nProperties.getString(Strings.messagePersonSaved);
			String notificationMessage = String.format("%s.%s", personSavedMessage, synchronizationMessage);
			if (responseDto == null || (responseDto.isSuccess() && responseDto.getErrors().isEmpty())) {
				NotificationHelper.show(notificationMessage, Type.WARNING_MESSAGE);
			} else {
				VaadinUiUtil.showWarningPopup(notificationMessage);
			}
		}

		SormasUI.refreshView();
	}

	private String getSynchronizationMessage(ExternalJournalSyncResponseDto responseDto) {
		if (responseDto == null) {
			return "";
		}

		if (!responseDto.isSuccess()) {
			return I18nProperties.getValidationError(Validations.externalJournalPersonSynchronizationFailure, responseDto.getMessage());
		} else if (!responseDto.getErrors().isEmpty()) {
			StringBuffer sb = new StringBuffer();
			responseDto.getErrors().forEach((errorKey, errorValue) -> {
				sb.append(errorValue);
				sb.append(";");
			});
			return I18nProperties.getValidationError(Validations.externalJournalPersonSynchronizationPartial, sb.toString());
		} else {
			return I18nProperties.getValidationError(Validations.externalJournalPersonSynchronizationSuccess);
		}
	}

	private void onPersonChanged(PersonDto existingPerson, PersonDto changedPerson) {
		// approximate age reference date
		if (existingPerson == null
			|| !DataHelper.equal(changedPerson.getApproximateAge(), existingPerson.getApproximateAge())
			|| !DataHelper.equal(changedPerson.getApproximateAgeType(), existingPerson.getApproximateAgeType())) {
			if (changedPerson.getApproximateAge() == null) {
				changedPerson.setApproximateAgeReferenceDate(null);
			} else {
				changedPerson.setApproximateAgeReferenceDate(changedPerson.getDeathDate() != null ? changedPerson.getDeathDate() : new Date());
			}
		}
	}

	public void navigateToPersons() {
		SormasUI.get().getNavigator().navigateTo(PersonsView.VIEW_NAME);
	}

	public void navigateToPersons(PersonCriteria criteria) {
		ViewModelProviders.of(PersonsView.class).remove(PersonCriteria.class);
		ViewModelProviders.of(PersonsView.class).get(PersonCriteria.class, criteria);

		String navigationState = AbstractView.buildNavigationState(PersonsView.VIEW_NAME, criteria);
		SormasUI.get().getNavigator().navigateTo(navigationState);
	}

	public void navigateToPerson(String uuid) {
		final String navigationState = PersonDataView.VIEW_NAME + "/" + uuid;
		SormasUI.get().getNavigator().navigateTo(navigationState);
	}

	public void validateBirthDate(Integer year, Integer month, Integer day) throws Field.InvalidValueException {
		try {
			PersonHelper.validateBirthDate(year, month, day);
		} catch (ValidationRuntimeException ex) {
			throw new Field.InvalidValueException(ex.getMessage());
		}
	}
}
