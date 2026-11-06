/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2026 SORMAS Foundation gGmbH
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
package de.symeda.sormas.ui.utils.grid.renderers;

import java.util.Locale;

import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * HTML renderer with the Vaadin 7/8 {@code HtmlRenderer} API: the String value is put into the cell as HTML, not
 * escaped (as before; callers sanitise user data, e.g. with {@code HtmlHelper.cleanHtml}).
 */
public class HtmlRenderer extends ValueRenderer<String> {

	private static final long serialVersionUID = 1L;

	public HtmlRenderer() {
		this("");
	}

	public HtmlRenderer(String nullRepresentation) {
		super(nullRepresentation);
	}

	@Override
	public boolean isHtml() {
		return true;
	}

	/**
	 * The HTML of an icon, for HTML cell content; replaces Vaadin 8 {@code VaadinIcons.X.getHtml()}.
	 */
	public static String iconHtml(VaadinIcon icon) {
		return "<vaadin-icon icon=\"vaadin:" + icon.name().toLowerCase(Locale.ENGLISH).replace('_', '-') + "\"></vaadin-icon>";
	}
}
