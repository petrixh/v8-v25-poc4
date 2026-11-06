/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2024 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.ui.adverseeventsfollowingimmunization;

import java.util.Collections;
import java.util.Objects;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.adverseeventsfollowingimmunization.AefiInvestigationCriteria;
import de.symeda.sormas.api.common.DeletableEntityType;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.feature.FeatureTypeProperty;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.UserProvider;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.adverseeventsfollowingimmunization.components.directory.AefiInvestigationDataLayout;
import de.symeda.sormas.ui.adverseeventsfollowingimmunization.components.directory.AefiInvestigationFilterFormLayout;
import de.symeda.sormas.ui.navigation.ViewChangeListener;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ComboBoxHelper;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.ExportEntityName;
import de.symeda.sormas.ui.utils.GridExportStreamResource;
import de.symeda.sormas.ui.utils.PopupButton;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.ViewConfiguration;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

public class AefiInvestigationView extends AbstractAefiView {

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/investigations";

	private final AefiInvestigationCriteria criteria;

	private AefiInvestigationFilterFormLayout filterFormLayout;
	private final AefiInvestigationDataLayout dataLayout;

	// Filters
	private Span relevanceStatusInfoLabel;
	private ComboBox relevanceStatusFilter;
	private ViewConfiguration viewConfiguration;

	public AefiInvestigationView() {
		super(VIEW_NAME);

		CssStyles.style(getViewTitleLabel(), CssStyles.PAGE_TITLE);

		viewConfiguration = ViewModelProviders.of(getClass()).get(ViewConfiguration.class);

		criteria = ViewModelProviders.of(AefiInvestigationView.class).get(AefiInvestigationCriteria.class);
		if (criteria.getRelevanceStatus() == null) {
			criteria.setRelevanceStatus(EntityRelevanceStatus.ACTIVE);
		}
		dataLayout = new AefiInvestigationDataLayout(criteria);

		if (UserProvider.getCurrent().hasUserRight(UserRight.ADVERSE_EVENTS_FOLLOWING_IMMUNIZATION_EXPORT)) {
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
			DownloadHandler streamResource = GridExportStreamResource.createStreamResourceWithSelectedItems(
				dataLayout.getGrid(),
				() -> viewConfiguration.isInEagerMode() ? dataLayout.getGrid().asMultiSelect().getSelectedItems() : Collections.emptySet(),
				ExportEntityName.ADVERSE_EVENTS_FOLLOWING_IMMUNIZATION_INVESTIGATION);
			DownloadUtil.attachDownloader(basicExportButton, streamResource);
		}

		final VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.add(createFilterBar());

		final VerticalLayout gridLayout = new VerticalLayout();
		gridLayout.setPadding(false);
		gridLayout.setSpacing(false);
		gridLayout.setSizeFull();
		//CssStyles.style(gridLayout, CssStyles.VIEW_SECTION, CssStyles.VSPACE_TOP_3);

		gridLayout.add(createStatusFilterBar());
		gridLayout.add(dataLayout);
		gridLayout.setFlexGrow(1, dataLayout);

		mainLayout.add(gridLayout);

		// was margins without the top one (MarginInfo); spacing details belong to the theme work
		mainLayout.setPadding(true);
		mainLayout.setSpacing(false);
		mainLayout.setSizeFull();
		mainLayout.setFlexGrow(1, gridLayout);
		mainLayout.addClassNames("crud-main-layout", CssStyles.VSPACE_TOP_4);

		addComponent(mainLayout);
	}

	private void updateFilterComponents() {
		// TODO replace with Vaadin 8 databinding
		applyingCriteria = true;

		if (relevanceStatusFilter != null) {
			relevanceStatusFilter.setValue(criteria.getRelevanceStatus());
		}

		filterFormLayout.setValue(criteria);

		applyingCriteria = false;
	}

	private AefiInvestigationFilterFormLayout createFilterBar() {
		filterFormLayout = new AefiInvestigationFilterFormLayout();

		filterFormLayout.addResetHandler(clickEvent -> {
			ViewModelProviders.of(AefiInvestigationView.class).remove(AefiInvestigationCriteria.class);
			navigateTo(null, true);
		});

		filterFormLayout.addApplyHandler(clickEvent -> {
			dataLayout.refreshGrid();
		});

		return filterFormLayout;
	}

	public HorizontalLayout createStatusFilterBar() {
		HorizontalLayout statusFilterLayout = new HorizontalLayout();
		statusFilterLayout.setSpacing(true);
		statusFilterLayout.setPadding(false);
		statusFilterLayout.setWidth(100, Unit.PERCENTAGE);
		statusFilterLayout.addClassName(CssStyles.VSPACE_3);

		HorizontalLayout actionButtonsLayout = new HorizontalLayout();
		actionButtonsLayout.setSpacing(true);

		// Show active/archived/all dropdown
		if (Objects.nonNull(UserProvider.getCurrent())
			&& UserProvider.getCurrent().hasUserRight(UserRight.ADVERSE_EVENTS_FOLLOWING_IMMUNIZATION_VIEW)) {

			if (FacadeProvider.getFeatureConfigurationFacade()
				.isFeatureEnabled(FeatureType.AUTOMATIC_ARCHIVING, DeletableEntityType.ADVERSE_EVENTS_FOLLOWING_IMMUNIZATION)) {

				int daysAfterAefiEntryGetsArchived = FacadeProvider.getFeatureConfigurationFacade()
					.getProperty(
						FeatureType.AUTOMATIC_ARCHIVING,
						DeletableEntityType.ADVERSE_EVENTS_FOLLOWING_IMMUNIZATION,
						FeatureTypeProperty.THRESHOLD_IN_DAYS,
						Integer.class);
				if (daysAfterAefiEntryGetsArchived > 0) {
					relevanceStatusInfoLabel = VaadinUiUtil.createLabel(
						HtmlRenderer.iconHtml(VaadinIcon.INFO_CIRCLE) + " "
							+ String.format(I18nProperties.getString(Strings.infoArchivedAefiEntries), daysAfterAefiEntryGetsArchived),
						ContentMode.HTML);
					relevanceStatusInfoLabel.setVisible(false);
					relevanceStatusInfoLabel.addClassName(CssStyles.LABEL_VERTICAL_ALIGN_SUPER);
					actionButtonsLayout.add(relevanceStatusInfoLabel);
					actionButtonsLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, relevanceStatusInfoLabel);
				}
			}
			relevanceStatusFilter = ComboBoxHelper.createComboBoxV7();
			relevanceStatusFilter.setId("relevanceStatus");
			relevanceStatusFilter.setWidth(260, Unit.PIXELS);
			relevanceStatusFilter.setNullSelectionAllowed(false);
			relevanceStatusFilter.setTextInputAllowed(false);
			relevanceStatusFilter.addItems((Object[]) EntityRelevanceStatus.values());
			relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ACTIVE, I18nProperties.getCaption(Captions.aefiActiveInvestigations));
			relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ARCHIVED, I18nProperties.getCaption(Captions.aefiArchivedInvestigations));
			relevanceStatusFilter.setItemCaption(
				EntityRelevanceStatus.ACTIVE_AND_ARCHIVED,
				I18nProperties.getCaption(Captions.aefiAllActiveAndArchivedInvestigations));
			relevanceStatusFilter.setCaption(null);
			relevanceStatusFilter.addClassName(CssStyles.VSPACE_NONE);

			if (UserProvider.getCurrent().hasUserRight(UserRight.IMMUNIZATION_DELETE)) {
				relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.DELETED, I18nProperties.getCaption(Captions.aefiDeletedInvestigations));
			} else {
				relevanceStatusFilter.removeItem(EntityRelevanceStatus.DELETED);
			}

			relevanceStatusFilter.addValueChangeListener(e -> {
				if (relevanceStatusInfoLabel != null) {
					relevanceStatusInfoLabel.setVisible(EntityRelevanceStatus.ARCHIVED.equals(e.getValue()));
				}
				criteria.setRelevanceStatus((EntityRelevanceStatus) e.getValue());
				navigateTo(criteria);
			});
			actionButtonsLayout.add(relevanceStatusFilter);
		}

		if (actionButtonsLayout.getComponentCount() > 0) {
			statusFilterLayout.add(actionButtonsLayout);
			// was Alignment.TOP_RIGHT: the layout grows, its content goes right
			statusFilterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, actionButtonsLayout);
			statusFilterLayout.setFlexGrow(1, actionButtonsLayout);
			actionButtonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		}

		return statusFilterLayout;
	}

	@Override
	public void enter(ViewChangeListener.ViewChangeEvent event) {

		String params = event.getParameters().trim();
		if (params.startsWith("?")) {
			params = params.substring(1);
			criteria.fromUrlParams(params);
		}
		updateFilterComponents();

		super.enter(event);
	}
}
