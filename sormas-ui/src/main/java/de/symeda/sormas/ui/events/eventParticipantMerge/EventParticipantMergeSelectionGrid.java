package de.symeda.sormas.ui.events.eventParticipantMerge;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;

import de.symeda.sormas.api.event.EventParticipantDto;
import de.symeda.sormas.api.event.EventParticipantSelectionDto;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.location.LocationDto;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.caze.CaseDataView;
import de.symeda.sormas.ui.events.EventParticipantDataView;
import de.symeda.sormas.ui.navigation.Navigator;
import de.symeda.sormas.ui.utils.GridExportStreamResource;
import de.symeda.sormas.ui.utils.UuidRenderer;
import de.symeda.sormas.ui.utils.grid.Grid;

public class EventParticipantMergeSelectionGrid extends Grid<EventParticipantSelectionDto> {

	public static final String EVENT_PARTICIPANT_COLUMN_UUID = "eventParticipantUuidLink";
	public static final String EVENT_UUID = "eventUuidLink";
	public static final String RESULTING_CASE_UUID = "resultingCaseUuidLink";

	public EventParticipantMergeSelectionGrid(List<EventParticipantSelectionDto> eventParticipantSelectionDtos) {
		super(EventParticipantSelectionDto.class);
		buildGrid();

		setDataProvider(eventParticipantSelectionDtos.stream());
	}

	private void buildGrid() {
		setWidthFull();
		setSelectionMode(SelectionMode.SINGLE);
		setHeight("150px");

		Column<EventParticipantSelectionDto, Anchor> eventParticipantColumn = addComponentColumn(indexDto -> {
			// was a #! link (decision 2026-11-05: routes without #!)
			Anchor link = new Anchor(
				Navigator.getUrl(EventParticipantDataView.VIEW_NAME + "/" + indexDto.getUuid()),
				DataHelper.getShortUuid(indexDto.getUuid()));
			link.setTarget(AnchorTarget.BLANK);
			return link;
		});
		eventParticipantColumn.setId(EVENT_PARTICIPANT_COLUMN_UUID);
		GridExportStreamResource.setExportValueProvider(eventParticipantColumn, indexDto -> DataHelper.getShortUuid(indexDto.getUuid()));

		Column<EventParticipantSelectionDto, Anchor> resultingCaseColumn = addComponentColumn(indexDto -> {
			// was a #! link (decision 2026-11-05: routes without #!)
			Anchor link = new Anchor(
				Navigator.getUrl(CaseDataView.VIEW_NAME + "/" + indexDto.getResultingCaseUuid()),
				DataHelper.getShortUuid(indexDto.getResultingCaseUuid()));
			link.setTarget(AnchorTarget.BLANK);
			return link;
		});
		resultingCaseColumn.setId(RESULTING_CASE_UUID);
		GridExportStreamResource
			.setExportValueProvider(resultingCaseColumn, indexDto -> DataHelper.getShortUuid(indexDto.getResultingCaseUuid()));

		setColumns(
			EVENT_PARTICIPANT_COLUMN_UUID,
			EventParticipantSelectionDto.PERSON_UUID,
			EventParticipantSelectionDto.FIRST_NAME,
			EventParticipantSelectionDto.LAST_NAME,
			EventParticipantSelectionDto.AGE_AND_BIRTH_DATE,
			EventParticipantSelectionDto.SEX,
			EventParticipantSelectionDto.DISTRICT_NAME,
			EventParticipantSelectionDto.INVOLVEMENT_DESCRIPTION,
			RESULTING_CASE_UUID,
			EventParticipantSelectionDto.CONTACT_COUNT);

		for (Column<EventParticipantSelectionDto, ?> column : getAllColumns()) {
			column.setCaption(
				I18nProperties.findPrefixCaptionWithDefault(
					column.getKey(),
					column.getCaption(),
					EventParticipantSelectionDto.I18N_PREFIX,
					EventParticipantDto.I18N_PREFIX,
					PersonDto.I18N_PREFIX,
					LocationDto.I18N_PREFIX));
		}

		getColumn(EventParticipantSelectionDto.FIRST_NAME).setMinimumWidth(150);
		getColumn(EventParticipantSelectionDto.LAST_NAME).setMinimumWidth(150);
		((Column<EventParticipantSelectionDto, String>) getColumn(EventParticipantSelectionDto.PERSON_UUID)).setRenderer(new UuidRenderer());
		getColumn(EventParticipantSelectionDto.CONTACT_COUNT).setSortable(false);

		if (UiUtil.enabled(FeatureType.HIDE_JURISDICTION_FIELDS)) {
			getColumn(EventParticipantSelectionDto.DISTRICT_NAME).setHidden(true);
		}
	}

	/**
	 * Vaadin 8 {@code DataProvider.fromStream}: an in-memory list of the given items.
	 */
	public void setDataProvider(Stream<EventParticipantSelectionDto> items) {
		setItems(items.collect(Collectors.toList()));
	}
}
