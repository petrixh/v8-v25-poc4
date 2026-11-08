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

package de.symeda.sormas.ui.customexport;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import de.symeda.sormas.ui.utils.NotificationHelper;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.contact.ContactCriteria;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.importexport.ExportConfigurationDto;
import de.symeda.sormas.api.importexport.ExportPropertyMetaInfo;
import de.symeda.sormas.api.importexport.ExportType;
import de.symeda.sormas.api.importexport.ImportExportUtils;
import de.symeda.sormas.api.person.PersonCriteria;
import de.symeda.sormas.api.task.TaskCriteria;
import de.symeda.sormas.ui.utils.ContactDownloadUtil;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.PersonDownloadUtil;
import de.symeda.sormas.ui.utils.TaskDownloadUtil;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class CustomExportController {

	public void openContactExportWindow(ContactCriteria contactCriteria, Supplier<Collection<String>> selectedRows) {
		Dialog customExportWindow = VaadinUiUtil.createPopupWindow();
		ExportConfigurationsLayout customExportsLayout = new ExportConfigurationsLayout(
			ExportType.CONTACT,
			ImportExportUtils.getContactExportProperties(
				ContactDownloadUtil::getPropertyCaption,
				FacadeProvider.getConfigFacade().getCountryLocale(),
				FacadeProvider.getFeatureConfigurationFacade().getActiveServerFeatureConfigurations()),
			customExportWindow::close);
		customExportsLayout.setExportCallback(
			exportConfig -> openDownload(
				ContactDownloadUtil.createContactExportResource(contactCriteria, selectedRows, exportConfig),
				customExportsLayout));
		customExportWindow.setWidth(1024, Unit.PIXELS);
		customExportWindow.setHeaderTitle(I18nProperties.getCaption(Captions.exportCustom));
		customExportWindow.add(customExportsLayout);
		customExportWindow.open();
	}

	public void openTaskExportWindow(TaskCriteria taskCriteria, Supplier<Collection<String>> selectedRows) {
		Dialog customExportWindow = VaadinUiUtil.createPopupWindow();
		ExportConfigurationsLayout customExportsLayout = new ExportConfigurationsLayout(
			ExportType.TASK,
			ImportExportUtils.getTaskExportProperties(
				TaskDownloadUtil::getPropertyCaption,
				FacadeProvider.getConfigFacade().getCountryLocale(),
				FacadeProvider.getFeatureConfigurationFacade().getActiveServerFeatureConfigurations()),
			customExportWindow::close);
		customExportsLayout.setExportCallback(
			exportConfig -> openDownload(TaskDownloadUtil.createTaskExportResource(taskCriteria, selectedRows, exportConfig), customExportsLayout));
		customExportWindow.setWidth(1024, Unit.PIXELS);
		customExportWindow.setHeaderTitle(I18nProperties.getCaption(Captions.exportCustom));
		customExportWindow.add(customExportsLayout);
		customExportWindow.open();
	}

	public void openPersonExportWindow(PersonCriteria personCriteria) {
		Dialog customExportWindow = VaadinUiUtil.createPopupWindow();
		ExportConfigurationsLayout customExportsLayout = new ExportConfigurationsLayout(
			ExportType.PERSON,
			ImportExportUtils.getPersonExportProperties(
				PersonDownloadUtil::getPropertyCaption,
				FacadeProvider.getConfigFacade().getCountryLocale(),
				FacadeProvider.getFeatureConfigurationFacade().getActiveServerFeatureConfigurations()),
			customExportWindow::close);
		customExportsLayout.setExportCallback(
			exportConfig -> openDownload(PersonDownloadUtil.createPersonExportResource(personCriteria, exportConfig), customExportsLayout));
		customExportWindow.setWidth(1024, Unit.PIXELS);
		customExportWindow.setHeaderTitle(I18nProperties.getCaption(Captions.exportCustom));
		customExportWindow.add(customExportsLayout);
		customExportWindow.open();
	}

	public void openEditExportConfigurationWindow(
		ExportConfigurationsGrid grid,
		ExportConfigurationDto config,
		List<ExportPropertyMetaInfo> availableProperties,
		String caption) {

		Dialog exportWindow = VaadinUiUtil.createPopupWindow();
		ExportConfigurationEditLayout editLayout = new ExportConfigurationEditLayout(config, availableProperties, (exportConfiguration) -> {
			FacadeProvider.getExportFacade().saveExportConfiguration(exportConfiguration);
			exportWindow.close();
			NotificationHelper.show(null, I18nProperties.getString(Strings.messageExportConfigurationSaved), NotificationHelper.Type.WARNING_MESSAGE, false);
			grid.reload(false);
		}, () -> {
			exportWindow.close();
			grid.reload(false);
		});
		exportWindow.setWidth(1024, Unit.PIXELS);
		exportWindow.setHeaderTitle(caption);
		exportWindow.add(editLayout);
		exportWindow.open();
	}

	/**
	 * Was {@code Page.getCurrent().open(resource, null, true)}; owned by the export window's layout, which is not inert.
	 */
	private static void openDownload(DownloadHandler resource, Component owner) {
		DownloadUtil.startDownload(resource, owner);
	}
}
