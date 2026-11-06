package de.symeda.sormas.ui.caze.caselink;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.caze.CaseListEntryDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.symptoms.SymptomsDto;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentField;

public class CaseListEntry extends SideComponentField {

	private static final long serialVersionUID = -7072019489290707667L;
	public static final String SEPARATOR = ": ";
	private final CaseListEntryDto caseListEntryDto;

	public CaseListEntry(CaseListEntryDto caseListEntryDto) {
		this.caseListEntryDto = caseListEntryDto;

		HorizontalLayout uuidReportDateLayout = new HorizontalLayout();
		uuidReportDateLayout.setPadding(false);
		uuidReportDateLayout.setSpacing(true);

		Span caseUuidLabel = new Span(DataHelper.toStringNullable(DataHelper.getShortUuid(caseListEntryDto.getUuid())));
		caseUuidLabel.setTitle(caseListEntryDto.getUuid());
		uuidReportDateLayout.add(caseUuidLabel);

		Span reportDateLabel = new Span(
			I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.REPORT_DATE)
				+ SEPARATOR
				+ DateFormatHelper.formatDate(caseListEntryDto.getReportDate()));
		uuidReportDateLayout.add(reportDateLabel);

		uuidReportDateLayout.setWidthFull();
		// was each label aligned left and right in its half
		uuidReportDateLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		uuidReportDateLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		addComponentToField(uuidReportDateLayout);

		HorizontalLayout diseaseClassificationLayout = new HorizontalLayout();
		diseaseClassificationLayout.setPadding(false);
		diseaseClassificationLayout.setSpacing(true);

		Span diseaseLabel = new Span(caseListEntryDto.getDisease().toString());
		diseaseLabel.addClassNames(CssStyles.LABEL_BOLD);
		diseaseLabel.setTitle(caseListEntryDto.getDisease().toString());

		Span classificationLabel = new Span(caseListEntryDto.getCaseClassification().toString());
		classificationLabel.addClassNames(CssStyles.LABEL_BOLD);
		classificationLabel.setTitle(caseListEntryDto.getCaseClassification().toString());

		diseaseClassificationLayout.add(diseaseLabel);
		diseaseClassificationLayout.add(classificationLabel);
		diseaseClassificationLayout.setWidthFull();
		// was each label aligned left and right in its half
		diseaseClassificationLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		diseaseClassificationLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		addComponentToField(diseaseClassificationLayout);

		if (caseListEntryDto.getSymptomsOnsetDate() != null) {
			HorizontalLayout symptomsOnsetDateLayout = new HorizontalLayout();
			uuidReportDateLayout.setPadding(false);
			uuidReportDateLayout.setSpacing(true);

			Span symptomsOnsetDateLabel = new Span(
				I18nProperties.getPrefixCaption(SymptomsDto.I18N_PREFIX, SymptomsDto.ONSET_DATE)
					+ SEPARATOR
					+ DateFormatHelper.formatDate(caseListEntryDto.getSymptomsOnsetDate()));
			symptomsOnsetDateLayout.add(symptomsOnsetDateLabel);

			symptomsOnsetDateLayout.setAlignItems(FlexComponent.Alignment.CENTER);
			addComponentToField(symptomsOnsetDateLayout);
		}
	}

	public CaseListEntryDto getCaseListEntryDto() {
		return caseListEntryDto;
	}
}
