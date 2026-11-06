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
package de.symeda.sormas.ui.utils.grid.renderers;

import java.io.Serializable;

import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.ValueProvider;

/**
 * A renderer of one column value, as in Vaadin 7 and 8, where a grid renderer turned the column's value into what the
 * cell shows ({@code encode(value)}). Flow renderers render a whole row item; {@link #createRenderer(ValueProvider)}
 * builds one from the column's value provider. The output is text (escaped) or, for {@link #isHtml()} renderers, HTML
 * put into the cell as is, like the Vaadin 8 {@code HtmlRenderer}.
 *
 * @param <V>
 *            the value type the renderer accepts
 */
public abstract class ValueRenderer<V> implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String nullRepresentation;

	protected ValueRenderer(String nullRepresentation) {
		this.nullRepresentation = nullRepresentation;
	}

	/**
	 * @return what the cell shows for a {@code null} value (default: empty)
	 */
	public String getNullRepresentation() {
		return nullRepresentation;
	}

	/**
	 * The text or HTML the cell shows for the value; {@code null} shows an empty cell. Subclasses override this as they
	 * overrode {@code encode} in Vaadin 7 and 8 (which returned JSON).
	 */
	public String encode(V value) {
		return value == null ? getNullRepresentation() : value.toString();
	}

	/**
	 * @return {@code true} if {@link #encode} returns HTML (shown unescaped), {@code false} for text
	 */
	public abstract boolean isHtml();

	/**
	 * @return a Flow renderer showing {@link #encode} of the value the provider gives for each row
	 */
	public <T> Renderer<T> createRenderer(ValueProvider<T, ? extends V> valueProvider) {
		ValueProvider<T, String> output = item -> {
			String encoded = encode(valueProvider.apply(item));
			return encoded != null ? encoded : "";
		};
		if (isHtml()) {
			return LitRenderer.<T> of("<span .innerHTML=\"${item.html}\"></span>").withProperty("html", output);
		}
		return LitRenderer.<T> of("${item.text}").withProperty("text", output);
	}
}
