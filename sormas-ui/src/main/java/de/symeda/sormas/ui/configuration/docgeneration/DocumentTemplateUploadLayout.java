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

import java.util.Map;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.docgeneneration.DocumentWorkflow;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.importer.DocumentTemplateReceiver;
import de.symeda.sormas.ui.importer.ImportLayoutComponent;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DownloadUtil;

/*
 * Layout for Uploading Templates
 */
public class DocumentTemplateUploadLayout extends VerticalLayout {

	protected Upload upload;
	private ImportLayoutComponent importGuideComponent;
	private final DocumentWorkflow documentWorkflow;
	private final boolean hasDisease;
	private DocumentTemplateReceiver documentTemplateReceiver;

	private static final Map<DocumentWorkflow, DocumentTemplateInfoData> templateInfoData = Map.ofEntries(
		Map.entry(
			DocumentWorkflow.QUARANTINE_ORDER_CASE,
			DocumentTemplateInfoData.forDocumentTemplate(Captions.DocumentTemplate_exampleTemplateCases, "ExampleDocumentTemplateCases.docx")),
		Map.entry(
			DocumentWorkflow.QUARANTINE_ORDER_CONTACT,
			DocumentTemplateInfoData.forDocumentTemplate(Captions.DocumentTemplate_exampleTemplateContacts, "ExampleDocumentTemplateContacts.docx")),
		Map.entry(
			DocumentWorkflow.QUARANTINE_ORDER_EVENT_PARTICIPANT,
			DocumentTemplateInfoData
				.forDocumentTemplate(Captions.DocumentTemplate_exampleTemplateEventParticipants, "ExampleDocumentTemplateEventParticipant.docx")),
		Map.entry(
			DocumentWorkflow.QUARANTINE_ORDER_TRAVEL_ENTRY,
			DocumentTemplateInfoData
				.forDocumentTemplate(Captions.DocumentTemplate_exampleTemplateTravelEntries, "ExampleDocumentTemplateTravelEntry.docx")),
		Map.entry(
			DocumentWorkflow.EVENT_HANDOUT,
			DocumentTemplateInfoData
				.forDocumentTemplate(Captions.DocumentTemplate_exampleTemplateEventHandout, "ExampleDocumentTemplateEventHandout.html")),
		Map.entry(
			DocumentWorkflow.CASE_EMAIL,
			DocumentTemplateInfoData.forEmailTemplate(Captions.DocumentTemplate_exampleTemplateCaseEmail, "ExampleDocumentTemplateCaseEmail.txt")),
		Map.entry(
			DocumentWorkflow.CONTACT_EMAIL,
			DocumentTemplateInfoData
				.forEmailTemplate(Captions.DocumentTemplate_exampleTemplateContactEmail, "ExampleDocumentTemplateContactEmail.txt")),
		Map.entry(
			DocumentWorkflow.EVENT_PARTICIPANT_EMAIL,
			DocumentTemplateInfoData.forEmailTemplate(
				Captions.DocumentTemplate_exampleTemplateEventParticipantEmail,
				"ExampleDocumentTemplateEventParticipantEmail.txt")),
		Map.entry(
			DocumentWorkflow.TRAVEL_ENTRY_EMAIL,
			DocumentTemplateInfoData
				.forEmailTemplate(Captions.DocumentTemplate_exampleTemplateTravelEntryEmail, "ExampleDocumentTemplateTravelEntryEmail.txt")),
		Map.entry(
			DocumentWorkflow.SURVEY_DOCUMENT,
			DocumentTemplateInfoData.forDocumentTemplate(Captions.DocumentTemplate_exampleTemplateSurveyDocument, "ExampleDocumentTemplateSurveys.docx")),
		Map.entry(
			DocumentWorkflow.SURVEY_EMAIL,
			DocumentTemplateInfoData.forDocumentTemplate(Captions.DocumentTemplate_exampleTemplateSurveyEmail, "ExampleDocumentTemplateSurveyEmail.txt")));

	public DocumentTemplateUploadLayout(DocumentWorkflow documentWorkflow, boolean hasDisease, DocumentTemplateReceiver documentTemplateReceiver) {
		super();
		this.documentWorkflow = documentWorkflow;
		this.hasDisease = hasDisease;
		this.documentTemplateReceiver = documentTemplateReceiver;
		addDownloadResourcesComponent();
		addUploadResourceComponent();
	}

	protected void addDownloadResourcesComponent() {
		importGuideComponent = new ImportLayoutComponent(
			1,
			I18nProperties.getString(Strings.headingDownloadDocumentTemplateGuide),
			I18nProperties.getString(Strings.infoDownloadDocumentTemplateImportGuide),
			VaadinIcon.FILE_PRESENTATION,
			I18nProperties.getCaption(Captions.DocumentTemplate_documentTemplateGuide));

		Button button = importGuideComponent.getButton();
		addFileDownloader(button, classResource("/" + templateInfoData.get(documentWorkflow).guideFileName));

		addExampleTemplatesQuarantineOrder();

		addDownloadResource(Captions.importDownloadDataDictionary, VaadinIcon.FILE_TABLE, classResource("/doc/SORMAS_Data_Dictionary.xlsx"));

		add(importGuideComponent);
	}

	private void addExampleTemplatesQuarantineOrder() {
		DocumentTemplateInfoData templateInfo = DocumentTemplateUploadLayout.templateInfoData.get(documentWorkflow);
		addDownloadResource(templateInfo.captionKey, VaadinIcon.FILE_TEXT, classResource("/" + templateInfo.fileName));
	}

	private void addUploadResourceComponent() {
		String headline = I18nProperties.getCaption(Captions.DocumentTemplate_uploadTemplate);
		String infoText = String.format(I18nProperties.getString(Strings.infoUploadDocumentTemplate), documentWorkflow.getFileExtension());

		ImportLayoutComponent uploadTemplateComponent = new ImportLayoutComponent(2, headline, infoText, null, null);
		add(uploadTemplateComponent);

		VerticalLayout uploadLatout = new VerticalLayout();
		uploadLatout.setPadding(false);
		uploadLatout.setSpacing(false);
		uploadLatout.setSizeFull();
		add(uploadLatout);

		if (hasDisease) {
			ComboBox<Disease> diseaseComboBox = new ComboBox<>();
			diseaseComboBox.setLabel(I18nProperties.getCaption(Captions.disease));
			diseaseComboBox.setItems(FacadeProvider.getDiseaseConfigurationFacade().getAllDiseases(true, true, true));
			diseaseComboBox.setPlaceholder(I18nProperties.getString(Strings.all));
			// was setEmptySelectionAllowed(true): the value can be cleared
			diseaseComboBox.setClearButtonVisible(true);
			uploadLatout.add(diseaseComboBox);
			documentTemplateReceiver.setDiseaseSupplier(diseaseComboBox::getValue);
		}

		// the receiver is a Flow UploadHandler (1a-69): it does the started and succeeded steps itself.
		// One file, uploaded when chosen, no drop zone; the list is cleared after each upload so the button can be used again
		upload = new Upload(documentTemplateReceiver);
		upload.setUploadButton(new Button(I18nProperties.getCaption(Captions.DocumentTemplate_buttonUploadTemplate)));
		upload.setMaxFiles(1);
		upload.setDropAllowed(false);
		upload.addAllFinishedListener(e -> upload.clearFileList());
		CssStyles.style(upload, CssStyles.VSPACE_2);
		uploadLatout.add(upload);
	}

	private void addDownloadResource(String caption, VaadinIcon icon, DownloadHandler resource) {
		Button exampleTemplateWordButton = ButtonHelper.createIconButton(caption, icon, null, CssStyles.BUTTON_PRIMARY, CssStyles.VSPACE_TOP_3);
		importGuideComponent.add(exampleTemplateWordButton);
		addFileDownloader(exampleTemplateWordButton, resource);
	}

	private void addFileDownloader(Button button, DownloadHandler importGuideResource) {
		// was a FileDownloader on the button: a download link around it, in its place
		DownloadUtil.attachDownloader(button, importGuideResource);
	}

	/**
	 * Was {@code new ClassResource(path)}: the file from the class path, downloaded under the last part of its path.
	 */
	private static DownloadHandler classResource(String path) {
		return DownloadHandler.forClassResource(DocumentTemplateUploadLayout.class, path, path.substring(path.lastIndexOf('/') + 1));
	}

	private static final class DocumentTemplateInfoData {

		private final String captionKey;
		private final String fileName;
		private final String guideFileName;
		private final String headingTextKey;
		private final String infoTextKey;

		public static DocumentTemplateInfoData forDocumentTemplate(String caption, String fileName) {
			return new DocumentTemplateInfoData(
				caption,
				fileName,
				"SORMAS_Document_Template_Guide.pdf",
				Strings.headingDownloadDocumentTemplateGuide,
				Strings.infoDownloadDocumentTemplateImportGuide);
		}

		public static DocumentTemplateInfoData forEmailTemplate(String caption, String fileName) {
			return new DocumentTemplateInfoData(
				caption,
				fileName,
				"SORMAS_Email_Template_Guide.pdf",
				Strings.headingDownloadEmailTemplateGuide,
				Strings.infoDownloadEmailTemplateImportGuide);
		}

		public static DocumentTemplateInfoData of(String caption, String fileName, String guideFileName, String headingTextKey, String infoTextKey) {
			return new DocumentTemplateInfoData(caption, fileName, guideFileName, headingTextKey, infoTextKey);
		}

		private DocumentTemplateInfoData(String captionKey, String fileName, String guideFileName, String headingTextKey, String infoTextKey) {
			this.captionKey = captionKey;
			this.fileName = fileName;
			this.guideFileName = guideFileName;
			this.headingTextKey = headingTextKey;
			this.infoTextKey = infoTextKey;
		}
	}
}
