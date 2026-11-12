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
package de.symeda.sormas.ui.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.symeda.sormas.ui.utils.fields.ComboBox;

import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.task.TaskCriteria;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ComboBoxHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.MenuBarHelper;

@SuppressWarnings("serial")
public class TaskGridComponent extends VerticalLayout {

	private static final String MY_TASKS = "myTasks";
	private static final String OFFICER_TASKS = "officerTasks";
	private static final String STATUS_DATA = "status";

	private TaskCriteria criteria;

	private TaskGrid grid;
	private TasksView tasksView;
	private Map<Button, String> statusButtons;
	private Button activeStatusButton;

	// Filter
	private TaskGridFilterForm filterForm;
	private ComboBox relevanceStatusFilter;

	MenuBar bulkOperationsDropdown;

	private Span viewTitleLabel;
	private String originalViewTitle;

	public TaskGridComponent(Span viewTitleLabel, TasksView tasksView) {
		setSizeFull();
		setMargin(false);

		this.viewTitleLabel = viewTitleLabel;
		this.tasksView = tasksView;
		originalViewTitle = viewTitleLabel.getText();

		criteria = ViewModelProviders.of(TasksView.class).get(TaskCriteria.class);
		if (criteria.getRelevanceStatus() == null) {
			criteria.relevanceStatus(EntityRelevanceStatus.ACTIVE);
		}

		grid = new TaskGrid(criteria);
		VerticalLayout gridLayout = new VerticalLayout();
		gridLayout.add(createFilterBar());
		gridLayout.add(createAssigneeFilterBar());
		gridLayout.add(grid);
		grid.addDataSizeChangeListener(e -> updateAssigneeFilterButtons());

		gridLayout.setPadding(true);
		styleGridLayout(gridLayout);

		add(gridLayout);
	}

	public HorizontalLayout createFilterBar() {
		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.setPadding(false);
		filterLayout.setSpacing(true);
		filterLayout.setSizeUndefined();

		filterForm = new TaskGridFilterForm();
		filterForm.addResetHandler(e -> {
			ViewModelProviders.of(TasksView.class).remove(TaskCriteria.class);
			tasksView.navigateTo(null, true);
		});
		filterForm.addApplyHandler(e -> grid.reload());

		filterLayout.add(filterForm);

		return filterLayout;
	}

	public HorizontalLayout createAssigneeFilterBar() {
		HorizontalLayout assigneeFilterLayout = new HorizontalLayout();
		assigneeFilterLayout.setPadding(false);
		assigneeFilterLayout.setSpacing(true);
		assigneeFilterLayout.setWidthFull();
		assigneeFilterLayout.addClassName(CssStyles.VSPACE_3);

		statusButtons = new HashMap<>();

		HorizontalLayout buttonFilterLayout = new HorizontalLayout();
		buttonFilterLayout.setSpacing(true);
		{
			Button allTasks =
				ButtonHelper.createButton(Captions.all, e -> processAssigneeFilterChange(null), CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER);
			// was setCaptionAsHtml(true): the count badge is a suffix component (updateAssigneeFilterButtons)

			buttonFilterLayout.add(allTasks);
			statusButtons.put(allTasks, I18nProperties.getCaption(Captions.all));

			createAndAddStatusButton(Captions.taskOfficerTasks, OFFICER_TASKS, buttonFilterLayout);
			Button myTasks = createAndAddStatusButton(Captions.taskMyTasks, MY_TASKS, buttonFilterLayout);

			// Default filter for lab users (that don't have any other role) is "My tasks"
			if (UiUtil.hasLaboratoryOrExternalLaboratoryJurisdictionLevel()) {
				activeStatusButton = myTasks;
			} else {
				activeStatusButton = allTasks;
			}
		}
		assigneeFilterLayout.add(buttonFilterLayout);

		HorizontalLayout actionButtonsLayout = new HorizontalLayout();
		actionButtonsLayout.setSpacing(true);
		{
			// Show active/archived/all dropdown
			if (UiUtil.permitted(UserRight.TASK_VIEW)) {
				relevanceStatusFilter = ComboBoxHelper.createComboBoxV7();
				relevanceStatusFilter.setId("relevanceStatusFilter");
				// was 140%: Vaadin 8 ignored a relative width in this undefined-width layout and showed the default width
				relevanceStatusFilter.setWidth(160, Unit.PIXELS);
				relevanceStatusFilter.setNullSelectionAllowed(false);
				relevanceStatusFilter.addItems(EntityRelevanceStatus.getAllExceptDeleted());
				relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ACTIVE, I18nProperties.getCaption(Captions.taskActiveTasks));

				if (UiUtil.permitted(UserRight.TASK_VIEW_ARCHIVED)) {
					relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ARCHIVED, I18nProperties.getCaption(Captions.taskArchivedTasks));
					relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ACTIVE_AND_ARCHIVED, I18nProperties.getCaption(Captions.taskAllTasks));
				} else {
					relevanceStatusFilter.removeItem(EntityRelevanceStatus.ARCHIVED);
					relevanceStatusFilter.removeItem(EntityRelevanceStatus.ACTIVE_AND_ARCHIVED);
				}

				relevanceStatusFilter.addValueChangeListener(e -> {
					criteria.relevanceStatus((EntityRelevanceStatus) e.getValue());
					tasksView.navigateTo(criteria);
				});
				actionButtonsLayout.add(relevanceStatusFilter);
			}

			// Bulk operation dropdown
			boolean hasBulkOperationsRight = UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS);
			if (hasBulkOperationsRight) {
				assigneeFilterLayout.setWidthFull();

				final List<MenuBarHelper.MenuBarItem> menuBarItems = new ArrayList<>();

				menuBarItems.add(
					new MenuBarHelper.MenuBarItem(
						I18nProperties.getCaption(Captions.bulkEdit),
						VaadinIcon.ELLIPSIS_H,
						mi -> ControllerProvider.getTaskController()
							.showBulkTaskDataEditComponent(this.grid.asMultiSelect().getSelectedItems(), grid, () -> tasksView.navigateTo(criteria)),
						hasBulkOperationsRight && UiUtil.permitted(UserRight.TASK_EDIT)));
				menuBarItems.add(
					new MenuBarHelper.MenuBarItem(
						//here
						I18nProperties.getCaption(Captions.bulkDelete),
						VaadinIcon.TRASH,
						selectedItem -> ControllerProvider.getTaskController()
							.deleteAllSelectedItems(this.grid.asMultiSelect().getSelectedItems(), grid, () -> tasksView.navigateTo(criteria)),
						hasBulkOperationsRight && UiUtil.permitted(UserRight.TASK_DELETE)));
				menuBarItems.add(
					new MenuBarHelper.MenuBarItem(
						I18nProperties.getCaption(Captions.actionArchiveCoreEntity),
						VaadinIcon.ARCHIVE,
						mi -> ControllerProvider.getTaskController()
							.archiveAllSelectedItems(this.grid.asMultiSelect().getSelectedItems(), grid, () -> tasksView.navigateTo(criteria)),
						hasBulkOperationsRight
							&& UiUtil.permitted(UserRight.TASK_ARCHIVE)
							&& EntityRelevanceStatus.ACTIVE.equals(criteria.getRelevanceStatus())));
				menuBarItems.add(
					new MenuBarHelper.MenuBarItem(
						I18nProperties.getCaption(Captions.actionDearchiveCoreEntity),
						VaadinIcon.ARCHIVE,
						mi -> ControllerProvider.getTaskController()
							.dearchiveAllSelectedItems(this.grid.asMultiSelect().getSelectedItems(), grid, () -> tasksView.navigateTo(criteria)),
						hasBulkOperationsRight
							&& UiUtil.permitted(UserRight.TASK_ARCHIVE, UserRight.TASK_VIEW_ARCHIVED)
							&& EntityRelevanceStatus.ARCHIVED.equals(criteria.getRelevanceStatus())));

				bulkOperationsDropdown = MenuBarHelper.createDropDown(Captions.bulkActions, menuBarItems);

				bulkOperationsDropdown.setVisible(tasksView.getViewConfiguration().isInEagerMode());
				actionButtonsLayout.add(bulkOperationsDropdown);
			}
		}
		assigneeFilterLayout.add(actionButtonsLayout);
		// was Alignment.TOP_RIGHT in an expanding slot: the layout grows, its content goes right
		assigneeFilterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, actionButtonsLayout);
		assigneeFilterLayout.setFlexGrow(1, actionButtonsLayout);
		actionButtonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

		return assigneeFilterLayout;
	}

	private void styleGridLayout(VerticalLayout gridLayout) {
		gridLayout.setSpacing(false);
		gridLayout.setSizeFull();
		gridLayout.setFlexGrow(1, grid);
		gridLayout.setClassName("crud-main-layout");
	}

	private void processAssigneeFilterChange(String assignee) {
		if (OFFICER_TASKS.equals(assignee)) {
			criteria.assigneeUser(null);
			criteria.excludeAssigneeUser(FacadeProvider.getUserFacade().getCurrentUserAsReference());
		} else if (MY_TASKS.equals(assignee)) {
			criteria.excludeAssigneeUser(null);
			criteria.assigneeUser(FacadeProvider.getUserFacade().getCurrentUserAsReference());
		} else {
			criteria.excludeAssigneeUser(null);
			criteria.assigneeUser(null);
		}

		tasksView.navigateTo(criteria);
	}

	private Button createAndAddStatusButton(String captionKey, String status, HorizontalLayout filterLayout) {
		Button button = ButtonHelper.createButton(
			captionKey,
			e -> processAssigneeFilterChange(status),
			CssStyles.BUTTON_BORDERLESS,
			CssStyles.BUTTON_FILTER,
			CssStyles.BUTTON_FILTER_LIGHT);
		ComponentUtil.setData(button, STATUS_DATA, status);

		filterLayout.add(button);
		statusButtons.put(button, button.getText());

		return button;
	}

	public void reload(ViewChangeEvent event) {
		String params = event.getParameters().trim();
		if (params.startsWith("?")) {
			params = params.substring(1);
			criteria.fromUrlParams(params);
		}
		updateFilterComponents();
		grid.reload();
	}

	private void updateAssigneeFilterButtons() {
		statusButtons.keySet().forEach(b -> {
			CssStyles.style(b, CssStyles.BUTTON_FILTER_LIGHT);
			b.setText(statusButtons.get(b));
			b.setSuffixComponent(null);
			Object status = ComponentUtil.getData(b, STATUS_DATA);
			if ((OFFICER_TASKS.equals(status) && criteria.getExcludeAssigneeUser() != null)
				|| (MY_TASKS.equals(status) && criteria.getAssigneeUser() != null)
				|| (status == null && criteria.getExcludeAssigneeUser() == null && criteria.getAssigneeUser() == null)) {
				activeStatusButton = b;
			}
		});
		if (activeStatusButton != null) {
			CssStyles.removeStyles(activeStatusButton, CssStyles.BUTTON_FILTER_LIGHT);
			// was an HTML caption with LayoutUtil.spanCss(BADGE, count): the count is a badge after the text
			activeStatusButton.setText(statusButtons.get(activeStatusButton));
			Span badge = new Span(String.valueOf(grid.getDataSize()));
			badge.addClassName(CssStyles.BADGE);
			activeStatusButton.setSuffixComponent(badge);
		}
	}

	public void updateFilterComponents() {
		// TODO replace with Vaadin 8 databinding
		tasksView.setApplyingCriteria(true);

		updateAssigneeFilterButtons();
		if (relevanceStatusFilter != null) {
			relevanceStatusFilter.setValue(criteria.getRelevanceStatus());
		}

		filterForm.setValue(criteria);

		tasksView.setApplyingCriteria(false);
	}

	public TaskGrid getGrid() {
		return grid;
	}

	public MenuBar getBulkOperationsDropdown() {
		return bulkOperationsDropdown;
	}

	public TaskCriteria getCriteria() {
		return criteria;
	}
}
