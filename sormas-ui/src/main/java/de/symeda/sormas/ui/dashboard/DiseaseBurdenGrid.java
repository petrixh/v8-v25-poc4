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
package de.symeda.sormas.ui.dashboard;

import java.util.List;

import com.vaadin.flow.component.icon.VaadinIcon;

import de.symeda.sormas.api.disease.DiseaseBurdenDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.PercentageRenderer;
import de.symeda.sormas.ui.utils.grid.Grid;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

@SuppressWarnings("serial")
public class DiseaseBurdenGrid extends Grid<DiseaseBurdenDto> {

	private static final String VIEW_DETAILS_BTN_ID = "viewDetails";

	@SuppressWarnings("unchecked")
	public DiseaseBurdenGrid() {

		super(DiseaseBurdenDto.class);
		setSizeFull();

		setColumns(/* VIEW_DETAILS_BTN_ID, */
			DiseaseBurdenDto.DISEASE,
			DiseaseBurdenDto.CASE_COUNT,
			DiseaseBurdenDto.PREVIOUS_CASE_COUNT,
			DiseaseBurdenDto.CASES_DIFFERENCE_PERCENTAGE,
			DiseaseBurdenDto.EVENT_COUNT,
			DiseaseBurdenDto.OUTBREAK_DISTRICT_COUNT,
			DiseaseBurdenDto.CASE_DEATH_COUNT,
			DiseaseBurdenDto.CASE_FATALITY_RATE);

		for (Column<DiseaseBurdenDto, ?> column : getAllColumns()) {
			if (column.getPropertyId().equals(VIEW_DETAILS_BTN_ID)) {
				column.setHeaderCaption("");
			} else {
				column.setHeaderCaption(
					I18nProperties.getPrefixCaption(DiseaseBurdenDto.I18N_PREFIX, column.getPropertyId().toString(), column.getHeaderCaption()));
			}
		}

		getColumn(DiseaseBurdenDto.CASES_DIFFERENCE_PERCENTAGE)
			.setHeaderCaption(I18nProperties.getPrefixCaption(DiseaseBurdenDto.I18N_PREFIX, DiseaseBurdenDto.CASES_DIFFERENCE));

		// format columns
		getColumn(DiseaseBurdenDto.CASE_FATALITY_RATE).setRenderer(new PercentageRenderer());

		// format casesGrowth column with chevrons
		// was setConverter(new StringToFloatConverter() {..}).setRenderer(new HtmlRenderer()); the converter's own text was not used
		((Column<DiseaseBurdenDto, Float>) getColumn(DiseaseBurdenDto.CASES_DIFFERENCE_PERCENTAGE)).setRenderer(value -> {

			String stringRepresentation;
			String chevronType = "";
			String criticalLevel = "";

			if (value > 0) {
				chevronType = HtmlRenderer.iconHtml(VaadinIcon.CHEVRON_UP);
				criticalLevel = CssStyles.LABEL_CRITICAL;
			} else if (value < 0) {
				chevronType = HtmlRenderer.iconHtml(VaadinIcon.CHEVRON_DOWN);
				criticalLevel = CssStyles.LABEL_POSITIVE;
			} else {
				chevronType = HtmlRenderer.iconHtml(VaadinIcon.CHEVRON_RIGHT);
				criticalLevel = CssStyles.LABEL_IMPORTANT;
			}

			String strValue = "" + Math.abs(value);
			if (strValue.equals("100.0"))
				strValue = "100";

			//@formatter:off
			stringRepresentation =
				  "<div style=\"width:100%\">"
				+	"<div class=\"\" style=\"display: inline-block;margin-top: 2px;width: 70%;text-align:left;\">" + strValue + "%" + "</div>"
				+	"<div class=\"v-label v-widget " + criticalLevel + " v-label-" + criticalLevel
				+		" align-center v-label-align-center bold v-label-bold large v-label-large v-has-width\" "
				+		" style=\"width: 15px;width: 30%;text-align: left;\">"
				+		"<span class=\"v-icon\" style=\"font-family: VaadinIcons;\">" + chevronType + "</span>" 
				+ 	"</div>"
				+ "</div>";
			//@formatter:on

			return stringRepresentation;
		}, new HtmlRenderer());

		setSelectionMode(SelectionMode.NONE);
	}

	public void reload(List<DiseaseBurdenDto> items) {
		setItems(items);
	}
}
