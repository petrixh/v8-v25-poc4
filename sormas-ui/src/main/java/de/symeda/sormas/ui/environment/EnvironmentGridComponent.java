/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2023 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.environment;

import java.util.HashMap;
import java.util.Set;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.caze.InvestigationStatus;
import de.symeda.sormas.api.environment.EnvironmentCriteria;
import de.symeda.sormas.api.environment.EnvironmentIndexDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.RelevanceStatusFilter;
import de.symeda.sormas.ui.utils.ViewConfiguration;

public class EnvironmentGridComponent extends VerticalLayout {

	private static final long serialVersionUID = 8767664326530203161L;
	private static final String STATUS_DATA = "status";
	private final EnvironmentFilterForm filterForm;
	private final EnvironmentGrid grid;

	private HashMap<Button, String> statusButtons;
	private Button activeStatusButton;

	public EnvironmentGridComponent(
		EnvironmentCriteria criteria,
		ViewConfiguration viewConfiguration,
		Runnable filterChangeHandler,
		Runnable filterResetHandler) {
		setSizeFull();
		setSpacing(false);

		grid = new EnvironmentGrid(criteria, viewConfiguration);
		grid.addDataSizeChangeListener(e -> updateStatusButtons(criteria));

		filterForm = new EnvironmentFilterForm();
		filterForm.addResetHandler(e -> filterResetHandler.run());
		filterForm.addApplyHandler(e -> filterChangeHandler.run());

		HorizontalLayout statusFilterBar = buildStatusFilterBar(criteria, filterChangeHandler);

		add(filterForm, statusFilterBar, grid);
		setFlexGrow(1, grid);
	}

	public void reload() {
		grid.reload();
	}

	public void updateFilterComponents(EnvironmentCriteria criteria) {
		filterForm.setValue(criteria);
		updateStatusButtons(criteria);
	}

	private HorizontalLayout buildStatusFilterBar(EnvironmentCriteria criteria, Runnable filterChangeHandler) {
		HorizontalLayout statusFilterBar = new HorizontalLayout();
		statusFilterBar.setPadding(false);
		statusFilterBar.setSpacing(true);
		statusFilterBar.setWidthFull();
		statusFilterBar.addClassName(CssStyles.VSPACE_3);

		HorizontalLayout buttonFilterLayout = new HorizontalLayout();
		// investigation status filter
		statusButtons = new HashMap<>();

		Button statusAll = ButtonHelper.createButton(Captions.all, e -> {
			criteria.investigationStatus(null);
			filterChangeHandler.run();
		}, CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER);

		buttonFilterLayout.add(statusAll);
		statusButtons.put(statusAll, I18nProperties.getCaption(Captions.all));
		activeStatusButton = statusAll;

		for (InvestigationStatus status : InvestigationStatus.values()) {
			Button statusButton = ButtonHelper.createButton(status.toString(), e -> {
				criteria.setInvestigationStatus(status);
				filterChangeHandler.run();
			}, CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER, CssStyles.BUTTON_FILTER_LIGHT);
			ComponentUtil.setData(statusButton, STATUS_DATA, status);

			buttonFilterLayout.add(statusButton);
			statusButtons.put(statusButton, status.toString());
		}

		statusFilterBar.add(buttonFilterLayout);

		HorizontalLayout actionButtonsLayout = new HorizontalLayout();
		actionButtonsLayout.setSpacing(true);
		actionButtonsLayout.add(
			RelevanceStatusFilter.createRelevanceStatusFilter(
				Captions.environmentActiveEnvironments,
				Captions.environmentArchivedEnvironments,
				Captions.environmentAllActiveAndArchivedEnvironments,
				Captions.environmentDeletedEnvironments,
				criteria.getRelevanceStatus(),
				UserRight.ENVIRONMENT_DELETE,
				relevanceStatus -> {
					if (grid.getColumn(grid.DELETE_REASON_COLUMN) != null) {
						grid.getColumn(grid.DELETE_REASON_COLUMN).setHidden(!relevanceStatus.equals(EntityRelevanceStatus.DELETED));
					}
					criteria.setRelevanceStatus(relevanceStatus);
					filterChangeHandler.run();
				}));

		statusFilterBar.add(actionButtonsLayout);
		// was Alignment.TOP_RIGHT in an expanding slot: the layout grows, its content goes right
		statusFilterBar.setVerticalComponentAlignment(FlexComponent.Alignment.START, actionButtonsLayout);
		statusFilterBar.setFlexGrow(1, actionButtonsLayout);
		actionButtonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

		return statusFilterBar;
	}

	private void updateStatusButtons(EnvironmentCriteria criteria) {
		statusButtons.keySet().forEach(b -> {
			CssStyles.style(b, CssStyles.BUTTON_FILTER_LIGHT);
			b.setText(statusButtons.get(b));
			b.setSuffixComponent(null);
			if (ComponentUtil.getData(b, STATUS_DATA) == criteria.getInvestigationStatus()) {
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

	public EnvironmentGrid getGrid() {
		return grid;
	}

	public Set<EnvironmentIndexDto> getSelectedItems() {
		return grid.asMultiSelect().getSelectedItems();
	}
}
