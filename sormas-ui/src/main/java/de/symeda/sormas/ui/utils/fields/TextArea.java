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
 * Flow text area as a SORMAS {@link Field} (replaces {@code com.vaadin.v7.ui.TextArea}).
 */
public class TextArea extends com.vaadin.flow.component.textfield.TextArea implements AbstractTextField {

	private static final long serialVersionUID = 1L;

	/**
	 * The value is {@code null}; the Flow value underneath is the null representation (Flow text fields do not take
	 * {@code null}). No initializer: Flow's constructors call {@link #setValue(String)} before field initializers run.
	 */
	private boolean nullValue;

	public TextArea() {
		super();
	}

	public TextArea(String caption) {
		super(caption);
	}

	/**
	 * V7 rows: the minimum number of visible rows.
	 */
	public void setRows(int rows) {
		setMinRows(rows);
	}

	/**
	 * No-op: Flow text areas always wrap.
	 */
	public void setWordWrap(boolean wordWrap) {
		// nothing to do
	}

	/**
	 * V7 {@code setValue(null)}: the value is {@code null} and the field shows the
	 * {@link #getNullRepresentation() null representation} (Flow rejects {@code null}). A change between {@code null}
	 * and the null representation fires a value change event, as in V7, though the field looks the same.
	 */
	@Override
	public void setValue(String value) {
		String oldValue = getValue();
		boolean wasNull = nullValue;
		nullValue = value == null;
		String shown = value != null ? value : getNullRepresentation();
		if (!Objects.equals(super.getValue(), shown)) {
			super.setValue(shown);
		} else if (wasNull != nullValue) {
			fireEvent(new ComponentValueChangeEvent<>(this, this, oldValue, false));
		}
	}

	/**
	 * {@code null} while the value is {@code null} (V7), else the text.
	 */
	@Override
	public String getValue() {
		return nullValue ? null : super.getValue();
	}

	/**
	 * Text from the browser, as V7 took it ({@link AbstractTextField#valueFromClient}).
	 */
	@Override
	protected void setModelValue(String newModelValue, boolean fromClient) {
		if (fromClient) {
			nullValue = AbstractTextField.valueFromClient(this, newModelValue, nullValue) == null;
		}
		super.setModelValue(newModelValue, fromClient);
	}

	/**
	 * V7: {@code null} and {@code ""} are empty.
	 */
	@Override
	public boolean isEmpty() {
		String value = getValue();
		return value == null || value.isEmpty();
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
