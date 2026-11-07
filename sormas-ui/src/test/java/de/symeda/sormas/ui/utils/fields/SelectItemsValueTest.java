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
 * or all items clears it; removing another item keeps it.
 */
public class SelectItemsValueTest {

	private static UserReferenceDto user(String uuid) {
		return new UserReferenceDto(uuid, "First" + uuid, "Last" + uuid);
	}

	@Test
	public void comboBoxKeepsValueWhileItemsAreAdded() {
		ComboBox combo = new ComboBox();
		AtomicInteger events = new AtomicInteger();
		combo.setValue("b");
		combo.addValueChangeListener(e -> events.incrementAndGet());

		combo.addItem("a");
		combo.addItem("b");
		combo.addItems(Arrays.asList("c", "d"));

		assertEquals("b", combo.getValue());
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

	@Test
	@SuppressWarnings("unchecked")
	public void singleOptionGroupKeepsValueWhileItemsAreAdded() {
		OptionGroup group = new OptionGroup();
		group.setValue("b");

		group.addItem("a");
		group.addItem("b");
		group.getSelectItems().flushRefresh();

		assertEquals("b", group.getValue());
		assertEquals("b", ((RadioButtonGroup<Object>) group.getGroupComponent()).getValue());

		group.removeItem("a");
		assertEquals("b", group.getValue());
		group.removeItem("b");
		assertNull(group.getValue());
	}
}
