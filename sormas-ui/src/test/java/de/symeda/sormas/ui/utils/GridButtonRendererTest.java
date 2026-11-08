package de.symeda.sormas.ui.utils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Ticket 2b-06: a Flow grid does not report a click on a focusable element in a cell (a {@code vaadin-button}) as an
 * item click. The button of {@link GridButtonRenderer} has no listener of its own (Vaadin 8: the grid's item click on its
 * column does the action), so it must let the click through to the cell.
 */
public class GridButtonRendererTest {

	@Test
	public void testButtonLetsTheClickReachTheCell() {

		String html = new GridButtonRenderer().encode("Document treatment");

		assertTrue(html.startsWith("<vaadin-button "), html);
		assertTrue(html.contains("style='pointer-events: none'"), html);
		assertTrue(html.contains(">Document treatment</vaadin-button>"), html);
	}

	@Test
	public void testEmptyValueRendersNoButton() {

		assertNull(new GridButtonRenderer().encode(null));
		assertNull(new GridButtonRenderer().encode(""));
	}
}
