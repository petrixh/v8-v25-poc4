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

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.event.EventGroupIndexDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.HtmlHelper;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ClickableLabel;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentField;

public class EventGroupListEntry extends SideComponentField {

	private final EventGroupIndexDto eventGroup;
	private Button editButton;
	private Button unlinkEventButton;
	private Button listEventsButton;

	public EventGroupListEntry(EventGroupIndexDto eventGroup) {
		this.eventGroup = eventGroup;

		HorizontalLayout topLayout = new HorizontalLayout();
		topLayout.setWidthFull();
		topLayout.setPadding(false);
		topLayout.setSpacing(false);
		addComponentToField(topLayout);

		VerticalLayout topLeftLayout = new VerticalLayout();
		{
			topLeftLayout.setPadding(false);
			topLeftLayout.setSpacing(false);

			Span eventTitleLabel = new Span(DataHelper.toStringNullable(eventGroup.getName()));
			CssStyles.style(eventTitleLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
			eventTitleLabel.setWidthFull();

			topLeftLayout.add(eventTitleLabel);

			String hyperlinkedUuid = HtmlHelper.buildHyperlinkTitle(eventGroup.getUuid(), DataHelper.getShortUuid(eventGroup.getUuid()));
			ClickableLabel uuidValue = new ClickableLabel(hyperlinkedUuid, ContentMode.HTML);
			uuidValue.addClickListener(event -> {
				ControllerProvider.getEventGroupController().navigateToData(eventGroup.getUuid());
			});

			Span uuidLabel = VaadinUiUtil.createLabel(I18nProperties.getCaption(Captions.EventGroup_uuid) + ":", ContentMode.HTML);
			CssStyles.style(uuidLabel, CssStyles.HSPACE_RIGHT_4);

			HorizontalLayout labelLayout = new HorizontalLayout();
			labelLayout.setSpacing(false);
			labelLayout.add(uuidLabel);
			labelLayout.add(uuidValue);

			Span eventCountLabel = new Span(I18nProperties.getCaption(Captions.EventGroup_eventCount) + ": " + eventGroup.getEventCount());

			topLeftLayout.add(labelLayout);
			topLeftLayout.add(eventCountLabel);
		}
		topLayout.add(topLeftLayout);
	}

	public void addEditListener(int rowIndex, ComponentEventListener<ClickEvent<Button>> editClickListener) {
		if (editButton == null) {
			editButton = ButtonHelper.createIconButtonWithCaption(
				"add-event-" + rowIndex,
				null,
				VaadinIcon.PENCIL,
				null,
				CssStyles.BUTTON_LINK,
				CssStyles.BUTTON_COMPACT);
			editButton.setTooltipText(I18nProperties.getCaption(Captions.eventEditEventGroup));

			add(editButton);
			setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, editButton);
			setFlexGrow(0, editButton);
		}

		editButton.addClickListener(editClickListener);
	}

	public void addUnlinkEventListener(int rowIndex, ComponentEventListener<ClickEvent<Button>> unlinkEventClickListener) {
		if (unlinkEventButton == null) {
			unlinkEventButton = ButtonHelper.createIconButtonWithCaption(
				"unlink-event-" + rowIndex,
				null,
				VaadinIcon.UNLINK,
				null,
				CssStyles.BUTTON_LINK,
				CssStyles.BUTTON_COMPACT);
			unlinkEventButton.setTooltipText(I18nProperties.getCaption(Captions.eventUnlinkEventGroup));

			add(unlinkEventButton);
			setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, unlinkEventButton);
			setFlexGrow(0, unlinkEventButton);
		}

		unlinkEventButton.addClickListener(unlinkEventClickListener);
	}

	public void addListEventsListener(int rowIndex, ComponentEventListener<ClickEvent<Button>> listEventsClickListener) {
		if (listEventsButton == null) {
			listEventsButton = ButtonHelper.createIconButtonWithCaption(
				"list-events-" + rowIndex,
				null,
				VaadinIcon.LIST_UL,
				null,
				CssStyles.BUTTON_LINK,
				CssStyles.BUTTON_COMPACT);
			listEventsButton.setTooltipText(I18nProperties.getCaption(Captions.eventGroupListEvents));

			add(listEventsButton);
			setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, listEventsButton);
			setFlexGrow(0, listEventsButton);
		}

		listEventsButton.addClickListener(listEventsClickListener);
	}

	public EventGroupIndexDto getEventGroup() {
		return eventGroup;
	}
}
