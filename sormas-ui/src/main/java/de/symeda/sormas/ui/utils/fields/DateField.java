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
import java.util.Calendar;
import java.util.Date;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.datepicker.DatePicker;

import de.symeda.sormas.api.utils.UtilDate;

/**
 * Date field with a {@link java.util.Date} value on a Flow {@link DatePicker} (replaces {@code com.vaadin.v7.ui.DateField}
 * and, through {@link PopupDateField}, {@code PopupDateField}), with the V7 day-resolution semantics: a day picked or
 * typed by the user replaces only the year, month and day of the current value and keeps its time of day; with no
 * current value it is the start of the selected day. Server (JVM) time zone. See {@link #generateModelValue()}.
 * <p>
 * Typed text the date picker cannot parse empties the value and makes the field invalid with the
 * {@link #getParseErrorMessage() parse error message} until the text is corrected or a value is set, as in V7 (see
 * {@link #getUnparsableInputError()}): a form commit then fails and writes nothing.
 */
public class DateField extends CustomField<Date> {

	private static final long serialVersionUID = 1L;

	/**
	 * The V7 default ({@code com.vaadin.v7.ui.DateField}, 8.14.3); not localised there either, and SORMAS never changed
	 * it.
	 */
	public static final String DEFAULT_PARSE_ERROR_MESSAGE = "Date format not recognized";

	private final Picker datePicker = new Picker();
	private String dateFormat;
	private String parseErrorMessage = DEFAULT_PARSE_ERROR_MESSAGE;

	/**
	 * The date picker with its unparsable-input state readable by the field.
	 */
	private static class Picker extends DatePicker {

		private static final long serialVersionUID = 1L;

		boolean hasUnparsableInput() {
			return isInputUnparsable();
		}
	}

	public DateField() {
		super();
		datePicker.setWidthFull();
		// V7 PopupDateField opened its calendar only from its button, not on typing or clicking into the text. An overlay
		// opened by typing would also take the next click outside (e.g. on Save) only to close itself.
		datePicker.setAutoOpen(false);
		// V7 without setDateFormat: the short date pattern of the locale (e.g. M/d/yy), not Flow's four-digit year
		DefaultDateFormat.applyOnAttach(datePicker);
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

	/**
	 * As Vaadin 7 ({@code com.vaadin.v7.ui.DateField.changeVariables} with {@code Resolution.DAY}, 8.14.3): the new value
	 * is a calendar in the JVM time zone set to the current value, with only year, month and day replaced by the user's
	 * day. The time of day of the current value (hours to milliseconds) is kept, e.g. a stored report date
	 * {@code 2026-10-26 11:00:17.25} edited to the 24th becomes {@code 2026-10-24 11:00:17.25}. With no current value it
	 * is the start of the day; an emptied picker gives {@code null}. Called only for user changes (see the constructor).
	 */
	@Override
	protected Date generateModelValue() {
		LocalDate date = datePicker.getValue();
		if (date == null) {
			return null;
		}
		Date current = getValue();
		if (current == null) {
			return UtilDate.from(date);
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(current);
		calendar.set(Calendar.YEAR, date.getYear());
		calendar.set(Calendar.MONTH, date.getMonthValue() - 1);
		calendar.set(Calendar.DAY_OF_MONTH, date.getDayOfMonth());
		return calendar.getTime();
	}

	@Override
	protected void setPresentationValue(Date newPresentationValue) {
		datePicker.setValue(newPresentationValue != null ? UtilDate.toLocalDate(newPresentationValue) : null);
	}

	@Override
	public Class<? extends Date> getType() {
		return Date.class;
	}

	/**
	 * As V7 ({@code DateField.changeVariables} and {@code validate()}): while the picker holds typed text it cannot parse,
	 * the value is empty and the field is not valid, with the {@link #getParseErrorMessage() parse error message}. A
	 * value set by code (e.g. a form's discard or read) replaces the text and ends this state.
	 */
	@Override
	public String getUnparsableInputError() {
		return datePicker.hasUnparsableInput() ? parseErrorMessage : null;
	}

	/**
	 * V7 {@code getParseErrorMessage()}. Default {@value #DEFAULT_PARSE_ERROR_MESSAGE}.
	 */
	public String getParseErrorMessage() {
		return parseErrorMessage;
	}

	public void setParseErrorMessage(String parseErrorMessage) {
		this.parseErrorMessage = parseErrorMessage;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	/**
	 * Display and parse pattern, e.g. {@code dd/MM/yyyy}. A typed two-digit year is read as the old app's form fields
	 * read it ({@link DefaultDateFormat#ownFormatTwoDigitYears}).
	 */
	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
		DatePicker.DatePickerI18n i18n = datePicker.getI18n() != null ? datePicker.getI18n() : new DatePicker.DatePickerI18n();
		i18n.setDateFormat(dateFormat);
		DefaultDateFormat.ownFormatTwoDigitYears(i18n, LocalDate.now());
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
