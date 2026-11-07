package de.symeda.sormas.ui.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.symeda.sormas.api.person.SymptomJournalStatus;
import de.symeda.sormas.api.sormastosormas.SormasToSormasShareableDto;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.dialog.Dialog;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.externaljournal.ExternalJournalFacade;
import de.symeda.sormas.api.externaljournal.ExternalJournalValidation;
import de.symeda.sormas.api.externaljournal.patientdiary.PatientDiaryResult;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.ui.SormasUI;

public class ExternalJournalUtil {

	private final static ExternalJournalFacade externalJournalFacade = FacadeProvider.getExternalJournalFacade();

	/**
	 * Creates a Button to be added wherever a patient needs to be managed by an external journal.
	 * If no external journal is enabled with the current settings, an empty optional is returned.
	 * If the person is not registered in the external journal, a create account/register button is returned
	 * If the person is registered, a button is returned which opens a popup with further options.
	 *
	 * The button is:
	 * - enabled if the associated sormasToSormasEntity(case or contact) is not shared or it is shared and the current SORMAS instance has
	 * ownership
	 * - disabled if the associated sormasToSormasEntity(case or contact) is shared and the current SORMAS instance does not have ownership
	 *
	 * @param person
	 *            person to be managed by the external journal
	 * @param sormasToSormasEntity
	 *            the associated case or contact
	 * @return Optional containing appropriate Button
	 */
	public static Optional<Button> getExternalJournalUiButton(PersonDto person, SormasToSormasShareableDto sormasToSormasEntity) {
		Optional<Button> externalJournalUiButton = getExternalJournalUiButton(person);
		return externalJournalUiButton.map(button -> {
			boolean buttonEnabled = !sormasToSormasEntity.isOwnershipHandedOver()
				&& (sormasToSormasEntity.getSormasToSormasOriginInfo() == null
					|| sormasToSormasEntity.getSormasToSormasOriginInfo().isOwnershipHandedOver());
			button.setEnabled(buttonEnabled);
			return button;
		});
	}

	private static Optional<Button> getExternalJournalUiButton(PersonDto person) {
		if (FacadeProvider.getConfigFacade().getSymptomJournalConfig().isActive()) {
			if (person.isEnrolledInExternalJournal()) {
				return Optional.of(createSymptomJournalOptionsButton(person));
			} else {
				return Optional.of(createSymptomJournalRegisterButton(person));
			}
		} else if (FacadeProvider.getConfigFacade().getPatientDiaryConfig().isActive()) {
			if (person.isEnrolledInExternalJournal()) {
				return Optional.of(createPatientDiaryOptionsButton(person));
			} else {
				return Optional.of(createPatientDiaryRegisterButton(person));
			}

		}
		return Optional.empty();
	}

	private static Button createSymptomJournalOptionsButton(PersonDto person) {
		VerticalLayout popupLayout = new VerticalLayout();
		popupLayout.setSpacing(true);
		popupLayout.setPadding(true);
		popupLayout.addClassName(CssStyles.LAYOUT_MINIMAL);
		PopupButton ediaryButton =
			ButtonHelper.createPopupButton(I18nProperties.getCaption(Captions.symptomJournalOptionsButton), popupLayout, CssStyles.BUTTON_PRIMARY);
		ComponentEventListener<ClickEvent<Button>> openListener = clickEvent -> {
			openSymptomJournalWindow(person);
			ediaryButton.setPopupVisible(false);
		};
		// TODO: implement cancel for PIA
		ComponentEventListener<ClickEvent<Button>> cancelListener = clickEvent -> {
			showForceDeletionPrompt(I18nProperties.getString(Strings.messageDeletionUnsupportedByExternalJournalWarning), person);
		};
		Button cancelButton =
			ButtonHelper.createButton(I18nProperties.getCaption(Captions.cancelExternalFollowUpButton), cancelListener, CssStyles.BUTTON_PRIMARY);
		Button openButton =
			ButtonHelper.createButton(I18nProperties.getCaption(Captions.openInSymptomJournalButton), openListener, CssStyles.BUTTON_PRIMARY);
		popupLayout.add(cancelButton);
		popupLayout.add(openButton);
		return ediaryButton;
	}

	private static Button createSymptomJournalRegisterButton(PersonDto person) {
		Button btnCreateSymptomJournalAccount = ButtonHelper.createButton(I18nProperties.getCaption(Captions.createSymptomJournalAccountButton));
		CssStyles.style(btnCreateSymptomJournalAccount, CssStyles.BUTTON_PRIMARY);
		btnCreateSymptomJournalAccount.addClickListener(clickEvent -> enrollPatientInSymptomJournal(person));
		return btnCreateSymptomJournalAccount;
	}

	private static Button createPatientDiaryOptionsButton(PersonDto person) {
		VerticalLayout popupLayout = new VerticalLayout();
		popupLayout.setSpacing(true);
		popupLayout.setPadding(true);
		popupLayout.addClassName(CssStyles.LAYOUT_MINIMAL);
		PopupButton ediaryButton =
			ButtonHelper.createPopupButton(I18nProperties.getCaption(Captions.patientDiaryOptionsButton), popupLayout, CssStyles.BUTTON_PRIMARY);
		ComponentEventListener<ClickEvent<Button>> cancelListener = clickEvent -> {
			showCancelFollowUpConfirmationPopup(person);
			ediaryButton.setPopupVisible(false);
		};
		ComponentEventListener<ClickEvent<Button>> openListener = clickEvent -> {
			openPatientDiaryPage(person.getUuid());
			ediaryButton.setPopupVisible(false);
		};
		Button cancelButton =
			ButtonHelper.createButton(I18nProperties.getCaption(Captions.cancelExternalFollowUpButton), cancelListener, CssStyles.BUTTON_PRIMARY);
		Button openButton =
			ButtonHelper.createButton(I18nProperties.getCaption(Captions.openInPatientDiaryButton), openListener, CssStyles.BUTTON_PRIMARY);
		popupLayout.add(cancelButton);
		popupLayout.add(openButton);
		return ediaryButton;
	}

	private static Button createPatientDiaryRegisterButton(PersonDto person) {
		Button btnPatientDiaryAccount = ButtonHelper.createButton(I18nProperties.getCaption(Captions.registerInPatientDiaryButton));
		CssStyles.style(btnPatientDiaryAccount, CssStyles.BUTTON_PRIMARY);
		btnPatientDiaryAccount.addClickListener(clickEvent -> enrollPatientInPatientDiary(person));
		return btnPatientDiaryAccount;
	}

	private static void enrollPatientInSymptomJournal(PersonDto person) {
		ExternalJournalValidation validationResult = externalJournalFacade.validateSymptomJournalPerson(person);
		if (!validationResult.isValid()) {
			showExternalJournalWarningPopup(validationResult.getMessage());
		} else {
			openSymptomJournalWindow(person);
		}
	}

	/**
	 * Opens a window that contains an iFrame with the symptom journal website specified in the properties.
	 * The steps to build that iFrame are:
	 * 1. Request an authentication token based on the stored client ID and secret
	 * 2. Build an HTML page containing a form with the auth token and some personal details as parameters
	 * 3. The form is automatically submitted and replaced by the iFrame
	 */
	private static void openSymptomJournalWindow(PersonDto person) {
		String authToken = externalJournalFacade.getSymptomJournalAuthToken();
		// was a BrowserFrame showing a StreamResource
		IFrame frame = new IFrame();
		frame.setSrc(new StreamSourceDownloadHandler(() -> {
			String formUrl = FacadeProvider.getConfigFacade().getSymptomJournalConfig().getUrl();
			Map<String, String> parameters = new LinkedHashMap<>();
			parameters.put("token", authToken);
			parameters.put("uuid", person.getUuid());
			parameters.put("firstname", person.getFirstName());
			parameters.put("lastname", person.getLastName());
			parameters.put("email", person.getEmailAddress(false));
			byte[] document = createSymptomJournalForm(formUrl, parameters);

			return new ByteArrayInputStream(document);
		}, "symptomJournal.html", "text/html").inline());
		frame.setWidth("100%");
		frame.setHeight("100%");

		Dialog window = VaadinUiUtil.createPopupWindow();
		window.add(frame);
		window.setHeaderTitle(I18nProperties.getString(Strings.headingSymptomJournalAccountCreation));
		window.setWidth("80%");
		window.setHeight("80%");

		window.open();
	}

	/**
	 * @return An HTML page containing a form that is automatically submitted in order to display the symptom journal iFrame
	 */
	private static byte[] createSymptomJournalForm(String formUrl, Map<String, String> inputs) {
		Document document;
		try (InputStream in = ExternalJournalUtil.class.getResourceAsStream("/symptomJournal.html")) {
			document = Jsoup.parse(in, StandardCharsets.UTF_8.name(), formUrl);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		Element form = document.getElementById("form");
		form.attr("action", formUrl);
		Element parametersElement = form.getElementById("parameters");

		inputs.forEach((k, v) -> parametersElement.appendChild(new Element("input").attr("type", "hidden").attr("name", k).attr("value", v)));
		return document.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static void showCancelFollowUpConfirmationPopup(PersonDto personDto) {
		VaadinUiUtil.showConfirmationPopup(
			I18nProperties.getCaption(Captions.cancelExternalFollowUpPopupTitle),
			new Span(I18nProperties.getString(Strings.confirmationCancelExternalFollowUpPopup)),
			I18nProperties.getString(Strings.yes),
			I18nProperties.getString(Strings.no),
			600,
			confirmed -> {
				if (confirmed)
					cancelPatientDiaryFollowUp(personDto);
			});
	}

	private static void cancelPatientDiaryFollowUp(PersonDto personDto) {
		PatientDiaryResult result = externalJournalFacade.cancelPatientDiaryFollowUp(personDto);
		if (result.isSuccess()) {
			showPatientDiaryResultPopup(result, null);
			SormasUI.refreshView();
		} else {
			showForceDeletionPrompt(result.getMessage(), personDto);
		}
	}

	private static void openPatientDiaryPage(String personUuid) {
		String url = FacadeProvider.getConfigFacade().getPatientDiaryConfig().getUrl();
		url += "/data?q=" + personUuid + "&queryKey=sicFieldIdentifier";
		String authToken = externalJournalFacade.getPatientDiaryAuthToken(true);
		if (StringUtils.isNotEmpty(authToken)) {
			url += "&token=" + authToken;
		}
		UI.getCurrent().getPage().open(url, "_blank");
	}

	private static void enrollPatientInPatientDiary(PersonDto person) {
		ExternalJournalValidation validationResult = externalJournalFacade.validatePatientDiaryPerson(person);
		if (!validationResult.isValid()) {
			showExternalJournalWarningPopup(validationResult.getMessage());
		} else {
			PatientDiaryResult registerResult = externalJournalFacade.registerPatientDiaryPerson(person);
			showPatientDiaryResultPopup(registerResult, I18nProperties.getCaption(Captions.patientDiaryRegistrationError));
			if (registerResult.isSuccess()) {
				SormasUI.refreshView();
			}
		}
	}

	private static void showExternalJournalWarningPopup(String message) {
		VerticalLayout warningLayout = new VerticalLayout();
		warningLayout.setPadding(true);
		Image warningIcon = new Image(VaadinUiUtil.THEME_IMAGE_PATH + "img/warning-icon.png", "");
		warningIcon.setHeight("35px");
		warningIcon.setWidth("35px");
		warningLayout.addComponentAsFirst(warningIcon);
		Dialog popupWindow = VaadinUiUtil.showPopupWindow(warningLayout);
		Span messageLabel = new Span(I18nProperties.getValidationError(Validations.externalJournalPersonValidationError));
		CssStyles.style(messageLabel, CssStyles.LABEL_LARGE, CssStyles.LABEL_WHITE_SPACE_NORMAL);
		warningLayout.add(messageLabel);
		Span infoLabel = new Span(message);
		CssStyles.style(infoLabel, CssStyles.LABEL_LARGE, CssStyles.LABEL_WHITE_SPACE_NORMAL);
		warningLayout.add(infoLabel);
		CssStyles.style(warningLayout, CssStyles.ALIGN_CENTER);
		VaadinUiUtil.addCloseListener(popupWindow, e -> popupWindow.close());
		popupWindow.setWidth("400px");
	}

	private static void showPatientDiaryResultPopup(PatientDiaryResult result, String errorMessage) {
		VerticalLayout resultLayout = new VerticalLayout();
		resultLayout.setPadding(true);
		Image errorIcon = new Image(VaadinUiUtil.THEME_IMAGE_PATH + "img/error-icon.png", "");
		errorIcon.setHeight("35px");
		errorIcon.setWidth("35px");
		Image successIcon = new Image(VaadinUiUtil.THEME_IMAGE_PATH + "img/success-icon.png", "");
		successIcon.setHeight("35px");
		successIcon.setWidth("35px");
		CssStyles.style(resultLayout, CssStyles.ALIGN_CENTER);
		if (result.isSuccess()) {
			// (removeComponent of the other icon, never in the layout, was a no-op)
			resultLayout.addComponentAsFirst(successIcon);
		} else {
			resultLayout.addComponentAsFirst(errorIcon);
			Span infoLabel = new Span();
			CssStyles.style(infoLabel, CssStyles.LABEL_LARGE, CssStyles.LABEL_WHITE_SPACE_NORMAL);
			resultLayout.add(infoLabel);
			infoLabel.setText(errorMessage);
		}
		Span messageLabel = new Span();
		CssStyles.style(messageLabel, CssStyles.LABEL_LARGE, CssStyles.LABEL_WHITE_SPACE_NORMAL);
		resultLayout.add(messageLabel);
		messageLabel.setText(result.getMessage());
		Dialog popupWindow = VaadinUiUtil.showPopupWindow(resultLayout);
		VaadinUiUtil.addCloseListener(popupWindow, e -> popupWindow.close());
		popupWindow.setWidth("400px");
	}

	private static void showForceDeletionPrompt(String errorMessage, PersonDto person) {

		HorizontalLayout errorLayout = VaadinUiUtil.createErrorComponent(errorMessage);
		Span forceDeletionQuestion = new Span(I18nProperties.getString(Strings.promptExternalJournalForceDeletion));
		forceDeletionQuestion.setWidth("500px");
		errorLayout.add(forceDeletionQuestion);
		VerticalLayout errorAndQuestion = new VerticalLayout(errorLayout, forceDeletionQuestion);

		Consumer<Boolean> resultConsumer = new Consumer<Boolean>() {

			@Override
			public void accept(Boolean forceDelete) {
				if (forceDelete) {
					person.setSymptomJournalStatus(SymptomJournalStatus.DELETED);
					FacadeProvider.getPersonFacade().save(person);
					SormasUI.refreshView();
				}
			}
		};
		VaadinUiUtil.showChooseOptionPopup(
			I18nProperties.getCaption(Captions.patientDiaryCancelError),
			errorAndQuestion,
			I18nProperties.getString(Strings.yes),
			I18nProperties.getString(Strings.no),
			600,
			resultConsumer);
	}
}
