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
package de.symeda.sormas.ui.login;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;

import de.symeda.sormas.api.AuthProvider;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.ui.SormasErrorHandler;
import de.symeda.sormas.ui.SormasUI;
import fish.payara.security.openid.api.OpenIdContext;

/**
 * The login side of every UI of the main servlet (was the Vaadin 8 UI of the separate login servlet).
 * <p>
 * Vaadin 8 had two servlets: the main UI at {@code /*}, which the container refused to everyone without the
 * {@code SORMAS_UI} right ({@code @ServletSecurity}), and this login UI at {@code /login/*}. On Payara 7 a container
 * constraint next to an unprotected address refuses the servlet root, which every Flow request uses (E001). So there is
 * one Flow servlet now, the login screen is the route {@value LoginView#ROUTE} of each session, and
 * {@link #init(UI, VaadinRequest)} installs the check that replaces the container constraint: before every navigation,
 * a UI whose user is not logged in with the {@code SORMAS_UI} right is sent to the login route. The main screen and the
 * routes of the main application are only built for a UI whose user is logged in ({@code SormasUIInitListener}).
 * The backend facades keep checking the user's rights as before.
 */
public final class LoginUI {

	/**
	 * Session attribute: the address a user asked for before logging in (old app: the {@code #!} fragment kept by the
	 * browser across the redirect to the login page).
	 */
	private static final String LOGIN_TARGET = LoginUI.class.getName() + ".target";

	private LoginUI() {
		// Hide Utility Class Constructor
	}

	/**
	 * Called for every new UI of the main servlet, before its first navigation: what the login UI's {@code init} did
	 * (error handler, locale, title), the login route of the session and the login check.
	 */
	public static void init(UI ui, VaadinRequest vaadinRequest) {

		VaadinSession.getCurrent().setErrorHandler(SormasErrorHandler.get());
		if (vaadinRequest != null) {
			ui.setLocale(vaadinRequest.getLocale());
		}
		ui.getPage().setTitle(FacadeProvider.getConfigFacade().getSormasInstanceName());

		RouteConfiguration routes = RouteConfiguration.forSessionScope();
		if (!routes.isRouteRegistered(LoginView.class)) {
			routes.setRoute(LoginView.ROUTE, LoginView.class);
		}

		ui.addBeforeEnterListener(LoginUI::checkLogin);
	}

	/**
	 * The login check: the user must be logged in with the {@code SORMAS_UI} right, and the UI must have been built
	 * for a logged-in user. Otherwise the login screen is shown instead, and the address asked for is kept for after
	 * the login.
	 */
	private static void checkLogin(BeforeEnterEvent event) {

		if (event.getNavigationTarget() == LoginView.class) {
			return;
		}

		boolean mainUi = ComponentUtil.getData(event.getUI(), SormasUI.class) != null;
		// requests always exist in practice; without one (background access) the UI's state decides
		boolean loggedIn = VaadinServletRequest.getCurrent() == null || LoginHelper.isLoggedInToWebUi();
		if (mainUi && loggedIn) {
			return;
		}

		String target = event.getLocation().getPathWithQueryParameters();
		VaadinSession session = VaadinSession.getCurrent();
		if (session != null) {
			session.setAttribute(LOGIN_TARGET, target.isEmpty() ? null : target);
		}
		event.forwardTo(LoginView.class);
	}

	/**
	 * @return the address to open after a successful login: the one asked for before the login, or the start page
	 */
	static String getTargetUrl() {

		String target = null;
		VaadinSession session = VaadinSession.getCurrent();
		if (session != null) {
			target = (String) session.getAttribute(LOGIN_TARGET);
			session.setAttribute(LOGIN_TARGET, null);
		}
		// a path inside this application only
		String path = target != null ? target.replaceFirst("^/+", "") : "";
		return VaadinServletRequest.getCurrent().getContextPath() + "/" + path;
	}

	public static class OidcLogoutServlet extends HttpServlet {

		@Inject
		private OpenIdContext openIdContext;

		@Override
		protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			AuthProvider authProvider = AuthProvider.getProvider(FacadeProvider.getConfigFacade());
			if (AuthProvider.KEYCLOAK.equals(authProvider.getName()) && request != null && response != null) {
				openIdContext.logout(request, response);
			}
		}
	}

	/**
	 * Registers the OIDC logout servlet when Keycloak is the authentication provider. The SORMAS login servlet of
	 * Vaadin 8 is gone (the login is a route of the main servlet). No servlet security element: any URL constraint in
	 * this web module makes Payara 7 refuse the main servlet's root (E001).
	 */
	@WebListener
	public static class ServletStartupListener implements ServletContextListener {

		private final Logger logger = LoggerFactory.getLogger(getClass());

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			ServletContext ctx = sce.getServletContext();
			String authenticationProvider = FacadeProvider.getConfigFacade().getAuthenticationProvider();
			if (authenticationProvider.equalsIgnoreCase(AuthProvider.SORMAS)) {
				logger.debug("SORMAS login route enabled");
			} else {
				ServletRegistration.Dynamic servletRegistration = ctx.addServlet("OidcLogoutServlet", OidcLogoutServlet.class);
				servletRegistration.addMapping("/logout/*");
				servletRegistration.setAsyncSupported(true);
				logger.debug("OIDC logout servlet enabled");
			}
		}
	}
}
