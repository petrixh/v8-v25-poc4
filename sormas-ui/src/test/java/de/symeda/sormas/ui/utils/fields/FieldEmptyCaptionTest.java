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
package de.symeda.sormas.ui.utils.fields;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Ticket 2a-07: Vaadin 8 kept the caption row of a field whose caption was set to {@code ""}, so the field lined up with
 * captioned fields beside it. The shim gives such a field a zero-width-space label (a blank label is hidden by Flow);
 * {@code getCaption()} still returns {@code ""}.
 */
public class FieldEmptyCaptionTest {

	@Test
	public void testEmptyCaptionKeepsLabelRow() {
		ComboBox field = new ComboBox();
		field.setCaption("");
		assertEquals(Field.EMPTY_CAPTION_LABEL, field.getLabel());
		assertEquals("", field.getCaption());
	}

	@Test
	public void testOtherCaptionsUnchanged() {
		ComboBox field = new ComboBox();
		field.setCaption("Month");
		assertEquals("Month", field.getLabel());
		assertEquals("Month", field.getCaption());

		field.setCaption(null);
		assertNull(field.getLabel());
		assertNull(field.getCaption());
	}
}
