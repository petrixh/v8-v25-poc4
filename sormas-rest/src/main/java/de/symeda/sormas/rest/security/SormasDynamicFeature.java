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

import java.lang.reflect.Method;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SormasDynamicFeature implements DynamicFeature {

	@Context
	private HttpServletRequest request;

	@Context
	private HttpServletResponse response;

	@Override
	public void configure(ResourceInfo resourceInfo, FeatureContext context) {
		context.register(new SormasRestUserRightFilter(request, response, getRolesAllowed(resourceInfo)));
	}

	/**
	 * The rights a resource method needs (any one of them), resolved as Payara's JAX-RS {@code @RolesAllowed} support
	 * did before the migration: {@code @DenyAll} on the method (none suffices), else {@code @PermitAll} on the method (no
	 * requirement), else {@code @RolesAllowed} of the method, else of the resource class. {@code null}: no requirement.
	 */
	static String[] getRolesAllowed(ResourceInfo resourceInfo) {

		Method method = resourceInfo.getResourceMethod();
		if (method.isAnnotationPresent(DenyAll.class)) {
			return new String[0];
		}
		if (method.isAnnotationPresent(PermitAll.class)) {
			return null;
		}
		RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
		if (rolesAllowed == null) {
			rolesAllowed = resourceInfo.getResourceClass().getAnnotation(RolesAllowed.class);
		}
		return rolesAllowed != null ? rolesAllowed.value() : null;
	}
}
