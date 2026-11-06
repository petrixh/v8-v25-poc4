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
package de.symeda.sormas.ui.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.symeda.sormas.ui.navigation.View;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import de.symeda.sormas.ui.utils.NotificationHelper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.dialog.Dialog;
import de.symeda.sormas.ui.utils.fields.ComboBox;

import de.symeda.sormas.api.AuthProvider;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.user.UserRoleDto;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.SormasUI;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.BulkOperationHandler;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent.CommitListener;
import de.symeda.sormas.ui.utils.ConfirmationComponent;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class UserController {

	public void create() {
		CommitDiscardWrapperComponent<UserEditForm> userCreateComponent = getUserCreateComponent();
		Dialog window = VaadinUiUtil.showModalPopupWindow(userCreateComponent, I18nProperties.getString(Strings.headingCreateNewUser));
		// user form is too big for typical screens
		window.setWidth(Unit.getSize(userCreateComponent.getWrappedComponent().getWidth()) + 64 + 20, Unit.PIXELS);
		window.setHeight(90, Unit.PERCENTAGE);
	}

	public void edit(UserDto user) {
		Dialog window = VaadinUiUtil.createPopupWindow();
		CommitDiscardWrapperComponent<UserEditForm> userComponent = getUserEditComponent(user.getUuid(), window::close);

		window.setHeaderTitle(I18nProperties.getString(Strings.headingEditUser));
		window.add(userComponent);
		// user form is too big for typical screens
		window.setWidth(Unit.getSize(userComponent.getWrappedComponent().getWidth()) + 64 + 20, Unit.PIXELS);
		window.setHeight(90, Unit.PERCENTAGE);
		window.open();
	}

	public void overview() {
		String navigationState = UsersView.VIEW_NAME;
		SormasUI.get().getNavigator().navigateTo(navigationState);
	}

	/**
	 * Update the fragment without causing navigator to change view
	 */
	public void setUriFragmentParameter(String caseUuid) {
		String fragmentParameter;
		if (caseUuid == null || caseUuid.isEmpty()) {
			fragmentParameter = "";
		} else {
			fragmentParameter = caseUuid;
		}

		SormasUI.get().getNavigator().updateUrl(UsersView.VIEW_NAME + "/" + fragmentParameter);
	}

	public CommitDiscardWrapperComponent<UserEditForm> getUserEditComponent(final String userUuid, Runnable closeWindowCallback) {
		UserEditForm userEditForm = new UserEditForm(false);
		UserDto userDto = FacadeProvider.getUserFacade().getByUuid(userUuid);
		userEditForm.setValue(userDto);
		final CommitDiscardWrapperComponent<UserEditForm> editView =
			new CommitDiscardWrapperComponent<UserEditForm>(userEditForm, UiUtil.permitted(UserRight.USER_EDIT), userEditForm.getFieldGroup());

		// Add reset password button
		if (FacadeProvider.getFeatureConfigurationFacade().isFeatureDisabled(FeatureType.AUTH_PROVIDER_TO_SORMAS_USER_SYNC)) {
			Button resetPasswordButton = createResetPasswordButton(userUuid, userDto.getUserEmail(), editView);
			editView.getButtonsPanel().addComponentAtIndex(0, resetPasswordButton);
		}

		editView.addDiscardListener(closeWindowCallback::run);
		editView.addCommitListener(() -> {
			if (!userEditForm.getFieldGroup().isModified()) {
				UserDto user = userEditForm.getValue();
				UserDto existingUser = FacadeProvider.getUserFacade().getByUuid(user.getUuid());
				// If user roles have changed, the user might no longer have a district assigned and therefore
				// needs to be removed as surveillance/contact officer from existing cases/contacts
				if (existingUser.getDistrict() != null && user.getDistrict() == null) {
					openRemoveUserAsOfficerPrompt(result -> {
						if (result) {
							FacadeProvider.getUserFacade().removeUserAsSurveillanceAndContactOfficer(user.getUuid());
							closeWindowCallback.run();
						}
					});
				}
				if (DataHelper.isSame(user, UiUtil.getUser())) {

					Set<UserRight> oldUserRights =
						UserRoleDto.getUserRights(FacadeProvider.getUserRoleFacade().getByReferences(existingUser.getUserRoles()));
					Set<UserRight> newUserRights = UserRoleDto.getUserRights(FacadeProvider.getUserRoleFacade().getByReferences(user.getUserRoles()));

					if (oldUserRights.contains(UserRight.USER_ROLE_EDIT) && !newUserRights.contains(UserRight.USER_ROLE_EDIT)) {
						NotificationHelper.show(
							I18nProperties.getString(Strings.messageCheckInputData),
							I18nProperties.getValidationError(Validations.removeRolesWithEditRightFromOwnUser),
							NotificationHelper.Type.ERROR_MESSAGE,
							true);
					} else if (!newUserRights.contains(UserRight.USER_EDIT)) {
						NotificationHelper.show(
							I18nProperties.getString(Strings.messageCheckInputData),
							I18nProperties.getValidationError(Validations.removeRolesWithEditUserFromOwnUser),
							NotificationHelper.Type.ERROR_MESSAGE,
							true);
					} else {
						saveUser(user);
						closeWindowCallback.run();
					}
				} else {
					saveUser(user);
					closeWindowCallback.run();
				}
			}
		});

		return editView;
	}

	private void openRemoveUserAsOfficerPrompt(Consumer<Boolean> callback) {
		VaadinUiUtil.showConfirmationPopup(
			I18nProperties.getString(Strings.headingSaveUser),
			new Span(I18nProperties.getString(Strings.confirmationRemoveUserAsOfficer)),
			I18nProperties.getString(Strings.yes),
			I18nProperties.getString(Strings.no),
			640,
			callback);
	}

	private void saveUser(UserDto user) {
		if (FacadeProvider.getFeatureConfigurationFacade().isFeatureEnabled(FeatureType.AUTH_PROVIDER_TO_SORMAS_USER_SYNC)) {
			FacadeProvider.getUserFacade().saveUserRolesAndRestrictions(user, user.getUserRoles());
		} else {
			FacadeProvider.getUserFacade().saveUser(user, false);
		}
		refreshView();
	}

	public CommitDiscardWrapperComponent<UserEditForm> getUserCreateComponent() {

		UserEditForm createForm = new UserEditForm(true);
		createForm.setValue(UserDto.build());
		final CommitDiscardWrapperComponent<UserEditForm> editView =
			new CommitDiscardWrapperComponent<UserEditForm>(createForm, UiUtil.permitted(UserRight.USER_CREATE), createForm.getFieldGroup());

		editView.addCommitListener(new CommitListener() {

			@Override
			public void onCommit() {
				if (!createForm.getFieldGroup().isModified()) {
					UserDto dto = createForm.getValue();
					dto = FacadeProvider.getUserFacade().saveUser(dto, false);
					refreshView();
					makeInitialPassword(dto.getUuid(), dto.getUserEmail());
				}
			}
		});
		return editView;
	}

	public boolean isLoginUnique(String uuid, String userName) {
		return FacadeProvider.getUserFacade().isLoginUnique(uuid, userName);
	}

	public void makeInitialPassword(String userUuid, String userEmail) {
		if (StringUtils.isBlank(userEmail) || AuthProvider.getProvider(FacadeProvider.getConfigFacade()).isDefaultProvider()) {
			String newPassword = FacadeProvider.getUserFacade().resetPassword(userUuid);
			showPasswordResetInternalSuccessPopup(newPassword);
		} else {
			showAccountCreatedSuccessful();
		}
	}

	public void makeNewPassword(String userUuid, String userEmail) {
		String newPassword = FacadeProvider.getUserFacade().resetPassword(userUuid);

		if (StringUtils.isBlank(userEmail) || AuthProvider.getProvider(FacadeProvider.getConfigFacade()).isDefaultProvider()) {
			showPasswordResetInternalSuccessPopup(newPassword);
		} else {
			showPasswordResetExternalSuccessPopup();
		}
	}

	private void showPasswordResetInternalSuccessPopup(String newPassword) {
		VerticalLayout layout = new VerticalLayout();
		layout.add(new Span(I18nProperties.getString(Strings.messageCopyPassword)));
		Span passwordLabel = new Span(newPassword);
		passwordLabel.addClassName(CssStyles.H2);
		layout.add(passwordLabel);
		Dialog popupWindow = VaadinUiUtil.showPopupWindow(layout);
		popupWindow.setHeaderTitle(I18nProperties.getString(Strings.headingNewPassword));
		layout.setPadding(true);
	}

	private void showAccountCreatedSuccessful() {
		VerticalLayout layout = new VerticalLayout();
		layout.add(new Span(I18nProperties.getString(Strings.messageActivateAccount)));
		Dialog popupWindow = VaadinUiUtil.showPopupWindow(layout);
		popupWindow.setHeaderTitle(I18nProperties.getString(Strings.headingNewAccount));
		popupWindow.setWidth(350, Unit.PIXELS);
		layout.setPadding(true);
	}

	private void showPasswordResetExternalSuccessPopup() {
		VerticalLayout layout = new VerticalLayout();
		layout.add(new Span(I18nProperties.getString(Strings.messagePasswordResetEmailLink)));
		Dialog popupWindow = VaadinUiUtil.showPopupWindow(layout);
		popupWindow.setHeaderTitle(I18nProperties.getString(Strings.headingNewPassword));
		popupWindow.setWidth(450, Unit.PIXELS);
		layout.setPadding(true);
	}

	private void refreshView() {
		View currentView = SormasUI.get().getNavigator().getCurrentView();
		if (currentView instanceof UsersView) {
			// force refresh, because view didn't change
			((UsersView) currentView).enter(null);
		}
	}

	public Button createResetPasswordButton(String userUuid, String userEmail, CommitDiscardWrapperComponent<UserEditForm> editView) {

		return ButtonHelper.createIconButton(Captions.userResetPassword, VaadinIcon.UNLOCK, event -> {
			ConfirmationComponent resetPasswordComponent = getResetPasswordConfirmationComponent(userUuid, userEmail, editView);
			Dialog popupWindow = VaadinUiUtil.showPopupWindow(resetPasswordComponent);
			resetPasswordComponent.addDoneListener(() -> popupWindow.close());
			resetPasswordComponent.getCancelButton().addClickListener(e -> popupWindow.close());
			popupWindow.setHeaderTitle(I18nProperties.getString(Strings.headingUpdatePassword));
		}, CssStyles.BUTTON_LINK);
	}

	public ConfirmationComponent getResetPasswordConfirmationComponent(
		String userUuid,
		String userEmail,
		CommitDiscardWrapperComponent<UserEditForm> editView) {
		ConfirmationComponent resetPasswordConfirmationComponent = new ConfirmationComponent(false) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfirm() {
				onDone();
				editView.discard();
				makeNewPassword(userUuid, userEmail);
			}

			@Override
			protected void onCancel() {
			}
		};
		resetPasswordConfirmationComponent.getConfirmButton().setText(I18nProperties.getCaption(Captions.userUpdatePasswordConfirmation));
		resetPasswordConfirmationComponent.getCancelButton().setText(I18nProperties.getCaption(Captions.actionCancel));
		resetPasswordConfirmationComponent.setPadding(true);
		return resetPasswordConfirmationComponent;
	}

	public CommitDiscardWrapperComponent<UserSettingsForm> getUserSettingsComponent(Runnable commitOrDiscardCallback) {
		UserSettingsForm form = new UserSettingsForm();
		UserDto user = FacadeProvider.getUserFacade().getByUuid(UiUtil.getUserUuid());
		form.setValue(user);

		final CommitDiscardWrapperComponent<UserSettingsForm> component = new CommitDiscardWrapperComponent<>(form, form.getFieldGroup());
		component.addCommitListener(() -> {
			if (!form.getFieldGroup().isModified()) {
				UserDto changedUser = form.getValue();
				FacadeProvider.getUserFacade().saveUser(changedUser, true);
				I18nProperties.setUserLanguage(changedUser.getLanguage());
				FacadeProvider.getI18nFacade().setUserLanguage(changedUser.getLanguage());
				UI.getCurrent().getPage().reload();
				commitOrDiscardCallback.run();
			}
		});
		component.addDiscardListener(commitOrDiscardCallback::run);

		return component;
	}

	public void setFlagIcons(ComboBox cbLanguage) {
		// was setItemIcon per language (a ThemeResource of the sormas theme): Flow combo boxes show item icons through
		// a renderer, in the item list
		cbLanguage.setRenderer(new ComponentRenderer<Component, Object>(item -> {
			HorizontalLayout itemLayout = new HorizontalLayout();
			if (item instanceof Language) {
				itemLayout.add(
					new Image(VaadinUiUtil.THEME_IMAGE_PATH + "img/flag-icons/" + ((Language) item).name().toLowerCase() + ".png", ""));
			}
			itemLayout.add(new Span(cbLanguage.getItemCaption(item)));
			return itemLayout;
		}));
	}

	public void sync() {
		if (UiUtil.permitted(FeatureType.AUTH_PROVIDER_TO_SORMAS_USER_SYNC)) {
			if (StringUtils.isBlank(FacadeProvider.getConfigFacade().getAuthenticationProviderSyncedNewUserRole())) {
				VaadinUiUtil.showSimplePopupWindow(
					I18nProperties.getString(Strings.headingSyncUsers),
					I18nProperties.getString(Strings.messageSyncUsersFromAuthProviderConfigurationError));
				return;
			}
			FacadeProvider.getUserFacade().syncUsersFromAuthenticationProvider();
			SormasUI.refreshView();
		} else {
			Dialog window = VaadinUiUtil.showPopupWindow(new UsersSyncLayout());
			window.setHeaderTitle(I18nProperties.getCaption(Captions.syncUsers));
		}
	}

	public void enableAllSelectedItems(Collection<UserDto> selectedRows, UserGrid userGrid) {

		if (selectedRows.isEmpty()) {
			NotificationHelper.show(
				I18nProperties.getString(Strings.headingNoUsersSelected),
				I18nProperties.getString(Strings.messageNoUsersSelected),
				NotificationHelper.Type.WARNING_MESSAGE,
				false);
		} else {
			VaadinUiUtil.showConfirmationPopup(
				I18nProperties.getString(Strings.headingConfirmEnabling),
				new Span(String.format(I18nProperties.getString(Strings.confirmationEnableUsers), selectedRows.size())),
				I18nProperties.getString(Strings.yes),
				I18nProperties.getString(Strings.no),
				null,
				confirmed -> {
					if (Boolean.FALSE.equals(confirmed)) {
						return;
					}

					new BulkOperationHandler<UserDto>(
						Strings.messageUsersEnabled,
						null,
						Strings.headingSomeUsersNotEnabled,
						Strings.headingUsersNotEnabled,
						Strings.messageCountUsersNotEnabled,
						null,
						null,
						null,
						null,
						Strings.infoBulkProcessFinishedWithSkips,
						Strings.infoBulkProcessFinishedWithoutSuccess).doBulkOperation(batch -> {

							List<String> uuids = batch.stream().map(UserDto::getUuid).collect(Collectors.toList());
							return FacadeProvider.getUserFacade().enableUsers(uuids);
						}, new ArrayList<>(selectedRows), remaining -> {
							userGrid.reload();
							if (CollectionUtils.isNotEmpty(remaining)) {
								userGrid.asMultiSelect().select(remaining.toArray(new UserDto[0]));
							} else {
								overview();
							}
						});
				});
		}
	}

	public void disableAllSelectedItems(Collection<UserDto> selectedRows, UserGrid userGrid) {

		if (selectedRows.isEmpty()) {
			NotificationHelper.show(
				I18nProperties.getString(Strings.headingNoUsersSelected),
				I18nProperties.getString(Strings.messageNoUsersSelected),
				NotificationHelper.Type.WARNING_MESSAGE,
				false);
		} else {
			VaadinUiUtil.showConfirmationPopup(
				I18nProperties.getString(Strings.headingConfirmDisabling),
				new Span(String.format(I18nProperties.getString(Strings.confirmationDisableUsers), selectedRows.size())),
				I18nProperties.getString(Strings.yes),
				I18nProperties.getString(Strings.no),
				null,
				confirmed -> {
					if (Boolean.FALSE.equals(confirmed)) {
						return;
					}

					new BulkOperationHandler<UserDto>(
						Strings.messageUsersDisabled,
						null,
						Strings.headingSomeUsersNotDisabled,
						Strings.headingUsersNotDisabled,
						Strings.messageCountUsersNotDisabled,
						null,
						null,
						null,
						null,
						Strings.infoBulkProcessFinishedWithSkips,
						Strings.infoBulkProcessFinishedWithoutSuccess).doBulkOperation(batch -> {

							List<String> uuids = batch.stream().map(UserDto::getUuid).collect(Collectors.toList());
							return FacadeProvider.getUserFacade().disableUsers(uuids);
						}, new ArrayList<>(selectedRows), remaining -> {
							userGrid.reload();
							if (CollectionUtils.isNotEmpty(remaining)) {
								userGrid.asMultiSelect().select(remaining.toArray(new UserDto[0]));
							} else {
								overview();
							}
						});
				});
		}
	}
}
