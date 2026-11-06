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

package de.symeda.sormas.ui.utils;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;

public class DirtyCheckPopup {

	public static Dialog show(DirtyStateComponent dirtyStateComponent, Runnable callback) {
		Dialog warningPopup = VaadinUiUtil.showConfirmationPopup(
			I18nProperties.getString(Strings.unsavedChanges_warningTitle),
			new Span(I18nProperties.getString(Strings.unsavedChanges_warningMessage)),
			popupWindow -> {
				ConfirmationComponent confirmationComponent = new ConfirmationComponent(false, null) {

					private static final long serialVersionUID = 3664636750443474734L;

					@Override
					protected void onConfirm() {
						boolean committedSuccessfully = dirtyStateComponent.commitAndHandle();
						popupWindow.close();

						if (committedSuccessfully) {
							callback.run();
						}
					}

					@Override
					protected void onCancel() {
						dirtyStateComponent.discard();
						popupWindow.close();
						callback.run();
					}
				};

				confirmationComponent.getConfirmButton().setText(I18nProperties.getString(Strings.unsavedChanges_save));
				confirmationComponent.getCancelButton().setText(I18nProperties.getString(Strings.unsavedChanges_discard));

				confirmationComponent.addExtraButton(
					ButtonHelper.createButton(
						Strings.unsavedChanges_cancel,
						I18nProperties.getString(Strings.unsavedChanges_cancel),
						null,
						CssStyles.BUTTON_LINK),
					buttonEvent -> popupWindow.close());

				return confirmationComponent;
			},
			600);

		VaadinUiUtil.setClosable(warningPopup, true);

		return warningPopup;
	}
}
