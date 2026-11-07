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
package de.symeda.sormas.ui.caze;

import java.util.Date;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
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
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.DetailSubComponentWrapper;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.ExportEntityName;
import de.symeda.sormas.ui.utils.MenuBarHelper;
import de.symeda.sormas.ui.visit.VisitGrid;

public class CaseVisitsView extends AbstractCaseView {

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/visits";
	private static final long serialVersionUID = -4715387348091488461L;
	private VisitCriteria criteria;

	private VisitGrid grid;
	private Button newButton;
	private DetailSubComponentWrapper gridLayout;

	public CaseVisitsView() {
		super(VIEW_NAME, false);
		setSizeFull();

		criteria = ViewModelProviders.of(CaseVisitsView.class).get(VisitCriteria.class);
	}

	public HorizontalLayout createTopBar() {
		HorizontalLayout topLayout = new HorizontalLayout();
		topLayout.setSpacing(true);
		topLayout.setWidth(100, Unit.PERCENTAGE);
		topLayout.addClassName(CssStyles.VSPACE_3);

		if (isEditAllowed()) {
			if (UiUtil.permitted(UserRight.PERFORM_BULK_OPERATIONS, UserRight.CASE_EDIT, UserRight.VISIT_EDIT, UserRight.VISIT_DELETE)) {
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
				{
					topLayout.add(exportButton);
					topLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, exportButton);
					if (topLayout.getComponentCount() == 1) {
						topLayout.setFlexGrow(1, exportButton);
					}
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
					ExportEntityName.CASE_VISITS,
					null);

				DownloadUtil.attachDownloader(exportButton, exportStreamResource);
			}

			if (UiUtil.permitted(UserRight.VISIT_CREATE, UserRight.CASE_EDIT)) {
				newButton = ButtonHelper.createIconButton(Captions.visitNewVisit, VaadinIcon.PLUS_CIRCLE, e -> {
					ControllerProvider.getVisitController().createVisit(this.getCaseRef(), r -> navigateTo(criteria));
				}, CssStyles.BUTTON_PRIMARY);

				topLayout.add(newButton);
				topLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, newButton);
				newButton.setEnabled(isEditAllowed());
			}
		}

		return topLayout;
	}

	@Override
	protected void initView(String params) {
		criteria.caze(getCaseRef());

		if (grid == null) {
			grid = new VisitGrid(
				criteria,
				UiUtil.permitted(isEditAllowed(), UserRight.CASE_EDIT, UserRight.VISIT_EDIT),
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
