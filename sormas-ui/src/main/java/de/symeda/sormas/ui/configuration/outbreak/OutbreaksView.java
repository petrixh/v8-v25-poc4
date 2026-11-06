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
package de.symeda.sormas.ui.configuration.outbreak;

import de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.configuration.AbstractConfigurationView;
import de.symeda.sormas.ui.utils.CssStyles;

public class OutbreaksView extends AbstractConfigurationView {

	private static final long serialVersionUID = -6589135368637794263L;

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/outbreaks";

	private OutbreakOverviewGrid grid;
	private VerticalLayout contentLayout;

	public OutbreaksView() {
		super(VIEW_NAME);

		Span infoTextLabel = new Span(I18nProperties.getString(Strings.infoDefineOutbreaks));
		CssStyles.style(infoTextLabel, CssStyles.LABEL_MEDIUM);

		grid = new OutbreakOverviewGrid();

		contentLayout = new VerticalLayout();
		contentLayout.add(infoTextLabel);
		contentLayout.add(grid);
		contentLayout.setPadding(true);
		contentLayout.setSpacing(true);
		contentLayout.setSizeFull();
		contentLayout.setClassName("crud-main-layout");
		contentLayout.setFlexGrow(1, grid);

		addComponent(contentLayout);
	}

	@Override
	public void enter(ViewChangeEvent event) {
		grid.reload();
		super.enter(event);
	}
}
