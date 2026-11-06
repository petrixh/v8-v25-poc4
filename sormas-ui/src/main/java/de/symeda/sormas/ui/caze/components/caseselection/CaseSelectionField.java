package de.symeda.sormas.ui.caze.components.caseselection;

import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseCriteria;
import de.symeda.sormas.api.caze.CaseSelectionDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.fields.CustomField;

@SuppressWarnings("serial")
public class CaseSelectionField extends CustomField<CaseSelectionDto> {

	private final Disease disease;

	protected CaseSelectionGrid grid;
	protected Consumer<Boolean> selectionChangeCallback;
	protected VerticalLayout mainLayout;

	public CaseSelectionField(Disease disease) {

		this.disease = disease;

		mainLayout = new VerticalLayout();
		mainLayout.setSpacing(true);
		mainLayout.setSizeUndefined();
		mainLayout.setWidthFull();
	}

	private void addInfoComponent() {
		mainLayout.add(VaadinUiUtil.createInfoComponent(I18nProperties.getString(Strings.infoSearchCaseForContact)));
	}

	private void addFilterComponent() {

		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.setSpacing(true);

		TextField searchField = new TextField();
		searchField.setPlaceholder(I18nProperties.getString(Strings.promptSearch));
		searchField.setWidth("400px");
		filterLayout.add(searchField);

		Button searchButton = ButtonHelper.createButton(Captions.caseSearchCase, e -> {
			CaseCriteria criteria = new CaseCriteria().disease(disease);
			if (StringUtils.isNotEmpty(searchField.getValue()) && StringUtils.isNotEmpty(searchField.getValue().trim())) {
				criteria.setSourceCaseInfoLike(searchField.getValue());
				grid.setCases(FacadeProvider.getCaseFacade().getCaseSelectionList(criteria));
			} else {
				criteria.setSourceCaseInfoLike(null);
				grid.clearCases();
			}
		}, CssStyles.BUTTON_PRIMARY);

		filterLayout.add(searchButton);

		mainLayout.add(filterLayout);
	}

	private void addGrid() {

		grid = new CaseSelectionGrid(null);
		grid.addSelectionListener(e -> {
			if (selectionChangeCallback != null) {
				selectionChangeCallback.accept(!e.getAllSelectedItems().isEmpty());
			}
		});
		mainLayout.add(grid);
	}

	@Override
	public CaseSelectionDto getValue() {
		return grid.getSelectedItems().stream().findFirst().orElse(null);
	}

	@Override
	protected Component initContent() {

		addInfoComponent();
		addFilterComponent();
		addGrid();

		return mainLayout;
	}

	@Override
	protected void setPresentationValue(CaseSelectionDto value) {
		grid.select(value);
	}

	public void setSelectionChangeCallback(Consumer<Boolean> callback) {
		this.selectionChangeCallback = callback;
	}
}
