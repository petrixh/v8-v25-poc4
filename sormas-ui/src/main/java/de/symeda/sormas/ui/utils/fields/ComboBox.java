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
package de.symeda.sormas.ui.utils.fields;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

import com.vaadin.flow.component.UI;

/**
 * Flow combo box with the V7 {@code com.vaadin.v7.ui.ComboBox} API: untyped item ids added one by one, explicit item
 * captions, filtering mode, null selection (shown as the clear button; when off, the user cannot clear the value) and new
 * items.
 */
public class ComboBox extends com.vaadin.flow.component.combobox.ComboBox<Object> implements SelectField {

	private static final long serialVersionUID = 1L;

	/**
	 * V7 {@code AbstractSelect.NewItemHandler}.
	 */
	@FunctionalInterface
	public interface NewItemHandler extends java.io.Serializable {

		void addNewItem(String newItemCaption);
	}

	private final SelectItems selectItems = new SelectItems(this, this::itemsRemoved);
	private FilteringMode filteringMode = FilteringMode.STARTSWITH;
	private NewItemHandler newItemHandler;

	public ComboBox() {
		super();
		init();
	}

	public ComboBox(String caption) {
		super(caption);
		init();
	}

	public ComboBox(String caption, Collection<?> options) {
		this(caption);
		addItems(options);
	}

	private void init() {
		setItems(this::matchesFilter, selectItems.getDataProvider());
		setItemLabelGenerator(selectItems::getCaption);
		setClearButtonVisible(selectItems.isNullSelectionAllowed());
		selectItems.setOnNullSelectionAllowedChange(() -> setClearButtonVisible(selectItems.isNullSelectionAllowed()));
		addCustomValueSetListener(e -> {
			if (!isNewItemsAllowed()) {
				return;
			}
			if (newItemHandler != null) {
				newItemHandler.addNewItem(e.getDetail());
			} else {
				addNewItemDefault(e.getDetail());
			}
		});
		// V7 default: no new items. Flow's addCustomValueSetListener turns custom values on; without this, typed text
		// that is no item plus Enter became the value (a String where the binding expects the item type).
		setAllowCustomValue(false);
	}

	/**
	 * V7 {@code AbstractSelect.DefaultNewItemHandler}: adds the typed text as a new item and selects it. A read-only
	 * combo box takes no new items (V7 threw a {@code ReadOnlyException}).
	 */
	private void addNewItemDefault(String newItemCaption) {
		if (isReadOnly()) {
			return;
		}
		if (addItem(newItemCaption) != null) {
			setValue(newItemCaption);
		}
	}

	private boolean matchesFilter(Object item, String filterText) {
		if (filterText == null || filterText.isEmpty() || filteringMode == FilteringMode.OFF) {
			return true;
		}
		UI ui = UI.getCurrent();
		Locale locale = ui != null ? ui.getLocale() : Locale.getDefault();
		String caption = selectItems.getCaption(item).toLowerCase(locale);
		String filter = filterText.toLowerCase(locale);
		return filteringMode == FilteringMode.CONTAINS ? caption.contains(filter) : caption.startsWith(filter);
	}

	/**
	 * V7 null selection off: the user cannot clear the combo box. A {@code null} from the client is ignored (the value
	 * stays, no value change event) and the client shows the kept value again. A programmatic {@code setValue(null)} or
	 * {@code clear()} still works, as in V7.
	 */
	@Override
	protected void setModelValue(Object newModelValue, boolean fromClient) {
		if (fromClient && newModelValue == null && !isNullSelectionAllowed()) {
			refreshValue();
			return;
		}
		super.setModelValue(newModelValue, fromClient);
	}

	/**
	 * V7: removing the selected item, or all items, clears the value. Adding items or removing other items keeps it, also
	 * when it is not an item (yet).
	 */
	protected void itemsRemoved(java.util.function.Predicate<Object> wasRemoved) {
		Object value = getValue();
		if (value != null && wasRemoved.test(value)) {
			clear();
		}
	}

	@Override
	public SelectItems getSelectItems() {
		return selectItems;
	}

	public FilteringMode getFilteringMode() {
		return filteringMode;
	}

	public void setFilteringMode(FilteringMode filteringMode) {
		this.filteringMode = Objects.requireNonNull(filteringMode);
		getDataProvider().refreshAll();
	}

	public boolean isNewItemsAllowed() {
		return isAllowCustomValue();
	}

	public void setNewItemsAllowed(boolean allowNewItems) {
		setAllowCustomValue(allowNewItems);
	}

	public NewItemHandler getNewItemHandler() {
		return newItemHandler;
	}

	/**
	 * Called with the typed text when the user enters a value that is not an item (and new items are allowed). Without
	 * a handler, the text is added as a new item and selected (V7 default).
	 */
	public void setNewItemHandler(NewItemHandler newItemHandler) {
		this.newItemHandler = newItemHandler;
	}

	/**
	 * No-op: Flow combo boxes always allow typing to filter.
	 */
	public void setTextInputAllowed(boolean textInputAllowed) {
		// see register: typing cannot be disabled in Flow
	}

	/**
	 * No-op: the Flow overlay sizes itself.
	 */
	public void setPageLength(int pageLength) {
		// nothing to do
	}

	/**
	 * No-op: the Flow overlay scrolls to the selected item.
	 */
	public void setScrollToSelectedItem(boolean scrollToSelectedItem) {
		// nothing to do
	}

	// Flow implements these as interface defaults; declared here because Field declares them too

	@Override
	public void setReadOnly(boolean readOnly) {
		super.setReadOnly(readOnly);
	}

	@Override
	public boolean isReadOnly() {
		return super.isReadOnly();
	}

	@Override
	public void setRequiredIndicatorVisible(boolean requiredIndicatorVisible) {
		super.setRequiredIndicatorVisible(requiredIndicatorVisible);
	}

	@Override
	public boolean isRequiredIndicatorVisible() {
		return super.isRequiredIndicatorVisible();
	}

	@Override
	public void clear() {
		super.clear();
	}
}
