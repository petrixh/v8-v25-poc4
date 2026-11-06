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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;

/**
 * Base for composite SORMAS fields (replaces {@code com.vaadin.v7.ui.CustomField}). Like in V7, subclasses build their
 * content in {@link #initContent()}, which runs once, on first attach or first {@link #getContent()} call.
 * <p>
 * Flow contract: when an inner component changes, call {@link #updateValue()} (it reads
 * {@link #generateModelValue()}) or {@code setValue}; override {@link #setPresentationValue(Object)} to show a value
 * set from outside. The defaults keep the value in the field itself.
 *
 * @param <T>
 *            value type
 */
public abstract class CustomField<T> extends com.vaadin.flow.component.customfield.CustomField<T> implements Field<T> {

	private static final long serialVersionUID = 1L;

	private Component content;

	protected CustomField() {
		super();
	}

	protected CustomField(T defaultValue) {
		super(defaultValue);
	}

	/**
	 * @return the content component, or {@code null} if the subclass adds its components itself
	 */
	protected abstract Component initContent();

	public Component getContent() {
		if (content == null) {
			content = initContent();
			if (content != null) {
				add(content);
			}
		}
		return content;
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		getContent();
		super.onAttach(attachEvent);
	}

	@Override
	protected T generateModelValue() {
		return getValue();
	}

	@Override
	protected void setPresentationValue(T newPresentationValue) {
		// value is kept in the field
	}

	/**
	 * V7 {@code getType()}. Optional on Flow; subclasses may override it.
	 */
	public Class<? extends T> getType() {
		return null;
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
