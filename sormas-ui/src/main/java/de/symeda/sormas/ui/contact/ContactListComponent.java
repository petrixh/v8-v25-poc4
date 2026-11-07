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
package de.symeda.sormas.ui.contact;

import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.event.EventParticipantReferenceDto;
import de.symeda.sormas.api.event.EventReferenceDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

@SuppressWarnings("serial")
public class ContactListComponent extends VerticalLayout {

	private final ContactList list;

	public ContactListComponent(EventParticipantReferenceDto eventParticipantRef, Consumer<Runnable> actionCallback, boolean isEditAllowed) {

		HorizontalLayout componentHeader = new HorizontalLayout();
		componentHeader.setPadding(false);
		componentHeader.setSpacing(false);
		componentHeader.setWidthFull();
		add(componentHeader);

		Span contactsHeader = new Span(I18nProperties.getString(Strings.entityContacts));
		contactsHeader.addClassName(CssStyles.H3);
		componentHeader.add(contactsHeader);
		componentHeader.setFlexGrow(1, contactsHeader);

		ContactList contactList = new ContactList(eventParticipantRef);
		setWidthFull();
		setPadding(false);
		setSpacing(false);

		EventReferenceDto eventRef = FacadeProvider.getEventFacade().getReferenceByEventParticipant(eventParticipantRef.getUuid());
		Checkbox contactOnlyWithSourceCaseInGivenEvent = new Checkbox(I18nProperties.getCaption(Captions.contactOnlyWithSourceCaseInGivenEvent));
		contactOnlyWithSourceCaseInGivenEvent.addClassNames(CssStyles.CHECKBOX_FILTER_INLINE, CssStyles.VSPACE_4);
		contactOnlyWithSourceCaseInGivenEvent.addValueChangeListener(e -> {
			if (e.getValue()) {
				contactList.filterContactListBySourceCaseInGivenEvent(eventRef);
			} else {
				contactList.filterContactListBySourceCaseInGivenEvent(null);
			}
			contactList.reload();
		});
		add(contactOnlyWithSourceCaseInGivenEvent);

		list = contactList;
		add(list);
		list.reload();

		if (UiUtil.permitted(isEditAllowed, UserRight.CONTACT_CREATE)) {
			Button createButton = ButtonHelper.createButton(I18nProperties.getCaption(Captions.contactNewContact));
			CssStyles.style(createButton, CssStyles.BUTTON_PRIMARY);
			ButtonHelper.setIcon(createButton, VaadinIcon.PLUS_CIRCLE);
			createButton.addClickListener(e -> actionCallback.accept(() -> ControllerProvider.getContactController().create(eventParticipantRef)));
			componentHeader.add(createButton);
			componentHeader.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, createButton);
		}
	}

	public void reload() {
		list.reload();
	}
}
