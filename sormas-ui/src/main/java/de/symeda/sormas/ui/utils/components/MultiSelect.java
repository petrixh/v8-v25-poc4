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

package de.symeda.sormas.ui.utils.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.fields.CustomField;

public class MultiSelect<T> extends CustomField<Set<T>> {

    /** Was {@code BUTTON_ICON_ALIGN_TOP}. */
    private static final String BUTTON_ICON_ALIGN_TOP = "icon-align-top";
    private ComboBox selectComponent = new ComboBox();
    private Collection<T> items = new ArrayList<>();
    private VerticalLayout labelLayout = new VerticalLayout();
    private Map<T, String> selectedItemsWithCaption = new HashMap<>();
    private Map<T, Span> selectedItemsWithLabelComponent = new HashMap<>();

    public static <T> MultiSelect<T> create(Class<T> clazz) {
        return new MultiSelect<>();
    }

    @Override
    protected Component initContent() {
        VerticalLayout verticalLayout = new VerticalLayout();
        // was MarginInfo(false, false, true, false): bottom margin only (look, parked)
        verticalLayout.setPadding(false);

        selectComponent.setWidth("100%");

        selectComponent.addValueChangeListener(event -> {
            T item = (T) selectComponent.getValue();
            if (item == null) {
                return;
            }

            String caption = selectComponent.getItemCaption(item);
            selectedItemsWithCaption.put(item, caption);
            setValue(new HashSet<>(selectedItemsWithCaption.keySet()));

            selectComponent.setValue(null);
            updateItemsOnSelectionChange();

            listSelectedItems();
        });

        verticalLayout.add(selectComponent);
        verticalLayout.setSpacing(false);

        if (isReadOnly()) {
            selectComponent.setVisible(false);
            selectComponent.setReadOnly(true);
        }

        labelLayout.setPadding(false);
        labelLayout.setSpacing(false);
        verticalLayout.add(labelLayout);

        initFromCurrentValue();

        return verticalLayout;
    }

    @Override
    public Class<? extends Set<T>> getType() {
        return (Class) Set.class;
    }

    public void setItems(Collection<T> items) {
        this.items = items;

        selectedItemsWithCaption.clear();
        selectComponent.removeAllItems();

        selectComponent.addItems(items);
        listSelectedItems();
    }

    public void addItem(T item) {
        items.add(item);
        selectComponent.addItem(item);
    }

    public void setItemCaption(T item, String caption) {
        selectComponent.setItemCaption(item, caption);

        if (selectedItemsWithCaption.containsKey(item)) {
            selectedItemsWithCaption.put(item, caption);
        }
        if (selectedItemsWithLabelComponent.get(item) != null) {
            selectedItemsWithLabelComponent.get(item).setText(caption);
        }
    }

    public void removeAllItems() {
        selectComponent.removeAllItems();
        selectedItemsWithCaption.clear();

        listSelectedItems();
    }

    private void initFromCurrentValue() {
        if (getValue() == null) {
            return;
        }

        for (T item : getValue()) {
            String caption = selectComponent.getItemCaption(item);
            selectedItemsWithCaption.put(item, caption != null ? caption : "");
        }

        listSelectedItems();
    }

    private void listSelectedItems() {
        selectedItemsWithLabelComponent.clear();
        labelLayout.removeAll();

        for (Map.Entry<T, String> itemEntry : new HashMap<>(selectedItemsWithCaption).entrySet()) {
            HorizontalLayout itemLayout = new HorizontalLayout();
            itemLayout.setPadding(false);
            itemLayout.setWidth("100%");

            Span label = new Span(itemEntry.getValue());
            label.setWidth("100%");
            itemLayout.add(label);

            if (!isReadOnly()) {
                Button removeButton = ButtonHelper.createIconButtonWithCaption(
                        null,
                        null,
                        VaadinIcon.TRASH,
                        e -> removeSelectedItem(itemEntry.getKey()),
                        CssStyles.BUTTON_ICON_ONLY,
                        CssStyles.BUTTON_BORDERLESS,
                        BUTTON_ICON_ALIGN_TOP);
                itemLayout.add(removeButton);
            }

            labelLayout.add(itemLayout);

            selectedItemsWithLabelComponent.put(itemEntry.getKey(), label);
        }
    }

    private void removeSelectedItem(T item) {
        selectedItemsWithCaption.remove(item);
        setValue(new HashSet<>(selectedItemsWithCaption.keySet()));
        updateItemsOnSelectionChange();

        listSelectedItems();
    }

    private void updateItemsOnSelectionChange() {
        selectComponent.removeAllItems();
        selectComponent.addItems(items.stream().filter(item -> !selectedItemsWithCaption.containsKey(item)).collect(Collectors.toList()));
    }
}
