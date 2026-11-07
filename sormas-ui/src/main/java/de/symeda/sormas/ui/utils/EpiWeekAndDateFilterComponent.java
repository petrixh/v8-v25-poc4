/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.ui.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.shared.Tooltip;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.utils.DateFilterOption;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.EpiWeek;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.fields.PopupDateField;

public class EpiWeekAndDateFilterComponent<DATE_TYPE> extends HorizontalLayout {

	private static final long serialVersionUID = 8752630393182185034L;

	private final ComboBox dateFilterOptionFilter;
	private final ComboBox dateTypeSelector;
	private final ComboBox weekFromFilter;
	private final ComboBox weekToFilter;
	private final PopupDateField dateFromFilter;
	private final PopupDateField dateToFilter;

	public EpiWeekAndDateFilterComponent(boolean fillAutomatically, boolean showCaption, String infoText, AbstractFilterForm parentFilterForm) {
		this(fillAutomatically, showCaption, infoText, null, null, null, parentFilterForm);
	}

	public EpiWeekAndDateFilterComponent(
		boolean fillAutomatically,
		boolean showCaption,
		String infoText,
		DATE_TYPE[] dateTypes,
		String dateTypePrompt,
		DATE_TYPE defaultDateType,
		AbstractFilterForm parentFilterForm) {
		setSpacing(true);

		Calendar c = Calendar.getInstance();
		c.setTime(new Date());

		dateFilterOptionFilter = ComboBoxHelper.createComboBoxV7();
		dateTypeSelector = ComboBoxHelper.createComboBoxV7();
		weekFromFilter = ComboBoxHelper.createComboBoxV7();
		weekToFilter = ComboBoxHelper.createComboBoxV7();
		dateFromFilter = new PopupDateField();
		dateToFilter = new PopupDateField();

		// Date filter options
		dateFilterOptionFilter.setId("dateFilterOption");
		dateFilterOptionFilter.setWidth(200, Unit.PIXELS);
		dateFilterOptionFilter.addItems((Object[]) DateFilterOption.values());
		dateFilterOptionFilter.setNullSelectionAllowed(false);
		dateFilterOptionFilter.setValue(DateFilterOption.EPI_WEEK);
		if (showCaption) {
			dateFilterOptionFilter.setCaption(I18nProperties.getCaption(Captions.dashboardCustomPeriod));
		}

		dateFilterOptionFilter.addValueChangeListener(e -> {
			if (e.getValue() == DateFilterOption.DATE) {
				int newIndex = indexOf(weekFromFilter);
				remove(weekFromFilter);
				remove(weekToFilter);
				addComponentAtIndex(newIndex, dateFromFilter);
				addComponentAtIndex(newIndex + 1, dateToFilter);

				if (fillAutomatically) {
					dateFromFilter.setValue(DateHelper.subtractDays(c.getTime(), 7));
				}
				if (fillAutomatically) {
					dateToFilter.setValue(c.getTime());
				}
			} else if (indexOf(dateFromFilter) != -1) {
				int newIndex = indexOf(dateFromFilter);
				remove(dateFromFilter);
				remove(dateToFilter);
				addComponentAtIndex(newIndex, weekFromFilter);
				addComponentAtIndex(newIndex + 1, weekToFilter);

				if (fillAutomatically) {
					weekFromFilter.setValue(DateHelper.getEpiWeek(c.getTime()));
				}
				if (fillAutomatically) {
					weekToFilter.setValue(DateHelper.getEpiWeek(c.getTime()));
				}
			}
		});
		add(dateFilterOptionFilter);

		// New case date type selector
		if (dateTypes != null) {
			dateTypeSelector.setId("dateType");
			dateTypeSelector.setWidth(200, Unit.PIXELS);
			dateTypeSelector.addItems(dateTypes);
			if (dateTypePrompt != null) {
				dateTypeSelector.setInputPrompt(dateTypePrompt);
			}
			if (defaultDateType != null) {
				dateTypeSelector.setValue(defaultDateType);
			}
			if (showCaption) {
				CssStyles.style(dateTypeSelector, CssStyles.FORCE_CAPTION);
			}
			add(dateTypeSelector);

			if (!StringUtils.isEmpty(infoText)) {
				Span infoLabel = new Span(VaadinIcon.INFO_CIRCLE.create());
				infoLabel.setSizeUndefined();
				// was setDescription(infoText, ContentMode.HTML): the tooltip renders the HTML through Markdown
				Tooltip.forComponent(infoLabel).setMarkdown(infoText);
				CssStyles.style(infoLabel, CssStyles.LABEL_XLARGE, CssStyles.LABEL_SECONDARY);
				add(infoLabel);
			}
		}

		// Epi week filter
		List<EpiWeek> epiWeekList = DateHelper.createEpiWeekList(c.get(Calendar.YEAR), c.get(Calendar.WEEK_OF_YEAR));

		weekFromFilter.setId("weekFrom");
		weekFromFilter.setWidth(200, Unit.PIXELS);
		for (EpiWeek week : epiWeekList) {
			weekFromFilter.addItem(week);
		}
		weekFromFilter.setNullSelectionAllowed(false);
		if (fillAutomatically) {
			weekFromFilter.setValue(DateHelper.getEpiWeek(c.getTime()));
		}
		if (showCaption) {
			weekFromFilter.setCaption(I18nProperties.getCaption(Captions.epiWeekFrom));
		}
		add(weekFromFilter);

		weekToFilter.setId("weekTo");
		weekToFilter.setWidth(200, Unit.PIXELS);
		for (EpiWeek week : epiWeekList) {
			weekToFilter.addItem(week);
		}
		weekToFilter.setNullSelectionAllowed(false);
		if (fillAutomatically) {
			weekToFilter.setValue(DateHelper.getEpiWeek(c.getTime()));
		}
		if (showCaption) {
			weekToFilter.setCaption(I18nProperties.getCaption(Captions.epiWeekTo));
		}
		add(weekToFilter);

		// Date filter
		dateFromFilter.setId("dateFrom");
		dateFromFilter.setWidth(200, Unit.PIXELS);
		if (showCaption) {
			dateFromFilter.setCaption(I18nProperties.getCaption(Captions.from));
		}

		dateToFilter.setId("dateTo");
		dateToFilter.setWidth(200, Unit.PIXELS);
		if (showCaption) {
			dateToFilter.setCaption(I18nProperties.getCaption(Captions.to));
		}

		if (parentFilterForm != null) {
			dateFilterOptionFilter.addValueChangeListener(e -> parentFilterForm.onChange());
			dateTypeSelector.addValueChangeListener(e -> parentFilterForm.onChange());
			weekFromFilter.addValueChangeListener(e -> parentFilterForm.onChange());
			weekToFilter.addValueChangeListener(e -> parentFilterForm.onChange());
			dateFromFilter.addValueChangeListener(e -> parentFilterForm.onChange());
			dateToFilter.addValueChangeListener(e -> parentFilterForm.onChange());
		}
	}

	public void setNotificationsForMissingFilters() {
		DateFilterOption dateFilterOption = (DateFilterOption) dateFilterOptionFilter.getValue();
		Notification notification;
		if (dateFilterOption == DateFilterOption.DATE) {
			notification = NotificationHelper.createNotification(
				I18nProperties.getString(Strings.headingMissingDateFilter),
				I18nProperties.getString(Strings.messageMissingDateFilter),
				NotificationHelper.Type.WARNING_MESSAGE,
				false);
		} else {
			notification = NotificationHelper.createNotification(
				I18nProperties.getString(Strings.headingMissingEpiWeekFilter),
				I18nProperties.getString(Strings.messageMissingEpiWeekFilter),
				NotificationHelper.Type.WARNING_MESSAGE,
				false);
		}
		NotificationHelper.setDelayMsec(notification, -1);
		notification.open();
	}

	public ComboBox getDateFilterOptionFilter() {
		return dateFilterOptionFilter;
	}

	public ComboBox getDateTypeSelector() {
		return dateTypeSelector;
	}

	public ComboBox getWeekFromFilter() {
		return weekFromFilter;
	}

	public ComboBox getWeekToFilter() {
		return weekToFilter;
	}

	public PopupDateField getDateFromFilter() {
		return dateFromFilter;
	}

	public PopupDateField getDateToFilter() {
		return dateToFilter;
	}
}
