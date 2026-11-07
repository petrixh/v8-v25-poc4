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
package de.symeda.sormas.ui.dashboard.surveillance;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.dashboard.DashboardDataProvider;
import de.symeda.sormas.ui.dashboard.surveillance.components.disease.DiseaseOverviewComponent;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.CustomLayout;
import de.symeda.sormas.ui.utils.LayoutUtil;
import de.symeda.sormas.ui.utils.fields.CheckBox;

@SuppressWarnings("serial")
public class SurveillanceOverviewLayout extends CustomLayout {

	private static final String BURDEN_LOC = "burden";
	private static final String DIFFERENCE_LOC = "difference";
	private static final String EXTEND_BUTTONS_LOC = "extendButtons";

	private final DashboardDataProvider dashboardDataProvider;

	private DiseaseOverviewComponent diseaseOverviewComponent;
	private CaseCountDifferenceComponent diseaseDifferenceComponent;
	private Button showMoreButton;
	private Button showLessButton;
	private CheckBox hideOverview;
	private Boolean isShowingAllDiseases;

	public SurveillanceOverviewLayout(DashboardDataProvider dashboardDataProvider) {
		// a Vaadin 8 CustomLayout was 100% wide by default; the Flow CustomLayout is as wide as its content
		setWidthFull();

		setTemplateContents(
			LayoutUtil.fluidRow(LayoutUtil.fluidColumnLoc(6, 0, 12, 0, BURDEN_LOC), LayoutUtil.fluidColumnLoc(6, 0, 12, 0, DIFFERENCE_LOC))
				+ LayoutUtil.loc(EXTEND_BUTTONS_LOC));

		this.dashboardDataProvider = dashboardDataProvider;

		diseaseDifferenceComponent = new CaseCountDifferenceComponent(dashboardDataProvider);

		addDiseaseBurdenView();

		addComponent(diseaseDifferenceComponent, DIFFERENCE_LOC);

		addShowMoreAndLessButtons();
	}

	private void addDiseaseBurdenView() {
		diseaseOverviewComponent = new DiseaseOverviewComponent();
		addComponent(diseaseOverviewComponent, BURDEN_LOC);

		if (UiUtil.hasRegionJurisdictionLevel())
			diseaseOverviewComponent.getShowTableViewButton().click();
	}

	private void addShowMoreAndLessButtons() {

		HorizontalLayout buttonsLayout = new HorizontalLayout();
		buttonsLayout.setHeight(null);
		buttonsLayout.setWidth(100, Unit.PERCENTAGE);
		// was a left and right margin only (MarginInfo); spacing details belong to the theme work
		buttonsLayout.setPadding(false);

		showMoreButton = ButtonHelper.createIconButton(
			Captions.dashboardShowAllDiseases,
			VaadinIcon.CHEVRON_DOWN,
			null,
			CssStyles.BUTTON_BORDERLESS,
			CssStyles.VSPACE_TOP_NONE,
			CssStyles.VSPACE_4);
		showLessButton = ButtonHelper.createIconButton(
			Captions.dashboardShowFirstDiseases,
			VaadinIcon.CHEVRON_UP,
			null,
			CssStyles.BUTTON_BORDERLESS,
			CssStyles.VSPACE_TOP_NONE,
			CssStyles.VSPACE_4);
		hideOverview = new CheckBox(I18nProperties.getCaption(Captions.dashboardHideOverview));
		hideOverview.setId("hideOverview");
		CssStyles.style(hideOverview, CssStyles.VSPACE_3);

		showMoreButton.addClickListener(e -> {
			isShowingAllDiseases = true;
			refresh();

			showMoreButton.setVisible(false);
			showLessButton.setVisible(true);
		});

		showLessButton.addClickListener(e -> {
			isShowingAllDiseases = false;
			refresh();

			showLessButton.setVisible(false);
			showMoreButton.setVisible(true);
		});

		hideOverview.addValueChangeListener(e -> {
			if (hideOverview.getValue()) {
				diseaseOverviewComponent.setVisible(false);
				diseaseDifferenceComponent.setVisible(false);
				showLessButton.setVisible(false);
				showMoreButton.setVisible(false);
			} else {
				diseaseOverviewComponent.setVisible(true);
				diseaseDifferenceComponent.setVisible(true);
				showLessButton.setVisible(isShowingAllDiseases);
				showMoreButton.setVisible(!isShowingAllDiseases);
			}
		});

		buttonsLayout.add(showMoreButton);
		buttonsLayout.add(showLessButton);
		// was BOTTOM_CENTER in a slot with expand ratio 1: the growing button shows its caption centred
		buttonsLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, showMoreButton);
		buttonsLayout.setFlexGrow(1, showMoreButton);
		buttonsLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, showLessButton);
		buttonsLayout.setFlexGrow(1, showLessButton);
		buttonsLayout.add(hideOverview);
		buttonsLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, hideOverview);
		buttonsLayout.setFlexGrow(0, hideOverview);
		// was BOTTOM_RIGHT: the check box stays at the right end also when both buttons are hidden
		buttonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

		addComponent(buttonsLayout, EXTEND_BUTTONS_LOC);

		isShowingAllDiseases = false;
		showLessButton.setVisible(false);
		buttonsLayout.setFlexGrow(1, showLessButton);
	}

	public void refresh() {
		diseaseOverviewComponent.refresh(dashboardDataProvider.getDiseasesBurden(), isShowingAllDiseases);
		diseaseDifferenceComponent.refresh(isShowingAllDiseases ? 0 : 10);
	}

	public void updateDifferenceComponentSubHeader() {
		diseaseDifferenceComponent.updateSubHeader();
	}
}
