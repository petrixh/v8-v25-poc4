/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2021 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;

import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;
import de.symeda.sormas.ui.utils.fields.CustomField;
import de.symeda.sormas.ui.utils.fields.Field;
import de.symeda.sormas.ui.utils.fields.TextField;

public class JsonForm extends CustomField<Map<String, String>> {

	private static final long serialVersionUID = 5762983268792751775L;

	/**
	 * Was a two-column {@code GridLayout}.
	 */
	private final FormLayout gridLayout;
	private final SormasFieldGroupFieldFactory fieldFactory;

	private Field<?>[] fields;

	public JsonForm(FieldVisibilityCheckers fieldVisibilityCheckers, UiFieldAccessCheckers fieldAccessCheckers) {
		super();
		setWidthFull();

		gridLayout = new FormLayout();
		gridLayout.setWidthFull();
		gridLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
		CssStyles.style(gridLayout, CssStyles.VSPACE_3);

		fieldFactory = new SormasFieldGroupFieldFactory(fieldVisibilityCheckers, fieldAccessCheckers);
	}

	@Override
	protected Component initContent() {
		return gridLayout;
	}

	@Override
	public Class<? extends Map<String, String>> getType() {
		return (Class<Map<String, String>>) (Class) Map.class;
	}

	@Override
	protected void setPresentationValue(Map<String, String> newValue) {
		doSetValue(newValue);
	}

	private void doSetValue(Map<String, String> value) {
		gridLayout.removeAll();

		if (value != null) {
			fields = buildFields(value);
			for (Field<?> field : fields) {
				gridLayout.add(field.asComponent());
			}
		}
	}

	/**
	 * As in V7, the value is the set value with the edited texts of the fields.
	 */
	@Override
	public Map<String, String> getValue() {
		Map<String, String> data = new HashMap<>(super.getValue());

		for (Field<?> field : fields) {
			data.put(field.getId().orElse(null), (String) field.getValue());
		}

		return data;
	}

	private Field<?>[] buildFields(Map<String, String> jsonData) {

		return jsonData.entrySet().stream().map((e) -> {
			final TextField textField = fieldFactory.createField(String.class, TextField.class);
			textField.setWidthFull();
			textField.setId(e.getKey());
			textField.setCaption(e.getKey());
			CssStyles.style(textField, CssStyles.TEXTFIELD_ROW);

			textField.setValue(e.getValue());

			return textField;
		}).collect(Collectors.toList()).toArray(new Field[] {});
	}
}
