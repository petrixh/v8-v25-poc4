/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2021 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.servlet.ServletException;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinSession;

import de.symeda.sormas.api.AuthProvider;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.user.UserRoleDto;

public final class LoginHelper {

	private LoginHelper() {
		// Hide Utility Class Constructor
	}

	public static boolean login(String username, String password) {

		if (username == null || username.isEmpty()) {
			return false;
		}

		BeanManager bm = CDI.current().getBeanManager();
		@SuppressWarnings("unchecked")
		Bean<SecurityContext> securityContextBean = (Bean<SecurityContext>) bm.getBeans(SecurityContext.class).iterator().next();
		CreationalContext<SecurityContext> ctx = bm.createCreationalContext(securityContextBean);
		SecurityContext securityContext = (SecurityContext) bm.getReference(securityContextBean, SecurityContext.class, ctx);

		AuthenticationParameters authentication = new AuthenticationParameters();
		authentication.credential(new UsernamePasswordCredential(username, password));
		authentication.newAuthentication(true);
		authentication.setRememberMe(true);
		AuthenticationStatus status = securityContext.authenticate(
			VaadinServletRequest.getCurrent().getHttpServletRequest(),
			VaadinServletResponse.getCurrent().getHttpServletResponse(),
			authentication);

		if (status == AuthenticationStatus.SUCCESS) {
			Language userLanguage = FacadeProvider.getUserFacade().getByUserName(username).getLanguage();
			I18nProperties.setUserLanguage(userLanguage);
			FacadeProvider.getI18nFacade().setUserLanguage(userLanguage);
			return true;
		}

		return false;
	}

	public static boolean hasApplicationRight() {
		if (!isLoggedInToWebUi()) {
			try {
				VaadinServletRequest.getCurrent().getHttpServletRequest().logout();
			} catch (ServletException e) {
				// just do not crash
			}
			return false;
		}
		return true;
	}

	/**
	 * @return whether the caller of the current request is logged in and has the right to use the web UI
	 *         ({@link UserRight#SORMAS_UI}). Before the migration the container checked this for every request of the
	 *         main UI ({@code @ServletSecurity} on its servlet); now {@link LoginUI} checks it before every navigation.
	 */
	public static boolean isLoggedInToWebUi() {
		VaadinServletRequest request = VaadinServletRequest.getCurrent();
		return request != null && request.getUserPrincipal() != null && currentUserHasWebUiRight();
	}

	/**
	 * Whether the logged-in caller has the right {@link UserRight#SORMAS_UI}: the user rights of the user's roles, the same
	 * set the login gives the caller as groups (and so as roles). Not {@code isUserInRole}: on Payara 7 the web role check
	 * fails with an exception when the request's authorization context is not set, which is the case for this app
	 * (migration ticket 1a-91).
	 */
	public static boolean currentUserHasWebUiRight() {
		UserDto user = FacadeProvider.getUserFacade().getCurrentUser();
		return user != null && UserRoleDto.getUserRights(FacadeProvider.getUserFacade().getUserRoles(user)).contains(UserRight.SORMAS_UI);
	}

	/**
	 * Trigger logout event for Authentication Mechanism or other components to react.
	 */
	public static boolean logout() {

		if (!AuthProvider.getProvider(FacadeProvider.getConfigFacade()).isDefaultProvider()) {
			UI.getCurrent().getPage().setLocation("logout");
		} else {
			try {
				VaadinServletRequest.getCurrent().getHttpServletRequest().logout();
			} catch (ServletException e) {
				return false;
			}

			VaadinSession.getCurrent().getSession().invalidate();

			UI.getCurrent().getPage().reload();
		}

		return true;
	}
}
