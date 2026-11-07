/*
 * ******************************************************************************
 * * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * *
 * * This program is free software: you can redistribute it and/or modify
 * * it under the terms of the GNU General Public License as published by
 * * the Free Software Foundation, either version 3 of the License, or
 * * (at your option) any later version.
 * *
 * * This program is distributed in the hope that it will be useful,
 * * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * * GNU General Public License for more details.
 * *
 * * You should have received a copy of the GNU General Public License
 * * along with this program. If not, see <https://www.gnu.org/licenses/>.
 * ******************************************************************************
 */

package de.symeda.sormas.ui.events.groups;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.event.EventGroupReferenceDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

public class EventGroupMemberListComponent extends VerticalLayout {

	private EventGroupReferenceDto eventGroupReference;
	private EventGroupMemberList list;
	private Button createButton;

	public EventGroupMemberListComponent(EventGroupReferenceDto eventGroupReference) {

		this.eventGroupReference = eventGroupReference;

		createEventListComponent(new EventGroupMemberList(eventGroupReference), I18nProperties.getString(Strings.entityEvents));

	}

	private void createEventListComponent(EventGroupMemberList eventList, String heading) {
		setWidthFull();
		setPadding(false);
		setSpacing(false);

		HorizontalLayout componentHeader = new HorizontalLayout();
		componentHeader.setPadding(false);
		componentHeader.setSpacing(false);
		componentHeader.setWidthFull();
		add(componentHeader);

		list = eventList;
		add(list);
		list.reload();

		Span eventLabel = new Span(heading);
		eventLabel.addClassName(CssStyles.H3);
		componentHeader.add(eventLabel);
		// Vaadin 8: the heading's slot took the free space, the button sat right in its own slot
		componentHeader.setFlexGrow(1, eventLabel);

		if (UiUtil.permitted(UserRight.EVENTGROUP_LINK)) {
			createButton = ButtonHelper.createButton(I18nProperties.getCaption(Captions.linkEvent));
			CssStyles.style(createButton, CssStyles.BUTTON_PRIMARY);
			ButtonHelper.setIcon(createButton, VaadinIcon.PLUS_CIRCLE);
			createButton.addClickListener(event -> ControllerProvider.getEventController().selectEvent(eventGroupReference));
			componentHeader.add(createButton);
			componentHeader.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, createButton);
		}
	}
}
