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

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.UI;
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
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.report.WeeklyReportOfficerSummaryDto;
import de.symeda.sormas.api.utils.EpiWeek;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.PercentageRenderer;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.grid.Grid;
import de.symeda.sormas.ui.utils.grid.HeightMode;
import de.symeda.sormas.ui.utils.grid.renderers.ConvertingHtmlRenderer;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;
import de.symeda.sormas.ui.utils.grid.renderers.TextRenderer;

/**
 * Was a Vaadin 7 container grid; the items are now set with {@link #setItems}.
 */
@SuppressWarnings("serial")
public class WeeklyReportOfficersGrid extends Grid<WeeklyReportOfficerSummaryDto> {

	private static final String VIEW_DETAILS_BTN_ID = "viewDetails";

	@SuppressWarnings("unused")
	private RegionReferenceDto region;
	private int week;
	private int year;

	/**
	 * Was the V7 cell style generator: the informant columns are shaded, the percentage is coloured by its value.
	 */
	private static String getCellStyle(String columnId, WeeklyReportOfficerSummaryDto summary) {
		String css;
		if (WeeklyReportOfficerSummaryDto.INFORMANTS.equals(columnId)
			|| WeeklyReportOfficerSummaryDto.INFORMANT_REPORTS.equals(columnId)
			|| WeeklyReportOfficerSummaryDto.INFORMANT_REPORT_PERCENTAGE.equals(columnId)
			|| WeeklyReportOfficerSummaryDto.INFORMANT_ZERO_REPORTS.equals(columnId)) {
			css = CssStyles.GRID_CELL_ODD;
		} else {
			css = "";
		}

		if (WeeklyReportOfficerSummaryDto.INFORMANT_REPORT_PERCENTAGE.equals(columnId)) {
			Integer reportPercentage = summary.getInformantReportPercentage();
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

	public WeeklyReportOfficersGrid() {
		super(WeeklyReportOfficerSummaryDto.class);
		setSizeFull();

		// was a generated container property with the icon's HTML (empty without informants)
		addColumn(summary -> summary.getInformants() > 0 ? HtmlRenderer.iconHtml(VaadinIcon.EYE) : "", new HtmlRenderer())
			.setKey(VIEW_DETAILS_BTN_ID);

		setColumns(
			VIEW_DETAILS_BTN_ID,
			WeeklyReportOfficerSummaryDto.OFFICER,
			WeeklyReportOfficerSummaryDto.DISTRICT,
			WeeklyReportOfficerSummaryDto.OFFICER_REPORT_DATE,
			WeeklyReportOfficerSummaryDto.TOTAL_CASE_COUNT,
			WeeklyReportOfficerSummaryDto.INFORMANTS,
			WeeklyReportOfficerSummaryDto.INFORMANT_REPORTS,
			WeeklyReportOfficerSummaryDto.INFORMANT_REPORT_PERCENTAGE,
			WeeklyReportOfficerSummaryDto.INFORMANT_ZERO_REPORTS);

		for (Column<WeeklyReportOfficerSummaryDto, ?> column : getAllColumns()) {
			if (column.getPropertyId().equals(VIEW_DETAILS_BTN_ID)) {
				column.setHeaderCaption("");
			} else {
				column.setHeaderCaption(
					I18nProperties
						.getPrefixCaption(WeeklyReportOfficerSummaryDto.I18N_PREFIX, column.getPropertyId().toString(), column.getHeaderCaption()));
			}
		}

		// was setStyleName on the header cells: part names in Flow
		HeaderRow headerRow = getDefaultHeaderRow();
		headerRow.getCell(getColumn(WeeklyReportOfficerSummaryDto.INFORMANTS)).setPartName(CssStyles.GRID_CELL_ODD);
		headerRow.getCell(getColumn(WeeklyReportOfficerSummaryDto.INFORMANT_REPORTS)).setPartName(CssStyles.GRID_CELL_ODD);
		headerRow.getCell(getColumn(WeeklyReportOfficerSummaryDto.INFORMANT_REPORT_PERCENTAGE)).setPartName(CssStyles.GRID_CELL_ODD);
		headerRow.getCell(getColumn(WeeklyReportOfficerSummaryDto.INFORMANT_ZERO_REPORTS)).setPartName(CssStyles.GRID_CELL_ODD);

		HeaderRow preHeaderRow = prependHeaderRow();
		HeaderCell preHeaderCell = preHeaderRow.join(
			getColumn(WeeklyReportOfficerSummaryDto.INFORMANTS),
			getColumn(WeeklyReportOfficerSummaryDto.INFORMANT_REPORTS),
			getColumn(WeeklyReportOfficerSummaryDto.INFORMANT_REPORT_PERCENTAGE),
			getColumn(WeeklyReportOfficerSummaryDto.INFORMANT_ZERO_REPORTS));
		// was setHtml
		preHeaderCell.setComponent(VaadinUiUtil.createLabel(I18nProperties.getCaption(Captions.weeklyReportOfficerInformants), ContentMode.HTML));
		preHeaderCell.setPartName(CssStyles.GRID_CELL_ODD);

		getColumn(VIEW_DETAILS_BTN_ID).setWidth(60);

		setV7ColumnRenderers();
		getColumn(WeeklyReportOfficerSummaryDto.INFORMANT_REPORT_PERCENTAGE).setRenderer(new PercentageRenderer());

		for (Column<WeeklyReportOfficerSummaryDto, ?> column : getAllColumns()) {
			String columnId = column.getKey();
			column.setStyleGenerator(summary -> getCellStyle(columnId, summary));
		}

		setSelectionMode(SelectionMode.NONE);
		addItemClickListener(this::itemClick);
	}

	/**
	 * The Vaadin 7 grid showed the property values through the session's default converters: references as their
	 * caption, numbers in the UI locale, the report date in the medium date-time format of the UI locale (V7
	 * {@code StringToDateConverter}) inside the HTML renderer.
	 */
	@SuppressWarnings("unchecked")
	private void setV7ColumnRenderers() {
		for (String columnId : new String[] {
			WeeklyReportOfficerSummaryDto.OFFICER,
			WeeklyReportOfficerSummaryDto.DISTRICT,
			WeeklyReportOfficerSummaryDto.TOTAL_CASE_COUNT,
			WeeklyReportOfficerSummaryDto.INFORMANTS,
			WeeklyReportOfficerSummaryDto.INFORMANT_REPORTS,
			WeeklyReportOfficerSummaryDto.INFORMANT_ZERO_REPORTS }) {
			((Column<WeeklyReportOfficerSummaryDto, Object>) getColumn(columnId)).setRenderer(ConvertingHtmlRenderer::toPresentation, new TextRenderer());
		}

		UI ui = UI.getCurrent();
		Locale locale = ui != null ? ui.getLocale() : Locale.getDefault();
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
		((Column<WeeklyReportOfficerSummaryDto, Date>) getColumn(WeeklyReportOfficerSummaryDto.OFFICER_REPORT_DATE)).setRenderer(
			date -> date != null ? dateFormat.format(date) : null,
			new HtmlRenderer(I18nProperties.getCaption(Captions.weeklyReportNoReport)));
	}

	public void reload(RegionReferenceDto region, int year, int week) {

		this.region = region;
		this.week = week;
		this.year = year;

		EpiWeek epiWeek = new EpiWeek(year, week);

		List<WeeklyReportOfficerSummaryDto> summaryDtos = FacadeProvider.getWeeklyReportFacade().getSummariesPerOfficer(region, epiWeek);
		setItems(summaryDtos);
	}

	public void itemClick(ItemClickEvent<WeeklyReportOfficerSummaryDto> event) {
		if (event.getColumn() != null && VIEW_DETAILS_BTN_ID.equals(event.getColumn().getKey())) {
			WeeklyReportOfficerSummaryDto summaryDto = event.getItem();
			if (summaryDto.getInformants() > 0) {
				VerticalLayout layout = new VerticalLayout();
				layout.setSizeUndefined();
				layout.setPadding(true);
				Dialog window = VaadinUiUtil.showPopupWindow(layout);

				WeeklyReportInformantsGrid grid = new WeeklyReportInformantsGrid(summaryDto.getOfficer(), new EpiWeek(year, week));
				grid.setWidth(960, Unit.PIXELS);
				grid.setHeightMode(HeightMode.ROW);
				grid.setHeightUndefined();
				layout.add(grid);
				window.setHeaderTitle(
					String.format(I18nProperties.getCaption(Captions.weeklyReportsInDistrict), summaryDto.getDistrict().buildCaption()) + " - "
						+ I18nProperties.getString(Strings.epiWeek) + " " + week + "/" + year);
			}
		}
	}
}
