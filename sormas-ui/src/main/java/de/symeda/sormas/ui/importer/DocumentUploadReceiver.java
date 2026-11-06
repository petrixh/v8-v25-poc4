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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.importexport.ImportExportUtils;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.utils.NotificationHelper;

/**
 * The temporary file of one uploaded document (was the MultiFileUpload add-on's {@code UploadReceiver}), used by
 * {@link DocumentMultiFileUpload}.
 */
public class DocumentUploadReceiver implements Serializable {

	private static final long serialVersionUID = 2971535728102027968L;

	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentUploadReceiver.class);

	private final String userUuid;
	private File file;

	/**
	 * @param userUuid
	 *            the uploading user (the upload request runs outside the UI, where the current user is not known)
	 */
	public DocumentUploadReceiver(String userUuid) {
		this.userUuid = userUuid;
	}

	/**
	 * @param ui
	 *            for the error notification
	 */
	public OutputStream receiveUpload(UI ui) {
		try {
			// Flow can upload several files at the same time (the add-on did one after the other): a unique suffix keeps
			// their temporary files apart
			String newFileName = ImportExportUtils.TEMP_FILE_PREFIX + "_document_upload" + DateHelper.formatDateForExport(new Date()) + "_"
				+ DataHelper.getShortUuid(userUuid) + "_" + DataHelper.createUuid();
			file = Paths.get(FacadeProvider.getConfigFacade().getTempFilesPath()).resolve(newFileName).toFile();
			return new BufferedOutputStream(Files.newOutputStream(file.toPath()));

		} catch (IOException e) {
			deleteTempFile();
			LOGGER.error(e.getMessage(), e);
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

	public void deleteTempFile() {
		if (file != null && file.exists()) {
			file.delete();
			file = null;
		}
	}

	/**
	 * @return whether the upload was written to a temporary file that still exists (when it could not be created,
	 *         {@link #receiveUpload(UI)} wrote to memory and showed an error)
	 */
	public boolean hasFile() {
		return file != null && file.exists();
	}

	public InputStream getStream() {
		try {
			if (file != null) {
				return new BufferedInputStream(Files.newInputStream(file.toPath()));
			}
			return null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
