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
package de.symeda.sormas.ui.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Ticket 2a-10: a Vaadin 8 {@code CustomLayout} was 100% wide unless a width was set (8.14.3 constructor:
 * {@code setWidth(100, PERCENTAGE)}). The shim does the same; an explicit width still wins.
 */
public class CustomLayoutWidthTest {

	@Test
	public void testFullWidthByDefault() {
		assertEquals("100%", new CustomLayout().getWidth());
		assertEquals("100%", new CustomLayout("<div location=\"a\"></div>").getWidth());
		assertEquals("100%", new CustomLayout() {
		}.getWidth());
		assertNull(new CustomLayout().getHeight());
	}

	@Test
	public void testExplicitWidthWins() {
		CustomLayout layout = new CustomLayout();
		layout.setWidthUndefined();
		assertNull(layout.getWidth());

		layout.setWidth("520px");
		assertEquals("520px", layout.getWidth());
	}
}
