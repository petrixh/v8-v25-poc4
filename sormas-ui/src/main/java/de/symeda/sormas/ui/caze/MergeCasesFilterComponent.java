package de.symeda.sormas.ui.caze;

import java.time.ZoneId;
import java.util.function.Consumer;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.converter.LocalDateToDateConverter;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.ReferenceDto;
import de.symeda.sormas.api.caze.CaseCriteria;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.caze.NewCaseDateType;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.QueryDetails;

@SuppressWarnings("serial")
public class MergeCasesFilterComponent extends VerticalLayout {

	// Layouts
	private HorizontalLayout firstRowLayout;
	private HorizontalLayout secondRowLayout;
	private HorizontalLayout thirdRowLayout;

	private DatePicker dfCreationDateFrom;
	private DatePicker dfCreationDateTo;
	private ComboBox<Disease> cbDisease;
	private TextField tfSearch;
	private TextField eventSearch;
	private TextField tfReportingUser;
	private Checkbox cbIgnoreRegion;
	private ComboBox<RegionReferenceDto> cbRegion;
	private ComboBox<DistrictReferenceDto> cbDistrict;
	private ComboBox<NewCaseDateType> cbNewCaseDateType;
	private DatePicker dfNewCaseDateFrom;
	private DatePicker dfNewCaseDateTo;
	private Button btnConfirmFilters;
	private Button btnResetFilters;

	private Binder<CaseCriteria> criteriaBinder = new Binder<>(CaseCriteria.class);
	private CaseCriteria criteria;
	private Binder<QueryDetails> queryDetailsBinder = new Binder<>(QueryDetails.class);
	private QueryDetails queryDetails;
	private Runnable filtersUpdatedCallback;
	private Consumer<Boolean> ignoreRegionCallback;

	private Span lblNumberOfDuplicates;
	private ComboBox<EntityRelevanceStatus> relevanceStatusFilter;

	public MergeCasesFilterComponent(CaseCriteria criteria, QueryDetails queryDetails) {

		setSpacing(false);
		setPadding(false);
		setWidth(100, Unit.PERCENTAGE);

		addFirstRowLayout();
		if (UiUtil.disabled(FeatureType.HIDE_JURISDICTION_FIELDS)) {
			addSecondRowLayout();
		}
		addThirdRowLayout();

		setValue(criteria, queryDetails);
	}

	public void setValue(CaseCriteria criteria, QueryDetails queryDetails) {
		this.criteria = criteria;
		this.queryDetails = queryDetails;

		criteriaBinder.readBean(this.criteria);
		queryDetailsBinder.readBean(this.queryDetails);
	}

	private void addFirstRowLayout() {

		firstRowLayout = new HorizontalLayout();
		firstRowLayout.setPadding(false);
		firstRowLayout.setWidth(100, Unit.PERCENTAGE);

		dfCreationDateFrom = new DatePicker();
		dfCreationDateFrom.setId(CaseCriteria.CREATION_DATE_FROM);
		dfCreationDateFrom.setWidth(120, Unit.PIXELS);
		dfCreationDateFrom.setPlaceholder(I18nProperties.getString(Strings.promptCreationDateFrom));
		dfCreationDateFrom.setLabel(I18nProperties.getCaption(Captions.creationDate));
		criteriaBinder.forField(dfCreationDateFrom)
			.withConverter(new LocalDateToDateConverter(ZoneId.systemDefault()))
			.bind(CaseCriteria.CREATION_DATE_FROM);
		firstRowLayout.add(dfCreationDateFrom);

		dfCreationDateTo = new DatePicker();
		dfCreationDateTo.setId(CaseCriteria.CREATION_DATE_TO);
		dfCreationDateTo.setWidth(120, Unit.PIXELS);
		CssStyles.style(dfCreationDateTo, CssStyles.FORCE_CAPTION);
		dfCreationDateTo.setPlaceholder(I18nProperties.getString(Strings.promptDateTo));
		criteriaBinder.forField(dfCreationDateTo)
			.withConverter(new LocalDateToDateConverter(ZoneId.systemDefault()))
			.bind(CaseCriteria.CREATION_DATE_TO);
		firstRowLayout.add(dfCreationDateTo);

		cbDisease = new ComboBox<>();
		cbDisease.setId(CaseDataDto.DISEASE);
		cbDisease.setWidth(200, Unit.PIXELS);
		CssStyles.style(cbDisease, CssStyles.FORCE_CAPTION);
		cbDisease.setPlaceholder(I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.DISEASE));
		cbDisease.setItems(FacadeProvider.getDiseaseConfigurationFacade().getAllDiseases(true, true, true));
		criteriaBinder.bind(cbDisease, CaseDataDto.DISEASE);
		firstRowLayout.add(cbDisease);

		tfSearch = new TextField();
		tfSearch.setId(CaseCriteria.CASE_LIKE);
		tfSearch.setWidth(200, Unit.PIXELS);
		CssStyles.style(tfSearch, CssStyles.FORCE_CAPTION);
		tfSearch.setPlaceholder(I18nProperties.getString(Strings.promptCasesSearchField));
		criteriaBinder.bind(tfSearch, CaseCriteria.CASE_LIKE);
		firstRowLayout.add(tfSearch);

		// Temporarily disabled because #9054 has introduced CaseService.hasAnyToManyJoin which leads to an error 
		// when trying to search for duplicates by any property related to events (documented in #11712)
//		eventSearch = new TextField();
//		eventSearch.setId(CaseCriteria.EVENT_LIKE);
//		eventSearch.setWidth(200, Unit.PIXELS);
//		CssStyles.style(eventSearch, CssStyles.FORCE_CAPTION);
//		eventSearch.setPlaceholder(I18nProperties.getString(Strings.promptCaseOrContactEventSearchField));
//		criteriaBinder.bind(eventSearch, CaseCriteria.EVENT_LIKE);
//		firstRowLayout.add(eventSearch);

		tfReportingUser = new TextField();
		tfReportingUser.setId(CaseCriteria.REPORTING_USER_LIKE);
		tfReportingUser.setWidth(200, Unit.PIXELS);
		CssStyles.style(tfReportingUser, CssStyles.FORCE_CAPTION);
		tfReportingUser.setPlaceholder(I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.REPORTING_USER));
		criteriaBinder.bind(tfReportingUser, CaseCriteria.REPORTING_USER_LIKE);
		firstRowLayout.add(tfReportingUser);
		firstRowLayout.setFlexGrow(1, tfReportingUser);

		add(firstRowLayout);
	}

	private void addSecondRowLayout() {

		secondRowLayout = new HorizontalLayout();
		secondRowLayout.setPadding(false);
		secondRowLayout.setWidth(100, Unit.PERCENTAGE);

		cbRegion = new ComboBox<>();
		cbDistrict = new ComboBox<>();
		cbRegion.setItemLabelGenerator(ReferenceDto::buildCaption);
		cbDistrict.setItemLabelGenerator(ReferenceDto::buildCaption);

		cbRegion.setId(CaseDataDto.REGION);
		cbRegion.setWidth(200, Unit.PIXELS);
		CssStyles.style(cbRegion, CssStyles.FORCE_CAPTION);
		cbRegion.setPlaceholder(I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.REGION));
		cbRegion.setItems(FacadeProvider.getRegionFacade().getAllActiveByServerCountry());
		criteriaBinder.bind(cbRegion, CaseDataDto.REGION);
		cbRegion.addValueChangeListener(e -> {
			RegionReferenceDto region = e.getValue();
			cbDistrict.clear();
			if (region != null) {
				cbDistrict.setItems(FacadeProvider.getDistrictFacade().getAllActiveByRegion(region.getUuid()));
				cbDistrict.setEnabled(true);
			} else {
				cbDistrict.setEnabled(false);
			}
		});
		secondRowLayout.add(cbRegion);
		if (UiUtil.getUser().getRegion() != null) {
			cbRegion.setEnabled(false);
		}

		cbDistrict.setId(CaseDataDto.DISTRICT);
		cbDistrict.setWidth(200, Unit.PIXELS);
		CssStyles.style(cbDistrict, CssStyles.FORCE_CAPTION);
		cbDistrict.setPlaceholder(I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.DISTRICT));
		criteriaBinder.bind(cbDistrict, CaseDataDto.DISTRICT);
		secondRowLayout.add(cbDistrict);

		cbIgnoreRegion = new Checkbox();
		cbIgnoreRegion.setId(Captions.caseFilterWithDifferentRegion);
		CssStyles.style(cbIgnoreRegion, CssStyles.CHECKBOX_FILTER_INLINE);
		cbIgnoreRegion.setLabel(I18nProperties.getCaption(Captions.caseFilterWithDifferentRegion));
		cbIgnoreRegion.addValueChangeListener(e -> ignoreRegionCallback.accept(e.getValue()));
		secondRowLayout.add(cbIgnoreRegion);
		secondRowLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, cbIgnoreRegion);
		secondRowLayout.setFlexGrow(1, cbIgnoreRegion);

		add(secondRowLayout);
	}

	private void addThirdRowLayout() {
		thirdRowLayout = new HorizontalLayout();
		thirdRowLayout.setSpacing(true);
		thirdRowLayout.setPadding(false);
		thirdRowLayout.setWidth(100, Unit.PERCENTAGE);

		cbNewCaseDateType = new ComboBox<>();
		dfNewCaseDateFrom = new DatePicker();
		dfNewCaseDateTo = new DatePicker();
		ComboBox<Integer> cbResultLimit = new ComboBox<>();

		cbNewCaseDateType.setId(CaseCriteria.NEW_CASE_DATE_TYPE);
		cbNewCaseDateType.setWidth(200, Unit.PIXELS);
		cbNewCaseDateType.setPlaceholder(I18nProperties.getString(Strings.promptNewCaseDateType));
		cbNewCaseDateType.setLabel(I18nProperties.getCaption(Captions.caseNewCaseDate));
		cbNewCaseDateType.setItems(NewCaseDateType.values());
		criteriaBinder.bind(cbNewCaseDateType, CaseCriteria.NEW_CASE_DATE_TYPE);
		cbNewCaseDateType.addValueChangeListener(event -> {
			dfNewCaseDateFrom.setEnabled(event.getValue() != null);
			dfNewCaseDateTo.setEnabled(event.getValue() != null);
		});
		thirdRowLayout.add(cbNewCaseDateType);

		dfNewCaseDateFrom.setId(CaseCriteria.NEW_CASE_DATE_FROM);
		dfNewCaseDateFrom.setWidth(120, Unit.PIXELS);
		CssStyles.style(dfNewCaseDateFrom, CssStyles.FORCE_CAPTION);
		dfNewCaseDateFrom.setPlaceholder(I18nProperties.getString(Strings.promptCasesDateFrom));
		criteriaBinder.forField(dfNewCaseDateFrom)
			.withConverter(new LocalDateToDateConverter(ZoneId.systemDefault()))
			.bind(CaseCriteria.NEW_CASE_DATE_FROM);
		dfNewCaseDateFrom.setEnabled(false);
		thirdRowLayout.add(dfNewCaseDateFrom);

		dfNewCaseDateTo.setId(CaseCriteria.NEW_CASE_DATE_TO);
		dfNewCaseDateTo.setWidth(120, Unit.PIXELS);
		CssStyles.style(dfNewCaseDateTo, CssStyles.FORCE_CAPTION);
		dfNewCaseDateTo.setPlaceholder(I18nProperties.getString(Strings.promptDateTo));
		criteriaBinder.forField(dfNewCaseDateTo)
			.withConverter(new LocalDateToDateConverter(ZoneId.systemDefault()))
			.bind(CaseCriteria.NEW_CASE_DATE_TO);
		dfNewCaseDateTo.setEnabled(false);
		thirdRowLayout.add(dfNewCaseDateTo);

		cbResultLimit.setId(QueryDetails.RESULT_LIMIT);
		cbResultLimit.setWidth(200, Unit.PIXELS);
		cbResultLimit.setLabel(I18nProperties.getCaption(Captions.QueryDetails_resultLimit));
		cbResultLimit.setItems(50, 100, 500, 1000);
		// was setEmptySelectionAllowed(false): a Flow combo box has no empty item
		queryDetailsBinder.bind(cbResultLimit, QueryDetails.RESULT_LIMIT);
		thirdRowLayout.add(cbResultLimit);

		btnConfirmFilters = ButtonHelper.createButton(Captions.actionConfirmFilters, event -> {
			try {
				criteriaBinder.writeBean(criteria);
				queryDetailsBinder.writeBean(queryDetails);
				filtersUpdatedCallback.run();
			} catch (ValidationException e) {
				// No validation needed
			}
		}, CssStyles.FORCE_CAPTION, CssStyles.BUTTON_PRIMARY);

		thirdRowLayout.add(btnConfirmFilters);

		btnResetFilters = ButtonHelper.createButton(Captions.actionResetFilters, event -> {
			ViewModelProviders.of(MergeCasesView.class).remove(CaseCriteria.class);
			filtersUpdatedCallback.run();
		}, CssStyles.FORCE_CAPTION);

		thirdRowLayout.add(btnResetFilters);

		HorizontalLayout relevanceStatusFilterLayout = new HorizontalLayout();

		lblNumberOfDuplicates = new Span("");
		lblNumberOfDuplicates.setId("numberOfDuplicates");
		CssStyles.style(
			lblNumberOfDuplicates,
			CssStyles.FORCE_CAPTION,
			CssStyles.LABEL_ROUNDED_CORNERS,
			CssStyles.LABEL_BACKGROUND_FOCUS_LIGHT,
			CssStyles.LABEL_BOLD);
		relevanceStatusFilterLayout.add(lblNumberOfDuplicates);
		relevanceStatusFilterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, lblNumberOfDuplicates);

		relevanceStatusFilter = new ComboBox<>();
		relevanceStatusFilter.setId(CaseCriteria.ENTITY_RELEVANCE_STATUS);
		relevanceStatusFilter.setWidth(210, Unit.PIXELS);
		relevanceStatusFilter.setEnabled(true);
		// was setEmptySelectionAllowed(false): a Flow combo box has no empty item
		relevanceStatusFilter.setItems(EntityRelevanceStatus.getAllExceptDeleted());
		relevanceStatusFilter.setItemLabelGenerator(item -> {
			switch (item) {
			case ACTIVE:
				return I18nProperties.getCaption(Captions.caseActiveCases);
			case ARCHIVED:
				return I18nProperties.getCaption(Captions.caseArchivedCases);
			case ACTIVE_AND_ARCHIVED:
				return I18nProperties.getCaption(Captions.caseAllActiveAndArchivedCases);
			default:
				return item.toString();
			}
		});

		criteriaBinder.bind(relevanceStatusFilter, CaseCriteria.ENTITY_RELEVANCE_STATUS);
		relevanceStatusFilterLayout.add(relevanceStatusFilter);
		relevanceStatusFilterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, relevanceStatusFilter);

		thirdRowLayout.add(relevanceStatusFilterLayout);
		thirdRowLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, relevanceStatusFilterLayout);
		thirdRowLayout.setFlexGrow(1, relevanceStatusFilterLayout);

		add(thirdRowLayout);
	}

	public void updateDuplicateCountLabel(int count) {
		lblNumberOfDuplicates.setText(String.format(I18nProperties.getCaption(Captions.caseNumberOfDuplicatesDetected), count));
	}

	public void setFiltersUpdatedCallback(Runnable filtersUpdatedCallback) {
		this.filtersUpdatedCallback = filtersUpdatedCallback;
	}

	public void setIgnoreRegionCallback(Consumer<Boolean> ignoreRegionCallback) {
		this.ignoreRegionCallback = ignoreRegionCallback;
	}
}
