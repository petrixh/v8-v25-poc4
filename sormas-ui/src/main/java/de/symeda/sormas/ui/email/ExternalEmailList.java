/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2023 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.email;

import static de.symeda.sormas.ui.utils.CssStyles.LABEL_BOLD;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.manualmessagelog.ManualMessageLogCriteria;
import de.symeda.sormas.api.manualmessagelog.ManualMessageLogIndexDto;
import de.symeda.sormas.api.user.UserReferenceDto;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.PaginationList;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.components.MultilineLabel;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentField;

public class ExternalEmailList extends PaginationList<ManualMessageLogIndexDto> {

	private static final long serialVersionUID = -8851803923161005416L;
	private static final int MAX_DISPLAYED_ENTRIES = 5;

	private final ManualMessageLogCriteria criteria;
	private final Span placeHolderLabel;

	public ExternalEmailList(ManualMessageLogCriteria criteria, String placeholderKey) {
		super(MAX_DISPLAYED_ENTRIES);

		this.criteria = criteria;
		this.placeHolderLabel = new Span(I18nProperties.getString(placeholderKey));
	}

	@Override
	public void reload() {
		List<ManualMessageLogIndexDto> emails = FacadeProvider.getManualMessageLogFacade().getIndexList(criteria);

		setEntries(emails);
		if (!emails.isEmpty()) {
			showPage(1);
		} else {
			listLayout.removeAll();
			updatePaginationLayout();
			listLayout.add(placeHolderLabel);
		}
	}

	@Override
	protected void drawDisplayedEntries() {
		for (ManualMessageLogIndexDto messageLog : getDisplayedEntries()) {
			ListEntry listEntry = new ListEntry(messageLog);

			listEntry.addViewButton("info-email" + messageLog.getUuid(), (Button.ClickListener) event -> {
				showEmailDetailsPopup(messageLog);
			}, VaadinIcon.INFO_CIRCLE);

			listLayout.add(listEntry);
		}
	}

	private static class ListEntry extends SideComponentField {

		private static final long serialVersionUID = 6121787433712938068L;

		public ListEntry(ManualMessageLogIndexDto messageLog) {
			VerticalLayout layout = new VerticalLayout();
			layout.setWidthFull();
			layout.setPadding(false);
			layout.setSpacing(false);

			layout.add(new Span(DateFormatHelper.formatLocalDateTime(messageLog.getSentDate())));
			layout.add(new Span(messageLog.getUsedTemplate()));
			layout.add(buildSendingUserInfo(messageLog.getSendingUser()));

			addComponentToField(layout);
		}

		private static HorizontalLayout buildSendingUserInfo(UserReferenceDto sendingUser) {
			Span captionLabel = new Span(I18nProperties.getCaption(Captions.externalEmailSentBy) + " ");

			final Span sendingUserLabel;
			if (sendingUser != null) {
				sendingUserLabel = new Span(sendingUser.getShortCaption());
			} else {
				sendingUserLabel = new Span(I18nProperties.getCaption(Captions.inaccessibleValue));
				sendingUserLabel.addClassNames(CssStyles.INACCESSIBLE_LABEL);
			}

			return new HorizontalLayout(captionLabel, sendingUserLabel);
		}
	}

	private static void showEmailDetailsPopup(ManualMessageLogIndexDto messageLog) {
		VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.setPadding(false);
		verticalLayout.setSpacing(false);

		Span sentToCaptionLabel = new Span(I18nProperties.getCaption(Captions.externalEmailSentTo));
		sentToCaptionLabel.addClassName(LABEL_BOLD);
		if (messageLog.isPseudonymized()) {
			sentToCaptionLabel.addClassName(CssStyles.INACCESSIBLE_LABEL);
		}
		verticalLayout.add(new HorizontalLayout(sentToCaptionLabel, new Span(messageLog.getEmailAddress())));

		Span attachmentsCaptionLabel = new Span(I18nProperties.getCaption(Captions.externalEmailAttachedDocuments));
		attachmentsCaptionLabel.addClassName(LABEL_BOLD);
		List<String> attachedDocuments = messageLog.getAttachedDocuments();
		String attachmentsValue = CollectionUtils.isNotEmpty(attachedDocuments)
			? String.join(", ", attachedDocuments)
			: I18nProperties.getString(Strings.messageExternalEmailNoAttachments);
		verticalLayout.add(attachmentsCaptionLabel, new MultilineLabel(attachmentsValue));

		VaadinUiUtil.showConfirmationPopup(
			I18nProperties.getString(Strings.headingExternalEmailDetails),
			verticalLayout,
			I18nProperties.getString(Strings.close),
			null,
			640,
			confirmed -> true);
	}
}
