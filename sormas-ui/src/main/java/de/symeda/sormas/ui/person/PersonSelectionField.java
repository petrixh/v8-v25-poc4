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

package de.symeda.sormas.ui.person;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.location.LocationDto;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.person.PersonHelper;
import de.symeda.sormas.api.person.PersonSimilarityCriteria;
import de.symeda.sormas.api.person.SimilarPersonDto;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.fields.CustomField;

@SuppressWarnings("serial")
public class PersonSelectionField extends CustomField<SimilarPersonDto> {

	public static final String CREATE_PERSON = "createPerson";
	public static final String SELECT_PERSON = Captions.personSelect;
	public static final String SEARCH_AND_SELECT_PERSON = Captions.personSearchAndSelect;

	protected VerticalLayout mainLayout;
	protected PersonSelectionGrid personGrid;

	private String infoText;
	protected Consumer<Boolean> selectionChangeCallback;
	private PersonSimilarityCriteria defaultCriteria;
	private RadioButtonGroup<String> rbSelectPerson;
	private RadioButtonGroup<String> rbCreatePerson;
	protected PersonSelectionFilterForm filterForm;
	private PersonDto referencePerson;
	private boolean hasMatches;
	private FieldVisibilityCheckers fieldVisibilityCheckers;

	public PersonSelectionField(PersonDto referencePerson, String infoText) {
		this(referencePerson, infoText, null);
	}

	/**
	 * Generate a selection field which contains a grid containing all similar persons to the `referencePerson`.
	 * 
	 * @param referencePerson
	 *            The person for which similar persons are searched and displayed.
	 * @param infoText
	 *            Information displayed to the user.
	 * @param infoTextWithoutMatches
	 *            Information displayed to the user if the field is shown without matches.
	 */
	public PersonSelectionField(PersonDto referencePerson, String infoText, String infoTextWithoutMatches) {
		this.referencePerson = referencePerson;

		fieldVisibilityCheckers = FieldVisibilityCheckers.withCountry(FacadeProvider.getConfigFacade().getCountryLocale());

		initializeGrid();

		this.hasMatches =
			referencePerson != null && FacadeProvider.getPersonFacade().checkMatchingNameInDatabase(UiUtil.getUserReference(), defaultCriteria);
		if (infoTextWithoutMatches == null) {
			this.infoText = infoText;
		} else {
			this.infoText = hasMatches ? infoText : infoTextWithoutMatches;
		}
	}

	protected void addInfoComponent() {
		mainLayout.add(VaadinUiUtil.createInfoComponent(infoText));
	}

	private void addPersonDetailsComponent() {
		HorizontalLayout personDetailsLayout1 = new HorizontalLayout();
		personDetailsLayout1.setSpacing(true);

		addLabelIfVisible(personDetailsLayout1, referencePerson.getFirstName(), PersonDto.I18N_PREFIX, PersonDto.FIRST_NAME, PersonDto.class);
		addLabelIfVisible(personDetailsLayout1, referencePerson.getLastName(), PersonDto.I18N_PREFIX, PersonDto.LAST_NAME, PersonDto.class);
		addLabelIfVisible(personDetailsLayout1, referencePerson.getNickname(), PersonDto.I18N_PREFIX, PersonDto.NICKNAME, PersonDto.class);

		Component lblBirthDateAndAge = createCaptionedLabel(
			PersonHelper.getAgeAndBirthdateString(
				referencePerson.getApproximateAge(),
				referencePerson.getApproximateAgeType(),
				referencePerson.getBirthdateDD(),
				referencePerson.getBirthdateMM(),
				referencePerson.getBirthdateYYYY()),
			I18nProperties.getCaption(Captions.personAgeAndBirthdate));
		personDetailsLayout1.add(lblBirthDateAndAge);

		addLabelIfVisible(
			personDetailsLayout1,
			referencePerson.getSex() != null ? referencePerson.getSex().toString() : "",
			PersonDto.I18N_PREFIX,
			PersonDto.SEX,
			PersonDto.class);

		addLabelIfVisible(
			personDetailsLayout1,
			referencePerson.getPresentCondition() != null ? referencePerson.getPresentCondition().toString() : "",
			PersonDto.I18N_PREFIX,
			PersonDto.PRESENT_CONDITION,
			PersonDto.class);

		addLabelIfVisible(personDetailsLayout1, referencePerson.getPhone(), PersonDto.I18N_PREFIX, PersonDto.PHONE, PersonDto.class);

		mainLayout.add(personDetailsLayout1);

		HorizontalLayout personDetailsLayout2 = new HorizontalLayout();
		personDetailsLayout2.setSpacing(true);

		if (UiUtil.disabled(FeatureType.HIDE_JURISDICTION_FIELDS)) {
			addLabelIfVisible(
				personDetailsLayout2,
				referencePerson.getAddress().getDistrict() != null ? referencePerson.getAddress().getDistrict().buildCaption() : "",
				LocationDto.I18N_PREFIX,
				LocationDto.DISTRICT,
				LocationDto.class);
			addLabelIfVisible(
				personDetailsLayout2,
				referencePerson.getAddress().getCommunity() != null ? referencePerson.getAddress().getCommunity().buildCaption() : "",
				LocationDto.I18N_PREFIX,
				LocationDto.COMMUNITY,
				LocationDto.class);
		}

		addLabelIfVisible(
			personDetailsLayout2,
			referencePerson.getAddress().getPostalCode(),
			LocationDto.I18N_PREFIX,
			LocationDto.POSTAL_CODE,
			LocationDto.class);

		addLabelIfVisible(personDetailsLayout2, referencePerson.getAddress().getCity(), LocationDto.I18N_PREFIX, LocationDto.CITY, LocationDto.class);

		addLabelIfVisible(
			personDetailsLayout2,
			referencePerson.getAddress().getStreet(),
			LocationDto.I18N_PREFIX,
			LocationDto.STREET,
			LocationDto.class);

		addLabelIfVisible(
			personDetailsLayout2,
			referencePerson.getAddress().getHouseNumber(),
			LocationDto.I18N_PREFIX,
			LocationDto.HOUSE_NUMBER,
			LocationDto.class);

		mainLayout.add(personDetailsLayout2);

		HorizontalLayout personDetailsLayout3 = new HorizontalLayout();
		personDetailsLayout3.setSpacing(true);

		addLabelIfVisible(
			personDetailsLayout3,
			referencePerson.getNationalHealthId(),
			PersonDto.I18N_PREFIX,
			PersonDto.NATIONAL_HEALTH_ID,
			PersonDto.class);
		addLabelIfVisible(
			personDetailsLayout3,
			referencePerson.getPassportNumber(),
			PersonDto.I18N_PREFIX,
			PersonDto.PASSPORT_NUMBER,
			PersonDto.class);

		mainLayout.add(personDetailsLayout3);
	}

	private void addLabelIfVisible(HorizontalLayout layout, String text, String i18nPrefix, String captionKey, Class<?> clazz) {
		if (fieldVisibilityCheckers.isVisible(clazz, captionKey)) {
			layout.add(createCaptionedLabel(text, I18nProperties.getPrefixCaption(i18nPrefix, captionKey)));
		}
	}

	/**
	 * Was a Vaadin 8 {@code Label} with a caption (rendered above it by the layout); Flow layouts render no captions, so
	 * the caption is a span above the text.
	 */
	private static Component createCaptionedLabel(String text, String caption) {
		VerticalLayout captionedLabel = new VerticalLayout(new Span(caption), new Span(text != null ? text : ""));
		captionedLabel.setPadding(false);
		captionedLabel.setSpacing(false);
		captionedLabel.setWidth(null);
		return captionedLabel;
	}

	private void addSelectPersonRadioGroup() {
		List<String> selectPersonOption = new ArrayList<>();
		if (hasMatches) {
			selectPersonOption.add(SELECT_PERSON);
		}
		if (UiUtil.enabled(FeatureType.PERSON_DUPLICATE_CUSTOM_SEARCH)) {
			selectPersonOption.add(SEARCH_AND_SELECT_PERSON);
		}

		rbSelectPerson = new RadioButtonGroup<>();
		rbSelectPerson.setItems(selectPersonOption);
		rbSelectPerson.setItemLabelGenerator(I18nProperties::getCaption);
		CssStyles.style(rbSelectPerson, CssStyles.VSPACE_NONE);

		rbSelectPerson.addValueChangeListener(e -> {
			String value = e.getValue();
			if (value != null) {
				rbCreatePerson.setValue(null);
				personGrid.setEnabled(true);
				if (selectionChangeCallback != null) {
					selectionChangeCallback.accept(!personGrid.getSelectedItems().isEmpty());
				}

				if (SELECT_PERSON.equals(value)) {
					filterForm.setVisible(false);
					if (filterForm.validateFields()) {
						personGrid.loadData(defaultCriteria);
					}
					selectBestMatch();
				} else {
					filterForm.setVisible(true);
					if (filterForm.validateFields()) {
						personGrid.loadData(filterForm.getValue());
					}
					setValue(null);
				}
			}
		});

		mainLayout.add(rbSelectPerson);
	}

	protected void addFilterForm() {
		filterForm = new PersonSelectionFilterForm();

		final PersonSimilarityCriteria searchCriteria = new PersonSimilarityCriteria();
		if (referencePerson != null) {
			searchCriteria.setName(referencePerson);
		}
		filterForm.setValue(searchCriteria);
		filterForm.addApplyHandler((e) -> {
			if (filterForm.validateFields()) {
				personGrid.loadData(filterForm.getValue());
			}
		});
		filterForm.addResetHandler((e) -> {
			filterForm.setValue(new PersonSimilarityCriteria());
			filterForm.clearValidation();
		});

		mainLayout.add(filterForm);
	}

	/**
	 * Load a grid of all persons similar to the given reference person.
	 */
	protected void initializeGrid() {
		defaultCriteria = PersonSimilarityCriteria.forPerson(referencePerson, true, false);
		personGrid = new PersonSelectionGrid();

		personGrid.addSelectionListener(e -> {
			// the field value is the grid selection (Vaadin 8: getValue() read the grid)
			updateValue();

			if (e.getAllSelectedItems().size() > 0) {
				rbCreatePerson.setValue(null);
			}

			if (selectionChangeCallback != null) {
				selectionChangeCallback.accept(!e.getAllSelectedItems().isEmpty());
			}
		});
	}

	private void addCreatePersonRadioGroup() {
		rbCreatePerson = new RadioButtonGroup<>();
		rbCreatePerson.setItems(CREATE_PERSON);
		rbCreatePerson.setItemLabelGenerator((item) -> I18nProperties.getCaption(Captions.personCreateNew));
		rbCreatePerson.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				rbSelectPerson.setValue(null);
				personGrid.deselectAll();
				personGrid.setEnabled(false);
				personGrid.setHeightByRows(1);
				if (selectionChangeCallback != null) {
					selectionChangeCallback.accept(true);
				}
			}
		});
		rbCreatePerson.addValueChangeListener(e -> {
			filterForm.setVisible(!CREATE_PERSON.equals(e.getValue()));
		});

		mainLayout.add(rbCreatePerson);
	}

	@Override
	protected Component initContent() {
		// Main layout
		mainLayout = new VerticalLayout();
		mainLayout.setSpacing(true);
		mainLayout.setPadding(false);
		mainLayout.setSizeUndefined();
		mainLayout.setWidth(100, Unit.PERCENTAGE);

		addInfoComponent();
		addPersonDetailsComponent();
		addSelectPersonRadioGroup();
		addFilterForm();
		mainLayout.add(personGrid);
		addCreatePersonRadioGroup();

		if (hasMatches) {
			rbSelectPerson.setValue(SELECT_PERSON);
		} else {
			rbCreatePerson.setValue(CREATE_PERSON);
		}

		return mainLayout;
	}

	public void selectBestMatch() {
		// Vaadin 8 built the content when the window was added; a Flow dialog attaches its content only before the
		// response, after the callers' selectBestMatch(), so build it here
		getContent();
		if (personGrid.getListDataView().getItemCount() == 1) {
			setValue(personGrid.getListDataView().getItem(0));
		} else {
			setValue(null);
		}
	}

	/**
	 * Was the Vaadin 8 {@code getValue()} override: the value is the person selected in the grid.
	 */
	@Override
	protected SimilarPersonDto generateModelValue() {
		if (personGrid != null) {
			return personGrid.getSelectedItems().stream().findFirst().orElse(null);
		}

		return null;
	}

	/**
	 * Was {@code doSetValue}: selects the person in the grid.
	 */
	@Override
	protected void setPresentationValue(SimilarPersonDto newValue) {
		if (rbSelectPerson.getValue() == null) {
			rbSelectPerson.setValue(SELECT_PERSON);
		}

		selectInGrid(personGrid, newValue);
	}

	/**
	 * Vaadin 7 {@code Grid.select(null)} cleared the single selection; Flow needs {@code deselectAll()} for that.
	 */
	static void selectInGrid(PersonSelectionGrid grid, SimilarPersonDto person) {
		if (person == null) {
			grid.deselectAll();
		} else {
			grid.select(person);
		}
	}

	public boolean hasMatches() {
		return hasMatches;
	}

	/**
	 * Callback is executed with 'true' when a grid entry or "Create new person" is selected.
	 */
	public void setSelectionChangeCallback(Consumer<Boolean> callback) {
		this.selectionChangeCallback = callback;
	}
}
