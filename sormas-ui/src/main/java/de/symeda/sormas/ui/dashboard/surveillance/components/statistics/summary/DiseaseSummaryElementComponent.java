package de.symeda.sormas.ui.dashboard.surveillance.components.statistics.summary;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.utils.CssStyles;

public class DiseaseSummaryElementComponent extends HorizontalLayout {

	private final Span totalLabel;

	public DiseaseSummaryElementComponent(String heading) {
		setPadding(false);
		setSpacing(false);

		Span headingLabel = new Span(I18nProperties.getString(heading));
		CssStyles.style(headingLabel, CssStyles.LABEL_PRIMARY, CssStyles.LABEL_UPPERCASE, CssStyles.VSPACE_TOP_4);
		add(headingLabel);

		totalLabel = new Span();
		CssStyles.style(
			totalLabel,
			CssStyles.LABEL_PRIMARY,
			CssStyles.LABEL_CAPTION_TRUNCATED,
			CssStyles.LABEL_BOLD,
			CssStyles.LABEL_LARGE,
			CssStyles.HSPACE_LEFT_3,
			CssStyles.VSPACE_TOP_5);
		add(totalLabel);
	}

	public DiseaseSummaryElementComponent(String heading, String defaultValue) {
		this(heading);
		updateTotalLabel(defaultValue);
	}

	public void updateTotalLabel(String value) {
		totalLabel.setText(value);
	}
}
