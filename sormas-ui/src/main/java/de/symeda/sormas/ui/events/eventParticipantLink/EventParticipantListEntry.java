package de.symeda.sormas.ui.events.eventParticipantLink;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.event.EventDto;
import de.symeda.sormas.api.event.EventParticipantDto;
import de.symeda.sormas.api.event.EventParticipantListEntryDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

public class EventParticipantListEntry extends HorizontalLayout {

	private static final long serialVersionUID = -124120232066365444L;
	public static final String SEPARATOR = ": ";
	private final EventParticipantListEntryDto eventParticipantListEntryDto;
	private Button editButton;

	public EventParticipantListEntry(EventParticipantListEntryDto eventParticipantListEntryDto) {
		this.eventParticipantListEntryDto = eventParticipantListEntryDto;
		setSpacing(true);
		setWidthFull();
		addClassName(CssStyles.SORMAS_LIST_ENTRY);

		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setWidthFull();
		mainLayout.setPadding(false);
		mainLayout.setSpacing(false);
		add(mainLayout);
		setFlexGrow(1, mainLayout);

		HorizontalLayout idLayout = new HorizontalLayout();
		Span eventParticipantUuidLabel = new Span(
			I18nProperties.getPrefixCaption(EventParticipantDto.I18N_PREFIX, EventParticipantDto.UUID)
				+ SEPARATOR
				+ DataHelper.toStringNullable(DataHelper.getShortUuid(eventParticipantListEntryDto.getUuid())));
		eventParticipantUuidLabel.setTitle(eventParticipantListEntryDto.getUuid());
		Span eventUuidLabel = new Span(
			I18nProperties.getPrefixCaption(EventDto.I18N_PREFIX, EventDto.UUID)
				+ SEPARATOR
				+ DataHelper.toStringNullable(DataHelper.getShortUuid(eventParticipantListEntryDto.getEventUuid())));
		eventUuidLabel.setTitle(eventParticipantListEntryDto.getEventUuid());

		idLayout.add(eventParticipantUuidLabel);
		idLayout.add(eventUuidLabel);
		idLayout.setWidthFull();
		// was each label aligned left and right in its half
		idLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		idLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		mainLayout.add(idLayout);

		HorizontalLayout diseaseStatusLayout = new HorizontalLayout();
		String eventDiseaseString = eventParticipantListEntryDto.getDisease() != null
			? eventParticipantListEntryDto.getDisease().toString()
			: I18nProperties.getCaption(Captions.eventNoDisease);
		Span diseaseLabel = new Span(eventDiseaseString);
		diseaseLabel.addClassNames(CssStyles.LABEL_BOLD);
		diseaseLabel.setTitle(eventDiseaseString);
		diseaseStatusLayout.add(diseaseLabel);

		Span statusLabel = new Span(eventParticipantListEntryDto.getEventStatus().toString());
		statusLabel.addClassNames(CssStyles.LABEL_BOLD);
		statusLabel.setTitle(eventParticipantListEntryDto.getEventStatus().toString());
		diseaseStatusLayout.add(statusLabel);

		diseaseStatusLayout.add(diseaseLabel);
		diseaseStatusLayout.add(statusLabel);
		diseaseStatusLayout.setWidthFull();
		// was each label aligned left and right in its half
		diseaseStatusLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		diseaseStatusLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		mainLayout.add(diseaseStatusLayout);

		Span titleLabel = new Span(eventParticipantListEntryDto.getEventTitle());
		titleLabel.setTitle(eventParticipantListEntryDto.getEventTitle());
		mainLayout.add(titleLabel);

		if (eventParticipantListEntryDto.getEventStartDate() != null) {
			Language userLanguage = I18nProperties.getUserLanguage();
			String eventDate;
			if (eventParticipantListEntryDto.getEventEndDate() == null) {
				eventDate = DateHelper.formatLocalDate(eventParticipantListEntryDto.getEventStartDate(), userLanguage);
			} else {
				eventDate = String.format(
					"%s - %s",
					DateHelper.formatLocalDate(eventParticipantListEntryDto.getEventStartDate(), userLanguage),
					DateHelper.formatLocalDate(eventParticipantListEntryDto.getEventEndDate(), userLanguage));
			}
			Span eventDateLabel = new Span(I18nProperties.getCaption(Captions.singleDayEventDate) + ": " + eventDate);
			mainLayout.add(eventDateLabel);
		}
	}

	public void addEditListener(int rowIndex, ComponentEventListener<ClickEvent<Button>> editClickListener) {
		if (editButton == null) {
			editButton = ButtonHelper.createIconButtonWithCaption(
				"edit-participant-" + rowIndex,
				null,
				VaadinIcon.PENCIL,
				null,
				CssStyles.BUTTON_LINK,
				CssStyles.BUTTON_COMPACT);

			add(editButton);
			setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, editButton);
			setFlexGrow(0, editButton);
		}

		editButton.addClickListener(editClickListener);
	}

	public void setActive() {
		addClassName(CssStyles.ACTIVE_SIDE_COMPONENT_ELEMENT);
	}

	public EventParticipantListEntryDto getEventParticipantListEntryDto() {
		return eventParticipantListEntryDto;
	}
}
