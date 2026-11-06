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

/**
 * Text renderer with the Vaadin 8 {@code com.vaadin.ui.renderers.TextRenderer} API: shows {@code value.toString()},
 * escaped.
 */
public class TextRenderer extends ValueRenderer<Object> {

	private static final long serialVersionUID = 1L;

	public TextRenderer() {
		this("");
	}

	public TextRenderer(String nullRepresentation) {
		super(nullRepresentation);
	}

	@Override
	public boolean isHtml() {
		return false;
	}
}
