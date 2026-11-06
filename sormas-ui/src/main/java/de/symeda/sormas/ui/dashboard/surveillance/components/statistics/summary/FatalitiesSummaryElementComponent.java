package de.symeda.sormas.ui.dashboard.surveillance.components.statistics.summary;

import java.util.List;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.dashboard.DashboardCaseDto;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

public class FatalitiesSummaryElementComponent extends HorizontalLayout {

	private final DiseaseSummaryElementComponent caseFatalityRateValue;
	private final DiseaseSummaryElementComponent caseFatalityCountValue;
	private final Span caseFatalityCountGrowth;

	public FatalitiesSummaryElementComponent() {
		setPadding(false);
		setSpacing(false);

		caseFatalityRateValue = new DiseaseSummaryElementComponent(Strings.headingCaseFatalityRate, "00.0%");
		add(caseFatalityRateValue);

		add(new DiseaseSummaryElementComponent(" ", " "));

		caseFatalityCountValue = new DiseaseSummaryElementComponent(Strings.headingFatalities, "0");

		caseFatalityCountGrowth = VaadinUiUtil.createLabel("", ContentMode.HTML);
		CssStyles.style(caseFatalityCountGrowth, CssStyles.VSPACE_TOP_5);
		caseFatalityCountValue.add(caseFatalityCountGrowth);

		add(caseFatalityCountValue);
		setFlexGrow(1, caseFatalityCountValue);
		// was MIDDLE_RIGHT in a slot with expand ratio 1
		setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, caseFatalityCountValue);
		caseFatalityCountValue.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
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
			chevronType = HtmlRenderer.iconHtml(VaadinIcon.CHEVRON_UP);
			criticalLevel = CssStyles.LABEL_CRITICAL;
		} else if (fatalCasesGrowth < 0) {
			chevronType = HtmlRenderer.iconHtml(VaadinIcon.CHEVRON_DOWN);
			criticalLevel = CssStyles.LABEL_POSITIVE;
		} else {
			chevronType = HtmlRenderer.iconHtml(VaadinIcon.CHEVRON_RIGHT);
			criticalLevel = CssStyles.LABEL_IMPORTANT;
		}

		VaadinUiUtil.setLabelValue(
			caseFatalityCountGrowth,
			"<div class=\"v-label v-widget " + criticalLevel + " v-label-" + criticalLevel
				+ " align-center v-label-align-center bold v-label-bold v-has-width\" " + "	  style=\"margin-top: 4px;margin-left: 5px;\">"
				+ "		<span class=\"v-icon\" style=\"font-family: VaadinIcons;\">" + chevronType + "		</span>" + "</div>",
			ContentMode.HTML);
	}
}
