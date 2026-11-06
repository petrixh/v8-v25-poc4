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

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import de.symeda.sormas.ui.utils.PopupButton;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.task.TaskContext;
import de.symeda.sormas.api.task.TaskCriteria;
import de.symeda.sormas.api.task.TaskIndexDto;
import de.symeda.sormas.api.task.TaskStatus;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.utils.AbstractView;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.ExportEntityName;
import de.symeda.sormas.ui.utils.GridExportStreamResource;
import de.symeda.sormas.ui.utils.TaskDownloadUtil;
import de.symeda.sormas.ui.utils.ViewConfiguration;

@SuppressWarnings("serial")
public class TasksView extends AbstractView {

	public static final String VIEW_NAME = "tasks";

	private final TaskGridComponent taskListComponent;
	private ViewConfiguration viewConfiguration;

	public TasksView() {

		super(VIEW_NAME);

		if (!ViewModelProviders.of(TasksView.class).has(TaskCriteria.class)) {
			// init default filter
			TaskCriteria taskCriteria = new TaskCriteria();
			taskCriteria.taskStatus(TaskStatus.PENDING);
			ViewModelProviders.of(TasksView.class).get(TaskCriteria.class, taskCriteria);
		}

		viewConfiguration = ViewModelProviders.of(getClass()).get(ViewConfiguration.class);
		taskListComponent = new TaskGridComponent(getViewTitleLabel(), this);
		addComponent(taskListComponent);

		if (UiUtil.permitted(UserRight.TASK_EXPORT)) {
			VerticalLayout exportLayout = new VerticalLayout();
			exportLayout.setSpacing(true);
			exportLayout.setPadding(true);
			exportLayout.addClassName(CssStyles.LAYOUT_MINIMAL);
			exportLayout.setWidth(200, Unit.PIXELS);

			PopupButton exportButton = ButtonHelper.createIconPopupButton(Captions.export, VaadinIcon.DOWNLOAD, exportLayout);
			addHeaderComponent(exportButton);

			Button basicExportButton = ButtonHelper.createIconButton(Captions.exportBasic, VaadinIcon.TABLE, null, CssStyles.BUTTON_PRIMARY);
			basicExportButton.setTooltipText(I18nProperties.getString(Strings.infoBasicExport));
			basicExportButton.setWidthFull();
			exportLayout.add(basicExportButton);
			DownloadHandler streamResource = GridExportStreamResource.createStreamResourceWithSelectedItems(
				taskListComponent.getGrid(),
				() -> viewConfiguration.isInEagerMode() ? taskListComponent.getGrid().asMultiSelect().getSelectedItems() : Collections.emptySet(),
				ExportEntityName.TASKS,
				TaskGrid.ACTION_BTN_ID);
			DownloadUtil.attachDownloader(basicExportButton, streamResource);

			DownloadHandler extendedExportStreamResource =
				TaskDownloadUtil.createTaskExportResource(taskListComponent.getGrid().getCriteria(), this::getSelectedRowUuids, null);
			addExportButton(
				extendedExportStreamResource,
				exportButton.getPopover(),
				exportLayout,
				VaadinIcon.FILE_TEXT,
				Captions.exportDetailed,
				Strings.infoDetailedExport);

			Button btnCustomExport = ButtonHelper.createIconButton(Captions.exportCustom, VaadinIcon.FILE_TEXT, e -> {
				ControllerProvider.getCustomExportController().openTaskExportWindow(taskListComponent.getCriteria(), this::getSelectedRowUuids);
				exportButton.setPopupVisible(false);
			}, CssStyles.BUTTON_PRIMARY);
			btnCustomExport.setTooltipText(I18nProperties.getString(Strings.infoCustomExport));
			btnCustomExport.setWidthFull();
			exportLayout.add(btnCustomExport);
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
				taskListComponent.getBulkOperationsDropdown().setVisible(true);
				ViewModelProviders.of(TasksView.class).get(ViewConfiguration.class).setInEagerMode(true);
				btnEnterBulkEditMode.setVisible(false);
				btnLeaveBulkEditMode.setVisible(true);
				taskListComponent.getGrid().reload();
			});
			btnLeaveBulkEditMode.addClickListener(e -> {
				taskListComponent.getBulkOperationsDropdown().setVisible(false);
				ViewModelProviders.of(TasksView.class).get(ViewConfiguration.class).setInEagerMode(false);
				btnLeaveBulkEditMode.setVisible(false);
				btnEnterBulkEditMode.setVisible(true);
				navigateTo(taskListComponent.getCriteria());
			});
		}

		if (UiUtil.permitted(UserRight.TASK_CREATE)) {
			Button createButton = ButtonHelper.createIconButton(
				Captions.taskNewTask,
				VaadinIcon.PLUS_CIRCLE,
				e -> ControllerProvider.getTaskController().create(TaskContext.GENERAL, null, null, taskListComponent.getGrid()::reload),
				CssStyles.BUTTON_PRIMARY);

			addHeaderComponent(createButton);
		}
	}

	private Set<String> getSelectedRowUuids() {
		return viewConfiguration.isInEagerMode()
			? taskListComponent.getGrid().asMultiSelect().getSelectedItems().stream().map(TaskIndexDto::getUuid).collect(Collectors.toSet())
			: Collections.emptySet();
	}

	@Override
	public void enter(ViewChangeEvent event) {
		taskListComponent.reload(event);
	}

	public ViewConfiguration getViewConfiguration() {
		return viewConfiguration;
	}
}
