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

package de.symeda.sormas.ui.configuration.customizablefield;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.customizablefield.CustomizableFieldMetadataDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

/**
 * Vaadin 8 component for editing per-language translations of a customizable field's name and description.
 * Renders one row per language: [Language ▾] [Name] [Description] [🗑]
 * Reads/writes {@code Map<localeString, Map<"name"|"description", translatedValue>>}.
 */
@SuppressWarnings({
	"java:S110", // suppress sonar too many parents warning
	"java:S2160" // suppress sonar missing equals
})
public class CustomizableFieldTranslationsComponent extends VerticalLayout {

	private static final long serialVersionUID = 1L;

	private final VerticalLayout rowsLayout;
	private final List<TranslationRow> rows = new ArrayList<>();
	private final Span lblNoTranslations;

	public CustomizableFieldTranslationsComponent() {

		setWidthFull();
		setPadding(false);
		// was a bottom-only margin (MarginInfo(false, false, true, false)): spacing below the rows is look only
		setSpacing(false);
		CssStyles.style(this, CssStyles.VSPACE_TOP_4);

		lblNoTranslations = new Span(I18nProperties.getString(Strings.infoNoCustomizableFieldTranslations));
		add(lblNoTranslations);

		rowsLayout = new VerticalLayout();
		rowsLayout.setWidthFull();
		rowsLayout.setPadding(false);
		rowsLayout.setSpacing(false);
		add(rowsLayout);

		Button btnAdd =
			ButtonHelper.createIconButtonWithCaption(null, null, VaadinIcon.PLUS, e -> addRow(null, null, null, true), CssStyles.VSPACE_TOP_5);
		btnAdd.setHeight(25, Unit.PIXELS);
		btnAdd.setWidthFull();
		add(btnAdd);
	}

	public void setValue(Map<String, Map<String, String>> translations) {

		rows.clear();
		rowsLayout.removeAll();

		if (translations != null) {
			translations.forEach((localeStr, translationMap) -> {
				Language lang = Language.fromLocaleString(localeStr);
				String name = translationMap != null ? translationMap.get(CustomizableFieldMetadataDto.NAME) : null;
				String description = translationMap != null ? translationMap.get(CustomizableFieldMetadataDto.DESCRIPTION) : null;
				addRow(lang, name, description, false);
			});
			rows.forEach(rowsLayout::add);
		}

		updateNoTranslationsLabelVisibility();
	}

	/**
	 * Returns the current translations, or {@code null} if there are none.
	 * Rows with no language selected or no values filled in are skipped.
	 */
	@SuppressWarnings("java:S1168") // suppress "return empty map instead of null" warning
	public Map<String, Map<String, String>> getValue() {

		if (rows.isEmpty()) {
			// we could return a empty map but it might be interpreted as no translaton and postgres might create a '{}' jsonb entry
			return null;
		}
		Map<String, Map<String, String>> result = new HashMap<>();
		for (TranslationRow row : rows) {
			Language lang = row.getLanguage();
			if (lang == null) {
				continue;
			}
			Map<String, String> entry = new HashMap<>();
			String name = row.getName();
			String description = row.getDescription();
			if (name != null && !name.trim().isEmpty()) {
				entry.put(CustomizableFieldMetadataDto.NAME, name.trim());
			}
			if (description != null && !description.trim().isEmpty()) {
				entry.put(CustomizableFieldMetadataDto.DESCRIPTION, description.trim());
			}
			if (!entry.isEmpty()) {
				result.put(lang.getLocale().toString(), entry);
			}
		}
		return result.isEmpty() ? null : result;
	}

	/** Returns {@code true} if any two rows share the same language. */
	public boolean hasDuplicateLanguages() {

		Set<Language> seen = new HashSet<>();
		return rows.stream().map(TranslationRow::getLanguage).filter(l -> l != null).anyMatch(l -> !seen.add(l));
	}

	private void addRow(Language language, String name, String description, boolean render) {

		TranslationRow row = new TranslationRow(language, name, description);
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

	private void updateNoTranslationsLabelVisibility() {

		if (lblNoTranslations != null) {
			lblNoTranslations.setVisible(rows.isEmpty());
		}
	}

	private static final class TranslationRow extends HorizontalLayout {

		private static final long serialVersionUID = 1L;

		private final ComboBox<Language> cbLanguage;
		private final TextField tfName;
		private final TextField tfDescription;
		private transient Runnable deleteCallback;

		public TranslationRow(Language language, String name, String description) {

			cbLanguage = new ComboBox<>();
			cbLanguage.setItems(Language.values());
			cbLanguage.setItemLabelGenerator(Language::toString);
			cbLanguage.setWidth(220, Unit.PIXELS);
			cbLanguage.setPlaceholder(I18nProperties.getString(Strings.promptCustomizableEnumTranslationLanguage));
			if (language != null) {
				cbLanguage.setValue(language);
			}

			tfName = new TextField();
			tfName.setWidthFull();
			tfName.setPlaceholder(I18nProperties.getPrefixCaption(CustomizableFieldMetadataDto.I18N_PREFIX, CustomizableFieldMetadataDto.NAME));
			if (name != null) {
				tfName.setValue(name);
			}

			tfDescription = new TextField();
			tfDescription.setWidthFull();
			tfDescription
				.setPlaceholder(I18nProperties.getPrefixCaption(CustomizableFieldMetadataDto.I18N_PREFIX, CustomizableFieldMetadataDto.DESCRIPTION));
			if (description != null) {
				tfDescription.setValue(description);
			}

			CssStyles.style(CssStyles.VSPACE_NONE, cbLanguage, tfName, tfDescription);

			Button btnDelete = ButtonHelper.createIconButtonWithCaption(null, null, VaadinIcon.TRASH, e -> deleteCallback.run());

			add(cbLanguage);
			add(tfName);
			add(tfDescription);
			add(btnDelete);
			setFlexGrow(1, tfName);
			setFlexGrow(1, tfDescription);
			setWidthFull();
			setPadding(false);
			CssStyles.style(this, CssStyles.VSPACE_4);
		}

		public Language getLanguage() {
			return cbLanguage.getValue();
		}

		public String getName() {
			return tfName.getValue();
		}

		public String getDescription() {
			return tfDescription.getValue();
		}

		public void setDeleteCallback(Runnable deleteCallback) {
			this.deleteCallback = deleteCallback;
		}
	}
}
