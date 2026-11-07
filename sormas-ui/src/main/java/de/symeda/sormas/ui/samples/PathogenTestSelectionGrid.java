package de.symeda.sormas.ui.samples;

import java.util.Date;
import java.util.List;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.sample.PathogenTestDto;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.utils.V7UuidRenderer;
import de.symeda.sormas.ui.utils.grid.Grid;
import de.symeda.sormas.ui.utils.grid.HeightMode;
import de.symeda.sormas.ui.utils.grid.renderers.DateRenderer;

public class PathogenTestSelectionGrid extends Grid<PathogenTestDto> {

	public PathogenTestSelectionGrid(List<PathogenTestDto> tests) {
		super(PathogenTestDto.class);
		buildGrid();
		setContainerData(tests);
	}

	@SuppressWarnings("unchecked")
	private void buildGrid() {
		setSizeFull();
		setSelectionMode(SelectionMode.SINGLE);
		setHeightMode(HeightMode.ROW);

		setColumns(
			PathogenTestDto.UUID,
			PathogenTestDto.TEST_DATE_TIME,
			PathogenTestDto.TESTED_DISEASE,
			PathogenTestDto.TEST_TYPE,
			PathogenTestDto.LAB,
			PathogenTestDto.TEST_RESULT);

		getColumn(PathogenTestDto.UUID).setRenderer(new V7UuidRenderer());
		((Column<PathogenTestDto, Date>) getColumn(PathogenTestDto.TEST_DATE_TIME))
			.setRenderer(new DateRenderer(DateHelper.getLocalDateTimeFormat(I18nProperties.getUserLanguage())));

		for (Column<PathogenTestDto, ?> column : getAllColumns()) {
			column.setHeaderCaption(I18nProperties.getPrefixCaption(PathogenTestDto.I18N_PREFIX, column.getPropertyId().toString()));
		}
	}

	private void setContainerData(List<PathogenTestDto> tests) {
		setItems(tests);
		setHeightByRows(tests.size() > 0 ? (tests.size() <= 10 ? tests.size() : 10) : 1);
	}

	/**
	 * Was the Vaadin 7 grid's {@code getSelectedRow()}.
	 */
	public PathogenTestDto getSelectedRow() {
		return getSelectedItems().stream().findFirst().orElse(null);
	}
}
