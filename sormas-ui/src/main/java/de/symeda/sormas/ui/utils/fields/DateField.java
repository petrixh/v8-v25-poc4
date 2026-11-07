/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2026 SORMAS Foundation gGmbH
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
package de.symeda.sormas.ui.utils.fields;

import java.time.LocalDate;
import java.util.Date;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.datepicker.DatePicker;

import de.symeda.sormas.api.utils.UtilDate;

/**
 * Date field with a {@link java.util.Date} value on a Flow {@link DatePicker} (replaces {@code com.vaadin.v7.ui.DateField}
 * and, through {@link PopupDateField}, {@code PopupDateField}). The value is the start of the selected day in the server
 * time zone, like a V7 date field with day resolution.
 */
public class DateField extends CustomField<Date> {

	private static final long serialVersionUID = 1L;

	private final DatePicker datePicker = new DatePicker();
	private String dateFormat;

	public DateField() {
		super();
		datePicker.setWidthFull();
		datePicker.addValueChangeListener(e -> {
			if (e.isFromClient()) {
				updateValue();
			}
		});
		add(datePicker);
	}

	public DateField(String caption) {
		this();
		setCaption(caption);
	}

	@Override
	protected Component initContent() {
		return null;
	}

	public DatePicker getDatePicker() {
		return datePicker;
	}

	@Override
	protected Date generateModelValue() {
		LocalDate date = datePicker.getValue();
		return date != null ? UtilDate.from(date) : null;
	}

	@Override
	protected void setPresentationValue(Date newPresentationValue) {
		datePicker.setValue(newPresentationValue != null ? UtilDate.toLocalDate(newPresentationValue) : null);
	}

	@Override
	public Class<? extends Date> getType() {
		return Date.class;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	/**
	 * Display and parse pattern, e.g. {@code dd/MM/yyyy}.
	 */
	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
		DatePicker.DatePickerI18n i18n = datePicker.getI18n() != null ? datePicker.getI18n() : new DatePicker.DatePickerI18n();
		i18n.setDateFormat(dateFormat);
		datePicker.setI18n(i18n);
	}

	/**
	 * No-op. V7 lenient parsing also accepted out-of-range day and month values and rolled them over (e.g. 32.01. became
	 * 01.02.); the Flow date picker rejects such input as invalid. It parses with {@link #setDateFormat(String) the date
	 * format} only. Extra parse formats (e.g. {@code d.M.yy}) would go through {@code DatePickerI18n.setDateFormats};
	 * parked (parking lot, register row of 1a-32).
	 */
	public void setLenient(boolean lenient) {
		// see register
	}

	@Override
	public String getInputPrompt() {
		return datePicker.getPlaceholder();
	}

	@Override
	public void setInputPrompt(String inputPrompt) {
		datePicker.setPlaceholder(inputPrompt);
	}

	public void setRangeStart(Date startDate) {
		datePicker.setMin(startDate != null ? UtilDate.toLocalDate(startDate) : null);
	}

	public Date getRangeStart() {
		return datePicker.getMin() != null ? UtilDate.from(datePicker.getMin()) : null;
	}

	public void setRangeEnd(Date endDate) {
		datePicker.setMax(endDate != null ? UtilDate.toLocalDate(endDate) : null);
	}

	public Date getRangeEnd() {
		return datePicker.getMax() != null ? UtilDate.from(datePicker.getMax()) : null;
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		super.setReadOnly(readOnly);
		datePicker.setReadOnly(readOnly);
	}
}
