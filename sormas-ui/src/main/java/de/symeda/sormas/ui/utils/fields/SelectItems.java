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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.vaadin.flow.data.provider.ListDataProvider;

/**
 * Item ids, captions and enabled state of a {@link SelectField}, backed by one {@link ListDataProvider}. After each
 * change the owner refreshes its component and keeps its value if the value is still an item (V7 behaviour).
 */
public class SelectItems implements Serializable {

	private static final long serialVersionUID = 1L;

	private final List<Object> itemIds = new ArrayList<>();
	private final Map<Object, String> captions = new HashMap<>();
	private final Set<Object> disabled = new HashSet<>();
	private final ListDataProvider<Object> dataProvider = new ListDataProvider<>(itemIds);
	private final Runnable onChange;
	private boolean nullSelectionAllowed = true;
	private Runnable onNullSelectionAllowedChange;

	public SelectItems(Runnable onChange) {
		this.onChange = onChange;
	}

	public ListDataProvider<Object> getDataProvider() {
		return dataProvider;
	}

	public boolean add(Object itemId) {
		if (itemId == null || itemIds.contains(itemId)) {
			return false;
		}
		itemIds.add(itemId);
		changed();
		return true;
	}

	public void addAll(Collection<?> ids) {
		boolean added = false;
		if (ids != null) {
			for (Object id : ids) {
				if (id != null && !itemIds.contains(id)) {
					itemIds.add(id);
					added = true;
				}
			}
		}
		if (added) {
			changed();
		}
	}

	public boolean remove(Object itemId) {
		boolean removed = itemIds.remove(itemId);
		captions.remove(itemId);
		disabled.remove(itemId);
		if (removed) {
			changed();
		}
		return removed;
	}

	public boolean removeAll() {
		boolean hadItems = !itemIds.isEmpty();
		itemIds.clear();
		captions.clear();
		disabled.clear();
		changed();
		return hadItems;
	}

	/**
	 * Keeps only the ids the filter accepts (used by subclasses that keep special items such as a placeholder).
	 */
	public boolean retainAll(Collection<?> ids) {
		boolean changed = itemIds.retainAll(ids);
		captions.keySet().retainAll(ids);
		disabled.retainAll(ids);
		if (changed) {
			changed();
		}
		return changed;
	}

	/**
	 * Sorts the items (V7 sorted the container).
	 */
	public void sort(java.util.Comparator<Object> comparator) {
		itemIds.sort(comparator);
		dataProvider.refreshAll();
	}

	public Collection<?> getItemIds() {
		return Collections.unmodifiableList(itemIds);
	}

	public boolean contains(Object itemId) {
		return itemIds.contains(itemId);
	}

	public void setCaption(Object itemId, String caption) {
		if (caption == null) {
			captions.remove(itemId);
		} else {
			captions.put(itemId, caption);
		}
		if (itemIds.contains(itemId)) {
			dataProvider.refreshItem(itemId);
		}
	}

	/**
	 * Explicit caption, else {@code toString()} (V7 caption mode EXPLICIT_DEFAULTS_ID).
	 */
	public String getCaption(Object itemId) {
		if (itemId == null) {
			return "";
		}
		String caption = captions.get(itemId);
		return caption != null ? caption : Objects.toString(itemId, "");
	}

	public void setEnabled(Object itemId, boolean enabled) {
		if (enabled) {
			disabled.remove(itemId);
		} else {
			disabled.add(itemId);
		}
		if (itemIds.contains(itemId)) {
			dataProvider.refreshItem(itemId);
		}
	}

	public boolean isEnabled(Object itemId) {
		return !disabled.contains(itemId);
	}

	public boolean isNullSelectionAllowed() {
		return nullSelectionAllowed;
	}

	public void setNullSelectionAllowed(boolean nullSelectionAllowed) {
		this.nullSelectionAllowed = nullSelectionAllowed;
		if (onNullSelectionAllowedChange != null) {
			onNullSelectionAllowedChange.run();
		}
	}

	public void setOnNullSelectionAllowedChange(Runnable onNullSelectionAllowedChange) {
		this.onNullSelectionAllowedChange = onNullSelectionAllowedChange;
	}

	private void changed() {
		dataProvider.refreshAll();
		if (onChange != null) {
			onChange.run();
		}
	}
}
