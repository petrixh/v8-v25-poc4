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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Listener for view changes of the {@link Navigator}, as in the Vaadin 8 {@code com.vaadin.navigator.ViewChangeListener}.
 */
public interface ViewChangeListener extends Serializable {

	/**
	 * Called before the view changes. Return {@code false} to cancel; a listener may call {@link Navigator#navigateTo(String)}
	 * to send the user elsewhere instead.
	 */
	boolean beforeViewChange(ViewChangeEvent event);

	default void afterViewChange(ViewChangeEvent event) {
	}

	class ViewChangeEvent implements Serializable {

		private static final long serialVersionUID = 1L;

		private final Navigator navigator;
		private final View oldView;
		private final View newView;
		private final String viewName;
		private final String parameters;

		public ViewChangeEvent(Navigator navigator, View oldView, View newView, String viewName, String parameters) {
			this.navigator = navigator;
			this.oldView = oldView;
			this.newView = newView;
			this.viewName = viewName;
			this.parameters = parameters;
		}

		public Navigator getNavigator() {
			return navigator;
		}

		public View getOldView() {
			return oldView;
		}

		/**
		 * @return the new view; {@code null} in {@link ViewChangeListener#beforeViewChange(ViewChangeEvent)}, because Flow
		 *         creates the view instance only after the navigation is accepted.
		 */
		public View getNewView() {
			return newView;
		}

		public String getViewName() {
			return viewName;
		}

		public String getParameters() {
			return parameters;
		}

		/**
		 * @return the parameters split at {@code &} into key/value pairs, as in Vaadin 8
		 */
		public Map<String, String> getParameterMap() {
			return getParameterMap("&");
		}

		public Map<String, String> getParameterMap(String separator) {
			if (parameters == null || parameters.isEmpty()) {
				return Collections.emptyMap();
			}
			Map<String, String> map = new LinkedHashMap<>();
			for (String entry : parameters.split(separator)) {
				if (entry.isEmpty()) {
					continue;
				}
				int index = entry.indexOf('=');
				if (index < 0) {
					map.put(entry, "");
				} else {
					map.put(entry.substring(0, index), entry.substring(index + 1));
				}
			}
			return map;
		}
	}
}
