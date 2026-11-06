/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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
package de.symeda.sormas.ui.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.utils.fields.OptionGroup;

/**
 * Option group for nullable values (Yes/No/Unknown, symptom states, booleans): while not required it shows check boxes
 * of which at most one is selected, so a selected option can be clicked again to clear the value. The field value is
 * then a set with zero or one item id, and {@link NullableOptionGroupConverter} maps it to the single property value.
 * When required it is a plain single select (radio buttons) without converter, as in V7.
 */
@SuppressWarnings({
	"rawtypes",
	"unchecked" })
public class NullableOptionGroup extends OptionGroup {

	private static final long serialVersionUID = 1L;

	public NullableOptionGroup() {
		setup();
	}

	public NullableOptionGroup(String caption, Collection<?> options) {
		super(caption, options);
		setup();
	}

	public NullableOptionGroup(String caption) {
		super(caption);
		setup();
	}

	private void setup() {
		super.setMultiSelect(!isRequired());
		if (isRequired()) {
			setConverter(null);
		} else {
			setConverter(new NullableOptionGroupConverter(getValue() instanceof Set ? getFirstValue((Set) getValue()) : null));
		}
		addStyleName("multiline");
	}

	@Override
	public void setMultiSelect(boolean multiSelect) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Keeps at most one option selected: when the user selects a second option, the previous one is deselected (in V7
	 * the bound property did this through the converter).
	 */
	@Override
	protected Object generateModelValue() {
		Object newValue = super.generateModelValue();
		if (isMultiSelect() && newValue instanceof Set && ((Set<?>) newValue).size() > 1) {
			Set<Object> added = new LinkedHashSet<>((Set<?>) newValue);
			Object oldValue = getValue();
			if (oldValue instanceof Collection) {
				added.removeAll((Collection<?>) oldValue);
			}
			Set<Object> single = new LinkedHashSet<>();
			added.stream().findFirst().ifPresent(single::add);
			setPresentationValue(single);
			return single;
		}
		return newValue;
	}

	public Object getNullableValue() {
		final Object value = super.getValue();
		return ObjectUtils.isNotEmpty(value) ? getFirstValue((Set) value) : null;
	}

	public void setNullableValue(Object value) {
		if (value != null) {
			super.setValue(new LinkedHashSet<>(Collections.singleton(value)));
		} else {
			super.setValue(null);
		}
	}

	@Override
	public void setRequired(boolean required) {
		boolean readOnly = isReadOnly();
		setReadOnly(false);
		super.setRequired(required);
		setup();
		setReadOnly(readOnly);
	}

	private Object getFirstValue(Set value) {
		// Flow: the empty value of the field is null (V7: an empty set)
		return value != null ? value.stream().findFirst().orElse(null) : null;
	}

	public void setInaccessible() {
		this.removeAllItems();
		this.addItem(1);
		this.setItemCaption(1, I18nProperties.getCaption(Captions.inaccessibleValue));
	}
}
