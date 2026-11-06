/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2024 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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
 */

package de.symeda.sormas.ui.adverseeventsfollowingimmunization.components.information;

import java.util.function.Consumer;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.immunization.ImmunizationDto;
import de.symeda.sormas.api.immunization.ImmunizationListEntryDto;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponent;

public class AefiImmunizationInfo extends SideComponent {

	public static final String SEPARATOR = ": ";

	private ImmunizationDto immunization;
	private final VerticalLayout mainLayout;

	public AefiImmunizationInfo(ImmunizationDto immunization, Consumer<Runnable> actionCallback) {
		super(I18nProperties.getString(Strings.entityImmunization), actionCallback);

		this.immunization = immunization;
		mainLayout = new VerticalLayout();
		mainLayout.setWidth(100, Unit.PERCENTAGE);
		mainLayout.setPadding(false);
		mainLayout.setSpacing(false);

		buildMainLayout();
		add(mainLayout);
	}

	public void buildMainLayout() {

		HorizontalLayout uuidReportLayout = new HorizontalLayout();
		uuidReportLayout.setPadding(false);
		uuidReportLayout.setSpacing(true);

		Span immunizationUuidLabel = new Span(DataHelper.getShortUuid(immunization.getUuid()));
		immunizationUuidLabel.setTitle(immunization.getUuid());
		CssStyles.style(immunizationUuidLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
		uuidReportLayout.add(immunizationUuidLabel);

		Span diseaseLabel = new Span(DataHelper.toStringNullable(immunization.getDisease()));
		CssStyles.style(diseaseLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
		uuidReportLayout.add(diseaseLabel);

		uuidReportLayout.setWidthFull();
		// was MIDDLE_LEFT / MIDDLE_RIGHT in a full-width layout: the two labels at the ends, centred vertically
		uuidReportLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		uuidReportLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		mainLayout.add(uuidReportLayout);

		HorizontalLayout meansOfImmunizationLayout = new HorizontalLayout();
		Span meansOfImmunizationLabel = new Span(
			I18nProperties.getPrefixCaption(ImmunizationListEntryDto.I18N_PREFIX, ImmunizationListEntryDto.MEANS_OF_IMMUNIZATION)
				+ SEPARATOR
				+ DataHelper.toStringNullable(immunization.getMeansOfImmunization()));
		meansOfImmunizationLayout.add(meansOfImmunizationLabel);
		mainLayout.add(meansOfImmunizationLayout);

		HorizontalLayout immunizationStatusLayout = new HorizontalLayout();
		Span immunizationStatusLabel = new Span(
			I18nProperties.getPrefixCaption(ImmunizationListEntryDto.I18N_PREFIX, ImmunizationListEntryDto.IMMUNIZATION_STATUS)
				+ SEPARATOR
				+ DataHelper.toStringNullable(immunization.getImmunizationStatus()));
		immunizationStatusLayout.add(immunizationStatusLabel);
		mainLayout.add(immunizationStatusLayout);

		HorizontalLayout managementStatusLayout = new HorizontalLayout();
		Span managementStatusLabel = new Span(
			I18nProperties.getPrefixCaption(ImmunizationListEntryDto.I18N_PREFIX, ImmunizationListEntryDto.IMMUNIZATION_MANAGEMENT_STATUS)
				+ SEPARATOR
				+ DataHelper.toStringNullable(immunization.getImmunizationManagementStatus()));
		managementStatusLayout.add(managementStatusLabel);
		mainLayout.add(managementStatusLayout);

		HorizontalLayout immunizationPeriodLayout = new HorizontalLayout();
		Span reportDateLabel = new Span(
			I18nProperties.getPrefixCaption(ImmunizationListEntryDto.I18N_PREFIX, ImmunizationListEntryDto.IMMUNIZATION_PERIOD)
				+ SEPARATOR
				+ DateFormatHelper.buildPeriodString(immunization.getStartDate(), immunization.getEndDate()));
		immunizationPeriodLayout.add(reportDateLabel);
		mainLayout.add(immunizationPeriodLayout);
	}
}
