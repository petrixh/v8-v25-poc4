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
package de.symeda.sormas.ui.contact;

import java.util.Date;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.contact.ContactDto;
import de.symeda.sormas.api.contact.ContactStatus;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.symptoms.SymptomsDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.visit.VisitCriteria;
import de.symeda.sormas.api.visit.VisitDto;
import de.symeda.sormas.api.visit.VisitExportDto;
import de.symeda.sormas.api.visit.VisitExportType;
import de.symeda.sormas.api.visit.VisitIndexDto;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.DetailSubComponentWrapper;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.ExportEntityName;
import de.symeda.sormas.ui.utils.MenuBarHelper;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;
import de.symeda.sormas.ui.visit.VisitGrid;

public class ContactVisitsView extends AbstractContactView {

	private static final long serialVersionUID = -1L;

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/visits";

	private VisitCriteria criteria;

	private VisitGrid grid;
	private Button newButton;
	private DetailSubComponentWrapper gridLayout;
//	private HashMap<Button, String> statusButtons;
//	private Button activeStatusButton;

	public ContactVisitsView() {
		super(VIEW_NAME);

		setSizeFull();

		criteria = ViewModelProviders.of(ContactVisitsView.class).get(VisitCriteria.class);
	}

	public HorizontalLayout createTopBar() {
		HorizontalLayout topLayout = new HorizontalLayout();
		topLayout.setSpacing(true);
		topLayout.setWidth(100, Unit.PERCENTAGE);
		topLayout.addClassName(CssStyles.VSPACE_3);

		if (isEditAllowed()) {
			if (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS, UserRight.CONTACT_EDIT, UserRight.VISIT_EDIT, UserRight.VISIT_DELETE)) {
				topLayout.setWidth(100, Unit.PERCENTAGE);

				MenuBar bulkOperationsDropdown = MenuBarHelper.createDropDown(
					Captions.bulkActions,
					new MenuBarHelper.MenuBarItem(I18nProperties.getCaption(Captions.bulkDelete), VaadinIcon.TRASH, selectedItem -> {
						ControllerProvider.getVisitController()
							.deleteAllSelectedItems(grid.asMultiSelect().getSelectedItems(), grid, () -> navigateTo(criteria));
					}));

				topLayout.add(bulkOperationsDropdown);
				topLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, bulkOperationsDropdown);
				topLayout.setFlexGrow(1, bulkOperationsDropdown);
				// was expand ratio 1 and Alignment.MIDDLE_RIGHT: the dropdown takes the free width, its button sits at the right end
				bulkOperationsDropdown.addThemeVariants(MenuBarVariant.LUMO_END_ALIGNED);
			}

			if (UiUtil.permitted(UserRight.VISIT_EXPORT)) {
				Button exportButton = ButtonHelper.createIconButton(Captions.export, VaadinIcon.DOWNLOAD, null, CssStyles.BUTTON_PRIMARY);
				final boolean exportFirst;
				{
					topLayout.add(exportButton);
					topLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, exportButton);
					// was expand ratio 1 and Alignment.MIDDLE_RIGHT when it is the first component: its slot took the free width, the
					// button kept its own width at the slot's right end (set on the download link below, which takes the button's place)
					exportFirst = topLayout.getComponentCount() == 1;
				}

				DownloadHandler exportStreamResource = DownloadUtil.createCsvExportStreamResource(
					VisitExportDto.class,
					VisitExportType.CONTACT_VISITS,
					(Integer start, Integer max) -> FacadeProvider.getVisitFacade()
						.getVisitsExportList(
							grid.getCriteria(),
							grid.getSelectionModel() instanceof GridMultiSelectionModel
								? grid.asMultiSelect().getSelectedItems().stream().map(VisitIndexDto::getUuid).collect(Collectors.toSet())
								: null,
							VisitExportType.CONTACT_VISITS,
							start,
							max,
							null),
					(propertyId, type) -> {
						String caption = findPrefixCaption(
							propertyId,
							VisitExportDto.I18N_PREFIX,
							VisitDto.I18N_PREFIX,
							PersonDto.I18N_PREFIX,
							SymptomsDto.I18N_PREFIX);
						if (Date.class.isAssignableFrom(type)) {
							caption += " (" + DateFormatHelper.getDateFormatPattern() + ")";
						}
						return caption;
					},
					ExportEntityName.CONTACT_VISITS,
					null);

				Anchor exportLink = DownloadUtil.attachDownloader(exportButton, exportStreamResource);
				if (exportFirst) {
					exportLink.getElement().getStyle().set("margin-left", "auto");
				}
			}

			if (UiUtil.permitted(UserRight.VISIT_CREATE, UserRight.CONTACT_EDIT)) {
				newButton = ButtonHelper.createIconButton(
					Captions.visitNewVisit,
					VaadinIcon.PLUS_CIRCLE,
					e -> ControllerProvider.getVisitController().createVisit(this.getContactRef(), r -> navigateTo(criteria)),
					CssStyles.BUTTON_PRIMARY);

				topLayout.add(newButton);
				topLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, newButton);

				final ContactDto contactDto = FacadeProvider.getContactFacade().getByUuid(this.getContactRef().getUuid());
				if (contactDto.getResultingCase() != null) {
					newButton.setEnabled(false);
					if (topLayout.getComponentCount() == 1) {
						// was expand ratio 1 and Alignment.MIDDLE_RIGHT: its slot took the free width, the button kept its own width at the
						// slot's right end
						newButton.getElement().getStyle().set("margin-left", "auto");
					}
					final Span label = VaadinUiUtil.createLabel(HtmlRenderer.iconHtml(VaadinIcon.INFO_CIRCLE), ContentMode.HTML);
					label.setTitle(I18nProperties.getString(Strings.infoContactAlreadyConvertedToCase));
					topLayout.add(label);
					topLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label);
				}
			}
		}

		return topLayout;
	}

	@Override
	protected void initView(String params) {

		// Hide the "New visit" button for converted contacts
		if (newButton != null
			&& FacadeProvider.getContactFacade().getByUuid(getContactRef().getUuid()).getContactStatus() == ContactStatus.CONVERTED) {
			newButton.setVisible(false);
		}

		criteria.contact(getContactRef());

		if (grid == null) {
			grid = new VisitGrid(
				criteria,
				UiUtil.permitted(isEditAllowed(), UserRight.CONTACT_EDIT, UserRight.VISIT_EDIT),
				UiUtil.permitted(isEditAllowed(), UserRight.VISIT_DELETE));

			gridLayout = new DetailSubComponentWrapper(() -> null);
			gridLayout.setSizeFull();
			gridLayout.setPadding(true);
			gridLayout.setSpacing(false);
			gridLayout.add(createTopBar());
			gridLayout.add(grid);
			gridLayout.setFlexGrow(1, grid);
			setSubComponent(gridLayout);
		}

		grid.reload();
	}
}
