package de.symeda.sormas.ui.campaign.components;

import com.vaadin.flow.component.combobox.ComboBox;

import de.symeda.sormas.api.campaign.CampaignJurisdictionLevel;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;

import static de.symeda.sormas.api.campaign.CampaignJurisdictionLevel.AREA;
import static de.symeda.sormas.api.campaign.CampaignJurisdictionLevel.COMMUNITY;
import static de.symeda.sormas.api.campaign.CampaignJurisdictionLevel.DISTRICT;
import static de.symeda.sormas.api.campaign.CampaignJurisdictionLevel.REGION;

public class JurisdictionSelector extends ComboBox<CampaignJurisdictionLevel> {

	public JurisdictionSelector() {
		setLabel(I18nProperties.getCaption(Captions.Campaign_grouping));
		setItems(AREA, REGION, DISTRICT, COMMUNITY);
		setValue(AREA);
	}

	/**
	 * Was {@code setEmptySelectionAllowed(false)}: a null from the client is ignored (no value change event) and the client
	 * shows the value again, as {@code utils.fields.ComboBox} does for {@code setNullSelectionAllowed(false)}.
	 */
	@Override
	protected void setModelValue(CampaignJurisdictionLevel newModelValue, boolean fromClient) {
		if (fromClient && newModelValue == null) {
			refreshValue();
			return;
		}
		super.setModelValue(newModelValue, fromClient);
	}
}
