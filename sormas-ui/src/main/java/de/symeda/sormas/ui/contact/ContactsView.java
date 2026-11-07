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
package de.symeda.sormas.ui.contact;

import static de.symeda.sormas.ui.UiUtil.permitted;
import static de.symeda.sormas.ui.docgeneration.DocGenerationHelper.isDocGenerationAllowed;
import static de.symeda.sormas.ui.utils.FollowUpUtils.createFollowUpLegend;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.CountryHelper;
import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.ReferenceDto;
import de.symeda.sormas.api.bagexport.BAGExportContactDto;
import de.symeda.sormas.api.common.DeletableEntityType;
import de.symeda.sormas.api.contact.ContactCriteria;
import de.symeda.sormas.api.contact.ContactIndexDto;
import de.symeda.sormas.api.contact.ContactStatus;
import de.symeda.sormas.api.docgeneneration.DocumentWorkflow;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.feature.FeatureTypeProperty;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.Descriptions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.importexport.ExportConfigurationDto;
import de.symeda.sormas.api.importexport.ExportPropertyMetaInfo;
import de.symeda.sormas.api.importexport.ExportType;
import de.symeda.sormas.api.importexport.ImportExportUtils;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.UtilDate;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.SormasUI;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.caze.CaseController;
import de.symeda.sormas.ui.contact.importer.ContactsImportLayout;
import de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;
import de.symeda.sormas.ui.samples.HasName;
import de.symeda.sormas.ui.utils.AbstractView;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ComboBoxHelper;
import de.symeda.sormas.ui.utils.ContactDownloadUtil;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
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
import de.symeda.sormas.ui.utils.fields.OptionGroup;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

/**
 * A view for performing create-read-update-delete operations on products.
 * <p>
 * See also {@link CaseController} for fetching the data, the actual CRUD
 * operations and controlling the view based on events from outside.
 */
public class ContactsView extends AbstractView implements HasName {

	private static final long serialVersionUID = -3533557348144005469L;

	private static final int MAX_FOLLOW_UP_VIEW_DAYS = 90;

	public static final String VIEW_NAME = "contacts";

	private final ContactCriteria criteria;
	private final ContactsViewConfiguration viewConfiguration;

	private final FilteredGrid<?, ContactCriteria> grid;

	private MenuBar bulkOperationsDropdown;
	private HashMap<Button, String> statusButtons;
	private Button activeStatusButton;

	private Span relevanceStatusInfoLabel;

	// Filters
	private ContactsFilterForm filterForm;
	private ComboBox relevanceStatusFilter;

	private int followUpRangeInterval = 14;
	private boolean buttonPreviousOrNextClick = false;
	private Date followUpToDate;
	private final ExportConfigurationDto detailedExportConfiguration;

	private Set<String> getSelectedRows() {
		AbstractContactGrid<?> contactGrid = (AbstractContactGrid<?>) this.grid;
		return this.viewConfiguration.isInEagerMode()
			? contactGrid.asMultiSelect().getSelectedItems().stream().map(ContactIndexDto::getUuid).collect(Collectors.toSet())
			: Collections.emptySet();
	}

	public ContactsView() {
		super(VIEW_NAME);

		viewConfiguration = ViewModelProviders.of(getClass()).get(ContactsViewConfiguration.class);
		if (viewConfiguration.getViewType() == null) {
			viewConfiguration.setViewType(ContactsViewType.CONTACTS_OVERVIEW);
		}
		criteria = ViewModelProviders.of(ContactsView.class).get(ContactCriteria.class, getDefaultCriteria());

		if (ContactsViewType.FOLLOW_UP_VISITS_OVERVIEW.equals(viewConfiguration.getViewType())) {
			if (criteria.getFollowUpVisitsInterval() == null) {
				criteria.setFollowUpVisitsInterval(followUpRangeInterval);
				grid = new ContactFollowUpGrid(criteria, getClass());
			} else {
				grid = new ContactFollowUpGrid(criteria, getClass());
			}
		} else {
			criteria.followUpUntilFrom(null);
			grid = ContactsViewType.DETAILED_OVERVIEW.equals(viewConfiguration.getViewType())
				? new ContactGridDetailed(criteria, getClass(), ContactsViewConfiguration.class)
				: new ContactGrid(criteria, getClass(), ContactsViewConfiguration.class);
		}

		detailedExportConfiguration = buildDetailedExportConfiguration();

		final VerticalLayout gridLayout = new VerticalLayout();
		gridLayout.add(createFilterBar());
		gridLayout.add(createStatusFilterBar());
		gridLayout.add(grid);
		if (ContactsViewType.FOLLOW_UP_VISITS_OVERVIEW.equals(viewConfiguration.getViewType())) {
			gridLayout.add(createFollowUpLegend());
		}
		gridLayout.setPadding(true);
		gridLayout.setSpacing(false);
		gridLayout.setSizeFull();
		gridLayout.setFlexGrow(1, grid);
		gridLayout.setClassName("crud-main-layout");
		grid.addDataSizeChangeListener(e -> updateStatusButtons());

		OptionGroup contactsViewSwitcher = new OptionGroup();
		contactsViewSwitcher.setId("contactsViewSwitcher");
		CssStyles.style(contactsViewSwitcher, CssStyles.FORCE_CAPTION, SormasFieldGroupFieldFactory.OPTIONGROUP_HORIZONTAL, CssStyles.OPTIONGROUP_HORIZONTAL_PRIMARY);
		contactsViewSwitcher.addItem(ContactsViewType.CONTACTS_OVERVIEW);
		contactsViewSwitcher.setItemCaption(ContactsViewType.CONTACTS_OVERVIEW, I18nProperties.getCaption(Captions.contactContactsOverview));

		contactsViewSwitcher.addItem(ContactsViewType.DETAILED_OVERVIEW);
		contactsViewSwitcher.setItemCaption(ContactsViewType.DETAILED_OVERVIEW, I18nProperties.getCaption(Captions.contactDetailedOverview));

		contactsViewSwitcher.addItem(ContactsViewType.FOLLOW_UP_VISITS_OVERVIEW);
		contactsViewSwitcher
			.setItemCaption(ContactsViewType.FOLLOW_UP_VISITS_OVERVIEW, I18nProperties.getCaption(Captions.contactFollowUpVisitsOverview));

		contactsViewSwitcher.setValue(viewConfiguration.getViewType());
		contactsViewSwitcher.addValueChangeListener(e -> {
			ContactsViewType viewType = (ContactsViewType) e.getValue();

			viewConfiguration.setViewType(viewType);
			SormasUI.get().getNavigator().navigateTo(ContactsView.VIEW_NAME);
		});
		addHeaderComponent(contactsViewSwitcher);

		final PopupMenu moreButton = new PopupMenu(I18nProperties.getCaption(Captions.moreActions));

		if (viewConfiguration.getViewType().isContactOverview() && UiUtil.permitted(UserRight.CONTACT_IMPORT)) {
			Button importButton = ButtonHelper.createIconButton(Captions.actionImport, VaadinIcon.UPLOAD, e -> {
				Dialog popupWindow = VaadinUiUtil.showPopupWindow(new ContactsImportLayout());
				popupWindow.setHeaderTitle(I18nProperties.getString(Strings.headingImportContacts));
				VaadinUiUtil.addCloseListener(popupWindow, c -> {
					AbstractContactGrid<?> grid = (AbstractContactGrid<?>) this.grid;
					grid.reload();
				});
				moreButton.setPopupVisible(false);
			}, CssStyles.BUTTON_PRIMARY);
			importButton.setWidth(100, Unit.PERCENTAGE);

			moreButton.addMenuEntry(importButton);
		}

		if (viewConfiguration.getViewType().isContactOverview() && UiUtil.permitted(UserRight.CONTACT_EXPORT)) {
			VerticalLayout exportLayout = new VerticalLayout();
			{
				exportLayout.setSpacing(true);
				exportLayout.setPadding(true);
				exportLayout.addClassName(CssStyles.LAYOUT_MINIMAL);
				exportLayout.setWidth(200, Unit.PIXELS);
			}

			PopupButton exportButton = ButtonHelper.createIconPopupButton(Captions.export, VaadinIcon.DOWNLOAD, exportLayout);
			addHeaderComponent(exportButton);

			{
				DownloadHandler streamResource = GridExportStreamResource.createStreamResourceWithSelectedItems(
					grid,
					() -> this.viewConfiguration.isInEagerMode() ? this.grid.asMultiSelect().getSelectedItems() : Collections.emptySet(),
					ExportEntityName.CONTACTS);
				addExportButton(
					streamResource,
					exportButton.getPopover(),
					exportLayout,
					VaadinIcon.TABLE,
					Captions.exportBasic,
					Descriptions.descExportButton);
			}
			{
				DownloadHandler extendedExportStreamResource =
					ContactDownloadUtil.createContactExportResource(grid.getCriteria(), this::getSelectedRows, detailedExportConfiguration);

				addExportButton(
					extendedExportStreamResource,
					exportButton.getPopover(),
					exportLayout,
					VaadinIcon.FILE_TEXT,
					Captions.exportDetailed,
					Descriptions.descDetailedExportButton);
			}

			if (UiUtil.permitted(UserRight.VISIT_EXPORT)) {
				DownloadHandler followUpVisitsExportStreamResource =
					DownloadUtil.createVisitsExportStreamResource(grid.getCriteria(), this::getSelectedRows, ExportEntityName.CONTACT_FOLLOW_UPS);

				addExportButton(
					followUpVisitsExportStreamResource,
					exportButton.getPopover(),
					exportLayout,
					VaadinIcon.FILE_TEXT,
					Captions.exportFollowUp,
					Descriptions.descFollowUpExportButton);
			}

			{
				Button btnCustomExport = ButtonHelper.createIconButton(Captions.exportCustom, VaadinIcon.FILE_TEXT, e -> {
					ControllerProvider.getCustomExportController().openContactExportWindow(grid.getCriteria(), this::getSelectedRows);
					exportButton.setPopupVisible(false);
				}, CssStyles.BUTTON_PRIMARY);
				btnCustomExport.setTooltipText(I18nProperties.getString(Strings.infoCustomExport));
				btnCustomExport.setWidth(100, Unit.PERCENTAGE);
				exportLayout.add(btnCustomExport);
			}

			if (FacadeProvider.getConfigFacade().isConfiguredCountry(CountryHelper.COUNTRY_CODE_SWITZERLAND)
				&& UiUtil.permitted(UserRight.BAG_EXPORT)) {
				DownloadHandler bagExportResource = DownloadUtil.createCsvExportStreamResource(
					BAGExportContactDto.class,
					null,
					(Integer start, Integer max) -> FacadeProvider.getBAGExportFacade().getContactExportList(this.getSelectedRows(), start, max),
					(propertyId, type) -> propertyId,
					ExportEntityName.BAG_CONTACTS,
					null);

				addExportButton(
					bagExportResource,
					exportButton.getPopover(),
					exportLayout,
					VaadinIcon.FILE_TEXT,
					Captions.BAGExport,
					Strings.infoBAGExport);
			}

			// Warning if no filters have been selected
			{
				Span warningLabel = new Span(I18nProperties.getString(Strings.infoExportNoFilters));
				warningLabel.setId("contactWarningLabel");
				warningLabel.setWidth(100, Unit.PERCENTAGE);
				exportLayout.add(warningLabel);
				warningLabel.setVisible(false);

				exportButton.addClickListener(e -> warningLabel.setVisible(!criteria.hasAnyFilterActive()));
			}
		}

		if (isBulkEditAllowed()) {
			Button btnEnterBulkEditMode =
				ButtonHelper.createIconButton(Captions.actionEnterBulkEditMode, VaadinIcon.CHECK_SQUARE_O, null, CssStyles.BUTTON_PRIMARY);
			{
				btnEnterBulkEditMode.setVisible(!viewConfiguration.isInEagerMode());
				btnEnterBulkEditMode.setWidth(100, Unit.PERCENTAGE);
				moreButton.addMenuEntry(btnEnterBulkEditMode);
			}

			Button btnLeaveBulkEditMode =
				ButtonHelper.createIconButton(Captions.actionLeaveBulkEditMode, VaadinIcon.CLOSE, null, CssStyles.BUTTON_PRIMARY);
			{
				btnLeaveBulkEditMode.setVisible(viewConfiguration.isInEagerMode());
				btnLeaveBulkEditMode.setWidth(100, Unit.PERCENTAGE);
				moreButton.addMenuEntry(btnLeaveBulkEditMode);
			}

			btnEnterBulkEditMode.addClickListener(e -> {
				bulkOperationsDropdown.setVisible(true);
				ViewModelProviders.of(getClass()).get(ContactsViewConfiguration.class).setInEagerMode(true);
				btnEnterBulkEditMode.setVisible(false);
				btnLeaveBulkEditMode.setVisible(true);
				((AbstractContactGrid<?>) grid).reload();
			});
			btnLeaveBulkEditMode.addClickListener(e -> {
				bulkOperationsDropdown.setVisible(false);
				ViewModelProviders.of(getClass()).get(ContactsViewConfiguration.class).setInEagerMode(false);
				btnLeaveBulkEditMode.setVisible(false);
				btnEnterBulkEditMode.setVisible(true);
				navigateTo(criteria);
			});
		}

		if (UiUtil.permitted(UserRight.CONTACT_MERGE)) {
			Button mergeDuplicatesButton = ButtonHelper.createIconButton(
				Captions.contactMergeDuplicates,
				VaadinIcon.COMPRESS_SQUARE,
				e -> ControllerProvider.getContactController().navigateToMergeContactsView(),
				CssStyles.BUTTON_PRIMARY);
			mergeDuplicatesButton.setWidth(100, Unit.PERCENTAGE);
			moreButton.addMenuEntry(mergeDuplicatesButton);
		}

		if (viewConfiguration.getViewType().isContactOverview() && UiUtil.permitted(UserRight.CONTACT_CREATE)) {
			final ExpandableButton lineListingButton =
				new ExpandableButton(Captions.lineListing).expand(e -> ControllerProvider.getContactController().openLineListingWindow());
			addHeaderComponent(lineListingButton);

			final Button btnNewContact = ButtonHelper.createIconButton(
				Captions.contactNewContact,
				VaadinIcon.PLUS_CIRCLE,
				e -> ControllerProvider.getContactController().create(),
				CssStyles.BUTTON_PRIMARY);
			addHeaderComponent(btnNewContact);
		}

		if (moreButton.hasMenuEntries()) {
			addHeaderComponent(moreButton);
		}

		addComponent(gridLayout);
	}

	private ContactCriteria getDefaultCriteria() {
		ContactCriteria criteria = new ContactCriteria().relevanceStatus(EntityRelevanceStatus.ACTIVE);

		if (FacadeProvider.getSormasToSormasFacade().isAnyFeatureConfigured(FeatureType.SORMAS_TO_SORMAS_SHARE_CONTACTS)) {
			criteria.setWithOwnership(true);
		}

		return criteria;
	}

	public VerticalLayout createFilterBar() {
		VerticalLayout filterLayout = new VerticalLayout();
		filterLayout.setSpacing(false);
		filterLayout.setPadding(false);
		filterLayout.setWidth(100, Unit.PERCENTAGE);

		filterForm = new ContactsFilterForm();
		filterForm.addValueChangeListener(e -> {
			if (!filterForm.hasFilter()) {
				navigateTo(null);
			}
		});
		filterForm.addResetHandler(e -> {
			ViewModelProviders.of(ContactsView.class).remove(ContactCriteria.class);
			navigateTo(null, true);
		});
		filterForm.addApplyHandler(e -> {
			if (ContactsViewType.FOLLOW_UP_VISITS_OVERVIEW.equals(viewConfiguration.getViewType())) {
				((ContactFollowUpGrid) grid).reload();
			} else {
				((AbstractContactGrid<?>) grid).reload();
			}
		});
		filterLayout.add(filterForm);

		// Follow-up overview scrolling
		if (ContactsViewType.FOLLOW_UP_VISITS_OVERVIEW.equals(viewConfiguration.getViewType())) {
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

	public HorizontalLayout createStatusFilterBar() {
		HorizontalLayout statusFilterLayout = new HorizontalLayout();
		statusFilterLayout.setPadding(false);
		statusFilterLayout.setSpacing(true);
		statusFilterLayout.setWidth(100, Unit.PERCENTAGE);
		statusFilterLayout.addClassName(CssStyles.VSPACE_3);

		statusButtons = new HashMap<>();

		Button statusAll = ButtonHelper.createButton(I18nProperties.getCaption(Captions.all), e -> {
			criteria.contactStatus(null);
			navigateTo(criteria);
		}, CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER);

		if (ContactsViewType.FOLLOW_UP_VISITS_OVERVIEW.equals(viewConfiguration.getViewType())) {
			CssStyles.style(statusAll, CssStyles.FORCE_CAPTION);
		}

		statusFilterLayout.add(statusAll);

		statusButtons.put(statusAll, I18nProperties.getCaption(Captions.all));
		activeStatusButton = statusAll;

		for (ContactStatus status : ContactStatus.values()) {
			Button statusButton = ButtonHelper.createButton("status-" + status.toString(), status.toString(), e -> {
				criteria.contactStatus(status);
				navigateTo(criteria);
			}, CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER, CssStyles.BUTTON_FILTER_LIGHT);

			ComponentUtil.setData(statusButton, ContactStatus.class, status);
			if (ContactsViewType.FOLLOW_UP_VISITS_OVERVIEW.equals(viewConfiguration.getViewType())) {
				CssStyles.style(statusButton, CssStyles.FORCE_CAPTION);
			}

			statusFilterLayout.add(statusButton);

			statusButtons.put(statusButton, status.toString());
		}

		HorizontalLayout actionButtonsLayout = new HorizontalLayout();
		actionButtonsLayout.setSpacing(true);
		{
			if (FacadeProvider.getSormasToSormasFacade().isAnyFeatureConfigured(FeatureType.SORMAS_TO_SORMAS_SHARE_CONTACTS)) {
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
			if (UiUtil.permitted(UserRight.CONTACT_VIEW)) {
				if (UiUtil.enabled(FeatureType.AUTOMATIC_ARCHIVING, DeletableEntityType.CONTACT)) {
					int daysAfterContactGetsArchived = FacadeProvider.getFeatureConfigurationFacade()
						.getProperty(
							FeatureType.AUTOMATIC_ARCHIVING,
							DeletableEntityType.CONTACT,
							FeatureTypeProperty.THRESHOLD_IN_DAYS,
							Integer.class);
					if (daysAfterContactGetsArchived > 0) {
						relevanceStatusInfoLabel = VaadinUiUtil.createLabel(
							HtmlRenderer.iconHtml(VaadinIcon.INFO_CIRCLE) + " "
								+ String.format(I18nProperties.getString(Strings.infoArchivedContacts), daysAfterContactGetsArchived),
							ContentMode.HTML);
						relevanceStatusInfoLabel.setVisible(false);
						relevanceStatusInfoLabel.addClassName(CssStyles.LABEL_VERTICAL_ALIGN_SUPER);
						actionButtonsLayout.add(relevanceStatusInfoLabel);
						actionButtonsLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, relevanceStatusInfoLabel);
					}
				}

				relevanceStatusFilter = ComboBoxHelper.createComboBoxV7();
				relevanceStatusFilter.setId("relevanceStatus");
				relevanceStatusFilter.setWidth(220, Unit.PIXELS);
				relevanceStatusFilter.setNullSelectionAllowed(false);
				relevanceStatusFilter.addItems((Object[]) EntityRelevanceStatus.values());
				relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ACTIVE, I18nProperties.getCaption(Captions.contactActiveContacts));

				if (UiUtil.permitted(UserRight.CONTACT_VIEW_ARCHIVED)) {
					relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ARCHIVED, I18nProperties.getCaption(Captions.contactArchivedContacts));
					relevanceStatusFilter.setItemCaption(
						EntityRelevanceStatus.ACTIVE_AND_ARCHIVED,
						I18nProperties.getCaption(Captions.contactAllActiveAndArchiveContacts));
				} else {
					relevanceStatusFilter.removeItem(EntityRelevanceStatus.ARCHIVED);
					relevanceStatusFilter.removeItem(EntityRelevanceStatus.ACTIVE_AND_ARCHIVED);
				}

				if (UiUtil.permitted(UserRight.CONTACT_DELETE)) {
					relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.DELETED, I18nProperties.getCaption(Captions.contactDeletedContacts));
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

			// Bulk operation dropdown
			if (isBulkEditAllowed()) {
				statusFilterLayout.setWidth(100, Unit.PERCENTAGE);

				boolean hasBulkOperationsRight = UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS);

				List<MenuBarHelper.MenuBarItem> bulkActions;
				if (criteria.getRelevanceStatus() != EntityRelevanceStatus.DELETED) {
					bulkActions = new ArrayList<>(
						Arrays.asList(
							new MenuBarHelper.MenuBarItem(
								I18nProperties.getCaption(Captions.bulkEdit),
								VaadinIcon.ELLIPSIS_H,
								mi -> grid.bulkActionHandler(
									items -> ControllerProvider.getContactController()
										.showBulkContactDataEditComponent(items, null, (AbstractContactGrid<?>) grid)),
								hasBulkOperationsRight && UiUtil.permitted(UserRight.CONTACT_EDIT)),
							new MenuBarHelper.MenuBarItem(
								I18nProperties.getCaption(Captions.bulkCancelFollowUp),
								VaadinIcon.CLOSE,
								mi -> grid.bulkActionHandler(
									items -> ControllerProvider.getContactController()
										.cancelFollowUpOfAllSelectedItems(items, null, (AbstractContactGrid<?>) grid)),
								hasBulkOperationsRight && UiUtil.permitted(UserRight.CONTACT_EDIT)),
							new MenuBarHelper.MenuBarItem(
								I18nProperties.getCaption(Captions.bulkLostToFollowUp),
								VaadinIcon.UNLINK,
								mi -> grid.bulkActionHandler(
									items -> ControllerProvider.getContactController()
										.setAllSelectedItemsToLostToFollowUp(items, null, (AbstractContactGrid<?>) grid)),
								hasBulkOperationsRight && UiUtil.permitted(UserRight.CONTACT_EDIT)),
							new MenuBarHelper.MenuBarItem(
								I18nProperties.getCaption(Captions.bulkDelete),
								VaadinIcon.TRASH,
								mi -> grid.bulkActionHandler(
									items -> ControllerProvider.getContactController().deleteAllSelectedItems(items, (AbstractContactGrid<?>) grid),
									true),
								hasBulkOperationsRight && UiUtil.permitted(UserRight.CONTACT_DELETE)),
							new MenuBarHelper.MenuBarItem(
								I18nProperties.getCaption(Captions.actionArchiveCoreEntity),
								VaadinIcon.ARCHIVE,
								mi -> grid.bulkActionHandler(
									items -> ControllerProvider.getContactController().archiveAllSelectedItems(items, (AbstractContactGrid<?>) grid),
									true),
								hasBulkOperationsRight
									&& UiUtil.permitted(UserRight.CONTACT_ARCHIVE)
									&& EntityRelevanceStatus.ACTIVE.equals(criteria.getRelevanceStatus())),
							new MenuBarHelper.MenuBarItem(
								I18nProperties.getCaption(Captions.actionDearchiveCoreEntity),
								VaadinIcon.ARCHIVE,
								mi -> grid.bulkActionHandler(
									items -> ControllerProvider.getContactController()
										.dearchiveAllSelectedItems(items, (AbstractContactGrid<?>) grid),
									true),
								hasBulkOperationsRight
									&& UiUtil.permitted(UserRight.CONTACT_ARCHIVE, UserRight.CONTACT_VIEW_ARCHIVED)
									&& EntityRelevanceStatus.ARCHIVED.equals(criteria.getRelevanceStatus())),
							new MenuBarHelper.MenuBarItem(
								I18nProperties.getCaption(Captions.sormasToSormasShare),
								VaadinIcon.SHARE,
								mi -> grid.bulkActionHandler(
									items -> ControllerProvider.getSormasToSormasController()
										.shareSelectedContacts(items, () -> navigateTo(criteria))),
								FacadeProvider.getFeatureConfigurationFacade()
									.isPropertyValueTrue(FeatureType.CASE_AND_CONTACT_BULK_ACTIONS, FeatureTypeProperty.S2S_SHARING)
									&& FacadeProvider.getSormasToSormasFacade().isSharingContactsEnabledForUser())));

					if (isDocGenerationAllowed() && grid instanceof AbstractContactGrid) {
						bulkActions.add(
							new MenuBarHelper.MenuBarItem(
								I18nProperties.getCaption(Captions.bulkActionCreatDocuments),
								VaadinIcon.FILE_TEXT,
								mi -> grid.bulkActionHandler(items -> {
									List<ReferenceDto> references = ((AbstractContactGrid<?>) grid).asMultiSelect()
										.getSelectedItems()
										.stream()
										.map(ContactIndexDto::toReference)
										.collect(Collectors.toList());

									if (references.size() == 0) {
										NotificationHelper.show(
											I18nProperties.getString(Strings.headingNoContactsSelected),
											I18nProperties.getString(Strings.messageNoContactsSelected),
											Type.WARNING_MESSAGE);

										return;
									}

									ControllerProvider.getDocGenerationController()
										.showBulkQuarantineOrderDocumentDialog(references, DocumentWorkflow.QUARANTINE_ORDER_CONTACT);
								})));
					}

					if (permitted(FeatureType.EVENT_SURVEILLANCE, UserRight.EVENT_CREATE, UserRight.EVENTPARTICIPANT_CREATE)) {
						bulkActions.add(
							new MenuBarHelper.MenuBarItem(
								I18nProperties.getCaption(Captions.bulkLinkToEvent),
								VaadinIcon.PHONE,
								mi -> grid.bulkActionHandler(
									items -> ControllerProvider.getContactController()
										.linkSelectedContactsToEvent(items, (AbstractContactGrid<?>) grid))));
					}

					bulkActions.add(new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.bulkEmailSend), VaadinIcon.ENVELOPE, mi -> {
						grid.bulkActionHandler(
							items -> ControllerProvider.getContactController().sendEmailsToAllSelectedItems(items, (AbstractContactGrid<?>) grid),
							true);
					}, hasBulkOperationsRight && UiUtil.permitted(UserRight.EXTERNAL_EMAIL_SEND)));
				} else {
					bulkActions = List.of(
						new MenuBarHelper.MenuBarItem(
							I18nProperties.getCaption(Captions.bulkRestore),
							VaadinIcon.ARROW_BACKWARD,
							mi -> grid.bulkActionHandler(
								items -> ControllerProvider.getContactController().restoreSelectedContacts(items, (AbstractContactGrid<?>) grid),
								true),
							hasBulkOperationsRight && UiUtil.permitted(UserRight.CONTACT_DELETE)));
				}

				bulkOperationsDropdown = MenuBarHelper.createDropDown(Captions.bulkActions, bulkActions);

				bulkOperationsDropdown.setVisible(viewConfiguration.isInEagerMode());
				actionButtonsLayout.add(bulkOperationsDropdown);
			}

		}
		statusFilterLayout.add(actionButtonsLayout);
		// was Alignment.TOP_RIGHT: the layout grows, its content goes right
		statusFilterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, actionButtonsLayout);
		statusFilterLayout.setFlexGrow(1, actionButtonsLayout);
		actionButtonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

		return statusFilterLayout;
	}

	private void reloadGrid() {
		((ContactFollowUpGrid) grid).setVisitColumns(criteria);
		criteria.setFollowUpVisitsTo(followUpToDate);
		criteria.setFollowUpVisitsInterval(followUpRangeInterval);
		((ContactFollowUpGrid) grid).reload();
		updateStatusButtons();
		buttonPreviousOrNextClick = false;
	}

	public void enter(ViewChangeEvent event) {
		String params = event.getParameters().trim();
		if (params.startsWith("?")) {
			params = params.substring(1);
			criteria.fromUrlParams(params);

			if (criteria.getEventUuid() != null) {
				criteria.eventLike(criteria.getEventUuid());
				criteria.eventUuid(null);
			}
		}
		updateFilterComponents();

		if (ContactsViewType.FOLLOW_UP_VISITS_OVERVIEW.equals(viewConfiguration.getViewType())) {
			((ContactFollowUpGrid) grid).reload();
		} else {
			if (viewConfiguration.isInEagerMode()) {
				((AbstractContactGrid<?>) grid).setEagerDataProvider();
			}
			((AbstractContactGrid<?>) grid).reload();
		}
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

	private boolean isBulkEditAllowed() {
		return UiUtil.enabled(FeatureType.CASE_AND_CONTACT_BULK_ACTIONS)
			&& viewConfiguration.getViewType().isContactOverview()
			&& (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS) || FacadeProvider.getSormasToSormasFacade().isSharingContactsEnabledForUser());
	}

	private HorizontalLayout dateRangeFollowUpVisitsFilterLayout() {
		HorizontalLayout scrollLayout = new HorizontalLayout();
		scrollLayout.setPadding(false);

		DatePicker toReferenceDate = criteria.getFollowUpVisitsTo() == null
			? new DatePicker(I18nProperties.getCaption(Captions.to), LocalDate.now())
			: new DatePicker(I18nProperties.getCaption(Captions.to), UtilDate.toLocalDate(criteria.getFollowUpVisitsTo()));

		toReferenceDate.setId("toReferenceDateField");
		LocalDate fromReferenceLocal = criteria.getFollowUpVisitsFrom() == null
			? UtilDate.toLocalDate(DateHelper.subtractDays(UtilDate.from(LocalDate.now()), followUpRangeInterval - 1))
			: UtilDate.toLocalDate(criteria.getFollowUpVisitsFrom());

		DatePicker fromReferenceDate = new DatePicker(I18nProperties.getCaption(Captions.from), fromReferenceLocal);
		fromReferenceDate.setId("fromReferenceDateField");

		Button minusDaysButton = ButtonHelper.createButton(I18nProperties.getCaption(Captions.contactMinusDays), e -> {
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
					String.format(I18nProperties.getString(Strings.messageSelectedPeriodTooLong), MAX_FOLLOW_UP_VIEW_DAYS),
					Type.WARNING_MESSAGE);
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
					String.format(I18nProperties.getString(Strings.messageSelectedPeriodTooLong), MAX_FOLLOW_UP_VIEW_DAYS),
					Type.WARNING_MESSAGE);
				fromReferenceDate.setValue(UtilDate.toLocalDate(criteria.getFollowUpVisitsFrom()));
			}
		});
		scrollLayout.add(fromReferenceDate);

		toReferenceDate.addValueChangeListener(e -> {
			Date newFollowUpToDate = e.getValue() != null ? UtilDate.from(e.getValue()) : new Date();
			if (newFollowUpToDate.equals(criteria.getFollowUpUntilTo())) {
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
					String.format(I18nProperties.getString(Strings.messageSelectedPeriodTooLong), MAX_FOLLOW_UP_VIEW_DAYS),
					Type.WARNING_MESSAGE);
				toReferenceDate.setValue(UtilDate.toLocalDate(followUpToDate));
			}
		});
		scrollLayout.add(toReferenceDate);

		Button plusDaysButton = ButtonHelper.createButton(I18nProperties.getCaption(Captions.contactPlusDays), e -> {
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
					String.format(I18nProperties.getString(Strings.messageSelectedPeriodTooLong), MAX_FOLLOW_UP_VIEW_DAYS),
					Type.WARNING_MESSAGE);
			}
		}, CssStyles.BUTTON_PRIMARY, CssStyles.FORCE_CAPTION);
		scrollLayout.add(plusDaysButton);
		return scrollLayout;
	}

	@Override
	public String getName() {
		return VIEW_NAME;
	}

	public static ExportConfigurationDto buildDetailedExportConfiguration() {
		ExportConfigurationDto config = ExportConfigurationDto.build(UiUtil.getUserReference(), ExportType.CASE);

		config.setProperties(
			ImportExportUtils
				.getContactExportProperties(
					ContactDownloadUtil::getPropertyCaption,
					FacadeProvider.getConfigFacade().getCountryLocale(),
					FacadeProvider.getFeatureConfigurationFacade().getActiveServerFeatureConfigurations())
				.stream()
				.map(ExportPropertyMetaInfo::getPropertyId)
				.collect(Collectors.toSet()));
		return config;
	}
}
