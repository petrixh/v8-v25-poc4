package de.symeda.sormas.ui.reports.aggregate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.server.ErrorMessage;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.ErrorLevel;
import com.vaadin.ui.Alignment;
import com.vaadin.flow.component.button.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.v7.data.fieldgroup.FieldGroup;
import de.symeda.sormas.ui.utils.fields.OptionGroup;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.facility.FacilityReferenceDto;
import de.symeda.sormas.api.infrastructure.pointofentry.PointOfEntryReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.report.AggregateReportCriteria;
import de.symeda.sormas.api.report.AggregateReportDto;
import de.symeda.sormas.api.report.DiseaseAgeGroup;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.utils.AgeGroupUtils;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.EpiWeek;
import de.symeda.sormas.ui.SormasUI;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.EpiWeekFilterOption;

/**
 * @author Christopher Riedel
 * 
 *         Should better be an edit form and use an new DTO that contains the
 *         list of AggregateReport entries. The save method should be moved to a
 *         controller
 */
public class AggregateReportsEditLayout extends VerticalLayout {

	private static final long serialVersionUID = -7806379599024578146L;

	private Dialog window;

	private OptionGroup epiWeekOptions;
	private ComboBox<Integer> comboBoxYear;
	private ComboBox<EpiWeek> comboBoxEpiWeek;
	private ComboBox<RegionReferenceDto> comboBoxRegion;
	private ComboBox<DistrictReferenceDto> comboBoxDistrict;
	private ComboBox<FacilityReferenceDto> comboBoxFacility;
	private ComboBox<PointOfEntryReferenceDto> comboBoxPoe;
	private Button deleteButton;
	private Button cancelButton;
	private Button saveButton;
	private List<AggregateReportEditForm> editForms = new ArrayList<>();
	private Map<String, Disease> diseaseMap;
	private List<Disease> diseasesWithoutReport = new ArrayList<>();
	private List<DiseaseAgeGroup> diseaseAgeGroupsWithoutReport = new ArrayList<>();
	private List<AggregateReportDto> reports;
	private Span duplicateMessage;
	private boolean existsExpiredAgeGroups = false;
	private Span expiredAgeGroupsMessage;

	public AggregateReportsEditLayout(Dialog window, boolean edit, AggregateReportDto selectedAggregateReport) {

		setWidth(560, Unit.PIXELS);
		setSpacing(false);

		this.window = window;

		epiWeekOptions = new OptionGroup();
		epiWeekOptions.addItems(EpiWeekFilterOption.values());
		epiWeekOptions.addClassNames(SormasFieldGroupFieldFactory.OPTIONGROUP_HORIZONTAL, CssStyles.OPTIONGROUP_HORIZONTAL_PRIMARY);
		CssStyles.style(epiWeekOptions, SormasFieldGroupFieldFactory.OPTIONGROUP_HORIZONTAL);
		epiWeekOptions.addValueChangeListener(e -> updateEpiWeekFields());
		if (!edit) {
			add(epiWeekOptions);
		}

		comboBoxYear = new ComboBox<>(I18nProperties.getString(Strings.year), DateHelper.getYearsToNow(2000));
		comboBoxYear.setWidth(250, Unit.PIXELS);
		comboBoxYear.addValueChangeListener(e -> {
			updateEpiWeekFields();
			setEpiWeekComponentErrors();
		});
		comboBoxYear.setRequiredIndicatorVisible(true);

		comboBoxEpiWeek = new ComboBox<>(I18nProperties.getString(Strings.epiWeek));
		comboBoxEpiWeek.setWidth(250, Unit.PIXELS);
		comboBoxEpiWeek.addValueChangeListener(e -> {
			checkForExistingData(edit);
			setEpiWeekComponentErrors();
		});
		comboBoxEpiWeek.setRequiredIndicatorVisible(true);
		add(new HorizontalLayout(comboBoxYear, comboBoxEpiWeek));

		comboBoxRegion = new ComboBox<>();
		comboBoxRegion.setWidth(250, Unit.PIXELS);
		comboBoxRegion.setItemLabelGenerator(item -> item.buildCaption());
		comboBoxRegion.setCaption(I18nProperties.getPrefixCaption(AggregateReportDto.I18N_PREFIX, AggregateReportDto.REGION));
		comboBoxRegion.setItems(FacadeProvider.getRegionFacade().getAllActiveByServerCountry());
		comboBoxRegion.setRequiredIndicatorVisible(true);
		comboBoxRegion.addValueChangeListener(e -> {
			RegionReferenceDto region = e.getValue();
			comboBoxDistrict.clear();
			if (region != null) {
				checkForExistingData(edit);
				comboBoxRegion.setComponentError(null);
				comboBoxDistrict.setItems(FacadeProvider.getDistrictFacade().getAllActiveByRegion(region.getUuid()));
				comboBoxDistrict.setEnabled(true);
			} else {
				comboBoxRegion.setComponentError(new UserError(I18nProperties.getString(Validations.required)));
				comboBoxDistrict.setEnabled(false);
			}
		});

		comboBoxDistrict = new ComboBox<>();
		comboBoxDistrict.setItemLabelGenerator(item -> item.buildCaption());
		comboBoxDistrict.setWidth(250, Unit.PIXELS);
		comboBoxDistrict.setCaption(I18nProperties.getPrefixCaption(AggregateReportDto.I18N_PREFIX, AggregateReportDto.DISTRICT));
		comboBoxDistrict.setRequiredIndicatorVisible(true);
		comboBoxDistrict.addValueChangeListener(e -> {
			DistrictReferenceDto district = e.getValue();
			if (comboBoxFacility != null) {
				comboBoxFacility.clear();
			}
			if (comboBoxPoe != null) {
				comboBoxPoe.clear();
			}
			if (district != null) {
				checkForExistingData(edit);
				comboBoxDistrict.setComponentError(null);
				if (comboBoxFacility != null && !UiUtil.isPortHealthUser()) {
					comboBoxFacility.setItems(FacadeProvider.getFacilityFacade().getActiveHospitalsByDistrict(district, false));
					comboBoxFacility.setEnabled(true);
				}
				if (comboBoxPoe != null) {
					comboBoxPoe.setItems(FacadeProvider.getPointOfEntryFacade().getAllActiveByDistrict(district.getUuid(), false));
					comboBoxPoe.setEnabled(true);
				}
			} else {
				comboBoxDistrict.setComponentError(new UserError(I18nProperties.getString(Validations.required)));
				comboBoxFacility.setEnabled(false);
				comboBoxPoe.setEnabled(false);
			}
		});

		comboBoxDistrict.setEnabled(false);
		add(new HorizontalLayout(comboBoxRegion, comboBoxDistrict));

		comboBoxFacility = new ComboBox<>();
		comboBoxFacility.setItemLabelGenerator(item -> item.buildCaption());
		comboBoxFacility.setWidth(250, Unit.PIXELS);
		comboBoxFacility.setCaption(I18nProperties.getPrefixCaption(AggregateReportDto.I18N_PREFIX, AggregateReportDto.HEALTH_FACILITY));
		comboBoxFacility.setEnabled(false);
		comboBoxFacility.addValueChangeListener(e -> {
			if (comboBoxFacility.getValue() != null) {
				comboBoxPoe.clear();
			}
			checkForExistingData(edit);
		});

		comboBoxPoe = new ComboBox<>();
		comboBoxPoe.setItemLabelGenerator(item -> item.buildCaption());
		comboBoxPoe.setWidth(250, Unit.PIXELS);
		comboBoxPoe.setCaption(I18nProperties.getPrefixCaption(AggregateReportDto.I18N_PREFIX, AggregateReportDto.POINT_OF_ENTRY));
		comboBoxPoe.setEnabled(false);
		if (UiUtil.isPortHealthUser()) {
			comboBoxPoe.setRequiredIndicatorVisible(true);
		}
		comboBoxPoe.addValueChangeListener(e -> {
			if (comboBoxPoe.getValue() != null) {
				comboBoxFacility.clear();
			} else {
				if (UiUtil.isPortHealthUser()) {
					comboBoxPoe.setComponentError(new UserError(I18nProperties.getString(Validations.required)));
				}
			}
			checkForExistingData(edit);
		});
		add(new HorizontalLayout(comboBoxFacility, comboBoxPoe));

		duplicateMessage = new Span(I18nProperties.getString(Strings.messageAggregateReportFound));
		duplicateMessage.addClassNames(CssStyles.LABEL_WHITE_SPACE_NORMAL, CssStyles.LABEL_CRITICAL);
		duplicateMessage.setVisible(false);
		add(new HorizontalLayout(duplicateMessage));

		if (edit) {
			comboBoxYear.setValue(selectedAggregateReport.getYear());
			comboBoxYear.setEnabled(false);
			comboBoxEpiWeek.setValue(new EpiWeek(selectedAggregateReport.getYear(), selectedAggregateReport.getEpiWeek()));
			comboBoxEpiWeek.setEnabled(false);
			comboBoxRegion.setValue(selectedAggregateReport.getRegion());
			comboBoxRegion.setEnabled(false);
			comboBoxDistrict.setValue(selectedAggregateReport.getDistrict());
			comboBoxDistrict.setEnabled(false);
			comboBoxFacility.setValue(selectedAggregateReport.getHealthFacility());
			comboBoxFacility.setEnabled(false);
			comboBoxPoe.setValue(selectedAggregateReport.getPointOfEntry());
			comboBoxPoe.setEnabled(false);

			reports = FacadeProvider.getAggregateReportFacade().getSimilarAggregateReports(selectedAggregateReport);
		}

		List<Disease> diseaseList = FacadeProvider.getDiseaseConfigurationFacade().getAllDiseases(true, null, false, true);
		Map<Disease, Set<String>> diseasesWithReports = new HashMap<>();
		diseaseMap = diseaseList.stream().collect(Collectors.toMap(Disease::toString, disease -> disease));
		diseaseMap.values().forEach(disease -> {
			List<String> ageGroups = FacadeProvider.getDiseaseConfigurationFacade().getAgeGroups(disease);
			if (ageGroups != null) {
				ageGroups.forEach(ageGroup -> {
					diseaseAgeGroupsWithoutReport.add(new DiseaseAgeGroup(disease, ageGroup));
				});
			} else {
				diseaseAgeGroupsWithoutReport.add(new DiseaseAgeGroup(disease, null));
			}
			diseasesWithoutReport.add(disease);
		});

		if (reports != null) {
			for (AggregateReportDto report : reports) {

				String firstAgeGroup = FacadeProvider.getDiseaseConfigurationFacade().getFirstAgeGroup(report.getDisease());

				boolean isFirstAgeGroup = report.getAgeGroup() != null && report.getAgeGroup().equals(firstAgeGroup);

				if (report.isExpiredAgeGroup()) {
					existsExpiredAgeGroups = true;
				}

				AggregateReportEditForm editForm =
					new AggregateReportEditForm(report.getDisease(), report.getAgeGroup(), isFirstAgeGroup, report.isExpiredAgeGroup());
				editForm.setNewCases(report.getNewCases());
				editForm.setLabConfirmations(report.getLabConfirmations());
				editForm.setDeaths(report.getDeaths());
				editForms.add(editForm);
				diseaseAgeGroupsWithoutReport.remove(new DiseaseAgeGroup(report.getDisease(), report.getAgeGroup()));
				diseasesWithReports.putIfAbsent(report.getDisease(), new HashSet<>());
				diseasesWithReports.get(report.getDisease()).add(report.getAgeGroup());
				diseasesWithoutReport.remove(report.getDisease());
			}
		}

		for (Disease disease : diseasesWithoutReport) {

			List<String> ageGroups = FacadeProvider.getDiseaseConfigurationFacade().getAgeGroups(disease);
			if (ageGroups == null || ageGroups.isEmpty()) {
				editForms.add(new AggregateReportEditForm(disease, null, false, false));
			} else {
				int i = 0;
				for (String ageGroup : ageGroups) {
					editForms.add(new AggregateReportEditForm(disease, ageGroup, i == 0, false));
					i++;
				}
			}
		}

		for (Disease disease : diseasesWithReports.keySet()) {

			List<String> ageGroups = FacadeProvider.getDiseaseConfigurationFacade().getAgeGroups(disease);
			if (ageGroups != null) {
				int i = 0;
				for (String ageGroup : ageGroups) {
					if (!diseasesWithReports.get(disease).contains(ageGroup)) {
						editForms.add(new AggregateReportEditForm(disease, ageGroup, i == 0, false));
					}
					i++;
				}
			} else {
				if (!diseasesWithReports.get(disease).contains(null)) {
					editForms.add(new AggregateReportEditForm(disease, null, true, false));
				}
			}
		}

		Span legend = new Span(
			String.format(
				I18nProperties.getString(Strings.aggregateReportLegend),
				I18nProperties.getCaption(Captions.aggregateReportNewCasesShort),
				I18nProperties.getPrefixCaption(AggregateReportDto.I18N_PREFIX, AggregateReportDto.NEW_CASES),
				I18nProperties.getCaption(Captions.aggregateReportLabConfirmationsShort),
				I18nProperties.getPrefixCaption(AggregateReportDto.I18N_PREFIX, AggregateReportDto.LAB_CONFIRMATIONS),
				I18nProperties.getCaption(Captions.aggregateReportDeathsShort),
				I18nProperties.getPrefixCaption(AggregateReportDto.I18N_PREFIX, AggregateReportDto.DEATHS)));
		add(legend);
		legend.addClassName(CssStyles.VSPACE_TOP_1);

		editForms = editForms.stream()
			.sorted(
				Comparator.comparing(AggregateReportEditForm::getDisease, Comparator.nullsFirst(Comparator.comparing(Disease::toString)))
					.thenComparing(AggregateReportEditForm::isExpiredAgeGroup)
					.thenComparing(AggregateReportEditForm::getAgeGroup, AgeGroupUtils.getComparator()))
			.collect(Collectors.toList());

		editForms.forEach(this::addComponent);

		if (!editForms.isEmpty()) {
			editForms.get(0).addClassName(CssStyles.VSPACE_TOP_1);
		}

		if (existsExpiredAgeGroups) {
			expiredAgeGroupsMessage = new Span(I18nProperties.getString(Strings.messageAggregateReportExpiredAgeGroups));
			expiredAgeGroupsMessage.addClassNames(CssStyles.LABEL_WHITE_SPACE_NORMAL, CssStyles.LABEL_CRITICAL);
			expiredAgeGroupsMessage.setVisible(true);
			add(new HorizontalLayout(expiredAgeGroupsMessage));
		}

		HorizontalLayout buttonsPanel = new HorizontalLayout();
		buttonsPanel.setPadding(false);
		buttonsPanel.setSpacing(true);
		buttonsPanel.setWidthFull();

		if (edit) {
			deleteButton = ButtonHelper.createButton(Captions.actionDelete, clickEvent -> {
				deleteAggregateReports();
			});
			buttonsPanel.add(deleteButton);
			buttonsPanel.setComponentAlignment(deleteButton, Alignment.BOTTOM_RIGHT);
			buttonsPanel.setFlexGrow(0, deleteButton);
			checkForExistingData(edit);
		}

		cancelButton = ButtonHelper.createButton(Captions.actionDiscard, e -> window.close());

		buttonsPanel.add(cancelButton);
		buttonsPanel.setComponentAlignment(cancelButton, Alignment.BOTTOM_RIGHT);
		buttonsPanel.setFlexGrow(1, cancelButton);

		saveButton = ButtonHelper.createButton(Captions.actionSave, event -> save(), CssStyles.BUTTON_PRIMARY);

		buttonsPanel.add(saveButton);
		buttonsPanel.setComponentAlignment(saveButton, Alignment.BOTTOM_RIGHT);
		buttonsPanel.setFlexGrow(0, saveButton);

		buttonsPanel.addClassName(CssStyles.VSPACE_TOP_2);
		add(buttonsPanel);
		setComponentAlignment(buttonsPanel, Alignment.BOTTOM_RIGHT);

		if (!edit) {
			initialize();
		}
	}

	private void checkForExistingData(boolean editChecking) {
		duplicateMessage.setVisible(false);
		if (comboBoxEpiWeek.getValue() != null && comboBoxRegion.getValue() != null && comboBoxDistrict.getValue() != null) {
			AggregateReportCriteria criteria = new AggregateReportCriteria();
			criteria.setDistrict(comboBoxDistrict.getValue());
			criteria.setEpiWeekFrom(comboBoxEpiWeek.getValue());
			criteria.setEpiWeekTo(comboBoxEpiWeek.getValue());
			criteria.setHealthFacility(comboBoxFacility.getValue());
			criteria.setPointOfEntry(comboBoxPoe.getValue());
			criteria.setRegion(comboBoxRegion.getValue());
			criteria.setConsiderNullJurisdictionCheck(true);
			List<AggregateReportDto> duplicateReports = FacadeProvider.getAggregateReportFacade().getAggregateReports(criteria);

			//if there is an edit operation edited aggregate reports are removed in order to avoid false duplicates
			if (editChecking && !duplicateReports.isEmpty() && reports != null && !reports.isEmpty()) {
				duplicateReports.removeIf(duplicateReport -> reports.contains(duplicateReport));
			}

			//reset previous marked edit form
			editForms.stream()
				.filter(editForm -> editForm.isFirstGroup() || (editForm.getAgeGroup() == null && !editForm.isExpiredAgeGroup()))
				.forEach(editForm -> {
					editForm.getContent().getComponent(AggregateReportEditForm.DISEASE_LOC).removeClassName(CssStyles.LABEL_CRITICAL);
				});

			//mark duplicate diseases red
			if (!duplicateReports.isEmpty()) {
				editForms.stream().forEach(editForm -> {
					if (editForm.isFirstGroup() || (editForm.getAgeGroup() == null && !editForm.isExpiredAgeGroup())) {
						if (duplicateReports.stream()
							.anyMatch(aggregateReportDto -> (aggregateReportDto.getDisease().equals(editForm.getDisease())))) {
							editForm.getContent().getComponent(AggregateReportEditForm.DISEASE_LOC).addClassName(CssStyles.LABEL_CRITICAL);
						}
					}
				});
				duplicateMessage.setVisible(true);
			}
			SormasUI.refreshView();
		}
	}

	private void initialize() {

		epiWeekOptions.setValue(EpiWeekFilterOption.THIS_WEEK);
		final UserDto currentUser = UiUtil.getUser();
		RegionReferenceDto region = currentUser.getRegion();
		if (region != null) {
			comboBoxRegion.setValue(region);
			comboBoxRegion.setEnabled(false);
		}
		DistrictReferenceDto district = currentUser.getDistrict();
		if (district != null) {
			comboBoxDistrict.setValue(district);
			comboBoxDistrict.setEnabled(false);
		}
		FacilityReferenceDto healthFacility = currentUser.getHealthFacility();
		PointOfEntryReferenceDto pointOfEntry = currentUser.getPointOfEntry();
		if (healthFacility != null || pointOfEntry != null) {
			comboBoxFacility.setValue(healthFacility);
			comboBoxPoe.setValue(pointOfEntry);
			comboBoxFacility.setEnabled(false);
			comboBoxPoe.setEnabled(false);
		}
	}

	private void switchToEditMode() {

		window.setCaption(I18nProperties.getString(Strings.headingEditAggregateReport));
		for (AggregateReportEditForm editForm : editForms) {
			String ageGroup = editForm.getAgeGroup();
			Optional<AggregateReportDto> optionalAggregateReportDto = getReportByDiseaseAndAgeGroup(editForm.getDisease(), ageGroup);
			if (optionalAggregateReportDto.isPresent()) {
				AggregateReportDto report = optionalAggregateReportDto.get();
				editForm.setNewCases(report.getNewCases());
				editForm.setLabConfirmations(report.getLabConfirmations());
				editForm.setDeaths(report.getDeaths());
				diseaseAgeGroupsWithoutReport.remove(new DiseaseAgeGroup(editForm.getDisease(), ageGroup));
			}
		}
		removeComponent(epiWeekOptions);
		comboBoxYear.setEnabled(false);
		comboBoxEpiWeek.setEnabled(false);
		comboBoxRegion.setEnabled(false);
		comboBoxDistrict.setEnabled(false);
		comboBoxFacility.setEnabled(false);
		comboBoxPoe.setEnabled(false);
	}

	private Optional<AggregateReportDto> getReportByDiseaseAndAgeGroup(Disease disease, String ageGroup) {

		List<AggregateReportDto> foundReports = new ArrayList<>();
		reports.stream().filter(dto -> {
			boolean ageGroupMatches = dto.getAgeGroup() != null ? dto.getAgeGroup().equals(ageGroup) : ageGroup == null;
			boolean diseaseMatches = dto.getDisease() != null ? dto.getDisease().equals(disease) : disease == null;
			return diseaseMatches && ageGroupMatches;
		}).max(Comparator.comparing(AggregateReportDto::getChangeDate)).ifPresent(foundReports::add);

		if (foundReports.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(foundReports.get(0));
	}

	private void save() {

		if (!isValid()) {
			NotificationHelper.show(I18nProperties.getString(Strings.errorIntegerFieldValidationFailed), "", Type.ERROR_MESSAGE);
			return;
		}

		if (comboBoxYear.getComponentError() != null) {
			NotificationHelper.show(I18nProperties.getValidationError(Validations.specifyYear), "", Type.ERROR_MESSAGE);
			return;
		}

		if (comboBoxEpiWeek.getComponentError() != null) {
			NotificationHelper.show(I18nProperties.getValidationError(Validations.specifyEpiWeek), "", Type.ERROR_MESSAGE);
			return;
		}

		if (comboBoxRegion.getValue() == null) {
			comboBoxRegion.setComponentError(new UserError(I18nProperties.getString(Validations.required)));
			NotificationHelper.show(I18nProperties.getValidationError(Validations.validRegion), "", Type.ERROR_MESSAGE);
			return;
		}

		if (comboBoxDistrict.getValue() == null) {
			comboBoxDistrict.setComponentError(new UserError(I18nProperties.getString(Validations.required)));
			NotificationHelper.show(I18nProperties.getValidationError(Validations.validDistrict), "", Type.ERROR_MESSAGE);
			return;
		}

		if (UiUtil.isPortHealthUser() && comboBoxPoe.getValue() == null) {
			comboBoxPoe.setComponentError(new UserError(I18nProperties.getString(Validations.required)));
			NotificationHelper.show(I18nProperties.getValidationError(Validations.validPointOfEntry), "", Type.ERROR_MESSAGE);
			return;
		}

		for (AggregateReportEditForm editForm : editForms) {

			String deathsFieldValue = (String) editForm.getField(AggregateReportDto.DEATHS).getValue();
			String labFieldValue = (String) editForm.getField(AggregateReportDto.LAB_CONFIRMATIONS).getValue();
			String caseFieldValue = (String) editForm.getField(AggregateReportDto.NEW_CASES).getValue();

			int deaths;
			if (!deathsFieldValue.isEmpty()) {
				deaths = Integer.parseInt(deathsFieldValue);
			} else {
				deaths = 0;
			}

			int labConfirmations;
			if (!labFieldValue.isEmpty()) {
				labConfirmations = Integer.parseInt(labFieldValue);
			} else {
				labConfirmations = 0;
			}

			int newCases;
			if (!caseFieldValue.isEmpty()) {
				newCases = Integer.parseInt(caseFieldValue);
			} else {
				newCases = 0;
			}

			AggregateReportDto report = null;
			if (reports != null) {
				Optional<AggregateReportDto> reportByDiseaseAndAgeGroup =
					getReportByDiseaseAndAgeGroup(editForm.getDisease(), editForm.getAgeGroup());
				if (reportByDiseaseAndAgeGroup.isPresent()) {
					report = reportByDiseaseAndAgeGroup.get();
				}
			}
			if (report != null && (deaths > 0 || labConfirmations > 0 || newCases > 0)) {
				report.setDeaths(deaths);
				report.setLabConfirmations(labConfirmations);
				report.setNewCases(newCases);
				FacadeProvider.getAggregateReportFacade().saveAggregateReport(report);
			} else if (report != null) {
				FacadeProvider.getAggregateReportFacade().deleteReport(report.getUuid());
			} else {
				if (deaths > 0 || labConfirmations > 0 || newCases > 0) {
					AggregateReportDto newReport = AggregateReportDto.build();
					newReport.setDeaths(deaths);
					newReport.setDisease(editForm.getDisease());
					newReport.setAgeGroup(editForm.getAgeGroup());
					newReport.setDistrict(comboBoxDistrict.getValue());
					newReport.setEpiWeek(comboBoxEpiWeek.getValue().getWeek());
					newReport.setHealthFacility(comboBoxFacility.getValue());
					newReport.setLabConfirmations(labConfirmations);
					newReport.setNewCases(newCases);
					newReport.setPointOfEntry(comboBoxPoe.getValue());
					newReport.setRegion(comboBoxRegion.getValue());
					newReport.setReportingUser(UiUtil.getUser().toReference());
					newReport.setYear(comboBoxYear.getValue());

					FacadeProvider.getAggregateReportFacade().saveAggregateReport(newReport);
				}
			}
		}

		window.close();
	}

	private boolean isValid() {
		boolean valid = true;
		for (AggregateReportEditForm editForm : editForms) {
			if (!editForm.isValid()) {
				valid = false;
			}
		}
		return valid;
	}

	private void deleteAggregateReports() {
		List<String> aggregateReportUuidList = reports.stream().map(AggregateReportDto::getUuid).collect(Collectors.toList());

		FacadeProvider.getAggregateReportFacade().deleteAggregateReports(aggregateReportUuidList);
		window.close();
	}

	private void updateEpiWeekFields() {

		boolean enableEpiweekFields;
		EpiWeek thisEpiweek = DateHelper.getEpiWeek(Calendar.getInstance().getTime());

		if (EpiWeekFilterOption.LAST_WEEK.equals(epiWeekOptions.getValue())) {
			EpiWeek lastEpiweek = DateHelper.getPreviousEpiWeek(Calendar.getInstance().getTime());
			comboBoxYear.setValue(lastEpiweek.getYear());
			comboBoxEpiWeek.setValue(lastEpiweek);
			enableEpiweekFields = false;
		} else if (EpiWeekFilterOption.THIS_WEEK.equals(epiWeekOptions.getValue())) {
			comboBoxYear.setValue(thisEpiweek.getYear());
			comboBoxEpiWeek.setValue(thisEpiweek);
			enableEpiweekFields = false;
		} else {
			enableEpiweekFields = true;
		}

		comboBoxYear.setEnabled(enableEpiweekFields);
		comboBoxEpiWeek.setEnabled(enableEpiweekFields);

		Integer year = comboBoxYear.getValue();

		if (year != null) {
			EpiWeek selectedEpiWeek = comboBoxEpiWeek.getValue();
			List<EpiWeek> epiWeekOptions;
			Calendar now = new GregorianCalendar();
			if (year == now.get(Calendar.YEAR)) {
				epiWeekOptions = DateHelper.createEpiWeekListFromInterval(new EpiWeek(year, 1), thisEpiweek);
			} else {
				epiWeekOptions = DateHelper.createEpiWeekList(year);
			}
			comboBoxEpiWeek.setItems(epiWeekOptions);
			if (selectedEpiWeek != null) {
				EpiWeek adjustedEpiWeek = DateHelper.getSameEpiWeek(selectedEpiWeek, epiWeekOptions);
				comboBoxEpiWeek.setValue(adjustedEpiWeek);
			}
		} else {
			comboBoxEpiWeek.clear();
		}
	}

	private void setEpiWeekComponentErrors() {

		comboBoxYear.setComponentError(comboBoxYear.getValue() == null ? new ErrorMessage() {

			@Override
			public ErrorLevel getErrorLevel() {
				return ErrorLevel.ERROR;
			}

			@Override
			public String getFormattedHtmlMessage() {
				return I18nProperties.getValidationError(Validations.specifyYear);
			}
		} : null);

		comboBoxEpiWeek.setComponentError(comboBoxEpiWeek.getValue() == null ? new ErrorMessage() {

			@Override
			public ErrorLevel getErrorLevel() {
				return ErrorLevel.ERROR;
			}

			@Override
			public String getFormattedHtmlMessage() {
				return I18nProperties.getValidationError(Validations.specifyEpiWeek);
			}
		} : null);
	}

	public FieldGroup getFieldGroups() {
		// TODO Auto-generated method stub
		return null;
	}
}
