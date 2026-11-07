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
package de.symeda.sormas.ui.dashboard.contacts;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.ConfigFacade;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.dashboard.AbstractDashboardView;
import de.symeda.sormas.ui.dashboard.DashboardCssStyles;
import de.symeda.sormas.ui.dashboard.DashboardDataProvider;
import de.symeda.sormas.ui.dashboard.DashboardType;
import de.symeda.sormas.ui.dashboard.contacts.components.ContactsFilterLayout;
import de.symeda.sormas.ui.dashboard.map.DashboardMapComponent;
import de.symeda.sormas.ui.dashboard.statistics.AbstractDashboardStatisticsComponent;
import de.symeda.sormas.ui.dashboard.visualisation.DashboardNetworkComponent;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

@SuppressWarnings("serial")
public class ContactsDashboardView extends AbstractDashboardView {

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/contacts";

	private static final int ROW_HEIGHT = 555;

	protected DashboardDataProvider dashboardDataProvider;
	protected ContactsFilterLayout filterLayout;

	protected AbstractDashboardStatisticsComponent statisticsComponent;
	protected ContactsEpiCurveComponent epiCurveComponent;
	protected DashboardMapComponent mapComponent;
	protected Optional<DashboardNetworkComponent> networkDiagramComponent;
	protected HorizontalLayout epiCurveAndMapLayout;
	protected HorizontalLayout networkDiagramRowLayout;
	protected HorizontalLayout caseStatisticsLayout;
	protected HorizontalLayout contactsStatisticsLayout;
	private VerticalLayout epiCurveLayout;
	private Optional<VerticalLayout> mapLayout;
	private Optional<VerticalLayout> networkDiagramLayout;
	private HorizontalLayout noNetworkDiagramLayout;

	private VerticalLayout rowsLayout;

	// Case counts
	private Span minLabel = new Span();
	private Span maxLabel = new Span();
	private Span avgLabel = new Span();
	private Span sourceCasesLabel = new Span();

	//Contacts in quarantine
	private Span contactsInQuarantineByDate = new Span();
	private Span contactsPlacedInQuarantineByDate = new Span();

	public ContactsDashboardView() {
		super(VIEW_NAME);

		dashboardDataProvider = new DashboardDataProvider();
		if (dashboardDataProvider.getDashboardType() == null) {
			dashboardDataProvider.setDashboardType(DashboardType.CONTACTS);
		}
		if (DashboardType.CONTACTS.equals(dashboardDataProvider.getDashboardType())) {
			dashboardDataProvider.setDisease(FacadeProvider.getDiseaseConfigurationFacade().getDefaultDisease());
		}

		filterLayout = new ContactsFilterLayout(this, dashboardDataProvider);
		dashboardLayout.add(filterLayout);

		dashboardSwitcher.setValue(DashboardType.CONTACTS);
		dashboardSwitcher.addValueChangeListener(e -> {
			dashboardDataProvider.setDashboardType((DashboardType) e.getValue());
			navigateToDashboardView(e);
		});

		rowsLayout = new VerticalLayout();
		rowsLayout.setPadding(false);
		rowsLayout.setSpacing(false);
		dashboardLayout.add(rowsLayout);
		dashboardLayout.setFlexGrow(1, rowsLayout);

		// Add statistics
		statisticsComponent = new ContactsDashboardStatisticsComponent(dashboardDataProvider);
		rowsLayout.add(statisticsComponent);
		rowsLayout.setFlexGrow(0, statisticsComponent);

		caseStatisticsLayout = createCaseStatisticsLayout();
		rowsLayout.add(caseStatisticsLayout);
		rowsLayout.setFlexGrow(0, caseStatisticsLayout);

		contactsStatisticsLayout = createContactsStatisticsLayout();
		rowsLayout.add(contactsStatisticsLayout);
		rowsLayout.setFlexGrow(0, contactsStatisticsLayout);

		epiCurveComponent = new ContactsEpiCurveComponent(dashboardDataProvider);
		mapComponent = new DashboardMapComponent(dashboardDataProvider);

		// Add epi curve and map
		epiCurveAndMapLayout = createEpiCurveAndMapLayout();
		rowsLayout.add(epiCurveAndMapLayout);

		// add network diagram
		if (UiUtil.permitted(UserRight.DASHBOARD_CONTACT_VIEW_TRANSMISSION_CHAINS)) {
			networkDiagramComponent = Optional.of(FacadeProvider.getConfigFacade())
				.map(ConfigFacade::getRScriptExecutable)
				.map(x -> new DashboardNetworkComponent(dashboardDataProvider));

			networkDiagramRowLayout = createNetworkDiagramRowLayout();
			rowsLayout.add(networkDiagramRowLayout);
		}
	}

	private HorizontalLayout createCaseStatisticsLayout() {
		HorizontalLayout layout = new HorizontalLayout();
		layout.addClassName(DashboardCssStyles.HIGHLIGHTED_STATISTICS_COMPONENT);
		layout.setWidth(100, Unit.PERCENTAGE);
		layout.setPadding(false);
		layout.setSpacing(false);

		HorizontalLayout contactsPerCaseLayout = createContactsPerCaseLayout();
		HorizontalLayout sourceCasesLayout = createSourceCasesLayout();
		layout.add(contactsPerCaseLayout);
		layout.add(sourceCasesLayout);
		layout.setFlexGrow(1, contactsPerCaseLayout);
		layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, sourceCasesLayout);

		return layout;
	}

	private HorizontalLayout createContactsPerCaseLayout() {
		HorizontalLayout layout = new HorizontalLayout();
		// was a left and right margin only (MarginInfo(false, true, false, true)): the layout margin at the sides
		layout.setPadding(false);
		layout.getStyle().set("padding-left", "var(--lumo-space-l)").set("padding-right", "var(--lumo-space-l)");
		layout.setSpacing(false);

		Span caption = new Span(I18nProperties.getString(Strings.headingContactsPerCase));
		CssStyles.style(caption, CssStyles.H3, CssStyles.HSPACE_RIGHT_1, CssStyles.VSPACE_TOP_NONE);
		layout.add(caption);

		CssStyles
			.style(minLabel, CssStyles.LABEL_PRIMARY, CssStyles.LABEL_LARGE_ALT, CssStyles.LABEL_BOLD, CssStyles.VSPACE_5, CssStyles.HSPACE_RIGHT_3);
		layout.add(minLabel);
		CssStyles
			.style(maxLabel, CssStyles.LABEL_PRIMARY, CssStyles.LABEL_LARGE_ALT, CssStyles.LABEL_BOLD, CssStyles.VSPACE_5, CssStyles.HSPACE_RIGHT_3);
		layout.add(maxLabel);
		CssStyles.style(avgLabel, CssStyles.LABEL_PRIMARY, CssStyles.LABEL_LARGE_ALT, CssStyles.LABEL_BOLD, CssStyles.VSPACE_5);
		layout.add(avgLabel);

		return layout;
	}

	private HorizontalLayout createSourceCasesLayout() {
		HorizontalLayout layout = new HorizontalLayout();
		// was a left and right margin only (MarginInfo(false, true, false, true)): the layout margin at the sides
		layout.setPadding(false);
		layout.getStyle().set("padding-left", "var(--lumo-space-l)").set("padding-right", "var(--lumo-space-l)");
		layout.setSpacing(false);

		Span caption = new Span(I18nProperties.getString(Strings.headingNewSourceCases));
		CssStyles.style(caption, CssStyles.H3, CssStyles.HSPACE_RIGHT_1, CssStyles.VSPACE_TOP_NONE);
		layout.add(caption);

		CssStyles.style(sourceCasesLabel, CssStyles.LABEL_PRIMARY, CssStyles.LABEL_LARGE_ALT, CssStyles.LABEL_BOLD, CssStyles.VSPACE_5);
		layout.add(sourceCasesLabel);

		return layout;
	}

	private HorizontalLayout createContactsStatisticsLayout() {

		HorizontalLayout layout = new HorizontalLayout();
		layout.addClassName(DashboardCssStyles.HIGHLIGHTED_STATISTICS_COMPONENT);
		layout.setWidth(100, Unit.PERCENTAGE);
		layout.setPadding(false);
		layout.setSpacing(false);

		HorizontalLayout contactsInQuarantine = createContactsInQuarantineLayout();
		layout.add(contactsInQuarantine);

		HorizontalLayout contactsPlacedInQuarantine = createContactsPlacedInQuarantineLayout();
		layout.add(contactsPlacedInQuarantine);

		// was MIDDLE_RIGHT in the second of two equal slots: at the right end
		layout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, contactsPlacedInQuarantine);

		return layout;
	}

	private HorizontalLayout createContactsInQuarantineLayout() {

		HorizontalLayout layout = new HorizontalLayout();
		// was a left and right margin only (MarginInfo(false, true, false, true)): the layout margin at the sides
		layout.setPadding(false);
		layout.getStyle().set("padding-left", "var(--lumo-space-l)").set("padding-right", "var(--lumo-space-l)");
		layout.setSpacing(false);

		Span captionInQuarantine = new Span(I18nProperties.getString(Strings.headingContactsInQuarantine));
		CssStyles.style(captionInQuarantine, CssStyles.H3, CssStyles.HSPACE_RIGHT_1, CssStyles.VSPACE_TOP_NONE);
		layout.add(captionInQuarantine);

		CssStyles.style(
			contactsInQuarantineByDate,
			CssStyles.LABEL_PRIMARY,
			CssStyles.LABEL_LARGE_ALT,
			CssStyles.LABEL_BOLD,
			CssStyles.VSPACE_5,
			CssStyles.HSPACE_RIGHT_3);
		layout.add(contactsInQuarantineByDate);

		return layout;
	}

	private HorizontalLayout createContactsPlacedInQuarantineLayout() {

		HorizontalLayout layout = new HorizontalLayout();
		// was a left and right margin only (MarginInfo(false, true, false, true)): the layout margin at the sides
		layout.setPadding(false);
		layout.getStyle().set("padding-left", "var(--lumo-space-l)").set("padding-right", "var(--lumo-space-l)");
		layout.setSpacing(false);

		Span captionPlacedInQuarantine = new Span(I18nProperties.getString(Strings.headingContactsPlacedInQuarantine));
		CssStyles.style(captionPlacedInQuarantine, CssStyles.H3, CssStyles.HSPACE_RIGHT_1, CssStyles.VSPACE_TOP_NONE);
		layout.add(captionPlacedInQuarantine);

		CssStyles.style(
			contactsPlacedInQuarantineByDate,
			CssStyles.LABEL_PRIMARY,
			CssStyles.LABEL_LARGE_ALT,
			CssStyles.LABEL_BOLD,
			CssStyles.VSPACE_5,
			CssStyles.HSPACE_RIGHT_3);
		layout.add(contactsPlacedInQuarantineByDate);

		return layout;
	}

	private void updateCaseCountsAndSourceCasesLabels() {
		List<Long> contactIds = dashboardDataProvider.getContacts().stream().map(dto -> dto.getId()).collect(Collectors.toList());
		int[] counts;
		if (contactIds.isEmpty()) {
			counts = new int[3];
		} else {
			counts = FacadeProvider.getContactFacade().getContactCountsByCasesForDashboard(contactIds);
		}

		int minContactCount = counts[0];
		int maxContactCount = counts[1];
		int avgContactCount = counts[2];

		minLabel.setText(I18nProperties.getString(Strings.min) + ": " + minContactCount);
		maxLabel.setText(I18nProperties.getString(Strings.max) + ": " + maxContactCount);
		avgLabel.setText(I18nProperties.getString(Strings.average) + ": " + avgContactCount);

		List<String> caseUuids = dashboardDataProvider.getCases().stream().map(dto -> dto.getUuid()).collect(Collectors.toList());
		int nonSourceCases = 0;
		if (!caseUuids.isEmpty()) {
			nonSourceCases = FacadeProvider.getContactFacade().getNonSourceCaseCountForDashboard(caseUuids);
		}

		int newSourceCases = caseUuids.size() - nonSourceCases;
		int newSourceCasesPercentage = newSourceCases == 0 ? 0 : (int) ((newSourceCases * 100.0f) / caseUuids.size());

		sourceCasesLabel.setText(newSourceCases + " (" + newSourceCasesPercentage + " %)");
	}

	private void updateContactsInQuarantineData() {

		contactsInQuarantineByDate.setText(dashboardDataProvider.getContactsInQuarantineCount().toString());

		contactsPlacedInQuarantineByDate.setText(dashboardDataProvider.getContactsPlacedInQuarantineCount().toString());
	}

	protected HorizontalLayout createEpiCurveAndMapLayout() {
		HorizontalLayout layout = new HorizontalLayout();
		layout.addClassName(DashboardCssStyles.CURVE_AND_MAP_LAYOUT);
		layout.setWidth(100, Unit.PERCENTAGE);
		layout.setPadding(false);
		layout.setSpacing(false);

		// Epi curve layout
		epiCurveLayout = createEpiCurveLayout();
		layout.add(epiCurveLayout);

		// Map layout
		mapLayout = createMapLayout();
		mapLayout.ifPresent(layout::add);

		return layout;
	}

	protected HorizontalLayout createNetworkDiagramRowLayout() {
		HorizontalLayout layout = new HorizontalLayout();
		//		layout.addClassName(DashboardCssStyles.CURVE_AND_MAP_LAYOUT);
		layout.setWidth(100, Unit.PERCENTAGE);
		layout.setPadding(false);
		layout.setSpacing(false);

		// network diagram layout 
		networkDiagramLayout = createNetworkDiagramLayout();
		networkDiagramLayout.ifPresent(layout::add);

		noNetworkDiagramLayout = new HorizontalLayout();
		noNetworkDiagramLayout.setPadding(true);
		Span noDiagramLabel = VaadinUiUtil
			.createLabel(HtmlRenderer.iconHtml(VaadinIcon.CLUSTER) + " " + I18nProperties.getString(Strings.infoNoNetworkDiagram), ContentMode.HTML);
		noNetworkDiagramLayout.add(noDiagramLabel);
		layout.add(noNetworkDiagramLayout);
		// was MIDDLE_CENTER
		layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
		layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, noNetworkDiagramLayout);
		noNetworkDiagramLayout.setVisible(false);

		networkDiagramLayout.ifPresent(l -> {
			l.setVisible(filterLayout.hasDiseaseSelected());
			noNetworkDiagramLayout.setVisible(!filterLayout.hasDiseaseSelected());
		});

		return layout;
	}

	protected VerticalLayout createEpiCurveLayout() {
		if (epiCurveComponent == null) {
			throw new UnsupportedOperationException("EpiCurveComponent needs to be initialized before calling createEpiCurveLayout");
		}

		VerticalLayout layout = new VerticalLayout();
		layout.setPadding(false);
		layout.setSpacing(false);
		layout.setHeight(ROW_HEIGHT, Unit.PIXELS);

		epiCurveComponent.setSizeFull();

		layout.add(epiCurveComponent);
		layout.setFlexGrow(1, epiCurveComponent);

		epiCurveComponent.setExpandListener(expanded -> {
			if (expanded) {
				rowsLayout.remove(statisticsComponent);
				mapLayout.ifPresent(epiCurveAndMapLayout::remove);
				ContactsDashboardView.this.setHeight(100, Unit.PERCENTAGE);
				epiCurveAndMapLayout.setHeight(100, Unit.PERCENTAGE);
				epiCurveLayout.setSizeFull();
				rowsLayout.setSizeFull();
			} else {
				rowsLayout.addComponentAtIndex(0, statisticsComponent);
				mapLayout.ifPresent(l -> {
					epiCurveAndMapLayout.addComponentAtIndex(1, l);
					mapComponent.refreshMap();
				});
				epiCurveLayout.setHeight(ROW_HEIGHT, Unit.PIXELS);
				ContactsDashboardView.this.setHeight(null);
				epiCurveAndMapLayout.setHeight(null);
				rowsLayout.setHeight(null);
			}
			caseStatisticsLayout.setVisible(!expanded);
			if (networkDiagramRowLayout != null) {
				networkDiagramRowLayout.setVisible(!expanded);
			}
			contactsStatisticsLayout.setVisible(!expanded);
		});

		return layout;
	}

	protected Optional<VerticalLayout> createMapLayout() {
		if (mapComponent == null) {
			throw new UnsupportedOperationException("MapComponent needs to be initialized before calling createMapLayout");
		}
		VerticalLayout layout = new VerticalLayout();
		layout.setPadding(false);
		layout.setSpacing(false);
		layout.setHeight(ROW_HEIGHT, Unit.PIXELS);

		mapComponent.setSizeFull();

		layout.add(mapComponent);
		layout.setFlexGrow(1, mapComponent);

		mapComponent.setExpandListener(expanded -> {

			if (expanded) {
				rowsLayout.remove(statisticsComponent);
				epiCurveAndMapLayout.remove(epiCurveLayout);
				ContactsDashboardView.this.setHeight(100, Unit.PERCENTAGE);
				epiCurveAndMapLayout.setHeight(100, Unit.PERCENTAGE);
				layout.setSizeFull();
				rowsLayout.setSizeFull();
			} else {
				rowsLayout.addComponentAtIndex(0, statisticsComponent);
				epiCurveAndMapLayout.addComponentAtIndex(0, epiCurveLayout);
				layout.setHeight(ROW_HEIGHT, Unit.PIXELS);
				ContactsDashboardView.this.setHeight(null);
				epiCurveAndMapLayout.setHeight(null);
				rowsLayout.setHeight(null);
			}
			caseStatisticsLayout.setVisible(!expanded);
			if (networkDiagramRowLayout != null) {
				networkDiagramRowLayout.setVisible(!expanded);
			}
			contactsStatisticsLayout.setVisible(!expanded);
		});

		return Optional.of(layout);
	}

	protected Optional<VerticalLayout> createNetworkDiagramLayout() {

		return networkDiagramComponent.map(ndc -> {

			VerticalLayout layout = new VerticalLayout();
			layout.setPadding(false);
			layout.setSpacing(false);
			layout.setHeight(null);

			ndc.setSizeFull();

			layout.add(ndc);
			layout.setFlexGrow(1, ndc);

			ndc.setExpandListener(expanded -> {
				if (expanded) {
					rowsLayout.remove(statisticsComponent);
					ContactsDashboardView.this.setHeight(100, Unit.PERCENTAGE);
					layout.setSizeFull();
					networkDiagramRowLayout.setHeight(100, Unit.PERCENTAGE);
					rowsLayout.setSizeFull();
				} else {
					rowsLayout.addComponentAtIndex(0, statisticsComponent);
					ContactsDashboardView.this.setHeight(null);
					layout.setHeight(null);
					networkDiagramRowLayout.setHeight(null);
					rowsLayout.setHeight(null);
				}
				caseStatisticsLayout.setVisible(!expanded);
				epiCurveAndMapLayout.setVisible(!expanded);
				if (!expanded) {
					mapLayout.ifPresent(l -> mapComponent.refreshMap());
				}
				contactsStatisticsLayout.setVisible(!expanded);
			});
			return layout;
		});
	}

	public void refreshDashboard() {

		dashboardDataProvider.refreshData();

		// Updates statistics
		statisticsComponent.updateStatistics(dashboardDataProvider.getDisease());
		updateCaseCountsAndSourceCasesLabels();

		updateContactsInQuarantineData();

		// Update cases and contacts shown on the map
		if (mapComponent != null) {
			mapComponent.refreshMap();
		}

		// Update cases and contacts shown on the map
		if (UiUtil.permitted(UserRight.DASHBOARD_CONTACT_VIEW_TRANSMISSION_CHAINS) && networkDiagramLayout.isPresent()) {
			boolean diseaseSelected = dashboardDataProvider.getDisease() != null;

			networkDiagramLayout.get().setVisible(diseaseSelected);
			noNetworkDiagramLayout.setVisible(!diseaseSelected);
			networkDiagramComponent.filter(c -> c.getParent().map(Component::isVisible).orElse(false)).ifPresent(DashboardNetworkComponent::refreshDiagram);
		}

		// Epi curve chart has to be created again due to a canvas resizing issue when
		// simply refreshing the component
		if (epiCurveComponent != null) {
			epiCurveComponent.clearAndFillEpiCurveChart();
		}
	}
}
