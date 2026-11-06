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

import java.util.Date;
import java.util.List;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.JurisdictionLevel;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.EpiWeek;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;
import de.symeda.sormas.ui.utils.AbstractView;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ComboBoxHelper;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.grid.Grid;
import de.symeda.sormas.ui.utils.grid.HeightMode;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

public class ReportsView extends AbstractView {

	private static final long serialVersionUID = -226852255434803180L;

	public static final String VIEW_NAME = "reports";

	private Grid<?> grid;
	private VerticalLayout gridLayout;
	private ComboBox yearFilter;
	private ComboBox epiWeekFilter;

	public ReportsView() {
		super(VIEW_NAME);

		JurisdictionLevel jurisdictionLevel = UiUtil.getJurisdictionLevel();
		if (jurisdictionLevel == JurisdictionLevel.NONE || jurisdictionLevel == JurisdictionLevel.NATION) {
			grid = new WeeklyReportRegionsGrid();
		} else {
			grid = new WeeklyReportOfficersGrid();
		}

		grid.setHeightMode(HeightMode.UNDEFINED);

		gridLayout = new VerticalLayout();
		gridLayout.add(createFilterBar());
		gridLayout.add(grid);
		gridLayout.setPadding(true);
		gridLayout.setSpacing(false);
		gridLayout.setSizeFull();
		gridLayout.setFlexGrow(1, grid);

		addComponent(gridLayout);
	}

	public HorizontalLayout createFilterBar() {
		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.setPadding(false);
		filterLayout.setSpacing(true);
		filterLayout.addClassName(CssStyles.VSPACE_3);
		filterLayout.setWidthFull();

		EpiWeek prevEpiWeek = DateHelper.getPreviousEpiWeek(new Date());
		int year = prevEpiWeek.getYear();
		int week = prevEpiWeek.getWeek();

		yearFilter = ComboBoxHelper.createComboBoxV7();
		yearFilter.setId(Strings.year);
		yearFilter.setWidth(200, Unit.PIXELS);
		yearFilter.setNullSelectionAllowed(false);
		yearFilter.addItems(DateHelper.getYearsToNow());
		yearFilter.setValue(year);
		yearFilter.setCaption(I18nProperties.getString(Strings.year));
		// was setItemCaptionMode(ID_TOSTRING): items without an explicit caption show toString()
		yearFilter.addValueChangeListener(e -> {
			updateEpiWeeks((int) e.getValue(), (int) epiWeekFilter.getValue());
			reloadGrid();
		});
		filterLayout.add(yearFilter);

		epiWeekFilter = ComboBoxHelper.createComboBoxV7();
		epiWeekFilter.setId(Strings.epiWeek);
		epiWeekFilter.setWidth(200, Unit.PIXELS);
		epiWeekFilter.setNullSelectionAllowed(false);
		updateEpiWeeks(year, week);
		epiWeekFilter.setCaption(I18nProperties.getString(Strings.epiWeek));
		epiWeekFilter.addValueChangeListener(e -> {
			reloadGrid();
		});
		filterLayout.add(epiWeekFilter);

		Button lastWeekButton = ButtonHelper.createButton(
			Captions.dashboardLastWeek,
			String.format(I18nProperties.getCaption(Captions.dashboardLastWeek), DateHelper.getPreviousEpiWeek(new Date()).toString()),
			e -> {
				EpiWeek epiWeek = DateHelper.getPreviousEpiWeek(new Date());
				yearFilter.setValue(epiWeek.getYear());
				epiWeekFilter.setValue(epiWeek.getWeek());
			},
			CssStyles.FORCE_CAPTION);

		filterLayout.add(lastWeekButton);

		Span infoLabel = VaadinUiUtil.createLabel(HtmlRenderer.iconHtml(VaadinIcon.INFO_CIRCLE), ContentMode.HTML);
		// was setDescription(info, ContentMode.HTML): the tooltip renders the HTML through Markdown
		Tooltip.forComponent(infoLabel).setMarkdown(I18nProperties.getString(Strings.infoWeeklyReportsView));
		infoLabel.setSizeUndefined();
		CssStyles.style(infoLabel, CssStyles.LABEL_XLARGE, CssStyles.LABEL_SECONDARY);
		filterLayout.add(infoLabel);
		// was Alignment.MIDDLE_RIGHT in an expanding slot: the icon sits at the right end (the tooltip stays on the icon)
		filterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, infoLabel);
		infoLabel.getStyle().set("margin-left", "auto");

		return filterLayout;
	}

	private void updateEpiWeeks(int year, int week) {
		List<EpiWeek> epiWeekList = DateHelper.createEpiWeekList(year);
		for (EpiWeek epiWeek : epiWeekList) {
			epiWeekFilter.addItem(epiWeek.getWeek());
			epiWeekFilter.setItemCaption(epiWeek.getWeek(), epiWeek.toString());
		}
		epiWeekFilter.setValue(week);
	}

	@Override
	public void enter(ViewChangeEvent event) {
		reloadGrid();
	}

	private void reloadGrid() {
		if (grid instanceof WeeklyReportRegionsGrid) {
			((WeeklyReportRegionsGrid) grid).reload((int) yearFilter.getValue(), (int) epiWeekFilter.getValue());
		} else {
			((WeeklyReportOfficersGrid) grid).reload(UiUtil.getUser().getRegion(), (int) yearFilter.getValue(), (int) epiWeekFilter.getValue());
		}
	}
}
