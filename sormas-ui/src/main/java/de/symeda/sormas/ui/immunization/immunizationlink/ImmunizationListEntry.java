package de.symeda.sormas.ui.immunization.immunizationlink;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.immunization.ImmunizationListEntryDto;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentField;

public class ImmunizationListEntry extends SideComponentField {

	public static final String SEPARATOR = ": ";

	private final ImmunizationListEntryDto immunization;

	public ImmunizationListEntry(ImmunizationListEntryDto immunization) {
		this.immunization = immunization;

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
		// was MIDDLE_LEFT / MIDDLE_RIGHT alignment of the two labels
		uuidReportLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		uuidReportLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		addComponentToField(uuidReportLayout);

		HorizontalLayout meansOfImmunizationLayout = new HorizontalLayout();
		Span meansOfImmunizationLabel = new Span(
			I18nProperties.getPrefixCaption(ImmunizationListEntryDto.I18N_PREFIX, ImmunizationListEntryDto.MEANS_OF_IMMUNIZATION)
				+ SEPARATOR
				+ DataHelper.toStringNullable(immunization.getMeansOfImmunization()));
		meansOfImmunizationLayout.add(meansOfImmunizationLabel);
		addComponentToField(meansOfImmunizationLayout);

		HorizontalLayout immunizationStatusLayout = new HorizontalLayout();
		Span immunizationStatusLabel = new Span(
			I18nProperties.getPrefixCaption(ImmunizationListEntryDto.I18N_PREFIX, ImmunizationListEntryDto.IMMUNIZATION_STATUS)
				+ SEPARATOR
				+ DataHelper.toStringNullable(immunization.getImmunizationStatus()));
		immunizationStatusLayout.add(immunizationStatusLabel);
		addComponentToField(immunizationStatusLayout);

		HorizontalLayout managementStatusLayout = new HorizontalLayout();
		Span managementStatusLabel = new Span(
			I18nProperties.getPrefixCaption(ImmunizationListEntryDto.I18N_PREFIX, ImmunizationListEntryDto.IMMUNIZATION_MANAGEMENT_STATUS)
				+ SEPARATOR
				+ DataHelper.toStringNullable(immunization.getImmunizationManagementStatus()));
		managementStatusLayout.add(managementStatusLabel);
		addComponentToField(managementStatusLayout);

		HorizontalLayout immunizationPeriodLayout = new HorizontalLayout();
		Span reportDateLabel = new Span(
			I18nProperties.getPrefixCaption(ImmunizationListEntryDto.I18N_PREFIX, ImmunizationListEntryDto.IMMUNIZATION_PERIOD)
				+ SEPARATOR
				+ DateFormatHelper.buildPeriodString(immunization.getStartDate(), immunization.getEndDate()));
		immunizationPeriodLayout.add(reportDateLabel);
		addComponentToField(immunizationPeriodLayout);
	}

	public ImmunizationListEntryDto getImmunizationEntry() {
		return immunization;
	}
}
