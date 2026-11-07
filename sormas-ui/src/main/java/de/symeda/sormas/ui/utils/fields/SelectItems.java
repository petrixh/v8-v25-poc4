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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.ListDataProvider;

import de.symeda.sormas.api.utils.HasCaption;

/**
 * Item ids, captions and enabled state of a {@link SelectField}, backed by one {@link ListDataProvider}. After a removal
 * the owner drops the removed items from its value (V7 {@code removeItem} unselected the item, {@code removeAllItems} set
 * the value to {@code null}). Adding items never changes the value: a value that is not (yet) an item stays, as V7 kept
 * a bound property's value whatever the container held (forms fill their selects after the bean is read). The data
 * provider refresh, which makes
 * Flow reset the client side of the component, is done once per round trip ({@link UI#beforeClientResponse}) however
 * many items were added, removed or re-captioned (V7 sent the container once per response too).
 */
public class SelectItems implements Serializable {

	private static final long serialVersionUID = 1L;

	private final List<Object> itemIds = new ArrayList<>();
	private final Map<Object, String> captions = new HashMap<>();
	private final Set<Object> disabled = new HashSet<>();
	private final ListDataProvider<Object> dataProvider = new ListDataProvider<>(itemIds);
	private final Component owner;
	private final Consumer<Predicate<Object>> onRemove;
	private boolean nullSelectionAllowed = true;
	private Runnable onNullSelectionAllowedChange;
	private Runnable onRefresh;
	private boolean refreshAllPending;
	private final Set<Object> refreshItemsPending = new LinkedHashSet<>();
	private boolean refreshScheduled;

	/**
	 * @param onRemove
	 *            called after items were removed, with a test that is true for each removed item id (for every id after
	 *            {@link #removeAll()})
	 */
	public SelectItems(Consumer<Predicate<Object>> onRemove) {
		this(null, onRemove);
	}

	/**
	 * @param owner
	 *            the component showing the items; its data provider refreshes are coalesced once per round trip. Without
	 *            an owner every change refreshes at once.
	 */
	public SelectItems(Component owner, Consumer<Predicate<Object>> onRemove) {
		this.owner = owner;
		this.onRemove = onRemove;
	}

	public ListDataProvider<Object> getDataProvider() {
		return dataProvider;
	}

	public boolean add(Object itemId) {
		if (itemId == null || itemIds.contains(itemId)) {
			return false;
		}
		itemIds.add(itemId);
		refreshAll();
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
			refreshAll();
		}
	}

	public boolean remove(Object itemId) {
		boolean removed = itemIds.remove(itemId);
		captions.remove(itemId);
		disabled.remove(itemId);
		if (removed) {
			removed(itemId::equals);
		}
		return removed;
	}

	public boolean removeAll() {
		boolean hadItems = !itemIds.isEmpty();
		itemIds.clear();
		captions.clear();
		disabled.clear();
		removed(id -> true);
		return hadItems;
	}

	/**
	 * Keeps only the ids the filter accepts (used by subclasses that keep special items such as a placeholder).
	 */
	public boolean retainAll(Collection<?> ids) {
		Set<Object> removedIds = new HashSet<>(itemIds);
		boolean changed = itemIds.retainAll(ids);
		removedIds.removeAll(itemIds);
		captions.keySet().retainAll(ids);
		disabled.retainAll(ids);
		if (changed) {
			removed(removedIds::contains);
		}
		return changed;
	}

	/**
	 * Sorts the items (V7 sorted the container).
	 */
	public void sort(java.util.Comparator<Object> comparator) {
		itemIds.sort(comparator);
		refreshAll();
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
			refreshItem(itemId);
		}
	}

	/**
	 * Explicit caption, else the item id as text (V7 caption mode EXPLICIT_DEFAULTS_ID). V7 converted the id with the
	 * session converter factory ({@code SormasDefaultConverterFactory}): a {@link HasCaption} (reference DTOs) shows
	 * {@code buildCaption()}, not its {@code toString()} (class name and uuid); other ids show {@code toString()}.
	 */
	public String getCaption(Object itemId) {
		if (itemId == null) {
			return "";
		}
		String caption = captions.get(itemId);
		if (caption != null) {
			return caption;
		}
		if (itemId instanceof HasCaption) {
			return Objects.toString(((HasCaption) itemId).buildCaption(), "");
		}
		return Objects.toString(itemId, "");
	}

	public void setEnabled(Object itemId, boolean enabled) {
		if (enabled) {
			disabled.remove(itemId);
		} else {
			disabled.add(itemId);
		}
		if (itemIds.contains(itemId)) {
			refreshItem(itemId);
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

	/**
	 * Run after each full data provider refresh (Flow's radio button and check box groups clear their selection on it).
	 */
	public void setOnRefresh(Runnable onRefresh) {
		this.onRefresh = onRefresh;
	}

	private void removed(Predicate<Object> wasRemoved) {
		refreshAll();
		if (onRemove != null) {
			onRemove.accept(wasRemoved);
		}
	}

	private void refreshAll() {
		refreshAllPending = true;
		refreshItemsPending.clear();
		scheduleRefresh();
	}

	private void refreshItem(Object itemId) {
		if (!refreshAllPending) {
			refreshItemsPending.add(itemId);
			scheduleRefresh();
		}
	}

	/**
	 * Runs the pending refresh before the next response. Flow keeps the task until the owner is attached, so a form
	 * built while detached refreshes once when it is shown. Without an owner or a UI (e.g. unit tests outside a
	 * request) it refreshes at once.
	 */
	private void scheduleRefresh() {
		if (refreshScheduled) {
			return;
		}
		UI ui = owner == null ? null : owner.getUI().orElseGet(UI::getCurrent);
		if (ui == null) {
			flushRefresh();
			return;
		}
		refreshScheduled = true;
		ui.beforeClientResponse(owner, context -> flushRefresh());
	}

	/**
	 * Refreshes the data provider now if a change is pending (also called by the owner before it relies on the
	 * component's item state).
	 */
	public void flushRefresh() {
		refreshScheduled = false;
		if (refreshAllPending) {
			refreshAllPending = false;
			dataProvider.refreshAll();
			if (onRefresh != null) {
				onRefresh.run();
			}
		} else if (!refreshItemsPending.isEmpty()) {
			List<Object> items = new ArrayList<>(refreshItemsPending);
			refreshItemsPending.clear();
			items.forEach(dataProvider::refreshItem);
		}
	}
}
