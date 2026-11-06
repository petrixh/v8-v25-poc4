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

import com.vaadin.flow.component.icon.VaadinIcon;
import de.symeda.sormas.ui.utils.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.ui.utils.CssStyles;

@SuppressWarnings("serial")
public class DashboardStatisticsGraphicalGrowthElement extends VerticalLayout {

	private SvgBarElement svgBarElement;
	private Span countLabel;
	private Span growthLabel;
	private Span percentageLabel;

	public DashboardStatisticsGraphicalGrowthElement(String caption, String svgFillClass) {
		this.setPadding(false);
		this.setSpacing(false);

		HorizontalLayout captionAndValueLayout = new HorizontalLayout();
		captionAndValueLayout.setPadding(false);
		captionAndValueLayout.setSpacing(false);
		captionAndValueLayout.setWidth(100, Unit.PERCENTAGE);

		Span captionLabel = new Span(caption);
		CssStyles.style(captionLabel, CssStyles.LABEL_SECONDARY, CssStyles.LABEL_BOLD);
		captionAndValueLayout.addComponent(captionLabel);

		countLabel = new Span();
		CssStyles.style(countLabel, CssStyles.LABEL_PRIMARY, CssStyles.LABEL_BOLD, CssStyles.HSPACE_RIGHT_4);
		countLabel.setWidthUndefined();
		captionAndValueLayout.addComponent(countLabel);
		growthLabel = new Span();
		growthLabel.setHeightUndefined();
		growthLabel.setWidthUndefined();
		growthLabel.setContentMode(ContentMode.HTML);
		CssStyles.style(growthLabel, CssStyles.LABEL_SMALL, CssStyles.LABEL_PRIMARY, CssStyles.LABEL_BOLD, CssStyles.HSPACE_RIGHT_4);
		captionAndValueLayout.addComponent(growthLabel);
		percentageLabel = new Span();
		CssStyles.style(percentageLabel, CssStyles.LABEL_PRIMARY, CssStyles.LABEL_BOLD);
		percentageLabel.setWidthUndefined();
		captionAndValueLayout.addComponent(percentageLabel);

		captionAndValueLayout.setComponentAlignment(captionLabel, Alignment.MIDDLE_LEFT);
		captionAndValueLayout.setComponentAlignment(countLabel, Alignment.MIDDLE_RIGHT);
		captionAndValueLayout.setComponentAlignment(growthLabel, Alignment.MIDDLE_RIGHT);
		captionAndValueLayout.setComponentAlignment(percentageLabel, Alignment.MIDDLE_RIGHT);
		captionAndValueLayout.setFlexGrow(1, captionLabel);

		addComponent(captionAndValueLayout);

		svgBarElement = new SvgBarElement(svgFillClass);
		svgBarElement.setWidth(100, Unit.PERCENTAGE);
		addComponent(svgBarElement);
	}

	public void update(int count, int percentage, int growthPercentage, boolean increaseIsPositive, boolean showPercentage) {
		countLabel.setValue(count + (showPercentage ? " (" + percentage + " %)" : ""));
		svgBarElement.updateSvg(percentage);
		percentageLabel.setValue(growthPercentage != Integer.MIN_VALUE ? (growthPercentage + " %") : "");
		CssStyles.removeStyles(growthLabel, CssStyles.LABEL_CRITICAL, CssStyles.LABEL_POSITIVE, CssStyles.LABEL_IMPORTANT);
		if (growthPercentage > 0) {
			growthLabel.setValue(VaadinIcon.CHEVRON_UP.getHtml());
			CssStyles.style(growthLabel, increaseIsPositive ? CssStyles.LABEL_POSITIVE : CssStyles.LABEL_CRITICAL);
		} else if (growthPercentage < 0) {
			growthLabel.setValue(VaadinIcon.CHEVRON_DOWN.getHtml());
			CssStyles.style(growthLabel, increaseIsPositive ? CssStyles.LABEL_CRITICAL : CssStyles.LABEL_POSITIVE);
		} else {
			growthLabel.setValue(VaadinIcon.CHEVRON_RIGHT.getHtml());
			CssStyles.style(growthLabel, CssStyles.LABEL_IMPORTANT);
		}
	}
}
