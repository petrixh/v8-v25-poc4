/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2021 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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
package de.symeda.sormas.ui.login;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

public class ChangeDefaultUserPasswordWindow extends Dialog {

	public ChangeDefaultUserPasswordWindow(Runnable onContinue, UserDto currentUser) {
		setHeaderTitle(" " + I18nProperties.getString(Strings.headingSecurityAlert));
		getHeader().add(VaadinIcon.WARNING.create());
		setWidth("40%");
		final VerticalLayout content = new VerticalLayout();
		content.setPadding(true);
		content.setWidthFull();

		Span introductionLabel = new Span(I18nProperties.getString(Strings.DefaultPassword_ownUserIntroduction));
		introductionLabel.setWidthFull();
		content.add(introductionLabel);

		Span actionLabel = new Span(I18nProperties.getString(Strings.DefaultPassword_ownUserAction));
		actionLabel.setWidthFull();
		content.add(actionLabel);

		Span newPasswordLabel = new Span();
		newPasswordLabel.setWidthFull();
		newPasswordLabel.setVisible(false);
		CssStyles.style(newPasswordLabel, CssStyles.H2);
		content.add(newPasswordLabel);

		Span newPasswordSetHintsLabel = new Span(I18nProperties.getString(Strings.DefaultPassword_ownUserNewPasswordSetHints));
		newPasswordSetHintsLabel.setWidthFull();
		CssStyles.style(newPasswordSetHintsLabel, CssStyles.LABEL_POSITIVE);
		newPasswordSetHintsLabel.setVisible(false);
		content.add(newPasswordSetHintsLabel);

		Button continueButton = ButtonHelper.createButton(Captions.actionRemindMeLater, clickEvent -> {
			close();
			onContinue.run();
		}, CssStyles.BUTTON_PRIMARY);

		Button generateNewPasswordButton = ButtonHelper.createButton(Captions.actionGenerateNewPassword, clickEvent -> {
			newPasswordLabel.setVisible(true);
			newPasswordSetHintsLabel.setVisible(true);
			continueButton.setText(I18nProperties.getCaption(Captions.actionContinue));
			clickEvent.getSource().setVisible(false);
			newPasswordLabel.setText(
				String.format(
					I18nProperties.getString(Strings.DefaultPassword_newPasswordPlaceholder),
					FacadeProvider.getUserFacade().resetPassword(currentUser.getUuid())));
		});
		content.add(generateNewPasswordButton);
		content.add(continueButton);

		setModal(true);
		setResizable(false);
		// not closable, as in Vaadin 8: no close button, Escape and a click outside do nothing
		setCloseOnEsc(false);
		setCloseOnOutsideClick(false);
		add(content);
		setDraggable(false);

	}
}
