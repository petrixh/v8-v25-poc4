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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;

/**
 * Replaces the V7 {@code RichTextArea}: a Vaadin {@link RichTextEditor} (commercial component, see the README) whose
 * value is the HTML text, as before.
 * <p>
 * The editor is wrapped in a {@link CustomField} because the Flow editor has no label, required indicator or error
 * message: the custom field shows the caption, the required indicator and validation errors like the other SORMAS
 * fields. The value is the editor's HTML value ({@link RichTextEditor#asHtml()}); a new field has the value {@code ""}.
 * As in V7, the value can be {@code null} (shown empty): an untouched field read from a {@code null} property stays
 * {@code null}, see {@link Field#getNullRepresentation()}.
 * <p>
 * Sanitising: the editor cleans the HTML it shows and the HTML the user's edits produce (Vaadin's safelist: basic
 * formatting, lists, links, images, headings h1-h3, inline styles; scripts and event attributes are removed). A value set
 * from the bean stays as stored until the user edits the field. The V7 rich text area stored lines as {@code <div>}
 * elements, which that safelist drops: they are shown as paragraphs (see {@link #toEditorHtml(String)}).
 */
public class SormasRichTextArea extends CustomField<String> {

	private static final long serialVersionUID = 1L;

	private final RichTextEditor editor = new RichTextEditor();

	public SormasRichTextArea() {
		super("");
		editor.setWidthFull();
		editor.addValueChangeListener(event -> {
			if (event.isFromClient()) {
				// V7 RichTextArea.changeVariables: an empty edit keeps a null value null
				setModelValue(AbstractTextField.valueFromClient(this, generateModelValue(), getValue() == null), true);
			}
		});
	}

	public SormasRichTextArea(String caption) {
		this();
		setCaption(caption);
	}

	@Override
	protected Component initContent() {
		return editor;
	}

	/**
	 * The editor inside the field, e.g. for its toolbar texts ({@link RichTextEditor#setI18n}).
	 */
	public RichTextEditor getEditor() {
		return editor;
	}

	@Override
	protected String generateModelValue() {
		return editor.asHtml().getValue();
	}

	/**
	 * Through {@link RichTextEditor#asHtml()}: {@code RichTextEditor.setValue} rejects values that start with a square
	 * bracket or a curly brace (it takes them for the Delta format), which a stored plain-text value may do.
	 */
	@Override
	protected void setPresentationValue(String newPresentationValue) {
		editor.asHtml().setValue(toEditorHtml(newPresentationValue));
	}

	/**
	 * HTML for the editor. Lines the V7 rich text area stored as {@code <div>} elements become paragraphs, because the
	 * editor's sanitising drops {@code <div>} and the lines would run together; a {@code <div>} holding other blocks is
	 * unwrapped. Other HTML is passed on unchanged. Only what the editor shows changes: the stored value changes only
	 * when the user edits the field.
	 */
	static String toEditorHtml(String html) {
		if (html == null) {
			return "";
		}
		if (!html.toLowerCase(Locale.ROOT).contains("<div")) {
			return html;
		}
		Document document = Jsoup.parseBodyFragment(html);
		document.outputSettings().prettyPrint(false);
		List<Element> divs = new ArrayList<>(document.body().select("div"));
		// innermost first, so a div whose child divs became paragraphs is unwrapped
		for (int i = divs.size() - 1; i >= 0; i--) {
			Element div = divs.get(i);
			if (div.children().stream().anyMatch(Element::isBlock)) {
				div.unwrap();
			} else {
				div.tagName("p");
			}
		}
		return document.body().html();
	}

	@Override
	public boolean isEmpty() {
		return editor.isEmpty();
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		super.setReadOnly(readOnly);
		editor.setReadOnly(readOnly);
	}
}
