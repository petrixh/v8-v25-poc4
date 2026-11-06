package de.symeda.sormas.ui.importer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;

import de.symeda.sormas.api.FacadeProvider;
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
import de.symeda.sormas.ui.utils.NotificationHelper.Type;

@SuppressWarnings("serial")
public class ImportReceiver implements UploadHandler {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private File file;
	private String fileNameAddition;
	Consumer<File> fileConsumer;
	/**
	 * Read when the receiver is created: the upload request runs outside the UI, where the current user is not known.
	 */
	private final String userUuid;

	public ImportReceiver(String fileNameAddition, Consumer<File> fileConsumer) {
		this.fileNameAddition = fileNameAddition;
		this.fileConsumer = fileConsumer;
		this.userUuid = UiUtil.getUserUuid();
	}

	/**
	 * Was the V7 upload's {@code Receiver}, {@code StartedListener} and {@code SucceededListener}. Flow calls this in the
	 * upload request, without the session lock and outside the UI: the size check and the file are handled here, in the
	 * same order as before; notifications, errors and the import itself run in the UI ({@link UI#access}).
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
		// Reject empty files
		if (fileName == null || fileName.isEmpty()) {
			file = null;
			ui.access(
				() -> NotificationHelper.show(
					I18nProperties.getString(Strings.headingNoFile),
					I18nProperties.getString(Strings.messageNoCsvFile),
					Type.ERROR_MESSAGE,
					false));
			// Workaround because returning null here throws an uncatchable UploadException
			return new ByteArrayOutputStream();
		}
		// Reject all files except .csv files - we also need to accept excel files here
		if (!("text/csv".equals(mimeType) || "application/vnd.ms-excel".equals(mimeType))) {
			file = null;
			ui.access(
				() -> NotificationHelper.show(
					I18nProperties.getString(Strings.headingWrongFileType),
					I18nProperties.getString(Strings.messageWrongFileType),
					Type.ERROR_MESSAGE,
					false));
			// Workaround because returning null here throws an uncatchable UploadException
			return new ByteArrayOutputStream();
		}

		final FileOutputStream fos;
		try {
			Path tempDirectory = Paths.get(FacadeProvider.getConfigFacade().getTempFilesPath());
			if (!tempDirectory.toFile().exists() || !tempDirectory.toFile().canWrite()) {
				throw new FileNotFoundException("Temp directory doesn't exist or cannot be accessed");
			}
			String newFileName = ImportExportUtils.TEMP_FILE_PREFIX + fileNameAddition + DateHelper.formatDateForExport(new Date()) + "_"
				+ DataHelper.getShortUuid(userUuid) + ".csv";
			file = new File(tempDirectory.resolve(newFileName).toString());
			fos = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			file = null;
			logger.error("Reading the file to import failed", e);
			ui.access(
				() -> NotificationHelper.show(
					I18nProperties.getString(Strings.headingImportError),
					I18nProperties.getString(Strings.messageImportError),
					Type.ERROR_MESSAGE,
					false));
			// Workaround because returning null here throws an uncatchable UploadException
			return new ByteArrayOutputStream();
		}

		return fos;
	}

	public void uploadStarted(long contentLength) {
		long fileSizeLimitMb = FacadeProvider.getConfigFacade().getImportFileSizeLimitMb();
		if (contentLength > fileSizeLimitMb * 1_000_000) {
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

		try {
			// Read file and create readers
			File csvFile = new File(file.getPath());
			if (!csvFile.exists()) {
				throw new FileNotFoundException("CSV file does not exist");
			}

			fileConsumer.accept(csvFile);
		} catch (IOException e) {
			NotificationHelper.show(
				I18nProperties.getString(Strings.headingImportFailed),
				I18nProperties.getString(Strings.messageImportFailed),
				Type.ERROR_MESSAGE,
				false);
		}
	}
}
