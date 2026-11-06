/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.ui.events.eventLink;

import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.event.EventCriteria;
import de.symeda.sormas.api.event.EventDto;
import de.symeda.sormas.api.event.EventHelper;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.AbstractInfoLayout;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.FieldAccessHelper;

public class SuperordinateEventComponent extends VerticalLayout {

	private final EventDto subordinateEvent;
	private final Consumer<Runnable> actionCallback;

	public SuperordinateEventComponent(EventDto subordinateEvent, Consumer<Runnable> actionCallback) {
		this.subordinateEvent = subordinateEvent;
		this.actionCallback = actionCallback;
		initialize();
	}

	private void initialize() {
		setWidthFull();
		setPadding(false);
		setSpacing(false);

		Span lblHeading = new Span(I18nProperties.getCaption(Captions.eventSuperordinateEvent));
		lblHeading.addClassName(CssStyles.H3);
		add(lblHeading);

		HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setPadding(false);
		buttonLayout.setSpacing(true);
		buttonLayout.setWidthFull();
		CssStyles.style(buttonLayout, CssStyles.VSPACE_TOP_3);

		if (subordinateEvent.getSuperordinateEvent() != null) {
			add(
				new SuperordinateEventInfoLayout(
					FacadeProvider.getEventFacade().getEventByUuid(subordinateEvent.getSuperordinateEvent().getUuid(), false)));

			if (UiUtil.permitted(UserRight.EVENT_EDIT)) {
				Button btnUnlinkEvent = ButtonHelper.createIconButtonWithCaption(
					"unlinkSuperordinateEvent",
					I18nProperties.getCaption(Captions.eventUnlinkEvent),
					VaadinIcon.UNLINK,
					e -> actionCallback.accept(
						() -> ControllerProvider.getEventController()
							.removeSuperordinateEvent(
								subordinateEvent,
								true,
								I18nProperties.getString(Strings.messageEventSuperordinateEventUnlinked))),
					CssStyles.BUTTON_PRIMARY);
				btnUnlinkEvent.setWidthFull();
				buttonLayout.add(btnUnlinkEvent);

				Button btnOpenEvent = ButtonHelper.createIconButtonWithCaption(
					"openSuperordinateEvent",
					I18nProperties.getCaption(Captions.eventOpenSuperordinateEvent),
					VaadinIcon.EYE,
					e -> ControllerProvider.getEventController().navigateToData(subordinateEvent.getSuperordinateEvent().getUuid()),
					CssStyles.BUTTON_PRIMARY);
				btnOpenEvent.setWidthFull();
				buttonLayout.add(btnOpenEvent);
			}
		} else {
			add(new Span(I18nProperties.getString(Strings.infoNoSuperordinateEvent)));

			if (UiUtil.permitted(UserRight.EVENT_CREATE, UserRight.EVENT_EDIT)) {
				Button btnLinkEvent = ButtonHelper.createIconButtonWithCaption(
					"linkSuperordinateEvent",
					I18nProperties.getCaption(Captions.linkEvent),
					VaadinIcon.PLUS_CIRCLE,
					thisEvent -> {
						long events = FacadeProvider.getEventFacade().count(new EventCriteria());
						if (events > 0) {
							actionCallback.accept(
								() -> ControllerProvider.getEventController().selectOrCreateSuperordinateEvent(subordinateEvent.toReference()));
						} else {
							actionCallback
								.accept(() -> ControllerProvider.getEventController().createSuperordinateEvent(subordinateEvent.toReference()));
						}
					},
					CssStyles.BUTTON_PRIMARY);
				buttonLayout.add(btnLinkEvent);
			}
		}

		add(buttonLayout);
	}

	private class SuperordinateEventInfoLayout extends AbstractInfoLayout<EventDto> {

		private static final long serialVersionUID = 695237049227590809L;

		private final EventDto superordinateEvent;

		public SuperordinateEventInfoLayout(EventDto superordinateEvent) {
			super(EventDto.class, FieldAccessHelper.getFieldAccessCheckers(superordinateEvent));
			this.superordinateEvent = superordinateEvent;
			setSpacing(true);
			setPadding(false);
			setWidthFull();
			initialize();
		}

		private void initialize() {
			VerticalLayout leftColumnLayout = new VerticalLayout();
			leftColumnLayout.setPadding(false);
			leftColumnLayout.setSpacing(true);
			{
				final Span uuidLabel = addDescLabel(
					leftColumnLayout,
					EventDto.UUID,
					DataHelper.getShortUuid(superordinateEvent.getUuid()),
					I18nProperties.getPrefixCaption(EventDto.I18N_PREFIX, EventDto.UUID));
				uuidLabel.setTitle(subordinateEvent.getUuid());

				addDescLabel(
					leftColumnLayout,
					EventDto.EVENT_TITLE,
					superordinateEvent.getEventTitle(),
					I18nProperties.getPrefixCaption(EventDto.I18N_PREFIX, EventDto.EVENT_TITLE));

				addDescLabel(
					leftColumnLayout,
					EventDto.START_DATE,
					EventHelper.buildEventDateString(superordinateEvent.getStartDate(), superordinateEvent.getEndDate()),
					I18nProperties.getCaption(Captions.singleDayEventDate));
			}
			add(leftColumnLayout);

			VerticalLayout rightColumnLayout = new VerticalLayout();
			rightColumnLayout.setPadding(false);
			rightColumnLayout.setSpacing(true);
			{
				addDescLabel(
					rightColumnLayout,
					EventDto.REPORT_DATE_TIME,
					DateFormatHelper.formatDate(superordinateEvent.getReportDateTime()),
					I18nProperties.getPrefixCaption(EventDto.I18N_PREFIX, EventDto.REPORT_DATE_TIME));

				addDescLabel(
					rightColumnLayout,
					EventDto.EVENT_STATUS,
					superordinateEvent.getEventStatus().toString(),
					I18nProperties.getPrefixCaption(EventDto.I18N_PREFIX, EventDto.EVENT_STATUS));
			}
			add(rightColumnLayout);
		}
	}

}
