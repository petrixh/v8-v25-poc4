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

import de.symeda.sormas.api.ReferenceDto;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.HtmlHelper;

@SuppressWarnings("serial")
public class V7HtmlReferenceDtoConverter implements Converter<String, ReferenceDto> {

	@Override
	public Result<ReferenceDto> convertToModel(String value, ValueContext context) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String convertToPresentation(ReferenceDto value, ValueContext context) {

		final String html;
		if (value != null) {
			html = String.format(
				"%s (%s)",
				HtmlHelper.buildHyperlinkTitle(value.getUuid(), DataHelper.getShortUuid(value.getUuid())),
				HtmlHelper.cleanHtml(value.getCaption()));
		} else {
			html = "";
		}
		return html;
	}

	public Class<ReferenceDto> getModelType() {
		return ReferenceDto.class;
	}

	public Class<String> getPresentationType() {
		return String.class;
	}
}
