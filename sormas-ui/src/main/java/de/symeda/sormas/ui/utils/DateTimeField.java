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

package de.symeda.sormas.ui.utils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.Date;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.UtilDate;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.fields.CustomField;
import de.symeda.sormas.ui.utils.fields.PopupDateField;

/**
 * Date and time field: a date picker and a combo box with times in 15 minute steps; other times can be typed.
 * <p>
 * Converters: a converter set with {@link #setConverter} applies to the whole date and time value in the form binding
 * (V7 applied it to the date part).
 */
@SuppressWarnings("serial")
public class DateTimeField extends CustomField<Date> {

	private PopupDateField dateField;
	private ComboBox timeField;
	private String inputPrompt;
	private boolean settingPresentation;

	@Override
	protected Component initContent() {

		HorizontalLayout layout = new HorizontalLayout();
		layout.setSpacing(true);
		layout.setWidthFull();

		dateField = new PopupDateField();
		dateField.setId(this.getId().orElse(null) + "_" + "date");
		dateField.setWidthFull();
		dateField.setDateFormat(DateFormatHelper.getDateFormatPattern());
		dateField.setLenient(true);
		dateField.setInputPrompt(inputPrompt);
		layout.add(dateField);
		layout.setFlexGrow(0.5, dateField);

		timeField = ComboBoxHelper.createComboBoxV7();
		timeField.setId(this.getId().orElse(null) + "_" + "time");

		// fill
		for (int hours = 0; hours <= 23; hours++) {
			for (int minutes = 0; minutes <= 59; minutes += 15) {
				ensureTimeEntry(hours, minutes);
			}
		}

		timeField.setNewItemsAllowed(true);
		timeField.setNewItemHandler(newItemCaption -> {
			Date date = DateHelper.parseTime(newItemCaption);
			timeField.setValue(ensureTimeEntry(date));
		});

		timeField.setWidthFull();
		layout.add(timeField);
		layout.setFlexGrow(0.5, timeField);

		// value can't be set on readOnly fields
		dateField.setReadOnly(false);
		timeField.setReadOnly(false);
		// set field values based on internal value
		setPresentationValue(getValue());
		dateField.setReadOnly(isReadOnly());
		timeField.setReadOnly(isReadOnly());

		dateField.addValueChangeListener(e -> onInnerValueChange());
		timeField.addValueChangeListener(e -> onInnerValueChange());

		return layout;
	}

	private void onInnerValueChange() {
		if (!settingPresentation) {
			updateValue();
		}
	}

	@Override
	public Class<? extends Date> getType() {
		return Date.class;
	}

	@Override
	protected void setPresentationValue(Date newValue) {
		if (dateField != null && timeField != null) {
			settingPresentation = true;
			try {
				if (newValue != null) {
					dateField.setValue(DateHelper.getStartOfDay(newValue));
					timeField.setValue(ensureTimeEntry(newValue));
				} else {
					dateField.setValue(null);
					timeField.setValue(null);
				}
			} finally {
				settingPresentation = false;
			}
		}
	}

	@Override
	protected Date generateModelValue() {
		if (dateField != null && timeField != null) {
			Date date = dateField.getValue();
			if (date != null) {
				Integer totalMinutes = (Integer) timeField.getValue();
				if (totalMinutes != null) {
					LocalDateTime dateTime = UtilDate.toLocalDateTime(date);
					dateTime = dateTime.withHour((totalMinutes / 60) % 24).withMinute(totalMinutes % 60);
					date = UtilDate.from(dateTime);
				}
				return date;
			}
			return null;
		}
		return getValue();
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		super.setReadOnly(readOnly);
		if (dateField != null && timeField != null) {
			dateField.setReadOnly(readOnly);
			timeField.setReadOnly(readOnly);
		}
	}

	/**
	 * @return itemId of the entry
	 */
	private Object ensureTimeEntry(Date time) {
		if (time == null) {
			return null;
		}
		LocalTime localTime = UtilDate.toLocalTime(time);
		return ensureTimeEntry(localTime.getHour(), localTime.getMinute());
	}

	/**
	 * @return itemId of the entry
	 */
	private Object ensureTimeEntry(int hours, int minutes) {
		int itemId = hours * 60 + minutes;
		if (!timeField.containsId(itemId)) {
			timeField.addItem(itemId);
			timeField.setItemCaption(itemId, String.format("%1$02d:%2$02d", hours, minutes));
			// don't do this on initialization
			if (timeField.getParent().isPresent()) {
				// order the entries by time
				timeField.getSelectItems().sort(Comparator.comparing(id -> timeField.getItemCaption(id)));
			}
		}
		return itemId;
	}

	public String getInputPrompt() {
		return inputPrompt;
	}

	public void setInputPrompt(String inputPrompt) {
		this.inputPrompt = inputPrompt;
		if (dateField != null) {
			dateField.setInputPrompt(inputPrompt);
		}
	}
}
