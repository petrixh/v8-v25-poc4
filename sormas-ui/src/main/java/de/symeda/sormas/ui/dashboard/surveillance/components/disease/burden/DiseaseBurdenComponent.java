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
package de.symeda.sormas.ui.dashboard.surveillance.components.disease.burden;

import java.util.List;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.disease.DiseaseBurdenDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.dashboard.DiseaseBurdenGrid;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.grid.HeightMode;

public class DiseaseBurdenComponent extends VerticalLayout {

	private static final long serialVersionUID = 6582975657305031105L;

	private DiseaseBurdenGrid grid;

	public DiseaseBurdenComponent() {

		Span title = new Span(I18nProperties.getCaption(Captions.dashboardDiseaseBurdenInfo));
		CssStyles.style(title, CssStyles.H2, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_NONE);

		grid = new DiseaseBurdenGrid();
		grid.setHeightMode(HeightMode.ROW);
		grid.setWidth(100, Unit.PERCENTAGE);

		// layout
		setWidth(100, Unit.PERCENTAGE);

		add(title);
		add(grid);
		// was a margin on all sides but the bottom (MarginInfo); spacing details belong to the theme work
		setPadding(true);
		setSpacing(false);
		setFlexGrow(1, grid);
	}

	public void refresh(List<DiseaseBurdenDto> diseasesBurden) {
		grid.reload(diseasesBurden);
		if (diseasesBurden != null && diseasesBurden.size() > 0) {
			grid.setHeightByRows(diseasesBurden.size());
		}
	}
}
