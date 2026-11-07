package de.symeda.sormas.ui.contact.components.linelisting.layout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.BirthDateDto;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.contact.ContactDto;
import de.symeda.sormas.api.contact.ContactProximity;
import de.symeda.sormas.api.event.EventDto;
import de.symeda.sormas.api.event.EventParticipantDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.utils.UtilDate;
import de.symeda.sormas.api.utils.ValidationRuntimeException;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.UserProvider;
import de.symeda.sormas.ui.contact.components.linelisting.contactfield.ContactLineField;
import de.symeda.sormas.ui.contact.components.linelisting.contactfield.ContactLineFieldDto;
import de.symeda.sormas.ui.contact.components.linelisting.sharedinfo.SharedInfoField;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.components.linelisting.line.DeleteLineEvent;
import de.symeda.sormas.ui.utils.components.linelisting.line.LineLayout;
import de.symeda.sormas.ui.utils.components.linelisting.model.LineDto;
import de.symeda.sormas.ui.utils.components.linelisting.person.PersonFieldDto;
import de.symeda.sormas.ui.utils.components.linelisting.section.LineListingSection;

public class LineListingLayout extends VerticalLayout {

	private static final long serialVersionUID = 5372515237174090496L;

	public static final float DEFAULT_WIDTH = 1860;

	private final SharedInfoField sharedInfoField;
	private final List<ContactLineLayout> lines;
	private final LineListingSection lineComponent;

	private final Dialog window;
	private Consumer<LinkedList<LineDto<ContactDto>>> saveCallback;

	private LineListingLayout(Dialog window, SharedInfoField sharedInfoField) {

		this.window = window;

		setSpacing(false);

		LineListingSection sharedInformationComponent = new LineListingSection(Captions.lineListingSharedInformation);

		this.sharedInfoField = sharedInfoField;
		this.sharedInfoField.setId("lineListingSharedInfoField");
		this.sharedInfoField.addDiseaseChangeHandler(this::updateTypeOfContactValues);
		sharedInformationComponent.add(this.sharedInfoField);

		add(sharedInformationComponent);

		lineComponent = new LineListingSection(Captions.lineListingNewContactsList);

		lines = new ArrayList<>();

		add(lineComponent);
	}

	public LineListingLayout(Dialog window, CaseDataDto caseDataDto) {
		this(window, new SharedInfoField(caseDataDto));

		Disease disease = caseDataDto == null ? FacadeProvider.getDiseaseConfigurationFacade().getDefaultDisease() : caseDataDto.getDisease();

		ContactLineLayout line = buildNewLine(lineComponent);
		line.updateTypeOfContactValues(disease);
		lines.add(line);
		lineComponent.add(line);

		HorizontalLayout actionBar = new HorizontalLayout();
		Button addLine = ButtonHelper.createIconButton(Captions.lineListingAddLine, VaadinIcon.PLUS, e -> {
			ContactLineLayout newLine = buildNewLine(lineComponent);
			newLine.updateTypeOfContactValues(sharedInfoField.getValue().getDisease());
			lines.add(newLine);
			lineComponent.add(newLine);
			lines.get(0).enableDelete(true);
		}, CssStyles.BUTTON_PRIMARY);

		actionBar.add(addLine);
		actionBar.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, addLine);

		add(actionBar);

		addButtons();
	}

	public LineListingLayout(Dialog window, EventDto eventDto, Iterable<EventParticipantDto> eventParticipantDtos) {
		this(window, new SharedInfoField(eventDto));

		for (EventParticipantDto eventParticipantDto : eventParticipantDtos) {
			PersonFieldDto personField = new PersonFieldDto();
			personField.setFirstName(eventParticipantDto.getPerson().getFirstName());
			personField.setLastName(eventParticipantDto.getPerson().getLastName());
			personField.setBirthDate(
				new BirthDateDto(
					eventParticipantDto.getPerson().getBirthdateDD(),
					eventParticipantDto.getPerson().getBirthdateMM(),
					eventParticipantDto.getPerson().getBirthdateYYYY()));
			personField.setSex(eventParticipantDto.getPerson().getSex());

			ContactLineLayoutDto newLineDto = new ContactLineLayoutDto();
			newLineDto.setLineField(new ContactLineFieldDto());
			newLineDto.getLineField().setPerson(personField);

			ContactLineLayout newLine = new ContactLineLayout(lines.size());
			newLine.enableDelete(false);
			newLine.enablePersonField(false);
			newLine.setPerson(eventParticipantDto.getPerson());
			newLine.setBean(newLineDto);
			if (eventDto != null) {
				newLine.updateTypeOfContactValues(eventDto.getDisease());
			}

			lines.add(newLine);
			lineComponent.add(newLine);
		}

		Disease disease = eventDto == null ? FacadeProvider.getDiseaseConfigurationFacade().getDefaultDisease() : eventDto.getDisease();
		updateTypeOfContactValues(disease);
		addButtons();
	}

	private void addButtons() {
		HorizontalLayout buttonsPanel = new HorizontalLayout();
		buttonsPanel.setPadding(false);
		buttonsPanel.setSpacing(true);
		buttonsPanel.setWidthFull();

		Button cancelButton = ButtonHelper.createButton(Captions.actionDiscard, event -> closeWindow());

		// was right-aligned in an expanded slot: both buttons sit on the right
		buttonsPanel.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		buttonsPanel.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
		buttonsPanel.add(cancelButton);

		Button saveButton =
			ButtonHelper.createButton(Captions.actionSave, event -> saveCallback.accept(getContactLineDtos()), CssStyles.BUTTON_PRIMARY);

		buttonsPanel.add(saveButton);

		add(buttonsPanel);
		setHorizontalComponentAlignment(FlexComponent.Alignment.END, buttonsPanel);
	}

	public void closeWindow() {
		window.close();
	}

	public void validate() throws ValidationRuntimeException {
		boolean validationFailed = sharedInfoField.hasErrors();
		// XXX: process each line unconditionally, as it will actually validate and display errors
		for (ContactLineLayout line : lines) {
			if (line.hasErrors()) {
				validationFailed = true;
			}
		}
		if (validationFailed) {
			throw new ValidationRuntimeException(I18nProperties.getString(Strings.errorFieldValidationFailed));
		}
	}

	private LinkedList<LineDto<ContactDto>> getContactLineDtos() {
		return lines.stream().map(line -> {
			ContactLineLayoutDto layoutBean = line.getBean();
			LineDto<ContactDto> result = new LineDto<>();

			final ContactDto contact = ContactDto.build();
			contact.setCaze(sharedInfoField.getValue().getCaze());
			contact.setDisease(sharedInfoField.getValue().getDisease());
			contact.setRegion(sharedInfoField.getValue().getRegion());
			contact.setDistrict(sharedInfoField.getValue().getDistrict());
			contact.setReportDateTime(UtilDate.from(layoutBean.getLineField().getDateOfReport()));
			contact.setMultiDayContact(layoutBean.getLineField().getMultiDaySelector().isMultiDay());
			contact.setFirstContactDate(UtilDate.from(layoutBean.getLineField().getMultiDaySelector().getStartDate()));
			contact.setLastContactDate(UtilDate.from(layoutBean.getLineField().getMultiDaySelector().getEndDate()));
			ContactProximity typeOfContact = layoutBean.getLineField().getTypeOfContact();
			if (typeOfContact != null) {
				contact.setContactProximities(Collections.singleton(typeOfContact));
			}
			contact.setRelationToCase(layoutBean.getLineField().getRelationToCase());
			if (UserProvider.getCurrent() != null) {
				contact.setReportingUser(UiUtil.getUserReference());
			}
			result.setEntity(contact);

			final PersonDto person;
			if (line.getPerson() != null) {
				person = line.getPerson();
			} else {
				person = PersonDto.build();
				person.setFirstName(layoutBean.getLineField().getPerson().getFirstName());
				person.setLastName(layoutBean.getLineField().getPerson().getLastName());
				if (layoutBean.getLineField().getPerson().getBirthDate() != null) {
					person.setBirthdateYYYY(layoutBean.getLineField().getPerson().getBirthDate().getDateOfBirthYYYY());
					person.setBirthdateMM(layoutBean.getLineField().getPerson().getBirthDate().getDateOfBirthMM());
					person.setBirthdateDD(layoutBean.getLineField().getPerson().getBirthDate().getDateOfBirthDD());
				}
				person.setSex(layoutBean.getLineField().getPerson().getSex());
			}
			result.setPerson(person);

			return result;
		}).collect(Collectors.toCollection(LinkedList::new));
	}

	public void setSaveCallback(Consumer<LinkedList<LineDto<ContactDto>>> saveCallback) {
		this.saveCallback = saveCallback;
	}

	public void updateTypeOfContactValues(Disease disease) {
		for (ContactLineLayout line : lines) {
			line.updateTypeOfContactValues(disease);
		}
	}

	private ContactLineLayout buildNewLine(VerticalLayout lineComponent) {
		ContactLineLayout newLine = new ContactLineLayout(lines.size());
		ContactLineLayoutDto newLineDto = new ContactLineLayoutDto();

		newLineDto.setLineField(new ContactLineFieldDto());
		if (lines.isEmpty()) {
			newLine.enableDelete(false);
		}

		newLine.setBean(newLineDto);
		newLine.addDeleteLineListener(e -> {
			ContactLineLayout selectedLine = (ContactLineLayout) e.getSource();
			lineComponent.remove(selectedLine);
			lines.remove(selectedLine);
			lines.get(0).enableDelete(lines.size() > 1);
		});

		return newLine;
	}

	class ContactLineLayout extends LineLayout {

		private static final long serialVersionUID = -6430490310394731389L;

		private final Binder<ContactLineLayoutDto> binder = new Binder<>(ContactLineLayoutDto.class);

		private final ContactLineField contactLineField;
		private final Button delete;
		private PersonDto person;

		public ContactLineLayout(int lineIndex) {

			addClassName(CssStyles.SPACING_SMALL);
			setPadding(false);

			contactLineField = new ContactLineField();
			contactLineField.setId("lineListingContactLineField_" + lineIndex);
			binder.forField(contactLineField).bind(ContactLineLayoutDto.LINE_FIELD);

			delete = ButtonHelper
				.createIconButtonWithCaption("delete_" + lineIndex, null, VaadinIcon.TRASH, event -> fireEvent(new DeleteLineEvent(this)));
			// was setStyleName: replaces the button's style names
			delete.setClassName(CssStyles.VSPACE_3);

			add(contactLineField, delete);

			// was Alignment.BOTTOM_LEFT
			setVerticalComponentAlignment(FlexComponent.Alignment.END, contactLineField, delete);

			contactLineField.showCaptions();
		}

		public void updateTypeOfContactValues(Disease disease) {
			contactLineField.updateTypeOfContactValues(disease);
		}

		public void setBean(ContactLineLayoutDto bean) {
			binder.setBean(bean);
		}

		public ContactLineLayoutDto getBean() {
			return binder.getBean();
		}

		public boolean hasErrors() {
			return sharedInfoField.hasErrors() || contactLineField.hasErrors();
		}

		public void enableDelete(boolean shouldEnable) {
			delete.setEnabled(shouldEnable);
		}

		public void enablePersonField(boolean shouldEnable) {
			contactLineField.enablePersonField(shouldEnable);
		}

		public void setPerson(PersonDto person) {
			this.person = person;
		}

		public PersonDto getPerson() {
			return person;
		}
	}

	public static class ContactLineLayoutDto implements Serializable {

		private static final long serialVersionUID = 1043859942972062173L;

		public static final String LINE_FIELD = "lineField";

		private ContactLineFieldDto lineField;

		public ContactLineFieldDto getLineField() {
			return lineField;
		}

		public void setLineField(ContactLineFieldDto lineField) {
			this.lineField = lineField;
		}
	}

	public SharedInfoField getSharedInfoField() {
		return sharedInfoField;
	}
}
