package de.symeda.sormas.ui.caze;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class CasesGuideLayout extends VerticalLayout {

	private static final long serialVersionUID = 8666113090299403477L;

	public CasesGuideLayout() {

		setPadding(true);
		setSpacing(false);

		Span lblHeadingTermsDefinition = new Span(I18nProperties.getString(Strings.headingExplanationOfTerms));
		CssStyles.style(lblHeadingTermsDefinition, CssStyles.H2);
		add(lblHeadingTermsDefinition);

		Span lblHeadingCompleteness = new Span(I18nProperties.getString(Strings.headingCompleteness));
		CssStyles.style(lblHeadingCompleteness, CssStyles.H3, CssStyles.VSPACE_TOP_5);
		add(lblHeadingCompleteness);
		Span lblCompletenessDescription = VaadinUiUtil.createLabel(I18nProperties.getString(Strings.infoCaseCompleteness), ContentMode.HTML);
		lblCompletenessDescription.setWidth(100, Unit.PERCENTAGE);
		add(lblCompletenessDescription);
	}
}
