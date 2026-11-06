/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2021 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.caze;

import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.contact.SimilarContactDto;
import de.symeda.sormas.api.event.SimilarEventParticipantDto;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.contact.ContactCaseConversionSelectionGrid;
import de.symeda.sormas.ui.events.EventParticipantCaseConversionSelectionGrid;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class ConvertToCaseSelectionField extends VerticalLayout {

	private ContactCaseConversionSelectionGrid contactSelectionGrid;
	private EventParticipantCaseConversionSelectionGrid eventParticipantSelectionGrid;

	private CaseDataDto caseDataDto;
	private List<SimilarContactDto> matchingContacts;
	private List<SimilarEventParticipantDto> matchingEventParticipants;

	public ConvertToCaseSelectionField(
		CaseDataDto caseDataDto,
		List<SimilarContactDto> matchingContacts,
		List<SimilarEventParticipantDto> matchingEventParticipants) {

		this.caseDataDto = caseDataDto;
		this.matchingContacts = matchingContacts;
		this.matchingEventParticipants = matchingEventParticipants;

		setPadding(true);
		setSpacing(true);

		add(VaadinUiUtil.createInfoComponent(I18nProperties.getString(Strings.infoConvertToCaseSelect)));
		addContactDetailsComponent();

		if (matchingContacts.size() > 0) {
			contactSelectionGrid = new ContactCaseConversionSelectionGrid(matchingContacts);
			contactSelectionGrid.setSelectionMode(Grid.SelectionMode.MULTI);
			// was the grid's caption
			add(createCaption(I18nProperties.getString(Strings.entityContacts)));
			add(contactSelectionGrid);
		}

		if (matchingEventParticipants.size() > 0) {
			eventParticipantSelectionGrid = new EventParticipantCaseConversionSelectionGrid(matchingEventParticipants);
			eventParticipantSelectionGrid.setSelectionMode(Grid.SelectionMode.MULTI);
			// was the grid's caption
			add(createCaption(I18nProperties.getString(Strings.entityEventParticipants)));
			add(eventParticipantSelectionGrid);
		}
	}

	private void addContactDetailsComponent() {

		HorizontalLayout contactDetailsLayout = new HorizontalLayout();
		contactDetailsLayout.setSpacing(true);

		final Span lblUuid = new Span(DataHelper.getShortUuid(caseDataDto));
		String lblUuidCaption = I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.UUID);
		lblUuid.setWidth(null);
		contactDetailsLayout.add(new Div(createCaption(lblUuidCaption), new Div(lblUuid)));

		final Span lblFirstName = new Span(caseDataDto.getPerson().getFirstName());
		lblFirstName.setWidth(null);
		String lblFirstNameCaption = I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, PersonDto.FIRST_NAME);
		contactDetailsLayout.add(new Div(createCaption(lblFirstNameCaption), new Div(lblFirstName)));

		final Span lblLastName = new Span(caseDataDto.getPerson().getLastName());
		lblLastName.setWidth(null);
		String lblLastNameCaption = I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, PersonDto.LAST_NAME);
		contactDetailsLayout.add(new Div(createCaption(lblLastNameCaption), new Div(lblLastName)));

		if (UiUtil.disabled(FeatureType.HIDE_JURISDICTION_FIELDS)) {
			final Span lblRegion = new Span(caseDataDto.getResponsibleRegion().buildCaption());
			lblRegion.setWidth(null);
			String lblRegionCaption = I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.RESPONSIBLE_REGION);
			contactDetailsLayout.add(new Div(createCaption(lblRegionCaption), new Div(lblRegion)));

			final Span lblDistrict = new Span(caseDataDto.getResponsibleDistrict().buildCaption());
			lblDistrict.setWidth(null);
			String lblDistrictCaption = I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.RESPONSIBLE_DISTRICT);
			contactDetailsLayout.add(new Div(createCaption(lblDistrictCaption), new Div(lblDistrict)));
		}

		add(new Scroller(contactDetailsLayout));
	}

	/**
	 * Was a Vaadin 8 component caption: shown above the component.
	 */
	private static Span createCaption(String caption) {
		Span captionLabel = new Span(caption);
		captionLabel.addClassName("v-caption");
		return captionLabel;
	}

	public List<SimilarContactDto> getSelectedContacts() {
		return contactSelectionGrid != null
			? contactSelectionGrid.getSelectedItems().stream().map(item -> (SimilarContactDto) item).collect(Collectors.toList())
			: matchingContacts;
	}

	public List<SimilarEventParticipantDto> getSelectedEventParticipants() {
		return eventParticipantSelectionGrid != null
			? eventParticipantSelectionGrid.getSelectedItems().stream().map(item -> (SimilarEventParticipantDto) item).collect(Collectors.toList())
			: matchingEventParticipants;
	}
}
