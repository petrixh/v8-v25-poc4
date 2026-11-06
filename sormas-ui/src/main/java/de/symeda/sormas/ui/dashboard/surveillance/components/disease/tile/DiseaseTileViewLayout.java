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
package de.symeda.sormas.ui.dashboard.surveillance.components.disease.tile;

import java.util.List;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.orderedlayout.FlexLayout;

import de.symeda.sormas.api.disease.DiseaseBurdenDto;

public class DiseaseTileViewLayout extends FlexLayout {

	private static final long serialVersionUID = 6582975657305031105L;

	/**
	 * Was a Vaadin 8 {@code CssLayout}: the tiles flow side by side and wrap.
	 */
	public DiseaseTileViewLayout() {
		setFlexWrap(FlexWrap.WRAP);
	}

	public void refresh(List<DiseaseBurdenDto> diseasesBurden) {
		this.removeAll();

		for (DiseaseBurdenDto diseaseBurden : diseasesBurden) {
			DiseaseTileComponent tile = new DiseaseTileComponent(diseaseBurden);
			tile.setWidth(230, Unit.PIXELS);
			// was CssLayout.getCss(..) for each tile
			tile.getStyle().set("margin-left", "18px").set("margin-bottom", "18px");
			add(tile);
		}
	}
}
