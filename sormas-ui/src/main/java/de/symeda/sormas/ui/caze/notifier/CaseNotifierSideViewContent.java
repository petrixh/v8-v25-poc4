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

package de.symeda.sormas.ui.caze.notifier;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;

import de.symeda.sormas.api.caze.surveillancereport.ReportingType;
import de.symeda.sormas.api.caze.surveillancereport.SurveillanceReportDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.person.notifier.NotifierDto;
import de.symeda.sormas.api.utils.YesNoUnknown;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;
import de.symeda.sormas.ui.utils.fields.DefaultDateFormat;

/**
 * Side view content for case notifiers.
 * Displays notifier details, notification dates, and treatment status.
 */
public class CaseNotifierSideViewContent extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    /** The surveillance report with treatment data. */
    private SurveillanceReportDto surveillanceReport;

    /** The notifier details. */
    private NotifierDto notifier;

    /**
     * Creates a new case notifier side view content.
     * 
     * @param surveillanceReport
     *            the surveillance report with treatment data and dates
     * @param notifier
     *            the notifier details
     */
    public CaseNotifierSideViewContent(SurveillanceReportDto surveillanceReport, NotifierDto notifier) {

        this.surveillanceReport = surveillanceReport;
        this.notifier = notifier;

        setClassName("case-notifier-side-view");
        setPadding(false);
        setSpacing(false);
        buildNotifierDetails();
    }

    /**
     * Builds the notifier details layout with personal info, dates, and treatment status.
     */
    private void buildNotifierDetails() {

        // Full name
        Span fullNameLabel = new Span(notifier.getFirstName() + " " + notifier.getLastName());
        CssStyles.style(fullNameLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
        add(fullNameLabel);

        // Registration number
        Span registrationNumberLabel = new Span(notifier.getRegistrationNumber());
        CssStyles.style(registrationNumberLabel, CssStyles.LABEL_BOLD);
        add(registrationNumberLabel);

        // Spacer before contact label
        if ((notifier.getPhone() != null && !notifier.getPhone().isEmpty()) || (notifier.getEmail() != null && !notifier.getEmail().isEmpty())) {
            Span spacerBeforeContact = new Span();
            spacerBeforeContact.setHeight("0.1rem");
            add(spacerBeforeContact);
        }

        // Phone
        if (notifier.getPhone() != null && !notifier.getPhone().isEmpty()) {
            Span phoneLabel = new Span(notifier.getPhone());
            add(phoneLabel);
        }

        // Email
        if (notifier.getEmail() != null && !notifier.getEmail().isEmpty()) {
            Span emailLabel = new Span(notifier.getEmail());
            add(emailLabel);
        }

        // Spacer before email label
        Span spacerDates = new Span();
        spacerDates.setHeight("0.1rem");

        // Date of notification
        DatePicker notificationDateField = new DatePicker(I18nProperties.getCaption(Captions.Notification_dateOfNotification));
        DefaultDateFormat.applyOnAttach(notificationDateField);
        if (surveillanceReport != null && surveillanceReport.getReportDate() != null) {
            notificationDateField.setValue(surveillanceReport.getReportDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        } else {
            notificationDateField.setValue(
                notifier.getChangeDate() != null
                    ? notifier.getChangeDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    : LocalDate.now());
        }
        notificationDateField.setReadOnly(true);

        // Date of diagnostic
        DatePicker diagnosticDateField = new DatePicker(I18nProperties.getCaption(Captions.SurveillanceReport_dateOfDiagnosis));
        DefaultDateFormat.applyOnAttach(diagnosticDateField);
        if (surveillanceReport != null && surveillanceReport.getDateOfDiagnosis() != null) {
            diagnosticDateField.setValue(surveillanceReport.getDateOfDiagnosis().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }
        diagnosticDateField.setReadOnly(true);

        // Horizontal layout for date fields
        HorizontalLayout dateFieldsLayout = new HorizontalLayout(notificationDateField, diagnosticDateField);
        dateFieldsLayout.setSpacing(true);
        dateFieldsLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        add(spacerDates);
        add(dateFieldsLayout);

        RadioButtonGroup<TreatmentOption> treatmentGroup = buildTreatmentOptions();
        add(treatmentGroup);

        if ((notifier.getAgentFirstName() != null && !notifier.getAgentFirstName().isBlank())
            || (notifier.getAgentLastName() != null && !notifier.getAgentLastName().isBlank())) {
            // Spacer before Reporting Agent label
            Span spacerAgent = new Span();
            spacerAgent.setHeight("0.1rem");
            add(spacerAgent);
            Span reportingAgent = new Span(I18nProperties.getCaption(Captions.Notification_reportingAgent));
            CssStyles.style(reportingAgent, CssStyles.LABEL_BOLD, CssStyles.LABEL_BOTTOM_LINE);
            add(reportingAgent);
            Span agentNameLabel = new Span(notifier.getAgentFirstName() + " " + notifier.getAgentLastName().toUpperCase(Locale.ROOT));
            CssStyles.style(agentNameLabel, CssStyles.LABEL_RELEVANT);
            add(agentNameLabel);
        }

        Span spacerNotificationType = new Span();
        spacerNotificationType.setHeight("0.1rem");
        add(spacerNotificationType);
        // Notification type
        Span notificationTypeLabel = new Span(
            (surveillanceReport != null && surveillanceReport.getReportingType() == ReportingType.PHONE_NOTIFICATION)
                ? I18nProperties.getCaption(Captions.Notification_notificationTypePhone)
                : I18nProperties.getCaption(Captions.Notification_notificationTypeExternal));
        CssStyles.style(notificationTypeLabel, CssStyles.BADGE);
        add(notificationTypeLabel);

    }

    /**
     * Builds treatment options as a radio button group.
     *
     * @return radio button group with treatment options
     */
    private RadioButtonGroup<TreatmentOption> buildTreatmentOptions() {
        ListDataProvider<TreatmentOption> dataProvider = DataProvider.ofCollection(TreatmentOption.ALL_OPTIONS);

        // Treatment radios
        RadioButtonGroup<TreatmentOption> treatmentGroup = new RadioButtonGroup<>(I18nProperties.getCaption(Captions.Treatment));
        treatmentGroup.setItems(dataProvider);
        treatmentGroup.setItemLabelGenerator(t -> t.getCaption());

        treatmentGroup.addClassName(SormasFieldGroupFieldFactory.OPTIONGROUP_HORIZONTAL);
        treatmentGroup.setReadOnly(true);
        treatmentGroup.clear();

        if (surveillanceReport == null) {
            return treatmentGroup;
        }

        if (surveillanceReport.isTreatmentNotApplicable()) {
            treatmentGroup.setValue(TreatmentOption.NOT_APPLICABLE);
            return treatmentGroup;
        }

        final YesNoUnknown treatmentStarted = surveillanceReport.getTreatmentStarted();

        if (treatmentStarted == null) {
            return treatmentGroup;
        }

        treatmentGroup.setValue(TreatmentOption.forValue(treatmentStarted));

        return treatmentGroup;
    }
}
