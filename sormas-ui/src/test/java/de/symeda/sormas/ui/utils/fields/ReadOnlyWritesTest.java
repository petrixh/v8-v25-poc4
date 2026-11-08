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
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;

import de.symeda.sormas.api.event.EventDto;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.utils.AbstractForm.BeanFieldGroup;
import de.symeda.sormas.ui.utils.DateTimeField;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;

/**
 * Ticket 2b-07: the V7 read-only rule for programmatic writes ({@link ReadOnlyWrites}). Each case is a row of the V7
 * oracle (8.14.3 compatibility classes, PR evidence {@code v7-result.txt}, probe {@code V7Probe2b07.java}): a read-only
 * field refuses a different value with a {@code ReadOnlyException}, keeps its value and fires no event; the same value
 * is accepted; the field group's read of the bean (the V7 data source) is not refused, but a listener's write during
 * that read is.
 */
public class ReadOnlyWritesTest {

	public static class Bean {

		private String name = "a";
		private String other = "x";
		private Set<String> tags = new LinkedHashSet<>(Collections.singleton("A"));
		private String text;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getOther() {
			return other;
		}

		public void setOther(String other) {
			this.other = other;
		}

		public Set<String> getTags() {
			return tags;
		}

		public void setTags(Set<String> tags) {
			this.tags = tags;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	static class TextCustomField extends CustomField<String> {

		private final boolean refused;

		TextCustomField(boolean refused) {
			this.refused = refused;
		}

		@Override
		protected Component initContent() {
			return new Span();
		}

		@Override
		protected boolean isReadOnlyWriteRefused() {
			return refused;
		}
	}

	/** A composite that shows its value in an inner field and passes read-only on (as the new DateTimeField does). */
	static class Composite extends CustomField<String> {

		private final TextField inner = textField();

		Composite() {
			getContent();
		}

		@Override
		protected Component initContent() {
			return inner;
		}

		@Override
		protected void setPresentationValue(String value) {
			inner.setValue(value);
		}

		@Override
		public void setReadOnly(boolean readOnly) {
			super.setReadOnly(readOnly);
			inner.setReadOnly(readOnly);
		}
	}

	private static <F extends Field<?>> F counted(F field, AtomicInteger events) {
		field.addValueChangeListener(e -> events.incrementAndGet());
		return field;
	}

	private static TextField textField() {
		TextField field = new TextField();
		field.setNullRepresentation("");
		return field;
	}

	@Test
	public void textFieldRefusesDifferentValue() {
		AtomicInteger events = new AtomicInteger();
		TextField field = counted(textField(), events);
		field.setValue("a");
		field.setReadOnly(true);
		events.set(0);

		assertThrows(Field.ReadOnlyException.class, () -> field.setValue("b"));
		assertEquals("a", field.getValue());
		field.setValue("a");
		assertThrows(Field.ReadOnlyException.class, field::clear);
		assertThrows(Field.ReadOnlyException.class, () -> field.setValue(null));
		assertEquals("a", field.getValue());
		assertEquals(0, events.get());

		TextField nullField = counted(textField(), events);
		nullField.setValue(null);
		nullField.setReadOnly(true);
		events.set(0);
		assertThrows(Field.ReadOnlyException.class, () -> nullField.setValue(""), "null and the null representation differ");
		nullField.setValue(null);
		assertThrows(Field.ReadOnlyException.class, nullField::clear);
		assertNull(nullField.getValue());
		assertEquals(0, events.get());

		TextField disabled = counted(textField(), events);
		disabled.setEnabled(false);
		events.set(0);
		disabled.setValue("b");
		assertEquals("b", disabled.getValue(), "disabled is not read-only");
		assertEquals(1, events.get());
	}

	@Test
	public void comboBoxRefusesDifferentValue() {
		AtomicInteger events = new AtomicInteger();
		ComboBox field = counted(new ComboBox(), events);
		field.addItems("item1", "item2");
		field.setValue("item1");
		field.setReadOnly(true);
		events.set(0);

		assertThrows(Field.ReadOnlyException.class, () -> field.setValue("item2"));
		field.setValue("item1");
		field.addItem("item3");
		field.removeItem("item2");
		assertThrows(Field.ReadOnlyException.class, () -> field.removeItem("item1"));
		assertEquals("item1", field.getValue());
		field.setReadOnly(false);
		field.addItem("item1");
		field.setReadOnly(true);
		assertThrows(Field.ReadOnlyException.class, field::removeAllItems);
		assertEquals("item1", field.getValue());
		assertEquals(0, events.get());

		ComboBox empty = counted(new ComboBox(), events);
		empty.addItems("item1");
		empty.setReadOnly(true);
		events.set(0);
		empty.removeAllItems();
		empty.setValue(null);
		assertEquals(0, events.get());
	}

	@Test
	public void optionGroupRefusesDifferentValue() {
		AtomicInteger events = new AtomicInteger();
		OptionGroup single = counted(new OptionGroup(), events);
		single.addItems("A", "B");
		single.setValue("A");
		single.setReadOnly(true);
		events.set(0);
		assertThrows(Field.ReadOnlyException.class, () -> single.select("B"));
		assertThrows(Field.ReadOnlyException.class, () -> single.setValue("B"));
		assertEquals("A", single.getValue());

		OptionGroup multi = counted(new OptionGroup(), events);
		multi.setMultiSelect(true);
		multi.addItems("A", "B");
		multi.setValue(new LinkedHashSet<>(Collections.singleton("A")));
		multi.setReadOnly(true);
		events.set(0);
		assertThrows(Field.ReadOnlyException.class, () -> multi.select("B"));
		multi.setValue(new LinkedHashSet<>(Collections.singleton("A")));
		multi.setValue(Collections.singletonList("A"));
		assertThrows(Field.ReadOnlyException.class, () -> multi.setValue(Collections.singleton("B")));
		multi.setValue("B");
		assertEquals(Collections.singleton("A"), multi.getValue());
		assertEquals(0, events.get());
	}

	@Test
	public void checkBoxDateFieldCustomField() {
		AtomicInteger events = new AtomicInteger();
		CheckBox checkBox = counted(new CheckBox(), events);
		checkBox.setValue(false);
		checkBox.setReadOnly(true);
		DateField dateField = counted(new DateField(), events);
		dateField.setReadOnly(true);
		TextCustomField customField = counted(new TextCustomField(true), events);
		customField.setReadOnly(true);
		events.set(0);

		assertThrows(Field.ReadOnlyException.class, () -> checkBox.setValue(true));
		assertThrows(Field.ReadOnlyException.class, () -> checkBox.setValue(null));
		assertEquals(Boolean.FALSE, checkBox.getValue());
		assertThrows(Field.ReadOnlyException.class, () -> dateField.setValue(new Date(0)));
		dateField.setValue(null);
		assertThrows(Field.ReadOnlyException.class, () -> customField.setValue("v"));
		assertNull(customField.getValue());
		assertEquals(0, events.get());

		// a field that replaces a Vaadin 8 custom field: read-only blocks only the user
		TextCustomField v8Field = new TextCustomField(false);
		v8Field.setReadOnly(true);
		v8Field.setValue("v");
		assertEquals("v", v8Field.getValue());
	}

	/** V7: the listener stops at the refused write; later listeners of the same field do not run. */
	@Test
	public void listenerStopsAtRefusedWrite() {
		TextField a = textField();
		TextField b = textField();
		b.setValue("b0");
		b.setReadOnly(true);
		List<String> trail = new ArrayList<>();
		a.addValueChangeListener(e -> {
			trail.add("before");
			b.setValue("b1");
			trail.add("after");
		});
		a.addValueChangeListener(e -> trail.add("second listener"));

		assertThrows(Field.ReadOnlyException.class, () -> a.setValue("x"));
		assertEquals("x", a.getValue());
		assertEquals(Collections.singletonList("before"), trail);
		assertEquals("b0", b.getValue());
	}

	/** The field group's bean read is the V7 data source: it writes read-only fields. */
	@Test
	public void fieldGroupReadsIntoReadOnlyFields() {
		BeanFieldGroup<Bean> group = new BeanFieldGroup<>(Bean.class);
		Bean bean1 = new Bean();
		group.setItemDataSource(bean1);
		AtomicInteger events = new AtomicInteger();
		TextField name = counted(textField(), events);
		TextField other = textField();
		OptionGroup tags = new OptionGroup();
		tags.setMultiSelect(true);
		tags.addItems("A", "B");
		TextCustomField text = new TextCustomField(true);
		group.bind(name, "name");
		group.bind(other, "other");
		group.bind(tags, "tags");
		group.bind(text, "text");
		name.setReadOnly(true);
		tags.setReadOnly(true);
		text.setReadOnly(true);

		Bean bean2 = new Bean();
		bean2.setName("b");
		bean2.setTags(new LinkedHashSet<>(Collections.singleton("B")));
		bean2.setText("t");
		events.set(0);
		group.setItemDataSource(bean2);
		assertEquals("b", name.getValue());
		assertEquals(Collections.singleton("B"), tags.getValue());
		assertEquals("t", text.getValue());
		assertEquals(1, events.get());
		group.commit();

		assertThrows(Field.ReadOnlyException.class, () -> name.setValue("c"));

		// written while editable, then made read-only: commit writes it (V7 too), discard shows the bean again
		name.setReadOnly(false);
		name.setValue("w");
		name.setReadOnly(true);
		group.commit();
		assertEquals("w", bean2.getName());
		name.setReadOnly(false);
		name.setValue("v");
		name.setReadOnly(true);
		group.discard();
		assertEquals("w", name.getValue());

		group.setReadOnly(true);
		group.setItemDataSource(bean1);
		assertEquals("a", name.getValue());
		assertThrows(Field.ReadOnlyException.class, () -> other.setValue("y"));
		assertEquals("x", other.getValue());
	}

	/**
	 * A composite showing a value read from the bean in its read-only inner field is not refused; its own value change
	 * listener writing the inner field afterwards is (V7: listeners' writes are checked).
	 */
	@Test
	public void compositeShowsReadValueInReadOnlyInnerField() {
		BeanFieldGroup<Bean> group = new BeanFieldGroup<>(Bean.class);
		Composite composite = new Composite();
		group.bind(composite, "text");
		composite.setReadOnly(true);

		Bean bean = new Bean();
		bean.setText("t1");
		group.setItemDataSource(bean);
		assertEquals("t1", composite.getValue());
		assertEquals("t1", composite.inner.getValue());

		composite.addValueChangeListener(e -> composite.inner.setValue("from listener"));
		Bean bean2 = new Bean();
		bean2.setText("t2");
		assertThrows(Field.ReadOnlyException.class, () -> group.setItemDataSource(bean2));
		assertEquals("t2", composite.inner.getValue());
	}

	/** The date and time field passes read-only on to its inner fields; reading a bean still shows the new value. */
	@Test
	public void readOnlyDateTimeFieldShowsReadValue() {
		DateTimeField startDate = new SormasFieldGroupFieldFactory(new FieldVisibilityCheckers(), UiFieldAccessCheckers.getNoop())
			.createField(Date.class, DateTimeField.class);
		BeanFieldGroup<EventDto> group = new BeanFieldGroup<>(EventDto.class);
		group.bind(startDate, EventDto.START_DATE);
		startDate.getContent();
		startDate.setReadOnly(true);

		EventDto event = EventDto.build();
		Date start = new Date(1_700_000_100_000L / 900_000L * 900_000L);
		event.setStartDate(start);
		group.setItemDataSource(event);
		assertEquals(start, startDate.getValue());
		assertThrows(Field.ReadOnlyException.class, () -> startDate.setValue(new Date(0)));
	}

	/** V7: a listener's write to a read-only field during the bean read is refused; the read stops there. */
	@Test
	public void listenerWriteDuringReadIsRefused() {
		BeanFieldGroup<Bean> group = new BeanFieldGroup<>(Bean.class);
		TextField name = textField();
		TextField other = textField();
		group.bind(name, "name");
		group.bind(other, "other");
		other.setReadOnly(true);
		List<String> trail = new ArrayList<>();
		name.addValueChangeListener(e -> {
			trail.add("listener");
			other.setValue("from listener");
			trail.add("after write");
		});

		Bean bean = new Bean();
		bean.setName("n3");
		assertThrows(Field.ReadOnlyException.class, () -> group.setItemDataSource(bean));
		assertEquals(Collections.singletonList("listener"), trail);
		assertEquals("n3", name.getValue());

		// outside any read, the rule applies again (no state left behind)
		other.setReadOnly(false);
		other.setValue("free");
		other.setReadOnly(true);
		assertThrows(Field.ReadOnlyException.class, () -> other.setValue("y"));
	}
}
