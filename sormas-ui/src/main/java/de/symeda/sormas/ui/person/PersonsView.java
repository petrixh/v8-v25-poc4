package de.symeda.sormas.ui.person;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.person.PersonAssociation;
import de.symeda.sormas.api.person.PersonCriteria;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.person.PersonIndexDto;
import de.symeda.sormas.api.person.PersonSimilarityCriteria;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.navigation.ViewChangeListener;
import de.symeda.sormas.ui.utils.AbstractView;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.ExportEntityName;
import de.symeda.sormas.ui.utils.FilteredGrid;
import de.symeda.sormas.ui.utils.GridExportStreamResource;
import de.symeda.sormas.ui.utils.MenuBarHelper;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.PersonDownloadUtil;
import de.symeda.sormas.ui.utils.PopupButton;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.ViewConfiguration;
import de.symeda.sormas.ui.utils.components.popupmenu.PopupMenu;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

public class PersonsView extends AbstractView {

	private static final long serialVersionUID = -6292580716619536538L;

	public static final String VIEW_NAME = "persons";

	private final PersonCriteria criteria;
	private final FilteredGrid<?, PersonCriteria> grid;
	private Span noAccessLabel;
	private LinkedHashMap<Button, PersonAssociation> associationButtons;
	private Button activeAssociationButton;
	private PersonFilterForm filterForm;

	private ViewConfiguration viewConfiguration;

	// Bulk operations
	private MenuBar bulkOperationsDropdown;

	public PersonsView() {
		super(VIEW_NAME);

		viewConfiguration = ViewModelProviders.of(PersonsView.class).get(ViewConfiguration.class);
		// Avoid calling ALL associations at view start because the query tends to take long time
		final VerticalLayout gridLayout = new VerticalLayout();
		gridLayout.add(createFilterBar());
		final HorizontalLayout associationFilterBar = createAssociationFilterBar();
		gridLayout.add(associationFilterBar);
		gridLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, associationFilterBar);

		PersonAssociation defaultAssociation = getFirstAllowedPersonAssociation();
		if (defaultAssociation != null) {
			PersonCriteria defaultCriteria = new PersonCriteria().personAssociation(defaultAssociation);
			criteria = ViewModelProviders.of(PersonsView.class).get(PersonCriteria.class, defaultCriteria);
			grid = new PersonGrid(criteria);

			gridLayout.add(grid);
			// was setMargin(true): the Vaadin 8 margin is the padding in Flow
			gridLayout.setPadding(true);
			gridLayout.setSpacing(false);
			gridLayout.setSizeFull();
			gridLayout.setFlexGrow(1, grid);
			gridLayout.setClassName("crud-main-layout");

			if (UiUtil.permitted(UserRight.PERSON_EXPORT)) {
				VerticalLayout exportLayout = new VerticalLayout();
				exportLayout.setSpacing(true);
				exportLayout.setPadding(true);
				exportLayout.addClassName(CssStyles.LAYOUT_MINIMAL);
				exportLayout.setWidth(200, Unit.PIXELS);

				PopupButton exportButton = ButtonHelper.createIconPopupButton(Captions.export, VaadinIcon.DOWNLOAD, exportLayout);
				addHeaderComponent(exportButton);

				Button basicExportButton = ButtonHelper.createIconButton(Captions.exportBasic, VaadinIcon.TABLE, null, CssStyles.BUTTON_PRIMARY);
				basicExportButton.setTooltipText(I18nProperties.getString(Strings.infoBasicExport));
				basicExportButton.setWidth(100, Unit.PERCENTAGE);
				exportLayout.add(basicExportButton);
				DownloadHandler streamResource =
					GridExportStreamResource.createStreamResourceWithSelectedItems(grid, Collections::emptySet, ExportEntityName.PERSONS);
				// was a FileDownloader extending the button
				DownloadUtil.attachDownloader(basicExportButton, streamResource);

				DownloadHandler extendedExportStreamResource = PersonDownloadUtil.createPersonExportResource(grid.getCriteria(), null);
				addExportButton(
					extendedExportStreamResource,
					exportButton.getPopover(),
					exportLayout,
					VaadinIcon.FILE_TEXT,
					Captions.exportDetailed,
					Strings.infoDetailedExport);

				Button btnCustomExport = ButtonHelper.createIconButton(Captions.exportCustom, VaadinIcon.FILE_TEXT, e -> {
					ControllerProvider.getCustomExportController().openPersonExportWindow(grid.getCriteria());
					exportButton.setPopupVisible(false);
				}, CssStyles.BUTTON_PRIMARY);
				btnCustomExport.setTooltipText(I18nProperties.getString(Strings.infoCustomExport));
				btnCustomExport.setWidth(100, Unit.PERCENTAGE);
				exportLayout.add(btnCustomExport);
			}

			// TODO: add option to re-enable bulk geocoord updates
			/* Disabled until option exists to togggle on/off this behaviour
			if (FacadeProvider.getGeocodingFacade().isEnabled()) {
				Button setMissingCoordinatesButton = ButtonHelper.createIconButton(
					I18nProperties.getCaption(Captions.personsSetMissingGeoCoordinates),
					VaadinIcons.MAP_MARKER,
					e -> showMissingCoordinatesPopUp(),
					ValoTheme.BUTTON_PRIMARY);
				addHeaderComponent(setMissingCoordinatesButton);
			}
			*/

			grid.addDataSizeChangeListener(e -> updateAssociationButtons());
		} else {
			criteria = new PersonCriteria();
			grid = null;
			associationFilterBar.setVisible(false);
			noAccessLabel = VaadinUiUtil.createLabel(
				HtmlRenderer.iconHtml(VaadinIcon.INFO_CIRCLE) + " " + I18nProperties.getString(Strings.infoNoAccessToPersonEntities),
				ContentMode.HTML);
			gridLayout.add(noAccessLabel);
		}

		addComponent(gridLayout);

		final PopupMenu moreButton = new PopupMenu(I18nProperties.getCaption(Captions.moreActions));

		if (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS)) {
			Button btnEnterBulkEditMode = ButtonHelper.createIconButton(Captions.actionEnterBulkEditMode, VaadinIcon.CHECK_SQUARE_O, null);
			btnEnterBulkEditMode.setVisible(!viewConfiguration.isInEagerMode());
			moreButton.addMenuEntry(btnEnterBulkEditMode);

			Button btnLeaveBulkEditMode =
				ButtonHelper.createIconButton(Captions.actionLeaveBulkEditMode, VaadinIcon.CLOSE, null, CssStyles.BUTTON_PRIMARY);
			btnLeaveBulkEditMode.setVisible(viewConfiguration.isInEagerMode());
			moreButton.addMenuEntry(btnLeaveBulkEditMode);

			btnEnterBulkEditMode.addClickListener(e -> {
				bulkOperationsDropdown.setVisible(true);
				ViewModelProviders.of(PersonsView.class).get(ViewConfiguration.class).setInEagerMode(true);
				btnEnterBulkEditMode.setVisible(false);
				btnLeaveBulkEditMode.setVisible(true);
				((PersonGrid) grid).reload();
				((PersonGrid) grid).setBulkEditMode(true);
			});
			btnLeaveBulkEditMode.addClickListener(e -> {
				bulkOperationsDropdown.setVisible(false);
				ViewModelProviders.of(PersonsView.class).get(ViewConfiguration.class).setInEagerMode(false);
				btnLeaveBulkEditMode.setVisible(false);
				btnEnterBulkEditMode.setVisible(true);
				navigateTo(criteria);
				((PersonGrid) grid).setBulkEditMode(false);
			});
		}

		if (moreButton.hasMenuEntries()) {
			addHeaderComponent(moreButton);
		}
	}

	private void showMissingCoordinatesPopUp() {

		Span popupDescLabel = new Span(I18nProperties.getString(Strings.confirmationSetMissingGeoCoordinates));
		Checkbox popupCheckbox = new Checkbox(I18nProperties.getCaption(Captions.personsReplaceGeoCoordinates));
		popupCheckbox.setValue(false);

		VerticalLayout popupLayout = new VerticalLayout();
		popupLayout.setPadding(false);
		popupLayout.setSpacing(true);
		popupDescLabel.setWidth(100, Unit.PERCENTAGE);
		popupCheckbox.setWidth(100, Unit.PERCENTAGE);
		popupLayout.add(popupDescLabel);
		popupLayout.add(popupCheckbox);

		VaadinUiUtil.showConfirmationPopup(
			I18nProperties.getCaption(Captions.personsSetMissingGeoCoordinates),
			popupLayout,
			I18nProperties.getCaption(Captions.actionContinue),
			I18nProperties.getCaption(Captions.actionCancel),
			640,
			confirmed -> {
				if (Boolean.TRUE.equals(confirmed)) {
					long changedPersons = FacadeProvider.getPersonFacade().setMissingGeoCoordinates(popupCheckbox.getValue());
					NotificationHelper.show(
						I18nProperties.getCaption(Captions.personsUpdated),
						String.format(I18nProperties.getString(Strings.notificationPersonsUpdated), changedPersons),
						NotificationHelper.Type.TRAY_NOTIFICATION);
				}
			});
	}

	@Override
	public void enter(ViewChangeListener.ViewChangeEvent event) {
		String params = event.getParameters().trim();
		if (params.startsWith("?")) {
			params = params.substring(1);
			criteria.fromUrlParams(params);
		}

		if (viewConfiguration.isInEagerMode()) {
			((PersonGrid) grid).setBulkEditMode(true);
		}

		updateFilterComponents();
	}

	public void updateFilterComponents() {
		// TODO replace with Vaadin 8 databinding
		applyingCriteria = true;

		if (criteria.getPersonAssociation() != null && !associationButtons.values().contains(criteria.getPersonAssociation())) {
			PersonAssociation firstAllowedAssociation = getFirstAllowedPersonAssociation();
			// The following line is needed because we want to correct the value in PersonCriteria in order to have a consistent state; setting the default association is 
			// necessary because calling PersonCriteria.setPersonAssociation with null throws an exception.
			criteria.setPersonAssociation(firstAllowedAssociation != null ? firstAllowedAssociation : PersonCriteria.DEFAULT_ASSOCIATION);
		}

		updateAssociationButtons();

		filterForm.setValue(criteria);

		applyingCriteria = false;
	}

	private void updateAssociationButtons() {

		associationButtons.keySet().forEach(b -> {
			CssStyles.style(b, CssStyles.BUTTON_FILTER_LIGHT);
			b.setText(associationButtons.get(b).toString());
			b.setSuffixComponent(null);
			if (ComponentUtil.getData(b, PersonAssociation.class) == criteria.getPersonAssociation()) {
				activeAssociationButton = b;
			}
		});
		if (activeAssociationButton != null && grid != null) {
			CssStyles.removeStyles(activeAssociationButton, CssStyles.BUTTON_FILTER_LIGHT);
			// was an HTML caption with LayoutUtil.spanCss(BADGE, count): the count is a badge after the text
			activeAssociationButton.setText(associationButtons.get(activeAssociationButton).toString());
			Span badge = new Span(String.valueOf(grid.getDataSize()));
			badge.addClassName(CssStyles.BADGE);
			activeAssociationButton.setSuffixComponent(badge);
		}
	}

	public VerticalLayout createFilterBar() {
		VerticalLayout filterLayout = new VerticalLayout();
		filterLayout.setSpacing(false);
		filterLayout.setPadding(false);
		filterLayout.setWidth(100, Unit.PERCENTAGE);

		filterForm = new PersonFilterForm();
		filterForm.addValueChangeListener(e -> {
			if (!filterForm.hasFilter()) {
				navigateTo(null);
			}
		});
		filterForm.addResetHandler(e -> {
			ViewModelProviders.of(PersonsView.class).remove(PersonCriteria.class);
			navigateTo(null, true);
		});
		filterForm.addApplyHandler(e -> {
			if (grid != null) {
				((PersonGrid) grid).reload();
			}
		});
		filterLayout.add(filterForm);

		return filterLayout;
	}

	public HorizontalLayout createAssociationFilterBar() {

		HorizontalLayout associationFilterLayout = new HorizontalLayout();
		associationFilterLayout.setSpacing(true);
		// was MarginInfo(true, false, false, false), a top margin only (look, ignored at this stage)
		associationFilterLayout.setPadding(false);
		associationFilterLayout.setWidth(100, Unit.PERCENTAGE);
		associationFilterLayout.addClassName(CssStyles.VSPACE_3);

		associationButtons = new LinkedHashMap<>();
		for (PersonAssociation association : FacadeProvider.getPersonFacade().getPermittedAssociations()) {

			Button associationButton = ButtonHelper.createButton(association.toString(), e -> {
				if (!UiUtil.hasNationJurisdictionLevel() && association == PersonAssociation.ALL) {
					Span contentLabel = new Span(I18nProperties.getString(Strings.confirmationSeeAllPersons));
					VaadinUiUtil.showConfirmationPopup(
						I18nProperties.getString(Strings.headingSeeAllPersons),
						contentLabel,
						I18nProperties.getString(Strings.yes),
						I18nProperties.getString(Strings.no),
						640,
						ee -> {
							if (Boolean.TRUE.equals(ee)) {
								criteria.personAssociation(association);
								navigateTo(criteria);
							}
						});
				} else {
					criteria.personAssociation(association);
					navigateTo(criteria);
				}
			}, CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER, CssStyles.BUTTON_FILTER_LIGHT);
			ComponentUtil.setData(associationButton, PersonAssociation.class, association);

			associationFilterLayout.add(associationButton);
			associationFilterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, associationButton);
			associationButtons.put(associationButton, association);
		}

		Span emptyLabel = new Span("");
		associationFilterLayout.add(emptyLabel);
		associationFilterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, emptyLabel);
		associationFilterLayout.setFlexGrow(1, emptyLabel);

		// Bulk operation dropdown
		if (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS)) {
			List<MenuBarHelper.MenuBarItem> bulkActions = new ArrayList<>();
			if (UiUtil.permitted(UserRight.PERSON_MERGE)) {
				bulkActions.add(
					new MenuBarHelper.MenuBarItem(
						I18nProperties.getCaption(Captions.actionMerge),
						VaadinIcon.COMPRESS_SQUARE,
						mi -> grid.bulkActionHandler(items -> {
							if (items.size() != 2) {
								VaadinUiUtil.showWarningPopup(I18nProperties.getString(Strings.messageCannotMergeMoreThanTwoPersons));
							} else {

								Iterator selectionsIterator = items.iterator();
								final PersonIndexDto person1 = (PersonIndexDto) selectionsIterator.next();
								final PersonIndexDto person2 = (PersonIndexDto) selectionsIterator.next();

								final PersonDto leadPersonDto = FacadeProvider.getPersonFacade().getByUuid(person1.getUuid());
								final PersonSimilarityCriteria criteria = new PersonSimilarityCriteria().sex(leadPersonDto.getSex())
									.nationalHealthId(leadPersonDto.getNationalHealthId())
									.passportNumber(leadPersonDto.getPassportNumber())
									.birthdateDD(leadPersonDto.getBirthdateDD())
									.birthdateMM(leadPersonDto.getBirthdateMM())
									.birthdateYYYY(leadPersonDto.getBirthdateYYYY());
								criteria.setName(leadPersonDto);

								if (!FacadeProvider.getPersonFacade().isPersonSimilar(criteria, person2.getUuid())) {
									VaadinUiUtil.showConfirmationPopup(
										I18nProperties.getString(Strings.headingPickOrMergePersonConfirmation),
										new Span(I18nProperties.getString(Strings.infoPersonMergeConfirmationForNonSimilarPersons)),
										I18nProperties.getCaption(Captions.actionProceed),
										I18nProperties.getCaption(Captions.actionCancel),
										800,
										confirmAgain -> {
											if (Boolean.TRUE.equals(confirmAgain)) {
												ControllerProvider.getPersonController().mergePersons(person1, person2);
											}
										});
								} else {
									ControllerProvider.getPersonController().mergePersons(person1, person2);
								}
							}
							grid.deselectAll();
						}, true)));
			} ;

			bulkOperationsDropdown = MenuBarHelper.createDropDown(Captions.bulkActions, bulkActions);
			bulkOperationsDropdown.setVisible(viewConfiguration.isInEagerMode());
			associationFilterLayout.add(bulkOperationsDropdown);
		}

		return associationFilterLayout;
	}

	private PersonAssociation getFirstAllowedPersonAssociation() {

		Iterator<PersonAssociation> associationsIterator = associationButtons.values().iterator();
		PersonAssociation defaultAssociation = associationsIterator.hasNext() ? associationsIterator.next() : null;
		if (defaultAssociation == PersonAssociation.ALL) {
			defaultAssociation = associationsIterator.hasNext() ? associationsIterator.next() : null;
		}
		return defaultAssociation;
	}
}
