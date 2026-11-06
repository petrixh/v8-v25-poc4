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

package de.symeda.sormas.ui.utils.components.customizablefield;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.function.ValueProvider;

import de.symeda.sormas.api.customizablefield.CustomizableFieldMetadataDto;
import de.symeda.sormas.api.customizablefield.CustomizableFieldValueDto;

/**
 * Concrete {@link CustomizableFieldInput} for {@link de.symeda.sormas.api.customizablefield.CustomizableFieldType#CHECKBOX}.
 * <p>
 * Renders a Flow {@link Checkbox}. The checked state is serialised to/from the DTO's
 * {@code value} field as {@code "true"} / {@code "false"} via
 * {@link CustomizableFieldValueDto#getValueAsBoolean()} and
 * {@link CustomizableFieldValueDto#setValueAsBoolean(Boolean)}.
 * An absent/unrecognised raw value is treated as {@code false}.
 */
public class CustomizableFieldInputCheckbox extends CustomizableFieldInput<Boolean> {

	private static final long serialVersionUID = 1L;

	private Checkbox checkBox;
	/**
	 * Holds a value that was pushed via {@link #setPresentationValue(Object)} before {@link #buildInputComponent()}
	 * had a chance to create the {@link Checkbox}. Applied to the check box on first render.
	 */
	private Boolean pendingValue;

	public CustomizableFieldInputCheckbox(CustomizableFieldMetadataDto metadata) {
		super(metadata);
	}

	@Override
	protected ValueProvider<CustomizableFieldValueDto, Boolean> getValueGetter() {
		return CustomizableFieldValueDto::getValueAsBoolean;
	}

	@Override
	protected Setter<CustomizableFieldValueDto, Boolean> getValueSetter() {
		return CustomizableFieldValueDto::setValueAsBoolean;
	}

	public Class<Boolean> getType() {
		return Boolean.class;
	}

	@Override
	protected Component buildInputComponent() {
		checkBox = new Checkbox();

		// Apply any value that arrived before the component was first rendered.
		if (pendingValue != null) {
			checkBox.setValue(pendingValue);
			pendingValue = null;
		}

		// Propagate user edits back to the field's internal value state.
		checkBox.addValueChangeListener(e -> setValue(e.getValue()));

		return checkBox;
	}

	/**
	 * Called by Vaadin when {@link #setValue(Object)} is invoked programmatically.
	 * Pushes the new state into the {@link Checkbox}, defaulting {@code null} to {@code false}.
	 * If the {@link Checkbox} has not been created yet (component not yet rendered),
	 * the value is stored as a pending value and applied in {@link #buildInputComponent()}.
	 */
	@Override
	protected void applyValueToWidget(Boolean value) {
		boolean checked = Boolean.TRUE.equals(value);
		if (checkBox != null) {
			checkBox.setValue(checked);
		} else {
			pendingValue = checked;
		}
	}
}
