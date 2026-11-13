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
package de.symeda.sormas.ui.utils;

/**
 * A value is not valid; replaces the Vaadin 7 {@code com.vaadin.v7.data.Validator.InvalidValueException} that
 * {@link Buffered#commit()} throws and {@link CommitDiscardWrapperComponent#commitAndHandle()} shows. An exception with
 * causes stands for several invalid values; an invisible one (validation not shown on its field) is not listed.
 */
public class InvalidValueException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private static final InvalidValueException[] NO_CAUSES = new InvalidValueException[0];

	private final InvalidValueException[] causes;
	private boolean invisible;

	public InvalidValueException(String message) {
		this(message, NO_CAUSES);
	}

	public InvalidValueException(String message, InvalidValueException... causes) {
		super(message);
		this.causes = causes != null ? causes : NO_CAUSES;
	}

	/**
	 * @return the message escaped for HTML, as in Vaadin 7
	 */
	public String getHtmlMessage() {
		String message = getLocalizedMessage();
		if (message == null) {
			return null;
		}
		return message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}

	public InvalidValueException[] getCauses() {
		return causes;
	}

	public boolean isInvisible() {
		return invisible;
	}

	public void setInvisible(boolean invisible) {
		this.invisible = invisible;
	}
}
