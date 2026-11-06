/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2022 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.utils.components;

import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.shared.Registration;

/**
 * Was a V7 {@code Property<Boolean>} used as the property data source of a check box. Flow fields have no property
 * data source: {@link #bind(HasValue)} shows the current state in the check box and writes the user's changes back.
 */
public class CheckboxSetItemDataSource<T> implements Serializable {

	private static final long serialVersionUID = -1956478277209246907L;

	private final T propertyValue;
	private final Function<T, Boolean> isChecked;
	private final BiConsumer<Boolean, T> setChecked;

	private boolean readOnly;

	public CheckboxSetItemDataSource(T propertyValue, Function<T, Boolean> isChecked, BiConsumer<Boolean, T> setChecked) {
		this.propertyValue = propertyValue;
		this.isChecked = isChecked;
		this.setChecked = setChecked;
	}

	public Boolean getValue() {
		return isChecked.apply(propertyValue);
	}

	/**
	 * @throws IllegalStateException
	 *             if read-only (was the V7 {@code Property.ReadOnlyException})
	 */
	public void setValue(Boolean checked) {
		if (readOnly) {
			throw new IllegalStateException("read-only data source");
		}

		setChecked.accept(checked, propertyValue);
	}

	public Class<? extends Boolean> getType() {
		return Boolean.class;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	/**
	 * Was {@code new CheckBox(caption, dataSource)}: sets the check box to the current state and writes each change of
	 * the check box to this data source before the check box's later listeners run.
	 */
	public Registration bind(HasValue<?, Boolean> checkBox) {
		checkBox.setValue(Boolean.TRUE.equals(getValue()));
		return checkBox.addValueChangeListener(e -> setValue(e.getValue()));
	}
}
