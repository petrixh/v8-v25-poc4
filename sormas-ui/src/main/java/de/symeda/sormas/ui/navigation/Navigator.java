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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.VaadinSession;
/**
 * Session-scope route shim with the API of the Vaadin 8 {@code com.vaadin.navigator.Navigator} that SORMAS uses.
 * <p>
 * <b>Registration is the access check.</b> As before, {@code MainScreen} and the {@code registerViews} methods call
 * {@link #addView(String, Class)} only for the views the current user may open (the same rights and feature guards).
 * Each call registers a Flow route for this session only ({@link RouteConfiguration#forSessionScope()}), with
 * {@link MainLayout} as parent layout. A route that is not registered for the session is not found: the user gets the
 * access denied or view not found page ({@link NavigationErrorView}).
 * <p>
 * <b>Route names</b> are the old view names, now as URL paths ({@code cases}, {@code cases/data/<uuid>}). Views
 * implementing {@link HasViewParameters} also take path parameters after their name (wildcard route); all others only
 * match their exact name. Query parameters ({@code cases?district=...}) are passed to every view.
 * <p>
 * <b>Life cycle</b> as in Vaadin 8: {@link View#beforeLeave(ViewBeforeLeaveEvent)} of the current view, then
 * {@link ViewChangeListener#beforeViewChange(ViewChangeListener.ViewChangeEvent)} of the listeners, then the new view
 * is shown, then {@link View#enter(ViewChangeListener.ViewChangeEvent)}, then the listeners'
 * {@link ViewChangeListener#afterViewChange(ViewChangeListener.ViewChangeEvent)}. Every navigation gets a new view
 * instance, also to the current view class or the current state ({@code SormasUI.refreshView()}), as with Vaadin 8's
 * class-based view registration. {@link MainLayout} forwards the Flow navigation events here.
 * <p>
 * <b>Errors</b> go to the session error handler, as in Vaadin 8: from {@code enter()} and the after-view-change
 * listeners directly (the view stays shown), from all earlier stages through {@link NavigationExceptionView}.
 */
public class Navigator implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Name of the wildcard route parameter that holds the path parameters of a {@link HasViewParameters} view.
	 */
	public static final String PARAMETERS = "parameters";

	// session attribute: the route templates registered for the session by any of its navigators
	private static final String SESSION_ROUTES = Navigator.class.getName() + ".routes";

	private final UI ui;
	private final Component display;
	private final List<ViewChangeListener> listeners = new ArrayList<>();
	private final Map<String, String> routeTemplates = new LinkedHashMap<>();

	private View currentView;
	private String state = "";

	private boolean inBeforeEnter;
	private String forwardState;
	private boolean internalRefresh;
	// the location of a refresh of the current state (navigateTo(getState())) that is under way
	private Location refreshLocation;

	/**
	 * @param ui
	 *            the UI this navigator belongs to
	 * @param display
	 *            the component that shows the current view (the view container of the main screen)
	 */
	public Navigator(UI ui, Component display) {
		this.ui = ui;
		this.display = display;

		// Vaadin 8 kept the scroll position of the view container when the current view was shown again (refreshView()
		// after Save): the container stayed and the browser kept its scroll offset. Flow empties the container before it
		// adds the new view, which scrolls it to the top. The container remembers its offset here, and a refresh of the
		// current state puts it back (afterNavigation).
		display.getElement()
			.executeJs("this.addEventListener('scroll', () => { this.$sormasScrollTop = this.scrollTop; }, { passive: true });");

		// Vaadin 8 had one Navigator per UI, filled when the UI was created. Session-scope routes outlive a UI, so a new UI
		// of the session starts from an empty registration and registers again with the user's current rights.
		RouteConfiguration routes = RouteConfiguration.forSessionScope();
		routes.update(() -> {
			Set<String> sessionRoutes = getSessionRoutes();
			sessionRoutes.forEach(routes::removeRoute);
			sessionRoutes.clear();
			routes.setRoute("", RootView.class, MainLayout.class);
			sessionRoutes.add("");
		});
	}

	/**
	 * Registers a view class under a view name for this session, as {@code Navigator.addView(String, Class)} did.
	 * Registering a name again replaces the earlier registration.
	 */
	public void addView(String viewName, Class<? extends View> viewClass) {

		if (!Component.class.isAssignableFrom(viewClass)) {
			throw new IllegalArgumentException("View class " + viewClass.getName() + " is not a Flow component");
		}
		@SuppressWarnings("unchecked")
		Class<? extends Component> navigationTarget = (Class<? extends Component>) viewClass;
		String template = HasViewParameters.class.isAssignableFrom(viewClass) ? viewName + "/:" + PARAMETERS + "*" : viewName;

		RouteConfiguration routes = RouteConfiguration.forSessionScope();
		routes.update(() -> {
			String previous = routeTemplates.put(viewName, template);
			if (previous != null) {
				routes.removeRoute(previous);
			}
			routes.setRoute(template, navigationTarget, MainLayout.class);
			getSessionRoutes().add(template);
		});
	}

	@SuppressWarnings("unchecked")
	private static Set<String> getSessionRoutes() {
		VaadinSession session = VaadinSession.getCurrent();
		Set<String> sessionRoutes = (Set<String>) session.getAttribute(SESSION_ROUTES);
		if (sessionRoutes == null) {
			sessionRoutes = new HashSet<>();
			session.setAttribute(SESSION_ROUTES, sessionRoutes);
		}
		return sessionRoutes;
	}

	/**
	 * Navigates to a state ({@code <viewName>[/<parameters>][?<query>]}), as {@code Navigator.navigateTo(String)} did.
	 * Navigating to the current state shows a new instance of the current view.
	 */
	public void navigateTo(String navigationState) {

		String target = navigationState != null ? navigationState : "";
		if (inBeforeEnter) {
			// called from a beforeViewChange listener: Flow wants a forward instead of a new navigation
			forwardState = target;
			return;
		}

		Location location = toLocation(target);
		if (currentView != null && location.getPathWithQueryParameters().equals(toLocation(state).getPathWithQueryParameters())) {
			refreshLocation = location;
			ui.refreshCurrentRoute(false);
		} else {
			ui.navigate(location.getPath(), location.getQueryParameters());
		}
	}

	/**
	 * @return the current state ({@code <viewName>[/<parameters>]}, parameters as passed to {@link View#enter})
	 */
	public String getState() {
		return state;
	}

	public View getCurrentView() {
		return currentView;
	}

	public UI getUI() {
		return ui;
	}

	public void addViewChangeListener(ViewChangeListener listener) {
		listeners.add(listener);
	}

	public void removeViewChangeListener(ViewChangeListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Replaces the browser URL with the given state without navigating (was {@code Page.setUriFragment("!" + state, false)}).
	 */
	public void updateUrl(String navigationState) {

		String target = navigationState != null ? navigationState : "";
		ui.getPage().getHistory().replaceState(null, toLocation(target));
		if (currentView != null) {
			state = target;
		}
	}

	/**
	 * @return the relative URL of a state, for links and new browser tabs (was {@code "#!" + state})
	 */
	public static String getUrl(String navigationState) {
		return toLocation(navigationState != null ? navigationState : "").getPathWithQueryParameters();
	}

	static Location toLocation(String navigationState) {

		String path = navigationState;
		String query = "";
		int queryIndex = navigationState.indexOf('?');
		if (queryIndex >= 0) {
			path = navigationState.substring(0, queryIndex);
			query = navigationState.substring(queryIndex + 1);
		}
		while (path.startsWith("/")) {
			path = path.substring(1);
		}
		while (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return new Location(path, query.isEmpty() ? QueryParameters.empty() : QueryParameters.fromString(query));
	}

	/**
	 * @return view name and parameters of a location, split as the Vaadin 8 Navigator did: parameters are the path
	 *         parameters and, after a {@code ?}, the query ({@code <uuid>/?<query>} when there are both)
	 */
	static String[] toViewNameAndParameters(Location location, RouteParameters routeParameters) {

		String path = location.getPath();
		String pathParameters = routeParameters.get(PARAMETERS).orElse("");
		String viewName = path;
		if (!pathParameters.isEmpty() && path.endsWith("/" + pathParameters)) {
			viewName = path.substring(0, path.length() - pathParameters.length() - 1);
		}

		String parameters = pathParameters;
		String query = location.getQueryParameters().getQueryString();
		if (!query.isEmpty()) {
			parameters = (pathParameters.isEmpty() ? "" : pathParameters + "/") + "?" + query;
		}
		return new String[] {
			viewName,
			parameters };
	}

	// Called by MainLayout

	void showView(HasElement content) {
		display.getElement().removeAllChildren();
		if (content != null) {
			display.getElement().appendChild(content.getElement());
		}
	}

	void removeView(HasElement oldContent) {
		if (oldContent != null) {
			display.getElement().removeChild(oldContent.getElement());
		}
	}

	void beforeLeave(BeforeLeaveEvent event) {

		if (currentView == null || internalRefresh || isExceptionNavigation(event)) {
			return;
		}
		LeaveAction leaveAction = new LeaveAction(event);
		currentView.beforeLeave(new ViewBeforeLeaveEvent(this, leaveAction::proceed));
		leaveAction.postponeIfNotProceeded();
	}

	void beforeEnter(BeforeEnterEvent event) {

		if (internalRefresh || isExceptionNavigation(event)) {
			return;
		}

		String[] viewNameAndParameters = toViewNameAndParameters(event.getLocation(), event.getRouteParameters());
		ViewChangeListener.ViewChangeEvent changeEvent =
			new ViewChangeListener.ViewChangeEvent(this, currentView, null, viewNameAndParameters[0], viewNameAndParameters[1]);

		boolean accepted = true;
		forwardState = null;
		inBeforeEnter = true;
		try {
			for (ViewChangeListener listener : new ArrayList<>(listeners)) {
				if (!listener.beforeViewChange(changeEvent)) {
					accepted = false;
					break;
				}
			}
		} finally {
			inBeforeEnter = false;
		}

		String forwardTo = forwardState;
		forwardState = null;
		if (forwardTo == null && !accepted && currentView != null) {
			// navigation cancelled: stay where we are
			forwardTo = state;
		}
		if (forwardTo != null) {
			Location location = toLocation(forwardTo);
			event.forwardTo(location.getPath(), location.getQueryParameters());
		}
	}

	void afterNavigation(AfterNavigationEvent event) {

		List<HasElement> chain = event.getActiveChain();
		HasElement target = chain.isEmpty() ? null : chain.get(0);
		boolean keepScrollPosition = refreshLocation != null
			&& refreshLocation.getPathWithQueryParameters().equals(event.getLocation().getPathWithQueryParameters());
		refreshLocation = null;
		if (!(target instanceof View)) {
			return;
		}
		View newView = (View) target;

		if (newView instanceof NavigationExceptionView) {
			// the navigation failed (NavigationExceptionView has reported it); as in Vaadin 8, no enter() and no
			// after-view-change listeners. The error view is now what the user sees, so it is the current view.
			internalRefresh = false;
			currentView = newView;
			state = toState(toViewNameAndParameters(event.getLocation(), event.getRouteParameters()));
			return;
		}

		if (internalRefresh) {
			internalRefresh = false;
		} else if (newView == currentView) {
			// Flow keeps the instance when the target class stays the same; Vaadin 8 created a new view for every
			// navigation, and the views rely on that. Show a new instance of the target (no new leave or enter checks).
			internalRefresh = true;
			ui.beforeClientResponse(ui, context -> {
				try {
					ui.refreshCurrentRoute(false);
				} finally {
					internalRefresh = false;
				}
			});
			return;
		}

		if (keepScrollPosition) {
			restoreScrollPosition(newView.isScrollPositionKeptOnRefresh());
		}

		String[] viewNameAndParameters = toViewNameAndParameters(event.getLocation(), event.getRouteParameters());
		View oldView = currentView;
		currentView = newView;
		state = toState(viewNameAndParameters);

		ViewChangeListener.ViewChangeEvent changeEvent =
			new ViewChangeListener.ViewChangeEvent(this, oldView, newView, viewNameAndParameters[0], viewNameAndParameters[1]);
		try {
			newView.enter(changeEvent);
			for (ViewChangeListener listener : new ArrayList<>(listeners)) {
				listener.afterViewChange(changeEvent);
			}
		} catch (Throwable e) {
			// Vaadin 8: anything thrown by enter() or an after-view-change listener reached the session error handler
			// (log, generic error notification); the view stayed shown as far as it was built and the remaining
			// listeners did not run. Flow would instead replace the view with an error view (NavigationExceptionView).
			if (!toSessionErrorHandler(e)) {
				throw e;
			}
		}
	}

	/**
	 * After the current view was shown again: puts the view container back to the scroll offset it had before, once
	 * the new view is in the page and once more in the next frame (for parts of the view that get their height a little
	 * later); or shows the view from the top when the view asks for that.
	 */
	private void restoreScrollPosition(boolean keep) {
		if (!keep) {
			display.getElement().executeJs("this.scrollTop = 0;");
			return;
		}
		display.getElement()
			.executeJs(
				"const top = this.$sormasScrollTop; if (!top) { return; } this.scrollTop = top;"
					+ " requestAnimationFrame(() => { if (this.scrollTop < top) { this.scrollTop = top; } });");
	}

	private static String toState(String[] viewNameAndParameters) {
		return viewNameAndParameters[1].isEmpty() ? viewNameAndParameters[0] : viewNameAndParameters[0] + "/" + viewNameAndParameters[1];
	}

	/**
	 * @return whether this is Flow's navigation to {@link NavigationExceptionView} after an exception in the navigation
	 *         (view constructor, before-leave or before-view-change): the views and listeners are not asked again
	 */
	private static boolean isExceptionNavigation(BeforeEvent event) {
		return NavigationExceptionView.class.equals(event.getNavigationTarget());
	}

	/**
	 * Hands an error to the session error handler ({@code SormasErrorHandler}: log, generic error notification), where
	 * Vaadin 8 sent every exception of a request.
	 *
	 * @return {@code false} when there is no session or handler
	 */
	static boolean toSessionErrorHandler(Throwable throwable) {
		VaadinSession session = VaadinSession.getCurrent();
		ErrorHandler errorHandler = session != null ? session.getErrorHandler() : null;
		if (errorHandler == null) {
			return false;
		}
		errorHandler.error(new ErrorEvent(throwable));
		return true;
	}

	/**
	 * Lets a view confirm leaving now (navigate() inside beforeLeave) or later (Flow postpones the navigation).
	 */
	private static final class LeaveAction implements Serializable {

		private static final long serialVersionUID = 1L;

		private final BeforeLeaveEvent event;
		private boolean inHandler = true;
		private boolean proceeded;
		private BeforeLeaveEvent.ContinueNavigationAction postponed;

		private LeaveAction(BeforeLeaveEvent event) {
			this.event = event;
		}

		private void proceed() {
			if (inHandler) {
				proceeded = true;
			} else if (postponed != null) {
				postponed.proceed();
			}
		}

		private void postponeIfNotProceeded() {
			inHandler = false;
			if (!proceeded) {
				postponed = event.postpone();
			}
		}
	}
}
