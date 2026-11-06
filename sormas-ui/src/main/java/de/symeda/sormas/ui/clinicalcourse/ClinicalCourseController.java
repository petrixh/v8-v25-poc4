package de.symeda.sormas.ui.clinicalcourse;

import java.util.Collection;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.dialog.Dialog;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.clinicalcourse.ClinicalCourseReferenceDto;
import de.symeda.sormas.api.clinicalcourse.ClinicalVisitDto;
import de.symeda.sormas.api.clinicalcourse.ClinicalVisitIndexDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent.CommitListener;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class ClinicalCourseController {

	public ClinicalCourseController() {

	}

	public void openClinicalVisitCreateForm(ClinicalCourseReferenceDto clinicalCourse, String caseUuid, Runnable callback) {

		CaseDataDto caze = FacadeProvider.getCaseFacade().getCaseDataByUuid(caseUuid);
		ClinicalVisitDto clinicalVisit = ClinicalVisitDto.build(clinicalCourse, caze.getDisease());
		ClinicalVisitForm form = new ClinicalVisitForm(
			true,
			clinicalVisit.getDisease(),
			FacadeProvider.getPersonFacade().getByUuid(caze.getPerson().getUuid()),
			false,
			true); // Valid because jurisdiction doesn't matter for entities that are about to be created
		form.setValue(clinicalVisit);

		final CommitDiscardWrapperComponent<ClinicalVisitForm> view =
			new CommitDiscardWrapperComponent<>(form, UiUtil.permitted(UserRight.CLINICAL_VISIT_CREATE), form.getFieldGroup());
		view.setWidth(100, Unit.PERCENTAGE);
		view.addCommitListener(new CommitListener() {

			@Override
			public void onCommit() {
				if (!form.getFieldGroup().isModified()) {
					ClinicalVisitDto dto = form.getValue();
					dto = FacadeProvider.getClinicalVisitFacade().saveClinicalVisit(dto, caseUuid);
					NotificationHelper.show(I18nProperties.getString(Strings.messageClinicalVisitCreated), Type.TRAY_NOTIFICATION);
					if (callback != null) {
						callback.run();
					}
				}
			}
		});

		Dialog popupWindow = VaadinUiUtil.showModalPopupWindow(view, I18nProperties.getString(Strings.headingCreateNewClinicalVisit));
		// Clinical visit form is too big for typical screens
		popupWindow.setWidth("calc(" + form.getWidth() + " + 90px)");
		popupWindow.setHeight("80%");
	}

	public void openClinicalVisitEditForm(
		ClinicalVisitIndexDto clinicalVisitIndex,
		String caseUuid,
		Runnable callback,
		boolean isEditAllowed,
		boolean isDeleteAllowed) {
		CaseDataDto caze = FacadeProvider.getCaseFacade().getCaseDataByUuid(caseUuid);
		ClinicalVisitDto clinicalVisit = FacadeProvider.getClinicalVisitFacade().getClinicalVisitByUuid(clinicalVisitIndex.getUuid());
		ClinicalVisitForm form = new ClinicalVisitForm(
			false,
			clinicalVisit.getDisease(),
			FacadeProvider.getPersonFacade().getByUuid(caze.getPerson().getUuid()),
			clinicalVisit.isPseudonymized(),
			clinicalVisit.isInJurisdiction());
		form.setValue(clinicalVisit);

		boolean isEditOrDeleteAllowed = isEditAllowed || isDeleteAllowed;
		final CommitDiscardWrapperComponent<ClinicalVisitForm> view =
			new CommitDiscardWrapperComponent<>(form, isEditOrDeleteAllowed, form.getFieldGroup());
		view.setWidth(100, Unit.PERCENTAGE);

		Dialog popupWindow = VaadinUiUtil.showModalPopupWindow(
			view,
			I18nProperties.getString(!isEditAllowed ? Strings.headingViewClinicalVisit : Strings.headingEditClinicalVisit));
		popupWindow.setWidth("calc(" + form.getWidth() + " + 90px)");
		popupWindow.setHeight("80%");

		if (isEditOrDeleteAllowed) {
			view.addCommitListener(new CommitListener() {

				@Override
				public void onCommit() {
					if (!form.getFieldGroup().isModified()) {
						ClinicalVisitDto dto = form.getValue();
						FacadeProvider.getClinicalVisitFacade().saveClinicalVisit(dto, caseUuid);
						popupWindow.close();
						NotificationHelper.show(I18nProperties.getString(Strings.messageClinicalVisitSaved), Type.TRAY_NOTIFICATION);
						if (callback != null) {
							callback.run();
						}
					}
				}
			});

			view.addDiscardListener(() -> popupWindow.close());

			if (isDeleteAllowed) {
				view.addDeleteListener(() -> {
					FacadeProvider.getClinicalVisitFacade().deleteClinicalVisit(clinicalVisit.getUuid());
					popupWindow.close();
					if (callback != null) {
						callback.run();
					}
				}, I18nProperties.getString(Strings.entityClinicalVisit));
			}

			view.restrictEditableComponentsOnEditView(
				UserRight.CASE_EDIT,
				UserRight.CLINICAL_VISIT_EDIT,
				UserRight.CLINICAL_VISIT_DELETE,
				null,
				clinicalVisit.isInJurisdiction());
		}
		view.getButtonsPanel().setVisible(isEditOrDeleteAllowed);
	}

	public void deleteAllSelectedClinicalVisits(Collection<?> selectedRows, Runnable callback) {
		if (selectedRows.size() == 0) {
			NotificationHelper.show(
				I18nProperties.getString(Strings.headingNoClinicalVisitsSelected),
				I18nProperties.getString(Strings.messageNoClinicalVisitsSelected),
				Type.WARNING_MESSAGE,
				false);
		} else {
			// todo confirmationDeleteEntity has only one format string so selectedRows.size() is not used
			VaadinUiUtil
				.showDeleteConfirmationWindow(String.format(I18nProperties.getString(Strings.confirmationDeleteEntity), selectedRows.size()), () -> {
					for (Object selectedRow : selectedRows) {
						FacadeProvider.getClinicalVisitFacade().deleteClinicalVisit(((ClinicalVisitIndexDto) selectedRow).getUuid());
					}
					callback.run();
					NotificationHelper.show(
						I18nProperties.getString(Strings.headingClinicalVisitsDeleted),
						I18nProperties.getString(Strings.messageClinicalVisitsDeleted),
						Type.HUMANIZED_MESSAGE,
						false);
				});
		}
	}
}
