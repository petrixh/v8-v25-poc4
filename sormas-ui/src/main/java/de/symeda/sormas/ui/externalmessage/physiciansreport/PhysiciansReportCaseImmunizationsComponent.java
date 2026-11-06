/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2022 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.ui.externalmessage.physiciansreport;

import static de.symeda.sormas.ui.utils.CssStyles.H3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.symeda.sormas.ui.utils.ContentMode;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.caze.Vaccine;
import de.symeda.sormas.api.common.DeletionDetails;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.vaccination.VaccinationAssociationType;
import de.symeda.sormas.api.vaccination.VaccinationCriteria;
import de.symeda.sormas.api.vaccination.VaccinationDto;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.DeletableUtils;
import de.symeda.sormas.ui.utils.DirtyCheckPopup;
import de.symeda.sormas.ui.utils.FieldAccessHelper;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.vaccination.VaccinationEditForm;

@Deprecated
public class PhysiciansReportCaseImmunizationsComponent extends CommitDiscardWrapperComponent<VerticalLayout> {

	private static final long serialVersionUID = -5128676869217088760L;

	private final VaccinationList vaccinationList;
	private final Button addVaccinationButton;
	private final CaseDataDto caze;
	private final List<VaccinationDto> vaccinationsToDisplay;
	private final List<VaccinationDto> vaccinationsToCreate = new ArrayList<>();
	private final List<VaccinationDto> vaccinationsToUpdate = new ArrayList<>();
	private final Map<VaccinationDto, DeletionDetails> vaccinationsToDelete = new HashMap<>();
	private CommitDiscardWrapperComponent<VaccinationEditForm> currentVaccinationEditComponent;
	private CommitDiscardWrapperComponent<VaccinationEditForm> createVaccinationComponent;

	public PhysiciansReportCaseImmunizationsComponent(CaseDataDto caze) {
		super(createLayout());
		this.caze = caze;

		Span immunizationsLabel = new Span(I18nProperties.getString(Strings.entityVaccinations));
		immunizationsLabel.addClassName(H3);
		getWrappedComponent().add(immunizationsLabel);

		VaccinationCriteria criteria = new VaccinationCriteria.Builder(caze.getPerson()).withDisease(caze.getDisease())
			.build()
			.vaccinationAssociationType(VaccinationAssociationType.CASE)
			.caseReference(caze.toReference())
			.region(getRegion())
			.district(getDistrict());

		vaccinationsToDisplay = FacadeProvider.getVaccinationFacade().getVaccinationsByCriteria(criteria, 0, null, null);

		vaccinationList = new VaccinationList(this::handleDeleteVaccination, (v) -> handleEditVaccination(v, caze));
		vaccinationList.setVaccinations(vaccinationsToDisplay);

		getWrappedComponent().add(vaccinationList);

		addVaccinationButton = ButtonHelper.createButton(Captions.physiciansReportCaseAddVaccination, e -> {
			handleAddVaccination(caze);
		});

		HorizontalLayout vaccinationListToolbar = new HorizontalLayout(addVaccinationButton);
		vaccinationListToolbar.setWidthFull();
		// was Alignment.MIDDLE_RIGHT
		vaccinationListToolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		vaccinationListToolbar.setAlignItems(FlexComponent.Alignment.CENTER);
		getWrappedComponent().add(vaccinationListToolbar);

		setPreCommitListener((callback) -> {
			if (!handleDirty(callback)) {
				callback.run();
			}
		});
	}

	private static VerticalLayout createLayout() {
		VerticalLayout layout = new VerticalLayout();
		// was MarginInfo(false, true): partial V8 margins become no padding (look only)
		layout.setPadding(false);
		layout.setWidth(900, Unit.PIXELS);

		return layout;
	}

	private DistrictReferenceDto getDistrict() {
		return caze.getResponsibleDistrict();
	}

	private RegionReferenceDto getRegion() {
		return caze.getResponsibleRegion();
	}

	@Override
	protected void onCommit() {
		for (VaccinationDto vaccination : vaccinationsToUpdate) {
			FacadeProvider.getVaccinationFacade().save(vaccination);
		}

		for (VaccinationDto vaccination : vaccinationsToCreate) {
			FacadeProvider.getVaccinationFacade()
				.createWithImmunization(vaccination, getRegion(), getDistrict(), caze.getPerson(), caze.getDisease());
		}

		for (Map.Entry<VaccinationDto, DeletionDetails> deleteEntry : vaccinationsToDelete.entrySet()) {
			FacadeProvider.getVaccinationFacade().deleteWithImmunization(deleteEntry.getKey().getUuid(), deleteEntry.getValue());
		}

		vaccinationsToUpdate.clear();
		vaccinationsToCreate.clear();
		vaccinationsToDelete.clear();

		super.onCommit();
	}

	@Override
	public boolean isDirty() {
		return !vaccinationsToUpdate.isEmpty() || !vaccinationsToCreate.isEmpty() || !vaccinationsToDelete.isEmpty();
	}

	private void handleAddVaccination(CaseDataDto caze) {
		if (handleDirty(() -> handleAddVaccination(caze))) {
			return;
		}

		createVaccinationComponent = ControllerProvider.getVaccinationController()
			.getVaccinationCreateComponent(
				null,
				getRegion(),
				getDistrict(),
				caze.getPerson(),
				caze.getDisease(),
				UiFieldAccessCheckers.getNoop(),
				false,
				(v -> {
					vaccinationsToCreate.add(v);
					vaccinationsToDisplay.add(v);
					vaccinationList.setVaccinations(vaccinationsToDisplay);
				}));
		createVaccinationComponent.getDiscardButton().setText(I18nProperties.getCaption(Captions.actionCancel));
		// was Alignment.BOTTOM_LEFT in the discard button's expanding slot: Cancel at the left
		alignDiscardButtonLeft(createVaccinationComponent);

		createVaccinationComponent.addDoneListener(() -> {
			getWrappedComponent().remove(createVaccinationComponent);
			createVaccinationComponent = null;
			addVaccinationButton.setVisible(true);
		});

		addVaccinationButton.setVisible(false);
		getWrappedComponent().add(createVaccinationComponent);
	}

	private void handleDeleteVaccination(VaccinationDto vaccination) {
		if (vaccinationsToCreate.contains(vaccination)) {
			vaccinationsToCreate.remove(vaccination);
		} else {
			DeletableUtils.showDeleteWithReasonPopup(
				String.format(I18nProperties.getString(Strings.confirmationDeleteEntity), I18nProperties.getCaption(Captions.Vaccination)),
				(details) -> {
					vaccinationsToUpdate.remove(vaccination);
					vaccinationsToDelete.put(vaccination, details);
				});
		}

		vaccinationsToDisplay.remove(vaccination);
		vaccinationList.setVaccinations(vaccinationsToDisplay);
	}

	private void handleEditVaccination(VaccinationDto vaccination, CaseDataDto caze) {
		if (handleDirty(() -> handleEditVaccination(vaccination, caze))) {
			return;
		}

		VaccinationListItem collapsedComponent = vaccinationList.getItemByVaccination(vaccination);

		currentVaccinationEditComponent = ControllerProvider.getVaccinationController()
			.getVaccinationEditComponent(vaccination, caze.getDisease(), FieldAccessHelper.getFieldAccessCheckers(caze), false, (v) -> {
				if (!vaccinationsToCreate.contains(v)) {
					vaccinationsToUpdate.add(v);
				}
			}, true, UiUtil.permitted(UserRight.IMMUNIZATION_DELETE));

		currentVaccinationEditComponent.getDiscardButton().setText(I18nProperties.getCaption(Captions.actionCancel));
		alignDiscardButtonLeft(currentVaccinationEditComponent);

		currentVaccinationEditComponent.addDoneListener(() -> {
			vaccinationList.setVaccinations(vaccinationsToDisplay);
			currentVaccinationEditComponent = null;
			addVaccinationButton.setVisible(true);
		});

		addVaccinationButton.setVisible(false);
		vaccinationList.replace(collapsedComponent, currentVaccinationEditComponent);
	}

	private static void alignDiscardButtonLeft(CommitDiscardWrapperComponent<?> component) {
		Button discardButton = component.getDiscardButton();
		discardButton.getStyle().remove("margin-left");
		discardButton.getStyle().set("margin-right", "auto");
		component.getButtonsPanel().setVerticalComponentAlignment(FlexComponent.Alignment.END, discardButton);
	}

	private boolean handleDirty(Runnable callback) {
		if (currentVaccinationEditComponent != null) {
			if (currentVaccinationEditComponent.isDirty()) {
				DirtyCheckPopup.show(currentVaccinationEditComponent, callback);

				return true;
			}

			currentVaccinationEditComponent.discard();
		}

		if (createVaccinationComponent != null) {
			if (createVaccinationComponent.isDirty()) {
				DirtyCheckPopup.show(createVaccinationComponent, callback);
				return true;
			}

			createVaccinationComponent.discard();
		}

		return false;
	}

	private static class VaccinationList extends VerticalLayout {

		private final Consumer<VaccinationDto> deleteHandler;
		private final Consumer<VaccinationDto> editHandler;

		private List<VaccinationDto> vaccinations;

		public VaccinationList(Consumer<VaccinationDto> deleteHandler, Consumer<VaccinationDto> editHandler) {
			this.deleteHandler = deleteHandler;
			this.editHandler = editHandler;

			setMargin(false);
			setSpacing(false);
		}

		public void setVaccinations(List<VaccinationDto> vaccinations) {
			this.vaccinations = vaccinations;

			removeAll();

			vaccinations.forEach(v -> {
				add(new VaccinationListItem(v, () -> deleteHandler.accept(v), () -> editHandler.accept(v)));
			});
		}

		public VaccinationListItem getItemByVaccination(VaccinationDto vaccination) {
			return (VaccinationListItem) getComponentAt(vaccinations.indexOf(vaccination));
		}
	}

	private static class VaccinationListItem extends VerticalLayout {

		private static final long serialVersionUID = -8775209997959611902L;

		private final VaccinationDto vaccination;

		public VaccinationListItem(VaccinationDto vaccination, Runnable deleteHandler, Runnable editHandler) {
			this.vaccination = vaccination;

			setWidthFull();
			setMargin(false);
			setSpacing(false);

			buildLayout(deleteHandler, editHandler);
		}

		private void buildLayout(Runnable deleteHandler, Runnable editHandler) {

			HorizontalLayout vaccineLayout = new HorizontalLayout();
			vaccineLayout.setPadding(false);
			vaccineLayout.setSpacing(false);
			vaccineLayout.setWidthFull();

			String vaccine = vaccination.getVaccineName() != null
				? (vaccination.getVaccineName() == Vaccine.OTHER ? vaccination.getOtherVaccineName() : vaccination.getVaccineName().toString())
				: null;
			Span vaccineLabel = new Span(StringUtils.isNotBlank(vaccine) ? vaccine : I18nProperties.getString(Strings.labelNoVaccineName));
			CssStyles.style(vaccineLabel, CssStyles.LABEL_BOLD);

			Span dateLabel = new Span(
				vaccination.getVaccinationDate() != null
					? DateFormatHelper.formatDate(vaccination.getVaccinationDate())
					: I18nProperties.getString(Strings.labelNoVaccinationDate));

			VerticalLayout vaccineInfoLayout = new VerticalLayout(vaccineLabel, dateLabel);
			vaccineInfoLayout.setPadding(false);
			vaccineInfoLayout.setSpacing(false);
			vaccineLayout.add(vaccineInfoLayout);
			vaccineLayout.setFlexGrow(1, vaccineInfoLayout);

			Button deleteButton = ButtonHelper.createIconButtonWithCaption("delete-vaccination", null, VaadinIcon.TRASH, (e) -> {
				deleteHandler.run();
			}, CssStyles.BUTTON_LINK, CssStyles.BUTTON_COMPACT);

			Button editButton = ButtonHelper.createIconButtonWithCaption("edit-vaccination", null, VaadinIcon.PENCIL, (e) -> {
				editHandler.run();
			}, CssStyles.BUTTON_LINK, CssStyles.BUTTON_COMPACT);

			HorizontalLayout buttonsLayout = new HorizontalLayout(deleteButton, editButton);
			vaccineLayout.add(buttonsLayout);
			// was Alignment.TOP_RIGHT; the info layout takes the free space
			vaccineLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, buttonsLayout);
			vaccineLayout.setFlexGrow(0, buttonsLayout);

			add(vaccineLayout);

			Span horizontalRule = VaadinUiUtil.createLabel("<br><hr /><br>", ContentMode.HTML);
			horizontalRule.setWidthFull();
			add(horizontalRule);
		}

		public VaccinationDto getVaccination() {
			return vaccination;
		}

	}
}
