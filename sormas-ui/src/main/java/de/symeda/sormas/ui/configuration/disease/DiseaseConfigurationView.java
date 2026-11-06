/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2024 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.configuration.disease;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Unit;
import de.symeda.sormas.ui.navigation.ViewChangeListener;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.ActiveRelevanceStatus;
import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.customizableenum.CustomizableEnumValueIndexDto;
import de.symeda.sormas.api.disease.DiseaseConfigurationCriteria;
import de.symeda.sormas.api.disease.DiseaseConfigurationFilterReportingType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.configuration.AbstractConfigurationView;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.RowCount;

public class DiseaseConfigurationView extends AbstractConfigurationView {

	private static final long serialVersionUID = 5057458066864240318L;

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/diseaseconfiguration";

	// Filters
	private ComboBox<DiseaseConfigurationFilterReportingType> reportingTypeFilter;
	private ComboBox<Disease> diseaseFilter;
	private ComboBox<ActiveRelevanceStatus> relevanceStatusFilter;

	private final DiseaseConfigurationCriteria criteria;
	private final DiseaseConfigurationGrid grid;
	private RowCount rowCount;

	public DiseaseConfigurationView() {

		super(VIEW_NAME);

		criteria = ViewModelProviders.of(DiseaseConfigurationView.class).get(DiseaseConfigurationCriteria.class, new DiseaseConfigurationCriteria());
		grid = new DiseaseConfigurationGrid(criteria);
		VerticalLayout gridLayout = new VerticalLayout();
		gridLayout.add(createFilterBar());

		rowCount = new RowCount(Strings.labelNumberOfDiseaseConfigurations, grid.getDataSize());
		rowCount.addClassName(CssStyles.VSPACE_NONE);
		grid.addDataSizeChangeListener(e -> rowCount.update(grid.getDataSize()));

		setUpRelevanceStatusFilter();

		HorizontalLayout relevanceStatusLayout = new HorizontalLayout();
		relevanceStatusLayout.setPadding(false);
		relevanceStatusLayout.add(rowCount);
		relevanceStatusLayout.add(relevanceStatusFilter);
		relevanceStatusLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

		gridLayout.add(relevanceStatusLayout);
		gridLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, relevanceStatusLayout);
		gridLayout.add(grid);
		gridLayout.setPadding(true);
		gridLayout.setSpacing(false);
		gridLayout.setFlexGrow(1, grid);
		gridLayout.setSizeFull();
		gridLayout.setClassName("crud-main-layout");

		addComponent(gridLayout);
	}

	private HorizontalLayout createFilterBar() {

		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.setPadding(false);
		filterLayout.setSpacing(true);

		reportingTypeFilter = new ComboBox<>(
			I18nProperties.getCaption(Captions.diseaseConfigurationReportingTypeFilter),
			Arrays.asList(DiseaseConfigurationFilterReportingType.values()));
		reportingTypeFilter.addValueChangeListener(e -> {
			criteria.reportingType(e.getValue());
			grid.reload();
		});
		filterLayout.add(reportingTypeFilter);

		diseaseFilter = new ComboBox<>(
			I18nProperties.getPrefixCaption(CustomizableEnumValueIndexDto.I18N_PREFIX, CustomizableEnumValueIndexDto.DISEASES),
			Arrays.stream(Disease.values()).sorted(Comparator.comparing(Disease::toString)).collect(Collectors.toList()));
		diseaseFilter.addValueChangeListener(e -> {
			criteria.disease(e.getValue());
			grid.reload();
		});
		filterLayout.add(diseaseFilter);

		filterLayout.add(ButtonHelper.createButton(Captions.actionResetFilters, event -> {
			ViewModelProviders.of(DiseaseConfigurationView.class).remove(DiseaseConfigurationCriteria.class);
			navigateTo(null);
		}, CssStyles.FORCE_CAPTION));

		return filterLayout;
	}

	private void setUpRelevanceStatusFilter() {

		relevanceStatusFilter = new ComboBox<>();
		relevanceStatusFilter.setId("relevanceStatus");
		relevanceStatusFilter.setWidth(210, Unit.PIXELS);
		relevanceStatusFilter.setItems(ActiveRelevanceStatus.values());
		relevanceStatusFilter.setItemLabelGenerator(ActiveRelevanceStatus::toString);
		relevanceStatusFilter.addValueChangeListener(e -> {
			if (e.isFromClient() && e.getValue() == null) {
				// was setEmptySelectionAllowed(false): a Flow combo box has no empty item, but the user can clear it;
				// the previous value comes back without a new navigation
				applyingCriteria = true;
				relevanceStatusFilter.setValue(e.getOldValue());
				applyingCriteria = false;
				return;
			}
			criteria.relevanceStatus(e.getValue());
			navigateTo(criteria);
		});
	}

	@Override
	public void enter(ViewChangeListener.ViewChangeEvent event) {

		super.enter(event);
		String params = event.getParameters().trim();
		if (params.startsWith("?")) {
			params = params.substring(1);
			criteria.fromUrlParams(params);
		}
		updateFilterComponents();
		grid.reload();
	}

	public void updateFilterComponents() {

		applyingCriteria = true;

		reportingTypeFilter.setValue(criteria.getReportingType());
		diseaseFilter.setValue(criteria.getDisease());

		if (relevanceStatusFilter != null) {
			relevanceStatusFilter.setValue(criteria.getRelevanceStatus());
		}

		applyingCriteria = false;
	}
}
