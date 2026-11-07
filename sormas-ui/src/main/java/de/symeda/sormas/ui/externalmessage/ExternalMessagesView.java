package de.symeda.sormas.ui.externalmessage;

import java.util.*;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.symeda.sormas.ui.navigation.ViewChangeListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.CountryHelper;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.externalmessage.ExternalMessageCriteria;
import de.symeda.sormas.api.externalmessage.ExternalMessageFetchResult;
import de.symeda.sormas.api.externalmessage.ExternalMessageStatus;
import de.symeda.sormas.api.externalmessage.NewMessagesState;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.feature.FeatureTypeProperty;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.systemevents.SystemEventType;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.SormasUI;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.utils.*;
import de.symeda.sormas.ui.utils.DateTimeField;

public class ExternalMessagesView extends AbstractView {

	private static final String SURVEY_FETCH_BUTTON_ENABLED = "SURVEY_FETCH_BUTTON_ENABLED";
	// was Button.setData(status)
	private static final String STATUS_DATA = "status";

	public static final String VIEW_NAME = "messages";

	private final ViewConfiguration viewConfiguration;
	private ExternalMessageCriteria criteria;

	private Button btnEnterBulkEditMode;
	private Button btnLeaveBulkEditMode;
	private MenuBar bulkOperationsDropdown;

	private Map<Button, String> statusButtons;
	private Button activeStatusButton;

	private ExternalMessageGridFilterForm filterForm;
	private final ExternalMessageGrid grid;

	public ExternalMessagesView() {

		super(VIEW_NAME);

		viewConfiguration = ViewModelProviders.of(ExternalMessagesView.class).get(ViewConfiguration.class);

		criteria = ViewModelProviders.of(ExternalMessagesView.class).get(ExternalMessageCriteria.class);
		if (criteria == null) {
			// init default filter
			criteria = new ExternalMessageCriteria();
			ViewModelProviders.of(ExternalMessagesView.class).get(ExternalMessageCriteria.class, criteria);
		}

		if (FacadeProvider.getFeatureConfigurationFacade().isPropertyValueTrue(FeatureType.EXTERNAL_MESSAGES, FeatureTypeProperty.FETCH_MODE)) {
			addHeaderComponent(ButtonHelper.createIconButton(Captions.externalMessageFetch, VaadinIcon.REFRESH, e -> {
				checkForConcurrentEventsAndFetch(false);
			}, CssStyles.BUTTON_PRIMARY));
		}

		if (FacadeProvider.getFeatureConfigurationFacade()
			.isPropertyValueTrue(FeatureType.EXTERNAL_MESSAGES, FeatureTypeProperty.SURVEY_FETCH_ENABLED)) {
			addHeaderComponent(
				ButtonHelper.createIconButton(
					Captions.surveyFetch,
					VaadinIcon.REFRESH,
					e -> checkForConcurrentEventsAndFetch(true),
					CssStyles.BUTTON_PRIMARY));
		}

		if (isBulkEditAllowed()) {
			btnEnterBulkEditMode = ButtonHelper.createIconButton(Captions.actionEnterBulkEditMode, VaadinIcon.CHECK_SQUARE_O, e -> {
				enterBulkEditMode();
			}, CssStyles.BUTTON_PRIMARY);
			btnEnterBulkEditMode.setVisible(!viewConfiguration.isInEagerMode());
			addHeaderComponent(btnEnterBulkEditMode);

			btnLeaveBulkEditMode = ButtonHelper.createIconButton(Captions.actionLeaveBulkEditMode, VaadinIcon.CLOSE, e -> {
				leaveBulkEditMode();
			}, CssStyles.BUTTON_PRIMARY);
			btnLeaveBulkEditMode.setVisible(viewConfiguration.isInEagerMode());
			addHeaderComponent(btnLeaveBulkEditMode);
		}

		VerticalLayout gridLayout = new VerticalLayout();
		addComponent(gridLayout);

		gridLayout.add(createFilterBar());

		gridLayout.add(createStatusFilterBar());

		grid = new ExternalMessageGrid(criteria);
		grid.addDataSizeChangeListener(e -> updateStatusButtons());

		gridLayout.add(grid);

		gridLayout.setPadding(true);
		styleGridLayout(gridLayout);
	}

	@Override
	public void enter(ViewChangeListener.ViewChangeEvent event) {
		String params = event.getParameters().trim();
		if (params.startsWith("?")) {
			params = params.substring(1);
			criteria.fromUrlParams(params);
		}
		updateFilterComponents();
		grid.reload();
	}

	public HorizontalLayout createFilterBar() {

		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.setPadding(false);
		filterLayout.setSpacing(true);
		filterLayout.setSizeUndefined();
		filterLayout.addClassName("wrap");

		filterForm = new ExternalMessageGridFilterForm();
		filterForm.addValueChangeListener(e -> {
			if (!filterForm.hasFilter()) {
				this.navigateTo(null);
			}
		});
		filterForm.addResetHandler(e -> {
			ViewModelProviders.of(ExternalMessagesView.class).remove(ExternalMessageCriteria.class);
			this.navigateTo(null, true);
		});
		filterForm.addApplyHandler(e -> {
			SormasUI.get().getNavigator().navigateTo(ExternalMessagesView.VIEW_NAME);
			grid.reload();
		});
		filterLayout.add(filterForm);

		return filterLayout;
	}

	public HorizontalLayout createStatusFilterBar() {
		HorizontalLayout statusFilterLayout = new HorizontalLayout();
		statusFilterLayout.setWidthFull();
		statusFilterLayout.setPadding(false);
		statusFilterLayout.setSpacing(true);
		statusFilterLayout.addClassName(CssStyles.VSPACE_3);

		statusButtons = new HashMap<>();

		activeStatusButton = createAndAddStatusButton(null, statusFilterLayout);

		createAndAddStatusButton(ExternalMessageStatus.UNPROCESSED, statusFilterLayout);
		createAndAddStatusButton(ExternalMessageStatus.PROCESSED, statusFilterLayout);
		if (!FacadeProvider.getConfigFacade().isConfiguredCountry(CountryHelper.COUNTRY_CODE_LUXEMBOURG)) {
			createAndAddStatusButton(ExternalMessageStatus.UNCLEAR, statusFilterLayout);
			createAndAddStatusButton(ExternalMessageStatus.FORWARDED, statusFilterLayout);
		}

		HorizontalLayout actionButtonsLayout = new HorizontalLayout();
		actionButtonsLayout.setSpacing(true);
		statusFilterLayout.add(actionButtonsLayout);
		// was Alignment.TOP_RIGHT: the layout grows, its content goes right
		statusFilterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, actionButtonsLayout);
		statusFilterLayout.setFlexGrow(1, actionButtonsLayout);
		actionButtonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

		bulkOperationsDropdown = createBulkOperationsDropdown();
		actionButtonsLayout.add(bulkOperationsDropdown);

		return statusFilterLayout;
	}

	private MenuBar createBulkOperationsDropdown() {
		final List<MenuBarHelper.MenuBarItem> menuBarItems = new ArrayList<>();

		if (UiUtil.permitted(UserRight.EXTERNAL_MESSAGE_LABORATORY_DELETE)
			|| UiUtil.permitted(UserRight.EXTERNAL_MESSAGE_DOCTOR_DECLARATION_DELETE)) {
			menuBarItems.add(new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.bulkDelete), VaadinIcon.TRASH, mi -> {
				ControllerProvider.getExternalMessageController()
					.deleteAllSelectedItems(grid.asMultiSelect().getSelectedItems(), grid, () -> navigateTo(criteria));
			}, true));
		}

		if (UiUtil.permitted(UserRight.EXTERNAL_MESSAGE_LABORATORY_PROCESS)
			|| UiUtil.permitted(UserRight.EXTERNAL_MESSAGE_DOCTOR_DECLARATION_PROCESS)) {
			menuBarItems.add(
				new MenuBarHelper.MenuBarItem(
					I18nProperties.getCaption(Captions.bulkEditAssignee),
					VaadinIcon.ELLIPSIS_H,
					mi -> ControllerProvider.getExternalMessageController()
						.assignAllSelectedItems(grid.asMultiSelect().getSelectedItems(), () -> navigateTo(criteria)),
					true));
		}

		MenuBar bulkOperationsDropdown = MenuBarHelper.createDropDown(Captions.bulkActions, menuBarItems);
		bulkOperationsDropdown.setVisible(viewConfiguration.isInEagerMode());

		return bulkOperationsDropdown;
	}

	private void styleGridLayout(VerticalLayout gridLayout) {
		gridLayout.setSpacing(false);
		gridLayout.setSizeFull();
		gridLayout.setFlexGrow(1, grid);
		gridLayout.setClassName("crud-main-layout");
	}

	private void updateFilterComponents() {
		setApplyingCriteria(true);
		updateStatusButtons();
		filterForm.setValue(criteria);
		setApplyingCriteria(false);
	}

	private void updateStatusButtons() {
		statusButtons.keySet().forEach(b -> {
			CssStyles.style(b, CssStyles.BUTTON_FILTER_LIGHT);
			b.setText(statusButtons.get(b));
			b.setSuffixComponent(null);
			if (ComponentUtil.getData(b, STATUS_DATA) == criteria.getExternalMessageStatus()) {
				activeStatusButton = b;
			}
		});
		ExternalMessageStatus activeStatus = null;
		if (activeStatusButton != null) {
			CssStyles.removeStyles(activeStatusButton, CssStyles.BUTTON_FILTER_LIGHT);
			// was an HTML caption with LayoutUtil.spanCss(BADGE, count): the count is a badge after the text
			activeStatusButton.setText(statusButtons.get(activeStatusButton));
			Span badge = new Span(String.valueOf(grid.getDataSize()));
			badge.addClassName(CssStyles.BADGE);
			activeStatusButton.setSuffixComponent(badge);
			activeStatus = (ExternalMessageStatus) ComponentUtil.getData(activeStatusButton, STATUS_DATA);
		}

		boolean processingPossible = UiUtil.permitted(FeatureType.CASE_SURVEILANCE, UserRight.CASE_CREATE, UserRight.CASE_EDIT)
			|| UiUtil.permitted(FeatureType.CONTACT_TRACING, UserRight.CONTACT_CREATE, UserRight.CONTACT_EDIT)
			|| UiUtil.permitted(FeatureType.EVENT_SURVEILLANCE, UserRight.EVENTPARTICIPANT_CREATE, UserRight.EVENTPARTICIPANT_EDIT);
		grid.updateProcessColumnVisibility((activeStatus == null || activeStatus.isProcessable()) && processingPossible);
	}

	private Button createAndAddStatusButton(@Nullable ExternalMessageStatus status, HorizontalLayout buttonLayout) {
		Button button = ButtonHelper.createButton(status == null ? I18nProperties.getCaption(Captions.all) : status.toString(), e -> {
			criteria.externalMessageStatus(status);
			navigateTo(criteria);
		}, CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER, CssStyles.BUTTON_FILTER_LIGHT);

		if (status != null) {
			ComponentUtil.setData(button, STATUS_DATA, status);
		}

		buttonLayout.add(button);
		statusButtons.put(button, button.getText());

		return button;
	}

	private void checkForConcurrentEventsAndFetch(boolean surveyMode) {
		boolean fetchAlreadyStarted = FacadeProvider.getSystemEventFacade().existsStartedEvent(SystemEventType.FETCH_EXTERNAL_MESSAGES);
		if (fetchAlreadyStarted) {
			VaadinUiUtil.showConfirmationPopup(
				I18nProperties.getString(Strings.headingFetchExternalMessages),
				new Span(I18nProperties.getString(Strings.confirmationFetchExternalMessages)),
				I18nProperties.getString(Strings.yes),
				I18nProperties.getString(Strings.no),
				480,
				confirmed -> {
					if (confirmed) {
						askForSinceDateAndFetch(surveyMode);
					}
				});
		} else {
			askForSinceDateAndFetch(surveyMode);
		}
	}

	private void askForSinceDateAndFetch(boolean surveyMode) {
		boolean atLeastOneFetchExecuted = FacadeProvider.getSyncFacade().hasAtLeastOneSuccessfullSyncOf(SystemEventType.FETCH_EXTERNAL_MESSAGES);
		if (atLeastOneFetchExecuted) {
			if (surveyMode) {
				fetchSurveyMessages(null);
			} else {
				fetchExternalMessages(null);
			}
		} else {
			showSinceDateSelectionWindow(since -> {
				if (surveyMode) {
					fetchSurveyMessages(since);
				} else {
					fetchExternalMessages(since);
				}
			});
		}
	}

	private void fetchExternalMessages(Date since) {
		ExternalMessageFetchResult fetchResult = FacadeProvider.getExternalMessageFacade().fetchAndSaveExternalMessages(since);
		if (!fetchResult.isSuccess()) {
			VaadinUiUtil.showWarningPopup(fetchResult.getError());
		} else if (NewMessagesState.NO_NEW_MESSAGES.equals(fetchResult.getNewMessagesState())) {
			VaadinUiUtil.showWarningPopup(I18nProperties.getCaption(Captions.externalMessageNoNewMessages));
		} else {
			grid.reload();
		}
	}

	private void fetchSurveyMessages(Date since) {
		FacadeProvider.getExternalMessageFacade().saveAndProcessSurveyResponses(since);
		grid.reload();
	}

	private void showSinceDateSelectionWindow(Consumer<Date> dateConsumer) {
		VerticalLayout verticalLayout = new VerticalLayout();
		Span label = new Span(I18nProperties.getString(Strings.confirmationSinceExternalMessages));
		verticalLayout.add(label);

		HorizontalLayout horizontalLayout = new HorizontalLayout();
		Button yesButton = ButtonHelper.createButton(Captions.actionYes);
		Button noButton = ButtonHelper.createButton(Captions.actionNo);
		Button cancelButton = ButtonHelper.createButton(Captions.actionCancel);
		CssStyles.style(cancelButton, CssStyles.BUTTON_PRIMARY);

		horizontalLayout.add(yesButton);
		horizontalLayout.add(noButton);
		horizontalLayout.add(cancelButton);
		horizontalLayout.setClassName(CssStyles.FLOAT_RIGHT);
		verticalLayout.add(horizontalLayout);

		Dialog window = VaadinUiUtil.showPopupWindow(verticalLayout);

		cancelButton.addClickListener(event -> window.close());
		noButton.addClickListener(event -> {
			dateConsumer.accept(null);
			window.close();
		});
		yesButton.addClickListener(yesEvent -> {
			horizontalLayout.remove(yesButton);
			horizontalLayout.remove(noButton);
			horizontalLayout.remove(cancelButton);

			Button confirmButton = ButtonHelper.createButton(Captions.actionConfirm);
			CssStyles.style(confirmButton, CssStyles.BUTTON_PRIMARY);

			DateTimeField dateTimeField = new DateTimeField();
			dateTimeField.addValidator(date -> {
				if (date == null) {
					throw new InvalidValueException("Since date has to be set");
				}
			});
			dateTimeField.addValidator(new FutureDateValidator(dateTimeField, 0, null));

			horizontalLayout.add(dateTimeField);
			horizontalLayout.add(confirmButton);

			confirmButton.addClickListener(confirmEvent -> {
				if (dateTimeField.isValid()) {
					dateConsumer.accept(dateTimeField.getValue());
					window.close();
				} else {
					NotificationHelper
						.show(I18nProperties.getString(Strings.messageCheckInputData), null, NotificationHelper.Type.ERROR_MESSAGE, true);
				}
			});
		});
	}

	private boolean isBulkEditAllowed() {
		return UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS);
	}

	private void enterBulkEditMode() {
		bulkOperationsDropdown.setVisible(true);
		viewConfiguration.setInEagerMode(true);
		btnEnterBulkEditMode.setVisible(false);
		btnLeaveBulkEditMode.setVisible(true);

		navigateTo(criteria);
	}

	private void leaveBulkEditMode() {
		bulkOperationsDropdown.setVisible(false);
		viewConfiguration.setInEagerMode(false);
		btnLeaveBulkEditMode.setVisible(false);
		btnEnterBulkEditMode.setVisible(true);

		navigateTo(criteria);
	}
}
