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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;

import de.symeda.sormas.api.task.TaskDto;
import de.symeda.sormas.api.user.UserReferenceDto;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.utils.AbstractForm.BeanFieldGroup;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;

/**
 * Vaadin 7 {@code AbstractSelect} value rules of the select shims: adding items never changes the value (also a value
 * that is not an item yet, such as a bound property read before the form fills the items); removing the selected item
 * or all items clears it; removing another item keeps it. A value that is not an item gets there only through the bean
 * read: a direct {@code setValue} of a non-item is ignored (2b-12, {@link SelectNonItemWriteTest}).
 */
public class SelectItemsValueTest {

	private static UserReferenceDto user(String uuid) {
		return new UserReferenceDto(uuid, "First" + uuid, "Last" + uuid);
	}

	/**
	 * 2b-12: V7 ignored a direct {@code setValue} of a value that is not an item (it took it before 2b-12); the value
	 * that is not an item yet comes from the bean read here, as in the forms.
	 */
	@Test
	public void comboBoxKeepsValueWhileItemsAreAdded() {
		BeanFieldGroup<TaskDto> group = new BeanFieldGroup<>(TaskDto.class);
		ComboBox combo = new ComboBox();
		group.bind(combo, TaskDto.ASSIGNEE_USER);
		TaskDto task = new TaskDto();
		task.setAssigneeUser(user("U2"));
		group.setItemDataSource(task);
		AtomicInteger events = new AtomicInteger();
		combo.addValueChangeListener(e -> events.incrementAndGet());

		combo.addItem(user("U1"));
		combo.addItem(user("U2"));
		combo.addItems(Arrays.asList(user("U3"), user("U4")));

		assertEquals(user("U2"), combo.getValue());
		assertEquals(0, events.get());
	}

	@Test
	public void comboBoxClearsValueOnlyWhenItsItemIsRemoved() {
		ComboBox combo = new ComboBox();
		combo.addItems(Arrays.asList("a", "b", "c"));
		combo.setValue("b");

		combo.removeItem("a");
		assertEquals("b", combo.getValue());

		combo.getSelectItems().retainAll(Arrays.asList("b", "c"));
		assertEquals("b", combo.getValue());

		combo.removeItem("b");
		assertNull(combo.getValue());

		combo.setValue("c");
		combo.getSelectItems().retainAll(Collections.singletonList("a"));
		assertNull(combo.getValue());
	}

	@Test
	public void comboBoxRemoveAllItemsClearsValue() {
		ComboBox combo = new ComboBox();
		combo.addItem("x");
		combo.setValue("x");
		combo.removeAllItems();
		assertNull(combo.getValue());

		// FieldHelper.updateItems: remove all, add, set the value again
		combo.addItems(Arrays.asList("a", "b"));
		combo.setValue("b");
		combo.removeAllItems();
		combo.addItems(Arrays.asList("b", "c"));
		combo.setValue("b");
		assertEquals("b", combo.getValue());

		// 2b-12: the value is not among the new items: cleared, as in V7
		combo.removeAllItems();
		combo.addItems(Arrays.asList("c", "d"));
		combo.setValue("b");
		assertNull(combo.getValue());
	}

	/**
	 * TaskEditForm: the bean is read first, then the assignable users are added one by one (other instances, equal by
	 * uuid). The assignee must stay, and a commit must not change it.
	 */
	@Test
	public void boundComboBoxKeepsValueWhenItemsAreFilledAfterRead() {
		for (boolean buffered : new boolean[] {
			true,
			false }) {
			BeanFieldGroup<TaskDto> group = new BeanFieldGroup<>(TaskDto.class);
			group.setFieldFactory(new SormasFieldGroupFieldFactory(new FieldVisibilityCheckers(), UiFieldAccessCheckers.getNoop()));
			group.setBuffered(buffered);
			ComboBox assignee = new ComboBox();
			group.bind(assignee, TaskDto.ASSIGNEE_USER);

			TaskDto task = new TaskDto();
			task.setAssigneeUser(user("U2"));
			group.setItemDataSource(task);
			assertEquals(user("U2"), assignee.getValue());

			List<UserReferenceDto> users = Arrays.asList(user("U1"), user("U2"), user("U3"));
			for (UserReferenceDto u : users) {
				assignee.addItem(u);
				assignee.setItemCaption(u, u.getCaption() + " (0)");
			}

			assertEquals(user("U2"), assignee.getValue(), buffered ? "buffered" : "unbuffered");
			// the client shows the value with the caption set after it (V7: the item caption of the value)
			assignee.getSelectItems().flushRefresh();
			String selectedItem = String.valueOf(assignee.getElement().getPropertyRaw("selectedItem"));
			assertTrue(selectedItem.contains("FirstU2 LASTU2 (0)"), selectedItem);
			assertEquals(user("U2"), task.getAssigneeUser());
			group.commit();
			assertEquals(user("U2"), task.getAssigneeUser());
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void multiOptionGroupKeepsValueWhileItemsAreAdded() {
		OptionGroup group = new OptionGroup();
		group.setMultiSelect(true);
		group.setValue(new LinkedHashSet<>(Arrays.asList("b", "c")));

		group.addItem("a");
		group.addItem("b");
		group.addItem("c");
		// the refresh runs before the response (or at once without a UI); another test may leave a UI current
		group.getSelectItems().flushRefresh();

		assertEquals(new LinkedHashSet<>(Arrays.asList("b", "c")), group.getValue());
		assertEquals(
			new LinkedHashSet<>(Arrays.asList("b", "c")),
			new LinkedHashSet<>(((CheckboxGroup<Object>) group.getGroupComponent()).getValue()));

		group.removeItem("a");
		assertEquals(new LinkedHashSet<>(Arrays.asList("b", "c")), group.getValue());
		group.removeItem("b");
		assertEquals(Collections.singleton("c"), group.getValue());
		group.removeAllItems();
		assertEquals(Set.of(), group.getValue());
	}

	/** 2b-12: the value that is not an item yet comes from the bean read (a direct {@code setValue} is ignored). */
	@Test
	@SuppressWarnings("unchecked")
	public void singleOptionGroupKeepsValueWhileItemsAreAdded() {
		OptionGroup group = new OptionGroup();
		BeanFieldGroup<TaskDto> fieldGroup = new BeanFieldGroup<>(TaskDto.class);
		fieldGroup.bind(group, TaskDto.ASSIGNEE_USER);
		TaskDto task = new TaskDto();
		task.setAssigneeUser(user("b"));
		fieldGroup.setItemDataSource(task);

		group.addItem(user("a"));
		group.addItem(user("b"));
		group.getSelectItems().flushRefresh();

		assertEquals(user("b"), group.getValue());
		assertEquals(user("b"), ((RadioButtonGroup<Object>) group.getGroupComponent()).getValue());

		group.removeItem(user("a"));
		assertEquals(user("b"), group.getValue());
		group.removeItem(user("b"));
		assertNull(group.getValue());
	}
}
