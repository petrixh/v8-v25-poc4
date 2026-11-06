package de.symeda.sormas.ui.survey;

import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.fields.TextField;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.survey.SurveyCriteria;
import de.symeda.sormas.api.survey.SurveyDto;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.ui.utils.AbstractFilterForm;
import de.symeda.sormas.ui.utils.FieldConfiguration;

public class SurveyFilterForm extends AbstractFilterForm<SurveyCriteria> {

	public SurveyFilterForm() {
		super(SurveyCriteria.class, SurveyDto.I18N_PREFIX, null);
	}

	@Override
	protected String[] getMainFilterLocators() {
		return new String[] {
			SurveyCriteria.FREE_TEXT,
			SurveyCriteria.DISEASE };
	}

	@Override
	protected void addFields() {
		UserDto user = currentUserDto();

		addField(FieldConfiguration.pixelSized(SurveyCriteria.DISEASE, 140), ComboBox.class);
		addField(
			FieldConfiguration.withCaptionAndPixelSized(SurveyCriteria.FREE_TEXT, I18nProperties.getString(Strings.promptSurveyFreeTextSearch), 200),
			TextField.class);
	}
}
