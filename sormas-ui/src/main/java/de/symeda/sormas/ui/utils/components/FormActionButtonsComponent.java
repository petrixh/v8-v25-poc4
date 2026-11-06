package de.symeda.sormas.ui.utils.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.ui.utils.CustomLayout;

public class FormActionButtonsComponent extends HorizontalLayout {

	private static final long serialVersionUID = -1159701271006170763L;

	private ToggleMoreFiltersComponent showHideMoreButton;
	private final ApplyResetButtonsComponent applyResetButtonsComponent;

	public FormActionButtonsComponent(String applyCaptionTag, String resetCaptionTag, CustomLayout moreFiltersLayout) {
		if (moreFiltersLayout != null) {
			showHideMoreButton = new ToggleMoreFiltersComponent(moreFiltersLayout);
			add(showHideMoreButton);
		}

		applyResetButtonsComponent = new ApplyResetButtonsComponent(applyCaptionTag, resetCaptionTag);
		add(applyResetButtonsComponent);
	}

	public void addResetHandler(ComponentEventListener<ClickEvent<Button>> resetHandler) {
		applyResetButtonsComponent.addResetHandler(resetHandler);
	}

	public void addApplyHandler(ComponentEventListener<ClickEvent<Button>> applyHandler) {
		applyResetButtonsComponent.addApplyHandler(applyHandler);
	}

	public void toggleMoreFilters(boolean shouldShowMoreFilters) {
		showHideMoreButton.toggleMoreFilters(shouldShowMoreFilters);
	}

	public void style(String style) {
		applyResetButtonsComponent.style(style);
	}

	public ApplyResetButtonsComponent getApplyResetButtonsComponent() {
		return applyResetButtonsComponent;
	}
}
