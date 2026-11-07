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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;

import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.sample.PathogenTestType;
import de.symeda.sormas.api.sample.SampleDto;
import de.symeda.sormas.api.utils.YesNoUnknown;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.utils.AbstractForm.BeanFieldGroup;
import de.symeda.sormas.ui.utils.NullableOptionGroup;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;

/**
 * Ticket 2a-11: value change events of option groups when a form reads {@code null}, as Vaadin 7 (8.14.3
 * {@code AbstractField.setPropertyDataSource}: an event when the read value is {@code null} and the internal value was
 * not). A multi select starts with an empty set, so the first read of {@code null} fires; form listeners that hide
 * dependent fields unless the value is Yes (case reinfection, event evidence) run. Expected counts come from the same
 * scenarios run on the V7 classes with the old app's {@code NullableOptionGroup} (PR evidence {@code v7-result.txt}).
 */
public class OptionGroupNullReadEventTest {

	private static <T> BeanFieldGroup<T> group(Class<T> beanType, boolean buffered) {
		BeanFieldGroup<T> group = new BeanFieldGroup<>(beanType);
		group.setFieldFactory(new SormasFieldGroupFieldFactory(new FieldVisibilityCheckers(), UiFieldAccessCheckers.getNoop()));
		group.setBuffered(buffered);
		return group;
	}

	private static CaseDataDto caze(YesNoUnknown reinfection) {
		CaseDataDto caze = new CaseDataDto();
		caze.setReInfection(reinfection);
		return caze;
	}

	/** A click in the browser on the inner group (Flow {@code AbstractField.setModelValue(value, true)}). */
	private static void clientSets(OptionGroup field, Object value) throws Exception {
		Component inner = field.getGroupComponent();
		Method setModelValue = com.vaadin.flow.component.AbstractField.class.getDeclaredMethod("setModelValue", Object.class, boolean.class);
		setModelValue.setAccessible(true);
		setModelValue.invoke(inner, value, true);
	}

	private static final class Bound {

		final BeanFieldGroup<CaseDataDto> group;
		final NullableOptionGroup field;
		final AtomicInteger events = new AtomicInteger();
		/** what the form listener did last: true = dependents shown (value Yes), false = hidden, null = never ran */
		Boolean shown;

		Bound(boolean buffered, boolean required) {
			group = group(CaseDataDto.class, buffered);
			field = group.buildAndBind(CaseDataDto.RE_INFECTION, NullableOptionGroup.class);
			if (required) {
				field.setRequired(true);
			}
			// as CaseDataForm: the listener is added before the form reads its bean
			field.addValueChangeListener(e -> {
				events.incrementAndGet();
				NullableOptionGroup source = (NullableOptionGroup) e.getHasValue();
				// getNullableValue() is for the not required (multi select) group, as in the old app
				shown = (source.isMultiSelect() ? source.getNullableValue() : source.getValue()) == YesNoUnknown.YES;
			});
		}

		int read(YesNoUnknown value) {
			events.set(0);
			group.setItemDataSource(caze(value));
			return events.getAndSet(0);
		}

		int run(Runnable action) {
			events.set(0);
			action.run();
			return events.getAndSet(0);
		}
	}

	/**
	 * V7: "nog read null" 1 event; "read null again" 0; the listener hides the dependents. The read is not a change,
	 * and the untouched field writes {@code null} back (1b-12).
	 */
	@Test
	public void readingNullFiresOnceAndHides() {
		for (boolean buffered : new boolean[] {
			true,
			false }) {
			Bound b = new Bound(buffered, false);
			assertEquals(1, b.read(null), "first read of null");
			assertEquals(Boolean.FALSE, b.shown, "the listener hid the dependents");
			assertEquals(Collections.emptySet(), b.field.getValue());
			assertNull(b.field.getNullableValue());
			assertFalse(b.group.isModified(), "reading is not a change");
			assertFalse(b.field.isModified());

			assertEquals(0, b.read(null), "null read again (V7 internal value already null)");

			if (buffered) {
				CaseDataDto caze = caze(null);
				b.group.setItemDataSource(caze);
				caze.setReInfection(YesNoUnknown.NO);
				b.group.commit();
				assertNull(caze.getReInfection(), "untouched null is written back as null");
			}
		}
	}

	/** V7: "nog read YES, read null" 1 + 1; the listener shows, then hides. */
	@Test
	public void readingYesThenNull() {
		Bound b = new Bound(true, false);
		assertEquals(1, b.read(YesNoUnknown.YES));
		assertEquals(Boolean.TRUE, b.shown);
		assertEquals(1, b.read(null));
		assertEquals(Boolean.FALSE, b.shown);
		assertEquals(1, b.read(YesNoUnknown.NO));
		assertEquals(0, b.read(YesNoUnknown.NO), "unchanged value, no event");
	}

	/**
	 * V7: "nog read YES; user deselects; read null" 1, 1, 0: the converter round trip made the cleared value
	 * {@code null}, so reading {@code null} afterwards changes nothing. The same after a programmatic clear of Yes.
	 */
	@Test
	public void readingNullAfterClearingFiresNothing() throws Exception {
		Bound b = new Bound(true, false);
		assertEquals(1, b.read(YesNoUnknown.YES));
		assertEquals(1, b.run(() -> {
			try {
				clientSets(b.field, new LinkedHashSet<>());
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}), "user deselects Yes");
		assertEquals(Boolean.FALSE, b.shown);
		assertEquals(0, b.read(null));

		Bound p = new Bound(true, false);
		assertEquals(1, p.read(YesNoUnknown.YES));
		assertEquals(1, p.run(() -> p.field.setNullableValue(null)));
		assertEquals(0, p.read(null));
	}

	/**
	 * A clear before the bean is read (e.g. a visibility rule clearing a hidden field) leaves V7's empty set: V7
	 * "nog unbound fresh; setValue(null)" 0 events, and the read of {@code null} still fires.
	 */
	@Test
	public void clearBeforeReadStillFiresOnRead() {
		Bound b = new Bound(true, false);
		assertEquals(0, b.run(() -> b.field.clear()));
		assertEquals(0, b.run(() -> b.field.setValue(null)));
		assertEquals(1, b.read(null));
		assertEquals(Boolean.FALSE, b.shown);
	}

	/**
	 * Unchanged empty selection, no event (1b-05, 1a-32f). Note: V7 fired here ("nog read null; setValue(null)" and
	 * "clear()" 1 event each, a null-to-null event); not copied, see the PR.
	 */
	@Test
	public void programmaticEmptyAfterReadFiresNothing() {
		Bound b = new Bound(true, false);
		assertEquals(1, b.read(null));
		assertEquals(0, b.run(() -> b.field.setValue(null)));
		assertEquals(0, b.run(() -> b.field.clear()));
		assertEquals(0, b.run(() -> b.field.setNullableValue(null)));
		assertEquals(0, b.run(() -> b.field.setValue(Collections.singleton(null))));
		assertEquals(0, b.read(null), "still V7's null: no event on the next read");
	}

	/** Discard re-reads the bean: V7 "nog read null; discard" 1, 0; "read YES; setValue(null); discard" 1, 1, 1. */
	@Test
	public void discard() {
		Bound b = new Bound(true, false);
		assertEquals(1, b.read(null));
		assertEquals(0, b.run(() -> b.group.discard()));

		Bound y = new Bound(true, false);
		assertEquals(1, y.read(YesNoUnknown.YES));
		assertEquals(1, y.run(() -> y.field.setValue(null)));
		assertEquals(1, y.run(() -> y.group.discard()));
		assertEquals(Boolean.TRUE, y.shown);
	}

	/** Items replaced after the read keep the empty value; the next read of null fires nothing (V7 internal null). */
	@Test
	public void itemChangesAfterRead() {
		Bound b = new Bound(true, false);
		assertEquals(1, b.read(null));
		b.run(() -> {
			b.field.removeAllItems();
			b.field.addItems(Arrays.asList(YesNoUnknown.values()));
		});
		assertEquals(Collections.emptySet(), b.field.getValue());
		assertEquals(0, b.read(null));
		assertEquals(1, b.read(YesNoUnknown.YES), "Yes still selectable after the item change");
	}

	/**
	 * Required (single select, no converter): V7 "nog required read null" 0, "read YES, read null" 1 + 1. Switching
	 * back to not required starts again from an empty set: V7 "setRequired(false)" internal [], so null fires again.
	 */
	@Test
	public void requiredSingleSelectAndModeSwitch() {
		Bound r = new Bound(true, true);
		assertEquals(0, r.read(null));
		assertEquals(1, r.read(YesNoUnknown.YES));
		assertEquals(1, r.read(null));

		Bound b = new Bound(true, false);
		assertEquals(1, b.read(null));
		// setRequired(true): the shim fires once (empty set -> null), V7 none (its internal value was already null);
		// as before this ticket, not asserted
		b.run(() -> b.field.setRequired(true));
		assertEquals(1, b.run(() -> b.field.setRequired(false)), "single null -> empty set (V7: 1)");
		assertEquals(1, b.read(null), "empty set -> null fires again (V7)");
	}

	/**
	 * Plain multi select bound to a {@code Set} (no converter): V7 "multi read null" 1, "read null, read null" 1, 0,
	 * "read [], read null" 0, 1, "read [a], read null" 1, 1; "read null; setValue(null)" makes it an empty set (V7
	 * internal []), so the next read of null fires again. Writes back as 1b-12. Not covered: reading an empty set
	 * after a null (V7 fired, null to []; the shim does not, see the PR).
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void plainMultiSelect() {
		BeanFieldGroup<SampleDto> group = group(SampleDto.class, true);
		OptionGroup tests = new OptionGroup();
		group.bind(tests, SampleDto.REQUESTED_PATHOGEN_TESTS);
		tests.setMultiSelect(true);
		tests.addItems(Arrays.asList(PathogenTestType.values()));
		AtomicInteger events = new AtomicInteger();
		tests.addValueChangeListener(e -> events.incrementAndGet());

		SampleDto sample = new SampleDto();
		group.setItemDataSource(sample);
		assertEquals(1, events.getAndSet(0), "read null");
		group.setItemDataSource(sample);
		assertEquals(0, events.getAndSet(0), "read null again");
		group.commit();
		assertNull(sample.getRequestedPathogenTests());

		tests.setValue(null);
		assertEquals(0, events.getAndSet(0));
		group.setItemDataSource(sample);
		assertEquals(1, events.getAndSet(0), "after setValue(null) V7 held an empty set");

		tests.setValue(null);
		SampleDto empty = new SampleDto();
		empty.setRequestedPathogenTests(new LinkedHashSet<>());
		group.setItemDataSource(empty);
		assertEquals(0, events.getAndSet(0), "read [] into [] (V7 0)");
		group.setItemDataSource(sample);
		assertEquals(1, events.getAndSet(0), "then null (V7 1)");

		SampleDto one = new SampleDto();
		one.setRequestedPathogenTests(new LinkedHashSet<>(Set.of(PathogenTestType.CULTURE)));
		group.setItemDataSource(one);
		assertEquals(1, events.getAndSet(0));
		group.setItemDataSource(sample);
		assertEquals(1, events.getAndSet(0));
		assertTrue(tests.isEmpty());
	}

	/**
	 * Unbuffered form: a change after a {@code null} read is written to the bean at once (the read marker is cleared
	 * before the change event the binder writes on).
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void unbufferedChangeAfterNullReadIsWritten() {
		BeanFieldGroup<SampleDto> group = group(SampleDto.class, false);
		OptionGroup tests = new OptionGroup();
		group.bind(tests, SampleDto.REQUESTED_PATHOGEN_TESTS);
		tests.setMultiSelect(true);
		tests.addItems(Arrays.asList(PathogenTestType.values()));
		SampleDto sample = new SampleDto();
		group.setItemDataSource(sample);
		assertNull(sample.getRequestedPathogenTests());
		tests.setValue(new LinkedHashSet<>(Set.of(PathogenTestType.CULTURE)));
		assertEquals(Set.of(PathogenTestType.CULTURE), sample.getRequestedPathogenTests());

		Bound b = new Bound(false, false);
		CaseDataDto caze = caze(null);
		b.group.setItemDataSource(caze);
		b.field.setNullableValue(YesNoUnknown.YES);
		assertEquals(YesNoUnknown.YES, caze.getReInfection());
	}

	/** Plain single select: V7 "single read null" 0, "read YES, read null" 1 + 1 (unchanged by this ticket). */
	@Test
	public void plainSingleSelect() {
		BeanFieldGroup<CaseDataDto> group = group(CaseDataDto.class, true);
		OptionGroup single = new OptionGroup();
		single.addItems(Arrays.asList(YesNoUnknown.values()));
		group.bind(single, CaseDataDto.RE_INFECTION);
		AtomicInteger events = new AtomicInteger();
		single.addValueChangeListener(e -> events.incrementAndGet());
		group.setItemDataSource(caze(null));
		assertEquals(0, events.getAndSet(0));
		group.setItemDataSource(caze(YesNoUnknown.YES));
		assertEquals(1, events.getAndSet(0));
		group.setItemDataSource(caze(null));
		assertEquals(1, events.getAndSet(0));
		assertNull(single.getValue());
	}
}
