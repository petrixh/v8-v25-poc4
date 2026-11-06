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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinSession;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.UserProvider.HasUserProvider;
import de.symeda.sormas.ui.ViewModelProviders.HasViewModelProviders;
import de.symeda.sormas.ui.navigation.Navigator;

import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;

/**
 * Main UI class of the application that shows either the login screen or the
 * main view of the application depending on whether a user is signed in.
 *
 * The viewport meta tags (formerly the @Viewport annotation here) are set by
 * {@link SormasAppShell}.
 */
@SuppressWarnings("serial")
public class SormasUI extends UI implements HasUserProvider, HasViewModelProviders {

	private final UserProvider userProvider = new UserProvider();
	private final ViewModelProviders viewModelProviders = new ViewModelProviders();

	private Navigator navigator;
	private MainScreen mainScreen;

	@Override
	protected void init(VaadinRequest vaadinRequest) {

		VaadinSession.getCurrent().setErrorHandler(SormasErrorHandler.get());
		setLocale(vaadinRequest.getLocale());

		getPage().setTitle(FacadeProvider.getConfigFacade().getSormasInstanceName());

		initMainScreen();
	}

	/**
	 * Builds the main screen (menu and view registration) when the UI is created, as before: the routes of this
	 * session are registered before the first navigation. {@link de.symeda.sormas.ui.navigation.MainLayout} shows it.
	 */
	protected void initMainScreen() {
		mainScreen = new MainScreen(SormasUI.this);
	}

	public static SormasUI get() {
		return (SormasUI) UI.getCurrent();
	}

	public Navigator getNavigator() {
		return navigator;
	}

	public void setNavigator(Navigator navigator) {
		this.navigator = navigator;
	}

	public MainScreen getMainScreen() {
		return mainScreen;
	}

	@Override
	public UserProvider getUserProvider() {
		return userProvider;
	}

	@Override
	public ViewModelProviders getViewModelProviders() {
		return viewModelProviders;
	}

	@WebServlet(urlPatterns = {
		"/*" }, name = "SormasUIServlet", asyncSupported = true, initParams = @WebInitParam(name = InitParameters.UI_PARAMETER,
			value = "de.symeda.sormas.ui.SormasUI"))
	@ServletSecurity(@HttpConstraint(rolesAllowed = UserRight._SORMAS_UI))
	public static class SormasUIServlet extends VaadinServlet {

	}

	public static void refreshView() {
		get().getNavigator().navigateTo(get().getNavigator().getState());
	}
}
