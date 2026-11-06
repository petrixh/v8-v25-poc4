/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.docgeneration;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AttachmentType;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.docgeneneration.DocumentTemplateDto;
import de.symeda.sormas.api.docgeneneration.DocumentTemplateException;
import de.symeda.sormas.api.docgeneneration.DocumentTemplateReferenceDto;
import de.symeda.sormas.api.docgeneneration.DocumentVariables;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.importexport.ExportConfigurationDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.StreamSourceDownloadHandler;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public abstract class AbstractDocgenerationLayout extends VerticalLayout {

	protected final Button createButton;
	protected final Button cancelButton;
	public ComboBox<DocumentTemplateDto> templateSelector;
	public final VerticalLayout additionalVariablesComponent;
	public final VerticalLayout additionalParametersComponent;
	/** Was the {@code FileDownloader} on the create button: the download link around it. */
	public Anchor fileDownloader;
	public DocumentVariables documentVariables;
	public Checkbox checkBoxUploadGeneratedDoc;
	public HorizontalLayout buttonBar;
	private Disease defaultDisease;

	protected AbstractDocgenerationLayout(
		Disease defaultDisease,
		String captionTemplateSelector,
		Function<DocumentTemplateDto, String> fileNameFunction,
		boolean isMultiFilesMode,
		boolean uploadCheckboxFirst) {

		this.defaultDisease = defaultDisease;

		additionalVariablesComponent = new VerticalLayout();
		additionalVariablesComponent.setSpacing(false);
		// was a bottom margin only (look, ignored in 1a)
		additionalVariablesComponent.setPadding(false);

		additionalParametersComponent = new VerticalLayout();
		additionalParametersComponent.setSpacing(false);
		// was a bottom margin only (look, ignored in 1a)
		additionalParametersComponent.setPadding(false);

		hideTextfields();
		hideAdditionalParameters();

		if (uploadCheckboxFirst) {
			addDiseaseSelector(defaultDisease);
			addTemplateSelector(captionTemplateSelector, fileNameFunction);
			addCheckboxUploadButton(isMultiFilesMode);
		} else {
			addCheckboxUploadButton(isMultiFilesMode);
			addDiseaseSelector(defaultDisease);
			addTemplateSelector(captionTemplateSelector, fileNameFunction);
		}

		createButton = ButtonHelper.createButton(I18nProperties.getCaption(Captions.actionCreate));
		CssStyles.style(createButton, CssStyles.BUTTON_PRIMARY);
		ButtonHelper.setIcon(createButton, VaadinIcon.FILE_TEXT);
		createButton.setEnabled(false);

		cancelButton = ButtonHelper.createButton(I18nProperties.getCaption(Captions.actionCancel));
		cancelButton.addClickListener((e) -> closeWindow());

		buttonBar = new HorizontalLayout();
		buttonBar.add(cancelButton, createButton);

		add(additionalParametersComponent);
		add(additionalVariablesComponent);
		add(buttonBar);
		setHorizontalComponentAlignment(Alignment.END, buttonBar);
	}

	private void addCheckboxUploadButton(boolean isMultiFilesMode) {
		if (UiUtil.permitted(UserRight.DOCUMENT_UPLOAD)) {
			checkBoxUploadGeneratedDoc = new Checkbox(
				I18nProperties.getPrefixCaption(
					ExportConfigurationDto.I18N_PREFIX,
					isMultiFilesMode
						? Captions.DocumentTemplate_uploadGeneratedDocumentsToEntities
						: Captions.DocumentTemplate_uploadGeneratedDocumentToEntity));
			checkBoxUploadGeneratedDoc.setValue(false);
			checkBoxUploadGeneratedDoc.setEnabled(true);
			checkBoxUploadGeneratedDoc.setClassName(CssStyles.FORCE_CAPTION_CHECKBOX);
			checkBoxUploadGeneratedDoc.setWidth(400, Unit.PIXELS);
			add(checkBoxUploadGeneratedDoc);
		}
	}

	private void addDiseaseSelector(Disease defaultDisease) {
		ComboBox<Disease> diseaseSelector = new ComboBox<>();
		diseaseSelector.setLabel(I18nProperties.getCaption(Captions.disease));
		diseaseSelector.setItems(FacadeProvider.getDiseaseConfigurationFacade().getAllDiseases(true, true, true));
		diseaseSelector.setPlaceholder(I18nProperties.getString(Strings.all));
		// was setEmptySelectionAllowed(true): the value can be cleared
		diseaseSelector.setClearButtonVisible(true);
		diseaseSelector.setWidthFull();
		diseaseSelector.setValue(defaultDisease);

		diseaseSelector.addValueChangeListener(e -> {
			Disease disease = e.getValue();
			templateSelector.setValue(null);

			templateSelector.setItems(getAvailableTemplates(disease));
			templateSelector.setItemLabelGenerator(DocumentTemplateDto::getFileName);
		});

		add(diseaseSelector);
	}

	private void addTemplateSelector(String captionTemplateSelector, Function<DocumentTemplateDto, String> fileNameFunction) {
		templateSelector = new ComboBox<>(captionTemplateSelector);
		templateSelector.setWidthFull();
		templateSelector.addValueChangeListener(e -> {
			DocumentTemplateDto template = e.getValue();
			boolean isValidTemplateFile = template != null;
			createButton.setEnabled(isValidTemplateFile);
			if (fileDownloader != null) {
				// a disabled link has no target: a click on the disabled button must not download the previous document
				fileDownloader.setEnabled(isValidTemplateFile);
			}
			additionalVariablesComponent.removeAll();
			hideTextfields();
			documentVariables = null;
			if (isValidTemplateFile) {
				try {
					documentVariables = getDocumentVariables(template.toReference());
					List<String> additionalVariables = documentVariables.getAdditionalVariables();
					if (additionalVariables != null && !additionalVariables.isEmpty()) {
						for (String variable : additionalVariables) {
							TextField variableInput = new TextField(variable);
							variableInput.setWidthFull();
							// Vaadin 8 text field default: the value reaches the server while typing, before the download is requested
							variableInput.setValueChangeMode(ValueChangeMode.LAZY);
							additionalVariablesComponent.add(variableInput);
						}
						showTextfields();
					}
					performTemplateUpdates();
					if (fileNameFunction != null) {
						setStreamResource(template, fileNameFunction.apply(template));
					}
				} catch (IOException | DocumentTemplateException ex) {
					LoggerFactory.getLogger(getClass()).error("Error while reading document variables.", e);
					NotificationHelper.show(I18nProperties.getString(Strings.errorOccurred), ex.getMessage(), NotificationHelper.Type.ERROR_MESSAGE, false);
				}
			}
		});

		add(templateSelector);
	}

	protected void init() {
		templateSelector.setItems(getAvailableTemplates(defaultDisease));
		templateSelector.setItemLabelGenerator(DocumentTemplateDto::getFileName);
	}

	private void showTextfields() {
		additionalVariablesComponent.setVisible(true);
		adjustSpacing();
	}

	private void hideTextfields() {
		additionalVariablesComponent.setVisible(false);
		adjustSpacing();
	}

	protected void showAdditionalParameters() {
		additionalParametersComponent.setVisible(true);
		adjustSpacing();
	}

	protected void hideAdditionalParameters() {
		additionalParametersComponent.setVisible(false);
		adjustSpacing();
	}

	private void adjustSpacing() {
		setSpacing(!(additionalVariablesComponent.isVisible() || additionalParametersComponent.isVisible()));
	}

	private void closeWindow() {
		Component parent = getParent().orElse(null);
		if (parent instanceof Dialog && VaadinUiUtil.isClosable((Dialog) parent)) {
			((Dialog) parent).close();
		}
	}

	protected Properties readAdditionalVariables() {
		Properties properties = new Properties();
		doForAllVariableInputs(textField -> {
			properties.setProperty(textField.getLabel(), textField.getValue());
			return null;
		});
		return properties;
	}

	private void doForAllVariableInputs(Function<TextField, Void> function) {
		for (int i = 0; i < additionalVariablesComponent.getComponentCount(); i++) {
			Component component = additionalVariablesComponent.getComponentAt(i);
			if (component instanceof TextField) {
				TextField textField = (TextField) component;
				function.apply(textField);
			}
		}
	}

	private void setStreamResource(DocumentTemplateDto template, String fileName) {
		StreamSourceDownloadHandler streamResource = createStreamResource(template, fileName);
		if (fileDownloader == null) {
			fileDownloader = DownloadUtil.attachDownloader(createButton, streamResource);
		} else {
			fileDownloader.setHref(streamResource, AttachmentType.DOWNLOAD);
		}
	}

	protected void performTemplateUpdates() {
		// do nothing
	}

	protected boolean shouldUploadGeneratedDocument() {
		return checkBoxUploadGeneratedDoc != null && Boolean.TRUE.equals(checkBoxUploadGeneratedDoc.getValue());
	}

	protected abstract List<DocumentTemplateDto> getAvailableTemplates(Disease disease);

	protected abstract DocumentVariables getDocumentVariables(DocumentTemplateReferenceDto templateReference)
		throws IOException, DocumentTemplateException;

	protected abstract StreamSourceDownloadHandler createStreamResource(DocumentTemplateDto template, String filename);

	protected abstract String getWindowCaption();
}
