package de.symeda.sormas.ui.importer;

import static de.symeda.sormas.ui.docgeneration.DocGenerationHelper.isFileSizeLimitExceeded;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.function.Supplier;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.docgeneneration.DocumentTemplateDto;
import de.symeda.sormas.api.docgeneneration.DocumentTemplateException;
import de.symeda.sormas.api.docgeneneration.DocumentWorkflow;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.importexport.ImportExportUtils;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.ValidationRuntimeException;
import de.symeda.sormas.ui.SormasErrorHandler;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class DocumentTemplateReceiver implements UploadHandler {

	private File file;
	private String fName;
	private final DocumentWorkflow documentWorkflow;
	private Supplier<Disease> diseaseSupplier;
	/**
	 * Read when the receiver is created: the upload request runs outside the UI, where the current user is not known.
	 */
	private final String userUuid;

	public DocumentTemplateReceiver(DocumentWorkflow documentWorkflow) {
		this.documentWorkflow = documentWorkflow;
		this.diseaseSupplier = () -> null;
		this.userUuid = UiUtil.getUserUuid();
	}

	/**
	 * Was the V7 upload's {@code Receiver}, {@code StartedListener} and {@code SucceededListener}. Flow calls this in the
	 * upload request, without the session lock and outside the UI: the size check and the file are handled here, in the
	 * same order as before; notifications, errors and saving the template run in the UI ({@link UI#access}).
	 */
	@Override
	public void handleUploadRequest(UploadEvent event) throws IOException {
		UI ui = event.getUI();
		try {
			uploadStarted(event.getFileSize());
		} catch (ValidationRuntimeException e) {
			// V7: the exception failed the upload and went to the error handler
			event.reject(e.getMessage());
			ui.access(() -> SormasErrorHandler.get().error(new ErrorEvent(e)));
			return;
		}

		try (OutputStream outputStream = receiveUpload(ui, event.getFileName(), event.getContentType())) {
			event.getInputStream().transferTo(outputStream);
		}

		ui.access(this::uploadSucceeded);
	}

	public OutputStream receiveUpload(UI ui, String fileName, String mimeType) {
		fName = fileName;
		// Reject empty files
		if (fileName == null || fileName.isEmpty()) {
			file = null;
			ui.access(
				() -> NotificationHelper.show(
					I18nProperties.getString(Strings.headingNoFile),
					I18nProperties.getString(Strings.messageNoDocumentTemplateUploadFile),
					NotificationHelper.Type.ERROR_MESSAGE,
					false));
			// Workaround because returning null here throws an uncatchable UploadException
			return new ByteArrayOutputStream();
		}

		try {
			String newFileName = ImportExportUtils.TEMP_FILE_PREFIX + "_template_upload" + DateHelper.formatDateForExport(new Date()) + "_"
				+ DataHelper.getShortUuid(userUuid) + ".docx";
			file = new File(Paths.get(FacadeProvider.getConfigFacade().getTempFilesPath()).resolve(newFileName).toString());

			return new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			file = null;
			ui.access(
				() -> NotificationHelper.show(
					I18nProperties.getString(Strings.headingImportError),
					I18nProperties.getString(Strings.messageImportError),
					NotificationHelper.Type.ERROR_MESSAGE,
					false));
			// Workaround because returning null here throws an uncatchable UploadException
			return new ByteArrayOutputStream();
		}
	}

	public void uploadStarted(long contentLength) {
		long fileSizeLimitMb = FacadeProvider.getConfigFacade().getDocumentUploadSizeLimitMb();

		if (isFileSizeLimitExceeded(contentLength, fileSizeLimitMb)) {
			throw new ValidationRuntimeException(I18nProperties.getValidationError(Validations.fileTooBig, fileSizeLimitMb));
		}
	}

	/**
	 * Runs in the UI, after the file has been received.
	 */
	public void uploadSucceeded() {
		if (file == null) {
			return;
		}

		// Check for duplicate files
		if (FacadeProvider.getDocumentTemplateFacade().isExistingTemplateFile(documentWorkflow, diseaseSupplier.get(), fName)) {
			duplicateTemplateFileResponse(fName);
		} else {
			writeTemplateFile();
		}
	}

	protected void duplicateTemplateFileResponse(String fileName) {
		VaadinUiUtil.showConfirmationPopup(
			I18nProperties.getString(Strings.headingFileExists),
			new Span(String.format(I18nProperties.getString(Strings.infoDocumentAlreadyExists), fileName)),
			I18nProperties.getCaption(Captions.actionConfirm),
			I18nProperties.getCaption(Captions.actionCancel),
			null,
			ok -> {
				if (ok) {
					writeTemplateFile();
				}
			});
	}

	public void writeTemplateFile() {
		try {
			byte[] fileContent = Files.readAllBytes(file.toPath());
			DocumentTemplateDto documentTemplateDto = DocumentTemplateDto.build(documentWorkflow, fName, diseaseSupplier.get());
			writeDocumentTemplate(documentTemplateDto, fileContent);
			VaadinUiUtil.showSimplePopupWindow(
				I18nProperties.getString(Strings.headingUploadSuccess),
				I18nProperties.getString(Strings.messageUploadSuccessful));
		} catch (Exception e) {
			NotificationHelper.show(I18nProperties.getString(Strings.headingImportFailed), e.getMessage(), NotificationHelper.Type.ERROR_MESSAGE, true);
		}
	}

	protected void writeDocumentTemplate(DocumentTemplateDto documentTemplateDto, byte[] fileContent) throws DocumentTemplateException {
		FacadeProvider.getDocumentTemplateFacade().saveDocumentTemplate(documentTemplateDto, fileContent);
	}

	public void setDiseaseSupplier(Supplier<Disease> diseaseSupplier) {
		this.diseaseSupplier = diseaseSupplier;
	}

	public String getfName() {
		return fName;
	}

	public File getFile() {
		return file;
	}
}
