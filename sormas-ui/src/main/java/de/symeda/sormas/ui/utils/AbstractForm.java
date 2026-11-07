package de.symeda.sormas.ui.utils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BeanPropertySet;
import com.vaadin.flow.data.binder.BindingValidationStatus;
import com.vaadin.flow.data.binder.PropertyDefinition;
import com.vaadin.flow.data.binder.PropertySet;
import com.vaadin.flow.data.binder.ValidationException;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.Month;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.fields.CustomField;
import de.symeda.sormas.ui.utils.fields.DateField;
import de.symeda.sormas.ui.utils.fields.Field;
import de.symeda.sormas.ui.utils.fields.FieldGroup;
import de.symeda.sormas.ui.utils.fields.FilteringMode;
import de.symeda.sormas.ui.utils.fields.TextArea;
import de.symeda.sormas.ui.utils.fields.TextField;

/**
 * Base of the SORMAS forms: a field (its value is the DTO) whose content is an HTML template ({@link CustomLayout}) with
 * one slot per field. The fields are bound by property id through a {@link BeanFieldGroup}, which keeps the Vaadin 7
 * {@code BeanFieldGroup} API on top of a Flow {@link Binder} in buffered mode.
 */
public abstract class AbstractForm<T> extends CustomField<T> implements WithChildComponents, Buffered {

	private static final long serialVersionUID = -3816438104870246080L;

	private static final Logger logger = LoggerFactory.getLogger(AbstractForm.class);

	protected final String propertyI18nPrefix;
	private final BeanFieldGroup<T> fieldGroup;
	private Class<T> type;
	private List<Field<?>> customFields = new ArrayList<>();
	private boolean buffered;

	protected AbstractForm(Class<T> type, String propertyI18nPrefix, SormasFieldGroupFieldFactory fieldFactory, boolean addFields) {

		this.type = type;
		this.propertyI18nPrefix = propertyI18nPrefix;

		fieldGroup = new BeanFieldGroup<>(type);

		fieldGroup.setFieldFactory(fieldFactory);
		setHeightUndefined();

		if (addFields) {
			addFields();
		}
	}

	protected abstract String createHtmlLayout();

	protected abstract void addFields();

	@Override
	public Class<? extends T> getType() {
		return type;
	}

	@Override
	public CustomLayout getContent() {
		return (CustomLayout) super.getContent();
	}

	@Override
	public CustomLayout initContent() {

		String htmlLayout = createHtmlLayout();
		CustomLayout layout = new CustomLayout();
		layout.setTemplateContents(htmlLayout);
		layout.setWidthFull();
		layout.setHeightUndefined();

		return layout;
	}

	@Override
	public void forEachComponent(Consumer<Component> componentConsumer) {
		getContent().iterator().forEachRemaining(componentConsumer);
	}

	public BeanFieldGroup<T> getFieldGroup() {
		return this.fieldGroup;
	}

	public List<Field<?>> getCustomFields() {
		return customFields;
	}

	protected void addFields(String... properties) {
		for (String property : properties) {
			addField(property);
		}
	}

	protected void addFields(List<String> properties) {
		for (String property : properties) {
			addField(property);
		}
	}

	public void addFields(FieldConfiguration... properties) {
		addFields(getContent(), properties);
	}

	public void addFields(CustomLayout layout, String... properties) {
		for (String property : properties) {
			addField(layout, property);
		}
	}

	public void addFields(CustomLayout layout, FieldConfiguration... properties) {
		for (FieldConfiguration property : properties) {
			addField(layout, property);
		}
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> F addField(FieldConfiguration configuration) {
		return addField(getContent(), configuration);
	}

	@SuppressWarnings({
		"rawtypes",
		"unchecked" })
	protected <F extends Field> F addField(FieldConfiguration configuration, Class<F> fieldType) {
		Field field = addField(getContent(), configuration.getPropertyId(), fieldType);

		applyFieldConfiguration(configuration, field);

		return (F) field;
	}

	@SuppressWarnings({
		"unchecked",
		"rawtypes" })
	protected <F extends Field> F addField(CustomLayout layout, FieldConfiguration configuration) {
		Field field = addField(layout, configuration.getPropertyId());

		applyFieldConfiguration(configuration, field);

		return (F) field;
	}

	@SuppressWarnings({
		"rawtypes",
		"unchecked" })
	protected <F extends Field> F addField(CustomLayout layout, Class<F> fieldType, FieldConfiguration configuration) {
		Field field = addField(layout, configuration.getPropertyId(), fieldType);

		applyFieldConfiguration(configuration, field);

		return (F) field;
	}

	@SuppressWarnings({
		"rawtypes",
		"unchecked" })
	protected void applyFieldConfiguration(FieldConfiguration configuration, Field field) {
		if (configuration.getWidth() != null) {
			field.setWidth(configuration.getWidth(), configuration.getWidthUnit());
		}

		if (configuration.getCaption() != null) {
			field.setCaption(configuration.getCaption());
		}

		if (configuration.getDescription() != null) {
			field.setDescription(configuration.getDescription());
		}

		if (configuration.getStyle() != null) {
			field.addStyleName(configuration.getStyle());
		}

		if (configuration.getValidationMessageProperty() != null && field instanceof TextField) {
			field.setConversionError(I18nProperties.getValidationError(Validations.onlyNumbersAllowed, field.getCaption()));
		}

		if (configuration.getValueChangeListener() != null) {
			field.addValueChangeListener(configuration.getValueChangeListener());
		}
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> void addFields(Class<F> fieldType, String... properties) {
		for (String property : properties) {
			addField(property, fieldType);
		}
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> void addFieldsWithCss(Class<F> fieldType, List<String> properties, String... cssClasses) {
		for (String property : properties) {
			F field = addField(property, fieldType);
			for (String cssClass : cssClasses) {
				field.addStyleName(cssClass);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> void addFields(CustomLayout layout, Class<F> fieldType, String... properties) {
		for (String property : properties) {
			addField(layout, property, fieldType);
		}
	}

	@SuppressWarnings({
		"unchecked",
		"rawtypes" })
	protected <F extends Field> F addField(String propertyId) {
		return (F) addField(propertyId, Field.class);
	}

	@SuppressWarnings({
		"unchecked",
		"rawtypes" })
	protected <F extends Field> F addField(CustomLayout layout, String propertyId) {
		return (F) addField(layout, propertyId, Field.class);
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> F addField(String propertyId, Class<F> fieldType) {
		return addField(getContent(), propertyId, fieldType);
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> F addField(String propertyId, F field) {
		return addField(getContent(), propertyId, field);
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> F addField(CustomLayout layout, String propertyId, Class<F> fieldType) {
		F field = createField(propertyId, fieldType);
		return addFieldToLayout(layout, propertyId, field);
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> F addField(CustomLayout layout, String propertyId, F field) {
		getFieldGroup().bind(field, propertyId);

		return addFieldToLayout(layout, propertyId, field);
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> F addFieldToLayout(CustomLayout layout, String propertyId, F field) {
		formatField(field, propertyId);
		field.setId(propertyId);
		layout.addComponent(field.asComponent(), propertyId);
		addDefaultAdditionalValidators(field, null);
		setDefaultAdditionalParameters(field);

		return field;
	}

	@SuppressWarnings("rawtypes")
	protected <T extends Field> T createField(String propertyId, Class<T> fieldType) {
		return getFieldGroup().buildAndBind(propertyId, (Object) propertyId, fieldType);
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field<?>> F addCustomField(FieldConfiguration fieldConfiguration, Class<?> dataType, Class<F> fieldType) {
		F field = addCustomField(fieldConfiguration.getPropertyId(), dataType, fieldType);

		field.setCaption(fieldConfiguration.getCaption());
		setStyleName(field, fieldConfiguration.getStyle());

		return field;
	}

	/**
	 * Vaadin 7 {@code setStyleName}: replaces the style names of the field.
	 */
	@SuppressWarnings("rawtypes")
	protected static void setStyleName(Field field, String style) {
		field.getClassNames().clear();
		field.addStyleName(style);
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> F addField(String propertyId, Class<F> fieldType, FieldWrapper<F> fieldWrapper) {
		return addField(getContent(), propertyId, fieldType, fieldWrapper);
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> F addField(CustomLayout layout, String propertyId, Class<F> fieldType, FieldWrapper<F> fieldWrapper) {
		F field = getFieldGroup().buildAndBind(propertyId, (Object) propertyId, fieldType);
		formatField(field, propertyId);
		field.setId(propertyId);
		// Add validators before wrapping field, so the wrapper can access validators
		addDefaultAdditionalValidators(field, null);
		setDefaultAdditionalParameters(field);
		layout.addComponent(fieldWrapper.wrap(field, Captions.numberOfCharacters), propertyId);
		return field;
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> F addCustomField(String fieldId, Class<?> dataType, Class<F> fieldType) {
		F field = getFieldGroup().getFieldFactory().createField(dataType, fieldType);
		field.setId(fieldId);
		formatField(field, fieldId);
		addDefaultAdditionalValidators(field, dataType);
		setDefaultAdditionalParameters(field);
		getContent().addComponent(field.asComponent(), fieldId);
		customFields.add(field);
		return field;
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> F addCustomField(String fieldId, String customCaption, Class<?> dataType, Class<F> fieldType) {
		F field = getFieldGroup().getFieldFactory().createField(dataType, fieldType);
		field.setId(fieldId);
		formatField(field, fieldId, customCaption);
		addDefaultAdditionalValidators(field, dataType);
		setDefaultAdditionalParameters(field);
		getContent().addComponent(field.asComponent(), fieldId);
		customFields.add(field);
		return field;
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> void addCustomField(F field, String fieldId, String customCaption) {
		field.setId(fieldId);
		formatField(field, fieldId, customCaption);
		addDefaultAdditionalValidators(field, getValueType(field));
		setDefaultAdditionalParameters(field);
		getContent().addComponent(field.asComponent(), fieldId);
		customFields.add(field);
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> F addCustomField(String fieldId, Class<?> dataType, Class<F> fieldType, String customCaption) {
		F field = getFieldGroup().getFieldFactory().createField(dataType, fieldType);
		field.setId(fieldId);
		formatField(field, fieldId, customCaption);

		addDefaultAdditionalValidators(field, dataType);
		setDefaultAdditionalParameters(field);
		getContent().addComponent(field.asComponent(), fieldId);
		customFields.add(field);
		return field;
	}

	/**
	 * Vaadin 7 {@code Field.getType()}: the value type of a field. {@link CustomField}s declare it; for the other fields
	 * it is the type of their empty value ({@code String} for text fields), else {@code Object}.
	 */
	@SuppressWarnings("rawtypes")
	protected static Class<?> getValueType(Field field) {
		if (field instanceof CustomField && ((CustomField) field).getType() != null) {
			return ((CustomField) field).getType();
		}
		Object emptyValue = field.getEmptyValue();
		return emptyValue != null ? emptyValue.getClass() : Object.class;
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> void formatField(F field, String propertyId) {
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> void formatField(F field, String propertyId, String customCaption) {
	}

	@SuppressWarnings("rawtypes")
	/**
	 * @param allowedDaysInFuture
	 *            How many days in the future the value of this field can be or
	 *            -1 for no restriction at all
	 */
	protected <F extends Field> F addDateField(String propertyId, Class<F> fieldType, int allowedDaysInFuture) {
		F field = createField(propertyId, fieldType);
		formatField(field, propertyId);
		field.setId(propertyId);
		getContent().addComponent(field.asComponent(), propertyId);
		addFutureDateValidator(field, allowedDaysInFuture);
		return field;
	}

	public void addBirthDateFields(CustomLayout moreFiltersContainer, String yearPropertyId, String monthPropertyId, String dayPropertyId) {
		final ComboBox birthDateYYYY = addField(
			moreFiltersContainer,
			ComboBox.class,
			FieldConfiguration
				.withCaptionAndPixelSized(yearPropertyId, I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, PersonDto.BIRTH_DATE_YYYY), 140));

		// was ItemCaptionMode.ID_TOSTRING: items without an explicit caption show toString()
		birthDateYYYY.addItems(DateHelper.getYearsToNow());

		final ComboBox birthDateMM = addField(
			moreFiltersContainer,
			ComboBox.class,
			FieldConfiguration
				.withCaptionAndPixelSized(monthPropertyId, I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, PersonDto.BIRTH_DATE_MM), 140));
		birthDateMM.addItems(DateHelper.getMonthsInYear());
		DateHelper.getMonthsInYear().forEach(month -> birthDateMM.setItemCaption(month, Month.values()[month - 1].toString()));

		final ComboBox birthDateDD = addField(
			moreFiltersContainer,
			ComboBox.class,
			FieldConfiguration
				.withCaptionAndPixelSized(dayPropertyId, I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, PersonDto.BIRTH_DATE_DD), 140));
	}

	@SuppressWarnings("rawtypes")
	/**
	 * @param fieldDataType
	 *            - must be specified if the current FieldGroup does not know about the field
	 */
	protected <F extends Field> F addDefaultAdditionalValidators(F field, Class<?> fieldDataType) {
		addLengthValidator(field, fieldDataType);
		addMinMaxValidator(field, fieldDataType);
		addFutureDateValidator(field, 0);
		return field;
	}

	@SuppressWarnings("rawtypes")
	/**
	 * Add additional Parameters to fields, like setting the filteringMode for ComboBoxes
	 *
	 * @param field
	 */
	protected <F extends Field> F setDefaultAdditionalParameters(F field) {
		if (field instanceof ComboBox) {
			((ComboBox) field).setFilteringMode(FilteringMode.CONTAINS);
		}
		return field;
	}

	/**
	 * @param fieldDataType
	 *            - must be specified if the current FieldGroup does not know about the field
	 */
	private <F extends Field<?>> void addLengthValidator(F field, Class<?> fieldDataType) {
		final Class<?> typeOfFieldData;
		if (fieldDataType != null) {
			typeOfFieldData = fieldDataType;
		} else {
			typeOfFieldData = fieldGroup.getPropertyType(fieldGroup.getPropertyId(field));
		}

		if (typeOfFieldData.equals(String.class)) {
			Integer maxLength = getPropertyMaxLength(field.getId().orElse(null));
			if (maxLength != null) {
				field.addValidator(new MaxLengthValidator(maxLength));
			} else {
				final Class<?> fieldType = field.getClass();
				if (fieldType.isAssignableFrom(TextArea.class)) {
					field.addValidator(new MaxLengthValidator(SormasFieldGroupFieldFactory.TEXT_AREA_MAX_LENGTH));
				} else if (fieldType.isAssignableFrom(TextField.class)) {
					field.addValidator(new MaxLengthValidator(SormasFieldGroupFieldFactory.TEXT_FIELD_MAX_LENGTH));
				}
			}
		}

	}

	private Integer getPropertyMaxLength(String propertyId) {
		Size sizeAnnotation = getPropertyAnnotation(propertyId, Size.class);
		return sizeAnnotation != null ? sizeAnnotation.max() : null;
	}

	private <T extends Annotation> T getPropertyAnnotation(String propertyId, Class<T> annotationType) {
		if (propertyId == null) {
			return null;
		}
		try {
			java.lang.reflect.Field field = type.getDeclaredField(propertyId);

			if (!field.isAnnotationPresent(annotationType)) {
				return null;
			}

			return field.getAnnotation(annotationType);
		} catch (NoSuchFieldException e) {
			return null;
		}
	}

	private <F extends Field<?>> void addMinMaxValidator(F field, Class<?> fieldDataType) {
		String propertyId = field.getId().orElse(null);
		Max maxAnnotation = getPropertyAnnotation(propertyId, Max.class);
		Long maxValue = maxAnnotation != null ? maxAnnotation.value() : null;

		Min minAnnotation = getPropertyAnnotation(propertyId, Min.class);
		Long minValue = minAnnotation != null ? minAnnotation.value() : null;

		if (minValue != null || maxValue != null) {
			String validationMessageTag;
			Map<String, Object> validationMessageArgs = new HashMap<>();
			if (minValue == null) {
				validationMessageTag = Validations.numberTooBig;
				validationMessageArgs.put("value", maxValue);
			} else if (maxValue == null) {
				validationMessageTag = Validations.numberTooSmall;
				validationMessageArgs.put("value", minValue);
			} else {
				validationMessageTag = Validations.numberNotInRange;
				validationMessageArgs.put("min", minValue);
				validationMessageArgs.put("max", maxValue);
			}

			field.addValidator(
				new NumberValidator(I18nProperties.getValidationError(validationMessageTag, validationMessageArgs), minValue, maxValue));
		}
	}

	@SuppressWarnings("rawtypes")
	protected <F extends Field> F addFutureDateValidator(F field, int amountOfDays) {
		if (amountOfDays < 0) {
			return field;
		}

		if (DateField.class.isAssignableFrom(field.getClass()) || DateTimeField.class.isAssignableFrom(field.getClass())) {
			field.addValidator(new FutureDateValidator(field, amountOfDays, field.getCaption()));
		}

		return field;
	}

	@SuppressWarnings("unchecked")
	public <T extends Field<?>> T getField(String fieldOrPropertyId) {
		Field<?> field = getFieldGroup().getField(fieldOrPropertyId);
		if (field == null) {
			// try to get the field from the layout
			Component component = getContent().getComponent(fieldOrPropertyId);
			if (component instanceof Field<?>) {
				field = (Field<?>) component;
			}
		}
		return (T) field;
	}

	/**
	 * Was {@code getInternalValue()}: the DTO being edited.
	 */
	@Override
	protected T generateModelValue() {
		return getFieldGroup().getBean();
	}

	/**
	 * Was {@code setInternalValue(T)}: the fields show the new DTO (buffered, as the Vaadin 7 field group).
	 */
	@Override
	protected void setPresentationValue(T newValue) {
		getFieldGroup().setItemDataSource(newValue);
	}

	/**
	 * A form shows the DTO instance it is given: a different instance is read again even if it is equal (DTOs are equal
	 * by uuid). Vaadin 7 read a form bound to a property of another form again on every read of its field group.
	 */
	@Override
	protected boolean valueEquals(T value1, T value2) {
		return value1 == value2;
	}

	/**
	 * Vaadin 7 {@code fireValueChange(boolean)}: tells the value change listeners that the DTO changed in place.
	 */
	protected void fireValueChange(boolean repaintIsNotNeeded) {
		fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), false));
	}

	// Buffered: the form as a field has no buffer of its own (Vaadin 7 AbstractField without data source)

	@Override
	public void commit() throws de.symeda.sormas.ui.utils.InvalidValueException, SourceException {
	}

	@Override
	public void discard() throws SourceException {
	}

	@Override
	public boolean isModified() {
		return false;
	}

	@Override
	public void setBuffered(boolean buffered) {
		this.buffered = buffered;
	}

	@Override
	public boolean isBuffered() {
		return buffered;
	}

	/**
	 * Vaadin 7 {@code isValid()} of the form as a field (its own validators, not those of its fields), as
	 * {@link Field#isValid()}.
	 */
	@Override
	public boolean isValid() {
		try {
			validateValue();
			return true;
		} catch (Field.InvalidValueException e) {
			return false;
		}
	}

	public void setHeightUndefined() {
		setHeight(null);
	}

	public void setWidthUndefined() {
		setWidth(null);
	}

	protected String getPropertyI18nPrefix() {
		return propertyI18nPrefix;
	}

	protected boolean isConfiguredServer(String countryCode) {
		return FacadeProvider.getConfigFacade().isConfiguredCountry(countryCode);
	}

	/**
	 * The Vaadin 7 {@code BeanFieldGroup} on a Flow {@link Binder}: fields are bound by property id, the bean is read
	 * into the fields ({@link Binder#readBean}) and written on {@link #commit()} ({@link Binder#writeBean}), with the
	 * Vaadin 7 rules:
	 * <ul>
	 * <li>commit runs the commit handlers, validates every bound field (also invisible ones), writes nothing if a field is
	 * invalid and throws one {@link de.symeda.sormas.ui.utils.InvalidValueException} whose causes are the invalid fields' errors;</li>
	 * <li>a field with {@code setInvalidCommitted(true)} is written even if invalid, and shows its error;</li>
	 * <li>a bound field that is itself {@link Buffered} (a form bound to a property of this form) commits and discards
	 * its own fields first and counts for {@link #isModified()}; a bound form commits even if another field of this group
	 * is invalid (Vaadin 7 committed field by field, and a bound form's commit wrote its bean at once);</li>
	 * <li>{@link #isModified()} is the Vaadin 7 modified state: any value change since the last read, commit or
	 * discard;</li>
	 * <li>errors are shown on a field only while {@code isValidationVisible()} and not for a required empty field (Vaadin
	 * 7 showed only the required indicator there);</li>
	 * <li>a property without setter makes its field read-only.</li>
	 * </ul>
	 */
	public static class BeanFieldGroup<T> implements FieldGroup, Buffered {

		private static final long serialVersionUID = 7271384537227612872L;

		private final Class<T> beanType;
		private final Binder<T> binder;
		private final PropertySet<T> propertySet;
		private final Map<Object, Field<?>> fieldsByPropertyId = new LinkedHashMap<>();
		private final Map<Field<?>, Binder.Binding<T, ?>> bindings = new LinkedHashMap<>();
		private final Set<Field<?>> readOnlyPropertyFields = new HashSet<>();
		private final List<CommitHandler> commitHandlers = new ArrayList<>();
		private SormasFieldGroupFieldFactory fieldFactory;
		private T bean;
		private boolean buffered = true;
		private boolean readOnly;
		private boolean enabled = true;
		/**
		 * Vaadin 7 modified state: set by any value change of a bound field (also a programmatic one, also back to the
		 * loaded value), cleared when the bean is read, committed or discarded. The changed field is marked too
		 * ({@link Field#isModified()}).
		 */
		private boolean modified;

		public BeanFieldGroup(Class<T> beanType) {
			this.beanType = beanType;
			this.binder = new Binder<>(beanType);
			this.propertySet = BeanPropertySet.get(beanType);
			binder.addValueChangeListener(e -> {
				modified = true;
				if (bindings.containsKey(e.getHasValue())) {
					setFieldModified((Field<?>) e.getHasValue(), true);
				}
			});
		}

		private static void setFieldModified(Field<?> field, boolean fieldModified) {
			ComponentUtil.setData(field.asComponent(), Field.DATA_MODIFIED, fieldModified ? Boolean.TRUE : null);
		}

		private void clearModified() {
			modified = false;
			bindings.keySet().forEach(f -> setFieldModified(f, false));
		}

		/**
		 * Vaadin 7 {@code field.discard()} of one bound field: it is no longer {@link Field#isModified() modified}.
		 */
		public void clearModified(Field<?> field) {
			setFieldModified(field, false);
		}

		public Binder<T> getBinder() {
			return binder;
		}

		public Class<T> getBeanType() {
			return beanType;
		}

		public SormasFieldGroupFieldFactory getFieldFactory() {
			return fieldFactory;
		}

		public void setFieldFactory(SormasFieldGroupFieldFactory fieldFactory) {
			this.fieldFactory = fieldFactory;
		}

		/**
		 * @return the type of the bean property (primitive types as their wrapper type)
		 */
		public Class<?> getPropertyType(Object propertyId) {
			return wrapperType(getDeclaredPropertyType(propertyId));
		}

		private Class<?> getDeclaredPropertyType(Object propertyId) {
			if (propertyId == null) {
				throw new IllegalArgumentException("Property id must not be null");
			}
			PropertyDefinition<T, ?> property = propertySet.getProperty(propertyId.toString())
				.orElseThrow(() -> new IllegalArgumentException("Property " + propertyId + " not found in " + beanType.getName()));
			return property.getType();
		}

		/**
		 * The property type the field factory gets, as in Vaadin 7: {@code BeanFieldGroup.getPropertyType} returned the
		 * declared type (a primitive stays primitive) while no bean was set, and the wrapper type once a bean was set. The
		 * factory makes a check box for a primitive {@code boolean} and a Yes/No option group for a {@code Boolean}.
		 */
		private Class<?> getFieldFactoryType(Object propertyId) {
			return bean != null ? getPropertyType(propertyId) : getDeclaredPropertyType(propertyId);
		}

		private static Class<?> wrapperType(Class<?> type) {
			if (!type.isPrimitive()) {
				return type;
			} else if (type == boolean.class) {
				return Boolean.class;
			} else if (type == int.class) {
				return Integer.class;
			} else if (type == long.class) {
				return Long.class;
			} else if (type == double.class) {
				return Double.class;
			} else if (type == float.class) {
				return Float.class;
			} else if (type == short.class) {
				return Short.class;
			} else if (type == byte.class) {
				return Byte.class;
			} else if (type == char.class) {
				return Character.class;
			}
			return type;
		}

		/**
		 * Vaadin 7 {@code buildAndBind(caption, propertyId, fieldType)}: creates the field with the field factory for the
		 * property type, sets the caption and binds it.
		 */
		@SuppressWarnings("rawtypes")
		public <F extends Field> F buildAndBind(String caption, Object propertyId, Class<F> fieldType) {
			Class<?> propertyType = getFieldFactoryType(propertyId);
			F field = fieldFactory.createField(propertyType, fieldType);
			if (field == null) {
				throw new IllegalStateException("Unable to build a field of type " + fieldType.getName() + " for editing " + propertyType.getName());
			}
			field.setCaption(caption);
			bind(field, propertyId);
			return field;
		}

		@SuppressWarnings("rawtypes")
		public <F extends Field> F buildAndBind(Object propertyId, Class<F> fieldType) {
			return buildAndBind(propertyId.toString(), propertyId, fieldType);
		}

		/**
		 * Binds the field to the bean property (also a nested one, {@code "a.b"}) through the field's binding converter
		 * and validator (see {@link Field#asBindingConverter(Class)}).
		 */
		@SuppressWarnings({
			"unchecked",
			"rawtypes" })
		public void bind(Field<?> field, Object propertyId) {

			if (fieldsByPropertyId.containsKey(propertyId)) {
				throw new IllegalStateException("Property id " + propertyId + " is already bound to another field");
			}
			if (bindings.containsKey(field)) {
				throw new IllegalStateException("Field is already bound to property id " + getPropertyId(field));
			}

			Class<?> propertyType = getPropertyType(propertyId);
			Field rawField = field;
			Binder.Binding<T, ?> binding = binder.forField((HasValue) rawField.asHasValue())
				.withConverter(rawField.asBindingConverter(propertyType))
				.withValidator(rawField.asBindingValidator())
				.withValidationStatusHandler(status -> showValidationStatus(rawField, status))
				.bind(propertyId.toString());
			// Vaadin 7 validated and wrote invisible fields too
			binding.setIsAppliedPredicate(b -> true);

			fieldsByPropertyId.put(propertyId, field);
			bindings.put(field, binding);

			if (binding.getSetter() == null || binding.isReadOnly()) {
				readOnlyPropertyFields.add(field);
			}
			configureField(field);

			if (bean != null) {
				// reading a field is not a change
				boolean wasModified = modified;
				binding.read(bean);
				modified = wasModified;
			}
		}

		private void configureField(Field<?> field) {
			if (!enabled) {
				field.setEnabled(false);
			}
			if (readOnlyPropertyFields.contains(field) || readOnly) {
				field.setReadOnly(true);
			}
		}

		@SuppressWarnings("rawtypes")
		private static void showValidationStatus(Field field, BindingValidationStatus<?> status) {
			if (!(field instanceof HasValidation)) {
				return;
			}
			HasValidation hasValidation = (HasValidation) field;
			boolean show = status.isError() && field.isValidationVisible() && !(field.isRequired() && field.isEmpty());
			hasValidation.setErrorMessage(show ? status.getMessage().orElse(null) : null);
			hasValidation.setInvalid(show);
		}

		public void unbind(Field<?> field) {
			Binder.Binding<T, ?> binding = bindings.remove(field);
			if (binding == null) {
				throw new IllegalArgumentException("The given field is not bound");
			}
			binding.unbind();
			setFieldModified(field, false);
			fieldsByPropertyId.values().remove(field);
			readOnlyPropertyFields.remove(field);
		}

		@Override
		public Field<?> getField(Object propertyId) {
			return fieldsByPropertyId.get(propertyId);
		}

		@Override
		public Object getPropertyId(Field<?> field) {
			for (Map.Entry<Object, Field<?>> entry : fieldsByPropertyId.entrySet()) {
				if (entry.getValue() == field) {
					return entry.getKey();
				}
			}
			return null;
		}

		@Override
		public Collection<Object> getBoundPropertyIds() {
			return new ArrayList<>(fieldsByPropertyId.keySet());
		}

		@Override
		public List<Field<?>> getFields() {
			return new ArrayList<>(bindings.keySet());
		}

		public Binder.Binding<T, ?> getBinding(Field<?> field) {
			return bindings.get(field);
		}

		/**
		 * Vaadin 7 {@code setItemDataSource}: the fields show the bean's values. In buffered mode the bean is changed only
		 * by {@link #commit()}.
		 */
		public void setItemDataSource(T bean) {
			this.bean = bean;
			if (buffered) {
				binder.readBean(bean);
			} else {
				binder.setBean(bean);
			}
			clearModified();
		}

		/**
		 * Was {@code getItemDataSource().getBean()}.
		 */
		public T getBean() {
			return bean;
		}

		public void addCommitHandler(CommitHandler commitHandler) {
			commitHandlers.add(commitHandler);
		}

		public void removeCommitHandler(CommitHandler commitHandler) {
			commitHandlers.remove(commitHandler);
		}

		/**
		 * Vaadin 7 {@code FieldGroup.commit()}: pre-commit handlers, validation of all fields, writing the bean, post-commit
		 * handlers. A {@link CommitException} of a handler becomes its cause if that is an {@link de.symeda.sormas.ui.utils.InvalidValueException},
		 * else a {@link SourceException}.
		 *
		 * @throws de.symeda.sormas.ui.utils.InvalidValueException
		 *             a {@link FieldGroupInvalidValueException} with one cause per invalid field; nothing was written
		 */
		@Override
		public void commit() throws de.symeda.sormas.ui.utils.InvalidValueException, SourceException {

			if (!buffered) {
				return;
			}

			CommitEvent event = new CommitEvent(this);
			try {
				for (CommitHandler handler : new ArrayList<>(commitHandlers)) {
					handler.preCommit(event);
				}
			} catch (CommitException e) {
				throw toRuntimeException(e);
			}

			if (bean != null) {
				Map<Field<?>, de.symeda.sormas.ui.utils.InvalidValueException> invalidFields = validateForCommit();
				if (!invalidFields.isEmpty()) {
					throw new FieldGroupInvalidValueException(invalidFields);
				}
				write();
				clearModified();
			}

			try {
				for (CommitHandler handler : new ArrayList<>(commitHandlers)) {
					handler.postCommit(event);
				}
			} catch (CommitException e) {
				throw toRuntimeException(e);
			}
		}

		private Map<Field<?>, de.symeda.sormas.ui.utils.InvalidValueException> validateForCommit() {

			Map<Field<?>, de.symeda.sormas.ui.utils.InvalidValueException> invalidFields = new LinkedHashMap<>();
			for (Map.Entry<Field<?>, Binder.Binding<T, ?>> entry : bindings.entrySet()) {
				Field<?> field = entry.getKey();
				Binder.Binding<T, ?> binding = entry.getValue();
				if (readOnlyPropertyFields.contains(field)) {
					// Vaadin 7 did not commit fields of read-only properties
					continue;
				}

				if (field instanceof Buffered) {
					try {
						((Buffered) field).commit();
					} catch (de.symeda.sormas.ui.utils.InvalidValueException e) {
						invalidFields.put(field, e);
						continue;
					}
				}

				BindingValidationStatus<?> status = binding.validate(true);
				if (status.isError() && !field.isInvalidCommitted()) {
					invalidFields.put(field, new de.symeda.sormas.ui.utils.InvalidValueException(status.getMessage().orElse("")));
				}
			}
			return invalidFields;
		}

		private void write() {

			List<Binder.Binding<T, ?>> invalidCommitted = new ArrayList<>();
			for (Map.Entry<Field<?>, Binder.Binding<T, ?>> entry : bindings.entrySet()) {
				if (entry.getKey().isInvalidCommitted() && !entry.getValue().isValidatorsDisabled()) {
					invalidCommitted.add(entry.getValue());
				}
			}

			invalidCommitted.forEach(b -> b.setValidatorsDisabled(true));
			try {
				binder.writeBean(bean);
			} catch (ValidationException e) {
				// only conversion errors of invalid committed fields get here
				List<de.symeda.sormas.ui.utils.InvalidValueException> causes = new ArrayList<>();
				e.getFieldValidationErrors().forEach(s -> causes.add(new de.symeda.sormas.ui.utils.InvalidValueException(s.getMessage().orElse(""))));
				throw new de.symeda.sormas.ui.utils.InvalidValueException(null, causes.toArray(new de.symeda.sormas.ui.utils.InvalidValueException[0]));
			} finally {
				invalidCommitted.forEach(b -> {
					b.setValidatorsDisabled(false);
					// show the error again, as Vaadin 7 did for invalid committed fields
					b.validate(true);
				});
			}
		}

		private static RuntimeException toRuntimeException(CommitException e) {
			if (e.getCause() instanceof de.symeda.sormas.ui.utils.InvalidValueException) {
				return (de.symeda.sormas.ui.utils.InvalidValueException) e.getCause();
			} else if (e.getCause() instanceof SourceException) {
				return (SourceException) e.getCause();
			}
			return new SourceException(e);
		}

		/**
		 * Vaadin 7 {@code FieldGroup.discard()}: the fields show the bean's values again.
		 */
		@Override
		public void discard() throws SourceException {
			for (Field<?> field : bindings.keySet()) {
				if (field instanceof Buffered) {
					((Buffered) field).discard();
				}
			}
			if (buffered && bean != null) {
				binder.readBean(bean);
			}
			clearModified();
		}

		/**
		 * Vaadin 7 {@code FieldGroup.isModified()}: a bound field changed since the bean was read, committed or
		 * discarded, or a bound form is modified. (Not {@code Binder.hasChanges()}, which forgets a field changed back to
		 * its loaded value.)
		 */
		@Override
		public boolean isModified() {
			if (modified) {
				return true;
			}
			for (Field<?> field : bindings.keySet()) {
				if (field instanceof Buffered && ((Buffered) field).isModified()) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Vaadin 7 {@code FieldGroup.isValid()}: all bound fields are valid (fields of a bound form are not checked, as
		 * in Vaadin 7).
		 */
		@Override
		public boolean isValid() {
			for (Binder.Binding<T, ?> binding : bindings.values()) {
				if (binding.validate(false).isError()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public void setBuffered(boolean buffered) {
			if (this.buffered == buffered) {
				return;
			}
			this.buffered = buffered;
			// re-reading the bean into the fields is not a change: the modified state of the group and of each field stays as
			// it was (V7 setBuffered did not touch it)
			boolean wasModified = modified;
			Set<Field<?>> modifiedFields = new HashSet<>();
			bindings.keySet().forEach(f -> {
				if (ComponentUtil.getData(f.asComponent(), Field.DATA_MODIFIED) != null) {
					modifiedFields.add(f);
				}
			});
			if (buffered) {
				binder.removeBean();
				binder.readBean(bean);
			} else {
				binder.setBean(bean);
			}
			modified = wasModified;
			bindings.keySet().forEach(f -> setFieldModified(f, modifiedFields.contains(f)));
		}

		@Override
		public boolean isBuffered() {
			return buffered;
		}

		@Override
		public boolean isReadOnly() {
			return readOnly;
		}

		/**
		 * Vaadin 7: sets all bound fields read-only; fields of read-only properties stay read-only.
		 */
		public void setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
			for (Field<?> field : bindings.keySet()) {
				if (readOnly || !readOnlyPropertyFields.contains(field)) {
					field.setReadOnly(readOnly);
				}
			}
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
			for (Field<?> field : bindings.keySet()) {
				field.setEnabled(enabled);
			}
		}

		/**
		 * Vaadin 7 {@code FieldGroup.CommitHandler}.
		 */
		public interface CommitHandler extends java.io.Serializable {

			void preCommit(CommitEvent commitEvent) throws CommitException;

			void postCommit(CommitEvent commitEvent) throws CommitException;
		}

		/**
		 * Vaadin 7 {@code FieldGroup.CommitEvent}.
		 */
		public static class CommitEvent implements java.io.Serializable {

			private static final long serialVersionUID = 1L;

			private final BeanFieldGroup<?> fieldBinder;

			public CommitEvent(BeanFieldGroup<?> fieldBinder) {
				this.fieldBinder = fieldBinder;
			}

			public BeanFieldGroup<?> getFieldBinder() {
				return fieldBinder;
			}
		}

		/**
		 * Vaadin 7 {@code FieldGroup.CommitException}, thrown by commit handlers.
		 */
		public static class CommitException extends Exception {

			private static final long serialVersionUID = 1L;

			public CommitException(String message) {
				super(message);
			}

			public CommitException(String message, Throwable cause) {
				super(message, cause);
			}

			public CommitException(Throwable cause) {
				super(cause);
			}
		}

		/**
		 * Vaadin 7 {@code FieldGroupInvalidValueException}: no message, one cause per invalid field.
		 */
		public static class FieldGroupInvalidValueException extends de.symeda.sormas.ui.utils.InvalidValueException {

			private static final long serialVersionUID = 1L;

			private final transient Map<Field<?>, de.symeda.sormas.ui.utils.InvalidValueException> invalidFields;

			public FieldGroupInvalidValueException(Map<Field<?>, de.symeda.sormas.ui.utils.InvalidValueException> invalidFields) {
				super(null, invalidFields.values().toArray(new de.symeda.sormas.ui.utils.InvalidValueException[0]));
				this.invalidFields = invalidFields;
			}

			public Map<Field<?>, de.symeda.sormas.ui.utils.InvalidValueException> getInvalidFields() {
				return invalidFields;
			}
		}
	}
}
