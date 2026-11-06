/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.ui.utils.CssStyles;

public abstract class AbstractInfoLayout<T> extends HorizontalLayout {

	private Class<T> dataType;

	private UiFieldAccessCheckers fieldAccessCheckers;

	public AbstractInfoLayout(Class<T> dataType, UiFieldAccessCheckers fieldAccessCheckers) {
		this.dataType = dataType;
		this.fieldAccessCheckers = fieldAccessCheckers;
	}

	protected Span addDescLabel(HasComponents layout, String fieldId, Object value, String caption) {
		return addCustomDescLabel(layout, dataType, fieldId, value, caption);
	}

	protected Span addCustomDescLabel(HasComponents layout, Class<?> dataType, String fieldId, Object value, String caption) {
		if (!fieldAccessCheckers.isAccessible(dataType, fieldId)
			|| (fieldAccessCheckers.isEmbedded(dataType, fieldId) && !fieldAccessCheckers.hasRight())) {

			Span label = addDescLabel(layout, I18nProperties.getCaption(Captions.inaccessibleValue), caption);
			label.addClassName(CssStyles.INACCESSIBLE_LABEL);
			label.setId("infoLayout_" + dataType.getSimpleName() + "_" + fieldId);

			return label;
		}

		return addDescLabel(layout, value, caption);
	}

	private Span addDescLabel(HasComponents layout, Object content, String caption) {

		String contentString = content != null ? content.toString() : "";
		Span label = new Span(contentString);
		label.setWidthFull();
		if (caption != null) {
			// Flow components have no caption: the caption goes above the label, as Vaadin 8 rendered it
			Span captionLabel = new Span(caption);
			captionLabel.addClassName("v-caption");
			Div captionedLabel = new Div(captionLabel, new Div(label));
			captionedLabel.setWidthFull();
			layout.add(captionedLabel);
		} else {
			layout.add(label);
		}
		return label;
	}
}
