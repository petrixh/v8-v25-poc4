package de.symeda.sormas.ui.utils;

import java.io.Serializable;

/**
 * Buffered editing of a form or field group: replaces the Vaadin 7 {@code com.vaadin.v7.data.Buffered} that
 * {@link CommitDiscardWrapperComponent} works with. The form layer's field group implements it (on a Flow
 * {@code Binder}: commit = write the bean, discard = read it again, modified = has changes), and so may a wrapped
 * component that commits by itself.
 */
public interface Buffered extends Serializable {

	/**
	 * Writes the buffered values.
	 *
	 * @throws InvalidValueException
	 *             if a value is not valid; the causes name the invalid values
	 * @throws SourceException
	 *             if writing failed for another reason
	 */
	void commit() throws InvalidValueException, SourceException;

	/**
	 * Drops the buffered changes and reads the values again.
	 */
	void discard() throws SourceException;

	boolean isModified();

	void setBuffered(boolean buffered);

	boolean isBuffered();

	/**
	 * @return whether all values are valid (Vaadin 7 {@code FieldGroup.isValid()})
	 */
	default boolean isValid() {
		return true;
	}

	/**
	 * @return whether the values cannot be edited (Vaadin 7 {@code FieldGroup.isReadOnly()})
	 */
	default boolean isReadOnly() {
		return false;
	}

	/**
	 * Was {@code com.vaadin.v7.data.Buffered.SourceException}: writing to or reading from the data source failed.
	 */
	class SourceException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public SourceException(String message, Throwable cause) {
			super(message, cause);
		}

		public SourceException(Throwable cause) {
			super(cause);
		}
	}
}
