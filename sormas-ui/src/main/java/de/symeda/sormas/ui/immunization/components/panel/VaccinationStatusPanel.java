/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2026 SORMAS Foundation gGmbH
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

package de.symeda.sormas.ui.immunization.components.panel;

import java.util.Date;
import java.util.Objects;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.caze.VaccinationStatus;
import de.symeda.sormas.api.contact.ContactDto;
import de.symeda.sormas.api.event.EventDto;
import de.symeda.sormas.api.event.EventParticipantDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.immunization.InformationReliability;
import de.symeda.sormas.api.immunization.VaccinationStatusData;
import de.symeda.sormas.api.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.CssStyles;

@SuppressWarnings({
    "java:S2160", // suppress sonar missing equals
    "java:S110" // suppress sonar too many parents warning
})
public class VaccinationStatusPanel extends VerticalLayout {

    private static final long serialVersionUID = -4863945869156850850L;

    private transient VaccinationStatusData data;

    private final String captionVaccinationStatus;
    private final String captionVaccinationStatusDetails;
    private final String captionNumberOfDoses;
    private final String captionDateOfLastDose;
    private final String captionInformationReliability;
    private final String captionReferenceDate;
    private final String captionReferencePeriod;
    private final String captionVaccinationStatusLastUpdated;

    private HorizontalLayout vaccinationStatusRow;
    private HorizontalLayout vaccinationStatusDetailsRow;
    private HorizontalLayout numberOfDosesRow;
    private HorizontalLayout dateOfLastDoseRow;
    private HorizontalLayout informationReliabilityRow;
    private HorizontalLayout referencePeriodRow;
    private HorizontalLayout vaccinationStatusLastUpdatedRow;

    private Span vaccinationStatusValueLabel;
    private Span vaccinationStatusDetailsValueLabel;
    private Span numberOfDosesValueLabel;
    private Span dateOfLastDoseValueLabel;
    private Span informationReliabilityValueLabel;
    private Span referencePeriodCaptionLabel;
    private Span referencePeriodValueLabel;
    private Span vaccinationStatusLastUpdatedValueLabel;

    public VaccinationStatusPanel() {

        captionVaccinationStatus = I18nProperties.getCaption("ImmunizationSidePanel.vaccinationStatus");
        captionVaccinationStatusDetails = I18nProperties.getCaption("ImmunizationSidePanel.vaccinationStatusDetails");
        captionNumberOfDoses = I18nProperties.getCaption("ImmunizationSidePanel.numberOfDoses");
        captionDateOfLastDose = I18nProperties.getCaption("ImmunizationSidePanel.dateOfLastDose");
        captionInformationReliability = I18nProperties.getCaption("ImmunizationSidePanel.informationReliability");
        captionReferenceDate = I18nProperties.getCaption("ImmunizationSidePanel.referenceDate", "Reference date");
        captionReferencePeriod = I18nProperties.getCaption("ImmunizationSidePanel.referencePeriod");
        captionVaccinationStatusLastUpdated = I18nProperties.getCaption("ImmunizationSidePanel.vaccinationStatusLastUpdated");

        setWidthFull();
        setPadding(false);
        setSpacing(false);

        initializeComponents();
        buildLayout();
    }

    public static VaccinationStatusPanel forCase(CaseDataDto caze) {
        VaccinationStatusData caseData = VaccinationStatusData.Builder.createBlank()
            .vaccinationStatus(caze.getVaccinationStatus())
            .vaccinationStatusDetails(caze.getVaccinationStatusDetails())
            .numberOfDoses(caze.getNumberOfDoses())
            .informationReliability(caze.getInformationReliability())
            .referencePeriodFrom(caze.getReportDate())
            .referencePeriodTo(caze.getReportDate())
            .vaccinationStatusLastUpdated(caze.getVaccinationStatusLastUpdated())
            .build();

        return createPanel(caseData);
    }

    public static VaccinationStatusPanel forContact(ContactDto contact) {
        Date referencePeriodFrom = contact.getFirstContactDate();
        if (referencePeriodFrom == null) {
            referencePeriodFrom = contact.getLastContactDate() != null ? contact.getLastContactDate() : contact.getReportDateTime();
        }
        Date referencePeriodTo = contact.getLastContactDate() != null ? contact.getLastContactDate() : referencePeriodFrom;

        VaccinationStatusData contactData = VaccinationStatusData.Builder.createBlank()
            .vaccinationStatus(contact.getVaccinationStatus())
            .vaccinationStatusDetails(contact.getVaccinationStatusDetails())
            .numberOfDoses(contact.getNumberOfDoses())
            .informationReliability(contact.getInformationReliability())
            .referencePeriodFrom(referencePeriodFrom)
            .referencePeriodTo(referencePeriodTo)
            .vaccinationStatusLastUpdated(contact.getVaccinationStatusLastUpdated())
            .build();

        return createPanel(contactData);
    }

    public static VaccinationStatusPanel forEventParticipant(EventParticipantDto eventParticipant, EventDto event) {
        Date referencePeriodFrom = event.getStartDate() != null ? event.getStartDate() : event.getReportDateTime();
        Date referencePeriodTo = event.getEndDate() != null ? event.getEndDate() : referencePeriodFrom;

        VaccinationStatusData eventParticipantData = VaccinationStatusData.Builder.createBlank()
            .vaccinationStatus(eventParticipant.getVaccinationStatus())
            .vaccinationStatusDetails(eventParticipant.getVaccinationStatusDetails())
            .numberOfDoses(eventParticipant.getNumberOfDoses())
            .informationReliability(eventParticipant.getInformationReliability())
            .referencePeriodFrom(referencePeriodFrom)
            .referencePeriodTo(referencePeriodTo)
            .vaccinationStatusLastUpdated(eventParticipant.getVaccinationStatusLastUpdated())
            .build();

        return createPanel(eventParticipantData);
    }

    private static VaccinationStatusPanel createPanel(VaccinationStatusData entityData) {
        VaccinationStatusPanel panel = new VaccinationStatusPanel();
        panel.setValue(entityData);
        return panel;
    }

    protected void initializeComponents() {
        vaccinationStatusValueLabel = new Span();
        vaccinationStatusRow = createLabelRow(captionVaccinationStatus, vaccinationStatusValueLabel);

        vaccinationStatusDetailsValueLabel = new Span();
        vaccinationStatusDetailsRow = createLabelRow(captionVaccinationStatusDetails, vaccinationStatusDetailsValueLabel);

        numberOfDosesValueLabel = new Span();
        numberOfDosesRow = createLabelRow(captionNumberOfDoses, numberOfDosesValueLabel);

        dateOfLastDoseValueLabel = new Span();
        dateOfLastDoseRow = createLabelRow(captionDateOfLastDose, dateOfLastDoseValueLabel);

        informationReliabilityValueLabel = new Span();
        informationReliabilityRow = createLabelRow(captionInformationReliability, informationReliabilityValueLabel);

        referencePeriodCaptionLabel = new Span(captionReferencePeriod);
        referencePeriodValueLabel = new Span();
        referencePeriodRow = createLabelRow(referencePeriodCaptionLabel, referencePeriodValueLabel);

        vaccinationStatusLastUpdatedValueLabel = new Span();
        vaccinationStatusLastUpdatedRow = createLabelRow(captionVaccinationStatusLastUpdated, vaccinationStatusLastUpdatedValueLabel);
    }

    protected HorizontalLayout createLabelRow(String caption, Span valueLabel) {
        return createLabelRow(new Span(caption), valueLabel);
    }

    protected HorizontalLayout createLabelRow(Span captionLabel, Span valueLabel) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setPadding(false);
        row.setSpacing(false);

        String caption = captionLabel.getText();
        captionLabel.setTitle(caption);
        valueLabel.setTitle(caption);
        captionLabel.setWidth(null);

        row.add(captionLabel, valueLabel);

        row.setFlexGrow(0.5, captionLabel);
        row.setFlexGrow(1.0, valueLabel);
        return row;
    }

    protected void buildLayout() {
        add(vaccinationStatusRow);
        add(vaccinationStatusDetailsRow);
        add(numberOfDosesRow);
        add(dateOfLastDoseRow);
        add(informationReliabilityRow);
        add(referencePeriodRow);
        add(vaccinationStatusLastUpdatedRow);
    }

    protected void updateComponents() {
        if (data == null) {
            clear();
            return;
        }

        VaccinationStatus vaccinationStatus = data.getVaccinationStatus();
        if (vaccinationStatus != null) {
            vaccinationStatusRow.setVisible(true);
            vaccinationStatusValueLabel.setText(vaccinationStatus.toString());
        } else {
            vaccinationStatusRow.setVisible(false);
            vaccinationStatusValueLabel.setText("");
        }
        vaccinationStatusValueLabel.removeClassNames(CssStyles.LABEL_POSITIVE, CssStyles.LABEL_CRITICAL);
        if (vaccinationStatus == VaccinationStatus.VACCINATED) {
            vaccinationStatusValueLabel.addClassName(CssStyles.LABEL_POSITIVE);
        } else if (vaccinationStatus == VaccinationStatus.UNVACCINATED) {
            vaccinationStatusValueLabel.addClassName(CssStyles.LABEL_CRITICAL);
        }

        String details = data.getVaccinationStatusDetails();
        boolean showDetails = data.getVaccinationStatus() == VaccinationStatus.OTHER && details != null && !details.trim().isEmpty();
        vaccinationStatusDetailsRow.setVisible(showDetails);
        vaccinationStatusDetailsValueLabel.setText(showDetails ? details : "");

        boolean showNumberOfDoses = data.getNumberOfDoses() != null;
        numberOfDosesRow.setVisible(showNumberOfDoses);
        numberOfDosesValueLabel.setText(showNumberOfDoses ? data.getNumberOfDoses().toString() : "");

        boolean showDateOfLastDose = data.getDateOfLastDose() != null;
        dateOfLastDoseRow.setVisible(showDateOfLastDose);
        dateOfLastDoseValueLabel.setText(showDateOfLastDose ? DateFormatHelper.formatLocalDate(data.getDateOfLastDose()) : "");

        InformationReliability reliability = data.getInformationReliability();
        if (reliability != null) {
            informationReliabilityRow.setVisible(true);
            informationReliabilityValueLabel.setText(reliability.toString());
        } else {
            informationReliabilityRow.setVisible(false);
            informationReliabilityValueLabel.setText("");
        }

        Date referencePeriodFromDate = data.getReferencePeriodFrom();
        Date referencePeriodToDate = data.getReferencePeriodTo();
        String periodFrom = referencePeriodFromDate != null ? DateFormatHelper.formatLocalDate(referencePeriodFromDate) : null;
        String periodTo = referencePeriodToDate != null ? DateFormatHelper.formatLocalDate(referencePeriodToDate) : null;
        boolean showReferencePeriod = periodFrom != null || periodTo != null;
        referencePeriodRow.setVisible(showReferencePeriod);
        if (showReferencePeriod) {
            boolean isSingleReferenceDate =
                referencePeriodFromDate == null || referencePeriodToDate == null || Objects.equals(referencePeriodFromDate, referencePeriodToDate);
            referencePeriodCaptionLabel.setText(isSingleReferenceDate ? captionReferenceDate : captionReferencePeriod);
            String referenceCaption = referencePeriodCaptionLabel.getText();
            referencePeriodCaptionLabel.setTitle(referenceCaption);
            referencePeriodValueLabel.setTitle(referenceCaption);
            String referencePeriod;
            if (periodFrom != null && periodTo != null && !Objects.equals(referencePeriodFromDate, referencePeriodToDate)) {
                referencePeriod = periodFrom + " - " + periodTo;
            } else if (periodFrom != null) {
                referencePeriod = periodFrom;
            } else {
                referencePeriod = periodTo;
            }
            referencePeriodValueLabel.setText(referencePeriod);
        } else {
            referencePeriodCaptionLabel.setText(captionReferencePeriod);
            referencePeriodCaptionLabel.setTitle(captionReferencePeriod);
            referencePeriodValueLabel.setTitle(captionReferencePeriod);
            referencePeriodValueLabel.setText("");
        }

        boolean showVaccinationStatusLastUpdated = data.getVaccinationStatusLastUpdated() != null;
        vaccinationStatusLastUpdatedRow.setVisible(showVaccinationStatusLastUpdated);
        vaccinationStatusLastUpdatedValueLabel
            .setText(showVaccinationStatusLastUpdated ? DateFormatHelper.formatDate(data.getVaccinationStatusLastUpdated()) : "");
    }

    public void setValue(VaccinationStatusData data) {
        this.data = data;
        updateComponents();
    }

    public void clear() {
        vaccinationStatusValueLabel.setText("");
        vaccinationStatusRow.setVisible(false);
        vaccinationStatusValueLabel.removeClassNames(CssStyles.LABEL_POSITIVE, CssStyles.LABEL_CRITICAL);
        vaccinationStatusDetailsValueLabel.setText("");
        vaccinationStatusDetailsRow.setVisible(false);
        numberOfDosesValueLabel.setText("");
        numberOfDosesRow.setVisible(false);
        dateOfLastDoseValueLabel.setText("");
        dateOfLastDoseRow.setVisible(false);
        informationReliabilityValueLabel.setText("");
        informationReliabilityRow.setVisible(false);
        referencePeriodValueLabel.setText("");
        referencePeriodRow.setVisible(false);
        vaccinationStatusLastUpdatedValueLabel.setText("");
        vaccinationStatusLastUpdatedRow.setVisible(false);
    }
}
