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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.vaadin.flow.component.map.configuration.style.Icon;

/**
 * Marker images for {@link LeafletMap}. Vaadin 8 drew the markers in the browser with CSS (MarkerCluster.css: circles for cases,
 * squares for contacts, icon font glyphs for samples and AEFI, images for facilities and events). Vaadin Map draws images, so the
 * CSS shapes and colours are drawn here as SVG; facilities and events use the same images as before.
 */
final class LeafletMapIcons {

	private LeafletMapIcons() {
	}

	// outer and inner colours from MarkerCluster.css
	private static final String RED_OUTER = "rgba(216,49,43,0.8)";
	private static final String RED_INNER = "rgba(229,166,160,0.8)";
	private static final String ORANGE_OUTER = "rgba(204,103,20,0.8)";
	private static final String ORANGE_INNER = "rgba(250,190,146,0.8)";
	private static final String YELLOW_OUTER = "rgba(204,173,20,0.8)";
	private static final String YELLOW_INNER = "rgba(250,225,146,0.8)";
	private static final String GREY_OUTER = "rgba(127,127,127,0.8)";
	private static final String GREY_INNER = "rgba(203,203,203,0.8)";
	private static final String GREEN_OUTER = "rgba(76,165,67,0.8)";
	private static final String GREEN_INNER = "rgba(175,227,168,0.8)";

	/**
	 * Marker size in pixels for a marker that stands for <code>count</code> items (leaflet-connector.js).
	 */
	static int size(int count) {
		return 20 + 5 * Math.min((int) Math.ceil((count - 1) / 10.0), 4);
	}

	static Icon createIcon(MarkerIcon markerIcon, int size) {
		Icon.Options options = new Icon.Options();
		String image = imageUrl(markerIcon);
		if (image != null) {
			options.setSrc(image);
			// the images are 40 x 40 pixels
			options.setScale(size / 40.0);
		} else {
			options.setSrc("data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg(markerIcon, size).getBytes(StandardCharsets.UTF_8)));
			options.setImgSize(new Icon.ImageSize(size, size));
		}
		if (markerIcon == MarkerIcon.EVENT_OUTBREAK || markerIcon == MarkerIcon.EVENT_RUMOR) {
			// CSS moved event markers up by a quarter of their size
			options.setAnchor(new Icon.Anchor(0.5, 0.75));
		} else {
			options.setAnchor(new Icon.Anchor(0.5, 0.5));
		}
		return new Icon(options);
	}

	private static String imageUrl(MarkerIcon markerIcon) {
		switch (markerIcon) {
		case FACILITY_CONFIRMED:
		case FACILITY_SUSPECT:
		case FACILITY_PROBABLE:
		case FACILITY_UNCLASSIFIED:
		case EVENT_OUTBREAK:
		case EVENT_RUMOR:
			// e.g. VAADIN/map/marker/facility-confirmed.png
			return "VAADIN/map/marker/" + markerIcon.name().toLowerCase().replace('_', '-') + ".png";
		default:
			return null;
		}
	}

	private static String svg(MarkerIcon markerIcon, int size) {
		String outer;
		String inner;
		Shape shape;
		switch (markerIcon) {
		case CASE_CONFIRMED:
			shape = Shape.CIRCLE;
			outer = RED_OUTER;
			inner = RED_INNER;
			break;
		case CASE_PROBABLE:
			shape = Shape.CIRCLE;
			outer = ORANGE_OUTER;
			inner = ORANGE_INNER;
			break;
		case CASE_SUSPECT:
			shape = Shape.CIRCLE;
			outer = YELLOW_OUTER;
			inner = YELLOW_INNER;
			break;
		case CASE_UNCLASSIFIED:
			shape = Shape.CIRCLE;
			outer = GREY_OUTER;
			inner = GREY_INNER;
			break;
		case CONTACT_LONG_OVERDUE:
			shape = Shape.ROUNDED_SQUARE;
			outer = RED_OUTER;
			inner = RED_INNER;
			break;
		case CONTACT_OVERDUE:
			shape = Shape.ROUNDED_SQUARE;
			outer = YELLOW_OUTER;
			inner = YELLOW_INNER;
			break;
		case CONTACT_OK:
			shape = Shape.ROUNDED_SQUARE;
			outer = GREEN_OUTER;
			inner = GREEN_INNER;
			break;
		// samples and AEFI: Vaadin 8 drew an icon font glyph (flask, AEFI symbol) behind a square; drawn as two squares
		case SAMPLE_CASE:
		case AEFI_SERIOUS:
			shape = Shape.SQUARE;
			outer = RED_OUTER;
			inner = "rgba(216,49,43,0.6)";
			break;
		case SAMPLE_CONTACT:
		case AEFI_NON_SERIOUS:
			shape = Shape.SQUARE;
			outer = YELLOW_OUTER;
			inner = "rgba(204,173,20,0.6)";
			break;
		case SAMPLE_EVENT_PARTICIPANT:
			shape = Shape.SQUARE;
			outer = "rgba(0,90,156,0.8)";
			inner = "rgba(0,90,156,0.6)";
			break;
		case SAMPLE_ENVIRONMENT:
			shape = Shape.SQUARE;
			outer = GREEN_OUTER;
			inner = GREEN_INNER;
			break;
		case AEFI_UNCLASSIFIED:
			shape = Shape.SQUARE;
			outer = GREY_OUTER;
			inner = GREY_INNER;
			break;
		default:
			throw new IllegalArgumentException(markerIcon.toString());
		}

		// inner area: 80% of the marker, centred (CSS: height 80%, margin 10%)
		double innerOffset = size * 0.1;
		double innerSize = size * 0.8;
		StringBuilder svg = new StringBuilder();
		svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='")
			.append(size)
			.append("' height='")
			.append(size)
			.append("' viewBox='0 0 ")
			.append(size)
			.append(' ')
			.append(size)
			.append("'>");
		if (shape == Shape.CIRCLE) {
			double r = size / 2.0;
			svg.append(circle(r, r, r, outer)).append(circle(r, r, innerSize / 2, inner));
		} else {
			double outerRadius = shape == Shape.ROUNDED_SQUARE ? 2 : 0;
			double innerRadius = shape == Shape.ROUNDED_SQUARE ? 1 : 0;
			svg.append(rect(0, size, outerRadius, outer)).append(rect(innerOffset, innerSize, innerRadius, inner));
		}
		svg.append("</svg>");
		return svg.toString();
	}

	private static String circle(double cx, double cy, double r, String fill) {
		return "<circle cx='" + cx + "' cy='" + cy + "' r='" + r + "' fill='" + fill + "'/>";
	}

	private static String rect(double offset, double size, double radius, String fill) {
		return "<rect x='" + offset + "' y='" + offset + "' width='" + size + "' height='" + size + "' rx='" + radius + "' fill='" + fill
			+ "'/>";
	}

	private enum Shape {
		CIRCLE,
		ROUNDED_SQUARE,
		SQUARE
	}
}
