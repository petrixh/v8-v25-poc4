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
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import de.symeda.sormas.ui.utils.ContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

@SuppressWarnings("serial")
public class DashboardStatisticsGrowthElement extends VerticalLayout {

	protected HorizontalLayout growthLayout;
	protected Span countLabel;
	protected Span growthLabel;
	protected Span percentageLabel;
	protected Span captionLabel;

	protected String caption;
	private String captionClass;

	public DashboardStatisticsGrowthElement(String caption) {
		this(caption, null, null);
	}

	public DashboardStatisticsGrowthElement(String caption, String captionClass, FlexComponent.Alignment alignment) {
		this.setPadding(false);
		this.setSpacing(false);

		this.caption = caption;
		this.captionClass = captionClass;

		if (alignment != null) {
			setDefaultHorizontalComponentAlignment(alignment);
		}
		CssStyles.style(this, CssStyles.VSPACE_3);

		growthLayout = new HorizontalLayout();

		createCountLabel();
		growthLayout.add(countLabel);
		createGrowthLabel();
		growthLayout.add(growthLabel);
		growthLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, growthLabel);
		createPercentageLabel();
		growthLayout.add(percentageLabel);

		add(growthLayout);

		createCaptionLabel();
		add(captionLabel);
	}

	protected void createCountLabel() {
		countLabel = new Span();
		CssStyles.style(countLabel, CssStyles.LABEL_LARGE, CssStyles.LABEL_PRIMARY, CssStyles.LABEL_BOLD, CssStyles.HSPACE_RIGHT_4);
	}

	protected void createGrowthLabel() {
		growthLabel = new Span();
		growthLabel.setHeight(null);
		CssStyles.style(growthLabel, CssStyles.LABEL_MEDIUM, CssStyles.LABEL_PRIMARY, CssStyles.LABEL_BOLD, CssStyles.HSPACE_RIGHT_4);

	}

	protected void createPercentageLabel() {
		percentageLabel = new Span();
		CssStyles.style(percentageLabel, CssStyles.LABEL_LARGE, CssStyles.LABEL_PRIMARY, CssStyles.LABEL_BOLD);
	}

	protected void createCaptionLabel() {
		captionLabel = new Span(caption);
		captionLabel.setWidth(null);
		CssStyles.style(captionLabel, CssStyles.LABEL_MEDIUM, CssStyles.LABEL_UPPERCASE, CssStyles.LABEL_BOLD, captionClass);

	}

	public void update(int count, int percentage, boolean increaseIsPositive) {
		countLabel.setText(Integer.toString(count));
		percentageLabel.setText(percentage != Integer.MIN_VALUE ? (percentage + " %") : "");
		CssStyles.removeStyles(growthLabel, CssStyles.LABEL_CRITICAL, CssStyles.LABEL_POSITIVE, CssStyles.LABEL_IMPORTANT);
		if (percentage > 0) {
			VaadinUiUtil.setLabelValue(growthLabel, HtmlRenderer.iconHtml(VaadinIcon.CHEVRON_UP), ContentMode.HTML);
			CssStyles.style(growthLabel, increaseIsPositive ? CssStyles.LABEL_POSITIVE : CssStyles.LABEL_CRITICAL);
		} else if (percentage < 0) {
			VaadinUiUtil.setLabelValue(growthLabel, HtmlRenderer.iconHtml(VaadinIcon.CHEVRON_DOWN), ContentMode.HTML);
			CssStyles.style(growthLabel, increaseIsPositive ? CssStyles.LABEL_CRITICAL : CssStyles.LABEL_POSITIVE);
		} else {
			VaadinUiUtil.setLabelValue(growthLabel, HtmlRenderer.iconHtml(VaadinIcon.CHEVRON_RIGHT), ContentMode.HTML);
			CssStyles.style(growthLabel, CssStyles.LABEL_IMPORTANT);
		}
	}
}
