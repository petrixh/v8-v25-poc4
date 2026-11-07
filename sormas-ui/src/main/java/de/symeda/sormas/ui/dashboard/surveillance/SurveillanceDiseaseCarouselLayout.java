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

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.SormasUI;
import de.symeda.sormas.ui.SubMenu;
import de.symeda.sormas.ui.dashboard.DashboardCssStyles;
import de.symeda.sormas.ui.dashboard.DashboardDataProvider;
import de.symeda.sormas.ui.dashboard.map.DashboardMapComponent;
import de.symeda.sormas.ui.dashboard.surveillance.components.epicurve.SurveillanceEpiCurveComponent;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.fields.CheckBox;

@SuppressWarnings("serial")
public class SurveillanceDiseaseCarouselLayout extends VerticalLayout {

	private DashboardDataProvider dashboardDataProvider;

	private DiseaseStatisticsComponent statisticsComponent;
	private SurveillanceEpiCurveComponent epiCurveComponent;
	private DashboardMapComponent mapComponent;
	private Consumer<Boolean> externalExpandListener;
	private SubMenu carouselMenu;
	private List<Disease> diseases;
	private Registration pollRegistration;

	public SurveillanceDiseaseCarouselLayout(DashboardDataProvider dashboardDataProvider) {
		this.dashboardDataProvider = dashboardDataProvider;

		statisticsComponent = new DiseaseStatisticsComponent(dashboardDataProvider);

		epiCurveComponent = new SurveillanceEpiCurveComponent(dashboardDataProvider);
		mapComponent = new DashboardMapComponent(dashboardDataProvider);
		diseases = FacadeProvider.getDiseaseConfigurationFacade().getAllDiseases(true, true, true);
		//remove 'other' and 'undefined' if present
		diseases.removeAll(Arrays.asList(Disease.OTHER, Disease.UNDEFINED));

		this.initLayout();
	}

	public void setExpandListener(Consumer<Boolean> listener) {
		externalExpandListener = listener;
	}

	private void initLayout() {
		addClassName(DashboardCssStyles.CURVE_AND_MAP_LAYOUT);
		setWidth(100, Unit.PERCENTAGE);
		setHeight(null);
		setPadding(false);
		setSpacing(false);

		HorizontalLayout carouselMenuLayout = createCarouselMenuLayout();
		add(carouselMenuLayout);
		setFlexGrow(0, carouselMenuLayout);

		add(statisticsComponent);
		statisticsComponent.addClassName(DashboardCssStyles.HIGHLIGHTED_STATISTICS_COMPONENT);
		setFlexGrow(0, statisticsComponent);

		HorizontalLayout epiCurveAndMapLayout = createEpiCurveAndMapLayout();
		add(epiCurveAndMapLayout);
		setFlexGrow(1, epiCurveAndMapLayout);
	}

	private HorizontalLayout createCarouselMenuLayout() {
		HorizontalLayout layout = new HorizontalLayout();
		layout.setPadding(false);
		CssStyles.style(layout, CssStyles.HSPACE_LEFT_2);

		CheckBox autoSlide = this.setupSlideShow();
		layout.add(autoSlide);
		layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, autoSlide);
		// keeps its natural width next to the full-width tab bar, as in Vaadin 8 (the caption does not wrap)
		layout.setFlexShrink(0, autoSlide);

		carouselMenu = new SubMenu();

		for (Disease disease : diseases) {
			carouselMenu.addView(disease.getName(), disease.toShortString(), () -> {
				this.changeSelectedDisease(disease, true);

				return true;
			});
		}

		if (diseases.size() > 0) {
			this.setActiveDisease(diseases.get(0), false);
		}

		layout.add(carouselMenu);

		return layout;
	}

	private HorizontalLayout createEpiCurveAndMapLayout() {
		HorizontalLayout epiCurveAndMapLayout = new HorizontalLayout();
		epiCurveAndMapLayout.setWidth(100, Unit.PERCENTAGE);
		final int BASE_HEIGHT = 480;
		epiCurveAndMapLayout.setHeight(BASE_HEIGHT, Unit.PIXELS);

		epiCurveAndMapLayout.add(epiCurveComponent);
		epiCurveAndMapLayout.add(mapComponent);

		epiCurveComponent.setExpandListener(expanded -> {
			if (expanded) {
				remove(statisticsComponent);
				epiCurveAndMapLayout.remove(mapComponent);
				epiCurveAndMapLayout.setHeight(100, Unit.PERCENTAGE);
				setHeight(100, Unit.PERCENTAGE);
			} else {
				addComponentAtIndex(1, statisticsComponent);
				epiCurveAndMapLayout.addComponentAtIndex(1, mapComponent);
				mapComponent.refreshMap();
				epiCurveAndMapLayout.setHeight(BASE_HEIGHT, Unit.PIXELS);
				setHeight(null);
			}
			if (externalExpandListener != null) {
				externalExpandListener.accept(expanded);
			}
		});

		mapComponent.setExpandListener(expanded -> {
			if (expanded) {
				remove(statisticsComponent);
				epiCurveAndMapLayout.remove(epiCurveComponent);
				epiCurveAndMapLayout.setHeight(100, Unit.PERCENTAGE);
				setHeight(100, Unit.PERCENTAGE);
			} else {
				addComponentAtIndex(1, statisticsComponent);
				epiCurveAndMapLayout.addComponentAtIndex(0, epiCurveComponent);
				epiCurveAndMapLayout.setHeight(BASE_HEIGHT, Unit.PIXELS);
				setHeight(null);
			}
			if (externalExpandListener != null) {
				externalExpandListener.accept(expanded);
			}
		});

		return epiCurveAndMapLayout;
	}

	private CheckBox setupSlideShow() {
		// slideshow option
		CheckBox autoSlide = new CheckBox(I18nProperties.getCaption(Captions.dashboardDiseaseCarouselSlideShow));
		autoSlide.addValueChangeListener(e -> {
			this.changeAutoSlideOption(autoSlide.getValue());
		});

		// enabled by default
		autoSlide.setValue(false);

		return autoSlide;
	}

	private void setActiveDisease(Disease selectedDisease, boolean doRefresh) {
		carouselMenu.setActiveView(selectedDisease.getName());
		this.changeSelectedDisease(selectedDisease, doRefresh);
	}

	private void changeSelectedDisease(Disease disease, boolean doRefresh) {
		this.dashboardDataProvider.setDisease(disease);
		if (doRefresh) {
			this.dashboardDataProvider.refreshDataForSelectedDisease();
			refresh();
		}
	}

	@Override
	protected void onDetach(DetachEvent detachEvent) {
		super.onDetach(detachEvent);

		// deactivate polling
		changeAutoSlideOption(false);
	}

	private void changeAutoSlideOption(boolean isActivated) {

		if (isActivated) {
			SormasUI.getCurrent().setPollInterval(1000 * 90);

			// set timer for slideshow
			if (pollRegistration == null) {
				pollRegistration = SormasUI.getCurrent().addPollListener(e -> {
					Disease selectedDisease = dashboardDataProvider.getDisease();
					int nextDiseaseIndex = 0;

					if (selectedDisease != null) {
						nextDiseaseIndex = diseases.indexOf(selectedDisease) + 1;

						if (nextDiseaseIndex >= diseases.size()) {
							nextDiseaseIndex = 0;
						}
					}

					this.setActiveDisease(diseases.get(nextDiseaseIndex), true);
				});
			}
		} else {
			SormasUI.getCurrent().setPollInterval(-1);

			if (pollRegistration != null) {
				pollRegistration.remove();
				pollRegistration = null;
			}
		}
	}

	public void refresh() {
		this.statisticsComponent.refresh();
		this.epiCurveComponent.clearAndFillEpiCurveChart();
		this.mapComponent.refreshMap();
	}
}
