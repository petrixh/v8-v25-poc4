/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2022 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

import static de.symeda.sormas.ui.contact.ContactsView.buildDetailedExportConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.EditPermissionType;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.contact.ContactClassification;
import de.symeda.sormas.api.contact.ContactCriteria;
import de.symeda.sormas.api.contact.ContactDto;
import de.symeda.sormas.api.contact.ContactIndexDto;
import de.symeda.sormas.api.contact.ContactStatus;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.Descriptions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.importexport.ExportConfigurationDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserReferenceDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.contact.AbstractContactGrid;
import de.symeda.sormas.ui.contact.ContactGrid;
import de.symeda.sormas.ui.contact.importer.CaseContactsImportLayout;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ComboBoxHelper;
import de.symeda.sormas.ui.utils.ContactDownloadUtil;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DetailSubComponentWrapper;
import de.symeda.sormas.ui.utils.ExportEntityName;
import de.symeda.sormas.ui.utils.GridExportStreamResource;
import de.symeda.sormas.ui.utils.MenuBarHelper;
import de.symeda.sormas.ui.utils.PopupButton;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.ViewConfiguration;
import de.symeda.sormas.ui.utils.components.expandablebutton.ExpandableButton;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.fields.TextField;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

public class CaseContactsView extends AbstractCaseView {

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/contacts";
	private static final long serialVersionUID = -1L;
	private final ContactCriteria criteria;
	private final ViewConfiguration viewConfiguration;

	private ContactGrid grid;

	//Filters
	private ComboBox classificationFilter;
	private ComboBox regionFilter;
	private ComboBox districtFilter;
	private ComboBox officerFilter;
	private TextField personLikeField;
	private TextField searchField;
	private Button resetButton;
	private Button applyButton;

	private DetailSubComponentWrapper gridLayout;
	private HashMap<Button, String> statusButtons;
	private Button activeStatusButton;
	private final ExportConfigurationDto detailedExportConfiguration;

	public CaseContactsView() {

		super(VIEW_NAME, false);
		setSizeFull();

		viewConfiguration = ViewModelProviders.of(getClass()).get(ViewConfiguration.class);
		viewConfiguration.setInEagerMode(true);
		criteria = ViewModelProviders.of(CaseContactsView.class).get(ContactCriteria.class);
		detailedExportConfiguration = buildDetailedExportConfiguration();
	}

	public HorizontalLayout createFilterBar() {

		HorizontalLayout topLayout = new HorizontalLayout();
		topLayout.setSpacing(true);
		topLayout.setSizeUndefined();

		classificationFilter = ComboBoxHelper.createComboBoxV7();
		classificationFilter.setWidth(240, Unit.PIXELS);
		classificationFilter.setInputPrompt(I18nProperties.getPrefixCaption(ContactIndexDto.I18N_PREFIX, ContactIndexDto.CONTACT_CLASSIFICATION));
		classificationFilter.setDescription(I18nProperties.getPrefixCaption(ContactIndexDto.I18N_PREFIX, ContactIndexDto.CONTACT_CLASSIFICATION));
		classificationFilter.addValueChangeListener(e -> criteria.setContactClassification((ContactClassification) e.getValue()));
		topLayout.add(classificationFilter);

		UserDto user = UiUtil.getUser();
		regionFilter = ComboBoxHelper.createComboBoxV7();
		if (user.getRegion() == null) {
			regionFilter.setWidth(240, Unit.PIXELS);
			regionFilter.setInputPrompt(I18nProperties.getPrefixCaption(ContactIndexDto.I18N_PREFIX, ContactIndexDto.REGION_UUID));
			regionFilter.setDescription(I18nProperties.getPrefixCaption(ContactIndexDto.I18N_PREFIX, ContactIndexDto.REGION_UUID));
			regionFilter.addItems(FacadeProvider.getRegionFacade().getAllActiveByServerCountry());
			regionFilter.addValueChangeListener(e -> {
				RegionReferenceDto region = (RegionReferenceDto) e.getValue();
				if (region != null) {
					officerFilter.addItems(
						FacadeProvider.getUserFacade().getUsersByRegionAndRights(region, criteria.getDisease(), UserRight.CONTACT_RESPONSIBLE));
				} else {
					officerFilter.removeAllItems();
				}
				criteria.region(region);
			});
			topLayout.add(regionFilter);
		}

		districtFilter = ComboBoxHelper.createComboBoxV7();
		districtFilter.setWidth(240, Unit.PIXELS);
		districtFilter.setInputPrompt(I18nProperties.getPrefixCaption(ContactIndexDto.I18N_PREFIX, ContactIndexDto.DISTRICT_UUID));
		districtFilter.setDescription(I18nProperties.getPrefixCaption(ContactIndexDto.I18N_PREFIX, ContactIndexDto.DISTRICT_UUID));
		districtFilter.addValueChangeListener(e -> criteria.district((DistrictReferenceDto) e.getValue()));

		if (user.getRegion() != null && user.getDistrict() == null) {
			districtFilter.addItems(FacadeProvider.getDistrictFacade().getAllActiveByRegion(user.getRegion().getUuid()));
			districtFilter.setEnabled(true);
		} else {
			regionFilter.addValueChangeListener(e -> {
				RegionReferenceDto region = (RegionReferenceDto) e.getValue();
				districtFilter.removeAllItems();
				if (region != null) {
					districtFilter.addItems(FacadeProvider.getDistrictFacade().getAllActiveByRegion(region.getUuid()));
					districtFilter.setEnabled(true);
				} else {
					districtFilter.setEnabled(false);
				}
			});
			districtFilter.setEnabled(false);
		}
		topLayout.add(districtFilter);

		Span infoLabel = VaadinUiUtil.createLabel(HtmlRenderer.iconHtml(VaadinIcon.INFO_CIRCLE), ContentMode.HTML);
		infoLabel.setSizeUndefined();
		// was an HTML description: Flow tooltips are text
		infoLabel.setTitle(I18nProperties.getString(Strings.infoContactsViewRegionDistrictFilter));
		CssStyles.style(infoLabel, CssStyles.LABEL_XLARGE, CssStyles.LABEL_SECONDARY);
		topLayout.add(infoLabel);

		officerFilter = ComboBoxHelper.createComboBoxV7();
		officerFilter.setWidth(240, Unit.PIXELS);
		officerFilter.setInputPrompt(I18nProperties.getPrefixCaption(ContactIndexDto.I18N_PREFIX, ContactIndexDto.CONTACT_OFFICER_UUID));
		officerFilter.setDescription(I18nProperties.getPrefixCaption(ContactIndexDto.I18N_PREFIX, ContactIndexDto.CONTACT_OFFICER_UUID));
		officerFilter.addValueChangeListener(e -> criteria.setContactOfficer((UserReferenceDto) e.getValue()));
		if (user.getRegion() != null) {
			officerFilter.addItems(
				FacadeProvider.getUserFacade().getUsersByRegionAndRights(user.getRegion(), criteria.getDisease(), UserRight.CONTACT_RESPONSIBLE));
		}
		topLayout.add(officerFilter);

		searchField = new TextField();
		searchField.setWidth(150, Unit.PIXELS);
		searchField.setNullRepresentation("");
		searchField.setInputPrompt(I18nProperties.getString(Strings.promptContactsSearchField));
		searchField.setDescription(I18nProperties.getString(Strings.promptContactsSearchField));
		// was a V7 text change listener (lazy, 400 ms): the value follows typing
		searchField.setValueChangeMode(ValueChangeMode.LAZY);
		searchField.addValueChangeListener(e -> {
			if (e.isFromClient()) {
				criteria.setContactOrCaseLike(e.getValue());
			}
		});
		topLayout.add(searchField);

		personLikeField = new TextField();
		personLikeField.setWidth(150, Unit.PIXELS);
		personLikeField.setNullRepresentation("");
		personLikeField.setInputPrompt(I18nProperties.getString(Strings.promptRelatedPersonLikeField));
		personLikeField.setDescription(I18nProperties.getString(Strings.promptRelatedPersonLikeField));
		// was a V7 text change listener (lazy, 400 ms): the value follows typing
		personLikeField.setValueChangeMode(ValueChangeMode.LAZY);
		personLikeField.addValueChangeListener(e -> {
			if (e.isFromClient()) {
				criteria.setPersonLike(e.getValue());
			}
		});
		topLayout.add(personLikeField);

		resetButton = ButtonHelper.createButton(Captions.actionResetFilters, event -> {
			ViewModelProviders.of(CaseContactsView.class).remove(ContactCriteria.class);
			navigateTo(null);
		});
		resetButton.setVisible(false);
		topLayout.add(resetButton);

		applyButton = ButtonHelper.createButton(Captions.actionApplyFilters, event -> navigateTo(criteria));
		applyButton.addClickShortcut(Key.ENTER);
		applyButton.setVisible(false);
		topLayout.add(applyButton);

		classificationFilter.addValueChangeListener(e -> updateApplyResetButtons());
		regionFilter.addValueChangeListener(e -> updateApplyResetButtons());
		officerFilter.addValueChangeListener(e -> updateApplyResetButtons());
		districtFilter.addValueChangeListener(e -> updateApplyResetButtons());
		updateApplyResetButtonsOnBlur(searchField);
		updateApplyResetButtonsOnBlur(personLikeField);

		return topLayout;
	}

	public HorizontalLayout createStatusFilterBar(boolean isEditAllowed) {

		HorizontalLayout statusFilterLayout = new HorizontalLayout();
		statusFilterLayout.setSpacing(true);
		statusFilterLayout.setWidth("100%");
		statusFilterLayout.addClassName(CssStyles.VSPACE_3);

		statusButtons = new HashMap<>();

		Button statusAll = ButtonHelper.createButton(Captions.all, e -> {
			criteria.contactStatus(null);
			navigateTo(criteria);
		}, CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER);

		statusFilterLayout.add(statusAll);
		statusButtons.put(statusAll, I18nProperties.getCaption(Captions.all));
		activeStatusButton = statusAll;

		for (ContactStatus status : ContactStatus.values()) {
			Button statusButton = ButtonHelper.createButton(status.toString(), e -> {
				criteria.contactStatus(status);
				navigateTo(criteria);
			}, CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER, CssStyles.BUTTON_FILTER_LIGHT);
			ComponentUtil.setData(statusButton, ContactStatus.class, status);

			statusFilterLayout.add(statusButton);
			statusButtons.put(statusButton, status.toString());
		}
		statusFilterLayout.setFlexGrow(1, statusFilterLayout.getComponentAt(statusFilterLayout.getComponentCount() - 1));

		if (isEditAllowed) {
			// Bulk operation dropdown
			if (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS)) {
				statusFilterLayout.setWidth(100, Unit.PERCENTAGE);

				MenuBar bulkOperationsDropdown = MenuBarHelper.createDropDown(
					Captions.bulkActions,
					new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.bulkEdit), VaadinIcon.ELLIPSIS_H, selectedItem -> {
						ControllerProvider.getContactController()
							.showBulkContactDataEditComponent(grid.asMultiSelect().getSelectedItems(), getCaseRef().getUuid(), grid);
					}, UiUtil.permitted(UserRight.CONTACT_EDIT)),
					new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.bulkCancelFollowUp), VaadinIcon.CLOSE, selectedItem -> {
						ControllerProvider.getContactController()
							.cancelFollowUpOfAllSelectedItems(grid.asMultiSelect().getSelectedItems(), getCaseRef().getUuid(), grid);
					}, UiUtil.permitted(UserRight.CONTACT_EDIT)),
					new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.bulkLostToFollowUp), VaadinIcon.UNLINK, selectedItem -> {
						ControllerProvider.getContactController()
							.setAllSelectedItemsToLostToFollowUp(grid.asMultiSelect().getSelectedItems(), getCaseRef().getUuid(), grid);
					}, UiUtil.permitted(UserRight.CONTACT_EDIT)),
					new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.bulkDelete), VaadinIcon.TRASH, selectedItem -> {
						ControllerProvider.getContactController()
							.deleteAllSelectedItems(grid.asMultiSelect().getSelectedItems(), (AbstractContactGrid<?>) grid);
					}, UiUtil.permitted(UserRight.CONTACT_DELETE)));

				statusFilterLayout.add(bulkOperationsDropdown);
				statusFilterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, bulkOperationsDropdown);
				statusFilterLayout.setFlexGrow(1, bulkOperationsDropdown);
			}

			if (UiUtil.permitted(UserRight.CONTACT_IMPORT)) {
				Button importButton = ButtonHelper.createIconButton(Captions.actionImport, VaadinIcon.UPLOAD, e -> {
					Dialog popupWindow = VaadinUiUtil.showPopupWindow(
						new CaseContactsImportLayout(FacadeProvider.getCaseFacade().getCaseDataByUuid(criteria.getCaze().getUuid())));
					popupWindow.setHeaderTitle(I18nProperties.getString(Strings.headingImportCaseContacts));
					VaadinUiUtil.addCloseListener(popupWindow, c -> {
						grid.reload();
					});
				}, CssStyles.BUTTON_PRIMARY);

				statusFilterLayout.add(importButton);
				statusFilterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, importButton);
				if (!UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS)) {
					statusFilterLayout.setFlexGrow(1, importButton);
				}
			}

			if (UiUtil.permitted(UserRight.CONTACT_EXPORT)) {
				VerticalLayout exportLayout = new VerticalLayout();
				exportLayout.setSpacing(true);
				exportLayout.setPadding(true);
				exportLayout.addClassName(CssStyles.LAYOUT_MINIMAL);
				exportLayout.setWidth(200, Unit.PIXELS);

				PopupButton exportButton = ButtonHelper.createIconPopupButton(Captions.export, VaadinIcon.DOWNLOAD, exportLayout);

				statusFilterLayout.add(exportButton);
				statusFilterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, exportButton);
				if (!UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS)) {
					statusFilterLayout.setFlexGrow(1, exportButton);
				}
				DownloadHandler streamResource = GridExportStreamResource.createStreamResourceWithSelectedItems(
					grid,
					() -> viewConfiguration.isInEagerMode() ? this.grid.asMultiSelect().getSelectedItems() : null,
					ExportEntityName.CONTACTS);
				addExportButton(streamResource, exportButton.getPopover(), exportLayout, VaadinIcon.TABLE, Captions.exportBasic, Descriptions.descExportButton);

				DownloadHandler extendedExportStreamResource =
					ContactDownloadUtil.createContactExportResource(grid.getCriteria(), this::getSelectedRows, detailedExportConfiguration);
				addExportButton(
					extendedExportStreamResource,
					exportButton.getPopover(),
					exportLayout,
					VaadinIcon.FILE_TEXT,
					Captions.exportDetailed,
					Descriptions.descDetailedExportButton);

				Button btnCustomExport = ButtonHelper.createIconButton(Captions.exportCustom, VaadinIcon.FILE_TEXT, e -> {
					ControllerProvider.getCustomExportController().openContactExportWindow(grid.getCriteria(), this::getSelectedRows);
				}, CssStyles.BUTTON_PRIMARY);
				btnCustomExport.setTooltipText(I18nProperties.getString(Strings.infoCustomExport));
				btnCustomExport.setWidth(100, Unit.PERCENTAGE);
				exportLayout.add(btnCustomExport);

				// Warning if no filters have been selected
				Span warningLabel = new Span(I18nProperties.getString(Strings.infoExportNoFilters));
				warningLabel.setWidth(100, Unit.PERCENTAGE);
				exportLayout.add(warningLabel);
				warningLabel.setVisible(false);

				exportButton.addClickListener(e -> warningLabel.setVisible(!criteria.hasAnyFilterActive()));
			}

			if (UiUtil.permitted(UserRight.CONTACT_CREATE)) {
				final CaseDataDto caseDto = FacadeProvider.getCaseFacade().getCaseDataByUuid(this.getCaseRef().getUuid());
				final ExpandableButton lineListingButton =
					new ExpandableButton(Captions.lineListing).expand(e -> ControllerProvider.getContactController().openLineListingWindow(caseDto));

				statusFilterLayout.add(lineListingButton);

				final Button newButton = ButtonHelper.createIconButtonWithCaption(
					Captions.contactNewContact,
					I18nProperties.getPrefixCaption(ContactDto.I18N_PREFIX, Captions.contactNewContact),
					VaadinIcon.PLUS_CIRCLE,
					e -> ControllerProvider.getContactController().create(this.getCaseRef()),
					CssStyles.BUTTON_PRIMARY);

				statusFilterLayout.add(newButton);
				statusFilterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, newButton);
			}
		}

		statusFilterLayout.addClassName("top-bar");
		activeStatusButton = statusAll;
		return statusFilterLayout;
	}

	private Set<String> getSelectedRows() {
		return viewConfiguration.isInEagerMode()
			? this.grid.asMultiSelect().getSelectedItems().stream().map(ContactIndexDto::getUuid).collect(Collectors.toSet())
			: Collections.emptySet();
	}

	@Override
	protected void initView(String params) {

		criteria.caze(getCaseRef());

		if (grid == null) {
			grid = new ContactGrid(criteria, getClass(), ViewConfiguration.class);
			gridLayout = new DetailSubComponentWrapper(() -> null);
			gridLayout.add(createFilterBar());
			gridLayout.add(createStatusFilterBar(isEditAllowed()));
			gridLayout.add(grid);
			gridLayout.setPadding(true);
			gridLayout.setSpacing(false);
			gridLayout.setSizeFull();
			gridLayout.setFlexGrow(1, grid);

			if (viewConfiguration.isInEagerMode()) {
				grid.setEagerDataProvider();
			}

			grid.addDataSizeChangeListener(e -> updateStatusButtons());

			setSubComponent(gridLayout);
		}

		if (params.startsWith("?")) {
			params = params.substring(1);
			criteria.fromUrlParams(params);
		}
		updateFilterComponents();

		grid.reload();
		if (!FacadeProvider.getCaseFacade().hasCurrentUserSpecialAccess(getCaseRef())) {
			setEditPermission(gridLayout);
		}
	}

	@Override
	protected boolean isEditAllowed() {
		return FacadeProvider.getCaseFacade().isEditContactAllowed(getReference().getUuid()).equals(EditPermissionType.ALLOWED);
	}

	public void updateFilterComponents() {

		// TODO replace with Vaadin 8 databinding
		applyingCriteria = true;

		updateStatusButtons();

		classificationFilter.removeAllItems();
		classificationFilter.addItems((Object[]) ContactClassification.values());
		classificationFilter.setValue(criteria.getContactClassification());

		regionFilter.setValue(criteria.getRegion());
		districtFilter.setValue(criteria.getDistrict());
		// Flow text fields take no null
		personLikeField.setValue(StringUtils.defaultString(criteria.getPersonLike()));
		searchField.setValue(StringUtils.defaultString(criteria.getContactOrCaseLike()));
		officerFilter.setValue(criteria.getContactOfficer());

		applyingCriteria = false;
	}

	private void updateStatusButtons() {

		statusButtons.keySet().forEach(b -> {
			CssStyles.style(b, CssStyles.BUTTON_FILTER_LIGHT);
			b.setText(statusButtons.get(b));
			b.setSuffixComponent(null);
			if (ComponentUtil.getData(b, ContactStatus.class) == criteria.getContactStatus()) {
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

	/**
	 * V7 fired the value change (which updates the apply/reset buttons) when the text field lost focus with changed text,
	 * and on setValue; the lazy listeners above only set the criteria while typing.
	 */
	private void updateApplyResetButtonsOnBlur(TextField field) {
		String[] lastValue = {
			field.getValue() };
		field.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				lastValue[0] = e.getValue();
				updateApplyResetButtons();
			}
		});
		field.addBlurListener(e -> {
			if (!Objects.equals(field.getValue(), lastValue[0])) {
				lastValue[0] = field.getValue();
				updateApplyResetButtons();
			}
		});
	}

	private void updateApplyResetButtons() {
		boolean hasFilters = hasFilters();
		resetButton.setVisible(hasFilters);
		applyButton.setVisible(hasFilters);
		if (!hasFilters) {
			navigateTo(null);
		}
	}

	private boolean hasFilters() {
		return !classificationFilter.isEmpty()
			|| !regionFilter.isEmpty()
			|| !districtFilter.isEmpty()
			|| !officerFilter.isEmpty()
			|| !personLikeField.isEmpty()
			|| !searchField.isEmpty();
	}
}
