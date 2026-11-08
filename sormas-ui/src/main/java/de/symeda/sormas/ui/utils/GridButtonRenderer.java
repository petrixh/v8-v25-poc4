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

import de.symeda.sormas.api.utils.HtmlHelper;
import de.symeda.sormas.ui.utils.grid.renderers.ConvertingHtmlRenderer;

@SuppressWarnings("serial")
public class GridButtonRenderer extends ConvertingHtmlRenderer {

	@Override
	protected String encodeString(String value) {

		if (!StringUtils.isEmpty(value)) {
			// Vaadin 8 drew a primary button with its own markup; the Flow button web component replaces it. The button is
			// drawn only: as in Vaadin 8, a click on it is the grid's item click on its column, which does the action. A Flow
			// grid fires no item click for a click on a focusable element in a cell, so the button takes no pointer events
			// and the click lands on the cell.
			value = "<vaadin-button theme='primary' style='pointer-events: none'>" + HtmlHelper.cleanHtml(value) + "</vaadin-button>";

			return value;
		} else {
			return null;
		}
	}
}
