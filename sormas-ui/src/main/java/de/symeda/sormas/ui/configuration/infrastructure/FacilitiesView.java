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

package de.symeda.sormas.ui.configuration.infrastructure;

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.InfrastructureType;
import de.symeda.sormas.api.infrastructure.community.CommunityReferenceDto;
import de.symeda.sormas.api.infrastructure.country.CountryReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.facility.FacilityCriteria;
import de.symeda.sormas.api.infrastructure.facility.FacilityDto;
import de.symeda.sormas.api.infrastructure.facility.FacilityExportDto;
import de.symeda.sormas.api.infrastructure.facility.FacilityIndexDto;
import de.symeda.sormas.api.infrastructure.facility.FacilityType;
import de.symeda.sormas.api.infrastructure.facility.FacilityTypeGroup;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.configuration.AbstractConfigurationView;
import de.symeda.sormas.ui.configuration.infrastructure.components.SearchField;
import de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;
import de.symeda.sormas.ui.utils.ArchiveHandlers;
import de.symeda.sormas.ui.utils.ArchiveMessages;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ComboBoxHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.ExportEntityName;
import de.symeda.sormas.ui.utils.FieldHelper;
import de.symeda.sormas.ui.utils.GridExportStreamResource;
import de.symeda.sormas.ui.utils.MenuBarHelper;
import de.symeda.sormas.ui.utils.PopupButton;
import de.symeda.sormas.ui.utils.RowCount;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.ViewConfiguration;
import de.symeda.sormas.ui.utils.fields.ComboBox;

public class FacilitiesView extends AbstractConfigurationView {

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/facilities";
	private static final long serialVersionUID = -2015225571046243640L;
	protected FacilitiesGrid grid;
	protected Button importButton;
	protected Button createButton;
	protected PopupButton exportPopupButton;
	private FacilityCriteria criteria;
	private ViewConfiguration viewConfiguration;
	// Filter
	private SearchField searchField;
	private ComboBox typeGroupFilter;
	private ComboBox typeFilter;
	private ComboBox countryFilter;
	private ComboBox regionFilter;
	private ComboBox districtFilter;
	private ComboBox communityFilter;
	private ComboBox relevanceStatusFilter;
	private Button resetButton;
	//	private HorizontalLayout headerLayout;
	private HorizontalLayout filterLayout;
	private VerticalLayout gridLayout;
	private MenuBar bulkOperationsDropdown;
	private RowCount rowCount;

	public FacilitiesView() {

		super(VIEW_NAME);

		viewConfiguration = ViewModelProviders.of(getClass()).get(ViewConfiguration.class);
		criteria = ViewModelProviders.of(getClass())
			.get(FacilityCriteria.class, new FacilityCriteria().country(FacadeProvider.getCountryFacade().getServerCountry()));
		if (criteria.getRelevanceStatus() == null) {
			criteria.relevanceStatus(EntityRelevanceStatus.ACTIVE);
		}

		grid = new FacilitiesGrid(criteria);
		gridLayout = new VerticalLayout();
		gridLayout.add(createFilterBar());
		rowCount = new RowCount(Strings.labelNumberOfFacilities, grid.getDataSize());
		grid.addDataSizeChangeListener(e -> rowCount.update(grid.getDataSize()));
		gridLayout.add(rowCount);
		gridLayout.add(grid);
		gridLayout.setPadding(true);
		gridLayout.setSpacing(false);
		gridLayout.setFlexGrow(1, grid);
		gridLayout.setSizeFull();
		gridLayout.setClassName("crud-main-layout");

		boolean infrastructureDataEditable = UiUtil.enabled(FeatureType.EDIT_INFRASTRUCTURE_DATA);

		if (UiUtil.permitted(infrastructureDataEditable, UserRight.INFRASTRUCTURE_IMPORT)) {
			importButton = ButtonHelper.createIconButton(Captions.actionImport, VaadinIcon.UPLOAD, e -> {
				Dialog window = VaadinUiUtil.showPopupWindow(new InfrastructureImportLayout(InfrastructureType.FACILITY));
				window.setHeaderTitle(I18nProperties.getString(Strings.headingImportFacilities));
				VaadinUiUtil.addCloseListener(window, c -> {
					grid.reload();
					rowCount.update(grid.getDataSize());
				});
			}, CssStyles.BUTTON_PRIMARY);

			addHeaderComponent(importButton);
		} else if (!infrastructureDataEditable) {
			// Flow components have no caption or icon: the caption with the icon goes above the text, as Vaadin 8 rendered it
			Span infrastructureDataLockedCaption =
				new Span(VaadinIcon.WARNING.create(), new Span(I18nProperties.getString(Strings.headingInfrastructureLocked)));
			infrastructureDataLockedCaption.addClassName(CssStyles.VAADIN_CAPTION);
			Div infrastructureDataLocked =
				new Div(infrastructureDataLockedCaption, new Div(new Span(I18nProperties.getString(Strings.messageInfrastructureLocked))));
			addHeaderComponent(infrastructureDataLocked);
		}

		if (UiUtil.permitted(UserRight.INFRASTRUCTURE_EXPORT)) {
			VerticalLayout exportLayout = new VerticalLayout();
			exportLayout.setSpacing(true);
			exportLayout.setPadding(true);
			exportLayout.addClassName(CssStyles.LAYOUT_MINIMAL);
			exportLayout.setWidth(250, Unit.PIXELS);

			exportPopupButton = ButtonHelper.createIconPopupButton(Captions.export, VaadinIcon.DOWNLOAD, exportLayout);
			addHeaderComponent(exportPopupButton);

			DownloadHandler basicExportStreamResource = GridExportStreamResource
				.createStreamResourceWithSelectedItems(grid, this::getSelectedRows, ExportEntityName.FACILITIES, FacilitiesGrid.ACTION_BTN_ID);

			addExportButton(
				basicExportStreamResource,
				exportPopupButton.getPopover(),
				exportLayout,
				VaadinIcon.TABLE,
				Captions.exportBasic,
				Strings.infoBasicExport);

			// Detailed export

			DownloadHandler detailedExportStreamResource = DownloadUtil.createCsvExportStreamResource(
				FacilityExportDto.class,
				null,
				(Integer start, Integer max) -> FacadeProvider.getFacilityFacade()
					.getExportList(
						grid.getCriteria(),
						getSelectedRows().stream().map(FacilityIndexDto::getUuid).collect(Collectors.toList()),
						start,
						max),
				(propertyId, type) -> {
					String caption = I18nProperties.findPrefixCaption(propertyId, FacilityExportDto.I18N_PREFIX, FacilityDto.I18N_PREFIX);
					if (Date.class.isAssignableFrom(type)) {
						caption += " (" + DateFormatHelper.getDateFormatPattern() + ")";
					}
					return caption;
				},
				ExportEntityName.FACILITIES,
				null);

			addExportButton(
				detailedExportStreamResource,
				exportPopupButton.getPopover(),
				exportLayout,
				VaadinIcon.FILE_TEXT,
				Captions.exportDetailed,
				Strings.infoDetailedExport);
		}

		if (UiUtil.permitted(infrastructureDataEditable, UserRight.INFRASTRUCTURE_CREATE)) {
			createButton = ButtonHelper.createIconButtonWithCaption(
				"create",
				I18nProperties.getCaption(Captions.actionNewEntry),
				VaadinIcon.PLUS_CIRCLE,
				e -> ControllerProvider.getInfrastructureController().createFacility(),
				CssStyles.BUTTON_PRIMARY);
			addHeaderComponent(createButton);
		}

		if (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS)) {
			Button btnEnterBulkEditMode = ButtonHelper.createIconButton(Captions.actionEnterBulkEditMode, VaadinIcon.CHECK_SQUARE_O, null);
			btnEnterBulkEditMode.setVisible(!viewConfiguration.isInEagerMode());
			addHeaderComponent(btnEnterBulkEditMode);

			Button btnLeaveBulkEditMode = ButtonHelper.createIconButton(Captions.actionLeaveBulkEditMode, VaadinIcon.CLOSE, null);
			btnLeaveBulkEditMode.setVisible(viewConfiguration.isInEagerMode());
			btnLeaveBulkEditMode.setClassName(CssStyles.BUTTON_PRIMARY);
			addHeaderComponent(btnLeaveBulkEditMode);

			btnEnterBulkEditMode.addClickListener(e -> {
				viewConfiguration.setInEagerMode(true);
				bulkOperationsDropdown.setVisible(isBulkOperationsDropdownVisible());
				btnEnterBulkEditMode.setVisible(false);
				btnLeaveBulkEditMode.setVisible(true);
				searchField.setEnabled(false);
				grid.setEagerDataProvider();
				grid.reload();
				rowCount.update(grid.getDataSize());
			});
			btnLeaveBulkEditMode.addClickListener(e -> {
				viewConfiguration.setInEagerMode(false);
				bulkOperationsDropdown.setVisible(false);
				btnLeaveBulkEditMode.setVisible(false);
				btnEnterBulkEditMode.setVisible(true);
				searchField.setEnabled(true);
				navigateTo(criteria);
			});
		}

		addComponent(gridLayout);

	}

	//	TODO additional filter bar (active, archived and other)
	//	private HorizontalLayout createHeaderBar() {
	//		headerLayout = new HorizontalLayout();
	//		headerLayout.setSpacing(true);
	//		headerLayout.setWidth(100, Unit.PERCENTAGE);
	//
	//		return headerLayout;
	//	}

	private Set<FacilityIndexDto> getSelectedRows() {
		FacilitiesGrid facilitiesGrid = this.grid;
		return this.viewConfiguration.isInEagerMode() ? facilitiesGrid.asMultiSelect().getSelectedItems() : Collections.emptySet();
	}

	private HorizontalLayout createFilterBar() {
		filterLayout = new HorizontalLayout();
		filterLayout.setPadding(false);
		filterLayout.setSpacing(true);
		filterLayout.setWidth(100, Unit.PERCENTAGE);

		searchField = new SearchField();
		searchField.addValueChangeListener(e -> {
			// was a V7 text change listener: user input only
			if (!e.isFromClient()) {
				return;
			}
			criteria.nameAddressLike(e.getValue());
			grid.reload();
			rowCount.update(grid.getDataSize());
		});
		filterLayout.add(searchField);

		typeGroupFilter = ComboBoxHelper.createComboBoxV7();
		typeGroupFilter.setId("typeGroup");
		typeGroupFilter.setWidth(220, Unit.PIXELS);
		typeGroupFilter.setCaption(I18nProperties.getCaption(Captions.Facility_typeGroup));
		typeGroupFilter.addItems(FacilityTypeGroup.values());
		typeGroupFilter.addValueChangeListener(e -> {
			criteria.typeGroup((FacilityTypeGroup) e.getValue());
			FieldHelper.updateItems(typeFilter, FacilityType.getTypes((FacilityTypeGroup) typeGroupFilter.getValue()));
		});
		filterLayout.add(typeGroupFilter);

		typeFilter = ComboBoxHelper.createComboBoxV7();
		typeFilter.setId(FacilityDto.TYPE);
		typeFilter.setWidth(220, Unit.PIXELS);
		typeFilter.setCaption(I18nProperties.getPrefixCaption(FacilityDto.I18N_PREFIX, FacilityDto.TYPE));
		typeFilter.addValueChangeListener(e -> {
			criteria.type((FacilityType) e.getValue());
			navigateTo(criteria);
		});
		filterLayout.add(typeFilter);

		countryFilter = addCountryFilter(filterLayout, country -> {
			criteria.country(country);
			grid.reload();
			rowCount.update(grid.getDataSize());
		}, regionFilter);
		countryFilter.addValueChangeListener(country -> {
			CountryReferenceDto countryReferenceDto = (CountryReferenceDto) country.getValue();
			criteria.country(countryReferenceDto);
			navigateTo(criteria);
			FieldHelper.updateItems(
				regionFilter,
				countryReferenceDto != null ? FacadeProvider.getRegionFacade().getAllActiveByCountry(countryReferenceDto.getUuid()) : null);

		});

		regionFilter = ComboBoxHelper.createComboBoxV7();
		regionFilter.setId(FacilityDto.REGION);
		regionFilter.setWidth(140, Unit.PIXELS);
		regionFilter.setCaption(I18nProperties.getPrefixCaption(FacilityDto.I18N_PREFIX, FacilityDto.REGION));
		regionFilter.addItems(FacadeProvider.getRegionFacade().getAllActiveAsReference());
		regionFilter.addValueChangeListener(e -> {
			RegionReferenceDto region = (RegionReferenceDto) e.getValue();
			criteria.region(region);
			navigateTo(criteria);
			FieldHelper
				.updateItems(districtFilter, region != null ? FacadeProvider.getDistrictFacade().getAllActiveByRegion(region.getUuid()) : null);

		});
		filterLayout.add(regionFilter);

		districtFilter = ComboBoxHelper.createComboBoxV7();
		districtFilter.setId(FacilityDto.DISTRICT);
		districtFilter.setWidth(140, Unit.PIXELS);
		districtFilter.setCaption(I18nProperties.getPrefixCaption(FacilityDto.I18N_PREFIX, FacilityDto.DISTRICT));
		districtFilter.addValueChangeListener(e -> {
			DistrictReferenceDto district = (DistrictReferenceDto) e.getValue();
			criteria.district(district);
			navigateTo(criteria);
			FieldHelper.updateItems(
				communityFilter,
				district != null ? FacadeProvider.getCommunityFacade().getAllActiveByDistrict(district.getUuid()) : null);
		});
		filterLayout.add(districtFilter);

		communityFilter = ComboBoxHelper.createComboBoxV7();
		communityFilter.setId(FacilityDto.COMMUNITY);
		communityFilter.setWidth(140, Unit.PIXELS);
		communityFilter.setCaption(I18nProperties.getPrefixCaption(FacilityDto.I18N_PREFIX, FacilityDto.COMMUNITY));
		communityFilter.addValueChangeListener(e -> {
			criteria.community((CommunityReferenceDto) e.getValue());
			navigateTo(criteria);
		});
		filterLayout.add(communityFilter);

		resetButton = ButtonHelper.createButton(Captions.actionResetFilters, event -> {
			ViewModelProviders.of(FacilitiesView.class).remove(FacilityCriteria.class);
			navigateTo(null);
		}, CssStyles.FORCE_CAPTION);
		resetButton.setVisible(false);

		filterLayout.add(resetButton);

		HorizontalLayout actionButtonsLayout = new HorizontalLayout();
		actionButtonsLayout.setSpacing(true);
		{
			// Show active/archived/all dropdown
			if (UiUtil.permitted(UserRight.INFRASTRUCTURE_VIEW)) {
				relevanceStatusFilter = ComboBoxHelper.createComboBoxV7();
				relevanceStatusFilter.setId("relevanceStatus");
				// was 220%: Vaadin 8 ignored a relative width in this undefined-width layout and showed the default width
				relevanceStatusFilter.setWidth(160, Unit.PIXELS);
				relevanceStatusFilter.setNullSelectionAllowed(false);

				relevanceStatusFilter.addItems(EntityRelevanceStatus.getAllExceptDeleted());
				relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ACTIVE, I18nProperties.getCaption(Captions.facilityActiveFacilities));

				if (UiUtil.permitted(UserRight.INFRASTRUCTURE_VIEW_ARCHIVED)) {
					relevanceStatusFilter
						.setItemCaption(EntityRelevanceStatus.ARCHIVED, I18nProperties.getCaption(Captions.facilityArchivedFacilities));
					relevanceStatusFilter
						.setItemCaption(EntityRelevanceStatus.ACTIVE_AND_ARCHIVED, I18nProperties.getCaption(Captions.facilityAllFacilities));
				} else {
					relevanceStatusFilter.removeItem(EntityRelevanceStatus.ARCHIVED);
					relevanceStatusFilter.removeItem(EntityRelevanceStatus.ACTIVE_AND_ARCHIVED);
				}

				relevanceStatusFilter.addValueChangeListener(e -> {
					criteria.relevanceStatus((EntityRelevanceStatus) e.getValue());
					navigateTo(criteria);
				});
				actionButtonsLayout.add(relevanceStatusFilter);

				// Bulk operation dropdown
				if (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS)) {
					bulkOperationsDropdown = MenuBarHelper.createDropDown(
						Captions.bulkActions,
						new MenuBarHelper.MenuBarItem(
							I18nProperties.getCaption(Captions.actionArchiveInfrastructure),
							VaadinIcon.ARCHIVE,
							selectedItem -> {
								ControllerProvider.getInfrastructureController()
									.archiveOrDearchiveAllSelectedItems(
										true,
										ArchiveHandlers.forInfrastructure(FacadeProvider.getFacilityFacade(), ArchiveMessages.FACILITY),
										grid,
										grid::reload,
										() -> navigateTo(criteria));
							},
							UiUtil.permitted(UserRight.INFRASTRUCTURE_ARCHIVE) && EntityRelevanceStatus.ACTIVE.equals(criteria.getRelevanceStatus())),
						new MenuBarHelper.MenuBarItem(
							I18nProperties.getCaption(Captions.actionDearchiveInfrastructure),
							VaadinIcon.ARCHIVE,
							selectedItem -> {
								ControllerProvider.getInfrastructureController()
									.archiveOrDearchiveAllSelectedItems(
										false,
										ArchiveHandlers.forInfrastructure(FacadeProvider.getFacilityFacade(), ArchiveMessages.FACILITY),
										grid,
										grid::reload,
										() -> navigateTo(criteria));
							},
							UiUtil.permitted(UserRight.INFRASTRUCTURE_ARCHIVE, UserRight.INFRASTRUCTURE_VIEW_ARCHIVED)
								&& EntityRelevanceStatus.ARCHIVED.equals(criteria.getRelevanceStatus())));

					bulkOperationsDropdown.setVisible(isBulkOperationsDropdownVisible());
					actionButtonsLayout.add(bulkOperationsDropdown);
				}
			}
		}
		filterLayout.add(actionButtonsLayout);
		// was Alignment.BOTTOM_RIGHT: the layout grows, its content goes right
		filterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, actionButtonsLayout);
		actionButtonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		filterLayout.setFlexGrow(1, actionButtonsLayout);

		return filterLayout;
	}

	@Override
	public void enter(ViewChangeEvent event) {
		super.enter(event);
		String params = event.getParameters().trim();
		if (params.startsWith("?")) {
			params = params.substring(1);
			criteria.fromUrlParams(params);
		}
		updateFilterComponents();
		grid.reload();
		rowCount.update(grid.getDataSize());
	}

	public void updateFilterComponents() {
		// TODO replace with Vaadin 8 databinding
		applyingCriteria = true;

		resetButton.setVisible(criteria.hasAnyFilterActive());

		if (relevanceStatusFilter != null) {
			relevanceStatusFilter.setValue(criteria.getRelevanceStatus());
		}
		searchField.setValue(criteria.getNameAddressLike());
		typeGroupFilter.setValue(criteria.getTypeGroup());
		typeFilter.setValue(criteria.getType());
		countryFilter.setValue(criteria.getCountry());
		regionFilter.setValue(criteria.getRegion());
		districtFilter.setValue(criteria.getDistrict());
		communityFilter.setValue(criteria.getCommunity());

		applyingCriteria = false;
	}

	private boolean isBulkOperationsDropdownVisible() {
		boolean infrastructureDataEditable = UiUtil.enabled(FeatureType.EDIT_INFRASTRUCTURE_DATA);

		return viewConfiguration.isInEagerMode()
			&& (EntityRelevanceStatus.ACTIVE.equals(criteria.getRelevanceStatus())
				|| (infrastructureDataEditable && EntityRelevanceStatus.ARCHIVED.equals(criteria.getRelevanceStatus())));
	}

}
