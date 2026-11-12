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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.component.Component;

/**
 * The V7 read-only rule for programmatic writes ({@code AbstractField.setValue}): a {@link Field} that is read-only
 * refuses a value that differs from its current value with a {@link Field.ReadOnlyException}; the value stays, no value
 * change event is fired, and the rest of the calling code (e.g. a value change listener) does not run. The same value is
 * accepted silently. Disabled fields that are not read-only accept writes, as in V7.
 * <p>
 * As in V7, the data source writes the value of a read-only field: a form's field group reading its bean into the
 * fields ({@code setItemDataSource}, {@code bind}, {@code discard}), see {@link #readFromDataSource}. Writes that value
 * change listeners make during such a read are checked again (V7 threw there too). A composite field writing its inner
 * fields to show its new value is not checked either, but its value change listeners' writes are.
 * <p>
 * The shim fields call {@link #write} from {@code setValue}. Fields that replace Vaadin 8 fields opt out (Vaadin 8
 * read-only blocks only the user), see {@link CustomField#isReadOnlyWriteRefused()}.
 */
public final class ReadOnlyWrites {

	private static final ThreadLocal<State> STATE = new ThreadLocal<>();

	private ReadOnlyWrites() {
	}

	private static final class State {

		/** Nesting of field writes: a write made inside another field's write (by a listener) is one level deeper. */
		private int depth;
		/** The field whose write is innermost; a subclass passing the write on to its superclass is the same write. */
		private Field<?> writing;
		/** The innermost write is still showing its value (before its value change listeners run). */
		private boolean presenting;
		/** The innermost write comes from the data source ({@link #readFromDataSource}). */
		private boolean fromDataSource;
		private final Deque<Scope> scopes = new ArrayDeque<>();
	}

	private static final class Scope {

		private final Collection<? extends Field<?>> fields;
		private final int depth;

		private Scope(Collection<? extends Field<?>> fields, int depth) {
			this.fields = fields;
			this.depth = depth;
		}
	}

	private static State state() {
		State state = STATE.get();
		if (state == null) {
			state = new State();
			STATE.set(state);
		}
		return state;
	}

	private static void release(State state) {
		if (state.depth == 0 && state.scopes.isEmpty()) {
			STATE.remove();
		}
	}

	/**
	 * Runs a read of the data source into the given fields (V7 {@code setPropertyDataSource}, {@code discard}, the
	 * field group's {@code setItemDataSource}): their writes made directly by {@code read} are not checked. Writes made
	 * by value change listeners during the read, and writes of other fields, are.
	 */
	public static void readFromDataSource(Collection<? extends Field<?>> fields, Runnable read) {
		State state = state();
		state.scopes.push(new Scope(fields, state.depth));
		try {
			read.run();
		} finally {
			state.scopes.pop();
			release(state);
		}
	}

	/**
	 * Writes {@code newValue} with {@code write} unless the V7 rule refuses it.
	 *
	 * @throws Field.ReadOnlyException
	 *             if the field is read-only, the value differs from its current value ({@code equals}) and the write does
	 *             not come from the data source
	 */
	public static void write(Field<?> field, Object newValue, Runnable write) {
		State state = state();
		if (state.writing == field) {
			// the same write, passed on by a subclass's setValue to its superclass's: checked once
			write.run();
			return;
		}
		try {
			boolean fromDataSource = isDataSourceWrite(state, field);
			if (field.isReadOnly()
				&& !Objects.equals(newValue, field.getValue())
				&& !fromDataSource
				&& !isInsideWritingField(state, field)) {
				throw new Field.ReadOnlyException();
			}
			Field<?> outer = state.writing;
			boolean outerPresenting = state.presenting;
			boolean outerFromDataSource = state.fromDataSource;
			state.writing = field;
			state.presenting = true;
			state.fromDataSource = fromDataSource;
			state.depth++;
			try {
				write.run();
			} finally {
				state.depth--;
				state.writing = outer;
				state.presenting = outerPresenting;
				state.fromDataSource = outerFromDataSource;
			}
		} finally {
			release(state);
		}
	}

	/**
	 * Whether a write of {@code field} made now comes from the data source ({@link #readFromDataSource}): the form's field
	 * group reading its bean into the field, not code calling {@code setValue} (also not a value change listener during
	 * such a read). The V7 select rule uses it ({@link ComboBox#setValue}, {@link OptionGroup#setValue}): V7 took a data
	 * source's value whatever the items were, but ignored a {@code setValue} of a value that is not an item.
	 */
	public static boolean isDataSourceWrite(Field<?> field) {
		State state = STATE.get();
		if (state == null) {
			return false;
		}
		if (state.writing == field) {
			// the same write, passed on by a subclass's setValue to its superclass's
			return state.fromDataSource;
		}
		return isDataSourceWrite(state, field);
	}

	/**
	 * Called by the first value change listener of a composite field ({@link CustomField}): the field has shown its new
	 * value; writes made by its value change listeners are checked again (V7 threw there).
	 */
	static void presented(Field<?> field) {
		State state = STATE.get();
		if (state != null && state.writing == field) {
			state.presenting = false;
		}
	}

	/**
	 * A composite field shows its new value in its own inner fields ({@code setPresentationValue}). Not refused: in V7
	 * those inner fields were not read-only at that point ({@code DateTimeField} did not pass read-only on to them), or
	 * the composite had taken its value already ({@code UserField}).
	 */
	private static boolean isInsideWritingField(State state, Field<?> field) {
		if (state.writing == null || !state.presenting) {
			return false;
		}
		Component writing = state.writing.asComponent();
		Optional<Component> parent = field.asComponent().getParent();
		while (parent.isPresent()) {
			if (parent.get() == writing) {
				return true;
			}
			parent = parent.get().getParent();
		}
		return false;
	}

	private static boolean isDataSourceWrite(State state, Field<?> field) {
		Scope scope = state.scopes.peek();
		return scope != null && scope.depth == state.depth && scope.fields.contains(field);
	}
}
