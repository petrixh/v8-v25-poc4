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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.data.binder.Binder;

/**
 * Vaadin 8 {@code com.vaadin.ui.ComboBox}: a value can be set before the items (the old line listing and aggregate
 * report windows did so while they were built). Flow throws "Cannot set a value for a ComboBox without items";
 * {@link KeepValueComboBox} takes the value, and items set later keep it (1a-32e rule: cleared when it is not among
 * them).
 */
public class KeepValueComboBoxValueBeforeItemsTest {

	/** Exposes the item the client's value key stands for. */
	static class Probed<T> extends KeepValueComboBox<T> {

		Probed() {
			super();
		}

		Probed(String label) {
			super(label);
		}

		Object clientItem() {
			return getKeyMapper().get(getElement().getProperty("value"));
		}
	}

	public static class Bean {

		private String choice;

		public String getChoice() {
			return choice;
		}

		public void setChoice(String choice) {
			this.choice = choice;
		}
	}

	private static List<String> events(KeepValueComboBox<String> combo) {
		List<String> events = new ArrayList<>();
		combo.addValueChangeListener(e -> events.add(e.getOldValue() + "->" + e.getValue()));
		return events;
	}

	@Test
	public void plainFlowComboBoxThrowsWithoutItems() {
		// control: the Flow behaviour this shim works around
		ComboBox<String> plain = new ComboBox<>("plain");
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> plain.setValue("a"));
		assertEquals(true, e.getMessage().startsWith("Cannot set a value for a ComboBox without items"));
	}

	@Test
	public void valueBeforeItemsIsTakenAndShown() {
		Probed<String> combo = new Probed<>("label");
		List<String> events = events(combo);

		combo.setValue("a");

		assertEquals("a", combo.getValue());
		assertEquals(Arrays.asList("null->a"), events);
		assertEquals("a", combo.clientItem());
	}

	@Test
	public void valueBeforeItemsWithNoArgConstructor() {
		Probed<String> combo = new Probed<>();
		combo.setValue("a");
		assertEquals("a", combo.getValue());
	}

	@Test
	public void nullBeforeItemsIsANoOp() {
		KeepValueComboBox<String> combo = new KeepValueComboBox<>();
		List<String> events = events(combo);
		combo.setValue(null);
		assertNull(combo.getValue());
		assertEquals(0, events.size());
	}

	@Test
	public void itemsArrivingLaterKeepTheValue() {
		Probed<String> combo = new Probed<>();
		combo.setValue("a");
		List<String> events = events(combo);

		combo.setItems(Arrays.asList("a", "b"));

		assertEquals("a", combo.getValue());
		assertEquals(0, events.size());
		assertEquals("a", combo.clientItem());
	}

	@Test
	public void itemsArrivingLaterWithoutTheValueClearIt() {
		// 1a-32e rule (Vaadin 8 kept a stale value here; the old callers cleared it themselves)
		KeepValueComboBox<String> combo = new KeepValueComboBox<>();
		combo.setValue("a");
		List<String> events = events(combo);

		combo.setItems(Arrays.asList("x", "y"));

		assertNull(combo.getValue());
		assertEquals(Arrays.asList("a->null"), events);
	}

	@Test
	public void valueSetByAListenerBeforeTheItems() {
		// AggregateReportsEditLayout: the year's listener sets the week before the week combo has items, then sets them
		ComboBox<Integer> year = new ComboBox<>("year", Arrays.asList(2025, 2026));
		KeepValueComboBox<String> week = new KeepValueComboBox<>("week");
		year.addValueChangeListener(e -> {
			week.setValue(e.getValue() + "-W45");
			week.setItems(Arrays.asList(e.getValue() + "-W44", e.getValue() + "-W45"));
		});

		year.setValue(2026);

		assertEquals("2026-W45", week.getValue());
	}

	@Test
	public void binderReadsAValueBeforeTheItems() {
		Probed<String> combo = new Probed<>();
		Binder<Bean> binder = new Binder<>(Bean.class);
		binder.forField(combo).bind(Bean::getChoice, Bean::setChoice);
		Bean bean = new Bean();
		bean.setChoice("a");

		binder.setBean(bean);

		assertEquals("a", combo.getValue());
		assertEquals("a", combo.clientItem());
	}

	@Test
	public void valueAfterItemsIsUnchanged() {
		KeepValueComboBox<String> combo = new KeepValueComboBox<>();
		combo.setItems(Arrays.asList("a", "b"));
		List<String> events = events(combo);
		combo.setValue("b");
		combo.setValue("z");
		assertEquals("z", combo.getValue());
		assertEquals(Arrays.asList("null->b", "b->z"), events);
	}
}
