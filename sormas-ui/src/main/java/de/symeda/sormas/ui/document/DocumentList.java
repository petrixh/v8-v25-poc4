package de.symeda.sormas.ui.document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import de.symeda.sormas.ui.utils.NotificationHelper;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.ReferenceDto;
import de.symeda.sormas.api.document.DocumentDto;
import de.symeda.sormas.api.document.DocumentFacade;
import de.symeda.sormas.api.document.DocumentRelatedEntityType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.StreamSourceDownloadHandler;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class DocumentList extends VerticalLayout {

	private final DocumentRelatedEntityType relatedEntityType;
	private final ReferenceDto entityRef;
	private final UserRight editRight;
	private final boolean pseudonymized;
	private final boolean isEditAllowed;
	private final boolean isDeleteAllowed;

	public DocumentList(
		DocumentRelatedEntityType relatedEntityType,
		ReferenceDto entityRef,
		UserRight editRight,
		boolean pseudonymized,
		boolean isEditAllowed,
		boolean isDeleteAllowed) {
		this.relatedEntityType = relatedEntityType;
		this.entityRef = entityRef;
		this.editRight = editRight;
		this.pseudonymized = pseudonymized;
		this.isEditAllowed = isEditAllowed;
		this.isDeleteAllowed = isDeleteAllowed;

		setSpacing(true);
		setPadding(false);
	}

	public void reload() {
		List<DocumentDto> docs = Collections.emptyList();
		if (!pseudonymized) {
			docs = FacadeProvider.getDocumentFacade().getDocumentsRelatedToEntity(relatedEntityType, entityRef.getUuid());
		}
		removeAll();
		if (docs.isEmpty()) {
			Span noActionsLabel = new Span(String.format(I18nProperties.getCaption(Captions.documentNoDocuments), relatedEntityType.toString()));
			setSpacing(false);
			add(noActionsLabel);
		} else {
			setSpacing(true);
			add(docs.stream().map(this::toComponent).toArray(Component[]::new));
		}
	}

	private Component toComponent(DocumentDto document) {
		HorizontalLayout res = new HorizontalLayout();
		res.setSpacing(true);
		res.setPadding(false);
		res.setWidthFull();

		// TODO: show content-type and/or size?
		Span nameLabel = new Span(DataHelper.toStringNullable(document.getName()));
		nameLabel.addClassName(CssStyles.LABEL_CAPTION_TRUNCATED);
		res.add(nameLabel);
		res.setFlexGrow(1, nameLabel);

		Component downloadButton = buildDownloadButton(document);
		res.add(downloadButton);
		res.setFlexGrow(0, downloadButton);

		if (UiUtil.permitted(editRight, UserRight.DOCUMENT_DELETE) && isDeleteAllowed) {
			Button deleteButton = buildDeleteButton(document, entityRef.getUuid(), relatedEntityType);
			res.add(deleteButton);
			res.setFlexGrow(0, deleteButton);
		} else {
			nameLabel.setEnabled(false);
		}

		return res;
	}

	private Component buildDownloadButton(DocumentDto document) {
		Button viewButton = ButtonHelper.createIconButton(VaadinIcon.DOWNLOAD);

		StreamSourceDownloadHandler streamResource = new StreamSourceDownloadHandler(() -> {
			DocumentFacade documentFacade = FacadeProvider.getDocumentFacade();
			try {
				return new ByteArrayInputStream(documentFacade.getContent(document.getUuid()));
			} catch (IOException | IllegalArgumentException e) {
				NotificationHelper.show(
					String.format(I18nProperties.getString(Strings.errorReadingDocument), document),
					e.getMessage(),
					NotificationHelper.Type.ERROR_MESSAGE,
					false);
				return null;
			}
		}, document.getName());
		streamResource.setMIMEType(document.getMimeType());

		// was a FileDownloader on the button: a download link around it
		return DownloadUtil.createDownloadLink(viewButton, streamResource);
	}

	private Button buildDeleteButton(DocumentDto document, String relatedEntityUuid, DocumentRelatedEntityType relatedEntityType) {
		return ButtonHelper.createIconButton(
			"",
			VaadinIcon.TRASH,
			e -> VaadinUiUtil
				.showDeleteConfirmationWindow(String.format(I18nProperties.getString(Strings.confirmationDeleteFile), document.getName()), () -> {
					try {
						FacadeProvider.getDocumentFacade().deleteDocument(document.getUuid(), relatedEntityUuid, relatedEntityType);
					} catch (IllegalArgumentException ex) {
						NotificationHelper.show(
							I18nProperties.getString(Strings.errorDeletingDocument),
							ex.getMessage(),
							NotificationHelper.Type.ERROR_MESSAGE,
							false);
					}
					reload();
				}));
	}
}
