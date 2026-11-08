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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

import de.symeda.sormas.api.event.TypeOfPlace;
import de.symeda.sormas.ui.utils.FieldHelper;
import de.symeda.sormas.ui.utils.NullableOptionGroup;

/**
 * Ticket 2b-04 (sweep 2b-002 RC1): V7 {@code AbstractSelect.setValue} in multi select ignores a value that is not a
 * collection and stores a collection as a set. {@code CaseCreateForm.setValue} sets {@code TypeOfPlace.FACILITY} on
 * the place-of-stay group, which stays a multi select for health facility users (read-only, so never required); the
 * shim stored the enum and the {@code setVisibleWhen} listener failed with a {@code ClassCastException}. Expected values
 * and event counts are those of the same steps on the 8.14.3 classes with the old app's {@code NullableOptionGroup}
 * (PR evidence {@code v7-result.txt}).
 */
public class OptionGroupNonCollectionValueTest {

	private static NullableOptionGroup placeOfStay(AtomicInteger events) {
		NullableOptionGroup field = new NullableOptionGroup("place of stay");
		field.removeAllItems();
		for (TypeOfPlace place : TypeOfPlace.FOR_CASES) {
			field.addItem(place);
		}
		field.addValueChangeListener(e -> events.incrementAndGet());
		return field;
	}

	/** The CaseCreateForm sequence for a health facility user (HospInf). */
	@Test
	public void healthFacilityUserCaseCreateSequence() {
		AtomicInteger events = new AtomicInteger();
		NullableOptionGroup field = placeOfStay(events);
		TextField target = new TextField();
		// as CaseCreateForm: the facility fields are visible only for FACILITY
		FieldHelper.setVisibleWhen(field, Collections.singletonList(target), Collections.singletonList(TypeOfPlace.FACILITY), false);
		events.set(0);

		field.setValue(Sets.newHashSet(TypeOfPlace.FACILITY));
		field.setReadOnly(true);
		assertEquals(1, events.getAndSet(0));
		assertTrue(field.isMultiSelect());

		// CaseCreateForm.setValue: the case's health facility is the user's
		field.setValue(TypeOfPlace.FACILITY);
		assertEquals(0, events.get(), "V7 ignores the enum: no event");
		assertEquals(Collections.singleton(TypeOfPlace.FACILITY), field.getValue());
		assertEquals(TypeOfPlace.FACILITY, field.getNullableValue());
		assertEquals(TypeOfPlace.FACILITY, FieldHelper.getNullableSourceFieldValue(field));
		assertTrue(target.isVisible());

		field.setReadOnly(false);
		field.setValue(TypeOfPlace.HOME);
		assertEquals(0, events.get());
		assertEquals(Collections.singleton(TypeOfPlace.FACILITY), field.getValue());
	}

	/** Other users: the group is required, so a single select that takes the enum. */
	@Test
	public void requiredGroupTakesTheEnum() {
		AtomicInteger events = new AtomicInteger();
		NullableOptionGroup field = placeOfStay(events);
		field.setRequired(true);
		assertFalse(field.isMultiSelect());
		events.set(0);

		field.setValue(TypeOfPlace.FACILITY);
		assertEquals(1, events.get());
		assertEquals(TypeOfPlace.FACILITY, field.getValue());
		assertEquals(TypeOfPlace.FACILITY, FieldHelper.getNullableSourceFieldValue(field));
		// V7 threw a ClassCastException here; the shim returns the selected item
		assertEquals(TypeOfPlace.FACILITY, field.getNullableValue());
	}

	@Test
	public void multiSelectStoresACollectionAsASet() {
		AtomicInteger events = new AtomicInteger();
		NullableOptionGroup field = placeOfStay(events);
		events.set(0);

		field.setValue(Arrays.asList(TypeOfPlace.HOME));
		assertEquals(1, events.getAndSet(0));
		assertTrue(field.getValue() instanceof Set, "value is a set, not the list");
		assertEquals(Collections.singleton(TypeOfPlace.HOME), field.getValue());
		assertEquals(TypeOfPlace.HOME, field.getNullableValue());

		field.setValue(null);
		assertEquals(1, events.getAndSet(0));
		assertEquals(Collections.emptySet(), field.getValue());
		assertNull(field.getNullableValue());
	}

	@Test
	public void plainMultiSelectIgnoresANonCollection() {
		AtomicInteger events = new AtomicInteger();
		OptionGroup field = new OptionGroup("tags");
		field.setMultiSelect(true);
		field.addItems("a", "b");
		field.addValueChangeListener(e -> events.incrementAndGet());

		field.setValue("a");
		assertEquals(0, events.get());
		assertEquals(Collections.emptySet(), field.getValue());

		field.setValue(Arrays.asList("a"));
		assertEquals(1, events.getAndSet(0));
		assertEquals(new LinkedHashSet<>(Collections.singleton("a")), field.getValue());

		field.setValue("b");
		assertEquals(0, events.get());
		assertEquals(Collections.singleton("a"), field.getValue());
	}

	/** A plain single select is unchanged: it takes the item (1b-06 keeps a value that is not an item yet). */
	@Test
	public void singleSelectUnchanged() {
		OptionGroup field = new OptionGroup("one");
		field.addItems("a", "b");
		field.setValue("a");
		assertEquals("a", field.getValue());
		field.setValue(null);
		assertNull(field.getValue());
	}
}
