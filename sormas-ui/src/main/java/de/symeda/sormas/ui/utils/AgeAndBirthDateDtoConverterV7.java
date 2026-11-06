package de.symeda.sormas.ui.utils;


import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

import de.symeda.sormas.api.caze.AgeAndBirthDateDto;
import de.symeda.sormas.api.person.PersonHelper;

public class AgeAndBirthDateDtoConverterV7 implements Converter<String, AgeAndBirthDateDto> {

	private static final long serialVersionUID = 4515813046485937433L;

	@Override
	public Result<AgeAndBirthDateDto> convertToModel(String value, ValueContext context) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public String convertToPresentation(AgeAndBirthDateDto value, ValueContext context) {

		if (value == null) {
			return "";
		}

		return PersonHelper.getAgeAndBirthdateString(
			value.getAge(),
			value.getAgeType(),
			value.getDateOfBirthDD(),
			value.getDateOfBirthMM(),
			value.getDateOfBirthYYYY());
	}

	public Class<AgeAndBirthDateDto> getModelType() {
		return AgeAndBirthDateDto.class;
	}

	public Class<String> getPresentationType() {
		return String.class;
	}
}
