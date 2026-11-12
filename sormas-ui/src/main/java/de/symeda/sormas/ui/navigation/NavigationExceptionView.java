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

package de.symeda.sormas.ui.navigation;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.server.HttpStatusCode;

/**
 * Shown in the main layout, with an empty view area, when a navigation fails with an exception: thrown by a view
 * constructor, a view's {@code beforeLeave}, a {@code beforeViewChange} listener or another router stage (replaces
 * Flow's default error page, which shows the exception text). As in Vaadin 8, the exception goes to the session error
 * handler, which logs it and shows the generic error notification; the page shows no exception text. Locations with
 * no route still go to {@link NavigationErrorView}, which handles the more specific {@code NotFoundException}.
 */
@ParentLayout(MainLayout.class)
public class NavigationExceptionView extends Div implements HasErrorParameter<Exception>, View {

	private static final long serialVersionUID = 1L;

	@Override
	public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {

		setSizeFull();
		try {
			if (!Navigator.toSessionErrorHandler(parameter.getCaughtException())) {
				LoggerFactory.getLogger(NavigationExceptionView.class)
					.error("Navigation to '" + event.getLocation().getPath() + "' failed", parameter.getCaughtException());
			}
		} catch (Throwable handlerError) {
			// never fall back to Flow's error page (it shows the exception text): log both here
			LoggerFactory.getLogger(NavigationExceptionView.class)
				.error("Navigation to '" + event.getLocation().getPath() + "' failed", parameter.getCaughtException());
			LoggerFactory.getLogger(NavigationExceptionView.class).error("The session error handler failed", handlerError);
		}
		return HttpStatusCode.INTERNAL_SERVER_ERROR.getCode();
	}
}
