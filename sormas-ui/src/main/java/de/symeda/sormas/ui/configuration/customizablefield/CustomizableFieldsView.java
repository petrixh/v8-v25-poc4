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

import java.util.Arrays;
import java.util.Objects;

import de.symeda.sormas.ui.navigation.ViewChangeListener;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.customizablefield.CustomizableFieldContext;
import de.symeda.sormas.api.customizablefield.CustomizableFieldMetadataCriteria;
import de.symeda.sormas.api.customizablefield.CustomizableFieldMetadataDto;
import de.symeda.sormas.api.customizablefield.CustomizableFieldType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.utils.criteria.BaseCriteria;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.configuration.AbstractConfigurationView;
import de.symeda.sormas.ui.configuration.infrastructure.components.SearchField;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

/**
 * Admin view for managing customizable field metadata.
 */
@SuppressWarnings({
    "java:S110", // suppress sonar too many parents warning
    "java:S2160" // suppress sonar missing equals
})
public class CustomizableFieldsView extends AbstractConfigurationView {

    public static final String VIEW_NAME = ROOT_VIEW_NAME + "/customizableFields";

    private final CustomizableFieldMetadataCriteria criteria;
    private final CustomizableFieldsGrid grid;

    private SearchField searchField;
    private ComboBox<CustomizableFieldContext> contextFilter;
    private ComboBox<CustomizableFieldType> fieldTypeFilter;
    private ComboBox<String> activeFilter;

    public CustomizableFieldsView() {

        super(VIEW_NAME);

        criteria =
            ViewModelProviders.of(CustomizableFieldsView.class).get(CustomizableFieldMetadataCriteria.class, new CustomizableFieldMetadataCriteria());
        grid = new CustomizableFieldsGrid(criteria);

        final VerticalLayout gridLayout = new VerticalLayout();
        gridLayout.add(createFilterBar());
        gridLayout.add(grid);
        gridLayout.setPadding(true);
        gridLayout.setSpacing(false);
        gridLayout.setFlexGrow(1, grid);
        gridLayout.setSizeFull();
        gridLayout.setClassName("crud-main-layout");

        addComponent(gridLayout);
    }

    private HorizontalLayout createFilterBar() {

        final HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setPadding(false);
        filterLayout.setSpacing(true);

        searchField = new SearchField();
        searchField.setInputPrompt(I18nProperties.getString(Strings.promptCustomizableFieldSearchField));
        searchField.addTextChangeListener(e -> {
            criteria.freeTextFilter(e.getText());
            grid.reload();
        });
        filterLayout.add(searchField);

        contextFilter = new ComboBox<>(
            I18nProperties.getPrefixCaption(CustomizableFieldMetadataDto.I18N_PREFIX, CustomizableFieldMetadataDto.CONTEXT_CLASS),
            Arrays.asList(CustomizableFieldContext.values()));
        contextFilter.setItemLabelGenerator(Objects::toString);
        contextFilter.addValueChangeListener(e -> {
            criteria.setContextClass(e.getValue());
            grid.reload();
        });
        filterLayout.add(contextFilter);

        fieldTypeFilter = new ComboBox<>(
            I18nProperties.getPrefixCaption(CustomizableFieldMetadataDto.I18N_PREFIX, CustomizableFieldMetadataDto.FIELD_TYPE),
            Arrays.asList(CustomizableFieldType.values()));
        fieldTypeFilter.setItemLabelGenerator(Objects::toString);
        fieldTypeFilter.addValueChangeListener(e -> {
            criteria.setFieldType(e.getValue());
            grid.reload();
        });
        filterLayout.add(fieldTypeFilter);

        activeFilter = new ComboBox<>(I18nProperties.getPrefixCaption(CustomizableFieldMetadataDto.I18N_PREFIX, CustomizableFieldMetadataDto.ACTIVE));
        activeFilter.setItems(
            I18nProperties.getCaption(Captions.customizableFieldsAllActive),
            I18nProperties.getCaption(Captions.customizableFieldsActiveOnly),
            I18nProperties.getCaption(Captions.customizableFieldsInactiveOnly));
        activeFilter.addValueChangeListener(e -> {
            String val = e.getValue();
            if (val == null || val.equals(I18nProperties.getCaption(Captions.customizableFieldsAllActive))) {
                criteria.setActive(null);
            } else if (val.equals(I18nProperties.getCaption(Captions.customizableFieldsActiveOnly))) {
                criteria.setActive(Boolean.TRUE);
            } else {
                criteria.setActive(Boolean.FALSE);
            }
            grid.reload();
        });
        filterLayout.add(activeFilter);

        filterLayout.add(ButtonHelper.createButton(Captions.actionResetFilters, event -> {
            ViewModelProviders.of(CustomizableFieldsView.class).remove(CustomizableFieldMetadataCriteria.class);
            navigateTo((BaseCriteria[]) null);
        }, CssStyles.FORCE_CAPTION));

        filterLayout.add(
            ButtonHelper.createButton(
                Captions.actionCreate,
                event -> ControllerProvider.getCustomizableFieldsController().createField(),
                CssStyles.FORCE_CAPTION));

        return filterLayout;
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {

        super.enter(event);
        String params = event.getParameters().trim();
        if (params.startsWith("?")) {
            params = params.substring(1);
            criteria.fromUrlParams(params);
        }
        updateFilterComponents();
        grid.reload();
    }

    public void updateFilterComponents() {

        applyingCriteria = true;
        searchField.setValue(criteria.getFreeTextFilter());
        contextFilter.setValue(criteria.getContextClass());
        fieldTypeFilter.setValue(criteria.getFieldType());
        Boolean active = criteria.getActive();
        if (active == null) {
            activeFilter.setValue(I18nProperties.getCaption(Captions.customizableFieldsAllActive));
        } else if (Boolean.TRUE.equals(active)) {
            activeFilter.setValue(I18nProperties.getCaption(Captions.customizableFieldsActiveOnly));
        } else {
            activeFilter.setValue(I18nProperties.getCaption(Captions.customizableFieldsInactiveOnly));
        }
        applyingCriteria = false;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
