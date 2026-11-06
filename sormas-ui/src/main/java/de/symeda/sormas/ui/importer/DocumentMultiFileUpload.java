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

package de.symeda.sormas.ui.importer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.streams.UploadEvent;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.ui.SormasErrorHandler;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.NotificationHelper;

/**
 * Document upload for one or several files. Was a subclass of the MultiFileUpload add-on ({@code migration/addons.md}
 * item 3), now a Flow {@link Upload} with the same constructor handlers and the add-on methods SORMAS called.
 * <p>
 * As with the add-on: files above the document size limit (and empty files) are rejected with a warning, the started
 * handler runs when a file starts uploading, the finished handler gets each file with the number of files still left,
 * and the all-finished handler runs at the end. Each file is received into a temporary file
 * ({@link DocumentUploadReceiver}) during its upload request, which runs outside the UI; the finished handler runs in
 * the UI once the browser reports that all files are uploaded, one file after the other, so that the "files left" count
 * is exact although Flow can upload several files at the same time. The add-on's upload state window and the UI polling
 * it needed are gone: Flow shows the progress per file in the upload itself.
 */
public class DocumentMultiFileUpload extends Upload {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentMultiFileUpload.class);

	private static final String INTERRUPTED_MSG = "All uploads have been interrupted.";

	/**
	 * Was the add-on's {@code UploadStartedHandler}.
	 */
	@FunctionalInterface
	public interface UploadStartedHandler extends Serializable {

		void handleUploadStarted();
	}

	/**
	 * Was the add-on's {@code UploadFinishedHandler}.
	 */
	@FunctionalInterface
	public interface UploadFinishedHandler extends Serializable {

		void handleFile(InputStream stream, String fileName, String mimeType, long length, int filesLeftInQueue);
	}

	/**
	 * Was the add-on's {@code AllUploadFinishedHandler}.
	 */
	@FunctionalInterface
	public interface AllUploadFinishedHandler extends Serializable {

		void finished();
	}

	private static final class ReceivedFile implements Serializable {

		private static final long serialVersionUID = 1L;

		private final DocumentUploadReceiver receiver;
		private final String fileName;
		private final String mimeType;
		private final long length;

		private ReceivedFile(DocumentUploadReceiver receiver, String fileName, String mimeType, long length) {
			this.receiver = receiver;
			this.fileName = fileName;
			this.mimeType = mimeType;
			this.length = length;
		}
	}

	private final UploadFinishedHandler uploadFinishedHandler;
	private final boolean multiple;
	private final long maxFileSize;
	private final String sizeErrorMsg;
	/**
	 * Files received by the upload requests and not yet given to the finished handler (filled outside the UI).
	 */
	private final List<ReceivedFile> receivedFiles = new ArrayList<>();
	private AllUploadFinishedHandler allUploadFinishedHandler;

	public DocumentMultiFileUpload(UploadStartedHandler uploadStartedHandler, UploadFinishedHandler uploadFinishedHandler, boolean multiple) {
		this.uploadFinishedHandler = uploadFinishedHandler;
		this.multiple = multiple;

		long fileSizeLimitMb = FacadeProvider.getConfigFacade().getDocumentUploadSizeLimitMb();
		maxFileSize = fileSizeLimitMb * 1_000_000;
		sizeErrorMsg = I18nProperties.getValidationError(Validations.fileTooBig, fileSizeLimitMb);

		String userUuid = UiUtil.getUserUuid();
		setUploadHandler(event -> receiveFile(event, userUuid));

		if (!multiple) {
			setMaxFiles(1);
		}
		// the add-on had no drop zone (SORMAS never wrapped a drop component)
		setDropAllowed(false);
		// browser-side size check, as the add-on's client did
		setMaxFileSize((int) Math.min(maxFileSize, Integer.MAX_VALUE));
		addFileRejectedListener(e -> NotificationHelper.show(sizeErrorMsg, NotificationHelper.Type.WARNING_MESSAGE));

		if (uploadStartedHandler != null) {
			getElement().addEventListener("upload-start", e -> uploadStartedHandler.handleUploadStarted());
		}
		addAllFinishedListener(e -> handleReceivedFiles());

		// was MultiFileUpload.detach(): uploads still running are interrupted
		addDetachListener(e -> {
			if (isUploading()) {
				interruptUpload();
				NotificationHelper.show(INTERRUPTED_MSG, NotificationHelper.Type.WARNING_MESSAGE);
			}
			deleteReceivedFiles();
		});
	}

	public void setUploadButtonCaptions(String singleUploadCaption, String multiUploadCaption) {
		setUploadButton(new Button(multiple ? multiUploadCaption : singleUploadCaption));
	}

	public void setAllUploadFinishedHandler(AllUploadFinishedHandler allUploadFinishedHandler) {
		this.allUploadFinishedHandler = allUploadFinishedHandler;
	}

	public AllUploadFinishedHandler getAllUploadFinishedHandler() {
		return allUploadFinishedHandler;
	}

	/**
	 * Runs in the upload request, outside the UI.
	 */
	private void receiveFile(UploadEvent event, String userUuid) throws IOException {
		UI ui = event.getUI();
		long length = event.getFileSize();
		// the client side file size check may not work in old browsers (add-on comment)
		if (length > maxFileSize || length <= 0) {
			event.reject(sizeErrorMsg);
			ui.access(() -> NotificationHelper.show(sizeErrorMsg, NotificationHelper.Type.WARNING_MESSAGE));
			return;
		}

		DocumentUploadReceiver receiver = new DocumentUploadReceiver(userUuid);
		try (OutputStream outputStream = receiver.receiveUpload(ui)) {
			event.getInputStream().transferTo(outputStream);
		} catch (IOException e) {
			// was streamingFailed
			receiver.deleteTempFile();
			throw e;
		}

		synchronized (receivedFiles) {
			receivedFiles.add(new ReceivedFile(receiver, event.getFileName(), event.getContentType(), length));
		}
	}

	/**
	 * Runs in the UI when the browser reports that no file is uploading any more.
	 */
	private void handleReceivedFiles() {
		List<ReceivedFile> files;
		synchronized (receivedFiles) {
			files = new ArrayList<>(receivedFiles);
			receivedFiles.clear();
		}

		for (int i = 0; i < files.size(); i++) {
			ReceivedFile file = files.get(i);
			try (InputStream stream = file.receiver.getStream()) {
				if (stream != null) {
					uploadFinishedHandler.handleFile(stream, file.fileName, file.mimeType, file.length, files.size() - i - 1);
				}
			} catch (IOException e) {
				LOGGER.error("Could not close stream!", e);
			} catch (RuntimeException e) {
				// with the add-on each file had its own request, so an error in one file did not stop the others
				SormasErrorHandler.get().error(new ErrorEvent(e));
			} finally {
				file.receiver.deleteTempFile();
			}
		}

		// the uploaded files stay in Flow's list (and one allowed file would block the next upload) until cleared
		clearFileList();

		if (allUploadFinishedHandler != null) {
			allUploadFinishedHandler.finished();
		}
	}

	private void deleteReceivedFiles() {
		synchronized (receivedFiles) {
			receivedFiles.forEach(file -> file.receiver.deleteTempFile());
			receivedFiles.clear();
		}
	}
}
