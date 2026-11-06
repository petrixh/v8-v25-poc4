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
package de.symeda.sormas.ui.user;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.data.value.ValueChangeMode;

import de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.fields.TextField;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.Descriptions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.user.UserCriteria;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.user.UserRoleReferenceDto;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ComboBoxHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.MenuBarHelper;
import de.symeda.sormas.ui.utils.RowCount;
import de.symeda.sormas.ui.utils.ViewConfiguration;

/**
 * A view for performing create-read-update-delete operations on products.
 *
 * See also {@link UserController} for fetching the data, the actual CRUD
 * operations and controlling the view based on events from outside.
 */
public class UsersView extends AbstractUserView {

	private static final long serialVersionUID = -3533557348144005469L;

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/users";

	public static final String ACTIVE_FILTER = I18nProperties.getString(Strings.active);
	public static final String INACTIVE_FILTER = I18nProperties.getString(Strings.inactive);

	private UserCriteria criteria;

	private UserGrid grid;
	private Button createButton;
	private Button syncButton;

	private VerticalLayout gridLayout;

	// Filters
	private ComboBox activeFilter;
	private ComboBox userRolesFilter;
	private ComboBox regionFilter;
	private ComboBox districtFilter;
	private TextField searchField;
	private Checkbox showOnlyRestrictedAccessToAssignedEntities;

	private RowCount rowsCount;

	// Bulk operations
	private MenuBar bulkOperationsDropdown;

	public UsersView() {
		super(VIEW_NAME);

		criteria = ViewModelProviders.of(UsersView.class).get(UserCriteria.class);

		grid = new UserGrid();
		grid.setCriteria(criteria);
		gridLayout = new VerticalLayout();
		gridLayout.add(createFilterBar());
		gridLayout.add(createActionsBar());

		rowsCount = new RowCount(Strings.labelNumberOfUsers, grid.getDataSize());
		grid.addDataSizeChangeListener(e -> rowsCount.update(grid.getDataSize()));
		gridLayout.add(rowsCount);

		gridLayout.add(grid);
		gridLayout.setPadding(true);
		gridLayout.setSpacing(false);
		gridLayout.setSizeFull();
		gridLayout.setFlexGrow(1, grid);
		gridLayout.setClassName("crud-main-layout");

		addComponent(gridLayout);

		if (UiUtil.permitted(UserRight.USER_CREATE)
			&& FacadeProvider.getFeatureConfigurationFacade().isFeatureDisabled(FeatureType.AUTH_PROVIDER_TO_SORMAS_USER_SYNC)) {
			createButton = ButtonHelper.createIconButton(
				Captions.userNewUser,
				VaadinIcon.PLUS_CIRCLE,
				e -> ControllerProvider.getUserController().create(),
				CssStyles.BUTTON_PRIMARY);

			addHeaderComponent(createButton);
		}

		if (FacadeProvider.getUserFacade().isSyncEnabled()) {
			syncButton = ButtonHelper.createIconButton(Captions.syncUsers, VaadinIcon.REFRESH, e -> ControllerProvider.getUserController().sync());

			addHeaderComponent(syncButton);
		}

		if (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS)) {
			Button btnEnterBulkEditMode = ButtonHelper.createIconButton(Captions.actionEnterBulkEditMode, VaadinIcon.CHECK_SQUARE_O, null);
			btnEnterBulkEditMode.setVisible(!ViewModelProviders.of(UsersView.class).get(ViewConfiguration.class).isInEagerMode());

			addHeaderComponent(btnEnterBulkEditMode);

			Button btnLeaveBulkEditMode =
				ButtonHelper.createIconButton(Captions.actionLeaveBulkEditMode, VaadinIcon.CLOSE, null, CssStyles.BUTTON_PRIMARY);
			btnLeaveBulkEditMode.setVisible(ViewModelProviders.of(UsersView.class).get(ViewConfiguration.class).isInEagerMode());

			addHeaderComponent(btnLeaveBulkEditMode);

			btnEnterBulkEditMode.addClickListener(e -> {
				bulkOperationsDropdown.setVisible(true);
				ViewModelProviders.of(UsersView.class).get(ViewConfiguration.class).setInEagerMode(true);
				btnEnterBulkEditMode.setVisible(false);
				btnLeaveBulkEditMode.setVisible(true);
				grid.reload();
			});
			btnLeaveBulkEditMode.addClickListener(e -> {
				bulkOperationsDropdown.setVisible(false);
				ViewModelProviders.of(UsersView.class).get(ViewConfiguration.class).setInEagerMode(false);
				btnLeaveBulkEditMode.setVisible(false);
				btnEnterBulkEditMode.setVisible(true);
				navigateTo(criteria);
			});
		}
	}

	public HorizontalLayout createFilterBar() {

		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.setPadding(false);
		filterLayout.setSpacing(true);
		filterLayout.setSizeUndefined();

		activeFilter = ComboBoxHelper.createComboBoxV7();
		activeFilter.setId(UserDto.ACTIVE);
		activeFilter.setWidth(200, Unit.PIXELS);
		activeFilter.setInputPrompt(I18nProperties.getPrefixCaption(UserDto.I18N_PREFIX, UserDto.ACTIVE));
		activeFilter.setDescription(I18nProperties.getPrefixCaption(UserDto.I18N_PREFIX, UserDto.ACTIVE));
		activeFilter.addItems(ACTIVE_FILTER, INACTIVE_FILTER);
		activeFilter.addValueChangeListener(e -> {
			criteria.active(
				ACTIVE_FILTER.equals(e.getValue())
					? Boolean.TRUE
					: INACTIVE_FILTER.equals(e.getValue()) ? Boolean.FALSE : null);
			navigateTo(criteria);
		});
		filterLayout.add(activeFilter);

		userRolesFilter = ComboBoxHelper.createComboBoxV7();
		userRolesFilter.setId(UserDto.USER_ROLES);
		userRolesFilter.setWidth(200, Unit.PIXELS);
		userRolesFilter.setInputPrompt(I18nProperties.getPrefixCaption(UserDto.I18N_PREFIX, UserDto.USER_ROLES));
		userRolesFilter.setDescription(I18nProperties.getPrefixCaption(UserDto.I18N_PREFIX, UserDto.USER_ROLES));
		userRolesFilter.addItems(FacadeProvider.getUserRoleFacade().getAllActiveAsReference());
		userRolesFilter.addValueChangeListener(e -> {
			criteria.userRole((UserRoleReferenceDto) e.getValue());
			navigateTo(criteria);
		});
		filterLayout.add(userRolesFilter);

		UserDto user = UiUtil.getUser();

		regionFilter = ComboBoxHelper.createComboBoxV7();
		regionFilter.setId(CaseDataDto.REGION);

		if (user.getRegion() == null) {
			regionFilter.setWidth(140, Unit.PIXELS);
			regionFilter.setInputPrompt(I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.REGION));
			regionFilter.setDescription(I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.REGION));
			regionFilter.addItems(FacadeProvider.getRegionFacade().getAllActiveByServerCountry());
			regionFilter.addValueChangeListener(e -> {
				RegionReferenceDto region = (RegionReferenceDto) e.getValue();

				if (!DataHelper.equal(region, criteria.getRegion())) {
					criteria.district(null);
				}

				criteria.region(region);
				navigateTo(criteria);
			});
			filterLayout.add(regionFilter);
		}

		districtFilter = ComboBoxHelper.createComboBoxV7();
		districtFilter.setId(CaseDataDto.DISTRICT);
		districtFilter.setWidth(140, Unit.PIXELS);
		districtFilter.setInputPrompt(I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.DISTRICT));
		districtFilter.setDescription(I18nProperties.getDescription(Descriptions.descDistrictFilter));
		districtFilter.addValueChangeListener(e -> {
			DistrictReferenceDto district = (DistrictReferenceDto) e.getValue();
			criteria.district(district);
			navigateTo(criteria);
		});
		filterLayout.add(districtFilter);

		searchField = new TextField();
		searchField.setId("search");
		searchField.setWidth(200, Unit.PIXELS);
		searchField.setNullRepresentation("");
		searchField.setInputPrompt(I18nProperties.getString(Strings.promptUserSearch));
		searchField.setDescription(I18nProperties.getString(Strings.promptUserSearch));
		searchField.setImmediate(true);
		// was a V7 text change listener (lazy, user input only)
		searchField.setValueChangeMode(ValueChangeMode.LAZY);
		searchField.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			criteria.freeText(e.getValue());
			grid.reload();
			rowsCount.update(grid.getDataSize());
		});
		filterLayout.add(searchField);

		showOnlyRestrictedAccessToAssignedEntities = new Checkbox();
		showOnlyRestrictedAccessToAssignedEntities.setId("showOnly");
		showOnlyRestrictedAccessToAssignedEntities.setLabel(I18nProperties.getCaption(Captions.userRoleShowOnlyRestrictedAccessToAssignCases));
		showOnlyRestrictedAccessToAssignedEntities.addClassName(CssStyles.CHECKBOX_FILTER_INLINE);
		showOnlyRestrictedAccessToAssignedEntities.addValueChangeListener(e -> {
			criteria.setShowOnlyRestrictedAccessToAssignedEntities(e.getValue());
			navigateTo(criteria);
		});
		filterLayout.add(showOnlyRestrictedAccessToAssignedEntities);

		return filterLayout;
	}

	public HorizontalLayout createActionsBar() {

		HorizontalLayout statusFilterLayout = new HorizontalLayout();
		statusFilterLayout.setSpacing(true);
		statusFilterLayout.setPadding(false);
		statusFilterLayout.setWidthFull();
		statusFilterLayout.addClassName(CssStyles.VSPACE_3);

		// Bulk operation dropdown
		if (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS)) {
			HorizontalLayout actionButtonsLayout = new HorizontalLayout();
			actionButtonsLayout.setSpacing(true);

			bulkOperationsDropdown = MenuBarHelper.createDropDown(
				Captions.bulkActions,
				new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.actionEnable), VaadinIcon.CHECK_SQUARE_O, selectedItem -> {
					ControllerProvider.getUserController().enableAllSelectedItems(grid.asMultiSelect().getSelectedItems(), grid);
				}, UiUtil.permitted(UserRight.USER_EDIT)),
				new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.actionDisable), VaadinIcon.THIN_SQUARE, selectedItem -> {
					ControllerProvider.getUserController().disableAllSelectedItems(grid.asMultiSelect().getSelectedItems(), grid);
				}, UiUtil.permitted(UserRight.USER_EDIT)));

			bulkOperationsDropdown.setVisible(ViewModelProviders.of(UsersView.class).get(ViewConfiguration.class).isInEagerMode());
			actionButtonsLayout.add(bulkOperationsDropdown);

			statusFilterLayout.add(actionButtonsLayout);
			// was Alignment.TOP_RIGHT: the layout grows, its content goes right
			actionButtonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
			statusFilterLayout.setFlexGrow(1, actionButtonsLayout);
		}

		return statusFilterLayout;
	}

	@Override
	public void enter(ViewChangeEvent event) {

		if (event != null) {
			String params = event.getParameters().trim();
			if (params.startsWith("?")) {
				params = params.substring(1);
				criteria.fromUrlParams(params);
			}
			updateFilterComponents();
		}
		grid.reload();
		super.enter(event);
	}

	public void updateFilterComponents() {

		applyingCriteria = true;
		UserDto user = UiUtil.getUser();

		activeFilter.setValue(criteria.getActive() == null ? null : criteria.getActive() ? ACTIVE_FILTER : INACTIVE_FILTER);
		userRolesFilter.setValue(criteria.getUserRole());
		regionFilter.setValue(criteria.getRegion());
		showOnlyRestrictedAccessToAssignedEntities
			.setValue(criteria.getShowOnlyRestrictedAccessToAssignedEntities() != null && criteria.getShowOnlyRestrictedAccessToAssignedEntities());

		if (user.getRegion() != null && user.getDistrict() == null) {
			districtFilter.addItems(FacadeProvider.getDistrictFacade().getAllActiveByRegion(user.getRegion().getUuid()));
			districtFilter.setEnabled(true);
		} else if (criteria.getRegion() != null) {
			districtFilter.addItems(FacadeProvider.getDistrictFacade().getAllActiveByRegion(criteria.getRegion().getUuid()));
			districtFilter.setEnabled(true);
		} else {
			districtFilter.setEnabled(false);
		}

		districtFilter.setValue(criteria.getDistrict());
		searchField.setValue(criteria.getFreeText());

		applyingCriteria = false;
	}
}
