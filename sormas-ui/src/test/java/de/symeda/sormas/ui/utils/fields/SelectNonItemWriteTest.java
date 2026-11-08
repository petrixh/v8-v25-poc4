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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.radiobutton.RadioButtonGroup;

import de.symeda.sormas.ui.utils.AbstractForm.BeanFieldGroup;
import de.symeda.sormas.ui.utils.ComboBoxWithPlaceholder;

/**
 * Ticket 2b-12: the V7 {@code AbstractSelect.setValue} rule for values that are not items, on the select shims. Each
 * case is a row of the V7 oracle (8.14.3 compatibility classes, PR evidence {@code v7-result.txt}, probe
 * {@code V7Probe2b12.java}; row ids in the comments): a single select ignores a programmatic {@code setValue} or
 * {@code select} of a non-item (no change, no event, also when read-only), and does not select it when the item is added
 * later; {@code null} is always taken; a multi select takes any collection but ignores {@code select} of a non-item; the
 * field group's read of the bean (V7 data source) takes any value, and a listener's write during that read follows the
 * rule. {@link KeepValueComboBox} replaces a Vaadin 8 combo box and keeps the Vaadin 8 rule (it takes the value).
 */
public class SelectNonItemWriteTest {

	public static class Bean {

		private Object choice;
		private Object option;
		private Set<Object> tags;

		public Object getChoice() {
			return choice;
		}

		public void setChoice(Object choice) {
			this.choice = choice;
		}

		public Object getOption() {
			return option;
		}

		public void setOption(Object option) {
			this.option = option;
		}

		public Set<Object> getTags() {
			return tags;
		}

		public void setTags(Set<Object> tags) {
			this.tags = tags;
		}
	}

	private static <F extends Field<?>> F counted(F field, AtomicInteger events) {
		field.addValueChangeListener(e -> events.incrementAndGet());
		return field;
	}

	private static Set<Object> set(Object... values) {
		return new LinkedHashSet<>(Arrays.asList(values));
	}

	/** C1-C12: no items, other items, the item added later, null. */
	@Test
	public void comboBoxIgnoresNonItem() {
		AtomicInteger events = new AtomicInteger();
		ComboBox combo = counted(new ComboBox(), events);

		combo.setValue("a"); // C1
		assertNull(combo.getValue());
		assertEquals(0, events.get());
		combo.addItem("a"); // C2: the item added later does not select it
		assertNull(combo.getValue());
		assertEquals(0, events.get());

		combo.setValue("a"); // C3
		assertEquals("a", combo.getValue());
		assertEquals(1, events.get());
		combo.setValue("z"); // C4
		assertEquals("a", combo.getValue());
		assertEquals(1, events.get());
		combo.setValue(null); // C5
		assertNull(combo.getValue());
		assertEquals(2, events.get());
		combo.setValue(null); // C6
		assertEquals(2, events.get());

		combo.setValue("a");
		combo.setNullSelectionAllowed(false); // C9: null is taken also without null selection
		combo.setValue(null);
		assertNull(combo.getValue());

		ComboBox other = counted(new ComboBox(), events);
		other.addItem("x");
		events.set(0);
		other.setValue("a"); // C10
		other.addItem("a"); // C11
		assertNull(other.getValue());
		assertEquals(0, events.get());
		other.setValue("a"); // C12
		assertEquals("a", other.getValue());
		assertEquals(1, events.get());
	}

	/** C13, C14: the item check comes before the read-only check. */
	@Test
	public void readOnlyComboBoxIgnoresNonItemWithoutException() {
		AtomicInteger events = new AtomicInteger();
		ComboBox combo = counted(new ComboBox(), events);
		combo.addItem("x");
		combo.setReadOnly(true);

		combo.setValue("a"); // C13
		assertNull(combo.getValue());
		assertThrows(Field.ReadOnlyException.class, () -> combo.setValue("x")); // C14
		assertNull(combo.getValue());
		assertEquals(0, events.get());
	}

	/** Subclasses follow the same rule. */
	@Test
	public void comboBoxSubclassIgnoresNonItem() {
		ComboBoxWithPlaceholder combo = new ComboBoxWithPlaceholder();
		combo.addItem("x");
		combo.setValue("a");
		assertNull(combo.getValue());
		combo.setValue("x");
		assertEquals("x", combo.getValue());
	}

	/**
	 * G1, G3-G5, P1-P3: the field group's read takes a value that is not an item (combo box, single and multi option
	 * group), keeps it when the item is added later, writes it back on commit; a direct write of a non-item to the bound
	 * field is ignored. Buffered and unbuffered.
	 */
	@Test
	public void fieldGroupReadTakesNonItem() {
		for (boolean buffered : new boolean[] {
			true,
			false }) {
			BeanFieldGroup<Bean> group = new BeanFieldGroup<>(Bean.class);
			group.setBuffered(buffered);
			ComboBox choice = new ComboBox();
			choice.addItem("x");
			OptionGroup option = new OptionGroup();
			option.addItem("x");
			OptionGroup tags = new OptionGroup();
			tags.setMultiSelect(true);
			tags.addItem("x");
			group.bind(choice, "choice");
			group.bind(option, "option");
			group.bind(tags, "tags");

			Bean bean = new Bean();
			bean.setChoice("a");
			bean.setOption("a");
			bean.setTags(set("a"));
			group.setItemDataSource(bean);
			String mode = buffered ? "buffered" : "unbuffered";
			assertEquals("a", choice.getValue(), mode);
			assertEquals("a", option.getValue(), mode);
			assertEquals(set("a"), tags.getValue(), mode);

			choice.setValue("b"); // G4
			option.setValue("b");
			assertEquals("a", choice.getValue(), mode);
			assertEquals("a", option.getValue(), mode);

			choice.addItem("a"); // P3
			option.addItem("a");
			option.getSelectItems().flushRefresh();
			assertEquals("a", choice.getValue(), mode);
			assertEquals("a", option.getValue(), mode);
			assertEquals("a", ((RadioButtonGroup<?>) option.getGroupComponent()).getValue(), mode);

			group.commit(); // G5
			assertEquals("a", bean.getChoice(), mode);
			assertEquals("a", bean.getOption(), mode);
		}
	}

	/** Discard (V7 data source read) shows a bean value that is not an item again. */
	@Test
	public void fieldGroupDiscardTakesNonItem() {
		BeanFieldGroup<Bean> group = new BeanFieldGroup<>(Bean.class);
		ComboBox choice = new ComboBox();
		choice.addItems("x", "y");
		group.bind(choice, "choice");
		Bean bean = new Bean();
		bean.setChoice("a");
		group.setItemDataSource(bean);

		choice.setValue("y");
		assertEquals("y", choice.getValue());
		group.discard();
		assertEquals("a", choice.getValue());
	}

	/** G1, G2: a listener writing a non-item into another select during the read follows the rule. */
	@Test
	public void listenerWriteDuringReadIgnoresNonItem() {
		BeanFieldGroup<Bean> group = new BeanFieldGroup<>(Bean.class);
		ComboBox choice = new ComboBox();
		choice.addItem("x");
		group.bind(choice, "choice");
		ComboBox other = new ComboBox();
		other.addItem("x");
		other.setValue("x");
		OptionGroup otherOption = new OptionGroup();
		otherOption.addItem("x");
		AtomicInteger listenerRuns = new AtomicInteger();
		choice.addValueChangeListener(e -> {
			if ("a".equals(e.getValue())) {
				listenerRuns.incrementAndGet();
				other.setValue("fromListener");
				otherOption.setValue("fromListener");
			}
		});

		Bean bean = new Bean();
		bean.setChoice("a");
		group.setItemDataSource(bean);
		assertEquals(1, listenerRuns.get());
		assertEquals("a", choice.getValue());
		assertEquals("x", other.getValue());
		assertNull(otherOption.getValue());

		// outside any read, the rule applies to the bound field too (no state left behind)
		choice.setValue("b");
		assertEquals("a", choice.getValue());
	}

	/** O1-O7: single option group. */
	@Test
	public void singleOptionGroupIgnoresNonItem() {
		AtomicInteger events = new AtomicInteger();
		OptionGroup group = counted(new OptionGroup(), events);

		group.setValue("a"); // O1
		assertNull(group.getValue());
		group.addItem("x");
		group.setValue("a"); // O2
		group.addItem("a"); // O3
		assertNull(group.getValue());
		assertEquals(0, events.get());

		group.setValue("a"); // O4
		assertEquals("a", group.getValue());
		group.select("z"); // O5
		assertEquals("a", group.getValue());
		assertEquals(1, events.get());
		group.setValue(null); // O6
		assertNull(group.getValue());
		assertEquals(2, events.get());

		group.setItemEnabled("x", false); // O7: a disabled item is an item
		group.setValue("x");
		assertEquals("x", group.getValue());

		group.setReadOnly(true); // as C13: no exception for a non-item
		group.setValue("z");
		assertEquals("x", group.getValue());
	}

	/** M1-M6: a multi select takes any collection, but select of a non-item is ignored. */
	@Test
	public void multiOptionGroupTakesAnyCollection() {
		AtomicInteger events = new AtomicInteger();
		OptionGroup group = counted(new OptionGroup(), events);
		group.setMultiSelect(true);
		events.set(0);

		group.setValue(set("a")); // M1
		assertEquals(set("a"), group.getValue());
		group.addItem("x");
		group.setValue(set("x", "a")); // M2
		assertEquals(set("x", "a"), group.getValue());
		group.setValue("a"); // M3
		assertEquals(set("x", "a"), group.getValue());
		group.setValue(null); // M4
		assertEquals(Collections.emptySet(), group.getValue());
		assertEquals(3, events.get());

		group.select("z"); // M5
		assertEquals(Collections.emptySet(), group.getValue());
		assertEquals(3, events.get());
		group.select("x"); // M6
		assertEquals(set("x"), group.getValue());
		assertEquals(4, events.get());
	}

	/** Control: KeepValueComboBox replaces a Vaadin 8 combo box, which took a value that is not an item (2b-09). */
	@Test
	public void keepValueComboBoxKeepsVaadin8Rule() {
		KeepValueComboBox<String> combo = new KeepValueComboBox<>();
		combo.setItems("x");
		combo.setValue("a");
		assertEquals("a", combo.getValue());
	}
}
