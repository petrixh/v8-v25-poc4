package de.symeda.sormas.ui.immunization.components.layout.directory;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.immunization.ImmunizationCriteria;
import de.symeda.sormas.ui.immunization.components.grid.ImmunizationGrid;

public class ImmunizationDataLayout extends VerticalLayout {

	private final ImmunizationGrid grid;

	public ImmunizationDataLayout(ImmunizationCriteria criteria) {
		grid = new ImmunizationGrid(criteria);
		add(grid);

		setPadding(false);
		setSpacing(false);
		setSizeFull();
		setFlexGrow(1, grid);
	}

	public ImmunizationGrid getGrid() {
		return grid;
	}

	public void refreshGrid() {
		grid.reload();
	}
}
