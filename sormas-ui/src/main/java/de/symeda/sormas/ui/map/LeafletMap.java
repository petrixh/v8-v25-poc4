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
package de.symeda.sormas.ui.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.map.configuration.Coordinate;
import com.vaadin.flow.component.map.configuration.Feature;
import com.vaadin.flow.component.map.configuration.feature.MarkerFeature;
import com.vaadin.flow.component.map.configuration.feature.PolygonFeature;
import com.vaadin.flow.component.map.configuration.layer.FeatureLayer;
import com.vaadin.flow.component.map.configuration.layer.TileLayer;
import com.vaadin.flow.component.map.configuration.source.XYZSource;
import com.vaadin.flow.component.map.configuration.style.Fill;
import com.vaadin.flow.component.map.configuration.style.Icon;
import com.vaadin.flow.component.map.configuration.style.Stroke;
import com.vaadin.flow.component.map.configuration.style.Style;
import com.vaadin.flow.component.map.configuration.style.TextStyle;
import com.vaadin.flow.shared.Registration;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.geo.GeoLatLon;

/**
 * The SORMAS map: a tile layer, marker groups and polygon groups, drawn by Vaadin Map (OpenLayers).
 * <p>
 * Until Vaadin 8 this was a JavaScript component around Leaflet 1.3.4 with the marker cluster and full-screen plugins. The API
 * of that component is kept: markers and polygons are added in groups ({@link #addMarkerGroup}, {@link #addPolygonGroup}),
 * removed by group ({@link #removeGroup}), and a click on a marker or polygon fires a {@link MarkerClickEvent} with the group id
 * and the index of the marker or polygon in its group.
 * <p>
 * How the Leaflet features are drawn now:
 * <ul>
 * <li>Markers are images ({@link LeafletMapIcons}); a marker that stands for more than one item shows the count.</li>
 * <li>Clusters: markers that are less than 15 pixels apart are drawn as one cluster with the number of markers, as before.
 * A cluster shows the marker of its kind (e.g. confirmed cases); markers of different kinds are clustered separately (Vaadin
 * Map has one cluster style per layer). A click on a cluster zooms to its markers. At the highest zoom level the markers are
 * not clustered (Leaflet spread them out on click instead).</li>
 * <li>Polygons: the Leaflet path options string ({@link LeafletPolygon#setOptions}: stroke, color, weight, opacity, fill,
 * fillColor, fillOpacity) sets the stroke and fill. The polygon caption is not shown (Leaflet showed it as a tooltip on
 * hover; Vaadin Map has no hover tooltips).</li>
 * <li>Full screen: a button in the top right corner, as the Leaflet full-screen control.</li>
 * </ul>
 */
@StyleSheet("context://VAADIN/map/MarkerCluster.css") // marker legends of the dashboards (MarkerIcon.getHtmlElement)
public class LeafletMap extends Composite<Div> implements HasSize, HasStyle {

	private static final long serialVersionUID = 1671451734103288729L;

	private static final String DEFAULT_TILES_URL = "https://{a-c}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png";
	private static final String DEFAULT_TILES_ATTRIBUTION =
		"&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors. Tiles courtesy of Humanitarian OpenStreetMap Team";
	/** Leaflet's default maximum zoom of a tile layer */
	private static final int MAX_ZOOM = 18;
	/** Markers are clustered up to this zoom level, above it they are shown one by one */
	private static final int MAX_CLUSTER_ZOOM = MAX_ZOOM - 1;
	/** Leaflet marker cluster maxClusterRadius */
	private static final int CLUSTER_DISTANCE = 15;
	private static final int CLUSTER_ICON_SIZE = 25;
	/** Web Mercator cannot show the poles; Leaflet clamps latitudes the same way */
	private static final double MAX_LATITUDE = 85.0511287798;

	private static final int POLYGON_Z_INDEX = 10;
	private static final int MARKER_Z_INDEX = 20;

	private static int currMapId = 0;

	public static int nextMapId() {
		return ++currMapId;
	}

	private int mapId = nextMapId();

	private final com.vaadin.flow.component.map.Map map = new com.vaadin.flow.component.map.Map();
	private final TileLayer tileLayer = new TileLayer();
	private String tilesUrl = DEFAULT_TILES_URL;
	private final List<String> attributions = new ArrayList<>();

	private final Map<String, List<FeatureLayer>> groupLayers = new HashMap<>();
	private final Map<Feature, MarkerRef> featureRefs = new IdentityHashMap<>();
	private final Map<String, Icon> icons = new HashMap<>();

	/**
	 * Creates the map object.
	 */
	public LeafletMap() {
		Div content = getContent();
		setId(getDomId());
		content.getStyle().set("position", "relative");

		map.setSizeFull();
		// Leaflet map background where no tiles are shown (theme components/leaflet.scss)
		map.getStyle().set("background-color", "#555");
		map.getView().setMaxZoom(MAX_ZOOM);
		map.setZoom(5);

		attributions.add(DEFAULT_TILES_ATTRIBUTION);
		String configuredTilesUrl = FacadeProvider.getConfigFacade().getMapTilersUrl();
		if (StringUtils.isNoneBlank(configuredTilesUrl)) {
			setTileLayer(configuredTilesUrl, FacadeProvider.getConfigFacade().getMapTilersAttribution());
		} else {
			updateTileSource();
		}
		map.setBackgroundLayer(tileLayer);
		tileLayer.setVisible(true);
		tileLayer.setOpacity(1);

		map.addFeatureClickListener(event -> {
			MarkerRef ref = featureRefs.get(event.getFeature());
			if (ref != null) {
				fireEvent(new MarkerClickEvent(this, ref.groupId, ref.index));
			}
		});
		// Leaflet marker cluster: a click on a cluster zooms to its markers
		map.addClusterClickListener(event -> map.zoomToFit(event.getFeatures()));

		content.add(map, createFullscreenButton());

		// credit where credit's due
		String attribution = FacadeProvider.getGeoShapeProvider().loadShapefileAttributions();
		this.addShapefileAttribution(attribution);
	}

	private Button createFullscreenButton() {
		Button button = new Button(VaadinIcon.EXPAND_SQUARE.create());
		button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
		button.setTooltipText("Full Screen");
		button.getStyle()
			.set("position", "absolute")
			.set("top", "10px")
			.set("right", "10px")
			.set("z-index", "1")
			.set("background-color", "white");
		// the map's container goes full screen and back, as with the Leaflet full-screen control
		button.addClickListener(
			e -> getElement().executeJs("if (document.fullscreenElement === this) { document.exitFullscreen(); } else { this.requestFullscreen(); }"));
		return button;
	}

	/**
	 * @return the DOM ID of the map component
	 */
	public String getDomId() {
		return "leaflet_" + mapId;
	}

	/**
	 * Uses another tile server. <code>layerUri</code> is a Leaflet tile URL template, e.g.
	 * <code>https://{s}.tile.example.org/{z}/{x}/{y}.png</code>; <code>{s}</code> stands for the subdomains a, b and c.
	 */
	private void setTileLayer(String layerUri, String attribution) {
		tilesUrl = layerUri.replace("{s}", "{a-c}").replace("{r}", "");
		attributions.set(0, StringUtils.defaultString(attribution));
		updateTileSource();
	}

	private void updateTileSource() {
		XYZSource.Options options = new XYZSource.Options();
		options.setUrl(tilesUrl);
		List<String> nonEmptyAttributions = new ArrayList<>();
		attributions.stream().filter(StringUtils::isNotBlank).forEach(nonEmptyAttributions::add);
		options.setAttributions(nonEmptyAttributions);
		options.setAttributionsCollapsible(false);
		tileLayer.setSource(new XYZSource(options));
	}

	public void setZoom(int zoom) {
		map.setZoom(zoom);
	}

	public int getZoom() {
		return (int) Math.round(map.getZoom());
	}

	public void setCenter(GeoLatLon coordinates) {
		map.setCenter(new Coordinate(coordinates.getLon(), coordinates.getLat()));
	}

	public void setTileLayerVisible(boolean tileLayerVisible) {
		tileLayer.setVisible(tileLayerVisible);
	}

	public void setTileLayerOpacity(float tileLayerOpacity) {
		tileLayer.setOpacity(tileLayerOpacity);
	}

	public void addMarkerGroup(String groupId, List<LeafletMarker> markers) {

		// one cluster layer per kind of marker; facilities are clustered with the cases of the same classification
		Map<MarkerIcon, List<Integer>> markerIndexesByClusterIcon = new LinkedHashMap<>();
		for (int i = 0; i < markers.size(); i++) {
			markerIndexesByClusterIcon.computeIfAbsent(clusterIcon(markers.get(i).getIcon()), k -> new ArrayList<>()).add(i);
		}

		List<FeatureLayer> layers = groupLayers.computeIfAbsent(groupId, k -> new ArrayList<>());
		markerIndexesByClusterIcon.forEach((clusterIcon, indexes) -> {
			FeatureLayer clustered = new FeatureLayer();
			clustered.setClusteringEnabled(true);
			clustered.setClusterDistance(CLUSTER_DISTANCE);
			clustered.setClusterStyle(createClusterStyle(clusterIcon));
			clustered.setMaxZoom((double) MAX_CLUSTER_ZOOM);

			FeatureLayer single = new FeatureLayer();
			single.setMinZoom((double) MAX_CLUSTER_ZOOM);

			for (FeatureLayer layer : new FeatureLayer[] {
				clustered,
				single }) {
				layer.setzIndex(MARKER_Z_INDEX);
				for (int index : indexes) {
					MarkerFeature feature = createMarkerFeature(markers.get(index));
					layer.addFeature(feature);
					featureRefs.put(feature, new MarkerRef(groupId, index));
				}
				map.addLayer(layer);
				layers.add(layer);
			}
		});
	}

	private static MarkerIcon clusterIcon(MarkerIcon icon) {
		switch (icon) {
		case FACILITY_CONFIRMED:
			return MarkerIcon.CASE_CONFIRMED;
		case FACILITY_SUSPECT:
			return MarkerIcon.CASE_SUSPECT;
		case FACILITY_PROBABLE:
			return MarkerIcon.CASE_PROBABLE;
		case FACILITY_UNCLASSIFIED:
			return MarkerIcon.CASE_UNCLASSIFIED;
		default:
			return icon;
		}
	}

	private MarkerFeature createMarkerFeature(LeafletMarker marker) {
		int count = marker.getMarkerCount();
		MarkerFeature feature = new MarkerFeature(
			new Coordinate(roundCoordinate(marker.getLongitude()), clampLatitude(roundCoordinate(marker.getLatitude()))),
			getIcon(marker.getIcon(), LeafletMapIcons.size(count)));
		if (count > 1) {
			feature.setText(String.valueOf(count));
			feature.setTextStyle(createCountTextStyle());
		}
		return feature;
	}

	private Icon getIcon(MarkerIcon markerIcon, int size) {
		return icons.computeIfAbsent(markerIcon.name() + "-" + size, k -> LeafletMapIcons.createIcon(markerIcon, size));
	}

	private Style createClusterStyle(MarkerIcon clusterIcon) {
		Style style = new Style();
		style.setImage(LeafletMapIcons.createIcon(clusterIcon, CLUSTER_ICON_SIZE));
		style.setTextStyle(createCountTextStyle());
		return style;
	}

	private static TextStyle createCountTextStyle() {
		// MarkerCluster.css: .marker span
		TextStyle textStyle = new TextStyle();
		textStyle.setFont("12px \"Helvetica Neue\", Arial, Helvetica, sans-serif");
		textStyle.setFill("#000");
		textStyle.setStroke("rgba(0,0,0,0)", 0);
		textStyle.setTextAlign(TextStyle.TextAlign.CENTER);
		textStyle.setTextBaseline(TextStyle.TextBaseline.MIDDLE);
		return textStyle;
	}

	public void addPolygonGroup(String groupId, List<LeafletPolygon> polygons) {
		FeatureLayer layer = new FeatureLayer();
		layer.setzIndex(POLYGON_Z_INDEX);
		for (int i = 0; i < polygons.size(); i++) {
			PolygonFeature feature = createPolygonFeature(polygons.get(i));
			layer.addFeature(feature);
			featureRefs.put(feature, new MarkerRef(groupId, i));
		}
		map.addLayer(layer);
		groupLayers.computeIfAbsent(groupId, k -> new ArrayList<>()).add(layer);
	}

	private static PolygonFeature createPolygonFeature(LeafletPolygon polygon) {
		List<Coordinate[]> rings = new ArrayList<>();
		rings.add(toRing(polygon.getLatLons()));
		if (polygon.getHoleLatLons() != null) {
			for (double[][] hole : polygon.getHoleLatLons()) {
				rings.add(toRing(hole));
			}
		}
		PolygonFeature feature = new PolygonFeature();
		feature.setCoordinates(rings.toArray(new Coordinate[0][]));
		feature.setStyle(createPolygonStyle(polygon.getOptions()));
		return feature;
	}

	private static Coordinate[] toRing(double[][] latLons) {
		List<Coordinate> ring = new ArrayList<>();
		for (double[] latLon : latLons) {
			ring.add(new Coordinate(roundCoordinate(latLon[1]), clampLatitude(roundCoordinate(latLon[0]))));
		}
		// Leaflet closes the ring itself
		if (!ring.isEmpty()) {
			Coordinate first = ring.get(0);
			Coordinate last = ring.get(ring.size() - 1);
			if (first.getX() != last.getX() || first.getY() != last.getY()) {
				ring.add(new Coordinate(first.getX(), first.getY()));
			}
		}
		return ring.toArray(new Coordinate[0]);
	}

	/**
	 * lat & lon is less precise to save bandwidth (as in Vaadin 8)
	 */
	private static double roundCoordinate(double value) {
		return (int) (value * 10000.0) / 10000.0;
	}

	private static double clampLatitude(double latitude) {
		return Math.max(-MAX_LATITUDE, Math.min(MAX_LATITUDE, latitude));
	}

	private static final Pattern OPTION_PATTERN = Pattern.compile("[\"']?(\\w+)[\"']?\\s*:\\s*(?:'([^']*)'|\"([^\"]*)\"|([^,}\\s]+))");

	/**
	 * Stroke and fill from the Leaflet path options of a polygon (JSON with single or double quotes), with Leaflet's defaults.
	 */
	static Style createPolygonStyle(String leafletOptions) {
		Map<String, String> options = new HashMap<>();
		if (leafletOptions != null) {
			Matcher matcher = OPTION_PATTERN.matcher(leafletOptions);
			while (matcher.find()) {
				String value = matcher.group(2) != null ? matcher.group(2) : matcher.group(3) != null ? matcher.group(3) : matcher.group(4);
				options.put(matcher.group(1), value);
			}
		}
		boolean stroke = Boolean.parseBoolean(options.getOrDefault("stroke", "true"));
		String color = options.getOrDefault("color", "#3388ff");
		double weight = Double.parseDouble(options.getOrDefault("weight", "3"));
		double opacity = Double.parseDouble(options.getOrDefault("opacity", "1"));
		boolean fill = Boolean.parseBoolean(options.getOrDefault("fill", "true"));
		String fillColor = options.getOrDefault("fillColor", color);
		double fillOpacity = Double.parseDouble(options.getOrDefault("fillOpacity", "0.2"));

		Style style = new Style();
		if (stroke) {
			style.setStroke(new Stroke(withOpacity(color, opacity), weight));
		}
		if (fill) {
			style.setFill(new Fill(withOpacity(fillColor, fillOpacity)));
		}
		return style;
	}

	/**
	 * <code>#rgb</code> or <code>#rrggbb</code> with an opacity as <code>rgba(...)</code>; other colours are used as they are.
	 */
	static String withOpacity(String color, double opacity) {
		String hex = color.startsWith("#") ? color.substring(1) : null;
		if (hex != null && hex.length() == 3) {
			hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
		}
		if (hex == null || !hex.matches("[0-9a-fA-F]{6}")) {
			return color;
		}
		int rgb = Integer.parseInt(hex, 16);
		return "rgba(" + ((rgb >> 16) & 0xff) + "," + ((rgb >> 8) & 0xff) + "," + (rgb & 0xff) + "," + opacity + ")";
	}

	public void removeGroup(String groupId) {
		List<FeatureLayer> layers = groupLayers.remove(groupId);
		if (layers != null) {
			for (FeatureLayer layer : layers) {
				layer.getFeatures().forEach(featureRefs::remove);
				map.removeLayer(layer);
			}
		}
	}

	public Registration addMarkerClickListener(MarkerClickListener listener) {
		return addListener(MarkerClickEvent.class, listener::markerClick);
	}

	/**
	 * Append the give attribution to the attribution list of the map.
	 * 
	 * @param attribution
	 *            The attribution to be given.
	 */
	public void addShapefileAttribution(String attribution) {
		if (StringUtils.isNotBlank(attribution)) {
			attributions.add(attribution);
			updateTileSource();
		}
	}

	@FunctionalInterface
	public interface MarkerClickListener extends ComponentEventListener<MarkerClickEvent> {

		void markerClick(MarkerClickEvent event);

		@Override
		default void onComponentEvent(MarkerClickEvent event) {
			markerClick(event);
		}
	}

	public static class MarkerClickEvent extends ComponentEvent<LeafletMap> {

		private static final long serialVersionUID = -2607378360765308016L;

		private final String groupId;
		private final int markerIndex;

		public MarkerClickEvent(LeafletMap map, String groupId, int markerIndex) {
			super(map, true);
			this.groupId = groupId;
			this.markerIndex = markerIndex;
		}

		public String getGroupId() {
			return groupId;
		}

		public int getMarkerIndex() {
			return markerIndex;
		}
	}

	private static final class MarkerRef implements java.io.Serializable {

		private static final long serialVersionUID = 1L;

		private final String groupId;
		private final int index;

		private MarkerRef(String groupId, int index) {
			this.groupId = groupId;
			this.index = index;
		}
	}
}
