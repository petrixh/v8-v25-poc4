package de.symeda.sormas.ui.contact;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;

import de.symeda.sormas.api.contact.ContactDto;
import de.symeda.sormas.api.contact.ContactSimilarityCriteria;
import de.symeda.sormas.api.contact.SimilarContactDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.person.PersonReferenceDto;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.fields.CustomField;

import java.util.function.Consumer;

public class ContactSelectionField extends CustomField<SimilarContactDto> {

	private static final long serialVersionUID = 1595770585498718792L;

	public static final String CREATE_CONTACT = "createContact";
	public static final String SELECT_CONTACT = "selectContact";

	private final ContactDto referenceContact;
	private final PersonReferenceDto referencePerson;
	private final String infoText;
	private final String referenceFirstName;
	private final String referenceLastName;
	private VerticalLayout mainLayout;
	private ContactSelectionGrid contactSelectionGrid;
	private RadioButtonGroup<String> rbSelectContact;
	private RadioButtonGroup<String> rbCreateContact;
	private Consumer<Boolean> selectionChangeCallback;

	public ContactSelectionField(ContactDto referenceContact, PersonReferenceDto referencePerson, String infoText) {
		this.referenceContact = referenceContact;
		this.referencePerson = referencePerson;
		this.infoText = infoText;
		this.referenceFirstName = referencePerson.getFirstName();
		this.referenceLastName = referencePerson.getLastName();

		initializeGrid();
	}

	private void initializeGrid() {

		final ContactSimilarityCriteria criteria = new ContactSimilarityCriteria(
			referencePerson,
			referenceContact.getCaze(),
			referenceContact.getDisease(),
			referenceContact.getLastContactDate(),
			referenceContact.getReportDateTime());
		contactSelectionGrid = new ContactSelectionGrid(criteria);

		contactSelectionGrid.addSelectionListener(e -> {
			if (e.getAllSelectedItems().size() > 0) {
				rbCreateContact.setValue(null);
			}

			if (selectionChangeCallback != null) {
				selectionChangeCallback.accept(!e.getAllSelectedItems().isEmpty());
			}
		});
	}

	@Override
	protected Component initContent() {

		mainLayout = new VerticalLayout();
		mainLayout.setSpacing(true);
		mainLayout.setPadding(false);
		mainLayout.setSizeUndefined();
		mainLayout.setWidthFull();

		addInfoComponent();
		addContactDetailsComponent();
		addSelectContactRadioGroup();
		mainLayout.add(contactSelectionGrid);
		addCreateContactRadioGroup();

		rbSelectContact.setValue(SELECT_CONTACT);

		return mainLayout;
	}

	@Override
	protected void setPresentationValue(SimilarContactDto similarContactDto) {

		rbSelectContact.setValue(SELECT_CONTACT);

		if (similarContactDto != null) {
			contactSelectionGrid.select(similarContactDto);
		}
	}

	@Override
	public SimilarContactDto getValue() {

		if (contactSelectionGrid != null) {
			return contactSelectionGrid.getSelectedItems().stream().findFirst().orElse(null);
		}

		return null;
	}

	public boolean hasMatches() {
		return contactSelectionGrid.getGenericDataView().getItems().count() > 0;
	}

	public void selectBestMatch() {

		if (contactSelectionGrid.getGenericDataView().getItems().count() == 1) {
			setValue(contactSelectionGrid.getGenericDataView().getItems().findFirst().get());
		} else {
			setValue(null);
		}
	}

	/**
	 * Callback is executed with 'true' when a grid entry or "Create new person" is selected.
	 */
	public void setSelectionChangeCallback(Consumer<Boolean> callback) {
		this.selectionChangeCallback = callback;
	}

	private void addInfoComponent() {
		mainLayout.add(VaadinUiUtil.createInfoComponent(infoText));
	}

	private void addContactDetailsComponent() {

		HorizontalLayout contactDetailsLayout = new HorizontalLayout();
		contactDetailsLayout.setSpacing(true);

		final Span lblFirstName = new Span(referenceFirstName);
		contactDetailsLayout.add(withCaption(I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, PersonDto.FIRST_NAME), lblFirstName));

		final Span lblLastName = new Span(referenceLastName);
		contactDetailsLayout.add(withCaption(I18nProperties.getPrefixCaption(PersonDto.I18N_PREFIX, PersonDto.LAST_NAME), lblLastName));

		final Span lblCase = new Span(referenceContact.getCaze() != null ? referenceContact.getCaze().getCaption() : "");
		contactDetailsLayout.add(withCaption(I18nProperties.getPrefixCaption(ContactDto.I18N_PREFIX, ContactDto.CAZE), lblCase));

		final Span lblCaseIdExternalSystem = new Span(referenceContact.getCaseIdExternalSystem());
		contactDetailsLayout
			.add(withCaption(I18nProperties.getPrefixCaption(ContactDto.I18N_PREFIX, ContactDto.CASE_ID_EXTERNAL_SYSTEM), lblCaseIdExternalSystem));

		final Span lblLastContactDate = new Span(DateFormatHelper.formatDate(referenceContact.getLastContactDate()));
		contactDetailsLayout.add(withCaption(I18nProperties.getPrefixCaption(ContactDto.I18N_PREFIX, ContactDto.LAST_CONTACT_DATE), lblLastContactDate));

		final Span lblContactProximity = new Span(
			referenceContact.getContactProximities() != null && !referenceContact.getContactProximities().isEmpty()
				? referenceContact.getContactProximities().stream().map(Object::toString).reduce((a, b) -> a + ", " + b).orElse("")
				: "");
		contactDetailsLayout
			.add(withCaption(I18nProperties.getPrefixCaption(ContactDto.I18N_PREFIX, ContactDto.CONTACT_PROXIMITIES), lblContactProximity));

		final Span lblContactClassification =
			new Span(referenceContact.getContactClassification() != null ? referenceContact.getContactClassification().toString() : "");
		contactDetailsLayout
			.add(withCaption(I18nProperties.getPrefixCaption(ContactDto.I18N_PREFIX, ContactDto.CONTACT_CLASSIFICATION), lblContactClassification));

		final Span lblContactStatus = new Span(referenceContact.getContactStatus() != null ? referenceContact.getContactStatus().toString() : "");
		contactDetailsLayout.add(withCaption(I18nProperties.getPrefixCaption(ContactDto.I18N_PREFIX, ContactDto.CONTACT_STATUS), lblContactStatus));

		// Was a Vaadin 8 Panel (a framed box); Flow has no panel component
		mainLayout.add(new Div(contactDetailsLayout));
	}

	/**
	 * Was a Vaadin 8 label with a caption: the caption is shown above the value, as Vaadin rendered it.
	 */
	private static Div withCaption(String caption, Span label) {
		Span captionLabel = new Span(caption);
		captionLabel.addClassName("v-caption");
		return new Div(captionLabel, new Div(label));
	}

	private void addSelectContactRadioGroup() {

		rbSelectContact = new RadioButtonGroup<>();
		rbSelectContact.setItems(SELECT_CONTACT);
		rbSelectContact.setItemLabelGenerator((item) -> I18nProperties.getCaption(Captions.contactSelect));
		CssStyles.style(rbSelectContact, CssStyles.VSPACE_NONE);
		rbSelectContact.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				rbCreateContact.setValue(null);
				contactSelectionGrid.setEnabled(true);
				if (selectionChangeCallback != null) {
					selectionChangeCallback.accept(!contactSelectionGrid.getSelectedItems().isEmpty());
				}
			}
		});

		mainLayout.add(rbSelectContact);
	}

	private void addCreateContactRadioGroup() {

		rbCreateContact = new RadioButtonGroup<>();
		rbCreateContact.setItems(CREATE_CONTACT);
		rbCreateContact.setItemLabelGenerator((item) -> I18nProperties.getCaption(Captions.contactCreateNew));
		rbCreateContact.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				rbSelectContact.setValue(null);
				contactSelectionGrid.deselectAll();
				contactSelectionGrid.setEnabled(false);
				if (selectionChangeCallback != null) {
					selectionChangeCallback.accept(true);
				}
			}
		});

		mainLayout.add(rbCreateContact);
	}
}
