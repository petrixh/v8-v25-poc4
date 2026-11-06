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
package de.symeda.sormas.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;

import de.symeda.sormas.ui.navigation.Navigator;

/**
 * A sub navigation menu presenting a list of available views to the user.
 * It emulates the HTML components of a tabsheet to use it's styling.
 */
@SuppressWarnings("serial")
public class SubMenu extends Div {

	// attribute that makes Flow handle a link click as a router navigation
	private static final String ROUTER_LINK = "router-link";

	private Map<String, Div> viewMenuItemMap = new HashMap<String, Div>();

	private Div menuItemsLayout;

	public SubMenu() {

		setWidthFull();
		setHeight(null);
		setClassName("v-tabsheet");

		menuItemsLayout = new Div();
		menuItemsLayout.setClassName("v-tabsheet-tabcontainer");
		menuItemsLayout.setWidthFull();
		menuItemsLayout.setHeight(null);
		add(menuItemsLayout);
	}

	public void addView(final String name, String caption) {
		addView(name, caption, null, true);
	}

	public void addView(final String name, String caption, String params) {
		addView(name, caption, params, false);
	}

	public void addView(final String name, String caption, String params, boolean isBackNavigation) {

		String target = Navigator.getUrl(name + (params != null ? "/" + params : ""));

		Div tabItemCell = new Div();
		tabItemCell.setSizeUndefined();
		tabItemCell.setClassName("v-tabsheet-tabitemcell");

		Div tabItem = new Div();
		tabItem.setId("tab-" + name.replaceAll("/", "-"));
		tabItem.setSizeUndefined();
		tabItem.setClassName("v-tabsheet-tabitem");
		if (isBackNavigation) {
			tabItem.addClassName("back");
		}
		tabItemCell.add(tabItem);

		Anchor link = new Anchor(target, caption);
		link.getElement().setAttribute(ROUTER_LINK, true);
		link.addClassName("v-caption");
		if (isBackNavigation)
			link.addComponentAsFirst(VaadinIcon.ARROW_CIRCLE_LEFT.create());
		tabItem.add(link);

		menuItemsLayout.add(tabItemCell);
		viewMenuItemMap.put(name, tabItem);
	}

	public void addView(final String name, String caption, Supplier<Boolean> onClick) {

		Div tabItemCell = new Div();
		tabItemCell.setSizeUndefined();
		tabItemCell.setClassName("v-tabsheet-tabitemcell");

		Div tabItem = new Div();
		tabItem.setSizeUndefined();
		tabItem.setClassName("v-tabsheet-tabitem");
		tabItemCell.add(tabItem);

		Anchor link = new Anchor();
		link.setText(caption);
		link.addClassName("v-caption");

		tabItem.add(link);
		tabItem.addClickListener((e) -> {
			if (Boolean.TRUE.equals(onClick.get())) {
				this.setActiveView(name);
			}
		});

		menuItemsLayout.add(tabItemCell);
		viewMenuItemMap.put(name, tabItem);
	}

	public void removeAllViews() {

		menuItemsLayout.removeAll();
		viewMenuItemMap.clear();
	}

	/**
	 * Highlights a view navigation button as the currently active view in the
	 * menu. This method does not perform the actual navigation.
	 *
	 * @param viewName
	 *            the name of the view to show as active
	 */
	public void setActiveView(String viewName) {

		for (Div button : viewMenuItemMap.values()) {
			button.removeClassName("selected");
		}
		Div selected = viewMenuItemMap.get(viewName);
		if (selected != null) {
			selected.addClassName("selected");
		}
	}
}
