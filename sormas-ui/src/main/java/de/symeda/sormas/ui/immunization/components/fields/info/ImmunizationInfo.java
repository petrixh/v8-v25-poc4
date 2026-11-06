package de.symeda.sormas.ui.immunization.components.fields.info;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.ui.utils.CssStyles;

public class ImmunizationInfo extends VerticalLayout {

	public ImmunizationInfo(String heading) {
		setWidthFull();
		CssStyles.style(this, CssStyles.BACKGROUND_ROUNDED_CORNERS, CssStyles.BACKGROUND_SUB_CRITERIA, CssStyles.VSPACE_3, "v-scrollable");

		Span newCaseLabel = new Span(heading);
		CssStyles.style(newCaseLabel, CssStyles.LABEL_BOLD, CssStyles.VSPACE_4);
		add(newCaseLabel);
	}

}
