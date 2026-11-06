package de.symeda.sormas.ui.caze;

import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.caze.CaseSelectionDto;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.facility.FacilityHelper;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.person.PersonHelper;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.caze.components.caseselection.CaseSelectionGrid;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.fields.CustomField;
import de.symeda.sormas.ui.utils.fields.OptionGroup;

@SuppressWarnings("serial")
public class CasePickOrCreateField extends CustomField<CaseSelectionDto> {

	public static final String CREATE_CASE = "createCase";
	public static final String PICK_CASE = "pickCase";

	protected List<CaseSelectionDto> similarCases;
	protected CaseSelectionGrid grid;
	protected OptionGroup pickCase;
	protected OptionGroup createCase;
	protected Consumer<Boolean> selectionChangeCallback;
	protected CaseDataDto newCase;
	protected PersonDto newPerson;

	protected VerticalLayout mainLayout;

	public CasePickOrCreateField(CaseDataDto newCase, PersonDto newPerson, List<CaseSelectionDto> similarCases) {

		this.similarCases = similarCases;
		this.newCase = newCase;
		this.newPerson = newPerson;

		mainLayout = new VerticalLayout();
		mainLayout.setSpacing(true);
		mainLayout.setSizeUndefined();
		mainLayout.setWidthFull();
	}

	protected void addInfoComponent() {

		HorizontalLayout infoLayout = new HorizontalLayout();
		infoLayout.setWidthFull();
		infoLayout.setSpacing(true);
		Image icon = new Image(VaadinUiUtil.THEME_IMAGE_PATH + "img/info-icon.png", "");
		icon.setHeight("35px");
		icon.setWidth("35px");
		infoLayout.add(icon);
		Span infoLabel = VaadinUiUtil.createLabel(I18nProperties.getString(Strings.infoPickOrCreateCase), ContentMode.HTML);
		infoLayout.add(infoLabel);
		infoLayout.setFlexGrow(1, infoLabel);
		mainLayout.add(infoLayout);
		CssStyles.style(infoLayout, CssStyles.VSPACE_3);
		// Imported case info
		VerticalLayout caseInfoContainer = new VerticalLayout();
		caseInfoContainer.setWidthFull();
		CssStyles
			.style(caseInfoContainer, CssStyles.BACKGROUND_ROUNDED_CORNERS, CssStyles.BACKGROUND_SUB_CRITERIA, CssStyles.VSPACE_3, "v-scrollable");

		Span newCaseLabel = new Span(I18nProperties.getString(Strings.infoPickOrCreateCaseNewCase));
		CssStyles.style(newCaseLabel, CssStyles.LABEL_BOLD, CssStyles.VSPACE_4);
		caseInfoContainer.add(newCaseLabel);

		HorizontalLayout caseInfoLayout = new HorizontalLayout();
		caseInfoLayout.setSpacing(true);
		caseInfoLayout.setSizeUndefined();
		{
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

			if (UiUtil.disabled(FeatureType.HIDE_JURISDICTION_FIELDS)) {
				Span responsibleDistrictField = new Span();
				String responsibleDistrictFieldCaption = I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.RESPONSIBLE_DISTRICT);
				responsibleDistrictField.setText(newCase.getResponsibleDistrict() != null ? newCase.getResponsibleDistrict().buildCaption() : "");
				responsibleDistrictField.setWidth(null);
				caseInfoLayout.add(withCaption(responsibleDistrictFieldCaption, responsibleDistrictField));

				if (newCase.getDistrict() != null) {
					Span districtField = new Span();
					String districtFieldCaption = I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.DISTRICT);
					districtField.setText(newCase.getDistrict().buildCaption());
					districtField.setWidth(null);
					caseInfoLayout.add(withCaption(districtFieldCaption, districtField));
				}
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

			Span reportDateField = new Span();
			String reportDateFieldCaption = I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.REPORT_DATE);
			reportDateField.setText(DateFormatHelper.formatDate(newCase.getReportDate()));
			reportDateField.setWidth(null);
			caseInfoLayout.add(withCaption(reportDateFieldCaption, reportDateField));

			Span sexField = new Span();
			String sexFieldCaption = I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, PersonDto.SEX);
			sexField.setText(newPerson.getSex() != null ? newPerson.getSex().toString() : "");
			sexField.setWidth(null);
			caseInfoLayout.add(withCaption(sexFieldCaption, sexField));

			Span classificationField = new Span();
			String classificationFieldCaption = I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.CASE_CLASSIFICATION);
			classificationField.setText(newCase.getCaseClassification().toString());
			classificationField.setWidth(null);
			caseInfoLayout.add(withCaption(classificationFieldCaption, classificationField));

			Span outcomeField = new Span();
			String outcomeFieldCaption = I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.OUTCOME);
			outcomeField.setText(newCase.getOutcome().toString());
			outcomeField.setWidth(null);
			caseInfoLayout.add(withCaption(outcomeFieldCaption, outcomeField));

		}

		caseInfoContainer.add(caseInfoLayout);
		mainLayout.add(caseInfoContainer);
	}

	/**
	 * Was a Vaadin 7 label with a caption: the caption is shown above the value, as Vaadin rendered it.
	 */
	protected static Div withCaption(String caption, Span label) {
		Span captionLabel = new Span(caption);
		captionLabel.addClassName("v-caption");
		return new Div(captionLabel, new Div(label));
	}

	protected void addPickCaseComponent() {

		pickCase = new OptionGroup(null);
		pickCase.addItem(PICK_CASE);
		pickCase.setItemCaption(PICK_CASE, I18nProperties.getCaption(Captions.casePickCase));
		CssStyles.style(pickCase, CssStyles.VSPACE_NONE);
		pickCase.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				createCase.setValue(null);
				grid.setEnabled(true);
				if (similarCases.size() == 1) {
					pickCase.setValue(PICK_CASE);
					grid.select(similarCases.get(0));
				}
				if (selectionChangeCallback != null) {
					selectionChangeCallback.accept(!grid.getSelectedItems().isEmpty());
				}
			}
		});
		mainLayout.add(pickCase);
	}

	protected void addAndConfigureGrid() {

		initGrid();

		grid.setEnabled(false);
		grid.addSelectionListener(e -> {
			if (e.getAllSelectedItems().size() > 0) {
				createCase.setValue(null);
			}
			updateValue();
		});
		mainLayout.add(grid);

		grid.addSelectionListener(e -> {
			if (selectionChangeCallback != null) {
				selectionChangeCallback.accept(!e.getAllSelectedItems().isEmpty());
			}
		});
	}

	protected void addCreateCaseComponent() {

		createCase = new OptionGroup(null);
		createCase.addItem(CREATE_CASE);
		createCase.setItemCaption(CREATE_CASE, I18nProperties.getCaption(Captions.caseCreateCase));
		createCase.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				pickCase.setValue(null);
				grid.deselectAll();
				grid.setEnabled(false);
				if (selectionChangeCallback != null) {
					selectionChangeCallback.accept(true);
				}
			}
		});
		mainLayout.add(createCase);
	}

	@Override
	protected Component initContent() {

		addInfoComponent();
		addPickCaseComponent();
		addAndConfigureGrid();
		addCreateCaseComponent();

		grid.setEnabled(false);

		return mainLayout;
	}

	private void initGrid() {
		if (grid == null) {
			grid = new CaseSelectionGrid(similarCases);
		}
	}

	@Override
	protected void setPresentationValue(CaseSelectionDto newValue) {

		super.setPresentationValue(newValue);

		if (pickCase != null) {
			pickCase.setValue(PICK_CASE);
		}

		if (newValue != null) {
			grid.select(newValue);
		}
	}

	@Override
	protected CaseSelectionDto generateModelValue() {

		if (grid != null) {
			CaseSelectionDto value = grid.getSelectedItems().stream().findFirst().orElse(null);
			return value;
		}

		return super.generateModelValue();
	}

	@Override
	public Class<? extends CaseSelectionDto> getType() {
		return CaseSelectionDto.class;
	}

	public void setSelectionChangeCallback(Consumer<Boolean> callback) {
		this.selectionChangeCallback = callback;
	}
}
