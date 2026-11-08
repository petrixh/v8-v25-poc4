package de.symeda.sormas.ui.customexport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Ticket 2b-10: Flow answers a download with HTTP 403 "Resource not available" when the element that owns the
 * registered resource is inert. While a modal window (the custom export window) is open, the UI element is inert and
 * the window's content is not, so the custom export downloads are owned by the window's layout, not by the UI.
 */
public class CustomExportDownloadOwnerTest {

	@Test
	public void testUiIsInertButModalWindowContentIsNot() {

		UI ui = new UI();
		VerticalLayout windowContent = new VerticalLayout();
		Dialog window = new Dialog(windowContent);
		ui.add(window);
		ui.setChildComponentModal(window, true);
		// inertness is resolved when the changes for the client are collected (each round trip)
		ui.getInternals().getStateTree().collectChanges(change -> {
		});

		assertTrue(ui.getElement().getNode().isInert(), "the UI is inert behind the modal window");
		assertFalse(windowContent.getElement().getNode().isInert(), "the modal window's content is not inert");
	}
}
