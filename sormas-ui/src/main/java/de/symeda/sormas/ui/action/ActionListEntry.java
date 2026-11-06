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
package de.symeda.sormas.ui.action;

import static de.symeda.sormas.api.utils.HtmlHelper.cleanHtml;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.action.ActionDto;
import de.symeda.sormas.api.action.ActionPriority;
import de.symeda.sormas.api.action.ActionStatus;
import de.symeda.sormas.api.document.DocumentRelatedEntityType;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.HtmlHelper;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.document.DocumentListComponent;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentLayout;

@SuppressWarnings("serial")
public class ActionListEntry extends HorizontalLayout {

	private final ActionDto action;
	HorizontalLayout titleLayout;
	private Button editButton;

	public ActionListEntry(ActionDto action) {

		this.action = action;

		setPadding(false);
		setSpacing(true);
		setWidthFull();
		addClassName(CssStyles.SORMAS_LIST_ENTRY);

		VerticalLayout withContentLayout = new VerticalLayout();
		withContentLayout.setPadding(false);
		withContentLayout.setSpacing(false);
		add(withContentLayout);
		setFlexGrow(3, withContentLayout);

		titleLayout = new HorizontalLayout();
		titleLayout.setPadding(false);
		titleLayout.setSpacing(false);
		Span title = new Span(MoreObjects.firstNonNull(Strings.emptyToNull(action.getTitle()), "-"));
		title.addClassNames(CssStyles.H3, CssStyles.HSPACE_LEFT_3);
		titleLayout.add(title);
		withContentLayout.add(titleLayout);

		HorizontalLayout topLayout = new HorizontalLayout();
		topLayout.setPadding(false);
		topLayout.setSpacing(false);
		topLayout.setWidthFull();
		withContentLayout.add(topLayout);

		VerticalLayout descReplyLayout = new VerticalLayout();
		descReplyLayout.setPadding(false);
		descReplyLayout.setSpacing(false);
		descReplyLayout.setWidthFull();
		descReplyLayout.addClassName(CssStyles.RICH_TEXT_CONTENT_CONTAINER);
		withContentLayout.add(descReplyLayout);

		Span description = VaadinUiUtil.createLabel(cleanHtml(action.getDescription(), HtmlHelper.EVENTACTION_WHITELIST), ContentMode.HTML);
		description.setWidthFull();
		descReplyLayout.add(description);
		if (!Strings.isNullOrEmpty(action.getReply())) {
			Span replyLabel = VaadinUiUtil.createLabel(cleanHtml(action.getReply(), HtmlHelper.EVENTACTION_WHITELIST), ContentMode.HTML);
			replyLabel.setWidthFull();
			replyLabel.addClassName(CssStyles.REPLY);
			descReplyLayout.add(replyLabel);
		}

		// TOP LEFT
		VerticalLayout topLeftLayout = new VerticalLayout();

		topLeftLayout.setPadding(false);
		topLeftLayout.setSpacing(false);

		Span dateLabel =
			new Span(I18nProperties.getPrefixCaption(ActionDto.I18N_PREFIX, ActionDto.DATE) + ": " + DateFormatHelper.formatDate(action.getDate()));
		topLeftLayout.add(dateLabel);

		Span creatorLabel = new Span(
			String.format(
				I18nProperties.getCaption(Captions.actionCreatingLabel),
				DateFormatHelper.formatDate(action.getCreationDate()),
				action.getCreatorUser().getCaption()));
		creatorLabel.addClassName(CssStyles.LABEL_ITALIC);
		topLeftLayout.add(creatorLabel);

		Span lastModifiedByLabel = null;
		if (action.getLastModifiedBy() != null) {
			lastModifiedByLabel = new Span(
				String.format(
					I18nProperties.getCaption(Captions.actionLastModifiedByLabel),
					DateFormatHelper.formatDate(action.getChangeDate()),
					action.getLastModifiedBy().getCaption()));
			lastModifiedByLabel.addClassName(CssStyles.LABEL_ITALIC);
			topLeftLayout.add(lastModifiedByLabel);
		}

		topLayout.add(topLeftLayout);

		// TOP RIGHT
		VerticalLayout topRightLayout = new VerticalLayout();

		topRightLayout.addClassName(CssStyles.ALIGN_RIGHT);
		topRightLayout.setPadding(false);
		topRightLayout.setSpacing(false);

		HorizontalLayout statusContainer = new HorizontalLayout();
		Span statusLabel = new Span(
			I18nProperties.getPrefixCaption(ActionDto.I18N_PREFIX, ActionDto.ACTION_STATUS) + ": "
				+ DataHelper.toStringNullable(action.getActionStatus()));
		CssStyles.style(statusLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
		statusContainer.add(statusLabel);
		Span statusChangeLabel = null;
		if (action.getStatusChangeDate() != null) {
			statusChangeLabel = new Span(
				String.format(I18nProperties.getCaption(Captions.actionStatusChangeDate), DateFormatHelper.formatDate(action.getStatusChangeDate())));
			statusChangeLabel.addClassName(CssStyles.LABEL_ITALIC);
			statusContainer.add(statusChangeLabel);
		}

		topRightLayout.add(statusContainer);

		Span priorityLabel = new Span(
			DataHelper.toStringNullable(I18nProperties.getPrefixCaption(ActionDto.I18N_PREFIX, ActionDto.PRIORITY) + ": " + action.getPriority()));
		if (ActionPriority.HIGH == action.getPriority()) {
			priorityLabel.addClassName(CssStyles.LABEL_IMPORTANT);
		} else if (ActionPriority.NORMAL == action.getPriority()) {
			priorityLabel.addClassName(CssStyles.LABEL_NEUTRAL);
		}
		topRightLayout.add(priorityLabel);

		topLayout.add(topRightLayout);
		// was Alignment.TOP_RIGHT
		topLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, topRightLayout);
		topRightLayout.getStyle().set("margin-left", "auto");

		String statusStyle = action.getActionStatus() == ActionStatus.DONE ? CssStyles.LABEL_DONE : null;
		if (statusStyle != null) {
			statusLabel.addClassName(statusStyle);
			dateLabel.addClassName(statusStyle);
			if (statusChangeLabel != null) {
				statusChangeLabel.addClassName(statusStyle);
			}
			creatorLabel.addClassName(statusStyle);
			if (lastModifiedByLabel != null) {
				lastModifiedByLabel.addClassName(statusStyle);
			}
			priorityLabel.addClassName(statusStyle);
		}

		if (UiUtil.permitted(FeatureType.DOCUMENTS, UserRight.DOCUMENT_VIEW)) {
			VerticalLayout documentLayout = new VerticalLayout();
			documentLayout.setSpacing(false);
			add(documentLayout);
			setFlexGrow(1, documentLayout);
			// was a top and right margin (MarginInfo); spacing details belong to the theme work
			documentLayout.setPadding(false);

			DocumentListComponent documentList =
				new DocumentListComponent(DocumentRelatedEntityType.ACTION, action.toReference(), UserRight.EVENT_EDIT, false, true, true);
			documentLayout.add(new SideComponentLayout(documentList));
		}
	}

	public void addEditListener(int rowIndex, ComponentEventListener<ClickEvent<Button>> editClickListener) {
		if (editButton == null) {
			editButton = ButtonHelper.createIconButtonWithCaption(
				"edit-action-" + rowIndex,
				null,
				VaadinIcon.PENCIL,
				null,
				CssStyles.BUTTON_LINK,
				CssStyles.BUTTON_COMPACT,
				CssStyles.LABEL_VERTICAL_ALIGN_TOP);

			titleLayout.addComponentAtIndex(0, editButton);
			editButton.addClassNames(CssStyles.LABEL_HEADING_TITLE, CssStyles.HSPACE_LEFT_4);
			titleLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, editButton);
			titleLayout.setFlexGrow(0, editButton);
		}

		editButton.addClickListener(editClickListener);
	}

	public ActionDto getAction() {
		return action;
	}
}
