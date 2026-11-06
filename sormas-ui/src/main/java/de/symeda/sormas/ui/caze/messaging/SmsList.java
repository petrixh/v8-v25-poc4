package de.symeda.sormas.ui.caze.messaging;

import java.util.List;

import com.vaadin.flow.component.html.Span;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.manualmessagelog.ManualMessageLogDto;
import de.symeda.sormas.api.messaging.MessageType;
import de.symeda.sormas.api.person.PersonReferenceDto;
import de.symeda.sormas.ui.utils.PaginationList;

public class SmsList extends PaginationList<ManualMessageLogDto> {

	private PersonReferenceDto personReferenceDto;
	private boolean hasPhoneNumber;

	public SmsList(PersonReferenceDto personReferenceDto, boolean hasPhoneNumber) {
		super(5);
		this.personReferenceDto = personReferenceDto;
		this.hasPhoneNumber = hasPhoneNumber;
	}

	@Override
	public void reload() {

		List<ManualMessageLogDto> messageLogs = FacadeProvider.getCaseFacade().getMessageLog(personReferenceDto.getUuid(), MessageType.SMS);

		if (!hasPhoneNumber){
			Span noPhoneNumberLabel = new Span(I18nProperties.getCaption(Captions.messagesNoPhoneNumberForCasePerson));
			listLayout.add(noPhoneNumberLabel);
		}

		setEntries(messageLogs);
		if (!messageLogs.isEmpty()) {
			showPage(1);
		} else {
			updatePaginationLayout();
			Span noMessageLabel = new Span(I18nProperties.getCaption(Captions.messagesNoSmsSentForCase));
			listLayout.add(noMessageLabel);
		}
	}

	@Override
	protected void drawDisplayedEntries() {
		List<ManualMessageLogDto> displayedEntries = getDisplayedEntries();
		for (int i = 0, displayedEntriesSize = displayedEntries.size(); i < displayedEntriesSize; i++) {
			SmsListEntry listEntry = new SmsListEntry(displayedEntries.get(i));
			listLayout.add(listEntry);
		}
	}
}
