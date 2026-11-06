/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.utils;

import de.symeda.sormas.api.ReferenceDto;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.fields.FilteringMode;

public class ComboBoxWithPlaceholder extends ComboBox {

	private static final long serialVersionUID = 27751677500982303L;

	PlaceholderReferenceDto placeholder = null;

	public ComboBoxWithPlaceholder() {
		super();
		this.setFilteringMode(FilteringMode.CONTAINS);
	}

	/**
	 * V7 showed the placeholder as the caption of the null selection item; Flow shows it as the placeholder text of the
	 * empty combo box. Null selection stays allowed.
	 */
	@Override
	public void setPlaceholder(String placeholderText) {
		if (placeholder == null) {
			placeholder = new PlaceholderReferenceDto(placeholderText);
			setNullSelectionAllowed(true);
		} else {
			placeholder.setCaption(placeholderText);
		}
		super.setPlaceholder(placeholderText);
	}

	public static class PlaceholderReferenceDto extends ReferenceDto {

		private static final long serialVersionUID = -5675799622797725127L;

		public PlaceholderReferenceDto(String caption) {
			super(null, caption);
		}
	}
}
