package de.symeda.sormas.ui.configuration.linelisting;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.notification.Notification;

import de.symeda.sormas.ui.utils.fields.DefaultDateFormat;
import de.symeda.sormas.ui.utils.grid.Grid;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;

import de.symeda.sormas.api.feature.FeatureConfigurationIndexDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.UtilDate;

@SuppressWarnings("serial")
public class LineListingConfigurationsGrid extends Grid<FeatureConfigurationIndexDto> {

	private boolean nationLevel;
	private List<FeatureConfigurationIndexDto> configurations;
	private Set<FeatureConfigurationIndexDto> changedConfigurations;
	private Map<FeatureConfigurationIndexDto, DatePicker> dateFieldMap;

	public LineListingConfigurationsGrid(List<FeatureConfigurationIndexDto> configurations, boolean nationLevel) {

		this.nationLevel = nationLevel;
		this.configurations = configurations;
		this.changedConfigurations = new HashSet<>();
		this.dateFieldMap = new HashMap<>();
		buildGrid();
		reload();
	}

	public void enableAll() {

		configurations.stream().forEach(config -> {
			config.setEnabled(true);
			if (config.getEndDate() == null) {
				config.setEndDate(DateHelper.addDays(new Date(), 21));
			}
		});
		changedConfigurations.addAll(configurations);
		reload();
	}

	public void disableAll() {
		configurations.stream().forEach(config -> {
			config.setEnabled(false);
			config.setEndDate(null);
		});
		changedConfigurations.addAll(configurations);
		reload();
	}

	public void setEndDateForAll(LocalDate endDate) {
		configurations.stream().forEach(config -> config.setEndDate(UtilDate.from(endDate)));
		changedConfigurations.addAll(configurations);
		reload();
	}

	public boolean validateDates() {
		for (FeatureConfigurationIndexDto config : configurations) {
			if (dateFieldMap.get(config) != null && dateFieldMap.get(config).isInvalid()) {
				return false;
			}
		}

		return true;
	}

	private void buildGrid() {

		setSelectionMode(SelectionMode.NONE);

		if (nationLevel) {
			addColumn(FeatureConfigurationIndexDto::getRegionName)
				.setCaption(I18nProperties.getPrefixCaption(FeatureConfigurationIndexDto.I18N_PREFIX, FeatureConfigurationIndexDto.REGION_NAME));
		}

		addColumn(FeatureConfigurationIndexDto::getDistrictName)
			.setCaption(I18nProperties.getPrefixCaption(FeatureConfigurationIndexDto.I18N_PREFIX, FeatureConfigurationIndexDto.DISTRICT_NAME));
		addComponentColumn(config -> {
			Checkbox cbActive = new Checkbox();
			cbActive.setValue(config.isEnabled());
			cbActive.addValueChangeListener(e -> {
				config.setEnabled(e.getValue());
				if (Boolean.TRUE.equals(e.getValue())) {
					config.setEndDate(UtilDate.from(LocalDate.now().plusDays(21)));
					dateFieldMap.get(config).setValue(LocalDate.now().plusDays(21));
				} else {
					config.setEndDate(null);
					dateFieldMap.get(config).setValue(null);
				}
				changedConfigurations.add(config);
			});
			return cbActive;
		}).setCaption(I18nProperties.getPrefixCaption(FeatureConfigurationIndexDto.I18N_PREFIX, FeatureConfigurationIndexDto.ENABLED));
		addComponentColumn(config -> {
			DatePicker dfEndDate = new DatePicker();
			DefaultDateFormat.applyOnAttach(dfEndDate);
			dfEndDate.setValue(UtilDate.toLocalDate(config.getEndDate()));
			dfEndDate.addValueChangeListener(e -> {
				if (e.getValue() != null && e.getValue().isBefore(LocalDate.now())) {
					Notification errorNotification = NotificationHelper.createNotification(
						I18nProperties.getString(Strings.headingInvalidDateEntered),
						I18nProperties.getValidationError(Validations.noPastDateAllowed),
						Type.TRAY_NOTIFICATION,
						false);
					errorNotification.addClassNames("tray", "notification-error");
					errorNotification.open();
				} else {
					config.setEndDate(UtilDate.from(e.getValue()));
					changedConfigurations.add(config);
				}
			});
			dfEndDate.setMin(LocalDate.now());
			dateFieldMap.put(config, dfEndDate);
			return dfEndDate;
		}).setCaption(I18nProperties.getPrefixCaption(FeatureConfigurationIndexDto.I18N_PREFIX, FeatureConfigurationIndexDto.END_DATE));
	}

	public void reload() {
		setItems(configurations);
	}

	public Set<FeatureConfigurationIndexDto> getChangedConfigurations() {
		return changedConfigurations;
	}
}
