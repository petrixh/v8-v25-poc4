/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.campaign.campaigns;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.symeda.sormas.ui.utils.fields.TextField;

import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.campaign.CampaignCriteria;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.campaign.AbstractCampaignView;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ViewConfiguration;

@SuppressWarnings("serial")
public class CampaignsView extends AbstractCampaignView {

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/campaigns";

	private CampaignCriteria criteria;
	private VerticalLayout gridLayout;
	private CampaignGrid grid;
	private Button createButton;
	private Button validateFormsButton;

	// Filter
	private TextField searchField;
	private de.symeda.sormas.ui.utils.fields.ComboBox relevanceStatusFilter;

	public CampaignsView() {

		super(VIEW_NAME);

		ViewModelProviders.of(getClass()).get(ViewConfiguration.class);

		criteria = ViewModelProviders.of(CampaignsView.class).get(CampaignCriteria.class);
		if (criteria.getRelevanceStatus() == null) {
			criteria.relevanceStatus(EntityRelevanceStatus.ACTIVE);
		}

		grid = new CampaignGrid(criteria);
		gridLayout = new VerticalLayout();
		gridLayout.add(createFilterBar());
		gridLayout.add(grid);
		gridLayout.setPadding(true);
		gridLayout.setSpacing(false);
		gridLayout.setSizeFull();
		gridLayout.setFlexGrow(1, grid);
		gridLayout.setClassName("crud-main-layout");

		addComponent(gridLayout);

		if (UiUtil.permitted(UserRight.CAMPAIGN_EDIT)) {
			validateFormsButton = ButtonHelper.createIconButton(Captions.campaignValidateForms, VaadinIcon.CHECK_CIRCLE, e -> {
				FacadeProvider.getCampaignFormMetaFacade().validateAllFormMetas();
				NotificationHelper.show(I18nProperties.getString(Strings.messageAllCampaignFormsValid), Type.TRAY_NOTIFICATION);
			}, CssStyles.BUTTON_PRIMARY);

			addHeaderComponent(validateFormsButton);

			createButton = ButtonHelper.createIconButton(
				Captions.campaignNewCampaign,
				VaadinIcon.PLUS_CIRCLE,
				e -> ControllerProvider.getCampaignController().createOrEditCampaign(null),
				CssStyles.BUTTON_PRIMARY);

			addHeaderComponent(createButton);
		}
	}

	private HorizontalLayout createFilterBar() {

		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.setWidthFull();
		filterLayout.setSpacing(true);

		searchField = new TextField();
		searchField.setCaption(I18nProperties.getCaption(Captions.campaignSearch));
		searchField.setId("search");
		searchField.setWidth(200, Unit.PIXELS);
		searchField.setNullRepresentation("");
		searchField.setInputPrompt(I18nProperties.getString(Strings.promptCampaignSearch));
		searchField.setImmediate(true);
		searchField.addTextChangeListener(e -> {
			criteria.freeText(e.getText());
			grid.reload();
		});
		filterLayout.add(searchField);

		// Show active/archived/all dropdown
		relevanceStatusFilter = new de.symeda.sormas.ui.utils.fields.ComboBox();
		relevanceStatusFilter.setId("relevanceStatus");
		relevanceStatusFilter.setWidth(160, Unit.PIXELS);
		relevanceStatusFilter.setNullSelectionAllowed(false);
		relevanceStatusFilter.addItems((Object[]) EntityRelevanceStatus.values());
		relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ACTIVE, I18nProperties.getCaption(Captions.campaignActiveCampaigns));

		if (UiUtil.permitted(UserRight.CAMPAIGN_VIEW_ARCHIVED)) {
			relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.ARCHIVED, I18nProperties.getCaption(Captions.campaignArchivedCampaigns));
			relevanceStatusFilter
				.setItemCaption(EntityRelevanceStatus.ACTIVE_AND_ARCHIVED, I18nProperties.getCaption(Captions.campaignAllActiveAndArchivedCampaigns));
		} else {
			relevanceStatusFilter.removeItem(EntityRelevanceStatus.ARCHIVED);
			relevanceStatusFilter.removeItem(EntityRelevanceStatus.ACTIVE_AND_ARCHIVED);
		}

		if (UiUtil.permitted(UserRight.CAMPAIGN_DELETE)) {
			relevanceStatusFilter.setItemCaption(EntityRelevanceStatus.DELETED, I18nProperties.getCaption(Captions.campaignDeletedCampaigns));
		} else {
			relevanceStatusFilter.removeItem(EntityRelevanceStatus.DELETED);
		}

		relevanceStatusFilter.addValueChangeListener(e -> {
			criteria.relevanceStatus((EntityRelevanceStatus) e.getValue());
			navigateTo(criteria);
		});
		filterLayout.add(relevanceStatusFilter);
		filterLayout.setComponentAlignment(relevanceStatusFilter, Alignment.MIDDLE_RIGHT);
		filterLayout.setFlexGrow(1, relevanceStatusFilter);

		return filterLayout;
	}

	@Override
	public void enter(ViewChangeEvent event) {

		if (event != null) {
			String params = event.getParameters().trim();
			if (params.startsWith("?")) {
				params = params.substring(1);
				criteria.fromUrlParams(params);
			}
			updateFilterComponents();
		}
		grid.reload();

		super.enter(event);
	}

	private void updateFilterComponents() {

		applyingCriteria = true;

		if (relevanceStatusFilter != null) {
			relevanceStatusFilter.setValue(criteria.getRelevanceStatus());
		}
		searchField.setValue(criteria.getFreeText());

		applyingCriteria = false;
	}
}
