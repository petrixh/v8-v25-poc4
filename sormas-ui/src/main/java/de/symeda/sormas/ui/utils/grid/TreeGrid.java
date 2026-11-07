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

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.ValueProvider;

import de.symeda.sormas.ui.utils.grid.renderers.ValueRenderer;

/**
 * Flow {@code TreeGrid} with the Vaadin 8 column API of {@link Grid} (see there). Vaadin 8 used the first column as the
 * hierarchy column by default; in Flow the hierarchy column is added with {@code addHierarchyColumn} or
 * {@link #addComponentHierarchyColumn(ValueProvider)}.
 */
public class TreeGrid<T> extends com.vaadin.flow.component.treegrid.TreeGrid<T> {

	private static final long serialVersionUID = 1L;

	public TreeGrid() {
		super();
		initSorting();
	}

	public TreeGrid(Class<T> beanType) {
		super(beanType);
		initSorting();
	}

	/**
	 * Vaadin 7/8: a click on a header sorts by that column, a shift-click adds it as a further sort column.
	 */
	private void initSorting() {
		setMultiSort(true, MultiSortPriority.APPEND, true);
	}

	@Override
	protected Column<T, ?> createColumn(Renderer<T> renderer, String columnId) {
		return new Column<>(this, columnId, renderer);
	}

	@Override
	protected <C extends com.vaadin.flow.component.grid.Grid.Column<T>> C addColumn(
		ValueProvider<T, ?> valueProvider,
		BiFunction<Renderer<T>, String, C> columnFactory) {
		C column = super.addColumn(valueProvider, columnFactory);
		GridSupport.initValueColumn(column, valueProvider);
		return column;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Column addColumn(ValueProvider<T, ?> valueProvider) {
		return (Column) super.addColumn(valueProvider);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Column addColumn(String propertyName) {
		Column column = (Column) super.addColumn(propertyName);
		column.setSortable(true);
		return column;
	}

	@SuppressWarnings("unchecked")
	public <V> Column<T, V> addColumn(ValueProvider<T, V> valueProvider, ValueRenderer<? super V> renderer) {
		Column<T, V> column = addColumn(valueProvider);
		column.setRenderer(renderer);
		return column;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <V extends Component> Column<T, V> addComponentColumn(ValueProvider<T, V> componentProvider) {
		return (Column<T, V>) super.addComponentColumn(componentProvider);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <V extends Component> Column<T, V> addComponentHierarchyColumn(ValueProvider<T, V> componentProvider) {
		return (Column<T, V>) super.addComponentHierarchyColumn(componentProvider);
	}

	@SuppressWarnings("unchecked")
	public Column<T, ?> getColumn(String columnId) {
		return (Column<T, ?>) getColumnByKey(columnId);
	}

	@SuppressWarnings("unchecked")
	public List<Column<T, ?>> getAllColumns() {
		return getColumns().stream().map(column -> (Column<T, ?>) column).collect(Collectors.toList());
	}

	public void removeColumn(String columnId) {
		removeColumnByKey(columnId);
	}

	@Override
	public void setColumns(String... columnIds) {
		GridSupport.setColumns(this, columnIds);
	}

	/**
	 * Vaadin 8 row style generator, as {@link Grid#setStyleGenerator(SerializableFunction)}.
	 */
	public void setStyleGenerator(SerializableFunction<T, String> styleGenerator) {
		setPartNameGenerator(item -> GridSupport.rowPartNames(styleGenerator.apply(item)));
	}

	@Override
	protected void onDataProviderChange() {
		super.onDataProviderChange();
		getAllColumns().forEach(Column::updateSortable);
	}

	/**
	 * {@link Grid.Column} of a tree grid.
	 */
	public static class Column<T, V> extends Grid.Column<T, V> {

		private static final long serialVersionUID = 1L;

		public Column(com.vaadin.flow.component.grid.Grid<T> grid, String columnId, Renderer<T> renderer) {
			super(grid, columnId, renderer);
		}
	}
}
