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

package de.symeda.sormas.ui.configuration.system;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import de.symeda.sormas.api.systemconfiguration.SystemConfigurationValueDataProvider;
import de.symeda.sormas.api.systemconfiguration.SystemConfigurationValueDto;

/**
 * Custom field for dynamic input of system configuration values.
 * The component will render a text field, drop down, password field, or checkbox grid depending on the
 * {@link SystemConfigurationValueDataProvider} if any is available.
 * The default is to render a text field.
 */
public class SystemConfigurationValueDynamicInput extends CustomField<SystemConfigurationValueDto> {

    /**
     * Enum representing the types of input values.
     */
    public enum ValueTypes {
        TEXT,
        PASSWORD,
        DROPDOWN,
        CHECKBOX_GRID
    }
    /**
     * Interface for listening to value change events.
     */
    public interface ValueChangeListener {

        /**
         * Called when the value changes.
         *
         * @param event
         *            the value change event
         */
        void valueChange(ValueChangeEvent event);
    }
    /**
     * Event representing a change in value.
     */
    public static class ValueChangeEvent extends ComponentEvent<SystemConfigurationValueDynamicInput> {

        /**
         * Constructs a new ValueChangeEvent.
         *
         * @param source
         *            the source component
         */
        public ValueChangeEvent(final SystemConfigurationValueDynamicInput source) {
            super(source, false);
        }
    }

    private static final String STYLE_NAME_DYNAMIC_INPUT = "system-configuration-dynamic-input";

    private static final String STYLE_NAME_DYNAMIC_INPUT_FIXED_SIZE = "system-configuration-dynamic-input-fixed-size";

    private static final String STYLE_NAME_TOGGLE_PASSWORD = "toggle-password";

    private static final String STYLE_NAME_WRAPPING_CHECKBOX_GROUP = "wrapping-checkbox-group";

    private static final long serialVersionUID = 1L;

    private SystemConfigurationValueDto value;

    private boolean contentInitialized;

    @Override
    public SystemConfigurationValueDto getValue() {
        return value;
    }

    /**
     * Was {@code doSetValue}: the value is the DTO itself, edited in place by the inputs.
     */
    @Override
    protected void setPresentationValue(final SystemConfigurationValueDto newFieldValue) {

        Objects.requireNonNull(newFieldValue);
        this.value = newFieldValue;
    }

    @Override
    protected SystemConfigurationValueDto generateModelValue() {
        return value;
    }

    /**
     * Vaadin 8 built the content ({@link #initContent()}) when the field was first attached, after its value was set.
     */
    @Override
    protected void onAttach(final AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!contentInitialized) {
            contentInitialized = true;
            add(initContent());
        }
    }

    /**
     * Adds a value change listener.
     *
     * @param listener
     *            the listener to add
     */
    public void addValueChangeListener(final ValueChangeListener listener) {
        addListener(ValueChangeEvent.class, listener::valueChange);
    }

    /**
     * Determines the type of value input.
     *
     * @return the value type
     */
    public ValueTypes getValueType() {

        final var dataProvider = getValue().getDataProvider();

        if (null != dataProvider) {

            final var options = dataProvider.getOptions().values();
            final var keys = dataProvider.getKeys();

            if (keys.size() > 1) {
                return ValueTypes.CHECKBOX_GRID;
            }

            if (keys.size() == 1 && options.size() > 1) {
                return ValueTypes.DROPDOWN;
            }
        }

        if (Boolean.TRUE.equals(getValue().getEncrypt())) {
            return ValueTypes.PASSWORD;
        }

        return ValueTypes.TEXT;
    }

    @Override
    public boolean equals(final Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    protected Component initContent() {

        addClassName(STYLE_NAME_DYNAMIC_INPUT);

        final var mainLayout = new VerticalLayout();

        mainLayout.setSpacing(true);
        mainLayout.setPadding(false);
        mainLayout.setSizeUndefined();
        mainLayout.setWidthFull();

        switch (getValueType()) {
        case DROPDOWN:
            mainLayout.add(createDropdown());
            break;
        case CHECKBOX_GRID:
            mainLayout.add(createCheckboxGrid());
            mainLayout.addClassName(STYLE_NAME_DYNAMIC_INPUT_FIXED_SIZE);
            break;
        case PASSWORD:
            mainLayout.add(createPasswordField());
            break;
        default:
            mainLayout.add(createTextField());
        }

        return mainLayout;
    }

    /**
     * Creates a text field for input.
     *
     * @return the text field
     */
    private TextField createTextField() {

        final var field = new TextField();
        field.setValue(getValue().getValue());
        field.addValueChangeListener(event -> fireValueChange(field));
        field.setWidthFull();
        return field;
    }

    /**
     * Creates a password field for input.
     *
     * @return the password field
     */
    private Component createPasswordField() {

        final var layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.addClassName(STYLE_NAME_TOGGLE_PASSWORD);
        final var passwordField = new PasswordField();
        // the own toggle button below shows the value, as in Vaadin 8; Flow's reveal button would be a second one
        passwordField.setRevealButtonVisible(false);
        passwordField.setValue(getValue().getValue());
        passwordField.setWidthFull();

        final var textField = new TextField();
        textField.setValue(getValue().getValue());
        textField.setWidthFull();
        textField.setVisible(false);
        passwordField.setValue(getValue().getValue());

        final var toggleButton = new Button();
        toggleButton.setIcon(VaadinIcon.EYE_SLASH.create());

        passwordField.addValueChangeListener(event -> {
            textField.setValue(passwordField.getValue());
            fireValueChange(passwordField);
        });
        textField.addValueChangeListener(event -> {
            passwordField.setValue(event.getValue());
            fireValueChange(passwordField);
        });

        toggleButton.addClickListener(event -> {
            if (passwordField.isVisible()) {
                passwordField.setVisible(false);
                textField.setVisible(true);
                textField.setValue(passwordField.getValue());
                toggleButton.setIcon(VaadinIcon.EYE.create());
            } else {
                passwordField.setVisible(true);
                textField.setVisible(false);
                passwordField.setValue(textField.getValue());
                toggleButton.setIcon(VaadinIcon.EYE_SLASH.create());
            }
        });

        layout.add(passwordField, textField, toggleButton);
        return layout;
    }

    /**
     * Creates a dropdown for input.
     *
     * @return the dropdown
     */
    private ComboBox<String> createDropdown() {

        final var combo = new ComboBox<String>();
        combo.setItems(getValue().getDataProvider().getOptions().values());
        combo.setValue(getValue().getValue());
        combo.addValueChangeListener(event -> {
            // was setEmptySelectionAllowed(false): a value the user clears is restored
            if (event.isFromClient() && event.getValue() == null && event.getOldValue() != null) {
                combo.setValue(event.getOldValue());
                return;
            }
            fireValueChange(combo);
        });
        combo.setWidthFull();
        return combo;
    }

    /**
     * Creates a checkbox grid for input.
     *
     * @return the checkbox grid
     */
    private CheckboxGroup<String> createCheckboxGrid() {

        final var cbg = new CheckboxGroup<String>();
        ArrayList<String> values = new ArrayList<>(getValue().getDataProvider().getOptions().values());
        values.sort(Comparator.naturalOrder());
        cbg.setItems(values);
        cbg.select(getValue().getDataProvider().getMappedValues(getValue()).values().toArray(new String[] {}));
        cbg.addValueChangeListener(event -> fireValueChange(cbg));
        cbg.addClassName(STYLE_NAME_WRAPPING_CHECKBOX_GROUP);
        cbg.setWidthFull();
        return cbg;
    }

    /**
     * Fires a value change event.
     *
     * @param source
     *            the source component
     */
    @SuppressWarnings("unchecked")
    private void fireValueChange(final Component source) {

        if (source instanceof TextField) {
            getValue().setValue(((TextField) source).getValue());
        }

        if (source instanceof PasswordField) {
            getValue().setValue(((PasswordField) source).getValue());
        }

        if (source instanceof ComboBox) {
            getValue().getDataProvider().getKeys().stream().findFirst().ifPresent(k -> {
                final HashMap<String, String> values = new HashMap<>(1);
                values.put(k, ((ComboBox<String>) source).getValue());
                getValue().getDataProvider().applyValues(values, getValue());
            });
        }

        if (source instanceof CheckboxGroup) {
            final HashMap<String, String> selectedValues = new HashMap<>(getValue().getDataProvider().getKeys().size());
            ((CheckboxGroup<String>) source).getSelectedItems()
                .forEach(
                    v -> getValue().getDataProvider()
                        .getOptions()
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue().equals(v))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .ifPresent(k -> selectedValues.put(k, v)));
            getValue().getDataProvider().applyValues(selectedValues, getValue());
        }

        fireEvent(new ValueChangeEvent(this));
    }
}
