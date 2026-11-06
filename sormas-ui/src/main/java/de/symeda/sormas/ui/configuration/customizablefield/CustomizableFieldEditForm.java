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

package de.symeda.sormas.ui.configuration.customizablefield;

import static de.symeda.sormas.ui.utils.CssStyles.H3;
import static de.symeda.sormas.ui.utils.LayoutUtil.fluidRowLocs;
import static de.symeda.sormas.ui.utils.LayoutUtil.loc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import de.symeda.sormas.ui.utils.CustomLayout;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.customizablefield.CustomizableFieldContext;
import de.symeda.sormas.api.customizablefield.CustomizableFieldCustomProperties;
import de.symeda.sormas.api.customizablefield.CustomizableFieldGroup;
import de.symeda.sormas.api.customizablefield.CustomizableFieldMetadataDto;
import de.symeda.sormas.api.customizablefield.CustomizableFieldType;
import de.symeda.sormas.api.customizablefield.CustomizableFieldVisibilityRestrictions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.utils.DateFormatHelper;
import de.symeda.sormas.api.utils.YesNoUnknown;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.components.CheckboxSet;
import de.symeda.sormas.ui.utils.fields.KeepValueComboBox;

/**
 * Edit form for creating and editing customizable field metadata.
 */
@SuppressWarnings({
    "java:S110", // suppress sonar too many parents warning
    "java:S2160" // suppress missing equals
})
public class CustomizableFieldEditForm extends CustomLayout {

    private static final long serialVersionUID = 1L;

    private static final String DISEASES_LOC = "diseasesLoc";
    private static final String BASICS_HEADING_LOC = "basicsHeadingLoc";
    private static final String PLACEMENT_HEADING_LOC = "placementHeadingLoc";
    private static final String BEHAVIOR_HEADING_LOC = "behaviorHeadingLoc";
    private static final String VISIBILITY_HEADING_LOC = "visibilityHeadingLoc";
    private static final String OPTIONS_HEADING_LOC = "optionsHeadingLoc";
    private static final String OPTIONS_LOC = "optionsLoc";
    private static final String TRANSLATIONS_HEADING_LOC = "translationsHeadingLoc";
    private static final String TRANSLATIONS_LOC = "translationsLoc";

    private static final Set<CustomizableFieldType> OPTIONS_TYPES =
        EnumSet.of(CustomizableFieldType.COMBOBOX, CustomizableFieldType.CHECKBOX_LIST, CustomizableFieldType.RADIO_BUTTON_LIST);

    //@formatter:off
	private static final String HTML_LAYOUT =
		loc(BASICS_HEADING_LOC)
			+ fluidRowLocs(CustomizableFieldMetadataDto.NAME, CustomizableFieldMetadataDto.FIELD_TYPE)
			+ fluidRowLocs(CustomizableFieldMetadataDto.DESCRIPTION)
            + fluidRowLocs(CustomizableFieldMetadataDto.DEFAULT_VALUE)
			+ loc(OPTIONS_HEADING_LOC)
			+ fluidRowLocs(OPTIONS_LOC)
			+ loc(PLACEMENT_HEADING_LOC)
			+ fluidRowLocs(CustomizableFieldMetadataDto.CONTEXT_CLASS, CustomizableFieldMetadataDto.UI_GROUP)
			+ fluidRowLocs(CustomizableFieldMetadataDto.UI_LINE_POSITION, CustomizableFieldMetadataDto.UI_LINE_WEIGHT)
			+ loc(BEHAVIOR_HEADING_LOC)
			+ fluidRowLocs(CustomizableFieldMetadataDto.ACTIVE, CustomizableFieldMetadataDto.READ_ONLY)
			+ fluidRowLocs(CustomizableFieldMetadataDto.MANDATORY)
			+ loc(VISIBILITY_HEADING_LOC)
			+ fluidRowLocs(DISEASES_LOC)
			+ loc(TRANSLATIONS_HEADING_LOC)
			+ fluidRowLocs(TRANSLATIONS_LOC);
	//@formatter:on

    private final Binder<CustomizableFieldMetadataDto> binder = new Binder<>(CustomizableFieldMetadataDto.class);
    private final KeepValueComboBox<CustomizableFieldGroup> groupCombo;
    private final CheckboxSet<Disease> cbsDiseases;
    private final TextField defaultValueField;
    private final KeepValueComboBox<String> defaultValueCombo;
    private final ComboBox<Boolean> defaultValueCheckboxCombo;
    private final ComboBox<YesNoUnknown> defaultValueYesNoUnknownCombo;
    private final DatePicker defaultValueDateField;
    private final DateTimePicker defaultValueDateTimeField;
    private final CustomizableFieldOptionsComponent optionsComponent;
    private final Span optionsHeading;
    private final CustomizableFieldTranslationsComponent translationsComponent;

    private CustomizableFieldMetadataDto dto;

    public CustomizableFieldEditForm(boolean isEdit) {

        setTemplateContents(HTML_LAYOUT);
        setWidth(840, Unit.PIXELS);

        // Section headings
        Span basicsHeading = new Span(I18nProperties.getString(Strings.headingCustomizableFieldBasics));
        basicsHeading.addClassName(H3);
        addComponent(basicsHeading, BASICS_HEADING_LOC);

        Span placementHeading = new Span(I18nProperties.getString(Strings.headingCustomizableFieldPlacement));
        placementHeading.addClassName(H3);
        addComponent(placementHeading, PLACEMENT_HEADING_LOC);

        Span behaviorHeading = new Span(I18nProperties.getString(Strings.headingCustomizableFieldBehavior));
        behaviorHeading.addClassName(H3);
        addComponent(behaviorHeading, BEHAVIOR_HEADING_LOC);

        // --- BASICS ---

        TextField nameField = new TextField(caption(CustomizableFieldMetadataDto.NAME));
        nameField.setWidth(100, Unit.PERCENTAGE);
        nameField.setRequiredIndicatorVisible(true);
        if (isEdit) {
            nameField.setPlaceholder(caption(CustomizableFieldMetadataDto.NAME));
        }
        addComponent(nameField, CustomizableFieldMetadataDto.NAME);
        binder.forField(nameField)
            .asRequired(I18nProperties.getValidationError(Validations.required, caption(CustomizableFieldMetadataDto.NAME)))
            .withValidator(new StringLengthValidator(I18nProperties.getValidationError(Validations.textTooLong, 512), null, 512))
            .bind(CustomizableFieldMetadataDto::getName, CustomizableFieldMetadataDto::setName);

        ComboBox<CustomizableFieldType> fieldTypeCombo = new ComboBox<>(caption(CustomizableFieldMetadataDto.FIELD_TYPE));
        fieldTypeCombo.setWidth(100, Unit.PERCENTAGE);
        fieldTypeCombo.setItems(CustomizableFieldType.values());
        fieldTypeCombo.setItemLabelGenerator(I18nProperties::getEnumCaption);
        fieldTypeCombo.setRequiredIndicatorVisible(true);
        if (isEdit) {
            fieldTypeCombo.setEnabled(false);
        }
        addComponent(fieldTypeCombo, CustomizableFieldMetadataDto.FIELD_TYPE);
        binder.forField(fieldTypeCombo)
            .asRequired(I18nProperties.getValidationError(Validations.required, caption(CustomizableFieldMetadataDto.FIELD_TYPE)))
            .bind(CustomizableFieldMetadataDto::getFieldType, CustomizableFieldMetadataDto::setFieldType);
        fieldTypeCombo.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getValue() == null) {
                // was setEmptySelectionAllowed(false): a Flow combo box has no empty item, but the user can clear it
                fieldTypeCombo.setValue(e.getOldValue());
                return;
            }
            updateOptionsVisibility(e.getValue());
        });

        TextField descriptionField = new TextField(caption(CustomizableFieldMetadataDto.DESCRIPTION));
        descriptionField.setWidth(100, Unit.PERCENTAGE);
        addComponent(descriptionField, CustomizableFieldMetadataDto.DESCRIPTION);
        binder.forField(descriptionField)
            .withNullRepresentation("")
            .bind(CustomizableFieldMetadataDto::getDescription, CustomizableFieldMetadataDto::setDescription);

        String defaultValueCaption = caption(CustomizableFieldMetadataDto.DEFAULT_VALUE);
        defaultValueField = new TextField(defaultValueCaption);
        defaultValueField.setWidth(100, Unit.PERCENTAGE);
        addComponent(defaultValueField, CustomizableFieldMetadataDto.DEFAULT_VALUE);
        binder.forField(defaultValueField)
            .withNullRepresentation("")
            .withValidator(
                value -> value == null || value.isEmpty() || isValidDefaultValueForType(value, fieldTypeCombo.getValue()),
                errorMessage -> getDefaultValueErrorMessage(fieldTypeCombo.getValue()))
            .bind(CustomizableFieldMetadataDto::getDefaultValue, CustomizableFieldMetadataDto::setDefaultValue);

        defaultValueCombo = new KeepValueComboBox<>(defaultValueCaption);
        defaultValueCombo.setWidth(100, Unit.PERCENTAGE);
        // was setEmptySelectionAllowed(true): the empty item is the clear button
        defaultValueCombo.setClearButtonVisible(true);

        defaultValueCheckboxCombo = new ComboBox<>(defaultValueCaption);
        defaultValueCheckboxCombo.setWidth(100, Unit.PERCENTAGE);
        // was setEmptySelectionAllowed(true): the empty item is the clear button
        defaultValueCheckboxCombo.setClearButtonVisible(true);
        defaultValueCheckboxCombo.setItems(Boolean.TRUE, Boolean.FALSE);
        defaultValueCheckboxCombo.setItemLabelGenerator(value -> Boolean.TRUE.equals(value) ? "True" : "False");

        defaultValueYesNoUnknownCombo = new ComboBox<>(defaultValueCaption);
        defaultValueYesNoUnknownCombo.setWidth(100, Unit.PERCENTAGE);
        // was setEmptySelectionAllowed(true): the empty item is the clear button
        defaultValueYesNoUnknownCombo.setClearButtonVisible(true);
        defaultValueYesNoUnknownCombo.setItems(YesNoUnknown.values());
        defaultValueYesNoUnknownCombo.setItemLabelGenerator(I18nProperties::getEnumCaption);

        defaultValueDateField = new DatePicker(defaultValueCaption);
        defaultValueDateField.setWidth(100, Unit.PERCENTAGE);
        DatePicker.DatePickerI18n dateI18n = new DatePicker.DatePickerI18n();
        dateI18n.setDateFormat(DateFormatHelper.getDateFormatPattern());
        defaultValueDateField.setI18n(dateI18n);

        defaultValueDateTimeField = new DateTimePicker(defaultValueCaption);
        defaultValueDateTimeField.setWidth(100, Unit.PERCENTAGE);
        // was setDateFormat(<user's date-time pattern>): the Flow picker formats the date part with the user's date
        // pattern and the time part by locale (as CustomizableFieldInputDateTime)
        DatePicker.DatePickerI18n dateTimeI18n = new DatePicker.DatePickerI18n();
        dateTimeI18n.setDateFormat(DateFormatHelper.getDateFormatPattern());
        defaultValueDateTimeField.setDatePickerI18n(dateTimeI18n);

        optionsHeading = new Span(I18nProperties.getString(Strings.labelCustomizableFieldOptions));
        optionsHeading.addClassName(CssStyles.VAADIN_CAPTION);
        optionsHeading.setVisible(false);
        addComponent(optionsHeading, OPTIONS_HEADING_LOC);

        optionsComponent = new CustomizableFieldOptionsComponent();
        optionsComponent.setVisible(false);
        addComponent(optionsComponent, OPTIONS_LOC);
        optionsComponent.addOptionsChangeListener(() -> {
            List<String> current = optionsComponent.getValue();
            String selected = defaultValueCombo.getValue();
            defaultValueCombo.setItems(current != null ? current : Collections.emptyList());
            defaultValueCombo.setValue(current != null && current.contains(selected) ? selected : null);
        });

        // --- PLACEMENT ---

        ComboBox<CustomizableFieldContext> contextCombo = new ComboBox<>(caption(CustomizableFieldMetadataDto.CONTEXT_CLASS));
        contextCombo.setWidth(100, Unit.PERCENTAGE);
        contextCombo.setItems(CustomizableFieldContext.values());
        contextCombo.setItemLabelGenerator(I18nProperties::getEnumCaption);
        contextCombo.setRequiredIndicatorVisible(true);
        addComponent(contextCombo, CustomizableFieldMetadataDto.CONTEXT_CLASS);
        binder.forField(contextCombo)
            .asRequired(I18nProperties.getValidationError(Validations.required, caption(CustomizableFieldMetadataDto.CONTEXT_CLASS)))
            .bind(CustomizableFieldMetadataDto::getContextClass, CustomizableFieldMetadataDto::setContextClass);

        groupCombo = new KeepValueComboBox<>(caption(CustomizableFieldMetadataDto.UI_GROUP));
        groupCombo.setWidth(100, Unit.PERCENTAGE);
        // was setEmptySelectionAllowed(false): a Flow combo box has no empty item, but the user can clear it
        groupCombo.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getValue() == null) {
                groupCombo.setValue(e.getOldValue());
            }
        });
        groupCombo.setItemLabelGenerator(I18nProperties::getEnumCaption);
        groupCombo.setRequiredIndicatorVisible(true);
        addComponent(groupCombo, CustomizableFieldMetadataDto.UI_GROUP);
        binder.forField(groupCombo)
            .asRequired(I18nProperties.getValidationError(Validations.required, caption(CustomizableFieldMetadataDto.UI_GROUP)))
            .bind(CustomizableFieldMetadataDto::getUiGroup, CustomizableFieldMetadataDto::setUiGroup);

        // Repopulate group items when context changes; clear value first, then retain only if still valid
        contextCombo.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getValue() == null) {
                // was setEmptySelectionAllowed(false): a Flow combo box has no empty item, but the user can clear it
                contextCombo.setValue(e.getOldValue());
                return;
            }
            CustomizableFieldContext ctx = e.getValue();
            CustomizableFieldGroup current = groupCombo.getValue();
            groupCombo.setValue(null);
            if (ctx != null) {
                groupCombo.setItems(CustomizableFieldGroup.getGroupsForContext(ctx));
                if (current != null && current.getContext() == ctx) {
                    groupCombo.setValue(current);
                }
            } else {
                groupCombo.setItems();
            }
        });

        String posCaption = caption(CustomizableFieldMetadataDto.UI_LINE_POSITION);
        TextField positionField = new TextField(posCaption);
        positionField.setWidth(100, Unit.PERCENTAGE);
        addComponent(positionField, CustomizableFieldMetadataDto.UI_LINE_POSITION);
        binder.forField(positionField)
            .withNullRepresentation("")
            .withConverter(new StringToIntegerConverter(I18nProperties.getValidationError(Validations.onlyIntegerNumbersAllowed, posCaption)))
            .bind(CustomizableFieldMetadataDto::getUiLinePosition, CustomizableFieldMetadataDto::setUiLinePosition);

        String weightCaption = caption(CustomizableFieldMetadataDto.UI_LINE_WEIGHT);
        TextField weightField = new TextField(weightCaption);
        weightField.setWidth(100, Unit.PERCENTAGE);
        addComponent(weightField, CustomizableFieldMetadataDto.UI_LINE_WEIGHT);
        binder.forField(weightField)
            .withNullRepresentation("")
            .withConverter(stringToFloatConverter(I18nProperties.getValidationError(Validations.onlyNumbersAllowed, weightCaption)))
            .bind(CustomizableFieldMetadataDto::getUiLineWeight, CustomizableFieldMetadataDto::setUiLineWeight);

        // --- BEHAVIOR ---

        Checkbox activeBox = new Checkbox(caption(CustomizableFieldMetadataDto.ACTIVE));
        addComponent(activeBox, CustomizableFieldMetadataDto.ACTIVE);
        binder.forField(activeBox).bind(d -> d.isActive(), (d, v) -> d.setActive(Boolean.TRUE.equals(v)));

        Checkbox readOnlyBox = new Checkbox(caption(CustomizableFieldMetadataDto.READ_ONLY));
        addComponent(readOnlyBox, CustomizableFieldMetadataDto.READ_ONLY);
        binder.forField(readOnlyBox).bind(d -> d.isReadOnly(), (d, v) -> d.setReadOnly(Boolean.TRUE.equals(v)));

        Checkbox mandatoryBox = new Checkbox(caption(CustomizableFieldMetadataDto.MANDATORY));
        addComponent(mandatoryBox, CustomizableFieldMetadataDto.MANDATORY);
        binder.forField(mandatoryBox).bind(d -> d.isMandatory(), (d, v) -> d.setMandatory(Boolean.TRUE.equals(v)));

        // --- VISIBILITY ---

        Span visibilityHeading = new Span(I18nProperties.getString(Strings.headingCustomizableFieldVisibility));
        visibilityHeading.addClassName(H3);
        addComponent(visibilityHeading, VISIBILITY_HEADING_LOC);

        cbsDiseases = new CheckboxSet<>();
        cbsDiseases.setCaption(
            I18nProperties
                .getPrefixCaption(CustomizableFieldMetadataDto.I18N_PREFIX, CustomizableFieldMetadataDto.VISIBILITY_RESTRICTIONS + "Diseases"));
        cbsDiseases.setItems(FacadeProvider.getDiseaseConfigurationFacade().getAllDiseases(true, true, true), null, null);
        cbsDiseases.setColumnCount(3);
        addComponent(cbsDiseases, DISEASES_LOC);

        // --- TRANSLATIONS ---

        Span translationsHeading = new Span(I18nProperties.getString(Strings.headingCustomizableFieldTranslations));
        translationsHeading.addClassName(H3);
        addComponent(translationsHeading, TRANSLATIONS_HEADING_LOC);

        translationsComponent = new CustomizableFieldTranslationsComponent();
        addComponent(translationsComponent, TRANSLATIONS_LOC);
    }

    private void updateOptionsVisibility(CustomizableFieldType type) {
        boolean visible = OPTIONS_TYPES.contains(type);
        optionsHeading.setVisible(visible);
        optionsComponent.setVisible(visible);

        if (!visible) {
            optionsComponent.setValue(null);
            defaultValueCombo.clear();
        }

        if (type == null) {
            addComponent(defaultValueField, CustomizableFieldMetadataDto.DEFAULT_VALUE);
            return;
        }

        switch (type) {
        case COMBOBOX:
        case CHECKBOX_LIST:
        case RADIO_BUTTON_LIST:
            addComponent(defaultValueCombo, CustomizableFieldMetadataDto.DEFAULT_VALUE);
            break;
        case CHECKBOX:
            addComponent(defaultValueCheckboxCombo, CustomizableFieldMetadataDto.DEFAULT_VALUE);
            break;
        case YES_NO_UNKNOWN:
            addComponent(defaultValueYesNoUnknownCombo, CustomizableFieldMetadataDto.DEFAULT_VALUE);
            break;
        case DATE:
            addComponent(defaultValueDateField, CustomizableFieldMetadataDto.DEFAULT_VALUE);
            break;
        case DATE_TIME:
            addComponent(defaultValueDateTimeField, CustomizableFieldMetadataDto.DEFAULT_VALUE);
            break;
        default:
            addComponent(defaultValueField, CustomizableFieldMetadataDto.DEFAULT_VALUE);
            break;
        }
    }

    @SuppressWarnings("java:S3776")
    public void setValue(CustomizableFieldMetadataDto newDto) {

        this.dto = newDto;

        // Pre-populate group items before readBean so the binder can find the bound value
        CustomizableFieldContext ctx = newDto != null ? newDto.getContextClass() : null;
        if (ctx != null) {
            groupCombo.setItems(CustomizableFieldGroup.getGroupsForContext(ctx));
        } else {
            groupCombo.setItems();
        }

        binder.readBean(newDto);

        Set<Disease> diseases =
            newDto != null && newDto.getVisibilityRestrictions() != null && newDto.getVisibilityRestrictions().getDiseases() != null
                ? new HashSet<>(newDto.getVisibilityRestrictions().getDiseases())
                : null;
        cbsDiseases.setValue(diseases);

        CustomizableFieldType type = newDto != null ? newDto.getFieldType() : null;
        updateOptionsVisibility(type);
        setTypeSpecificDefaultValue(type, newDto != null ? newDto.getDefaultValue() : null);
        if (newDto != null && OPTIONS_TYPES.contains(type)) {
            List<String> options = newDto.getCustomProperties() != null ? newDto.getCustomProperties().getOptions() : null;
            optionsComponent.setValue(options);
            defaultValueCombo.setItems(options != null ? options : Collections.emptyList());
            defaultValueCombo.setValue(options != null && options.contains(newDto.getDefaultValue()) ? newDto.getDefaultValue() : null);
        }

        translationsComponent.setValue(newDto != null ? newDto.getTranslations() : null);
    }

    /**
     * Writes the binder values to the DTO and returns it.
     * Returns {@code null} if binder validation fails (errors are shown inline on the fields).
     */
    @SuppressWarnings("java:S3776")
    public CustomizableFieldMetadataDto getValue() {

        if (dto == null) {
            return null;
        }
        try {
            binder.writeBean(dto);
        } catch (ValidationException e) {
            return null;
        }
        // Options, diseases and translations are outside the binder — write manually
        if (OPTIONS_TYPES.contains(dto.getFieldType())) {
            dto.setDefaultValue(defaultValueCombo.getValue());
            List<String> options = optionsComponent.getValue();
            CustomizableFieldCustomProperties props = dto.getCustomProperties();
            if (options != null && !options.isEmpty()) {
                if (props == null) {
                    props = new CustomizableFieldCustomProperties();
                    dto.setCustomProperties(props);
                }
                props.setOptions(options);
            } else if (props != null) {
                props.setOptions(null);
            }
        } else if (dto.getFieldType() == CustomizableFieldType.CHECKBOX) {
            dto.setDefaultValue(formatCheckboxDefaultValue(defaultValueCheckboxCombo.getValue()));
        } else if (dto.getFieldType() == CustomizableFieldType.YES_NO_UNKNOWN) {
            dto.setDefaultValue(formatYesNoUnknownDefaultValue(defaultValueYesNoUnknownCombo.getValue()));
        } else if (dto.getFieldType() == CustomizableFieldType.DATE) {
            dto.setDefaultValue(formatDateDefaultValue(defaultValueDateField.getValue()));
        } else if (dto.getFieldType() == CustomizableFieldType.DATE_TIME) {
            dto.setDefaultValue(formatDateTimeDefaultValue(defaultValueDateTimeField.getValue()));
        }
        dto.setTranslations(translationsComponent.getValue());

        Set<Disease> selectedDiseases = cbsDiseases.getValue();
        if (selectedDiseases != null && !selectedDiseases.isEmpty()) {
            CustomizableFieldVisibilityRestrictions restrictions = dto.getVisibilityRestrictions();
            if (restrictions == null) {
                restrictions = new CustomizableFieldVisibilityRestrictions();
            }
            restrictions.setDiseases(new ArrayList<>(selectedDiseases));
            dto.setVisibilityRestrictions(restrictions);
        } else {
            if (dto.getVisibilityRestrictions() != null) {
                dto.getVisibilityRestrictions().setDiseases(null);
            }
        }
        return dto;
    }

    /** Returns the binder validation status without writing to the DTO. */
    public BinderValidationStatus<CustomizableFieldMetadataDto> validate() {
        return binder.validate();
    }

    private String caption(String propertyId) {
        return I18nProperties.getPrefixCaption(CustomizableFieldMetadataDto.I18N_PREFIX, propertyId);
    }

    private void setTypeSpecificDefaultValue(CustomizableFieldType type, String defaultValue) {
        if (type == null) {
            defaultValueCheckboxCombo.clear();
            defaultValueYesNoUnknownCombo.clear();
            defaultValueDateField.clear();
            defaultValueDateTimeField.clear();
            return;
        }

        switch (type) {
        case CHECKBOX:
            defaultValueCheckboxCombo.setValue(parseCheckboxDefaultValue(defaultValue));
            break;
        case YES_NO_UNKNOWN:
            defaultValueYesNoUnknownCombo.setValue(parseYesNoUnknownDefaultValue(defaultValue));
            break;
        case DATE:
            defaultValueDateField.setValue(parseDateDefaultValue(defaultValue));
            break;
        case DATE_TIME:
            defaultValueDateTimeField.setValue(parseDateTimeDefaultValue(defaultValue));
            break;
        default:
            defaultValueCheckboxCombo.clear();
            defaultValueYesNoUnknownCombo.clear();
            defaultValueDateField.clear();
            defaultValueDateTimeField.clear();
            break;
        }
    }

    @Nullable
    private static Boolean parseCheckboxDefaultValue(String defaultValue) {
        if (defaultValue == null || defaultValue.trim().isEmpty()) {
            return null;
        }
        if (Boolean.TRUE.toString().equalsIgnoreCase(defaultValue.trim())) {
            return Boolean.TRUE;
        }
        if (Boolean.FALSE.toString().equalsIgnoreCase(defaultValue.trim())) {
            return Boolean.FALSE;
        }
        return null;
    }

    @Nullable
    private static String formatCheckboxDefaultValue(Boolean value) {
        return value != null ? value.toString() : null;
    }

    @Nullable
    private static YesNoUnknown parseYesNoUnknownDefaultValue(String defaultValue) {
        if (defaultValue == null || defaultValue.trim().isEmpty()) {
            return null;
        }
        try {
            return YesNoUnknown.valueOf(defaultValue.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    private static String formatYesNoUnknownDefaultValue(YesNoUnknown value) {
        return value != null ? value.name() : null;
    }

    @Nullable
    private static LocalDate parseDateDefaultValue(String defaultValue) {
        if (defaultValue == null || defaultValue.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(defaultValue.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Nullable
    private static String formatDateDefaultValue(LocalDate value) {
        return value != null ? value.toString() : null;
    }

    @Nullable
    private static LocalDateTime parseDateTimeDefaultValue(String defaultValue) {
        if (defaultValue == null || defaultValue.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(defaultValue.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Nullable
    private static String formatDateTimeDefaultValue(LocalDateTime value) {
        return value != null ? value.withSecond(0).withNano(0).toString() : null;
    }

    private static Converter<String, Float> stringToFloatConverter(String errorMessage) {
        return new Converter<String, Float>() {

            @Override
            public Result<Float> convertToModel(String value, com.vaadin.flow.data.binder.ValueContext context) {
                if (value == null || value.trim().isEmpty()) {
                    return Result.ok(null);
                }
                try {
                    return Result.ok(Float.parseFloat(value.trim()));
                } catch (NumberFormatException e) {
                    return Result.error(errorMessage);
                }
            }

            @Override
            public String convertToPresentation(Float value, com.vaadin.flow.data.binder.ValueContext context) {
                return value == null ? "" : value.toString();
            }
        };
    }

    /**
     * Checks if the given default value is valid for the specified field type.
     */
    private boolean isValidDefaultValueForType(String value, CustomizableFieldType type) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        if (type == CustomizableFieldType.NUMBER) {
            try {
                Integer.parseInt(value.trim());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (type == CustomizableFieldType.DECIMAL) {
            try {
                Float.parseFloat(value.trim());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the appropriate error message for an invalid default value based on the field type.
     */
    private String getDefaultValueErrorMessage(CustomizableFieldType type) {
        if (type == CustomizableFieldType.NUMBER) {
            return I18nProperties.getValidationError(Validations.onlyIntegerNumbersAllowed, caption(CustomizableFieldMetadataDto.DEFAULT_VALUE));
        } else if (type == CustomizableFieldType.DECIMAL) {
            return I18nProperties.getValidationError(Validations.onlyDecimalNumbersAllowed, caption(CustomizableFieldMetadataDto.DEFAULT_VALUE));
        }
        return I18nProperties.getValidationError(Validations.onlyNumbersAllowed, caption(CustomizableFieldMetadataDto.DEFAULT_VALUE));
    }
}
