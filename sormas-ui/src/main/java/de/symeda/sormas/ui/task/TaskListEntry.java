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

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.ui.Alignment;
import com.vaadin.flow.component.button.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.task.TaskDto;
import de.symeda.sormas.api.task.TaskIndexDto;
import de.symeda.sormas.api.task.TaskPriority;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentField;

@SuppressWarnings("serial")
public class TaskListEntry extends SideComponentField {

	private final TaskIndexDto task;
	private Button editButton;

	public TaskListEntry(TaskIndexDto task) {

		this.task = task;

		HorizontalLayout topLayout = new HorizontalLayout();
		topLayout.setPadding(false);
		topLayout.setSpacing(false);
		topLayout.setWidthFull();
		addComponentToField(topLayout);

		// TOP LEFT
		VerticalLayout topLeftLayout = new VerticalLayout();

		topLeftLayout.setPadding(false);
		topLeftLayout.setSpacing(false);

		Span taskTypeLabel = new Span(DataHelper.toStringNullable(task.getTaskType()));
		CssStyles.style(taskTypeLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
		taskTypeLabel.setWidthFull();
		topLeftLayout.add(taskTypeLabel);

		Span statusLabel = new Span(DataHelper.toStringNullable(task.getTaskStatus()));
		CssStyles.style(statusLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
		topLeftLayout.add(statusLabel);

		Span priorityLabel = new Span(
			DataHelper.toStringNullable(I18nProperties.getPrefixCaption(TaskDto.I18N_PREFIX, TaskDto.PRIORITY) + ": " + task.getPriority()));
		if (TaskPriority.HIGH == task.getPriority()) {
			priorityLabel.addClassName(CssStyles.LABEL_IMPORTANT);
		} else if (TaskPriority.NORMAL == task.getPriority()) {
			priorityLabel.addClassName(CssStyles.LABEL_NEUTRAL);
		}
		topLeftLayout.add(priorityLabel);

		Span suggestedStartLabel = new Span(
			I18nProperties.getPrefixCaption(TaskDto.I18N_PREFIX, TaskDto.SUGGESTED_START) + ": "
				+ DateFormatHelper.formatDate(task.getSuggestedStart()));
		topLeftLayout.add(suggestedStartLabel);

		Span dueDateLabel =
			new Span(I18nProperties.getPrefixCaption(TaskDto.I18N_PREFIX, TaskDto.DUE_DATE) + ": " + DateFormatHelper.formatDate(task.getDueDate()));
		topLeftLayout.add(dueDateLabel);

		topLayout.add(topLeftLayout);

		// TOP RIGHT
		VerticalLayout topRightLayout = new VerticalLayout();
		topLayout.setComponentAlignment(topLeftLayout, Alignment.TOP_LEFT);

		topRightLayout.addClassName(CssStyles.ALIGN_RIGHT);
		/*
		 * topRightLayout.setPadding(false);
		 * topRightLayout.setSpacing(false);
		 */

		Span userLabel =
			new Span(I18nProperties.getPrefixCaption(TaskDto.I18N_PREFIX, TaskDto.ASSIGNEE_USER) + ": " + task.getAssigneeUser().getCaption());
		topLeftLayout.add(userLabel);

		topLayout.add(topRightLayout);
		topLayout.setComponentAlignment(topRightLayout, Alignment.TOP_RIGHT);

		String statusStyle;
		switch (task.getTaskStatus()) {
		case DONE:
			statusStyle = CssStyles.LABEL_DONE;
			break;
		case NOT_EXECUTABLE:
			statusStyle = CssStyles.LABEL_NOT;
			break;
		case REMOVED:
			statusStyle = CssStyles.LABEL_DISCARDED;
			break;
		default:
			statusStyle = null;
			break;
		}

		if (statusStyle != null) {
			taskTypeLabel.addClassName(statusStyle);
			suggestedStartLabel.addClassName(statusStyle);
			dueDateLabel.addClassName(statusStyle);
			statusLabel.addClassName(statusStyle);
			priorityLabel.addClassName(statusStyle);
			userLabel.addClassName(statusStyle);
		}
	}

	public void addEditListener(int rowIndex, ClickListener editClickListener) {
		if (editButton == null) {
			editButton = ButtonHelper.createIconButtonWithCaption(
				"edit-task-" + rowIndex,
				null,
				VaadinIcon.PENCIL,
				null,
				CssStyles.BUTTON_LINK,
				CssStyles.BUTTON_COMPACT);

			add(editButton);
			setComponentAlignment(editButton, Alignment.TOP_RIGHT);
			setFlexGrow(0, editButton);
		}

		editButton.addClickListener(editClickListener);
	}

	public TaskIndexDto getTask() {
		return task;
	}
}
