/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2024 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.adverseeventsfollowingimmunization.components.information;

import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.adverseeventsfollowingimmunization.AefiDto;
import de.symeda.sormas.api.adverseeventsfollowingimmunization.AefiHelper;
import de.symeda.sormas.api.adverseeventsfollowingimmunization.AefiListEntryDto;
import de.symeda.sormas.api.adverseeventsfollowingimmunization.AefiType;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.utils.YesNoUnknown;
import de.symeda.sormas.api.vaccination.VaccinationDto;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponent;

public class AefiInfo extends SideComponent {

	public static final String SEPARATOR = ": ";

	private AefiDto aefi;
	private final VerticalLayout mainLayout;

	public AefiInfo(AefiDto aefi, Consumer<Runnable> actionCallback) {
		super(I18nProperties.getString(Strings.entityAdverseEvent), actionCallback);

		this.aefi = aefi;
		mainLayout = new VerticalLayout();
		mainLayout.setWidth(100, Unit.PERCENTAGE);
		mainLayout.setPadding(false);
		mainLayout.setSpacing(false);

		buildMainLayout();
		add(mainLayout);
	}

	public void buildMainLayout() {

		Span labelAefiType = new Span(AefiType.toString(aefi.getSerious()));
		CssStyles.style(labelAefiType, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
		if (aefi.getSerious() == YesNoUnknown.YES) {
			CssStyles.style(labelAefiType, CssStyles.LABEL_CRITICAL);
		}
		mainLayout.add(labelAefiType);

		Span labelVaccineName = new Span(aefi.getPrimarySuspectVaccine().getVaccineName().toString());
		CssStyles.style(labelVaccineName, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
		mainLayout.add(labelVaccineName);

		if (!StringUtils.isBlank(aefi.getPrimarySuspectVaccine().getVaccineDose())) {
			Span labelVaccineDose = new Span(
				I18nProperties.getPrefixCaption(VaccinationDto.I18N_PREFIX, VaccinationDto.VACCINE_DOSE)
					+ SEPARATOR
					+ aefi.getPrimarySuspectVaccine().getVaccineDose());
			CssStyles.style(labelVaccineDose, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
			mainLayout.add(labelVaccineDose);
		}

		Span labelVaccineDate = new Span(
			I18nProperties.getPrefixCaption(AefiListEntryDto.I18N_PREFIX, AefiListEntryDto.PRIMARY_VACCINE_VACCINATION_DATE)
				+ SEPARATOR
				+ DateFormatHelper.formatLocalDate(aefi.getPrimarySuspectVaccine().getVaccinationDate()));
		mainLayout.add(labelVaccineDate);

		Span labelAdverseEvents = new Span(StringUtils.abbreviate(AefiHelper.buildAdverseEventsString(aefi.getAdverseEvents()), 56));
		CssStyles.style(labelAdverseEvents, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
		mainLayout.add(labelAdverseEvents);
	}
}
