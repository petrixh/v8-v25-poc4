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
package de.symeda.sormas.ui.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriter;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasOrderedComponents;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AttachmentType;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.AgeGroup;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseCriteria;
import de.symeda.sormas.api.caze.CaseExportType;
import de.symeda.sormas.api.clinicalcourse.ClinicalVisitDto;
import de.symeda.sormas.api.clinicalcourse.ClinicalVisitExportDto;
import de.symeda.sormas.api.contact.ContactCriteria;
import de.symeda.sormas.api.contact.ContactDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.importexport.DatabaseTable;
import de.symeda.sormas.api.importexport.ExportConfigurationDto;
import de.symeda.sormas.api.importexport.ExportTarget;
import de.symeda.sormas.api.infrastructure.PopulationDataDto;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.person.Sex;
import de.symeda.sormas.api.symptoms.SymptomsDto;
import de.symeda.sormas.api.therapy.PrescriptionDto;
import de.symeda.sormas.api.therapy.PrescriptionExportDto;
import de.symeda.sormas.api.therapy.TreatmentDto;
import de.symeda.sormas.api.therapy.TreatmentExportDto;
import de.symeda.sormas.api.utils.CSVUtils;
import de.symeda.sormas.api.utils.CsvStreamUtils;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.ExportErrorException;
import de.symeda.sormas.api.visit.VisitDto;
import de.symeda.sormas.api.visit.VisitExportType;
import de.symeda.sormas.api.visit.VisitSummaryExportDto;
import de.symeda.sormas.ui.AboutView;
import de.symeda.sormas.ui.statistics.DatabaseExportView;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;
import de.symeda.sormas.ui.utils.StreamSourceDownloadHandler.StreamSource;

public final class DownloadUtil {

	private DownloadUtil() {
		// Hide Utility Class Constructor
	}

	public static final int DETAILED_EXPORT_STEP_SIZE = 50;

	public static StreamSourceDownloadHandler createDatabaseExportStreamResource(
		DatabaseExportView databaseExportView,
		String fileName,
		String mimeType) {

		StreamSourceDownloadHandler streamResource = new StreamSourceDownloadHandler(() -> {
			Map<? extends HasValue<?, Boolean>, DatabaseTable> databaseToggles = databaseExportView.getDatabaseTableToggles();
			List<DatabaseTable> tablesToExport = new ArrayList<>();
			for (HasValue<?, Boolean> checkBox : databaseToggles.keySet()) {
				if (Boolean.TRUE.equals(checkBox.getValue())) {
					tablesToExport.add(databaseToggles.get(checkBox));
				}
			}
			return new DelayedInputStream(() -> {

				try {
					String zipPath = FacadeProvider.getExportFacade().generateDatabaseExportArchive(tablesToExport);
					return new BufferedInputStream(Files.newInputStream(new File(zipPath).toPath()));
				} catch (IOException | ExportErrorException e) {
					LoggerFactory.getLogger(DownloadUtil.class).error(e.getMessage(), e);
					// TODO This currently requires the user to click the "Export" button again or reload the page as the UI
					// is not automatically updated; this should be changed once Vaadin push is enabled (see #516)
					databaseExportView.showExportErrorNotification();
					return null;
				}

			});
		}, fileName, mimeType);
		return streamResource;
	}

	/**
	 * Was {@code createGridExportStreamResource(Indexed container, List<Column> columns, ..)} for a Vaadin 7 grid: now
	 * the Flow grid that replaced it.
	 */
	public static StreamSourceDownloadHandler createGridExportStreamResource(
		Grid<?> grid,
		ExportEntityName entityName,
		String... ignoredPropertyIds) {

		String fileName = createFileNameWithCurrentDate(entityName, ".csv");
		return new V7GridExportStreamResource(grid, fileName, ignoredPropertyIds);
	}

	public static StreamSourceDownloadHandler createFileStreamResource(
		String filePath,
		String fileName,
		String mimeType,
		String errorTitle,
		String errorText) {

		StreamSourceDownloadHandler streamResource = new StreamSourceDownloadHandler(() -> {
			try {
				return new BufferedInputStream(Files.newInputStream(new File(filePath).toPath()));
			} catch (IOException e) {
				// TODO This currently requires the user to click the "Export" button again or reload the page as the UI
				// is not automatically updated; this should be changed once Vaadin push is enabled (see #516)
				NotificationHelper.show(errorTitle, errorText, Type.ERROR_MESSAGE, false);
				return null;
			}
		}, fileName, mimeType);
		return streamResource;
	}

	public static StreamSourceDownloadHandler createStringStreamResource(String content, String fileName, String mimeType) {
		return createByteArrayStreamResource(content.getBytes(StandardCharsets.UTF_8), fileName, mimeType);
	}

	public static StreamSourceDownloadHandler createByteArrayStreamResource(byte[] content, String fileName, String mimeType) {
		// Vaadin 8 kept the default cache time here
		return new StreamSourceDownloadHandler(() -> new ByteArrayInputStream(content), fileName, mimeType);
	}

	@SuppressWarnings("serial")
	public static StreamSourceDownloadHandler createPopulationDataExportResource() {

		String exportFileName = createFileNameWithCurrentDate(ExportEntityName.POPULATION_DATA, ".csv");
		StreamSourceDownloadHandler populationDataStreamResource = new StreamSourceDownloadHandler(new StreamSource() {

			@Override
			public InputStream getStream() {
				try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
					try (CSVWriter writer = CSVUtils.createCSVWriter(
						new OutputStreamWriter(byteStream, StandardCharsets.UTF_8.name()),
						FacadeProvider.getConfigFacade().getCsvSeparator())) {
						// Generate and write columns to CSV writer
						List<String> columnNames = new ArrayList<>();
						columnNames.add(I18nProperties.getPrefixCaption(PopulationDataDto.I18N_PREFIX, PopulationDataDto.REGION));
						columnNames.add(I18nProperties.getPrefixCaption(PopulationDataDto.I18N_PREFIX, PopulationDataDto.DISTRICT));
						columnNames.add(I18nProperties.getPrefixCaption(PopulationDataDto.I18N_PREFIX, PopulationDataDto.COMMUNITY));
						columnNames.add(I18nProperties.getString(Strings.total));
						columnNames.add(I18nProperties.getCaption(Captions.populationDataMaleTotal));
						columnNames.add(I18nProperties.getCaption(Captions.populationDataFemaleTotal));

						Map<AgeGroup, Integer> ageGroupPositions = new HashMap<>();
						int ageGroupIndex = 6;
						for (AgeGroup ageGroup : AgeGroup.values()) {
							columnNames.add(DataHelper.getSexAndAgeGroupString(ageGroup, null));
							columnNames.add(DataHelper.getSexAndAgeGroupString(ageGroup, Sex.MALE));
							columnNames.add(DataHelper.getSexAndAgeGroupString(ageGroup, Sex.FEMALE));
							columnNames.add(DataHelper.getSexAndAgeGroupString(ageGroup, Sex.OTHER));
							ageGroupPositions.put(ageGroup, ageGroupIndex);
							ageGroupIndex += 4;
						}

						writer.writeNext(columnNames.toArray(new String[columnNames.size()]));

						List<Object[]> populationExportDataList = FacadeProvider.getPopulationDataFacade().getPopulationDataForExport();

						String[] exportLine = new String[columnNames.size()];
						String regionName = "";
						String districtName = "";
						String communityName = "";
						for (Object[] populationExportData : populationExportDataList) {
							String dataRegionName = (String) populationExportData[0];
							String dataDistrictName = populationExportData[1] == null ? "" : (String) populationExportData[1];
							String dataCommunityName = populationExportData[2] == null ? "" : (String) populationExportData[2];
							if (exportLine[0] != null
								&& (!dataRegionName.equals(regionName)
									|| !dataDistrictName.equals(districtName)
									|| !dataCommunityName.equals(communityName))) {
								// New region or district reached; write line to CSV
								writer.writeNext(exportLine);
								exportLine = new String[columnNames.size()];
							}
							regionName = dataRegionName;
							districtName = dataDistrictName;
							communityName = dataCommunityName;

							// Region
							if (exportLine[0] == null) {
								exportLine[0] = (String) populationExportData[0];
							}
							// District
							if (exportLine[1] == null) {
								exportLine[1] = (String) populationExportData[1];
							}
							// Community
							if (exportLine[2] == null) {
								exportLine[2] = (String) populationExportData[2];
							}

							if (populationExportData[3] == null) {
								// Total population
								String sexString = (String) populationExportData[4];
								if (Sex.MALE.getName().equals(sexString)) {
									exportLine[4] = String.valueOf((int) populationExportData[5]);
								} else if (Sex.FEMALE.getName().equals(sexString)) {
									exportLine[5] = String.valueOf((int) populationExportData[5]);
								} else if (Sex.OTHER.getName().equals(sexString)) {
									exportLine[6] = String.valueOf((int) populationExportData[5]);
								} else {
									exportLine[3] = String.valueOf((int) populationExportData[5]);
								}
							} else {
								// Population based on age group position and sex
								Integer ageGroupPosition = ageGroupPositions.get(AgeGroup.valueOf((String) populationExportData[3]));
								String sexString = (String) populationExportData[4];
								if (Sex.MALE.getName().equals(sexString)) {
									ageGroupPosition += 1;
								} else if (Sex.FEMALE.getName().equals(sexString)) {
									ageGroupPosition += 2;
								} else if (Sex.OTHER.getName().equals(sexString)) {
									ageGroupPosition += 3;
								}
								exportLine[ageGroupPosition] = String.valueOf((int) populationExportData[5]);
							}
						}

						// Write last line to CSV
						writer.writeNext(exportLine);
						writer.flush();
					}
					return new ByteArrayInputStream(byteStream.toByteArray());
				} catch (IOException e) {
					// TODO This currently requires the user to click the "Export" button again or reload the page as the UI
					// is not automatically updated; this should be changed once Vaadin push is enabled (see #516)
					NotificationHelper.show(
						I18nProperties.getString(Strings.headingExportFailed),
						I18nProperties.getString(Strings.messageExportFailed),
						Type.ERROR_MESSAGE,
						false);
					return null;
				}
			}
		}, exportFileName, "text/csv");
		return populationDataStreamResource;
	}

	public static StreamSourceDownloadHandler createCaseManagementExportResource(
		CaseCriteria criteria,
		Supplier<Collection<String>> selectedRows,
		ExportEntityName entityName) {
		String exportFileName = createFileNameWithCurrentDate(entityName, ".zip");
		StreamSourceDownloadHandler casesResource = CaseDownloadUtil.createCaseExportResource(criteria, selectedRows, CaseExportType.CASE_MANAGEMENT, null);

		StreamSourceDownloadHandler prescriptionsResource = createCsvExportStreamResource(
			PrescriptionExportDto.class,
			null,
			(Integer start, Integer max) -> FacadeProvider.getPrescriptionFacade().getExportList(criteria, selectedRows.get(), start, max),
			(propertyId, type) -> {
				String caption = I18nProperties.getPrefixCaption(
					PrescriptionExportDto.I18N_PREFIX,
					propertyId,
					I18nProperties.getPrefixCaption(PrescriptionDto.I18N_PREFIX, propertyId));
				if (Date.class.isAssignableFrom(type)) {
					caption += " (" + DateFormatHelper.getDateFormatPattern() + ")";
				}
				return caption;
			},
			ExportEntityName.PRESCRIPTIONS,
			null);

		StreamSourceDownloadHandler treatmentsResource = createCsvExportStreamResource(
			TreatmentExportDto.class,
			null,
			(Integer start, Integer max) -> FacadeProvider.getTreatmentFacade().getExportList(criteria, selectedRows.get(), start, max),
			(propertyId, type) -> {
				String caption = I18nProperties.getPrefixCaption(
					TreatmentExportDto.I18N_PREFIX,
					propertyId,
					I18nProperties.getPrefixCaption(TreatmentDto.I18N_PREFIX, propertyId));
				if (Date.class.isAssignableFrom(type)) {
					caption += " (" + DateFormatHelper.getDateFormatPattern() + ")";
				}
				return caption;
			},
			ExportEntityName.PRESCRIPTIONS,
			null);

		StreamSourceDownloadHandler clinicalVisitsResource = createCsvExportStreamResource(
			ClinicalVisitExportDto.class,
			null,
			(Integer start, Integer max) -> FacadeProvider.getClinicalVisitFacade().getExportList(criteria, selectedRows.get(), start, max),
			(propertyId, type) -> {
				String caption = I18nProperties.getPrefixCaption(
					ClinicalVisitExportDto.I18N_PREFIX,
					propertyId,
					I18nProperties.getPrefixCaption(
						ClinicalVisitDto.I18N_PREFIX,
						propertyId,
						I18nProperties.getPrefixCaption(SymptomsDto.I18N_PREFIX, propertyId)));
				if (Date.class.isAssignableFrom(type)) {
					caption += " (" + DateFormatHelper.getDateFormatPattern() + ")";
				}
				return caption;
			},
			ExportEntityName.CLINICAL_VISITS,
			null);

		StreamSourceDownloadHandler caseManagementStreamResource = new StreamSourceDownloadHandler(() -> {
			String zipFile = FacadeProvider.getExportFacade()
				.generateZipArchive(DateHelper.formatDateForExport(new Date()), new Random().nextInt(Integer.MAX_VALUE));
			try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
				writeCsvToZip(zos, casesResource.getStreamSource(), "cases.csv");
				writeCsvToZip(zos, prescriptionsResource.getStreamSource(), "prescriptions.csv");
				writeCsvToZip(zos, treatmentsResource.getStreamSource(), "treatments.csv");
				writeCsvToZip(zos, clinicalVisitsResource.getStreamSource(), "clinical_assessments.csv");
				zos.close();
				return new BufferedInputStream(new FileInputStream(new File(zipFile)));
			} catch (IOException e) {
				LoggerFactory.getLogger(DownloadUtil.class).error("Failed to generate a zip file for case management export.");
				return null;
			}
		}, exportFileName, "application/zip");
		return caseManagementStreamResource;
	}

	private static void writeCsvToZip(ZipOutputStream zos, StreamSource source, String fileName) throws IOException {
		zos.putNextEntry(new ZipEntry(fileName));
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length;
		InputStream input = source.getStream();
		while ((length = input.read(buffer)) != -1) {
			result.write(buffer, 0, length);
		}
		result.writeTo(zos);
		zos.closeEntry();
	}

	public static interface OutputStreamConsumer {

		void writeTo(OutputStream os) throws IOException;
	}

	/**
	 * The buffer can be used for an input stream without having to copy it
	 */
	private static class SharedByteArrayOutputStream extends ByteArrayOutputStream {

		public SharedByteArrayOutputStream() {
			super(2048);
		}

		public ByteArrayInputStream toInputStream() {
			return new ByteArrayInputStream(buf, 0, count);
		}
	}

	public static class DelayedInputStream extends FilterInputStream {

		private Supplier<InputStream> lazyInputStreamSupplier;

		protected DelayedInputStream(Supplier<InputStream> lazyInputStreamSupplier) {
			super(null);
			this.lazyInputStreamSupplier = lazyInputStreamSupplier;
		}

		public DelayedInputStream(OutputStreamConsumer osConsumer, Consumer<IOException> exceptionHandler) {
			this(() -> {
				try (SharedByteArrayOutputStream os = new SharedByteArrayOutputStream()) {
					osConsumer.writeTo(os);
					return os.toInputStream();
				} catch (IOException e) {
					exceptionHandler.accept(e);
					throw new UncheckedIOException(e);
				}
			});
		}

		private void ensureInited() {
			if (lazyInputStreamSupplier != null) {
				in = lazyInputStreamSupplier.get();
				lazyInputStreamSupplier = null;
			}
		}

		@Override
		public int read() throws IOException {
			ensureInited();
			return super.read();
		}

		@Override
		public int read(byte[] b) throws IOException {
			ensureInited();
			return super.read(b);
		}

		@Override
		public synchronized int read(byte[] b, int off, int len) throws IOException {
			ensureInited();
			return super.read(b, off, len);
		}

		@Override
		public synchronized long skip(long n) throws IOException {
			ensureInited();
			return super.skip(n);
		}

		@Override
		public synchronized int available() throws IOException {
			ensureInited();
			return super.available();
		}

		@Override
		public synchronized void mark(int readAheadLimit) {
			ensureInited();
			super.mark(readAheadLimit);
		}
	}

	public static StreamSourceDownloadHandler createVisitsExportStreamResource(
		ContactCriteria contactCriteria,
		Supplier<Set<String>> selectedRows,
		ExportEntityName entityName) {
		String exportFileName = createFileNameWithCurrentDate(entityName, ".csv");
		StreamSourceDownloadHandler extendedStreamResource = new StreamSourceDownloadHandler(() -> new DelayedInputStream((out) -> {
			try (CSVWriter writer = CSVUtils
				.createCSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8.name()), FacadeProvider.getConfigFacade().getCsvSeparator())) {

				final List<String> columnNames = new ArrayList<>();
				final List<String> dayColumns = new ArrayList<>();
				columnNames.add(I18nProperties.getPrefixCaption(ContactDto.I18N_PREFIX, ContactDto.UUID));
				columnNames.add(I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, PersonDto.FIRST_NAME));
				columnNames.add(I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, PersonDto.LAST_NAME));
				dayColumns.add("");
				dayColumns.add("");
				dayColumns.add("");

				final long maximumFollowUps = FacadeProvider.getContactFacade().countMaximumFollowUpDays(contactCriteria);

				for (int index = 0; index < maximumFollowUps; index++) {
					columnNames.add(I18nProperties.getPrefixCaption(VisitDto.I18N_PREFIX, VisitDto.VISIT_DATE_TIME));
					columnNames.add(I18nProperties.getPrefixCaption(VisitDto.I18N_PREFIX, VisitDto.VISIT_STATUS));
					columnNames.add(I18nProperties.getPrefixCaption(VisitDto.I18N_PREFIX, VisitDto.SYMPTOMS));
					final String dayString = I18nProperties.getCaption(Captions.contactFollowUpDay) + " " + (index + 1);

					dayColumns.add(dayString);
					dayColumns.add(dayString);
					dayColumns.add(dayString);
				}

				writer.writeNext(columnNames.toArray(new String[columnNames.size()]));
				writer.writeNext(dayColumns.toArray(new String[columnNames.size()]));

				int startIndex = 0;
				List<VisitSummaryExportDto> exportRows = FacadeProvider.getContactFacade()
					.getVisitSummaryExportList(contactCriteria, selectedRows.get(), 0, DETAILED_EXPORT_STEP_SIZE, I18nProperties.getUserLanguage());
				while (!exportRows.isEmpty()) {

					for (VisitSummaryExportDto exportRow : exportRows) {
						final List<String> values = new ArrayList<>();
						values.add(exportRow.getUuid());
						values.add(exportRow.getFirstName());
						values.add(exportRow.getLastName());
						exportRow.getVisitDetails().forEach(contactVisitsDetailsExportDto -> {
							values.add(DateFormatHelper.formatDate(contactVisitsDetailsExportDto.getVisitDateTime()));
							values.add(contactVisitsDetailsExportDto.getVisitStatus().toString());
							values.add(contactVisitsDetailsExportDto.getSymptoms());
						});

						writer.writeNext(values.toArray(new String[columnNames.size()]));
					}

					writer.flush();
					startIndex += DETAILED_EXPORT_STEP_SIZE;
					exportRows = FacadeProvider.getContactFacade()
						.getVisitSummaryExportList(
							contactCriteria,
							selectedRows.get(),
							startIndex,
							DETAILED_EXPORT_STEP_SIZE,
							I18nProperties.getUserLanguage());
				}
			}
		},
			e -> {
				// TODO This currently requires the user to click the "Export" button again or reload the page
				//  as the UI
				// is not automatically updated; this should be changed once Vaadin push is enabled (see #516)
				NotificationHelper.showAsync(
					UI.getCurrent(),
					I18nProperties.getString(Strings.headingExportFailed),
					I18nProperties.getString(Strings.messageExportFailed),
					Type.ERROR_MESSAGE);
			}), exportFileName, "text/csv");
		return extendedStreamResource;
	}

	public static <T> StreamSourceDownloadHandler createCsvExportStreamResource(
		Class<T> exportRowClass,
		Enum<?> exportType,
		CsvStreamUtils.SupplierBiFunction<Integer, Integer, List<T>> exportRowsSupplier,
		CsvStreamUtils.SupplierBiFunction<String, Class<?>, String> propertyIdCaptionFunction,
		ExportEntityName entityName,
		ExportConfigurationDto exportConfiguration) {

		String exportFileName = createFileNameWithCurrentDate(entityName, ".csv");
		StreamSourceDownloadHandler extendedStreamResource = new StreamSourceDownloadHandler(() -> new DelayedInputStream((out) -> {
			try {
				CsvStreamUtils.writeCsvContentToStream(
					exportRowClass,
					exportRowsSupplier,
					propertyIdCaptionFunction,
					exportConfiguration,
					(o) -> exportType == null || hasExportTarget(exportType, (Method) o),
					FacadeProvider.getConfigFacade(),
					out);
			} catch (Exception e) {
				LoggerFactory.getLogger(DownloadUtil.class).error(e.getMessage(), e);

				throw e;
			}

		},
			e -> {
				// TODO This currently requires the user to click the "Export" button again or reload the page
				//  as the UI
				// is not automatically updated; this should be changed once Vaadin push is enabled (see #516)
				NotificationHelper.showAsync(
					UI.getCurrent(),
					I18nProperties.getString(Strings.headingExportFailed),
					I18nProperties.getString(Strings.messageExportFailed),
					Type.ERROR_MESSAGE);
			}), exportFileName, "text/csv");
		return extendedStreamResource;
	}

	@SuppressWarnings("rawtypes")
	private static boolean hasExportTarget(Enum<?> exportType, Method m) {

		if (m.isAnnotationPresent(ExportTarget.class)) {
			final Class<? extends Enum> exportTypeClass = exportType.getClass();
			final ExportTarget exportTarget = m.getAnnotation(ExportTarget.class);
			Supplier<Enum[]> exportTypeSupplier = null;
			if (exportTypeClass.isAssignableFrom(CaseExportType.class)) {
				exportTypeSupplier = exportTarget::caseExportTypes;
			}
			if (exportTypeClass.isAssignableFrom(VisitExportType.class)) {
				exportTypeSupplier = exportTarget::visitExportTypes;

			}
			return exportTypeSupplier != null && containsExportType(exportType, exportTypeSupplier);
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	private static boolean containsExportType(Enum<?> exportType, Supplier<Enum[]> supplier) {
		return Arrays.asList(supplier.get()).contains(exportType);
	}

	/**
	 * <p>
	 * When downloading through a download link (was a FileDownloader),
	 * the link must remain attached and not behind a modal dialog (inert).
	 * Otherwise the download request is rejected and the download may fail.
	 * </p>
	 * <p>
	 * This method display a modal dialog that includes the exportComponent without actually showing it to the user.
	 * When the dialog is closed, it up to the closeListener to decide the fate of the exportComponent.
	 * </p>
	 *
	 * @param exportComponent
	 * @param closeListener
	 *            called when the dialog closes (was a Window.CloseListener)
	 */
	public static void showExportWaitDialog(Component exportComponent, ComponentEventListener<Dialog.OpenedChangeEvent> closeListener) {

		// was the dialog caption: the caption of the export button
		String caption = exportComponent.getElement().getTextRecursively();

		//the button has to remain in the UI for the download to succeed, but it should not be seen 
		Div hidingLayout = new Div();
		hidingLayout.getStyle().set("display", "none");
		hidingLayout.add(exportComponent);

		Span lbl = VaadinUiUtil.createLabel(I18nProperties.getString(Strings.infoDownloadExport), ContentMode.HTML);
		HorizontalLayout layout = new HorizontalLayout(lbl, hidingLayout);
		layout.setPadding(true);
		layout.setFlexGrow(1, lbl);
		Dialog dialog = VaadinUiUtil.showPopupWindow(layout);
		dialog.setHeaderTitle(caption);

		VaadinUiUtil.addCloseListener(dialog, closeListener);
	}

	/**
	 * Was {@code Page.getCurrent().open(resource, ...)} for a download: the browser downloads the resource and the page
	 * stays in use, as in Vaadin 8. {@code Page.open(url, "_self")} would stop the Flow client (Flow stops the
	 * application before a "_self" navigation), so the UI would no longer answer after the download; a download link
	 * that is clicked once does not navigate. The resource is owned by {@code owner}: Flow refuses (HTTP 403) a request
	 * for a resource whose owner is inert, as everything outside an open modal window is (the UI too).
	 */
	public static void startDownload(DownloadHandler downloadHandler, Component owner) {
		StreamRegistration registration = VaadinSession.getCurrent().getResourceRegistry().registerResource(downloadHandler, owner.getElement());
		UI.getCurrent()
			.getPage()
			.executeJs(
				"const a = document.createElement('a'); a.href = $0; a.download = ''; a.style.display = 'none';"
					+ " document.body.appendChild(a); a.click(); a.remove();",
				registration.getResourceUri().toString());
	}

	/**
	 * Was {@code new FileDownloader(resource).extend(target)} for a target that is not in a layout yet: returns a
	 * download link around {@code target}; add the link where the target would have gone.
	 */
	public static Anchor createDownloadLink(Component target, DownloadHandler downloadHandler) {
		Anchor link = new Anchor();
		link.setHref(downloadHandler, AttachmentType.DOWNLOAD);
		link.add(target);
		return link;
	}

	/**
	 * Was {@code new FileDownloader(resource).extend(target)} for a target that is already in a layout: puts a download
	 * link around {@code target} at its place. If the target has no parent yet, the link is returned for the caller to
	 * add (see {@link #createDownloadLink}).
	 */
	public static Anchor attachDownloader(Component target, DownloadHandler downloadHandler) {
		Component parent = target.getParent().orElse(null);
		int index = -1;
		if (parent instanceof HasOrderedComponents) {
			index = ((HasOrderedComponents) parent).indexOf(target);
		}
		Anchor link = createDownloadLink(target, downloadHandler);
		if (parent instanceof HasOrderedComponents && index >= 0) {
			((HasOrderedComponents) parent).addComponentAtIndex(index, link);
		} else if (parent instanceof HasComponents) {
			((HasComponents) parent).add(link);
		}
		return link;
	}

	public static String createFileNameWithCurrentDate(ExportEntityName entityName, String fileExtension) {
		String instanceName = FacadeProvider.getConfigFacade().getSormasInstanceName().toLowerCase();
		String processedInstanceName = DataHelper.cleanStringForFileName(instanceName);
		String processedEntityName = DataHelper.cleanStringForFileName(entityName.getLocalizedNameInSystemLanguage());
		String exportDate = DateHelper.formatDateForExport(new Date());
		return String.join("_", processedInstanceName, processedEntityName, exportDate, fileExtension);
	}

	public static Anchor attachDataDictionaryDownloader(Component target) {
		return attachDownloader(target, new StreamSourceDownloadHandler(() -> new DownloadUtil.DelayedInputStream((out) -> {
			try {
				String documentPath = FacadeProvider.getInfoFacade().generateDataDictionary();
				IOUtils.copy(Files.newInputStream(new File(documentPath).toPath()), out);
			} catch (IOException e) {
				LoggerFactory.getLogger(AboutView.class).error("Failed to generate data dictionary", e);

				// fall back to pre-generated document
				InputStream preGeneratedDocumentStream = DownloadUtil.class.getResourceAsStream("/doc/SORMAS_Data_Dictionary.xlsx");
				IOUtils.copy(preGeneratedDocumentStream, out);
			}
		}, (e) -> {
		}), createFileNameWithCurrentDate(ExportEntityName.DATA_DICTIONARY, ".xlsx")));
	}
}
