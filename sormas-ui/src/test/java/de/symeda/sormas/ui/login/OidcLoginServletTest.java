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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;

import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;


/**
 * {@link LoginUI.OidcLoginServlet} with a stub of the OpenID mechanism behind {@link SecurityContext#authenticate}: it
 * redirects to the provider when the request has no caller, as the Payara OpenID mechanism does for a programmatic
 * authentication, and reports success otherwise. No Keycloak is contacted.
 */
public class OidcLoginServletTest {

	private static final String PROVIDER_LOGIN = "https://keycloak.invalid/realms/SORMAS/protocol/openid-connect/auth";

	private SecurityContext securityContext;
	private HttpServletRequest request;
	private HttpServletResponse response;
	private Principal caller;
	/** the web UI right of the logged-in caller, as the backend reports it */
	private Boolean webUiRight;
	private int webUiRightChecks;

	private LoginUI.OidcLoginServlet servlet() {
		return new LoginUI.OidcLoginServlet(securityContext, () -> {
			webUiRightChecks++;
			return webUiRight;
		});
	}

	@BeforeEach
	public void setUp() throws Exception {
		webUiRight = null;
		webUiRightChecks = 0;
		securityContext = mock(SecurityContext.class);
		request = mock(HttpServletRequest.class);
		response = mock(HttpServletResponse.class);
		when(request.getContextPath()).thenReturn("/sormas-ui");
		when(request.getUserPrincipal()).thenAnswer(i -> caller);
		// stub mechanism
		when(securityContext.authenticate(any(), any(), any())).thenAnswer(i -> {
			HttpServletRequest req = i.getArgument(0);
			HttpServletResponse resp = i.getArgument(1);
			if (req.getUserPrincipal() == null) {
				resp.sendRedirect(PROVIDER_LOGIN);
				return AuthenticationStatus.SEND_CONTINUE;
			}
			return AuthenticationStatus.SUCCESS;
		});
	}

	@Test
	public void testNotLoggedInRedirectsToProvider() throws Exception {
		caller = null;

		servlet().doGet(request, response);

		ArgumentCaptor<AuthenticationParameters> parameters = ArgumentCaptor.forClass(AuthenticationParameters.class);
		verify(securityContext).authenticate(same(request), same(response), parameters.capture());
		// no credential: the mechanism (not the SORMAS identity store) does the login
		assertNull(parameters.getValue().getCredential());
		assertFalse(parameters.getValue().isNewAuthentication());
		verify(response).sendRedirect(PROVIDER_LOGIN);
		verify(response, never()).sendRedirect("/sormas-ui/");
		verify(response, never()).sendError(anyInt());
		assertEquals(0, webUiRightChecks);
	}

	@Test
	public void testLoggedInWithWebRightGoesToApp() throws Exception {
		caller = () -> "natuser";
		webUiRight = true;

		servlet().doGet(request, response);

		verify(response).sendRedirect("/sormas-ui/");
		verify(response, never()).sendError(anyInt());
		// the right comes from the backend, not from the container role check (fails on Payara 7)
		assertEquals(1, webUiRightChecks);
		verify(request, never()).isUserInRole(anyString());
	}

	@Test
	public void testLoggedInWithoutWebRightIsForbiddenNotRedirected() throws Exception {
		caller = () -> "poeinf";
		webUiRight = false;

		servlet().doGet(request, response);

		// no redirect back to the app, so no loop between the app and this servlet
		verify(response, never()).sendRedirect(anyString());
		verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
		assertEquals(1, webUiRightChecks);
		verify(request, never()).isUserInRole(anyString());
	}

	@Test
	public void testMechanismFailureIsUnauthorized() throws Exception {
		doReturn(AuthenticationStatus.SEND_FAILURE).when(securityContext).authenticate(any(), any(), any());

		servlet().doGet(request, response);

		verify(response, never()).sendRedirect(anyString());
		verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
		assertEquals(0, webUiRightChecks);
	}

	@Test
	public void testPath() {
		assertEquals("oidc-login", LoginUI.OidcLoginServlet.PATH);
	}
}
