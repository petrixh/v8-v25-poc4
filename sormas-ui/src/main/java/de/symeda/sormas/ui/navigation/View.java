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

import com.vaadin.flow.component.Component;

/**
 * A view of the main screen, as in the Vaadin 8 {@code com.vaadin.navigator.View} contract.
 * <p>
 * View classes are registered per session with {@link Navigator#addView(String, Class)}; the {@link Navigator}
 * calls {@link #enter(ViewChangeListener.ViewChangeEvent)} after the view is shown, and
 * {@link #beforeLeave(ViewBeforeLeaveEvent)} before the user navigates away. A view class must be a Flow
 * {@link Component} with a public no-argument constructor.
 */
public interface View extends Serializable {

	/**
	 * Called after the view has been shown. {@code event.getParameters()} holds the part of the location after the view
	 * name: path parameters (only for views implementing {@link HasViewParameters}) and, after a {@code ?}, the query.
	 */
	default void enter(ViewChangeListener.ViewChangeEvent event) {
	}

	/**
	 * Called before navigating away from this view. The navigation goes on only when {@link ViewBeforeLeaveEvent#navigate()}
	 * is called, now or later (e.g. after a confirmation dialog).
	 */
	default void beforeLeave(ViewBeforeLeaveEvent event) {
		event.navigate();
	}

	default Component getViewComponent() {
		return (Component) this;
	}
}
