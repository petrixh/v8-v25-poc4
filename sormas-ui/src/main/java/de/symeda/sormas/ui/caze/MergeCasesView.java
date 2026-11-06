package de.symeda.sormas.ui.caze;

import static de.symeda.sormas.ui.utils.AbstractMergeGrid.DUPLICATE_MERGING_LIMIT_DEFAULT;
import static de.symeda.sormas.ui.utils.AbstractMergeGrid.DUPLICATE_MERGING_LIMIT_MAX;

import java.util.Date;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.caze.CaseCriteria;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;
import de.symeda.sormas.ui.utils.AbstractView;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;
import de.symeda.sormas.ui.utils.QueryDetails;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

@SuppressWarnings("serial")
public class MergeCasesView extends AbstractView {

	public static final String VIEW_NAME = CasesView.VIEW_NAME + "/merge";

	private final MergeCasesGrid grid;
	private final MergeCasesFilterComponent filterComponent;
	private final CaseCriteria criteria;

	public MergeCasesView() {
		super(VIEW_NAME);

		boolean criteriaUninitialized = !ViewModelProviders.of(MergeCasesView.class).has(CaseCriteria.class);
		criteria = ViewModelProviders.of(MergeCasesView.class).get(CaseCriteria.class);
		if (criteriaUninitialized) {
			criteria.creationDateFrom(DateHelper.getEpiWeekStart(DateHelper.getPreviousEpiWeek(new Date())))
				.creationDateTo(new Date())
				.relevanceStatus(EntityRelevanceStatus.ACTIVE)
				.setRegion(UiUtil.getUser().getRegion());
		}

		boolean queryDetailsUninitialized = !ViewModelProviders.of(MergeCasesView.class).has(QueryDetails.class);
		QueryDetails queryDetails = ViewModelProviders.of(MergeCasesView.class).get(QueryDetails.class);
		if (queryDetailsUninitialized || queryDetails.getResultLimit() == null) {
			queryDetails.setResultLimit(DUPLICATE_MERGING_LIMIT_DEFAULT);
		}

		grid = new MergeCasesGrid();
		grid.setCriteria(criteria);
		grid.setQueryDetails(queryDetails);

		VerticalLayout gridLayout = new VerticalLayout();
		filterComponent = new MergeCasesFilterComponent(criteria, queryDetails);
		filterComponent.setFiltersUpdatedCallback(() -> {
			if (ViewModelProviders.of(MergeCasesView.class).has(CaseCriteria.class)) {
				navigateTo(criteria, queryDetails);
			} else {
				navigateTo();
			}
		});
		filterComponent.setIgnoreRegionCallback(grid::reload);
		gridLayout.add(filterComponent);

		gridLayout.add(grid);
		gridLayout.setPadding(true);
		gridLayout.setSpacing(false);
		gridLayout.setSizeFull();
		gridLayout.setFlexGrow(1, grid);
		gridLayout.setClassName("crud-main-layout");
		addComponent(gridLayout);

		Button btnOpenGuide = ButtonHelper.createIconButton(Captions.caseOpenMergeGuide, VaadinIcon.QUESTION, e -> buildAndOpenMergeInstructions());
		addHeaderComponent(btnOpenGuide);

		Button btnCalculateCompleteness =
			ButtonHelper.createIconButton(Captions.caseCalculateCompleteness, VaadinIcon.CALC, e -> showCalculateCompletenessWindow());

		addHeaderComponent(btnCalculateCompleteness);

		Button btnBack = ButtonHelper.createIconButton(
			Captions.caseBackToDirectory,
			VaadinIcon.ARROW_BACKWARD,
			e -> ControllerProvider.getCaseController().navigateToIndex(),
			CssStyles.BUTTON_PRIMARY);

		addHeaderComponent(btnBack);
	}

	private void reloadAndUpdateDuplicateCount() {
		grid.reload();
		filterComponent.updateDuplicateCountLabel(grid.getTreeData().getRootItems().size());
	}

	private void buildAndOpenMergeInstructions() {
		Dialog window = VaadinUiUtil.showPopupWindow(new MergeGuideLayout());
		window.setWidth(1024, Unit.PIXELS);
		window.setHeaderTitle(I18nProperties.getString(Strings.headingMergeGuide));
	}

	private void showCalculateCompletenessWindow() {
		VaadinUiUtil.showConfirmationPopup(
			I18nProperties.getString(Strings.headingConfirmUpdateCompleteness),
			new Span(I18nProperties.getString(Strings.confirmationUpdateCompleteness)),
			I18nProperties.getString(Strings.yes),
			I18nProperties.getString(Strings.no),
			null,
			e -> {
				if (e) {
					grid.calculateCompletenessValues();
					NotificationHelper.show("", I18nProperties.getString(Strings.messageCompletenessValuesUpdated), Type.HUMANIZED_MESSAGE);
				}
			});
	}

	@Override
	public void enter(ViewChangeEvent event) {

		String params = event.getParameters().trim();
		if (params.startsWith("?")) {
			params = params.substring(1);

			CaseCriteria criteria = ViewModelProviders.of(MergeCasesView.class).get(CaseCriteria.class);
			criteria.fromUrlParams(params);
			QueryDetails queryDetails = ViewModelProviders.of(MergeCasesView.class).get(QueryDetails.class);
			queryDetails.fromUrlParams(params);

			queryDetails.setResultLimit(
				queryDetails.getResultLimit() != null
					? Math.max(1, Math.min(queryDetails.getResultLimit(), DUPLICATE_MERGING_LIMIT_MAX))
					: DUPLICATE_MERGING_LIMIT_DEFAULT);

			filterComponent.setValue(criteria, queryDetails);
		}

		if (!ViewModelProviders.of(MergeCasesView.class).get(MergeCasesViewConfiguration.class).isFiltersApplied()) {
			VaadinUiUtil.showSimplePopupWindow(
				I18nProperties.getString(Strings.headingCaution),
				I18nProperties.getString(Strings.infoMergeFiltersHint),
				ContentMode.HTML,
				640);
			ViewModelProviders.of(MergeCasesView.class).get(MergeCasesViewConfiguration.class).setFiltersApplied(true);
		} else {
			reloadAndUpdateDuplicateCount();
		}
	}
}
