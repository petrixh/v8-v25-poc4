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

	private final SelectItems selectItems = new SelectItems(this::itemsChanged);
	private boolean multiSelect;
	private RadioButtonGroup<Object> radioGroup;
	private CheckboxGroup<Object> checkboxGroup;

	public OptionGroup() {
		super();
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
		updateValue();
	}

	/**
	 * V7 {@code AbstractSelect.setValue}: in multi select, {@code null} is stored as an empty set, so the value of a
	 * multi select is never {@code null}.
	 */
	@Override
	public void setValue(Object value) {
		super.setValue(multiSelect && value == null ? new LinkedHashSet<>() : value);
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
			checkboxGroup.setValue(selected);
		} else if (radioGroup != null) {
			Object single = value instanceof Collection ? ((Collection<?>) value).stream().findFirst().orElse(null) : value;
			radioGroup.setValue(single != null && selectItems.contains(single) ? single : null);
		}
	}

	/**
	 * V7: removing the selected item removes it from the value.
	 */
	protected void itemsChanged() {
		Object value = getValue();
		if (multiSelect && value instanceof Collection) {
			Set<Object> retained = new LinkedHashSet<>((Collection<?>) value);
			retained.retainAll(selectItems.getItemIds());
			if (retained.size() != ((Collection<?>) value).size()) {
				setValue(retained);
				return;
			}
		} else if (!multiSelect && value != null && !selectItems.contains(value)) {
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
