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

package de.symeda.sormas.ui.contact;

import org.apache.commons.lang3.StringUtils;

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
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentField;

public class SourceContactListEntry extends SideComponentField {

	private final ContactIndexDto contact;
	private Button editButton;

	public SourceContactListEntry(ContactIndexDto contact) {

		this.contact = contact;

		HorizontalLayout mainLayout = new HorizontalLayout();
		mainLayout.setWidth(100, Unit.PERCENTAGE);
		mainLayout.setPadding(false);
		mainLayout.setSpacing(true);
		addComponentToField(mainLayout);

		VerticalLayout leftColumn = new VerticalLayout();
		leftColumn.setPadding(false);
		leftColumn.setSpacing(false);
		{
			Span lblSourceCaseName = VaadinUiUtil.createLabel(
				contact.getCaze() != null
					? I18nProperties.getCaption(Captions.contactSourceCase) + ":<br/>" + contact.getCaze().getCaption()
					: I18nProperties.getString(Strings.infoNoSourceCase),
				ContentMode.HTML);
			CssStyles.style(lblSourceCaseName, CssStyles.LABEL_BOLD, CssStyles.VSPACE_3);
			lblSourceCaseName.setWidth(100, Unit.PERCENTAGE);
			leftColumn.add(lblSourceCaseName);

			Span lblRegion = new Span(StringUtils.isNotBlank(contact.getRegionName()) ? contact.getRegionName() : contact.getCaseRegionName());
			lblRegion.setWidth(100, Unit.PERCENTAGE);
			leftColumn.add(lblRegion);

			Span lblDistrict =
				new Span(StringUtils.isNotBlank(contact.getDistrictName()) ? contact.getDistrictName() : contact.getCaseDistrictName());
			lblDistrict.setWidth(100, Unit.PERCENTAGE);
			leftColumn.add(lblDistrict);
		}
		mainLayout.add(leftColumn);

		VerticalLayout rightColumn = new VerticalLayout();
		rightColumn.addClassName(CssStyles.ALIGN_RIGHT);
		rightColumn.setPadding(false);
		rightColumn.setSpacing(false);
		{
			Span lblContactUuid = VaadinUiUtil.createLabel(
				I18nProperties.getPrefixCaption(ContactDto.I18N_PREFIX, ContactDto.UUID) + ":<br/>"
					+ DataHelper.getShortUuid(contact.getUuid().toUpperCase()),
				ContentMode.HTML);
			CssStyles.style(lblContactUuid, CssStyles.LABEL_BOLD, CssStyles.VSPACE_3, CssStyles.LABEL_TEXT_ALIGN_RIGHT);
			lblContactUuid.setWidth(100, Unit.PERCENTAGE);
			rightColumn.add(lblContactUuid);

			if (contact.getContactProximities() != null && !contact.getContactProximities().isEmpty()) {
				String proximitiesString = contact.getContactProximities().stream()
					.map(Object::toString)
					.reduce((a, b) -> a + ", " + b)
					.orElse("");
				Span lblContactProximity = new Span(StringUtils.abbreviate(proximitiesString, 50));
				CssStyles.style(lblContactProximity, CssStyles.LABEL_TEXT_ALIGN_RIGHT);
				lblContactProximity.setWidth(100, Unit.PERCENTAGE);
				lblContactProximity.setTitle(proximitiesString);
				rightColumn.add(lblContactProximity);
			}

			Span lblLastContactDate = new Span(DateHelper.formatLocalDate(contact.getLastContactDate(), I18nProperties.getUserLanguage()));
			CssStyles.style(lblLastContactDate, CssStyles.LABEL_TEXT_ALIGN_RIGHT);
			lblLastContactDate.setWidth(100, Unit.PERCENTAGE);
			rightColumn.add(lblLastContactDate);
		}
		mainLayout.add(rightColumn);
		// was Alignment.TOP_RIGHT
		mainLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, rightColumn);
	}

	public void addEditListener(int rowIndex, ComponentEventListener<ClickEvent<Button>> editClickListener) {
		if (editButton == null) {
			editButton = ButtonHelper.createIconButtonWithCaption(
				"edit-task-" + rowIndex,
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

	public ContactIndexDto getContact() {
		return contact;
	}

}
