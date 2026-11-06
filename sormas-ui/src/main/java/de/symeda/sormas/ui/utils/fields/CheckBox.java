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

/**
 * Flow checkbox as a SORMAS {@link Field} (replaces {@code com.vaadin.v7.ui.CheckBox}). The empty value is {@code false}.
 */
public class CheckBox extends com.vaadin.flow.component.checkbox.Checkbox implements Field<Boolean> {

	private static final long serialVersionUID = 1L;

	public CheckBox() {
		super();
	}

	public CheckBox(String caption) {
		super(caption);
	}

	public CheckBox(String caption, boolean initialValue) {
		super(caption, initialValue);
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
