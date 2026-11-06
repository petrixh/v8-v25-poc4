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

import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.shared.Registration;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.utils.fields.TextField;

public class ValidationUtils {

	private static final String DATA_LISTENER_REGISTRATION = "sormas.componentErrorValidator";

	/**
	 * V8 {@code com.vaadin.shared.ui.ErrorLevel} for {@link #initComponentErrorValidator}. Flow has one invalid state:
	 * both levels show the message as the field's error message.
	 */
	public enum ErrorLevel {
		ERROR,
		WARNING
	}

	private ValidationUtils() {
	}

	public static void initComponentErrorValidator(
		TextField field,
		String initialFieldValue,
		String validationMessageTag,
		Component warningLabel,
		Function<String, Boolean> exists) {
		initComponentErrorValidator(field, initialFieldValue, validationMessageTag, warningLabel, exists, ErrorLevel.ERROR);
	}

	public static void initComponentErrorValidator(
		TextField field,
		String initialFieldValue,
		String validationMessageTag,
		Component warningLabel,
		Function<String, Boolean> isInvlaid,
		ErrorLevel errorLevel) {
		if (field == null) {
			return;
		}

		// already initialized
		Object oldRegistration = ComponentUtil.getData(field, DATA_LISTENER_REGISTRATION);
		if (oldRegistration instanceof Registration) {
			// remove old listener
			((Registration) oldRegistration).remove();
		}

		Function<String, Void> validateExternalToken = (String value) -> {
			if (field.isVisible() && !StringUtils.isEmpty(value) && isInvlaid.apply(value)) {
				field.setErrorMessage(I18nProperties.getValidationError(validationMessageTag));
				field.setInvalid(true);
				warningLabel.setVisible(true);
			} else {
				field.setErrorMessage(null);
				field.setInvalid(false);
				warningLabel.setVisible(false);
			}

			return null;
		};

		validateExternalToken.apply(initialFieldValue);
		Registration registration = field.addValueChangeListener(f -> validateExternalToken.apply(f.getValue()));
		ComponentUtil.setData(field, DATA_LISTENER_REGISTRATION, registration);
	}
}
