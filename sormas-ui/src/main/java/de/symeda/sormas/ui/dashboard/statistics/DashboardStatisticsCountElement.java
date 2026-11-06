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

import com.vaadin.ui.Alignment;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.ui.utils.CssStyles;

@SuppressWarnings("serial")
public class DashboardStatisticsCountElement extends VerticalLayout {

	private Span countLabel;
	private Span captionLabel;

	public DashboardStatisticsCountElement(String caption, CountElementStyle countElementStyle) {
		this.setPadding(false);
		this.setSpacing(false);

		addClassName("count-element");
		addClassName(countElementStyle.getCssClass());
		countLabel = new Span();
		countLabel.setSizeUndefined();
		CssStyles.style(
			countLabel,
			CssStyles.LABEL_PRIMARY,
			CssStyles.LABEL_MEDIUM,
			CssStyles.LABEL_BOLD,
			CssStyles.LABEL_UPPERCASE,
			CssStyles.VSPACE_5,
			CssStyles.VSPACE_TOP_NONE);
		addComponent(countLabel);

		captionLabel = new Span(caption);
		captionLabel.setSizeUndefined();
		CssStyles.style(
			captionLabel,
			CssStyles.LABEL_SECONDARY,
			CssStyles.LABEL_SMALL,
			CssStyles.LABEL_BOLD,
			CssStyles.LABEL_UPPERCASE,
			CssStyles.VSPACE_5,
			CssStyles.VSPACE_TOP_NONE);
		addComponent(captionLabel);

		setComponentAlignment(countLabel, Alignment.MIDDLE_CENTER);
		setComponentAlignment(captionLabel, Alignment.MIDDLE_CENTER);
	}

	public void updateCountLabel(int count) {
		countLabel.setValue(Integer.toString(count));
	}

	public void updateCountLabel(String count) {
		countLabel.setValue(count);
	}
}
