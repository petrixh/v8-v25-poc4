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

import com.vaadin.flow.function.SerializableRunnable;

/**
 * Event for {@link View#beforeLeave(ViewBeforeLeaveEvent)}, as in Vaadin 8: the navigation goes on only once
 * {@link #navigate()} is called.
 */
public class ViewBeforeLeaveEvent implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Navigator navigator;
	private final SerializableRunnable navigateAction;
	private boolean navigated;

	public ViewBeforeLeaveEvent(Navigator navigator, SerializableRunnable navigateAction) {
		this.navigator = navigator;
		this.navigateAction = navigateAction;
	}

	public Navigator getNavigator() {
		return navigator;
	}

	/**
	 * Performs the navigation that was postponed by the view. Calling it more than once has no effect.
	 */
	public void navigate() {
		if (navigated) {
			return;
		}
		navigated = true;
		navigateAction.run();
	}

	public boolean isNavigated() {
		return navigated;
	}
}
