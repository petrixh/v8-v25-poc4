package de.symeda.sormas.ui.dashboard.surveillance.components.statistics;

import de.symeda.sormas.ui.dashboard.components.DashboardHeadingComponent;
import de.symeda.sormas.ui.dashboard.statistics.DashboardStatisticsCountElement;
import de.symeda.sormas.ui.dashboard.statistics.DashboardStatisticsSubComponent;

public class DiseaseSectionStatisticsComponent extends DashboardStatisticsSubComponent {

	private final DashboardHeadingComponent heading;

	public DiseaseSectionStatisticsComponent(String titleCaption, String infoIconText) {
		heading = new DashboardHeadingComponent(titleCaption, infoIconText);
		add(heading);
	}

	public DiseaseSectionStatisticsComponent(String titleCaption, String description, String infoIconText) {
		this(titleCaption, infoIconText);
		heading.setTotalLabelDescription(description);
	}

	protected void updateTotalLabel(String value) {
		heading.updateTotalLabel(value);
	}

	public void hideHeading() {
		heading.setVisible(false);
	}

	protected void buildCountLayout(DashboardStatisticsCountElement... dashboardStatisticsCountElements) {
		// was CssLayout: the type follows DashboardStatisticsSubComponent.createCountLayout (1a-62)
		var countLayout = createCountLayout(true);
		for (DashboardStatisticsCountElement dashboardStatisticsCountElement : dashboardStatisticsCountElements) {
			addComponentToCountLayout(countLayout, dashboardStatisticsCountElement);
		}
		add(countLayout);
	}

	public DashboardHeadingComponent getHeading() {
		return heading;
	}
}
