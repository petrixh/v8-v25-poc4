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
package de.symeda.sormas.ui.events;

import static de.symeda.sormas.api.user.UserRight.EVENT_ARCHIVE;
import static de.symeda.sormas.api.user.UserRight.EVENT_VIEW_ARCHIVED;
import static de.symeda.sormas.ui.docgeneration.DocGenerationHelper.isDocGenerationAllowed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.action.ActionDto;
import de.symeda.sormas.api.action.ActionStatus;
import de.symeda.sormas.api.caze.CaseReferenceDto;
import de.symeda.sormas.api.common.DeletableEntityType;
import de.symeda.sormas.api.event.EventActionExportDto;
import de.symeda.sormas.api.event.EventActionIndexDto;
import de.symeda.sormas.api.event.EventCriteria;
import de.symeda.sormas.api.event.EventDto;
import de.symeda.sormas.api.event.EventGroupCriteria;
import de.symeda.sormas.api.event.EventIndexDto;
import de.symeda.sormas.api.event.EventReferenceDto;
import de.symeda.sormas.api.event.EventStatus;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.feature.FeatureTypeProperty;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.importexport.ExportConfigurationDto;
import de.symeda.sormas.api.importexport.ExportPropertyMetaInfo;
import de.symeda.sormas.api.importexport.ImportExportUtils;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.SearchSpecificLayout;
import de.symeda.sormas.ui.SormasUI;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.events.importer.EventImportLayout;
import de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;
import de.symeda.sormas.ui.utils.AbstractView;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ComboBoxHelper;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.EventDownloadUtil;
import de.symeda.sormas.ui.utils.ExportEntityName;
import de.symeda.sormas.ui.utils.FilteredGrid;
import de.symeda.sormas.ui.utils.GridExportStreamResource;
import de.symeda.sormas.ui.utils.MenuBarHelper;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.PopupButton;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.components.popupmenu.PopupMenu;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.fields.OptionGroup;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

public class EventsView extends AbstractView {

	private static final long serialVersionUID = -3048977745713631200L;

	public static final String VIEW_NAME = "events";

	/** Key of the status a status filter button stands for (was {@code Button.setData}). */
	private static final String STATUS_DATA = "status";

	private EventCriteria eventCriteria;
	private EventGroupCriteria eventGroupCriteria;
	private EventsViewConfiguration viewConfiguration;

	private FilteredGrid<?, ?> grid;
	private Button createButton;
	private HashMap<Button, String> statusButtons;
	private Button activeStatusButton;

	// Filter
	private EventsFilterForm eventsFilterForm;
	private EventGroupsFilterForm eventGroupsFilterForm;
	private Span relevanceStatusInfoLabel;
	private ComboBox eventRelevanceStatusFilter;
	private ComboBox groupRelevanceStatusFilter;

	private ComboBox contactCountMethod;

	private VerticalLayout gridLayout;

	// Bulk operations
	private MenuBar bulkOperationsDropdown;

	private Set<String> getSelectedRows() {
		EventGrid eventGrid = (EventGrid) this.grid;
		return this.viewConfiguration.isInEagerMode()
			? eventGrid.asMultiSelect().getSelectedItems().stream().map(EventIndexDto::getUuid).collect(Collectors.toSet())
			: Collections.emptySet();
	}

	public EventsView() {
		super(VIEW_NAME);

		viewConfiguration = ViewModelProviders.of(getClass()).get(EventsViewConfiguration.class);
		if (viewConfiguration.getViewType() == null) {
			viewConfiguration.setViewType(EventsViewType.DEFAULT);
		}

		eventGroupCriteria = ViewModelProviders.of(EventsView.class).get(EventGroupCriteria.class);
		if (eventGroupCriteria.getRelevanceStatus() == null) {
			eventGroupCriteria.relevanceStatus(EntityRelevanceStatus.ACTIVE);
		}

		eventCriteria = ViewModelProviders.of(EventsView.class).get(EventCriteria.class);
		if (eventCriteria.getRelevanceStatus() == null) {
			eventCriteria.relevanceStatus(EntityRelevanceStatus.ACTIVE);
		}

		if (isDefaultViewType()) {
			grid = new EventGrid(eventCriteria, getClass());
		} else if (isActionViewType()) {
			grid = new EventActionsGrid(eventCriteria, getClass());
			getViewTitleLabel().setText(I18nProperties.getCaption(Captions.View_actions));
		} else {
			grid = new EventGroupsGrid(eventGroupCriteria, getClass());
			getViewTitleLabel().setText(I18nProperties.getCaption(Captions.View_groups));
		}
		grid.addDataSizeChangeListener(e -> updateStatusButtons());

		gridLayout = new VerticalLayout();
		gridLayout.add(createFilterBar());
		gridLayout.add(createStatusFilterBar());
		gridLayout.add(grid);
		gridLayout.setPadding(true);
		gridLayout.setSpacing(false);
		gridLayout.setSizeFull();
		gridLayout.setFlexGrow(1, grid);
		gridLayout.setClassName("crud-main-layout");

		addComponent(gridLayout);

		OptionGroup eventsViewSwitcher = new OptionGroup();
		eventsViewSwitcher.setId("eventsViewSwitcher");
		CssStyles.style(
			eventsViewSwitcher,
			CssStyles.FORCE_CAPTION,
			SormasFieldGroupFieldFactory.OPTIONGROUP_HORIZONTAL,
			CssStyles.OPTIONGROUP_HORIZONTAL_PRIMARY,
			CssStyles.VSPACE_TOP_3);
		eventsViewSwitcher.addItem(EventsViewType.DEFAULT);
		eventsViewSwitcher.setItemCaption(EventsViewType.DEFAULT, I18nProperties.getCaption(Captions.eventDefaultView));

		eventsViewSwitcher.addItem(EventsViewType.ACTIONS);
		eventsViewSwitcher.setItemCaption(EventsViewType.ACTIONS, I18nProperties.getCaption(Captions.eventActionsView));

		boolean eventGroupsFeatureEnabled = UiUtil.enabled(FeatureType.EVENT_GROUPS);
		if (eventGroupsFeatureEnabled) {
			eventsViewSwitcher.addItem(EventsViewType.GROUPS);
			eventsViewSwitcher.setItemCaption(EventsViewType.GROUPS, I18nProperties.getCaption(Captions.eventGroupsView));
		}

		eventsViewSwitcher.setValue(viewConfiguration.getViewType());
		eventsViewSwitcher.addValueChangeListener(e -> {
			EventsViewType viewType = (EventsViewType) e.getValue();

			viewConfiguration.setViewType(viewType);
			SormasUI.get().getNavigator().navigateTo(EventsView.VIEW_NAME);
		});
		addHeaderComponent(eventsViewSwitcher);

		if (isDefaultViewType() && UiUtil.permitted(UserRight.EVENT_IMPORT)) {
			Button importButton = ButtonHelper.createIconButton(Captions.actionImport, VaadinIcon.UPLOAD, e -> {
				Dialog popupWindow = VaadinUiUtil.showPopupWindow(new EventImportLayout());
				popupWindow.setHeaderTitle(I18nProperties.getString(Strings.headingImportEvent));
				VaadinUiUtil.addCloseListener(popupWindow, c -> ((EventGrid) grid).reload());
			}, CssStyles.BUTTON_PRIMARY);

			addHeaderComponent(importButton);
		}

		if (UiUtil.permitted(UserRight.EVENT_EXPORT)) {
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
					() -> isDefaultViewType() && this.viewConfiguration.isInEagerMode()
						? this.grid.asMultiSelect().getSelectedItems()
						: Collections.emptySet(),
					ExportEntityName.EVENTS);
				addExportButton(
					streamResource,
					exportPopupButton.getPopover(),
					exportLayout,
					VaadinIcon.TABLE,
					Captions.exportBasic,
					Strings.infoBasicExport);
			}

			{
				if (isDefaultViewType()) {
					DownloadHandler exportStreamResource = EventDownloadUtil
						.createEventExportResource((EventCriteria) grid.getCriteria(), this::getSelectedRows, buildDetailedExportConfiguration());

					addExportButton(
						exportStreamResource,
						exportPopupButton.getPopover(),
						exportLayout,
						VaadinIcon.FILE_TEXT,
						Captions.exportDetailed,
						Strings.infoDetailedExport);
				} else if (isActionViewType()) {
					DownloadHandler exportStreamResource = DownloadUtil.createCsvExportStreamResource(
						EventActionExportDto.class,
						null,
						(Integer start, Integer max) -> FacadeProvider.getActionFacade()
							.getEventActionExportList((EventCriteria) grid.getCriteria(), start, max),
						(propertyId, type) -> {
							String caption = I18nProperties.findPrefixCaption(
								propertyId,
								EventActionExportDto.I18N_PREFIX,
								EventActionIndexDto.I18N_PREFIX,
								ActionDto.I18N_PREFIX,
								EventDto.I18N_PREFIX);
							if (Date.class.isAssignableFrom(type)) {
								caption += " (" + DateFormatHelper.getDateFormatPattern() + ")";
							}
							return caption;
						},
						ExportEntityName.EVENT_ACTIONS,
						null);
					addExportButton(
						exportStreamResource,
						exportPopupButton.getPopover(),
						exportLayout,
						VaadinIcon.FILE_TEXT,
						Captions.exportDetailed,
						Strings.infoDetailedExport);
				} else {
					// NOOP: No detailed export for the groups view
				}
			}
		}

		if (UiUtil.permitted(UserRight.EVENT_CREATE)) {
			createButton = ButtonHelper.createIconButton(
				Captions.eventNewEvent,
				VaadinIcon.PLUS_CIRCLE,
				e -> ControllerProvider.getEventController().create((CaseReferenceDto) null),
				CssStyles.BUTTON_PRIMARY);

			addHeaderComponent(createButton);
		}

		final PopupMenu moreButton = new PopupMenu(I18nProperties.getCaption(Captions.moreActions));

		if (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS) && isDefaultViewType()) {
			Button btnEnterBulkEditMode = ButtonHelper.createIconButton(Captions.actionEnterBulkEditMode, VaadinIcon.CHECK_SQUARE_O, null);
			{
				btnEnterBulkEditMode.setVisible(!viewConfiguration.isInEagerMode());
				CssStyles.style(btnEnterBulkEditMode, CssStyles.BUTTON_PRIMARY);
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
				ViewModelProviders.of(EventsView.class).get(EventsViewConfiguration.class).setInEagerMode(true);
				btnEnterBulkEditMode.setVisible(false);
				btnLeaveBulkEditMode.setVisible(true);
				((EventGrid) grid).reload();
			});
			btnLeaveBulkEditMode.addClickListener(e -> {
				bulkOperationsDropdown.setVisible(false);
				ViewModelProviders.of(EventsView.class).get(EventsViewConfiguration.class).setInEagerMode(false);
				btnLeaveBulkEditMode.setVisible(false);
				btnEnterBulkEditMode.setVisible(true);
				navigateTo(eventCriteria);
			});
		}

		if (isDefaultViewType()) {
			Button searchSpecificEventButton = ButtonHelper.createIconButton(Captions.eventSearchSpecificEvent, VaadinIcon.SEARCH, e -> {
				buildAndOpenSearchSpecificEventWindow();
				moreButton.setPopupVisible(false);
			}, CssStyles.BUTTON_PRIMARY);
			searchSpecificEventButton.setWidth(100, Unit.PERCENTAGE);
			moreButton.addMenuEntry(searchSpecificEventButton);
		}

		if (moreButton.hasMenuEntries()) {
			addHeaderComponent(moreButton);
		}
	}

	private boolean isDefaultViewType() {
		return viewConfiguration.getViewType() == EventsViewType.DEFAULT;
	}

	private boolean isActionViewType() {
		return viewConfiguration.getViewType() == EventsViewType.ACTIONS;
	}

	private boolean isGroupViewType() {
		return viewConfiguration.getViewType() == EventsViewType.GROUPS;
	}

	public HorizontalLayout createFilterBar() {
		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.setSpacing(true);
		filterLayout.setPadding(false);
		filterLayout.setSizeUndefined();

		eventGroupsFilterForm = new EventGroupsFilterForm();
		eventGroupsFilterForm.addValueChangeListener(e -> {
			if (!eventGroupsFilterForm.hasFilter()) {
				navigateTo(null);
			}
		});
		eventGroupsFilterForm.addResetHandler(e -> {
			ViewModelProviders.of(EventsView.class).remove(EventGroupCriteria.class);
			ViewModelProviders.of(EventsView.class).remove(EventCriteria.class);
			navigateTo(null);
		});
		eventGroupsFilterForm.addApplyHandler(e -> {
			((EventGroupsGrid) grid).reload();
		});

		eventsFilterForm = new EventsFilterForm(isDefaultViewType(), isDefaultViewType());
		eventsFilterForm.addValueChangeListener(e -> {
			if (!eventsFilterForm.hasFilter()) {
				navigateTo(null);
			}
		});
		eventsFilterForm.addResetHandler(e -> {
			ViewModelProviders.of(EventsView.class).remove(EventGroupCriteria.class);
			ViewModelProviders.of(EventsView.class).remove(EventCriteria.class);
			navigateTo(null);
		});
		eventsFilterForm.addApplyHandler(e -> {
			if (isDefaultViewType()) {
				((EventGrid) grid).reload();
			} else {
				((EventActionsGrid) grid).reload();
			}
		});

		if (isGroupViewType()) {
			filterLayout.add(eventGroupsFilterForm);
		} else {
			filterLayout.add(eventsFilterForm);
		}

		return filterLayout;
	}

	private void buildAndOpenSearchSpecificEventWindow() {
		Dialog window = VaadinUiUtil.createPopupWindow();
		window.setHeaderTitle(I18nProperties.getCaption(Captions.eventSearchSpecificEvent));
		window.setWidth(768, Unit.PIXELS);

		SearchSpecificLayout layout = buildSearchSpecificLayout(window);
		window.add(layout);
		window.open();
	}

	private SearchSpecificLayout buildSearchSpecificLayout(Dialog window) {

		String description = I18nProperties.getString(Strings.infoSpecificEventSearch);
		String confirmCaption = I18nProperties.getCaption(Captions.eventSearchEvent);

		TextField searchField = new TextField();
		Runnable confirmCallback = () -> {
			String foundEventUuid = FacadeProvider.getEventFacade().getUuidByCaseUuidOrPersonUuid(searchField.getValue());

			if (foundEventUuid != null) {
				ControllerProvider.getEventController().navigateToData(foundEventUuid);
				window.close();
			} else {
				VaadinUiUtil.showSimplePopupWindow(
					I18nProperties.getString(Strings.headingNoEventFound),
					I18nProperties.getString(Strings.messageNoEventFound));
			}
		};

		return new SearchSpecificLayout(confirmCallback, () -> window.close(), searchField, description, confirmCaption);
	}

	public HorizontalLayout createStatusFilterBar() {

		HorizontalLayout statusFilterLayout = new HorizontalLayout();
		statusFilterLayout.setSpacing(true);
		statusFilterLayout.setPadding(false);
		statusFilterLayout.setWidth(100, Unit.PERCENTAGE);
		statusFilterLayout.addClassName(CssStyles.VSPACE_3);

		statusButtons = new HashMap<>();

		if (isDefaultViewType()) {
			Button statusAll = ButtonHelper.createButton(Captions.all, e -> {
				eventCriteria.setEventStatus(null);
				navigateTo(eventCriteria);
			}, CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER);

			statusFilterLayout.add(statusAll);

			statusButtons.put(statusAll, I18nProperties.getCaption(Captions.all));
			activeStatusButton = statusAll;

			for (EventStatus status : EventStatus.values()) {
				Button statusButton = ButtonHelper.createButton("status-" + status, status.toString(), e -> {
					eventCriteria.setEventStatus(status);
					navigateTo(eventCriteria);
				}, CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER, CssStyles.BUTTON_FILTER_LIGHT);
				ComponentUtil.setData(statusButton, STATUS_DATA, status);

				statusFilterLayout.add(statusButton);

				statusButtons.put(statusButton, status.toString());
			}
		} else if (isActionViewType()) {
			Button statusAll = ButtonHelper.createButton(Captions.all, e -> {
				eventCriteria.setActionStatus(null);
				navigateTo(eventCriteria);
			}, CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER);

			statusFilterLayout.add(statusAll);

			statusButtons.put(statusAll, I18nProperties.getCaption(Captions.all));
			activeStatusButton = statusAll;

			for (ActionStatus status : ActionStatus.values()) {
				Button statusButton = ButtonHelper.createButton("status-" + status, status.toString(), e -> {
					eventCriteria.actionStatus(status);
					navigateTo(eventCriteria);
				}, CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER, CssStyles.BUTTON_FILTER_LIGHT);
				ComponentUtil.setData(statusButton, STATUS_DATA, status);

				statusFilterLayout.add(statusButton);

				statusButtons.put(statusButton, status.toString());
			}
		}

		HorizontalLayout actionButtonsLayout = new HorizontalLayout();
		actionButtonsLayout.setSpacing(true);
		{
			// Show active/archived/all dropdown
			if (UiUtil.permitted(UserRight.EVENT_VIEW)) {
				if (isGroupViewType()) {
					groupRelevanceStatusFilter =
						buildRelevanceStatus(Captions.eventActiveGroups, Captions.eventArchivedGroups, Captions.eventAllGroups);
					groupRelevanceStatusFilter.removeItem(EntityRelevanceStatus.DELETED);
					groupRelevanceStatusFilter.addValueChangeListener(e -> {
						eventGroupCriteria.relevanceStatus((EntityRelevanceStatus) e.getValue());
						navigateTo(eventGroupCriteria);
					});
					actionButtonsLayout.add(groupRelevanceStatusFilter);
				} else {
					if (UiUtil.enabled(FeatureType.AUTOMATIC_ARCHIVING, DeletableEntityType.EVENT)) {

						int daysAfterEventGetsArchived = FacadeProvider.getFeatureConfigurationFacade()
							.getProperty(
								FeatureType.AUTOMATIC_ARCHIVING,
								DeletableEntityType.EVENT,
								FeatureTypeProperty.THRESHOLD_IN_DAYS,
								Integer.class);
						if (daysAfterEventGetsArchived > 0) {
							relevanceStatusInfoLabel = VaadinUiUtil.createLabel(
								HtmlRenderer.iconHtml(VaadinIcon.INFO_CIRCLE) + " "
									+ String.format(I18nProperties.getString(Strings.infoArchivedEvents), daysAfterEventGetsArchived),
								ContentMode.HTML);
							relevanceStatusInfoLabel.setVisible(false);
							relevanceStatusInfoLabel.addClassName(CssStyles.LABEL_VERTICAL_ALIGN_TOP);
							actionButtonsLayout.add(relevanceStatusInfoLabel);
							// was new Alignment(38) (right and middle bits; Vaadin 8 showed it as middle right)
							actionButtonsLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, relevanceStatusInfoLabel);
						}
					}

					eventRelevanceStatusFilter =
						buildRelevanceStatus(Captions.eventActiveEvents, Captions.eventArchivedEvents, Captions.eventAllActiveAndArchivedEvents);
					eventRelevanceStatusFilter.addValueChangeListener(e -> {
						if (relevanceStatusInfoLabel != null) {
							relevanceStatusInfoLabel.setVisible(EntityRelevanceStatus.ARCHIVED.equals(e.getValue()));
						}

						if (grid.getColumn(grid.DELETE_REASON_COLUMN) != null) {
							grid.getColumn(grid.DELETE_REASON_COLUMN)
								.setHidden(!eventRelevanceStatusFilter.getValue().equals(EntityRelevanceStatus.DELETED));
						}

						eventCriteria.relevanceStatus((EntityRelevanceStatus) e.getValue());
						navigateTo(eventCriteria);
					});
					actionButtonsLayout.add(eventRelevanceStatusFilter);
				}
			}

			// Bulk operation dropdown
			if (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS) && isDefaultViewType()) {
				EventGrid eventGrid = (EventGrid) grid;
				List<MenuBarHelper.MenuBarItem> bulkActions = new ArrayList<>();
				if (eventCriteria.getRelevanceStatus() != EntityRelevanceStatus.DELETED) {
					if (UiUtil.permitted(UserRight.EVENT_EDIT)) {
						bulkActions.add(
							new MenuBarHelper.MenuBarItem(
								I18nProperties.getCaption(Captions.bulkEdit),
								VaadinIcon.ELLIPSIS_H,
								mi -> grid.bulkActionHandler(
									items -> ControllerProvider.getEventController().showBulkEventDataEditComponent(items, (EventGrid) grid))));
					}
					if (UiUtil.permitted(UserRight.EVENT_DELETE)) {
						bulkActions.add(
							new MenuBarHelper.MenuBarItem(
								I18nProperties.getCaption(Captions.bulkDelete),
								VaadinIcon.TRASH,
								mi -> grid.bulkActionHandler(
									items -> ControllerProvider.getEventController().deleteAllSelectedItems(items, (EventGrid) grid),
									true)));
					}
					bulkActions.add(
						new MenuBarHelper.MenuBarItem(
							I18nProperties.getCaption(Captions.actionArchiveCoreEntity),
							VaadinIcon.ARCHIVE,
							mi -> grid
								.bulkActionHandler(items -> ControllerProvider.getEventController().archiveAllSelectedItems(items, eventGrid), true),
							UiUtil.permitted(EVENT_ARCHIVE) && EntityRelevanceStatus.ACTIVE.equals(eventCriteria.getRelevanceStatus())));
					bulkActions.add(
						new MenuBarHelper.MenuBarItem(
							I18nProperties.getCaption(Captions.actionDearchiveCoreEntity),
							VaadinIcon.ARCHIVE,
							mi -> grid.bulkActionHandler(
								items -> ControllerProvider.getEventController()
									.dearchiveAllSelectedItems(eventGrid.asMultiSelect().getSelectedItems(), eventGrid),
								true),
							UiUtil.permitted(EVENT_ARCHIVE, EVENT_VIEW_ARCHIVED)
								&& EntityRelevanceStatus.ARCHIVED.equals(eventCriteria.getRelevanceStatus())));

					if (UiUtil.permitted(UserRight.EVENTGROUP_CREATE, UserRight.EVENTGROUP_LINK)) {
						bulkActions.add(
							new MenuBarHelper.MenuBarItem(
								I18nProperties.getCaption(Captions.actionGroupEvent),
								VaadinIcon.FILE_TREE,
								mi -> grid.bulkActionHandler(
									items -> ControllerProvider.getEventController()
										.linkAllToGroup(eventGrid.asMultiSelect().getSelectedItems(), eventGrid))));
					}
					bulkActions.add(
						new MenuBarHelper.MenuBarItem(
							I18nProperties.getCaption(Captions.ExternalSurveillanceToolGateway_send),
							VaadinIcon.SHARE,
							mi -> grid.bulkActionHandler(
								items -> ControllerProvider.getEventController()
									.sendAllSelectedToExternalSurveillanceTool(eventGrid.asMultiSelect().getSelectedItems(), eventGrid)),
							UiUtil.permitted(UserRight.EXTERNAL_SURVEILLANCE_SHARE)
								&& FacadeProvider.getExternalSurveillanceToolFacade().isFeatureEnabled()));

					if (isDocGenerationAllowed()) {
						bulkActions.add(
							new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.bulkActionCreatDocuments), VaadinIcon.FILE_TEXT, mi -> {
								grid.bulkActionHandler(items -> {

									EventGrid eventGrid1 = (EventGrid) this.grid;
									List<EventReferenceDto> references = eventGrid1.asMultiSelect()
										.getSelectedItems()
										.stream()
										.map(EventIndexDto::toReference)
										.collect(Collectors.toList());
									if (references.size() == 0) {
										NotificationHelper.show(
											I18nProperties.getString(Strings.headingNoEventsSelected),
											I18nProperties.getString(Strings.headingNoEventsSelected),
											NotificationHelper.Type.WARNING_MESSAGE);

										return;
									}

									ControllerProvider.getDocGenerationController().showEventDocumentDialog(references);
								});
							}));
					}
				} else if (UiUtil.permitted(UserRight.EVENT_DELETE)) {
					bulkActions.add(
						new MenuBarHelper.MenuBarItem(
							I18nProperties.getCaption(Captions.bulkRestore),
							VaadinIcon.ARROW_BACKWARD,
							mi -> grid.bulkActionHandler(
								items -> ControllerProvider.getEventController().restoreSelectedEvents(items, (EventGrid) grid),
								true)));
				}

				bulkOperationsDropdown = MenuBarHelper.createDropDown(Captions.bulkActions, bulkActions);

				bulkOperationsDropdown.setVisible(viewConfiguration.isInEagerMode());
				// was setCaption(""): a Flow menu bar has no caption
				actionButtonsLayout.add(bulkOperationsDropdown);
			}

			if (isDefaultViewType()) {
				// Contact Count Method Dropdown
				contactCountMethod = ComboBoxHelper.createComboBoxV7();
				contactCountMethod.setCaption(I18nProperties.getCaption(Captions.Event_contactCountMethod));
				contactCountMethod.addItem(EventContactCountMethod.ALL);
				contactCountMethod.addItem(EventContactCountMethod.SOURCE_CASE_IN_EVENT);
				contactCountMethod.addItem(EventContactCountMethod.BOTH_METHODS);
				contactCountMethod.setItemCaption(EventContactCountMethod.ALL, I18nProperties.getEnumCaption(EventContactCountMethod.ALL));
				contactCountMethod.setItemCaption(
					EventContactCountMethod.SOURCE_CASE_IN_EVENT,
					I18nProperties.getEnumCaption(EventContactCountMethod.SOURCE_CASE_IN_EVENT));
				contactCountMethod
					.setItemCaption(EventContactCountMethod.BOTH_METHODS, I18nProperties.getEnumCaption(EventContactCountMethod.BOTH_METHODS));
				contactCountMethod.setValue(EventContactCountMethod.ALL);
				contactCountMethod.setTextInputAllowed(false);
				contactCountMethod.setNullSelectionAllowed(false);
				contactCountMethod.addValueChangeListener(event -> {
					((EventGrid) grid).setContactCountMethod((EventContactCountMethod) event.getValue());
					((EventGrid) grid).reload();
				});
				actionButtonsLayout.add(contactCountMethod);
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
		if (isGroupViewType()) {
			eventCriteria = ViewModelProviders.of(EventsView.class).get(EventCriteria.class);
		} else {
			eventGroupCriteria = ViewModelProviders.of(EventsView.class).get(EventGroupCriteria.class);
		}
		String params = event.getParameters().trim();
		if (params.startsWith("?")) {
			params = params.substring(1);
			if (isGroupViewType()) {
				eventGroupCriteria.fromUrlParams(params);
			} else {
				eventCriteria.fromUrlParams(params);
			}
		}

		if (viewConfiguration.isInEagerMode() && isDefaultViewType()) {
			((EventGrid) grid).setEagerDataProvider();
		}

		updateFilterComponents();
		if (isDefaultViewType()) {
			((EventGrid) grid).reload();
		} else if (isActionViewType()) {
			((EventActionsGrid) grid).reload();
		} else {
			((EventGroupsGrid) grid).reload();
		}
	}

	public void updateFilterComponents() {

		// TODO replace with Vaadin 8 databinding
		applyingCriteria = true;

		updateStatusButtons();
		if (eventRelevanceStatusFilter != null) {
			eventRelevanceStatusFilter.setValue(eventCriteria.getRelevanceStatus());
		}
		if (groupRelevanceStatusFilter != null) {
			groupRelevanceStatusFilter.setValue(eventGroupCriteria.getRelevanceStatus());
		}

		eventsFilterForm.setValue(eventCriteria);
		eventGroupsFilterForm.setValue(eventGroupCriteria);

		applyingCriteria = false;
	}

	private void updateStatusButtons() {

		statusButtons.keySet().forEach(b -> {
			CssStyles.style(b, CssStyles.BUTTON_FILTER_LIGHT);
			b.setText(statusButtons.get(b));
			b.setSuffixComponent(null);
			if (ComponentUtil.getData(b, STATUS_DATA) == (isDefaultViewType() ? eventCriteria.getEventStatus() : eventCriteria.getActionStatus())) {
				activeStatusButton = b;
			}
		});

		if (activeStatusButton != null) {
			CssStyles.removeStyles(activeStatusButton, CssStyles.BUTTON_FILTER_LIGHT);
			if (activeStatusButton != null) {
				// was an HTML caption with LayoutUtil.spanCss(BADGE, count): the count is a badge after the text
				activeStatusButton.setText(statusButtons.get(activeStatusButton));
				Span badge = new Span(String.valueOf(grid.getDataSize()));
				badge.addClassName(CssStyles.BADGE);
				activeStatusButton.setSuffixComponent(badge);
			}
		}
	}

	private ExportConfigurationDto buildDetailedExportConfiguration() {
		ExportConfigurationDto config = ExportConfigurationDto.build(UiUtil.getUserReference(), null);
		boolean eventGroupFeatureEnabled = UiUtil.enabled(FeatureType.EVENT_GROUPS);
		config.setProperties(
			ImportExportUtils
				.getEventExportProperties(
					EventDownloadUtil::getPropertyCaption,
					eventGroupFeatureEnabled,
					FacadeProvider.getConfigFacade().getCountryLocale(),
					FacadeProvider.getFeatureConfigurationFacade().getActiveServerFeatureConfigurations())
				.stream()
				.map(ExportPropertyMetaInfo::getPropertyId)
				.collect(Collectors.toSet()));
		return config;
	}

	private ComboBox buildRelevanceStatus(String activeCaption, String archivedCaption, String allCaption) {
		ComboBox relevanceStatusFilter = ComboBoxHelper.createComboBoxV7();
		relevanceStatusFilter.setId("relevanceStatusFilter");
		relevanceStatusFilter.setWidth(210, Unit.PIXELS);
		relevanceStatusFilter.setNullSelectionAllowed(false);
		relevanceStatusFilter.setTextInputAllowed(false);
		relevanceStatusFilter.addItems((Object[]) EntityRelevanceStatus.values());
		relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ACTIVE, I18nProperties.getCaption(activeCaption));

		if (isGroupViewType()) {
			setRelevanceStatusFilterItems(relevanceStatusFilter, UserRight.EVENTGROUP_VIEW_ARCHIVED, archivedCaption, allCaption);
		} else {
			setRelevanceStatusFilterItems(relevanceStatusFilter, UserRight.EVENT_VIEW_ARCHIVED, archivedCaption, allCaption);
			if (UiUtil.permitted(UserRight.EVENT_DELETE)) {
				relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.DELETED, I18nProperties.getCaption(Captions.eventDeletedEvents));
			} else {
				relevanceStatusFilter.removeItem(EntityRelevanceStatus.DELETED);
			}
		}

		relevanceStatusFilter.setCaption("");
		return relevanceStatusFilter;
	}

	private void setRelevanceStatusFilterItems(ComboBox relevanceStatusFilter, UserRight userRight, String archivedCaption, String allCaption) {
		if (UiUtil.permitted(userRight)) {
			relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ARCHIVED, I18nProperties.getCaption(archivedCaption));
			relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ACTIVE_AND_ARCHIVED, I18nProperties.getCaption(allCaption));
		} else {
			relevanceStatusFilter.removeItem(EntityRelevanceStatus.ARCHIVED);
			relevanceStatusFilter.removeItem(EntityRelevanceStatus.ACTIVE_AND_ARCHIVED);
		}
	}
}
