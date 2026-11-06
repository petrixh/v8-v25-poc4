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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.Alignment;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import de.symeda.sormas.ui.utils.NotificationHelper;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseReferenceDto;
import de.symeda.sormas.api.docgeneneration.RootEntityType;
import de.symeda.sormas.api.document.DocumentFacade;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.survey.SurveyTokenCriteria;
import de.symeda.sormas.api.survey.SurveyTokenIndexDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.PaginationList;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponent;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentField;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentLayout;

public class SurveyListComponentLayout extends SideComponentLayout {

	private static final long serialVersionUID = -4364573774979104517L;

	public SurveyListComponentLayout(
		CaseReferenceDto caseRef,
		Disease disease,
		PersonDto person,
		boolean isEditAllowed,
		boolean isEmailAllowed,
		Consumer<Runnable> actionWrapper,
		Runnable createCallback) {
		super(
			new SurveyListComponent(
				I18nProperties.getString(Strings.headingSurveySideComponent),
				caseRef,
				disease,
				person,
				new SurveyTokenCriteria().caseAssignedTo(caseRef),
				actionWrapper,
				createCallback,
				isEditAllowed,
				isEmailAllowed));
	}

	private static class SurveyListComponent extends SideComponent {

		private static final long serialVersionUID = 4793190763340951494L;

		public SurveyListComponent(
			String heading,
			CaseReferenceDto caseRef,
			Disease disease,
			PersonDto person,
			SurveyTokenCriteria surveyTokenCriteria,
			Consumer<Runnable> actionWrapper,
			Runnable createCallback,
			boolean isEditAllowed,
			boolean isEmailAllowed) {
			super(heading, actionWrapper);

			setWidthFull();
			setMargin(false);
			setSpacing(false);

			VerticalLayout uploadLayout = new VerticalLayout();
			uploadLayout.setSpacing(true);
			uploadLayout.setPadding(true);
			uploadLayout.addClassName(CssStyles.LAYOUT_MINIMAL);

			if (isEmailAllowed) {
				Button sendSurveyButton = ButtonHelper.createButton(Captions.surveySend, I18nProperties.getCaption(Captions.surveySend), (e) -> {
					ControllerProvider.getSurveyDocumentController()
						.sendSurveyDocument(RootEntityType.ROOT_CASE, caseRef, disease, person, createCallback);
				}, CssStyles.BUTTON_PRIMARY);
				sendSurveyButton.setWidthFull();
				uploadLayout.add(sendSurveyButton);
			}

			Button generateSurveyDocButton =
				ButtonHelper.createButton(Captions.surveyGenerate, I18nProperties.getCaption(Captions.surveyGenerate), (e) -> {
					ControllerProvider.getSurveyDocumentController()
						.generateSurveyDocument(RootEntityType.ROOT_CASE, caseRef, disease, createCallback);
				});
			if (!isEmailAllowed) {
				generateSurveyDocButton.addClassName(CssStyles.BUTTON_PRIMARY);
			}

			generateSurveyDocButton.setWidthFull();
			uploadLayout.add(generateSurveyDocButton);

			addCreateButton(ButtonHelper.createIconPopupButton(Captions.surveyNew, VaadinIcon.PLUS_CIRCLE, uploadLayout, CssStyles.BUTTON_PRIMARY));

			SurveyList selfReportList = new SurveyList(surveyTokenCriteria, isEditAllowed);
			add(selfReportList);
			selfReportList.reload();
		}
	}

	private static class SurveyList extends PaginationList<SurveyTokenIndexDto> {

		private static final long serialVersionUID = 2918333514559847082L;

		private static final int MAX_DISPLAYED_ENTRIES = 5;

		private final SurveyTokenCriteria surveyTokenCriteria;
		private final boolean isEditAllowed;
		private final Span noSurveyLabel;

		public SurveyList(SurveyTokenCriteria surveyTokenCriteria, boolean isEditAllowed) {
			super(MAX_DISPLAYED_ENTRIES);
			this.surveyTokenCriteria = surveyTokenCriteria;
			this.isEditAllowed = isEditAllowed;
			this.noSurveyLabel = new Span(I18nProperties.getString(Strings.infoNoSurveys));
		}

		@Override
		public void reload() {
			List<SurveyTokenIndexDto> surveyTokens = FacadeProvider.getSurveyTokenFacade().getIndexList(surveyTokenCriteria, 0, null, null);

			setEntries(surveyTokens);
			if (!surveyTokens.isEmpty()) {
				showPage(1);
			} else {
				listLayout.removeAll();
				updatePaginationLayout();
				listLayout.add(noSurveyLabel);
			}
		}

		@Override
		protected void drawDisplayedEntries() {
			List<SurveyTokenIndexDto> displayedEntries = getDisplayedEntries();
			for (int i = 0, displayedEntriesSize = displayedEntries.size(); i < displayedEntriesSize; i++) {
				SurveyTokenIndexDto token = displayedEntries.get(i);
				SurveyListEntry listEntry = new SurveyListEntry(token);

				Button downloadButton = buildDownloadButton(token);
				listEntry.add(downloadButton);
				listEntry.setComponentAlignment(downloadButton, Alignment.TOP_RIGHT);
				listEntry.setFlexGrow(0, downloadButton);

				if (Boolean.TRUE.equals(token.getResponseReceived()) && StringUtils.isNotBlank(token.getExternalRespondentId())) {
					Button eyeButton = ButtonHelper.createIconButton(null, VaadinIcon.EYE, e -> {
						new SurveyQuestionnaireWindow(listEntry.getToken().toReference());
					}, CssStyles.BUTTON_LINK, CssStyles.BUTTON_COMPACT);
					listEntry.add(eyeButton);
					listEntry.setComponentAlignment(eyeButton, Alignment.TOP_RIGHT);
					listEntry.setFlexGrow(0, eyeButton);
				}

				listEntry.addActionButton(String.valueOf(i), (Button.ClickListener) clickEvent -> {
					ControllerProvider.getSurveyTokenController().showCaseSurveyDetails(listEntry.getToken().toReference(), this::reload);
				}, UiUtil.permitted(isEditAllowed, UserRight.SURVEY_EDIT));
				listEntry.setEnabled(isEditAllowed);
				listLayout.add(listEntry);
			}
		}

		private Button buildDownloadButton(SurveyTokenIndexDto token) {
			Button viewButton = ButtonHelper.createIconButton(null, VaadinIcon.DOWNLOAD, null, CssStyles.BUTTON_LINK, CssStyles.BUTTON_COMPACT);

			StreamResource streamResource = new StreamResource((StreamResource.StreamSource) () -> {
				DocumentFacade documentFacade = FacadeProvider.getDocumentFacade();
				try {
					return new ByteArrayInputStream(documentFacade.getContent(token.getGeneratedDocumentUuid()));
				} catch (IOException | IllegalArgumentException e) {
					NotificationHelper.show(
						String.format(I18nProperties.getString(Strings.errorReadingDocument), token.getGeneratedDocumentName()),
						e.getMessage(),
						NotificationHelper.Type.ERROR_MESSAGE,
						false);
					return null;
				}
			}, token.getGeneratedDocumentName());
			streamResource.setMIMEType(token.getGeneratedDocumentMimeType());

			FileDownloader fileDownloader = new FileDownloader(streamResource);
			fileDownloader.extend(viewButton);
			fileDownloader.setFileDownloadResource(streamResource);

			return viewButton;
		}
	}

	private static class SurveyListEntry extends SideComponentField {

		private static final long serialVersionUID = 1876258478335703352L;

		private final SurveyTokenIndexDto token;

		public SurveyListEntry(SurveyTokenIndexDto token) {
			this.token = token;

			HorizontalLayout topLayout = new HorizontalLayout();
			topLayout.setWidthFull();
			topLayout.setPadding(false);
			topLayout.setSpacing(false);
			addComponentToField(topLayout);

			VerticalLayout topLeftLayout = new VerticalLayout();
			{
				topLeftLayout.setPadding(false);
				topLeftLayout.setSpacing(false);

				Span surveyNameLabel = new Span(DataHelper.toStringNullable(token.getSurveyName()));
				CssStyles.style(surveyNameLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
				surveyNameLabel.setWidthFull();
				topLeftLayout.add(surveyNameLabel);

				Span assignmentDateLabel = new Span(I18nProperties.getCaption(Captions.creationDate));
				Span assignmentDate = new Span(DateFormatHelper.formatLocalDate(token.getAssignmentDate()));
				assignmentDate.setWidthFull();
				topLeftLayout.add(new HorizontalLayout(assignmentDateLabel, assignmentDate));

				if (StringUtils.isNotBlank(token.getRecipientEmail())) {
					Span sentToCaptionLabel = new Span(I18nProperties.getCaption(Captions.externalEmailSentTo));
					Span emailLabel = new Span(token.getRecipientEmail());
					if (token.isPseudonymized()) {
						emailLabel.addClassName(CssStyles.INACCESSIBLE_LABEL);
					}
					topLeftLayout.add(new HorizontalLayout(sentToCaptionLabel, emailLabel));
				}

				topLeftLayout.add(
					new Span(
						token.getResponseReceived()
							? I18nProperties.getString(Strings.infoSurveyResponseReceived)
							: I18nProperties.getString(Strings.infoSurveyResponseNotReceived)));
			}
			topLayout.add(topLeftLayout);
		}

		public void addDeleteButton(String id, Button.ClickListener actionClickListener) {
			Button actionButton = ButtonHelper.createIconButtonWithCaption(
				"delete" + id,
				null,
				VaadinIcon.TRASH,
				actionClickListener,
				CssStyles.BUTTON_LINK,
				CssStyles.BUTTON_COMPACT);

		}

		public SurveyTokenIndexDto getToken() {
			return token;
		}
	}
}
