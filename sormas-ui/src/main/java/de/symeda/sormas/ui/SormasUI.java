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

import java.io.Serializable;
import java.util.concurrent.Future;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.server.Command;
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
 * Per-UI state of the main application (user provider, view model providers, navigator, main screen), attached to the
 * Flow {@link UI}.
 * <p>
 * In Vaadin 8 this class was the UI subclass. Flow creates a plain {@link UI} for every browser tab and ignores the
 * {@code UI} servlet init parameter, so this class is a holder now: {@link SormasUIInitListener} creates one for each
 * new UI of {@link SormasUIServlet} and runs {@link #init(UI, VaadinRequest)} (what {@code UI.init} did) before the
 * UI's first navigation. {@link #get()} returns the holder of the current UI; the UI methods that callers used on it
 * ({@link #getPage()}, {@link #setPollInterval(int)}, {@link #access(Command)}, {@link #getCurrent()}) delegate to
 * the UI.
 * <p>
 * The viewport meta tags (formerly the @Viewport annotation here) are set by {@link SormasAppShell}.
 */
@SuppressWarnings("serial")
public class SormasUI implements HasUserProvider, HasViewModelProviders, Serializable {

	private final UI ui;
	private final UserProvider userProvider = new UserProvider();
	private final ViewModelProviders viewModelProviders = new ViewModelProviders();

	private Navigator navigator;
	private MainScreen mainScreen;

	/**
	 * Creates a holder for the UI without attaching or initialising it ({@link #init(UI, VaadinRequest)} does both).
	 */
	public SormasUI(UI ui) {
		this.ui = ui;
	}

	/**
	 * Attaches a new holder to the UI and initialises it, as {@code UI.init} did in Vaadin 8. Called once per UI, before
	 * its first navigation, so that the routes of the session are registered when the router resolves the location.
	 */
	static SormasUI init(UI ui, VaadinRequest vaadinRequest) {

		SormasUI sormasUI = new SormasUI(ui);
		ComponentUtil.setData(ui, SormasUI.class, sormasUI);

		VaadinSession.getCurrent().setErrorHandler(SormasErrorHandler.get());
		if (vaadinRequest != null) {
			ui.setLocale(vaadinRequest.getLocale());
		}

		ui.getPage().setTitle(FacadeProvider.getConfigFacade().getSormasInstanceName());

		sormasUI.initMainScreen();
		return sormasUI;
	}

	/**
	 * Builds the main screen (menu and view registration) when the UI is created, as before: the routes of this
	 * session are registered before the first navigation. {@link de.symeda.sormas.ui.navigation.MainLayout} shows it.
	 */
	protected void initMainScreen() {
		mainScreen = new MainScreen(SormasUI.this);
	}

	/**
	 * @return the holder of the current UI, or {@code null} when there is no current UI or it is not a UI of the main
	 *         application
	 */
	public static SormasUI get() {
		UI currentUI = UI.getCurrent();
		return currentUI != null ? ComponentUtil.getData(currentUI, SormasUI.class) : null;
	}

	/**
	 * @return the current Flow UI ({@link UI#getCurrent()}); kept because callers used the inherited static method
	 */
	public static UI getCurrent() {
		return UI.getCurrent();
	}

	/**
	 * @return the Flow UI this holder belongs to
	 */
	public UI getUI() {
		return ui;
	}

	public Page getPage() {
		return ui.getPage();
	}

	public void setPollInterval(int intervalInMillis) {
		ui.setPollInterval(intervalInMillis);
	}

	public Future<Void> access(Command command) {
		return ui.access(command);
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
