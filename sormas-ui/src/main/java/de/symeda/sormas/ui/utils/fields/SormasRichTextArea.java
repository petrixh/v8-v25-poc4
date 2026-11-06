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

/**
 * Stands in for the V7 {@code RichTextArea}: a plain {@link TextArea} that holds the HTML as text.
 * <p>
 * Vaadin 25's rich text editor is a commercial component, which this migration does not use (open question Q4). When
 * that is answered, only this class changes. Until then users see and edit the HTML markup, and nothing is sanitised
 * or rendered here (as before, callers that display the value as HTML must clean it).
 */
public class SormasRichTextArea extends TextArea {

	private static final long serialVersionUID = 1L;

	public SormasRichTextArea() {
		super();
	}

	public SormasRichTextArea(String caption) {
		super(caption);
	}
}
