/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.ui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.dashboard.surveillance.SurveillanceDashboardView;
import de.symeda.sormas.ui.login.LoginHelper;
import de.symeda.sormas.ui.navigation.Navigator;
import de.symeda.sormas.ui.navigation.View;
import de.symeda.sormas.ui.user.UserSettingsForm;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

/**
 * Responsive navigation menu presenting a list of available views to the user.
 */
public class Menu extends Div {

	// Class names of the Vaadin 8 Valo menu (ValoTheme constants), kept for the theme work in a later stage
	private static final String VALO_MENU = "valo-menu";
	private static final String VALO_MENU_PART = "valo-menu-part";
	private static final String VALO_MENU_TITLE = "valo-menu-title";
	private static final String VALO_MENU_LOGO = "valo-menu-logo";
	private static final String VALO_MENU_ITEM = "valo-menu-item";
	private static final String VALO_BUTTON_LINK = "link";
	private static final String VALO_MENUITEMS = "valo-menuitems";
	private static final String VALO_MENU_TOGGLE = "valo-menu-toggle";
	private static final String VALO_MENU_VISIBLE = "valo-menu-visible";

	// was ThemeResource("img/sormas-logo.png") of the Vaadin 8 theme "sormas"
	private static final String SORMAS_LOGO = "VAADIN/themes/sormas/img/sormas-logo.png";

	private static final String BACKGROUND_COLOR_CONFIG_KEY = "MENU_BACKGROUND_COLOR";
	private static final String MENU_SUBTITLE_CONFIG_KEY = "MENU_SUBTITLE";

	private static final String DEFAULT_BACKGROUND_COLOR_NAME = "default";

	private static final String COLOR_BACKGROUND_STYLE_NAME = "color-background";
	private static final String MENU_SUBTITLE_STYLE_NAME = "menu-subtitle";
	private static final String TOP_MENU_CONTAINER_STYLE_NAME = "top-menu-container";
	private static final String HASH = "#";


	private final Navigator navigator;
	private final Map<String, Button> viewButtons = new HashMap<>();

	private final Div menuItemsLayout;
	private final Div menuPart;

	public Menu(Navigator navigator) {

		this.navigator = navigator;
		setClassName(VALO_MENU);

		defineCustomColorBackgroundIfSystemConfigured();

		menuPart = new Div();
		menuPart.addClassName(VALO_MENU_PART);

		// header of the menu
		VerticalLayout topContainer = new VerticalLayout();
		topContainer.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
		topContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
		CssStyles.style(topContainer, VALO_MENU_TITLE, TOP_MENU_CONTAINER_STYLE_NAME);
		topContainer.setSpacing(false);

		HorizontalLayout top = new HorizontalLayout();
		top.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		top.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
		top.setSpacing(true);

		Span title = new Span(FacadeProvider.getConfigFacade().getSormasInstanceName());
		title.setSizeUndefined();

		Image image;
		if (FacadeProvider.getConfigFacade().isCustomBranding()
			&& StringUtils.isNotBlank(FacadeProvider.getConfigFacade().getCustomBrandingLogoPath())) {
			Path logoPath = Paths.get(FacadeProvider.getConfigFacade().getCustomBrandingLogoPath());
			image = new Image(DownloadHandler.forFile(logoPath.toFile()), "");
		} else {
			image = new Image(SORMAS_LOGO, "");
		}
		CssStyles.style(image, VALO_MENU_LOGO, VALO_BUTTON_LINK);
		top.add(image);
		top.add(title);

		topContainer.addClickListener(listener -> SormasUI.get().getNavigator().navigateTo(SurveillanceDashboardView.VIEW_NAME));
		topContainer.add(top);

		defineMenuSubtitleIfSystemConfigured(topContainer);

		menuPart.add(topContainer);

		// button for toggling the visibility of the menu when on a small screen
		final Button showMenu = ButtonHelper.createIconButton(Captions.menu, VaadinIcon.MENU, event -> {
			if (menuPart.hasClassName(VALO_MENU_VISIBLE)) {
				menuPart.removeClassName(VALO_MENU_VISIBLE);
			} else {
				menuPart.addClassName(VALO_MENU_VISIBLE);
			}
		}, VALO_MENU_TOGGLE);
		showMenu.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		menuPart.add(showMenu);

		// container for the navigation buttons, which are added by addView()
		menuItemsLayout = new Div();
		menuItemsLayout.setClassName(VALO_MENUITEMS);
		menuPart.add(menuItemsLayout);

		// settings menu item
		MenuBar settingsMenu = new MenuBar();
		settingsMenu.setId(Captions.actionSettings);
		MenuItem settingsItem = settingsMenu.addItem(VaadinIcon.COG.create(), selectedItem -> showSettingsPopup());
		settingsItem.add(new Text(I18nProperties.getCaption(Captions.actionSettings)));

		settingsMenu.addClassNames("user-menu", "settings-menu");
		menuPart.add(settingsMenu);

		// logout menu item
		MenuBar logoutMenu = new MenuBar();
		logoutMenu.setId(Captions.actionLogout);
		MenuItem logoutItem = logoutMenu.addItem(VaadinIcon.SIGN_OUT.create(), selectedItem -> LoginHelper.logout());
		logoutItem.add(new Text(I18nProperties.getCaption(Captions.actionLogout) + " (" + UiUtil.getUserName() + ")"));

		logoutMenu.addClassNames("user-menu", "logout-menu");
		menuPart.add(logoutMenu);

		add(menuPart);
	}

	private void defineCustomColorBackgroundIfSystemConfigured() {
		String backgroundColor = FacadeProvider.getSystemConfigurationValueFacade().getValue(BACKGROUND_COLOR_CONFIG_KEY);

		if (StringUtils.isBlank(backgroundColor) || DEFAULT_BACKGROUND_COLOR_NAME.equals(backgroundColor)) {
			// no need to configure anything as we can keep the default color.
			return;
		}

		addClassName(COLOR_BACKGROUND_STYLE_NAME);

		String actualColorBackgroundColor = determineActualColor(backgroundColor);

		// Vaadin 8 added a page style rule for the menu; Flow sets the colour on the menu element itself
		getStyle().set("background-color", actualColorBackgroundColor);
	}

	private static void defineMenuSubtitleIfSystemConfigured(VerticalLayout topContainer) {
		String menuSubtitle = FacadeProvider.getSystemConfigurationValueFacade().getValue(MENU_SUBTITLE_CONFIG_KEY);

		if (StringUtils.isBlank(menuSubtitle)) {
			// no need to configure anything as no subtitled must be added
			return;
		}

		Span systemConfigurationLabel = new Span(menuSubtitle);
		systemConfigurationLabel.setSizeUndefined();
		CssStyles.style(systemConfigurationLabel, CssStyles.LABEL_UNDERLINE, MENU_SUBTITLE_STYLE_NAME);

		topContainer.add(systemConfigurationLabel);
	}
	private static @NotNull String determineActualColor(String backgroundColor) {
		String actualColorBackgroundColor;
		switch (backgroundColor) {
		case "green":
			actualColorBackgroundColor = "#108548";
			break;
		case "red":
			actualColorBackgroundColor = "#dd2b0e";
			break;
		case "indigo":
			actualColorBackgroundColor = "#7b58cf";
			break;
		case "gray":
			actualColorBackgroundColor = "#737278";
			break;
		default:
			actualColorBackgroundColor = addHashIfMissing(backgroundColor);
		}
		return actualColorBackgroundColor;
	}

	private static String addHashIfMissing(String backgroundColor) {
		if (!StringUtils.startsWith(backgroundColor, HASH)) {
			return HASH + backgroundColor;
		}
		return backgroundColor;
	}

	private void showSettingsPopup() {

		Dialog window = VaadinUiUtil.createPopupWindow();
		window.setHeaderTitle(I18nProperties.getString(Strings.headingUserSettings));
		window.setModal(true);

		CommitDiscardWrapperComponent<UserSettingsForm> component = ControllerProvider.getUserController().getUserSettingsComponent(window::close);

		window.add(component);
		window.open();
	}

	/**
	 * Register a view in the navigation menu and in the {@link Navigator} based
	 * on a view class.
	 *
	 * @see Navigator#addView(String, Class)
	 *
	 * @param viewClass
	 *            class of the views to create
	 * @param name
	 *            view name
	 * @param caption
	 *            view caption in the menu
	 * @param icon
	 *            view icon in the menu
	 */
	public void addView(Class<? extends View> viewClass, final String name, String caption, VaadinIcon icon) {

		navigator.addView(name, viewClass);
		createViewButton(name, caption, icon);
	}

	private void createViewButton(final String name, String caption, VaadinIcon icon) {

		Button button = ButtonHelper.createIconButtonWithCaption(name, caption, icon, event -> navigator.navigateTo(name));
		button.addClassName(VALO_MENU_ITEM);

		menuItemsLayout.add(button);
		viewButtons.put(name, button);
	}

	/**
	 * Highlights a view navigation button as the currently active view in the
	 * menu. This method does not perform the actual navigation.
	 *
	 * @param viewName
	 *            the name of the view to show as active
	 */
	public void setActiveView(String viewName) {

		for (Button button : viewButtons.values()) {
			button.removeClassName("selected");
		}

		Button selected = viewButtons.get(viewName);
		if (selected == null && viewName.contains("/")) {
			// might be a sub-view
			viewName = viewName.substring(0, viewName.indexOf('/'));
			selected = viewButtons.get(viewName);
		}

		if (selected != null) {
			selected.addClassName("selected");
		}

		menuPart.removeClassName(VALO_MENU_VISIBLE);
	}
}
