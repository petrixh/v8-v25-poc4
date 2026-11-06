package de.symeda.sormas.ui.importer;

import java.io.IOException;
import java.util.function.Function;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AttachmentType;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.importexport.ValueSeparator;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.StreamSourceDownloadHandler;

@SuppressWarnings("serial")
public class AbstractImportLayout extends VerticalLayout {

	protected Button downloadErrorReportButton;
	/**
	 * The download link around {@link #downloadErrorReportButton} (was the button's {@code FileDownloader} extension);
	 * created with the first error report.
	 */
	private Anchor downloadErrorReportLink;
	protected Upload upload;
	protected final UserDto currentUser;
	protected final UI currentUI;
	protected ImportReceiver generatedReceiver;
	protected ComboBox<ValueSeparator> separator;

	public AbstractImportLayout() {
		currentUser = UiUtil.getUser();
		currentUI = UI.getCurrent();
		setSpacing(false);
		setPadding(true);
		createSeparatorComboBox();
	}

	protected void addDownloadResourcesComponent(int step, DownloadHandler importGuideResource) {
		String headline = I18nProperties.getString(Strings.headingDownloadImportGuide);
		String infoText = I18nProperties.getString(Strings.infoDownloadImportGuide);
		VaadinIcon buttonIcon = VaadinIcon.FILE_PRESENTATION;
		String buttonCaption = I18nProperties.getCaption(Captions.importDownloadImportGuide);
		ImportLayoutComponent importGuideComponent = new ImportLayoutComponent(step, headline, infoText, buttonIcon, buttonCaption);
		DownloadUtil.attachDownloader(importGuideComponent.getButton(), importGuideResource);
		add(importGuideComponent);

		Button dataDictionaryButton = ButtonHelper.createIconButton(
			Captions.importDownloadDataDictionary,
			VaadinIcon.FILE_TABLE,
			null,
			CssStyles.BUTTON_PRIMARY,
			CssStyles.VSPACE_TOP_3,
			CssStyles.VSPACE_2);
		add(DownloadUtil.attachDataDictionaryDownloader(dataDictionaryButton));
	}

	protected void addDownloadImportTemplateComponent(int step, String templateFilePath, String templateFileName) {
		String headline = I18nProperties.getString(Strings.headingDownloadImportTemplate);
		String infoText = I18nProperties.getString(Strings.infoDownloadImportTemplate);
		VaadinIcon buttonIcon = VaadinIcon.DOWNLOAD;
		String buttonCaption = I18nProperties.getCaption(Captions.importDownloadImportTemplate);
		ImportLayoutComponent importTemplateComponent = new ImportLayoutComponent(step, headline, infoText, buttonIcon, buttonCaption);

		try {
			String content = FacadeProvider.getImportFacade().getImportTemplateContent(templateFilePath);
			StreamSourceDownloadHandler templateResource = DownloadUtil.createStringStreamResource(content, templateFileName, "text/csv");

			DownloadUtil.attachDownloader(importTemplateComponent.getButton(), templateResource);

			CssStyles.style(importTemplateComponent, CssStyles.VSPACE_2);
			add(importTemplateComponent);
		} catch (IOException e) {
			NotificationHelper.show(
				I18nProperties.getString(Strings.headingTemplateNotAvailable),
				I18nProperties.getString(Strings.messageTemplateNotAvailable),
				NotificationHelper.Type.ERROR_MESSAGE,
				false);
		}
	}

	protected void addImportCsvComponent(int step, ImportReceiver receiver) {
		String headline = I18nProperties.getString(Strings.headingImportCsvFile);
		String infoText = I18nProperties.getString(Strings.infoImportCsvFile);
		ImportLayoutComponent importCsvComponent = new ImportLayoutComponent(step, headline, infoText, null, null);
		CssStyles.style(importCsvComponent, CssStyles.VSPACE_3);
		add(importCsvComponent);
		add(separator);
		upload = createUpload(receiver);
		CssStyles.style(upload, CssStyles.VSPACE_2);
		add(upload);
	}

	/**
	 * Was the V7 {@code Upload} with a button caption: one file at a time, uploaded as soon as it is chosen, no drop
	 * zone. Flow keeps an uploaded file in the upload's list and, with one file allowed, would block the next upload until
	 * the user removes it; the list is cleared after each upload so the button can be used again, as before.
	 */
	private Upload createUpload(ImportReceiver receiver) {
		Upload newUpload = new Upload(receiver);
		newUpload.setUploadButton(new Button(I18nProperties.getCaption(Captions.importImportData)));
		newUpload.setMaxFiles(1);
		newUpload.setDropAllowed(false);
		newUpload.addAllFinishedListener(e -> newUpload.clearFileList());
		return newUpload;
	}

	private ComboBox<ValueSeparator> createSeparatorComboBox() {
		ComboBox<ValueSeparator> comboBox = new ComboBox<>();
		comboBox.setLabel(I18nProperties.getCaption(Captions.importValueSeparator));
		comboBox.setItems(ValueSeparator.values());
		comboBox.setValue(ValueSeparator.DEFAULT);
		// was setEmptySelectionAllowed(false): a value the user clears is restored
		comboBox.addValueChangeListener(e -> {
			if (e.isFromClient() && e.getValue() == null) {
				comboBox.setValue(e.getOldValue());
			}
		});
		comboBox.setItemLabelGenerator(item -> item.getCaption(FacadeProvider.getConfigFacade().getCsvSeparator()));
		separator = comboBox;
		return comboBox;
	}

	protected void addImportCsvComponentWithOverwrite(int step, Function<Boolean, ImportReceiver> receiverGenerator) {
		String headline = I18nProperties.getString(Strings.headingImportCsvFile);
		String infoText = I18nProperties.getString(Strings.infoImportCsvFile);
		ImportLayoutComponent importCsvComponent = new ImportLayoutComponent(step, headline, infoText, null, null);
		add(importCsvComponent);
		generatedReceiver = receiverGenerator.apply(false);
		upload = createUpload(generatedReceiver);
		CssStyles.style(upload, CssStyles.VSPACE_2);

		HorizontalLayout checkboxBar = new HorizontalLayout();
		checkboxBar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		// was setDescription: the tooltip of the bar
		checkboxBar.getElement().setAttribute("title", I18nProperties.getString(Strings.infoImportInfrastructureAllowOverwrite));
		CssStyles.style(checkboxBar, CssStyles.VSPACE_TOP_3);
		Checkbox allowOverwrite = new Checkbox(I18nProperties.getCaption(Captions.infrastructureImportAllowOverwrite));
		allowOverwrite.setValue(false);
		checkboxBar.add(allowOverwrite);
		checkboxBar.add(VaadinIcon.INFO_CIRCLE.create());
		CssStyles.style(checkboxBar, CssStyles.VSPACE_3);
		add(checkboxBar);
		add(separator);
		add(upload);

		allowOverwrite.addValueChangeListener(e -> {
			generatedReceiver = receiverGenerator.apply(e.getValue());
			upload.setUploadHandler(generatedReceiver);
		});
	}

	protected void addDownloadErrorReportComponent(int step) {
		String headline = I18nProperties.getString(Strings.headingDownloadErrorReport);
		String infoText = I18nProperties.getString(Strings.infoDownloadErrorReport);
		VaadinIcon buttonIcon = VaadinIcon.DOWNLOAD;
		String buttonCaption = I18nProperties.getCaption(Captions.importDownloadErrorReport);
		ImportLayoutComponent errorReportComponent = new ImportLayoutComponent(step, headline, infoText, buttonIcon, buttonCaption);
		downloadErrorReportButton = errorReportComponent.getButton();
		errorReportComponent.getButton().setEnabled(false);
		add(errorReportComponent);
	}

	protected void resetDownloadErrorReportButton() {
		downloadErrorReportButton.setEnabled(false);
		// was: remove the button's FileDownloader extensions
		if (downloadErrorReportLink != null) {
			downloadErrorReportLink.removeHref();
		}
	}

	protected void extendDownloadErrorReportButton(StreamSourceDownloadHandler streamResource) {
		if (downloadErrorReportLink == null) {
			downloadErrorReportLink = DownloadUtil.attachDownloader(downloadErrorReportButton, streamResource);
		} else {
			downloadErrorReportLink.setHref(streamResource, AttachmentType.DOWNLOAD);
		}
		downloadErrorReportButton.setEnabled(true);
	}
}
