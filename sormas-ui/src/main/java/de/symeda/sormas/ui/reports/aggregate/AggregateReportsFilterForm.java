package de.symeda.sormas.ui.reports.aggregate;

import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.Descriptions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.facility.FacilityReferenceDto;
import de.symeda.sormas.api.infrastructure.pointofentry.PointOfEntryReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.report.AggregateCaseCountDto;
import de.symeda.sormas.api.report.AggregateReportCriteria;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.EpiWeek;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.AbstractFilterForm;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.FieldConfiguration;
import de.symeda.sormas.ui.utils.FieldHelper;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.fields.KeepValueComboBox;

public class AggregateReportsFilterForm extends AbstractFilterForm<AggregateReportCriteria> {

	private static final String EPI_WEEK_LOC = "epiWeekLoc";

	private ComboBox regionFilter;
	private ComboBox districtFilter;
	private ComboBox facilityFilter;
	private ComboBox pointOfEntryFilter;
	private ComboBox diseaseFilter;

	// Filters
	private HorizontalLayout hlSecondFilterRow;
	private com.vaadin.flow.component.combobox.ComboBox<Integer> cbFromYearFilter;
	// the epi week items change with the year: they keep their value as Vaadin 8 did (KeepValueComboBox)
	private KeepValueComboBox<EpiWeek> cbFromEpiWeekFilter;
	private com.vaadin.flow.component.combobox.ComboBox<Integer> cbToYearFilter;
	private KeepValueComboBox<EpiWeek> cbToEpiWeekFilter;

	protected AggregateReportsFilterForm() {
		super(
			AggregateReportCriteria.class,
			AggregateCaseCountDto.I18N_PREFIX,
			FieldVisibilityCheckers.withCountry(FacadeProvider.getConfigFacade().getCountryLocale()),
			null);
	}

	@Override
	protected String[] getMainFilterLocators() {

		return new String[] {
			AggregateReportCriteria.REGION,
			AggregateReportCriteria.DISTRICT,
			AggregateReportCriteria.HEALTH_FACILITY,
			AggregateReportCriteria.POINT_OF_ENTRY,
			AggregateReportCriteria.DISEASE,
			EPI_WEEK_LOC };
	}

	@Override
	protected void addFields() {

		regionFilter =
			addField(FieldConfiguration.withCaptionAndPixelSized(AggregateReportCriteria.REGION, I18nProperties.getCaption(Captions.Region), 200));
		regionFilter.setInputPrompt(I18nProperties.getString(Strings.promptAllRegions));
		regionFilter.addItems(FacadeProvider.getRegionFacade().getAllActiveByServerCountry());

		districtFilter = addField(
			FieldConfiguration.withCaptionAndPixelSized(AggregateReportCriteria.DISTRICT, I18nProperties.getCaption(Captions.District), 200));
		districtFilter.setInputPrompt(I18nProperties.getString(Strings.promptAllDistricts));

		if (!UiUtil.isPortHealthUser()) {
			facilityFilter = addField(getContent(), FieldConfiguration.pixelSized(AggregateReportCriteria.HEALTH_FACILITY, 200));
			facilityFilter.setDescription(I18nProperties.getDescription(Descriptions.descFacilityFilter));
		}

		if (UiUtil.permitted(UserRight.PORT_HEALTH_INFO_VIEW)) {
			pointOfEntryFilter = addField(getContent(), FieldConfiguration.pixelSized(CaseDataDto.POINT_OF_ENTRY, 200));
			pointOfEntryFilter.setDescription(I18nProperties.getDescription(Descriptions.descPointOfEntryFilter));
		}

		UserDto user = currentUserDto();
		final RegionReferenceDto userRegion = user.getRegion();
		final DistrictReferenceDto userDistrict = user.getDistrict();
		final FacilityReferenceDto userFacility = user.getHealthFacility();
		final PointOfEntryReferenceDto userPointOfEntry = user.getPointOfEntry();

		if (userRegion != null) {
			regionFilter.setEnabled(false);
			districtFilter.addItems(FacadeProvider.getDistrictFacade().getAllActiveByRegion(userRegion.getUuid()));
			if (userDistrict != null) {
				districtFilter.setEnabled(false);
				Optional.ofNullable(facilityFilter)
					.ifPresent(f -> f.addItems(FacadeProvider.getFacilityFacade().getActiveHospitalsByDistrict(userDistrict, false)));
				Optional.ofNullable(pointOfEntryFilter)
					.ifPresent(p -> p.addItems(FacadeProvider.getPointOfEntryFacade().getAllActiveByDistrict(userDistrict.getUuid(), false)));
				if (userFacility != null || userPointOfEntry != null) {
					Optional.ofNullable(facilityFilter).ifPresent(f -> f.setEnabled(false));
					Optional.ofNullable(pointOfEntryFilter).ifPresent(p -> p.setEnabled(false));
				}
			}
		}

		diseaseFilter = addField(FieldConfiguration.pixelSized(AggregateReportCriteria.DISEASE, 200), ComboBox.class);
		List<Disease> aggregateDiseases = FacadeProvider.getDiseaseConfigurationFacade().getAllDiseases(true, null, false, true);
		FieldHelper.updateItems(diseaseFilter, aggregateDiseases);
		for (Object r : aggregateDiseases) {
			diseaseFilter.setItemCaption(r, r.toString());
		}

		getContent().addComponent(createEpiWeekFilterBar(), EPI_WEEK_LOC);
	}

	@Override
	protected void applyDependenciesOnFieldChange(String propertyId, HasValue.ValueChangeEvent<?> event) {

		super.applyDependenciesOnFieldChange(propertyId, event);

		final UserDto user = currentUserDto();

		switch (propertyId) {
		case AggregateReportCriteria.REGION: {
			final RegionReferenceDto region = user.getRegion() != null ? user.getRegion() : (RegionReferenceDto) event.getValue();
			if (region != null) {
				enableFields(districtFilter);
				FieldHelper.updateItems(districtFilter, FacadeProvider.getDistrictFacade().getAllActiveByRegion(region.getUuid()));
				Optional.ofNullable(facilityFilter).ifPresent(ComboBox::clear);
				Optional.ofNullable(pointOfEntryFilter).ifPresent(ComboBox::clear);
				if (user.getDistrict() != null) {
					disableFields(districtFilter);
				}
			} else {
				clearAndDisableFields(districtFilter, facilityFilter, pointOfEntryFilter);
			}
			break;
		}

		case AggregateReportCriteria.DISTRICT: {
			final DistrictReferenceDto newDistrict = (DistrictReferenceDto) event.getValue();
			if (newDistrict != null) {
				clearAndDisableFields(facilityFilter, pointOfEntryFilter);
				enableFields(facilityFilter, pointOfEntryFilter);
				if (facilityFilter != null) {
					FieldHelper.updateItems(facilityFilter, FacadeProvider.getFacilityFacade().getActiveHospitalsByDistrict(newDistrict, false));
				}
				if (pointOfEntryFilter != null) {
					FieldHelper
						.updateItems(pointOfEntryFilter, FacadeProvider.getPointOfEntryFacade().getAllActiveByDistrict(newDistrict.getUuid(), false));
				}
			} else {
				clearAndDisableFields(facilityFilter, pointOfEntryFilter);
			}
			break;
		}
		case AggregateReportCriteria.HEALTH_FACILITY: {
			final FacilityReferenceDto facility = (FacilityReferenceDto) event.getValue();

			if (facility != null) {
				clearAndDisableFields(pointOfEntryFilter);
			} else {
				enableFields(pointOfEntryFilter);
			}
			break;
		}
		case AggregateReportCriteria.POINT_OF_ENTRY: {
			final PointOfEntryReferenceDto pointOfEntry = (PointOfEntryReferenceDto) event.getValue();

			if (pointOfEntry != null) {
				clearAndDisableFields(facilityFilter);
			} else {
				enableFields(facilityFilter);
			}
			break;
		}
		}
	}

	private HorizontalLayout createEpiWeekFilterBar() {
		hlSecondFilterRow = new HorizontalLayout();
		hlSecondFilterRow.setPadding(false);
		hlSecondFilterRow.setSpacing(true);
		hlSecondFilterRow.setWidth(null);

		Span lblFrom = new Span(I18nProperties.getCaption(Captions.from));
		CssStyles.style(lblFrom, CssStyles.LABEL_BOLD, CssStyles.VSPACE_TOP_4);
		hlSecondFilterRow.add(lblFrom);

		cbFromYearFilter = new com.vaadin.flow.component.combobox.ComboBox<>();
		cbFromYearFilter.setId("yearFrom");
		// was the Vaadin 8 empty item (empty selection allowed by default)
		cbFromYearFilter.setClearButtonVisible(true);
		cbFromYearFilter.addValueChangeListener(e -> clearFilterIfEmpty(cbFromYearFilter, cbFromEpiWeekFilter));
		cbFromEpiWeekFilter = new KeepValueComboBox<>();
		cbFromEpiWeekFilter.setId(AggregateReportCriteria.EPI_WEEK_FROM);
		cbFromEpiWeekFilter.setClearButtonVisible(true);
		cbFromEpiWeekFilter.addValueChangeListener(e -> getValue().setEpiWeekFrom(e.getValue()));
		cbToYearFilter = new com.vaadin.flow.component.combobox.ComboBox<>();
		cbToYearFilter.setId("yearTo");
		cbToYearFilter.setClearButtonVisible(true);
		cbToYearFilter.addValueChangeListener(e -> clearFilterIfEmpty(cbFromYearFilter, cbToEpiWeekFilter));
		cbToEpiWeekFilter = new KeepValueComboBox<>();
		cbToEpiWeekFilter.setId(AggregateReportCriteria.EPI_WEEK_TO);
		cbToEpiWeekFilter.setClearButtonVisible(true);
		cbToEpiWeekFilter.addValueChangeListener(e -> getValue().setEpiWeekTo(e.getValue()));

		cbFromYearFilter.setWidth(140, Unit.PIXELS);
		cbFromYearFilter.setPlaceholder(I18nProperties.getString(Strings.year));
		cbFromYearFilter.setItems(DateHelper.getYearsToNow(2000));
		cbFromYearFilter.addValueChangeListener(e -> {
			cbFromEpiWeekFilter.clear();
			if (e.getValue() != null) {
				cbFromEpiWeekFilter.setItems(DateHelper.createEpiWeekList(e.getValue()));
			}
		});
		hlSecondFilterRow.add(cbFromYearFilter);

		cbFromEpiWeekFilter.setWidth(200, Unit.PIXELS);
		cbFromEpiWeekFilter.setPlaceholder(I18nProperties.getString(Strings.epiWeek));

		hlSecondFilterRow.add(cbFromEpiWeekFilter);

		Span lblTo = new Span(I18nProperties.getCaption(Captions.to));
		CssStyles.style(lblTo, CssStyles.LABEL_BOLD, CssStyles.VSPACE_TOP_4);
		hlSecondFilterRow.add(lblTo);

		cbToYearFilter.setWidth(140, Unit.PIXELS);
		cbToYearFilter.setPlaceholder(I18nProperties.getString(Strings.year));
		cbToYearFilter.setItems(DateHelper.getYearsToNow(2000));
		cbToYearFilter.addValueChangeListener(e -> {
			cbToEpiWeekFilter.clear();
			if (e.getValue() != null) {
				cbToEpiWeekFilter.setItems(DateHelper.createEpiWeekList(e.getValue()));
			}
		});
		hlSecondFilterRow.add(cbToYearFilter);

		cbToEpiWeekFilter.setWidth(200, Unit.PIXELS);
		cbToEpiWeekFilter.setPlaceholder(I18nProperties.getString(Strings.epiWeek));
		hlSecondFilterRow.add(cbToEpiWeekFilter);

		return hlSecondFilterRow;
	}

	private void clearFilterIfEmpty(com.vaadin.flow.component.combobox.ComboBox<?> filter1, com.vaadin.flow.component.combobox.ComboBox<?> filter2) {
		if (filter1.getValue() == null) {
			filter2.clear();
		}
	}

	@Override
	public void setValue(AggregateReportCriteria newFieldValue) {
		super.setValue(newFieldValue);

		if (newFieldValue.getEpiWeekFrom() != null) {
			cbFromYearFilter.setValue(newFieldValue.getEpiWeekFrom().getYear());
			cbFromEpiWeekFilter.setValue(newFieldValue.getEpiWeekFrom());
		}

		if (newFieldValue.getEpiWeekTo() != null) {
			cbToYearFilter.setValue(newFieldValue.getEpiWeekTo().getYear());
			cbToEpiWeekFilter.setValue(newFieldValue.getEpiWeekTo());
		}
	}

	@Override
	protected void applyDependenciesOnNewValue(AggregateReportCriteria criteria) {

		applyRegionFilterDependency(criteria.getRegion(), AggregateReportCriteria.DISTRICT);

		if (criteria.getDistrict() != null) {
			if (facilityFilter != null) {
				FieldHelper
					.updateItems(facilityFilter, FacadeProvider.getFacilityFacade().getActiveHospitalsByDistrict(criteria.getDistrict(), false));
			}
			if (pointOfEntryFilter != null) {
				FieldHelper.updateItems(
					pointOfEntryFilter,
					FacadeProvider.getPointOfEntryFacade().getAllActiveByDistrict(criteria.getDistrict().getUuid(), false));
			}
		}

		Optional.ofNullable(facilityFilter).ifPresent(f -> f.setValue(criteria.getHealthFacility()));
		if (criteria.getHealthFacility() != null) {
			clearAndDisableFields(pointOfEntryFilter);
		}

		Optional.ofNullable(pointOfEntryFilter).ifPresent(p -> p.setValue(criteria.getPointOfEntry()));

		if (criteria.getPointOfEntry() != null) {
			clearAndDisableFields(facilityFilter);
		}
	}
}
