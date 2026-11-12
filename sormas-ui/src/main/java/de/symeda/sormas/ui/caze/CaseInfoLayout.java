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

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.CountryHelper;
import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.DiseaseHelper;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.person.ApproximateAgeType.ApproximateAgeHelper;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.symptoms.SymptomsDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.AbstractInfoLayout;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.FieldAccessHelper;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

@SuppressWarnings("serial")
public class CaseInfoLayout extends AbstractInfoLayout<CaseDataDto> {

	private final CaseDataDto caseDto;
	private final boolean isTravelEntry;

	public CaseInfoLayout(CaseDataDto caseDto) {
		this(caseDto, false);
	}

	public CaseInfoLayout(CaseDataDto caseDto, boolean isTravelEntry) {
		super(CaseDataDto.class, FieldAccessHelper.getFieldAccessCheckers(caseDto));

		this.caseDto = caseDto;
		this.isTravelEntry = isTravelEntry;
		setSpacing(true);
		setPadding(false);
		setWidth(100, Unit.PERCENTAGE);
		updateCaseInfo();
	}

	private void updateCaseInfo() {

		this.removeAll();

		PersonDto personDto = FacadeProvider.getPersonFacade().getByUuid(caseDto.getPerson().getUuid());

		VerticalLayout leftColumnLayout = new VerticalLayout();
		leftColumnLayout.setPadding(false);
		leftColumnLayout.setSpacing(true);
		boolean hasUserRightCaseView = UiUtil.permitted(UserRight.CASE_VIEW);
		{
			final Span caseIdLabel = addDescLabel(
				leftColumnLayout,
				CaseDataDto.UUID,
				DataHelper.getShortUuid(caseDto.getUuid()),
				I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.UUID));
			caseIdLabel.setId("caseIdLabel");
			caseIdLabel.setTitle(caseDto.getUuid());

			if (FacadeProvider.getConfigFacade().isConfiguredCountry(CountryHelper.COUNTRY_CODE_GERMANY)) {
				VaadinUiUtil.setDescription(
					addDescLabel(
						leftColumnLayout,
						CaseDataDto.EXTERNAL_ID,
						caseDto.getExternalID(),
						I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.EXTERNAL_ID)),
					caseDto.getEpidNumber());
				VaadinUiUtil.setDescription(
					addDescLabel(
						leftColumnLayout,
						CaseDataDto.EXTERNAL_TOKEN,
						caseDto.getExternalToken(),
						I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.EXTERNAL_TOKEN)),
					caseDto.getExternalToken());
			} else {
				VaadinUiUtil.setDescription(
					addDescLabel(
						leftColumnLayout,
						CaseDataDto.EPID_NUMBER,
						caseDto.getEpidNumber(),
						I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.EPID_NUMBER)),
					caseDto.getEpidNumber());
			}

			if (hasUserRightCaseView) {
				addDescLabel(
					leftColumnLayout,
					CaseDataDto.PERSON,
					caseDto.getPerson().buildCaption(),
					I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.PERSON));

				HorizontalLayout ageSexLayout = new HorizontalLayout();
				ageSexLayout.setPadding(false);
				ageSexLayout.setSpacing(true);
				addCustomDescLabel(
					ageSexLayout,
					PersonDto.class,
					PersonDto.APPROXIMATE_AGE,
					ApproximateAgeHelper.formatApproximateAge(personDto.getApproximateAge(), personDto.getApproximateAgeType()),
					I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, PersonDto.APPROXIMATE_AGE));
				addCustomDescLabel(
					ageSexLayout,
					PersonDto.class,
					PersonDto.SEX,
					personDto.getSex(),
					I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, PersonDto.SEX));
				leftColumnLayout.add(ageSexLayout);
			}

			if (UiUtil.permitted(UserRight.CASE_CLINICIAN_VIEW)) {
				addDescLabel(
					leftColumnLayout,
					CaseDataDto.CLINICIAN_NAME,
					caseDto.getClinicianName(),
					I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.CLINICIAN_NAME));
			}
			if (isTravelEntry && hasUserRightCaseView) {
				Anchor linkToData = ControllerProvider.getCaseController()
					.createLinkToData(caseDto.getUuid(), I18nProperties.getCaption(Captions.travelEntryOpenResultingCase));
				leftColumnLayout.add(linkToData);
				linkToData.setWidthFull();
			}
		}
		this.add(leftColumnLayout);

		VerticalLayout rightColumnLayout = new VerticalLayout();
		rightColumnLayout.setPadding(false);
		rightColumnLayout.setSpacing(true);
		{
			if (!isTravelEntry) {
				addDescLabel(
					rightColumnLayout,
					CaseDataDto.DISEASE,
					caseDto.getDisease() != Disease.OTHER
						? DiseaseHelper.toString(caseDto.getDisease(), caseDto.getDiseaseDetails(), caseDto.getDiseaseVariant())
						: DataHelper.toStringNullable(caseDto.getDiseaseDetails()),
					I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.DISEASE));
			} else {
				addDescLabel(rightColumnLayout, CaseDataDto.DISEASE, "", "");
			}

			if (hasUserRightCaseView) {
				addDescLabel(
					rightColumnLayout,
					CaseDataDto.CASE_CLASSIFICATION,
					caseDto.getCaseClassification(),
					I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.CASE_CLASSIFICATION));
				addDescLabel(
					rightColumnLayout,
					CaseDataDto.SYMPTOMS,
					DateFormatHelper.formatDate(caseDto.getSymptoms().getOnsetDate()),
					I18nProperties.getPrefixCaption(SymptomsDto.I18N_PREFIX, SymptomsDto.ONSET_DATE));
			}
		}
		this.add(rightColumnLayout);
	}
}
