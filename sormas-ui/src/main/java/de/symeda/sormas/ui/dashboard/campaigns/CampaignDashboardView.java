package de.symeda.sormas.ui.dashboard.campaigns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;

import de.symeda.sormas.api.campaign.CampaignReferenceDto;
import de.symeda.sormas.api.campaign.diagram.CampaignDashboardElement;
import de.symeda.sormas.api.campaign.diagram.CampaignDiagramDataDto;
import de.symeda.sormas.api.campaign.diagram.CampaignDiagramDefinitionDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.SubMenu;
import de.symeda.sormas.ui.dashboard.AbstractDashboardView;
import de.symeda.sormas.ui.dashboard.DashboardType;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;
import de.symeda.sormas.ui.utils.fields.OptionGroup;

public class CampaignDashboardView extends AbstractDashboardView {

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/campaigns";

	public static final String GRID_CONTAINER = "grid-container";

	protected CampaignDashboardFilterLayout filterLayout;
	protected CampaignDashboardDataProvider dataProvider;

	private List<VerticalLayout> campaignDashboardTabComponents = new ArrayList<>();
	private List<String> campaignDashboardDiagramStyles = new ArrayList<>();
	private Component currentSubTabsWrapper;
	private Component currentDiagramsWrapper;

	private Map<CampaignReferenceDto, String> lastTabIdForCampaign = new HashMap<>();
	private Map<CampaignReferenceDto, Map<String, String>> lastSubTabIdForTabIdAndCampaign = new HashMap<>();

	public CampaignDashboardView() {
		super(VIEW_NAME);

		dataProvider = new CampaignDashboardDataProvider();
		filterLayout = new CampaignDashboardFilterLayout(this, dataProvider);
		dashboardLayout.add(filterLayout);
		dashboardLayout.setPadding(false);

		dashboardSwitcher.setValue(DashboardType.CAMPAIGNS);
		dashboardSwitcher.addValueChangeListener(e -> navigateToDashboardView(e));

		filterLayout.setInfoLabelText(I18nProperties.getString(Strings.infoCampaignsDashboard));
		dashboardLayout.setFlexGrow(0, filterLayout);
		dashboardLayout.setSizeFull();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void refreshDashboard() {

		final Page page = UI.getCurrent().getPage();
		cleanupDashboard(page);

		final VerticalLayout tabLayout = new VerticalLayout();
		tabLayout.setSizeFull();
		tabLayout.setPadding(false);
		tabLayout.setSpacing(false);
		campaignDashboardTabComponents.add(tabLayout);
		dashboardLayout.add(tabLayout);
		dashboardLayout.setFlexGrow(1, tabLayout);

		final OptionGroup tabSwitcher = new OptionGroup();
		tabSwitcher.setWidthFull();

		final VerticalLayout tabSwitcherLayout = new VerticalLayout(tabSwitcher);
		tabSwitcherLayout.setWidthFull();
		// was a left margin only (MarginInfo); spacing details belong to the theme work
		tabSwitcherLayout.setPadding(false);
		tabSwitcherLayout.setSpacing(false);
		tabLayout.add(tabSwitcherLayout);
		tabLayout.setFlexGrow(0, tabSwitcherLayout);

		final List<String> tabs = new ArrayList<>(dataProvider.getTabIds());
		tabs.forEach(tabId -> {
			tabSwitcher.addItem(tabId);
			tabSwitcher.setItemCaption(tabId, tabId);
		});
		if (!(tabs.size() > 1)) {
			tabSwitcherLayout.setVisible(false);
		}
		final String lastTabId = lastTabIdForCampaign.get(dataProvider.getCampaign());
		tabSwitcher.setValue(tabs.isEmpty() ? StringUtils.EMPTY : lastTabId != null ? lastTabId : tabs.get(0));

		CssStyles.style(
			tabSwitcher,
			CssStyles.FORCE_CAPTION,
			SormasFieldGroupFieldFactory.OPTIONGROUP_HORIZONTAL,
			CssStyles.OPTIONGROUP_HORIZONTAL_PRIMARY);

		final VerticalLayout subTabLayout = new VerticalLayout();
		subTabLayout.setSizeFull();
		subTabLayout.setPadding(false);
		subTabLayout.setSpacing(false);
		campaignDashboardTabComponents.add(subTabLayout);
		tabLayout.add(subTabLayout);
		tabLayout.setFlexGrow(1, subTabLayout);

		tabSwitcher.addValueChangeListener(e -> {
			final String tabId = (String) e.getValue();
			removeIfChild(subTabLayout, currentDiagramsWrapper);
			removeIfChild(subTabLayout, currentSubTabsWrapper);
			lastTabIdForCampaign.put(dataProvider.getCampaign(), tabId);
			refreshSubTabs(page, tabId, subTabLayout);
		});
		refreshSubTabs(page, (String) tabSwitcher.getValue(), subTabLayout);
	}

	@SuppressWarnings("deprecation")
	private void refreshSubTabs(Page page, String tabId, VerticalLayout subTabLayout) {

		final List<String> subTabs = new ArrayList<>(dataProvider.getSubTabIds(tabId));
		final SubMenu subTabSwitcher = new SubMenu();

		final VerticalLayout subTabSwitcherLayout = new VerticalLayout(subTabSwitcher);
		// was a left margin only (MarginInfo); spacing details belong to the theme work
		subTabSwitcherLayout.setPadding(false);
		subTabSwitcherLayout.setSpacing(false);
		subTabSwitcherLayout.setId("subTabsOf" + tabId);
		currentSubTabsWrapper = subTabSwitcherLayout;
		subTabLayout.add(subTabSwitcherLayout);
		subTabLayout.setFlexGrow(0, subTabSwitcherLayout);
		subTabSwitcherLayout.addClassNames("statistics-sublayout", CssStyles.VSPACE_3);

		subTabs.forEach(subTabId -> subTabSwitcher.addView(subTabId, subTabId, () -> {
			removeIfChild(subTabLayout, currentDiagramsWrapper);
			if (lastSubTabIdForTabIdAndCampaign.containsKey(dataProvider.getCampaign())) {
				lastSubTabIdForTabIdAndCampaign.get(dataProvider.getCampaign()).put(tabId, subTabId);
			} else {
				final HashMap<String, String> subTabMap = new HashMap<>();
				subTabMap.put(tabId, subTabId);
				lastSubTabIdForTabIdAndCampaign.put(dataProvider.getCampaign(), subTabMap);
			}
			refreshDiagrams(page, subTabLayout, tabId, subTabId);

			return true;
		}));
		if (!(subTabs.size() > 1)) {
			subTabSwitcherLayout.setVisible(false);
		}
		final Map<String, String> subTabMap = lastSubTabIdForTabIdAndCampaign.get(dataProvider.getCampaign());
		final String lastSubTabId = subTabMap != null ? subTabMap.get(tabId) : null;
		final String activeSubTab = subTabs.isEmpty() ? StringUtils.EMPTY : lastSubTabId != null ? lastSubTabId : subTabs.get(0);
		subTabSwitcher.setActiveView(activeSubTab);

		refreshDiagrams(page, subTabLayout, tabId, activeSubTab);
	}

	private void refreshDiagrams(Page page, VerticalLayout layout, String tabId, String subTabId) {
		Map<CampaignDashboardDiagramDto, List<CampaignDiagramDataDto>> campaignFormDataMap = dataProvider.getCampaignFormDataMap(tabId, subTabId);

		if (campaignFormDataMap != null && !campaignFormDataMap.isEmpty()) {
			final List<CampaignDashboardElement> dashboardElements =
				campaignFormDataMap.keySet().stream().map(CampaignDashboardDiagramDto::getCampaignDashboardElement).collect(Collectors.toList());

			final GridTemplateAreaCreator gridTemplateAreaCreator = new GridTemplateAreaCreator(dashboardElements);

			final VerticalLayout diagramsWrapper = new VerticalLayout();
			// was a left and right margin only (MarginInfo(false, true, false, true)): the layout margin at the sides
			diagramsWrapper.setPadding(false);
			diagramsWrapper.getStyle().set("padding-left", "var(--lumo-space-l)").set("padding-right", "var(--lumo-space-l)");
			diagramsWrapper.setId(tabId + "_" + subTabId);
			currentDiagramsWrapper = diagramsWrapper;

			diagramsWrapper.setWidth(
				dashboardElements.size() == 1 && gridTemplateAreaCreator.getGridColumns() == 1 ? gridTemplateAreaCreator.getWidthsSum() : 100,
				Unit.PERCENTAGE);
			diagramsWrapper.setHeight(gridTemplateAreaCreator.getGridContainerHeight(), Unit.PERCENTAGE);

			// was a CssLayout; the grid style below lays it out
			final Div diagramsLayout = new Div();
			diagramsLayout.setSizeFull();
			final String gridCssClass = (tabId + subTabId).replaceAll("[^a-zA-Z]+", "") + generateRandomString() + GRID_CONTAINER;

			addStyles(
				page,
				createDiagramGridStyle(
					gridCssClass,
					gridTemplateAreaCreator.getFormattedGridTemplate(),
					gridTemplateAreaCreator.getGridRows(),
					gridTemplateAreaCreator.getGridColumns()));
			diagramsLayout.setClassName(gridCssClass);

			campaignFormDataMap.forEach((campaignDashboardDiagramDto, diagramData) -> {
				final CampaignDiagramDefinitionDto campaignDiagramDefinitionDto = campaignDashboardDiagramDto.getCampaignDiagramDefinitionDto();
				final String diagramId = campaignDiagramDefinitionDto.getDiagramId();
				final String diagramCssClass = diagramId + generateRandomString();

				final CampaignDashboardDiagramComponent diagramComponent = new CampaignDashboardDiagramComponent(
					campaignDiagramDefinitionDto,
					diagramData,
					dataProvider.getCampaignFormTotalsMap(tabId, subTabId).get(campaignDashboardDiagramDto),
					dataProvider.getCampaignJurisdictionLevelGroupBy());
				addStyles(page, createDiagramStyle(diagramCssClass, diagramId));
				diagramComponent.setClassName(diagramCssClass);

				diagramsLayout.add(diagramComponent);
			});
			diagramsWrapper.add(diagramsLayout);

			diagramsWrapper.setVisible(true);
			layout.add(diagramsWrapper);
			layout.setFlexGrow(1, diagramsWrapper);
		}
	}

	private void cleanupDashboard(Page page) {
		campaignDashboardTabComponents
			.forEach(campaignDashboardDiagramComponents -> removeIfChild(dashboardLayout, campaignDashboardDiagramComponents));
		campaignDashboardTabComponents.clear();
		campaignDashboardDiagramStyles.forEach(s -> page.executeJs(removeStyles(s)));
		campaignDashboardDiagramStyles.clear();
	}

	/**
	 * Was {@code layout.removeComponent(component)}: Vaadin 8 ignored {@code null} and components of another parent, Flow's
	 * {@code remove} throws for both.
	 */
	private static void removeIfChild(VerticalLayout layout, Component component) {
		if (component != null && component.getParent().orElse(null) == layout) {
			layout.remove(component);
		}
	}

	/**
	 * Was {@code Page.getStyles().add(css)}: Flow has no API that adds style text to the page, so the browser adds a
	 * {@code style} element to the document head, as Vaadin 8 did. {@link #removeStyles(String)} removes it again.
	 */
	private static void addStyles(Page page, String css) {
		page.executeJs("const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);", css);
	}

	private String generateRandomString() {
		return UUID.randomUUID().toString().substring(0, 6);
	}

	private String createDiagramGridStyle(String gridCssClass, String gridAreasTemplate, int rows, int columns) {
		final String s = "." + gridCssClass;
		campaignDashboardDiagramStyles.add(s);
		return s + "{ display: grid; grid-gap:1%; grid-auto-columns: " + (100 / columns - 1) + "%; grid-auto-rows: " + (100 / rows - 1)
			+ "%; grid-template-areas:" + gridAreasTemplate + "; }";
	}

	private String createDiagramStyle(String diagramCssClass, String diagramId) {
		final String s = "." + diagramCssClass;
		campaignDashboardDiagramStyles.add(s);
		return s + "{ grid-area: " + diagramId + "; }";
	}

	private String removeStyles(String styleInnerText) {
		return "var diagramStyles = document.getElementsByTagName('style');  for (var i=0; i < diagramStyles.length; i++) {"
			+ "if (diagramStyles[i].innerText.startsWith(\"" + styleInnerText
			+ "\" )) { diagramStyles[i].parentNode.removeChild(diagramStyles[i]);}}";
	}
}
