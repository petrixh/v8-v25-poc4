/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2022 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.user;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.symeda.sormas.ui.utils.ContentMode;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.fields.CustomField;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.Descriptions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.user.UserRoleDto;

public class UserRoleTemplateSelectionField extends CustomField<UserRoleDto> {

	private VerticalLayout mainLayout;
	private ComboBox templateRoleCombo;
	private UserRoleEditForm userRoleEditForm;
	private UserRoleNotificationsForm userRoleNotificationsForm;

	// the Vaadin 8 doSetValue override (never called) is gone: the value is the combo box value, see getValue()

	@Override
	protected Component initContent() {
		mainLayout = new VerticalLayout();
		mainLayout.setSpacing(true);
		mainLayout.setPadding(false);
		mainLayout.setSizeFull();
		mainLayout.setWidthFull();

		Span infoLabel = VaadinUiUtil
			.createLabel(HtmlRenderer.iconHtml(VaadinIcon.INFO_CIRCLE) + " " + I18nProperties.getDescription(Descriptions.userRoleTemplate), ContentMode.HTML);
		mainLayout.add(infoLabel);

		templateRoleCombo = new ComboBox();
		templateRoleCombo.setWidth(300, Unit.PIXELS);
		UserRoleFormHelper.setTemplateRoleItems(templateRoleCombo);
		templateRoleCombo.setCaption(I18nProperties.getCaption(Captions.userrole_applyUserRoleTemplate));
		mainLayout.add(templateRoleCombo);

		return mainLayout;
	}

	@Override
	public UserRoleDto getValue() {
		return (UserRoleDto) templateRoleCombo.getValue();
	}

	public UserRoleEditForm getUserRoleEditForm() {
		return userRoleEditForm;
	}

	public void setUserRoleEditForm(UserRoleEditForm userRoleEditForm) {
		this.userRoleEditForm = userRoleEditForm;
	}

	public UserRoleNotificationsForm getUserRoleNotificationsForm() {
		return userRoleNotificationsForm;
	}

	public void setUserRoleNotificationsForm(UserRoleNotificationsForm userRoleNotificationsForm) {
		this.userRoleNotificationsForm = userRoleNotificationsForm;
	}
}
