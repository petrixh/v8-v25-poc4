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

package de.symeda.sormas.ui.samples.humansample;

import java.util.Date;
import java.util.List;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.sample.SampleDto;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.utils.V7UuidRenderer;
import de.symeda.sormas.ui.utils.grid.Grid;
import de.symeda.sormas.ui.utils.grid.HeightMode;
import de.symeda.sormas.ui.utils.grid.renderers.DateRenderer;

public class SampleSelectionGrid extends Grid<SampleDto> {

	public SampleSelectionGrid(List<SampleDto> samples) {
		super(SampleDto.class);
		buildGrid();
		setContainerData(samples);
	}

	@SuppressWarnings("unchecked")
	private void buildGrid() {
		setSizeFull();
		setSelectionMode(SelectionMode.SINGLE);
		setHeightMode(HeightMode.ROW);

		setColumns(
			SampleDto.UUID,
			SampleDto.SAMPLE_DATE_TIME,
			SampleDto.RECEIVED_DATE,
			SampleDto.LAB_SAMPLE_ID,
			SampleDto.SAMPLE_MATERIAL,
			SampleDto.LAB,
			SampleDto.PATHOGEN_TEST_RESULT);

		getColumn(SampleDto.UUID).setRenderer(new V7UuidRenderer());
		((Column<SampleDto, Date>) getColumn(SampleDto.SAMPLE_DATE_TIME))
			.setRenderer(new DateRenderer(DateHelper.getLocalDateTimeFormat(I18nProperties.getUserLanguage())));
		((Column<SampleDto, Date>) getColumn(SampleDto.RECEIVED_DATE))
			.setRenderer(new DateRenderer(DateHelper.getLocalDateTimeFormat(I18nProperties.getUserLanguage())));

		for (Column<SampleDto, ?> column : getAllColumns()) {
			column.setHeaderCaption(I18nProperties.getPrefixCaption(SampleDto.I18N_PREFIX, column.getPropertyId().toString()));
		}
	}

	private void setContainerData(List<SampleDto> samples) {
		setItems(samples);
		setHeightByRows(samples.size() > 0 ? (samples.size() <= 10 ? samples.size() : 10) : 1);
	}

	/**
	 * Was the Vaadin 7 grid's {@code getSelectedRow()}.
	 */
	public SampleDto getSelectedRow() {
		return getSelectedItems().stream().findFirst().orElse(null);
	}
}
