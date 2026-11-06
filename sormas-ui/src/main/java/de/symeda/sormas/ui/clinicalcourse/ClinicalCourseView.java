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
package de.symeda.sormas.ui.clinicalcourse;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.clinicalcourse.ClinicalCourseDto;
import de.symeda.sormas.api.clinicalcourse.ClinicalVisitCriteria;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.caze.AbstractCaseView;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DetailSubComponentWrapper;
import de.symeda.sormas.ui.utils.MenuBarHelper;
import de.symeda.sormas.ui.utils.grid.HeightMode;

@SuppressWarnings("serial")
public class ClinicalCourseView extends AbstractCaseView {

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/clinicalcourse";

	private CommitDiscardWrapperComponent<ClinicalCourseForm> editComponent;

	private ClinicalVisitCriteria clinicalVisitCriteria;
	private ClinicalVisitGrid clinicalVisitGrid;

	public ClinicalCourseView() {

		super(VIEW_NAME, true);

		clinicalVisitCriteria = ViewModelProviders.of(ClinicalCourseView.class).get(ClinicalVisitCriteria.class);
	}

	private VerticalLayout createClinicalVisitsHeader() {

		VerticalLayout clinicalVisitsHeader = new VerticalLayout();
		clinicalVisitsHeader.setPadding(false);
		clinicalVisitsHeader.setSpacing(false);
		clinicalVisitsHeader.setWidth(100, Unit.PERCENTAGE);

		HorizontalLayout headlineRow = new HorizontalLayout();
		headlineRow.setPadding(false);
		headlineRow.setSpacing(true);
		headlineRow.setWidth(100, Unit.PERCENTAGE);
		{
			Span clinicalVisitsLabel = new Span(I18nProperties.getString(Strings.entityClinicalVisits));
			CssStyles.style(clinicalVisitsLabel, CssStyles.H3);
			headlineRow.add(clinicalVisitsLabel);
			headlineRow.setFlexGrow(1, clinicalVisitsLabel);

			if (isEditAllowed()) {
				// Bulk operations
				if (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS, UserRight.CASE_EDIT, UserRight.CLINICAL_COURSE_EDIT)) {
					MenuBar bulkOperationsDropdown = MenuBarHelper.createDropDown(
						Captions.bulkActions,
						new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.bulkDelete), VaadinIcon.TRASH, selectedItem -> {
							ControllerProvider.getClinicalCourseController()
								.deleteAllSelectedClinicalVisits(clinicalVisitGrid.getSelectedItems(), new Runnable() {

									public void run() {
										clinicalVisitGrid.reload();
									}
								});
						}));

					headlineRow.add(bulkOperationsDropdown);
					headlineRow.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, bulkOperationsDropdown);
				}

				if (UiUtil.permitted(UserRight.CASE_EDIT, UserRight.CLINICAL_VISIT_CREATE)) {
					Button newClinicalVisitButton = ButtonHelper.createButton(Captions.clinicalVisitNewClinicalVisit, e -> {
						ControllerProvider.getClinicalCourseController()
							.openClinicalVisitCreateForm(
								clinicalVisitCriteria.getClinicalCourse(),
								getCaseRef().getUuid(),
								() -> clinicalVisitGrid.reload());
					}, CssStyles.BUTTON_PRIMARY);

					headlineRow.add(newClinicalVisitButton);

					headlineRow.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, newClinicalVisitButton);
				}
			}
		}
		clinicalVisitsHeader.add(headlineRow);

		return clinicalVisitsHeader;
	}

	private void update() {

		CaseDataDto caze = FacadeProvider.getCaseFacade().getCaseDataByUuid(getCaseRef().getUuid());
		clinicalVisitCriteria.clinicalCourse(caze.getClinicalCourse().toReference());
	}

	@Override
	protected void initView(String params) {

		// TODO: Remove this once a proper ViewModel system has been introduced
		CaseDataDto caze = FacadeProvider.getCaseFacade().getCaseDataByUuid(getCaseRef().getUuid());
		if (caze.getClinicalCourse() == null) {
			ClinicalCourseDto clinicalCourse = ClinicalCourseDto.build();
			caze.setClinicalCourse(clinicalCourse);
			caze = FacadeProvider.getCaseFacade().save(caze);
		}

		DetailSubComponentWrapper container = new DetailSubComponentWrapper(() -> editComponent);
		container.setWidth(100, Unit.PERCENTAGE);
		container.setPadding(true);

		container.add(createClinicalVisitsHeader());

		clinicalVisitGrid = new ClinicalVisitGrid(
			getCaseRef(),
			caze.isPseudonymized(),
			UiUtil.permitted(isEditAllowed(), UserRight.CASE_EDIT, UserRight.CLINICAL_VISIT_EDIT),
			UiUtil.permitted(isEditAllowed(), UserRight.CLINICAL_VISIT_DELETE));
		clinicalVisitGrid.setCriteria(clinicalVisitCriteria);
		clinicalVisitGrid.setHeightMode(HeightMode.ROW);
		CssStyles.style(clinicalVisitGrid, CssStyles.VSPACE_3);
		container.add(clinicalVisitGrid);

		editComponent = ControllerProvider.getCaseController().getClinicalCourseComponent(getCaseRef().getUuid());
		editComponent.setPadding(false);
		container.add(editComponent);

		setSubComponent(container);

		update();
		clinicalVisitGrid.reload();
	}
}
