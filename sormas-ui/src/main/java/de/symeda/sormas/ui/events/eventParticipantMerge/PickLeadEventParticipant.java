package de.symeda.sormas.ui.events.eventParticipantMerge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.event.EventParticipantSelectionDto;
import de.symeda.sormas.api.event.EventReferenceDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.events.EventDataView;
import de.symeda.sormas.ui.navigation.Navigator;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.fields.CustomField;

public class PickLeadEventParticipant extends CustomField<List<String>> {

	private Map<String, EventParticipantSelectGrid> eventParticipantSelectGrids;
	private PickOrMerge pickOrMerge = PickOrMerge.PICK;

	protected VerticalLayout mainLayout;

	public PickLeadEventParticipant(List<EventParticipantSelectionDto> eventParticipantSelectionDtos) {
		Map<EventReferenceDto, List<EventParticipantSelectionDto>> eventParticipantsByEventSelectionDtos =
			eventParticipantSelectionDtos.stream().collect(Collectors.groupingBy(EventParticipantSelectionDto::getEvent));

		eventParticipantSelectGrids = initializeEventParticipantsSelectGrids(eventParticipantsByEventSelectionDtos);
	}

	@Override
	protected Component initContent() {
		mainLayout = new VerticalLayout();
		mainLayout.setSpacing(true);
		mainLayout.setPadding(false);
		mainLayout.setSizeUndefined();
		mainLayout.setWidthFull();

		HorizontalLayout infoLayout = new HorizontalLayout();
		infoLayout.add(VaadinUiUtil.createInfoComponent(I18nProperties.getString(Strings.infoPickEventParticipantsForPersonMerge)));
		mainLayout.add(infoLayout);

		eventParticipantSelectGrids.forEach((key, value) -> {
			mainLayout.add(value.getEventTitle());
			mainLayout.add(value.eventParticipantMergeSelectionGrid);
		});

		return mainLayout;
	}

	@Override
	public Class<? extends List<String>> getType() {
		return (Class<? extends List<String>>) new ArrayList<String>(0).getClass();
	}

	@Override
	public List<String> getValue() {
		return eventParticipantSelectGrids.values().stream().map(eventParticipantSelectGrid -> {
			Set<EventParticipantSelectionDto> selectedRow = eventParticipantSelectGrid.eventParticipantMergeSelectionGrid.getSelectedItems();
			if (!selectedRow.isEmpty()) {
				return selectedRow.iterator().next().getUuid();
			}
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	protected Map<String, EventParticipantSelectGrid> initializeEventParticipantsSelectGrids(
		Map<EventReferenceDto, List<EventParticipantSelectionDto>> eventParticipantByEventDtos) {

		Map<String, EventParticipantSelectGrid> ret = new HashMap<>();

		eventParticipantByEventDtos.forEach((key, value) -> {

			EventParticipantMergeSelectionGrid newGrid = new EventParticipantMergeSelectionGrid(value);
			Component eventComponent = buildEventComponent(key.getUuid(), key.getCaption());

			EventParticipantSelectGrid eventParticipantSelectGrid = new EventParticipantSelectGrid();
			eventParticipantSelectGrid.setEventTitle(eventComponent);
			eventParticipantSelectGrid.setEventParticipantMergeSelectionGrid(newGrid);

			ret.put(key.getUuid(), eventParticipantSelectGrid);

		});
		return ret;
	}

	private static class EventParticipantSelectGrid {

		private Component eventTitle;
		private EventParticipantMergeSelectionGrid eventParticipantMergeSelectionGrid;

		public Component getEventTitle() {
			return eventTitle;
		}

		public void setEventTitle(Component eventTitle) {
			this.eventTitle = eventTitle;
		}

		public EventParticipantMergeSelectionGrid getEventParticipantMergeSelectionGrid() {
			return eventParticipantMergeSelectionGrid;
		}

		public void setEventParticipantMergeSelectionGrid(EventParticipantMergeSelectionGrid eventParticipantMergeSelectionGrid) {
			this.eventParticipantMergeSelectionGrid = eventParticipantMergeSelectionGrid;
		}
	}

	private Component buildEventComponent(String eventUuid, String eventTitle) {
		HorizontalLayout eventLayout = new HorizontalLayout();
		eventLayout.setSpacing(true);
		eventLayout.setPadding(false);

		VerticalLayout eventUuidLayout = new VerticalLayout();
		eventUuidLayout.setPadding(false);
		eventUuidLayout.setSpacing(false);
		Span eventUuidLabel = new Span(I18nProperties.getCaption(Captions.Event_uuid));
		eventUuidLabel.setClassName(CssStyles.LABEL_BOLD);
		eventUuidLabel.addClassName(CssStyles.VSPACE_NONE);
		eventUuidLayout.add(eventUuidLabel);

		// was a #! link (decision 2026-11-05: routes without #!)
		Anchor linkUuidData = new Anchor(Navigator.getUrl(EventDataView.VIEW_NAME + "/" + eventUuid), DataHelper.getShortUuid(eventUuid));
		linkUuidData.setTarget(AnchorTarget.BLANK);
		eventUuidLayout.add(linkUuidData);
		eventLayout.add(eventUuidLayout);

		VerticalLayout eventTitleLayout = new VerticalLayout();
		eventTitleLayout.setPadding(false);
		eventTitleLayout.setSpacing(false);
		Span eventTitleLabel = new Span(I18nProperties.getCaption(Captions.Event_eventTitle));
		eventTitleLabel.setClassName(CssStyles.LABEL_BOLD);
		eventTitleLabel.addClassName(CssStyles.VSPACE_NONE);
		eventTitleLayout.add(eventTitleLabel);

		Span eventTitleData = new Span(eventTitle);
		eventTitleData.addClassName(CssStyles.VSPACE_TOP_NONE);
		eventTitleLayout.add(eventTitleData);
		eventLayout.add(eventTitleLayout);

		return eventLayout;
	}

	public PickOrMerge getPickOrMerge() {
		return pickOrMerge;
	}

	public void setPickOrMerge(PickOrMerge pickOrMerge) {
		this.pickOrMerge = pickOrMerge;
	}

	public enum PickOrMerge {
		PICK,
		MERGE
	}

}
