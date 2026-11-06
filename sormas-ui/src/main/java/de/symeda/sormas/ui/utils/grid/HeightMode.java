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
package de.symeda.sormas.ui.utils.grid;

/**
 * How a {@link Grid} sets its height, with the Vaadin 7/8 {@code HeightMode} constants.
 */
public enum HeightMode {

	/** The height set with {@code setHeight} (Flow default). */
	CSS,
	/** As many rows as {@link Grid#setHeightByRows(double)} says. */
	ROW,
	/** As high as all rows (Flow {@code setAllRowsVisible(true)}). */
	UNDEFINED
}
