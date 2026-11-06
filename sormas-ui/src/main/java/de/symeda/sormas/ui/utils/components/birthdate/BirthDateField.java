package de.symeda.sormas.ui.utils.components.birthdate;

import java.time.Month;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;

import de.symeda.sormas.api.caze.BirthDateDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.fields.CustomField;

public class BirthDateField extends CustomField<BirthDateDto> {

	private final Binder<BirthDateDto> binder = new Binder<>(BirthDateDto.class);

	private final ComboBox<Integer> dateOfBirthYear;
	private final ComboBox<Integer> dateOfBirthMonth;
	private final ComboBox<Integer> dateOfBirthDay;

	public BirthDateField() {
		dateOfBirthYear = new ComboBox<>();
		dateOfBirthMonth = new ComboBox<>();
		dateOfBirthDay = new ComboBox<>();
	}

	/**
	 * Built on first attach, as in Vaadin 8 ({@link CustomField}).
	 */
	@Override
	protected Component initContent() {
		if (getValue() == null) {
			setValue(new BirthDateDto());
		}

		HorizontalLayout layout = new HorizontalLayout();

		dateOfBirthYear.setId("dateOfBirthYear");
		dateOfBirthYear.setClearButtonVisible(true);
		dateOfBirthYear.setItems(DateHelper.getYearsToNow());
		dateOfBirthYear.setWidth("80px");
		dateOfBirthYear.addClassName(CssStyles.CAPTION_OVERFLOW);
		binder.forField(dateOfBirthYear).withValidator((e, context) -> {
			try {
				ControllerProvider.getPersonController().validateBirthDate(e, dateOfBirthMonth.getValue(), dateOfBirthDay.getValue());
				return ValidationResult.ok();
			} catch (de.symeda.sormas.ui.utils.InvalidValueException ex) {
				return ValidationResult.error(ex.getMessage());
			}
		}).bind(BirthDateDto.DATE_OF_BIRTH_YYYY);

		dateOfBirthMonth.setId("dateOfBirthMonth");
		dateOfBirthMonth.setClearButtonVisible(true);
		dateOfBirthMonth.setItems(DateHelper.getMonthsInYear());
		// setPageLength(12): the Flow overlay sizes itself
		setItemCaptionsForMonths(dateOfBirthMonth);
		dateOfBirthMonth.setWidth("120px");
		binder.forField(dateOfBirthMonth).withValidator((e, context) -> {
			try {
				ControllerProvider.getPersonController().validateBirthDate(dateOfBirthYear.getValue(), e, dateOfBirthDay.getValue());
				return ValidationResult.ok();
			} catch (de.symeda.sormas.ui.utils.InvalidValueException ex) {
				return ValidationResult.error(ex.getMessage());
			}
		}).bind(BirthDateDto.DATE_OF_BIRTH_MM);

		dateOfBirthDay.setId("dateOfBirthDay");
		dateOfBirthDay.setClearButtonVisible(true);
		dateOfBirthDay.setWidth("80px");
		binder.forField(dateOfBirthDay).withValidator((e, context) -> {
			try {
				ControllerProvider.getPersonController().validateBirthDate(dateOfBirthYear.getValue(), dateOfBirthMonth.getValue(), e);
				return ValidationResult.ok();
			} catch (de.symeda.sormas.ui.utils.InvalidValueException ex) {
				return ValidationResult.error(ex.getMessage());
			}
		}).bind(BirthDateDto.DATE_OF_BIRTH_DD);

		// Update the list of days according to the selected month and year
		dateOfBirthYear.addValueChangeListener(e -> {
			getValue().setDateOfBirthYYYY(e.getValue());
			updateListOfDays(e.getValue(), dateOfBirthMonth.getValue(), dateOfBirthDay);
		});
		dateOfBirthMonth.addValueChangeListener(e -> {
			getValue().setDateOfBirthMM(e.getValue());
			updateListOfDays(dateOfBirthYear.getValue(), e.getValue(), dateOfBirthDay);
		});
		dateOfBirthDay.addValueChangeListener(e -> {
			getValue().setDateOfBirthDD(e.getValue());
		});

		layout.add(dateOfBirthYear, dateOfBirthMonth, dateOfBirthDay);
		layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, dateOfBirthMonth, dateOfBirthDay);

		return layout;
	}

	@Override
	protected void setPresentationValue(BirthDateDto birthDateDto) {
		binder.setBean(birthDateDto);
	}

	@Override
	protected BirthDateDto generateModelValue() {
		return binder.getBean();
	}

	@Override
	public BirthDateDto getValue() {
		return binder.getBean();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		dateOfBirthYear.setEnabled(enabled);
		dateOfBirthMonth.setEnabled(enabled);
		dateOfBirthDay.setEnabled(enabled);
	}

	private void setItemCaptionsForMonths(ComboBox<Integer> comboBox) {
		comboBox.setItemLabelGenerator(item -> I18nProperties.getEnumCaption(Month.of(item)));
	}

	private void updateListOfDays(Integer selectedYear, Integer selectedMonth, ComboBox<Integer> dateOfBirthDay) {
		Integer currentlySelected = dateOfBirthDay.getValue();
		List<Integer> daysInMonth = DateHelper.getDaysInMonth(selectedMonth, selectedYear);
		dateOfBirthDay.setItems(daysInMonth);
		if (daysInMonth.contains(currentlySelected)) {
			dateOfBirthDay.setValue(currentlySelected);
		}
	}
}
