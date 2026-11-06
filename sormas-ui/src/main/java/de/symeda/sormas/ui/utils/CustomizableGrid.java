/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2022 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.utils;

import java.util.List;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.utils.grid.Grid;

/**
 * Was a Vaadin 7 grid; now a Flow grid of the bean type, with the columns filtered by the country's field visibility.
 */
public class CustomizableGrid<T> extends Grid<T> {

	public CustomizableGrid(Class<T> beanType) {
		super(beanType);
	}

	public void setColumns(Class<?> clazz, List<String> propertyIds) {

		FieldVisibilityCheckers fieldVisibilityCheckers = FieldVisibilityCheckers.withCountry(FacadeProvider.getConfigFacade().getCountryLocale());
		super.setColumns(propertyIds.stream().filter(propertyId -> fieldVisibilityCheckers.isVisible(clazz, propertyId)).toArray(String[]::new));
	}

	public void setColumns(List<CustomizableGridColumn> columns) {

		FieldVisibilityCheckers fieldVisibilityCheckers = FieldVisibilityCheckers.withCountry(FacadeProvider.getConfigFacade().getCountryLocale());
		super.setColumns(
			columns.stream()
				.filter(column -> fieldVisibilityCheckers.isVisible(column.getClazz(), column.getPropertyId()))
				// Vaadin 7 code passed the column objects as property ids (unused method); the ids are meant
				.map(CustomizableGridColumn::getPropertyId)
				.toArray(String[]::new));
	}

	@Override
	public void setColumns(String... propertyIds) {
		throw new UnsupportedOperationException("This method should not be used! Please use setColumns of CustomizableGrid!");
	}

	public static class CustomizableGridColumn {

		private Class<?> clazz;
		private String propertyId;

		public CustomizableGridColumn(Class<?> clazz, String propertyId) {
			this.clazz = clazz;
			this.propertyId = propertyId;
		}

		public Class<?> getClazz() {
			return clazz;
		}

		public String getPropertyId() {
			return propertyId;
		}
	}
}
