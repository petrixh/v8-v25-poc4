/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2022 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.rest.security;

import static jakarta.ws.rs.Priorities.AUTHORIZATION;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.user.UserRight;

/**
 * Filter to ensure authenticated user has SORMAS_REST user right,
 * return status 403 when missing
 * <p>
 * Also checks the rights of the resource's {@code @RolesAllowed} (resolved by {@link SormasDynamicFeature}), which
 * Payara's own JAX-RS filter checked before the migration (switched off in {@code glassfish-web.xml}).
 * <p>
 * The caller's rights are read through the backend (the rights of the user's roles: the set the login gives the caller
 * as groups, mapped 1:1 to roles in {@code glassfish-web.xml}), not with {@code isUserInRole} or the Jakarta Security
 * {@code SecurityContext}: on Payara 7 those container checks fail with an exception after any call to a remote EJB,
 * such as the backend facades, because the call clears the request's authorization context (migration ticket 1a-30e).
 */

@Priority(AUTHORIZATION)
public class SormasRestUserRightFilter implements ContainerRequestFilter {

	private final SecurityContext securityContext;

	private final HttpServletRequest request;
	private final HttpServletResponse response;

	/**
	 * Rights of the resource's {@code @RolesAllowed}, one of which the caller needs; {@code null} when it has none.
	 */
	private final String[] rolesAllowed;

	public SormasRestUserRightFilter(HttpServletRequest request, HttpServletResponse response) {
		this(request, response, null);
	}

	public SormasRestUserRightFilter(HttpServletRequest request, HttpServletResponse response, String[] rolesAllowed) {
		securityContext = CDI.current().select(SecurityContext.class).get();

		this.request = request;
		this.response = response;
		this.rolesAllowed = rolesAllowed;
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {

		if (!isAuthenticated()) {

			AuthenticationStatus status = securityContext.authenticate(request, response, AuthenticationParameters.withParams());

			// Authentication was not done at all (i.e. no credentials present) or
			// authentication failed (i.e. wrong credentials, credentials expired, etc)
			if (status == AuthenticationStatus.NOT_DONE || status == AuthenticationStatus.SEND_FAILURE) {
				throw new NotAuthorizedException("Authentication resulted in " + status, Response.status(Response.Status.UNAUTHORIZED).build());
			}

			if (status == AuthenticationStatus.SUCCESS && !isAuthenticated()) { // compensate for possible Soteria bug, need to investigate
				throw new NotAuthorizedException(
					"Authentication not done (i.e. no JWT credential found)",
					Response.status(Response.Status.UNAUTHORIZED).build());
			}

		}

		Set<String> callerRights = getCallerRights();

		boolean hasRestRole = callerRights.contains(UserRight.SORMAS_REST.name());

		if (!hasRestRole) {
			throw new ForbiddenException("Caller not in requested role");
		}

		if (rolesAllowed != null && Arrays.stream(rolesAllowed).noneMatch(callerRights::contains)) {
			// same answer as Payara's JAX-RS @RolesAllowed filter
			throw new ForbiddenException("Caller not in requested role");
		}
	}

	private boolean isAuthenticated() {
		return request.getUserPrincipal() != null;
	}

	private static Set<String> getCallerRights() {
		return FacadeProvider.getUserFacade().getUserRights(null).stream().map(Enum::name).collect(Collectors.toSet());
	}
}
