package de.symeda.sormas.ui.contact.components.linelisting.contactfield;

import java.time.LocalDate;
import java.util.Arrays;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.contact.ContactDto;
import de.symeda.sormas.api.contact.ContactProximity;
import de.symeda.sormas.api.contact.ContactRelation;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.contact.components.linelisting.MultiDayContactField;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.fields.CustomField;
import de.symeda.sormas.ui.utils.components.linelisting.person.PersonField;
import de.symeda.sormas.ui.utils.components.linelisting.person.PersonFieldDto;
import de.symeda.sormas.ui.utils.components.multidayselector.MultiDaySelectorDto;

public class ContactLineField extends CustomField<ContactLineFieldDto> {

	private static final long serialVersionUID = 7634199030865304458L;

	private final Binder<ContactLineFieldDto> binder = new Binder<>(ContactLineFieldDto.class);

	private final DatePicker dateOfReport;
	private final MultiDayContactField multiDay;
	private final ComboBox<ContactProximity> typeOfContact;
	private final ComboBox<ContactRelation> relationToCase;
	private final PersonField person;

	public ContactLineField() {
		dateOfReport = new DatePicker();
		multiDay = new MultiDayContactField(dateOfReport);
		typeOfContact = new ComboBox<>();
		relationToCase = new ComboBox<>();
		person = new PersonField();
	}

	/**
	 * Built on first attach, as in Vaadin 8 ({@link CustomField}).
	 */
	@Override
	protected Component initContent() {
		HorizontalLayout layout = new HorizontalLayout();

		layout.setPadding(false);
		layout.setSpacing(false);

		dateOfReport.setId("dateOfReport");
		dateOfReport.setWidth("150px");
		binder.forField(dateOfReport).asRequired().bind(ContactLineFieldDto.DATE_OF_REPORT);
		dateOfReport.setMax(LocalDate.now());

		multiDay.setId("multiDay");
		binder.forField(multiDay).bind(ContactLineFieldDto.MULTI_DAY_SELECTOR);

		typeOfContact.setId("typeOfContact");
		typeOfContact.setWidth("270px");
		typeOfContact.addClassName(CssStyles.CAPTION_OVERFLOW);
		binder.forField(typeOfContact).bind(ContactLineFieldDto.TYPE_OF_CONTACT);

		relationToCase.setId("relationToCase");
		relationToCase.setItems(ContactRelation.values());
		relationToCase.setWidth("270px");
		relationToCase.addClassName(CssStyles.CAPTION_OVERFLOW);
		binder.forField(relationToCase).bind(ContactLineFieldDto.RELATION_TO_CASE);

		person.setId("person");
		binder.forField(person).bind(ContactLineFieldDto.PERSON);

		layout.add(dateOfReport, multiDay, typeOfContact, relationToCase, person);

		// was Alignment.BOTTOM_LEFT
		layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, dateOfReport, typeOfContact, relationToCase, person);

		return layout;
	}

	public void updateTypeOfContactValues(Disease disease) {
		ContactProximity[] values = ContactProximity.getValues(disease, FacadeProvider.getConfigFacade().getCountryLocale());
		typeOfContact.setItems(values);
		if (!Arrays.asList(values).contains(typeOfContact.getValue())) {
			typeOfContact.setValue(null);
		}
	}

	@Override
	protected void setPresentationValue(ContactLineFieldDto contactFieldDto) {
		binder.setBean(contactFieldDto);
	}

	@Override
	protected ContactLineFieldDto generateModelValue() {
		return binder.getBean();
	}

	@Override
	public ContactLineFieldDto getValue() {
		return binder.getBean();
	}

	public boolean hasErrors() {
		BinderValidationStatus<MultiDaySelectorDto> contactDatesStatus = multiDay.validate();
		BinderValidationStatus<PersonFieldDto> personValidationStatus = person.validate();
		BinderValidationStatus<ContactLineFieldDto> lineValidationStatus = binder.validate();
		return contactDatesStatus.hasErrors() || personValidationStatus.hasErrors() || lineValidationStatus.hasErrors();
	}

	public void showCaptions() {
		dateOfReport.setLabel(I18nProperties.getPrefixCaption(CaseDataDto.I18N_PREFIX, CaseDataDto.REPORT_DATE));
		dateOfReport.removeClassName(CssStyles.CAPTION_HIDDEN);
		multiDay.showCaptions();
		typeOfContact.setLabel(I18nProperties.getPrefixCaption(ContactDto.I18N_PREFIX, ContactDto.CONTACT_PROXIMITIES));
		typeOfContact.removeClassName(CssStyles.CAPTION_HIDDEN);
		relationToCase.setLabel(I18nProperties.getPrefixCaption(ContactDto.I18N_PREFIX, ContactDto.RELATION_TO_CASE));
		relationToCase.removeClassName(CssStyles.CAPTION_HIDDEN);
		person.showCaptions();
	}

	public void enablePersonField(boolean shouldEnable) {
		person.setEnabled(shouldEnable);
	}
}
