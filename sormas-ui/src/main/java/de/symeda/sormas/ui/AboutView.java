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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.AttachmentType;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

import de.symeda.sormas.api.ConfigFacade;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.classification.ClassificationHtmlRenderer;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.HtmlHelper;
import de.symeda.sormas.api.utils.InfoProvider;
import de.symeda.sormas.ui.navigation.View;
import de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.ExportEntityName;

@SuppressWarnings("serial")
public class AboutView extends VerticalLayout implements View {

	public static final String VIEW_NAME = "about";

	private static final String XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private static final String SORMAS_GITHUB_URL = "https://github.com/SORMAS-Foundation/SORMAS-Project";

	public AboutView() {

		// Info section
		HorizontalLayout aboutLayout = new HorizontalLayout();
		{
			VerticalLayout infoLayout = new VerticalLayout();
			// was a top margin only (MarginInfo); spacing details belong to the theme work
			infoLayout.setPadding(false);
			infoLayout.add(createInfoSection());

			// Documents section
			if (shouldShowDocumentsSection()) {
				infoLayout.add(createDocumentsSection());
			}

			aboutLayout.add(infoLayout);
		}

		// Additional Info section
		VerticalLayout additionalInfoSection =
			createCustomHtmlSection(I18nProperties.getCaption(Captions.aboutAdditionalInfo), "additionalinfo.html");
		if (additionalInfoSection != null) {
			aboutLayout.add(additionalInfoSection);
		}

		// Copyright section
		VerticalLayout copyrightSection = createCustomHtmlSection(I18nProperties.getCaption(Captions.aboutCopyright), "copyrightnotices.html");
		if (copyrightSection != null) {
			aboutLayout.add(copyrightSection);
		}

		setSizeFull();
		setClassName("about-view");
		add(aboutLayout);
		// was Alignment.MIDDLE_CENTER
		setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
		setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, aboutLayout);
	}

	@Override
	public void enter(ViewChangeEvent event) {

	}

	private VerticalLayout createInfoSection() {

		VerticalLayout infoLayout = new VerticalLayout();
		infoLayout.setSpacing(false);
		infoLayout.setPadding(false);

		Span aboutLabel = createHtmlLabel(I18nProperties.getCaption(Captions.about));
		aboutLabel.addClassName(CssStyles.H1);
		infoLayout.add(aboutLabel);

		ConfigFacade configFacade = FacadeProvider.getConfigFacade();
		String infoLabelStr = configFacade.isCustomBranding()
			? String.format(I18nProperties.getCaption(Captions.aboutBrandedSormasVersion), configFacade.getCustomBrandingName())
			: "SORMAS";
		Span infoLabel = new Span(VaadinIcon.INFO_CIRCLE.create(), new Text(" " + infoLabelStr));
		infoLayout.add(infoLabel);

		String versionText = I18nProperties.getCaption(Captions.aboutVersion) + ": " + InfoProvider.get().getVersion();
		Span versionLabel = createHtmlLabel(versionText);
		CssStyles.style(versionLabel, CssStyles.VSPACE_3);
		infoLayout.add(versionLabel);

		if (InfoProvider.get().isSnapshotVersion()) {
			Anchor commitLink = new Anchor(
				SORMAS_GITHUB_URL + "/commits/" + InfoProvider.get().getLastCommitShortId(),
				String.format("%s (%s)", versionText, InfoProvider.get().getLastCommitShortId()),
				AnchorTarget.BLANK);
			CssStyles.style(commitLink, CssStyles.VSPACE_3);
			infoLayout.replace(versionLabel, commitLink);
		}

		if (FacadeProvider.getFeatureConfigurationFacade().isFeatureEnabled(FeatureType.EXTERNAL_MESSAGES)) {
			addExternalServiceVersion(
				Captions.aboutExternalMessageAdapter,
				() -> FacadeProvider.getExternalMessageFacade().getExternalMessagesAdapterVersion(),
				infoLayout);
		}

		if (FacadeProvider.getExternalSurveillanceToolFacade().isFeatureEnabled()) {
			addExternalServiceVersion(
				Captions.aboutExternalSurveillanceToolGateway,
				() -> FacadeProvider.getExternalSurveillanceToolFacade().getVersion(),
				infoLayout);
		}

		Anchor whatsNewLink = new Anchor(
			SORMAS_GITHUB_URL + "/releases/tag/v" + InfoProvider.get().getBaseVersion(),
			I18nProperties.getCaption(Captions.aboutWhatsNew),
			AnchorTarget.BLANK);
		infoLayout.add(whatsNewLink);

		Anchor sormasWebsiteLink = new Anchor("https://sormas.org/", I18nProperties.getCaption(Captions.aboutSormasWebsite), AnchorTarget.BLANK);
		infoLayout.add(sormasWebsiteLink);

		Anchor sormasGithubLink = new Anchor(SORMAS_GITHUB_URL, "SORMAS Github", AnchorTarget.BLANK);
		infoLayout.add(sormasGithubLink);

		Anchor changelogLink = new Anchor(SORMAS_GITHUB_URL + "/releases", I18nProperties.getCaption(Captions.aboutChangelog), AnchorTarget.BLANK);
		infoLayout.add(changelogLink);

		return infoLayout;
	}

	interface ExternalServiceVersionSupplier {

		String get() throws Exception;
	}

	private void addExternalServiceVersion(String captionTag, ExternalServiceVersionSupplier versionSupplier, VerticalLayout infoLayout) {
		String version;
		Span availabilityLabel;

		try {
			version = versionSupplier.get();
			availabilityLabel = new Span(VaadinIcon.CHECK.create());
			availabilityLabel.addClassName(CssStyles.LABEL_POSITIVE);
		} catch (Exception e) {
			version = I18nProperties.getCaption(Captions.aboutServiceNotAvailable);
			availabilityLabel = new Span(VaadinIcon.CLOSE.create());
			availabilityLabel.addClassName(CssStyles.LABEL_CRITICAL);
		}

		availabilityLabel.addClassName(CssStyles.HSPACE_LEFT_4);

		Span caption = new Span(I18nProperties.getCaption(captionTag));
		HorizontalLayout captionLayout = new HorizontalLayout(caption, availabilityLabel);
		captionLayout.setSpacing(false);
		infoLayout.add(captionLayout);

		Span versionLabel = createHtmlLabel(I18nProperties.getCaption(Captions.aboutVersion) + ": " + version);
		CssStyles.style(versionLabel, CssStyles.VSPACE_3);
		infoLayout.add(versionLabel);
	}

	private VerticalLayout createDocumentsSection() {

		VerticalLayout documentsLayout = new VerticalLayout();
		documentsLayout.setSpacing(false);
		// was a top margin only (MarginInfo)
		documentsLayout.setPadding(false);

		Span documentsLabel = createHtmlLabel(I18nProperties.getCaption(Captions.aboutDocuments));
		documentsLabel.addClassName(CssStyles.H1);
		documentsLayout.add(documentsLabel);

		List<String> customDocuments = listCustomDocumentsFiles();
		if (!customDocuments.isEmpty()) {
			customDocuments.stream().forEach(customDocument -> {
				Button customDocumentButton = ButtonHelper.createButton(customDocument, null, CssStyles.BUTTON_COMPACT);
				customDocumentButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
				String customDocumentPath = getCustomDocumentsPath() + File.separator + customDocument;
				documentsLayout.add(createDownloadLink(customDocumentButton, DownloadHandler.forFile(new File(customDocumentPath))));
			});
		}

		if (shouldShowClassificationDocumentLink()) {
			Button classificationDocumentButton = ButtonHelper.createButton(Captions.aboutCaseClassificationRules, null, CssStyles.BUTTON_COMPACT);
			classificationDocumentButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
			// was a FileDownloader on the button: Flow downloads through a link around it
			Anchor classificationDocumentLink = new Anchor();
			classificationDocumentLink.getElement().setAttribute("download", true);
			classificationDocumentLink.add(classificationDocumentButton);
			documentsLayout.add(classificationDocumentLink);

			try {
				String serverUrl =
					new URL(((VaadinServletRequest) VaadinService.getCurrentRequest()).getHttpServletRequest().getRequestURL().toString())
						.getAuthority();
				classificationDocumentLink.setHref(DownloadUtil.createStringStreamResource(
					ClassificationHtmlRenderer.createHtmlForDownload(
						serverUrl,
						FacadeProvider.getDiseaseConfigurationFacade().getAllDiseases(true, true, true),
						I18nProperties.getUserLanguage(),
						FacadeProvider.getCaseClassificationFacade(),
						FacadeProvider.getConfigFacade()),
					"classification_rules.html",
					"text/html"));
			} catch (MalformedURLException e) {

			}
		}

		if (shouldShowDataDictionaryLink()) {
			Button dataDictionaryButton = ButtonHelper.createButton(Captions.aboutDataDictionary, null, CssStyles.BUTTON_COMPACT);
			dataDictionaryButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
			documentsLayout.add(dataDictionaryButton);
			DownloadUtil.attachDataDictionaryDownloader(dataDictionaryButton);
		}

		if (FacadeProvider.getInfoFacade().isGenerateDataProtectionDictionaryAllowed()) {
			Button dataProtectionButton = ButtonHelper.createButton(Captions.aboutDataProtectionDictionary, null, CssStyles.BUTTON_COMPACT);
			dataProtectionButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
			documentsLayout.add(attachDataProtectionDictionaryDownloader(dataProtectionButton));
		}

		// This link is hidden until an updated version of the document is provided
		/*
		 * Link technicalManualLink = new Link(
		 * I18nProperties.getCaption(Captions.aboutTechnicalManual),
		 * new ExternalResource(
		 * "https://github.com/sormas-foundation/SORMAS-Project/files/2585973/SORMAS_Technical_Manual_Webversion_20180911.pdf"));
		 * technicalManualLink.setTargetName("_blank");
		 * documentsLayout.addComponent(technicalManualLink);
		 */

		return documentsLayout;
	}

	private VerticalLayout createCustomHtmlSection(String caption, String fileName) {

		String htmlContentString = "";

		Path customHtmlDirectory = Paths.get(FacadeProvider.getConfigFacade().getCustomFilesPath());
		Path customFilePath = customHtmlDirectory.resolve(fileName);

		try {
			byte[] encoded = Files.readAllBytes(customFilePath);
			htmlContentString = HtmlHelper.cleanHtmlRelaxed(new String(encoded, StandardCharsets.UTF_8));
			if (htmlContentString.isEmpty()) {
				return null;
			}
		} catch (IOException e) {
			return null;
		}

		VerticalLayout layout = new VerticalLayout();
		layout.setSpacing(false);
		// was a top margin only (MarginInfo)
		layout.setPadding(false);

		// Header
		Span headerLabel = createHtmlLabel(caption);
		headerLabel.addClassName(CssStyles.H1);
		layout.add(headerLabel);

		// Label, loaded from custom file
		Span htmlContent = createHtmlLabel(htmlContentString);
		layout.add(htmlContent);

		return layout;
	}

	private boolean shouldShowDocumentsSection() {
		return !listCustomDocumentsFiles().isEmpty() || shouldShowClassificationDocumentLink() || shouldShowDataDictionaryLink();
	}

	private List<String> listCustomDocumentsFiles() {
		File customDocumentsDir = new File(getCustomDocumentsPath());
		if (!customDocumentsDir.exists() || !customDocumentsDir.isDirectory()) {
			return Collections.emptyList();
		}
		File[] customDocuments = customDocumentsDir.listFiles();
		if (customDocuments == null) {
			return Collections.emptyList();
		}
		return Arrays.stream(customDocuments).map(File::getName).sorted(String::compareTo).collect(Collectors.toList());
	}

	private boolean shouldShowClassificationDocumentLink() {
		return FacadeProvider.getConfigFacade().isAnyCaseClassificationCalculationEnabled()
			&& FacadeProvider.getFeatureConfigurationFacade().isFeatureEnabled(FeatureType.CASE_SURVEILANCE);
	}

	private boolean shouldShowDataDictionaryLink() {
		return FacadeProvider.getFeatureConfigurationFacade().isFeatureEnabled(FeatureType.CASE_SURVEILANCE)
			|| FacadeProvider.getFeatureConfigurationFacade().isFeatureEnabled(FeatureType.EVENT_SURVEILLANCE);
	}

	private String getCustomDocumentsPath() {
		return FacadeProvider.getConfigFacade().getCustomFilesPath() + "aboutfiles";
	}

	/**
	 * @return a download link around {@code target} (was a FileDownloader extending it); add the link to the layout
	 */
	public Anchor attachDataProtectionDictionaryDownloader(Component target) {
		String fileName = DownloadUtil.createFileNameWithCurrentDate(ExportEntityName.DATA_PROTECTION_DICTIONARY, ".xlsx");
		return createDownloadLink(target, DownloadHandler.fromInputStream(event -> new DownloadResponse(new DownloadUtil.DelayedInputStream((out) -> {
			String documentPath = FacadeProvider.getInfoFacade().generateDataProtectionDictionary();
			IOUtils.copy(Files.newInputStream(new File(documentPath).toPath()), out);
		}, (e) -> {
		}), fileName, XLSX_MIME_TYPE, -1)));
	}

	/**
	 * Was a FileDownloader extending the component: Flow downloads through a link around it.
	 */
	private static Anchor createDownloadLink(Component target, DownloadHandler downloadHandler) {
		Anchor link = new Anchor();
		link.setHref(downloadHandler, AttachmentType.DOWNLOAD);
		link.add(target);
		return link;
	}

	/**
	 * Was a Label in HTML content mode.
	 */
	private static Span createHtmlLabel(String html) {
		Span label = new Span();
		label.getElement().setProperty("innerHTML", html);
		return label;
	}
}
