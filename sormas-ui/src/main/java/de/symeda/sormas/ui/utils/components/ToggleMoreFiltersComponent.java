package de.symeda.sormas.ui.utils.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.CustomLayout;

public class ToggleMoreFiltersComponent extends HorizontalLayout {

	private final CustomLayout moreFiltersLayout;
	private final String showMoreCaption;
	private final String showLessCaption;

	private final Button showHideMoreButton;

	public ToggleMoreFiltersComponent(CustomLayout moreFiltersLayout) {
		this.moreFiltersLayout = moreFiltersLayout;

		showMoreCaption = I18nProperties.getCaption(Captions.actionShowMoreFilters);
		showLessCaption = I18nProperties.getCaption(Captions.actionShowLessFilters);

		showHideMoreButton =
			ButtonHelper.createIconButtonWithCaption("showHideMoreFilters", buildShowHideMoreCaption(), VaadinIcon.CHEVRON_DOWN, e -> {
				Button showHideButton = e.getSource();
				if (moreFiltersLayout.isVisible()) {
					showHideButton.setText(showMoreCaption);
					ButtonHelper.setIcon(showHideButton, VaadinIcon.CHEVRON_DOWN);
					moreFiltersLayout.setVisible(false);
				} else {
					showHideButton.setText(showLessCaption);
					ButtonHelper.setIcon(showHideButton, VaadinIcon.CHEVRON_UP);
					moreFiltersLayout.setVisible(true);
				}
			}, CssStyles.BUTTON_BORDERLESS, CssStyles.VSPACE_TOP_NONE, CssStyles.LABEL_PRIMARY);

		add(showHideMoreButton);
	}

	public void toggleMoreFilters(boolean shouldShowMoreFilters) {
		moreFiltersLayout.setVisible(shouldShowMoreFilters);
		showHideMoreButton.setText(buildShowHideMoreCaption());
		ButtonHelper.setIcon(showHideMoreButton, buildShowHideMoreIcon());
	}

	private String buildShowHideMoreCaption() {
		return moreFiltersLayout.isVisible() ? showLessCaption : showMoreCaption;
	}

	private VaadinIcon buildShowHideMoreIcon() {
		return moreFiltersLayout.isVisible() ? VaadinIcon.CHEVRON_UP : VaadinIcon.CHEVRON_DOWN;
	}
}
