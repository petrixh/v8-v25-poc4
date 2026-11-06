package de.symeda.sormas.ui.dashboard.surveillance.components.statistics.summary;

import java.util.List;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.symeda.sormas.ui.utils.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;

import de.symeda.sormas.api.dashboard.DashboardCaseDto;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.utils.CssStyles;

public class FatalitiesSummaryElementComponent extends HorizontalLayout {

	private final DiseaseSummaryElementComponent caseFatalityRateValue;
	private final DiseaseSummaryElementComponent caseFatalityCountValue;
	private final Span caseFatalityCountGrowth;

	public FatalitiesSummaryElementComponent() {
		setPadding(false);
		setSpacing(false);

		caseFatalityRateValue = new DiseaseSummaryElementComponent(Strings.headingCaseFatalityRate, "00.0%");
		addComponent(caseFatalityRateValue);

		addComponent(new DiseaseSummaryElementComponent(" ", " "));

		caseFatalityCountValue = new DiseaseSummaryElementComponent(Strings.headingFatalities, "0");

		caseFatalityCountGrowth = new Span("", ContentMode.HTML);
		CssStyles.style(caseFatalityCountGrowth, CssStyles.VSPACE_TOP_5);
		caseFatalityCountValue.addComponent(caseFatalityCountGrowth);

		addComponent(caseFatalityCountValue);
		setFlexGrow(1, caseFatalityCountValue);
		setComponentAlignment(caseFatalityCountValue, Alignment.MIDDLE_RIGHT);
	}

	public void update(List<DashboardCaseDto> newCases, List<DashboardCaseDto> previousCases) {
		int casesCount = newCases.size();
		long fatalCasesCount = newCases.stream().filter(DashboardCaseDto::wasFatal).count();

		long previousFatalCasesCount = previousCases.stream().filter(DashboardCaseDto::wasFatal).count();
		long fatalCasesGrowth = fatalCasesCount - previousFatalCasesCount;
		float fatalityRate = 100 * ((float) fatalCasesCount / (float) (casesCount == 0 ? 1 : casesCount));
		fatalityRate = Math.round(fatalityRate * 100) / 100f;

		caseFatalityRateValue.updateTotalLabel(fatalityRate + "%");
		caseFatalityCountValue.updateTotalLabel(Long.toString(fatalCasesCount));

		String chevronType;
		String criticalLevel;

		if (fatalCasesGrowth > 0) {
			chevronType = VaadinIcon.CHEVRON_UP.getHtml();
			criticalLevel = CssStyles.LABEL_CRITICAL;
		} else if (fatalCasesGrowth < 0) {
			chevronType = VaadinIcon.CHEVRON_DOWN.getHtml();
			criticalLevel = CssStyles.LABEL_POSITIVE;
		} else {
			chevronType = VaadinIcon.CHEVRON_RIGHT.getHtml();
			criticalLevel = CssStyles.LABEL_IMPORTANT;
		}

		caseFatalityCountGrowth.setValue(
			"<div class=\"v-label v-widget " + criticalLevel + " v-label-" + criticalLevel
				+ " align-center v-label-align-center bold v-label-bold v-has-width\" " + "	  style=\"margin-top: 4px;margin-left: 5px;\">"
				+ "		<span class=\"v-icon\" style=\"font-family: VaadinIcons;\">" + chevronType + "		</span>" + "</div>");
	}
}
