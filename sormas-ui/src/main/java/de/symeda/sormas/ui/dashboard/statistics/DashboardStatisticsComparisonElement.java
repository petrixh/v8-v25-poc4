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

import org.apache.commons.lang3.StringUtils;

import com.vaadin.ui.Alignment;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.ui.utils.CssStyles;

public class DashboardStatisticsComparisonElement extends VerticalLayout {

	private static final long serialVersionUID = 5403374519423424520L;

	private Span countLabel;
	private Span captionLabel;
	private String leftCaption;
	private String rightCaption;

	public DashboardStatisticsComparisonElement(String leftCaption, String rightCaption) {
		this.setPadding(false);
		this.setSpacing(false);

		this.leftCaption = leftCaption;
		this.rightCaption = rightCaption;

		countLabel = new Span();
		CssStyles.style(countLabel, CssStyles.LABEL_PRIMARY, CssStyles.LABEL_BOLD, CssStyles.LABEL_REALLY_LARGE);
		addComponent(countLabel);

		captionLabel = new Span();
		CssStyles.style(captionLabel, CssStyles.LABEL_SECONDARY, CssStyles.LABEL_UPPERCASE, CssStyles.LABEL_BOLD);
		addComponent(captionLabel);
		setComponentAlignment(captionLabel, Alignment.MIDDLE_RIGHT);
	}

	public void update(int firstCount, int secondCount, String leftCaptionAddition, String rightCaptionAddition) {
		countLabel.setValue(firstCount + " / " + secondCount);
		captionLabel.setValue(
			leftCaption
				+ (!StringUtils.isEmpty(leftCaptionAddition) ? " (" + leftCaptionAddition + ") / " : " / ")
				+ rightCaption
				+ (!StringUtils.isEmpty(rightCaptionAddition) ? " (" + rightCaptionAddition + ")" : ""));
	}
}
