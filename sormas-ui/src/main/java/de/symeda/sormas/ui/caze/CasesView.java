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

import static de.symeda.sormas.ui.UiUtil.permitted;
import static de.symeda.sormas.ui.docgeneration.DocGenerationHelper.isDocGenerationAllowed;
import static de.symeda.sormas.ui.utils.FollowUpUtils.createFollowUpLegend;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.CountryHelper;
import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.ReferenceDto;
import de.symeda.sormas.api.bagexport.BAGExportCaseDto;
import de.symeda.sormas.api.caze.CaseCriteria;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.caze.CaseExportType;
import de.symeda.sormas.api.caze.CaseIndexDto;
import de.symeda.sormas.api.caze.InvestigationStatus;
import de.symeda.sormas.api.common.DeletableEntityType;
import de.symeda.sormas.api.docgeneneration.DocumentWorkflow;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.feature.FeatureTypeProperty;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.importexport.ExportConfigurationDto;
import de.symeda.sormas.api.importexport.ExportPropertyMetaInfo;
import de.symeda.sormas.api.importexport.ExportType;
import de.symeda.sormas.api.importexport.ImportExportUtils;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.sample.AdditionalTestDto;
import de.symeda.sormas.api.sample.SampleDto;
import de.symeda.sormas.api.sample.SampleExportDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.UtilDate;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.SearchSpecificLayout;
import de.symeda.sormas.ui.SormasUI;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.caze.importer.CaseImportLayout;
import de.symeda.sormas.ui.caze.importer.LineListingImportLayout;
import de.symeda.sormas.ui.customexport.ExportConfigurationsLayout;
import de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;
import de.symeda.sormas.ui.samples.HasName;
import de.symeda.sormas.ui.utils.AbstractView;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CaseDownloadUtil;
import de.symeda.sormas.ui.utils.ComboBoxHelper;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.ExportEntityName;
import de.symeda.sormas.ui.utils.FilteredGrid;
import de.symeda.sormas.ui.utils.GridExportStreamResource;
import de.symeda.sormas.ui.utils.MenuBarHelper;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;
import de.symeda.sormas.ui.utils.PopupButton;
import de.symeda.sormas.ui.utils.S2SOwnershipStatusFilter;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.components.expandablebutton.ExpandableButton;
import de.symeda.sormas.ui.utils.components.popupmenu.PopupMenu;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.fields.DefaultDateFormat;
import de.symeda.sormas.ui.utils.fields.OptionGroup;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

/**
 * A view for performing create-read-update-delete operations on products.
 * <p>
 * See also {@link CaseController} for fetching the data, the actual CRUD
 * operations and controlling the view based on events from outside.
 */
public class CasesView extends AbstractView implements HasName {

	private static final long serialVersionUID = -3533557348144005469L;

	private static final int MAX_FOLLOW_UP_VIEW_DAYS = 90;

	public static final String VIEW_NAME = "cases";

	/**
	 * When the number of cases exceeds this amount, the user will be confronted with a warning when trying
	 * to enter bulk edit mode.
	 */
	public static final int BULK_EDIT_MODE_WARNING_THRESHOLD = 1000;

	private final boolean caseFollowUpEnabled;
	private final boolean hasClinicalCourseRight;
	private final boolean hasTherapyRight;
	private final boolean hasExportSamplesRight;
	private final ExportConfigurationDto detailedExportConfiguration;

	private final CaseCriteria criteria;
	private final CasesViewConfiguration viewConfiguration;

	private final FilteredGrid<?, CaseCriteria> grid;
	private HashMap<Button, String> statusButtons;
	private Button activeStatusButton;

	private CaseFilterForm filterForm;

	// Filters
	private Span relevanceStatusInfoLabel;
	private ComboBox relevanceStatusFilter;

	// Bulk operations
	private MenuBar bulkOperationsDropdown;
	private Button btnEnterBulkEditMode;
	private Button btnLeaveBulkEditMode;

	private int followUpRangeInterval = 14;
	private boolean buttonPreviousOrNextClick = false;
	private Date followUpToDate;
	final List<MenuBarHelper.MenuBarItem> menuBarItems = new ArrayList<>();
	MenuBarHelper.MenuBarItem bulkDeleteMenuItem;

	public CasesView() {

		super(VIEW_NAME);

		caseFollowUpEnabled = UiUtil.enabled(FeatureType.CASE_FOLLOWUP);
		hasClinicalCourseRight = UiUtil.permitted(UserRight.CLINICAL_COURSE_VIEW);
		hasTherapyRight = UiUtil.permitted(UserRight.THERAPY_VIEW);
		hasExportSamplesRight = UiUtil.permitted(UserRight.SAMPLE_EXPORT);
		detailedExportConfiguration = buildDetailedExportConfiguration();
		viewConfiguration = ViewModelProviders.of(CasesView.class).get(CasesViewConfiguration.class);
		if (viewConfiguration.getViewType() == null) {
			viewConfiguration.setViewType(CasesViewType.DEFAULT);
		}

		criteria = ViewModelProviders.of(CasesView.class).get(CaseCriteria.class, getDefaultCriteria());

		if (isFollowUpVisitsOverviewType()) {
			if (criteria.getFollowUpVisitsInterval() == null) {
				criteria.setFollowUpVisitsInterval(followUpRangeInterval);
			}
			grid = new CaseFollowUpGrid(criteria, getClass());
		} else {
			criteria.followUpUntilFrom(null);
			grid = isDetailedViewType() ? new CaseGridDetailed(criteria) : new CaseGrid(criteria);
		}
		final VerticalLayout gridLayout = new VerticalLayout();
		gridLayout.add(createFilterBar());
		gridLayout.add(createStatusFilterBar());
		gridLayout.add(grid);

		if (isFollowUpVisitsOverviewType()) {
			gridLayout.add(createFollowUpLegend());
		}

		gridLayout.setPadding(true);
		gridLayout.setSpacing(false);
		gridLayout.setSizeFull();
		gridLayout.setFlexGrow(1, grid);
		gridLayout.setClassName("crud-main-layout");

		grid.addDataSizeChangeListener(e -> updateStatusButtons());

		OptionGroup casesViewSwitcher = new OptionGroup();
		casesViewSwitcher.setId("casesViewSwitcher");
		CssStyles.style(casesViewSwitcher, CssStyles.FORCE_CAPTION, SormasFieldGroupFieldFactory.OPTIONGROUP_HORIZONTAL, CssStyles.OPTIONGROUP_HORIZONTAL_PRIMARY);
		casesViewSwitcher.addItem(CasesViewType.DEFAULT);
		casesViewSwitcher.setItemCaption(CasesViewType.DEFAULT, I18nProperties.getCaption(Captions.caseDefaultView));

		casesViewSwitcher.addItem(CasesViewType.DETAILED);
		casesViewSwitcher.setItemCaption(CasesViewType.DETAILED, I18nProperties.getCaption(Captions.caseDetailedView));

		if (caseFollowUpEnabled) {
			casesViewSwitcher.addItem(CasesViewType.FOLLOW_UP_VISITS_OVERVIEW);
			casesViewSwitcher.setItemCaption(CasesViewType.FOLLOW_UP_VISITS_OVERVIEW, I18nProperties.getCaption(Captions.caseFollowupVisitsView));
		}

		casesViewSwitcher.setValue(viewConfiguration.getViewType());
		casesViewSwitcher.addValueChangeListener(e -> {
			CasesViewType viewType = (CasesViewType) e.getValue();

			viewConfiguration.setViewType(viewType);
			SormasUI.get().getNavigator().navigateTo(CasesView.VIEW_NAME);
		});
		addHeaderComponent(casesViewSwitcher);

		if (viewConfiguration.getViewType().isCaseOverview()) {
			addCommonCasesOverviewToolbar();
		}

		addComponent(gridLayout);
	}

	private CaseCriteria getDefaultCriteria() {
		CaseCriteria criteria = new CaseCriteria().relevanceStatus(EntityRelevanceStatus.ACTIVE);

		if (FacadeProvider.getSormasToSormasFacade().isAnyFeatureConfigured(FeatureType.SORMAS_TO_SORMAS_SHARE_CASES)) {
			criteria.setWithOwnership(true);
		}

		return criteria;
	}

	private ExportConfigurationDto buildDetailedExportConfiguration() {
		ExportConfigurationDto config = ExportConfigurationDto.build(UiUtil.getUserReference(), ExportType.CASE);

		config.setProperties(
			ImportExportUtils
				.getCaseExportProperties(
					CaseDownloadUtil::getPropertyCaption,
					caseFollowUpEnabled,
					hasClinicalCourseRight,
					hasTherapyRight,
					FacadeProvider.getConfigFacade().getCountryLocale(),
					FacadeProvider.getFeatureConfigurationFacade().getActiveServerFeatureConfigurations())
				.stream()
				.map(ExportPropertyMetaInfo::getPropertyId)
				.collect(Collectors.toSet()));
		return config;
	}

	private Set<String> getSelectedRows() {
		AbstractCaseGrid<?> caseGrid = (AbstractCaseGrid<?>) this.grid;
		return this.viewConfiguration.isInEagerMode()
			? caseGrid.asMultiSelect().getSelectedItems().stream().map(CaseIndexDto::getUuid).collect(Collectors.toSet())
			: Collections.emptySet();
	}

	private void addCommonCasesOverviewToolbar() {
		final PopupMenu moreButton = new PopupMenu(I18nProperties.getCaption(Captions.moreActions));

		Button openGuideButton = ButtonHelper.createIconButton(Captions.caseOpenCasesGuide, VaadinIcon.QUESTION, e -> {
			buildAndOpenCasesInstructions();
			moreButton.setPopupVisible(false);
		}, CssStyles.BUTTON_PRIMARY);
		openGuideButton.setWidth(100, Unit.PERCENTAGE);
		moreButton.addMenuEntry(openGuideButton);

		if (UiUtil.permitted(UserRight.CASE_IMPORT)) {
			VerticalLayout importLayout = new VerticalLayout();
			importLayout.setSpacing(true);
			importLayout.setPadding(true);
			importLayout.addClassName(CssStyles.LAYOUT_MINIMAL);
			importLayout.setWidth(250, Unit.PIXELS);

			PopupButton importButton = ButtonHelper.createIconPopupButton(Captions.actionImport, VaadinIcon.UPLOAD, importLayout);

			addHeaderComponent(importButton);
			addImportButton(importButton, importLayout, Captions.importLineListing, Strings.headingLineListingImport, LineListingImportLayout::new);
			addImportButton(importButton, importLayout, Captions.importDetailed, Strings.headingImportCases, CaseImportLayout::new);

		}

		if (UiUtil.permitted(UserRight.CASE_EXPORT)) {
			VerticalLayout exportLayout = new VerticalLayout();
			{
				exportLayout.setSpacing(true);
				exportLayout.setPadding(true);
				exportLayout.addClassName(CssStyles.LAYOUT_MINIMAL);
				exportLayout.setWidth(250, Unit.PIXELS);
			}

			PopupButton exportPopupButton = ButtonHelper.createIconPopupButton(Captions.export, VaadinIcon.DOWNLOAD, exportLayout);
			addHeaderComponent(exportPopupButton);

			{
				DownloadHandler streamResource = GridExportStreamResource.createStreamResourceWithSelectedItems(
					grid,
					() -> this.viewConfiguration.isInEagerMode() ? this.grid.asMultiSelect().getSelectedItems() : Collections.emptySet(),
					ExportEntityName.CASES);
				addExportButton(streamResource, exportPopupButton.getPopover(), exportLayout, VaadinIcon.TABLE, Captions.exportBasic, Strings.infoBasicExport);
			}

			{
				DownloadHandler exportStreamResource = CaseDownloadUtil.createCaseExportResource(
					grid.getCriteria(),
					this::getSelectedRows,
					CaseExportType.CASE_SURVEILLANCE,
					detailedExportConfiguration);

				addExportButton(
					exportStreamResource,
					exportPopupButton.getPopover(),
					exportLayout,
					VaadinIcon.FILE_TEXT,
					Captions.exportDetailed,
					Strings.infoDetailedExport);
			}

			if (hasClinicalCourseRight || hasTherapyRight) {
				DownloadHandler caseManagementExportStreamResource =
					DownloadUtil.createCaseManagementExportResource(grid.getCriteria(), this::getSelectedRows, ExportEntityName.CONTACTS);
				addExportButton(
					caseManagementExportStreamResource,
					exportPopupButton.getPopover(),
					exportLayout,
					VaadinIcon.FILE_TEXT,
					Captions.exportCaseManagement,
					Strings.infoCaseManagementExport);
			}

			if (hasExportSamplesRight) {
				DownloadHandler sampleExportStreamResource = DownloadUtil.createCsvExportStreamResource(
					SampleExportDto.class,
					null,
					(Integer start, Integer max) -> FacadeProvider.getSampleFacade()
						.getExportList(grid.getCriteria(), this.getSelectedRows(), start, max),
					(propertyId, type) -> {
						String caption = I18nProperties.findPrefixCaption(
							propertyId,
							SampleExportDto.I18N_PREFIX,
							SampleDto.I18N_PREFIX,
							CaseDataDto.I18N_PREFIX,
							PersonDto.I18N_PREFIX,
							AdditionalTestDto.I18N_PREFIX);
						if (Date.class.isAssignableFrom(type)) {
							caption += " (" + DateFormatHelper.getDateFormatPattern() + ")";
						}
						return caption;
					},
					ExportEntityName.SAMPLES,
					null);
				addExportButton(
					sampleExportStreamResource,
					exportPopupButton.getPopover(),
					exportLayout,
					VaadinIcon.FILE_TEXT,
					Captions.exportSamples,
					Strings.infoSampleExport);
			}

			if (FacadeProvider.getConfigFacade().isConfiguredCountry(CountryHelper.COUNTRY_CODE_SWITZERLAND)
				&& UiUtil.permitted(UserRight.BAG_EXPORT)) {
				DownloadHandler bagExportResource = DownloadUtil.createCsvExportStreamResource(
					BAGExportCaseDto.class,
					null,
					(Integer start, Integer max) -> FacadeProvider.getBAGExportFacade().getCaseExportList(this.getSelectedRows(), start, max),
					(propertyId, type) -> propertyId,
					ExportEntityName.BAG_CASES,
					null);

				addExportButton(bagExportResource, exportPopupButton.getPopover(), exportLayout, VaadinIcon.FILE_TEXT, Captions.BAGExport, Strings.infoBAGExport);
			}

			{
				Button btnCustomCaseExport = ButtonHelper.createIconButton(Captions.exportCaseCustom, VaadinIcon.FILE_TEXT, e -> {
					Dialog customExportWindow = VaadinUiUtil.createPopupWindow();

					ExportConfigurationsLayout customExportsLayout = new ExportConfigurationsLayout(
						ExportType.CASE,
						ImportExportUtils.getCaseExportProperties(
							CaseDownloadUtil::getPropertyCaption,
							caseFollowUpEnabled,
							hasClinicalCourseRight,
							hasTherapyRight,
							FacadeProvider.getConfigFacade().getCountryLocale(),
							FacadeProvider.getFeatureConfigurationFacade().getActiveServerFeatureConfigurations()),
						customExportWindow::close);
					customExportsLayout.setExportCallback((exportConfig) -> {
						// was Page.open(resource, null, true); owned by the export window, which is not inert
						DownloadUtil.startDownload(
							CaseDownloadUtil.createCaseExportResource(grid.getCriteria(), this::getSelectedRows, null, exportConfig),
							customExportsLayout);
					});
					customExportWindow.setWidth(1024, Unit.PIXELS);
					customExportWindow.setHeaderTitle(I18nProperties.getCaption(Captions.exportCaseCustom));
					customExportWindow.add(customExportsLayout);
					customExportWindow.open();
					exportPopupButton.setPopupVisible(false);
				}, CssStyles.BUTTON_PRIMARY);
				btnCustomCaseExport.setTooltipText(I18nProperties.getString(Strings.infoCustomExport));
				btnCustomCaseExport.setWidth(100, Unit.PERCENTAGE);
				exportLayout.add(btnCustomCaseExport);
			}

			{
				// Warning if no filters have been selected
				Span warningLabel = VaadinUiUtil.createLabel(I18nProperties.getString(Strings.infoExportNoFilters), ContentMode.HTML);
				warningLabel.setWidth(100, Unit.PERCENTAGE);
				exportLayout.add(warningLabel);
				warningLabel.setVisible(false);

				exportPopupButton.addClickListener(e -> warningLabel.setVisible(!criteria.hasAnyFilterActive()));
			}
		}

		if (isBulkEditAllowed()) {
			btnEnterBulkEditMode = ButtonHelper.createIconButton(Captions.actionEnterBulkEditMode, VaadinIcon.CHECK_SQUARE_O, e -> {
				if (grid.getDataSize() > BULK_EDIT_MODE_WARNING_THRESHOLD) {
					VaadinUiUtil.showConfirmationPopup(
						I18nProperties.getCaption(Captions.actionEnterBulkEditMode),
						new Span(String.format(I18nProperties.getString(Strings.confirmationEnterBulkEditMode), BULK_EDIT_MODE_WARNING_THRESHOLD)),
						I18nProperties.getString(Strings.yes),
						I18nProperties.getString(Strings.no),
						640,
						(result) -> {
							if (result == true) {
								enterBulkEditMode();
							}
						});
				} else {
					enterBulkEditMode();
				}
				moreButton.setPopupVisible(false);
			}, CssStyles.BUTTON_PRIMARY);

			btnEnterBulkEditMode.setVisible(!viewConfiguration.isInEagerMode());
			btnEnterBulkEditMode.setWidth(100, Unit.PERCENTAGE);
			moreButton.addMenuEntry(btnEnterBulkEditMode);

			btnLeaveBulkEditMode = ButtonHelper.createIconButton(Captions.actionLeaveBulkEditMode, VaadinIcon.CLOSE, e -> {
				bulkOperationsDropdown.setVisible(false);
				ViewModelProviders.of(CasesView.class).get(CasesViewConfiguration.class).setInEagerMode(false);
				btnLeaveBulkEditMode.setVisible(false);
				btnEnterBulkEditMode.setVisible(true);
				navigateTo(criteria);
			}, CssStyles.BUTTON_PRIMARY);
			btnLeaveBulkEditMode.setVisible(viewConfiguration.isInEagerMode());

			addHeaderComponent(btnLeaveBulkEditMode);
		}

		if (UiUtil.permitted(UserRight.CASE_MERGE)) {
			Button mergeDuplicatesButton = ButtonHelper.createIconButton(
				Captions.caseMergeDuplicates,
				VaadinIcon.COMPRESS_SQUARE,
				e -> ControllerProvider.getCaseController().navigateToMergeCasesView(),
				CssStyles.BUTTON_PRIMARY);
			mergeDuplicatesButton.setWidth(100, Unit.PERCENTAGE);
			moreButton.addMenuEntry(mergeDuplicatesButton);
		}

		Button searchSpecificCaseButton = ButtonHelper.createIconButton(Captions.caseSearchSpecificCase, VaadinIcon.SEARCH, e -> {
			buildAndOpenSearchSpecificCaseWindow();
			moreButton.setPopupVisible(false);
		}, CssStyles.BUTTON_PRIMARY);
		searchSpecificCaseButton.setWidth(100, Unit.PERCENTAGE);
		moreButton.addMenuEntry(searchSpecificCaseButton);

		if (UiUtil.permitted(UserRight.CASE_CREATE)) {
			final ExpandableButton lineListingButton =
				new ExpandableButton(Captions.lineListing).expand(e -> ControllerProvider.getCaseController().openLineListingWindow());
			addHeaderComponent(lineListingButton);

			final ExpandableButton createButton =
				new ExpandableButton(Captions.caseNewCase).expand(e -> ControllerProvider.getCaseController().create());
			addHeaderComponent(createButton);
		}

		if (moreButton.hasMenuEntries()) {
			addHeaderComponent(moreButton);
		}
	}

	protected void changeViewType(CasesViewType type) {
		viewConfiguration.setViewType(type);
		SormasUI.get().getNavigator().navigateTo(CasesView.VIEW_NAME);
	}

	private void addImportButton(
		PopupButton importPopupButton,
		VerticalLayout importLayout,
		String captionKey,
		String windowHeadingKey,
		Supplier<Component> windowContentSupplier) {
		Button lineListingImportButton = ButtonHelper.createIconButton(captionKey, VaadinIcon.UPLOAD, e -> {
			Dialog popupWindow = VaadinUiUtil.showPopupWindow(windowContentSupplier.get());
			popupWindow.setHeaderTitle(I18nProperties.getString(windowHeadingKey));
			AbstractCaseGrid<?> caseGrid = (AbstractCaseGrid<?>) this.grid;
			VaadinUiUtil.addCloseListener(popupWindow, c -> caseGrid.reload());
			importPopupButton.setPopupVisible(false);
		}, CssStyles.BUTTON_PRIMARY);
		lineListingImportButton.setWidth(100, Unit.PERCENTAGE);
		importLayout.add(lineListingImportButton);
	}

	private void buildAndOpenCasesInstructions() {
		Dialog window = VaadinUiUtil.showPopupWindow(new CasesGuideLayout());
		window.setWidth(1024, Unit.PIXELS);
		window.setHeaderTitle(I18nProperties.getString(Strings.headingCasesGuide));
	}

	private void buildAndOpenSearchSpecificCaseWindow() {
		Dialog window = VaadinUiUtil.createPopupWindow();
		window.setHeaderTitle(I18nProperties.getCaption(Captions.caseSearchSpecificCase));
		window.setWidth(768, Unit.PIXELS);

		SearchSpecificLayout layout = buildSearchSpecificLayout(window);
		window.add(layout);
		window.open();
	}

	private SearchSpecificLayout buildSearchSpecificLayout(Dialog window) {

		String description = I18nProperties.getString(Strings.infoSpecificCaseSearch);
		String confirmCaption = I18nProperties.getCaption(Captions.caseSearchCase);

		TextField searchField = new TextField();
		Runnable confirmCallback = () -> {
			String foundCaseUuid = FacadeProvider.getCaseFacade().getUuidByUuidEpidNumberOrExternalId(searchField.getValue(), null);

			if (foundCaseUuid != null) {
				ControllerProvider.getCaseController().navigateToCase(foundCaseUuid);
				window.close();
			} else {
				VaadinUiUtil.showSimplePopupWindow(
					I18nProperties.getString(Strings.headingNoCaseFound),
					I18nProperties.getString(Strings.messageNoCaseFound));
			}
		};

		return new SearchSpecificLayout(confirmCallback, window::close, searchField, description, confirmCaption);
	}

	private void enterBulkEditMode() {
		bulkOperationsDropdown.setVisible(true);
		ViewModelProviders.of(CasesView.class).get(CasesViewConfiguration.class).setInEagerMode(true);
		btnEnterBulkEditMode.setVisible(false);
		btnLeaveBulkEditMode.setVisible(true);
		((AbstractCaseGrid<?>) grid).reload();
	}

	private boolean isDefaultViewType() {
		return viewConfiguration.getViewType() == CasesViewType.DEFAULT;
	}

	private boolean isDetailedViewType() {
		return viewConfiguration.getViewType() == CasesViewType.DETAILED;
	}

	private boolean isFollowUpVisitsOverviewType() {
		return viewConfiguration.getViewType() == CasesViewType.FOLLOW_UP_VISITS_OVERVIEW;
	}

	public VerticalLayout createFilterBar() {
		VerticalLayout filterLayout = new VerticalLayout();
		filterLayout.setSpacing(false);
		filterLayout.setPadding(false);
		filterLayout.setWidth(100, Unit.PERCENTAGE);

		filterForm = new CaseFilterForm();
		filterForm.addValueChangeListener(e -> {
			if (!filterForm.hasFilter()) {
				navigateTo(null);
			}
		});
		filterForm.addResetHandler(e -> {
			ViewModelProviders.of(CasesView.class).remove(CaseCriteria.class);
			navigateTo(null, true);
		});
		filterForm.addApplyHandler(e -> {
			if (isFollowUpVisitsOverviewType()) {
				((CaseFollowUpGrid) grid).reload();
			} else {
				((AbstractCaseGrid<?>) grid).reload();
			}
		});
		filterLayout.add(filterForm);

		// Follow-up overview scrolling
		if (isFollowUpVisitsOverviewType()) {
			HorizontalLayout actionButtonsLayout = new HorizontalLayout();
			actionButtonsLayout.setSpacing(true);

			filterLayout.setWidth(100, Unit.PERCENTAGE);
			HorizontalLayout scrollLayout = dateRangeFollowUpVisitsFilterLayout();
			actionButtonsLayout.add(scrollLayout);

			filterLayout.add(actionButtonsLayout);
			// was Alignment.TOP_RIGHT
			filterLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, actionButtonsLayout);
			filterLayout.setFlexGrow(1, actionButtonsLayout);
		}

		return filterLayout;
	}

	private void reloadGrid() {
		((CaseFollowUpGrid) grid).setVisitColumns(criteria);
		criteria.setFollowUpVisitsTo(followUpToDate);
		criteria.setFollowUpVisitsInterval(followUpRangeInterval);
		((CaseFollowUpGrid) grid).reload();
		updateStatusButtons();
		buttonPreviousOrNextClick = false;
	}

	public HorizontalLayout createStatusFilterBar() {
		HorizontalLayout statusFilterLayout = new HorizontalLayout();
		statusFilterLayout.setSpacing(true);
		statusFilterLayout.setPadding(false);
		statusFilterLayout.setWidth(100, Unit.PERCENTAGE);
		statusFilterLayout.addClassName(CssStyles.VSPACE_3);

		statusButtons = new HashMap<>();

		Button statusAll = ButtonHelper.createButton(Captions.all, e -> {
			criteria.investigationStatus(null);
			navigateTo(criteria);
		}, CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER);

		statusFilterLayout.add(statusAll);
		statusButtons.put(statusAll, I18nProperties.getCaption(Captions.all));
		activeStatusButton = statusAll;

		for (InvestigationStatus status : InvestigationStatus.values()) {
			Button statusButton = ButtonHelper.createButton(status.toString(), e -> {
				criteria.investigationStatus(status);
				navigateTo(criteria);
			}, CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER, CssStyles.BUTTON_FILTER_LIGHT);
			ComponentUtil.setData(statusButton, InvestigationStatus.class, status);

			statusFilterLayout.add(statusButton);
			statusButtons.put(statusButton, status.toString());
		}

		HorizontalLayout actionButtonsLayout = new HorizontalLayout();
		actionButtonsLayout.setSpacing(true);
		{
			if (FacadeProvider.getSormasToSormasFacade().isAnyFeatureConfigured(FeatureType.SORMAS_TO_SORMAS_SHARE_CASES)) {
				ComboBox ownershipFilter = ComboBoxHelper.createComboBoxV7();
				ownershipFilter.setId("ownershipStatus");
				ownershipFilter.setWidth(140, Unit.PIXELS);
				ownershipFilter.setNullSelectionAllowed(false);
				ownershipFilter.addItems((Object[]) S2SOwnershipStatusFilter.values());
				ownershipFilter.setValue(S2SOwnershipStatusFilter.fromCriteriaValue(criteria.getWithOwnership()));
				ownershipFilter.addValueChangeListener(e -> {
					S2SOwnershipStatusFilter status = (S2SOwnershipStatusFilter) e.getValue();
					criteria.setWithOwnership(status.getCriteriaValue());
					navigateTo(criteria);
				});
				actionButtonsLayout.add(ownershipFilter);
			}

			// Show active/archived/all dropdown
			if (UiUtil.permitted(UserRight.CASE_VIEW)) {
				if (UiUtil.enabled(FeatureType.AUTOMATIC_ARCHIVING, DeletableEntityType.CASE)) {
					int daysAfterCaseGetsArchived = FacadeProvider.getFeatureConfigurationFacade()
						.getProperty(FeatureType.AUTOMATIC_ARCHIVING, DeletableEntityType.CASE, FeatureTypeProperty.THRESHOLD_IN_DAYS, Integer.class);
					if (daysAfterCaseGetsArchived > 0) {
						relevanceStatusInfoLabel = VaadinUiUtil.createLabel(
							HtmlRenderer.iconHtml(VaadinIcon.INFO_CIRCLE) + " "
								+ String.format(I18nProperties.getString(Strings.infoArchivedCases), daysAfterCaseGetsArchived),
							ContentMode.HTML);
						relevanceStatusInfoLabel.setVisible(false);
						relevanceStatusInfoLabel.addClassName(CssStyles.LABEL_VERTICAL_ALIGN_SUPER);
						actionButtonsLayout.add(relevanceStatusInfoLabel);
						actionButtonsLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, relevanceStatusInfoLabel);
					}
				}
				relevanceStatusFilter = ComboBoxHelper.createComboBoxV7();
				relevanceStatusFilter.setId("relevanceStatus");
				relevanceStatusFilter.setWidth(210, Unit.PIXELS);
				relevanceStatusFilter.setNullSelectionAllowed(false);
				relevanceStatusFilter.addItems((Object[]) EntityRelevanceStatus.values());
				relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ACTIVE, I18nProperties.getCaption(Captions.caseActiveCases));

				if (UiUtil.permitted(UserRight.CASE_VIEW_ARCHIVED)) {
					relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ARCHIVED, I18nProperties.getCaption(Captions.caseArchivedCases));
					relevanceStatusFilter
						.setItemCaption(EntityRelevanceStatus.ACTIVE_AND_ARCHIVED, I18nProperties.getCaption(Captions.caseAllActiveAndArchivedCases));
				} else {
					relevanceStatusFilter.removeItem(EntityRelevanceStatus.ARCHIVED);
					relevanceStatusFilter.removeItem(EntityRelevanceStatus.ACTIVE_AND_ARCHIVED);
				}

				if (UiUtil.permitted(UserRight.CASE_DELETE)) {
					relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.DELETED, I18nProperties.getCaption(Captions.caseDeletedCases));
				} else {
					relevanceStatusFilter.removeItem(EntityRelevanceStatus.DELETED);
				}
				relevanceStatusFilter.addValueChangeListener(e -> {
					if (relevanceStatusInfoLabel != null) {
						relevanceStatusInfoLabel.setVisible(EntityRelevanceStatus.ARCHIVED.equals(e.getValue()));
					}

					if (grid.getColumn(grid.DELETE_REASON_COLUMN) != null) {
						grid.getColumn(grid.DELETE_REASON_COLUMN).setHidden(!relevanceStatusFilter.getValue().equals(EntityRelevanceStatus.DELETED));
					}

					criteria.relevanceStatus((EntityRelevanceStatus) e.getValue());
					navigateTo(criteria);
				});
				actionButtonsLayout.add(relevanceStatusFilter);
			}

			if (viewConfiguration.getViewType().isCaseOverview()) {
				AbstractCaseGrid<?> caseGrid = (AbstractCaseGrid<?>) this.grid;
				// Bulk operation dropdown
				if (isBulkEditAllowed()) {
					boolean hasBulkOperationsRight = UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS);

					final List<MenuBarHelper.MenuBarItem> menuBarItems = new ArrayList<>();

					if (criteria.getRelevanceStatus() != EntityRelevanceStatus.DELETED) {
						menuBarItems.add(new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.bulkEdit), VaadinIcon.ELLIPSIS_H, mi -> {
							grid.bulkActionHandler(
								items -> ControllerProvider.getCaseController().showBulkCaseDataEditComponent(items, (AbstractCaseGrid<?>) grid));
						}, hasBulkOperationsRight && UiUtil.permitted(UserRight.CASE_EDIT)));
						menuBarItems.add(new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.bulkDelete), VaadinIcon.TRASH, mi -> {
							grid.bulkActionHandler(
								items -> ControllerProvider.getCaseController().deleteAllSelectedItems(items, (AbstractCaseGrid<?>) grid),
								true);
						}, hasBulkOperationsRight && UiUtil.permitted(UserRight.CASE_DELETE)));

						final boolean isSmsServiceSetUp = FacadeProvider.getConfigFacade().isSmsServiceSetUp();
						if (isSmsServiceSetUp && UiUtil.permitted(FeatureType.MANUAL_EXTERNAL_MESSAGES, UserRight.SEND_MANUAL_EXTERNAL_MESSAGES)) {
							menuBarItems.add(
								new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.messagesSendSMS), VaadinIcon.MOBILE_RETRO, mi -> {
									grid.bulkActionHandler(
										items -> ControllerProvider.getCaseController().sendSmsToAllSelectedItems(items, () -> navigateTo(criteria)));
								}, hasBulkOperationsRight));
						}
						menuBarItems.add(
							new MenuBarHelper.MenuBarItem(
								I18nProperties.getCaption(Captions.actionArchiveCoreEntity),
								VaadinIcon.ARCHIVE,
								mi -> grid.bulkActionHandler(
									items -> ControllerProvider.getCaseController().archiveAllSelectedItems(items, (AbstractCaseGrid<?>) grid),
									true),
								hasBulkOperationsRight
									&& UiUtil.permitted(UserRight.CASE_ARCHIVE)
									&& EntityRelevanceStatus.ACTIVE.equals(criteria.getRelevanceStatus())));
						menuBarItems.add(
							new MenuBarHelper.MenuBarItem(
								I18nProperties.getCaption(Captions.actionDearchiveCoreEntity),
								VaadinIcon.ARCHIVE,
								mi -> grid.bulkActionHandler(
									items -> ControllerProvider.getCaseController().dearchiveAllSelectedItems(items, (AbstractCaseGrid<?>) grid),
									true),
								hasBulkOperationsRight
									&& UiUtil.permitted(UserRight.CASE_ARCHIVE, UserRight.CASE_VIEW_ARCHIVED)
									&& EntityRelevanceStatus.ARCHIVED.equals(criteria.getRelevanceStatus())));
						menuBarItems
							.add(new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.sormasToSormasShare), VaadinIcon.SHARE, mi -> {
								grid.bulkActionHandler(
									items -> ControllerProvider.getSormasToSormasController().shareSelectedCases(items, () -> navigateTo(criteria)));
							},
								FacadeProvider.getFeatureConfigurationFacade()
									.isPropertyValueTrue(FeatureType.CASE_AND_CONTACT_BULK_ACTIONS, FeatureTypeProperty.S2S_SHARING)
									&& FacadeProvider.getSormasToSormasFacade().isSharingCasesEnabledForUser()));
						menuBarItems.add(
							new MenuBarHelper.MenuBarItem(
								I18nProperties.getCaption(Captions.ExternalSurveillanceToolGateway_send),
								VaadinIcon.SHARE,
								mi -> {
									grid.bulkActionHandler(items -> {
										if (getSelectedCases(caseGrid).isEmpty()) {
											showNoCasesSelectedWarning(caseGrid);
											return;
										}

										ControllerProvider.getCaseController().sendCasesToExternalSurveillanceTool(items, (AbstractCaseGrid<?>) grid);
									});
								},
								UiUtil.permitted(UserRight.EXTERNAL_SURVEILLANCE_SHARE)
									&& FacadeProvider.getExternalSurveillanceToolFacade().isFeatureEnabled()));

						if (isDocGenerationAllowed()) {
							menuBarItems.add(
								new MenuBarHelper.MenuBarItem(
									I18nProperties.getCaption(Captions.bulkActionCreatDocuments),
									VaadinIcon.FILE_TEXT,
									mi -> {
										grid.bulkActionHandler(items -> {
											List<ReferenceDto> references = caseGrid.asMultiSelect()
												.getSelectedItems()
												.stream()
												.map(CaseIndexDto::toReference)
												.collect(Collectors.toList());

											if (references.size() == 0) {
												NotificationHelper.show(
													I18nProperties.getString(Strings.headingNoCasesSelected),
													I18nProperties.getString(Strings.messageNoCasesSelected),
													Type.WARNING_MESSAGE);
												return;
											}

											ControllerProvider.getDocGenerationController()
												.showBulkQuarantineOrderDocumentDialog(references, DocumentWorkflow.QUARANTINE_ORDER_CASE);
										});
									}));
						}

						if (permitted(FeatureType.EVENT_SURVEILLANCE, UserRight.EVENT_CREATE, UserRight.EVENTPARTICIPANT_CREATE)) {
							menuBarItems.add(
								new MenuBarHelper.MenuBarItem(
									I18nProperties.getCaption(Captions.bulkLinkToEvent),
									VaadinIcon.PHONE,
									mi -> grid.bulkActionHandler(
										items -> ControllerProvider.getCaseController()
											.linkSelectedCasesToEvent(items, (AbstractCaseGrid<?>) grid))));
						}

						if (UiUtil.permitted(UserRight.GRANT_SPECIAL_CASE_ACCESS)) {
							menuBarItems.add(
								new MenuBarHelper.MenuBarItem(
									I18nProperties.getCaption(Captions.bulkSpecialCaseAccess),
									VaadinIcon.RASTER_LOWER_LEFT,
									mi -> grid.bulkActionHandler(
										items -> ControllerProvider.getSpecialCaseAccessController()
											.createForSelectedCases(items, (AbstractCaseGrid<?>) grid))));
						}
						menuBarItems
							.add(new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.bulkEmailSend), VaadinIcon.ENVELOPE, mi -> {
								grid.bulkActionHandler(
									items -> ControllerProvider.getCaseController().sendEmailsToAllSelectedItems(items, (AbstractCaseGrid<?>) grid),
									true);
							}, hasBulkOperationsRight && UiUtil.permitted(UserRight.EXTERNAL_EMAIL_SEND)));
					} else {
						menuBarItems
							.add(new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.bulkRestore), VaadinIcon.ARROW_BACKWARD, mi -> {
								grid.bulkActionHandler(
									items -> ControllerProvider.getCaseController().restoreSelectedCases(items, (AbstractCaseGrid<?>) grid),
									true);
							}, hasBulkOperationsRight && UiUtil.permitted(UserRight.CASE_DELETE)));
					}

					bulkOperationsDropdown = MenuBarHelper.createDropDown(Captions.bulkActions, menuBarItems);

					bulkOperationsDropdown.setVisible(viewConfiguration.isInEagerMode());
					actionButtonsLayout.add(bulkOperationsDropdown);
				}
			}
		}
		statusFilterLayout.add(actionButtonsLayout);
		// was Alignment.TOP_RIGHT: the layout grows, its content goes right
		statusFilterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, actionButtonsLayout);
		statusFilterLayout.setFlexGrow(1, actionButtonsLayout);
		actionButtonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

		return statusFilterLayout;
	}

	@Override
	public void enter(ViewChangeEvent event) {
		String params = event.getParameters().trim();
		if (params.startsWith("?")) {
			params = params.substring(1);
			criteria.fromUrlParams(params);
		}

		if (viewConfiguration.isInEagerMode() && !isFollowUpVisitsOverviewType()) {
			AbstractCaseGrid<?> caseGrid = (AbstractCaseGrid<?>) this.grid;
			caseGrid.setEagerDataProvider();
		}

		updateFilterComponents();
		if (isDefaultViewType()) {
			((CaseGrid) grid).reload();
		} else if (isFollowUpVisitsOverviewType()) {
			((CaseFollowUpGrid) grid).reload();
		} else {
			((CaseGridDetailed) grid).reload();
		}
	}

	public List<CaseIndexDto> getSelectedCases(AbstractCaseGrid<?> caseGrid) {
		return caseGrid.asMultiSelect().getSelectedItems().stream().collect(Collectors.toList());
	}

	public void showNoCasesSelectedWarning(AbstractCaseGrid<?> caseGrid) {
		NotificationHelper.show(
			I18nProperties.getString(Strings.headingNoCasesSelected),
			I18nProperties.getString(Strings.messageNoCasesSelected),
			Type.WARNING_MESSAGE);
	}

	public void updateFilterComponents() {
		// TODO replace with Vaadin 8 databinding
		applyingCriteria = true;

		updateStatusButtons();
		if (relevanceStatusFilter != null) {
			relevanceStatusFilter.setValue(criteria.getRelevanceStatus());
		}

		filterForm.setValue(criteria);

		applyingCriteria = false;
	}

	private void updateStatusButtons() {

		statusButtons.keySet().forEach(b -> {
			CssStyles.style(b, CssStyles.BUTTON_FILTER_LIGHT);
			b.setText(statusButtons.get(b));
			b.setSuffixComponent(null);
			if (ComponentUtil.getData(b, InvestigationStatus.class) == criteria.getInvestigationStatus()) {
				activeStatusButton = b;
			}
		});
		CssStyles.removeStyles(activeStatusButton, CssStyles.BUTTON_FILTER_LIGHT);
		if (activeStatusButton != null) {
			// was an HTML caption with LayoutUtil.spanCss(BADGE, count): the count is a badge after the text
			activeStatusButton.setText(statusButtons.get(activeStatusButton));
			Span badge = new Span(String.valueOf(grid.getDataSize()));
			badge.addClassName(CssStyles.BADGE);
			activeStatusButton.setSuffixComponent(badge);
		}
	}

	private boolean isBulkEditAllowed() {
		return UiUtil.enabled(FeatureType.CASE_AND_CONTACT_BULK_ACTIONS)
			&& (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS) || FacadeProvider.getSormasToSormasFacade().isSharingCasesEnabledForUser());
	}

	private HorizontalLayout dateRangeFollowUpVisitsFilterLayout() {
		HorizontalLayout scrollLayout = new HorizontalLayout();
		scrollLayout.setPadding(false);

		DatePicker toReferenceDate = criteria.getFollowUpVisitsTo() == null
			? new DatePicker(I18nProperties.getCaption(Captions.to), LocalDate.now())
			: new DatePicker(I18nProperties.getCaption(Captions.to), UtilDate.toLocalDate(criteria.getFollowUpVisitsTo()));
		DefaultDateFormat.applyOnAttach(toReferenceDate);
		toReferenceDate.setId("toReferenceDateField");

		LocalDate fromReferenceLocal = criteria.getFollowUpVisitsFrom() == null
			? LocalDate.now().minusDays(followUpRangeInterval - 1)
			: UtilDate.toLocalDate(criteria.getFollowUpVisitsFrom());

		DatePicker fromReferenceDate = new DatePicker(I18nProperties.getCaption(Captions.from), fromReferenceLocal);
		DefaultDateFormat.applyOnAttach(fromReferenceDate);
		fromReferenceDate.setId("fromReferenceDateField");

		Button minusDaysButton = ButtonHelper.createButton(I18nProperties.getCaption(Captions.caseMinusDays), e -> {
			final LocalDate fromReferenceDateValue = fromReferenceDate.getValue();
			final LocalDate toReferenceDateValue = toReferenceDate.getValue();
			if (fromReferenceDateValue == null || toReferenceDateValue == null) {
				NotificationHelper.show(I18nProperties.getValidationError(Validations.validDateRange), Type.ERROR_MESSAGE);
				return;
			}
			int newFollowUpRangeInterval = DateHelper.getDaysBetween(UtilDate.from(fromReferenceDateValue), UtilDate.from(toReferenceDateValue));
			if (newFollowUpRangeInterval <= MAX_FOLLOW_UP_VIEW_DAYS) {
				followUpRangeInterval = newFollowUpRangeInterval;
				buttonPreviousOrNextClick = true;
				toReferenceDate.setValue(toReferenceDateValue.minusDays(followUpRangeInterval));
				criteria.setFollowUpVisitsTo(UtilDate.from(toReferenceDate.getValue()));
				fromReferenceDate.setValue(fromReferenceDateValue.minusDays(followUpRangeInterval));
				criteria.setFollowUpVisitsInterval(followUpRangeInterval);
			} else {
				NotificationHelper.show(
					String.format(I18nProperties.getString(Strings.messageSelectedPeriodTooLong), MAX_FOLLOW_UP_VIEW_DAYS), Type.WARNING_MESSAGE);
			}
		}, CssStyles.BUTTON_PRIMARY, CssStyles.FORCE_CAPTION);
		scrollLayout.add(minusDaysButton);

		fromReferenceDate.addValueChangeListener(e -> {
			Date newFromDate = e.getValue() != null ? UtilDate.from(e.getValue()) : new Date();
			if (newFromDate.equals(criteria.getFollowUpVisitsFrom())) {
				return;
			}

			final LocalDate toReferenceDateValue = toReferenceDate.getValue();
			if (toReferenceDateValue == null) {
				NotificationHelper.show(I18nProperties.getValidationError(Validations.validDateRange), Type.ERROR_MESSAGE);
				return;
			}

			int newFollowUpRangeInterval = DateHelper.getDaysBetween(newFromDate, UtilDate.from(toReferenceDateValue));

			if (newFollowUpRangeInterval <= MAX_FOLLOW_UP_VIEW_DAYS) {
				applyingCriteria = true;
				criteria.setFollowUpVisitsFrom(DateHelper.getStartOfDay(newFromDate));
				applyingCriteria = false;
				followUpRangeInterval = newFollowUpRangeInterval;
				criteria.setFollowUpVisitsInterval(followUpRangeInterval);
				followUpToDate = criteria.getFollowUpVisitsTo();
				reloadGrid();
			} else {
				NotificationHelper.show(
					String.format(I18nProperties.getString(Strings.messageSelectedPeriodTooLong), MAX_FOLLOW_UP_VIEW_DAYS), Type.WARNING_MESSAGE);
				fromReferenceDate.setValue(UtilDate.toLocalDate(criteria.getFollowUpVisitsFrom()));
			}
		});
		scrollLayout.add(fromReferenceDate);

		toReferenceDate.addValueChangeListener(e -> {
			Date newFollowUpToDate = e.getValue() != null ? UtilDate.from(e.getValue()) : new Date();
			if (newFollowUpToDate.equals(criteria.getFollowUpVisitsTo())) {
				return;
			}

			final LocalDate fromReferenceDateValue = fromReferenceDate.getValue();
			if (fromReferenceDateValue == null) {
				NotificationHelper.show(I18nProperties.getValidationError(Validations.validDateRange), Type.ERROR_MESSAGE);
				return;
			}

			int newFollowUpRangeInterval = DateHelper.getDaysBetween(UtilDate.from(fromReferenceDateValue), newFollowUpToDate);

			if (newFollowUpRangeInterval <= MAX_FOLLOW_UP_VIEW_DAYS) {
				followUpToDate = newFollowUpToDate;
				applyingCriteria = true;
				criteria.reportDateTo(DateHelper.getEndOfDay(followUpToDate));
				criteria.setFollowUpVisitsTo(followUpToDate);
				applyingCriteria = false;
				if (!buttonPreviousOrNextClick) {
					followUpRangeInterval = newFollowUpRangeInterval;
					criteria.setFollowUpVisitsInterval(followUpRangeInterval);
					reloadGrid();
				}
			} else {
				NotificationHelper.show(
					String.format(I18nProperties.getString(Strings.messageSelectedPeriodTooLong), MAX_FOLLOW_UP_VIEW_DAYS), Type.WARNING_MESSAGE);
				toReferenceDate.setValue(UtilDate.toLocalDate(followUpToDate));
			}
		});
		scrollLayout.add(toReferenceDate);

		Button plusDaysButton = ButtonHelper.createButton(I18nProperties.getCaption(Captions.casePlusDays), e -> {
			final LocalDate fromReferenceDateValue = fromReferenceDate.getValue();
			final LocalDate toReferenceDateValue = toReferenceDate.getValue();
			if (fromReferenceDateValue == null || toReferenceDateValue == null) {
				NotificationHelper.show(I18nProperties.getValidationError(Validations.validDateRange), Type.ERROR_MESSAGE);
				return;
			}
			int newFollowUpRangeInterval = DateHelper.getDaysBetween(UtilDate.from(fromReferenceDateValue), UtilDate.from(toReferenceDateValue));

			if (newFollowUpRangeInterval <= MAX_FOLLOW_UP_VIEW_DAYS) {
				followUpRangeInterval = newFollowUpRangeInterval;
				buttonPreviousOrNextClick = true;
				toReferenceDate.setValue(toReferenceDateValue.plusDays(followUpRangeInterval));
				criteria.setFollowUpVisitsTo(UtilDate.from(toReferenceDate.getValue()));
				fromReferenceDate.setValue(fromReferenceDateValue.plusDays(followUpRangeInterval));
				criteria.setFollowUpVisitsInterval(followUpRangeInterval);
			} else {
				NotificationHelper.show(
					String.format(I18nProperties.getString(Strings.messageSelectedPeriodTooLong), MAX_FOLLOW_UP_VIEW_DAYS), Type.WARNING_MESSAGE);
			}
		}, CssStyles.BUTTON_PRIMARY, CssStyles.FORCE_CAPTION);
		scrollLayout.add(plusDaysButton);
		return scrollLayout;
	}

	@Override
	public String getName() {
		return VIEW_NAME;
	}
}
