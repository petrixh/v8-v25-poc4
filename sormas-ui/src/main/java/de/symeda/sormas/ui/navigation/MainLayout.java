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

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.RouterLayout;

import de.symeda.sormas.ui.SormasUI;

/**
 * Parent layout of all views registered by the {@link Navigator}. It holds the UI's {@code MainScreen} (menu and view
 * container, built when the UI is created, as in Vaadin 8), shows each view in the main screen's view container and
 * hands the Flow navigation events to the UI's {@link Navigator}. It also keeps the page title set when the UI was
 * created: Flow sets the title after every navigation, to an empty one unless a target in the chain gives it.
 */
public class MainLayout extends Div
	implements RouterLayout, BeforeEnterObserver, BeforeLeaveObserver, AfterNavigationObserver, HasDynamicTitle {

	private static final long serialVersionUID = 1L;

	public MainLayout() {
		setSizeFull();
		add(SormasUI.get().getMainScreen());
	}

	@Override
	public void showRouterLayoutContent(HasElement content) {
		navigator().showView(content);
	}

	@Override
	public void removeRouterLayoutContent(HasElement oldContent) {
		navigator().removeView(oldContent);
	}

	@Override
	public void beforeLeave(BeforeLeaveEvent event) {
		navigator().beforeLeave(event);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		navigator().beforeEnter(event);
	}

	@Override
	public void afterNavigation(AfterNavigationEvent event) {
		navigator().afterNavigation(event);
	}

	@Override
	public String getPageTitle() {
		SormasUI sormasUI = SormasUI.get();
		return sormasUI != null ? sormasUI.getPageTitle() : null;
	}

	private static Navigator navigator() {
		return SormasUI.get().getNavigator();
	}
}
