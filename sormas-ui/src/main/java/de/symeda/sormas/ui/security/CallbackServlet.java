/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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
 */

package de.symeda.sormas.ui.security;

import fish.payara.security.annotations.OpenIdAuthenticationDefinition;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * IDP Public callback url used for finalizing the authentication.
 *
 * @author Alex Vidrean
 * @see OpenIdAuthenticationDefinition#redirectURI()
 *      <p>
 *      No {@code @ServletSecurity(@HttpConstraint())} any more: it only marked this address as open, and any URL
 *      constraint in this web module makes Payara 7 refuse the main servlet's root (E001). Nothing is protected by URL
 *      now, so the address stays open.
 */
@WebServlet(urlPatterns = { "/Callback" })
public class CallbackServlet extends HttpServlet {

	private static final long serialVersionUID = 7429937658924321436L;

	/**
	 * Handles the HTTP <code>GET</code> method.
	 *
	 * @param request  servlet request
	 * @param response servlet response
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.sendRedirect("");
	}
}
