package de.symeda.sormas.ui.caze.components.caseselection;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.symeda.sormas.api.CountryHelper;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.AgeAndBirthDateDto;
import de.symeda.sormas.api.caze.CaseSelectionDto;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.AgeAndBirthDateDtoConverterV7;
import de.symeda.sormas.ui.utils.V7UuidRenderer;
import de.symeda.sormas.ui.utils.grid.Grid;
import de.symeda.sormas.ui.utils.grid.HeightMode;
import de.symeda.sormas.ui.utils.grid.renderers.DateRenderer;
import de.symeda.sormas.ui.utils.grid.renderers.TextRenderer;

@SuppressWarnings("serial")
public class CaseSelectionGrid extends Grid<CaseSelectionDto> {

	private List<CaseSelectionDto> cases;

	public CaseSelectionGrid(List<CaseSelectionDto> cases) {

		super(CaseSelectionDto.class);

		if (cases != null) {
			this.cases = cases;
		} else {
			this.cases = new ArrayList<>();
		}
		buildGrid();
		reload();
	}

	@SuppressWarnings("unchecked")
	private void buildGrid() {

		setSizeFull();
		setSelectionMode(SelectionMode.SINGLE);
		setHeightMode(HeightMode.ROW);

		setColumns(
			CaseSelectionDto.UUID,
			CaseSelectionDto.EPID_NUMBER,
			CaseSelectionDto.EXTERNAL_ID,
			CaseSelectionDto.PERSON_FIRST_NAME,
			CaseSelectionDto.PERSON_LAST_NAME,
			CaseSelectionDto.AGE_AND_BIRTH_DATE,
			CaseSelectionDto.RESPONSIBLE_DISTRICT_NAME,
			CaseSelectionDto.HEALTH_FACILITY_NAME,
			CaseSelectionDto.REPORT_DATE,
			CaseSelectionDto.SEX,
			CaseSelectionDto.CASE_CLASSIFICATION,
			CaseSelectionDto.OUTCOME);

		if (FacadeProvider.getConfigFacade().isConfiguredCountry(CountryHelper.COUNTRY_CODE_GERMANY)) {
			getColumn(CaseSelectionDto.EPID_NUMBER).setHidden(true);
		} else {
			getColumn(CaseSelectionDto.EXTERNAL_ID).setHidden(true);
		}

		getColumn(CaseSelectionDto.UUID).setRenderer(new V7UuidRenderer());
		AgeAndBirthDateDtoConverterV7 ageAndBirthDateConverter = new AgeAndBirthDateDtoConverterV7();
		((Column<CaseSelectionDto, AgeAndBirthDateDto>) getColumn(CaseSelectionDto.AGE_AND_BIRTH_DATE))
			.setRenderer(value -> ageAndBirthDateConverter.convertToPresentation(value, null), new TextRenderer());
		((Column<CaseSelectionDto, Date>) getColumn(CaseSelectionDto.REPORT_DATE))
			.setRenderer(new DateRenderer(DateHelper.getLocalDateTimeFormat(I18nProperties.getUserLanguage())));

		if (UiUtil.enabled(FeatureType.HIDE_JURISDICTION_FIELDS)) {
			getColumn(CaseSelectionDto.RESPONSIBLE_DISTRICT_NAME).setHidden(true);
		}

		for (Column<CaseSelectionDto, ?> column : getAllColumns()) {
			column.setHeaderCaption(
				I18nProperties.getPrefixCaption(CaseSelectionDto.I18N_PREFIX, column.getPropertyId().toString(), column.getHeaderCaption()));
		}
	}

	public void reload() {

		setItems(cases);
		setHeightByRows(cases.size() > 0 ? (cases.size() <= 10 ? cases.size() : 10) : 2);
	}

	public void setCases(List<CaseSelectionDto> cases) {
		this.cases = cases;
		reload();
	}

	public void clearCases() {
		cases.clear();
		reload();
	}
}
