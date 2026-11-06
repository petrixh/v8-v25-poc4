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

/**
 * The V7 {@code AbstractSelect} item API on Flow selection fields ({@link ComboBox}, {@link OptionGroup}): items are
 * added and removed by item id, with optional explicit captions; otherwise the caption is {@code toString()}.
 */
public interface SelectField extends Field<Object> {

	SelectItems getSelectItems();

	/**
	 * @return the item id if it was added, {@code null} if it was there already
	 */
	default Object addItem(Object itemId) {
		return getSelectItems().add(itemId) ? itemId : null;
	}

	default void addItems(Collection<?> itemIds) {
		getSelectItems().addAll(itemIds);
	}

	default void addItems(Object... itemIds) {
		getSelectItems().addAll(java.util.Arrays.asList(itemIds));
	}

	default boolean removeItem(Object itemId) {
		return getSelectItems().remove(itemId);
	}

	default boolean removeAllItems() {
		return getSelectItems().removeAll();
	}

	default Collection<?> getItemIds() {
		return getSelectItems().getItemIds();
	}

	default boolean containsId(Object itemId) {
		return getSelectItems().contains(itemId);
	}

	default int size() {
		return getSelectItems().getItemIds().size();
	}

	default void setItemCaption(Object itemId, String caption) {
		getSelectItems().setCaption(itemId, caption);
	}

	default String getItemCaption(Object itemId) {
		return getSelectItems().getCaption(itemId);
	}

	default void setItemEnabled(Object itemId, boolean enabled) {
		getSelectItems().setEnabled(itemId, enabled);
	}

	default boolean isItemEnabled(Object itemId) {
		return getSelectItems().isEnabled(itemId);
	}

	default boolean isNullSelectionAllowed() {
		return getSelectItems().isNullSelectionAllowed();
	}

	default void setNullSelectionAllowed(boolean nullSelectionAllowed) {
		getSelectItems().setNullSelectionAllowed(nullSelectionAllowed);
	}
}
