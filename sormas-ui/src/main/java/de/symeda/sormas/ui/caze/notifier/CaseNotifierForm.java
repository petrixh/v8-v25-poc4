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

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import de.symeda.sormas.api.caze.surveillancereport.SurveillanceReportDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.location.LocationDto;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.person.notifier.NotifierDto;
import de.symeda.sormas.api.utils.YesNoUnknown;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;
import de.symeda.sormas.ui.utils.fields.DefaultDateFormat;

/**
 * Form for creating and editing notifier information.
 * Provides editable fields for personal info, contact details, and treatment status.
 * 
 * Creates or updates a surveillance report with PHONE_NOTIFICATION reporting type.
 */
public class CaseNotifierForm extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    // Form fields
    private TextField firstNameField;
    private TextField lastNameField;
    private TextField registrationNumberField;
    private TextField phoneField;
    private TextField emailField;
    private TextArea addressField;
    private DatePicker notificationDateField;
    private DatePicker diagnosticDateField;
    private RadioButtonGroup<TreatmentOption> treatmentGroup;
    private TextField agentFirstNameField;
    private TextField agentLastNameField;

    // Data objects
    private NotifierDto notifier;
    private SurveillanceReportDto surveillanceReport;

    /**
     * Creates a new notifier form.
     */
    protected CaseNotifierForm() {
        setClassName("notifier-edit-form");
        setPadding(true);
        setSpacing(true);
        setWidth("800px");
        buildForm();
    }

    /**
     * Creates a new notifier form with initial data.
     */
    public CaseNotifierForm(NotifierDto notifier, SurveillanceReportDto surveillanceReport) {
        this();

        if (notifier == null) {
            throw new IllegalArgumentException("Notifier is null");
        }

        if (surveillanceReport == null) {
            throw new IllegalArgumentException("SurveillanceReport is null");
        }

        this.notifier = notifier;
        this.surveillanceReport = surveillanceReport;
        populateFields();
    }

    /**
     * Builds the form layout with all necessary fields.
     */
    private void buildForm() {
        // Personal Information Section
        Span personalInfoLabel = new Span(I18nProperties.getString(Strings.headingPersonInformation));
        CssStyles.style(personalInfoLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_BOTTOM_LINE);
        add(personalInfoLabel);

        // Name fields
        HorizontalLayout nameLayout = new HorizontalLayout();
        nameLayout.setSpacing(true);
        nameLayout.setWidthFull();

        firstNameField = new TextField(I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, NotifierDto.FIRST_NAME));
        firstNameField.setRequiredIndicatorVisible(true);
        firstNameField.setWidthFull();

        lastNameField = new TextField(I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, NotifierDto.LAST_NAME));
        lastNameField.setRequiredIndicatorVisible(true);
        lastNameField.setWidthFull();

        nameLayout.add(firstNameField, lastNameField);
        nameLayout.setFlexGrow(1, firstNameField);
        nameLayout.setFlexGrow(1, lastNameField);
        add(nameLayout);

        // Registration number
        registrationNumberField = new TextField(I18nProperties.getCaption(Captions.Notification_registrationNumber));
        registrationNumberField.setRequiredIndicatorVisible(true);
        registrationNumberField.setWidthFull();
        add(registrationNumberField);

        // Contact Information Section
        Span contactInfoLabel = new Span(I18nProperties.getString(Strings.headingContactInformation));
        CssStyles.style(contactInfoLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_BOTTOM_LINE);
        add(contactInfoLabel);

        // Contact fields
        HorizontalLayout contactLayout = new HorizontalLayout();
        contactLayout.setSpacing(true);
        contactLayout.setWidthFull();

        phoneField = new TextField(I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, NotifierDto.PHONE));
        phoneField.setWidthFull();

        emailField = new TextField(I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, NotifierDto.EMAIL));
        emailField.setWidthFull();

        contactLayout.add(phoneField, emailField);
        contactLayout.setFlexGrow(1, phoneField);
        contactLayout.setFlexGrow(1, emailField);
        add(contactLayout);

        // Address
        addressField = new TextArea(I18nProperties.getPrefixCaption(LocationDto.I18N_PREFIX, NotifierDto.ADDRESS));
        addressField.setMinRows(3);
        addressField.setWidthFull();
        add(addressField);

        // Notification Dates Section
        Span datesLabel = new Span(I18nProperties.getCaption(Captions.Notification_notifierInformation));
        CssStyles.style(datesLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_BOTTOM_LINE);
        add(datesLabel);

        // Date fields
        HorizontalLayout dateLayout = new HorizontalLayout();
        dateLayout.setSpacing(true);
        dateLayout.setWidthFull();

        notificationDateField = new DatePicker(I18nProperties.getCaption(Captions.Notification_dateOfNotification));
        DefaultDateFormat.applyOnAttach(notificationDateField);
        notificationDateField.setWidthFull();
        notificationDateField.setEnabled(false); // Read-only as it's automatically set
        notificationDateField.setTooltipText(I18nProperties.getString(Strings.notificationNotificationDateInformation));

        diagnosticDateField = new DatePicker(I18nProperties.getCaption(Captions.SurveillanceReport_dateOfDiagnosis));
        DefaultDateFormat.applyOnAttach(diagnosticDateField);
        diagnosticDateField.setWidthFull();

        dateLayout.add(notificationDateField, diagnosticDateField);
        dateLayout.setFlexGrow(1, notificationDateField);
        dateLayout.setFlexGrow(1, diagnosticDateField);
        add(dateLayout);

        // Treatment Section
        treatmentGroup = buildTreatmentOptions();
        add(treatmentGroup);

        // Reporting Agent Section
        Span agentLabel = new Span(I18nProperties.getCaption(Captions.Notification_reportingAgent));
        CssStyles.style(agentLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_BOTTOM_LINE);
        add(agentLabel);

        // Agent fields
        HorizontalLayout agentLayout = new HorizontalLayout();
        agentLayout.setSpacing(true);
        agentLayout.setWidthFull();

        agentFirstNameField = new TextField(I18nProperties.getCaption(Captions.firstName));
        agentFirstNameField.setWidthFull();

        agentLastNameField = new TextField(I18nProperties.getCaption(Captions.lastName));
        agentLastNameField.setWidthFull();

        agentLayout.add(agentFirstNameField, agentLastNameField);
        agentLayout.setFlexGrow(1, agentFirstNameField);
        agentLayout.setFlexGrow(1, agentLastNameField);
        add(agentLayout);
    }

    /**
     * Builds treatment options as a radio button group.
     */
    private RadioButtonGroup<TreatmentOption> buildTreatmentOptions() {
        RadioButtonGroup<TreatmentOption> group = new RadioButtonGroup<>(I18nProperties.getCaption(Captions.Treatment));
        group.setItems(TreatmentOption.ALL_OPTIONS);
        group.setItemLabelGenerator(TreatmentOption::getCaption);
        group.addClassName(SormasFieldGroupFieldFactory.OPTIONGROUP_HORIZONTAL);
        group.setValue(TreatmentOption.UNKNOWN); // Default value

        return group;
    }

    /**
     * Populates form fields with notifier and related data.
     */
    private void populateFields() {
        if (notifier != null) {
            firstNameField.setValue(notifier.getFirstName() != null ? notifier.getFirstName() : "");
            lastNameField.setValue(notifier.getLastName() != null ? notifier.getLastName() : "");
            registrationNumberField.setValue(notifier.getRegistrationNumber() != null ? notifier.getRegistrationNumber() : "");
            phoneField.setValue(notifier.getPhone() != null ? notifier.getPhone() : "");
            emailField.setValue(notifier.getEmail() != null ? notifier.getEmail() : "");
            addressField.setValue(notifier.getAddress() != null ? notifier.getAddress() : "");
            agentFirstNameField.setValue(notifier.getAgentFirstName() != null ? notifier.getAgentFirstName() : "");
            agentLastNameField.setValue(notifier.getAgentLastName() != null ? notifier.getAgentLastName() : "");

            // Set notification date to notifier's creation/modification date
            if (notifier.getCreationDate() != null) {
                notificationDateField.setValue(notifier.getCreationDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            } else if (notifier.getChangeDate() != null) {
                notificationDateField.setValue(notifier.getChangeDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            }

            // Set diagnostic date if available
            if (surveillanceReport != null && surveillanceReport.getDateOfDiagnosis() != null) {
                diagnosticDateField.setValue(surveillanceReport.getDateOfDiagnosis().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            }
        } else {
            // For new notifier, set notification date to current date
            notificationDateField.setValue(java.time.LocalDate.now());
        }

        treatmentGroup.clear();

        if (surveillanceReport != null) {
            // Read treatment values from surveillance report
            if (surveillanceReport.isTreatmentNotApplicable()) {
                treatmentGroup.setValue(TreatmentOption.NOT_APPLICABLE);
            } else {
                final YesNoUnknown treatmentStarted = surveillanceReport.getTreatmentStarted();
                if (treatmentStarted != null) {
                    treatmentGroup.setValue(TreatmentOption.forValue(treatmentStarted));
                }
            }

            // Set notification date from report date
            if (surveillanceReport.getReportDate() != null) {
                notificationDateField.setValue(surveillanceReport.getReportDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            }

            // Set diagnostic date if available
            if (surveillanceReport.getDateOfDiagnosis() != null) {
                diagnosticDateField.setValue(surveillanceReport.getDateOfDiagnosis().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            }
        }

    }

    /**
     * Gets current notifier data from form fields.
     */
    public NotifierDto getValue() {
        if (notifier == null) {
            notifier = new NotifierDto();
        }

        notifier.setFirstName(firstNameField.getValue());
        notifier.setLastName(lastNameField.getValue());
        notifier.setRegistrationNumber(registrationNumberField.getValue());
        notifier.setPhone(phoneField.getValue());
        notifier.setEmail(emailField.getValue());
        notifier.setAddress(addressField.getValue());
        notifier.setAgentFirstName(agentFirstNameField.getValue());
        notifier.setAgentLastName(agentLastNameField.getValue());

        // Note: notification date is handled automatically by the system
        // based on creation/modification times, not user input

        return notifier;
    }

    /**
     * Gets the notification date from the form.
     */
    public LocalDate getNotificationDate() {
        return notificationDateField.getValue();
    }

    /**
     * Gets the diagnostic date from the form.
     */
    public LocalDate getDiagnosticDate() {
        return diagnosticDateField.getValue();
    }

    /**
     * Gets the selected treatment option from the form.
     */
    public TreatmentOption getSelectedTreatmentOption() {
        return treatmentGroup.getValue();
    }

    /**
     * Sets notifier data and populates form fields.
     */
    public void setValue(NotifierDto notifier, SurveillanceReportDto surveillanceReport) {
        if (notifier == null) {
            throw new IllegalArgumentException("Notifier is null");
        }

        if (surveillanceReport == null) {
            throw new IllegalArgumentException("SurveillanceReport is null");
        }

        this.notifier = notifier;
        this.surveillanceReport = surveillanceReport;
        populateFields();
    }

    /**
     * Validates form and returns true if all required fields are filled.
     */
    public boolean isValid() {
        boolean valid = true;

        if (firstNameField.getValue() == null || firstNameField.getValue().trim().isEmpty()) {
            firstNameField.setErrorMessage(I18nProperties.getValidationError(Validations.requiredField));
            firstNameField.setInvalid(true);
            valid = false;
        } else {
            firstNameField.setErrorMessage(null);
            firstNameField.setInvalid(false);
        }

        if (lastNameField.getValue() == null || lastNameField.getValue().trim().isEmpty()) {
            lastNameField.setErrorMessage(I18nProperties.getValidationError(Validations.requiredField));
            lastNameField.setInvalid(true);
            valid = false;
        } else {
            lastNameField.setErrorMessage(null);
            lastNameField.setInvalid(false);
        }

        if (registrationNumberField.getValue() == null || registrationNumberField.getValue().trim().isEmpty()) {
            registrationNumberField.setErrorMessage(I18nProperties.getValidationError(Validations.requiredField));
            registrationNumberField.setInvalid(true);
            valid = false;
        } else {
            registrationNumberField.setErrorMessage(null);
            registrationNumberField.setInvalid(false);
        }

        return valid;
    }
}
