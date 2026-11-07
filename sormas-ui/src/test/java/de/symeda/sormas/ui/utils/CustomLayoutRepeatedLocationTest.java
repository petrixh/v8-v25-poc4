package de.symeda.sormas.ui.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Element;

/**
 * Ticket 2a-09: a location that is in the template more than once (the symptoms form's pneumonia field, the
 * immunization form's country field) was shown by Vaadin 8 in the last of its slots (VCustomLayout keeps the last slot
 * element per location); the other slots stay empty.
 */
public class CustomLayoutRepeatedLocationTest {

	private static final String TEMPLATE = "<div id='first'><div location='field'></div></div><div location='other'></div>"
		+ "<div id='second'><div location='field'></div></div>";

	@Test
	public void testRepeatedLocationUsesLastSlot() {
		CustomLayout layout = new CustomLayout(TEMPLATE);
		Span field = new Span("field");
		layout.addComponent(field, "field");

		Element first = layout.getElement().getChild(0);
		Element second = layout.getElement().getChild(2);
		assertEquals("first", first.getAttribute("id"));
		assertEquals("second", second.getAttribute("id"));
		assertEquals(0, first.getChild(0).getChildCount());
		assertSame(field.getElement(), second.getChild(0).getChild(0));
		assertSame(second.getChild(0), layout.getSlotElement("field"));
	}

	@Test
	public void testTemplateSetAfterComponentUsesLastSlot() {
		CustomLayout layout = new CustomLayout();
		Span field = new Span("field");
		layout.addComponent(field, "field");
		layout.setTemplateContents(TEMPLATE);

		Element second = layout.getElement().getChild(2);
		assertSame(field.getElement(), second.getChild(0).getChild(0));
		assertTrue(layout.hasLocation("other"));
	}
}
