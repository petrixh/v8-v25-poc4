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

import com.vaadin.flow.component.icon.VaadinIcon;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.HtmlHelper;
import de.symeda.sormas.ui.utils.grid.renderers.ConvertingHtmlRenderer;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

@SuppressWarnings("serial")
public class V7CaseUuidRenderer extends ConvertingHtmlRenderer {

	private final boolean withCreateCaseIfEmpty;

	public V7CaseUuidRenderer(boolean withCreateCaseIfEmpty) {
		this.withCreateCaseIfEmpty = withCreateCaseIfEmpty;
	}

	@Override
	protected String encodeString(String value) {

		if (StringUtils.isBlank(value) && withCreateCaseIfEmpty) {
			String createCase = String.format(
				"%s %s",
				HtmlHelper
					.buildHyperlinkTitle(I18nProperties.getString(Strings.headingCreateNewCase), I18nProperties.getCaption(Captions.actionCreate)),
				HtmlRenderer.iconHtml(VaadinIcon.EDIT));
			return createCase;
		} else if (StringUtils.isNotBlank(value)) {
			return HtmlHelper.buildHyperlinkTitle(value, DataHelper.getShortUuid(value));
		} else {
			return null;
		}
	}
}
