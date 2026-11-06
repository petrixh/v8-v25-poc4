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

import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.event.EventIndexDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.HtmlHelper;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ClickableLabel;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;

public class EventGroupMemberListEntry extends HorizontalLayout {

	private final EventIndexDto event;
	private Button navigateButton;
	private Button unlinkEventButton;

	public EventGroupMemberListEntry(EventIndexDto event) {
		this.event = event;

		setPadding(false);
		setSpacing(true);
		setWidthFull();
		addClassName(CssStyles.SORMAS_LIST_ENTRY);

		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setWidthFull();
		mainLayout.setPadding(false);
		mainLayout.setSpacing(false);
		add(mainLayout);
		setFlexGrow(1, mainLayout);

		HorizontalLayout topLayout = new HorizontalLayout();
		topLayout.setWidthFull();
		topLayout.setPadding(false);
		topLayout.setSpacing(false);
		mainLayout.add(topLayout);

		VerticalLayout topLeftLayout = new VerticalLayout();
		{
			topLeftLayout.setPadding(false);
			topLeftLayout.setSpacing(false);

			Span eventTitleLabel = new Span(DataHelper.toStringNullable(event.getEventTitle()));
			CssStyles.style(eventTitleLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
			eventTitleLabel.setWidthFull();

			topLeftLayout.add(eventTitleLabel);

			String hyperlinkedUuid = HtmlHelper.buildHyperlinkTitle(event.getUuid(), DataHelper.getShortUuid(event.getUuid()));
			ClickableLabel uuidValue = new ClickableLabel(hyperlinkedUuid, ContentMode.HTML);
			uuidValue.addClickListener(e -> {
				ControllerProvider.getEventController().navigateToData(event.getUuid());
			});

			Span uuidLabel = new Span(I18nProperties.getCaption(Captions.Event_uuid) + ":");
			CssStyles.style(uuidLabel, CssStyles.HSPACE_RIGHT_4);

			HorizontalLayout labelLayout = new HorizontalLayout();
			labelLayout.setSpacing(false);
			labelLayout.add(uuidLabel);
			labelLayout.add(uuidValue);
			topLeftLayout.add(labelLayout);

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

	public void addNavigateToEventButton(int rowIndex, boolean isEditable, ComponentEventListener<ClickEvent<Button>> editClickListener) {
		if (navigateButton == null) {
			navigateButton = ButtonHelper.createIconButtonWithCaption(
				"add-event-" + rowIndex,
				null,
				isEditable ? VaadinIcon.PENCIL : VaadinIcon.EYE,
				null,
				CssStyles.BUTTON_LINK,
				CssStyles.BUTTON_COMPACT);
			navigateButton.setTooltipText(I18nProperties.getCaption(Captions.eventEditEvent));

			add(navigateButton);
			setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, navigateButton);
			setFlexGrow(0, navigateButton);
		}

		navigateButton.addClickListener(editClickListener);
	}

	public void addUnlinkEventButton(int rowIndex, ComponentEventListener<ClickEvent<Button>> unlinkEventClickListener) {
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
