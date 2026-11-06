package de.symeda.sormas.ui.campaign.components;

import java.util.List;

import com.vaadin.flow.component.HasValue;
import com.vaadin.ui.Alignment;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.campaign.CampaignReferenceDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.utils.CssStyles;

public class CampaignSelector extends HorizontalLayout {

	private final ComboBox<CampaignReferenceDto> campaignCombo;

	public CampaignSelector() {
		setPadding(false);
		setSpacing(false);

		Span campaignLabel = new Span(I18nProperties.getCaption(Captions.Campaign));
		campaignLabel.addClassName("v-caption");
		campaignLabel.addClassName(CssStyles.HSPACE_RIGHT_4);
		add(campaignLabel);
		setComponentAlignment(campaignLabel, Alignment.MIDDLE_CENTER);

		campaignCombo = new ComboBox<>(" ");
		List<CampaignReferenceDto> campaigns = FacadeProvider.getCampaignFacade().getAllActiveCampaignsAsReference();
		campaignCombo.setItems(campaigns);
		campaignCombo.setItemLabelGenerator(item -> item.buildCaption());
		campaignCombo.setEmptySelectionCaption(I18nProperties.getCaption(Captions.campaignAllCampaigns));
		final CampaignReferenceDto lastStartedCampaign = FacadeProvider.getCampaignFacade().getLastStartedCampaign();
		if (lastStartedCampaign != null) {
			campaignCombo.setValue(lastStartedCampaign);
		}
		add(campaignCombo);
	}

	public CampaignReferenceDto getValue() {
		return campaignCombo.getValue();
	}

	public void setValue(CampaignReferenceDto value) {
		campaignCombo.setValue(value);
	}

	public void addValueChangeListener(HasValue.ValueChangeListener listener) {
		campaignCombo.addValueChangeListener(listener);
	}
}
