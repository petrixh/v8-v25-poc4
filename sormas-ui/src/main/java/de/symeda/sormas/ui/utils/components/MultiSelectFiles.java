/*
 * ******************************************************************************
 * * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * * Copyright © 2016-2024 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * *
 * * This program is free software: you can redistribute it and/or modify
 * * it under the terms of the GNU General Public License as published by
 * * the Free Software Foundation, either version 3 of the License, or
 * * (at your option) any later version.
 * *
 * * This program is distributed in the hope that it will be useful,
 * * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * * GNU General Public License for more details.
 * *
 * * You should have received a copy of the GNU General Public License
 * * along with this program. If not, see <https://www.gnu.org/licenses/>.
 * ******************************************************************************
 */

package de.symeda.sormas.ui.utils.components;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.email.ExternalBulkEmailOptionsForm;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.fields.CustomField;

public class MultiSelectFiles<T> extends CustomField<Set<T>> {

	/** Was {@code BUTTON_ICON_ALIGN_TOP}. */
	private static final String BUTTON_ICON_ALIGN_TOP = "icon-align-top";
	private VerticalLayout labelLayout = new VerticalLayout();
	private Map<String, T> selectedItemsWithCaption = new HashMap<>();

	public static <T> MultiSelectFiles<T> create(Class<T> clazz) {
		return new MultiSelectFiles<>();
	}

	@Override
	protected Component initContent() {
		VerticalLayout verticalLayout = new VerticalLayout();
		// was MarginInfo(false, false, true, false): bottom margin only (look, parked)
		verticalLayout.setPadding(false);

		verticalLayout.setSpacing(false);

		labelLayout.setPadding(false);
		labelLayout.setSpacing(false);
		verticalLayout.add(labelLayout);

		return verticalLayout;
	}

	@Override
	public Class<? extends Set<T>> getType() {
		return (Class) Set.class;
	}

	private void listSelectedItems() {
		labelLayout.removeAll();

		for (Map.Entry<String, T> itemEntry : new HashMap<>(selectedItemsWithCaption).entrySet()) {
			HorizontalLayout itemLayout = new HorizontalLayout();
			itemLayout.setPadding(false);
			itemLayout.setWidth("100%");

			Span label = new Span(itemEntry.getKey());
			label.setWidth("100%");
			itemLayout.add(label);

			if (!isReadOnly()) {
				Button removeButton = ButtonHelper.createIconButtonWithCaption(
					null,
					null,
					VaadinIcon.TRASH,
					e -> removeSelectedItem(itemEntry.getValue()),
					CssStyles.BUTTON_ICON_ONLY,
					CssStyles.BUTTON_BORDERLESS,
					BUTTON_ICON_ALIGN_TOP);
				itemLayout.add(removeButton);
			}

			labelLayout.add(itemLayout);
		}
	}

	private void removeSelectedItem(T item) {
		Optional<Map.Entry<String, T>> itemKey =
			selectedItemsWithCaption.entrySet().stream().filter(selection -> selection.getValue().equals(item)).findFirst();

		itemKey.ifPresent(stringTEntry -> selectedItemsWithCaption.remove(stringTEntry.getKey()));

		setValue(new HashSet<>(selectedItemsWithCaption.values()));
		listSelectedItems();
	}

	public void addSelectedItemWithCaption(T item, String caption) {

		if (selectedItemsWithCaption.size() <= 5) {
			Map<String, T> selection = new HashMap<>(selectedItemsWithCaption);
			selection.put(caption, item);
			selectedItemsWithCaption.putAll(selection);
			setValue(new HashSet<>(selectedItemsWithCaption.values()));
		} else {
			VaadinUiUtil.createWarningLayout()
				.add(
					new Span(
						String.format(
							I18nProperties.getString(Strings.messageBulkEmailMaxAttachedFiles),
							ExternalBulkEmailOptionsForm.MAX_ATTACHMENT_NUMBER)));
		}
		listSelectedItems();

	}

	public Map<String, T> getSelectedItemsWithCaption() {
		return selectedItemsWithCaption;
	}

}
