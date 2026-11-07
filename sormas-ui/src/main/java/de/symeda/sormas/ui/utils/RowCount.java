package de.symeda.sormas.ui.utils;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.i18n.I18nProperties;

public class RowCount extends HorizontalLayout {

	private Span labelValue;

	public RowCount(String labelCaption, int rowsCount) {
		createLayout(labelCaption, rowsCount);
	}

	private void createLayout(String caption, int rowsCount) {

		setPadding(false);
		addClassName(CssStyles.VSPACE_4);
		setSpacing(true);
		setWidthFull();
		setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

		Span labelCaption = new Span(I18nProperties.getString(caption) + ":");
		labelCaption.addClassNames(CssStyles.LABEL_BOLD, CssStyles.VSPACE_TOP_NONE, CssStyles.ALIGN_RIGHT);
		labelCaption.setWidthFull();
		add(labelCaption);
		setFlexGrow(1, labelCaption);

		labelValue = new Span(String.valueOf(rowsCount));
		labelValue.addClassNames(CssStyles.LABEL_BOLD, CssStyles.VSPACE_TOP_NONE, CssStyles.ALIGN_RIGHT);
		labelValue.setSizeUndefined();
		add(labelValue);
	}

	public void update(int rowsCount) {
		labelValue.setText(String.valueOf(rowsCount));
	}

}
