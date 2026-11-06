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
package de.symeda.sormas.ui.dashboard.statistics;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;

import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

@SuppressWarnings("serial")
// was a Vaadin 8 Label in HTML mode
public class SvgBarElement extends Span {

	private static final int BAR_HEIGHT = 4;

	private String fillClass;

	public SvgBarElement(String fillClass) {
		this.fillClass = fillClass;
		setHeight(BAR_HEIGHT, Unit.PIXELS);
		updateSvg(100);
	}

	public void updateSvg(int percentageValue) {

		//@formatter:off
		VaadinUiUtil.setLabelValue(this, "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100%\" height=\"" + BAR_HEIGHT + "px\">"
				+ "<rect class=\"svg-bar " + fillClass + "\" width=\"" + percentageValue + "%\" height=\"" + BAR_HEIGHT + "px\"/>"
				+ "<rect class=\"svg-bar " + CssStyles.SVG_FILL_BACKGROUND + "\" x=\"" + percentageValue + "%\" width=\"" + (100 - percentageValue) + "%\" height=\"" + BAR_HEIGHT + "px\"/>"
				+ "</svg>", ContentMode.HTML);
		//@formatter:on
	}

}
