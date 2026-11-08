package de.symeda.sormas.ui.utils.components.multidayselector;

import java.time.LocalDate;
import java.util.Properties;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.ValidationResult;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.fields.CustomField;
import de.symeda.sormas.ui.utils.fields.DefaultDateFormat;

public class MultiDaySelectorField extends CustomField<MultiDaySelectorDto> {

	private static final long serialVersionUID = -5990656967372610518L;

	private final Binder<MultiDaySelectorDto> binder = new Binder<>(MultiDaySelectorDto.class);

	private final Checkbox multiDaySelect;
	private final DatePicker startDate;
	private final DatePicker endDate;
	private final DatePicker reportDate;

	protected Properties properties;

	public MultiDaySelectorField(DatePicker reportDate) {
		this.reportDate = reportDate;

		multiDaySelect = new Checkbox();
		startDate = new DatePicker();
		endDate = new DatePicker();
		DefaultDateFormat.applyOnAttach(startDate);
		DefaultDateFormat.applyOnAttach(endDate);

		this.properties = new Properties();
	}

	/**
	 * Built on first attach, as in Vaadin 8 ({@link CustomField}).
	 */
	@Override
	protected Component initContent() {
		setValue(new MultiDaySelectorDto());

		VerticalLayout layout = new VerticalLayout();
		layout.setSpacing(false);
		layout.setPadding(false);

		HorizontalLayout selectorLayout = new HorizontalLayout();

		multiDaySelect.setId("multiDaySelect");
		binder.forField(multiDaySelect).bind(MultiDaySelectorDto.MULTI_DAY);
		multiDaySelect.addValueChangeListener(e -> {
			getValue().setMultiDay(e.getValue());
			startDate.setVisible(e.getValue());
			startDate.setValue(null);
		});
		selectorLayout.add(multiDaySelect);

		HorizontalLayout datesLayout = new HorizontalLayout();

		startDate.setId("firstDate");
		startDate.setWidth("150px");
		startDate.setMax(LocalDate.now());
		startDate.setVisible(getValue().isMultiDay());
		Binder.BindingBuilder<MultiDaySelectorDto, LocalDate> startDateBindingBuilder = binder.forField(startDate)
			.withValidator(
				new DateComparisonValidator(
					endDate,
					true,
					I18nProperties.getValidationError(Validations.beforeDate, startDate.getLabel(), endDate.getLabel())));
		if (reportDate != null) {
			startDateBindingBuilder = startDateBindingBuilder.withValidator(
				new DateComparisonValidator(
					reportDate,
					true,
					I18nProperties.getValidationError(Validations.beforeDate, startDate.getLabel(), reportDate.getLabel())));
		}
		startDateBindingBuilder.bind(MultiDaySelectorDto.START_DATE);
		startDate.addValueChangeListener(e -> {
			binder.validate();
			getValue().setStartDate(e.getValue());
			enableValidationForEndDate(e.getValue() != null);
		});

		reportDate.addValueChangeListener(event -> binder.validate());

		endDate.setId("lastDate");
		endDate.setWidth("150px");
		Binder.BindingBuilder<MultiDaySelectorDto, LocalDate> endDateBindingBuilder =
			binder.forField(endDate).asRequired((localDate, valueContext) -> {
				if (multiDaySelect.getValue() != Boolean.TRUE || startDate.getValue() == null || localDate != null) {
					return ValidationResult.ok();
				}
				return ValidationResult.error("");
			})
				.withValidator(
					new DateComparisonValidator(
						startDate,
						false,
						I18nProperties.getValidationError(Validations.afterDate, endDate.getLabel(), startDate.getLabel())));
		if (reportDate != null) {
			endDateBindingBuilder = endDateBindingBuilder.withValidator(
				new DateComparisonValidator(
					reportDate,
					true,
					I18nProperties.getValidationError(Validations.beforeDate, endDate.getLabel(), reportDate.getLabel())));
		}
		endDateBindingBuilder.bind(MultiDaySelectorDto.END_DATE);
		endDate.setMax(LocalDate.now());
		endDate.addValueChangeListener(e -> getValue().setEndDate(e.getValue()));

		enableValidationForEndDate(false);

		datesLayout.add(startDate, endDate);
		layout.add(selectorLayout, datesLayout);

		return layout;
	}

	@Override
	protected void setPresentationValue(MultiDaySelectorDto multiDaySelectorDto) {
		binder.setBean(multiDaySelectorDto);
	}

	@Override
	protected MultiDaySelectorDto generateModelValue() {
		return binder.getBean();
	}

	@Override
	public MultiDaySelectorDto getValue() {
		return binder.getBean();
	}

	public BinderValidationStatus<MultiDaySelectorDto> validate() {
		return binder.validate();
	}

	public void showCaptions() {
		String prefix = properties.getProperty("prefix");
		multiDaySelect.setLabel(I18nProperties.getPrefixCaption(prefix, properties.getProperty("multiDay")));
		multiDaySelect.removeClassName(CssStyles.CAPTION_HIDDEN);
		startDate.setLabel(I18nProperties.getPrefixCaption(prefix, properties.getProperty("firstDate")));
		startDate.removeClassName(CssStyles.CAPTION_HIDDEN);
		endDate.setLabel(I18nProperties.getPrefixCaption(prefix, properties.getProperty("lastDate")));
		endDate.removeClassName(CssStyles.CAPTION_HIDDEN);
	}

	private void enableValidationForEndDate(boolean enable) {
		binder.getBinding(MultiDaySelectorDto.END_DATE).get().setAsRequiredEnabled(enable);
	}
}
