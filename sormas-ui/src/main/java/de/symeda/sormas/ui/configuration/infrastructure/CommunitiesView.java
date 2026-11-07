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
import java.util.Set;

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
import de.symeda.sormas.api.i18n.Descriptions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.InfrastructureType;
import de.symeda.sormas.api.infrastructure.community.CommunityCriteria;
import de.symeda.sormas.api.infrastructure.community.CommunityDto;
import de.symeda.sormas.api.infrastructure.country.CountryReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
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
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.ExportEntityName;
import de.symeda.sormas.ui.utils.FieldHelper;
import de.symeda.sormas.ui.utils.GridExportStreamResource;
import de.symeda.sormas.ui.utils.MenuBarHelper;
import de.symeda.sormas.ui.utils.RowCount;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.ViewConfiguration;
import de.symeda.sormas.ui.utils.fields.ComboBox;

public class CommunitiesView extends AbstractConfigurationView {

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/communities";
	private static final long serialVersionUID = -3487830069266335042L;
	protected Button importButton;
	protected Button createButton;
	private CommunityCriteria criteria;
	private ViewConfiguration viewConfiguration;
	// Filter
	private SearchField searchField;
	private ComboBox countryFilter;
	private ComboBox regionFilter;
	private ComboBox districtFilter;
	private ComboBox relevanceStatusFilter;
	private Button resetButton;
	private HorizontalLayout filterLayout;
	private VerticalLayout gridLayout;
	private CommunitiesGrid grid;
	private MenuBar bulkOperationsDropdown;
	private RowCount rowCount;

	public CommunitiesView() {

		super(VIEW_NAME);

		viewConfiguration = ViewModelProviders.of(CommunitiesView.class).get(ViewConfiguration.class);
		criteria = ViewModelProviders.of(CommunitiesView.class)
			.get(CommunityCriteria.class, new CommunityCriteria().country(FacadeProvider.getCountryFacade().getServerCountry()));
		if (criteria.getRelevanceStatus() == null) {
			criteria.relevanceStatus(EntityRelevanceStatus.ACTIVE);
		}

		grid = new CommunitiesGrid(criteria);
		gridLayout = new VerticalLayout();
		gridLayout.add(createFilterBar());
		rowCount = new RowCount(Strings.labelNumberOfCommunities, grid.getDataSize());
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
				Dialog window = VaadinUiUtil.showPopupWindow(new InfrastructureImportLayout(InfrastructureType.COMMUNITY));
				window.setHeaderTitle(I18nProperties.getString(Strings.headingImportCommunities));
				VaadinUiUtil.addCloseListener(window, c -> grid.reload());
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
			Button exportButton = ButtonHelper.createIconButton(Captions.export, VaadinIcon.TABLE, null, CssStyles.BUTTON_PRIMARY);
			exportButton.setTooltipText(I18nProperties.getDescription(Descriptions.descExportButton));
			addHeaderComponent(exportButton);

			DownloadHandler streamResource = GridExportStreamResource
				.createStreamResourceWithSelectedItems(grid, this::getSelectedRows, ExportEntityName.COMMUNITIES, CommunitiesGrid.ACTION_BTN_ID);
			// was the IgnoreCancelDownloader (a FileDownloader that ignored the IOException of a cancelled download)
			DownloadUtil.attachDownloader(exportButton, streamResource);
		}

		if (UiUtil.permitted(infrastructureDataEditable, UserRight.INFRASTRUCTURE_CREATE)) {
			createButton = ButtonHelper.createIconButton(
				Captions.actionNewEntry,
				VaadinIcon.PLUS_CIRCLE,
				e -> ControllerProvider.getInfrastructureController().createCommunity(),
				CssStyles.BUTTON_PRIMARY);

			addHeaderComponent(createButton);
		}

		if (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS)) {
			Button btnEnterBulkEditMode = ButtonHelper.createIconButton(Captions.actionEnterBulkEditMode, VaadinIcon.CHECK_SQUARE_O, null);
			btnEnterBulkEditMode.setVisible(!viewConfiguration.isInEagerMode());
			addHeaderComponent(btnEnterBulkEditMode);

			Button btnLeaveBulkEditMode =
				ButtonHelper.createIconButton(Captions.actionLeaveBulkEditMode, VaadinIcon.CLOSE, null, CssStyles.BUTTON_PRIMARY);
			btnLeaveBulkEditMode.setVisible(viewConfiguration.isInEagerMode());
			addHeaderComponent(btnLeaveBulkEditMode);

			btnEnterBulkEditMode.addClickListener(e -> {
				viewConfiguration.setInEagerMode(true);
				bulkOperationsDropdown.setVisible(isBulkOperationsDropdownVisible());
				btnEnterBulkEditMode.setVisible(false);
				btnLeaveBulkEditMode.setVisible(true);
				searchField.setEnabled(false);
				grid.setEagerDataProvider();
				grid.reload();
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

	private Set<CommunityDto> getSelectedRows() {
		CommunitiesGrid communitiesGrid = this.grid;
		return this.viewConfiguration.isInEagerMode() ? communitiesGrid.asMultiSelect().getSelectedItems() : Collections.emptySet();
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
			criteria.nameLike(e.getValue());
			grid.reload();
		});
		filterLayout.add(searchField);

		countryFilter = addCountryFilter(filterLayout, country -> {
			criteria.country(country);
			grid.reload();
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
		regionFilter.setId(DistrictDto.REGION);
		regionFilter.setWidth(140, Unit.PIXELS);
		regionFilter.setCaption(I18nProperties.getPrefixCaption(DistrictDto.I18N_PREFIX, DistrictDto.REGION));
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
		districtFilter.setId(CommunityDto.DISTRICT);
		districtFilter.setWidth(140, Unit.PIXELS);
		districtFilter.setCaption(I18nProperties.getPrefixCaption(CommunityDto.I18N_PREFIX, CommunityDto.DISTRICT));
		districtFilter.addValueChangeListener(e -> {
			criteria.district((DistrictReferenceDto) e.getValue());
			navigateTo(criteria);
		});
		filterLayout.add(districtFilter);

		resetButton = ButtonHelper.createButton(Captions.actionResetFilters, event -> {
			ViewModelProviders.of(CommunitiesView.class).remove(CommunityCriteria.class);
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
				relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ACTIVE, I18nProperties.getCaption(Captions.communityActiveCommunities));

				if (UiUtil.permitted(UserRight.INFRASTRUCTURE_VIEW_ARCHIVED)) {
					relevanceStatusFilter
						.setItemCaption(EntityRelevanceStatus.ARCHIVED, I18nProperties.getCaption(Captions.communityArchivedCommunities));
					relevanceStatusFilter
						.setItemCaption(EntityRelevanceStatus.ACTIVE_AND_ARCHIVED, I18nProperties.getCaption(Captions.communityAllCommunities));
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
							selectedItem -> ControllerProvider.getInfrastructureController()
								.archiveOrDearchiveAllSelectedItems(
									true,
									ArchiveHandlers.forInfrastructure(FacadeProvider.getCommunityFacade(), ArchiveMessages.COMMUNITY),
									grid,
									grid::reload,
									() -> navigateTo(criteria)),
							UiUtil.permitted(UserRight.INFRASTRUCTURE_ARCHIVE) && EntityRelevanceStatus.ACTIVE.equals(criteria.getRelevanceStatus())),
						new MenuBarHelper.MenuBarItem(
							I18nProperties.getCaption(Captions.actionDearchiveInfrastructure),
							VaadinIcon.ARCHIVE,
							selectedItem -> ControllerProvider.getInfrastructureController()
								.archiveOrDearchiveAllSelectedItems(
									false,
									ArchiveHandlers.forInfrastructure(FacadeProvider.getCommunityFacade(), ArchiveMessages.COMMUNITY),
									grid,
									grid::reload,
									() -> navigateTo(criteria)),
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
	}

	public void updateFilterComponents() {

		// TODO replace with Vaadin 8 databinding
		applyingCriteria = true;

		resetButton.setVisible(criteria.hasAnyFilterActive());

		if (relevanceStatusFilter != null) {
			relevanceStatusFilter.setValue(criteria.getRelevanceStatus());
		}
		searchField.setValue(criteria.getNameLike());
		countryFilter.setValue(criteria.getCountry());
		regionFilter.setValue(criteria.getRegion());
		districtFilter.setValue(criteria.getDistrict());

		applyingCriteria = false;
	}

	private boolean isBulkOperationsDropdownVisible() {
		boolean infrastructureDataEditable = UiUtil.enabled(FeatureType.EDIT_INFRASTRUCTURE_DATA);

		return viewConfiguration.isInEagerMode()
			&& (EntityRelevanceStatus.ACTIVE.equals(criteria.getRelevanceStatus())
				|| (infrastructureDataEditable && EntityRelevanceStatus.ARCHIVED.equals(criteria.getRelevanceStatus())));
	}
}
