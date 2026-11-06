package de.symeda.sormas.ui.configuration.linelisting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.feature.FeatureConfigurationIndexDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.ui.SormasUI;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

@SuppressWarnings("serial")
public class LineListingRegionsLayout extends Div {

	private Disease disease;
	private Map<String, List<String>> regions;
	private Map<String, String> regionNames;

	public LineListingRegionsLayout(List<FeatureConfigurationIndexDto> configurations, Disease disease) {

		this.disease = disease;
		this.regionNames = new HashMap<>();
		this.regions = new TreeMap<>((r1, r2) -> regionNames.get(r1).compareTo(regionNames.get(r2)));

		for (RegionReferenceDto region : FacadeProvider.getRegionFacade().getAllActiveByServerCountry()) {
			regionNames.put(region.getUuid(), region.buildCaption());
			regions.put(region.getUuid(), new ArrayList<>());
		}

		if (configurations != null) {
			for (FeatureConfigurationIndexDto config : configurations) {
				regions.get(config.getRegionUuid()).add(config.getDistrictName());
			}
		}

		buildLayout();
	}

	private void buildLayout() {

		for (String regionUuid : regions.keySet()) {
			StringBuilder captionBuilder = new StringBuilder();
			captionBuilder.append("<b>").append(regionNames.get(regionUuid));
			if (regions.get(regionUuid).size() > 0) {
				captionBuilder.append(" (").append(regions.get(regionUuid).size()).append(")");
			}

			Button configButton = ButtonHelper.createButton("region-" + regionUuid, captionBuilder.toString(), e -> {
				SormasUI.get()
					.getNavigator()
					.navigateTo(LineListingConfigurationView.VIEW_NAME + "/" + regionUuid + "/?disease=" + disease.getName());
			}, CssStyles.BUTTON_BORDERLESS, CssStyles.BUTTON_FILTER, CssStyles.HSPACE_LEFT_4, CssStyles.VSPACE_4);
			// was setCaptionAsHtml(true): the caption is shown as HTML inside the button, as before
			configButton.setText(null);
			configButton.getElement().appendChild(VaadinUiUtil.createLabel(captionBuilder.toString(), ContentMode.HTML).getElement());

			if (regions.get(regionUuid).size() > 0) {
				CssStyles.style(configButton, CssStyles.BUTTON_FILTER_ENABLED);
			} else {
				CssStyles.style(configButton, CssStyles.BUTTON_FILTER_DISABLED);
			}

			add(configButton);
		}
	}
}
