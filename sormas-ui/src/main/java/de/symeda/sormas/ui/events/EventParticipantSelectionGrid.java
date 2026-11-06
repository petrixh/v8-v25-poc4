package de.symeda.sormas.ui.events;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.UI;

import de.symeda.sormas.api.event.EventDto;
import de.symeda.sormas.api.event.SimilarEventParticipantDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.person.SimilarPersonDto;
import de.symeda.sormas.ui.utils.V7UuidRenderer;
import de.symeda.sormas.ui.utils.grid.Grid;
import de.symeda.sormas.ui.utils.grid.HeightMode;
import de.symeda.sormas.ui.utils.grid.renderers.DateRenderer;

public class EventParticipantSelectionGrid extends Grid<SimilarEventParticipantDto> {

	private static final long serialVersionUID = 1L;

	public EventParticipantSelectionGrid(List<SimilarEventParticipantDto> eventParticipants) {
		super(SimilarEventParticipantDto.class);
		buildGrid();
		setContainerData(eventParticipants);
	}

	private void setContainerData(List<SimilarEventParticipantDto> similarEventParticipants) {
		if (similarEventParticipants != null) {
			setItems(similarEventParticipants);
			setHeightByRows(similarEventParticipants.size() > 0 ? (similarEventParticipants.size() <= 10 ? similarEventParticipants.size() : 10) : 1);
		}
	}

	private void buildGrid() {
		setSizeFull();
		setSelectionMode(SelectionMode.SINGLE);
		setHeightMode(HeightMode.ROW);

		setColumns();
	}

	protected void setColumns() {
		// some property ids are Object constants (Vaadin 7 property ids) in the API
		setColumns(
			(String) SimilarEventParticipantDto.UUID,
			(String) SimilarEventParticipantDto.FIRST_NAME,
			(String) SimilarEventParticipantDto.LAST_NAME,
			(String) SimilarEventParticipantDto.INVOLVEMENT_DESCRIPTION,
			(String) SimilarEventParticipantDto.EVENT_UUID,
			SimilarEventParticipantDto.EVENT_STATUS,
			SimilarEventParticipantDto.EVENT_TITLE,
			SimilarEventParticipantDto.START_DATE);

		for (Column<SimilarEventParticipantDto, ?> column : getAllColumns()) {
			column.setHeaderCaption(
				I18nProperties.findPrefixCaption(
					column.getPropertyId().toString(),
					SimilarEventParticipantDto.I18N_PREFIX,
					SimilarPersonDto.I18N_PREFIX,
					EventDto.I18N_PREFIX));
		}

		getColumn((String) SimilarEventParticipantDto.UUID).setRenderer(new V7UuidRenderer());
		getColumn((String) SimilarEventParticipantDto.FIRST_NAME).setMinimumWidth(150);
		getColumn((String) SimilarEventParticipantDto.LAST_NAME).setMinimumWidth(150);
		getColumn((String) SimilarEventParticipantDto.EVENT_UUID).setRenderer(new V7UuidRenderer());
		setV7ColumnRenderers();
	}

	/**
	 * The Vaadin 7 grid showed the start date through the session's default converter: the medium date-time format of the
	 * UI locale (V7 {@code StringToDateConverter}), as {@code ContactSelectionGrid} does.
	 */
	@SuppressWarnings("unchecked")
	protected void setV7ColumnRenderers() {
		UI ui = UI.getCurrent();
		Locale locale = ui != null ? ui.getLocale() : Locale.getDefault();
		((Column<SimilarEventParticipantDto, Date>) getColumn(SimilarEventParticipantDto.START_DATE))
			.setRenderer(new DateRenderer(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale)));
	}
}
