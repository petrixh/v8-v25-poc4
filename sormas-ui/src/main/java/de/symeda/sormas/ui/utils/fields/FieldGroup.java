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
import java.util.List;

/**
 * Property id based access to the fields of a form. Replaces the V7 {@code com.vaadin.v7.data.fieldgroup.FieldGroup}
 * in the {@link de.symeda.sormas.ui.utils.FieldHelper} API; the form base implements it on top of a Flow
 * {@link com.vaadin.flow.data.binder.Binder}.
 */
public interface FieldGroup {

	/**
	 * @return the field bound to the property id, or {@code null}
	 */
	Field<?> getField(Object propertyId);

	/**
	 * @return the property id the field is bound to, or {@code null}
	 */
	Object getPropertyId(Field<?> field);

	/**
	 * @return the bound property ids, in binding order
	 */
	Collection<Object> getBoundPropertyIds();

	/**
	 * @return the bound fields, in binding order
	 */
	List<Field<?>> getFields();
}
