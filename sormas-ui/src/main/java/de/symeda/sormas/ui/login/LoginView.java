/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2026 SORMAS Foundation gGmbH
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.ui.login;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.server.VaadinServletRequest;

import de.symeda.sormas.api.AuthProvider;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.ui.login.LoginScreen.LoginListener;

/**
 * The login page, route {@value #ROUTE} of every session (registered by {@link LoginUI#init}). It shows the
 * {@link LoginScreen} as the Vaadin 8 login UI did; after a successful login (and the default password dialogs) the
 * browser loads the address the user asked for, or the start page.
 * <p>
 * With Keycloak as the authentication provider the page shows nothing and sends the browser to the
 * {@link LoginUI.OidcLoginServlet}, which redirects to the provider's login page.
 */
public class LoginView extends Div implements HasDynamicTitle {

	private static final long serialVersionUID = 1L;

	public static final String ROUTE = "login";

	public LoginView() {
		setSizeFull();
		UI ui = UI.getCurrent();
		if (!AuthProvider.getProvider(FacadeProvider.getConfigFacade()).isDefaultProvider()) {
			ui.getPage().setLocation(VaadinServletRequest.getCurrent().getContextPath() + "/" + LoginUI.OidcLoginServlet.PATH);
			return;
		}
		add(
			new LoginScreen(
				DefaultPasswordUIHelper
					.getInterceptionLoginListener((LoginListener) () -> ui.getPage().setLocation(LoginUI.getTargetUrl()), ui)));
	}

	@Override
	public String getPageTitle() {
		// Flow sets the title after every navigation; the login UI's title was the instance name
		return FacadeProvider.getConfigFacade().getSormasInstanceName();
	}
}
