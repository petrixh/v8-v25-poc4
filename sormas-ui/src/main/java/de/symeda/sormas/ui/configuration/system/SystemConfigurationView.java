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

import de.symeda.sormas.ui.navigation.ViewChangeListener;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.systemconfiguration.SystemConfigurationCategoryDto;
import de.symeda.sormas.api.systemconfiguration.SystemConfigurationValueCriteria;
import de.symeda.sormas.api.systemconfiguration.SystemConfigurationValueDto;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.configuration.AbstractConfigurationView;
import de.symeda.sormas.ui.configuration.infrastructure.components.SearchField;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

/**
 * View for managing system configuration values.
 */
public class SystemConfigurationView extends AbstractConfigurationView {

    public static final String VIEW_NAME = ROOT_VIEW_NAME + "/systemConfiguration";

    private final SystemConfigurationValueCriteria criteria;

    private SearchField searchField;
    private final SystemConfigurationValuesGrid grid;

    /**
     * Constructor for SystemConfigurationView.
     */
    public SystemConfigurationView() {

        super(VIEW_NAME);

        criteria =
            ViewModelProviders.of(SystemConfigurationView.class).get(SystemConfigurationValueCriteria.class, new SystemConfigurationValueCriteria());
        grid = new SystemConfigurationValuesGrid(criteria);
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

    /**
     * Creates the filter bar layout.
     *
     * @return the filter bar layout
     */
    private HorizontalLayout createFilterBar() {

        final ComboBox<SystemConfigurationCategoryDto> categoryFilter;

        final HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setPadding(false);
        filterLayout.setSpacing(true);

        searchField = new SearchField();
        searchField.setInputPrompt(I18nProperties.getString(Strings.promptSystemConfigurationSearchField));
        searchField.addValueChangeListener(e -> {
            // was a V7 text change listener: user input only (SearchField is lazy, 1a-58)
            if (!e.isFromClient()) {
                return;
            }
            criteria.freeTextFilter(e.getValue());
            grid.reload();
        });
        filterLayout.add(searchField);

        categoryFilter = new ComboBox<>(
            I18nProperties.getPrefixCaption(SystemConfigurationValueDto.I18N_PREFIX, SystemConfigurationValueDto.CATEGORY_PROPERTY_NAME),
            FacadeProvider.getSystemConfigurationCategoryFacade().getAllAfter(null));

        categoryFilter.setItemLabelGenerator(v -> {
            final StringBuilder caption = new StringBuilder();
            SystemConfigurationI18nHelper.processI18nString(
                v.getCaption(),
                (defaultName, key) -> caption.append(I18nProperties.getPrefixCaption(SystemConfigurationValueDto.I18N_PREFIX, key, defaultName)));
            if (caption.length() == 0) {
                caption.append(v.getCaption());
            }
            return caption.toString();
        });

        categoryFilter.addValueChangeListener(e -> {
            final SystemConfigurationCategoryDto category = e.getValue();
            if (category == null) {
                criteria.setCategory(null);
                grid.reload();
                return;
            }
            criteria.setCategory(FacadeProvider.getSystemConfigurationCategoryFacade().getReferenceByUuid(e.getValue().getUuid()));
            grid.reload();
        });
        filterLayout.add(categoryFilter);

        filterLayout.add(ButtonHelper.createButton(Captions.actionResetFilters, event -> {
            ViewModelProviders.of(SystemConfigurationView.class).remove(SystemConfigurationValueCriteria.class);
            navigateTo(null);
        }, CssStyles.FORCE_CAPTION));

        return filterLayout;
    }

    /**
     * Called when the view is entered.
     *
     * @param event
     *            the view change event
     */
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

    /**
     * Updates the filter components based on the criteria.
     */
    public void updateFilterComponents() {

        applyingCriteria = true;
        searchField.setValue(criteria.getFreeTextFilter());
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
