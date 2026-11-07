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

import java.util.Objects;

/**
 * Flow checkbox as a SORMAS {@link Field} (replaces {@code com.vaadin.v7.ui.CheckBox}). The empty value is {@code false}.
 * <p>
 * As in V7, the value can be {@code null}: it shows unchecked, and {@link #getValue()} (so also the form's write-back of
 * a nullable {@code Boolean} property) stays {@code null} until the user or code sets {@code true} or {@code false}.
 */
public class CheckBox extends com.vaadin.flow.component.checkbox.Checkbox implements Field<Boolean> {

	private static final long serialVersionUID = 1L;

	/**
	 * The value is {@code null}; the Flow value underneath is {@code false} (Flow check boxes do not take {@code null}).
	 */
	private boolean nullValue;

	public CheckBox() {
		super();
	}

	public CheckBox(String caption) {
		super(caption);
	}

	public CheckBox(String caption, boolean initialValue) {
		super(caption, initialValue);
	}

	/**
	 * V7 {@code CheckBox.setValue(null)} was allowed: the box shows unchecked and the value stays {@code null}; Flow
	 * throws on {@code null}. A change between {@code null} and {@code false} fires a value change event, as in V7, though
	 * the box looks the same.
	 */
	@Override
	public void setValue(Boolean value) {
		Boolean oldValue = getValue();
		boolean wasNull = nullValue;
		nullValue = value == null;
		Boolean shown = value == null ? Boolean.FALSE : value;
		if (!Objects.equals(super.getValue(), shown)) {
			super.setValue(shown);
		} else if (wasNull != nullValue) {
			fireEvent(new ComponentValueChangeEvent<>(this, this, oldValue, false));
		}
	}

	@Override
	public Boolean getValue() {
		return nullValue ? null : super.getValue();
	}

	/**
	 * A click in the browser sets {@code true} or {@code false}.
	 */
	@Override
	protected void setModelValue(Boolean newModelValue, boolean fromClient) {
		if (fromClient) {
			nullValue = false;
		}
		super.setModelValue(newModelValue, fromClient);
	}

	/**
	 * {@code null} and {@code false} are empty.
	 */
	@Override
	public boolean isEmpty() {
		return !Boolean.TRUE.equals(getValue());
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
