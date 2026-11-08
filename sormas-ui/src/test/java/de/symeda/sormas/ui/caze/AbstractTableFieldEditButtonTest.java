package de.symeda.sormas.ui.caze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.button.Button;

/**
 * Ticket 2b-06: a Flow grid does not report a click on a focusable element in a cell (the edit/view button of the action
 * column) as an item click, so the button must open the entry through its own listener, as in Vaadin 7.
 */
public class AbstractTableFieldEditButtonTest {

	public static class Entry {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	private static class TestTableField extends AbstractTableField<Entry> {

		private final List<Entry> editedEntries = new ArrayList<>();
		private final List<Boolean> createFlags = new ArrayList<>();

		TestTableField(boolean isEditAllowed) {
			super(null, isEditAllowed);
		}

		@Override
		public Class<Entry> getEntryType() {
			return Entry.class;
		}

		@Override
		protected void editEntry(Entry entry, boolean create, Consumer<Entry> commitCallback) {
			editedEntries.add(entry);
			createFlags.add(create);
		}

		@Override
		protected void updateColumns() {
		}

		@Override
		protected boolean isEmpty(Entry entry) {
			return false;
		}

		@Override
		protected boolean isModified(Entry oldEntry, Entry newEntry) {
			return false;
		}
	}

	@Test
	public void testEditButtonClickOpensEntryOnce() {

		TestTableField field = new TestTableField(true);
		Entry entry = new Entry();

		Button button = (Button) field.generateEditCell(entry);
		button.click();

		assertEquals(1, field.editedEntries.size());
		assertSame(entry, field.editedEntries.get(0));
		assertFalse(field.createFlags.get(0));
	}

	@Test
	public void testViewButtonClickOpensEntryOnce() {

		TestTableField field = new TestTableField(false);
		Entry entry = new Entry();

		Button button = (Button) field.generateEditCell(entry);
		button.click();

		assertEquals(1, field.editedEntries.size());
		assertSame(entry, field.editedEntries.get(0));
	}
}
