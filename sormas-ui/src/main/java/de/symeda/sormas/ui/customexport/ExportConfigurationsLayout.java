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

import static de.symeda.sormas.ui.utils.CssStyles.H3;

import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.importexport.ExportConfigurationDto;
import de.symeda.sormas.api.importexport.ExportPropertyMetaInfo;
import de.symeda.sormas.api.importexport.ExportType;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

@SuppressWarnings("serial")
public class ExportConfigurationsLayout extends VerticalLayout {

	private Span lblDescription;
	private Button btnNewExportConfiguration;
	private ExportConfigurationsGrid grid;
	private ExportConfigurationsGrid gridSharedExportsToPublic;

	public ExportConfigurationsLayout(ExportType exportType, List<ExportPropertyMetaInfo> availableProperties, Runnable closeCallback) {

		lblDescription = new Span(I18nProperties.getString(Strings.infoCustomExport));
		lblDescription.setWidthFull();
		add(lblDescription);

		btnNewExportConfiguration = ButtonHelper.createIconButton(Captions.exportNewExportConfiguration, VaadinIcon.PLUS, e -> {
			ExportConfigurationDto newConfig = ExportConfigurationDto.build(UiUtil.getUserReference(), exportType);
			ControllerProvider.getCustomExportController()
				.openEditExportConfigurationWindow(
					grid,
					newConfig,
					availableProperties,
					I18nProperties.getCaption(Captions.exportNewExportConfiguration));
		}, CssStyles.BUTTON_PRIMARY);
		add(btnNewExportConfiguration);
		setHorizontalComponentAlignment(Alignment.END, btnNewExportConfiguration);

		Span myExportsLabel = new Span(I18nProperties.getPrefixCaption(ExportConfigurationDto.I18N_PREFIX, Captions.ExportConfiguration_myExports));
		myExportsLabel.addClassName(H3);
		add(myExportsLabel);

		grid = new ExportConfigurationsGrid(exportType, availableProperties, false);
		grid.setWidthFull();
		add(grid);

		Span sharedExportsLabel =
			new Span(I18nProperties.getPrefixCaption(ExportConfigurationDto.I18N_PREFIX, Captions.ExportConfiguration_sharedExports));
		sharedExportsLabel.addClassName(H3);

		gridSharedExportsToPublic = new ExportConfigurationsGrid(exportType, availableProperties, true);
		if (gridSharedExportsToPublic.getNbOfSharedExportsToPublic() > 0) {
			gridSharedExportsToPublic.setWidthFull();
			add(sharedExportsLabel);
			add(gridSharedExportsToPublic);
		}

		Button btnClose = ButtonHelper.createButton(Captions.actionClose, e -> closeCallback.run());
		add(btnClose);
		setHorizontalComponentAlignment(Alignment.END, btnClose);
	}

	public void setExportCallback(Consumer<ExportConfigurationDto> exportCallback) {
		grid.setExportCallback(exportCallback);
		gridSharedExportsToPublic.setExportCallback(exportCallback);
	}

}
