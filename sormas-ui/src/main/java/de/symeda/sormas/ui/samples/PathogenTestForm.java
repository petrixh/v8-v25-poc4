/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.ui.samples;

import static de.symeda.sormas.ui.utils.CssStyles.H3;
import static de.symeda.sormas.ui.utils.LayoutUtil.loc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.common.DeletionReason;
import de.symeda.sormas.api.disease.DiseaseVariant;
import de.symeda.sormas.api.environment.environmentsample.EnvironmentSampleDto;
import de.symeda.sormas.api.sample.PathogenTestDto;
import de.symeda.sormas.api.sample.PathogenTestResultType;
import de.symeda.sormas.api.sample.SampleDto;
import de.symeda.sormas.api.sample.SamplePurpose;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.samples.components.AdditionalTestInfoComponent;
import de.symeda.sormas.ui.samples.components.DeletionComponent;
import de.symeda.sormas.ui.samples.components.DiseaseSelectionComponent;
import de.symeda.sormas.ui.samples.components.DiseaseVariantComponent;
import de.symeda.sormas.ui.samples.components.FourFoldCtCqComponent;
import de.symeda.sormas.ui.samples.components.PrescriberComponent;
import de.symeda.sormas.ui.samples.components.ResultTextComponent;
import de.symeda.sormas.ui.samples.components.TestIdentificationComponent;
import de.symeda.sormas.ui.samples.components.TestMethodComponent;
import de.symeda.sormas.ui.samples.components.TestResultComponent;
import de.symeda.sormas.ui.samples.diseasesection.AbstractDiseaseSectionComponent;
import de.symeda.sormas.ui.samples.diseasesection.DiseaseSectionFactory;
import de.symeda.sormas.ui.samples.diseasesection.PathogenTestFormConfig;
import de.symeda.sormas.ui.samples.events.DiseaseChangedEvent;
import de.symeda.sormas.ui.samples.events.SetTestResultEvent;
import de.symeda.sormas.ui.utils.AbstractEditForm;
import de.symeda.sormas.ui.utils.FieldAccessHelper;
import de.symeda.sormas.ui.utils.FormComponent;
import de.symeda.sormas.ui.utils.FormEventBus;

/**
 * Component-based PathogenTestForm using Flow components.
 * <p>
 * Extends {@link AbstractEditForm} for compatibility with
 * {@link de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent} and
 * {@link de.symeda.sormas.ui.externalmessage.CorrectionPanel}.
 * <p>
 * Each component owns its own Flow Binder and manages its own field
 * visibility via value change listeners (no FieldHelper.setVisibleWhen).
 * The FieldGroup from AbstractEditForm is kept hollow — only DrugSusceptibilityForm
 * (in disease sections) binds to it for legacy compatibility.
 */
public class PathogenTestForm extends AbstractEditForm<PathogenTestDto> {

	private static final long serialVersionUID = -1218707278398543154L;

	private static final String COMPONENT_CONTAINER_LOC = "componentContainerLoc";
	private static final String HTML_LAYOUT = loc(COMPONENT_CONTAINER_LOC);

	private SampleDto sample;
	private EnvironmentSampleDto environmentSample;
	private AbstractSampleForm sampleForm;
	private final int caseSampleCount;
	private final boolean create;
	private Disease disease;
	private PathogenTestFormConfig formConfig;

	private final FormEventBus eventBus = new FormEventBus();
	private final List<FormComponent<PathogenTestDto>> formComponents = new ArrayList<>();
	private final List<Registration> eventRegistrations = new ArrayList<>();

	private Span headingLabel;
	private TestIdentificationComponent testIdentificationComponent;
	private TestMethodComponent testMethodComponent;
	private TestResultComponent testResultComponent;
	private DiseaseVariantComponent diseaseVariantComponent;
	private DiseaseSelectionComponent diseaseSelectionComponent;
	private DeletionComponent deletionComponent;

	private AbstractDiseaseSectionComponent activeSection;
	private VerticalLayout diseaseSectionSlot;

	public PathogenTestForm(
		AbstractSampleForm sampleForm,
		boolean create,
		int caseSampleCount,
		boolean isPseudonymized,
		boolean inJurisdiction,
		Disease disease) {

		this(create, caseSampleCount, isPseudonymized, inJurisdiction, disease);
		this.sampleForm = sampleForm;
		this.disease = disease;
		addFields();
		if (create) {
			hideValidationUntilNextCommit();
		}
	}

	public PathogenTestForm(SampleDto sample, boolean create, int caseSampleCount, boolean isPseudonymized, boolean inJurisdiction, Disease disease) {
		this(create, caseSampleCount, isPseudonymized, inJurisdiction, disease);
		this.sample = sample;
		this.disease = disease;
		addFields();
		if (create) {
			hideValidationUntilNextCommit();
		}
	}

	public PathogenTestForm(EnvironmentSampleDto sample, boolean create, boolean isPseudonymized, boolean inJurisdiction, Disease disease) {
		this(create, 0, isPseudonymized, inJurisdiction, disease);
		this.environmentSample = sample;
		addFields();
		if (create) {
			hideValidationUntilNextCommit();
		}
	}

	private PathogenTestForm(boolean create, int caseSampleCount, boolean isPseudonymized, boolean inJurisdiction, Disease disease) {
		super(
			PathogenTestDto.class,
			PathogenTestDto.I18N_PREFIX,
			false,
			FieldVisibilityCheckers.withDisease(disease).andWithCountry(FacadeProvider.getConfigFacade().getCountryLocale()),
			FieldAccessHelper.getFieldAccessCheckers(create || inJurisdiction, !create && isPseudonymized));

		this.caseSampleCount = caseSampleCount;
		this.create = create;
		this.disease = disease;
		setWidth(900, Unit.PIXELS);
	}

	@Override
	protected String createHtmlLayout() {
		return HTML_LAYOUT;
	}

	@Override
	protected void addFields() {
		formConfig = PathogenTestFormConfig.fromCurrentConfig();

		VerticalLayout container = new VerticalLayout();
		container.setWidthFull();
		container.setPadding(false);
		container.setSpacing(true);
		getContent().addComponent(container, COMPONENT_CONTAINER_LOC);

		headingLabel = new Span();
		headingLabel.addClassName(H3);
		container.add(headingLabel);

		testIdentificationComponent = new TestIdentificationComponent(eventBus);
		formComponents.add(testIdentificationComponent);
		container.add(testIdentificationComponent);

		diseaseSelectionComponent = new DiseaseSelectionComponent(eventBus, disease, create, environmentSample != null);
		formComponents.add(diseaseSelectionComponent);
		container.add(diseaseSelectionComponent);

		testMethodComponent = new TestMethodComponent(eventBus, this::getSampleDate, disease);
		formComponents.add(testMethodComponent);
		container.add(testMethodComponent);

		testResultComponent = new TestResultComponent(eventBus, formConfig.isLuxembourg, disease, formConfig.resultRequired);
		formComponents.add(testResultComponent);
		container.add(testResultComponent);

		diseaseVariantComponent = new DiseaseVariantComponent(eventBus, disease);
		formComponents.add(diseaseVariantComponent);
		container.add(diseaseVariantComponent);

		FourFoldCtCqComponent fourFoldCtCqComponent = new FourFoldCtCqComponent(eventBus, caseSampleCount, formConfig.isLuxembourg, disease);
		formComponents.add(fourFoldCtCqComponent);
		container.add(fourFoldCtCqComponent);

		diseaseSectionSlot = new VerticalLayout();
		diseaseSectionSlot.setWidthFull();
		diseaseSectionSlot.setPadding(false);
		diseaseSectionSlot.setSpacing(false);
		container.add(diseaseSectionSlot);

		activeSection = DiseaseSectionFactory.forDisease(disease);
		activeSection.initialize(getFieldGroup(), eventBus, formConfig, disease);
		activeSection.setVisibilityCallback(visible -> diseaseSectionSlot.setVisible(visible));
		diseaseSectionSlot.add(activeSection);

		AdditionalTestInfoComponent additionalTestInfoComponent = new AdditionalTestInfoComponent();
		formComponents.add(additionalTestInfoComponent);
		container.add(additionalTestInfoComponent);

		ResultTextComponent resultTextComponent = new ResultTextComponent();
		formComponents.add(resultTextComponent);
		container.add(resultTextComponent);

		PrescriberComponent prescriberComponent = new PrescriberComponent();
		formComponents.add(prescriberComponent);
		container.add(prescriberComponent);

		deletionComponent = new DeletionComponent();
		formComponents.add(deletionComponent);
		container.add(deletionComponent);

		wireEvents();

		finalizeForm();
	}

	private void wireEvents() {
		// Disease change → swap section + clear test type
		eventRegistrations.add(eventBus.on(DiseaseChangedEvent.class, event -> {
			Disease newDisease = event.getDisease();
			if (newDisease != disease) {
				testMethodComponent.getTestTypeField().clear();
			}
			disease = newDisease;
			swapDiseaseSection(newDisease);
		}));

		// Disease variant → auto-set test result to positive when a variant is selected.
		eventRegistrations.add(diseaseVariantComponent.getDiseaseVariantField().addValueChangeListener(e -> {
			DiseaseVariant variant = e.getValue();
			if (variant != null) {
				eventBus.fire(new SetTestResultEvent(PathogenTestResultType.POSITIVE));
			}
		}));
	}

	private void finalizeForm() {
		testMethodComponent.setLabRequired(SamplePurpose.INTERNAL.equals(getSamplePurpose()));

		initializeAccessAndAllowedAccesses();
		initializeVisibilitiesAndAllowedVisibilities();

		for (FormComponent<PathogenTestDto> comp : formComponents) {
			if (fieldVisibilityCheckers != null) {
				comp.applyVisibility(fieldVisibilityCheckers, PathogenTestDto.class);
			}
			if (fieldAccessCheckers != null) {
				comp.applyAccess(fieldAccessCheckers, PathogenTestDto.class);
			}
		}

		if (fieldVisibilityCheckers != null) {
			activeSection.applyVisibility(fieldVisibilityCheckers, PathogenTestDto.class);
		}

		if (fieldAccessCheckers != null) {
			activeSection.applyAccess(fieldAccessCheckers, PathogenTestDto.class);
		}

	}

	private void swapDiseaseSection(Disease newDisease) {
		AbstractDiseaseSectionComponent newSection = DiseaseSectionFactory.forDisease(newDisease);
		if (newSection.getClass() == activeSection.getClass()) {
			return;
		}

		activeSection.cleanup();
		diseaseSectionSlot.removeAll();

		activeSection = newSection;
		activeSection.initialize(getFieldGroup(), eventBus, formConfig, newDisease);
		activeSection.setVisibilityCallback(visible -> diseaseSectionSlot.setVisible(visible));
		diseaseSectionSlot.add(activeSection);

		PathogenTestDto dto = getValue();
		if (dto != null) {
			activeSection.setDto(dto);
		}

		if (fieldVisibilityCheckers != null) {
			activeSection.applyVisibility(fieldVisibilityCheckers, PathogenTestDto.class);
		}

		if (fieldAccessCheckers != null) {
			activeSection.applyAccess(fieldAccessCheckers, PathogenTestDto.class);
		}
	}

	private Date getSampleDate() {
		if (sample != null) {
			return sample.getSampleDateTime();
		}
		if (sampleForm != null) {
			return (Date) sampleForm.getField(SampleDto.SAMPLE_DATE_TIME).getValue();
		}
		if (environmentSample != null) {
			return environmentSample.getSampleDateTime();
		}
		return null;
	}

	private SamplePurpose getSamplePurpose() {
		if (sample != null) {
			return sample.getSamplePurpose();
		}
		if (sampleForm != null) {
			return (SamplePurpose) sampleForm.getField(SampleDto.SAMPLE_PURPOSE).getValue();
		}
		return null;
	}

	@Override
	public void preCommit(BeanFieldGroup.CommitEvent commitEvent) throws BeanFieldGroup.CommitException {
		super.preCommit(commitEvent);

		List<de.symeda.sormas.ui.utils.InvalidValueException> allCauses = new ArrayList<>();

		for (FormComponent<PathogenTestDto> comp : formComponents) {
			try {
				comp.validate();
			} catch (de.symeda.sormas.ui.utils.InvalidValueException e) {
				FormComponent.collectCauses(e, allCauses);
			}
		}

		if (activeSection != null) {
			try {
				activeSection.validate();
			} catch (de.symeda.sormas.ui.utils.InvalidValueException e) {
				FormComponent.collectCauses(e, allCauses);
			}
		}

		FormComponent.throwCombined(allCauses);
	}

	@Override
	public void setHeading(String heading) {
		headingLabel.setText(heading);
	}

	@Override
	public void setValue(PathogenTestDto newFieldValue) {
		super.setValue(newFieldValue);

		for (FormComponent<PathogenTestDto> comp : formComponents) {
			comp.setDto(newFieldValue);
		}

		if (activeSection != null) {
			activeSection.setDto(newFieldValue);
		}
		// V8 markAsDirty() dropped: Flow sends component changes without it
	}

	/** Reveals the typingId field if the existing test has a preset value. */
	public void showTypingIdIfPreset(String typingId) {
		testMethodComponent.showTypingIdIfPreset(typingId);
	}

	/** Sets initial field values for a newly created pathogen test. */
	public void initializeForNewTest(Disease disease) {
		testResultComponent.getTestResultField().setValue(PathogenTestResultType.PENDING);
		if (disease != null) {
			diseaseSelectionComponent.getDiseaseField().setValue(disease);
		}
	}

	/** Updates lab field required state (e.g. when sample purpose changes). */
	public void setLabRequired(boolean required) {
		testMethodComponent.setLabRequired(required);
	}

	/**
	 * Registers a validator ensuring test date is not before sample date.
	 * Evaluated during form commit via {@link de.symeda.sormas.ui.utils.FormComponent#validate()}.
	 */
	public void addTestDateAfterSampleDateValidator(Supplier<Date> sampleDateSupplier, String errorMessage) {
		testMethodComponent.addTestDateAfterSampleDateValidator(sampleDateSupplier, errorMessage);
	}

	/** Sets the VIA_LIMS checkbox value. */
	public void setViaLims(boolean checked) {
		testIdentificationComponent.getViaLimsField().setValue(checked);
	}

	/** Shows deletion reason fields for a deleted record. */
	public void showDeletionInfo(DeletionReason reason) {
		deletionComponent.showForDeletedRecord(reason);
	}

	@Override
	protected void onDetach(DetachEvent detachEvent) {
		for (Registration reg : eventRegistrations) {
			reg.remove();
		}
		eventRegistrations.clear();

		super.onDetach(detachEvent);
	}

	@Override
	public void forEachComponent(java.util.function.Consumer<Component> componentConsumer) {
		Component c = getContent().getComponent(COMPONENT_CONTAINER_LOC);
		if (c instanceof VerticalLayout) {
			VerticalLayout vl = (VerticalLayout) c;
			for (int i = 0; i < vl.getComponentCount(); i++) {
				componentConsumer.accept(vl.getComponentAt(i));
			}
		}
	}
}
