package de.symeda.sormas.ui.immunization.components.fields.pickorcreate;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.immunization.ImmunizationDto;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.DateHelper;

public class ImmunizationInfoLayout extends HorizontalLayout {

	ImmunizationInfoLayout(ImmunizationDto immunization) {
		setSpacing(true);
		setSizeUndefined();

		Span uuidField = new Span();
		String uuidFieldCaption = I18nProperties.getPrefixCaption(ImmunizationDto.I18N_PREFIX, ImmunizationDto.UUID);
		uuidField.setText(DataHelper.getShortUuid(immunization.getUuid()));
		uuidField.setWidth(null);
		add(withCaption(uuidFieldCaption, uuidField));

		Span meansOfImmunizationField = new Span();
		String meansOfImmunizationFieldCaption = I18nProperties.getPrefixCaption(ImmunizationDto.I18N_PREFIX, ImmunizationDto.MEANS_OF_IMMUNIZATION);
		meansOfImmunizationField.setText(immunization.getMeansOfImmunization().toString());
		meansOfImmunizationField.setWidth(null);
		add(withCaption(meansOfImmunizationFieldCaption, meansOfImmunizationField));

		Span managementStatusField = new Span();
		String managementStatusFieldCaption = I18nProperties.getPrefixCaption(ImmunizationDto.I18N_PREFIX, ImmunizationDto.IMMUNIZATION_MANAGEMENT_STATUS);
		managementStatusField.setText(immunization.getImmunizationManagementStatus().toString());
		managementStatusField.setWidth(null);
		add(withCaption(managementStatusFieldCaption, managementStatusField));

		Span immunizationStatusField = new Span();
		String immunizationStatusFieldCaption = I18nProperties.getPrefixCaption(ImmunizationDto.I18N_PREFIX, ImmunizationDto.IMMUNIZATION_STATUS);
		immunizationStatusField.setText(immunization.getImmunizationStatus().toString());
		immunizationStatusField.setWidth(null);
		add(withCaption(immunizationStatusFieldCaption, immunizationStatusField));

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

		Span recoveryDateField = new Span();
		String recoveryDateFieldCaption = I18nProperties.getPrefixCaption(ImmunizationDto.I18N_PREFIX, ImmunizationDto.RECOVERY_DATE);
		recoveryDateField.setText(DateHelper.formatLocalDate(immunization.getRecoveryDate(), userLanguage));
		recoveryDateField.setWidth(null);
		add(withCaption(recoveryDateFieldCaption, recoveryDateField));
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
