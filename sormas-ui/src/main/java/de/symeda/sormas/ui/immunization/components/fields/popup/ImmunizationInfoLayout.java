package de.symeda.sormas.ui.immunization.components.fields.popup;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.immunization.ImmunizationDto;
import de.symeda.sormas.api.utils.DateHelper;

public class ImmunizationInfoLayout extends HorizontalLayout {

	ImmunizationInfoLayout(ImmunizationDto immunization) {
		setSpacing(true);
		setSizeUndefined();

		Language userLanguage = I18nProperties.getUserLanguage();

		Span startDateField = new Span();
		String startDateFieldCaption = I18nProperties.getPrefixCaption(ImmunizationDto.I18N_PREFIX, ImmunizationDto.START_DATE);
		startDateField.setText(DateHelper.formatLocalDate(immunization.getStartDate(), userLanguage));
		startDateField.setWidth(null);
		add(withCaption(startDateFieldCaption, startDateField));

		Span endDateField = new Span();
		String endDateFieldCaption = I18nProperties.getPrefixCaption(ImmunizationDto.I18N_PREFIX, ImmunizationDto.END_DATE);
		endDateField.setText(DateHelper.formatLocalDate(immunization.getEndDate(), userLanguage));
		endDateField.setWidth(null);
		add(withCaption(endDateFieldCaption, endDateField));
	}

	/**
	 * Was a Vaadin 7 label with a caption: the caption is shown above the value, as Vaadin rendered it.
	 */
	private static Div withCaption(String caption, Span label) {
		Span captionLabel = new Span(caption);
		captionLabel.addClassName("v-caption");
		return new Div(captionLabel, new Div(label));
	}
}
