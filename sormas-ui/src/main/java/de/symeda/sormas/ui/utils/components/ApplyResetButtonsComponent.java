package de.symeda.sormas.ui.utils.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

public class ApplyResetButtonsComponent extends HorizontalLayout {

	private static final long serialVersionUID = 9117873923056534385L;

	private static final String FILTER_ITEM_STYLE = "filter-item";

	private final Button applyButton;
	private final Button resetButton;

	public ApplyResetButtonsComponent(String applyCaptionTag, String resetCaptionTag) {
		setSpacing(false);

		applyButton = ButtonHelper.createButton(applyCaptionTag, null, FILTER_ITEM_STYLE);
		applyButton.addClickShortcut(Key.ENTER);
		add(applyButton);

		resetButton = ButtonHelper.createButton(resetCaptionTag, null, FILTER_ITEM_STYLE);
		add(resetButton);
	}

	public void addApplyHandler(ComponentEventListener<ClickEvent<Button>> applyHandler) {
		applyButton.addClickListener(applyHandler);
	}

	public void addResetHandler(ComponentEventListener<ClickEvent<Button>> resetHandler) {
		resetButton.addClickListener(resetHandler);
	}

	public void style(String style) {
		CssStyles.style(style, applyButton, resetButton);
	}

	public Button getApplyButton() {
		return applyButton;
	}
}
