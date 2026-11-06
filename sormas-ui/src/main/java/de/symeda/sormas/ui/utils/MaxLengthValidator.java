package de.symeda.sormas.ui.utils;

import java.util.HashMap;
import java.util.Map;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Validations;

public class MaxLengthValidator extends com.vaadin.flow.data.validator.StringLengthValidator {

	public MaxLengthValidator(Integer maxLength) {
		super(I18nProperties.getValidationError(Validations.textTooLong, buildMessageParams(maxLength)), 0, maxLength);
	}

	private static Map<String, Object> buildMessageParams(Integer maxLength) {
		Map<String, Object> messageParams = new HashMap<>(1);
		messageParams.put("max", maxLength);

		return messageParams;
	}
}
