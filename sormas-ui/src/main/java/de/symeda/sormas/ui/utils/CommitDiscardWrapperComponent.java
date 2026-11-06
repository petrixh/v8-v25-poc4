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
package de.symeda.sormas.ui.utils;

import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.naming.CannotProceedException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.DomListenerRegistration;

import de.symeda.sormas.api.CoreFacade;
import de.symeda.sormas.api.DeletableFacade;
import de.symeda.sormas.api.EditPermissionType;
import de.symeda.sormas.api.common.DeletionDetails;
import de.symeda.sormas.api.event.EventDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.Descriptions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.location.LocationDto;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.SormasUI;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.clinicalcourse.HealthConditionsForm;
import de.symeda.sormas.ui.events.EventDataForm;
import de.symeda.sormas.ui.location.AccessibleTextField;
import de.symeda.sormas.ui.location.LocationEditForm;
import de.symeda.sormas.ui.person.PersonEditForm;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;
import de.symeda.sormas.ui.utils.fields.Field;
import de.symeda.sormas.ui.utils.fields.FieldGroup;

public class CommitDiscardWrapperComponent<C extends Component> extends VerticalLayout implements DirtyStateComponent, Buffered {

	private static final long serialVersionUID = 1L;
	private Set<String> activeButtons = new HashSet<>();

	public static final String DELETE_RESTORE = "deleteRestore";

	public static interface PreCommitListener {

		void onPreCommit(Runnable successCallback);
	}

	public static interface CommitListener {

		void onCommit() throws CannotProceedException;
	}

	public static interface DiscardListener {

		void onDiscard();
	}

	public static interface DoneListener {

		void onDone();
	}

	public static interface DeleteListener {

		void onDelete();
	}

	public static interface DeleteWithDetailsListener {

		void onDelete(DeletionDetails deletionDetails);
	}

	private transient PreCommitListener preCommitListener;
	private transient Runnable postCommitListener;
	private transient List<CommitListener> commitListeners = new ArrayList<>();
	private transient List<DiscardListener> discardListeners = new ArrayList<>();
	private transient List<DoneListener> doneListeners = new ArrayList<>();
	private transient List<DeleteListener> deleteListeners = new ArrayList<>();
	private transient List<DeleteWithDetailsListener> deleteWithDetailsListeners = new ArrayList<>();
	// only to check if it's set
	private transient CommitListener primaryCommitListener;

	// was a Panel: scrolls the wrapped component when it is larger than the wrapper
	private Scroller contentPanel;

	private C wrappedComponent;
	private ArrayList<FieldGroup> fieldGroups;

	private HorizontalLayout buttonsPanel;
	private Button commitButton;
	private Button discardButton;

	private Button deleteButton;

	private boolean commited = false;
	private boolean dirty = false;

	private boolean shortcutsEnabled = false;

	private boolean hideButtons = false;

	private boolean readOnly = false;

	protected transient List<ClickShortcut> actions;

	protected CommitDiscardWrapperComponent() {

	}

	public CommitDiscardWrapperComponent(C component, FieldGroup... fieldGroups) {
		this(component, null, fieldGroups);
	}

	public CommitDiscardWrapperComponent(C component, Boolean isEditingAllowed, FieldGroup... fieldGroups) {
		this(component, isEditingAllowed, false, fieldGroups);
	}

	public CommitDiscardWrapperComponent(C component, Boolean isEditingAllowed, boolean hideButtons, FieldGroup... fieldGroups) {
		this.hideButtons = hideButtons;
		setWrappedComponent(component, fieldGroups);
		if (isEditingAllowed != null) {
			setEnabled(isEditingAllowed);
		}
	}

	/**
	 * Meant to be used when the wrapper is required due to used API but the inner component(s) already contains the discard/save/delete UI.
	 * 
	 * @param component
	 *            that will be integrated within the wrapper.
	 * @param fieldGroups
	 *            additional fields
	 * @return invisible wrapper: no buttons
	 * @param <C>
	 *            component type.
	 */
	public static <C extends Component> CommitDiscardWrapperComponent<C> withoutButtons(C component, FieldGroup... fieldGroups) {
		return new CommitDiscardWrapperComponent<>(component, true, true, fieldGroups);
	}

	protected void setWrappedComponent(C component, FieldGroup... fieldGroups) {

		this.wrappedComponent = component;
		this.fieldGroups = new ArrayList(Arrays.asList(fieldGroups));

		if (contentPanel != null) {
			contentPanel.setContent(wrappedComponent);
			return;
		}

		setSpacing(false);
		// was setMargin(true): a Vaadin 8 margin is the layout's inner space, the padding in Flow
		setPadding(true);
		setSizeUndefined();

		contentPanel = new Scroller(component);
		updateInternalWidth();
		updateInternalHeight();
		add(contentPanel);
		setFlexGrow(1, contentPanel);

		if (!hideButtons) {
			buttonsPanel = new HorizontalLayout();
			buttonsPanel.setPadding(false);
			buttonsPanel.setSpacing(true);
			buttonsPanel.setWidthFull();

			// was expand ratio 1 and right alignment for discard: buttons added before it (delete, archive) stay left
			Button discardButton = getDiscardButton();
			buttonsPanel.add(discardButton);
			discardButton.getStyle().set("margin-left", "auto");

			Button commitButton = getCommitButton();
			buttonsPanel.add(commitButton);

			add(buttonsPanel);
		}

		setShortcutsEnabled(shortcutsEnabled);

		if (fieldGroups != null && fieldGroups.length > 0) {
			// convention: set wrapper to read-only when all wrapped field groups are read-only
			boolean allReadOnly = true;
			for (FieldGroup fieldGroup : fieldGroups) {
				if (!buffered(fieldGroup).isReadOnly()) {
					allReadOnly = false;
					break;
				}
			}
			if (allReadOnly) {
				setReadOnly(true);
			}
		} else if (wrappedComponent != null) {
			if (wrappedComponent instanceof HasValue && ((HasValue<?, ?>) wrappedComponent).isReadOnly()) {
				setReadOnly(true);
			}
		}

		dirty = false;
		addDirtyHandler(fieldGroups);
	}

	public void addFieldGroups(FieldGroup... fieldGroups) {

		if (this.fieldGroups == null) {
			this.fieldGroups = new ArrayList(Arrays.asList(fieldGroups));
		} else {
			this.fieldGroups.addAll(Arrays.asList(fieldGroups));
		}
		addDirtyHandler(fieldGroups);
	}

	public void removeFieldGroups(FieldGroup... fieldGroups) {
		if (fieldGroups == null || this.fieldGroups == null) {
			return;
		}
		this.fieldGroups.removeAll(Arrays.asList(fieldGroups));

	}

	@SuppressWarnings("deprecation")
	protected void addDirtyHandler(FieldGroup[] fieldGroups) {
		if (fieldGroups != null) {
			Stream.of(fieldGroups).forEach(fg -> fg.getFields().forEach(f -> f.addValueChangeListener(ev -> {
				final Object source = ev.getSource();
				// Note by @MateStrysewske: It seems like the duplicate code here is necessary; for some reason,
				// moving it to a separate method breaks the logic at least on my dev system
				if (source instanceof PersonEditForm) {
					final PersonEditForm personEditForm = (PersonEditForm) source;
					final LocationEditForm locationEditForm = personEditForm.getField(PersonDto.ADDRESS);
					if (atLeastOneFieldModified(
						locationEditForm.getField(LocationDto.LATITUDE),
						locationEditForm.getField(LocationDto.LONGITUDE),
						locationEditForm.getField(LocationDto.LAT_LON_ACCURACY))) {
						dirty = true;
					} else if (locationEditForm.getFieldGroup()
						.getFields()
						.stream()
						.filter(lf -> !(lf instanceof AccessibleTextField))
						.anyMatch(CommitDiscardWrapperComponent::isModified)) {
						dirty = true;
					} else if (personEditForm.getFieldGroup()
						.getFields()
						.stream()
						.filter(lf -> !(lf instanceof AccessibleTextField))
						.anyMatch(CommitDiscardWrapperComponent::isModified)) {
						dirty = true;
					}
				} else if (source instanceof EventDataForm) {
					final EventDataForm eventDataForm = (EventDataForm) source;
					final LocationEditForm locationEditForm = eventDataForm.getField(EventDto.EVENT_LOCATION);
					if (atLeastOneFieldModified(
						locationEditForm.getField(LocationDto.LATITUDE),
						locationEditForm.getField(LocationDto.LONGITUDE),
						locationEditForm.getField(LocationDto.LAT_LON_ACCURACY))) {
						dirty = true;
					} else if (locationEditForm.getFieldGroup()
						.getFields()
						.stream()
						.filter(lf -> !(lf instanceof AccessibleTextField))
						.anyMatch(CommitDiscardWrapperComponent::isModified)) {
						dirty = true;
					} else if (eventDataForm.getFieldGroup()
						.getFields()
						.stream()
						.filter(lf -> !(lf instanceof AccessibleTextField))
						.anyMatch(CommitDiscardWrapperComponent::isModified)) {
						dirty = true;
					}
				} else if (source instanceof LocationEditForm) {
					final LocationEditForm locationEditForm = (LocationEditForm) source;
					if (atLeastOneFieldModified(
						locationEditForm.getField(LocationDto.LATITUDE),
						locationEditForm.getField(LocationDto.LONGITUDE),
						locationEditForm.getField(LocationDto.LAT_LON_ACCURACY))) {
						dirty = true;
					} else if (locationEditForm.getFieldGroup()
						.getFields()
						.stream()
						.filter(lf -> !(lf instanceof AccessibleTextField))
						.anyMatch(CommitDiscardWrapperComponent::isModified)) {
						dirty = true;
					}
				} else if (source instanceof HealthConditionsForm) {
					final HealthConditionsForm healthConditionsForm = (HealthConditionsForm) source;
					if (healthConditionsForm.isModified()) {
						dirty = true;
					}
				} else if (source instanceof AccessibleTextField) {
					final AccessibleTextField accessibleTextField = (AccessibleTextField) source;
					if (accessibleTextField.isModified()) {
						dirty = true;
					}
				} else {
					dirty = true;
				}
			})));
		}
	}

	/**
	 * Was {@code Buffered::isModified} on Vaadin 7 fields: a field reports changes through {@link Buffered}.
	 */
	private static boolean isModified(Object field) {
		return field instanceof Buffered && ((Buffered) field).isModified();
	}

	private boolean atLeastOneFieldModified(AccessibleTextField... fields) {
		for (AccessibleTextField field : fields) {
			// was field.getState().modified, the same flag as isModified()
			if (field.isModified()) {
				return true;
			}
		}
		return false;
	}

	protected Stream<Field<?>> getFieldsStream() {

		if (fieldGroups != null) {
			return fieldGroups.stream().map(FieldGroup::getFields).flatMap(Collection::stream);
		} else {
			return Stream.empty();
		}
	}

	/**
	 * Whether the buttons can be operated by ENTER and ESC
	 * 
	 * @param shortcutsEnabled
	 */
	public void setShortcutsEnabled(boolean shortcutsEnabled) {

		if (shortcutsEnabled == this.shortcutsEnabled) {
			if (!shortcutsEnabled || actions != null)
				return;
		}

		this.shortcutsEnabled = shortcutsEnabled;

		if (actions == null) {
			actions = new ArrayList<>();
		}

		Collection<Component> notifiers = Arrays.asList(contentPanel);

		if (shortcutsEnabled) {
			for (Component notifier : notifiers) {
				registerActions(notifier);
			}

		} else {
			for (ClickShortcut action : actions) {
				action.remove();
			}
			actions.clear();
		}
	}

	protected void registerActions(Component notifier) {
		actions.add(new ClickShortcut(notifier, commitButton, "Enter"));
		actions.add(new ClickShortcut(notifier, discardButton, "Escape"));
	}

	public C getWrappedComponent() {
		return wrappedComponent;
	}

	public HorizontalLayout getButtonsPanel() {
		return buttonsPanel;
	}

	/**
	 * 
	 * Calling this method creates a save button. The button will not be added, though.
	 * This happens in setWrappedComponent().
	 * 
	 * @return
	 */
	public Button getCommitButton() {
		if (commitButton == null) {
			commitButton =
				ButtonHelper.createButton("commit", I18nProperties.getCaption(Captions.actionSave), event -> commitAndHandle(), CssStyles.BUTTON_PRIMARY);
		}

		return commitButton;
	}

	/**
	 * 
	 * Calling this method creates a discard button. The button will not be added, though.
	 * This happens in setWrappedComponent().
	 * 
	 * @return
	 */
	public Button getDiscardButton() {
		if (discardButton == null) {
			discardButton = ButtonHelper.createButton("discard", I18nProperties.getCaption(Captions.actionDiscard), event -> discard());
			discardButton.setTooltipText(I18nProperties.getDescription(Descriptions.discardDescription));
		}

		return discardButton;
	}

	public Button getDeleteButton(String entityName, Supplier<String> confirmationMessageSupplier) {
		if (deleteButton == null) {
			deleteButton = buildDeleteButton(() -> {
				String confirmationMessage = confirmationMessageSupplier == null ? null : confirmationMessageSupplier.get();

				VaadinUiUtil.showDeleteConfirmationWindow(
					StringUtils.isBlank(confirmationMessage)
						? String.format(I18nProperties.getString(Strings.confirmationDeleteEntity), entityName)
						: confirmationMessage,
					this::onDelete);
			}, false);
		}

		return deleteButton;
	}

	public Button getDeleteWithReasonOrRestoreButton(String entityName, boolean deleted, String details) {

		if (deleteButton == null) {
			deleteButton = buildDeleteButton(() -> {
				if (!deleted) {
					DeletableUtils.showDeleteWithReasonPopup(
						String.format(
							I18nProperties.getString(Strings.confirmationDeleteEntityWithDetails),
							entityName,
							details != null ? details : ""),
						this::onDeleteWithReason);
				} else {
					onDeleteWithReason(null);
				}
			}, deleted);
		}

		return deleteButton;
	}

	private Button buildDeleteButton(Runnable deletePopupCallback, boolean deleted) {

		Button deleteButton = ButtonHelper.createButton(
			DELETE_RESTORE,
			deleted ? I18nProperties.getCaption(Captions.actionRestore) : I18nProperties.getCaption(Captions.actionDelete),
			e -> {
				if (isDirty()) {
					DirtyCheckPopup.show(this, () -> deletePopupCallback.run());
				} else {
					deletePopupCallback.run();
				}
			},
			CssStyles.BUTTON_DANGER,
			CssStyles.BUTTON_BORDER_NEUTRAL);

		return deleteButton;
	}

	@Override
	public boolean isModified() {
		if (fieldGroups != null) {
			for (FieldGroup fieldGroup : fieldGroups) {
				if (buffered(fieldGroup).isModified()) {
					return true;
				}
			}
		} else if (wrappedComponent instanceof Buffered) {
			return ((Buffered) wrappedComponent).isModified();
		}
		return false;
	}

	public boolean isCommited() {
		return commited;
	}

	@Override
	public void commit() throws InvalidValueException, SourceException, CommitRuntimeException {

		if (preCommitListener != null) {
			preCommitListener.onPreCommit(this::doCommit);
		} else {
			doCommit();
		}

		if (postCommitListener != null) {
			postCommitListener.run();
		}

	}

	private void doCommit() throws InvalidValueException, SourceException, CommitRuntimeException {
		if (fieldGroups != null) {
			if (fieldGroups.size() > 1) {
				List<InvalidValueException> invalidValueExceptions =
					fieldGroups.stream().map(CommitDiscardWrapperComponent::buffered).filter(fieldGroup -> !fieldGroup.isValid()).map(fieldGroup -> {
						try {
							// all invalid fieldGroups are committed to fetch the InvalidValueExceptions
							fieldGroup.commit();
						} catch (InvalidValueException e) {
							// keep invalid value exceptions, the rest (SourceException) is thrown
							return e;
						}
						// when the fieldGroup did not throw an InvalidValueException, it is invalid and committed
						throw new IllegalStateException();
					}).collect(Collectors.toList());

				if (invalidValueExceptions.isEmpty()) {
					//NOOP
				} else if (invalidValueExceptions.size() == 1) {
					throw invalidValueExceptions.get(0);
				} else {
					throw new InvalidValueException(
						null,
						invalidValueExceptions.stream()
							.map(InvalidValueException::getCauses)
							.flatMap(Arrays::stream)
							.toArray(InvalidValueException[]::new));
				}
			}

			// was FieldGroup.commit() with a CommitException around the InvalidValueException or SourceException
			for (FieldGroup fieldGroup : fieldGroups) {
				buffered(fieldGroup).commit();
			}
		} else if (wrappedComponent instanceof Buffered) {
			((Buffered) wrappedComponent).commit();
		} else {
			// NOOP
		}
		dirty = false;

		onCommit();
		commited = true;
		onDone();
	}

	/**
	 * The field groups of the form layer implement {@link Buffered} (was the Vaadin 7 FieldGroup's commit, discard and
	 * modified state).
	 */
	private static Buffered buffered(FieldGroup fieldGroup) {
		if (fieldGroup instanceof Buffered) {
			return (Buffered) fieldGroup;
		}
		throw new IllegalStateException("FieldGroup does not implement Buffered: " + fieldGroup.getClass().getName());
	}

	private String findHtmlMessage(InvalidValueException exception) {
		if (!(exception.getMessage() == null || exception.getMessage().isEmpty()))
			return exception.getHtmlMessage();

		for (InvalidValueException cause : exception.getCauses()) {
			String message = findHtmlMessage(cause);
			if (message != null)
				return message;
		}

		return null;
	}

	private String findHtmlMessageDetails(InvalidValueException exception) {
		for (InvalidValueException cause : exception.getCauses()) {
			if (!cause.getMessage().equalsIgnoreCase(exception.getMessage())) {
				String message = findHtmlMessage(cause);
				if (message != null && !message.equalsIgnoreCase(exception.getMessage()))
					return message;
			}
		}

		return null;
	}

	@Override
	public boolean commitAndHandle() {
		try {
			commit();
			return true;
		} catch (InvalidValueException ex) {
			StringBuilder htmlMsg = new StringBuilder();
			String message = ex.getMessage();
			if (message != null && !message.isEmpty() && ex.getCauses().length == 0) {
				htmlMsg.append(ex.getHtmlMessage());
			} else {

				List<InvalidValueException> causes = extractCauses(ex);

				if (causes != null) {

					if (causes.size() > 1) {
						htmlMsg.append("<ul>");
						// All again
						for (InvalidValueException cause : causes) {
							htmlMsg.append("<li style=\"color: #FFF;\">").append(findHtmlMessage(cause)).append("</li>");
						}
						htmlMsg.append("</ul>");
					} else if (causes.size() > 0) {
						htmlMsg.append("<ul>");
						InvalidValueException firstCause = causes.get(0);
						String info = findHtmlMessage(firstCause);
						boolean validInfo = nonNull(info) && !info.isEmpty() && !info.equalsIgnoreCase("null");
						if (validInfo) {
							htmlMsg.append("<li style=\"color: #FFF;\">").append(info).append("</li>");
							htmlMsg.append("</ul>");
						}
						String additionalInfo = findHtmlMessageDetails(firstCause);
						if (nonNull(additionalInfo) && !additionalInfo.isEmpty()) {
							if (validInfo) {
								htmlMsg.append(" : ");
							}
							htmlMsg.append(findHtmlMessageDetails(firstCause));
						}
					}
				}
			}

			NotificationHelper.show(I18nProperties.getString(Strings.messageCheckInputData), htmlMsg.toString(), Type.ERROR_MESSAGE, true);

			return false;
		}
	}

	public List<InvalidValueException> extractCauses(InvalidValueException cause) {
		List<InvalidValueException> tempCauses = new ArrayList<>();

		if (cause.isInvisible()) {
			return tempCauses;
		}

		if (cause.getCauses().length == 0) {
			tempCauses.add(cause);
		}

		for (InvalidValueException childCause : cause.getCauses()) {
			tempCauses.addAll(extractCauses(childCause));
		}

		return tempCauses;
	}

	@Override
	public void discard() {
		if (fieldGroups != null) {
			for (FieldGroup fieldGroup : fieldGroups) {
				buffered(fieldGroup).discard();
			}
		} else if (wrappedComponent instanceof Buffered) {
			((Buffered) wrappedComponent).discard();
		} else {
			// NOOP
		}
		dirty = false;

		onDiscard();
		onDone();
	}

	@Override
	public void setBuffered(boolean buffered) {
		if (fieldGroups != null) {
			for (FieldGroup fieldGroup : fieldGroups) {
				buffered(fieldGroup).setBuffered(buffered);
			}
		} else if (wrappedComponent instanceof Buffered) {
			((Buffered) wrappedComponent).setBuffered(buffered);
		} else {
			// NOOP
		}
	}

	@Override
	public boolean isBuffered() {
		if (fieldGroups != null) {
			Boolean buffered = null;
			for (FieldGroup fieldGroup : fieldGroups) {
				if (buffered != null && buffered.booleanValue() != buffered(fieldGroup).isBuffered())
					throw new IllegalStateException("FieldGroups have different isBuffered states");
				buffered = buffered(fieldGroup).isBuffered();
			}
			return Boolean.TRUE.equals(buffered);
		} else if (wrappedComponent instanceof Buffered) {
			return ((Buffered) wrappedComponent).isBuffered();
		} else {
			return false;
		}
	}

	public void setPreCommitListener(PreCommitListener listener) {
		this.preCommitListener = listener;
	}

	public void setPostCommitListener(Runnable postCommitListener) {
		this.postCommitListener = postCommitListener;
	}

	public void addCommitListener(CommitListener listener) {
		if (!commitListeners.contains(listener))
			commitListeners.add(listener);
	}

	public void setPrimaryCommitListener(CommitListener listener) {
		if (primaryCommitListener != null)
			throw new UnsupportedOperationException("primary listener already set");
		if (!commitListeners.contains(listener))
			commitListeners.add(0, listener);
		primaryCommitListener = null;
	}

	public void removeCommitListener(CommitListener listener) {
		commitListeners.remove(listener);
		if (primaryCommitListener != null && primaryCommitListener.equals(listener))
			primaryCommitListener = null;
	}

	protected void onCommit() {

		for (CommitListener listener : commitListeners) {
			try {
				listener.onCommit();
			} catch (CannotProceedException e) {
				break;
			}
		}
	}

	/**
	 * 
	 * Adds a discard listener , but does not show it.
	 * 
	 * @param listener
	 */
	public void addDiscardListener(DiscardListener listener) {
		if (!discardListeners.contains(listener))
			discardListeners.add(listener);
	}

	public void removeDiscardListener(DiscardListener listener) {
		discardListeners.remove(listener);
	}

	private void onDiscard() {
		for (DiscardListener listener : discardListeners)
			listener.onDiscard();
	}

	public void addDoneListener(DoneListener listener) {
		if (!doneListeners.contains(listener))
			doneListeners.add(listener);
	}

	public void removeDoneListener(DoneListener listener) {
		doneListeners.remove(listener);
	}

	private void onDone() {
		for (DoneListener listener : doneListeners)
			listener.onDone();
	}

	public void addDeleteListener(DeleteListener listener, String entityName) {
		if (deleteListeners.isEmpty())
			buttonsPanel.addComponentAtIndex(0, getDeleteButton(entityName, null));
		if (!deleteListeners.contains(listener))
			deleteListeners.add(listener);
	}

	public void addDeleteListener(DeleteListener listener, String entityName, Supplier<String> confirmationMessageSupplier) {
		if (deleteListeners.isEmpty())
			buttonsPanel.addComponentAtIndex(0, getDeleteButton(entityName, confirmationMessageSupplier));
		if (!deleteListeners.contains(listener))
			deleteListeners.add(listener);
	}

	public void addDeleteWithReasonOrRestoreListener(DeleteWithDetailsListener listener, String entityName) {

		addDeleteWithReasonListener(listener, entityName, null);
	}

	public void addDeleteWithReasonListener(DeleteWithDetailsListener listener, String entityName, String details) {

		if (deleteWithDetailsListeners.isEmpty()) {
			buttonsPanel.addComponentAtIndex(0, getDeleteWithReasonOrRestoreButton(entityName, false, null));
		}
		if (!deleteWithDetailsListeners.contains(listener)) {
			deleteWithDetailsListeners.add(listener);
		}
	}

	public void addDeleteWithReasonOrRestoreListener(
		DeleteWithDetailsListener deleteListener,
		String details,
		DeleteWithDetailsListener restoreListener,
		String entityName,
		String entityUuid,
		CoreFacade coreFacade) {

		final boolean deleted = coreFacade.isDeleted(entityUuid);

		if (deleteWithDetailsListeners.isEmpty()) {
			buttonsPanel.addComponentAtIndex(0, getDeleteWithReasonOrRestoreButton(entityName, deleted, details));
		}

		if (!deleted) {
			deleteWithDetailsListeners.add(deleteListener);
		} else {
			deleteWithDetailsListeners.add(restoreListener);
		}
	}

	public void addDeleteWithReasonOrRestoreListener(
		DeleteWithDetailsListener deleteListener,
		DeleteWithDetailsListener restoreListener,
		String entityName,
		boolean isDeleted) {

		if (deleteWithDetailsListeners.isEmpty()) {
			buttonsPanel.addComponentAtIndex(0, getDeleteWithReasonOrRestoreButton(entityName, isDeleted, null));
		}

		if (!isDeleted) {
			deleteWithDetailsListeners.add(deleteListener);
		} else {
			deleteWithDetailsListeners.add(restoreListener);
		}
	}

	public void addDeleteWithReasonOrRestoreListener(
		String viewName,
		String details,
		String entityName,
		String entityUuid,
		DeletableFacade deletableFacade) {
		addDeleteWithReasonOrRestoreListener(viewName, details, entityName, entityUuid, deletableFacade, null, null);
	}

	public void addDeleteWithReasonOrRestoreListener(
		String viewName,
		String details,
		String entityName,
		String entityUuid,
		DeletableFacade deletableFacade,
		String restoreInvalidMessage,
		Function<String, Boolean> checkRestoreValidity) {

		final boolean deleted = deletableFacade.isDeleted(entityUuid);

		if (deleteWithDetailsListeners.isEmpty()) {
			buttonsPanel.addComponentAtIndex(0, getDeleteWithReasonOrRestoreButton(entityName, deleted, details));
		}

		if (!deleted) {
			deleteWithDetailsListeners.add((deleteDetails) -> deletableFacade.delete(entityUuid, deleteDetails));
		} else {
			deleteWithDetailsListeners.add((deleteDetails) -> {
				if (checkRestoreValidity == null || checkRestoreValidity.apply(entityUuid)) {
					deletableFacade.restore(entityUuid);
				} else {
					NotificationHelper.showNotification(restoreInvalidMessage, Type.ERROR_MESSAGE, -1);
				}
			});
		}
		deleteWithDetailsListeners.add((deleteDetails) -> SormasUI.get().getNavigator().navigateTo(viewName));

	}

	private void onDelete() {
		for (DeleteListener listener : deleteListeners)
			listener.onDelete();
	}

	private void onDeleteWithReason(DeletionDetails deletionDetails) {
		for (DeleteWithDetailsListener listener : deleteWithDetailsListeners) {
			listener.onDelete(deletionDetails);
		}
	}

	/**
	 * Vaadin 8 kept a read-only flag on the layout (it did not change the children); kept as a flag.
	 */
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * Clicks the button on a key pressed in the notifier, except in a text area (where Enter and Escape edit the text).
	 * Was a Vaadin 8 {@code Button.ClickShortcut} action.
	 */
	protected static class ClickShortcut implements java.io.Serializable {

		private static final long serialVersionUID = 1L;

		private final Component notifier;
		private final Button button;
		private final String key;
		private final DomListenerRegistration registration;

		public ClickShortcut(Component notifier, Button button, String key) {
			this.notifier = notifier;
			this.button = button;
			this.key = key;
			this.registration = notifier.getElement().addEventListener("keydown", e -> {
				// Vaadin 8: Button.click() did nothing for a disabled or read-only button
				if (button.isEnabled() && button.isVisible()) {
					button.click();
				}
			})
				.setFilter(
					"event.key === '" + key
						+ "' && !(event.target && event.target.closest && event.target.closest('vaadin-text-area, textarea, vaadin-rich-text-editor'))");
		}

		public void remove() {
			registration.remove();
		}

		@Override
		public String toString() {
			return notifier + "[" + key + "] =>" + button.getText();
		}
	}

	public static class CommitRuntimeException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public CommitRuntimeException(Throwable e) {
			super(e.getMessage(), e);
		}
	}

	/**
	 * Was {@code setWidth(float, Unit)}; all Flow width setters end here.
	 */
	@Override
	public void setWidth(String width) {
		super.setWidth(width);
		updateInternalWidth();
	}

	@Override
	public void setHeight(String height) {
		super.setHeight(height);
		updateInternalHeight();
	}

	private void updateInternalWidth() {
		if (contentPanel == null) {
			return;
		}
		if (getWidth() == null) {
			contentPanel.setWidth(null);
		} else {
			contentPanel.setWidthFull();
		}
	}

	private void updateInternalHeight() {
		if (contentPanel == null) {
			return;
		}
		if (getHeight() == null) {
			contentPanel.setHeight(null);
		} else {
			contentPanel.setHeightFull();
		}
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	//In case of having delete right without edit right the delete button should remain enabled
	public void restrictEditableComponentsOnEditView(
		UserRight editParentRight,
		UserRight editChildRight,
		UserRight deleteEntityRight,
		EditPermissionType editPermissionType,
		boolean isInJurisdiction) {

		boolean isEditAllowed = isEditAllowed(editParentRight, editChildRight, editPermissionType);

		if (!isEditAllowed) {
			if (isInJurisdiction && UiUtil.permitted(deleteEntityRight)) {
				addToActiveButtonsList(CommitDiscardWrapperComponent.DELETE_RESTORE);
			}

			this.setNonEditable();
		}
	}

	public void restrictEditableComponentsOnEditView(
		UserRight editParentRight,
		UserRight editChildRight,
		UserRight deleteEntityRight,
		UserRight archiveEntityRight,
		EditPermissionType editPermissionType,
		boolean isInJurisdiction) {

		boolean isEditAllowed = isEditAllowed(editParentRight, editChildRight, editPermissionType);

		if (!isEditAllowed) {
			if (isInJurisdiction && UiUtil.permitted(deleteEntityRight)) {
				addToActiveButtonsList(CommitDiscardWrapperComponent.DELETE_RESTORE);
			}
			if (isInJurisdiction && archiveEntityRight != null && UiUtil.permitted(archiveEntityRight)) {
				addToActiveButtonsList(ArchivingController.ARCHIVE_DEARCHIVE_BUTTON_ID);
			}

			this.setNonEditable();
		}
	}

	public void setNonEditable() {
		this.setEditable(false, activeButtons.stream().toArray(String[]::new));
	}

	public boolean isEditAllowed(UserRight editParentRight, UserRight editChildRight, EditPermissionType editPermissionType) {
		if (editChildRight != null) {
			return UiUtil.permitted(editParentRight, editChildRight);
		} else {
			return UiUtil.permitted(editPermissionType, editParentRight);
		}
	}

	//excludedButtons: contains the buttons attached to the CommitDiscardWrapperComponent which we intend to
	// exclude from applying a new editable status
	public void setEditable(boolean editable, String... excludedButtons) {
		if (WithChildComponents.class.isAssignableFrom(wrappedComponent.getClass())) {
			((WithChildComponents) wrappedComponent).forEachComponent(component -> {
				setEnabled(component, editable);
			});
		} else {
			setEnabled(wrappedComponent, editable);
		}

		for (int i = 0; i < buttonsPanel.getComponentCount(); i++) {
			Component button = buttonsPanel.getComponentAt(i);
			if (!ArrayUtils.contains(excludedButtons, button.getId().orElse(null))) {
				setEnabled(button, editable);
			}
		}
	}

	private static void setEnabled(Component component, boolean enabled) {
		if (component instanceof HasEnabled) {
			((HasEnabled) component).setEnabled(enabled);
		} else {
			component.getElement().setEnabled(enabled);
		}
	}

	public void addToActiveButtonsList(String button) {
		activeButtons.add(button);
	}

	public void setButtonsVisible(boolean visible) {
		buttonsPanel.setVisible(visible);
	}
}
