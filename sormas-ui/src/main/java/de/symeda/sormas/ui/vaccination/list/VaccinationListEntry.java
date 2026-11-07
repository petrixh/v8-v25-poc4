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

package de.symeda.sormas.ui.vaccination.list;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.caze.Vaccine;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.vaccination.VaccinationDto;
import de.symeda.sormas.api.vaccination.VaccinationListEntryDto;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentField;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

public class VaccinationListEntry extends SideComponentField {

	private static final long serialVersionUID = -8775209997959611902L;
	public static final String SEPARATOR = ": ";

	private final VaccinationListEntryDto vaccination;

	public VaccinationListEntry(VaccinationListEntryDto vaccination, boolean showDisease) {
		this.vaccination = vaccination;

		buildLayout(showDisease);
		setEnabled(vaccination.isRelevant());
	}

	private void buildLayout(boolean showDisease) {
		HorizontalLayout uuidDateLayout = new HorizontalLayout();
		uuidDateLayout.setPadding(false);
		uuidDateLayout.setSpacing(true);
		uuidDateLayout.setWidthFull();

		Span uuidLabel = new Span(DataHelper.getShortUuid(vaccination.getUuid()));
		uuidLabel.setTitle(vaccination.getUuid());
		CssStyles.style(uuidLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
		uuidDateLayout.add(uuidLabel);
		uuidDateLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, uuidLabel);

		Span dateLabel = new Span(
			vaccination.getVaccinationDate() != null
				? DateFormatHelper.formatDate(vaccination.getVaccinationDate())
				: I18nProperties.getString(Strings.labelNoVaccinationDate));
		CssStyles.style(dateLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
		uuidDateLayout.add(dateLabel);
		uuidDateLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, dateLabel);
		addComponentToField(uuidDateLayout);

		HorizontalLayout vaccineNameAndInfoLayout = new HorizontalLayout();
		vaccineNameAndInfoLayout.setPadding(false);
		vaccineNameAndInfoLayout.setSpacing(true);
		vaccineNameAndInfoLayout.setWidthFull();

		String vaccine = vaccination.getVaccineName() != null
			? (vaccination.getVaccineName() == Vaccine.OTHER ? vaccination.getOtherVaccineName() : vaccination.getVaccineName().toString())
			: null;
		Span vaccineLabel = new Span(
			StringUtils.isNotBlank(vaccine)
				? I18nProperties.getPrefixCaption(VaccinationDto.I18N_PREFIX, VaccinationDto.VACCINE_NAME) + SEPARATOR + vaccine
				: I18nProperties.getString(Strings.labelNoVaccineName));
		vaccineNameAndInfoLayout.add(vaccineLabel);
		vaccineNameAndInfoLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, vaccineLabel);

		if (!vaccination.isRelevant()) {
			Span vaccinationNotRelevantInfo = VaadinUiUtil.createLabel(HtmlRenderer.iconHtml(VaadinIcon.INFO_CIRCLE), ContentMode.HTML);
			VaadinUiUtil.setDescription(vaccinationNotRelevantInfo, vaccination.getNonRelevantMessage());
			vaccineNameAndInfoLayout.add(vaccinationNotRelevantInfo);
			vaccineNameAndInfoLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, vaccinationNotRelevantInfo);
		}

		addComponentToField(vaccineNameAndInfoLayout);

		if (showDisease) {
			Span diseaseLabel = new Span(vaccination.getDisease().toString());
			addComponentToField(diseaseLabel);
		}
	}

	public VaccinationListEntryDto getVaccination() {
		return vaccination;
	}

}
