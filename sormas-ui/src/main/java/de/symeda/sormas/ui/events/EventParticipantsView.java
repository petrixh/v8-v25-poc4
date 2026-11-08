/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2021 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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
package de.symeda.sormas.ui.events;

import static de.symeda.sormas.ui.docgeneration.DocGenerationHelper.isDocGenerationAllowed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.common.DeletableEntityType;
import de.symeda.sormas.api.event.EventDto;
import de.symeda.sormas.api.event.EventParticipantCriteria;
import de.symeda.sormas.api.event.EventParticipantIndexDto;
import de.symeda.sormas.api.event.EventParticipantReferenceDto;
import de.symeda.sormas.api.event.EventReferenceDto;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.feature.FeatureTypeProperty;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.Descriptions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.importexport.ExportType;
import de.symeda.sormas.api.importexport.ImportExportUtils;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.customexport.ExportConfigurationsLayout;
import de.symeda.sormas.ui.events.eventparticipantimporter.EventParticipantImportLayout;
import de.symeda.sormas.ui.samples.HasName;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ComboBoxHelper;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DetailSubComponentWrapper;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.EventParticipantDownloadUtil;
import de.symeda.sormas.ui.utils.ExportEntityName;
import de.symeda.sormas.ui.utils.GridExportStreamResource;
import de.symeda.sormas.ui.utils.MenuBarHelper;
import de.symeda.sormas.ui.utils.MenuBarHelper.MenuBarItem;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.PopupButton;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.components.expandablebutton.ExpandableButton;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

public class EventParticipantsView extends AbstractEventView implements HasName {

	private static final long serialVersionUID = -1L;

	public static final String EVENTPARTICIPANTS = "eventparticipants";
	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/" + EVENTPARTICIPANTS;

	private EventParticipantCriteria criteria;
	private EventParticipantsViewConfiguration viewConfiguration;

	private EventParticipantsGrid grid;
	private Button addButton;
	private MenuBar bulkOperationsDropdown;
	private DetailSubComponentWrapper gridLayout;
	private Button activeStatusButton;
	private EventParticipantsFilterForm filterForm;

	private Span relevanceStatusInfoLabel;
	private ComboBox eventParticipantRelevanceStatusFilter;

	public EventParticipantsView() {
		super(VIEW_NAME);

		setSizeFull();
		addClassName("crud-view");

		viewConfiguration = ViewModelProviders.of(getClass()).get(EventParticipantsViewConfiguration.class);
	}

	public HorizontalLayout createTopBar() {

		HorizontalLayout topLayout = new HorizontalLayout();
		topLayout.setSpacing(true);
		topLayout.setWidth("100%");

		filterForm = new EventParticipantsFilterForm();
		filterForm.addValueChangeListener(e -> {
			if (!filterForm.hasFilter()) {
				navigateTo(null);
			}
		});
		filterForm.addResetHandler(e -> {
			ViewModelProviders.of(EventParticipantsView.class).remove(EventParticipantCriteria.class);
			navigateTo(null);
		});
		filterForm.addApplyHandler(e -> grid.reload());

		topLayout.add(filterForm);

		return topLayout;
	}

	private boolean allowActions() {
		return !isEventDeleted() && isEditAllowed();
	}

	private boolean shouldDisableButton() {
		return UiUtil.disabled(FeatureType.EDIT_ARCHIVED_ENTITIES) && FacadeProvider.getEventFacade().isArchived(getEventRef().getUuid());
	}

	private Set<String> getSelectedRows() {
		return this.grid.getSelectionModel() instanceof GridMultiSelectionModel
			? grid.asMultiSelect().getSelectedItems().stream().map(EventParticipantIndexDto::getUuid).collect(Collectors.toSet())
			: Collections.emptySet();
	}

	@Override
	protected void initView(String params) {
		EventReferenceDto eventRef = getEventRef();

		criteria = ViewModelProviders.of(EventParticipantsView.class).get(EventParticipantCriteria.class);
		boolean isEventArchived = FacadeProvider.getEventFacade().isArchived(eventRef.getUuid());

		if (!DataHelper.isSame(eventRef, criteria.getEvent()) || !viewConfiguration.isRelevanceStatusChanged(eventRef)) {
			criteria.relevanceStatus(isEventArchived ? EntityRelevanceStatus.ACTIVE_AND_ARCHIVED : EntityRelevanceStatus.ACTIVE);
		}
		criteria.withEvent(eventRef);

		if (grid == null) {
			grid = new EventParticipantsGrid(criteria);
			gridLayout = new DetailSubComponentWrapper(() -> null);
			gridLayout.setSizeFull();
			gridLayout.setPadding(true);
			gridLayout.setSpacing(false);
			gridLayout.add(createTopBar());
			gridLayout.add(createStatusFilterBar(isEditAllowed()));
			gridLayout.add(grid);
			gridLayout.setFlexGrow(1, grid);
			gridLayout.setClassName("crud-main-layout");
			grid.addDataSizeChangeListener(e -> updateStatusButtons());
			setSubComponent(gridLayout);
			gridLayout.setEnabled(allowActions() || FacadeProvider.getEventFacade().hasParticipantWithSpecialAccess(eventRef));
		}

		if (params.startsWith("?")) {
			params = params.substring(1);
			criteria.fromUrlParams(params);
		}
		updateFilterComponents();
	}

	private HorizontalLayout createStatusFilterBar(boolean isEditAllowed) {

		HorizontalLayout statusFilterLayout = new HorizontalLayout();
		statusFilterLayout.setSpacing(true);
		statusFilterLayout.setPadding(false);
		statusFilterLayout.setWidth(100, Unit.PERCENTAGE);
		statusFilterLayout.addClassName(CssStyles.VSPACE_3);

		Button statusAll = ButtonHelper.createButton(Captions.all, e -> {
		}, CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER);
		statusFilterLayout.add(statusAll);
		activeStatusButton = statusAll;

		HorizontalLayout actionButtonsLayout = new HorizontalLayout();
		actionButtonsLayout.setSpacing(true);

		// Show active/archived/all dropdown
		if (UiUtil.enabled(FeatureType.AUTOMATIC_ARCHIVING, DeletableEntityType.EVENT_PARTICIPANT)) {
			int daysAfterEventParticipantGetsArchived = FacadeProvider.getFeatureConfigurationFacade()
				.getProperty(
					FeatureType.AUTOMATIC_ARCHIVING,
					DeletableEntityType.EVENT_PARTICIPANT,
					FeatureTypeProperty.THRESHOLD_IN_DAYS,
					Integer.class);
			if (daysAfterEventParticipantGetsArchived > 0) {
				relevanceStatusInfoLabel = VaadinUiUtil.createLabel(
					HtmlRenderer.iconHtml(VaadinIcon.INFO_CIRCLE) + " "
						+ String.format(I18nProperties.getString(Strings.infoArchivedEventParticipants), daysAfterEventParticipantGetsArchived),
					ContentMode.HTML);
				relevanceStatusInfoLabel.setVisible(false);
				relevanceStatusInfoLabel.addClassName(CssStyles.LABEL_VERTICAL_ALIGN_SUPER);
				actionButtonsLayout.add(relevanceStatusInfoLabel);
				actionButtonsLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, relevanceStatusInfoLabel);
			}
		}

		eventParticipantRelevanceStatusFilter = buildRelevanceStatusFilter(
			Captions.eventParticipantActiveEventParticipants,
			Captions.eventParticipantArchivedEventParticipants,
			Captions.eventParticipantActiveAndArchivedEventParticipants);

		eventParticipantRelevanceStatusFilter.addValueChangeListener(e -> {
			viewConfiguration.setRelevanceStatusChangedEvent(getEventRef().getUuid());
			if (relevanceStatusInfoLabel != null) {
				relevanceStatusInfoLabel.setVisible(EntityRelevanceStatus.ARCHIVED.equals(e.getValue()));
			}

			if (grid.getColumn(grid.DELETE_REASON_COLUMN) != null) {
				grid.getColumn(grid.DELETE_REASON_COLUMN)
					.setHidden(!eventParticipantRelevanceStatusFilter.getValue().equals(EntityRelevanceStatus.DELETED));
			}

			criteria.relevanceStatus((EntityRelevanceStatus) e.getValue());
			navigateTo(criteria);
		});
		actionButtonsLayout.add(eventParticipantRelevanceStatusFilter);

		if (isEditAllowed) {
			Button btnEnterBulkEditMode = ButtonHelper.createIconButton(Captions.actionEnterBulkEditMode, VaadinIcon.CHECK_SQUARE_O, null);
			btnEnterBulkEditMode.setVisible(!viewConfiguration.isInEagerMode());
			actionButtonsLayout.add(btnEnterBulkEditMode);

			Button btnLeaveBulkEditMode =
				ButtonHelper.createIconButton(Captions.actionLeaveBulkEditMode, VaadinIcon.CLOSE, null, CssStyles.BUTTON_PRIMARY);
			btnLeaveBulkEditMode.setVisible(viewConfiguration.isInEagerMode());

			if (shouldDisableButton()) {
				btnEnterBulkEditMode.setEnabled(false);
				btnLeaveBulkEditMode.setEnabled(false);
				bulkOperationsDropdown.setEnabled(false);
			}
			actionButtonsLayout.add(btnLeaveBulkEditMode);

			btnEnterBulkEditMode.addClickListener(e -> {
				bulkOperationsDropdown.setVisible(true);
				ViewModelProviders.of(EventParticipantsView.class).get(EventParticipantsViewConfiguration.class).setInEagerMode(true);
				btnEnterBulkEditMode.setVisible(false);
				btnLeaveBulkEditMode.setVisible(true);
				grid.reload();
			});
			btnLeaveBulkEditMode.addClickListener(e -> {
				bulkOperationsDropdown.setVisible(false);
				ViewModelProviders.of(EventParticipantsView.class).get(EventParticipantsViewConfiguration.class).setInEagerMode(false);
				btnLeaveBulkEditMode.setVisible(false);
				btnEnterBulkEditMode.setVisible(true);
				navigateTo(criteria);
			});

			// Bulk operation dropdown
			if (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS)) {
				bulkOperationsDropdown = MenuBarHelper.createDropDown(Captions.bulkActions, getBulkActions());
				bulkOperationsDropdown.setVisible(viewConfiguration.isInEagerMode());

				actionButtonsLayout.add(bulkOperationsDropdown);
			}

			addImportExportButtons(actionButtonsLayout);

			if (UiUtil.permitted(UserRight.EVENTPARTICIPANT_CREATE)) {
				final ExpandableButton lineListingButton = new ExpandableButton(Captions.lineListing)
					.expand(e -> ControllerProvider.getEventParticipantController().openLineListingWindow(getEventRef()));
				actionButtonsLayout.add(lineListingButton);
				lineListingButton.setEnabled(allowActions());

				addButton = ButtonHelper.createIconButton(Captions.eventParticipantAddPerson, VaadinIcon.PLUS_CIRCLE, e -> {
					ControllerProvider.getEventParticipantController().createEventParticipant(this.getEventRef(), r -> navigateTo(criteria));
				}, CssStyles.BUTTON_PRIMARY);

				actionButtonsLayout.add(addButton);
			}

			statusFilterLayout.add(actionButtonsLayout);
			// was Alignment.MIDDLE_RIGHT: the layout grows, its content goes right
			statusFilterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, actionButtonsLayout);
			statusFilterLayout.setFlexGrow(1, actionButtonsLayout);
			actionButtonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		}

		return statusFilterLayout;
	}

	private ComboBox buildRelevanceStatusFilter(
		String eventParticipantActiveCaption,
		String eventParticipantArchivedCaption,
		String eventParticipantAllCaption) {

		ComboBox relevanceStatusFilter = ComboBoxHelper.createComboBoxV7();
		relevanceStatusFilter.setId("relevanceStatusFilter");
		relevanceStatusFilter.setWidth(200, Unit.PIXELS);
		relevanceStatusFilter.setNullSelectionAllowed(false);
		relevanceStatusFilter.addItems((Object[]) EntityRelevanceStatus.values());
		relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ACTIVE, I18nProperties.getCaption(eventParticipantActiveCaption));

		if (UiUtil.permitted(UserRight.EVENTPARTICIPANT_VIEW_ARCHIVED)) {
			relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ARCHIVED, I18nProperties.getCaption(eventParticipantArchivedCaption));
			relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ACTIVE_AND_ARCHIVED, I18nProperties.getCaption(eventParticipantAllCaption));
		} else {
			relevanceStatusFilter.removeItem(EntityRelevanceStatus.ARCHIVED);
			relevanceStatusFilter.removeItem(EntityRelevanceStatus.ACTIVE_AND_ARCHIVED);
		}

		if (UiUtil.permitted(UserRight.EVENTPARTICIPANT_DELETE)) {
			relevanceStatusFilter
				.setItemCaption(EntityRelevanceStatus.DELETED, I18nProperties.getCaption(Captions.eventParticipantDeletedEventParticipants));
		} else {
			relevanceStatusFilter.removeItem(EntityRelevanceStatus.DELETED);
		}

		return relevanceStatusFilter;
	}

	private List<MenuBarItem> getBulkActions() {
		List<MenuBarItem> bulkActions = new ArrayList<>();
		if (criteria.getRelevanceStatus() != EntityRelevanceStatus.DELETED) {
			if (UiUtil.permitted(UserRight.CONTACT_CREATE)) {
				bulkActions.add(
					new MenuBarItem(
						I18nProperties.getCaption(Captions.bulkEventParticipantsToContacts),
						VaadinIcon.HAND,
						mi -> grid.bulkActionHandler(items -> {
							EventDto eventDto = FacadeProvider.getEventFacade().getEventByUuid(getEventRef().getUuid(), false);
							ControllerProvider.getContactController().openLineListingWindow(eventDto, items);
						}, true)));
			}
			if (UiUtil.permitted(UserRight.EVENTPARTICIPANT_DELETE)) {
				bulkActions.add(new MenuBarItem(I18nProperties.getCaption(Captions.bulkDelete), VaadinIcon.TRASH, mi -> {
					grid.bulkActionHandler(items -> {
						ControllerProvider.getEventParticipantController().deleteAllSelectedItems(items, grid, () -> grid.reload());
					}, true);
				}));
			}
			if (isDocGenerationAllowed()) {
				bulkActions.add(new MenuBarItem(I18nProperties.getCaption(Captions.bulkActionCreatDocuments), VaadinIcon.FILE_TEXT, mi -> {
					grid.bulkActionHandler(items -> {
						List<EventParticipantReferenceDto> references =
							grid.asMultiSelect().getSelectedItems().stream().map(EventParticipantIndexDto::toReference).collect(Collectors.toList());
						if (references.size() == 0) {
							NotificationHelper.show(
								I18nProperties.getString(Strings.headingNoEventParticipantsSelected),
								I18nProperties.getString(Strings.messageNoEventParticipantsSelected),
								NotificationHelper.Type.WARNING_MESSAGE);

							return;
						}

						EventDto eventDto = FacadeProvider.getEventFacade().getEventByUuid(getEventRef().getUuid(), false);

						ControllerProvider.getDocGenerationController()
							.showBulkEventParticipantQuarantineOrderDocumentDialog(references, eventDto.getDisease());
					});
				}));
			}

			bulkActions.add(new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.bulkEmailSend), VaadinIcon.ENVELOPE, mi -> {
				grid.bulkActionHandler(items -> ControllerProvider.getEventParticipantController().sendEmailsToAllSelectedItems(items, grid), true);
			}, UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS) && UiUtil.permitted(UserRight.EXTERNAL_EMAIL_SEND)));

		} else if (UiUtil.permitted(UserRight.EVENTPARTICIPANT_DELETE)) {
			bulkActions.add(new MenuBarItem(I18nProperties.getCaption(Captions.bulkRestore), VaadinIcon.ARROW_BACKWARD, mi -> {
				grid.bulkActionHandler(items -> {
					ControllerProvider.getEventParticipantController().restoreSelectedEventParticipants(items, grid, () -> grid.reload());
				}, true);
			}));
		}
		return bulkActions;
	}

	private void addImportExportButtons(HorizontalLayout actionButtonsLayout) {
		VerticalLayout exportLayout = new VerticalLayout();
		{
			exportLayout.setSpacing(true);
			exportLayout.setPadding(true);
			exportLayout.addClassName(CssStyles.LAYOUT_MINIMAL);
			exportLayout.setWidth(250, Unit.PIXELS);
		}

		if (UiUtil.permitted(UserRight.EVENTPARTICIPANT_IMPORT)) {
			Button importButton = ButtonHelper.createIconButton(Captions.actionImport, VaadinIcon.UPLOAD, e -> {
				Dialog popupWindow = VaadinUiUtil.showPopupWindow(new EventParticipantImportLayout(getEventRef()));
				popupWindow.setHeaderTitle(I18nProperties.getString(Strings.headingImportEventParticipant));
				VaadinUiUtil.addCloseListener(popupWindow, c -> this.grid.reload());
			}, CssStyles.BUTTON_PRIMARY);
			if (shouldDisableButton()) {
				importButton.setEnabled(false);
			}

			actionButtonsLayout.add(importButton);
		}

		// export
		PopupButton exportPopupButton = ButtonHelper.createIconPopupButton(Captions.export, VaadinIcon.DOWNLOAD, exportLayout);
		if (shouldDisableButton()) {
			exportPopupButton.setEnabled(false);
		}
		actionButtonsLayout.add(exportPopupButton);

		{
			DownloadHandler streamResource = GridExportStreamResource.createStreamResourceWithSelectedItems(
				grid,
				() -> this.grid.getSelectionModel() instanceof GridMultiSelectionModel ? this.grid.asMultiSelect().getSelectedItems() : null,
				ExportEntityName.EVENT_PARTICIPANTS);
			addExportButton(
				streamResource,
				exportPopupButton.getPopover(),
				exportLayout,
				VaadinIcon.TABLE,
				Captions.exportBasic,
				Strings.infoBasicExport);
		}

		{
			DownloadHandler extendedExportStreamResource =
				EventParticipantDownloadUtil.createExtendedEventParticipantExportResource(grid.getCriteria(), this::getSelectedRows, null);

			addExportButton(
				extendedExportStreamResource,
				exportPopupButton.getPopover(),
				exportLayout,
				VaadinIcon.FILE_TEXT,
				Captions.exportDetailed,
				Descriptions.descDetailedExportButton);
		}

		{
			Button btnCustomExport = ButtonHelper.createIconButton(Captions.exportCustom, VaadinIcon.FILE_TEXT, e -> {
				Dialog customExportWindow = VaadinUiUtil.createPopupWindow();

				ExportConfigurationsLayout customExportsLayout = new ExportConfigurationsLayout(
					ExportType.EVENT_PARTICIPANTS,
					ImportExportUtils.getEventParticipantExportProperties(
						EventParticipantDownloadUtil::getPropertyCaption,
						FacadeProvider.getConfigFacade().getCountryLocale(),
						FacadeProvider.getFeatureConfigurationFacade().getActiveServerFeatureConfigurations()),
					customExportWindow::close);
				customExportsLayout.setExportCallback((exportConfig) -> {
					// was Page.open(resource, null, true); owned by the export window, which is not inert
					DownloadUtil.startDownload(
						EventParticipantDownloadUtil
							.createExtendedEventParticipantExportResource(grid.getCriteria(), this::getSelectedRows, exportConfig),
						customExportsLayout);
				});
				customExportWindow.setWidth(1024, Unit.PIXELS);
				customExportWindow.setHeaderTitle(I18nProperties.getCaption(Captions.exportCustom));
				customExportWindow.add(customExportsLayout);
				customExportWindow.open();
				exportPopupButton.setPopupVisible(false);
			}, CssStyles.BUTTON_PRIMARY);
			btnCustomExport.setTooltipText(I18nProperties.getString(Strings.infoCustomExport));
			btnCustomExport.setWidth(100, Unit.PERCENTAGE);
			exportLayout.add(btnCustomExport);
		}
	}

	public void updateFilterComponents() {

		// TODO replace with Vaadin 8 databinding
		applyingCriteria = true;

		updateStatusButtons();

		if (eventParticipantRelevanceStatusFilter != null) {
			eventParticipantRelevanceStatusFilter.setValue(criteria.getRelevanceStatus());
		}

		filterForm.setValue(criteria);

		applyingCriteria = false;
	}

	private void updateStatusButtons() {

		if (activeStatusButton != null) {
			// was an HTML caption with LayoutUtil.spanCss(BADGE, count): the count is a badge after the text
			activeStatusButton.setText(I18nProperties.getCaption(Captions.all));
			Span badge = new Span(String.valueOf(grid.getDataSize()));
			badge.addClassName(CssStyles.BADGE);
			activeStatusButton.setSuffixComponent(badge);
		}
	}

	@Override
	public String getName() {
		return VIEW_NAME;
	}
}
