/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2023 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

import static de.symeda.sormas.ui.utils.CssStyles.H3;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.dialog.Dialog;

import de.symeda.sormas.api.docgeneneration.DocumentTemplateCriteria;
import de.symeda.sormas.api.docgeneneration.DocumentWorkflow;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.importer.DocumentTemplateReceiver;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class DocumentTemplateSection extends VerticalLayout {

	private static final long serialVersionUID = 379271838736314055L;

	private DocumentTemplateReceiver documentTemplateReceiver;

	public DocumentTemplateSection(
		DocumentTemplateCriteria documentTemplateCriteria,
		boolean hasDisease,
		DocumentTemplateReceiver documentTemplateReceiver) {
		this.documentTemplateReceiver = documentTemplateReceiver;

		HorizontalLayout sectionHeader = new HorizontalLayout();

		DocumentTemplatesGrid documentTemplatesGrid = new DocumentTemplatesGrid(documentTemplateCriteria, hasDisease);
		documentTemplatesGrid.setWidth(700, Unit.PIXELS);

		Span quarantineTemplatesLabel = new Span(documentTemplateCriteria.getDocumentWorkflow().toString());
		quarantineTemplatesLabel.addClassName(H3);
		Button uploadButton = buildUploadButton(documentTemplateCriteria.getDocumentWorkflow(), documentTemplatesGrid, hasDisease);
		sectionHeader.add(quarantineTemplatesLabel, uploadButton);
		// was Alignment.MIDDLE_RIGHT: the label takes the free width, so the button sits on the right
		sectionHeader.setFlexGrow(1, quarantineTemplatesLabel);
		sectionHeader.setVerticalComponentAlignment(Alignment.CENTER, uploadButton);
		sectionHeader.setWidth(700, Unit.PIXELS);

		add(sectionHeader);
		add(documentTemplatesGrid);
		setFlexGrow(1, documentTemplatesGrid);
	}

	private Button buildUploadButton(DocumentWorkflow documentWorkflow, DocumentTemplatesGrid documentTemplatesGrid, boolean hasDisease) {
		return ButtonHelper.createIconButton(I18nProperties.getCaption(Captions.DocumentTemplate_uploadTemplate), VaadinIcon.UPLOAD, e -> {
			Dialog window = VaadinUiUtil.showPopupWindow(new DocumentTemplateUploadLayout(documentWorkflow, hasDisease, documentTemplateReceiver));
			window.setHeaderTitle(String.format(I18nProperties.getCaption(Captions.DocumentTemplate_uploadWorkflowTemplate), documentWorkflow));
			VaadinUiUtil.addCloseListener(window, c -> documentTemplatesGrid.reload());
		}, CssStyles.BUTTON_PRIMARY);
	}
}
