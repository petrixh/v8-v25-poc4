/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2023 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.ui.dashboard.map;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import de.symeda.sormas.ui.utils.PopupButton;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.symeda.sormas.ui.utils.ContentMode;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.dashboard.BaseDashboardCriteria;
import de.symeda.sormas.api.geo.GeoLatLon;
import de.symeda.sormas.api.geo.GeoShapeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.dashboard.AbstractDashboardDataProvider;
import de.symeda.sormas.ui.dashboard.DashboardCssStyles;
import de.symeda.sormas.ui.map.LeafletMap;
import de.symeda.sormas.ui.map.MarkerIcon;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

public abstract class BaseDashboardMapComponent<C extends BaseDashboardCriteria<C>, P extends AbstractDashboardDataProvider<C>>
	extends VerticalLayout {

	private static final long serialVersionUID = -5711484213147987231L;
	protected final P dashboardDataProvider;
	protected LeafletMap map;
	// Layouts and components
	protected HorizontalLayout mapHeaderLayout;
	private final String headingStringTag;
	private final String headingInfoTag;
	private CssLayout overlayBackground;
	private VerticalLayout overlayLayout;
	private Span overlayMessageLabel;
	protected HorizontalLayout mapFooterLayout;
	protected boolean gisDashboard;
	private PopupButton legendDropdown;

	private Span headingInfoIcon;

	private Consumer<Boolean> externalExpandListener;

	public BaseDashboardMapComponent(String headingStringTag, P dashboardDataProvider, String headingInfoTag) {
		this(headingStringTag, dashboardDataProvider, headingInfoTag, false);
	}

	public BaseDashboardMapComponent(String headingStringTag, P dashboardDataProvider, String headingInfoTag, boolean gisDashboard) {
		this.gisDashboard = gisDashboard;
		this.headingStringTag = headingStringTag;
		this.dashboardDataProvider = dashboardDataProvider;
		this.headingInfoTag = headingInfoTag;

		setPadding(false);
		setSpacing(false);
		setSizeFull();

		this.setPadding(true);

		addComponents();
	}

	protected static HorizontalLayout buildMarkerLegendEntry(MarkerIcon icon, String labelCaption) {
		return buildLegendEntry(new Span(icon.getHtmlElement("16px"), ContentMode.HTML), labelCaption);
	}

	protected void refreshMap(boolean forced) {
		Date fromDate = dashboardDataProvider.getFromDate();
		Date toDate = dashboardDataProvider.getToDate();

		int maxDisplayCount = FacadeProvider.getConfigFacade().getDashboardMapMarkerLimit();
		Long count = 0L;
		if (!forced && maxDisplayCount >= 0) {
			count = getMarkerCount(fromDate, toDate, maxDisplayCount);
		}

		if (!forced && maxDisplayCount >= 0 && count > maxDisplayCount) {
			showMapOverlay(maxDisplayCount);
		} else {
			hideMapOverlay();

			loadMapData(fromDate, toDate);
			if (!gisDashboard) {
				// Re-create the map key layout to only show the keys for the selected layers
				legendDropdown.setContent(createLegend());
			}
		}
	}

	public void refreshMap() {
		refreshMap(false);
	}

	private void showMapOverlay(int maxCount) {
		overlayBackground.setVisible(true);
		overlayLayout.setVisible(true);
		overlayMessageLabel.setValue(String.format(I18nProperties.getString(Strings.warningDashboardMapTooManyMarkers), maxCount));
	}

	private void hideMapOverlay() {
		overlayBackground.setVisible(false);
		overlayLayout.setVisible(false);
	}

	protected abstract Long getMarkerCount(Date fromDate, Date toDate, int maxCount);

	protected abstract void loadMapData(Date fromDate, Date toDate);

	public void setExpandListener(Consumer<Boolean> listener) {
		externalExpandListener = listener;
	}

	private HorizontalLayout createHeader() {
		mapHeaderLayout = new HorizontalLayout();
		mapHeaderLayout.setWidth(100, Unit.PERCENTAGE);
		mapHeaderLayout.setSpacing(true);
		CssStyles.style(mapHeaderLayout, CssStyles.VSPACE_4);

		Span mapLabel = new Span(I18nProperties.getString(headingStringTag));

		mapLabel.setSizeUndefined();
		CssStyles.style(mapLabel, CssStyles.H2, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_NONE);

		Component mapLabelComponent = mapLabel;

		headingInfoIcon = new Span(VaadinIcon.INFO_CIRCLE.getHtml(), ContentMode.HTML);
		CssStyles.style(headingInfoIcon, CssStyles.H3, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_NONE, CssStyles.HSPACE_LEFT_4);

		if (headingInfoTag != null) {
			headingInfoIcon.setDescription(I18nProperties.getString(headingInfoTag));
			HorizontalLayout mapLabelLayout = new HorizontalLayout(mapLabel, headingInfoIcon);
			mapLabelLayout.setPadding(false);
			mapLabelLayout.setSpacing(false);

			mapLabelComponent = mapLabelLayout;
		}

		mapHeaderLayout.add(mapLabelComponent);
		mapHeaderLayout.setComponentAlignment(mapLabelComponent, Alignment.BOTTOM_LEFT);
		mapHeaderLayout.setFlexGrow(1, mapLabelComponent);

		// "Expand" and "Collapse" buttons
		Button expandMapButton =
			ButtonHelper.createIconButtonWithCaption("expandMap", "", VaadinIcon.EXPAND, null, CssStyles.BUTTON_SUBTLE, CssStyles.VSPACE_NONE);
		Button collapseMapButton =
			ButtonHelper.createIconButtonWithCaption("collapseMap", "", VaadinIcon.COMPRESS, null, CssStyles.BUTTON_SUBTLE, CssStyles.VSPACE_NONE);

		expandMapButton.addClickListener(e -> {
			externalExpandListener.accept(true);
			mapHeaderLayout.remove(expandMapButton);
			mapHeaderLayout.add(collapseMapButton);
			mapHeaderLayout.setComponentAlignment(collapseMapButton, Alignment.MIDDLE_RIGHT);
		});
		collapseMapButton.addClickListener(e -> {
			externalExpandListener.accept(false);
			mapHeaderLayout.remove(collapseMapButton);
			mapHeaderLayout.add(expandMapButton);
			mapHeaderLayout.setComponentAlignment(expandMapButton, Alignment.MIDDLE_RIGHT);
		});
		mapHeaderLayout.add(expandMapButton);
		mapHeaderLayout.setComponentAlignment(expandMapButton, Alignment.MIDDLE_RIGHT);

		return mapHeaderLayout;
	}

	private HorizontalLayout createFooter() {
		mapFooterLayout = new HorizontalLayout();
		mapFooterLayout.setWidth(100, Unit.PERCENTAGE);
		mapFooterLayout.setSpacing(true);
		CssStyles.style(mapFooterLayout, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_3);

		// Map key dropdown button
		legendDropdown = ButtonHelper.createPopupButton(Captions.dashboardMapKey, null, CssStyles.BUTTON_SUBTLE);
		legendDropdown.setContent(createLegend());

		mapFooterLayout.add(legendDropdown);
		mapFooterLayout.setComponentAlignment(legendDropdown, Alignment.MIDDLE_RIGHT);
		mapFooterLayout.setFlexGrow(1, legendDropdown);

		// Layers dropdown button
		VerticalLayout layersLayout = new VerticalLayout();
		{
			layersLayout.setPadding(true);
			layersLayout.setSpacing(false);
			layersLayout.setSizeUndefined();

			addLayerOptions(layersLayout);
		}

		if (layersLayout.getComponentCount() > 0) {
			PopupButton layersDropdown = ButtonHelper.createPopupButton(Captions.dashboardMapLayers, layersLayout, CssStyles.BUTTON_SUBTLE);

			mapFooterLayout.add(layersDropdown);
			mapFooterLayout.setComponentAlignment(layersDropdown, Alignment.MIDDLE_RIGHT);
		}

		return mapFooterLayout;
	}

	protected abstract void addLayerOptions(VerticalLayout layersLayout);

	private VerticalLayout createLegend() {
		VerticalLayout legendLayout = new VerticalLayout();
		legendLayout.setSpacing(false);
		legendLayout.setPadding(true);
		legendLayout.setSizeUndefined();

		List<Component> legendComponents = getLegendComponents();
		if (legendComponents.isEmpty()) {
			legendDropdown.setEnabled(false);
		} else {
			legendDropdown.setEnabled(true);
			legendLayout.addComponents(legendComponents.toArray(new Component[] {}));
		}

		return legendLayout;
	}

	protected abstract List<Component> getLegendComponents();

	protected abstract void onMarkerClicked(String groupId, int markerIndex);

	protected static HorizontalLayout buildLegendEntry(AbstractComponent icon, String labelCaption) {
		HorizontalLayout entry = new HorizontalLayout();
		entry.setSpacing(false);
		entry.setSizeUndefined();
		CssStyles.style(icon, CssStyles.HSPACE_RIGHT_4);
		entry.add(icon);
		Span label = new Span(labelCaption);
		label.setSizeUndefined();
		label.addClassName(ValoTheme.LABEL_SMALL);
		entry.add(label);
		return entry;
	}

	protected void addComponents() {
		map = new LeafletMap();
		map.setSizeFull();
		map.addMarkerClickListener(event -> onMarkerClicked(event.getGroupId(), event.getMarkerIndex()));

		{
			GeoShapeProvider geoShapeProvider = FacadeProvider.getGeoShapeProvider();

			final GeoLatLon mapCenter;
			// If map.usecountrycenter=true, use config coordinates. Else try to calculate the center of the user region/country
			if (FacadeProvider.getConfigFacade().isMapUseCountryCenter()) {
				mapCenter = FacadeProvider.getConfigFacade().getCountryCenter();
				map.setCenter(mapCenter);
			} else {
				UserDto user = UiUtil.getUser();
				if (user.getRegion() != null) {
					mapCenter = geoShapeProvider.getCenterOfRegion(user.getRegion());
				} else {
					mapCenter = geoShapeProvider.getCenterOfAllRegions();
				}

				GeoLatLon center = Optional.ofNullable(mapCenter).orElseGet(FacadeProvider.getConfigFacade()::getCountryCenter);
				map.setCenter(center);
			}

		}

		map.setZoom(FacadeProvider.getConfigFacade().getMapZoom());

		// Add components
		add(createHeader());

		CssLayout mapLayout = new CssLayout();
		mapLayout.setSizeFull();
		mapLayout.setClassName(DashboardCssStyles.MAP_CONTAINER);

		map.addClassName(DashboardCssStyles.MAP_COMPONENT);
		mapLayout.add(map);

		overlayBackground = new CssLayout();
		overlayBackground.setClassName(DashboardCssStyles.MAP_OVERLAY_BACKGROUND);
		overlayBackground.setVisible(false);
		mapLayout.add(overlayBackground);

		overlayMessageLabel = new Span();
		overlayMessageLabel.addClassNames(CssStyles.ALIGN_CENTER, CssStyles.LABEL_WHITE, CssStyles.LABEL_WHITE_SPACE_NORMAL);

		Button button = ButtonHelper.createButton(Captions.showPlacesOnMap, (e) -> refreshMap(true));

		overlayLayout = new VerticalLayout(overlayMessageLabel, button);
		overlayLayout.setClassName(DashboardCssStyles.MAP_OVERLAY);
		overlayLayout.setHeightFull();
		overlayLayout.setComponentAlignment(overlayMessageLabel, Alignment.MIDDLE_CENTER);
		overlayLayout.setFlexGrow(0, overlayMessageLabel);
		overlayLayout.setComponentAlignment(button, Alignment.MIDDLE_CENTER);
		overlayLayout.setFlexGrow(0, button);
		overlayLayout.setVisible(false);
		mapLayout.add(overlayLayout);

		if (gisDashboard) {
			mapLayout.add(createFooter());
			mapFooterLayout.setWidthUndefined();
			CssStyles.style(mapFooterLayout, CssStyles.GIS_DASHBOARD_MAP_FOOTER);
		}

		add(mapLayout);
		setFlexGrow(1, mapLayout);

		if (!gisDashboard) {
			add(createFooter());
		}
	}

	public void appendHeadingInfo(String description) {
		headingInfoIcon.setDescription(headingInfoIcon.getDescription() + description);
	}
}
