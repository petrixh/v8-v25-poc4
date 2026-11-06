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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.opencsv.CSVWriter;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.data.binder.BeanPropertySet;
import com.vaadin.flow.data.binder.PropertyDefinition;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.function.ValueProvider;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.ReferenceDto;
import de.symeda.sormas.api.caze.AgeAndBirthDateDto;
import de.symeda.sormas.api.caze.BirthDateDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.person.PersonHelper;
import de.symeda.sormas.api.utils.CSVCommentLineValidator;
import de.symeda.sormas.api.utils.CSVUtils;
import de.symeda.sormas.api.utils.YesNoUnknown;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;

@SuppressWarnings("serial")
public class GridExportStreamResource {

	private static final String EXPORT_VALUE_PROVIDER = GridExportStreamResource.class.getName() + ".valueProvider";

	/**
	 * Vaadin 8 read each column's value provider for the export; Flow columns do not expose it. Grids register it here
	 * for columns that are not a property of the grid's bean type (those are read through the bean property).
	 */
	public static <T> Column<T> setExportValueProvider(Column<T> column, ValueProvider<T, ?> valueProvider) {
		ComponentUtil.setData(column, EXPORT_VALUE_PROVIDER, valueProvider);
		return column;
	}

	/**
	 * @return the registered value provider of the column, else the getter of the bean property named like the column
	 *         key, else a provider of {@code null}
	 */
	@SuppressWarnings({
		"unchecked",
		"rawtypes" })
	public static ValueProvider getExportValueProvider(Grid<?> grid, Column<?> column) {
		ValueProvider valueProvider = (ValueProvider) ComponentUtil.getData(column, EXPORT_VALUE_PROVIDER);
		if (valueProvider != null) {
			return valueProvider;
		}
		if (grid.getBeanType() != null && column.getKey() != null) {
			try {
				java.util.Optional<PropertyDefinition<Object, ?>> property =
					((com.vaadin.flow.data.binder.PropertySet<Object>) BeanPropertySet.get((Class<Object>) grid.getBeanType()))
						.getProperty(column.getKey());
				if (property.isPresent()) {
					return property.get().getGetter();
				}
			} catch (IllegalArgumentException e) {
				// not a bean property path
			}
		}
		return row -> null;
	}

	public static StreamSourceDownloadHandler createStreamResource(Grid<?> grid, ExportEntityName entityName, String... excludePropertyIds) {
		return new GridExportStreamResource(grid, null, entityName, Arrays.asList(excludePropertyIds), Collections.emptyList()).getStreamResource();
	}

	public static StreamSourceDownloadHandler createStreamResourceWithSelectedItems(
		Grid<?> grid,
		Supplier<Set<?>> getSelectedRows,
		ExportEntityName entityName,
		String... excludePropertyIds) {
		return new GridExportStreamResource(grid, getSelectedRows, entityName, Arrays.asList(excludePropertyIds), Collections.emptyList())
			.getStreamResource();
	}

	public static StreamSourceDownloadHandler createStreamResourceWithSelectedItems(
		Grid<?> grid,
		Supplier<Set<?>> getSelectedRows,
		ExportEntityName entityName,
		List<String> excludePropertyIds,
		List<String> includePropertyIds) {
		return new GridExportStreamResource(grid, getSelectedRows, entityName, excludePropertyIds, includePropertyIds).getStreamResource();
	}

	public static StreamSourceDownloadHandler createStreamResource(
		Grid<?> grid,
		ExportEntityName entityName,
		List<String> excludePropertyIds,
		List<String> includePropertyIds) {
		return new GridExportStreamResource(grid, null, entityName, excludePropertyIds, includePropertyIds).getStreamResource();
	}

	private final StreamSourceDownloadHandler streamResource;

	private GridExportStreamResource(
		Grid<?> grid,
		Supplier<Set<?>> getSelectedRows,
		ExportEntityName entityName,
		List<String> excludePropertyIds,
		List<String> includePropertyIds) {
		String filename = DownloadUtil.createFileNameWithCurrentDate(entityName, ".csv");
		GridExportStreamSource streamSource = new GridExportStreamSource(grid, getSelectedRows, excludePropertyIds, includePropertyIds);
		this.streamResource = new StreamSourceDownloadHandler(streamSource, filename, "text/csv");
	}

	private StreamSourceDownloadHandler getStreamResource() {
		return this.streamResource;
	}

	private static class GridExportStreamSource implements StreamSourceDownloadHandler.StreamSource {

		private final Grid<?> grid;
		private final List<String> excludePropertyIds;
		private final List<String> includePropertyIds;
		private final Supplier<Set<?>> getSelectedRows;

		GridExportStreamSource(Grid<?> grid, Supplier<Set<?>> getSelectedRows, List<String> excludePropertyIds, List<String> includePropertyIds) {
			this.grid = grid;
			this.excludePropertyIds = excludePropertyIds;
			this.includePropertyIds = includePropertyIds;
			this.getSelectedRows = getSelectedRows;
		}

		@Override
		public InputStream getStream() {
			List<Column<?>> columns = getGridColumns();
			ValueProvider[] columnValueProviders =
				columns.stream().map(column -> getExportValueProvider(grid, column)).toArray(ValueProvider[]::new);
			String[] headerRow = columns.stream().map(Column::getKey).toArray(String[]::new);
			String[] labelsRow = columns.stream().map(Column::getHeaderText).toArray(String[]::new);
			labelsRow[0] = CSVCommentLineValidator.DEFAULT_COMMENT_LINE_PREFIX + labelsRow[0];
			DataProvider<?, ?> dataProvider = grid.getDataProvider();
			List<QuerySortOrder> sortOrder = getGridSortOrder();

			try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream(); CSVWriter writer = createCsvWriter(byteStream)) {
				writer.writeNext(headerRow);
				writer.writeNext(labelsRow, false);

				String[] rowValues = new String[columnValueProviders.length];

				Set<?> selectedRows = getSelectedRows != null ? getSelectedRows.get() : Collections.emptySet();

				if (CollectionUtils.isEmpty(selectedRows)) {
					selectedRows = Collections.emptySet();
				}

				if (selectedRows.isEmpty()) {
					int totalRowCount = dataProvider.size(new Query());
					for (int i = 0; i < totalRowCount; i += 100) {
						dataProvider.fetch(new Query(i, 100, sortOrder, null, null)).forEach(row -> {
							writeLine(writer, columnValueProviders, row, rowValues);
						});
						writer.flush();
					}
				} else {
					selectedRows.forEach(row -> {
						writeLine(writer, columnValueProviders, row, rowValues);
					});
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

		private static void writeLine(CSVWriter writer, ValueProvider[] columnValueProviders, Object row, String[] rowValues) {
			for (int c = 0; c < columnValueProviders.length; c++) {
				Object value = columnValueProviders[c].apply(row);

				final String valueString;
				if (value == null) {
					valueString = "";
				} else if (value instanceof Date) {
					valueString = DateFormatHelper.formatLocalDateTime((Date) value);
				} else if (value instanceof Boolean) {
					if ((Boolean) value == true) {
						valueString = I18nProperties.getEnumCaption(YesNoUnknown.YES);
					} else
						valueString = I18nProperties.getEnumCaption(YesNoUnknown.NO);
				} else if (value instanceof AgeAndBirthDateDto) {
					AgeAndBirthDateDto ageAndBirthDate = (AgeAndBirthDateDto) value;
					valueString = PersonHelper.getAgeAndBirthdateString(
						ageAndBirthDate.getAge(),
						ageAndBirthDate.getAgeType(),
						ageAndBirthDate.getDateOfBirthDD(),
						ageAndBirthDate.getDateOfBirthMM(),
						ageAndBirthDate.getDateOfBirthYYYY());
				} else if (value instanceof BirthDateDto) {
					BirthDateDto birthDate = (BirthDateDto) value;
					valueString =
						DateFormatHelper.formatDate(birthDate.getDateOfBirthDD(), birthDate.getDateOfBirthMM(), birthDate.getDateOfBirthYYYY());
				} else if (value instanceof HasText) {
					// was a Label
					valueString = ((HasText) value).getText();
				} else if (value instanceof ReferenceDto) {
					valueString = ((ReferenceDto) value).getCaption();
				} else {
					valueString = value.toString();
				}
				rowValues[c] = valueString;
			}
			writer.writeNext(rowValues);
		}

		private List<Column<?>> getGridColumns() {
			return grid.getColumns()
				.stream()
				.map(column -> (Column<?>) column)
				.filter(column -> column.isVisible() || includePropertyIds.contains(column.getKey()))
				.filter(column -> !excludePropertyIds.contains(column.getKey()))
				.collect(Collectors.toList());
		}

		private List<QuerySortOrder> getGridSortOrder() {
			return grid.getSortOrder()
				.stream()
				.flatMap(
					gridSortOrder -> grid.getColumns()
						.stream()
						.filter(column -> column.getKey().equals(gridSortOrder.getSorted().getKey()))
						.findFirst()
						.get()
						.getSortOrder(gridSortOrder.getDirection()))
				.collect(Collectors.toList());
		}

		private CSVWriter createCsvWriter(ByteArrayOutputStream byteStream) throws UnsupportedEncodingException {
			return CSVUtils.createCSVWriter(
				new OutputStreamWriter(byteStream, StandardCharsets.UTF_8.name()),
				FacadeProvider.getConfigFacade().getCsvSeparator());
		}
	}
}
