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
package de.symeda.sormas.ui.person;

import java.util.Arrays;
import java.util.List;


import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.person.PersonSimilarityCriteria;
import de.symeda.sormas.api.person.SimilarPersonDto;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.CustomizableGrid;
import de.symeda.sormas.ui.utils.grid.HeightMode;

@SuppressWarnings("serial")
public class PersonSelectionGrid extends CustomizableGrid<SimilarPersonDto> {

	/**
	 * Create a grid of persons listing all persons similar to the given criteria.
	 */
	public PersonSelectionGrid() {
		super(SimilarPersonDto.class);
		buildGrid();
	}

	private void buildGrid() {
		setSizeFull();
		setSelectionMode(SelectionMode.SINGLE);
		setHeightMode(HeightMode.ROW);

		setColumns(
			SimilarPersonDto.class,
			Arrays.asList(
				SimilarPersonDto.FIRST_NAME,
				SimilarPersonDto.LAST_NAME,
				SimilarPersonDto.NICKNAME,
				SimilarPersonDto.AGE_AND_BIRTH_DATE,
				SimilarPersonDto.SEX,
				SimilarPersonDto.PRESENT_CONDITION,
				SimilarPersonDto.PHONE,
				SimilarPersonDto.DISTRICT_NAME,
				SimilarPersonDto.COMMUNITY_NAME,
				SimilarPersonDto.POSTAL_CODE,
				SimilarPersonDto.CITY,
				SimilarPersonDto.STREET,
				SimilarPersonDto.HOUSE_NUMBER,
				SimilarPersonDto.NATIONAL_HEALTH_ID,
				SimilarPersonDto.PASSPORT_NUMBER));

		for (Column<SimilarPersonDto, ?> column : getAllColumns()) {
			String propertyId = column.getPropertyId().toString();
			String i18nPrefix = SimilarPersonDto.getI18nPrefix(propertyId);
			column.setHeaderCaption(I18nProperties.getPrefixCaption(i18nPrefix, propertyId, column.getHeaderCaption()));
		}

		getColumn(SimilarPersonDto.FIRST_NAME).setMinimumWidth(150);
		getColumn(SimilarPersonDto.LAST_NAME).setMinimumWidth(150);

		if (UiUtil.enabled(FeatureType.HIDE_JURISDICTION_FIELDS)) {
			getColumn(SimilarPersonDto.DISTRICT_NAME).setHidden(true);
			getColumn(SimilarPersonDto.COMMUNITY_NAME).setHidden(true);
		}
	}

	/**
	 * Load similar persons for the given criteria.
	 * 
	 * @param criteria
	 *            The person criteria.
	 */
	public void loadData(PersonSimilarityCriteria criteria) {
		List<SimilarPersonDto> similarPersons = FacadeProvider.getPersonFacade().getSimilarPersonDtos(criteria);

		setItems(similarPersons);
		setHeightByRows(similarPersons.size() > 0 ? (Math.min(similarPersons.size(), 10)) : 1);
	}

	public void loadData(List<SimilarPersonDto> similarPersons) {
		setItems(similarPersons);
		setHeightByRows(similarPersons.size() > 0 ? (Math.min(similarPersons.size(), 10)) : 1);
	}

}
