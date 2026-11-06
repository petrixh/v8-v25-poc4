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
package de.symeda.sormas.ui.statistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import de.symeda.sormas.ui.utils.fields.OptionGroup;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.statistics.StatisticsCaseAttribute;
import de.symeda.sormas.api.statistics.StatisticsCaseSubAttribute;
import de.symeda.sormas.ui.statistics.StatisticsVisualizationType.StatisticsVisualizationChartType;
import de.symeda.sormas.ui.statistics.StatisticsVisualizationType.StatisticsVisualizationMapType;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;

@SuppressWarnings("serial")
public class StatisticsVisualizationComponent extends HorizontalLayout {

	private StatisticsVisualizationType visualizationType;
	private StatisticsVisualizationMapType visualizationMapType;
	private StatisticsVisualizationChartType visualizationChartType;

	private final OptionGroup visualizationSelect;
	private final OptionGroup visualizationMapSelect;
	private final OptionGroup visualizationChartSelect;
	private StatisticsVisualizationElement rowsElement;
	private StatisticsVisualizationElement columnsElement;
	private Button switchRowsAndColumnsButton;
	private final List<Consumer<StatisticsVisualizationType>> visualizationTypeChangedListeners =
		new ArrayList<Consumer<StatisticsVisualizationType>>();

	public StatisticsVisualizationComponent() {
		setSpacing(true);
		setWidth(100, Unit.PERCENTAGE);

		visualizationSelect =
			new OptionGroup(I18nProperties.getCaption(Captions.statisticsVisualizationType), Arrays.asList(StatisticsVisualizationType.values()));
		visualizationSelect.setId(Captions.statisticsVisualizationType);
		visualizationSelect.addValueChangeListener(event -> {
			visualizationType = (StatisticsVisualizationType) event.getValue();
			updateComponentVisibility();
			if (rowsElement != null) {
				rowsElement.setType(rowsElement.getType(), visualizationType);
			}
			if (columnsElement != null) {
				columnsElement.setType(columnsElement.getType(), visualizationType);
			}

			for (Consumer<StatisticsVisualizationType> visualizationTypeChangedListener : visualizationTypeChangedListeners) {
				visualizationTypeChangedListener.accept(visualizationType);
			}
		});
		CssStyles.style(visualizationSelect, CssStyles.VSPACE_NONE, SormasFieldGroupFieldFactory.OPTIONGROUP_HORIZONTAL);
		visualizationSelect.setNullSelectionAllowed(false);
		add(visualizationSelect);
		setFlexGrow(0, visualizationSelect);

		visualizationMapSelect =
			new OptionGroup(I18nProperties.getCaption(Captions.statisticsMapType), Arrays.asList(StatisticsVisualizationMapType.values()));
		visualizationMapSelect.setId(Captions.statisticsMapType);
		visualizationMapSelect.addValueChangeListener(event -> {
			visualizationMapType = (StatisticsVisualizationMapType) event.getValue();
		});
		CssStyles.style(visualizationMapSelect, CssStyles.VSPACE_NONE, SormasFieldGroupFieldFactory.OPTIONGROUP_HORIZONTAL);
		visualizationMapSelect.setNullSelectionAllowed(false);
		add(visualizationMapSelect);
		setFlexGrow(0, visualizationSelect);

		visualizationChartSelect =
			new OptionGroup(I18nProperties.getCaption(Captions.statisticsChartType), Arrays.asList(StatisticsVisualizationChartType.values()));
		visualizationChartSelect.setId(Captions.statisticsChartType);
		visualizationChartSelect.addValueChangeListener(event -> {
			visualizationChartType = (StatisticsVisualizationChartType) event.getValue();
			updateComponentVisibility();
		});
		CssStyles.style(visualizationChartSelect, CssStyles.VSPACE_NONE, SormasFieldGroupFieldFactory.OPTIONGROUP_HORIZONTAL);
		visualizationChartSelect.setNullSelectionAllowed(false);
		add(visualizationChartSelect);
		setFlexGrow(0, visualizationChartSelect);

		rowsElement = new StatisticsVisualizationElement(StatisticsVisualizationElementType.ROWS, visualizationType);
		add(rowsElement);
		setFlexGrow(0, rowsElement);

		switchRowsAndColumnsButton = ButtonHelper.createIconButtonWithCaption("switchRowsAndColumns", null, VaadinIcon.EXCHANGE, event -> {
			StatisticsVisualizationElement newRowsElement = columnsElement;
			newRowsElement.setType(StatisticsVisualizationElementType.ROWS, visualizationType);
			StatisticsVisualizationElement newColumnsElement = rowsElement;
			newColumnsElement.setType(StatisticsVisualizationElementType.COLUMNS, visualizationType);
			remove(rowsElement);
			remove(columnsElement);
			addComponentAtIndex(indexOf(switchRowsAndColumnsButton), newRowsElement);
			addComponentAtIndex(indexOf(switchRowsAndColumnsButton) + 1, newColumnsElement);
			// both are children: Flow's replace swaps them, as Vaadin 8's replaceComponent did
			replace(rowsElement, newRowsElement);
			replace(columnsElement, newColumnsElement);
			rowsElement = newRowsElement;
			columnsElement = newColumnsElement;
		}, CssStyles.FORCE_CAPTION);
		switchRowsAndColumnsButton.setTooltipText(I18nProperties.getCaption(Captions.statisticsExchange));

		add(switchRowsAndColumnsButton);
		setFlexGrow(0, switchRowsAndColumnsButton);

		columnsElement = new StatisticsVisualizationElement(StatisticsVisualizationElementType.COLUMNS, visualizationType);
		add(columnsElement);
		setFlexGrow(0, columnsElement);

		Span spacer = new Span();
		add(spacer);
		setFlexGrow(1, spacer);

		visualizationSelect.setValue(StatisticsVisualizationType.TABLE);
		visualizationChartSelect.setValue(StatisticsVisualizationChartType.STACKED_COLUMN);
		visualizationMapSelect.setValue(StatisticsVisualizationMapType.REGIONS);
	}

	private void updateComponentVisibility() {
		visualizationMapSelect.setVisible(visualizationType == StatisticsVisualizationType.MAP);
		visualizationChartSelect.setVisible(visualizationType == StatisticsVisualizationType.CHART);

		rowsElement.setVisible(visualizationType == StatisticsVisualizationType.TABLE || visualizationType == StatisticsVisualizationType.CHART);

		switchRowsAndColumnsButton.setVisible(
			visualizationType == StatisticsVisualizationType.TABLE
				|| (visualizationType == StatisticsVisualizationType.CHART && visualizationChartType != StatisticsVisualizationChartType.PIE));

		columnsElement.setVisible(
			visualizationType == StatisticsVisualizationType.TABLE
				|| (visualizationType == StatisticsVisualizationType.CHART && visualizationChartType != StatisticsVisualizationChartType.PIE));
	}

	public StatisticsCaseAttribute getRowsAttribute() {
		switch (visualizationType) {
		case MAP:
			return StatisticsCaseAttribute.JURISDICTION;
		default:
			break;
		}
		return rowsElement.getAttribute();
	}

	public StatisticsCaseSubAttribute getRowsSubAttribute() {
		switch (visualizationType) {
		case MAP:
			switch (visualizationMapType) {
			case REGIONS:
				return StatisticsCaseSubAttribute.REGION;
			case DISTRICTS:
				return StatisticsCaseSubAttribute.DISTRICT;
			default:
				throw new IllegalArgumentException(visualizationMapType.toString());
			}
		default:
			break;
		}
		return rowsElement.getSubAttribute();
	}

	public StatisticsCaseAttribute getColumnsAttribute() {
		switch (visualizationType) {
		case MAP:
			return null;
		case CHART:
			switch (visualizationChartType) {
			case PIE:
				return null;
			//$CASES-OMITTED$
			default:
				break;
			}
			break;
		//$CASES-OMITTED$
		default:
			break;
		}
		return columnsElement.getAttribute();
	}

	public StatisticsCaseSubAttribute getColumnsSubAttribute() {
		switch (visualizationType) {
		case MAP:
			return null;
		case CHART:
			switch (visualizationChartType) {
			case PIE:
				return null;
			//$CASES-OMITTED$
			default:
				break;
			}
			break;
		//$CASES-OMITTED$
		default:
			break;
		}
		return columnsElement.getSubAttribute();
	}

	public StatisticsVisualizationType getVisualizationType() {
		return visualizationType;
	}

	public StatisticsVisualizationMapType getVisualizationMapType() {
		return visualizationMapType;
	}

	public StatisticsVisualizationChartType getVisualizationChartType() {
		return visualizationChartType;
	}

	public boolean hasRegionGrouping() {
		switch (visualizationType) {
		case TABLE:
		case CHART:
			return rowsElement.getSubAttribute() == StatisticsCaseSubAttribute.REGION
				|| columnsElement.getSubAttribute() == StatisticsCaseSubAttribute.REGION;
		case MAP:
			return visualizationMapType == StatisticsVisualizationMapType.REGIONS;
		default:
			throw new IllegalArgumentException(visualizationType.toString());
		}
	}

	public boolean hasDistrictGrouping() {
		switch (visualizationType) {
		case TABLE:
		case CHART:
			return rowsElement.getSubAttribute() == StatisticsCaseSubAttribute.DISTRICT
				|| columnsElement.getSubAttribute() == StatisticsCaseSubAttribute.DISTRICT;
		case MAP:
			return visualizationMapType == StatisticsVisualizationMapType.DISTRICTS;
		default:
			throw new IllegalArgumentException(visualizationType.toString());
		}
	}

	public boolean hasCommunityGrouping() {
		switch (visualizationType) {
		case TABLE:
		case CHART:
			return rowsElement.getSubAttribute() == StatisticsCaseSubAttribute.COMMUNITY
				|| columnsElement.getSubAttribute() == StatisticsCaseSubAttribute.COMMUNITY;
		//TODO: Community Grouping on this Visualisationtype may be implemented later
		case MAP:
			return false;
		default:
			throw new IllegalArgumentException(visualizationType.toString());
		}
	}

	public boolean hasIncidenceIncompatibleGrouping() {
		return rowsElement.getSubAttribute() == StatisticsCaseSubAttribute.FACILITY
			|| columnsElement.getSubAttribute() == StatisticsCaseSubAttribute.FACILITY;
	}

	public boolean hasSexGrouping() {
		switch (visualizationType) {
		case TABLE:
		case CHART:
			return rowsElement.getAttribute() == StatisticsCaseAttribute.SEX || columnsElement.getAttribute() == StatisticsCaseAttribute.SEX;
		case MAP:
			return false;
		default:
			throw new IllegalArgumentException(visualizationType.toString());
		}
	}

	public boolean hasAgeGroupGroupingWithPopulationData() {
		switch (visualizationType) {
		case TABLE:
		case CHART:
			return rowsElement.getAttribute() == StatisticsCaseAttribute.AGE_INTERVAL_5_YEARS
				|| columnsElement.getAttribute() == StatisticsCaseAttribute.AGE_INTERVAL_5_YEARS;
		case MAP:
			return false;
		default:
			throw new IllegalArgumentException(visualizationType.toString());
		}
	}

	public boolean hasAgeGroupGroupingWithoutPopulationData() {
		switch (visualizationType) {
		case TABLE:
		case CHART:
			return (rowsElement.getAttribute() != null
				&& rowsElement.getAttribute().isAgeGroup()
				&& rowsElement.getAttribute() != StatisticsCaseAttribute.AGE_INTERVAL_5_YEARS)
				|| (columnsElement.getAttribute() != null
					&& columnsElement.getAttribute().isAgeGroup()
					&& columnsElement.getAttribute() != StatisticsCaseAttribute.AGE_INTERVAL_5_YEARS);
		case MAP:
			return false;
		default:
			throw new IllegalArgumentException(visualizationType.toString());
		}
	}

	public boolean hasPopulationGrouping() {
		return hasRegionGrouping() || hasDistrictGrouping() || hasSexGrouping() || hasAgeGroupGroupingWithPopulationData();
	}

	public void setStackedColumnAndPieEnabled(boolean enabled) {
		visualizationChartSelect.setItemEnabled(StatisticsVisualizationChartType.STACKED_COLUMN, enabled);
		visualizationChartSelect.setItemEnabled(StatisticsVisualizationChartType.PIE, enabled);

		if (!enabled
			&& (StatisticsVisualizationChartType.STACKED_COLUMN == visualizationChartSelect.getValue()
				|| StatisticsVisualizationChartType.PIE == visualizationChartSelect.getValue())) {
			visualizationChartSelect.setValue(StatisticsVisualizationChartType.COLUMN);
		}
	}

	public void addVisualizationTypeChangedListener(Consumer<StatisticsVisualizationType> visualizationTypeChangedListener) {
		visualizationTypeChangedListeners.add(visualizationTypeChangedListener);
	}

	public void removeVisualizationTypeChangedListener(Consumer<StatisticsVisualizationType> visualizationTypeChangedListener) {
		visualizationTypeChangedListeners.remove(visualizationTypeChangedListener);
	}
}
