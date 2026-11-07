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
package de.symeda.sormas.ui.reports;

import java.util.List;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.HeaderRow.HeaderCell;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.report.WeeklyReportRegionSummaryDto;
import de.symeda.sormas.api.utils.EpiWeek;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.PercentageRenderer;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.grid.Grid;
import de.symeda.sormas.ui.utils.grid.HeightMode;
import de.symeda.sormas.ui.utils.grid.renderers.ConvertingHtmlRenderer;
import de.symeda.sormas.ui.utils.grid.renderers.TextRenderer;

/**
 * Was a Vaadin 7 container grid; the items are now set with {@link #setItems}.
 */
@SuppressWarnings("serial")
public class WeeklyReportRegionsGrid extends Grid<WeeklyReportRegionSummaryDto> {

	private static final String VIEW_DETAILS_BTN_ID = "viewDetails";

	private int week;
	private int year;

	/**
	 * Was the V7 cell style generator: the officer columns are shaded, the percentages are coloured by their value.
	 */
	private static String getCellStyle(String columnId, WeeklyReportRegionSummaryDto summary) {

		String css;
		if (WeeklyReportRegionSummaryDto.OFFICERS.equals(columnId)
			|| WeeklyReportRegionSummaryDto.OFFICER_REPORTS.equals(columnId)
			|| WeeklyReportRegionSummaryDto.OFFICER_REPORT_PERCENTAGE.equals(columnId)
			|| WeeklyReportRegionSummaryDto.OFFICER_ZERO_REPORTS.equals(columnId)) {
			css = CssStyles.GRID_CELL_ODD;
		} else {
			css = "";
		}

		if (WeeklyReportRegionSummaryDto.INFORMANT_REPORT_PERCENTAGE.equals(columnId)
			|| WeeklyReportRegionSummaryDto.OFFICER_REPORT_PERCENTAGE.equals(columnId)) {
			Integer reportPercentage = WeeklyReportRegionSummaryDto.INFORMANT_REPORT_PERCENTAGE.equals(columnId)
				? summary.getInformantReportPercentage()
				: summary.getOfficerReportPercentage();
			if (reportPercentage >= 90) {
				css += " " + CssStyles.GRID_CELL_PRIORITY_LOW;
			} else if (reportPercentage >= 60) {
				css += " " + CssStyles.GRID_CELL_PRIORITY_NORMAL;
			} else {
				css += " " + CssStyles.GRID_CELL_PRIORITY_HIGH;
			}
		}
		return css.trim().isEmpty() ? null : css.trim();
	}

	@SuppressWarnings("unchecked")
	public WeeklyReportRegionsGrid() {
		super(WeeklyReportRegionSummaryDto.class);
		setSizeFull();

		// was a generated container property with the icon's HTML (V7 addIconColumn) and an HTML renderer
		VaadinUiUtil.addIconColumn(this, VIEW_DETAILS_BTN_ID, VaadinIcon.EYE);

		setColumns(
			VIEW_DETAILS_BTN_ID,
			WeeklyReportRegionSummaryDto.REGION,
			WeeklyReportRegionSummaryDto.OFFICERS,
			WeeklyReportRegionSummaryDto.OFFICER_REPORTS,
			WeeklyReportRegionSummaryDto.OFFICER_REPORT_PERCENTAGE,
			WeeklyReportRegionSummaryDto.OFFICER_ZERO_REPORTS,
			WeeklyReportRegionSummaryDto.INFORMANTS,
			WeeklyReportRegionSummaryDto.INFORMANT_REPORTS,
			WeeklyReportRegionSummaryDto.INFORMANT_REPORT_PERCENTAGE,
			WeeklyReportRegionSummaryDto.INFORMANT_ZERO_REPORTS);

		for (Column<WeeklyReportRegionSummaryDto, ?> column : getAllColumns()) {
			if (column.getPropertyId().equals(VIEW_DETAILS_BTN_ID)) {
				column.setHeaderCaption("");
				column.setSortable(false);
			} else {
				if (column.getPropertyId().equals(WeeklyReportRegionSummaryDto.REGION)) {
					// was the V7CaptionConverter: the region's caption
					((Column<WeeklyReportRegionSummaryDto, Object>) column).setRenderer(ConvertingHtmlRenderer::toPresentation, new TextRenderer());
				}
				column.setHeaderCaption(
					I18nProperties
						.getPrefixCaption(WeeklyReportRegionSummaryDto.I18N_PREFIX, column.getPropertyId().toString(), column.getHeaderCaption()));
			}
		}

		// was setStyleName on the header cells: part names in Flow
		HeaderRow headerRow = getDefaultHeaderRow();
		headerRow.getCell(getColumn(WeeklyReportRegionSummaryDto.OFFICERS)).setPartName(CssStyles.GRID_CELL_ODD);
		headerRow.getCell(getColumn(WeeklyReportRegionSummaryDto.OFFICER_REPORTS)).setPartName(CssStyles.GRID_CELL_ODD);
		headerRow.getCell(getColumn(WeeklyReportRegionSummaryDto.OFFICER_REPORT_PERCENTAGE)).setPartName(CssStyles.GRID_CELL_ODD);
		headerRow.getCell(getColumn(WeeklyReportRegionSummaryDto.OFFICER_ZERO_REPORTS)).setPartName(CssStyles.GRID_CELL_ODD);

		HeaderRow preHeaderRow = prependHeaderRow();
		HeaderCell preHeaderCell = preHeaderRow.join(
			getColumn(WeeklyReportRegionSummaryDto.OFFICERS),
			getColumn(WeeklyReportRegionSummaryDto.OFFICER_REPORTS),
			getColumn(WeeklyReportRegionSummaryDto.OFFICER_REPORT_PERCENTAGE),
			getColumn(WeeklyReportRegionSummaryDto.OFFICER_ZERO_REPORTS));
		// was setHtml
		preHeaderCell.setComponent(VaadinUiUtil.createLabel(I18nProperties.getCaption(Captions.weeklyReportRegionOfficers), ContentMode.HTML));
		preHeaderCell.setPartName(CssStyles.GRID_CELL_ODD);

		preHeaderRow
			.join(
				getColumn(WeeklyReportRegionSummaryDto.INFORMANTS),
				getColumn(WeeklyReportRegionSummaryDto.INFORMANT_REPORTS),
				getColumn(WeeklyReportRegionSummaryDto.INFORMANT_REPORT_PERCENTAGE),
				getColumn(WeeklyReportRegionSummaryDto.INFORMANT_ZERO_REPORTS))
			.setComponent(VaadinUiUtil.createLabel(I18nProperties.getCaption(Captions.weeklyReportRegionInformants), ContentMode.HTML));

		getColumn(VIEW_DETAILS_BTN_ID).setWidth(60);

		// the V7 grid showed the counts through the session's default converters (numbers in the UI locale)
		for (String columnId : new String[] {
			WeeklyReportRegionSummaryDto.OFFICERS,
			WeeklyReportRegionSummaryDto.OFFICER_REPORTS,
			WeeklyReportRegionSummaryDto.OFFICER_ZERO_REPORTS,
			WeeklyReportRegionSummaryDto.INFORMANTS,
			WeeklyReportRegionSummaryDto.INFORMANT_REPORTS,
			WeeklyReportRegionSummaryDto.INFORMANT_ZERO_REPORTS }) {
			((Column<WeeklyReportRegionSummaryDto, Object>) getColumn(columnId)).setRenderer(ConvertingHtmlRenderer::toPresentation, new TextRenderer());
		}
		getColumn(WeeklyReportRegionSummaryDto.OFFICER_REPORT_PERCENTAGE).setRenderer(new PercentageRenderer());
		getColumn(WeeklyReportRegionSummaryDto.INFORMANT_REPORT_PERCENTAGE).setRenderer(new PercentageRenderer());

		for (Column<WeeklyReportRegionSummaryDto, ?> column : getAllColumns()) {
			String columnId = column.getKey();
			column.setStyleGenerator(summary -> getCellStyle(columnId, summary));
		}

		setSelectionMode(SelectionMode.NONE);
		addItemClickListener(this::itemClick);
	}

	public void reload(int year, int week) {

		this.week = week;
		this.year = year;

		EpiWeek epiWeek = new EpiWeek(year, week);

		List<WeeklyReportRegionSummaryDto> summaryDtos = FacadeProvider.getWeeklyReportFacade().getSummariesPerRegion(epiWeek);
		setItems(summaryDtos);
	}

	public void itemClick(ItemClickEvent<WeeklyReportRegionSummaryDto> event) {
		if (event.getColumn() != null && VIEW_DETAILS_BTN_ID.equals(event.getColumn().getKey())) {
			WeeklyReportRegionSummaryDto summaryDto = event.getItem();
			VerticalLayout layout = new VerticalLayout();
			layout.setSizeUndefined();
			layout.setPadding(true);
			Dialog window = VaadinUiUtil.showPopupWindow(layout);

			WeeklyReportOfficersGrid grid = new WeeklyReportOfficersGrid();
			grid.reload(summaryDto.getRegion(), year, week);
			grid.setWidth(1600, Unit.PIXELS);
			grid.setHeightMode(HeightMode.ROW);
			grid.setHeightUndefined();
			layout.add(grid);
			window.setHeaderTitle(
				String.format(I18nProperties.getCaption(Captions.weeklyReportsInDistrict), summaryDto.getRegion().buildCaption()) + " - "
					+ I18nProperties.getString(Strings.epiWeek) + " " + week + "/" + year);
		}
	}
}
