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

package de.symeda.sormas.ui.events.eventLink;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.event.EventIndexDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentField;

public class EventListEntry extends SideComponentField {

	private final EventIndexDto event;
	private Button editButton;
	private Button unlinkEventButton;

	public EventListEntry(EventIndexDto event) {
		this.event = event;

		HorizontalLayout topLayout = new HorizontalLayout();
		topLayout.setWidthFull();
		topLayout.setPadding(false);
		topLayout.setSpacing(false);
		addComponentToField(topLayout);

		VerticalLayout topLeftLayout = new VerticalLayout();
		{
			topLeftLayout.setPadding(false);
			topLeftLayout.setSpacing(false);

			Span eventTitleLabel = new Span(DataHelper.toStringNullable(event.getEventTitle()));
			CssStyles.style(eventTitleLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
			eventTitleLabel.setWidthFull();

			topLeftLayout.add(eventTitleLabel);

			Span locationLabel = new Span(DataHelper.toStringNullable(event.getEventLocation()));
			locationLabel.setWidthFull();
			topLeftLayout.add(locationLabel);

			Language userLanguage = I18nProperties.getUserLanguage();
			String eventDate;
			if (event.getEndDate() == null) {
				eventDate = DateHelper.formatLocalDate(event.getStartDate(), userLanguage);
			} else {
				eventDate = String.format(
					"%s - %s",
					DateHelper.formatLocalDate(event.getStartDate(), userLanguage),
					DateHelper.formatLocalDate(event.getEndDate(), userLanguage));
			}
			Span eventDateLabel = new Span(I18nProperties.getCaption(Captions.singleDayEventDate) + ": " + eventDate);

			topLeftLayout.add(eventDateLabel);
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
			editButton.setTooltipText(I18nProperties.getCaption(Captions.eventEditEvent));

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
			unlinkEventButton.setTooltipText(I18nProperties.getCaption(Captions.eventUnlinkEvent));

			add(unlinkEventButton);
			setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, unlinkEventButton);
			setFlexGrow(0, unlinkEventButton);
		}

		unlinkEventButton.addClickListener(unlinkEventClickListener);
	}

	public EventIndexDto getEvent() {
		return event;
	}
}
