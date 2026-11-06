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
package de.symeda.sormas.ui.statistics;

import de.symeda.sormas.api.ConfigFacade;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.feature.FeatureConfigurationDto;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.importexport.DatabaseTable;
import de.symeda.sormas.api.importexport.DatabaseTableType;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;
import de.symeda.sormas.ui.utils.fields.CheckBox;

public class DatabaseExportView extends AbstractStatisticsView {

	private static final long serialVersionUID = 1557269026685787333L;

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/database-export";

	private VerticalLayout databaseExportLayout;
	private Map<CheckBox, DatabaseTable> databaseTableToggles;

	public DatabaseExportView() {

		super(VIEW_NAME);

		databaseTableToggles = new HashMap<>();
		databaseExportLayout = new VerticalLayout();
		databaseExportLayout.setSpacing(false);
		databaseExportLayout.setPadding(false);
		HorizontalLayout headerLayout = new HorizontalLayout();
		headerLayout.setSpacing(true);
		headerLayout.setPadding(false);
		Span infoLabel = new Span(I18nProperties.getString(Strings.infoDatabaseExportTables));
		headerLayout.add(infoLabel);
		headerLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, infoLabel);
		headerLayout.add(createSelectionButtonsLayout());
		databaseExportLayout.add(headerLayout);
		databaseExportLayout.add(createDatabaseTablesLayout());

		Button exportButton = ButtonHelper.createIconButton(Captions.export, VaadinIcon.DOWNLOAD, null, CssStyles.BUTTON_PRIMARY);

		DownloadHandler streamResource = DownloadUtil
			.createDatabaseExportStreamResource(this, "sormas_export_" + DateHelper.formatDateForExport(new Date()) + ".zip", "application/zip");
		// was a FileDownloader on the button: a download link around the button, added in its place
		databaseExportLayout.add(DownloadUtil.createDownloadLink(exportButton, streamResource));
		databaseExportLayout.setPadding(true);
		databaseExportLayout.setSpacing(true);

		addComponent(databaseExportLayout);
	}

	public void showExportErrorNotification() {
		NotificationHelper.show(
			I18nProperties.getString(Strings.headingDatabaseExportFailed),
			I18nProperties.getString(Strings.messageDatabaseExportFailed),
			Type.ERROR_MESSAGE,
			false);
	}

	private HorizontalLayout createSelectionButtonsLayout() {
		HorizontalLayout selectionButtonsLayout = new HorizontalLayout();
		selectionButtonsLayout.setPadding(false);
		selectionButtonsLayout.setSpacing(true);

		Button selectAll = ButtonHelper.createButton(Captions.actionSelectAll, e -> {
			for (CheckBox checkBox : databaseTableToggles.keySet()) {
				checkBox.setValue(true);
			}
		}, CssStyles.BUTTON_LINK);

		selectionButtonsLayout.add(selectAll);

		Button selectAllSormasData = ButtonHelper.createButton(Captions.exportSelectSormasData, e -> {
			for (CheckBox checkBox : databaseTableToggles.keySet()) {
				if (databaseTableToggles.get(checkBox).getDatabaseTableType() == DatabaseTableType.SORMAS) {
					checkBox.setValue(true);
				} else {
					checkBox.setValue(false);
				}
			}
		}, CssStyles.BUTTON_LINK);

		selectionButtonsLayout.add(selectAllSormasData);

		Button deselectAll = ButtonHelper.createButton(Captions.actionDeselectAll, e -> {
			for (CheckBox checkBox : databaseTableToggles.keySet()) {
				checkBox.setValue(false);
			}
		}, CssStyles.BUTTON_LINK);

		selectionButtonsLayout.add(deselectAll);

		return selectionButtonsLayout;
	}

	private HorizontalLayout createDatabaseTablesLayout() {
		HorizontalLayout databaseTablesLayout = new HorizontalLayout();
		databaseTablesLayout.setPadding(false);
		databaseTablesLayout.setSpacing(true);

		VerticalLayout sormasDataLayout = new VerticalLayout();
		sormasDataLayout.setPadding(false);
		sormasDataLayout.setSpacing(false);
		Span sormasDataHeadline = new Span(I18nProperties.getCaption(Captions.exportSormasData));
		CssStyles.style(sormasDataHeadline, CssStyles.H4);
		sormasDataLayout.add(sormasDataHeadline);

		VerticalLayout infrastructureDataLayout = new VerticalLayout();
		infrastructureDataLayout.setPadding(false);
		infrastructureDataLayout.setSpacing(false);
		Span infrastructureDataHeadline = new Span(I18nProperties.getCaption(Captions.exportInfrastructureData));
		CssStyles.style(infrastructureDataHeadline, CssStyles.H4);
		infrastructureDataLayout.add(infrastructureDataHeadline);

		VerticalLayout configurationDataLayout = new VerticalLayout();
		configurationDataLayout.setPadding(false);
		configurationDataLayout.setSpacing(false);
		Span configurationDataHeadline = new Span(I18nProperties.getCaption(Captions.exportConfigurationData));
		CssStyles.style(configurationDataHeadline, CssStyles.H4);
		configurationDataLayout.add(configurationDataHeadline);

		VerticalLayout externalDataLayout = new VerticalLayout();
		externalDataLayout.setPadding(false);
		externalDataLayout.setSpacing(false);
		Span externalDataHeadline = new Span(I18nProperties.getCaption(Captions.exportExternalData));
		CssStyles.style(externalDataHeadline, CssStyles.H4);
		externalDataLayout.add(externalDataHeadline);

		List<FeatureConfigurationDto> featureConfigurations = FacadeProvider.getFeatureConfigurationFacade().getActiveServerFeatureConfigurations();
		ConfigFacade configFacade = FacadeProvider.getConfigFacade();

		for (DatabaseTable databaseTable : DatabaseTable.values()) {
			if(!databaseTable.isEnabled(featureConfigurations, configFacade)) {
				continue;
			}

			CheckBox checkBox = new CheckBox(databaseTable.toString());
			int indent = getIndent(databaseTable);
			if (indent == 1) {
				CssStyles.style(checkBox, CssStyles.INDENT_LEFT_1);
			} else if (indent == 2) {
				CssStyles.style(checkBox, CssStyles.INDENT_LEFT_2);
			} else if (indent == 3) {
				CssStyles.style(checkBox, CssStyles.INDENT_LEFT_3);
			}

			switch (databaseTable.getDatabaseTableType()) {
			case SORMAS:
				sormasDataLayout.add(checkBox);
				break;
			case INFRASTRUCTURE:
				infrastructureDataLayout.add(checkBox);
				break;
			case CONFIGURATION:
				configurationDataLayout.add(checkBox);
				break;
			case EXTERNAL:
				externalDataLayout.add(checkBox);
				break;
			default:
				throw new IllegalArgumentException(databaseTable.getDatabaseTableType().toString());
			}

			databaseTableToggles.put(checkBox, databaseTable);
		}

		databaseTablesLayout.add(sormasDataLayout);
		databaseTablesLayout.add(infrastructureDataLayout);
		databaseTablesLayout.add(configurationDataLayout);
		databaseTablesLayout.add(externalDataLayout);
		return databaseTablesLayout;
	}

	private int getIndent(DatabaseTable databaseTable) {

		int indent = 0;
		while (databaseTable.getParentTable() != null) {
			indent++;
			databaseTable = databaseTable.getParentTable();
		}
		return indent;
	}

	@Override
	public void enter(ViewChangeEvent event) {
		super.enter(event);
	}

	public Map<CheckBox, DatabaseTable> getDatabaseTableToggles() {
		return databaseTableToggles;
	}
}
