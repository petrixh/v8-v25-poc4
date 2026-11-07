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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Vaadin 8 column sizing of the {@link Grid} shim: width from the content, equal expansion until a column has an expand
 * ratio, fixed widths, minimum and maximum widths as part names.
 */
public class GridColumnSizingTest {

	@Test
	public void testColumnsSizeToContentAndExpandEqually() {
		Grid<String> grid = new Grid<>();
		Grid.Column<String, ?> a = grid.addColumn(s -> s);
		Grid.Column<String, ?> b = grid.addColumn(s -> s + s);

		assertTrue(a.isAutoWidth());
		assertTrue(b.isAutoWidth());
		assertEquals(1, a.getFlexGrow());
		assertEquals(1, b.getFlexGrow());
	}

	@Test
	public void testExpandRatioRemovesImplicitExpansion() {
		Grid<String> grid = new Grid<>();
		Grid.Column<String, ?> a = grid.addColumn(s -> s);
		Grid.Column<String, ?> b = grid.addColumn(s -> s);
		b.setExpandRatio(2);
		Grid.Column<String, ?> c = grid.addColumn(s -> s);

		assertEquals(0, a.getFlexGrow());
		assertEquals(2, b.getFlexGrow());
		assertEquals(0, c.getFlexGrow());
		assertTrue(a.isAutoWidth());
	}

	@Test
	public void testFixedWidth() {
		Grid<String> grid = new Grid<>();
		Grid.Column<String, ?> a = grid.addColumn(s -> s);
		a.setWidth(20);

		assertFalse(a.isAutoWidth());
		assertEquals("20px", a.getWidth());
		assertEquals(0, a.getFlexGrow());
		assertEquals(20, a.getFixedWidth());

		a.setWidthUndefined();
		assertTrue(a.isAutoWidth());
		assertEquals(1, a.getFlexGrow());

		// Flow API, as VaadinUiUtil.setupActionColumn uses it
		a.setWidth("20px");
		assertFalse(a.isAutoWidth());
		assertEquals(0, a.getFlexGrow());
		assertEquals(-1, a.getFixedWidth());
	}

	@Test
	public void testWidthLimitsAsPartNames() {
		Grid<String> grid = new Grid<>();
		Grid.Column<String, ?> a = grid.addColumn(s -> s);
		assertNull(a.getElement().getProperty("headerPartName"));

		a.setMaximumWidth(300);
		assertEquals("sormas-col-max-300", a.getElement().getProperty("headerPartName"));
		assertEquals("sormas-col-max-300", a.getElement().getProperty("footerPartName"));
		assertEquals("sormas-col-max-300", a.getPartNameGenerator().apply("x"));

		a.setStyleGenerator(s -> "link");
		a.setMinimumWidth(150);
		assertEquals("link sormas-col-min-150 sormas-col-max-300", a.getPartNameGenerator().apply("x"));
		assertEquals("sormas-col-min-150 sormas-col-max-300", a.getElement().getProperty("headerPartName"));

		a.setStyleGenerator(s -> null);
		assertEquals("sormas-col-min-150 sormas-col-max-300", a.getPartNameGenerator().apply("x"));

		a.setHeaderPartName("odd");
		assertEquals("odd", a.getHeaderPartName());
		assertEquals("odd sormas-col-min-150 sormas-col-max-300", a.getElement().getProperty("headerPartName"));
	}

	@Test
	public void testUndefinedHeightModeDropsTheHeight() {
		Grid<String> grid = new Grid<>();
		grid.setSizeFull();
		grid.setHeightMode(HeightMode.UNDEFINED);

		assertNull(grid.getHeight());
		assertTrue(grid.isAllRowsVisible());
		assertEquals("100%", grid.getWidth());
	}
}
