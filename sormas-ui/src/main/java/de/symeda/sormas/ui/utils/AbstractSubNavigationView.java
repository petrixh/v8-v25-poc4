/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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
 *******************************************************************************/
package de.symeda.sormas.ui.utils;

import java.util.Optional;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.symeda.sormas.ui.SubMenu;
import de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;

@SuppressWarnings("serial")
public abstract class AbstractSubNavigationView<SC extends HasElement> extends AbstractView {

	private final SubMenu subNavigationMenu;
	private HorizontalLayout buttonsLayout;
	protected SC subComponent;

	protected AbstractSubNavigationView(String viewName) {
		super(viewName);

		subNavigationMenu = new SubMenu();
		addComponent(subNavigationMenu);
		setFlexGrow(0, subNavigationMenu);

		createButtonsLayout().ifPresent(l -> {
			buttonsLayout = l;
			addHeaderComponent(l);
		});
	}

	protected Optional<HorizontalLayout> createButtonsLayout() {
		HorizontalLayout buttonsLayout = new HorizontalLayout();
		buttonsLayout.setPadding(false);

		return Optional.of(buttonsLayout);
	}

	@Override
	public void enter(ViewChangeEvent event) {

		String params = event != null ? event.getParameters() : "";
		refreshMenu(subNavigationMenu, params);
		selectInMenu();
	}

	public abstract void refreshMenu(SubMenu menu, String params);

	protected void setSubComponent(SC newComponent) {

		if (subComponent != null) {
			getElement().removeChild(subComponent.getElement());
		}
		subComponent = newComponent;
		if (subComponent != null) {
			// Make sure that the sub component is always the first component below the navigation
			getElement().insertChild(2, subComponent.getElement());
			// was setExpandRatio(subComponent, 1): the slot took the free height; a sub component of undefined height (most
			// forms and their CommitDiscardWrapperComponent) kept its own height at the top of the slot, so the Save / Discard
			// footer followed the form. Only a sub component with a height grows.
			if (!(subComponent instanceof HasSize) || ((HasSize) subComponent).getHeight() != null) {
				setFlexGrow(1, subComponent);
			}
		}
	}

	public void selectInMenu() {
		subNavigationMenu.setActiveView(viewName);
	}

	public HorizontalLayout getButtonsLayout() {
		return buttonsLayout;
	}
}
