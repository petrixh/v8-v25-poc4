package de.symeda.sormas.ui.utils.components.linelisting.person;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.person.Sex;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.fields.CustomField;
import de.symeda.sormas.ui.utils.components.birthdate.BirthDateField;

public class PersonField extends CustomField<PersonFieldDto> {

	private final Binder<PersonFieldDto> binder = new Binder<>(PersonFieldDto.class);

	private final TextField firstname;
	private final TextField lastname;
	private final BirthDateField birthDate;
	private final ComboBox<Sex> sex;

	public PersonField() {
		firstname = new TextField();
		lastname = new TextField();
		birthDate = new BirthDateField();
		sex = new ComboBox<>();
	}

	/**
	 * Built on first attach, as in Vaadin 8 ({@link CustomField}).
	 */
	@Override
	protected Component initContent() {
		if (getValue() == null) {
			setValue(new PersonFieldDto());
		}

		HorizontalLayout layout = new HorizontalLayout();

		firstname.setId("firstName");
		binder.forField(firstname).asRequired().bind(PersonFieldDto.FIRST_NAME);

		lastname.setId("lastName");
		binder.forField(lastname).asRequired().bind(PersonFieldDto.LAST_NAME);

		birthDate.setId("birthDate");
		binder.forField(birthDate).bind(PersonFieldDto.BIRTH_DATE);

		sex.setId("sex");
		sex.setItems(Sex.values());
		sex.setWidth("100px");
		binder.forField(sex).asRequired().bind(PersonFieldDto.SEX);

		layout.add(firstname, lastname, birthDate, sex);

		return layout;
	}

	@Override
	protected void setPresentationValue(PersonFieldDto personFieldDto) {
		binder.setBean(personFieldDto);
	}

	@Override
	protected PersonFieldDto generateModelValue() {
		return binder.getBean();
	}

	@Override
	public PersonFieldDto getValue() {
		return binder.getBean();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		firstname.setEnabled(enabled);
		lastname.setEnabled(enabled);
		birthDate.setEnabled(enabled);
		sex.setEnabled(enabled);
	}

	public BinderValidationStatus<PersonFieldDto> validate() {
		return binder.validate();
	}

	public void showCaptions() {
		firstname.setLabel(I18nProperties.getPrefixCaption(PersonFieldDto.I18N_PREFIX, PersonFieldDto.FIRST_NAME));
		firstname.removeClassName(CssStyles.CAPTION_HIDDEN);
		lastname.setLabel(I18nProperties.getPrefixCaption(PersonFieldDto.I18N_PREFIX, PersonFieldDto.LAST_NAME));
		lastname.removeClassName(CssStyles.CAPTION_HIDDEN);
		birthDate.setLabel(I18nProperties.getPrefixCaption(PersonFieldDto.I18N_PREFIX, PersonFieldDto.BIRTH_DATE));
		sex.setLabel(I18nProperties.getPrefixCaption(PersonFieldDto.I18N_PREFIX, PersonFieldDto.SEX));
		sex.removeClassName(CssStyles.CAPTION_HIDDEN);
	}

	public void hideCaptions() {
		CssStyles.style(firstname, CssStyles.CAPTION_HIDDEN);
		CssStyles.style(lastname, CssStyles.CAPTION_HIDDEN);
		CssStyles.style(sex, CssStyles.CAPTION_HIDDEN);
	}
}
