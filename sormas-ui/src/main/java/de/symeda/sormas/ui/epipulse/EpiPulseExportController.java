/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2024 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.epipulse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Date;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.epipulse.EpipulseExportDto;
import de.symeda.sormas.api.epipulse.EpipulseExportIndexDto;
import de.symeda.sormas.api.epipulse.EpipulseExportStatus;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.ValidationRuntimeException;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.UserProvider;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent;
import de.symeda.sormas.ui.utils.ConfirmationComponent;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;
import de.symeda.sormas.ui.utils.StreamSourceDownloadHandler;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class EpiPulseExportController {

	public EpiPulseExportController() {
	}

	public void create(Runnable callback) {

		boolean configured = FacadeProvider.getEpipulseExportFacade().isConfigured();
		if (configured) {
			EpipulseEditForm createForm = new EpipulseEditForm(true);
			createForm.setValue(EpipulseExportDto.build(UserProvider.getCurrent().getUserReference()));
			final CommitDiscardWrapperComponent<EpipulseEditForm> editView = new CommitDiscardWrapperComponent<EpipulseEditForm>(
				createForm,
				UiUtil.permitted(UserRight.EPIPULSE_EXPORT_CREATE),
				createForm.getFieldGroup());

			editView.addCommitListener(() -> {
				if (!createForm.getFieldGroup().isModified()) {
					EpipulseExportDto dto = createForm.getValue();
					dto.setStatus(EpipulseExportStatus.PENDING);
					dto.setStatusChangeDate(new Date());

					EpipulseExportDto savedEpipulseExport = FacadeProvider.getEpipulseExportFacade().saveEpipulseExport(dto);

					Notification notification;
					if (savedEpipulseExport != null) {
						notification = NotificationHelper.createNotification(
							I18nProperties.getString(Strings.messageEpipulseExportCreatedCaption),
							I18nProperties.getString(Strings.messageEpipulseExportCreatedDescription),
							Type.TRAY_NOTIFICATION,
							true);
						NotificationHelper.setDelayMsec(notification, -1);
					} else {
						notification = NotificationHelper.createNotification(
							"",
							I18nProperties.getString(Strings.messageEpipulseExportCreatedError),
							Type.ERROR_MESSAGE,
							true);
						NotificationHelper.setDelayMsec(notification, -1);
					}

					notification.open();

					if (callback != null) {
						callback.run();
					}

				}
			});

			VaadinUiUtil.showModalPopupWindow(editView, I18nProperties.getString(Strings.headingCreateNewEpipulseExport));
		} else {
			Notification notification =
				NotificationHelper.createNotification("", I18nProperties.getString(Strings.messageEpipulseInvalidConfigError), Type.ERROR_MESSAGE, true);
			NotificationHelper.setDelayMsec(notification, -1);
			notification.open();
		}
	}

	public void view(EpipulseExportIndexDto exportIndexDto, Runnable callback) {
		EpipulseExportDto epipulseExportDto = FacadeProvider.getEpipulseExportFacade().getEpiPulseExportByUuid(exportIndexDto.getUuid());
		EpipulseExportInfoLayout exportInfoLayout = new EpipulseExportInfoLayout(epipulseExportDto, null);

		VaadinUiUtil.showConfirmationPopup(I18nProperties.getString(Strings.headingViewEpipulseExport), exportInfoLayout, popupWindow -> {
			ConfirmationComponent confirmationComponent = new ConfirmationComponent(false) {

				private static final long serialVersionUID = 1L;

				@Override
				protected void onConfirm() {
					popupWindow.close();
				}

				@Override
				protected void onCancel() {
					popupWindow.close();
				}
			};

			confirmationComponent.getConfirmButton().setText(I18nProperties.getString(Strings.close));
			confirmationComponent.getCancelButton().setVisible(false);

			EpipulseExportStatus exportStatus = epipulseExportDto.getStatus();

			if (exportStatus == EpipulseExportStatus.PENDING || exportStatus == EpipulseExportStatus.IN_PROGRESS) {
				Button cancelExportButton = ButtonHelper.createButton(I18nProperties.getCaption(Captions.actionCancel), event -> {
					VaadinUiUtil.showConfirmationPopup(
						I18nProperties.getString(Strings.messageEpipulseExportCancelConfirmationCaption),
						new Span(I18nProperties.getString(Strings.messageEpipulseExportCancelConfirmationDescription)),
						I18nProperties.getCaption(Captions.actionYes),
						I18nProperties.getCaption(Captions.actionNo),
						null,
						result -> {
							if (result) {
								// Cancel the export
								try {
									FacadeProvider.getEpipulseExportFacade().cancelEpipulseExport(epipulseExportDto.getUuid());

									if (callback != null) {
										callback.run();
									}

									popupWindow.close();
								} catch (Exception e) {
									if (e instanceof ValidationRuntimeException) {
										NotificationHelper.show(e.getMessage(), Type.ERROR_MESSAGE);
									} else {
										NotificationHelper.show(
											I18nProperties.getString(Strings.errorOccurred, I18nProperties.getString(Strings.errorOccurred)),
											I18nProperties.getString(Strings.errorWasReported),
											Type.ERROR_MESSAGE);
									}
								}
							}
						});
				}, CssStyles.BUTTON_LINK);
				confirmationComponent.addExtraButton(cancelExportButton, e -> {
				});
			}

			// Delete button - always available
			if (exportStatus == EpipulseExportStatus.COMPLETED
				|| exportStatus == EpipulseExportStatus.FAILED
				|| exportStatus == EpipulseExportStatus.CANCELLED) {
				Button deleteButton = ButtonHelper.createButton(I18nProperties.getCaption(Captions.actionDelete), event -> {

					VaadinUiUtil.showConfirmationPopup(
						I18nProperties.getString(Strings.messageEpipulseExportDeleteConfirmationCaption),
						new Span(I18nProperties.getString(Strings.messageEpipulseExportDeleteConfirmationDescription)),
						I18nProperties.getCaption(Captions.actionYes),
						I18nProperties.getCaption(Captions.actionNo),
						null,
						result -> {
							if (result) {
								// Cancel the export
								try {
									FacadeProvider.getEpipulseExportFacade().deleteEpipulseExport(epipulseExportDto.getUuid());

									if (callback != null) {
										callback.run();
									}

									popupWindow.close();
								} catch (Exception e) {
									if (e instanceof ValidationRuntimeException) {
										NotificationHelper.show(e.getMessage(), Type.ERROR_MESSAGE);
									} else {
										NotificationHelper.show(
											I18nProperties.getString(Strings.errorOccurred, I18nProperties.getString(Strings.errorOccurred)),
											I18nProperties.getString(Strings.errorWasReported),
											Type.ERROR_MESSAGE);
									}
								}
							}
						});
				}, CssStyles.BUTTON_LINK);
				confirmationComponent.addExtraButton(deleteButton, e -> {
				});
			}

			return confirmationComponent;
		}, 640);
	}

	public void download(EpipulseExportIndexDto exportIndexDto) {
		if (exportIndexDto.getExportFileName() == null) {
			Notification notification = NotificationHelper.createNotification(
				"",
				I18nProperties.getString(Strings.messageEpipulseExportDownloadNoFileName),
				Type.ERROR_MESSAGE,
				true);
			NotificationHelper.setDelayMsec(notification, -1);
			notification.open();
			return;
		}

		String generatedFilesPath = FacadeProvider.getConfigFacade().getGeneratedFilesPath();
		String exportFilePath = generatedFilesPath + "/" + exportIndexDto.getExportFileName();

		File file = new File(exportFilePath);
		if (!file.exists()) {
			Notification notification = NotificationHelper.createNotification(
				"",
				I18nProperties.getString(Strings.messageEpipulseExportDownloadNoFileName),
				Type.ERROR_MESSAGE,
				true);
			NotificationHelper.setDelayMsec(notification, -1);
			notification.open();
			return;
		}

		// cache time 0 as before: StreamSourceDownloadHandler sends no-cache
		StreamSourceDownloadHandler streamResource = new StreamSourceDownloadHandler(() -> {
			try {
				return new FileInputStream(file);
			} catch (FileNotFoundException e) {
				NotificationHelper.show(I18nProperties.getString(Strings.messageEpipulseExportDownloadNoFileName), Type.ERROR_MESSAGE);
				return null;
			}
		}, exportIndexDto.getExportFileName(), "text/csv");

		// was Page.open(resource, null, false); called from the export grid of the view, so the UI owns it
		DownloadUtil.startDownload(streamResource, UI.getCurrent());
	}
}
