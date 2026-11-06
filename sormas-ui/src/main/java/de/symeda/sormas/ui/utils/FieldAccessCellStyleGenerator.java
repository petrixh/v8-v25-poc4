/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

import java.util.function.Function;

import com.vaadin.flow.function.SerializableFunction;

import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;

/**
 * Marks cells of inaccessible columns. Flow grids style cells per column: use {@link #forColumn(Object)} as the column's
 * part name generator (V7 grids and tables called {@link #getStyle(Object)} per cell).
 */
public class FieldAccessCellStyleGenerator implements java.io.Serializable {

	private static final long serialVersionUID = -1105771966243065908L;

	public static FieldAccessCellStyleGenerator withFieldAccessCheckers(Class<?> beanType, UiFieldAccessCheckers fieldAccessCheckers) {
		return new FieldAccessCellStyleGenerator(
			(columnId) -> fieldAccessCheckers.isEmbedded(beanType, columnId)
				? fieldAccessCheckers.hasRight()
				: fieldAccessCheckers.isAccessible(beanType, columnId));
	}

	private final Function<String, Boolean> accessCheck;

	public FieldAccessCellStyleGenerator(Function<String, Boolean> accessCheck) {
		this.accessCheck = accessCheck;
	}

	public <T> SerializableFunction<T, String> forColumn(Object propertyId) {
		return item -> getStyle(propertyId);
	}

	public String getStyle(Object propertyId) {
		if (propertyId != null && !accessCheck.apply(propertyId.toString())) {
			return CssStyles.INACCESSIBLE_COLUMN;
		}

		return "";
	}
}
