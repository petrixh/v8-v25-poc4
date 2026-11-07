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
import java.util.function.Supplier;

import com.vaadin.flow.component.combobox.dataview.ComboBoxDataView;
import com.vaadin.flow.component.combobox.dataview.ComboBoxLazyDataView;
import com.vaadin.flow.component.combobox.dataview.ComboBoxListDataView;
import com.vaadin.flow.data.provider.BackEndDataProvider;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.InMemoryDataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializablePredicate;

/**
 * Typed Flow combo box whose value survives a change of its items, as a Vaadin 8 {@code com.vaadin.ui.ComboBox} did.
 * Flow's {@code setItems(..)} sets the value to {@code null} (with a value change event); here, setting new items keeps
 * the value without an event when it is still among them, and clears it with one value change event when it is not.
 * With a lazy (back-end) data provider the value is kept, as membership cannot be checked.
 */
public class KeepValueComboBox<T> extends com.vaadin.flow.component.combobox.ComboBox<T> {

	private static final long serialVersionUID = 1L;

	// no initializer: the super constructor already sets items
	private boolean settingItems;

	public KeepValueComboBox() {
		super();
	}

	public KeepValueComboBox(String label) {
		super(label);
	}

	/**
	 * Ignores the reset to {@code null} that Flow performs while the items are being replaced.
	 */
	@Override
	public void setValue(T value) {
		if (settingItems) {
			return;
		}
		super.setValue(value);
	}

	private <R> R keepValue(Supplier<R> itemSetter) {
		if (settingItems) {
			return itemSetter.get();
		}
		T value = getValue();
		R result;
		settingItems = true;
		try {
			result = itemSetter.get();
		} finally {
			settingItems = false;
		}
		if (value != null) {
			if (isItem(value)) {
				// new data provider, new item keys: send the kept value to the client again
				refreshValue();
			} else {
				clear();
			}
		}
		return result;
	}

	private boolean isItem(T value) {
		DataProvider<T, ?> dataProvider = getDataProvider();
		if (dataProvider instanceof ListDataProvider) {
			return ((ListDataProvider<T>) dataProvider).getItems().contains(value);
		}
		return true;
	}

	@Override
	public ComboBoxListDataView<T> setItems(Collection<T> items) {
		return keepValue(() -> super.setItems(items));
	}

	@Override
	public ComboBoxListDataView<T> setItems(ItemFilter<T> itemFilter, Collection<T> items) {
		return keepValue(() -> super.setItems(itemFilter, items));
	}

	@Override
	@SuppressWarnings("unchecked")
	public ComboBoxListDataView<T> setItems(ItemFilter<T> itemFilter, T... items) {
		return keepValue(() -> super.setItems(itemFilter, items));
	}

	@Override
	public ComboBoxListDataView<T> setItems(ItemFilter<T> itemFilter, ListDataProvider<T> listDataProvider) {
		return keepValue(() -> super.setItems(itemFilter, listDataProvider));
	}

	@Override
	public ComboBoxListDataView<T> setItems(ListDataProvider<T> dataProvider) {
		return keepValue(() -> super.setItems(dataProvider));
	}

	@Override
	public ComboBoxDataView<T> setItems(DataProvider<T, String> dataProvider) {
		return keepValue(() -> super.setItems(dataProvider));
	}

	@Override
	public ComboBoxDataView<T> setItems(
		InMemoryDataProvider<T> inMemoryDataProvider,
		SerializableFunction<String, SerializablePredicate<T>> filterConverter) {
		return keepValue(() -> super.setItems(inMemoryDataProvider, filterConverter));
	}

	@Override
	public ComboBoxLazyDataView<T> setItems(BackEndDataProvider<T, String> dataProvider) {
		return keepValue(() -> super.setItems(dataProvider));
	}

	@Override
	public <C> ComboBoxLazyDataView<T> setItemsWithFilterConverter(
		CallbackDataProvider.FetchCallback<T, C> fetchCallback,
		SerializableFunction<String, C> filterConverter) {
		return keepValue(() -> super.setItemsWithFilterConverter(fetchCallback, filterConverter));
	}

	@Override
	public <C> ComboBoxLazyDataView<T> setItemsWithFilterConverter(
		CallbackDataProvider.FetchCallback<T, C> fetchCallback,
		CallbackDataProvider.CountCallback<T, C> countCallback,
		SerializableFunction<String, C> filterConverter) {
		return keepValue(() -> super.setItemsWithFilterConverter(fetchCallback, countCallback, filterConverter));
	}

	@Override
	public <C> void setDataProvider(DataProvider<T, C> dataProvider, SerializableFunction<String, C> filterConverter) {
		keepValue(() -> {
			super.setDataProvider(dataProvider, filterConverter);
			return null;
		});
	}

	@Override
	public void setDataProvider(FetchItemsCallback<T> fetchItems, SerializableFunction<String, Integer> sizeCallback) {
		keepValue(() -> {
			super.setDataProvider(fetchItems, sizeCallback);
			return null;
		});
	}
}
