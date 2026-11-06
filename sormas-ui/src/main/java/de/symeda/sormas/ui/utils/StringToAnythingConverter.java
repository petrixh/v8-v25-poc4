/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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


import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

@SuppressWarnings("serial")
public class StringToAnythingConverter<T> implements Converter<String, T> {

	private Class<T> modelType;

	public StringToAnythingConverter(Class<T> modelType) {
		this.modelType = modelType;
	}

	@Override
	public Result<T> convertToModel(String value, ValueContext context) {
		return Result.ok(null);
	}

	@Override
	public String convertToPresentation(T value, ValueContext context) {
		return value == null ? null : value.toString();
	}

	public Class<T> getModelType() {
		return modelType;
	}

	public Class<String> getPresentationType() {
		return String.class;
	}
}
