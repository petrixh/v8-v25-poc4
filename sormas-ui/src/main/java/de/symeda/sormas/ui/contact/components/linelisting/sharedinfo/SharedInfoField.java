package de.symeda.sormas.ui.contact.components.linelisting.sharedinfo;

import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.caze.CaseReferenceDto;
import de.symeda.sormas.api.event.EventDto;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.contact.components.linelisting.CaseSelector;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.fields.CustomField;
import de.symeda.sormas.ui.utils.fields.KeepValueComboBox;
import de.symeda.sormas.ui.utils.FieldHelper;

public class SharedInfoField extends CustomField<SharedInfoFieldDto> {

	private static final long serialVersionUID = 1869000092813141681L;

	private final Binder<SharedInfoFieldDto> binder = new Binder<>(SharedInfoFieldDto.class);

	private final CaseSelector caseSelector;
	private final ComboBox<Disease> disease;
	private final ComboBox<RegionReferenceDto> region;
	private final ComboBox<DistrictReferenceDto> district;

	private final Disease initialDiseaseValue;

	public SharedInfoField(CaseReferenceDto caseReferenceDto, Disease initialDiseaseValue) {
		caseSelector = caseReferenceDto != null
			? new CaseSelector(caseReferenceDto)
			: new CaseSelector(I18nProperties.getString(Strings.infoNoSourceCaseSelectedLineListing));
		disease = new ComboBox<>(I18nProperties.getCaption(Captions.lineListingDiseaseOfSourceCase));
		region = new KeepValueComboBox<>(I18nProperties.getCaption(Captions.Region));
		district = new KeepValueComboBox<>(I18nProperties.getCaption(Captions.District));
		region.setItemLabelGenerator(item -> item.buildCaption());
		district.setItemLabelGenerator(item -> item.buildCaption());

		this.initialDiseaseValue = initialDiseaseValue;
	}

	public SharedInfoField(CaseDataDto caseDataDto) {
		this(
			caseDataDto == null ? null : caseDataDto.toReference(),
			caseDataDto == null ? FacadeProvider.getDiseaseConfigurationFacade().getDefaultDisease() : caseDataDto.getDisease());
	}

	public SharedInfoField(EventDto eventDto) {
		this(null, eventDto == null ? FacadeProvider.getDiseaseConfigurationFacade().getDefaultDisease() : eventDto.getDisease());
	}

	/**
	 * Built on first attach, as in Vaadin 8 ({@link CustomField}).
	 */
	@Override
	protected Component initContent() {
		setValue(new SharedInfoFieldDto());

		VerticalLayout layout = new VerticalLayout();

		layout.setPadding(false);
		layout.setSpacing(false);

		caseSelector.setId("caseSelector");
		layout.add(caseSelector);

		HorizontalLayout sharedInformationBar = new HorizontalLayout();
		sharedInformationBar.addClassName(CssStyles.SPACING_SMALL);

		disease.setId("disease");
		disease.setItems(FacadeProvider.getDiseaseConfigurationFacade().getAllDiseases(true, true, true));
		sharedInformationBar.add(disease);

		region.setId("region");
		sharedInformationBar.add(region);

		district.setId("district");
		sharedInformationBar.add(district);

		binder.forField(caseSelector).bind(SharedInfoFieldDto.CAZE);
		binder.forField(disease).asRequired().bind(SharedInfoFieldDto.DISEASE);
		binder.forField(region).asRequired().bind(SharedInfoFieldDto.REGION);
		binder.forField(district).asRequired().bind(SharedInfoFieldDto.DISTRICT);

		caseSelector.addValueChangeListener(e -> {
			CaseReferenceDto caseReferenceDto = e.getValue();
			if (caseReferenceDto != null) {
				CaseDataDto selectedCase = FacadeProvider.getCaseFacade().getCaseDataByUuid(e.getValue().getUuid());
				disease.setValue(selectedCase.getDisease());
			} else {
				disease.setValue(null);
			}
			disease.setEnabled(caseReferenceDto == null);

			binder.getBinding(SharedInfoFieldDto.DISEASE).get().setAsRequiredEnabled(caseReferenceDto == null);
			binder.getBinding(SharedInfoFieldDto.REGION).get().setAsRequiredEnabled(caseReferenceDto == null);
			binder.getBinding(SharedInfoFieldDto.DISTRICT).get().setAsRequiredEnabled(caseReferenceDto == null);
		});

		if (initialDiseaseValue != null) {
			disease.setValue(initialDiseaseValue);
			disease.setEnabled(false);
		}

		region.addValueChangeListener(e -> {
			RegionReferenceDto regionDto = e.getValue();
			updateDistricts(regionDto);
		});

		layout.add(sharedInformationBar);

		if (UiUtil.hasRegionJurisdictionLevel()) {
			RegionReferenceDto userRegion = UiUtil.getUser().getRegion();
			region.setValue(userRegion);
			region.setVisible(false);
			updateDistricts(userRegion);
		} else {
			region.setItems(FacadeProvider.getRegionFacade().getAllActiveByServerCountry());
		}

		if (UiUtil.enabled(FeatureType.HIDE_JURISDICTION_FIELDS)) {
			region.setVisible(false);
			region.setValue(FacadeProvider.getRegionFacade().getDefaultInfrastructureReference());
			district.setVisible(false);
			district.setValue(FacadeProvider.getDistrictFacade().getDefaultInfrastructureReference());
		}

		return layout;
	}

	@Override
	protected void setPresentationValue(SharedInfoFieldDto sharedInfoFieldDto) {
		binder.setBean(sharedInfoFieldDto);
	}

	@Override
	protected SharedInfoFieldDto generateModelValue() {
		return binder.getBean();
	}

	@Override
	public SharedInfoFieldDto getValue() {
		return binder.getBean();
	}

	public boolean hasErrors() {
		BinderValidationStatus<SharedInfoFieldDto> validationStatus = binder.validate();
		return validationStatus.hasErrors();
	}

	private void updateDistricts(RegionReferenceDto regionDto) {
		FieldHelper.updateItems(district, regionDto != null ? FacadeProvider.getDistrictFacade().getAllActiveByRegion(regionDto.getUuid()) : null);
	}

	public void addDiseaseChangeHandler(Consumer<Disease> diseaseChangeHandler) {
		disease.addValueChangeListener(e -> diseaseChangeHandler.accept(disease.getValue()));
	}

	public CaseSelector getCaseSelector() {
		return caseSelector;
	}
}
