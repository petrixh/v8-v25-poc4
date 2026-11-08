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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import de.symeda.sormas.api.InfrastructureDataReferenceDto;
import de.symeda.sormas.api.infrastructure.facility.FacilityReferenceDto;
import de.symeda.sormas.ui.utils.AbstractForm.BeanFieldGroup;

/**
 * Vaadin 7 showed a combo box's selected value with the caption of the equal item id in its container, not the value's
 * own caption. Forms rely on it: {@code AbstractEditForm.addInfrastructureField} adds a copy of an inactive value
 * captioned "... (Inactive)", and V7 showed that caption.
 */
public class ComboBoxValueCaptionTest {

	private static final String INACTIVE = " (Inactive)";

	public static class Bean {

		private FacilityReferenceDto facility;

		public FacilityReferenceDto getFacility() {
			return facility;
		}

		public void setFacility(FacilityReferenceDto facility) {
			this.facility = facility;
		}
	}

	/** The form's read of the bean: the only way a value that is not an item reaches a V7 select (2b-12). */
	private static void read(ComboBox combo, FacilityReferenceDto value) {
		BeanFieldGroup<Bean> group = new BeanFieldGroup<>(Bean.class);
		group.bind(combo, "facility");
		Bean bean = new Bean();
		bean.setFacility(value);
		group.setItemDataSource(bean);
	}

	private static FacilityReferenceDto facility(String uuid, String caption) {
		return new FacilityReferenceDto(uuid, caption, null);
	}

	/** Same listener as {@code AbstractEditForm.addInfrastructureField}. */
	private static ComboBox infrastructureCombo() {
		ComboBox field = new ComboBox();
		field.addValueChangeListener(e -> {
			InfrastructureDataReferenceDto value = (InfrastructureDataReferenceDto) e.getValue();
			if (value != null && !field.containsId(value)) {
				InfrastructureDataReferenceDto inactiveValue = value.clone();
				inactiveValue.setCaption(value.getCaption() + INACTIVE);
				field.addItem(inactiveValue);
			}
		});
		return field;
	}

	private static String shownCaption(ComboBox combo) {
		return combo.getItemLabelGenerator().apply(combo.getValue());
	}

	private static String selectedItemJson(ComboBox combo) {
		return String.valueOf(combo.getElement().getPropertyRaw("selectedItem"));
	}

	@Test
	public void inactiveValueShowsCaptionOfItsInactiveCopy() {
		ComboBox combo = infrastructureCombo();
		FacilityReferenceDto value = facility("F1", "Default Facility");

		read(combo, value);

		assertSame(value, combo.getValue());
		assertEquals("Default Facility" + INACTIVE, shownCaption(combo));
		assertTrue(selectedItemJson(combo).contains("Default Facility" + INACTIVE), selectedItemJson(combo));
		// the value itself keeps its caption (the copy is another instance)
		assertEquals("Default Facility", value.getCaption());
	}

	@Test
	public void activeValueShowsItsItemCaption() {
		ComboBox combo = infrastructureCombo();
		combo.addItems(Arrays.asList(facility("F1", "Default Facility"), facility("F2", "Other Facility")));

		combo.setValue(facility("F1", "Default Facility"));

		assertEquals("Default Facility", shownCaption(combo));
		assertEquals(2, combo.size());
	}

	@Test
	public void valueThatIsNoItemShowsItsOwnCaption() {
		ComboBox combo = new ComboBox();
		combo.addItem(facility("F2", "Other Facility"));
		FacilityReferenceDto value = facility("F1", "Default Facility");

		assertEquals("Default Facility", combo.getSelectItems().getValueCaption(value));
	}

	@Test
	public void itemAddedAfterTheValueRecaptionsTheValue() {
		ComboBox combo = new ComboBox();
		combo.addItem(facility("F1", "Default Facility"));
		combo.setValue(facility("F1", "Default Facility"));
		assertEquals("Default Facility", shownCaption(combo));

		// replacing the items with an equal id of another caption shows the new caption, as V7 did
		combo.removeAllItems();
		combo.addItem(facility("F1", "Renamed Facility"));
		combo.setValue(facility("F1", "Default Facility"));

		assertEquals("Renamed Facility", shownCaption(combo));
		assertTrue(selectedItemJson(combo).contains("Renamed Facility"), selectedItemJson(combo));
	}

	@Test
	public void activeItemsReplacingTheInactiveCopyDropTheSuffix() {
		ComboBox combo = infrastructureCombo();
		FacilityReferenceDto value = facility("F1", "Default Facility");
		read(combo, value);
		assertEquals("Default Facility" + INACTIVE, shownCaption(combo));

		// FieldHelper.updateItems: remove all, add the active items, set the old value again
		combo.removeAllItems();
		combo.addItems(Arrays.asList(facility("F1", "Default Facility"), facility("F2", "Other Facility")));
		combo.setValue(value);

		assertEquals("Default Facility", shownCaption(combo));
	}

	@Test
	public void explicitCaptionStillWins() {
		ComboBox combo = new ComboBox();
		FacilityReferenceDto item = facility("F1", "Default Facility");
		combo.addItem(item);
		combo.setItemCaption(item, "Explicit");

		combo.setValue(facility("F1", "Default Facility"));

		assertEquals("Explicit", shownCaption(combo));
	}

	@Test
	public void storedIdFollowsRemovals() {
		SelectItems items = new SelectItems(null);
		FacilityReferenceDto stored = facility("F1", "Stored");
		FacilityReferenceDto other = facility("F1", "Other");

		items.add(stored);
		assertSame(stored, items.getStoredId(other));

		items.remove(other);
		assertSame(other, items.getStoredId(other));

		items.addAll(Arrays.asList(stored, facility("F2", "Two")));
		items.retainAll(Arrays.asList(facility("F2", "x")));
		assertSame(other, items.getStoredId(other));
	}
}
