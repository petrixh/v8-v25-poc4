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
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Unit;
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
public class StatisticsFilterComponent extends VerticalLayout {

	private static final String SPECIFY_YOUR_SELECTION = I18nProperties.getCaption(Captions.statisticsSpecifySelection);

	private StatisticsCaseAttribute selectedAttribute;
	private StatisticsCaseSubAttribute selectedSubAttribute;
	private StatisticsFilterElement filterElement;

	public StatisticsFilterComponent(int rowIndex) {
		setSpacing(true);
		setPadding(false);

		addClassName(CssStyles.LAYOUT_MINIMAL);
		setWidth(100, Unit.PERCENTAGE);

		add(createFilterAttributeElement(rowIndex));
	}

	private HorizontalLayout createFilterAttributeElement(int rowIndex) {
		HorizontalLayout filterAttributeLayout = new HorizontalLayout();
		filterAttributeLayout.setSpacing(true);
		filterAttributeLayout.setWidth(100, Unit.PERCENTAGE);

		MenuBar filterAttributeMenuBar = new MenuBar();
		filterAttributeMenuBar.setId(Captions.statisticsAttribute + "-" + rowIndex);
		VerticalLayout filterAttributeDropdown =
			withCaption(new Span(I18nProperties.getCaption(Captions.statisticsAttribute)), filterAttributeMenuBar);
		MenuItem filterAttributeItem = filterAttributeMenuBar.addItem(I18nProperties.getCaption(Captions.statisticsAttributeSelect));
		MenuBar filterSubAttributeMenuBar = new MenuBar();
		filterSubAttributeMenuBar.setId(Captions.statisticsAttributeSpecification + "-" + rowIndex);
		VerticalLayout filterSubAttributeDropdown =
			withCaption(new Span(I18nProperties.getCaption(Captions.statisticsAttributeSpecification)), filterSubAttributeMenuBar);
		MenuItem filterSubAttributeItem = filterSubAttributeMenuBar.addItem(SPECIFY_YOUR_SELECTION);

		// Add attribute groups
		for (StatisticsCaseAttributeGroup attributeGroup : StatisticsCaseAttributeGroup.values()) {
			MenuItem attributeGroupItem = filterAttributeItem.getSubMenu().addItem(attributeGroup.toString());
			attributeGroupItem.setEnabled(false);

			// Add attributes belonging to the current group
			for (StatisticsCaseAttribute attribute : attributeGroup.getAttributes()) {
				ComponentEventListener<ClickEvent<MenuItem>> attributeCommand = event -> {
					MenuItem selectedItem = event.getSource();
					selectedAttribute = attribute;
					selectedSubAttribute = null;
					filterAttributeItem.setText(attribute.toString());

					// Add style to keep chosen item selected and remove it from all other items
					for (MenuItem menuItem : filterAttributeItem.getSubMenu().getItems()) {
						setSelectedStyle(menuItem, false);
					}
					setSelectedStyle(selectedItem, true);

					// Reset the sub attribute dropdown
					filterSubAttributeItem.getSubMenu().removeAll();
					filterSubAttributeItem.setText(SPECIFY_YOUR_SELECTION);

					if (attribute.getSubAttributes().length > 0) {
						for (StatisticsCaseSubAttribute subAttribute : attribute.getSubAttributes()) {
							if (subAttribute.isUsedForFilters()) {
								ComponentEventListener<ClickEvent<MenuItem>> subAttributeCommand = subEvent -> {
									MenuItem selectedSubItem = subEvent.getSource();
									selectedSubAttribute = subAttribute;
									filterSubAttributeItem.setText(subAttribute.toString());

									// Add style to keep chosen item selected and remove it from all other items
									for (MenuItem menuItem : filterSubAttributeItem.getSubMenu().getItems()) {
										setSelectedStyle(menuItem, false);
									}
									setSelectedStyle(selectedSubItem, true);

									updateFilterElement(rowIndex);
								};

								filterSubAttributeItem.getSubMenu().addItem(subAttribute.toString(), subAttributeCommand);
							}
						}

						// Only add the sub attribute dropdown if there are any sub attributes that are relevant for the filters section
						if (!filterSubAttributeItem.getSubMenu().getItems().isEmpty()) {
							filterAttributeLayout.add(filterSubAttributeDropdown);
							filterAttributeLayout.setFlexGrow(1, filterSubAttributeDropdown);
						} else {
							filterAttributeLayout.remove(filterSubAttributeDropdown);
						}
					} else {
						filterAttributeLayout.remove(filterSubAttributeDropdown);
					}
					updateFilterElement(rowIndex);
				};

				filterAttributeItem.getSubMenu().addItem(attribute.toString(), attributeCommand);
			}
		}

		filterAttributeLayout.add(filterAttributeDropdown);
		filterAttributeLayout.setFlexGrow(0, filterAttributeDropdown);
		return filterAttributeLayout;
	}

	/**
	 * Was {@code MenuBar.setCaption}: a Flow menu bar has no caption, so the caption is shown above it.
	 */
	static VerticalLayout withCaption(Span caption, Component component) {
		VerticalLayout captionedLayout = new VerticalLayout(caption, component);
		captionedLayout.setPadding(false);
		captionedLayout.setSpacing(false);
		captionedLayout.setWidth(null);
		return captionedLayout;
	}

	/**
	 * Was {@code MenuItem.setStyleName("selected-filter")} / {@code setStyleName(null)}: marks the chosen item.
	 */
	static void setSelectedStyle(MenuItem menuItem, boolean selected) {
		menuItem.getElement().getClassList().set("selected-filter", selected);
	}

	private void updateFilterElement(int rowIndex) {
		if (filterElement != null) {
			remove(filterElement);
			filterElement = null;
		}

		if (selectedSubAttribute == StatisticsCaseSubAttribute.DATE_RANGE) {
			filterElement = new StatisticsFilterDateRangeElement(rowIndex);
		} else if (selectedAttribute == StatisticsCaseAttribute.JURISDICTION) {
			filterElement = new StatisticsFilterJurisdictionElement(rowIndex);
		} else if (selectedAttribute == StatisticsCaseAttribute.PLACE_OF_RESIDENCE) {
			filterElement = new StatisticsFilterResidenceElement(rowIndex);
		} else if (selectedAttribute.getSubAttributes().length == 0 || selectedSubAttribute != null) {
			filterElement = new StatisticsFilterValuesElement(
				selectedAttribute.toString() + (selectedSubAttribute != null ? " (" + selectedSubAttribute.toString() + ")" : ""),
				selectedAttribute,
				selectedSubAttribute,
				rowIndex);
		}

		if (filterElement != null) {
			add(filterElement);
		}
	}

	public StatisticsCaseAttribute getSelectedAttribute() {
		return selectedAttribute;
	}

	public StatisticsCaseSubAttribute getSelectedSubAttribute() {
		return selectedSubAttribute;
	}

	public StatisticsFilterElement getFilterElement() {
		return filterElement;
	}
}
