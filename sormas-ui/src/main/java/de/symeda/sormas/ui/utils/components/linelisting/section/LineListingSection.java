package de.symeda.sormas.ui.utils.components.linelisting.section;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.utils.CssStyles;

public class LineListingSection extends VerticalLayout {

	public LineListingSection(String caption) {
		setPadding(false);
		setSpacing(false);

		Span sectionTitle = new Span();
		sectionTitle.setText(I18nProperties.getCaption(caption));
		sectionTitle.addClassName(CssStyles.H3);
		add(sectionTitle);
	}
}
