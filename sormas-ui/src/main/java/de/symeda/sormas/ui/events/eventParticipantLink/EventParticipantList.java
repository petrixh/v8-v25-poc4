package de.symeda.sormas.ui.events.eventParticipantLink;

import java.util.List;

import com.vaadin.flow.component.html.Span;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.event.EventParticipantCriteria;
import de.symeda.sormas.api.event.EventParticipantListEntryDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.person.PersonReferenceDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.PaginationList;

public class EventParticipantList extends PaginationList<EventParticipantListEntryDto> {

	private final EventParticipantCriteria eventParticipantCriteria = new EventParticipantCriteria();
	private final Span noEventParticipantLabel;

	public EventParticipantList(PersonReferenceDto personRef) {
		super(5);
		eventParticipantCriteria.setPerson(personRef);
		noEventParticipantLabel = new Span(I18nProperties.getCaption(Captions.personNoEventParticipantLinkedToPerson));
	}

	@Override
	public void reload() {
		List<EventParticipantListEntryDto> eventParticipants =
			FacadeProvider.getEventParticipantFacade().getListEntries(eventParticipantCriteria, 0, maxDisplayedEntries * 20);

		setEntries(eventParticipants);
		if (!eventParticipants.isEmpty()) {
			showPage(1);
		} else {
			listLayout.removeAll();
			updatePaginationLayout();
			listLayout.add(noEventParticipantLabel);
		}
	}

	@Override
	protected void drawDisplayedEntries() {
		List<EventParticipantListEntryDto> displayedEntries = getDisplayedEntries();
		for (int i = 0, displayedEntriesSize = displayedEntries.size(); i < displayedEntriesSize; i++) {
			final EventParticipantListEntryDto eventParticipant = displayedEntries.get(i);
			final EventParticipantListEntry listEntry = new EventParticipantListEntry(eventParticipant);
			boolean isActiveEventParticipant = eventParticipant.getUuid().equals(getActiveUuid());
			if (isActiveEventParticipant) {
				listEntry.setActive();
			}
			if (UiUtil.permitted(UserRight.EVENTPARTICIPANT_EDIT) && !isActiveEventParticipant) {
				listEntry.addEditListener(
					i,
					event -> ControllerProvider.getEventParticipantController()
						.navigateToData(listEntry.getEventParticipantListEntryDto().getUuid()));
			}

			listLayout.add(listEntry);
		}
	}
}
