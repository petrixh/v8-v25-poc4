package de.symeda.sormas.ui.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.ui.utils.fields.CheckBox;
import de.symeda.sormas.ui.utils.fields.CustomField;

public class CheckBoxTree<ENUM extends Enum<?>> extends CustomField<Map<ENUM, Boolean>> {

	private static final String[] INDENTATION_STYLES = new String[] {
		CssStyles.INDENT_LEFT_1,
		CssStyles.INDENT_LEFT_2,
		CssStyles.INDENT_LEFT_3 };

	private Map<ENUM, CheckBox> enumToggles = new HashMap<>();
	private VerticalLayout checkBoxLayout;
	private HorizontalLayout groupLayout;
	private Class<ENUM> enumType;
	private boolean addVerticalSpaces;
	private UnaryOperator<ENUM> parentProvider;
	private Function<ENUM, ? extends Enum> groupProvider;
	private Class<? extends Enum> parentGroup;
	private boolean settingToggles = false;
	private int columns;

	public CheckBoxTree() {

	}

	@Override
	protected Component initContent() {

		if (enumType != null) {
			if (groupProvider == null) {
				checkBoxLayout = new VerticalLayout();
				checkBoxLayout.setMargin(false);
				checkBoxLayout.setSpacing(false);
				checkBoxLayout.setWidthFull();
				addCheckBoxes();
				// V8 bottom margin (MarginInfo) not carried over: no CSS in stage 1a
			} else {
				groupLayout = new HorizontalLayout();
				groupLayout.setMargin(false);
				groupLayout.setWidthFull();
				addGroups();
			}

			setToggleValues(getValue());
		}

		addValueChangeListener(valueChangeEvent -> {
			Map<ENUM, Boolean> value = valueChangeEvent.getValue();
			final Map<ENUM, Boolean> nullSafeValue = value == null ? new HashMap<>() : value;
			settingToggles = true;
			enumToggles.forEach(((anEnum, checkBox) -> {
				final Boolean orDefault = nullSafeValue.getOrDefault(anEnum, false);
				if (!Objects.equals(checkBox.getValue(), orDefault)) {
					checkBox.setValue(orDefault);
				}
			}));
			settingToggles = false;
		});

		if (checkBoxLayout != null) {
			return checkBoxLayout;
		}

		return groupLayout;
	}

	private void addGroups() {
		Map<Enum<?>, List<ENUM>> enumGroupMap =
			Arrays.stream(enumType.getEnumConstants()).collect(Collectors.groupingBy(anEnum -> groupProvider.apply(anEnum)));

		List<VerticalLayout> columnList = new ArrayList<>();

		for (int i = 0; i < columns; i++) {
			final VerticalLayout columnLayout = new VerticalLayout();
			columnLayout.setMargin(false);
			columnLayout.setSpacing(false);
			columnList.add(columnLayout);
			groupLayout.add(columnLayout);
		}

		final Enum<?>[] enumConstants = parentGroup.getEnumConstants();
		for (int i = 0; i < parentGroup.getEnumConstants().length; i++) {
			final Enum<?> enumListEntry = enumConstants[i];
			Span heading = new Span(enumListEntry.toString());
			heading.setWidthFull();
			heading.addClassName(CssStyles.H4);
			final VerticalLayout columnLayout = columnList.get(i % columns);
			columnLayout.add(heading);

			enumGroupMap.get(enumListEntry).forEach(anEnum -> {
				CheckBox checkbox = createCheckbox(0, anEnum, null);
				enumToggles.put(anEnum, checkbox);
				columnLayout.add(checkbox);
			});
		}

		// V8 bottom margin (MarginInfo) not carried over: no CSS in stage 1a

		setToggleValues(getValue());
	}

	private void addCheckBoxes() {

		final ENUM[] enumElements = enumType.getEnumConstants();

		for (ENUM enumElement : enumElements) {

			ENUM parentElement = null;
			if (parentProvider != null) {
				parentElement = parentProvider.apply(enumElement);
			}

			int level = getEnumElementLevel(enumElement, 0);
			CheckBox elementCheckbox = createCheckbox(level, enumElement, parentElement);

			enumToggles.put(enumElement, elementCheckbox);
			checkBoxLayout.add(elementCheckbox);
		}
	}

	private int getEnumElementLevel(ENUM enumElement, int level) {
		ENUM parentElement = null;
		if (parentProvider != null) {
			parentElement = parentProvider.apply(enumElement);
		}
		if (parentElement != null) {
			return getEnumElementLevel(parentElement, level + 1);
		} else {
			return level;
		}
	}

	@NotNull
	private CheckBox createCheckbox(int level, ENUM element, ENUM parentElement) {
		CheckBox elementCheckbox = new CheckBox(element.toString());
		final int safeLevel = Math.min(level - 1, INDENTATION_STYLES.length - 1);
		if (level > 0) {
			elementCheckbox.addStyleName(INDENTATION_STYLES[safeLevel]);
		}
		if (addVerticalSpaces) {
			elementCheckbox.addStyleName(CssStyles.VSPACE_4);
		}

		elementCheckbox.setWidthFull();
		elementCheckbox.addValueChangeListener(e -> {
			if (settingToggles){
				return;
			}
			final Boolean value = e.getValue();
			Map<ENUM, Boolean> newValue = getValue() != null ? new HashMap<>(getValue()) : new HashMap<>();
			newValue.put(element, value);
			setValue(newValue);
		});

		if (parentElement != null) {
			final CheckBox parentCheckBox = enumToggles.get(parentElement);
			elementCheckbox.setVisible(parentCheckBox.getValue() != null && parentCheckBox.getValue());
			parentCheckBox.addValueChangeListener(parentChangeEvent -> {
				if (!settingToggles) {
					elementCheckbox.setValue(false);
				}

				elementCheckbox.setVisible(parentChangeEvent.getValue());
			});
		}
		return elementCheckbox;
	}

	public void setEnumType(Class<ENUM> enumType, UnaryOperator<ENUM> parentProvider) {
		this.enumType = enumType;
		this.parentProvider = parentProvider;
	}

	public void setEnumType(Class<ENUM> enumType, Function<ENUM, ? extends Enum<?>> groupProvider, Class<? extends Enum> parentGroup, int columns) {
		this.enumType = enumType;
		this.groupProvider = groupProvider;
		this.parentGroup = parentGroup;
		this.columns = columns;
	}

	@Override
	public void setValue(Map<ENUM, Boolean> newFieldValue) {
		super.setValue(newFieldValue);

		if (!this.enumToggles.isEmpty()) {
			setToggleValues(newFieldValue);
		}
	}

	private void setToggleValues(Map<ENUM, Boolean> newFieldValue) {
		settingToggles = true;
		enumToggles.forEach((anEnum, checkBox) -> {
			checkBox.setValue(newFieldValue != null && (newFieldValue.get(anEnum) != null && newFieldValue.get(anEnum)));
		});
		settingToggles = false;
	}

	@Override
	public Class<? extends Map<ENUM, Boolean>> getType() {
		return (Class<? extends Map<ENUM, Boolean>>) new HashMap<>().getClass();
	}
}
