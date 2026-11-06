package de.symeda.sormas.ui.configuration.infrastructure;

import static de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.Descriptions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.InfrastructureType;
import de.symeda.sormas.api.infrastructure.area.AreaCriteria;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.configuration.AbstractConfigurationView;
import de.symeda.sormas.ui.configuration.infrastructure.components.SearchField;
import de.symeda.sormas.ui.utils.ArchiveHandlers;
import de.symeda.sormas.ui.utils.ArchiveMessages;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.ExportEntityName;
import de.symeda.sormas.ui.utils.GridExportStreamResource;
import de.symeda.sormas.ui.utils.MenuBarHelper;
import de.symeda.sormas.ui.utils.RowCount;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.ViewConfiguration;

@SuppressWarnings("serial")
public class AreasView extends AbstractConfigurationView {

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/areas";

	private final AreaCriteria criteria;
	private final ViewConfiguration viewConfiguration;

	private SearchField searchField;
	private ComboBox<EntityRelevanceStatus> filterRelevanceStatus;
	private Button btnResetFilters;

	private final AreasGrid grid;
	protected Button btnCreate;
	private Button btnImport;
	private MenuBar dropdownBulkOperations;

	public AreasView() {
		super(VIEW_NAME);

		viewConfiguration = ViewModelProviders.of(AreasView.class).get(ViewConfiguration.class);
		criteria = ViewModelProviders.of(AreasView.class).get(AreaCriteria.class);
		if (criteria.getRelevanceStatus() == null) {
			criteria.relevanceStatus(EntityRelevanceStatus.ACTIVE);
		}

		grid = new AreasGrid(criteria);
		VerticalLayout gridLayout = new VerticalLayout();
		gridLayout.add(createFilterBar());
		gridLayout.add(new RowCount(Strings.labelNumberOfAreas, grid.getDataSize()));
		gridLayout.add(grid);
		gridLayout.setPadding(true);
		gridLayout.setSpacing(false);
		gridLayout.setFlexGrow(1, grid);
		gridLayout.setSizeFull();
		gridLayout.setClassName("crud-main-layout");

		boolean infrastructureDataEditable = UiUtil.enabled(FeatureType.EDIT_INFRASTRUCTURE_DATA);

		if (UiUtil.permitted(infrastructureDataEditable, UserRight.INFRASTRUCTURE_IMPORT)) {
			btnImport = ButtonHelper.createIconButton(Captions.actionImport, VaadinIcon.UPLOAD, e -> {
				Dialog window = VaadinUiUtil.showPopupWindow(new InfrastructureImportLayout(InfrastructureType.AREA));
				window.setHeaderTitle(I18nProperties.getString(Strings.headingImportAreas));
				VaadinUiUtil.addCloseListener(window, c -> {
					grid.reload();
				});
			}, CssStyles.BUTTON_PRIMARY);

			addHeaderComponent(btnImport);
		} else if (!infrastructureDataEditable) {
			// Flow components have no caption or icon: the caption with the icon goes above the text, as Vaadin 8 rendered it
			Span infrastructureDataLockedCaption =
				new Span(VaadinIcon.WARNING.create(), new Span(I18nProperties.getString(Strings.headingInfrastructureLocked)));
			infrastructureDataLockedCaption.addClassName(CssStyles.VAADIN_CAPTION);
			Div infrastructureDataLocked =
				new Div(infrastructureDataLockedCaption, new Div(new Span(I18nProperties.getString(Strings.messageInfrastructureLocked))));
			addHeaderComponent(infrastructureDataLocked);
		}

		if (UiUtil.permitted(UserRight.INFRASTRUCTURE_EXPORT)) {
			Button btnExport = ButtonHelper.createIconButton(Captions.export, VaadinIcon.TABLE, null, CssStyles.BUTTON_PRIMARY);
			btnExport.setTooltipText(I18nProperties.getDescription(Descriptions.descExportButton));
			addHeaderComponent(btnExport);

			DownloadHandler streamResource = GridExportStreamResource.createStreamResource(grid, ExportEntityName.AREAS, AreasGrid.ACTION_BTN_ID);
			DownloadUtil.attachDownloader(btnExport, streamResource);
		}

		if (UiUtil.permitted(infrastructureDataEditable, UserRight.INFRASTRUCTURE_CREATE)) {
			btnCreate = ButtonHelper.createIconButton(
				Captions.actionNewEntry,
				VaadinIcon.PLUS_CIRCLE,
				e -> ControllerProvider.getInfrastructureController().createArea(),
				CssStyles.BUTTON_PRIMARY);

			addHeaderComponent(btnCreate);
		}

		if (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS)) {
			Button btnEnterBulkEditMode = ButtonHelper.createIconButton(Captions.actionEnterBulkEditMode, VaadinIcon.CHECK_SQUARE_O, null);
			btnEnterBulkEditMode.setVisible(!viewConfiguration.isInEagerMode());
			addHeaderComponent(btnEnterBulkEditMode);

			Button btnLeaveBulkEditMode =
				ButtonHelper.createIconButton(Captions.actionLeaveBulkEditMode, VaadinIcon.CLOSE, null, CssStyles.BUTTON_PRIMARY);
			btnLeaveBulkEditMode.setVisible(viewConfiguration.isInEagerMode());
			addHeaderComponent(btnLeaveBulkEditMode);

			btnEnterBulkEditMode.addClickListener(e -> {
				viewConfiguration.setInEagerMode(true);
				dropdownBulkOperations.setVisible(isBulkOperationsDropdownVisible());
				btnEnterBulkEditMode.setVisible(false);
				btnLeaveBulkEditMode.setVisible(true);
				searchField.setEnabled(false);
				grid.setEagerDataProvider();
				grid.reload();
			});
			btnLeaveBulkEditMode.addClickListener(e -> {
				viewConfiguration.setInEagerMode(false);
				dropdownBulkOperations.setVisible(false);
				btnLeaveBulkEditMode.setVisible(false);
				btnEnterBulkEditMode.setVisible(true);
				searchField.setEnabled(true);
				navigateTo(criteria);
			});
		}

		addComponent(gridLayout);
	}

	private HorizontalLayout createFilterBar() {
		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.setPadding(false);
		filterLayout.setSpacing(true);
		filterLayout.setWidth(100, Unit.PERCENTAGE);

		searchField = new SearchField();
		searchField.addValueChangeListener(e -> {
			// was a V7 text change listener: user input only
			if (!e.isFromClient()) {
				return;
			}
			criteria.textFilter(e.getValue());
			grid.reload();
		});
		filterLayout.add(searchField);

		btnResetFilters = ButtonHelper.createButton(Captions.actionResetFilters, event -> {
			ViewModelProviders.of(AreasView.class).remove(AreaCriteria.class);
			navigateTo(null);
		}, CssStyles.FORCE_CAPTION);
		btnResetFilters.setVisible(false);
		filterLayout.add(btnResetFilters);

		HorizontalLayout actionButtonsLayout = new HorizontalLayout();
		actionButtonsLayout.setSpacing(true);
		{
			// Show active/archived/all dropdown
			if (UiUtil.permitted(UserRight.INFRASTRUCTURE_VIEW)) {
				filterRelevanceStatus = new ComboBox<>();
				filterRelevanceStatus.setId("relevanceStatus");
				filterRelevanceStatus.setWidth(220, Unit.PERCENTAGE);
				filterRelevanceStatus.setItems(EntityRelevanceStatus.getAllExceptDeleted());
				filterRelevanceStatus.setItemLabelGenerator(status -> {
					switch (status) {
					case ACTIVE:
						return I18nProperties.getCaption(Captions.areaActiveAreas);
					case ARCHIVED:
						return I18nProperties.getCaption(Captions.areaArchivedAreas);
					default:
						return I18nProperties.getCaption(Captions.areaAllAreas);
					}
				});
				filterRelevanceStatus.addValueChangeListener(e -> {
					if (e.isFromClient() && e.getValue() == null) {
						// was setEmptySelectionAllowed(false): a Flow combo box has no empty item, but the user can clear it;
						// the previous value comes back without a new navigation
						applyingCriteria = true;
						filterRelevanceStatus.setValue(e.getOldValue());
						applyingCriteria = false;
						return;
					}
					criteria.relevanceStatus(e.getValue());
					navigateTo(criteria);
				});
				actionButtonsLayout.add(filterRelevanceStatus);

				// Bulk operation dropdown
				if (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS)) {
					dropdownBulkOperations = MenuBarHelper.createDropDown(
						Captions.bulkActions,
						new MenuBarHelper.MenuBarItem(
							I18nProperties.getCaption(Captions.actionArchiveInfrastructure),
							VaadinIcon.ARCHIVE,
							selectedItem -> ControllerProvider.getInfrastructureController()
								.archiveOrDearchiveAllSelectedItems(
									true,
									ArchiveHandlers.forInfrastructure(FacadeProvider.getAreaFacade(), ArchiveMessages.AREA),
									grid,
									grid::reload,
									() -> navigateTo(criteria)),
							UiUtil.permitted(UserRight.INFRASTRUCTURE_ARCHIVE) && EntityRelevanceStatus.ACTIVE.equals(criteria.getRelevanceStatus())),
						new MenuBarHelper.MenuBarItem(
							I18nProperties.getCaption(Captions.actionDearchiveInfrastructure),
							VaadinIcon.ARCHIVE,
							selectedItem -> ControllerProvider.getInfrastructureController()
								.archiveOrDearchiveAllSelectedItems(
									false,
									ArchiveHandlers.forInfrastructure(FacadeProvider.getAreaFacade(), ArchiveMessages.AREA),
									grid,
									grid::reload,
									() -> navigateTo(criteria)),
							UiUtil.permitted(UserRight.INFRASTRUCTURE_ARCHIVE)
								&& EntityRelevanceStatus.ARCHIVED.equals(criteria.getRelevanceStatus())));

					dropdownBulkOperations.setVisible(isBulkOperationsDropdownVisible());
					actionButtonsLayout.add(dropdownBulkOperations);
				}
			}
		}
		filterLayout.add(actionButtonsLayout);
		// was Alignment.BOTTOM_RIGHT: the layout grows, its content goes right
		filterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, actionButtonsLayout);
		actionButtonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		filterLayout.setFlexGrow(1, actionButtonsLayout);

		return filterLayout;
	}

	@Override
	public void enter(ViewChangeEvent event) {
		super.enter(event);
		String params = event.getParameters().trim();
		if (params.startsWith("?")) {
			params = params.substring(1);
			criteria.fromUrlParams(params);
		}
		updateFilterComponents();
		grid.reload();
	}

	public void updateFilterComponents() {
		applyingCriteria = true;

		btnResetFilters.setVisible(criteria.hasAnyFilterActive());

		if (filterRelevanceStatus != null) {
			filterRelevanceStatus.setValue(criteria.getRelevanceStatus());
		}
		searchField.setValue(criteria.getTextFilter());

		applyingCriteria = false;
	}

	private boolean isBulkOperationsDropdownVisible() {
		boolean infrastructureDataEditable = UiUtil.enabled(FeatureType.EDIT_INFRASTRUCTURE_DATA);

		return viewConfiguration.isInEagerMode()
			&& (EntityRelevanceStatus.ACTIVE.equals(criteria.getRelevanceStatus())
				|| (infrastructureDataEditable && EntityRelevanceStatus.ARCHIVED.equals(criteria.getRelevanceStatus())));
	}

}
