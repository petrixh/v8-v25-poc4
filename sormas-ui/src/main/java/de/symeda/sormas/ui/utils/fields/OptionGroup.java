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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;

/**
 * Option group with the V7 {@code com.vaadin.v7.ui.OptionGroup} API. Single select shows a Flow
 * {@link RadioButtonGroup} and has the selected item id as value; multi select shows a {@link CheckboxGroup} and has a
 * {@code Set} of item ids as value (empty set when nothing is selected), as in V7.
 */
public class OptionGroup extends CustomField<Object> implements SelectField {

	private static final long serialVersionUID = 1L;

	private final SelectItems selectItems = new SelectItems(this, this::itemsRemoved);
	private boolean multiSelect;
	private RadioButtonGroup<Object> radioGroup;
	private CheckboxGroup<Object> checkboxGroup;
	/**
	 * Multi select read from a {@code null} property and not changed since: V7 kept {@code null} as the field's
	 * internal value and wrote it back on commit, while {@link #getValue()} showed an empty set.
	 */
	private boolean nullRead;
	/**
	 * Set by the form binding right before it sets the {@code null} it read ({@link #readNull()}).
	 */
	private boolean readingNull;
	/**
	 * Multi select: V7's internal value is {@code null} while {@link #getValue()} shows an empty set. V7 had it after
	 * reading a {@code null} property, and, with a converter ({@link NullableOptionGroup}), after any change to an
	 * empty value (the {@code setValue} round trip through the converter gives {@code null}). Reading {@code null}
	 * fired a value change event only when the internal value was not {@code null} yet.
	 */
	private boolean internalNull;

	public OptionGroup() {
		super();
		// the inner Flow group clears its selection when its items are refreshed; show the value again
		selectItems.setOnRefresh(() -> setPresentationValue(getValue()));
		createGroup();
	}

	public OptionGroup(String caption) {
		this();
		setCaption(caption);
	}

	public OptionGroup(String caption, Collection<?> options) {
		this(caption);
		addItems(options);
	}

	@Override
	protected Component initContent() {
		return null;
	}

	@Override
	public SelectItems getSelectItems() {
		return selectItems;
	}

	public boolean isMultiSelect() {
		return multiSelect;
	}

	public void setMultiSelect(boolean multiSelect) {
		if (this.multiSelect != multiSelect) {
			Object value = getValue();
			this.multiSelect = multiSelect;
			createGroup();
			// V7 converts the value between single and set
			if (multiSelect) {
				setValue(value == null ? new LinkedHashSet<>() : new LinkedHashSet<>(Collections.singleton(value)));
			} else {
				setValue(value instanceof Collection ? ((Collection<?>) value).stream().findFirst().orElse(null) : value);
			}
			// V7 converted the value before the converter was set: a multi select starts with an empty set
			internalNull = false;
		}
	}

	private void createGroup() {
		Component old = multiSelect ? radioGroup : checkboxGroup;
		if (old != null) {
			remove(old);
		}
		radioGroup = null;
		checkboxGroup = null;
		if (multiSelect) {
			checkboxGroup = new CheckboxGroup<>();
			checkboxGroup.setItems(selectItems.getDataProvider());
			checkboxGroup.setItemLabelGenerator(selectItems::getCaption);
			checkboxGroup.setItemEnabledProvider(selectItems::isEnabled);
			checkboxGroup.setReadOnly(isReadOnly());
			checkboxGroup.addValueChangeListener(e -> {
				if (e.isFromClient()) {
					clientValueChanged(e.getValue());
				}
			});
			add(checkboxGroup);
		} else {
			radioGroup = new RadioButtonGroup<>();
			radioGroup.setItems(selectItems.getDataProvider());
			radioGroup.setItemLabelGenerator(selectItems::getCaption);
			radioGroup.setItemEnabledProvider(selectItems::isEnabled);
			radioGroup.setReadOnly(isReadOnly());
			radioGroup.addValueChangeListener(e -> {
				if (e.isFromClient()) {
					clientValueChanged(e.getValue());
				}
			});
			add(radioGroup);
		}
		setPresentationValue(getValue());
	}

	/**
	 * V7 null selection off: the user cannot deselect the last item (multi select) or the selected item. The client's
	 * empty selection is ignored (no value change event) and the client shows the kept value again. A programmatic
	 * {@code setValue} still works, as in V7.
	 */
	private void clientValueChanged(Object clientValue) {
		if (!isNullSelectionAllowed()) {
			if (clientValue == null || (clientValue instanceof Collection && ((Collection<?>) clientValue).isEmpty())) {
				setPresentationValue(getValue());
				return;
			}
		}
		nullRead = false;
		internalNull = multiSelect && isEmptyValue(clientValue) && getConverter() != null;
		updateValue();
	}

	/**
	 * V7 {@code AbstractSelect.setValue}: in multi select, {@code null} is stored as an empty set, so the value of a
	 * multi select is never {@code null}. A {@code null} the form binding read is the exception ({@link #readNull()}).
	 * <p>
	 * Reading {@code null} into a multi select whose value is already an empty set fires a value change event, as V7
	 * {@code AbstractField.setPropertyDataSource} did when its internal value was not {@code null} yet (a new field
	 * holds an empty set): form listeners on the field (e.g. hiding dependent fields unless the value is Yes) run when
	 * the form reads an empty property. Reading {@code null} again fires nothing, as in V7.
	 */
	@Override
	public void setValue(Object value) {
		boolean fromRead = readingNull && value == null;
		readingNull = false;
		Object newValue = multiSelect && value == null ? new LinkedHashSet<>() : value;
		Object oldValue = getValue();
		boolean fireRead = fromRead && multiSelect && !internalNull && isEmptyValue(oldValue);
		nullRead = fromRead && multiSelect;
		if (!multiSelect) {
			internalNull = false;
		} else if (fromRead) {
			internalNull = true;
		} else if (internalNull || !Objects.equals(newValue, oldValue)) {
			// V7 AbstractField.setValue: a value equal to the internal value changes nothing; otherwise the value goes
			// through the converter and back, which makes an empty value null with a converter
			internalNull = isEmptyValue(newValue) && getConverter() != null;
		}
		super.setValue(newValue);
		if (fireRead) {
			fireEvent(new ComponentValueChangeEvent<>(this, this, oldValue, false));
		}
	}

	private static boolean isEmptyValue(Object value) {
		return value == null || (value instanceof Collection && ((Collection<?>) value).isEmpty());
	}

	/**
	 * The form binding is about to set the {@code null} it read from the property (V7
	 * {@code AbstractField.setPropertyDataSource}): a multi select then shows an empty set but writes {@code null} back
	 * until the value changes ({@link #isNullRead()}).
	 */
	void readNull() {
		readingNull = true;
	}

	/**
	 * Whether the form binding writes {@code null} back: a multi select read from a {@code null} property and not
	 * changed since (V7 {@code AbstractField.commit} wrote the internal value, {@code null}).
	 */
	boolean isNullRead() {
		return nullRead;
	}

	/**
	 * The inner Flow component: a {@link RadioButtonGroup} or a {@link CheckboxGroup}.
	 */
	public Component getGroupComponent() {
		return multiSelect ? checkboxGroup : radioGroup;
	}

	@Override
	protected Object generateModelValue() {
		if (multiSelect) {
			return new LinkedHashSet<>(checkboxGroup.getValue());
		}
		return radioGroup.getValue();
	}

	/**
	 * Shows the value in the inner group. An unchanged selection is not set again: Flow's {@code CheckboxGroup.setValue}
	 * updates every check box (one client call each) even when the selection is equal.
	 */
	@Override
	protected void setPresentationValue(Object value) {
		if (multiSelect) {
			Set<Object> selected = new LinkedHashSet<>();
			if (value instanceof Collection) {
				selected.addAll((Collection<?>) value);
			} else if (value != null) {
				selected.add(value);
			}
			selected.retainAll(selectItems.getItemIds());
			if (!selected.equals(checkboxGroup.getValue())) {
				checkboxGroup.setValue(selected);
			}
		} else if (radioGroup != null) {
			Object single = value instanceof Collection ? ((Collection<?>) value).stream().findFirst().orElse(null) : value;
			Object shown = single != null && selectItems.contains(single) ? single : null;
			if (!Objects.equals(shown, radioGroup.getValue())) {
				radioGroup.setValue(shown);
			}
		}
	}

	/**
	 * V7: removing a selected item removes it from the value; removing all items empties it. Adding items or removing
	 * other items keeps the value, also a part that is not an item (yet); the inner group shows it once its item is
	 * there (after the refresh, see the constructor).
	 */
	protected void itemsRemoved(java.util.function.Predicate<Object> wasRemoved) {
		Object value = getValue();
		if (multiSelect && value instanceof Collection) {
			Set<Object> retained = new LinkedHashSet<>((Collection<?>) value);
			retained.removeIf(wasRemoved);
			if (retained.size() != ((Collection<?>) value).size()) {
				setValue(retained);
				return;
			}
		} else if (!multiSelect && value != null && wasRemoved.test(value)) {
			setValue(null);
			return;
		}
		setPresentationValue(value);
	}

	@Override
	public boolean isEmpty() {
		Object value = getValue();
		return value == null || (value instanceof Collection && ((Collection<?>) value).isEmpty());
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		super.setReadOnly(readOnly);
		if (radioGroup != null) {
			radioGroup.setReadOnly(readOnly);
		}
		if (checkboxGroup != null) {
			checkboxGroup.setReadOnly(readOnly);
		}
	}

	/**
	 * Selects the item (V7 {@code select}); in multi select it is added to the selection.
	 */
	public void select(Object itemId) {
		if (multiSelect) {
			Set<Object> selected = getValue() instanceof Collection ? new LinkedHashSet<>((Collection<?>) getValue()) : new LinkedHashSet<>();
			selected.add(itemId);
			setValue(selected);
		} else {
			setValue(itemId);
		}
	}

	/**
	 * Unselects the item (V7 {@code unselect}).
	 */
	public void unselect(Object itemId) {
		if (multiSelect) {
			Set<Object> selected = getValue() instanceof Collection ? new LinkedHashSet<>((Collection<?>) getValue()) : new LinkedHashSet<>();
			selected.remove(itemId);
			setValue(selected);
		} else if (itemId != null && itemId.equals(getValue())) {
			setValue(null);
		}
	}

	public boolean isSelected(Object itemId) {
		Object value = getValue();
		return value instanceof Collection ? ((Collection<?>) value).contains(itemId) : itemId != null && itemId.equals(value);
	}
}
