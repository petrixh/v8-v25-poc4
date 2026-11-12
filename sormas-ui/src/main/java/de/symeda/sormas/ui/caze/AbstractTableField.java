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
package de.symeda.sormas.ui.caze;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.CloneFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.BeanPropertySet;
import com.vaadin.flow.data.binder.PropertyDefinition;
import com.vaadin.flow.data.binder.PropertySet;
import com.vaadin.flow.data.provider.ListDataProvider;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.ui.utils.Buffered;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.FieldAccessCellStyleGenerator;
import de.symeda.sormas.ui.utils.GridExportStreamResource;
import de.symeda.sormas.ui.utils.fields.CustomField;
import de.symeda.sormas.ui.utils.grid.Grid;

/**
 * TODO replace table with grid?
 * TODO whole component seems to need improvement (e.g. should use setInternalValue instead of setValue)
 * Does probably not make sense, because of future update to Vaadin 8
 *
 * @author Martin Wahnschaffe
 */
@SuppressWarnings({
	"serial",
	"rawtypes" })
public abstract class AbstractTableField<E> extends CustomField<Collection> implements Buffered {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String ACTION_COLUMN_ID = "actionColumn";

	protected static final String SCROLLBAR_FIX_CSS = "scrollbarFix";

	private int maxTablePageLength = 10;

	public int getMaxTablePageLength() {
		return maxTablePageLength;
	}

	public void setMaxTablePageLength(int maxTablePageLength) {
		this.maxTablePageLength = maxTablePageLength;
	}

	private VerticalLayout layout;
	private Span captionLabel;
	private Button addButton;
	private Table table;

	/**
	 * The value read from the bound property (Vaadin 7: {@code dataSource.getValue()}). The table shows clones of its
	 * entries, so the source is changed only by {@link #commit()}.
	 */
	private Collection<E> dataSource;
	private BeanItemContainer<E> container;

	private boolean isEditAllowed;

	protected UiFieldAccessCheckers fieldAccessCheckers;

	public AbstractTableField(UiFieldAccessCheckers fieldAccessCheckers) {
		this.fieldAccessCheckers = fieldAccessCheckers;
		this.isEditAllowed = true;

		getContent();
	}

	public AbstractTableField(UiFieldAccessCheckers fieldAccessCheckers, boolean isEditAllowed) {
		this.fieldAccessCheckers = fieldAccessCheckers;
		this.isEditAllowed = isEditAllowed;

		getContent();
	}

	/**
	 * If there are different rows in the table
	 * and do not need to be scrolled vertically (<= 10 entries)
	 * (and expandRatio is used?), A horizontal scroll bar appears.
	 * This can be corrected in CSS as follows:
	 * 
	 * <pre>
	 * .v-table-scrollbarFix .v-table-body-wrapper,
	 * .v-table-scrollbarFix .v-table-body-wrapper:focus {
	 * 		overflow-y: hidden;
	 * }
	 * </pre>
	 */
	protected void applyScrollbarFix() {

		if (table.getPageLength() == 0) {
			table.addClassName(SCROLLBAR_FIX_CSS);
		} else {
			table.removeClassName(SCROLLBAR_FIX_CSS);
		}
	}

	protected void applyTablePageLength() {

		if (getTable().size() <= getMaxTablePageLength()) {
			table.setPageLength(0);
		} else {
			table.setPageLength(getMaxTablePageLength());
		}
		applyScrollbarFix();
	}

	@Override
	protected Component initContent() {

		this.addStyleName(CssStyles.CAPTION_HIDDEN);
		this.addStyleName(CssStyles.VSPACE_2);

		layout = new VerticalLayout();
		layout.setSpacing(false);
		layout.setPadding(false);

		HorizontalLayout headerLayout = new HorizontalLayout();
		{
			headerLayout.setWidthFull();
			headerLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
			headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

			captionLabel = new Span(getCaption());
			captionLabel.setSizeUndefined();
			headerLayout.add(captionLabel);

			addButton = createAddButton();
			headerLayout.add(addButton);
		}
		layout.add(headerLayout);

		table = createTable();
		table.setItemSetChangeListener(this::applyTablePageLength);
		layout.add(table);

		return layout;
	}

	/**
	 * The caption is shown in the field's own header (Vaadin 7 hid the field caption), not as the Flow field label.
	 */
	@Override
	public void setCaption(String caption) {

		if (captionLabel != null) {
			captionLabel.setText(caption);
		}
	}

	@Override
	public String getCaption() {
		return captionLabel != null ? captionLabel.getText() : null;
	}

	@Override
	public Class<Collection> getType() {
		return Collection.class;
	}

	public abstract Class<E> getEntryType();

	protected Table createTable() {

		final Table table = new Table(getEntryType());

		table.setSizeFull();

		createActionColumn(table);

		return table;
	}

	protected void createActionColumn(Table table) {

		Table.ColumnGenerator editColumnGenerator = new Table.ColumnGenerator() {

			@Override
			public Object generateCell(Table source, Object itemId, Object columnId) {
				return generateEditCell(itemId);
			}
		};
		table.addGeneratedColumn(ACTION_COLUMN_ID, editColumnGenerator);
		// was setColumnWidth(ACTION_COLUMN_ID, 20): Vaadin 7 set the content width, a Flow width includes the cell padding,
		// so a 20 px column cut off the button
		table.setColumnWidthToContent(ACTION_COLUMN_ID);
		table.setColumnHeader(ACTION_COLUMN_ID, "");

		// As in Vaadin 7: a double click on a row, or a click in the action column next to the button, opens the entry. A
		// Flow grid does not report a click on a focusable element in a cell (the button) as an item click, so the button
		// opens the entry through its own listener (see generateEditCell), once.
		table.addItemClickListener(event -> {
			if (event.getClickCount() > 1 || (event.getColumn() != null && ACTION_COLUMN_ID.equals(event.getColumn().getKey()))) {
				@SuppressWarnings("unchecked")
				final E entry = (E) event.getItem();
				if (entry != null) {
					editEntry(entry, false, result -> onEntryChanged(result));
				}
			}
		});
	}

	@SuppressWarnings("unchecked")
	protected Object generateEditCell(Object itemId) {

		return ButtonHelper.createIconButtonWithCaption(itemId + "-edit", null, isEditAllowed ? VaadinIcon.EDIT : VaadinIcon.EYE, e -> {
			editEntry((E) itemId, false, this::onEntryChanged);
		}, CssStyles.BUTTON_BORDERLESS);
	}

	protected Button createAddButton() {
		return ButtonHelper.createButton(Captions.actionNewEntry, event -> addEntry(), CssStyles.BUTTON_LINK);
	}

	/**
	 * @see #createEntry()
	 * @see #editEntry(Object)
	 */
	protected void addEntry() {

		final E entry = createEntry();

		editEntry(entry, true, new Consumer<E>() {

			@Override
			public void accept(E result) {
				table.addItem(result);
				fireValueChange(false);
			}
		});
	}

	protected abstract void editEntry(E entry, boolean create, Consumer<E> commitCallback);

	/**
	 * Override in order to do custom sort
	 */
	protected void onEntryChanged(E entry) {

		getTable().refreshRowCache();
		fireValueChange(false);
	}

	/**
	 * Set visible columns and their behavior. If entries are to be deleted, the "delete" column must be visible.
	 */
	protected abstract void updateColumns();

	protected E createEntry() {

		// TODO good way to do it like this?
		try {
			return getEntryType().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Copy of an entry. All editing is then done within this copy. At commit, this entry replaces the old one.
	 *
	 * @param sourceEntry
	 *            original
	 * @return
	 */
	protected E cloneEntry(E sourceEntry) {

		if (sourceEntry == null) {
			return null;
		}

		E clone = ObjectUtils.clone(sourceEntry);
		if (clone == null) {
			throw new CloneFailedException("Entry is not Cloneable");
		}
		return clone;
	}

	/**
	 * Delete entry (e.g., called by the user).
	 */
	public void removeEntry(E entry) {

		// gewünschten Eintrag löschen
		getTable().removeItem(entry);

		fireValueChange(false);
	}

	/**
	 * Check whether the entry is empty, so that such entries can be automatically removed from the list.
	 */
	protected abstract boolean isEmpty(E entry);

	/**
	 * Check whether the entry has changed in the past. Required for Field.isModified ().
	 */
	protected abstract boolean isModified(E oldEntry, E newEntry);

	/**
	 * Specifies the possibility to change the entry before it is commited.
	 *
	 * @return May the entry be commited?
	 */
	protected boolean preEntryCommit(E entry) {
		// don't commit empty entries
		return !isEmpty(entry);
	}

	/**
	 * @return Unchanged original entry from the data source
	 */
	protected E getUnmodifiedEntry(E entry) {
		Collection<E> oldEntries = dataSource;
		for (E oldEntry : oldEntries) {
			if (oldEntry.equals(entry)) {
				return oldEntry;
			}
		}
		return null;
	}

	/**
	 * Was {@code setPropertyDataSource(Property)}: the form's binder sets the property's value here when it reads the
	 * bean. The value serves as a source for the items in the table.
	 * A copy is created of all entries in the edit, so that the source data is not overwritten until the commit.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setValue(Collection newDataSource) {

		Collection<E> entries = newDataSource;
		if (entries == null) {
			throw new IllegalArgumentException("dataSource cannot be null");
		}

		dataSource = entries;

		Collection<E> clonedEntries;
		if (entries instanceof List) {
			clonedEntries = new ArrayList<>(entries.size());
		} else if (entries instanceof Set) {
			clonedEntries = new HashSet<>(entries.size());
		} else {
			throw new IllegalArgumentException("dataSource value must be List or Set: " + entries.getClass());
		}

		// Make a copy of all entries so that they can be freely edited
		// important: all fields must be placed on writeThrough!
		for (E entry : entries) {
			clonedEntries.add(cloneEntry(entry));
		}

		container = new BeanItemContainer<>(getEntryType(), clonedEntries);

		getContent();
		Table tbl = getTable();
		// keeps the visible columns
		tbl.setContainerDataSource(container);
		applyTablePageLength();

		updateColumns();
		initInaccessiblePlaceHolders();

		fireValueChange(false);
	}

	protected void initInaccessiblePlaceHolders() {
		for (Object columnId : table.getVisibleColumns()) {
			if (!isAccessible(columnId)) {
				if (table.getColumnGenerator(columnId) != null) {
					table.removeGeneratedColumn(columnId);
				}
				table.addGeneratedColumn(columnId, (source, itemId, columnId1) -> I18nProperties.getCaption(Captions.inaccessibleValue));
			}
		}
		table.setCellStyleGenerator(new FieldAccessCellStyleGenerator(e -> isAccessible(e)));
	}

	protected boolean isAccessible(Object columnId) {
		return fieldAccessCheckers.isAccessible(getEntryType(), columnId.toString());
	}

	/**
	 * Was {@code getPropertyDataSource().getValue()}: the value read from the bound property, or {@code null} before
	 * the first read.
	 */
	public Collection<E> getPropertyDataSource() {
		return dataSource;
	}

	@Override
	public boolean isModified() {

		if (dataSource == null) {
			return false;
		}

		ArrayList<E> oldEntries = new ArrayList<>(dataSource);
		ArrayList<E> newEntries = new ArrayList<>(getValue());

		// go through "new entries": there is in each case an equal old?!
		Iterator<E> iterator = newEntries.iterator();
		int newEntriesCount = 0;
		while (iterator.hasNext()) {
			E newEntry = iterator.next();
			// ignore empty entries
			if (isEmpty(newEntry)) {
				continue;
			}
			// search for entry
			int oldIndex = oldEntries.indexOf(newEntry);
			if (oldIndex == -1) {
				// no entry found -> new -> modified
				return true;
			}
			E oldEntry = oldEntries.get(oldIndex);
			if (isModified(oldEntry, newEntry)) {
				// entry modified -> modified
				return true;
			}
			newEntriesCount++;
		}

		// less entries than before? -> modified
		if (newEntriesCount < oldEntries.size()) {
			return true;
		}

		return false;
	}

	/**
	 * Auxiliary method for comparing two objects. Uses equals for comparing.
	 */
	public static boolean isModifiedObject(Object oldObject, Object newObject) {

		if (oldObject == newObject) {
			return false;
		}
		if (oldObject == null || newObject == null) {
			return true;
		}
		if (!oldObject.equals(newObject)) {
			return true;
		}
		return false;
	}

	/**
	 * Auxiliary method for comparing two collections. Used for equals () to compare.
	 */
	public static boolean isModifiedCollection(Collection<?> oldCollection, Collection<?> newCollection) {

		if (oldCollection.size() != newCollection.size()) {
			return true;
		}

		// iterate through all entries in order
		Iterator<?> oldIterator = oldCollection.iterator();
		Iterator<?> newIterator = newCollection.iterator();

		while (newIterator.hasNext()) {
			Object oldObject = oldIterator.next();
			Object newObject = newIterator.next();

			// Eintrag anders?
			if (isModifiedObject(oldObject, newObject)) {
				return true;
			}
		}

		boolean hasNext = !oldIterator.hasNext();
		assert hasNext; // iteration should also be finished

		return false;
	}

	/**
	 * Called by the form's field group before it writes the bean; the field group then writes {@link #getValue()} to
	 * the bound property. Entries that must not be committed ({@link #preEntryCommit(Object)}) are removed from the
	 * table first, so they are not written (Vaadin 7 left them in the table and left them out of the property).
	 */
	@Override
	public void commit() throws SourceException, de.symeda.sormas.ui.utils.InvalidValueException {

		// write new entries to data source (a read-only property is not committed by the field group)
		if (dataSource != null) {
			if ((isInvalidCommitted() || isValid())) {

				// creata a copy
				Collection<E> entries = getValue();
				Collection<E> entriesCopy;
				if (dataSource instanceof List) {
					entriesCopy = new ArrayList<>(entries);
				} else {
					// Set
					entriesCopy = new LinkedHashSet<>(entries);
				}

				// remove unwanted entries
				for (E entry : entries) {
					if (!preEntryCommit(entry)) {
						entriesCopy.remove(entry);
						getTable().removeItem(entry);
					}
				}

				// done
				dataSource = entriesCopy;

				fireValueChange(false);

			} else {
				/* An invalid value and we don't allow them, throw the exception */
				validateValue();
			}
		}

		// super.commit();
	}

	@Override
	public void discard() throws SourceException {

		// just reread the data source
		if (dataSource != null) {
			setValue(dataSource);
		}

		// super.discard();
	}

	/**
	 * Vaadin 7 {@code isValid()}: required check and validators.
	 */
	@Override
	public boolean isValid() {
		try {
			validateValue();
			return true;
		} catch (de.symeda.sormas.ui.utils.InvalidValueException e) {
			return false;
		}
	}

	/**
	 * Always buffered (Vaadin 7: the field kept its own copy of the entries).
	 */
	@Override
	public void setBuffered(boolean buffered) {
		// always buffered
	}

	@Override
	public boolean isBuffered() {
		return true;
	}

	/**
	 * All entries of the table, in a collection of the source's type (a list or a set).
	 */
	@Override
	public Collection<E> getValue() {

		BeanItemContainer<E> container = getContainer();
		if (container == null) {
			return null;
		}

		// return all entries from the container
		Collection<E> entries = container.getItemIds();
		if (dataSource instanceof Set) {
			return new LinkedHashSet<>(entries);
		}
		return new ArrayList<>(entries);
	}

	@Override
	public boolean isEmpty() {
		Collection<E> entries = getValue();
		if (entries == null) {
			return true;
		}
		for (E entry : entries) {
			if (!isEmpty(entry)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Was {@code fireValueChange(boolean)}: notifies the value change listeners (the form's binder included). The old
	 * value is not known, as in Vaadin 7.
	 */
	protected void fireValueChange(boolean repaintIsNotNeeded) {
		fireEvent(new ComponentValueChangeEvent<>(this, this, null, false));
	}

	/**
	 * @since Vaadin 7.4
	 *        Workaround, because in AbstractField.clear () calls setValue (null).
	 */
	@Override
	public void clear() {
		BeanItemContainer<E> container = getContainer();
		if (container != null) {
			container.removeAllItems();
		}
	}

	protected VerticalLayout getLayout() {
		return layout;
	}

	protected Table getTable() {
		return table;
	}

	protected Button getAddButton() {
		return addButton;
	}

	protected BeanItemContainer<E> getContainer() {
		return container;
	}

	protected void setLayoutSpacing(boolean value) {
		getLayout().setSpacing(value);
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		super.setReadOnly(readOnly);
		getAddButton().setVisible(!readOnly);
		if (readOnly) {
			getTable().removeContainerProperty(ACTION_COLUMN_ID);
		}
	}

	/**
	 * Was {@code com.vaadin.v7.data.util.BeanItemContainer}: the table's entries. Changes made through it show in the
	 * table after {@link Table#refreshRowCache()} (or at once with {@link Table#addItem}/{@link Table#removeItem}).
	 */
	public static class BeanItemContainer<E> implements java.io.Serializable {

		private static final long serialVersionUID = 1L;

		private final Class<E> type;
		private final List<E> items = new ArrayList<>();
		private Runnable itemSetChangeListener;

		public BeanItemContainer(Class<E> type, Collection<E> items) {
			this.type = type;
			this.items.addAll(items);
		}

		public Class<E> getBeanType() {
			return type;
		}

		/**
		 * @return the entries (the live list)
		 */
		public List<E> getItemIds() {
			return items;
		}

		public int size() {
			return items.size();
		}

		public void addItem(E item) {
			items.add(item);
			fireItemSetChange();
		}

		public void addAll(Collection<? extends E> collection) {
			items.addAll(collection);
			fireItemSetChange();
		}

		public boolean removeItem(Object item) {
			boolean removed = items.remove(item);
			fireItemSetChange();
			return removed;
		}

		public void removeAllItems() {
			items.clear();
			fireItemSetChange();
		}

		void setItemSetChangeListener(Runnable itemSetChangeListener) {
			this.itemSetChangeListener = itemSetChangeListener;
		}

		private void fireItemSetChange() {
			if (itemSetChangeListener != null) {
				itemSetChangeListener.run();
			}
		}
	}

	/**
	 * Was the Vaadin 7 {@code Table} of the field: a Flow grid with the Table methods table fields use. Columns are bean
	 * properties of the entry type or generated columns, shown in the order of {@link #setVisibleColumns(Object...)};
	 * headers, widths and expand ratios are kept per column id. Page length 0 shows all rows.
	 */
	public static class Table extends Grid<Object> {

		private static final long serialVersionUID = 1L;

		private final PropertySet<?> propertySet;
		private final Map<Object, ColumnGenerator> columnGenerators = new LinkedHashMap<>();
		private final List<Object> visibleColumns = new ArrayList<>();
		private final Map<Object, String> columnHeaders = new HashMap<>();
		/**
		 * Vaadin 7 table column widths are content widths; a grid column width includes the cell's horizontal padding
		 * ({@code sormas.css} "Grids": 6px on each side).
		 */
		private static final int CELL_PADDING = 12;

		private final Map<Object, Integer> columnWidths = new HashMap<>();
		private final Set<Object> contentWidthColumns = new HashSet<>();
		private final Map<Object, Float> columnExpandRatios = new HashMap<>();
		private FieldAccessCellStyleGenerator cellStyleGenerator;
		private BeanItemContainer<?> container;
		private Runnable itemSetChangeListener;
		private int pageLength = 15;

		/**
		 * Was {@code com.vaadin.v7.ui.Table.ColumnGenerator}: the cell of a generated column. A returned component is
		 * shown as it is, any other value as its text.
		 */
		@FunctionalInterface
		public interface ColumnGenerator extends java.io.Serializable {

			Object generateCell(Table source, Object itemId, Object columnId);
		}

		public Table(Class<?> entryType) {
			super();
			this.propertySet = BeanPropertySet.get(entryType);
			setSelectionMode(SelectionMode.NONE);
			setPageLength(pageLength);
		}

		/**
		 * Was {@code setContainerDataSource(container[, visibleColumns])}; the visible columns are kept.
		 */
		@SuppressWarnings({
			"unchecked",
			"rawtypes" })
		public void setContainerDataSource(BeanItemContainer<?> container) {
			if (this.container != null) {
				this.container.setItemSetChangeListener(null);
			}
			this.container = container;
			container.setItemSetChangeListener(this::onItemSetChange);
			setDataProvider(new ListDataProvider<>((List) container.getItemIds()));
			onItemSetChange();
		}

		void setItemSetChangeListener(Runnable itemSetChangeListener) {
			this.itemSetChangeListener = itemSetChangeListener;
		}

		private void onItemSetChange() {
			getDataProvider().refreshAll();
			if (itemSetChangeListener != null) {
				itemSetChangeListener.run();
			}
		}

		@SuppressWarnings({
			"unchecked",
			"rawtypes" })
		public void addItem(Object item) {
			((BeanItemContainer) container).addItem(item);
		}

		public void removeItem(Object item) {
			container.removeItem(item);
		}

		public int size() {
			return container != null ? container.size() : 0;
		}

		public void refreshRowCache() {
			getDataProvider().refreshAll();
		}

		/**
		 * Vaadin 7: a generated column with this id replaces the property column of the same id; a new id is appended to
		 * the visible columns.
		 */
		public void addGeneratedColumn(Object id, ColumnGenerator generatedColumn) {
			columnGenerators.put(id, generatedColumn);
			if (!visibleColumns.contains(id)) {
				visibleColumns.add(id);
			}
			updateColumns();
		}

		public ColumnGenerator getColumnGenerator(Object columnId) {
			return columnGenerators.get(columnId);
		}

		public boolean removeGeneratedColumn(Object columnId) {
			if (columnGenerators.remove(columnId) == null) {
				return false;
			}
			if (!isContainerProperty(columnId)) {
				visibleColumns.remove(columnId);
			}
			updateColumns();
			return true;
		}

		/**
		 * Vaadin 7 {@code getContainerPropertyIds().contains(id)}. An id with a dot (e.g. a caption key such as
		 * {@code Location.street}) is not a property: the Flow property set throws for a dotted name it cannot resolve as
		 * a nested path, where Vaadin 7 said no.
		 */
		private boolean isContainerProperty(Object id) {
			try {
				return propertySet.getProperty(id.toString()).isPresent();
			} catch (IllegalArgumentException e) {
				return false;
			}
		}

		/**
		 * Vaadin 7: hides the column; a generated column keeps its generator and can be shown again.
		 */
		public boolean removeContainerProperty(Object propertyId) {
			visibleColumns.remove(propertyId);
			columnHeaders.remove(propertyId);
			updateColumns();
			return true;
		}

		public void setVisibleColumns(Object... visibleColumns) {
			for (Object id : visibleColumns) {
				if (!columnGenerators.containsKey(id) && !isContainerProperty(id)) {
					throw new IllegalArgumentException("Ids must exist in the Container or as a generated column, missing id: " + id);
				}
			}
			this.visibleColumns.clear();
			this.visibleColumns.addAll(Arrays.asList(visibleColumns));
			updateColumns();
		}

		public Object[] getVisibleColumns() {
			return visibleColumns.toArray();
		}

		/**
		 * The header is text; the Vaadin 7 HTML placeholder {@code &nbsp} becomes an empty header.
		 */
		public void setColumnHeader(Object propertyId, String header) {
			columnHeaders.put(propertyId, header);
			Column<Object, ?> column = getColumn(propertyId.toString());
			if (column != null) {
				column.setHeader(headerText(header));
			}
		}

		public void setColumnWidth(Object propertyId, int width) {
			columnWidths.put(propertyId, width);
			updateColumns();
		}

		/**
		 * Flow only: the column is as wide as its content and takes no extra space. Vaadin 7 {@link #setColumnWidth}
		 * gave the content width without the cell padding, which a fixed Flow column width (padding included) does not.
		 */
		public void setColumnWidthToContent(Object propertyId) {
			contentWidthColumns.add(propertyId);
			updateColumns();
		}

		public void setColumnExpandRatio(Object propertyId, float expandRatio) {
			columnExpandRatios.put(propertyId, expandRatio);
			updateColumns();
		}

		/**
		 * The field access styles are applied to all visible columns, also after the columns change.
		 */
		public void setCellStyleGenerator(FieldAccessCellStyleGenerator cellStyleGenerator) {
			this.cellStyleGenerator = cellStyleGenerator;
			updateColumns();
		}

		public int getPageLength() {
			return pageLength;
		}

		/**
		 * Vaadin 7: 0 shows all rows, otherwise the table is as high as this many rows.
		 */
		public void setPageLength(int pageLength) {
			this.pageLength = pageLength;
			if (pageLength == 0) {
				setHeightUndefined();
			} else {
				setHeightByRows(pageLength);
			}
		}

		private static String headerText(String header) {
			if (header == null || "&nbsp".equals(header) || "&nbsp;".equals(header)) {
				return "";
			}
			return header;
		}

		private void updateColumns() {

			removeAllColumns();
			for (Object id : visibleColumns) {
				String key = id.toString();
				Column<Object, ?> column;
				ColumnGenerator generator = columnGenerators.get(id);
				if (generator != null) {
					column = addComponentColumn(item -> toComponent(generator.generateCell(this, item, id)));
					column.setSortable(false);
					// a text cell exports its text; a component cell exports empty
					GridExportStreamResource.setExportValueProvider(column, item -> {
						Object cell = generator.generateCell(this, item, id);
						return cell instanceof Component ? null : cell;
					});
				} else {
					PropertyDefinition<?, ?> property = propertySet.getProperty(key).get();
					column = addColumn(item -> getPropertyValue(property, item));
				}
				column.setKey(key);
				column.setHeader(headerText(columnHeaders.containsKey(id) ? columnHeaders.get(id) : key));
				if (contentWidthColumns.contains(id)) {
					column.setAutoWidth(true);
					column.setFlexGrow(0);
				} else if (columnWidths.containsKey(id)) {
					column.setWidth(columnWidths.get(id) + CELL_PADDING);
				} else {
					column.setAutoWidth(true);
				}
				Float expandRatio = columnExpandRatios.get(id);
				if (expandRatio != null && expandRatio > 0) {
					column.setFlexGrow(Math.round(expandRatio));
				}
				if (cellStyleGenerator != null) {
					column.setStyleGenerator(cellStyleGenerator.forColumn(id));
				}
			}
		}

		@SuppressWarnings({
			"unchecked",
			"rawtypes" })
		private static Object getPropertyValue(PropertyDefinition property, Object item) {
			return property.getGetter().apply(item);
		}

		private static Component toComponent(Object cell) {
			if (cell instanceof Component) {
				return (Component) cell;
			}
			return new Span(cell != null ? cell.toString() : "");
		}
	}
}
