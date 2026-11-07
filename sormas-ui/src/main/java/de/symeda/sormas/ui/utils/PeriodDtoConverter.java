package de.symeda.sormas.ui.utils;


import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

import de.symeda.sormas.api.therapy.PeriodDto;

public class PeriodDtoConverter implements Converter<String, PeriodDto> {

	private static final long serialVersionUID = 4119205056759462799L;

	@Override
	public Result<PeriodDto> convertToModel(String value, ValueContext context) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public String convertToPresentation(PeriodDto value, ValueContext context) {
		return value == null ? "" : DateFormatHelper.buildPeriodString(value.getStart(), value.getEnd());
	}

	public Class<PeriodDto> getModelType() {
		return PeriodDto.class;
	}

	public Class<String> getPresentationType() {
		return String.class;
	}
}
