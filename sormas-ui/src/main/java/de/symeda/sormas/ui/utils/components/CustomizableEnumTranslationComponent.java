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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.customizableenum.CustomizableEnumTranslation;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.fields.CustomField;

public class CustomizableEnumTranslationComponent extends CustomField<List<CustomizableEnumTranslation>> {

	private VerticalLayout rowsLayout;
	private List<TranslationRow> rows;
	private Span lblNoTranslations;
	private List<CustomizableEnumTranslation> translations;

	public CustomizableEnumTranslationComponent() {
		// V7 FieldGroup commits called validate(); the Flow form binding runs the field's validators instead
		addValidator(value -> validateTranslations());
	}
	@Override
	protected Component initContent() {

		VerticalLayout layout = new VerticalLayout();
		layout.setWidthFull();
		// was MarginInfo(false, false, true, false): bottom margin only (look, parked)
		layout.setPadding(false);
		layout.setSpacing(false);
		CssStyles.style(layout, CssStyles.VSPACE_TOP_4);

		lblNoTranslations = new Span(I18nProperties.getString(Strings.infoNoCustomizableEnumTranslations));
		layout.add(lblNoTranslations);

		rowsLayout = new VerticalLayout();
		rowsLayout.setWidthFull();
		rowsLayout.setPadding(false);
		rowsLayout.setSpacing(false);
		layout.add(rowsLayout);

		if (rows != null) {
			buildRowsLayout();
			updateNoTranslationsLabelVisibility();
		}

		Button btnAdd = ButtonHelper
			.createIconButtonWithCaption(null, null, VaadinIcon.PLUS, e -> buildTranslationRow(null, null, true), CssStyles.VSPACE_TOP_5);
		btnAdd.setHeight(25, Unit.PIXELS);
		btnAdd.setWidthFull();
		layout.add(btnAdd);

		return layout;
	}

	@Override
	public Class<? extends List<CustomizableEnumTranslation>> getType() {
		//noinspection unchecked,InstantiatingObjectToGetClassObject,InstantiatingObjectToGetClassObject
		return (Class<? extends List<CustomizableEnumTranslation>>) new ArrayList<CustomizableEnumTranslation>(0).getClass();
	}

	private void buildRowsLayout() {

		if (rowsLayout == null) {
			return;
		}

		rowsLayout.removeAll();
		rows.forEach(r -> rowsLayout.add(r));
	}

	private void buildTranslationRows() {

		rows = new ArrayList<>();
		if (translations != null) {
			translations.forEach(t -> buildTranslationRow(Language.fromLocaleString(t.getLanguageCode()), t.getValue()));
		}
	}

	private void buildTranslationRow(Language language, String caption) {
		buildTranslationRow(language, caption, false);
	}

	private void buildTranslationRow(Language language, String caption, boolean render) {

		TranslationRow row = new TranslationRow(language, caption);
		row.setDeleteCallback(() -> {
			rows.remove(row);
			rowsLayout.remove(row);
			updateNoTranslationsLabelVisibility();
		});
		rows.add(row);
		updateNoTranslationsLabelVisibility();

		if (render) {
			rowsLayout.add(row);
		}
	}

	@Override
	public void setValue(List<CustomizableEnumTranslation> newFieldValue) {

		super.setValue(newFieldValue);
		this.translations = newFieldValue;
		buildTranslationRows();
		buildRowsLayout();
	}

	/**
	 * Was the V7 {@code validate()} override (no required check, no other validators).
	 */
	@Override
	public void validateValue() {
		validateTranslations();
	}

	private void validateTranslations() {
		if (rows.stream().anyMatch(r -> r.getLanguage() == null || StringUtils.isBlank(r.getCaption()))) {
			throw new InvalidValueException(I18nProperties.getValidationError(Validations.customizableEnumValueEmptyTranslations));
		}
		Set<Language> selectedLanguages = new HashSet<>();
		if (rows.stream().anyMatch(r -> !selectedLanguages.add(r.getLanguage()))) {
			throw new InvalidValueException(I18nProperties.getValidationError(Validations.customizableEnumValueDuplicateLanguage));
		}
	}

	/**
	 * Was V7 {@code getInternalValue()}: the value is built from the rows.
	 */
	@Override
	public List<CustomizableEnumTranslation> getValue() {

		return rows != null
			? rows.stream()
				.map(r -> new CustomizableEnumTranslation(r.getLanguage().getLocale().toString(), r.getCaption()))
				.collect(Collectors.toList())
			: null;
	}

	private void updateNoTranslationsLabelVisibility() {

		if (lblNoTranslations == null) {
			return;
		}

		lblNoTranslations.setVisible(CollectionUtils.isEmpty(rows));
	}

	private static final class TranslationRow extends HorizontalLayout {

		private static final long serialVersionUID = 6883911907756570894L;

		private final ComboBox cbLanguage;
		private final TextField tfCaption;

		private Runnable deleteCallback;

		public TranslationRow(Language language, String caption) {

			tfCaption = new TextField();
			tfCaption.setWidthFull();
			if (caption != null) {
				tfCaption.setValue(caption);
			}
			tfCaption.setPlaceholder(I18nProperties.getString(Strings.promptCustomizableEnumTranslationLanguage));
			cbLanguage = new ComboBox(null, Arrays.asList(Language.values()));
			cbLanguage.setWidth(250, Unit.PIXELS);
			cbLanguage.setInputPrompt(I18nProperties.getString(Strings.promptCustomizableEnumTranslationCaption));
			CssStyles.style(CssStyles.VSPACE_NONE, cbLanguage, tfCaption);
			CssStyles.style(cbLanguage, CssStyles.COMBO_BOX_WITH_FLAG_ICON);
			ControllerProvider.getUserController().setFlagIcons(cbLanguage);
			cbLanguage.addValueChangeListener(e -> tfCaption.setEnabled(e.getValue() != null));
			if (language != null) {
				cbLanguage.setValue(language);
			}
			Button btnDelete = ButtonHelper.createIconButtonWithCaption(null, null, VaadinIcon.TRASH, e -> deleteCallback.run());
			add(cbLanguage);
			add(tfCaption);
			add(btnDelete);
			setFlexGrow(1, tfCaption);

			setWidthFull();
			setPadding(false);
			CssStyles.style(this, CssStyles.VSPACE_4);
		}

		public Language getLanguage() {
			return (Language) cbLanguage.getValue();
		}

		public String getCaption() {
			return tfCaption.getValue();
		}

		public void setDeleteCallback(Runnable deleteCallback) {
			this.deleteCallback = deleteCallback;
		}
	}
}
