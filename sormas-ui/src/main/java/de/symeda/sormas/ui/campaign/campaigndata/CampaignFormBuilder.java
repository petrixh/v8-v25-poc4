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

package de.symeda.sormas.ui.campaign.campaigndata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.fields.Field;
import de.symeda.sormas.ui.utils.fields.TextField;

import de.symeda.sormas.api.campaign.data.CampaignFormDataEntry;
import de.symeda.sormas.api.campaign.data.translation.TranslationElement;
import de.symeda.sormas.api.campaign.form.CampaignFormElement;
import de.symeda.sormas.api.campaign.form.CampaignFormElementStyle;
import de.symeda.sormas.api.campaign.form.CampaignFormElementType;
import de.symeda.sormas.api.campaign.form.CampaignFormTranslations;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.NullableOptionGroup;
import de.symeda.sormas.ui.utils.NumberNumericValueValidator;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;

public class CampaignFormBuilder {

	private final List<CampaignFormElement> formElements;
	private final Map<String, Object> formValuesMap;
	private final GridLayout campaignFormLayout;
	private final Locale userLocale;
	private Map<String, String> userTranslations = null;
	Map<String, Field<?>> fields;

	public CampaignFormBuilder(
		List<CampaignFormElement> formElements,
		List<CampaignFormDataEntry> formValues,
		GridLayout campaignFormLayout,
		List<CampaignFormTranslations> translations) {
		this.formElements = formElements;
		if (formValues != null) {
			this.formValuesMap = new HashMap<>();
			formValues.forEach(formValue -> formValuesMap.put(formValue.getId(), formValue.getValue()));
		} else {
			this.formValuesMap = new HashMap<>();
		}
		this.campaignFormLayout = campaignFormLayout;
		this.fields = new HashMap<>();

		this.userLocale = I18nProperties.getUserLanguage().getLocale();
		if (userLocale != null) {
			translations.stream()
				.filter(t -> t.getLanguageCode().equals(userLocale.toString()))
				.findFirst()
				.ifPresent(
					filteredTranslations -> userTranslations = filteredTranslations.getTranslations()
						.stream()
						.collect(Collectors.toMap(TranslationElement::getElementId, TranslationElement::getCaption)));
		}
	}

	public void buildForm() {
		int currentCol = -1;
		GridLayout currentLayout = campaignFormLayout;
		int sectionCount = 0;
		for (CampaignFormElement formElement : formElements) {
			CampaignFormElementType type = CampaignFormElementType.fromString(formElement.getType());
			List<CampaignFormElementStyle> styles;
			if (formElement.getStyles() != null) {
				styles = Arrays.stream(formElement.getStyles()).map(CampaignFormElementStyle::fromString).collect(Collectors.toList());
			} else {
				styles = new ArrayList<>();
			}
			String dependingOnId = formElement.getDependingOn();
			Object[] dependingOnValues = formElement.getDependingOnValues();

			Object value = formValuesMap.get(formElement.getId());

			int occupiedColumns = getOccupiedColumns(type, styles);

			if (type == CampaignFormElementType.SECTION) {
				sectionCount++;
				GridLayout sectionLayout = new GridLayout(12, 1);
				sectionLayout.setMargin(true);
				CssStyles.style(
					sectionLayout,
					CssStyles.GRID_LAYOUT_SECTION,
					sectionCount % 2 == 0 ? CssStyles.GRID_LAYOUT_EVEN : CssStyles.GRID_LAYOUT_ODD);
				sectionLayout.setWidthFull();
				currentLayout = sectionLayout;

				campaignFormLayout.addComponent(sectionLayout, 0, campaignFormLayout.getRows() - 1, 11, campaignFormLayout.getRows() - 1);
				campaignFormLayout.insertRow(campaignFormLayout.getRows());
			} else if (type == CampaignFormElementType.LABEL) {
				if ((currentCol + 1) + (occupiedColumns - 1) > 11 || currentCol > -1 && styles.contains(CampaignFormElementStyle.FIRST)) {
					currentLayout.insertRow(currentLayout.getRows());
					currentCol = -1;
				}

				Span field = new Span(get18nCaption(formElement.getId(), formElement.getCaption()));
				field.setId(formElement.getId());
				prepareComponent(field, formElement.getId(), formElement.getCaption(), type, styles);

				currentLayout.addComponent(
					field,
					(currentCol + 1),
					currentLayout.getRows() - 1,
					(currentCol + 1) + (occupiedColumns - 1),
					currentLayout.getRows() - 1);

				if (styles.contains(CampaignFormElementStyle.INLINE)) {
					currentCol += occupiedColumns;
				} else {
					currentLayout.insertRow(currentLayout.getRows());
					currentCol = -1;
				}

				if (dependingOnId != null && dependingOnValues != null) {
					setVisibilityDependency(field, dependingOnId, dependingOnValues);
				}
			} else {
				if ((currentCol + 1) + (occupiedColumns - 1) > 11 || currentCol > -1 && styles.contains(CampaignFormElementStyle.FIRST)) {
					currentLayout.insertRow(currentLayout.getRows());
					currentCol = -1;
				}

				Field<?> field = createField(formElement.getId(), formElement.getCaption(), type, styles);
				setFieldValue(field, type, value);
				field.setId(formElement.getId());
				field.setCaption(get18nCaption(formElement.getId(), formElement.getCaption()));
				// was setSizeFull(): the width is the one prepareComponent sets (a page style rule with !important in Vaadin 8)
				field.setHeightFull();

				currentLayout.addComponent(
					field.asComponent(),
					(currentCol + 1),
					currentLayout.getRows() - 1,
					(currentCol + 1) + (occupiedColumns - 1),
					currentLayout.getRows() - 1);

				if (styles.contains(CampaignFormElementStyle.ROW)) {
					currentLayout.insertRow(currentLayout.getRows());
					currentCol = -1;
				} else {
					currentCol += occupiedColumns;
				}

				fields.put(formElement.getId(), field);

				if (dependingOnId != null && dependingOnValues != null) {
					setVisibilityDependency(field.asComponent(), dependingOnId, dependingOnValues);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends Field<?>> T createField(String fieldId, String caption, CampaignFormElementType type, List<CampaignFormElementStyle> styles) {
		SormasFieldGroupFieldFactory fieldFactory = new SormasFieldGroupFieldFactory(new FieldVisibilityCheckers(), UiFieldAccessCheckers.getNoop());

		T field;
		if (type == CampaignFormElementType.YES_NO) {
			field = fieldFactory.createField(Boolean.class, (Class<T>) NullableOptionGroup.class);
		} else if (type == CampaignFormElementType.TEXT || type == CampaignFormElementType.NUMBER) {
			field = fieldFactory.createField(String.class, (Class<T>) TextField.class);
		} else {
			field = null;
		}

		prepareComponent(field.asComponent(), fieldId, caption, type, styles);
		return field;
	}

	private void prepareComponent(Component field, String fieldId, String caption, CampaignFormElementType type, List<CampaignFormElementStyle> styles) {

		if (type == CampaignFormElementType.LABEL) {
			// was a Vaadin 7 Label switched to ContentMode.HTML: the caption is HTML, as before
			VaadinUiUtil.setLabelValue((Span) field, ((Span) field).getText(), ContentMode.HTML);
		} else if (type == CampaignFormElementType.YES_NO) {
			if (!styles.contains(CampaignFormElementStyle.INLINE)) {
				CssStyles.style(
					(HasStyle) field,
					SormasFieldGroupFieldFactory.OPTIONGROUP_HORIZONTAL,
					CssStyles.OPTIONGROUP_CAPTION_INLINE,
					CssStyles.FLOAT_RIGHT);
			}
			CssStyles.style((HasStyle) field, CssStyles.OPTIONGROUP_GRID_LAYOUT);
		} else if (type == CampaignFormElementType.TEXT || type == CampaignFormElementType.NUMBER) {
			if (styles.contains(CampaignFormElementStyle.ROW)) {
				CssStyles.style((HasStyle) field, CssStyles.TEXTFIELD_ROW, CssStyles.TEXTFIELD_CAPTION_INLINE);
			}

			if (type == CampaignFormElementType.NUMBER) {
				TextField textField = (TextField) field;
				textField.addValidator(new NumberNumericValueValidator(I18nProperties.getValidationError(Validations.onlyNumbersAllowed, caption)));
				showValidationErrorOnValueChange(textField);
			}
		}

		// was a page style rule "#<fieldId> { width: <n>% !important; }"
		((HasSize) field).setWidth(calculateComponentWidth(type, styles) + "%");
	}

	/**
	 * Vaadin 7 showed the validator's message at a field as soon as its value was invalid. These fields are not bound to
	 * the form's binder, so the field shows the result of its own validators here. Manual validation keeps the Flow
	 * field's validation (on blur) from clearing it.
	 */
	private static void showValidationErrorOnValueChange(TextField textField) {
		textField.setManualValidation(true);
		textField.addValueChangeListener(e -> {
			String message = null;
			try {
				textField.validateValue();
			} catch (Field.InvalidValueException ex) {
				message = ex.getMessage();
			}
			textField.setErrorMessage(message);
			textField.setInvalid(message != null);
		});
	}

	private int getOccupiedColumns(CampaignFormElementType type, List<CampaignFormElementStyle> styles) {
		List<CampaignFormElementStyle> colStyles = styles.stream().filter(s -> s.toString().startsWith("col")).collect(Collectors.toList());

		if (type == CampaignFormElementType.YES_NO && !styles.contains(CampaignFormElementStyle.INLINE)
			|| (type == CampaignFormElementType.TEXT || type == CampaignFormElementType.NUMBER) && styles.contains(CampaignFormElementStyle.ROW)) {
			return 12;
		}

		if (colStyles.isEmpty()) {
			switch (type) {
			case LABEL:
			case SECTION:
				return 12;
			default:
				return 4;
			}
		}

		// Multiple col styles are not supported; use the first one
		String colStyle = colStyles.get(0).toString();
		return Integer.parseInt(colStyle.substring(colStyle.indexOf("-") + 1));
	}

	private float calculateComponentWidth(CampaignFormElementType type, List<CampaignFormElementStyle> styles) {
		List<CampaignFormElementStyle> colStyles = styles.stream().filter(s -> s.toString().startsWith("col")).collect(Collectors.toList());

		if (type == CampaignFormElementType.YES_NO && styles.contains(CampaignFormElementStyle.INLINE)
			|| (type == CampaignFormElementType.TEXT || type == CampaignFormElementType.NUMBER) && !styles.contains(CampaignFormElementStyle.ROW)
			|| type == CampaignFormElementType.LABEL
			|| type == CampaignFormElementType.SECTION) {
			return 100f;
		}

		if (colStyles.isEmpty()) {
			return 33.3f;
		}

		// Multiple col styles are not supported; use the first one
		String colStyle = colStyles.get(0).toString();
		return Integer.parseInt(colStyle.substring(colStyle.indexOf("-") + 1)) / 12f * 100;
	}

	public  <T extends Field<?>> void setFieldValue(T field, CampaignFormElementType type, Object value) {
		switch (type) {
		case YES_NO:
			((NullableOptionGroup) field).setValue(Sets.newHashSet(value));
			break;
		case TEXT:
		case NUMBER:
			((TextField) field).setValue(value != null ? value.toString() : null);
			break;
		default:
			throw new IllegalArgumentException(type.toString());
		}
	}

	private void setVisibilityDependency(Component component, String dependingOnId, Object[] dependingOnValues) {
		Field<?> dependingOnField = fields.get(dependingOnId);
		List<Object> dependingOnValuesList = Arrays.asList(dependingOnValues);

		if (dependingOnField == null) {
			return;
		}

		component
			.setVisible(dependingOnValuesList.stream().anyMatch(v -> fieldValueMatchesDependingOnValues(dependingOnField, dependingOnValuesList)));
		dependingOnField.addValueChangeListener(e -> {
			boolean visible = fieldValueMatchesDependingOnValues(dependingOnField, dependingOnValuesList);
			component.setVisible(visible);
			if (component instanceof Field) {
				if (!visible) {
					((Field) component).setValue(null);
				}
			}
		});
	}

	private boolean fieldValueMatchesDependingOnValues(Field<?> dependingOnField, List<Object> dependingOnValuesList) {
		if (dependingOnField.getValue() == null) {
			return false;
		}

		if (dependingOnField instanceof NullableOptionGroup) {
			String booleanValue = Boolean.TRUE.equals(((NullableOptionGroup) dependingOnField).getNullableValue()) ? "true" : "false";
			String stringValue = Boolean.TRUE.equals(((NullableOptionGroup) dependingOnField).getNullableValue()) ? "yes" : "no";

			return dependingOnValuesList.stream()
				.anyMatch(v -> v.toString().equalsIgnoreCase(booleanValue) || v.toString().equalsIgnoreCase(stringValue));
		} else {
			return dependingOnValuesList.stream().anyMatch(v -> v.toString().equalsIgnoreCase(dependingOnField.getValue().toString()));
		}
	}

	public String get18nCaption(String elementId, String defaultCaption) {
		if (userTranslations != null && userTranslations.containsKey(elementId)) {
			return userTranslations.get(elementId);
		}

		return defaultCaption;
	}

	public List<CampaignFormDataEntry> getFormValues() {
		return fields.keySet().stream().map(id -> {
			Field<?> field = fields.get(id);
			if (field instanceof NullableOptionGroup) {
				return new CampaignFormDataEntry(id, ((NullableOptionGroup) field).getNullableValue());
			} else {
				return new CampaignFormDataEntry(id, field.getValue());
			}
		}).collect(Collectors.toList());
	}

	public void validateFields() throws Field.InvalidValueException {
		fields.forEach((key, value) -> {
			value.validateValue();
		});
	}

	public void resetFormValues() {
		fields.keySet().forEach(key -> {
			Field<?> field = fields.get(key);
			Object value = formValuesMap.get(key);
			if (field instanceof NullableOptionGroup && value != null) {
				// Vaadin 7: the yes/no group took only a collection or null as its value and ignored a Boolean, so the
				// field kept its value; on Flow the Boolean would be taken and break getNullableValue()
				return;
			}
			((Field<Object>) field).setValue(value);
		});
	}

	public List<CampaignFormElement> getFormElements() {
		return formElements;
	}

	public Map<String, Field<?>> getFields() {
		return fields;
	}

	/**
	 * Was a Vaadin 8 {@code GridLayout}: a CSS grid with a fixed number of equal columns. A component is placed in the
	 * given cells (columns and rows from 0, both ends included, as in Vaadin 8). {@link #insertRow(int)} only adds a row at
	 * the end, the only use here.
	 */
	public static class GridLayout extends Div {

		private static final long serialVersionUID = 1L;

		private int rows;

		public GridLayout(int columns, int rows) {
			this.rows = rows;
			getStyle().set("display", "grid");
			getStyle().set("grid-template-columns", "repeat(" + columns + ", minmax(0, 1fr))");
		}

		public int getRows() {
			return rows;
		}

		public void insertRow(int row) {
			if (row != rows) {
				throw new UnsupportedOperationException("Rows can only be added at the end");
			}
			rows++;
		}

		public void addComponent(Component component, int column1, int row1, int column2, int row2) {
			component.getElement().getStyle().set("grid-column", (column1 + 1) + " / " + (column2 + 2));
			component.getElement().getStyle().set("grid-row", (row1 + 1) + " / " + (row2 + 2));
			add(component);
		}

		/**
		 * Vaadin 8 {@code setMargin(new MarginInfo(true, true))}: a margin on all sides.
		 */
		public void setMargin(boolean margin) {
			getStyle().set("padding", margin ? "var(--lumo-space-m)" : null);
		}
	}
}
