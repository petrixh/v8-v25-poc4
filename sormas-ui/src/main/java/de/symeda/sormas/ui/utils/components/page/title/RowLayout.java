package de.symeda.sormas.ui.utils.components.page.title;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.ui.utils.CssStyles;

public class RowLayout extends HorizontalLayout {

	public void addToLayout(String text, String... styles) {
		if (StringUtils.isNotBlank(text)) {
			Span label = new Span(text);
			label.addClassNames(styles);
			label.addClassNames(CssStyles.H3, CssStyles.VSPACE_NONE, CssStyles.VSPACE_TOP_NONE);
			add(label);
		}
	}
}
