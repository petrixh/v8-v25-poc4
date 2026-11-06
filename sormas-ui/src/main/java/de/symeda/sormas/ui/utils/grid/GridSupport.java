/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2026 SORMAS Foundation gGmbH
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
package de.symeda.sormas.ui.utils.grid;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.function.ValueProvider;

import de.symeda.sormas.ui.utils.GridExportStreamResource;

/**
 * Code shared by {@link Grid} and {@link TreeGrid}, which extend different Flow classes.
 */
final class GridSupport {

	private GridSupport() {
	}

	/**
	 * Called for every column created with a value provider: keeps the provider on the column (for value renderers) and
	 * registers it for the export, which read each column's value provider in Vaadin 8.
	 */
	@SuppressWarnings({
		"unchecked",
		"rawtypes" })
	static <T> void initValueColumn(Column<T> column, ValueProvider<T, ?> valueProvider) {
		if (column instanceof Grid.Column) {
			((Grid.Column) column).initValueProvider(valueProvider);
		}
		GridExportStreamResource.setExportValueProvider(column, valueProvider);
	}

	/**
	 * Vaadin 8 {@code setColumns(String...)}: keeps the columns with these keys in this order, adds bean property columns
	 * for keys that have no column yet and removes all other columns.
	 */
	static <T> void setColumns(com.vaadin.flow.component.grid.Grid<T> grid, String... columnIds) {
		List<Column<T>> ordered = new ArrayList<>(columnIds.length);
		for (String columnId : columnIds) {
			Column<T> column = grid.getColumnByKey(columnId);
			if (column == null) {
				column = grid.addColumn(columnId);
			}
			ordered.add(column);
		}
		for (Column<T> column : new ArrayList<>(grid.getColumns())) {
			if (!ordered.contains(column)) {
				grid.removeColumn(column);
			}
		}
		grid.setColumnOrder(ordered);
	}

	static String pixels(double pixels) {
		return BigDecimal.valueOf(pixels).stripTrailingZeros().toPlainString() + "px";
	}

	/**
	 * Height of a grid showing a header row and {@code rows} body rows (Vaadin 8 {@code HeightMode.ROW}), from the
	 * theme's row height.
	 */
	static String heightByRows(double rows) {
		return "calc(" + BigDecimal.valueOf(rows + 1).stripTrailingZeros().toPlainString() + " * (var(--lumo-size-m, 36px) + 1px) + 2px)";
	}
}
