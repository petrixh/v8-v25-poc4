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

package de.symeda.sormas.ui.samples.environmentsample;

import java.util.HashMap;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.environment.environmentsample.EnvironmentSampleCriteria;
import de.symeda.sormas.api.environment.environmentsample.EnvironmentSampleIndexDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.samples.SampleGridComponent;
import de.symeda.sormas.ui.samples.SamplesView;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ComboBoxHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.MenuBarHelper;
import de.symeda.sormas.ui.utils.fields.ComboBox;

public class EnvironmentSampleGridComponent extends SampleGridComponent<EnvironmentSampleIndexDto, EnvironmentSampleCriteria> {

	private static final long serialVersionUID = 4302760792255154403L;

	private static final String STATUS_DATA = "status";

	private static final String NOT_SHIPPED = "notShipped";
	private static final String SHIPPED = "shipped";
	private static final String RECEIVED = "received";

	private final SamplesView samplesView;

	private final EnvironmentSampleGrid grid;

	private final EnvironmentSampleCriteria criteria;

	private HashMap<Button, String> statusButtons;
	private Button activeStatusButton;

	private MenuBar bulkOperationsDropdown;

	private ComboBox relevanceStatusFilter;

	private EnvironmentSampleGridFilterForm filterForm;

	public EnvironmentSampleGridComponent(SamplesView samplesView) {
		setSizeFull();
		setPadding(false);

		this.samplesView = samplesView;

		criteria = ViewModelProviders.of(SamplesView.class).get(EnvironmentSampleCriteria.class);
		if (criteria.getRelevanceStatus() == null) {
			criteria.setRelevanceStatus(EntityRelevanceStatus.ACTIVE);
		}

		grid = new EnvironmentSampleGrid(criteria);
		VerticalLayout gridLayout = new VerticalLayout();
		gridLayout.add(createFilterBar());
		gridLayout.add(createShipmentFilterBar());
		gridLayout.add(grid);
		grid.addDataSizeChangeListener(e -> updateStatusButtons());

		styleGridLayout(gridLayout);
		gridLayout.setPadding(true);

		add(gridLayout);
	}

	@Override
	public EnvironmentSampleGrid getGrid() {
		return grid;
	}

	@Override
	public MenuBar getBulkOperationsDropdown() {
		return bulkOperationsDropdown;
	}

	@Override
	public EnvironmentSampleCriteria getCriteria() {
		return criteria;
	}

	@Override
	public void updateFilterComponents() {
		// TODO replace with Vaadin 8 databinding
		samplesView.setApplyingCriteria(true);

		updateStatusButtons();

		if (relevanceStatusFilter != null) {
			relevanceStatusFilter.setValue(criteria.getRelevanceStatus());
		}

		filterForm.setValue(criteria);

		samplesView.setApplyingCriteria(false);
	}

	public HorizontalLayout createFilterBar() {
		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.setPadding(false);
		filterLayout.setSpacing(true);
		filterLayout.setSizeUndefined();
		filterLayout.addClassName("wrap");

		filterForm = new EnvironmentSampleGridFilterForm();
		filterForm.addValueChangeListener(e -> {
			if (!filterForm.hasFilter()) {
				samplesView.navigateTo(null);
			}
		});
		filterForm.addResetHandler(e -> {
			ViewModelProviders.of(SamplesView.class).remove(EnvironmentSampleCriteria.class);
			samplesView.navigateTo(null, true);
		});
		filterForm.addApplyHandler(e -> grid.reload());
		filterLayout.add(filterForm);

		return filterLayout;
	}

	public HorizontalLayout createShipmentFilterBar() {
		HorizontalLayout shipmentFilterLayout = new HorizontalLayout();
		shipmentFilterLayout.setPadding(false);
		shipmentFilterLayout.setSpacing(true);
		shipmentFilterLayout.setWidth(100, Unit.PERCENTAGE);
		shipmentFilterLayout.addClassName(CssStyles.VSPACE_3);

		statusButtons = new HashMap<>();

		HorizontalLayout buttonFilterLayout = new HorizontalLayout();
		buttonFilterLayout.setSpacing(true);
		{
			Button statusAll =
				ButtonHelper.createButton(Captions.all, e -> processStatusChange(null), CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER);

			buttonFilterLayout.add(statusAll);

			statusButtons.put(statusAll, I18nProperties.getCaption(Captions.all));
			activeStatusButton = statusAll;

			createAndAddStatusButton(Captions.environmentSampleNotShipped, NOT_SHIPPED, buttonFilterLayout);
			createAndAddStatusButton(Captions.environmentSampleShipped, SHIPPED, buttonFilterLayout);
			createAndAddStatusButton(Captions.environmentSampleReceived, RECEIVED, buttonFilterLayout);
		}

		shipmentFilterLayout.add(buttonFilterLayout);

		HorizontalLayout actionButtonsLayout = new HorizontalLayout();
		actionButtonsLayout.setSpacing(true);
		{
			// Show active/archived/all dropdown
			if (UiUtil.permitted(UserRight.ENVIRONMENT_SAMPLE_VIEW)) {
				relevanceStatusFilter = ComboBoxHelper.createComboBoxV7();
				relevanceStatusFilter.setId("relevanceStatusFilter");
				relevanceStatusFilter.setWidth(220, Unit.PIXELS);
				relevanceStatusFilter.setNullSelectionAllowed(false);
				relevanceStatusFilter.addItems((Object[]) EntityRelevanceStatus.values());
				relevanceStatusFilter
					.setItemCaption(EntityRelevanceStatus.ACTIVE, I18nProperties.getCaption(Captions.environmentSampleActiveSamples));
				relevanceStatusFilter
					.setItemCaption(EntityRelevanceStatus.ARCHIVED, I18nProperties.getCaption(Captions.environmentSampleArchivedSamples));
				relevanceStatusFilter.setItemCaption(
					EntityRelevanceStatus.ACTIVE_AND_ARCHIVED,
					I18nProperties.getCaption(Captions.environmentSampleAllActiveAndArchivedSamples));

				if (UiUtil.permitted(UserRight.ENVIRONMENT_SAMPLE_DELETE)) {
					relevanceStatusFilter
						.setItemCaption(EntityRelevanceStatus.DELETED, I18nProperties.getCaption(Captions.environmentSampleDeletedSamples));
				} else {
					relevanceStatusFilter.removeItem(EntityRelevanceStatus.DELETED);
				}

				relevanceStatusFilter.addValueChangeListener(e -> {
					if (grid.getColumn(grid.DELETE_REASON_COLUMN) != null) {
						grid.getColumn(grid.DELETE_REASON_COLUMN).setHidden(!relevanceStatusFilter.getValue().equals(EntityRelevanceStatus.DELETED));
					}

					criteria.setRelevanceStatus((EntityRelevanceStatus) e.getValue());
					samplesView.navigateTo(criteria);
				});
				actionButtonsLayout.add(relevanceStatusFilter);
			}

			// Bulk operation dropdown
			if (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS)) {
				shipmentFilterLayout.setWidth(100, Unit.PERCENTAGE);

				if (criteria.getRelevanceStatus() != EntityRelevanceStatus.DELETED) {
					bulkOperationsDropdown = MenuBarHelper.createDropDown(
						Captions.bulkActions,
						new MenuBarHelper.MenuBarItem(
							I18nProperties.getCaption(Captions.bulkDelete),
							VaadinIcon.TRASH,
							selectedItem -> ControllerProvider.getEnvironmentSampleController()
								.deleteAllSelectedItems(grid.asMultiSelect().getSelectedItems(), grid, () -> samplesView.navigateTo(criteria)),
							UiUtil.permitted(UserRight.ENVIRONMENT_SAMPLE_DELETE)));
				} else {
					bulkOperationsDropdown = MenuBarHelper.createDropDown(
						Captions.bulkActions,
						new MenuBarHelper.MenuBarItem(
							I18nProperties.getCaption(Captions.bulkRestore),
							VaadinIcon.ARROW_BACKWARD,
							selectedItem -> ControllerProvider.getEnvironmentSampleController()
								.restoreSelectedSamples(grid.asMultiSelect().getSelectedItems(), grid, () -> samplesView.navigateTo(criteria)),
							UiUtil.permitted(UserRight.ENVIRONMENT_SAMPLE_DELETE)));
				}

				bulkOperationsDropdown.setVisible(samplesView.getViewConfiguration().isInEagerMode());

				actionButtonsLayout.add(bulkOperationsDropdown);
			}
		}
		shipmentFilterLayout.add(actionButtonsLayout);
		// was Alignment.TOP_RIGHT in an expanding slot: the layout grows, its content goes right
		shipmentFilterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, actionButtonsLayout);
		shipmentFilterLayout.setFlexGrow(1, actionButtonsLayout);
		actionButtonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

		return shipmentFilterLayout;
	}

	private void createAndAddStatusButton(String captionKey, String status, HorizontalLayout filterLayout) {
		Button button = ButtonHelper.createButton(
			captionKey,
			e -> processStatusChange(status),
			CssStyles.BUTTON_BORDERLESS,
			CssStyles.BUTTON_FILTER,
			CssStyles.BUTTON_FILTER_LIGHT);

		ComponentUtil.setData(button, STATUS_DATA, status);

		filterLayout.add(button);

		statusButtons.put(button, button.getText());
	}

	private void processStatusChange(String status) {
		if (NOT_SHIPPED.equals(status)) {
			criteria.setDispatched(false);
			criteria.setReceived(null);
		} else if (SHIPPED.equals(status)) {
			criteria.setDispatched(true);
			criteria.setReceived(null);
		} else if (RECEIVED.equals(status)) {
			criteria.setDispatched(null);
			criteria.setReceived(true);
		} else {
			criteria.setDispatched(null);
			criteria.setReceived(null);
		}

		samplesView.navigateTo(criteria);
	}

	private void updateStatusButtons() {
		statusButtons.keySet().forEach(b -> {
			CssStyles.style(b, CssStyles.BUTTON_FILTER_LIGHT);
			b.setText(statusButtons.get(b));
			b.setSuffixComponent(null);
			if ((NOT_SHIPPED.equals(ComponentUtil.getData(b, STATUS_DATA)) && criteria.getDispatched() == Boolean.FALSE)
				|| (SHIPPED.equals(ComponentUtil.getData(b, STATUS_DATA)) && criteria.getDispatched() == Boolean.TRUE)
				|| (RECEIVED.equals(ComponentUtil.getData(b, STATUS_DATA)) && criteria.getReceived() == Boolean.TRUE)) {
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

	private void styleGridLayout(VerticalLayout gridLayout) {
		gridLayout.setSpacing(false);
		gridLayout.setSizeFull();
		gridLayout.setFlexGrow(1, grid);
		gridLayout.setClassName("crud-main-layout");
	}
}
