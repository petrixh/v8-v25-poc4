package de.symeda.sormas.ui.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.PermanentlyDeletableFacade;
import de.symeda.sormas.api.common.progress.ProcessedEntity;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.uuid.HasUuid;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;

public class PermanentDeleteController<F extends PermanentlyDeletableFacade> {

	public <T extends HasUuid> void deleteAllSelectedItems(
		Collection<T> entities,
		IPermanentDeleteHandler<?> deleteHandler,
		boolean allItemsAreEligibleForDeletion,
		Consumer<List<T>> batchCallback) {

		if (entities.isEmpty()) {
			displayNothingSelectedToBeDeleted(deleteHandler.getDeleteRestoreMessages());
			return;
		}

		if (!allItemsAreEligibleForDeletion) {
			displayErrorMessage(deleteHandler.getDeleteRestoreMessages());
			return;
		}

		String deleteConfirmationMessage = String.format(
			I18nProperties.getString(Strings.confirmationDeleteEntities),
			entities.size(),
			I18nProperties.getString(deleteHandler.getDeleteRestoreMessages().getEntities()).toLowerCase());

		VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.setMargin(false);

		Span contentLabel = VaadinUiUtil.createLabel(deleteConfirmationMessage, ContentMode.HTML);
		contentLabel.addClassName(CssStyles.LABEL_WHITE_SPACE_NORMAL);
		contentLabel.setWidthFull();
		verticalLayout.add(contentLabel);

		VaadinUiUtil.showConfirmationPopup(
			I18nProperties.getString(Strings.headingDeleteConfirmation),
			verticalLayout,
			I18nProperties.getString(Strings.yes),
			I18nProperties.getString(Strings.no),
			640,
			confirmed -> {
				if (Boolean.TRUE.equals(confirmed)) {

					List<T> selectedEntitiesCpy = new ArrayList<>(entities);
					this.<T> createBulkOperationHandler(deleteHandler)
						.doBulkOperation(
							selectedEntries -> deleteHandler.delete(selectedEntries.stream().map(HasUuid::getUuid).collect(Collectors.toList())),
							selectedEntitiesCpy,
							batchCallback);
				}

				return true;
			});

	}

	private void displayNothingSelectedToBeDeleted(DeleteRestoreMessages messages) {
		NotificationHelper.show(
			I18nProperties.getString(messages.getHeadingNoSelection()),
			I18nProperties.getString(messages.getMessageNoSelection()),
			Type.WARNING_MESSAGE,
			false);
	}

	private void displayErrorMessage(DeleteRestoreMessages messages) {
		NotificationHelper.show(I18nProperties.getString(messages.getMessageEntitiesEligibleForDeletion()), Type.ERROR_MESSAGE);
	}

	private <T extends HasUuid> BulkOperationHandler<T> createBulkOperationHandler(IPermanentDeleteHandler<?> deleteHandler) {
		DeleteRestoreMessages deleteRestoreMessages = deleteHandler.getDeleteRestoreMessages();
		return new BulkOperationHandler<>(
			deleteRestoreMessages.getMessageEntitiesDeleted(),
			null,
			deleteRestoreMessages.getHeadingSomeEntitiesNotDeleted(),
			deleteRestoreMessages.getHeadingEntitiesNotDeleted(),
			deleteRestoreMessages.getMessageCountEntitiesNotDeleted(),
			null,
			null,
			deleteRestoreMessages.getMessageCountEntitiesNotDeletedAccessDeniedReason(),
			null,
			Strings.infoBulkProcessFinishedWithSkipsOutsideJurisdictionOrNotEligible,
			Strings.infoBulkProcessFinishedWithoutSuccess);
	}

	public interface IPermanentDeleteHandler<T extends HasUuid> {

		List<ProcessedEntity> delete(List<String> uuids);

		DeleteRestoreMessages getDeleteRestoreMessages();
	}

}
