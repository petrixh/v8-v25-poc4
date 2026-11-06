package de.symeda.sormas.ui.utils.components.sidecomponent;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.sample.PathogenSpecie;
import de.symeda.sormas.api.sample.PathogenTestDto;
import de.symeda.sormas.api.sample.PathogenTestType;
import de.symeda.sormas.api.sample.Serotype;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

public class SideComponentField extends HorizontalLayout {

	private static final long serialVersionUID = -7617896760817891457L;

	private final VerticalLayout mainLayout;

	public SideComponentField() {
		setPadding(false);
		setSpacing(true);
		setWidthFull();
		addClassName(CssStyles.SORMAS_LIST_ENTRY);

		mainLayout = new VerticalLayout();
		mainLayout.setWidthFull();
		mainLayout.setPadding(false);
		mainLayout.setSpacing(false);
		add(mainLayout);
		setFlexGrow(1, mainLayout);
	}

	public void addComponentToField(Component component) {
		mainLayout.add(component);
	}

	public void addEditButton(String id, ComponentEventListener<ClickEvent<Button>> editClickListener) {
		Button editButton = ButtonHelper
			.createIconButtonWithCaption(id, null, VaadinIcon.PENCIL, editClickListener, CssStyles.BUTTON_LINK, CssStyles.BUTTON_COMPACT);

		add(editButton);
		setVerticalComponentAlignment(FlexComponent.Alignment.START, editButton);
		setFlexGrow(0, editButton);
	}

	public void addViewButton(String id, ComponentEventListener<ClickEvent<Button>> viewClickListener) {
		addViewButton(id, viewClickListener, VaadinIcon.EYE);
	}

	public void addViewButton(String id, ComponentEventListener<ClickEvent<Button>> viewClickListener, VaadinIcon icon) {
		Button viewButton =
			ButtonHelper.createIconButtonWithCaption(id, null, icon, viewClickListener, CssStyles.BUTTON_LINK, CssStyles.BUTTON_COMPACT);
		add(viewButton);
		setVerticalComponentAlignment(FlexComponent.Alignment.START, viewButton);
		setFlexGrow(0, viewButton);
		viewButton.setEnabled(true);
	}

	@Override
	public void setEnabled(boolean enabled) {
		mainLayout.setEnabled(enabled);
	}

	public void addActionButton(String id, ComponentEventListener<ClickEvent<Button>> actionClickListener, boolean isEditEntry) {
		Button actionButton = ButtonHelper.createIconButtonWithCaption(
			isEditEntry ? "edit" + id : "view" + id,
			null,
			isEditEntry ? VaadinIcon.PENCIL : VaadinIcon.EYE,
			actionClickListener,
			CssStyles.BUTTON_LINK,
			CssStyles.BUTTON_COMPACT);
		add(actionButton);
		setVerticalComponentAlignment(FlexComponent.Alignment.START, actionButton);
		setFlexGrow(0, actionButton);
	}

	public void addDeleteButton(String id, ComponentEventListener<ClickEvent<Button>> actionClickListener) {
		Button actionButton = ButtonHelper.createIconButtonWithCaption(
			"delete" + id,
			null,
			VaadinIcon.TRASH,
			actionClickListener,
			CssStyles.BUTTON_LINK,
			CssStyles.BUTTON_COMPACT);
		add(actionButton);
		setVerticalComponentAlignment(FlexComponent.Alignment.START, actionButton);
		setFlexGrow(0, actionButton);
	}

	public void setActive() {
		mainLayout.addClassName(CssStyles.ACTIVE_SIDE_COMPONENT_ELEMENT);
	}

	/** Disease -> the test methods whose variant field the side component can show. Invariant, so built once. */
	private static final Map<Disease, List<PathogenTestType>> VARIANT_MAP;
	static {
		Map<Disease, List<PathogenTestType>> map = new EnumMap<>(Disease.class);
		map.put(
			Disease.MALARIA,
			Arrays.asList(
				PathogenTestType.THIN_BLOOD_SMEAR,
				PathogenTestType.LATERAL_FLOW_ASSAY,
				PathogenTestType.PCR_RT_PCR,
				PathogenTestType.Q_PCR,
				PathogenTestType.LAMP,
				PathogenTestType.INDIRECT_FLUORESCENT_ANTIBODY,
				PathogenTestType.OTHER_MOLECULAR_ASSAY,
				PathogenTestType.OTHER_SEROLOGICAL_TEST,
				PathogenTestType.OTHER_ANTIGEN_DETECTION_TEST,
				PathogenTestType.ENZYME_LINKED_IMMUNOSORBENT_ASSAY));
		map.put(Disease.DENGUE, Arrays.asList(PathogenTestType.NAAT, PathogenTestType.NEUTRALIZING_ANTIBODIES, PathogenTestType.PCR_RT_PCR));
		map.put(Disease.SHIGELLOSIS, Arrays.asList(PathogenTestType.SEROGROUPING, PathogenTestType.SEROTYPING));
		map.put(Disease.MEASLES, Arrays.asList(PathogenTestType.GENOTYPING));
		map.put(
			Disease.INVASIVE_PNEUMOCOCCAL_INFECTION,
			Arrays.asList(
				PathogenTestType.SEROGROUPING,
				PathogenTestType.MULTILOCUS_SEQUENCE_TYPING,
				PathogenTestType.SLIDE_AGGLUTINATION,
				PathogenTestType.WHOLE_GENOME_SEQUENCING,
				PathogenTestType.SEQUENCING));
		map.put(
			Disease.TUBERCULOSIS,
			Arrays.asList(
				PathogenTestType.MICROSCOPY,
				PathogenTestType.BEIJINGGENOTYPING,
				PathogenTestType.SPOLIGOTYPING,
				PathogenTestType.MIRU_PATTERN_CODE));
		map.replaceAll((disease, testTypes) -> Collections.unmodifiableList(testTypes));
		VARIANT_MAP = Collections.unmodifiableMap(map);
	}

	/**
	 * To display the variant of the pathogen test in the side component, we need to check the disease and test type to determine which
	 * field contains the variant information, and if there are any "other" options that require us to show the free-text field instead.
	 *
	 * @param pathogenTest
	 * @return
	 */
	public String determineSideComponentVariant(PathogenTestDto pathogenTest) {
		if (pathogenTest.getTestType() == null || pathogenTest.getTestedDisease() == null)
			return null;

		String variant = null;
		if (pathogenTest.getTestedDisease() == Disease.TUBERCULOSIS
			&& VARIANT_MAP.get(pathogenTest.getTestedDisease()).contains(pathogenTest.getTestType())) {
			if (pathogenTest.getTestType() == PathogenTestType.MICROSCOPY) {
				variant = StringUtils.abbreviate((pathogenTest.getTestScale() != null ? pathogenTest.getTestScale().toString() : ""), 125);
			} else if (pathogenTest.getTestType() == PathogenTestType.BEIJINGGENOTYPING) {
				variant =
					StringUtils.abbreviate((pathogenTest.getStrainCallStatus() != null ? pathogenTest.getStrainCallStatus().toString() : ""), 125);
			} else if (pathogenTest.getTestType() == PathogenTestType.SPOLIGOTYPING) {
				variant = StringUtils.abbreviate((pathogenTest.getSpecie() != null ? pathogenTest.getSpecie().toString() : ""), 125);
			} else if (pathogenTest.getTestType() == PathogenTestType.MIRU_PATTERN_CODE) {
				variant = StringUtils.abbreviate(pathogenTest.getPatternProfile(), 125);
			}
		} else if (Arrays.asList(Disease.MALARIA, Disease.SHIGELLOSIS).contains(pathogenTest.getTestedDisease())
			&& VARIANT_MAP.get(pathogenTest.getTestedDisease()).stream().anyMatch(pathogenTest.getTestType()::equals)) {	// handling other specie
			if (pathogenTest.getSpecie() == PathogenSpecie.OTHER) {
				variant = StringUtils.abbreviate((pathogenTest.getSpecieText() != null ? pathogenTest.getSpecieText().toString() : ""), 125);
			} else {
				variant = StringUtils.abbreviate((pathogenTest.getSpecie() != null ? pathogenTest.getSpecie().toString() : ""), 125);
			}

		} else if (pathogenTest.getTestedDisease() == Disease.INVASIVE_PNEUMOCOCCAL_INFECTION
			&& VARIANT_MAP.get(pathogenTest.getTestedDisease()).stream().anyMatch(pathogenTest.getTestType()::equals)) {	// IPI serotyping stores the serogroup/serotype in the free-text field; show it instead of the plain result
			if (!DataHelper.isNullOrEmpty(pathogenTest.getSerotypeText())) {
				variant = StringUtils.abbreviate(pathogenTest.getSerotypeText(), 125);
			} else if (pathogenTest.getSerotype() != null) {
				variant = StringUtils.abbreviate(pathogenTest.getSerotype().toString(), 125);
			} else {
				variant = null;
			}

		} else if (pathogenTest.getTestedDisease() == Disease.DENGUE
			&& VARIANT_MAP.get(pathogenTest.getTestedDisease()).stream().anyMatch(pathogenTest.getTestType()::equals)) {
			// handling other serotypes
			if (pathogenTest.getSerotype() == Serotype.OTHER) {
				variant = StringUtils.abbreviate((pathogenTest.getSerotypeText() != null ? pathogenTest.getSerotypeText().toString() : ""), 125);
			} else {
				variant = StringUtils.abbreviate((pathogenTest.getSerotype() != null ? pathogenTest.getSerotype().toString() : ""), 125);
			}

		} else {
			variant = null;
		}
		return variant;
	}
}
