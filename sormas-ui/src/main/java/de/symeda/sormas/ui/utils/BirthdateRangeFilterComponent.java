package de.symeda.sormas.ui.utils;

import static de.symeda.sormas.ui.utils.CssStyles.VSPACE_TOP_4;

import java.util.Calendar;
import java.util.Date;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.Descriptions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.utils.fields.CheckBox;
import de.symeda.sormas.ui.utils.fields.PopupDateField;

public class BirthdateRangeFilterComponent extends HorizontalLayout {

	private static final long serialVersionUID = 8752630393182144501L;

	private final PopupDateField dateFromFilter;
	private final PopupDateField dateToFilter;
	private final CheckBox includePartialMatch;

	public BirthdateRangeFilterComponent(boolean showCaption, AbstractFilterForm parentFilterForm) {
		setSpacing(true);

		Calendar c = Calendar.getInstance();
		c.setTime(new Date());

		dateFromFilter = new PopupDateField();
		dateToFilter = new PopupDateField();
		includePartialMatch = new CheckBox();

		add(dateFromFilter);
		add(dateToFilter);

		// Date filter
		dateFromFilter.setDateFormat(DateFormatHelper.getDateFormatPattern());
		dateFromFilter.setId("dateFrom");
		dateFromFilter.setWidth(200, Unit.PIXELS);
		if (showCaption) {
			dateFromFilter.setCaption(I18nProperties.getCaption(Captions.from));
		}
		dateFromFilter.setInputPrompt(I18nProperties.getString(Strings.promptBirthdateFrom));

		dateToFilter.setDateFormat(DateFormatHelper.getDateFormatPattern());
		dateToFilter.setId("dateTo");
		dateToFilter.setWidth(200, Unit.PIXELS);
		if (showCaption) {
			dateToFilter.setCaption(I18nProperties.getCaption(Captions.to));
		}
		dateToFilter.setInputPrompt(I18nProperties.getString(Strings.promptBirthdateTo));

		dateFromFilter.addValueChangeListener(e -> {
			Date dateFrom = (Date) e.getValue();
			Date dateTo = dateToFilter.getValue();
			notifyIfIncorrectRange(dateFrom, dateTo);
			parentFilterForm.onChange();
		});

		dateToFilter.addValueChangeListener(e -> {
			Date dateTo = (Date) e.getValue();
			Date dateFrom = dateFromFilter.getValue();
			notifyIfIncorrectRange(dateFrom, dateTo);
			parentFilterForm.onChange();
		});

		includePartialMatch.setCaption(I18nProperties.getCaption(Captions.includePartialBirthdates));
		includePartialMatch.addClassName(VSPACE_TOP_4);
		includePartialMatch.setDescription(I18nProperties.getDescription(Descriptions.birthdateFilterPartialMatchDescription));
		add(includePartialMatch);
	}

	private static void notifyIfIncorrectRange(Date dateFrom, Date dateTo) {
		if (dateFrom != null & dateTo != null) {
			if (DateHelper.isDateAfter(dateFrom, dateTo)) {
				Notification notification = NotificationHelper.createNotification(
					I18nProperties.getString(Strings.headingIncorrectDateRange),
					I18nProperties.getString(Strings.messageIncorrectDateRange),
					NotificationHelper.Type.WARNING_MESSAGE,
					false);

				NotificationHelper.setDelayMsec(notification, -1);
				notification.open();
			}
		}
	}

	public PopupDateField getDateFromFilter() {
		return dateFromFilter;
	}

	public PopupDateField getDateToFilter() {
		return dateToFilter;
	}

	public CheckBox getIncludePartialMatch() {
		return includePartialMatch;
	}
}
