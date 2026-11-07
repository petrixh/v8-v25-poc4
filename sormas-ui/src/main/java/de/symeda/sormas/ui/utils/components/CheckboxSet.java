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

package de.symeda.sormas.ui.utils.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.fields.CheckBox;
import de.symeda.sormas.ui.utils.fields.CustomField;

public class CheckboxSet<T> extends CustomField<Set<T>> {

	private static final long serialVersionUID = 4425620663378339988L;

	/**
	 * Key of the item a check box stands for (was {@code CheckBox.setData/getData}).
	 */
	private static final String CHECKBOX_DATA = "checkboxSetItem";

	private VerticalLayout layout;
	private List<CheckboxRow> rows = new ArrayList<>(0);

	private List<T> items;
	private Function<T, String> groupingFunction;
	private Function<T, String> itemDescriptionProvider;

	private int columnCount = 2;
	private boolean updatingCheckboxes;

	@Override
	protected Component initContent() {
		layout = new VerticalLayout();
		layout.setWidthFull();
		// was MarginInfo(true, false): vertical margin only; Flow padding is all sides or none (look, parked)
		layout.setPadding(false);

		if (items != null) {
			resetLayout();
		}

		return layout;
	}

	public void setItems(List<T> items, Function<T, String> groupingFunction, Function<T, String> itemDescriptionProvider) {
		this.items = items;
		this.groupingFunction = groupingFunction;
		this.itemDescriptionProvider = itemDescriptionProvider;

		resetLayout();
	}

	public void setColumnCount(int columnCount) {
		this.columnCount = columnCount;

		resetLayout();
	}

	private void resetLayout() {
		if (layout == null) {
			return;
		}

		layout.removeAll();

		rows = buildCheckboxRows(items);
	}

	private List<CheckboxRow> buildCheckboxRows(List<T> items) {
		List<String> groups;
		if (groupingFunction != null) {
			groups = items.stream().map(i -> groupingFunction.apply(i)).distinct().collect(Collectors.toList());
		} else {
			groups = Collections.singletonList(null);
		}

		return groups.stream().map((group) -> {
			List<CheckboxRow> groupRows;
			if (group != null) {
				groupRows = createRows(items.stream().filter(i -> group.equals(groupingFunction.apply(i))).collect(Collectors.toList()));
				layout.add(createGroupHeader(group, groupRows));
			} else {
				groupRows = createRows(items);
			}

			layout.add(groupRows.toArray(new Component[] {}));

			return groupRows;
		}).flatMap(List::stream).collect(Collectors.toList());
	}

	private List<CheckboxRow> createRows(List<T> groupItems) {
		List<CheckboxRow> rows = new ArrayList<>();
		for (int i = 0, size = groupItems.size(); i < size; i += columnCount) {
			List<T> rowItems = groupItems.subList(i, Math.min(i + columnCount, groupItems.size()));

			CheckboxRow checkboxRow = new CheckboxRow(rowItems, columnCount);

			checkboxRow.checkBoxes.forEach(cb -> cb.addValueChangeListener(e -> {
				fireEvent(new CheckboxValueChangeEvent(cb));
			}));

			rows.add(checkboxRow);
		}

		return rows;
	}

	private HorizontalLayout createGroupHeader(String group, List<CheckboxRow> rows) {
		Span groupLabel = new Span(group);
		groupLabel.addClassName(CssStyles.H3);

		Span tickForAllLAbel = new Span(I18nProperties.getString(Strings.checkboxSetTickAnAnswerForAll));
		tickForAllLAbel.addClassName(CssStyles.LABEL_ITALIC);

		Button yesAllBurron = ButtonHelper.createButton(
			I18nProperties.getCaption(Captions.actionYesAll),
			(e) -> rows.forEach(r -> r.setValueForAll(true)),
			CssStyles.BUTTON_LINK,
			CssStyles.BUTTON_COMPACT);
		Button noAllButton = ButtonHelper.createButton(
			I18nProperties.getCaption(Captions.actionNoAll),
			(e) -> rows.forEach(r -> r.setValueForAll(false)),
			CssStyles.BUTTON_LINK,
			CssStyles.BUTTON_COMPACT);

		HorizontalLayout buttonsLayout = new HorizontalLayout(tickForAllLAbel, yesAllBurron, noAllButton);
		buttonsLayout.setWidthFull();
		buttonsLayout.setFlexGrow(1, tickForAllLAbel);

		HorizontalLayout headerLayout = new HorizontalLayout(groupLabel, buttonsLayout);
		headerLayout.setWidthFull();

		headerLayout.setFlexGrow(0.5, groupLabel);
		headerLayout.setFlexGrow(0.5, buttonsLayout);
		// Vaadin 8 expand ratios share out the whole width, not what the content leaves: halves, as before
		groupLabel.getStyle().set("flex-basis", "0");
		buttonsLayout.getStyle().set("flex-basis", "0");
		headerLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, buttonsLayout);

		return headerLayout;
	}

	private CheckBox createCheckbox(T item) {
		CheckBox checkBox = new CheckBox(item.toString());
		createDataSource(item).bind(checkBox);
		ComponentUtil.setData(checkBox, CHECKBOX_DATA, item);
		if (itemDescriptionProvider != null) {
			checkBox.setDescription(itemDescriptionProvider.apply(item));
		}
		// the data source's write sets the value of this field, which fires its value change (was fireValueChange)

		return checkBox;
	}

	private CheckboxSetItemDataSource<T> createDataSource(T item) {
		return new CheckboxSetItemDataSource<>(item, v -> getSafeInternalValue().contains(v), (c, v) -> {
			if (updatingCheckboxes) {
				return;
			}
			Set<T> internalValue = getSafeInternalValue();
			if (c) {
				internalValue.add(v);
			} else {
				internalValue.remove(v);
			}

			setValue(internalValue);
		});
	}

	@Override
	public Class<? extends Set<T>> getType() {
		//noinspection unchecked,InstantiatingObjectToGetClassObject,InstantiatingObjectToGetClassObject
		return (Class<? extends Set<T>>) new HashSet<T>(0).getClass();
	}

	/**
	 * Keeps a copy of the given set, as V7 {@code setInternalValue} did.
	 */
	@Override
	public void setValue(Set<T> newValue) {
		super.setValue(newValue != null ? new HashSet<>(newValue) : null);
	}

	@Override
	protected void setPresentationValue(Set<T> newValue) {
		if (newValue != null) {
			updatingCheckboxes = true;
			try {
				rows.forEach(r -> r.checkBoxes.forEach(c -> c.setValue(newValue.contains(getCheckboxData(c)))));
			} finally {
				updatingCheckboxes = false;
			}
		}
	}

	protected Set<T> getSafeInternalValue() {
		Set<T> internalValue = getValue();
		return internalValue != null ? new HashSet<>(internalValue) : new HashSet<>();
	}

	public Registration addCheckboxValueChangeListener(CheckboxValueChangeListener listener) {
		return addListener(CheckboxValueChangeEvent.class, listener);
	}

	public Optional<CheckBox> getCheckboxByData(T data) {
		return rows.stream().map(r -> r.checkBoxes).flatMap(Collection::stream).filter(checkBox -> data.equals(getCheckboxData(checkBox))).findFirst();
	}

	/**
	 * Was {@code checkBox.getData()} for the check boxes of a {@link CheckboxSet}.
	 */
	public static Object getCheckboxData(CheckBox checkBox) {
		return ComponentUtil.getData(checkBox, CHECKBOX_DATA);
	}

	@FunctionalInterface
	public interface CheckboxValueChangeListener extends ComponentEventListener<CheckboxValueChangeEvent> {

		void checkboxValueChange(CheckboxValueChangeEvent event);

		@Override
		default void onComponentEvent(CheckboxValueChangeEvent event) {
			checkboxValueChange(event);
		}
	}

	public static final class CheckboxValueChangeEvent extends ComponentEvent<CheckBox> {

		private static final long serialVersionUID = 7689979222604280185L;

		public CheckboxValueChangeEvent(CheckBox source) {
			super(source, false);
		}

		public CheckBox getCheckbox() {
			return getSource();
		}
	}

	private final class CheckboxRow extends HorizontalLayout {

		private static final long serialVersionUID = -3874252190392021250L;

		private final List<CheckBox> checkBoxes;

		public CheckboxRow(List<T> items, int columnCount) {
			int itemCount = items.size();
			checkBoxes = new ArrayList<>(itemCount);

			setWidthFull();
			setPadding(false);

			items.forEach(i -> {
				CheckBox cb = createCheckbox(i);
				add(cb);
				checkBoxes.add(cb);
			});

			if (columnCount > itemCount) {
				// add empty columns to fill the row
				add(new HorizontalLayout());
			}

			// Vaadin 8: a full-width layout without expand ratios gives every slot an equal share, so the check boxes
			// line up in columns
			getChildren().forEach(c -> c.getElement().getStyle().set("flex", "1 1 0"));
		}

		public void setValueForAll(boolean checked) {
			checkBoxes.forEach(cb -> cb.setValue(checked));
		}
	}
}
