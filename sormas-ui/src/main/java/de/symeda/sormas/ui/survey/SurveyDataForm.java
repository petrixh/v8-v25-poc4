package de.symeda.sormas.ui.survey;

import static de.symeda.sormas.ui.utils.LayoutUtil.fluidRowLocs;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.symeda.sormas.ui.utils.fields.TextField;

import de.symeda.sormas.api.docgeneneration.DocumentTemplateCriteria;
import de.symeda.sormas.api.docgeneneration.DocumentWorkflow;
import de.symeda.sormas.api.survey.SurveyDto;
import de.symeda.sormas.api.survey.SurveyReferenceDto;
import de.symeda.sormas.ui.configuration.docgeneration.DocumentTemplateSection;
import de.symeda.sormas.ui.utils.AbstractEditForm;

public class SurveyDataForm extends AbstractEditForm<SurveyDto> {

	private static final String SURVEY_DOCUMENT_SECTION = "surveyDocumentSection";

	//@formatter:off
	private static final String HTML_LAYOUT =
			fluidRowLocs(SurveyDto.SURVEY_NAME, "") + fluidRowLocs(SurveyDto.DISEASE, "")
			+ fluidRowLocs(SurveyDto.EXTERNAL_ID, "")
			+ fluidRowLocs(SURVEY_DOCUMENT_SECTION, "");
	//@formatter:on

	private VerticalLayout gridLayout;
	private SurveyReferenceDto surveyReference;

	public SurveyDataForm(boolean isEditAllowed, SurveyReferenceDto surveyReference) {
		super(SurveyDto.class, SurveyDto.I18N_PREFIX, false, null, null, isEditAllowed);
		this.surveyReference = surveyReference;
		addFields();
	}

	@Override
	protected String createHtmlLayout() {
		return HTML_LAYOUT;
	}

	@Override
	protected void addFields() {
		addDiseaseField(SurveyDto.DISEASE, false, true, false);

		addField(SurveyDto.SURVEY_NAME, TextField.class);

		addField(SurveyDto.EXTERNAL_ID, TextField.class);

		DocumentTemplateSection documentTemplateSection = new DocumentTemplateSection(
			new DocumentTemplateCriteria(DocumentWorkflow.SURVEY_DOCUMENT, null, surveyReference),
			false,
			new SurveyDocumentTemplateReceiver(DocumentWorkflow.SURVEY_DOCUMENT, surveyReference));
		DocumentTemplateSection emailTemplateSection = new DocumentTemplateSection(
			new DocumentTemplateCriteria(DocumentWorkflow.SURVEY_EMAIL, null, surveyReference),
			false,
			new SurveyEmailTemplateReceiver(DocumentWorkflow.SURVEY_EMAIL, surveyReference));

		documentTemplateSection.setPadding(false);
		emailTemplateSection.setPadding(false);

		gridLayout = new VerticalLayout(documentTemplateSection, emailTemplateSection);

		gridLayout.setWidthFull();
		// was a top and bottom margin (MarginInfo(true, false, true, false)); spacing details belong to the theme work
		gridLayout.setPadding(false);
		gridLayout.setSpacing(true);
		gridLayout.setClassName("crud-main-layout");

		getContent().addComponent(gridLayout, SURVEY_DOCUMENT_SECTION);

		setRequired(true, SurveyDto.SURVEY_NAME, SurveyDto.DISEASE);
	}
}
