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

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.statistics.StatisticsCaseAttribute;
import de.symeda.sormas.api.statistics.StatisticsCaseAttributeGroup;
import de.symeda.sormas.api.statistics.StatisticsCaseSubAttribute;
import de.symeda.sormas.ui.utils.CssStyles;

@SuppressWarnings("serial")
public class StatisticsVisualizationElement extends HorizontalLayout {

	/** The attribute menu with its caption above it (was {@code MenuBar.setCaption}). */
	private VerticalLayout displayedAttributeDropdown;
	private Span displayedAttributeCaption;
	private VerticalLayout displayedSubAttributeDropdown;
	private MenuItem displayedAttributeItem;
	private MenuItem emptySelectionItem;
	private MenuItem displayedSubAttributeItem;

	private StatisticsVisualizationElementType type;
	private StatisticsVisualizationType visualizationType;
	private StatisticsCaseAttribute attribute;
	private StatisticsCaseSubAttribute subAttribute;

	public StatisticsVisualizationElement(StatisticsVisualizationElementType type, StatisticsVisualizationType visualizationType) {
		this.type = type;
		this.visualizationType = visualizationType;

		CssStyles.style(this, CssStyles.LAYOUT_MINIMAL);
		setSpacing(true);
		setWidth(null);

		createAndAddComponents();
	}

	private void createAndAddComponents() {
		MenuBar displayedAttributeMenuBar = new MenuBar();
		displayedAttributeMenuBar.setId("visualizationType");
		displayedAttributeCaption = new Span(type.toString(visualizationType));
		displayedAttributeDropdown = StatisticsFilterComponent.withCaption(displayedAttributeCaption, displayedAttributeMenuBar);
		displayedAttributeItem = displayedAttributeMenuBar.addItem(type.getEmptySelectionString(visualizationType));

		MenuBar displayedSubAttributeMenuBar = new MenuBar();
		displayedSubAttributeMenuBar.setId("displayedSubAttribute");
		// was CssStyles.FORCE_CAPTION (an empty caption keeps the menu in line with the captioned one)
		displayedSubAttributeDropdown = StatisticsFilterComponent.withCaption(new Span(), displayedSubAttributeMenuBar);
		CssStyles.style(displayedSubAttributeDropdown, CssStyles.FORCE_CAPTION);
		displayedSubAttributeItem = displayedSubAttributeMenuBar.addItem(I18nProperties.getCaption(Captions.statisticsSpecifySelection));

		// Empty selections
		ComponentEventListener<ClickEvent<MenuItem>> emptyItemCommand = event -> {
			attribute = null;
			subAttribute = null;
			resetSubAttributeDropdown();
			displayedAttributeItem.setText(type.getEmptySelectionString(visualizationType));
			removeSelections(displayedAttributeItem);
		};
		emptySelectionItem = displayedAttributeItem.getSubMenu().addItem(type.getEmptySelectionString(visualizationType), emptyItemCommand);

		// Add attribute groups
		for (StatisticsCaseAttributeGroup attributeGroup : StatisticsCaseAttributeGroup.values()) {
			MenuItem attributeGroupItem = displayedAttributeItem.getSubMenu().addItem(attributeGroup.toString());
			attributeGroupItem.setEnabled(false);

			// Add attributes belonging to the current group
			for (StatisticsCaseAttribute attribute : attributeGroup.getAttributes()) {
				if (attribute.isUsedForVisualisation()) {
					ComponentEventListener<ClickEvent<MenuItem>> attributeCommand = event -> {
						MenuItem selectedItem = event.getSource();
						resetSubAttributeDropdown();
						this.attribute = attribute;
						this.subAttribute = null;
						displayedAttributeItem.setText(attribute.toString());
						removeSelections(displayedAttributeItem);
						StatisticsFilterComponent.setSelectedStyle(selectedItem, true);

						// Build sub attribute dropdown
						if (attribute.getSubAttributes().length > 0) {
							for (StatisticsCaseSubAttribute subAttribute : attribute.getSubAttributes()) {
								if (subAttribute.isUsedForGrouping()) {
									ComponentEventListener<ClickEvent<MenuItem>> subAttributeCommand = subEvent -> {
										MenuItem selectedSubItem = subEvent.getSource();
										this.subAttribute = subAttribute;
										displayedSubAttributeItem.setText(subAttribute.toString());
										removeSelections(displayedSubAttributeItem);
										StatisticsFilterComponent.setSelectedStyle(selectedSubItem, true);
									};

									displayedSubAttributeItem.getSubMenu().addItem(subAttribute.toString(), subAttributeCommand);
								}
							}

							add(displayedSubAttributeDropdown);
						}
					};

					displayedAttributeItem.getSubMenu().addItem(attribute.toString(), attributeCommand);
				}
			}
		}

		add(displayedAttributeDropdown);
	}

	private void removeSelections(MenuItem parentItem) {
		for (MenuItem childItem : parentItem.getSubMenu().getItems()) {
			StatisticsFilterComponent.setSelectedStyle(childItem, false);
		}
	}

	private void resetSubAttributeDropdown() {
		displayedSubAttributeItem.getSubMenu().removeAll();
		displayedSubAttributeItem.setText(I18nProperties.getCaption(Captions.statisticsSpecifySelection));
		remove(displayedSubAttributeDropdown);
	}

	public StatisticsCaseAttribute getAttribute() {
		return attribute;
	}

	public StatisticsCaseSubAttribute getSubAttribute() {
		return subAttribute;
	}

	public StatisticsVisualizationElementType getType() {
		return type;
	}

	public void setType(StatisticsVisualizationElementType type, StatisticsVisualizationType visualizationType) {
		this.type = type;
		this.visualizationType = visualizationType;
		displayedAttributeCaption.setText(type.toString(visualizationType));
		emptySelectionItem.setText(type.getEmptySelectionString(visualizationType));
		if (attribute == null && subAttribute == null) {
			displayedAttributeItem.setText(type.getEmptySelectionString(visualizationType));
		}
	}
}
