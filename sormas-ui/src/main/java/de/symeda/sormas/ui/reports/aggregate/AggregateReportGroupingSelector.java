package de.symeda.sormas.ui.reports.aggregate;

import static de.symeda.sormas.api.report.AggregateReportGroupingLevel.DISTRICT;
import static de.symeda.sormas.api.report.AggregateReportGroupingLevel.HEALTH_FACILITY;
import static de.symeda.sormas.api.report.AggregateReportGroupingLevel.POINT_OF_ENTRY;
import static de.symeda.sormas.api.report.AggregateReportGroupingLevel.REGION;

import com.vaadin.flow.component.combobox.ComboBox;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.report.AggregateReportGroupingLevel;

public class AggregateReportGroupingSelector extends ComboBox<AggregateReportGroupingLevel> {

	public AggregateReportGroupingSelector() {
		setLabel(I18nProperties.getCaption(Captions.AggregateReport_grouping));
		setItems(REGION, DISTRICT, HEALTH_FACILITY, POINT_OF_ENTRY);
		// was setEmptySelectionAllowed(true): the empty item is the clear button
		setClearButtonVisible(true);
		setValue(REGION);
	}

}
