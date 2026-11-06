package de.symeda.sormas.ui.caze.importer;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.DiseaseHelper;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.caze.CaseSelectionDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.facility.FacilityHelper;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.person.PersonHelper;
import de.symeda.sormas.ui.caze.CasePickOrCreateField;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.fields.CheckBox;

@SuppressWarnings("serial")
public class CasePickOrImportField extends CasePickOrCreateField {

	private CheckBox overrideCheckBox;

	public CasePickOrImportField(CaseDataDto newCase, PersonDto importedPerson, List<CaseSelectionDto> similarCases) {
		super(newCase, importedPerson, similarCases);
	}

	@Override
	protected void addInfoComponent() {

		HorizontalLayout infoLayout = new HorizontalLayout();
		infoLayout.setWidthFull();
		infoLayout.setSpacing(true);
		Image icon = new Image(VaadinUiUtil.THEME_IMAGE_PATH + "img/info-icon.png", "");
		icon.setHeight("35px");
		icon.setWidth("35px");
		infoLayout.add(icon);
		Span infoLabel = new Span(I18nProperties.getString(Strings.infoImportSimilarity));
		infoLayout.add(infoLabel);
		infoLayout.setFlexGrow(1, infoLabel);
		mainLayout.add(infoLayout);
		CssStyles.style(infoLayout, CssStyles.VSPACE_3);

		// Imported case info
		VerticalLayout caseInfoContainer = new VerticalLayout();
		caseInfoContainer.setWidthFull();
		CssStyles
			.style(caseInfoContainer, CssStyles.BACKGROUND_ROUNDED_CORNERS, CssStyles.BACKGROUND_SUB_CRITERIA, CssStyles.VSPACE_3, "v-scrollable");

		Span newCaseLabel = new Span(I18nProperties.getString(Strings.headingImportedCaseInfo));
		CssStyles.style(newCaseLabel, CssStyles.LABEL_BOLD, CssStyles.VSPACE_4);
		caseInfoContainer.add(newCaseLabel);

		HorizontalLayout caseInfoLayout = new HorizontalLayout();
		caseInfoLayout.setSpacing(true);
		caseInfoLayout.setSizeUndefined();
		{
			Span diseaseField = new Span();
			String diseaseFieldCaption = I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.DISEASE);
			diseaseField.setText(DiseaseHelper.toString(newCase.getDisease(), newCase.getDiseaseDetails()));
			diseaseField.setWidth(null);
			caseInfoLayout.add(withCaption(diseaseFieldCaption, diseaseField));

			Span reportDateField = new Span();
			String reportDateFieldCaption = I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.REPORT_DATE);
			reportDateField.setText(DateFormatHelper.formatDate(newCase.getReportDate()));
			reportDateField.setWidth(null);
			caseInfoLayout.add(withCaption(reportDateFieldCaption, reportDateField));

			Span responsibleRegionField = new Span();
			String responsibleRegionFieldCaption = I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.RESPONSIBLE_REGION);
			responsibleRegionField.setText(newCase.getResponsibleRegion().buildCaption());
			responsibleRegionField.setWidth(null);
			caseInfoLayout.add(withCaption(responsibleRegionFieldCaption, responsibleRegionField));

			Span responsibleDistrictField = new Span();
			String responsibleDistrictFieldCaption = I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.RESPONSIBLE_DISTRICT);
			responsibleDistrictField.setText(newCase.getResponsibleDistrict().buildCaption());
			responsibleDistrictField.setWidth(null);
			caseInfoLayout.add(withCaption(responsibleDistrictFieldCaption, responsibleDistrictField));

			if (newCase.getRegion() != null) {
				Span regionField = new Span();
				String regionFieldCaption = I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.REGION);
				regionField.setText(newCase.getRegion().buildCaption());
				regionField.setWidth(null);
				caseInfoLayout.add(withCaption(regionFieldCaption, regionField));
			}

			if (newCase.getDistrict() != null) {
				Span districtField = new Span();
				String districtFieldCaption = I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.DISTRICT);
				districtField.setText(newCase.getDistrict().buildCaption());
				districtField.setWidth(null);
				caseInfoLayout.add(withCaption(districtFieldCaption, districtField));
			}

			Span facilityField = new Span();
			String facilityFieldCaption = I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.HEALTH_FACILITY);
			facilityField.setText(
				FacilityHelper.buildFacilityString(
					null,
					newCase.getHealthFacility() != null ? newCase.getHealthFacility().buildCaption() : "",
					newCase.getHealthFacilityDetails()));
			facilityField.setWidth(null);
			caseInfoLayout.add(withCaption(facilityFieldCaption, facilityField));

			Span firstNameField = new Span();
			String firstNameFieldCaption = I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, PersonDto.FIRST_NAME);
			firstNameField.setText(newPerson.getFirstName());
			firstNameField.setWidth(null);
			caseInfoLayout.add(withCaption(firstNameFieldCaption, firstNameField));

			Span lastNameField = new Span();
			String lastNameFieldCaption = I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, PersonDto.LAST_NAME);
			lastNameField.setText(newPerson.getLastName());
			lastNameField.setWidth(null);
			caseInfoLayout.add(withCaption(lastNameFieldCaption, lastNameField));

			Span ageAndBirthDateField = new Span();
			String ageAndBirthDateFieldCaption = I18nProperties.getCaption(Captions.personAgeAndBirthdate);
			ageAndBirthDateField.setText(
				PersonHelper.getAgeAndBirthdateString(
					newPerson.getApproximateAge(),
					newPerson.getApproximateAgeType(),
					newPerson.getBirthdateDD(),
					newPerson.getBirthdateMM(),
					newPerson.getBirthdateYYYY()));
			ageAndBirthDateField.setWidth(null);
			caseInfoLayout.add(withCaption(ageAndBirthDateFieldCaption, ageAndBirthDateField));

			Span sexField = new Span();
			String sexFieldCaption = I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, PersonDto.SEX);
			sexField.setText(newPerson.getSex() != null ? newPerson.getSex().toString() : "");
			sexField.setWidth(null);
			caseInfoLayout.add(withCaption(sexFieldCaption, sexField));
		}

		caseInfoContainer.add(caseInfoLayout);
		mainLayout.add(caseInfoContainer);
	}

	@Override
	protected Component initContent() {

		addInfoComponent();
		addPickCaseComponent();

		overrideCheckBox = new CheckBox();
		overrideCheckBox.setCaption(I18nProperties.getCaption(Captions.caseImportMergeCase));
		CssStyles.style(overrideCheckBox, CssStyles.VSPACE_3);
		mainLayout.add(overrideCheckBox);

		addAndConfigureGrid();
		addCreateCaseComponent();

		pickCase.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				overrideCheckBox.setEnabled(true);
			}
		});

		createCase.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				overrideCheckBox.setEnabled(false);
			}
		});

		setPresentationValue(super.generateModelValue());
		pickCase.setValue(PICK_CASE);

		return mainLayout;
	}

	public boolean isOverrideCase() {
		return overrideCheckBox.getValue();
	}
}
