package de.symeda.sormas.ui.selfreport.selfreportLink;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.sample.SampleListEntryDto;
import de.symeda.sormas.api.selfreport.SelfReportDto;
import de.symeda.sormas.api.selfreport.SelfReportListEntryDto;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentField;

public class SelfReportListEntry extends SideComponentField {

	public SelfReportListEntry(SelfReportListEntryDto selfReportListEntryDto) {

		HorizontalLayout topLayout = new HorizontalLayout();
		topLayout.setWidthFull();
		topLayout.setPadding(false);
		topLayout.setSpacing(false);
		addComponentToField(topLayout);

		VerticalLayout topLeftLayout = new VerticalLayout();
		{
			topLeftLayout.setPadding(false);
			topLeftLayout.setSpacing(false);

			Span uuidLabel = new Span(
				I18nProperties.getPrefixCaption(SelfReportListEntryDto.I18N_PREFIX, SelfReportDto.UUID) + ": "
					+ DataHelper.getShortUuid(selfReportListEntryDto.getUuid()));
			CssStyles.style(uuidLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
			uuidLabel.setWidth(50, Unit.PERCENTAGE);
			topLeftLayout.add(uuidLabel);

			Span reportDateLabel = new Span(
				I18nProperties.getPrefixCaption(SelfReportListEntryDto.I18N_PREFIX, SelfReportDto.REPORT_DATE) + ": "
					+ DateFormatHelper.formatDate(selfReportListEntryDto.getReportingDate()));
			topLeftLayout.add(reportDateLabel);

			Span caseReferenceLabel = new Span(
				I18nProperties.getPrefixCaption(SelfReportListEntryDto.I18N_PREFIX, SelfReportDto.CASE_REFERENCE) + ": "
					+ selfReportListEntryDto.getCaseReference());
			topLeftLayout.add(caseReferenceLabel);

			Span diseaseLabel = new Span(
				I18nProperties.getPrefixCaption(SelfReportListEntryDto.I18N_PREFIX, SelfReportDto.DISEASE) + ": "
					+ selfReportListEntryDto.getDisease());
			topLeftLayout.add(diseaseLabel);

			Span dateOfTestLabel = new Span(
				I18nProperties.getPrefixCaption(SelfReportListEntryDto.I18N_PREFIX, SelfReportDto.DATE_OF_TEST) + ": "
					+ DateFormatHelper.formatDate(selfReportListEntryDto.getDateOfTest()));
			topLeftLayout.add(dateOfTestLabel);
		}

		topLayout.add(topLeftLayout);
		// was Alignment.TOP_LEFT
		topLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, topLeftLayout);
	}
}
