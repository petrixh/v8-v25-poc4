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

import java.util.Date;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

import de.symeda.sormas.api.utils.DateHelper;

@SuppressWarnings("serial")
public class DateConverter implements Converter<Date, Date> {

	@Override
	public Result<Date> convertToModel(Date value, ValueContext context) {
		return Result.ok(DateHelper.toCorrectCentury(value));
	}

	@Override
	public Date convertToPresentation(Date value, ValueContext context) {
		return value;
	}

	public Class<Date> getModelType() {
		return Date.class;
	}

	public Class<Date> getPresentationType() {
		return Date.class;
	}
}
