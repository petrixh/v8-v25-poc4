/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2023 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.utils.components;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.customizableenum.CustomizableEnum;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.fields.CustomField;

public class CustomizableEnumPropertiesComponent extends CustomField<Map<String, Object>> {

	private VerticalLayout layout;
	private Map<String, Class<?>> allProperties;
	private Map<String, Object> properties;

	@Override
	protected Component initContent() {

		layout = new VerticalLayout();
		layout.setWidthFull();
		// was MarginInfo(false, false, true, false): bottom margin only (look, parked)
		layout.setPadding(false);
		layout.setSpacing(false);
		CssStyles.style(layout, CssStyles.VSPACE_TOP_4);

		if (allProperties != null) {
			buildPropertyFields();
		}

		return layout;
	}

	@Override
	public Class<? extends Map<String, Object>> getType() {
		//noinspection unchecked,InstantiatingObjectToGetClassObject,InstantiatingObjectToGetClassObject
		return (Class<? extends Map<String, Object>>) new HashMap<String, Object>().getClass();
	}

	private void buildPropertyFields() {

		if (layout == null) {
			return;
		}

		layout.removeAll();

		allProperties.keySet().forEach(p -> {
			if (allProperties.get(p) == boolean.class) {
				Boolean value = (Boolean) properties.get(p);
				Checkbox checkBox = new Checkbox(I18nProperties.getPrefixCaption(CustomizableEnum.I18N_PREFIX, p), value != null ? value : false);
				CssStyles.style(checkBox, CssStyles.VSPACE_NONE);
				checkBox.addValueChangeListener(e -> properties.put(p, e.getValue()));
				layout.add(checkBox);
			} else {
				throw new UnsupportedOperationException(
					String.format("Class %s is not yet implemented for properties component", allProperties.get(p).getName()));
			}
		});
	}

	@Override
	public void setValue(Map<String, Object> newFieldValue) {
		super.setValue(newFieldValue);
		this.properties = newFieldValue != null ? newFieldValue : new HashMap<>();
	}

	/**
	 * Was V7 {@code getInternalValue()}: the value is the edited properties map.
	 */
	@Override
	public Map<String, Object> getValue() {
		return properties != null ? (!properties.isEmpty() ? properties : null) : null;
	}

	public void setAllProperties(Map<String, Class<?>> allProperties) {
		this.allProperties = allProperties;
		buildPropertyFields();
	}

	public boolean hasContent() {
		return !allProperties.isEmpty();
	}
}
