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
package de.symeda.sormas.ui.dashboard.map;

import java.util.Date;
import java.util.List;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.function.ValueProvider;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.infrastructure.facility.FacilityHelper;
import de.symeda.sormas.api.infrastructure.facility.FacilityReferenceDto;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.dashboard.HasDashboardCasePopupGrid;
import de.symeda.sormas.ui.utils.GridExportStreamResource;
import de.symeda.sormas.ui.utils.V7UuidRenderer;
import de.symeda.sormas.ui.utils.grid.Grid;
import de.symeda.sormas.ui.utils.grid.renderers.DateRenderer;

@SuppressWarnings({
	"serial",
	"unchecked" })
public class CasePopupGrid extends Grid<CaseDataDto> {

	public static final String DISEASE_SHORT = Captions.columnDiseaseShort;
	public static final String FIRST_NAME = PersonDto.FIRST_NAME;
	public static final String LAST_NAME = PersonDto.LAST_NAME;

	private final FacilityReferenceDto facility;
	private final HasDashboardCasePopupGrid dashboardMapComponent;

	public CasePopupGrid(Dialog window, FacilityReferenceDto facility, HasDashboardCasePopupGrid dashboardMapComponent) {

		super(CaseDataDto.class);
		this.facility = facility;
		this.dashboardMapComponent = dashboardMapComponent;
		setWidth(960, Unit.PIXELS);
		setHeightUndefined();

		setSelectionMode(SelectionMode.NONE);

		// were generated properties of the V7 container
		addGeneratedColumn(DISEASE_SHORT, caseDataDto -> {
			String diseaseName = caseDataDto.getDisease().getName();
			return Disease.valueOf(diseaseName).toShortString();
		});

		addGeneratedColumn(FIRST_NAME, caseDataDto -> {
			PersonDto personDto = FacadeProvider.getPersonFacade().getByUuid(caseDataDto.getPerson().getUuid());
			return personDto.getFirstName();
		});

		addGeneratedColumn(LAST_NAME, caseDataDto -> {
			PersonDto personDto = FacadeProvider.getPersonFacade().getByUuid(caseDataDto.getPerson().getUuid());
			return personDto.getLastName();
		});

		setColumns(
			CaseDataDto.UUID,
			DISEASE_SHORT,
			CaseDataDto.CASE_CLASSIFICATION,
			FIRST_NAME,
			LAST_NAME,
			CaseDataDto.REPORT_DATE,
			CaseDataDto.HEALTH_FACILITY_DETAILS);

		getColumn(CaseDataDto.UUID).setRenderer(new V7UuidRenderer());
		Language userLanguage = I18nProperties.getUserLanguage();
		((Column<CaseDataDto, Date>) getColumn(CaseDataDto.REPORT_DATE))
			.setRenderer(new DateRenderer(DateHelper.getLocalDateTimeFormat(userLanguage)));

		if (facility == null || !FacilityHelper.isOtherOrNoneHealthFacility(facility.getUuid())) {
			getColumn(CaseDataDto.HEALTH_FACILITY_DETAILS).setHidden(true);
		}

		for (Column<CaseDataDto, ?> column : getAllColumns()) {
			column.setHeaderCaption(
				I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, column.getPropertyId().toString(), column.getHeaderCaption()));
		}

		addItemClickListener(e -> {
			window.close();
			ControllerProvider.getCaseController().navigateToCase(e.getItem().getUuid(), true);
		});

		reload();
	}

	private void addGeneratedColumn(String key, ValueProvider<CaseDataDto, String> valueProvider) {
		GridExportStreamResource.setExportValueProvider(addColumn(valueProvider).setKey(key), valueProvider);
	}

	public void reload() {
		List<CaseDataDto> cases = dashboardMapComponent.getCasesForFacility(facility);

		setItems(cases);
		this.setHeightByRows(cases.size());
	}
}
