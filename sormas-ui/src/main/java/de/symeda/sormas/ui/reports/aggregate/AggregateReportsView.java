package de.symeda.sormas.ui.reports.aggregate;

import java.util.Date;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.flow.component.button.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.symeda.sormas.ui.utils.NotificationHelper;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.report.AggregateReportCriteria;
import de.symeda.sormas.api.report.AggregateReportGroupingLevel;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.EpiWeek;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.ExportEntityName;
import de.symeda.sormas.ui.utils.GridExportStreamResource;
import de.symeda.sormas.ui.utils.NotificationHelper;

@SuppressWarnings("serial")
public class AggregateReportsView extends AbstractAggregateReportsView {

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/aggregatereporting";

	private AggregateReportsGrid grid;
	private VerticalLayout gridLayout;
	private Button btnExport;
	private Button btnCreate;
	private Button btnEdit;

	private CheckBox showZeroRows;

	private AggregateReportsFilterForm aggregateReportsFilterForm;

	public AggregateReportsView() {
		super(VIEW_NAME);

		UserDto user = UiUtil.getUser();

		boolean criteriaUninitialized = !ViewModelProviders.of(AggregateReportsView.class).has(AggregateReportCriteria.class);
		criteria = ViewModelProviders.of(AggregateReportsView.class).get(AggregateReportCriteria.class);
		if (criteriaUninitialized || criteria.getEpiWeekFrom() == null || criteria.getEpiWeekTo() == null) {
			criteria.epiWeekFrom(DateHelper.getEpiWeek(new Date())).epiWeekTo(DateHelper.getEpiWeek(new Date()));
		}

		criteria.setAggregateReportGroupingLevel(
			criteria.getAggregateReportGroupingLevel() != null ? criteria.getAggregateReportGroupingLevel() : AggregateReportGroupingLevel.REGION);

		gridLayout = new VerticalLayout();

		gridLayout.add(createGroupingBar());

		gridLayout.add(createFilterBar(user));

		grid = new AggregateReportsGrid(criteria);
		gridLayout.add(grid);
		gridLayout.setPadding(true);
		gridLayout.setSpacing(false);
		gridLayout.setSizeFull();
		gridLayout.setFlexGrow(1, grid);
		gridLayout.setClassName("crud-main-layout");

		addComponent(gridLayout);

		if (UiUtil.permitted(UserRight.AGGREGATE_REPORT_EDIT)) {
			btnCreate = ButtonHelper.createIconButton(
				Captions.aggregateReportNewAggregateReport,
				VaadinIcon.PLUS_CIRCLE,
				e -> ControllerProvider.getAggregateReportController().openEditOrCreateWindow(() -> navigateTo(criteria), false, null),
				CssStyles.BUTTON_PRIMARY);

			addHeaderComponent(btnCreate);

			btnEdit = ButtonHelper.createIconButton(
				Captions.aggregateReportEditAggregateReport,
				VaadinIcon.EDIT,
				e -> ControllerProvider.getAggregateReportController().openEditOrCreateWindow(() -> navigateTo(criteria), true, null),
				CssStyles.BUTTON_PRIMARY);
			btnEdit.setVisible(false);

			addHeaderComponent(btnEdit);
		}

		if (UiUtil.permitted(UserRight.AGGREGATE_REPORT_EXPORT)) {
			btnExport = ButtonHelper.createIconButton(Captions.export, VaadinIcon.DOWNLOAD, null, CssStyles.BUTTON_PRIMARY);

			addHeaderComponent(btnExport);

			StreamResource streamResource = GridExportStreamResource.createStreamResource(grid, ExportEntityName.AGGREGATE_REPORTS);
			FileDownloader fileDownloader = new FileDownloader(streamResource);
			fileDownloader.extend(btnExport);
		}
	}

	private HorizontalLayout createGroupingBar() {
		HorizontalLayout jurisdictionLayout = new HorizontalLayout();
		AggregateReportGroupingSelector aggregateReportGroupingSelector = new AggregateReportGroupingSelector();
		aggregateReportGroupingSelector.setValue(criteria.getAggregateReportGroupingLevel());

		aggregateReportGroupingSelector.addValueChangeListener(e -> {
			AggregateReportGroupingLevel groupingValue = (AggregateReportGroupingLevel) e.getValue();
			criteria.setAggregateReportGroupingLevel(groupingValue);
			grid.setColumnsVisibility(groupingValue);
			grid.reload();
		});
		jurisdictionLayout.add(aggregateReportGroupingSelector);

		showZeroRows = new CheckBox();
		showZeroRows.setId(AggregateReportCriteria.SHOW_ZERO_ROWS);
		showZeroRows.setCaption(I18nProperties.getCaption(Captions.aggregateReportShowZeroRows));
		showZeroRows.addClassName(CssStyles.FORCE_CAPTION_CHECKBOX);

		showZeroRows.setValue(criteria.getShowZeroRows());

		showZeroRows.addValueChangeListener(e -> {
			criteria.setShowZeroRows(e.getValue());
			grid.reload();
		});

		jurisdictionLayout.add(showZeroRows);

		return jurisdictionLayout;
	}

	private VerticalLayout createFilterBar(UserDto user) {

		VerticalLayout filterLayout = new VerticalLayout();
		filterLayout.setSpacing(false);
		filterLayout.setPadding(false);
		filterLayout.setWidthFull();

		if (criteria.getRegion() == null) {
			criteria.setRegion(user.getRegion());
		}

		if (criteria.getDistrict() == null) {
			criteria.setDistrict(user.getDistrict());
		}

		if (criteria.getHealthFacility() == null) {
			criteria.setHealthFacility(user.getHealthFacility());
		}

		if (criteria.getPointOfEntry() == null) {
			criteria.setPointOfEntry(user.getPointOfEntry());
		}

		aggregateReportsFilterForm = new AggregateReportsFilterForm();
		aggregateReportsFilterForm.setValue(criteria);

		aggregateReportsFilterForm.addValueChangeListener(e -> {
			if (!aggregateReportsFilterForm.hasFilter()) {
				navigateTo(null);
			}
		});

		aggregateReportsFilterForm.addResetHandler(e -> {
			ViewModelProviders.of(AggregateReportsView.class).remove(AggregateReportCriteria.class);
			AggregateReportCriteria emptyCriteria = new AggregateReportCriteria();
			navigateTo(emptyCriteria, true);
		});

		aggregateReportsFilterForm.addApplyHandler(e -> {
			if (epiWeekFilterBarDataValidation()) {
				grid.reload();
			} else {
				NotificationHelper.showNotification(
					I18nProperties.getString(Strings.messageAggregatedReportEpiWeekFilterNotFilled),
					NotificationHelper.Type.HUMANIZED_MESSAGE,
					10000);
			}
		});

		filterLayout.add(aggregateReportsFilterForm);

		return filterLayout;
	}

	private boolean epiWeekFilterBarDataValidation() {
		return criteria.getEpiWeekTo() != null && criteria.getEpiWeekFrom() != null;
	}

	@Override
	public void enter(ViewChangeEvent event) {
		super.enter(event);
		EpiWeek epiWeekFrom = criteria.getEpiWeekFrom();
		EpiWeek epiWeekTo = criteria.getEpiWeekTo();

		if (criteria.getEpiWeekFrom() == null) {
			criteria.setEpiWeekFrom(epiWeekFrom != null ? epiWeekFrom : DateHelper.getEpiWeek(new Date()));
		}
		if (criteria.getEpiWeekTo() == null) {
			criteria.setEpiWeekTo(epiWeekTo != null ? epiWeekTo : DateHelper.getEpiWeek(new Date()));
		}

		grid.reload();
	}

}
