/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2024 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.dashboard.gis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.CaseMeasure;
import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.adverseeventsfollowingimmunization.AefiType;
import de.symeda.sormas.api.caze.CaseClassification;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.caze.CaseFacade;
import de.symeda.sormas.api.caze.MapCaseDto;
import de.symeda.sormas.api.contact.ContactClassification;
import de.symeda.sormas.api.contact.MapContactDto;
import de.symeda.sormas.api.dashboard.AefiDashboardCriteria;
import de.symeda.sormas.api.dashboard.DashboardCriteria;
import de.symeda.sormas.api.dashboard.DashboardEventDto;
import de.symeda.sormas.api.dashboard.GisDashboardCriteria;
import de.symeda.sormas.api.dashboard.SampleDashboardCriteria;
import de.symeda.sormas.api.dashboard.adverseeventsfollowingimmunization.MapAefiDto;
import de.symeda.sormas.api.dashboard.sample.MapSampleDto;
import de.symeda.sormas.api.event.EventStatus;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.geo.GeoLatLon;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.InfrastructureHelper;
import de.symeda.sormas.api.infrastructure.district.DistrictDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.facility.FacilityDto;
import de.symeda.sormas.api.infrastructure.facility.FacilityReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.person.Sex;
import de.symeda.sormas.api.sample.SampleAssociationType;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.dashboard.HasDashboardCasePopupGrid;
import de.symeda.sormas.ui.dashboard.map.BaseDashboardMapComponent;
import de.symeda.sormas.ui.dashboard.map.CasePopupGrid;
import de.symeda.sormas.ui.dashboard.map.MapCaseClassificationOption;
import de.symeda.sormas.ui.dashboard.map.MapCaseDisplayMode;
import de.symeda.sormas.ui.map.LeafletMapUtil;
import de.symeda.sormas.ui.map.LeafletMarker;
import de.symeda.sormas.ui.map.LeafletPolygon;
import de.symeda.sormas.ui.map.MarkerIcon;
import de.symeda.sormas.ui.utils.ComboBoxHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.fields.CheckBox;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.fields.OptionGroup;
import de.symeda.sormas.ui.utils.grid.HeightMode;

public class GisDashboardMapComponent extends BaseDashboardMapComponent<GisDashboardCriteria, GisDashboardDataProvider>
	implements HasDashboardCasePopupGrid {

	//marker groups
	private static final String CASES_GROUP_ID = "cases";
	private static final String CONTACTS_GROUP_ID = "contacts";
	private static final String EVENTS_GROUP_ID = "events";
	private static final String REGIONS_GROUP_ID = "regions";
	private static final String DISTRICTS_GROUP_ID = "districts";
	private static final String SAMPLES_GROUP_ID = "samples";
	private static final String AEFI_GROUP_ID = "adverseevents";

	//layers
	private MapCaseClassificationOption caseClassificationOption;
	private boolean showCases;
	private boolean showContacts;
	private boolean showConfirmedContacts;
	private boolean showUnconfirmedContacts;
	private boolean showEvents;
	private boolean showRegions;
	private boolean hideOtherCountries;
	private boolean showCurrentEpiSituation;

	private Set<SampleAssociationType> displayedHumanSamples;
	private boolean showEnvironmentalSamples;

	// Entities
	private final HashMap<FacilityReferenceDto, List<MapCaseDto>> casesByFacility = new HashMap<>();
	private List<MapCaseDto> mapCaseDtos = new ArrayList<>();
	private List<MapCaseDto> mapAndFacilityCases = new ArrayList<>();
	private List<MapContactDto> mapContactDtos = new ArrayList<>();

	// Map data
	private final List<FacilityReferenceDto> markerCaseFacilities = new ArrayList<FacilityReferenceDto>();
	private final List<MapContactDto> markerContacts = new ArrayList<MapContactDto>();
	private final List<DashboardEventDto> markerEvents = new ArrayList<DashboardEventDto>();
	private final List<RegionReferenceDto> polygonRegions = new ArrayList<RegionReferenceDto>();
	private final List<DistrictReferenceDto> polygonDistricts = new ArrayList<DistrictReferenceDto>();

	//others
	private MapCaseDisplayMode mapCaseDisplayMode = MapCaseDisplayMode.FACILITY_OR_CASE_ADDRESS;
	private CaseMeasure caseMeasure = CaseMeasure.CASE_COUNT;
	private BigDecimal districtValuesLowerQuartile;
	private BigDecimal districtValuesMedian;
	private BigDecimal districtValuesUpperQuartile;
	private boolean emptyPopulationDistrictPresent;

	private boolean loadCases;
	private boolean loadContacts;
	private boolean loadSamples;
	private boolean loadEvents;
	private boolean loadAefi;
	private CheckBox showCasesCheckBox;
	private CheckBox showContactsCheckBox;
	private CheckBox showCaseSamplesCheckBox;
	private CheckBox showContactSamplesCheckBox;
	private CheckBox showEventParticipantSamplesCheckBox;
	private CheckBox showEnvironmentSamplesCheckBox;
	private CheckBox showSeriousAefiCheckBox;
	private CheckBox showNonSeriousAefiCheckBox;
	private CheckBox showEventsCheckBox;

	public GisDashboardMapComponent(GisDashboardDataProvider dashboardDataProvider) {
		super(Strings.headingGisDashboardMap, dashboardDataProvider, null, true);
	}

	@Override
	protected void addComponents() {
		displayedHumanSamples = new HashSet<>();
		if (UiUtil.permitted(FeatureType.ENVIRONMENT_MANAGEMENT, UserRight.ENVIRONMENT_SAMPLE_VIEW)) {
			showEnvironmentalSamples = false;
		}

		caseClassificationOption = MapCaseClassificationOption.ALL_CASES;

		showCases = true;
		showContacts = false;
		showConfirmedContacts = true;
		showUnconfirmedContacts = true;

		super.addComponents();

		setPadding(false);
		setSpacing(false);

		map.setZoom(5);

		mapHeaderLayout.setWidth(null);
		mapHeaderLayout.removeAll();
		CssStyles.style(mapHeaderLayout, CssStyles.GIS_DASHBOARD_MAP_HEADER);
	}

	protected void updateEntityLoadingStatus() {
		if (showCasesCheckBox != null) {
			loadCases = showCasesCheckBox.getValue();
		}

		if (showContactsCheckBox != null) {
			loadContacts = showContactsCheckBox.getValue();
		}

		if (showCaseSamplesCheckBox != null
			&& showContactSamplesCheckBox != null
			&& showEventParticipantSamplesCheckBox != null
			&& showEnvironmentSamplesCheckBox != null) {
			loadSamples = showCaseSamplesCheckBox.getValue()
				|| showContactSamplesCheckBox.getValue()
				|| showEventParticipantSamplesCheckBox.getValue()
				|| showEnvironmentSamplesCheckBox.getValue();
		}

		if (showSeriousAefiCheckBox != null && showNonSeriousAefiCheckBox != null) {
			loadAefi = showSeriousAefiCheckBox.getValue() || showNonSeriousAefiCheckBox.getValue();
		}

		if (showEventsCheckBox != null) {
			loadEvents = showEventsCheckBox.getValue();
		}
	}

	@Override
	protected Long getMarkerCount(Date fromDate, Date toDate, int maxCount) {
		long markerCount = 0L;

		RegionReferenceDto region = dashboardDataProvider.getRegion();
		DistrictReferenceDto district = dashboardDataProvider.getDistrict();
		Disease disease = dashboardDataProvider.getDisease();
		Sex sex = dashboardDataProvider.getSex();

		if (loadCases) {
			if (showCases) {
				markerCount += FacadeProvider.getCaseFacade()
					.countCasesForMap(
						region,
						district,
						disease,
						fromDate,
						toDate,
						showCurrentEpiSituation ? null : dashboardDataProvider.getCaseDateType(),
						sex);
			}
		} else if (loadContacts) {
			if (showContacts) {
				markerCount += FacadeProvider.getContactFacade().countContactsForMap(region, district, disease, fromDate, toDate);
			}
		} else if (loadSamples) {
			markerCount += FacadeProvider.getSampleDashboardFacade()
				.countSamplesForMap(dashboardDataProvider.buildSampleDashboardCriteria(), displayedHumanSamples)
				+ (showEnvironmentalSamples
					? FacadeProvider.getSampleDashboardFacade().countEnvironmentalSamplesForMap(dashboardDataProvider.buildSampleDashboardCriteria())
					: 0);
		} else if (loadAefi) {
			markerCount += FacadeProvider.getAefiDashboardFacade().countAefiForMap(dashboardDataProvider.buildAefiDashboardCriteria());
		} else if (loadEvents) {
			if (showEvents) {
				DashboardCriteria eventDashboardCriteria = dashboardDataProvider.buildEventDashboardCriteria();
				List<DashboardEventDto> events = FacadeProvider.getDashboardFacade().getNewEvents(eventDashboardCriteria);
				markerCount += events.size();
			}
		}

		return markerCount;
	}

	@Override
	protected void loadMapData(Date fromDate, Date toDate) {
		RegionReferenceDto region = dashboardDataProvider.getRegion();
		DistrictReferenceDto district = dashboardDataProvider.getDistrict();
		Disease disease = dashboardDataProvider.getDisease();
		Sex sex = dashboardDataProvider.getSex();

		clearRegionShapes();

		LeafletMapUtil.clearOtherCountriesOverlay(map);
		if (hideOtherCountries) {
			LeafletMapUtil.addOtherCountriesOverlay(map);
		}

		if (showRegions) {
			showRegionsShapes(caseMeasure, fromDate, toDate, dashboardDataProvider.getDisease());
		}

		if (loadCases) {
			clearCaseMarkers();
			if (showCases) {
				showCaseMarkers(
					FacadeProvider.getCaseFacade()
						.getCasesForMap(
							region,
							district,
							disease,
							fromDate,
							toDate,
							showCurrentEpiSituation ? null : dashboardDataProvider.getCaseDateType(),
							sex));
			}
			loadCases = false;
		}

		if (loadContacts) {
			clearContactMarkers();
			if (showContacts) {
				showContactMarkers(FacadeProvider.getContactFacade().getContactsForMap(region, district, disease, fromDate, toDate));
			}
			loadContacts = false;
		}

		if (loadSamples) {
			clearSampleMarkers();
			showSampleMarkers();
			loadSamples = false;
		}

		if (loadAefi) {
			clearAefiMarkers();
			showAefiMarkers();
			loadAefi = false;
		}

		if (loadEvents) {
			clearEventMarkers();
			if (showEvents) {
				showEventMarkers();
			}
			loadEvents = false;
		}
	}

	@Override
	protected void addLayerOptions(VerticalLayout layersLayout) {
		HorizontalLayout mainLayersLayout = new HorizontalLayout();
		mainLayersLayout.setPadding(false);
		mainLayersLayout.setSpacing(true);

		//cases layers
		VerticalLayout casesLayers = new VerticalLayout();
		{
			casesLayers.setPadding(false);
			casesLayers.setSpacing(false);

			Span casesLayersLabel = new Span(I18nProperties.getString(Strings.entityCases));
			CssStyles.style(casesLayersLabel, CssStyles.H4, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_NONE);
			casesLayers.add(casesLayersLabel);

			//case classifications
			OptionGroup caseClassificationOptions = new OptionGroup();
			caseClassificationOptions.addItems((Object[]) MapCaseClassificationOption.values());
			caseClassificationOptions.setValue(caseClassificationOption);
			caseClassificationOptions.addValueChangeListener(event -> {
				caseClassificationOption = (MapCaseClassificationOption) event.getValue();
				loadCases = true;
				refreshMap(true);
			});

			// Optiongroup to select what property the coordinates should be based on
			OptionGroup mapCaseDisplayModeSelect = new OptionGroup();
			mapCaseDisplayModeSelect.setWidth(100, Unit.PERCENTAGE);
			mapCaseDisplayModeSelect.addItems((Object[]) MapCaseDisplayMode.values());
			mapCaseDisplayModeSelect.setValue(mapCaseDisplayMode);
			mapCaseDisplayModeSelect.addValueChangeListener(event -> {
				mapCaseDisplayMode = (MapCaseDisplayMode) event.getValue();
				loadCases = true;
				refreshMap(true);
			});

			//sex combobox
			ComboBox caseSexCombobox = ComboBoxHelper.createComboBoxV7();
			caseSexCombobox.setId("caseSexFilter");
			caseSexCombobox.setInputPrompt(I18nProperties.getString(Strings.promptCaseSex));
			caseSexCombobox.addItems((Object[]) Sex.values());
			caseSexCombobox.addValueChangeListener(event -> {
				dashboardDataProvider.setSex((Sex) event.getValue());
				loadCases = true;
				refreshMap(true);
			});

			if (UiUtil.permitted(UserRight.CASE_VIEW)) {
				HorizontalLayout showCasesLayout = new HorizontalLayout();
				{
					showCasesLayout.setPadding(false);
					showCasesLayout.setSpacing(false);

					showCasesCheckBox = new CheckBox();
					showCasesCheckBox.setId(Captions.dashboardShowCases);
					showCasesCheckBox.setCaption(I18nProperties.getCaption(Captions.dashboardShowCases));
					showCasesCheckBox.setValue(showCases);
					showCasesCheckBox.addValueChangeListener(e -> {
						showCases = (boolean) e.getValue();
						mapCaseDisplayModeSelect.setEnabled(showCases);
						mapCaseDisplayModeSelect.setValue(mapCaseDisplayMode);
						caseClassificationOptions.setEnabled(showCases);
						caseSexCombobox.setEnabled(showCases);
						loadCases = true;
						refreshMap(true);
					});
					showCasesLayout.add(showCasesCheckBox);

					Span infoLabel = new Span(VaadinIcon.INFO_CIRCLE.create());
					infoLabel.setTitle(I18nProperties.getString(Strings.infoCaseMap));
					CssStyles.style(infoLabel, CssStyles.LABEL_MEDIUM, CssStyles.LABEL_SECONDARY, CssStyles.HSPACE_LEFT_3);
					infoLabel.setHeight(null);
					showCasesLayout.add(infoLabel);
					showCasesLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, infoLabel);
				}
				casesLayers.add(showCasesLayout);

				casesLayers.add(mapCaseDisplayModeSelect);
				mapCaseDisplayModeSelect.setEnabled(showCases);

				casesLayers.add(caseClassificationOptions);
				caseClassificationOptions.setEnabled(showCases);

				casesLayers.add(caseSexCombobox);
				caseSexCombobox.setEnabled(showCases);
			}
		}

		//contact layers
		{
			if (UiUtil.permitted(UserRight.CONTACT_VIEW)) {
				Span contactsLayersLabel = new Span(I18nProperties.getString(Strings.entityContacts));
				CssStyles.style(contactsLayersLabel, CssStyles.H4, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_3);
				casesLayers.add(contactsLayersLabel);

				CheckBox showConfirmedContactsCheckBox = new CheckBox();
				showConfirmedContactsCheckBox.setId(Captions.dashboardShowConfirmedContacts);
				CheckBox showUnconfirmedContactsCheckBox = new CheckBox();
				showUnconfirmedContactsCheckBox.setId(Captions.dashboardShowUnconfirmedContacts);

				showContactsCheckBox = new CheckBox();
				showContactsCheckBox.setId(Captions.dashboardShowContacts);
				showContactsCheckBox.setCaption(I18nProperties.getCaption(Captions.dashboardShowContacts));
				showContactsCheckBox.setValue(showContacts);
				showContactsCheckBox.addValueChangeListener(e -> {
					showContacts = (boolean) e.getValue();
					showConfirmedContactsCheckBox.setEnabled(showContacts);
					showConfirmedContactsCheckBox.setValue(true);
					showUnconfirmedContactsCheckBox.setEnabled(showContacts);
					showUnconfirmedContactsCheckBox.setValue(true);
					loadContacts = true;
					refreshMap(true);
				});
				casesLayers.add(showContactsCheckBox);

				showConfirmedContactsCheckBox.setCaption(I18nProperties.getCaption(Captions.dashboardShowConfirmedContacts));
				showConfirmedContactsCheckBox.setValue(showConfirmedContacts);
				showConfirmedContactsCheckBox.addValueChangeListener(e -> {
					showConfirmedContacts = (boolean) e.getValue();
					loadContacts = true;
					refreshMap(true);
				});
				casesLayers.add(showConfirmedContactsCheckBox);

				CssStyles.style(showUnconfirmedContactsCheckBox, CssStyles.VSPACE_3);
				showUnconfirmedContactsCheckBox.setCaption(I18nProperties.getCaption(Captions.dashboardShowUnconfirmedContacts));
				showUnconfirmedContactsCheckBox.setValue(showUnconfirmedContacts);
				showUnconfirmedContactsCheckBox.addValueChangeListener(e -> {
					showUnconfirmedContacts = (boolean) e.getValue();
					loadContacts = true;
					refreshMap(true);
				});
				casesLayers.add(showUnconfirmedContactsCheckBox);

				showConfirmedContactsCheckBox.setEnabled(showContacts);
				showUnconfirmedContactsCheckBox.setEnabled(showContacts);
			}
		}

		//samples layers
		VerticalLayout samplesLayers = new VerticalLayout();
		{
			samplesLayers.setPadding(false);
			samplesLayers.setSpacing(false);

			Span sampleLayersLabel = new Span(I18nProperties.getString(Strings.entitySamples));
			CssStyles.style(sampleLayersLabel, CssStyles.H4, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_NONE);
			samplesLayers.add(sampleLayersLabel);

			if (UiUtil.permitted(UserRight.CASE_VIEW)) {
				showCaseSamplesCheckBox = new CheckBox();
				showCaseSamplesCheckBox.setId(Captions.sampleDashboardShowCaseSamples);
				showCaseSamplesCheckBox.setCaption(I18nProperties.getCaption(Captions.sampleDashboardShowCaseSamples));
				showCaseSamplesCheckBox.setValue(shouldShowCaseSamples());
				showCaseSamplesCheckBox.addValueChangeListener(e -> {
					if ((boolean) e.getValue()) {
						displayedHumanSamples.add(SampleAssociationType.CASE);
					} else {
						displayedHumanSamples.remove(SampleAssociationType.CASE);
					}

					loadSamples = true;
					refreshMap(true);
				});
				samplesLayers.add(showCaseSamplesCheckBox);
			}

			if (UiUtil.permitted(UserRight.CONTACT_VIEW)) {
				showContactSamplesCheckBox = new CheckBox();
				showContactSamplesCheckBox.setId(Captions.sampleDashboardShowContactSamples);
				showContactSamplesCheckBox.setCaption(I18nProperties.getCaption(Captions.sampleDashboardShowContactSamples));
				showContactSamplesCheckBox.setValue(shouldShowContactSamples());
				showContactSamplesCheckBox.addValueChangeListener(e -> {
					if ((boolean) e.getValue()) {
						displayedHumanSamples.add(SampleAssociationType.CONTACT);
					} else {
						displayedHumanSamples.remove(SampleAssociationType.CONTACT);
					}

					loadSamples = true;
					refreshMap(true);
				});
				samplesLayers.add(showContactSamplesCheckBox);
			}

			if (UiUtil.permitted(UserRight.EVENTPARTICIPANT_VIEW)) {
				showEventParticipantSamplesCheckBox = new CheckBox();
				showEventParticipantSamplesCheckBox.setId(Captions.sampleDashboardShowEventParticipantSamples);
				showEventParticipantSamplesCheckBox.setCaption(I18nProperties.getCaption(Captions.sampleDashboardShowEventParticipantSamples));
				showEventParticipantSamplesCheckBox.setValue(shouldShowEventParticipantSamples());
				showEventParticipantSamplesCheckBox.addValueChangeListener(e -> {
					if ((boolean) e.getValue()) {
						displayedHumanSamples.add(SampleAssociationType.EVENT_PARTICIPANT);
					} else {
						displayedHumanSamples.remove(SampleAssociationType.EVENT_PARTICIPANT);
					}

					loadSamples = true;
					refreshMap(true);
				});
				samplesLayers.add(showEventParticipantSamplesCheckBox);
			}

			if (UiUtil.permitted(UserRight.ENVIRONMENT_VIEW)) {
				showEnvironmentSamplesCheckBox = new CheckBox();
				showEnvironmentSamplesCheckBox.setId(Captions.sampleDashboardShowEnvironmentSamples);
				showEnvironmentSamplesCheckBox.setCaption(I18nProperties.getCaption(Captions.sampleDashboardShowEnvironmentSamples));
				showEnvironmentSamplesCheckBox.setValue(showEnvironmentalSamples);
				showEnvironmentSamplesCheckBox.addValueChangeListener(e -> {
					if ((boolean) e.getValue()) {
						showEnvironmentalSamples = true;
					} else {
						showEnvironmentalSamples = false;
					}

					loadSamples = true;
					refreshMap(true);
				});
				samplesLayers.add(showEnvironmentSamplesCheckBox);
			}
		}

		//aefi layers
		{
			if (UiUtil.permitted(UserRight.ADVERSE_EVENTS_FOLLOWING_IMMUNIZATION_VIEW)) {
				Span aefiLayersLabel = new Span(I18nProperties.getString(Strings.entityAdverseEvent));
				CssStyles.style(aefiLayersLabel, CssStyles.H4, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_3);
				samplesLayers.add(aefiLayersLabel);

				showSeriousAefiCheckBox = new CheckBox();
				showSeriousAefiCheckBox.setId(Captions.aefiDashboardShowSeriousAefi);
				showSeriousAefiCheckBox.setCaption(I18nProperties.getCaption(Captions.aefiDashboardShowSeriousAefi));
				showSeriousAefiCheckBox.setValue(false);
				showSeriousAefiCheckBox.addValueChangeListener(e -> {
					loadAefi = true;
					refreshMap(true);
				});
				samplesLayers.add(showSeriousAefiCheckBox);

				showNonSeriousAefiCheckBox = new CheckBox();
				showNonSeriousAefiCheckBox.setId(Captions.aefiDashboardShowNonSeriousAefi);
				showNonSeriousAefiCheckBox.setCaption(I18nProperties.getCaption(Captions.aefiDashboardShowNonSeriousAefi));
				showNonSeriousAefiCheckBox.setValue(false);
				showNonSeriousAefiCheckBox.addValueChangeListener(e -> {
					loadAefi = true;
					refreshMap(true);
				});
				samplesLayers.add(showNonSeriousAefiCheckBox);
			}
		}

		//other layers
		{
			Span otherLayersLabel = new Span(I18nProperties.getCaption(Captions.dashboardOtherLabel));
			CssStyles.style(otherLayersLabel, CssStyles.H4, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_3);
			samplesLayers.add(otherLayersLabel);

			if (UiUtil.permitted(UserRight.EVENT_VIEW)) {
				showEventsCheckBox = new CheckBox();
				showEventsCheckBox.setId(Captions.dashboardShowEvents);
				CssStyles.style(showEventsCheckBox, CssStyles.VSPACE_3);
				showEventsCheckBox.setCaption(I18nProperties.getCaption(Captions.dashboardShowEvents));
				showEventsCheckBox.setValue(showEvents);
				showEventsCheckBox.addValueChangeListener(e -> {
					showEvents = (boolean) e.getValue();
					loadEvents = true;
					refreshMap(true);
				});
				samplesLayers.add(showEventsCheckBox);
				showEventsCheckBox.setVisible(true);
			}

			if (UiUtil.hasNationJurisdictionLevel() && UiUtil.permitted(UserRight.CASE_VIEW)) {
				OptionGroup regionMapVisualizationSelect = new OptionGroup();
				regionMapVisualizationSelect.setWidth(100, Unit.PERCENTAGE);
				regionMapVisualizationSelect.addItems((Object[]) CaseMeasure.values());
				regionMapVisualizationSelect.setValue(caseMeasure);
				regionMapVisualizationSelect.addValueChangeListener(event -> {
					caseMeasure = (CaseMeasure) event.getValue();
					loadCases = true;
					refreshMap(true);
				});

				HorizontalLayout showRegionsLayout = new HorizontalLayout();
				{
					showRegionsLayout.setPadding(false);
					showRegionsLayout.setSpacing(false);
					CheckBox showRegionsCheckBox = new CheckBox();
					showRegionsCheckBox.setId(Captions.dashboardShowRegions);
					showRegionsCheckBox.setCaption(I18nProperties.getCaption(Captions.dashboardShowRegions));
					showRegionsCheckBox.setValue(showRegions);
					showRegionsCheckBox.addValueChangeListener(e -> {
						showRegions = (boolean) e.getValue();
						regionMapVisualizationSelect.setEnabled(showRegions);
						regionMapVisualizationSelect.setValue(caseMeasure);
						refreshMap(true);
					});
					showRegionsLayout.add(showRegionsCheckBox);

					Span infoLabel = new Span(VaadinIcon.INFO_CIRCLE.create());
					infoLabel.setTitle(I18nProperties.getString(Strings.infoCaseIncidence));
					CssStyles.style(infoLabel, CssStyles.LABEL_MEDIUM, CssStyles.LABEL_SECONDARY, CssStyles.HSPACE_LEFT_3);
					infoLabel.setHeight(null);
					showRegionsLayout.add(infoLabel);
					showRegionsLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, infoLabel);
				}
				samplesLayers.add(showRegionsLayout);
				samplesLayers.add(regionMapVisualizationSelect);
				regionMapVisualizationSelect.setEnabled(showRegions);
			}

			CheckBox hideOtherCountriesCheckBox = new CheckBox();
			hideOtherCountriesCheckBox.setId(Captions.dashboardHideOtherCountries);
			hideOtherCountriesCheckBox.setCaption(I18nProperties.getCaption(Captions.dashboardHideOtherCountries));
			hideOtherCountriesCheckBox.setValue(hideOtherCountries);
			hideOtherCountriesCheckBox.addValueChangeListener(e -> {
				hideOtherCountries = (boolean) e.getValue();
				refreshMap(true);
			});
			CssStyles.style(hideOtherCountriesCheckBox, CssStyles.VSPACE_3);
			samplesLayers.add(hideOtherCountriesCheckBox);

			CheckBox showCurrentEpiSituationCB = new CheckBox();
			showCurrentEpiSituationCB.setId(Captions.dashboardMapShowEpiSituation);
			showCurrentEpiSituationCB.setCaption(I18nProperties.getCaption(Captions.dashboardMapShowEpiSituation));
			showCurrentEpiSituationCB.setValue(false);
			showCurrentEpiSituationCB.addValueChangeListener(e -> {
				showCurrentEpiSituation = (boolean) e.getValue();
				refreshMap(true);
			});
			samplesLayers.add(showCurrentEpiSituationCB);
		}

		mainLayersLayout.add(casesLayers);
		mainLayersLayout.add(samplesLayers);

		layersLayout.add(mainLayersLayout);
	}

	@Override
	protected List<Component> getLegendComponents() {
		List<Component> legendComponents = new ArrayList<>();

		// Cases
		{
			{
				Span facilitiesKeyLabel = new Span(I18nProperties.getCaption(Captions.dashboardFacilities));
				CssStyles.style(facilitiesKeyLabel, CssStyles.H4, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_NONE);
				legendComponents.add(facilitiesKeyLabel);

				HorizontalLayout facilitiesKeyLayout = new HorizontalLayout();
				{
					facilitiesKeyLayout.setSpacing(false);
					facilitiesKeyLayout.setPadding(false);
					HorizontalLayout legendEntry =
						buildMarkerLegendEntry(MarkerIcon.FACILITY_UNCLASSIFIED, I18nProperties.getCaption(Captions.dashboardNotYetClassifiedOnly));
					CssStyles.style(legendEntry, CssStyles.HSPACE_RIGHT_3);
					facilitiesKeyLayout.add(legendEntry);
					legendEntry = buildMarkerLegendEntry(MarkerIcon.FACILITY_SUSPECT, I18nProperties.getCaption(Captions.dashboardGt1SuspectCases));
					CssStyles.style(legendEntry, CssStyles.HSPACE_RIGHT_3);
					facilitiesKeyLayout.add(legendEntry);
					legendEntry = buildMarkerLegendEntry(MarkerIcon.FACILITY_PROBABLE, I18nProperties.getCaption(Captions.dashboardGt1ProbableCases));
					CssStyles.style(legendEntry, CssStyles.HSPACE_RIGHT_3);
					facilitiesKeyLayout.add(legendEntry);
					legendEntry =
						buildMarkerLegendEntry(MarkerIcon.FACILITY_CONFIRMED, I18nProperties.getCaption(Captions.dashboardGt1ConfirmedCases));
					facilitiesKeyLayout.add(legendEntry);
				}
				legendComponents.add(facilitiesKeyLayout);
			}

			{
				Span casesKeyLabel = new Span(I18nProperties.getString(Strings.entityCases));
				CssStyles.style(casesKeyLabel, CssStyles.H4, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_3);
				legendComponents.add(casesKeyLabel);

				HorizontalLayout casesKeyLayout = new HorizontalLayout();
				{
					casesKeyLayout.setSpacing(false);
					casesKeyLayout.setPadding(false);
					HorizontalLayout legendEntry =
						buildMarkerLegendEntry(MarkerIcon.CASE_UNCLASSIFIED, I18nProperties.getCaption(Captions.dashboardNotYetClassified));
					CssStyles.style(legendEntry, CssStyles.HSPACE_RIGHT_3);
					casesKeyLayout.add(legendEntry);
					legendEntry = buildMarkerLegendEntry(MarkerIcon.CASE_SUSPECT, I18nProperties.getCaption(Captions.dashboardSuspect));
					CssStyles.style(legendEntry, CssStyles.HSPACE_RIGHT_3);
					casesKeyLayout.add(legendEntry);
					legendEntry = buildMarkerLegendEntry(MarkerIcon.CASE_PROBABLE, I18nProperties.getCaption(Captions.dashboardProbable));
					CssStyles.style(legendEntry, CssStyles.HSPACE_RIGHT_3);
					casesKeyLayout.add(legendEntry);
					legendEntry = buildMarkerLegendEntry(MarkerIcon.CASE_CONFIRMED, I18nProperties.getCaption(Captions.dashboardConfirmed));
					casesKeyLayout.add(legendEntry);
				}
				legendComponents.add(casesKeyLayout);
			}
		}

		// Contacts
		{
			Span contactsKeyLabel = new Span(I18nProperties.getString(Strings.entityContacts));
			CssStyles.style(contactsKeyLabel, CssStyles.H4, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_3);
			legendComponents.add(contactsKeyLabel);

			HorizontalLayout contactsKeyLayout = new HorizontalLayout();
			{
				contactsKeyLayout.setSpacing(false);
				contactsKeyLayout.setPadding(false);
				HorizontalLayout legendEntry =
					buildMarkerLegendEntry(MarkerIcon.CONTACT_OK, I18nProperties.getCaption(Captions.dashboardNotAContact));
				CssStyles.style(legendEntry, CssStyles.HSPACE_RIGHT_3);
				contactsKeyLayout.add(legendEntry);
				legendEntry = buildMarkerLegendEntry(MarkerIcon.CONTACT_OVERDUE, I18nProperties.getCaption(Captions.dashboardUnconfirmedContact));
				CssStyles.style(legendEntry, CssStyles.HSPACE_RIGHT_3);
				contactsKeyLayout.add(legendEntry);
				legendEntry = buildMarkerLegendEntry(MarkerIcon.CONTACT_LONG_OVERDUE, I18nProperties.getCaption(Captions.dashboardConfirmedContact));
				contactsKeyLayout.add(legendEntry);
			}
			legendComponents.add(contactsKeyLayout);
		}

		// Districts
		{
			if (showRegions && districtValuesLowerQuartile != null && districtValuesMedian != null && districtValuesUpperQuartile != null) {
				Span districtsKeyLabel = new Span(I18nProperties.getString(Strings.entityDistricts));
				CssStyles.style(districtsKeyLabel, CssStyles.H4, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_3);
				legendComponents.add(districtsKeyLabel);
				legendComponents.add(
					buildRegionLegend(
						false,
						caseMeasure,
						emptyPopulationDistrictPresent,
						districtValuesLowerQuartile,
						districtValuesMedian,
						districtValuesUpperQuartile,
						InfrastructureHelper.CASE_INCIDENCE_DIVISOR));

				Span descLabel = new Span(I18nProperties.getString(Strings.infoDashboardIncidence));
				CssStyles.style(descLabel, CssStyles.LABEL_SMALL);
				legendComponents.add(descLabel);
			}
		}

		//samples
		{
			Span samplesKeyLabel = new Span(I18nProperties.getString(Strings.entitySample));
			CssStyles.style(samplesKeyLabel, CssStyles.H4, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_3);
			legendComponents.add(samplesKeyLabel);

			HorizontalLayout samplesLegendLayout = new HorizontalLayout();
			samplesLegendLayout.setSpacing(false);
			samplesLegendLayout.setPadding(false);

			HorizontalLayout caseSampleslegendEntry =
				buildMarkerLegendEntry(MarkerIcon.SAMPLE_CASE, I18nProperties.getCaption(Captions.sampleDashboardCaseSamples));
			CssStyles.style(caseSampleslegendEntry, CssStyles.HSPACE_RIGHT_3);
			samplesLegendLayout.add(caseSampleslegendEntry);

			HorizontalLayout contactSamplesLegendEntry =
				buildMarkerLegendEntry(MarkerIcon.SAMPLE_CONTACT, I18nProperties.getCaption(Captions.sampleDashboardContactSamples));
			CssStyles.style(contactSamplesLegendEntry, CssStyles.HSPACE_RIGHT_3);
			samplesLegendLayout.add(contactSamplesLegendEntry);

			HorizontalLayout eventParticipantSamplesLegendEntry = buildMarkerLegendEntry(
				MarkerIcon.SAMPLE_EVENT_PARTICIPANT,
				I18nProperties.getCaption(Captions.sampleDashboardEventParticipantSamples));
			CssStyles.style(eventParticipantSamplesLegendEntry, CssStyles.HSPACE_RIGHT_3);
			samplesLegendLayout.add(eventParticipantSamplesLegendEntry);

			HorizontalLayout environmentSamplesLegendEntry =
				buildMarkerLegendEntry(MarkerIcon.SAMPLE_ENVIRONMENT, I18nProperties.getCaption(Captions.sampleDashboardEnvironmentsSamples));
			samplesLegendLayout.add(environmentSamplesLegendEntry);

			legendComponents.add(samplesLegendLayout);
		}

		//aefi
		{
			Span aefiKeyLabel = new Span(I18nProperties.getString(Strings.entityAdverseEvent));
			CssStyles.style(aefiKeyLabel, CssStyles.H4, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_3);
			legendComponents.add(aefiKeyLabel);

			HorizontalLayout aefiLegendLayout = new HorizontalLayout();
			aefiLegendLayout.setSpacing(false);
			aefiLegendLayout.setPadding(false);

			HorizontalLayout seriousLegendEntry =
				buildMarkerLegendEntry(MarkerIcon.AEFI_SERIOUS, I18nProperties.getCaption(Captions.aefiDashboardSeriousAefi));
			CssStyles.style(seriousLegendEntry, CssStyles.HSPACE_RIGHT_3);
			aefiLegendLayout.add(seriousLegendEntry);

			HorizontalLayout nonSeriousLegendEntry =
				buildMarkerLegendEntry(MarkerIcon.AEFI_NON_SERIOUS, I18nProperties.getCaption(Captions.aefiDashboardNonSeriousAefi));
			CssStyles.style(nonSeriousLegendEntry, CssStyles.HSPACE_RIGHT_3);
			aefiLegendLayout.add(nonSeriousLegendEntry);

			legendComponents.add(aefiLegendLayout);
		}

		// Events
		{
			Span eventsKeyLabel = new Span(I18nProperties.getString(Strings.entityEvents));
			CssStyles.style(eventsKeyLabel, CssStyles.H4, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_3);
			legendComponents.add(eventsKeyLabel);

			HorizontalLayout eventsKeyLayout = new HorizontalLayout();
			{
				eventsKeyLayout.setSpacing(false);
				eventsKeyLayout.setPadding(false);
				HorizontalLayout legendEntry = buildMarkerLegendEntry(MarkerIcon.EVENT_RUMOR, EventStatus.SIGNAL.toString());
				CssStyles.style(legendEntry, CssStyles.HSPACE_RIGHT_3);
				eventsKeyLayout.add(legendEntry);
				legendEntry = buildMarkerLegendEntry(MarkerIcon.EVENT_OUTBREAK, EventStatus.EVENT.toString());
				eventsKeyLayout.add(legendEntry);
			}
			legendComponents.add(eventsKeyLayout);
		}

		return legendComponents;
	}

	public static HorizontalLayout buildMapIconLegendEntry(String iconName, String labelCaption) {
		Image icon = new Image("VAADIN/map/marker/" + iconName + ".png", "");
		icon.setWidth(12.375f, Unit.PIXELS);
		icon.setHeight(16.875f, Unit.PIXELS);
		return buildLegendEntry(icon, labelCaption);
	}

	/**
	 * @return a {@link VerticalLayout} when {@code vertical}, else a {@link HorizontalLayout} (see
	 *         {@code DashboardMapComponent.buildRegionLegend})
	 */
	@SuppressWarnings("unchecked")
	public static <L extends Component & FlexComponent> L buildRegionLegend(
		boolean vertical,
		CaseMeasure caseMeasure,
		boolean emptyPopulationDistrictPresent,
		BigDecimal districtShapesLowerQuartile,
		BigDecimal districtShapesMedian,
		BigDecimal districtShapesUpperQuartile,
		int caseIncidenceDivisor) {
		var regionLegendLayout = vertical ? new VerticalLayout() : new HorizontalLayout();
		regionLegendLayout.setSpacing(true);
		CssStyles.style(regionLegendLayout, CssStyles.LAYOUT_MINIMAL);
		regionLegendLayout.setSizeUndefined();

		HorizontalLayout legendEntry;
		switch (caseMeasure) {
		case CASE_COUNT:
			legendEntry = buildMapIconLegendEntry(
				"lowest-region-small",
				districtShapesLowerQuartile.compareTo(BigDecimal.ONE) > 0
					? "1 - " + districtShapesLowerQuartile + " " + I18nProperties.getString(Strings.entityCases)
					: "1 " + I18nProperties.getString(Strings.entityCase));
			break;
		case CASE_INCIDENCE:
			legendEntry = buildMapIconLegendEntry(
				"lowest-region-small",
				"<= " + DataHelper.getTruncatedBigDecimal(districtShapesLowerQuartile) + " " + I18nProperties.getString(Strings.entityCases) + " / "
					+ caseIncidenceDivisor);
			break;
		default:
			throw new IllegalArgumentException(caseMeasure.toString());
		}
		regionLegendLayout.add(legendEntry);

		if (districtShapesLowerQuartile.compareTo(districtShapesMedian) < 0) {
			switch (caseMeasure) {
			case CASE_COUNT:
				legendEntry = buildMapIconLegendEntry(
					"low-region-small",
					districtShapesMedian.compareTo(districtShapesLowerQuartile.add(BigDecimal.ONE)) > 0
						? districtShapesLowerQuartile.add(BigDecimal.ONE) + " - " + districtShapesMedian + " "
							+ I18nProperties.getString(Strings.entityCases)
						: districtShapesMedian + " " + I18nProperties.getString(Strings.entityCases));
				break;
			case CASE_INCIDENCE:
				legendEntry = buildMapIconLegendEntry(
					"low-region-small",
					DataHelper.getTruncatedBigDecimal(districtShapesLowerQuartile.add(new BigDecimal(0.1)).setScale(1, RoundingMode.HALF_UP)) + " - "
						+ DataHelper.getTruncatedBigDecimal(districtShapesMedian) + " " + I18nProperties.getString(Strings.entityCases) + " / "
						+ caseIncidenceDivisor);
				break;
			default:
				throw new IllegalArgumentException(caseMeasure.toString());
			}

			regionLegendLayout.add(legendEntry);
		}

		if (districtShapesMedian.compareTo(districtShapesUpperQuartile) < 0) {
			switch (caseMeasure) {
			case CASE_COUNT:
				legendEntry = buildMapIconLegendEntry(
					"high-region-small",
					districtShapesUpperQuartile.compareTo(districtShapesMedian.add(BigDecimal.ONE)) > 0
						? districtShapesMedian.add(BigDecimal.ONE) + " - " + districtShapesUpperQuartile + " "
							+ I18nProperties.getString(Strings.entityCases)
						: districtShapesUpperQuartile + " " + I18nProperties.getString(Strings.entityCases));
				break;
			case CASE_INCIDENCE:
				legendEntry = buildMapIconLegendEntry(
					"high-region-small",
					DataHelper.getTruncatedBigDecimal(districtShapesMedian.add(new BigDecimal(0.1)).setScale(1, RoundingMode.HALF_UP)) + " - "
						+ DataHelper.getTruncatedBigDecimal(districtShapesUpperQuartile) + " " + I18nProperties.getString(Strings.entityCases) + " / "
						+ caseIncidenceDivisor);
				break;
			default:
				throw new IllegalArgumentException(caseMeasure.toString());
			}

			regionLegendLayout.add(legendEntry);
		}

		switch (caseMeasure) {
		case CASE_COUNT:
			legendEntry = buildMapIconLegendEntry(
				"highest-region-small",
				"> " + districtShapesUpperQuartile + " " + I18nProperties.getString(Strings.entityCases));
			break;
		case CASE_INCIDENCE:
			legendEntry = buildMapIconLegendEntry(
				"highest-region-small",
				"> " + DataHelper.getTruncatedBigDecimal(districtShapesUpperQuartile) + " " + I18nProperties.getString(Strings.entityCases) + " / "
					+ caseIncidenceDivisor);
			break;
		default:
			throw new IllegalArgumentException(caseMeasure.toString());
		}
		regionLegendLayout.add(legendEntry);

		if (caseMeasure == CaseMeasure.CASE_INCIDENCE && emptyPopulationDistrictPresent) {
			legendEntry = buildMapIconLegendEntry("no-population-region-small", I18nProperties.getCaption(Captions.dashboardNoPopulationData));
			regionLegendLayout.add(legendEntry);
		}

		return (L) regionLegendLayout;
	}

	private void clearRegionShapes() {
		map.removeGroup(REGIONS_GROUP_ID);
		map.removeGroup(DISTRICTS_GROUP_ID);
		polygonRegions.clear();
		polygonDistricts.clear();

		emptyPopulationDistrictPresent = false;
		map.setTileLayerOpacity(1);
	}

	private void showRegionsShapes(CaseMeasure caseMeasure, Date fromDate, Date toDate, Disease disease) {
		clearRegionShapes();
		map.setTileLayerOpacity(0.5f);

		List<RegionReferenceDto> regions = FacadeProvider.getRegionFacade().getAllActiveByServerCountry();
		List<LeafletPolygon> regionPolygons = new ArrayList<LeafletPolygon>();

		// draw outlines of all regions
		for (RegionReferenceDto region : regions) {

			GeoLatLon[][] regionShape = FacadeProvider.getGeoShapeProvider().getRegionShape(region);
			if (regionShape == null) {
				continue;
			}

			for (GeoLatLon[] regionShapePart : regionShape) {
				LeafletPolygon polygon = new LeafletPolygon();
				polygon.setCaption(region.getCaption());
				// fillOpacity is used, so we can still hover the region
				polygon.setOptions("{\"weight\": 1, \"color\": '#444', \"fillOpacity\": 0.02}");
				polygon.setLatLons(regionShapePart);
				regionPolygons.add(polygon);
				polygonRegions.add(region);
			}
		}

		map.addPolygonGroup(REGIONS_GROUP_ID, regionPolygons);

		List<DataHelper.Pair<DistrictDto, BigDecimal>> measurePerDistrict =
			FacadeProvider.getCaseFacade().getCaseMeasurePerDistrict(fromDate, toDate, disease, caseMeasure);
		if (caseMeasure == CaseMeasure.CASE_COUNT) {
			districtValuesLowerQuartile =
				measurePerDistrict.size() > 0 ? measurePerDistrict.get((int) (measurePerDistrict.size() * 0.25)).getElement1() : null;
			districtValuesMedian =
				measurePerDistrict.size() > 0 ? measurePerDistrict.get((int) (measurePerDistrict.size() * 0.5)).getElement1() : null;
			districtValuesUpperQuartile =
				measurePerDistrict.size() > 0 ? measurePerDistrict.get((int) (measurePerDistrict.size() * 0.75)).getElement1() : null;
		} else {
			// For case incidence, districts without or with a population <= 0 should not be
			// used for the calculation of the quartiles because they will falsify the
			// result
			List<DataHelper.Pair<DistrictDto, BigDecimal>> measurePerDistrictWithoutMissingPopulations = new ArrayList<>();
			measurePerDistrictWithoutMissingPopulations.addAll(measurePerDistrict);
			measurePerDistrictWithoutMissingPopulations.removeIf(d -> d.getElement1() == null || d.getElement1().intValue() <= 0);
			districtValuesLowerQuartile = measurePerDistrictWithoutMissingPopulations.size() > 0
				? measurePerDistrictWithoutMissingPopulations.get((int) (measurePerDistrictWithoutMissingPopulations.size() * 0.25)).getElement1()
				: null;
			districtValuesMedian = measurePerDistrictWithoutMissingPopulations.size() > 0
				? measurePerDistrictWithoutMissingPopulations.get((int) (measurePerDistrictWithoutMissingPopulations.size() * 0.5)).getElement1()
				: null;
			districtValuesUpperQuartile = measurePerDistrictWithoutMissingPopulations.size() > 0
				? measurePerDistrictWithoutMissingPopulations.get((int) (measurePerDistrictWithoutMissingPopulations.size() * 0.75)).getElement1()
				: null;
		}

		List<LeafletPolygon> districtPolygons = new ArrayList<LeafletPolygon>();

		// Draw relevant district fills
		for (DataHelper.Pair<DistrictDto, BigDecimal> districtMeasure : measurePerDistrict) {

			DistrictDto district = districtMeasure.getElement0();
			DistrictReferenceDto districtRef = district.toReference();
			BigDecimal districtValue = districtMeasure.getElement1();
			GeoLatLon[][] districtShape = FacadeProvider.getGeoShapeProvider().getDistrictShape(districtRef);
			if (districtShape == null) {
				continue;
			}

			String fillColor;
			if (districtValue.compareTo(BigDecimal.ZERO) == 0) {
				fillColor = "#000";
			} else if (districtValue.compareTo(districtValuesLowerQuartile) < 0) {
				fillColor = "#FEDD6C";
			} else if (districtValue.compareTo(districtValuesMedian) < 0) {
				fillColor = "#FDBF44";
			} else if (districtValue.compareTo(districtValuesUpperQuartile) < 0) {
				fillColor = "#F47B20";
			} else {
				fillColor = "#ED1B24";
			}

			if (caseMeasure == CaseMeasure.CASE_INCIDENCE) {
				if (districtValue == null || districtValue.intValue() <= 0) {
					// grey when region has no population data
					emptyPopulationDistrictPresent = true;
					fillColor = "#999";
				}
			}

			for (GeoLatLon[] districtShapePart : districtShape) {
				LeafletPolygon polygon = new LeafletPolygon();
				polygon.setCaption(district.getName() + "<br>" + districtValue);
				polygon.setOptions("{\"stroke\": false, \"color\": '" + fillColor + "', \"fillOpacity\": 0.8}");
				polygon.setLatLons(districtShapePart);
				districtPolygons.add(polygon);
				polygonDistricts.add(districtRef);
			}
		}

		map.addPolygonGroup(DISTRICTS_GROUP_ID, districtPolygons);
	}

	private void clearCaseMarkers() {

		map.removeGroup(CASES_GROUP_ID);
		markerCaseFacilities.clear();
		casesByFacility.clear();
		mapCaseDtos.clear();
		mapAndFacilityCases.clear();
	}

	private void showCaseMarkers(List<MapCaseDto> cases) {

		clearCaseMarkers();

		fillCaseLists(cases);

		List<LeafletMarker> caseMarkers = new ArrayList<LeafletMarker>();

		for (FacilityReferenceDto facilityReference : casesByFacility.keySet()) {

			List<MapCaseDto> casesList = casesByFacility.get(facilityReference);
			// colorize the icon by the "strongest" classification type (order as in enum)
			// and set its size depending
			// on the number of cases
			int numberOfCases = casesList.size();
			Set<CaseClassification> classificationSet = new HashSet<>();
			for (MapCaseDto caze : casesList) {
				classificationSet.add(caze.getCaseClassification());
			}

			MarkerIcon icon;
			if (classificationSet.contains(CaseClassification.CONFIRMED)) {
				icon = MarkerIcon.FACILITY_CONFIRMED;
			} else if (classificationSet.contains(CaseClassification.PROBABLE)) {
				icon = MarkerIcon.FACILITY_PROBABLE;
			} else if (classificationSet.contains(CaseClassification.SUSPECT)) {
				icon = MarkerIcon.FACILITY_SUSPECT;
			} else {
				icon = MarkerIcon.FACILITY_UNCLASSIFIED;
			}

			// create and place the marker
			markerCaseFacilities.add(facilityReference);

			MapCaseDto firstCase = casesList.get(0);
			LeafletMarker leafletMarker = new LeafletMarker();
			leafletMarker.setLatLon(firstCase.getHealthFacilityLat(), firstCase.getHealthFacilityLon());
			leafletMarker.setIcon(icon);
			leafletMarker.setMarkerCount(numberOfCases);
			caseMarkers.add(leafletMarker);
		}

		for (MapCaseDto caze : mapCaseDtos) {
			LeafletMarker marker = new LeafletMarker();
			CaseClassification caseClassification = caze.getCaseClassification();
			if (caseClassification == CaseClassification.CONFIRMED
				|| caseClassification == CaseClassification.CONFIRMED_NO_SYMPTOMS
				|| caseClassification == CaseClassification.CONFIRMED_UNKNOWN_SYMPTOMS) {
				marker.setIcon(MarkerIcon.CASE_CONFIRMED);
			} else if (caseClassification == CaseClassification.PROBABLE) {
				marker.setIcon(MarkerIcon.CASE_PROBABLE);
			} else if (caseClassification == CaseClassification.SUSPECT) {
				marker.setIcon(MarkerIcon.CASE_SUSPECT);
			} else {
				marker.setIcon(MarkerIcon.CASE_UNCLASSIFIED);
			}

			if (caze.getAddressLat() != null && caze.getAddressLon() != null) {
				marker.setLatLon(caze.getAddressLat(), caze.getAddressLon());
			} else {
				marker.setLatLon(caze.getReportLat(), caze.getReportLon());
			}

			caseMarkers.add(marker);
		}

		map.addMarkerGroup("cases", caseMarkers);
	}

	private void fillCaseLists(List<MapCaseDto> cases) {
		for (MapCaseDto caze : cases) {
			// these filters need to be used for the count too
			CaseClassification classification = caze.getCaseClassification();
			if (caseClassificationOption == MapCaseClassificationOption.CONFIRMED_CASES_ONLY && classification != CaseClassification.CONFIRMED)
				continue;
			boolean hasCaseGps =
				(caze.getAddressLat() != null && caze.getAddressLon() != null) || (caze.getReportLat() != null && caze.getReportLon() != null);
			boolean hasFacilityGps = caze.getHealthFacilityLat() != null && caze.getHealthFacilityLon() != null;

			if (mapCaseDisplayMode == MapCaseDisplayMode.CASE_ADDRESS) {
				if (!hasCaseGps) {
					continue;
				}
				mapCaseDtos.add(caze);
			} else {
				if (FacilityDto.NONE_FACILITY_UUID.equals(caze.getHealthFacilityUuid())
					|| FacilityDto.OTHER_FACILITY_UUID.equals(caze.getHealthFacilityUuid())
					|| !hasFacilityGps) {
					if (mapCaseDisplayMode == MapCaseDisplayMode.FACILITY_OR_CASE_ADDRESS) {
						if (!hasCaseGps) {
							continue;
						}
						mapCaseDtos.add(caze);
					} else {
						continue;
					}
				} else {
					FacilityReferenceDto facility = new FacilityReferenceDto();
					facility.setUuid(caze.getHealthFacilityUuid());
					if (casesByFacility.get(facility) == null) {
						casesByFacility.put(facility, new ArrayList<MapCaseDto>());
					}
					casesByFacility.get(facility).add(caze);
				}
			}

			mapAndFacilityCases.add(caze);
		}
	}

	private void clearContactMarkers() {
		map.removeGroup(CONTACTS_GROUP_ID);
		markerContacts.clear();
		mapContactDtos.clear();
	}

	private void showContactMarkers(List<MapContactDto> contacts) {

		clearContactMarkers();

		List<LeafletMarker> contactMarkers = new ArrayList<LeafletMarker>();

		for (MapContactDto contact : contacts) {

			// Don't show a marker for contacts that don't have geo coordinates
			if (contact.getAddressLat() == null || contact.getAddressLon() == null) {
				if (contact.getReportLat() == null || contact.getReportLon() == null) {
					continue;
				}
			}

			// Don't show a marker for contacts that are filtered out
			if (!showUnconfirmedContacts && contact.getContactClassification() == ContactClassification.UNCONFIRMED) {
				continue;
			}
			if (!showConfirmedContacts && contact.getContactClassification() != ContactClassification.UNCONFIRMED) {
				continue;
			}

			MarkerIcon icon;
			// #1274 Temporarily disabled because it severely impacts the performance of the Dashboard
			//			Date lastVisitDateTime = contact.getLastVisitDateTime();
			//			long currentTime = new Date().getTime();
			//			if (lastVisitDateTime != null) {
			//				// 1000 ms = 1 second; 3600 seconds = 1 hour
			//				if (currentTime - lastVisitDateTime.getTime() >= 1000 * 3600 * 48) {
			//					icon = MarkerIcon.CONTACT_LONG_OVERDUE;
			//				} else if (currentTime - lastVisitDateTime.getTime() >= 1000 * 3600 * 24) {
			//					icon = MarkerIcon.CONTACT_OVERDUE;
			//				} else {
			//					icon = MarkerIcon.CONTACT_OK;
			//				}
			//			} else {
			//				icon = MarkerIcon.CONTACT_LONG_OVERDUE;
			//			}
			switch (contact.getContactClassification()) {
			case CONFIRMED:
				icon = MarkerIcon.CONTACT_LONG_OVERDUE;
				break;
			case UNCONFIRMED:
				icon = MarkerIcon.CONTACT_OVERDUE;
				break;
			case NO_CONTACT:
				icon = MarkerIcon.CONTACT_OK;
				break;
			default:
				icon = MarkerIcon.CONTACT_OK;
				break;
			}

			LeafletMarker marker = new LeafletMarker();
			marker.setIcon(icon);
			if (contact.getAddressLat() != null && contact.getAddressLon() != null) {
				marker.setLatLon(contact.getAddressLat(), contact.getAddressLon());
			} else {
				marker.setLatLon(contact.getReportLat(), contact.getReportLon());
			}
			markerContacts.add(contact);
			contactMarkers.add(marker);
		}
		map.addMarkerGroup(CONTACTS_GROUP_ID, contactMarkers);
	}

	private void clearSampleMarkers() {
		map.removeGroup(SAMPLES_GROUP_ID);
	}

	private void showSampleMarkers() {
		SampleDashboardCriteria criteria = dashboardDataProvider.buildSampleDashboardCriteria();
		List<MapSampleDto> humanSamples = FacadeProvider.getSampleDashboardFacade().getSamplesForMap(criteria, displayedHumanSamples);
		List<MapSampleDto> environmentSamples =
			showEnvironmentalSamples ? FacadeProvider.getSampleDashboardFacade().getEnvironmentalSamplesForMap(criteria) : Collections.emptyList();

		List<LeafletMarker> markers = new ArrayList<>(environmentSamples.size() + environmentSamples.size());

		markers.addAll(humanSamples.stream().map(sample -> {
			LeafletMarker marker = new LeafletMarker();
			switch (sample.getAssociationType()) {
			case CASE:
				marker.setIcon(MarkerIcon.SAMPLE_CASE);
				break;
			case CONTACT:
				marker.setIcon(MarkerIcon.SAMPLE_CONTACT);
				break;
			default:
				marker.setIcon(MarkerIcon.SAMPLE_EVENT_PARTICIPANT);
			}
			marker.setLatLon(sample.getLatitude(), sample.getLongitude());

			return marker;
		}).collect(Collectors.toList()));

		markers.addAll(environmentSamples.stream().map(sample -> {
			LeafletMarker marker = new LeafletMarker();
			marker.setIcon(MarkerIcon.SAMPLE_ENVIRONMENT);
			marker.setLatLon(sample.getLatitude(), sample.getLongitude());

			return marker;
		}).collect(Collectors.toList()));

		map.addMarkerGroup(SAMPLES_GROUP_ID, markers);
	}

	private void clearAefiMarkers() {
		map.removeGroup(AEFI_GROUP_ID);
	}

	private void showAefiMarkers() {
		AefiDashboardCriteria criteria = dashboardDataProvider.buildAefiDashboardCriteria()
			.gisMapCriteria(true)
			.showSeriousAefiForMap(showSeriousAefiCheckBox.getValue())
			.showNonSeriousAefiForMap(showNonSeriousAefiCheckBox.getValue());
		List<MapAefiDto> aefiMapData = FacadeProvider.getAefiDashboardFacade().getAefiForMap(criteria);

		//temporary fix: remove data without coordinates
		//ideally do this in the backend code
		List<MapAefiDto> filteredAefiMapData = new ArrayList<>();
		for (MapAefiDto mapAefiDto : aefiMapData) {
			if (mapAefiDto.getLatitude() != null && mapAefiDto.getLongitude() != null) {
				filteredAefiMapData.add(mapAefiDto);
			}
		}

		List<LeafletMarker> markers = new ArrayList<>(aefiMapData.size());

		markers.addAll(filteredAefiMapData.stream().map(mapAefiDto -> {
			LeafletMarker marker = new LeafletMarker();
			switch (mapAefiDto.getAefiType()) {
			case SERIOUS:
				marker.setIcon(MarkerIcon.AEFI_SERIOUS);
				break;
			case NON_SERIOUS:
				marker.setIcon(MarkerIcon.AEFI_NON_SERIOUS);
				break;
			default:
				marker.setIcon(MarkerIcon.AEFI_UNCLASSIFIED);
				break;
			}
			marker.setLatLon(mapAefiDto.getLatitude(), mapAefiDto.getLongitude());

			return marker;
		}).collect(Collectors.toList()));

		map.addMarkerGroup(AEFI_GROUP_ID, markers);
	}

	private void clearEventMarkers() {
		map.removeGroup(EVENTS_GROUP_ID);
		markerEvents.clear();
	}

	private void showEventMarkers() {
		clearEventMarkers();

		DashboardCriteria eventDashboardCriteria = dashboardDataProvider.buildEventDashboardCriteria();
		List<DashboardEventDto> events = FacadeProvider.getDashboardFacade().getNewEvents(eventDashboardCriteria);

		List<LeafletMarker> eventMarkers = new ArrayList<LeafletMarker>();

		for (DashboardEventDto event : events) {
			MarkerIcon icon;
			switch (event.getEventStatus()) {
			case EVENT:
				icon = MarkerIcon.EVENT_OUTBREAK;
				break;
			case SIGNAL:
				icon = MarkerIcon.EVENT_RUMOR;
				break;
			default:
				continue;
			}

			// Because events are pulled from the dashboardDataProvider, we do not need to add additional filters for event dates here

			LeafletMarker marker = new LeafletMarker();
			if (event.getLocationLat() != null && event.getLocationLon() != null) {
				marker.setLatLon(event.getLocationLat(), event.getLocationLon());
			} else if (event.getReportLat() != null && event.getReportLon() != null) {
				marker.setLatLon(event.getReportLat(), event.getReportLon());
			} else if (event.getDistrict() != null) {
				GeoLatLon districtCenter = FacadeProvider.getGeoShapeProvider().getCenterOfDistrict(event.getDistrict());
				if (districtCenter != null) {
					marker.setLatLon(districtCenter.getLat(), districtCenter.getLon());
				} else {
					GeoLatLon countryCenter = FacadeProvider.getConfigFacade().getCountryCenter();
					marker.setLatLon(countryCenter.getLat(), countryCenter.getLon());
				}
			} else {
				continue;
			}

			marker.setIcon(icon);
			markerEvents.add(event);
			eventMarkers.add(marker);
		}

		map.addMarkerGroup(EVENTS_GROUP_ID, eventMarkers);
	}

	private boolean shouldShowCaseSamples() {
		return displayedHumanSamples.contains(SampleAssociationType.CASE);
	}

	private boolean shouldShowContactSamples() {
		return displayedHumanSamples.contains(SampleAssociationType.CONTACT);
	}

	private boolean shouldShowEventParticipantSamples() {
		return displayedHumanSamples.contains(SampleAssociationType.EVENT_PARTICIPANT);
	}

	private boolean shouldShowSeriousAefi() {
		return dashboardDataProvider.buildAefiDashboardCriteria().getAefiType() == AefiType.SERIOUS;
	}

	private boolean shouldShowNonSeriousAefi() {
		return dashboardDataProvider.buildAefiDashboardCriteria().getAefiType() == AefiType.NON_SERIOUS;
	}

	@Override
	protected void onMarkerClicked(String groupId, int markerIndex) {
		switch (groupId) {
		case CASES_GROUP_ID:
			if (markerIndex < markerCaseFacilities.size()) {
				FacilityReferenceDto facility = markerCaseFacilities.get(markerIndex);
				VerticalLayout layout = new VerticalLayout();
				Dialog window = VaadinUiUtil.showPopupWindow(layout);
				CasePopupGrid caseGrid = new CasePopupGrid(window, facility, GisDashboardMapComponent.this);
				caseGrid.setHeightMode(HeightMode.ROW);
				layout.add(caseGrid);
				layout.setPadding(true);
				FacilityDto facilityDto = FacadeProvider.getFacilityFacade().getByUuid(facility.getUuid());
				window.setHeaderTitle(I18nProperties.getCaption(Captions.dashboardCasesIn) + " " + facilityDto.buildCaption());
			} else {
				markerIndex -= markerCaseFacilities.size();
				MapCaseDto caze = mapCaseDtos.get(markerIndex);
				ControllerProvider.getCaseController().navigateToCase(caze.getUuid(), true);
			}
			break;
		case CONTACTS_GROUP_ID:
			MapContactDto contact = markerContacts.get(markerIndex);
			ControllerProvider.getContactController().navigateToData(contact.getUuid(), true);
			break;
		case EVENTS_GROUP_ID:
			DashboardEventDto event = markerEvents.get(markerIndex);
			ControllerProvider.getEventController().navigateToData(event.getUuid(), true);
			break;
		}
	}

	public List<CaseDataDto> getCasesForFacility(FacilityReferenceDto facility) {
		List<CaseDataDto> casesForFacility = new ArrayList<>();
		CaseFacade caseFacade = FacadeProvider.getCaseFacade();
		for (MapCaseDto mapCaseDto : casesByFacility.get(facility)) {
			casesForFacility.add(caseFacade.getCaseDataByUuid(mapCaseDto.getUuid()));
		}
		return casesForFacility;
	}

	protected HorizontalLayout getMapHeaderLayout() {
		return mapHeaderLayout;
	}

	protected HorizontalLayout getMapFooterLayout() {
		return mapFooterLayout;
	}
}
