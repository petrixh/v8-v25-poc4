package de.symeda.sormas.ui.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.BindingValidationStatus;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.sample.PathogenTestCategory;
import de.symeda.sormas.api.sample.PathogenTestType;
import de.symeda.sormas.api.utils.ApplicableToPathogenTests;
import de.symeda.sormas.api.utils.Diseases;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.utils.fields.DatePickerLocale;
import de.symeda.sormas.ui.utils.fields.KeepValueComboBox;

/**
 * Base class for composable form components that participate in the parent form lifecycle.
 * Extends {@link VerticalLayout} so that {@link #onDetach(DetachEvent)} is called automatically
 * when the parent form is removed — ensuring binder unbinding and listener cleanup.
 * <p>
 * Provides optional field factory methods ({@link #createComboBox}, {@link #createTextField}, etc.)
 * that auto-track fields for visibility/access application. Subclasses that build fields manually
 * can override {@link #applyVisibility} and {@link #applyAccess} directly.
 *
 * @param <T>
 *            the DTO type managed by the parent form
 */
public abstract class FormComponent<T> extends VerticalLayout {

	private static final long serialVersionUID = 1L;

	protected final Binder<T> binder;

	private final List<Registration> registrations = new ArrayList<>();
	private final List<Component> trackedFields = new ArrayList<>();
	private final List<Component> validationFields = new ArrayList<>();
	private final List<HorizontalLayout> trackedRows = new ArrayList<>();
	private boolean validationRevealed;

	protected FormComponent(Class<T> dtoClass) {
		binder = new Binder<>(dtoClass);
		setWidthFull();
		// was setMargin(false): a Vaadin 8 margin is the padding of a Flow layout
		setPadding(false);
		setSpacing(true);
	}

	/** Tracks a registration for automatic removal on detach. */
	protected void track(Registration registration) {
		registrations.add(registration);
	}

	/** Removes all tracked registrations. Called by detach() automatically, but can also be called explicitly for mid-lifecycle cleanup. */
	protected void removeRegistrations() {
		for (Registration reg : registrations) {
			reg.remove();
		}
		registrations.clear();
	}

	/** Tracks a field for automatic visibility/access application. */
	protected void trackField(Component field) {
		trackedFields.add(field);
		trackValidationField(field);
	}

	/** Tracks a field so validation can be revealed on save attempts. */
	protected void trackValidationField(Component field) {
		validationFields.add(field);
	}

	/**
	 * Was a Vaadin 8 {@code DateField}: the value is a {@code LocalDate} as before.
	 */
	protected DatePicker createDateField(String propertyId, String i18nPrefix) {
		DatePicker field = new DatePicker();
		field.setId(propertyId);
		field.setLabel(I18nProperties.getPrefixCaption(i18nPrefix, propertyId));
		CssStyles.style(field, CssStyles.CAPTION_ON_TOP);
		field.setWidthFull();
		field.setLocale(I18nProperties.getUserLanguage().getLocale());
		DatePicker.DatePickerI18n i18n = new DatePicker.DatePickerI18n();
		i18n.setDateFormat(DateFormatHelper.getDateFormatPattern());
		field.setI18n(i18n);
		DatePickerLocale.applyOwnLocaleOnAttach(field);
		trackField(field);
		return field;
	}

	/**
	 * The combo box keeps its value when its items change and the value is still among them (Vaadin 8); see
	 * {@link KeepValueComboBox}.
	 */
	protected <V> ComboBox<V> createComboBox(String propertyId, String i18nPrefix) {
		ComboBox<V> field = new KeepValueComboBox<>();
		field.setId(propertyId);
		field.setLabel(I18nProperties.getPrefixCaption(i18nPrefix, propertyId));
		CssStyles.style(field, CssStyles.CAPTION_ON_TOP);
		field.setWidthFull();
		trackField(field);
		return field;
	}

	protected TextField createTextField(String propertyId, String i18nPrefix) {
		TextField field = new TextField();
		field.setId(propertyId);
		field.setLabel(I18nProperties.getPrefixCaption(i18nPrefix, propertyId));
		CssStyles.style(field, CssStyles.CAPTION_ON_TOP);
		field.setWidthFull();
		trackField(field);
		return field;
	}

	protected TextField createTextField(String propertyId, String i18nPrefix, ValueChangeMode mode) {
		TextField field = createTextField(propertyId, i18nPrefix);
		field.setValueChangeMode(mode);
		return field;
	}

	protected TextArea createTextArea(String propertyId, String i18nPrefix) {
		TextArea field = new TextArea();
		field.setId(propertyId);
		field.setLabel(I18nProperties.getPrefixCaption(i18nPrefix, propertyId));
		CssStyles.style(field, CssStyles.CAPTION_ON_TOP);
		field.setWidthFull();
		trackField(field);
		return field;
	}

	protected Checkbox createCheckBox(String propertyId, String i18nPrefix) {
		Checkbox field = new Checkbox();
		field.setId(propertyId);
		field.setLabel(I18nProperties.getPrefixCaption(i18nPrefix, propertyId));
		CssStyles.style(field, CssStyles.CAPTION_ON_TOP);
		field.setWidthFull();
		trackField(field);
		return field;
	}

	protected RadioButtonGroup<Boolean> createBooleanRadioGroup(String propertyId, String i18nPrefix) {
		RadioButtonGroup<Boolean> field = new RadioButtonGroup<>();
		field.setId(propertyId);
		field.setLabel(I18nProperties.getPrefixCaption(i18nPrefix, propertyId));
		CssStyles.style(field, CssStyles.CAPTION_ON_TOP, SormasFieldGroupFieldFactory.OPTIONGROUP_HORIZONTAL);
		field.setItems(Boolean.TRUE, Boolean.FALSE);
		field.setItemLabelGenerator(
			b -> b
				? I18nProperties.getEnumCaption(de.symeda.sormas.api.utils.YesNoUnknown.YES)
				: I18nProperties.getEnumCaption(de.symeda.sormas.api.utils.YesNoUnknown.NO));
		trackField(field);
		return field;
	}

	protected <E extends Enum<E>> RadioButtonGroup<E> createEnumRadioGroup(String propertyId, String i18nPrefix, Class<E> enumClass) {
		RadioButtonGroup<E> field = new RadioButtonGroup<>();
		field.setId(propertyId);
		field.setLabel(I18nProperties.getPrefixCaption(i18nPrefix, propertyId));
		CssStyles.style(field, CssStyles.CAPTION_ON_TOP, SormasFieldGroupFieldFactory.OPTIONGROUP_HORIZONTAL);
		field.setItems(enumClass.getEnumConstants());
		field.setItemLabelGenerator(I18nProperties::getEnumCaption);
		field.setWidthFull();
		trackField(field);
		return field;
	}

	/**
	 * Creates a row layout with the given fields, adds it to this component, and tracks it.
	 * If only one field (or two with the second null), a spacer is added for alignment.
	 */
	protected HorizontalLayout addRow(Component... fields) {
		HorizontalLayout row = new HorizontalLayout();
		row.setWidthFull();
		row.setSpacing(true);
		for (Component field : fields) {
			if (field != null) {
				row.add(field);
				row.setFlexGrow(1, field);
			}
		}
		if (fields.length == 1 || (fields.length == 2 && fields[1] == null)) {
			Span spacer = new Span();
			spacer.setWidthFull();
			row.add(spacer);
			row.setFlexGrow(1, spacer);
		}
		add(row);
		trackedRows.add(row);
		return row;
	}

	/** Creates a row with custom expand ratios per field. */
	protected HorizontalLayout addRow(float[] expandRatios, Component... fields) {
		HorizontalLayout row = new HorizontalLayout();
		row.setWidthFull();
		row.setSpacing(true);
		for (int i = 0; i < fields.length; i++) {
			if (fields[i] != null) {
				row.add(fields[i]);
				row.setFlexGrow(expandRatios[i], fields[i]);
			}
		}
		add(row);
		trackedRows.add(row);
		return row;
	}

	/** Creates a 3-component row for field + detail + spacer toggle pattern. */
	protected HorizontalLayout addToggleRow(Component main, Component detail, Component spacer) {
		HorizontalLayout row = new HorizontalLayout();
		row.setWidthFull();
		row.setSpacing(true);
		row.add(main);
		row.setFlexGrow(1, main);
		row.add(detail);
		row.setFlexGrow(1, detail);
		row.add(spacer);
		row.setFlexGrow(1, spacer);
		add(row);
		trackedRows.add(row);
		return row;
	}

	/** Creates a row with a leading spacer for right-alignment. */
	protected HorizontalLayout addRowWithLeadingSpacer(Component field) {
		HorizontalLayout row = new HorizontalLayout();
		row.setWidthFull();
		row.setSpacing(true);
		Span spacer = new Span();
		spacer.setWidthFull();
		row.add(spacer);
		row.setFlexGrow(1, spacer);
		row.add(field);
		row.setFlexGrow(1, field);
		add(row);
		trackedRows.add(row);
		return row;
	}

	/** Creates a single-field row without spacer (full width). */
	protected HorizontalLayout addFullWidthRow(Component field) {
		HorizontalLayout row = new HorizontalLayout();
		row.setWidthFull();
		row.setSpacing(true);
		row.add(field);
		row.setFlexGrow(1, field);
		add(row);
		trackedRows.add(row);
		return row;
	}

	/**
	 * Updates a ComboBox's items to only those enum values visible for the given disease
	 * (based on {@link Diseases} annotations). Keeps the current selection if still valid, without a
	 * value change ({@link KeepValueComboBox}, from {@link #createComboBox}); otherwise clears it.
	 */
	protected static <E extends Enum<E>> void updateComboBoxByDisease(ComboBox<E> comboBox, Class<E> enumClass, Disease disease) {
		List<E> filtered = Diseases.DiseasesConfiguration.getVisibleValues(enumClass, disease);
		filtered.sort(Comparator.comparing(Object::toString));
		comboBox.setItems(filtered);
	}

	/**
	 * Updates a ComboBox's items to only those enum values visible for the given disease
	 * AND applicable to the given pathogen test type (based on {@link Diseases} and
	 * {@link ApplicableToPathogenTests} annotations). Keeps the current selection if still valid, without a
	 * value change ({@link KeepValueComboBox}, from {@link #createComboBox}); otherwise clears it.
	 */
	protected static <E extends Enum<E>> void updateComboBoxByDiseaseAndTestType(
		ComboBox<E> comboBox,
		Class<E> enumClass,
		Disease disease,
		PathogenTestType testType) {

		List<E> filtered = Diseases.DiseasesConfiguration.getVisibleValues(enumClass, disease).stream().filter(value -> {
			if (testType == null) {
				return true;
			}
			try {
				java.lang.reflect.Field enumField = enumClass.getField(value.name());
				ApplicableToPathogenTests ann = enumField.getAnnotation(ApplicableToPathogenTests.class);
				return ann == null || java.util.Arrays.asList(ann.value()).contains(testType);
			} catch (NoSuchFieldException e) {
				return true;
			}
		}).sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList());
		comboBox.setItems(filtered);
	}

	/**
	 * Scopes the pathogen-test method combo to the methods that are visible for the given disease
	 * (via {@link Diseases}), selectable for new tests (via
	 * {@link PathogenTestType#isSelectableForNewTests}) and — when a category is selected — belong to
	 * that {@link PathogenTestCategory}. With no category selected the combo shows all
	 * selectable, disease-visible methods. The currently saved value is always kept selectable so an
	 * existing test recorded with a now-hidden method can still be edited. Sorted alphabetically.
	 */
	protected static void updateTestTypeItemsByCategory(ComboBox<PathogenTestType> comboBox, Disease disease, PathogenTestCategory category) {
		updateTestTypeItemsByCategory(comboBox, disease, category, comboBox.getValue());
	}

	/**
	 * As {@link #updateTestTypeItemsByCategory(ComboBox, Disease, PathogenTestCategory)} but with an
	 * explicit {@code retainType} that is always kept in the list (and its category honoured) even if it
	 * is hidden or not selectable — used when loading an existing test whose method may be legacy.
	 */
	protected static void updateTestTypeItemsByCategory(
		ComboBox<PathogenTestType> comboBox,
		Disease disease,
		PathogenTestCategory category,
		PathogenTestType retainType) {
		updateTestTypeItemsByCategory(comboBox, disease, category, retainType, true);
	}

	/**
	 * @param selectRetainType
	 *            when {@code true} the {@code retainType} is also selected; pass {@code false} to only
	 *            make it a selectable item and leave the value untouched (e.g. when the binder is about
	 *            to set the value itself, so the value-change event still fires for listeners).
	 */
	protected static void updateTestTypeItemsByCategory(
		ComboBox<PathogenTestType> comboBox,
		Disease disease,
		PathogenTestCategory category,
		PathogenTestType retainType,
		boolean selectRetainType) {

		List<PathogenTestType> filtered = Diseases.DiseasesConfiguration.getVisibleValues(PathogenTestType.class, disease)
			.stream()
			.filter(type -> type == retainType || PathogenTestType.isSelectableForNewTests(type))
			.filter(type -> type == retainType || category == null || PathogenTestType.getCategory(type) == category)
			.collect(Collectors.toList());
		// getVisibleValues drops values marked @Diseases(hide = true) for this disease, so the
		// type == retainType guards above cannot re-add a saved legacy method that is hidden here.
		// Add it explicitly so an existing test recorded with such a method still loads and displays.
		if (retainType != null && !filtered.contains(retainType)) {
			filtered.add(retainType);
		}
		filtered.sort(Comparator.comparing(Object::toString));
		comboBox.setItems(filtered);
		if (selectRetainType && retainType != null && filtered.contains(retainType)) {
			comboBox.setValue(retainType);
		}
	}

	/**
	 * Lists the {@link PathogenTestCategory} values that have at least one selectable method visible
	 * for the given disease, so empty categories are not offered. Sorted alphabetically.
	 */
	protected static List<PathogenTestCategory> getVisibleTestCategories(Disease disease) {
		return getVisibleTestCategories(disease, null);
	}

	/**
	 * As {@link #getVisibleTestCategories(Disease)} but additionally includes {@code retainCategory}
	 * even when no selectable, disease-visible method maps to it — used when loading an existing test
	 * whose (possibly legacy/hidden) method's category would otherwise be absent from the selector.
	 */
	protected static List<PathogenTestCategory> getVisibleTestCategories(Disease disease, PathogenTestCategory retainCategory) {
		List<PathogenTestCategory> categories = Diseases.DiseasesConfiguration.getVisibleValues(PathogenTestType.class, disease)
			.stream()
			.filter(PathogenTestType::isSelectableForNewTests)
			.map(PathogenTestType::getCategory)
			.filter(java.util.Objects::nonNull)
			.distinct()
			.collect(Collectors.toList());
		if (retainCategory != null && !categories.contains(retainCategory)) {
			categories.add(retainCategory);
		}
		categories.sort(Comparator.comparing(Object::toString));
		return categories;
	}

	/**
	 * Was a Vaadin 8 {@code Label}; {@link #updateRowVisibility} ignores spacers.
	 */
	protected Span createSpacer() {
		Span spacer = new Span();
		spacer.setWidthFull();
		return spacer;
	}

	/** Called when the parent form receives a new DTO value. */
	public void setDto(T dto) {
		binder.setBean(dto);
		revealValidation();
		if (dto != null) {
			binder.validate();
		}
	}

	/** Enables component-managed validation styling. */
	public void revealValidation() {
		validationRevealed = true;
	}

	/** Validates this component's fields; throws {@link InvalidValueException} on failure. */
	public void validate() {
		validateBinderOnly();
	}

	/**
	 * Runs only this component's binder validation & throws on failure. Subclasses that override {@link #validate()}
	 * use this as a check in {@link #validateAll} (they cannot call {@code super.validate()} from a method reference).
	 */
	protected void validateBinderOnly() {
		revealValidation();
		validateBinder(binder.validate());
	}

	/**
	 * Runs every {@code check} and accumulates their failures instead of stopping at the first, then throws a
	 * single combined {@link InvalidValueException} carrying all of them as causes. Lets a component report all
	 * of its missing/invalid fields in one pass (binder {@code asRequired} fields plus any manual checks) so the
	 * parent form's aggregation surfaces them together rather than one save at a time.
	 */
	protected void validateAll(Runnable... checks) {
		revealValidation();
		List<InvalidValueException> causes = new ArrayList<>();
		for (Runnable check : checks) {
			try {
				check.run();
			} catch (InvalidValueException e) {
				collectCauses(e, causes);
			}
		}
		throwCombined(causes);
	}

	/** Flattens {@code e} into {@code target}: its leaf causes if it is a combined exception, else itself. */
	public static void collectCauses(InvalidValueException e, List<InvalidValueException> target) {
		if (e.getCauses().length > 0) {
			Collections.addAll(target, e.getCauses());
		} else {
			target.add(e);
		}
	}

	/** Throws a single {@link InvalidValueException} carrying all {@code causes}, or nothing if empty. */
	public static void throwCombined(List<InvalidValueException> causes) {
		if (!causes.isEmpty()) {
			String joinedCaptions = causes.stream().map(InvalidValueException::getMessage).collect(Collectors.joining(", "));
			throw new InvalidValueException(joinedCaptions, causes.toArray(new InvalidValueException[0]));
		}
	}

	protected void updateRequiredValidation(HasValue<?, ?> field) {
		Component component = (Component) field;
		if (!component.isVisible() || !field.isEmpty()) {
			clearValidationError(component);
			return;
		}
		applyValidationError(component, requiredErrorMessage(component));
	}

	protected void validateRequiredVisible(HasValue<?, ?> field) {
		Component component = (Component) field;
		if (!component.isVisible()) {
			clearValidationError(component);
			return;
		}
		if (field.isEmpty()) {
			String errorMessage = requiredErrorMessage(component);
			applyValidationError(component, errorMessage);
			throw new InvalidValueException(errorMessage);
		}
		clearValidationError(component);
	}

	protected void updateValidationError(Component field, String errorMessage) {
		if (errorMessage == null) {
			clearValidationError(field);
		} else {
			applyValidationError(field, errorMessage);
		}
	}

	protected void validateField(Component field, String errorMessage) {
		updateValidationError(field, errorMessage);
		if (errorMessage != null) {
			throw new InvalidValueException(errorMessage);
		}
	}

	private String requiredErrorMessage(Component field) {
		return I18nProperties.getValidationError(Validations.required, getCaption(field));
	}

	/**
	 * Was {@code Component.getCaption()}: the label of a Flow field.
	 */
	protected static String getCaption(Component field) {
		return field instanceof HasLabel ? ((HasLabel) field).getLabel() : null;
	}

	private void applyValidationError(Component field, String errorMessage) {
		if (!validationRevealed) {
			clearValidationError(field);
			return;
		}
		// was setComponentError(new UserError(errorMessage))
		if (field instanceof HasValidation) {
			((HasValidation) field).setErrorMessage(errorMessage);
			((HasValidation) field).setInvalid(true);
		}
	}

	protected void clearValidationError(Component field) {
		if (field instanceof HasValidation) {
			((HasValidation) field).setErrorMessage(null);
			((HasValidation) field).setInvalid(false);
		}
	}

	/**
	 * Applies annotation-based field visibility rules.
	 * Default implementation uses tracked fields; subclasses may override for manual control.
	 */
	public void applyVisibility(FieldVisibilityCheckers checkers, Class<?> dtoClass) {
		for (Component field : trackedFields) {
			String id = field.getId().orElse(null);
			if (id != null && !checkers.isVisible(dtoClass, id)) {
				field.setVisible(false);
			}
		}
		updateRowAndSelfVisibility();
	}

	/**
	 * Applies annotation-based field access/editability rules.
	 * Default implementation uses tracked fields; subclasses may override for manual control.
	 */
	public void applyAccess(UiFieldAccessCheckers checkers, Class<?> dtoClass) {
		for (Component field : trackedFields) {
			String id = field.getId().orElse(null);
			if (id != null && !checkers.isAccessible(dtoClass, id)) {
				if (field instanceof HasEnabled) {
					((HasEnabled) field).setEnabled(false);
				}
				if (field instanceof HasStyle) {
					CssStyles.style((HasStyle) field, CssStyles.INACCESSIBLE_FIELD);
				}
			}
		}
	}

	/** Subclasses perform any additional cleanup beyond binder unbinding. */
	protected void onCleanup() {
	}

	@Override
	protected void onDetach(DetachEvent detachEvent) {
		for (Registration reg : registrations) {
			reg.remove();
		}
		registrations.clear();
		binder.removeBean();
		onCleanup();
		super.onDetach(detachEvent);
	}

	/**
	 * Updates visibility of tracked rows and this component itself.
	 * Hides rows where all fields are hidden, and hides this section entirely
	 * if no rows are visible.
	 */
	protected void updateRowAndSelfVisibility() {
		for (HorizontalLayout row : trackedRows) {
			updateRowVisibility(row);
		}

		boolean anyVisible = hasVisibleContent();
		for (HorizontalLayout row : trackedRows) {
			if (row.isVisible()) {
				anyVisible = true;
				break;
			}
		}
		setVisible(anyVisible);
	}

	/**
	 * Subclasses override to indicate additional visible content beyond tracked rows
	 * (e.g. drug susceptibility fields). Default returns false.
	 */
	protected boolean hasVisibleContent() {
		return false;
	}

	/**
	 * Validates a Binder and throws an {@link InvalidValueException} if validation fails.
	 * Creates a structured exception hierarchy so that field captions appear in the error notification.
	 */
	public static <T> void validateBinder(BinderValidationStatus<T> status) {
		if (status.hasErrors()) {
			List<InvalidValueException> fieldCauses = new ArrayList<>();

			for (BindingValidationStatus<?> fieldStatus : status.getFieldValidationErrors()) {
				String caption = null;
				if (fieldStatus.getField() instanceof Component) {
					caption = getCaption((Component) fieldStatus.getField());
				}
				String errorMsg = fieldStatus.getMessage().orElse(caption != null ? caption : "");
				fieldCauses.add(new InvalidValueException(caption != null ? caption : errorMsg));
			}

			for (ValidationResult beanError : status.getBeanValidationErrors()) {
				fieldCauses.add(new InvalidValueException(beanError.getErrorMessage()));
			}

			if (fieldCauses.isEmpty()) {
				String message = status.getValidationErrors().stream().map(ValidationResult::getErrorMessage).collect(Collectors.joining("; "));
				throw new InvalidValueException(message);
			}

			String joinedCaptions = fieldCauses.stream().map(InvalidValueException::getMessage).collect(Collectors.joining(", "));
			throw new InvalidValueException(joinedCaptions, fieldCauses.toArray(new InvalidValueException[0]));
		}
	}

	/**
	 * Hides a layout row if none of its actual field children are visible (ignores spacers: {@link Span}s, which were
	 * Vaadin 8 Labels).
	 */
	public static void updateRowVisibility(Component row) {
		boolean anyChildVisible = false;
		for (Component child : (Iterable<Component>) row.getChildren()::iterator) {
			if (child instanceof Span) {
				continue;
			}
			if (child instanceof FlexComponent) {
				updateRowVisibility(child);
			}
			if (child.isVisible()) {
				anyChildVisible = true;
			}
		}
		row.setVisible(anyChildVisible);
	}
}
