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

package de.symeda.sormas.ui.configuration.docgeneration;

import java.io.ByteArrayInputStream;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.symeda.sormas.ui.utils.grid.HeightMode;
import com.vaadin.flow.component.button.Button;
import de.symeda.sormas.ui.utils.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.symeda.sormas.ui.utils.NotificationHelper;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.docgeneneration.DocumentTemplateCriteria;
import de.symeda.sormas.api.docgeneneration.DocumentTemplateDto;
import de.symeda.sormas.api.docgeneneration.DocumentTemplateException;
import de.symeda.sormas.api.docgeneneration.DocumentWorkflow;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.StreamSourceDownloadHandler;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class DocumentTemplatesGrid extends Grid<DocumentTemplateDto> {

	private static final long serialVersionUID = 2589713987152595369L;

	private final DocumentWorkflow documentWorkflow;
	private final DocumentTemplateCriteria criteria;

	public DocumentTemplatesGrid(DocumentTemplateCriteria documentTemplateCriteria, boolean hasDisease) {
		super(DocumentTemplateDto.class);
		this.criteria = documentTemplateCriteria;
		this.documentWorkflow = documentTemplateCriteria.getDocumentWorkflow();
		setSizeFull();

		List<DocumentTemplateDto> availableTemplates = FacadeProvider.getDocumentTemplateFacade().getAvailableTemplates(documentTemplateCriteria);
		setItems(availableTemplates);

		setColumns(DocumentTemplateDto.FILE_NAME);
		if (hasDisease) {
			addColumn(DocumentTemplateDto.DISEASE);
		}

		for (Column<DocumentTemplateDto, ?> column : getAllColumns()) {
			column.setCaption(I18nProperties.getPrefixCaption(DocumentTemplateDto.I18N_PREFIX, column.getKey(), column.getCaption()));
		}

		addComponentColumn(this::buildActionButtons).setCaption(I18nProperties.getCaption(Captions.eventActionsView))
			.setWidth(100)
			.setStyleGenerator(item -> "v-align-center");

		setSelectionMode(SelectionMode.NONE);
		setHeightMode(HeightMode.ROW);
		setHeightByRows(Math.max(1, availableTemplates.size()));
	}

	public void reload() {
		// This is bad practice but it works (unlike refreshAll), and in this case its sufficient
		List<DocumentTemplateDto> availableTemplates = FacadeProvider.getDocumentTemplateFacade().getAvailableTemplates(criteria);
		setItems(availableTemplates);
		getDataProvider().refreshAll();
		setHeightByRows(Math.max(1, availableTemplates.size()));
	}

	private Button buildDeleteButton(DocumentTemplateDto template) {
		return ButtonHelper.createIconButton(
			"",
			VaadinIcon.TRASH,
			e -> VaadinUiUtil
				.showDeleteConfirmationWindow(String.format(I18nProperties.getString(Strings.confirmationDeleteFile), template.getFileName()), () -> {
					try {
						FacadeProvider.getDocumentTemplateFacade().deleteDocumentTemplate(template.toReference(), documentWorkflow);
					} catch (DocumentTemplateException ex) {
						NotificationHelper.show(
							I18nProperties.getString(Strings.errorDeletingDocumentTemplate),
							ex.getMessage(),
							NotificationHelper.Type.ERROR_MESSAGE,
							false);
					}
					reload();
				}));
	}

	private Component buildViewDocumentButton(DocumentTemplateDto template) {
		Button viewButton = ButtonHelper.createIconButton(VaadinIcon.DOWNLOAD);

		StreamSourceDownloadHandler streamResource = new StreamSourceDownloadHandler(() -> {
			try {
				return new ByteArrayInputStream(FacadeProvider.getDocumentTemplateFacade().getDocumentTemplateContent(template.toReference()));
			} catch (DocumentTemplateException e) {
				NotificationHelper.show(
					String.format(I18nProperties.getString(Strings.errorReadingTemplate), template.getFileName()),
					e.getMessage(),
					NotificationHelper.Type.ERROR_MESSAGE,
					false);
				return null;
			}
		}, template.getFileName());
		// was a FileDownloader on the button: a download link around it
		return DownloadUtil.createDownloadLink(viewButton, streamResource);
	}

	private HorizontalLayout buildActionButtons(DocumentTemplateDto template) {
		HorizontalLayout horizontalLayout = new HorizontalLayout();

		horizontalLayout.add(buildViewDocumentButton(template));
		horizontalLayout.add(buildDeleteButton(template));

		horizontalLayout.setSpacing(false);
		horizontalLayout.setPadding(false);
		horizontalLayout.setWidth("100px");

		return horizontalLayout;
	}
}
