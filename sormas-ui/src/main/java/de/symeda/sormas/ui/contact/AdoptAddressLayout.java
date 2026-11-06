package de.symeda.sormas.ui.contact;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseReferenceDto;
import de.symeda.sormas.api.contact.ContactDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.location.LocationEditForm;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.fields.CheckBox;

public class AdoptAddressLayout extends HorizontalLayout {

	ContactDto contact;
	CaseReferenceDto caseReference;
	CheckBox adoptAddress;
	Button showAddressButton;

	public AdoptAddressLayout() {
		adoptAddress = new CheckBox(I18nProperties.getCaption(Captions.adoptHomeAddressOfCasePerson));
		showAddressButton = ButtonHelper.createIconButton(VaadinIcon.EYE);
		showAddressButton.addClickListener(e -> showAddressOfCasePerson(contact.getCaze()));
		// was setStyleName(BUTTON_BORDERLESS): replaces the button's styles
		CssStyles.style(showAddressButton, CssStyles.BUTTON_BORDERLESS);
		add(adoptAddress, showAddressButton);
		setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, adoptAddress);
	}

	public AdoptAddressLayout(CaseReferenceDto caseReference) {
		this.caseReference = caseReference;
		adoptAddress = new CheckBox(I18nProperties.getCaption(Captions.adoptHomeAddressOfCasePersonIfRelationMatches));
		showAddressButton = ButtonHelper.createIconButton(VaadinIcon.EYE);
		showAddressButton.addClickListener(e -> showAddressOfCasePerson(this.caseReference));
		// was setStyleName(BUTTON_BORDERLESS): replaces the button's styles
		CssStyles.style(showAddressButton, CssStyles.BUTTON_BORDERLESS);
		add(adoptAddress, showAddressButton);
		setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, adoptAddress);
	}

	private void showAddressOfCasePerson(CaseReferenceDto caseReference) {
		LocationEditForm locationForm = new LocationEditForm(
			FieldVisibilityCheckers.withCountry(FacadeProvider.getConfigFacade().getCountryLocale()),
			UiFieldAccessCheckers.getNoop());
		locationForm.setValue(
			FacadeProvider.getPersonFacade()
				.getByUuid(FacadeProvider.getCaseFacade().getByUuid(caseReference.getUuid()).getPerson().getUuid())
				.getAddress());
		locationForm.setEnabled(false);
		HorizontalLayout layout = new HorizontalLayout(locationForm);
		layout.setPadding(true);
		Dialog window = VaadinUiUtil.showPopupWindow(layout);
		window.setHeaderTitle(I18nProperties.getCaption(Captions.casePersonAddress));
	}

	public boolean isAdoptAddress() {
		return adoptAddress.getValue();
	}

	public void setAdoptAddress(boolean adoptAddress) {
		this.adoptAddress.setValue(adoptAddress);
	}

	public void setCaseReference(CaseReferenceDto caseReference) {
		this.caseReference = caseReference;
	}

	public void setContact(ContactDto contact) {
		this.contact = contact;
	}
}
