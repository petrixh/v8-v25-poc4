package de.symeda.sormas.ui.contact;

import java.util.Date;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.contact.ContactCriteria;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;
import de.symeda.sormas.ui.utils.AbstractMergeGrid;
import de.symeda.sormas.ui.utils.AbstractView;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;
import de.symeda.sormas.ui.utils.QueryDetails;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

@SuppressWarnings("serial")
public class MergeContactsView extends AbstractView {

	public static final String VIEW_NAME = ContactsView.VIEW_NAME + "/merge";

	private ContactCriteria criteria;

	private MergeContactsGrid grid;
	private MergeContactsFilterComponent filterComponent;

	public MergeContactsView() {
		super(VIEW_NAME);

		boolean criteriaUninitialized = !ViewModelProviders.of(MergeContactsView.class).has(ContactCriteria.class);

		criteria = ViewModelProviders.of(MergeContactsView.class).get(ContactCriteria.class);
		if (criteriaUninitialized) {
			criteria.creationDateFrom(DateHelper.subtractDays(new Date(), 30)).creationDateTo(new Date()).setRegion(UiUtil.getUser().getRegion());
		}

		boolean queryDetailsUninitialized = !ViewModelProviders.of(MergeContactsView.class).has(QueryDetails.class);
		QueryDetails queryDetails = ViewModelProviders.of(MergeContactsView.class).get(QueryDetails.class);
		if (queryDetailsUninitialized || queryDetails.getResultLimit() == null) {
			queryDetails.setResultLimit(AbstractMergeGrid.DUPLICATE_MERGING_LIMIT_DEFAULT);
		}

		grid = new MergeContactsGrid();
		grid.setCriteria(criteria);
		grid.setQueryDetails(queryDetails);

		VerticalLayout gridLayout = new VerticalLayout();
		filterComponent = new MergeContactsFilterComponent(criteria, queryDetails);
		filterComponent.setFiltersUpdatedCallback(() -> {
			if (ViewModelProviders.of(MergeContactsView.class).has(ContactCriteria.class)) {
				navigateTo(criteria, queryDetails);
			} else {
				navigateTo();
			}
		});
		filterComponent.setIgnoreRegionCallback(this::reloadAndUpdateDuplicateCount);
		gridLayout.add(filterComponent);

		gridLayout.add(grid);
		gridLayout.setPadding(true);
		gridLayout.setSpacing(false);
		gridLayout.setSizeFull();
		gridLayout.setFlexGrow(1, grid);
		gridLayout.setClassName("crud-main-layout");
		addComponent(gridLayout);

		Button btnOpenGuide =
			ButtonHelper.createIconButton(Captions.contactOpenMergeGuide, VaadinIcon.QUESTION, e -> buildAndOpenMergeInstructions());
		addHeaderComponent(btnOpenGuide);

		Button btnCalculateCompleteness =
			ButtonHelper.createIconButton(Captions.contactCalculateCompleteness, VaadinIcon.CALC, e -> showCalculateCompletenessWindow());

		addHeaderComponent(btnCalculateCompleteness);

		Button btnBack = ButtonHelper.createIconButton(
			Captions.contactBackToDirectory,
			VaadinIcon.ARROW_BACKWARD,
			e -> ControllerProvider.getContactController().navigateToIndex(),
			CssStyles.BUTTON_PRIMARY);

		addHeaderComponent(btnBack);
	}

	private void buildAndOpenMergeInstructions() {
		Dialog window = VaadinUiUtil.showPopupWindow(new MergeGuideLayout());
		window.setWidth(1024, Unit.PIXELS);
		window.setHeaderTitle(I18nProperties.getString(Strings.headingContactMergeGuide));
	}

	private void showCalculateCompletenessWindow() {
		VaadinUiUtil.showConfirmationPopup(
			I18nProperties.getString(Strings.headingConfirmUpdateCompleteness),
			new Span(I18nProperties.getString(Strings.confirmationUpdateCompleteness)),
			I18nProperties.getString(Strings.yes),
			I18nProperties.getString(Strings.no),
			null,
			e -> {
				if (e.booleanValue() == true) {
					grid.calculateCompletenessValues();
					NotificationHelper.show("", I18nProperties.getString(Strings.messageCompletenessValuesUpdated), Type.HUMANIZED_MESSAGE);
				}
			});
	}

	@Override
	public void enter(ViewChangeEvent event) {
		if (!ViewModelProviders.of(MergeContactsView.class).get(MergeContactsViewConfiguration.class).isFiltersApplied()) {
			VaadinUiUtil.showSimplePopupWindow(
				I18nProperties.getString(Strings.headingCaution),
				I18nProperties.getString(Strings.infoMergeFiltersHint),
				ContentMode.HTML,
				640);
			ViewModelProviders.of(MergeContactsView.class).get(MergeContactsViewConfiguration.class).setFiltersApplied(true);
		} else {
			reloadAndUpdateDuplicateCount(false);
		}
	}

	private void reloadAndUpdateDuplicateCount(boolean ignoreRegion) {
		grid.reload(ignoreRegion);
		filterComponent.updateDuplicateCountLabel(grid.getTreeData().getRootItems().size());

	}
}
