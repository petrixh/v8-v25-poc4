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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.server.HttpStatusCode;

import de.symeda.sormas.ui.MainScreen;

/**
 * Shown when a location matches no route registered for the session (was the Navigator's error provider in
 * {@code MainScreen}): {@code AccessDeniedView} for a known view name the user has no access to, {@code ErrorView} for
 * any other name. An unregistered sub-route of a registered view is not found either; the old prefix fallback to the
 * parent view (B028) is not copied.
 */
@ParentLayout(MainLayout.class)
public class NavigationErrorView extends Div implements HasErrorParameter<NotFoundException>, View {

	private static final long serialVersionUID = 1L;

	private View errorView;

	@Override
	public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {

		setSizeFull();
		removeAll();
		try {
			errorView = MainScreen.getErrorViewType(event.getLocation().getPath()).getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		add(errorView.getViewComponent());
		return HttpStatusCode.NOT_FOUND.getCode();
	}

	@Override
	public void enter(ViewChangeListener.ViewChangeEvent event) {
		if (errorView != null) {
			errorView.enter(event);
		}
	}
}
