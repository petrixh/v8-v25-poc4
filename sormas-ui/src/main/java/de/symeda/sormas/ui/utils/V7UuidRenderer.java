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

import org.apache.commons.lang3.StringUtils;

import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.HtmlHelper;
import de.symeda.sormas.ui.utils.grid.renderers.ConvertingHtmlRenderer;

@SuppressWarnings("serial")
public class V7UuidRenderer extends ConvertingHtmlRenderer {

	@Override
	protected String encodeString(String value) {

		if (StringUtils.isNotBlank(value)) {
			return HtmlHelper.buildHyperlinkTitle(value, DataHelper.getShortUuid(value));
		} else {
			return null;
		}
	}
}
