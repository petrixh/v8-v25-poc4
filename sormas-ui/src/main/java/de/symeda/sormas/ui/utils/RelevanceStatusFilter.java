/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2023 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

import java.util.function.Consumer;

import com.vaadin.flow.component.Unit;


import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.fields.ComboBox;

public final class RelevanceStatusFilter {

	private RelevanceStatusFilter() {
	}

	public static ComboBox createRelevanceStatusFilter(
		String activeCaption,
		String archivedCaption,
		String allActiveAndArchivedCaption,
		String deletedCaption,
		EntityRelevanceStatus initialValue,
		UserRight deleteUserRight,
		Consumer<EntityRelevanceStatus> changeListener) {

		ComboBox relevanceStatusFilter = ComboBoxHelper.createComboBoxV7();
		relevanceStatusFilter.setId("relevanceStatus");
		relevanceStatusFilter.setWidth(260, Unit.PIXELS);
		relevanceStatusFilter.setNullSelectionAllowed(false);
		relevanceStatusFilter.addItems((Object[]) EntityRelevanceStatus.values());
		relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ACTIVE, I18nProperties.getCaption(activeCaption));

		if (UiUtil.permitted(UserRight.ENVIRONMENT_VIEW_ARCHIVED)) {
			relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ARCHIVED, I18nProperties.getCaption(archivedCaption));
			relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ACTIVE_AND_ARCHIVED, I18nProperties.getCaption(allActiveAndArchivedCaption));
		} else {
			relevanceStatusFilter.removeItem(EntityRelevanceStatus.ARCHIVED);
			relevanceStatusFilter.removeItem(EntityRelevanceStatus.ACTIVE_AND_ARCHIVED);
		}

		if (UiUtil.permitted(deleteUserRight)) {
			relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.DELETED, I18nProperties.getCaption(deletedCaption));
		} else {
			relevanceStatusFilter.removeItem(EntityRelevanceStatus.DELETED);
		}

		relevanceStatusFilter.setValue(initialValue);
		relevanceStatusFilter.addValueChangeListener(e -> changeListener.accept((EntityRelevanceStatus) e.getValue()));

		return relevanceStatusFilter;
	}
}
