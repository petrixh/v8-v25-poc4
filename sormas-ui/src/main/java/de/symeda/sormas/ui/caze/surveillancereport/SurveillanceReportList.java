/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2021 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.caze.surveillancereport;

import java.util.List;

import jakarta.validation.constraints.Null;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.caze.CaseReferenceDto;
import de.symeda.sormas.api.caze.surveillancereport.ReportingType;
import de.symeda.sormas.api.caze.surveillancereport.SurveillanceReportCriteria;
import de.symeda.sormas.api.caze.surveillancereport.SurveillanceReportDto;
import de.symeda.sormas.api.externalmessage.ExternalMessageDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.Descriptions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.infrastructure.facility.FacilityDto;
import de.symeda.sormas.api.infrastructure.facility.FacilityReferenceDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.FieldAccessHelper;
import de.symeda.sormas.ui.utils.PaginationList;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentField;

public class SurveillanceReportList extends PaginationList<SurveillanceReportDto> {

	private final SurveillanceReportCriteria criteria = new SurveillanceReportCriteria();
	private final CaseReferenceDto caseRef;
	private final boolean isEditAllowed;
	private final UserRight editRight;

	public SurveillanceReportList(CaseReferenceDto caze, UserRight editRight, boolean isEditAllowed) {
		super(5);
		criteria.caze(caze);
		this.caseRef = caze;
		this.editRight = editRight;
		this.isEditAllowed = isEditAllowed;
	}

	@Override
	public void reload() {
		List<SurveillanceReportDto> reports = FacadeProvider.getSurveillanceReportFacade().getIndexList(criteria, 0, maxDisplayedEntries * 20, null);

		setEntries(reports);
		if (!reports.isEmpty()) {
			showPage(1);
		} else {
			listLayout.removeAll();
			updatePaginationLayout();
			Span noSamplesLabel = new Span(I18nProperties.getCaption(Captions.surveillanceReportNoReportsForCase));
			listLayout.add(noSamplesLabel);
		}
	}

	/**
	 * Draws the currently displayed surveillance report entries in the list.
	 * <p>
	 * This method first checks if the current user has the EXTERNAL_MESSAGE_ACCESS right; if not, it returns immediately and does not
	 * display any entries.
	 * It then determines the user's permissions for viewing and processing laboratory and doctor declaration reports, as well as case
	 * editing rights.
	 * <p>
	 * For each displayed {@link SurveillanceReportDto} entry:
	 * <ul>
	 * <li>Skips the entry if the user lacks the required view permission for the report's reporting type (laboratory or doctor
	 * declaration).</li>
	 * <li>Creates a {@link SurveillanceReportListEntry} for the report.</li>
	 * <li>Determines if the entry is editable based on the following conditions:
	 * <ul>
	 * <li>User has case edit rights.</li>
	 * <li>The report is not owned by another user.</li>
	 * <li>The report is not a Sormas-to-Sormas report, or ownership has been handed over.</li>
	 * <li>If the report is a laboratory report, the user must have laboratory processing rights.</li>
	 * <li>If the report is a doctor declaration, the user must have doctor declaration processing rights.</li>
	 * </ul>
	 * </li>
	 * <li>Adds an action button to edit the report if editable, and enables or disables the entry accordingly.</li>
	 * <li>Adds a button to view the associated external message if the user has permission and such a message exists.</li>
	 * <li>Adds the entry to the list layout for display.</li>
	 * </ul>
	 * <p>
	 * This ensures that only authorized users can view or edit surveillance reports, and that the UI reflects the user's permissions for
	 * each entry.
	 */
	@Override
	protected void drawDisplayedEntries() {

		// Don't do anyting if the user doesn't have the permission to view external messages
		if (!UiUtil.getUserRights().contains(UserRight.EXTERNAL_MESSAGE_ACCESS)) {
			return;
		}

		final boolean userHasLaboratoryView = UiUtil.permitted(UserRight.EXTERNAL_MESSAGE_LABORATORY_VIEW);
		final boolean userHasDoctorDeclarationView = UiUtil.permitted(UserRight.EXTERNAL_MESSAGE_DOCTOR_DECLARATION_VIEW);

		final boolean userHasCaseEdit = UiUtil.permitted(editRight, UserRight.CASE_EDIT);
		final boolean userHasLaboratoryProcessing = UiUtil.permitted(UserRight.EXTERNAL_MESSAGE_LABORATORY_PROCESS);
		final boolean userHasDoctorDeclarationProcessing = UiUtil.permitted(UserRight.EXTERNAL_MESSAGE_DOCTOR_DECLARATION_PROCESS);

		List<SurveillanceReportDto> displayedEntries = getDisplayedEntries();
		for (int i = 0, displayedEntriesSize = displayedEntries.size(); i < displayedEntriesSize; i++) {
			SurveillanceReportDto report = displayedEntries.get(i);

			// Skip entry if the user doesn't have the permission to view the report
			if (ReportingType.LABORATORY.equals(report.getReportingType()) && !userHasLaboratoryView) {
				continue;
			}

			// Skip entry if the user doesn't have the permission to view the report
			if (ReportingType.DOCTOR.equals(report.getReportingType()) && !userHasDoctorDeclarationView) {
				continue;
			}

			// Skip entry if the user doesn't have the permission to view the report
			if (ReportingType.PHONE_NOTIFICATION.equals(report.getReportingType()) && !userHasDoctorDeclarationView) {
				continue;
			}

			SurveillanceReportListEntry listEntry = new SurveillanceReportListEntry(report);

			// Only allow editing if the user has the permission to edit the case
			// and the report is not owned by another user
			// and the report is not a Sormas-to-Sormas report.
			// Also if the report is a laboratory report, the user must have the permission to process laboratory reports
			// and if the report is a doctor declaration, the user must have the permission to process doctor declarations
			boolean isEditable = userHasCaseEdit
				&& !report.isOwnershipHandedOver()
				&& (report.getSormasToSormasOriginInfo() == null || report.getSormasToSormasOriginInfo().isOwnershipHandedOver())
				&& ((ReportingType.LABORATORY.equals(report.getReportingType()) && userHasLaboratoryProcessing)
					|| (ReportingType.DOCTOR.equals(report.getReportingType()) && userHasDoctorDeclarationProcessing)
					|| (ReportingType.PHONE_NOTIFICATION.equals(report.getReportingType()) && userHasDoctorDeclarationProcessing));

			if (ReportingType.PHONE_NOTIFICATION.equals(report.getReportingType())) {
				// we do not allow editing frome the side view because we cannot reload the case page
				// phone notifications are only editable from the case page notification box
				listEntry.addActionButton(
					report.getUuid(),
					event -> ControllerProvider.getCaseNotifierSideViewController()
						.editPhoneNotification(caseRef, this::reload, false),
					false);
			} else {
				listEntry.addActionButton(
					report.getUuid(),
					event -> ControllerProvider.getSurveillanceReportController()
						.editSurveillanceReport(listEntry.getReport(), this::reload, isEditable),
					isEditable);
			}

			listEntry.setEnabled(isEditable);

			// We can safely add it because we already checked the permissions above
			ExternalMessageDto externalMessage =
				FacadeProvider.getExternalMessageFacade().getForSurveillanceReport(listEntry.getReport().toReference());
			if (externalMessage != null) {
				listEntry.addAssociatedMessageListener(
					clickEvent -> ControllerProvider.getExternalMessageController().showExternalMessage(externalMessage.getUuid(), false, null));
			}

			listLayout.add(listEntry);
		}
	}

	public class SurveillanceReportListEntry extends SideComponentField {

		private final SurveillanceReportDto report;

		private Button editButton;

		private Button associatedMessageButton;
		private UiFieldAccessCheckers fieldAccessCheckers;

		public SurveillanceReportListEntry(SurveillanceReportDto report) {
			this.report = report;
			this.fieldAccessCheckers = FieldAccessHelper.getFieldAccessCheckers(report);

			VerticalLayout mainLayout = new VerticalLayout();
			mainLayout.setWidthFull();
			mainLayout.setPadding(false);
			mainLayout.setSpacing(false);
			addComponentToField(mainLayout);

			Language userLanguage = UiUtil.getUser().getLanguage();
			mainLayout.add(createRow(null, report.getReportingType(), SurveillanceReportDto.REPORTING_TYPE));
			mainLayout.add(
				createRow(
					I18nProperties.getPrefixCaption(SurveillanceReportDto.I18N_PREFIX, SurveillanceReportDto.REPORT_DATE),
					DateHelper.formatLocalDate(report.getReportDate(), userLanguage),
					SurveillanceReportDto.REPORT_DATE));

			if (report.getDateOfDiagnosis() != null) {
				mainLayout.add(
					createRow(
						I18nProperties.getPrefixCaption(SurveillanceReportDto.I18N_PREFIX, SurveillanceReportDto.DATE_OF_DIAGNOSIS),
						DateHelper.formatLocalDate(report.getDateOfDiagnosis(), userLanguage),
						SurveillanceReportDto.DATE_OF_DIAGNOSIS));
			}

			FacilityReferenceDto facility = report.getFacility();
			if (facility != null) {
				boolean isOtherFacility = facility.getUuid().equals(FacilityDto.OTHER_FACILITY_UUID);
				String facilityName = isOtherFacility ? report.getFacilityDetails() : facility.getCaption();
				String propertyId = isOtherFacility ? SurveillanceReportDto.FACILITY_DETAILS : SurveillanceReportDto.FACILITY;

				mainLayout.add(
					createRow(
						I18nProperties.getPrefixCaption(SurveillanceReportDto.I18N_PREFIX, propertyId),
						facilityName,
						SurveillanceReportDto.FACILITY_DETAILS));
			}
		}

		public SurveillanceReportDto getReport() {
			return report;
		}

		public void addEditListener(int rowIndex, ComponentEventListener<ClickEvent<Button>> clickListener) {
			if (editButton == null) {
				editButton = ButtonHelper.createIconButtonWithCaption(
					"edit-sample-" + rowIndex,
					null,
					VaadinIcon.PENCIL,
					null,
					CssStyles.BUTTON_LINK,
					CssStyles.BUTTON_COMPACT);

				add(editButton);
				setVerticalComponentAlignment(FlexComponent.Alignment.START, editButton);
			}

			editButton.addClickListener(clickListener);
		}

		private HorizontalLayout createRow(@Null String label, Object value, String propertyId) {
			HorizontalLayout row = new HorizontalLayout();
			row.setPadding(false);
			row.setSpacing(false);
			row.setWidthFull();

			if (label != null) {
				Span rowLabel = new Span(DataHelper.toStringNullable(label) + ":");
				CssStyles.style(rowLabel, CssStyles.HSPACE_RIGHT_4);
				row.add(rowLabel);
			}

			Span rowValue = new Span(DataHelper.toStringNullable(value));
			rowValue.addClassName(CssStyles.LABEL_WHITE_SPACE_NORMAL);
			if (!fieldAccessCheckers.isAccessible(SurveillanceReportDto.class, propertyId)) {
				rowValue.addClassName(CssStyles.INACCESSIBLE_LABEL);
			}
			if (label == null) {
				rowValue.addClassName(CssStyles.LABEL_BOLD);
			}
			row.add(rowValue);

			return row;
		}

		public void addAssociatedMessageListener(ComponentEventListener<ClickEvent<Button>> associatedMessageClickListener) {
			if (associatedMessageButton == null) {
				associatedMessageButton = ButtonHelper.createIconButtonWithCaption(
					"see-associated-message-" + report.getUuid(),
					null,
					VaadinIcon.NOTEBOOK,
					associatedMessageClickListener,
					CssStyles.BUTTON_LINK,
					CssStyles.BUTTON_COMPACT);

				add(associatedMessageButton);
				setVerticalComponentAlignment(FlexComponent.Alignment.START, associatedMessageButton);
				associatedMessageButton.setTooltipText(I18nProperties.getDescription(Descriptions.SurveillanceReport_associatedMessage));
			}
		}
	}
}
