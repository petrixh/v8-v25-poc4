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
package de.symeda.sormas.ui.dashboard.surveillance.components.disease.tile;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.disease.DiseaseBurdenDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

public class DiseaseTileComponent extends VerticalLayout {

	private static final long serialVersionUID = 6582975657305031105L;

	public DiseaseTileComponent(DiseaseBurdenDto diseaseBurden) {
		setPadding(false);
		setSpacing(false);

		addTopLayout(
			diseaseBurden.getDisease(),
			diseaseBurden.getCaseCount(),
			diseaseBurden.getPreviousCaseCount(),
			diseaseBurden.getOutbreakDistrictCount() > 0);
		addStatsLayout(diseaseBurden.getCaseDeathCount(), diseaseBurden.getEventCount(), diseaseBurden.getLastReportedDistrictName());
	}

	private void addTopLayout(Disease disease, Long casesCount, Long previousCasesCount, boolean isOutbreak) {
		HorizontalLayout layout = new HorizontalLayout();
		layout.setPadding(false);
		layout.setSpacing(false);
		layout.addClassName(CssStyles.getDiseaseColor(disease));
		layout.setHeight(75, Unit.PIXELS);
		layout.setWidth(100, Unit.PERCENTAGE);

		VerticalLayout nameAndOutbreakLayout = new VerticalLayout();
		nameAndOutbreakLayout.setPadding(false);
		nameAndOutbreakLayout.setSpacing(false);
		nameAndOutbreakLayout.setHeight(100, Unit.PERCENTAGE);
		nameAndOutbreakLayout.setWidth(100, Unit.PERCENTAGE);

		HorizontalLayout nameLayout = new HorizontalLayout();
		nameLayout.setPadding(false);
		nameLayout.setSpacing(false);
		nameLayout.setWidth(100, Unit.PERCENTAGE);
		nameLayout.setHeight(100, Unit.PERCENTAGE);
		Span nameLabel = new Span(disease.toShortString());
		CssStyles.style(
			nameLabel,
			CssStyles.LABEL_WHITE,
			nameLabel.getText().length() > 12 ? CssStyles.LABEL_LARGE : CssStyles.LABEL_XLARGE,
			CssStyles.LABEL_BOLD,
			CssStyles.ALIGN_CENTER,
			CssStyles.LABEL_UPPERCASE);
		nameLayout.add(nameLabel);
		nameLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
		nameLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, nameLabel);
		nameAndOutbreakLayout.add(nameLayout);
		nameAndOutbreakLayout.setFlexGrow(1, nameLayout);

		if (isOutbreak) {
			HorizontalLayout outbreakLayout = new HorizontalLayout();
			outbreakLayout.setPadding(false);
			outbreakLayout.setSpacing(false);
			outbreakLayout.addClassName(CssStyles.BACKGROUND_CRITICAL);
			outbreakLayout.setWidth(100, Unit.PERCENTAGE);
			outbreakLayout.setHeight(30, Unit.PIXELS);
			Span outbreakLabel = new Span(I18nProperties.getCaption(Captions.dashboardOutbreak));
			CssStyles.style(outbreakLabel, CssStyles.LABEL_WHITE, CssStyles.ALIGN_CENTER, CssStyles.LABEL_UPPERCASE);
			outbreakLayout.add(outbreakLabel);
			outbreakLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
			outbreakLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, outbreakLabel);
			nameAndOutbreakLayout.add(outbreakLayout);
		}

		layout.add(nameAndOutbreakLayout);
		layout.setFlexGrow(1, nameAndOutbreakLayout);

		VerticalLayout countLayout = new VerticalLayout();
		countLayout.setPadding(false);
		countLayout.setSpacing(false);
		CssStyles.style(countLayout, CssStyles.getDiseaseColor(disease), CssStyles.BACKGROUND_DARKER);
		countLayout.setHeight(100, Unit.PERCENTAGE);
		countLayout.setWidth(100, Unit.PERCENTAGE);

		Span countLabel = new Span(casesCount.toString());
		CssStyles.style(countLabel, CssStyles.LABEL_WHITE, CssStyles.LABEL_BOLD, CssStyles.LABEL_XXXLARGE, CssStyles.ALIGN_CENTER, CssStyles.VSPACE_TOP_4);
		countLayout.add(countLabel);
		// was BOTTOM_CENTER in its slot
		countLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, countLabel);

		HorizontalLayout comparisonLayout = new HorizontalLayout();
		{
			comparisonLayout.setPadding(false);
			comparisonLayout.setSpacing(false);

			Span growthLabel = VaadinUiUtil.createLabel("", ContentMode.HTML);
			String chevronType;
			if (previousCasesCount < casesCount) {
				chevronType = HtmlRenderer.iconHtml(VaadinIcon.CHEVRON_UP);
			} else if (previousCasesCount > casesCount) {
				chevronType = HtmlRenderer.iconHtml(VaadinIcon.CHEVRON_DOWN);
			} else {
				chevronType = HtmlRenderer.iconHtml(VaadinIcon.CHEVRON_RIGHT);
			}
			VaadinUiUtil.setLabelValue(
				growthLabel,
				"<div class=\"v-label v-widget " + CssStyles.LABEL_WHITE + " v-label-" + CssStyles.LABEL_WHITE
					+ " align-center v-label-align-center bold v-label-bold v-has-width\" " + "	  style=\"margin-top: 3px;\">"
					+ "		<span class=\"v-icon\" style=\"font-family: VaadinIcons;\">" + chevronType + "		</span>" + "</div>",
				ContentMode.HTML);

			comparisonLayout.add(growthLabel);

			Span previousCountLabel = new Span(previousCasesCount.toString());
			CssStyles.style(previousCountLabel, CssStyles.LABEL_WHITE, CssStyles.LABEL_BOLD, CssStyles.LABEL_XLARGE, CssStyles.HSPACE_LEFT_4);
			comparisonLayout.add(previousCountLabel);
			comparisonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, growthLabel);
			comparisonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, previousCountLabel);
		}
		countLayout.add(comparisonLayout);
		countLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, comparisonLayout);

		countLayout.setFlexGrow(0.4f, countLabel);
		countLayout.setFlexGrow(0.6f, comparisonLayout);

		layout.add(countLayout);
		layout.setFlexGrow(0.65f, countLayout);

		add(layout);
	}

	private void addStatsLayout(Long fatalities, Long events, String district) {
		VerticalLayout layout = new VerticalLayout();
		layout.setWidth(100, Unit.PERCENTAGE);
		layout.setPadding(false);
		layout.setSpacing(false);
		layout.addClassName(CssStyles.BACKGROUND_HIGHLIGHT);

		StatsItem lastReportItem =
			new StatsItem.Builder(Captions.dashboardLastReport, district.length() == 0 ? I18nProperties.getString(Strings.none) : district)
				.singleColumn(true)
				.build();
		lastReportItem.addClassName(CssStyles.VSPACE_TOP_4);
		layout.add(lastReportItem);
		layout.add(new StatsItem.Builder(Captions.dashboardFatalities, fatalities).critical(fatalities > 0).build());
		StatsItem noOfEventsItem = new StatsItem.Builder(Captions.DiseaseBurden_eventCount, events).build();
		noOfEventsItem.addClassName(CssStyles.VSPACE_4);
		layout.add(noOfEventsItem);

		add(layout);
	}
}
