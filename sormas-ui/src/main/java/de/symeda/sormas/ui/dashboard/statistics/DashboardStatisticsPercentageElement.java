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
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.ui.utils.CssStyles;

@SuppressWarnings("serial")
public class DashboardStatisticsPercentageElement extends VerticalLayout {

	private SvgBarElement svgBarElement;
	private Span percentageLabel;

	public DashboardStatisticsPercentageElement(String caption, String svgFillClass) {
		this.setPadding(false);
		this.setSpacing(false);

		HorizontalLayout captionAndValueLayout = new HorizontalLayout();
		captionAndValueLayout.setWidth(100, Unit.PERCENTAGE);

		Span captionLabel = new Span(caption);
		captionLabel.setWidth(null);
		CssStyles.style(captionLabel, CssStyles.LABEL_SECONDARY, CssStyles.LABEL_BOLD);
		captionAndValueLayout.add(captionLabel);

		percentageLabel = new Span();
		CssStyles.style(percentageLabel, CssStyles.LABEL_PRIMARY, CssStyles.LABEL_BOLD);
		percentageLabel.setWidth(null);
		captionAndValueLayout.add(percentageLabel);

		// was MIDDLE_LEFT and MIDDLE_RIGHT in two equal slots: the caption at the left end, the value at the right end
		captionAndValueLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		captionAndValueLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, captionLabel, percentageLabel);

		add(captionAndValueLayout);

		svgBarElement = new SvgBarElement(svgFillClass);
		svgBarElement.setWidth(100, Unit.PERCENTAGE);
		add(svgBarElement);
	}

	public void updatePercentageValue(int percentageValue) {
		percentageLabel.setText(Integer.toString(percentageValue) + "%");
		svgBarElement.updateSvg(percentageValue);
	}

	public void updatePercentageValueWithCount(int count, int percentageValue) {
		percentageLabel.setText(Integer.toString(count) + " (" + Integer.toString(percentageValue) + " %)");
		svgBarElement.updateSvg(percentageValue);
	}
}
