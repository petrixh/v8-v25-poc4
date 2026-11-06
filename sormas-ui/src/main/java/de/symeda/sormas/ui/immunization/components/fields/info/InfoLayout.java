package de.symeda.sormas.ui.immunization.components.fields.info;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class InfoLayout extends HorizontalLayout {

	public InfoLayout(String text) {
		setWidthFull();
		setSpacing(true);
		Image icon = new Image(VaadinUiUtil.THEME_IMAGE_PATH + "img/info-icon.png", "");
		icon.setHeight("25px");
		icon.setWidth("25px");
		add(icon);
		Span infoLabel = VaadinUiUtil.createLabel(text, ContentMode.HTML);
		add(infoLabel);
		setFlexGrow(1, infoLabel);
	}
}
