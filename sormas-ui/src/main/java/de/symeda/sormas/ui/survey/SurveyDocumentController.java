/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2025 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.survey;

import static de.symeda.sormas.ui.utils.LayoutUtil.fluidRowLocs;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.Unit;
import de.symeda.sormas.ui.utils.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import de.symeda.sormas.ui.utils.NotificationHelper;
import com.vaadin.ui.TextField;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.symeda.sormas.ui.utils.fields.ComboBox;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.ReferenceDto;
import de.symeda.sormas.api.docgeneneration.DocumentTemplateException;
import de.symeda.sormas.api.docgeneneration.DocumentVariables;
import de.symeda.sormas.api.docgeneneration.RootEntityType;
import de.symeda.sormas.api.externalemail.AttachmentException;
import de.symeda.sormas.api.externalemail.ExternalEmailException;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.survey.SurveyDocumentOptionsDto;
import de.symeda.sormas.api.survey.SurveyDto;
import de.symeda.sormas.api.utils.ValidationException;
import de.symeda.sormas.ui.utils.AbstractEditForm;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.FieldHelper;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.components.MultilineLabel;

public class SurveyDocumentController {

	public void sendSurveyDocument(
		RootEntityType rootEntityType,
		ReferenceDto rootEntityReference,
		Disease disease,
		PersonDto person,
		Runnable callback) {
		boolean attachmentAvailable = FacadeProvider.getExternalEmailFacade().isAttachmentAvailable(person.toReference());
		SurveyDocumentOptionsForm form = new SurveyDocumentOptionsForm(disease, true, person, attachmentAvailable);
		form.setWidth(600, Unit.PIXELS);
		SurveyDocumentOptionsDto options = new SurveyDocumentOptionsDto(rootEntityType, rootEntityReference);
		options.setRecipientEmail(person.getEmailAddress(true));

		form.setValue(options);
		CommitDiscardWrapperComponent<SurveyDocumentOptionsForm> editView = new CommitDiscardWrapperComponent<>(form, form.getFieldGroup());
		editView.getCommitButton().setCaption(I18nProperties.getCaption(Captions.surveySend));

		if (attachmentAvailable) {
			editView.addCommitListener(() -> {
				SurveyDocumentOptionsDto surveyOptions = form.getValue();
				try {
					FacadeProvider.getSurveyFacade().sendDocument(surveyOptions);
					callback.run();
				} catch (DocumentTemplateException | ValidationException e) {
					NotificationHelper.show(
						String.format(I18nProperties.getString(Strings.errorDocumentGeneration), surveyOptions.getSurvey().getCaption()),
						e.getMessage(),
						NotificationHelper.Type.ERROR_MESSAGE,
						false);
				} catch (AttachmentException | IOException | ExternalEmailException e) {
					NotificationHelper.show(
						I18nProperties.getString(Strings.headingErrorSendingExternalEmail),
						e.getMessage(),
						NotificationHelper.Type.ERROR_MESSAGE,
						false);
				}
			});
		} else {
			editView.getCommitButton().setEnabled(false);
		}
		form.setSurveyErrorCallback(inError -> {
			editView.getCommitButton().setEnabled(!inError);
		});

		VaadinUiUtil.showModalPopupWindow(editView, I18nProperties.getString(Strings.headingSurveySendDocument));
	}

	public void generateSurveyDocument(RootEntityType rootEntityType, ReferenceDto rootEntityReference, Disease disease, Runnable callback) {
		SurveyDocumentOptionsForm form = new SurveyDocumentOptionsForm(disease, false, null, true);
		form.setWidth(600, Unit.PIXELS);
		form.setValue(new SurveyDocumentOptionsDto(rootEntityType, rootEntityReference));

		CommitDiscardWrapperComponent<SurveyDocumentOptionsForm> editView = new CommitDiscardWrapperComponent<>(form, form.getFieldGroup());
		editView.getCommitButton().setCaption(I18nProperties.getCaption(Captions.surveyGenerate));

		editView.addCommitListener(() -> {
			SurveyDocumentOptionsDto surveyOptions = form.getValue();
			try {
				FacadeProvider.getSurveyFacade().generateDocument(surveyOptions);
				callback.run();
			} catch (DocumentTemplateException | ValidationException e) {
				NotificationHelper.show(
					String.format(I18nProperties.getString(Strings.errorDocumentGeneration), surveyOptions.getSurvey().getCaption()),
					e.getMessage(),
					e instanceof ValidationException ? NotificationHelper.Type.WARNING_MESSAGE : NotificationHelper.Type.ERROR_MESSAGE,
					false);
			}
		});

		form.getSurveyCombo().addValueChangeListener(e -> {
			SurveyDto survey = (SurveyDto) e.getValue();
			editView.getCommitButton().setEnabled(survey.getDocumentTemplate() != null);
		});

		VaadinUiUtil.showModalPopupWindow(editView, I18nProperties.getString(Strings.headingSurveyGenerateDocument));
	}

	private static class SurveyDocumentOptionsForm extends AbstractEditForm<SurveyDocumentOptionsDto> {

		private static final long serialVersionUID = 360077311341123575L;

		private static final String SURVEY_WARNING_LOC = "surveyWarningLoc";
		private static final String TEMPLATE_ADDITIONAL_VARIABLES_LOC = "templateAdditionalVariablesLoc";
		private static final String ATTACHMENT_NOT_AVAILABLE_INFO_LOC = "attachmentNotAvailableInfoLoc";

		private static final String HTML_LAYOUT = fluidRowLocs(SurveyDocumentOptionsDto.RECIPIENT_EMAIL)
			+ fluidRowLocs(SurveyDocumentOptionsDto.SURVEY)
			+ fluidRowLocs(SURVEY_WARNING_LOC)
			+ fluidRowLocs(TEMPLATE_ADDITIONAL_VARIABLES_LOC)
			+ fluidRowLocs(ATTACHMENT_NOT_AVAILABLE_INFO_LOC);

		private final Disease disease;
		private final boolean forEmail;
		private final PersonDto person;
		private final boolean attachmentAvailable;

		private VerticalLayout additionalVariablesComponent;
		private Consumer<Boolean> surveyErrorCallback;

		protected SurveyDocumentOptionsForm(Disease disease, boolean forEmail, PersonDto person, boolean attachmentAvailable) {
			super(SurveyDocumentOptionsDto.class, SurveyDocumentOptionsDto.I18N_PREFIX, false);
			this.disease = disease;
			this.forEmail = forEmail;
			this.person = person;
			this.attachmentAvailable = attachmentAvailable;
			this.surveyErrorCallback = e -> {
			};

			addFields();
			hideValidationUntilNextCommit();
		}

		@Override
		protected String createHtmlLayout() {
			return HTML_LAYOUT;
		}

		@Override
		protected void addFields() {
			if (forEmail) {
				ComboBox recipientEmailCombo = addField(SurveyDocumentOptionsDto.RECIPIENT_EMAIL, ComboBox.class);
				recipientEmailCombo.setRequired(true);
				List<String> recipientEmails = person.getAllEmailAddresses();
				FieldHelper.updateItems(recipientEmailCombo, recipientEmails);
				String primaryEmailAddress = person.getEmailAddress(true);
				if (StringUtils.isNotBlank(primaryEmailAddress)) {
					recipientEmailCombo
						.setItemCaption(primaryEmailAddress, primaryEmailAddress + " (" + I18nProperties.getCaption(Captions.primarySuffix) + ")");
				}
			}

			ComboBox surveyCombo = addCustomField(SurveyDocumentOptionsDto.SURVEY, SurveyDto.class, ComboBox.class);
			surveyCombo.setRequired(true);
			List<SurveyDto> surveys = FacadeProvider.getSurveyFacade().getAllByDisease(disease);
			FieldHelper.updateItems(surveyCombo, surveys);
			surveys.forEach(survey -> surveyCombo.setItemCaption(survey, survey.getName()));

			Span surveyWarningLabel = new Span();
			surveyWarningLabel.addClassName(CssStyles.LABEL_WARNING);
			surveyWarningLabel.setVisible(false);
			getContent().addComponent(surveyWarningLabel, SURVEY_WARNING_LOC);

			additionalVariablesComponent = new VerticalLayout();
			additionalVariablesComponent.setSpacing(false);
			additionalVariablesComponent.setMargin(new MarginInfo(false, false, true, false));
			getContent().addComponent(additionalVariablesComponent, TEMPLATE_ADDITIONAL_VARIABLES_LOC);

			surveyCombo.addValueChangeListener(e -> {
				SurveyDto survey = (SurveyDto) e.getValue();
				additionalVariablesComponent.removeAll();
				surveyErrorCallback.accept(false);
				surveyWarningLabel.setVisible(false);

				if (survey == null) {
					return;
				}

				if (survey.getDocumentTemplate() == null) {
					surveyWarningLabel.setVisible(true);
					surveyWarningLabel.setValue(I18nProperties.getString(Strings.messageSurveyNoDocumentTemplate));
					surveyErrorCallback.accept(true);
					return;
				}

				if (forEmail && survey.getEmailTemplate() == null) {
					surveyWarningLabel.setVisible(true);
					surveyWarningLabel.setValue(I18nProperties.getString(Strings.messageSurveyNoEmailTemplate));
					surveyErrorCallback.accept(true);
					return;
				}

				if (!FacadeProvider.getSurveyFacade().hasUnassignedTokens(survey.toReference())) {
					surveyWarningLabel.setVisible(true);
					surveyWarningLabel.setValue(I18nProperties.getString(Strings.messageSurveyNoTokens));
					surveyErrorCallback.accept(true);
					return;
				}

				try {
					DocumentVariables documentVariables = FacadeProvider.getSurveyFacade().getDocumentVariables(survey.toReference());
					List<String> additionalVariables = documentVariables.getAdditionalVariables();
					if (additionalVariables != null && !additionalVariables.isEmpty()) {
						for (String variable : additionalVariables) {
							TextField variableInput = new TextField(variable);
							variableInput.setWidthFull();
							additionalVariablesComponent.add(variableInput);
						}
					}
				} catch (DocumentTemplateException ex) {
					LoggerFactory.getLogger(getClass()).error("Error while reading document variables.", e);
					NotificationHelper.show(I18nProperties.getString(Strings.errorOccurred), ex.getMessage(), NotificationHelper.Type.ERROR_MESSAGE, false);
				}
			});

			if (!attachmentAvailable) {
				MultilineLabel attachmentUnavailableInfo = new MultilineLabel(
					VaadinIcon.INFO_CIRCLE.getHtml() + " " + I18nProperties.getString(Strings.messageExternalEmailAttachmentNotAvailableInfo),
					ContentMode.HTML);
				attachmentUnavailableInfo.addClassNames(CssStyles.VSPACE_2, CssStyles.VSPACE_TOP_4);
				getContent().addComponent(attachmentUnavailableInfo, ATTACHMENT_NOT_AVAILABLE_INFO_LOC);
			}
		}

		protected Properties readAdditionalVariables() {
			Properties properties = new Properties();
			doForAllVariableInputs(textField -> {
				properties.setProperty(textField.getCaption(), textField.getValue());
				return null;
			});
			return properties;
		}

		private void doForAllVariableInputs(Function<TextField, Void> function) {
			for (int i = 0; i < additionalVariablesComponent.getComponentCount(); i++) {
				Component component = additionalVariablesComponent.getComponent(i);
				if (component instanceof TextField) {
					TextField textField = (TextField) component;
					function.apply(textField);
				}
			}
		}

		public ComboBox getSurveyCombo() {
			return getField(SurveyDocumentOptionsDto.SURVEY);
		}

		@Override
		public SurveyDocumentOptionsDto getValue() {
			SurveyDocumentOptionsDto options = super.getValue();
			options.setSurvey(((SurveyDto) getSurveyCombo().getValue()).toReference());
			options.setTemplateProperties(readAdditionalVariables());

			return options;
		}

		public void setSurveyErrorCallback(Consumer<Boolean> callback) {
			surveyErrorCallback = callback;
		}
	}
}
