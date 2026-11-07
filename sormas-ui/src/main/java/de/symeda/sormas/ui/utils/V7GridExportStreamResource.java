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

import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.opencsv.CSVWriter;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.function.ValueProvider;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.utils.CSVUtils;
import de.symeda.sormas.api.utils.YesNoUnknown;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;

/**
 * CSV export of a grid that was a Vaadin 7 grid (now a Flow grid): one header row with the column captions, then all
 * items of the grid in their displayed order (the user's sort) and with its filter. Was built on the Vaadin 7 container and its columns.
 */
@SuppressWarnings("serial")
public class V7GridExportStreamResource extends StreamSourceDownloadHandler {

	public V7GridExportStreamResource(Grid<?> grid, String filename, String... ignoredPropertyIds) {

		super(new StreamSource() {

			@SuppressWarnings({
				"unchecked",
				"rawtypes" })
			@Override
			public InputStream getStream() {
				List<String> ignoredPropertyIdsList = Arrays.asList(ignoredPropertyIds);
				List<Column<?>> columns = new ArrayList<>(grid.getColumns());
				columns.removeIf(column -> !column.isVisible());
				columns.removeIf(column -> ignoredPropertyIdsList.contains(column.getKey()));

				List<String> headerRow = columns.stream().map(Column::getHeaderText).collect(toList());
				List<ValueProvider> valueProviders =
					columns.stream().map(column -> GridExportStreamResource.getExportValueProvider(grid, column)).collect(toList());

				try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
					try (CSVWriter writer = CSVUtils.createCSVWriter(
						new OutputStreamWriter(byteStream, StandardCharsets.UTF_8.name()),
						FacadeProvider.getConfigFacade().getCsvSeparator())) {

						writer.writeNext(headerRow.toArray(new String[headerRow.size()]));

						DataProvider dataProvider = grid.getDataProvider();
						// All rows in the order and with the filter the grid shows them, as the V7 container did: the grid's
						// query carries the back-end sort orders (lazy providers), the in-memory comparator (list providers)
						// and the grid's in-memory filter.
						Query query = grid.getDataCommunicator().buildQuery(0, Integer.MAX_VALUE);
						((java.util.stream.Stream<Object>) dataProvider.fetch(query)).forEach(item -> {
							List<String> row = new ArrayList<>();
							valueProviders.forEach(valueProvider -> {
								Object value = valueProvider.apply(item);
								if (value != null) {
									if (value instanceof Date) {
										row.add(DateFormatHelper.formatLocalDateTime((Date) value));
									} else if (value instanceof Boolean) {
										if ((Boolean) value) {
											row.add(I18nProperties.getEnumCaption(YesNoUnknown.YES));
										} else
											row.add(I18nProperties.getEnumCaption(YesNoUnknown.NO));
									} else {
										row.add(value.toString());
									}
								} else {
									row.add("");
								}
							});

							writer.writeNext(row.toArray(new String[row.size()]));
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
		}, filename, "text/csv");
	}
}
