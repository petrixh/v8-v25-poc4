package de.symeda.sormas.ui.utils;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.ReferenceDto;
import de.symeda.sormas.api.adverseeventsfollowingimmunization.AdverseEventState;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.symptoms.SymptomState;
import de.symeda.sormas.api.utils.FieldConstraints;
import de.symeda.sormas.api.utils.YesNoUnknown;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.ActivityAsCase.ActivityAsCaseField;
import de.symeda.sormas.ui.adverseeventsfollowingimmunization.components.fields.vaccines.AefiVaccinationsField;
import de.symeda.sormas.ui.adverseeventsfollowingimmunization.components.form.AdverseEventsForm;
import de.symeda.sormas.ui.clinicalcourse.HealthConditionsForm;
import de.symeda.sormas.ui.exposure.ExposuresField;
import de.symeda.sormas.ui.hospitalization.PreviousHospitalizationsField;
import de.symeda.sormas.ui.location.LocationEditForm;
import de.symeda.sormas.ui.person.LocationsField;
import de.symeda.sormas.ui.person.PersonContactDetailsField;
import de.symeda.sormas.ui.therapy.DrugSusceptibilityForm;
import de.symeda.sormas.ui.user.UserRoleNotificationCheckboxSet;
import de.symeda.sormas.ui.utils.components.CheckboxSet;
import de.symeda.sormas.ui.utils.components.CustomizableEnumPropertiesComponent;
import de.symeda.sormas.ui.utils.components.CustomizableEnumTranslationComponent;
import de.symeda.sormas.ui.utils.components.DiseaseConfigurationAgeGroupComponent;
import de.symeda.sormas.ui.utils.components.JsonForm;
import de.symeda.sormas.ui.utils.components.MultiSelect;
import de.symeda.sormas.ui.utils.components.MultiSelectFiles;
import de.symeda.sormas.ui.utils.components.SormasTextField;
import de.symeda.sormas.ui.utils.fields.AbstractTextField;
import de.symeda.sormas.ui.utils.fields.CheckBox;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.fields.DateField;
import de.symeda.sormas.ui.utils.fields.Field;
import de.symeda.sormas.ui.utils.fields.FilteringMode;
import de.symeda.sormas.ui.utils.fields.OptionGroup;
import de.symeda.sormas.ui.utils.fields.PasswordField;
import de.symeda.sormas.ui.utils.fields.PopupDateField;
import de.symeda.sormas.ui.utils.fields.SelectField;
import de.symeda.sormas.ui.utils.fields.SormasRichTextArea;
import de.symeda.sormas.ui.utils.fields.TextArea;
import de.symeda.sormas.ui.utils.fields.TextField;
import de.symeda.sormas.ui.vaccination.VaccinationsField;

/**
 * Creates the Flow field for a property type and a requested field class, as the V7 field group field factory did.
 * The requested classes are the SORMAS field types in {@code de.symeda.sormas.ui.utils.fields} (same simple names as
 * the V7 ones) and the SORMAS custom fields. All created fields implement {@link Field}.
 */
public class SormasFieldGroupFieldFactory implements java.io.Serializable {

	/**
	 * V7 {@code DefaultFieldGroupFieldFactory.CAPTION_PROPERTY_ID}.
	 */
	public static final String CAPTION_PROPERTY_ID = "Caption";
	/**
	 * V7 Valo style name of a horizontal option group, kept as class name.
	 */
	public static final String OPTIONGROUP_HORIZONTAL = "horizontal";

	public static final int TEXT_AREA_MAX_LENGTH = FieldConstraints.CHARACTER_LIMIT_BIG;
	public static final int TEXT_FIELD_MAX_LENGTH = FieldConstraints.CHARACTER_LIMIT_DEFAULT;
	private static final long serialVersionUID = 471700572643936674L;
	private final FieldVisibilityCheckers fieldVisibilityCheckers;
	private final UiFieldAccessCheckers fieldAccessCheckers;
	private final boolean isEditAllowed;

	public SormasFieldGroupFieldFactory(FieldVisibilityCheckers fieldVisibilityCheckers, UiFieldAccessCheckers fieldAccessCheckers) {
		this.fieldVisibilityCheckers = fieldVisibilityCheckers;
		this.fieldAccessCheckers = fieldAccessCheckers;
		this.isEditAllowed = true;
	}

	public SormasFieldGroupFieldFactory(
		FieldVisibilityCheckers fieldVisibilityCheckers,
		UiFieldAccessCheckers fieldAccessCheckers,
		boolean isEditAllowed) {
		this.fieldVisibilityCheckers = fieldVisibilityCheckers;
		this.fieldAccessCheckers = fieldAccessCheckers;
		this.isEditAllowed = isEditAllowed;
	}

	@SuppressWarnings({
		"unchecked",
		"rawtypes" })
	public <T extends Field> T createField(Class<?> type, Class<T> fieldType) {
		if (type.isEnum()) {
			if (fieldType.isAssignableFrom(Field.class) // no specific fieldType defined?
				&& (SymptomState.class.isAssignableFrom(type)
					|| YesNoUnknown.class.isAssignableFrom(type)
					|| AdverseEventState.class.isAssignableFrom(type))) {
				NullableOptionGroup field = new NullableOptionGroup();
				field.setImmediate(true);
				populateWithEnumData(field, (Class<? extends Enum>) type);
				field.addStyleName(OPTIONGROUP_HORIZONTAL);
				field.addStyleName(CssStyles.OPTIONGROUP_CAPTION_INLINE);
				return (T) field;
			} else {
				if (Disease.class.isAssignableFrom(type)) {
					fieldType = (Class<T>) ComboBox.class;
					ComboBox field = ComboBoxHelper.createComboBoxV7();
					field.setImmediate(true);
					field.setNullSelectionAllowed(true);
					populateWithDiseaseData(field);
					return (T) field;
				} else {
					if (!SelectField.class.isAssignableFrom(fieldType)) {
						fieldType = (Class<T>) ComboBox.class;
					}
					T field = ComboBoxWithPlaceholder.class == fieldType ? (T) new ComboBoxWithPlaceholder() : createEnumField(type, fieldType);
					if (field instanceof OptionGroup) {
						field.addStyleName(OPTIONGROUP_HORIZONTAL);
					} else if (fieldType.isAssignableFrom(NullableOptionGroup.class)) {
						NullableOptionGroup select = new NullableOptionGroup();
						select.setImmediate(true);
						populateWithEnumData(select, (Class<? extends Enum>) type);
						select.addStyleName(OPTIONGROUP_HORIZONTAL);
						field = (T) select;
					} else if (field instanceof ComboBox) {
						((ComboBox) field).setFilteringMode(FilteringMode.CONTAINS);
						((ComboBox) field).setNullSelectionAllowed(true);
					}
					return field;
				}
			}
		} else if (Boolean.class.isAssignableFrom(type)) {
			fieldType = CheckBox.class.isAssignableFrom(fieldType) ? (Class<T>) CheckBox.class : (Class<T>) NullableOptionGroup.class;

			return createBooleanField(fieldType);
		} else if (ComboBox.class.isAssignableFrom(fieldType) || ComboBoxWithPlaceholder.class.isAssignableFrom(fieldType)) {
			ComboBoxWithPlaceholder combo = new ComboBoxWithPlaceholder();
			combo.setImmediate(true);

			return (T) combo;
		} else if (OptionGroup.class.isAssignableFrom(fieldType) || OptionGroupWithCaption.class.isAssignableFrom(fieldType)) {
			OptionGroupWithCaption optionGroupWithCaption = new OptionGroupWithCaption();
			optionGroupWithCaption.setImmediate(true);

			return (T) optionGroupWithCaption;
		} else if (SelectField.class.isAssignableFrom(fieldType)) {
			SelectField field = createCompatibleSelect((Class<? extends SelectField>) fieldType);
			field.setNullSelectionAllowed(true);
			return (T) field;
		} else if (LocationEditForm.class.isAssignableFrom(fieldType)) {
			return (T) new LocationEditForm(fieldVisibilityCheckers, fieldAccessCheckers);
		} else if (HealthConditionsForm.class.isAssignableFrom(fieldType)) {
			return (T) new HealthConditionsForm(fieldVisibilityCheckers, fieldAccessCheckers);
		} else if (DateTimeField.class.isAssignableFrom(fieldType)) {
			DateTimeField field = new DateTimeField();
			field.setConverter(SormasDefaultConverterFactory.createDateConverter(Date.class));
			return (T) field;
		} else if (DateField.class.isAssignableFrom(fieldType)) {
			DateField field = createDateField(fieldType);
			field.setDateFormat(DateFormatHelper.getDateFormatPattern());
			field.setLenient(true);
			field.setConverter(SormasDefaultConverterFactory.createDateConverter(Date.class));
			return (T) field;
		} else if (PreviousHospitalizationsField.class.isAssignableFrom(fieldType)) {
			return (T) new PreviousHospitalizationsField(fieldVisibilityCheckers, fieldAccessCheckers, isEditAllowed);
		} else if (ExposuresField.class.isAssignableFrom(fieldType)) {
			return (T) new ExposuresField(null,fieldVisibilityCheckers, fieldAccessCheckers, isEditAllowed);
		} else if (ActivityAsCaseField.class.isAssignableFrom(fieldType)) {
			return (T) new ActivityAsCaseField(fieldVisibilityCheckers, fieldAccessCheckers, isEditAllowed);
		} else if (LocationsField.class.isAssignableFrom(fieldType)) {
			return (T) new LocationsField(fieldVisibilityCheckers, fieldAccessCheckers, isEditAllowed);
		} else if (PersonContactDetailsField.class.isAssignableFrom(fieldType)) {
			return (T) new PersonContactDetailsField(fieldVisibilityCheckers, fieldAccessCheckers, isEditAllowed);
		} else if (VaccinationsField.class.isAssignableFrom(fieldType)) {
			return (T) new VaccinationsField(fieldAccessCheckers);
		} else if (JsonForm.class.isAssignableFrom(fieldType)) {
			return (T) new JsonForm(fieldVisibilityCheckers, fieldAccessCheckers);
		} else if (fieldType.equals(Field.class)) {
			// no specific field type defined -> fallbacks
			if (Date.class.isAssignableFrom(type)) {
				DateField field = createDateField(DateField.class);
				field.setDateFormat(DateFormatHelper.getDateFormatPattern());
				field.setLenient(true);
				field.setConverter(SormasDefaultConverterFactory.createDateConverter(Date.class));
				return (T) field;
			} else if (ReferenceDto.class.isAssignableFrom(type)) {
				return (T) ComboBoxHelper.createComboBoxV7();
			}
		} else if (MultiSelect.class.isAssignableFrom(fieldType)) {
			return (T) MultiSelect.create(type);
		} else if (MultiSelectFiles.class.isAssignableFrom(fieldType)) {
			return (T) MultiSelectFiles.create(type);
		} else if (CheckboxSet.class.isAssignableFrom(fieldType)) {
			return (T) new CheckboxSet();
		} else if (UserRoleNotificationCheckboxSet.class.isAssignableFrom(fieldType)) {
			return (T) new UserRoleNotificationCheckboxSet();
		} else if (CustomizableEnumTranslationComponent.class.isAssignableFrom(fieldType)) {
			return (T) new CustomizableEnumTranslationComponent();
		} else if (DiseaseConfigurationAgeGroupComponent.class.isAssignableFrom(fieldType)) {
			return (T) new DiseaseConfigurationAgeGroupComponent();
		} else if (CustomizableEnumPropertiesComponent.class.isAssignableFrom(fieldType)) {
			return (T) new CustomizableEnumPropertiesComponent();
		} else if (UserField.class.isAssignableFrom(fieldType)) {
			return (T) new UserField();
		} else if (AefiVaccinationsField.class.isAssignableFrom(fieldType)) {
			return (T) new AefiVaccinationsField(fieldAccessCheckers);
		} else if (AdverseEventsForm.class.isAssignableFrom(fieldType)) {
			return (T) new AdverseEventsForm(fieldVisibilityCheckers, fieldAccessCheckers);
		} else if (DrugSusceptibilityForm.class.isAssignableFrom(fieldType)) {
			return (T) new DrugSusceptibilityForm(fieldVisibilityCheckers, fieldAccessCheckers);
		} else if (CheckBoxTree.class.isAssignableFrom(fieldType)) {
			return (T) new CheckBoxTree<>();
		} else if (SormasRichTextArea.class.isAssignableFrom(fieldType)) {
			return (T) new SormasRichTextArea();
		} else if (SormasTextField.class.isAssignableFrom(fieldType)) {
			return (T) new SormasTextField();
		}
		return createDefaultField(type, fieldType);
	}

	/**
	 * V7 {@code DefaultFieldGroupFieldFactory.createField} for the cases not handled above.
	 */
	@SuppressWarnings({
		"unchecked",
		"rawtypes" })
	protected <T extends Field> T createDefaultField(Class<?> type, Class<T> fieldType) {
		if (Enum.class.isAssignableFrom(type)) {
			return createEnumField(type, fieldType);
		} else if (Date.class.isAssignableFrom(type)) {
			if (fieldType == Field.class || DateField.class.isAssignableFrom(fieldType)) {
				return (T) createDateField(fieldType == Field.class ? DateField.class : (Class<? extends DateField>) fieldType);
			} else if (AbstractTextField.class.isAssignableFrom(fieldType)) {
				return (T) createAbstractTextField((Class<? extends AbstractTextField>) fieldType);
			}
			return null;
		} else if (Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)) {
			return createBooleanField(fieldType);
		}
		if (AbstractTextField.class.isAssignableFrom(fieldType)) {
			return (T) createAbstractTextField((Class<? extends AbstractTextField>) fieldType);
		}
		if (fieldType.isAssignableFrom(TextField.class)) {
			return (T) createAbstractTextField(TextField.class);
		}
		return createInstance(fieldType);
	}

	/**
	 * V7 {@code createEnumField}: a select filled with the enum values, or a text field.
	 */
	@SuppressWarnings({
		"unchecked",
		"rawtypes" })
	protected <T extends Field> T createEnumField(Class<?> type, Class<T> fieldType) {
		Class<? extends SelectField> selectClass = null;
		if (SelectField.class.isAssignableFrom(fieldType)) {
			selectClass = (Class<? extends SelectField>) fieldType;
		} else if (fieldType == Field.class) {
			selectClass = ComboBox.class;
		}
		if (selectClass != null) {
			SelectField select = createCompatibleSelect(selectClass);
			populateWithEnumData(select, (Class<? extends Enum>) type);
			return (T) select;
		} else if (AbstractTextField.class.isAssignableFrom(fieldType)) {
			return (T) createAbstractTextField((Class<? extends AbstractTextField>) fieldType);
		}
		return null;
	}

	/**
	 * V7 {@code createDateField}: a popup date field (Flow date picker) unless a subclass is requested.
	 */
	protected DateField createDateField(Class<?> fieldType) {
		DateField field;
		if (fieldType == DateField.class || fieldType == PopupDateField.class || !DateField.class.isAssignableFrom(fieldType)) {
			field = new PopupDateField();
		} else {
			field = (DateField) createInstance(fieldType.asSubclass(Field.class));
		}
		field.setImmediate(true);
		return field;
	}

	protected SelectField createCompatibleSelect(Class<? extends SelectField> fieldType) {
		if (NullableOptionGroup.class.isAssignableFrom(fieldType)) {
			return new NullableOptionGroup();
		}
		if (ComboBoxWithPlaceholder.class.isAssignableFrom(fieldType)) {
			return new ComboBoxWithPlaceholder();
		}
		SelectField select;
		if (fieldType.isAssignableFrom(ComboBox.class)) {
			// V7: a plain combo box without null selection
			select = new ComboBox();
			select.setNullSelectionAllowed(false);
		} else if (fieldType.isAssignableFrom(OptionGroup.class)) {
			select = new OptionGroup();
		} else {
			select = createInstance(fieldType);
		}
		select.setImmediate(true);
		return select;
	}

	protected <T extends AbstractTextField> T createAbstractTextField(Class<T> fieldType) {
		T textField;
		if (fieldType == AbstractTextField.class || fieldType == TextField.class) {
			textField = fieldType.cast(new TextField());
		} else if (fieldType == TextArea.class) {
			textField = fieldType.cast(new TextArea());
		} else if (fieldType == PasswordField.class) {
			textField = fieldType.cast(new PasswordField());
		} else {
			textField = createInstance(fieldType);
		}
		textField.setImmediate(true);
		textField.setNullRepresentation("");
		return textField;
	}

	@SuppressWarnings({
		"unchecked",
		"rawtypes" })
	protected <T extends Field> T createBooleanField(Class<T> fieldType) {
		if (NullableOptionGroup.class.isAssignableFrom(fieldType)) {
			final SelectField s = new NullableOptionGroup();
			s.addItem(Boolean.TRUE);
			s.setItemCaption(Boolean.TRUE, I18nProperties.getEnumCaption(YesNoUnknown.YES));
			s.addItem(Boolean.FALSE);
			s.setItemCaption(Boolean.FALSE, I18nProperties.getEnumCaption(YesNoUnknown.NO));

			s.addStyleName(OPTIONGROUP_HORIZONTAL);

			return (T) s;
		} else if (fieldType.isAssignableFrom(CheckBox.class)) {
			CheckBox cb = new CheckBox();
			cb.setImmediate(true);
			return (T) cb;
		} else if (AbstractTextField.class.isAssignableFrom(fieldType)) {
			return (T) createAbstractTextField((Class<? extends AbstractTextField>) fieldType);
		}
		return null;
	}

	/**
	 * V7 {@code ReflectTools.createInstance} for field classes with a public no-argument constructor.
	 */
	protected <T> T createInstance(Class<T> fieldType) {
		try {
			return fieldType.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException("Could not create a field of type " + fieldType.getName(), e);
		}
	}

	@SuppressWarnings("unchecked")
	protected void populateWithDiseaseData(ComboBox diseaseField) {

		diseaseField.removeAllItems();
		List<Disease> diseases = FacadeProvider.getDiseaseConfigurationFacade().getAllDiseases(true, true, true);
		for (Object r : diseases) {
			diseaseField.addItem(r);
			diseaseField.setItemCaption(r, r.toString());
		}
	}

	@SuppressWarnings("rawtypes")
	protected void populateWithEnumData(SelectField select, Class<? extends Enum> enumClass) {

		select.removeAllItems();
		EnumSet<?> enumSet = EnumSet.allOf(enumClass);
		for (Object r : enumSet) {
			boolean visible = true;
			if (fieldVisibilityCheckers != null) {
				visible = fieldVisibilityCheckers.isVisible(enumClass, ((Enum<?>) r).name());
			}

			if (visible) {
				select.addItem(r);
				select.setItemCaption(r, r.toString());
			}
		}
	}

}
