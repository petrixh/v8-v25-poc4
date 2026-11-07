package de.symeda.sormas.ui.caze.importer;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import com.opencsv.exceptions.CsvValidationException;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.importexport.ImportFacade;
import de.symeda.sormas.api.importexport.ValueSeparator;
import de.symeda.sormas.ui.importer.AbstractImportLayout;
import de.symeda.sormas.ui.importer.ImportReceiver;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;

public class LineListingImportLayout extends AbstractImportLayout {

	private static final long serialVersionUID = 8979253668010454656L;

	public LineListingImportLayout() {

		super();

		ImportFacade importFacade = FacadeProvider.getImportFacade();

		addDownloadResourcesComponent(1, DownloadHandler.forClassResource(LineListingImportLayout.class, "/SORMAS_Line_Listing_Import_Guide.pdf"));
		addDownloadImportTemplateComponent(
			2,
			importFacade.getCaseLineListingImportTemplateFilePath(),
			importFacade.getCaseLineListingImportTemplateFileName());
		addImportCsvComponent(3, new ImportReceiver("_line_listing_import_", file -> {
			resetDownloadErrorReportButton();

			try {
				CaseImporter importer = new CaseImporter(file, false, currentUser, (ValueSeparator) separator.getValue());
				importer.startImport(this::extendDownloadErrorReportButton, currentUI, true);
			} catch (IOException | CsvValidationException e) {
				NotificationHelper.show(
					I18nProperties.getString(Strings.headingImportFailed),
					I18nProperties.getString(Strings.messageImportFailed),
					Type.ERROR_MESSAGE,
					false);
			}
		}));
		addDownloadErrorReportComponent(4);
	}
}
