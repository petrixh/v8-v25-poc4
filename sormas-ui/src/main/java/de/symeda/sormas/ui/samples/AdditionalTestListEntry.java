package de.symeda.sormas.ui.samples;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.sample.AdditionalTestDto;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentField;

@SuppressWarnings("serial")
public class AdditionalTestListEntry extends SideComponentField {

	private final AdditionalTestDto additionalTest;
	private Button editButton;

	public AdditionalTestListEntry(AdditionalTestDto additionalTest) {
		setSpacing(true);
		setPadding(false);
		setWidthFull();
		addClassName(CssStyles.SORMAS_LIST_ENTRY);
		this.additionalTest = additionalTest;

		VerticalLayout labelLayout = new VerticalLayout();
		labelLayout.setSpacing(false);
		labelLayout.setPadding(false);
		labelLayout.setWidthFull();
		addComponentToField(labelLayout);

		Span dateLabel = new Span(DateFormatHelper.formatLocalDateTime(additionalTest.getTestDateTime()));
		CssStyles.style(dateLabel, CssStyles.VSPACE_3);
		labelLayout.add(dateLabel);

		if (additionalTest.getHaemoglobinuria() != null) {
			labelLayout.add(
				createFieldLabel(
					I18nProperties.getPrefixCaption(AdditionalTestDto.I18N_PREFIX, AdditionalTestDto.HAEMOGLOBINURIA),
					additionalTest.getHaemoglobinuria().toString()));
		}
		if (additionalTest.getProteinuria() != null) {
			labelLayout.add(
				createFieldLabel(
					I18nProperties.getPrefixCaption(AdditionalTestDto.I18N_PREFIX, AdditionalTestDto.PROTEINURIA),
					additionalTest.getProteinuria().toString()));
		}
		if (additionalTest.getHematuria() != null) {
			labelLayout.add(
				createFieldLabel(
					I18nProperties.getPrefixCaption(AdditionalTestDto.I18N_PREFIX, AdditionalTestDto.HEMATURIA),
					additionalTest.getHematuria().toString()));
		}
		if (additionalTest.hasArterialVenousGasValue()) {
			labelLayout.add(
				createFieldLabel(
					I18nProperties.getPrefixCaption(AdditionalTestDto.I18N_PREFIX, AdditionalTestDto.ARTERIAL_VENOUS_BLOOD_GAS),
					additionalTest.buildArterialVenousGasValuesString()));
			if (additionalTest.getGasOxygenTherapy() != null) {
				labelLayout.add(
					new Span(
						I18nProperties.getPrefixCaption(AdditionalTestDto.I18N_PREFIX, AdditionalTestDto.GAS_OXYGEN_THERAPY) + ": "
							+ additionalTest.getGasOxygenTherapy()));
			}
		}
		if (additionalTest.getAltSgpt() != null) {
			labelLayout.add(
				createFieldLabel(
					I18nProperties.getPrefixCaption(AdditionalTestDto.I18N_PREFIX, AdditionalTestDto.ALT_SGPT),
					additionalTest.getAltSgpt().toString()));
		}
		if (additionalTest.getAstSgot() != null) {
			labelLayout.add(
				createFieldLabel(
					I18nProperties.getPrefixCaption(AdditionalTestDto.I18N_PREFIX, AdditionalTestDto.AST_SGOT),
					additionalTest.getAstSgot().toString()));
		}
		if (additionalTest.getCreatinine() != null) {
			labelLayout.add(
				createFieldLabel(
					I18nProperties.getPrefixCaption(AdditionalTestDto.I18N_PREFIX, AdditionalTestDto.CREATININE),
					additionalTest.getCreatinine().toString()));
		}
		if (additionalTest.getPotassium() != null) {
			labelLayout.add(
				createFieldLabel(
					I18nProperties.getPrefixCaption(AdditionalTestDto.I18N_PREFIX, AdditionalTestDto.POTASSIUM),
					additionalTest.getPotassium().toString()));
		}
		if (additionalTest.getUrea() != null) {
			labelLayout.add(
				createFieldLabel(
					I18nProperties.getPrefixCaption(AdditionalTestDto.I18N_PREFIX, AdditionalTestDto.UREA),
					additionalTest.getUrea().toString()));
		}
		if (additionalTest.getHaemoglobin() != null) {
			labelLayout.add(
				createFieldLabel(
					I18nProperties.getPrefixCaption(AdditionalTestDto.I18N_PREFIX, AdditionalTestDto.HAEMOGLOBIN),
					additionalTest.getHaemoglobin().toString()));
		}
		if (additionalTest.getTotalBilirubin() != null) {
			labelLayout.add(
				createFieldLabel(
					I18nProperties.getPrefixCaption(AdditionalTestDto.I18N_PREFIX, AdditionalTestDto.TOTAL_BILIRUBIN),
					additionalTest.getTotalBilirubin().toString()));
		}
		if (additionalTest.getConjBilirubin() != null) {
			labelLayout.add(
				createFieldLabel(
					I18nProperties.getPrefixCaption(AdditionalTestDto.I18N_PREFIX, AdditionalTestDto.CONJ_BILIRUBIN),
					additionalTest.getConjBilirubin().toString()));
		}
		if (additionalTest.getWbcCount() != null) {
			labelLayout.add(
				createFieldLabel(
					I18nProperties.getPrefixCaption(AdditionalTestDto.I18N_PREFIX, AdditionalTestDto.WBC_COUNT),
					additionalTest.getWbcCount().toString()));
		}
		if (additionalTest.getPlatelets() != null) {
			labelLayout.add(
				createFieldLabel(
					I18nProperties.getPrefixCaption(AdditionalTestDto.I18N_PREFIX, AdditionalTestDto.PLATELETS),
					additionalTest.getPlatelets().toString()));
		}
		if (additionalTest.getProthrombinTime() != null) {
			labelLayout.add(
				createFieldLabel(
					I18nProperties.getPrefixCaption(AdditionalTestDto.I18N_PREFIX, AdditionalTestDto.PROTHROMBIN_TIME),
					additionalTest.getProthrombinTime().toString()));
		}
		if (!StringUtils.isEmpty(additionalTest.getOtherTestResults())) {
			labelLayout.add(
				createFieldLabel(I18nProperties.getPrefixCaption(AdditionalTestDto.I18N_PREFIX, AdditionalTestDto.OTHER_TEST_RESULTS), ""));
			labelLayout.add(new Span(additionalTest.getOtherTestResults()));
		}
	}

	private Span createFieldLabel(String caption, String value) {
		return new Span(caption.toUpperCase() + ": " + value);
	}

	public void addEditListener(int rowIndex, ComponentEventListener<ClickEvent<Button>> editClickListener) {
		if (editButton == null) {
			editButton = ButtonHelper.createIconButtonWithCaption(
				"edit-test-" + rowIndex,
				null,
				VaadinIcon.PENCIL,
				null,
				CssStyles.BUTTON_LINK,
				CssStyles.BUTTON_COMPACT);

			add(editButton);
			setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, editButton);
			setFlexGrow(0, editButton);
		}

		editButton.addClickListener(editClickListener);
	}

	public AdditionalTestDto getAdditionalTest() {
		return additionalTest;
	}
}
