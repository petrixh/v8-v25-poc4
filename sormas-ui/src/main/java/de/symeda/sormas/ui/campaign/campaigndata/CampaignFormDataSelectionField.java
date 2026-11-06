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

package de.symeda.sormas.ui.campaign.campaigndata;

import com.vaadin.ui.Alignment;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.campaign.data.CampaignFormDataDto;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class CampaignFormDataSelectionField extends VerticalLayout {

	private static final long serialVersionUID = 1771273989939992148L;

	private final CampaignFormDataDto newData;
	private final CampaignFormDataDto existingData;
	private final Runnable cancelCallback;
	private final Runnable skipCallback;
	private final Runnable overwriteCallback;
	private final String infoText;

	public CampaignFormDataSelectionField(
		CampaignFormDataDto newData,
		CampaignFormDataDto existingData,
		String infoText,
		Runnable cancelCallback,
		Runnable skipCallback,
		Runnable overwriteCallback) {
		this.newData = newData;
		this.existingData = existingData;
		this.infoText = infoText;
		this.cancelCallback = cancelCallback;
		this.skipCallback = skipCallback;
		this.overwriteCallback = overwriteCallback;

		initialize();
	}

	private void initialize() {

		setSpacing(true);
		setPadding(true);
		setHeightUndefined();
		setWidthFull();

		addInfoComponent();
		addFormDataDetailsComponent();
		addButtons();
	}

	private void addInfoComponent() {
		add(VaadinUiUtil.createInfoComponent(infoText));
	}

	private void addFormDataDetailsComponent() {

		VerticalLayout formDataDetailsLayout = new VerticalLayout();
		formDataDetailsLayout.setPadding(false);

		Span newFormDataHeading = new Span(I18nProperties.getString(Strings.headingCampaignFormDataDuplicateNew));
		CssStyles.style(newFormDataHeading, CssStyles.H3);
		formDataDetailsLayout.add(newFormDataHeading);
		formDataDetailsLayout.add(buildFormDataDetailsComponent(newData, false));

		Span existingFormDataHeading = new Span(I18nProperties.getString(Strings.headingCampaignFormDataDuplicateExisting));
		CssStyles.style(existingFormDataHeading, CssStyles.H3);
		formDataDetailsLayout.add(existingFormDataHeading);
		formDataDetailsLayout.add(buildFormDataDetailsComponent(existingData, true));

		add(formDataDetailsLayout);
	}

	private void addButtons() {

		HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setWidthUndefined();

		Button btnCancel = ButtonHelper.createButton(I18nProperties.getCaption(Captions.importCancelImport));
		btnCancel.addClickListener(e -> cancelCallback.run());
		buttonLayout.add(btnCancel);

		Button btnSkip = ButtonHelper.createButton(I18nProperties.getCaption(Captions.actionSkip));
		btnSkip.addClickListener(e -> skipCallback.run());
		buttonLayout.add(btnSkip);

		Button btnOverwrite = ButtonHelper.createButton(I18nProperties.getCaption(Captions.actionOverwrite));
		CssStyles.style(btnOverwrite, CssStyles.BUTTON_PRIMARY);
		btnOverwrite.addClickListener(e -> overwriteCallback.run());
		buttonLayout.add(btnOverwrite);

		add(buttonLayout);
		setComponentAlignment(buttonLayout, Alignment.MIDDLE_RIGHT);
	}

	private HorizontalLayout buildFormDataDetailsComponent(CampaignFormDataDto formData, boolean existingData) {
		HorizontalLayout formDataLayout = new HorizontalLayout();
		formDataLayout.setSpacing(true);
		{
			if (UiUtil.disabled(FeatureType.HIDE_JURISDICTION_FIELDS)) {
				Span fdRegion = new Span(formData.getRegion() != null ? formData.getRegion().buildCaption() : "");
				fdRegion.setCaption(I18nProperties.getPrefixCaption(CampaignFormDataDto.I18N_PREFIX, CampaignFormDataDto.REGION));
				fdRegion.setWidthUndefined();
				formDataLayout.add(fdRegion);

				Span fdDistrict = new Span(formData.getDistrict() != null ? formData.getDistrict().buildCaption() : "");
				fdDistrict.setCaption(I18nProperties.getPrefixCaption(CampaignFormDataDto.I18N_PREFIX, CampaignFormDataDto.DISTRICT));
				fdDistrict.setWidthUndefined();
				formDataLayout.add(fdDistrict);

				Span fdCommunity = new Span(formData.getCommunity() != null ? formData.getCommunity().buildCaption() : "");
				fdCommunity.setCaption(I18nProperties.getPrefixCaption(CampaignFormDataDto.I18N_PREFIX, CampaignFormDataDto.COMMUNITY));
				fdCommunity.setWidthUndefined();
				formDataLayout.add(fdCommunity);
			}

			Span fdFormDate = new Span(DateFormatHelper.formatDate(formData.getFormDate()));
			fdFormDate.setCaption(I18nProperties.getPrefixCaption(CampaignFormDataDto.I18N_PREFIX, CampaignFormDataDto.FORM_DATE));
			fdFormDate.setWidthUndefined();
			formDataLayout.add(fdFormDate);

			if (existingData) {
				Span fdCreatingUser = new Span(formData.getCreatingUser() != null ? formData.getCreatingUser().buildCaption() : "");
				fdCreatingUser.setCaption(I18nProperties.getPrefixCaption(CampaignFormDataDto.I18N_PREFIX, CampaignFormDataDto.CREATING_USER));
				fdCreatingUser.setWidthUndefined();
				formDataLayout.add(fdCreatingUser);

				Span fdCreationDate = new Span(DateFormatHelper.formatDate(formData.getCreationDate()));
				fdCreationDate.setCaption(I18nProperties.getPrefixCaption(CampaignFormDataDto.I18N_PREFIX, CampaignFormDataDto.CREATION_DATE));
				fdCreationDate.setWidthUndefined();
				formDataLayout.add(fdCreationDate);
			}
		}

		return formDataLayout;
	}

}
