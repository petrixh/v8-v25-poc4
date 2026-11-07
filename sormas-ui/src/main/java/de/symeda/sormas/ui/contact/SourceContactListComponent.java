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

package de.symeda.sormas.ui.contact;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.ReferenceDto;
import de.symeda.sormas.api.caze.CaseReferenceDto;
import de.symeda.sormas.api.contact.ContactReferenceDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.SormasUI;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.AbstractDetailView;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

public class SourceContactListComponent extends VerticalLayout {

	private static final long serialVersionUID = -168334035260718276L;

	private SourceContactList list;
	private final CaseReferenceDto caseReference;
	private final AbstractDetailView<? extends ReferenceDto> view;

	public SourceContactListComponent(CaseReferenceDto caseReference, AbstractDetailView<? extends ReferenceDto> view, boolean isEditAllowed) {
		this.caseReference = caseReference;
		this.view = view;
		createSourceContactListComponent(new SourceContactList(caseReference, isEditAllowed), isEditAllowed);
	}

	private void createSourceContactListComponent(SourceContactList sourceContactList, boolean isEditAllowed) {
		setWidthFull();
		setPadding(false);
		setSpacing(false);

		HorizontalLayout componentHeader = new HorizontalLayout();
		componentHeader.setPadding(false);
		componentHeader.setSpacing(false);
		componentHeader.setWidthFull();
		add(componentHeader);

		list = sourceContactList;
		add(list);
		list.reload();

		Span sourceContactsHeader = new Span(I18nProperties.getString(Strings.headingEpiDataSourceCaseContacts));
		sourceContactsHeader.addClassName(CssStyles.H3);
		componentHeader.add(sourceContactsHeader);
		componentHeader.setFlexGrow(1, sourceContactsHeader);

		if (UiUtil.permitted(isEditAllowed, UserRight.CONTACT_CREATE)) {
			Button createButton = ButtonHelper.createIconButton(
				Captions.contactNewContact,
				VaadinIcon.PLUS_CIRCLE,
					e -> view.showUnsavedChangesPopup(() -> ControllerProvider.getContactController().create(caseReference, true, () -> {
						// For case classification effectiveness, extra save is needed.
						// Ideally, we should mark the epi case data form as dirty when a contact is created, but we couldn't find a way to do that.
						FacadeProvider.getCaseFacade().save(FacadeProvider.getCaseFacade().getByUuid(caseReference.getUuid()));
						SormasUI.refreshView();
					})),
				CssStyles.BUTTON_PRIMARY);
			componentHeader.add(createButton);
			componentHeader.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, createButton);
		}
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public List<ContactReferenceDto> getEntries() {
		return list.getSourceContacts();
	}

}
