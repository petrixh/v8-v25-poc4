/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.ui.contact;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.contact.ContactDto;
import de.symeda.sormas.api.contact.ContactIndexDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;

@SuppressWarnings("serial")
public class ContactListEntry extends HorizontalLayout {

	private final ContactIndexDto contact;
	private Button editButton;
	private Button deleteButton;

	public ContactListEntry(ContactIndexDto contact) {

		this.contact = contact;

		setPadding(false);
		setSpacing(true);
		setWidth(100, Unit.PERCENTAGE);
		addClassName(CssStyles.SORMAS_LIST_ENTRY);

		HorizontalLayout mainLayout = new HorizontalLayout();
		mainLayout.setWidth(100, Unit.PERCENTAGE);
		mainLayout.setPadding(false);
		mainLayout.setSpacing(false);
		add(mainLayout);
		setFlexGrow(1, mainLayout);

		VerticalLayout leftLayout = new VerticalLayout();
		leftLayout.setPadding(false);
		leftLayout.setSpacing(false);

		Span contactUuid = new Span(DataHelper.toStringNullable(DataHelper.getShortUuid(contact.getUuid())));
		contactUuid.addClassNames(CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
		contactUuid.setTitle(contact.getUuid());
		leftLayout.add(contactUuid);

		Span contactClassification = new Span(contact.getContactClassification().toString());
		contactClassification.addClassName(CssStyles.LABEL_BOLD);
		contactClassification.setTitle(contact.getContactClassification().toString());
		contactClassification.setWidthFull();
		leftLayout.add(contactClassification);

		Span contactStatus = new Span(contact.getContactStatus().toString());
		contactStatus.setTitle(contact.getContactStatus().toString());
		contactStatus.setWidthFull();
		leftLayout.add(contactStatus);

		if (contact.getLastContactDate() != null) {
			Span lastContactDate = new Span(
				I18nProperties.getPrefixCaption(ContactDto.I18N_PREFIX, ContactDto.LAST_CONTACT_DATE) + ": "
					+ DateFormatHelper.formatDate(contact.getLastContactDate()));
			leftLayout.add(lastContactDate);
		}
		mainLayout.add(leftLayout);
	}

	public void addEditListener(int rowIndex, ComponentEventListener<ClickEvent<Button>> editClickListener) {
		if (editButton == null) {
			editButton = ButtonHelper.createIconButtonWithCaption(
				"edit-contact-" + rowIndex,
				null,
				VaadinIcon.PENCIL,
				null,
				CssStyles.BUTTON_LINK,
				CssStyles.BUTTON_COMPACT);

			add(editButton);
			setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, editButton);
		}

		editButton.addClickListener(editClickListener);
	}

	public void addDeleteListener(int rowIndex, ComponentEventListener<ClickEvent<Button>> deleteClickListener) {
		if (deleteButton == null) {
			deleteButton = ButtonHelper.createIconButtonWithCaption(
				"delete-contact-" + rowIndex,
				null,
				VaadinIcon.TRASH,
				null,
				CssStyles.BUTTON_LINK,
				CssStyles.BUTTON_COMPACT);

			add(deleteButton);
			setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, deleteButton);
		}

		deleteButton.addClickListener(deleteClickListener);
	}

	public void setActive() {
		addClassName(CssStyles.ACTIVE_SIDE_COMPONENT_ELEMENT);
	}

	public ContactIndexDto getContact() {
		return contact;
	}
}
