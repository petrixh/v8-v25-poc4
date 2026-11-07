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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.ValueProvider;

import de.symeda.sormas.ui.utils.FieldAccessCellStyleGenerator;
import de.symeda.sormas.ui.utils.grid.renderers.ValueRenderer;

/**
 * Flow {@code Grid} with the column API SORMAS grids used on Vaadin 8 (and, where it fits, Vaadin 7), so that grid
 * classes mostly change imports:
 * <ul>
 * <li>Columns are {@link Column}{@code <T, V>} with their value type, created by a bean property name (all bean
 * properties for {@link #Grid(Class)}), {@link #addColumn(ValueProvider)} or
 * {@link #addColumn(ValueProvider, ValueRenderer)}; {@link #getColumn(String)}, {@link #getAllColumns()},
 * {@link #removeColumn(String)} and {@link #setColumns(String...)} with Vaadin 8 semantics.</li>
 * <li>Value renderers ({@code utils.grid.renderers}) render the column's value, as Vaadin 8 renderers did.</li>
 * <li>Sorting as in Vaadin 8: every column is sortable unless {@code setSortable(false)}; in memory by the value, with a
 * lazy data provider by the sort properties, which default to the column key (Vaadin 8: the column id).</li>
 * <li>Height by rows ({@link #setHeightByRows(double)}, {@link HeightMode}), row and cell style generators (as part
 * names), description generators (as tooltips).</li>
 * </ul>
 * Not kept: {@code getColumns()} returns Flow columns (use {@link #getAllColumns()}); {@code Column.getId()} is the
 * component id ({@code Optional}; use {@link Column#getKey()}); {@code Column.setId} is void (chain with
 * {@link Column#setKey(String)}).
 */
public class Grid<T> extends com.vaadin.flow.component.grid.Grid<T> {

	private static final long serialVersionUID = 1L;

	/** Vaadin 8 default for {@code getHeightByRows()}. */
	private static final double DEFAULT_HEIGHT_BY_ROWS = 10;

	private HeightMode heightMode = HeightMode.CSS;
	private double heightByRows = DEFAULT_HEIGHT_BY_ROWS;

	public Grid() {
		super();
		initSorting();
	}

	/**
	 * A grid with a column for every property of the bean type, keyed by the property name (Vaadin 8
	 * {@code Grid(Class)}).
	 */
	public Grid(Class<T> beanType) {
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

	/**
	 * Returns the raw column type, so that callers can assign it to {@code Column<T, V>} with their value type as in
	 * Vaadin 8 (Flow's method returns {@code Grid.Column<T>}).
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public Column addColumn(ValueProvider<T, ?> valueProvider) {
		return (Column) super.addColumn(valueProvider);
	}

	/**
	 * Adds a column for the bean property; sortable as in Vaadin 8, whatever the property type.
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public Column addColumn(String propertyName) {
		Column column = (Column) super.addColumn(propertyName);
		column.setSortable(true);
		return column;
	}

	/**
	 * Vaadin 8 {@code addColumn(ValueProvider, Renderer)}.
	 */
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

	/**
	 * @return the column with this key (Vaadin 8: id), or {@code null}
	 */
	@SuppressWarnings("unchecked")
	public Column<T, ?> getColumn(String columnId) {
		return (Column<T, ?>) getColumnByKey(columnId);
	}

	/**
	 * Vaadin 8 {@code getColumns()}: all columns in their order, as {@link Column}s.
	 */
	@SuppressWarnings("unchecked")
	public List<Column<T, ?>> getAllColumns() {
		return getColumns().stream().map(column -> (Column<T, ?>) column).collect(Collectors.toList());
	}

	/**
	 * Vaadin 8 {@code removeColumn(String)}.
	 */
	public void removeColumn(String columnId) {
		removeColumnByKey(columnId);
	}

	/**
	 * Vaadin 8 semantics: the columns with these keys in this order; columns of bean properties are added when missing,
	 * all other columns are removed. (Flow's {@code setColumns} removes every column and adds property columns only.)
	 */
	@Override
	public void setColumns(String... columnIds) {
		GridSupport.setColumns(this, columnIds);
	}

	/**
	 * Vaadin 8 row style generator: the class names become part names of the row.
	 */
	public void setStyleGenerator(SerializableFunction<T, String> styleGenerator) {
		setPartNameGenerator(styleGenerator);
	}

	/**
	 * Vaadin 8 row description generator: shown as the rows' tooltip.
	 */
	public void setDescriptionGenerator(SerializableFunction<T, String> descriptionGenerator) {
		setTooltipGenerator(descriptionGenerator);
	}

	/**
	 * Vaadin 7 {@code setCellStyleGenerator} for the field access styles: applied to the columns the grid has now.
	 */
	public void setCellStyleGenerator(FieldAccessCellStyleGenerator cellStyleGenerator) {
		getAllColumns().forEach(column -> column.setStyleGenerator(cellStyleGenerator.forColumn(column.getKey())));
	}

	/**
	 * Vaadin 8 {@code getSelectionModel().isUserSelectionAllowed()} / Vaadin 7 {@code HasUserSelectionAllowed}.
	 */
	public boolean isUserSelectionAllowed() {
		return getSelectionMode() != SelectionMode.NONE;
	}

	public HeightMode getHeightMode() {
		return heightMode;
	}

	public void setHeightMode(HeightMode heightMode) {
		this.heightMode = heightMode;
		switch (heightMode) {
		case UNDEFINED:
			// Vaadin 8: the rows define the height, a height set before (setSizeFull) no longer counts
			setHeight(null);
			setAllRowsVisible(true);
			break;
		case ROW:
			setAllRowsVisible(false);
			setHeight(GridSupport.heightByRows(heightByRows));
			break;
		case CSS:
		default:
			setAllRowsVisible(false);
		}
	}

	public double getHeightByRows() {
		return heightByRows;
	}

	/**
	 * Vaadin 8: the grid is as high as this many rows plus the header, and its height mode becomes {@link HeightMode#ROW}.
	 */
	public void setHeightByRows(double rows) {
		this.heightByRows = rows;
		setHeightMode(HeightMode.ROW);
	}

	/**
	 * Vaadin 8: as high as its rows ({@link HeightMode#UNDEFINED}).
	 */
	public void setHeightUndefined() {
		setHeightMode(HeightMode.UNDEFINED);
	}

	@Override
	protected void onDataProviderChange() {
		super.onDataProviderChange();
		getAllColumns().forEach(Column::updateSortable);
	}

	/**
	 * A grid column with the Vaadin 8 column API (and the Vaadin 7 names where they differ).
	 *
	 * @param <T>
	 *            the row type
	 * @param <V>
	 *            the type of the column's value
	 */
	public static class Column<T, V> extends com.vaadin.flow.component.grid.Grid.Column<T> {

		private static final long serialVersionUID = 1L;

		private ValueProvider<T, V> valueProvider;
		private ValueRenderer<?> valueRenderer;
		private String caption;
		private boolean sortable = true;
		private boolean sortPropertySet;
		/** Set while Flow's own sort setters run: they call {@link #setSortable(boolean)}, which is the user's switch here. */
		private boolean flowSortUpdate;
		/** Vaadin 8 {@code getWidth()}: a fixed width in pixels, -1 when the width comes from the content. */
		private double fixedWidth = -1;
		/** A fixed width is set ({@link #setWidth(double)} or a CSS width with {@link #setWidth(String)}). */
		private boolean widthFixed;
		/** Vaadin 8 {@code getExpandRatio()}: -1 until set. */
		private int expandRatio = -1;
		private double minimumWidth = -1;
		private double maximumWidth = -1;
		/** The cell part names set by the caller ({@link #setStyleGenerator}), without the width limit part names. */
		private SerializableFunction<T, String> cellPartNameGenerator;
		/** The header and footer part names set by the caller (Vaadin 8 header cell style names). */
		private String headerPartName;
		private String footerPartName;

		/**
		 * Vaadin 8 column sizing: the width comes from the content (header and the rendered rows) and the column takes an
		 * equal share of the free width, unless another column of the grid has an expand ratio
		 * ({@link GridSupport#updateFlexGrow}).
		 */
		public Column(com.vaadin.flow.component.grid.Grid<T> grid, String columnId, Renderer<T> renderer) {
			super(grid, columnId, renderer);
			setAutoWidth(true);
			super.setFlexGrow(GridSupport.hasExpandRatio(grid) ? 0 : 1);
		}

		@SuppressWarnings("unchecked")
		void initValueProvider(ValueProvider<T, ?> valueProvider) {
			this.valueProvider = (ValueProvider<T, V>) valueProvider;
			updateSortable();
		}

		/**
		 * @return the provider of the column's value ({@code null} for component and renderer columns)
		 */
		public ValueProvider<T, V> getValueProvider() {
			return valueProvider;
		}

		/**
		 * The column's identifier (Vaadin 8 {@code setId}); also the default sort property, as in Vaadin 8.
		 */
		@Override
		public Column<T, V> setKey(String key) {
			super.setKey(key);
			if (!sortPropertySet && key != null) {
				flowSortUpdate = true;
				try {
					super.setSortProperty(key);
				} finally {
					flowSortUpdate = false;
				}
			}
			updateSortable();
			return this;
		}

		/**
		 * Vaadin 8 {@code setId}: sets the column key ({@link #setKey(String)}), not a DOM id. Flow's {@code Component.setId}
		 * returns nothing, so chained calls use {@link #setKey(String)}.
		 */
		@Override
		public void setId(String id) {
			setKey(id);
		}

		/**
		 * @deprecated This is the component id ({@code Optional}), not the Vaadin 8 column id: use {@link #getKey()}.
		 *             {@code "x".equals(column.getId())} compiles but is always {@code false}.
		 */
		@Override
		@Deprecated
		public Optional<String> getId() {
			return super.getId();
		}

		/**
		 * Vaadin 7 {@code getPropertyId()}: the column key.
		 */
		public Object getPropertyId() {
			return getKey();
		}

		/**
		 * Vaadin 8 {@code setCaption}: the header text.
		 */
		public Column<T, V> setCaption(String caption) {
			this.caption = caption;
			setHeader(caption != null ? caption : "");
			return this;
		}

		/**
		 * @return the caption set, else the header text (bean property columns: the property name made readable, as in
		 *         Vaadin 8)
		 */
		public String getCaption() {
			return caption != null ? caption : getHeaderText();
		}

		/**
		 * Vaadin 7 name of {@link #setCaption(String)}.
		 */
		public Column<T, V> setHeaderCaption(String caption) {
			return setCaption(caption);
		}

		/**
		 * Vaadin 7 name of {@link #getCaption()}.
		 */
		public String getHeaderCaption() {
			return getCaption();
		}

		public Column<T, V> setHidden(boolean hidden) {
			setVisible(!hidden);
			return this;
		}

		public boolean isHidden() {
			return !isVisible();
		}

		/**
		 * Vaadin 8 {@code setRenderer(Renderer)}: renders the column's value with the value renderer.
		 */
		public Column<T, V> setRenderer(ValueRenderer<? super V> renderer) {
			this.valueRenderer = renderer;
			super.setRenderer(renderer.createRenderer(requireValueProvider()));
			return this;
		}

		/**
		 * Vaadin 8 {@code setRenderer(ValueProvider, Renderer)}: renders the presentation of the column's value.
		 */
		public <P> Column<T, V> setRenderer(ValueProvider<V, P> presentationProvider, ValueRenderer<? super P> renderer) {
			this.valueRenderer = renderer;
			ValueProvider<T, V> columnValueProvider = requireValueProvider();
			super.setRenderer(renderer.createRenderer(item -> presentationProvider.apply(columnValueProvider.apply(item))));
			return this;
		}

		/**
		 * @return the value renderer set with {@link #setRenderer(ValueRenderer)}, {@code null} for the default text (Vaadin
		 *         8: {@code getRenderer()}, which was a {@code TextRenderer} by default)
		 */
		public ValueRenderer<?> getValueRenderer() {
			return valueRenderer;
		}

		private ValueProvider<T, V> requireValueProvider() {
			if (valueProvider == null) {
				throw new IllegalStateException("Column " + getKey() + " has no value provider for a value renderer");
			}
			return valueProvider;
		}

		@Override
		public Column<T, V> setSortable(boolean sortable) {
			if (!flowSortUpdate) {
				this.sortable = sortable;
				updateSortable();
			}
			return this;
		}

		@Override
		public Column<T, V> setSortProperty(String... properties) {
			sortPropertySet = true;
			flowSortUpdate = true;
			try {
				super.setSortProperty(properties);
			} finally {
				flowSortUpdate = false;
			}
			updateSortable();
			return this;
		}

		@Override
		public Column<T, V> setComparator(Comparator<T> comparator) {
			flowSortUpdate = true;
			try {
				super.setComparator(comparator);
			} finally {
				flowSortUpdate = false;
			}
			updateSortable();
			return this;
		}

		/**
		 * Vaadin 8 rule: user-sortable if not switched off, and the data is in memory or the column has sort properties.
		 */
		@SuppressWarnings("rawtypes")
		void updateSortable() {
			DataProvider dataProvider = getGrid() != null ? getGrid().getDataProvider() : null;
			boolean inMemory = dataProvider == null || dataProvider.isInMemory();
			boolean hasSortProperties = sortPropertySet || getKey() != null;
			super.setSortable(sortable && (inMemory || hasSortProperties));
		}

		/**
		 * Vaadin 8 cell style generator: the class names become part names of the cells.
		 */
		public Column<T, V> setStyleGenerator(SerializableFunction<T, String> styleGenerator) {
			setPartNameGenerator(styleGenerator);
			return this;
		}

		/**
		 * The cells' part names: those of the generator plus the column's width limit part names
		 * ({@link #setMinimumWidth(double)}, {@link #setMaximumWidth(double)}).
		 */
		@Override
		public Column<T, V> setPartNameGenerator(SerializableFunction<T, String> partNameGenerator) {
			this.cellPartNameGenerator = Objects.requireNonNull(partNameGenerator, "Part name generator can not be null");
			updatePartNames();
			return this;
		}

		/**
		 * Vaadin 8 cell description generator: shown as the cells' tooltip.
		 */
		public Column<T, V> setDescriptionGenerator(SerializableFunction<T, String> descriptionGenerator) {
			setTooltipGenerator(descriptionGenerator);
			return this;
		}

		/**
		 * Vaadin 8 fixed width in pixels: the column does not grow and its content does not change its width.
		 */
		public Column<T, V> setWidth(double pixels) {
			setWidth(GridSupport.pixels(pixels));
			this.fixedWidth = pixels;
			return this;
		}

		/**
		 * A fixed CSS width, as {@link #setWidth(double)}: the column's content no longer sizes it and it does not grow;
		 * {@code null} goes back to the width from the content.
		 */
		@Override
		public Column<T, V> setWidth(String width) {
			if (width == null) {
				return setWidthUndefined();
			}
			super.setWidth(width);
			this.fixedWidth = -1;
			this.widthFixed = true;
			setAutoWidth(false);
			GridSupport.updateFlexGrow(getGrid());
			return this;
		}

		/**
		 * Vaadin 8 {@code getWidth()}: the fixed width in pixels, -1 when the width comes from the content (or is a CSS
		 * width set with {@link #setWidth(String)}).
		 */
		public double getFixedWidth() {
			return fixedWidth;
		}

		/**
		 * Vaadin 8: width from the content (the default).
		 */
		public Column<T, V> setWidthUndefined() {
			this.fixedWidth = -1;
			this.widthFixed = false;
			setAutoWidth(true);
			GridSupport.updateFlexGrow(getGrid());
			return this;
		}

		/**
		 * Vaadin 8 expand ratio: the column's share of the free width (flex grow). Once a column of the grid has an expand
		 * ratio, the columns without one do not grow (Vaadin 8: the implicit ratio 1 of all columns is dropped). A fixed
		 * width overrides it.
		 */
		public Column<T, V> setExpandRatio(int expandRatio) {
			this.expandRatio = expandRatio;
			GridSupport.updateFlexGrow(getGrid());
			return this;
		}

		/**
		 * Vaadin 8 {@code getExpandRatio()}: -1 when not set.
		 */
		public int getExpandRatio() {
			return expandRatio;
		}

		/**
		 * Flex grow from the Vaadin 8 sizing: 0 for a fixed width, the expand ratio, or 1 for every column when no column
		 * of the grid has one.
		 */
		void applyFlexGrow(boolean gridHasExpandRatio) {
			int flexGrow;
			if (widthFixed) {
				flexGrow = 0;
			} else if (gridHasExpandRatio) {
				flexGrow = Math.max(expandRatio, 0);
			} else {
				flexGrow = 1;
			}
			if (getFlexGrow() != flexGrow) {
				super.setFlexGrow(flexGrow);
			}
		}

		/**
		 * Vaadin 8 minimum width: the column is at least this wide, whatever its content. Applied as the CSS
		 * {@code min-width} of its cells (part name {@code sormas-col-min-<pixels>}); the theme
		 * ({@code sormas.css} "Grids") has a rule for the widths the app uses.
		 */
		public Column<T, V> setMinimumWidth(double pixels) {
			this.minimumWidth = pixels;
			updatePartNames();
			return this;
		}

		public double getMinimumWidth() {
			return minimumWidth;
		}

		/**
		 * Vaadin 8 maximum width: the column neither sizes to its content nor grows beyond this width; longer text is cut
		 * with an ellipsis. Applied as the CSS {@code max-width} of its cells (part name {@code sormas-col-max-<pixels>});
		 * the theme ({@code sormas.css} "Grids") has a rule for the widths the app uses.
		 */
		public Column<T, V> setMaximumWidth(double pixels) {
			this.maximumWidth = pixels;
			updatePartNames();
			return this;
		}

		public double getMaximumWidth() {
			return maximumWidth;
		}

		/**
		 * Puts the width limit part names on the header, footer and body cells, so that all cells of the column have the
		 * same limits.
		 */
		private void updatePartNames() {
			String limits = GridSupport.widthLimitPartNames(minimumWidth, maximumWidth);
			super.setHeaderPartName(GridSupport.joinPartNames(headerPartName, limits));
			super.setFooterPartName(GridSupport.joinPartNames(footerPartName, limits));
			SerializableFunction<T, String> generator = cellPartNameGenerator;
			if (limits == null) {
				super.setPartNameGenerator(generator != null ? generator : item -> null);
			} else if (generator == null) {
				super.setPartNameGenerator(item -> limits);
			} else {
				super.setPartNameGenerator(item -> GridSupport.joinPartNames(generator.apply(item), limits));
			}
		}

		/**
		 * The header cell's part names (Vaadin 8 header cell style name), plus the column's width limit part names.
		 */
		@Override
		public com.vaadin.flow.component.grid.Grid.Column<T> setHeaderPartName(String partName) {
			this.headerPartName = partName;
			updatePartNames();
			return this;
		}

		/**
		 * @return the header part names set with {@link #setHeaderPartName(String)}, without the width limits
		 */
		@Override
		public String getHeaderPartName() {
			return headerPartName;
		}

		/**
		 * The footer cell's part names, plus the column's width limit part names.
		 */
		@Override
		public com.vaadin.flow.component.grid.Grid.Column<T> setFooterPartName(String partName) {
			this.footerPartName = partName;
			updatePartNames();
			return this;
		}

		/**
		 * @return the footer part names set with {@link #setFooterPartName(String)}, without the width limits
		 */
		@Override
		public String getFooterPartName() {
			return footerPartName;
		}

		@Override
		public Column<T, V> setResizable(boolean resizable) {
			super.setResizable(resizable);
			return this;
		}

		/**
		 * Vaadin 8 {@code setEditorBinding}: the binding's field is the column's editor component.
		 */
		public Column<T, V> setEditorBinding(Binder.Binding<T, ?> binding) {
			setEditorComponent((Component) binding.getField());
			return this;
		}
	}
}
