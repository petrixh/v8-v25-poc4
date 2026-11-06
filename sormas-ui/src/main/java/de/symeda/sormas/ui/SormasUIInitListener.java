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

package de.symeda.sormas.ui;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletService;

/**
 * Runs {@link SormasUI#init} for every new UI of the main servlet ({@link SormasUI.SormasUIServlet}).
 * <p>
 * Flow creates a plain {@code UI} and ignores the {@code UI} servlet init parameter, so the Vaadin 8 {@code UI.init}
 * of {@link SormasUI} would never run. A {@code UIInitListener} runs after the UI is created and before its first
 * navigation, as {@code UI.init} did. Registered through {@code META-INF/services}: the main servlet is a plain
 * {@code VaadinServlet} (no CDI servlet, so a CDI-observed {@code ServiceInitEvent} would not be fired).
 * <p>
 * UIs of other Vaadin servlets in the war (the login servlet) are left alone: their users are not logged in yet.
 */
public class SormasUIInitListener implements VaadinServiceInitListener {

	private static final long serialVersionUID = 1L;

	@Override
	public void serviceInit(ServiceInitEvent event) {

		VaadinService service = event.getSource();
		if (!(service instanceof VaadinServletService)
			|| !(((VaadinServletService) service).getServlet() instanceof SormasUI.SormasUIServlet)) {
			return;
		}

		service.addUIInitListener(uiInitEvent -> SormasUI.init(uiInitEvent.getUI(), VaadinRequest.getCurrent()));
	}
}
