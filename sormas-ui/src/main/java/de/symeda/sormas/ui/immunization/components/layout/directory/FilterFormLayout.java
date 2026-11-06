package de.symeda.sormas.ui.immunization.components.layout.directory;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.immunization.ImmunizationCriteria;
import de.symeda.sormas.ui.immunization.components.filter.ImmunizationFilterForm;

public class FilterFormLayout extends VerticalLayout {

	private final ImmunizationFilterForm filterForm;

	public FilterFormLayout() {
		setSpacing(false);
		setPadding(false);
		setWidthFull();

		filterForm = new ImmunizationFilterForm();
		add(filterForm);
	}

	public ImmunizationCriteria getValue() {
		return filterForm.getValue();
	}

	public void setValue(ImmunizationCriteria criteria) {
		filterForm.setValue(criteria);
	}

	public void addResetHandler(ComponentEventListener<ClickEvent<Button>> listener) {
		filterForm.addResetHandler(listener);
	}

	public void addApplyHandler(ComponentEventListener<ClickEvent<Button>> listener) {
		filterForm.addApplyHandler(listener);
	}
}
