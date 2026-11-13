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
package de.symeda.sormas.ui;

import java.io.Serializable;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.ValidationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.utils.AccessDeniedException;
import de.symeda.sormas.api.utils.OutdatedEntityException;
import de.symeda.sormas.api.utils.ValidationRuntimeException;
import de.symeda.sormas.ui.utils.NotificationHelper;

public class SormasErrorHandler implements ErrorHandler {

	private static final long serialVersionUID = -8550777561547915589L;

	private static final SormasErrorHandler INSTANCE = new SormasErrorHandler();

	public static SormasErrorHandler get() {
		return INSTANCE;
	}

	@Override
	public void error(ErrorEvent event) {
		handleError(event);
	}

	public static void handleError(ErrorEvent event) {

		Logger logger = LoggerFactory.getLogger(SormasErrorHandler.class);
		final Throwable t = event.getThrowable();

		if (t instanceof SocketException) {
			// Most likely client browser closed socket
			logger.info("SocketException in CommunicationManager." + " Most likely client (browser) closed socket.");
			return;
		}

		ErrorMessage errorMessage = getErrorMessageForException(t);

		if (t != null) {
			t.printStackTrace();
			// log the error or warning
			if (errorMessage instanceof SystemError) {
				logger.error(getMessage(t), t);
			} else {
				logger.warn(t.getMessage(), t);
			}
		}

		// finds the original source of the error/exception. Flow does not name a component for every error that
		// Vaadin 8 attributed to one (e.g. an exception in a click listener), so the notification is also shown when
		// only the UI is known.
		Component component = event.getComponent().orElse(null);
		if (errorMessage != null && (component != null || UI.getCurrent() != null)) {
			if (errorMessage instanceof SystemError) {
				showNotification(
					I18nProperties.getString(Strings.errorOccurred, I18nProperties.getString(Strings.errorOccurred)),
					I18nProperties.getString(Strings.errorWasReported),
					false,
					NotificationVariant.LUMO_ERROR);
			} else {
				showNotification(
					I18nProperties.getString(Strings.errorProblemOccurred, I18nProperties.getString(Strings.errorProblemOccurred)),
					errorMessage.getFormattedHtmlMessage(),
					true,
					NotificationVariant.LUMO_WARNING);
				// Shows the error on the component (was setComponentError); Flow has it for fields only
				if (component instanceof HasValidation) {
					((HasValidation) component).setErrorMessage(errorMessage.getMessage());
					((HasValidation) component).setInvalid(true);
				}
			}
		}
	}

	/**
	 * A notification that stays until the user clicks it, as the Vaadin 8 error and (here) warning notifications did.
	 */
	private static void showNotification(String caption, String description, boolean htmlDescription, NotificationVariant variant) {

		Div captionDiv = new Div();
		captionDiv.setText(caption);
		Div descriptionDiv = new Div();
		if (htmlDescription) {
			descriptionDiv.getElement().setProperty("innerHTML", description);
		} else {
			descriptionDiv.setText(description);
		}
		Div content = new Div(captionDiv, descriptionDiv);
		content.addClassName(NotificationHelper.CLOSABLE_STYLE);

		Notification notification = new Notification(content);
		notification.setDuration(0);
		notification.setPosition(Notification.Position.MIDDLE);
		notification.addThemeVariants(variant);
		content.addClickListener(e -> notification.close());
		notification.open();
	}

	/**
	 * Taken and cleaned from AbstractErrorMessage
	 */
	public static ErrorMessage getErrorMessageForException(Throwable t) {

		//return AbstractErrorMessage.getErrorMessageForException(t)
		if (null == t) {
			return null;
		} else if (t instanceof com.vaadin.flow.data.binder.ValidationException) {
			// Binder validation (was the Vaadin 7 Validator.InvalidValueException / Buffered.SourceException):
			// no message, only the causes to be painted
			LocalUserError error = new LocalUserError(null);
			error.setErrorLevel(ErrorLevel.ERROR);
			com.vaadin.flow.data.binder.ValidationException validationException = (com.vaadin.flow.data.binder.ValidationException) t;
			validationException.getFieldValidationErrors()
				.forEach(e -> e.getMessage().ifPresent(m -> error.addCause(new LocalUserError(m, ContentMode.TEXT, ErrorLevel.ERROR))));
			for (ValidationResult result : validationException.getBeanValidationErrors()) {
				error.addCause(new LocalUserError(result.getErrorMessage(), ContentMode.TEXT, ErrorLevel.ERROR));
			}
			return error;
		} else {
			Throwable rootCause = ExceptionUtils.getRootCause(t);
			if (rootCause instanceof ValidationRuntimeException) {
				final LocalUserError error;
				if (rootCause instanceof OutdatedEntityException) {
					error = new LocalUserError(I18nProperties.getString(Strings.errorEntityOutdated), ContentMode.HTML, ErrorLevel.WARNING);
				} else if (rootCause instanceof AccessDeniedException) {
					error = new LocalUserError(
						rootCause.getMessage() + "<br>" + I18nProperties.getString(Strings.reloadPageToSeeChanges),
						ContentMode.HTML,
						ErrorLevel.WARNING);
				} else if (((ValidationRuntimeException) rootCause).getPropertyErrors() != null) {
					error = new LocalUserError(
						((ValidationRuntimeException) rootCause).getPropertyErrors()
							.entrySet()
							.stream()
							.map(e -> String.join(".", e.getKey()) + ": " + e.getValue())
							.collect(Collectors.joining("<br>")),
						ContentMode.HTML,
						ErrorLevel.WARNING);
				} else {
					error = new LocalUserError(rootCause.getMessage(), ContentMode.HTML, ErrorLevel.WARNING);
				}

				return error;

			} else if (rootCause instanceof ValidationException) {
				return new LocalUserError(rootCause.getMessage(), ContentMode.HTML, ErrorLevel.WARNING);
			} else if (rootCause instanceof NO_PERMISSION) {
				return new LocalUserError(I18nProperties.getString(Strings.errorForbidden), ContentMode.TEXT, ErrorLevel.ERROR);
			} else {
				LoggerFactory.getLogger(SormasErrorHandler.class)
					.info("SocketException in CommunicationManager." + " Most likely client (browser) closed socket.");

				String message = t.getMessage();
				if (message == null) {
					message = I18nProperties.getString(Strings.errorOccurred);
				}
				return new SystemError(message);
			}
		}
	}

	/**
	 * @return A more explicit plain message if available.
	 */
	private static String getMessage(Throwable throwable) {

		Throwable rootCause = ExceptionUtils.getRootCause(throwable);

		String message;
		if (rootCause instanceof SystemException) {

			// Exception coming from Backend: Extract last Exception message from server-side StackTrace
			try {
				String[] causeBlocks = rootCause.getMessage().split("Caused by: ");
				String lastCauseBlock = causeBlocks[causeBlocks.length - 1];
				message = lastCauseBlock.split("\n\tat ")[0];
			} catch (ArrayIndexOutOfBoundsException e) {
				// In case this fails somehow return to default behaviour
				message = rootCause.getMessage();
			}
		} else {

			// Usually the rootCause message is more meaningful than the top
			message = rootCause.getMessage();
		}

		// Discard extracted message if it is empty
		if (StringUtils.isBlank(message)) {
			message = throwable.getMessage();
		}

		return message;
	}

	/**
	 * Content mode of an {@link ErrorMessage} (was {@code com.vaadin.server.AbstractErrorMessage.ContentMode}).
	 */
	public enum ContentMode {
		TEXT,
		PREFORMATTED,
		HTML
	}

	/**
	 * Level of an {@link ErrorMessage} (was {@code com.vaadin.shared.ui.ErrorLevel}).
	 */
	public enum ErrorLevel {
		INFO,
		WARNING,
		ERROR,
		CRITICAL,
		SYSTEM
	}

	/**
	 * Error message with nested causes, as Vaadin 8's {@code AbstractErrorMessage} (removed in Flow): the handler shows
	 * {@link #getFormattedHtmlMessage()} in its notification.
	 */
	public static class ErrorMessage implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String message;
		private final ContentMode mode;
		private ErrorLevel errorLevel;
		private final List<ErrorMessage> causes = new ArrayList<>();

		protected ErrorMessage(String message, ContentMode mode, ErrorLevel errorLevel) {
			this.message = message;
			this.mode = mode;
			this.errorLevel = errorLevel;
		}

		public String getMessage() {
			return message;
		}

		public ErrorLevel getErrorLevel() {
			return errorLevel;
		}

		protected void setErrorLevel(ErrorLevel level) {
			this.errorLevel = level;
		}

		public List<ErrorMessage> getCauses() {
			return causes;
		}

		protected void addCause(ErrorMessage cause) {
			causes.add(cause);
		}

		/**
		 * Same rules as Vaadin 8's {@code AbstractErrorMessage.getFormattedHtmlMessage()}: the escaped or HTML message,
		 * or else the messages of the causes in one {@code <div>} each.
		 */
		public String getFormattedHtmlMessage() {

			String result = null;
			if (message != null) {
				switch (mode) {
				case TEXT:
					result = escapeHtml(message);
					break;
				case PREFORMATTED:
					result = "<pre>" + escapeHtml(message) + "</pre>";
					break;
				case HTML:
					result = message;
					break;
				}
			}
			if (result == null && !causes.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				for (ErrorMessage cause : causes) {
					String childMessage = cause.getFormattedHtmlMessage();
					if (childMessage != null && !childMessage.isEmpty()) {
						sb.append("<div>");
						sb.append(childMessage);
						sb.append("</div>\n");
					}
				}
				if (sb.length() > 0) {
					result = sb.toString();
				}
			}
			return result != null ? result : "";
		}

		private static String escapeHtml(String text) {
			return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
		}
	}

	/**
	 * Unexpected error: logged with its stack trace; the user gets a generic notification (was Vaadin 8's
	 * {@code SystemError}).
	 */
	private static final class SystemError extends ErrorMessage {

		private static final long serialVersionUID = 1L;

		public SystemError(String message) {
			super(message, ContentMode.HTML, ErrorLevel.SYSTEM);
		}
	}

	private static final class LocalUserError extends ErrorMessage {

		private static final long serialVersionUID = 1L;

		public LocalUserError(String textErrorMessage) {
			super(textErrorMessage, ContentMode.TEXT, ErrorLevel.ERROR);
		}

		public LocalUserError(String message, ContentMode contentMode, ErrorLevel errorLevel) {
			super(message, contentMode, errorLevel);
		}
	}
}
