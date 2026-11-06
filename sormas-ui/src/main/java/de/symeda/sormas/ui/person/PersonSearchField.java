/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2021 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.person;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.person.SimilarPersonDto;

public class PersonSearchField extends PersonSelectionField {

	public PersonSearchField(PersonDto referencePerson, String infoText) {
		super(referencePerson, infoText);
	}

	@Override
	protected Component initContent() {
		mainLayout = new VerticalLayout();
		mainLayout.setSpacing(true);
		// was MarginInfo(false, false, true, false), a bottom margin only (look, ignored at this stage)
		mainLayout.setPadding(false);
		mainLayout.setSizeUndefined();
		mainLayout.setWidth(100, Unit.PERCENTAGE);

		addInfoComponent();
		addFilterForm();
		filterForm.setVisible(true);
		setValue(null);
		mainLayout.add(personGrid);

		return mainLayout;
	}

	@Override
	protected void setPresentationValue(SimilarPersonDto newValue) {
		PersonSelectionField.selectInGrid(personGrid, newValue);
	}

	@Override
	protected void initializeGrid() {
		personGrid = new PersonSelectionGrid();

		personGrid.addSelectionListener(e -> {
			// the field value is the grid selection (Vaadin 8: getValue() read the grid)
			updateValue();

			if (selectionChangeCallback != null) {
				selectionChangeCallback.accept(!e.getAllSelectedItems().isEmpty());
			}
		});
	}
}
