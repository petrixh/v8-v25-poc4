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
package de.symeda.sormas.ui.configuration.outbreak;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.component.grid.ItemClickEvent;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.outbreak.OutbreakCriteria;
import de.symeda.sormas.api.outbreak.OutbreakDto;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.LayoutUtil;
import de.symeda.sormas.ui.utils.grid.Grid;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

/**
 * Was a V7 container grid with one item per region and one property per disease. The items are now the regions; the
 * configuration of a region and disease (the V7 property value) is kept in {@link #configurations}.
 */
@SuppressWarnings("serial")
public class OutbreakOverviewGrid extends Grid<RegionReferenceDto> {

	private static final String REGION = Captions.Region;

	private UserDto user;

	private final Map<RegionReferenceDto, Map<Disease, OutbreakRegionConfiguration>> configurations = new LinkedHashMap<>();

	public OutbreakOverviewGrid() {

		super();
		setSizeFull();
		setSelectionMode(SelectionMode.NONE);

		user = UiUtil.getUser();

		// was a V7 HtmlRenderer with the V7CaptionConverter (the region caption, as HTML)
		Column<RegionReferenceDto, String> column = addColumn(RegionReferenceDto::buildCaption, new HtmlRenderer()).setMaximumWidth(200);
		column.setKey(REGION);
		getColumn(REGION).setHeaderCaption(I18nProperties.getCaption(Captions.Region));

		for (Disease disease : FacadeProvider.getDiseaseConfigurationFacade().getAllDiseases(true, true, true)) {
			addColumn(region -> getConfiguration(region, disease), new HtmlRenderer()).setMaximumWidth(200)
				.setKey(disease.name())
				.setHeaderCaption(disease.toShortString())
				// V7 could not sort the configuration property (not Comparable)
				.setSortable(false)
				// was the V7 cell description and cell style generators (style only for the disease cells)
				.setDescriptionGenerator(region -> getCellDescription(region, disease))
				.setStyleGenerator(region -> CssStyles.ALIGN_CENTER);
		}

		addItemClickListener(this::itemClick);
	}

	/**
	 * Was the V7 converter of the disease columns: the cell HTML for a configuration.
	 */
	private String getConfiguration(RegionReferenceDto region, Disease disease) {

		OutbreakRegionConfiguration value = configurations.get(region).get(disease);

		boolean styleAsButton = (UiUtil.hasNoneJurisdictionLevel() || UiUtil.hasNationJurisdictionLevel())
			|| UiUtil.permitted(UserRight.OUTBREAK_EDIT) && DataHelper.equal(UiUtil.getUser().getRegion(), value.getRegion());
		boolean moreThanHalfOfDistricts = value.getAffectedDistricts().size() >= value.getTotalDistricts() / 2.0f;

		String styles;
		if (styleAsButton) {
			if (moreThanHalfOfDistricts) {
				styles = CssStyles.buildVaadinStyle(CssStyles.VAADIN_BUTTON, CssStyles.BUTTON_CRITICAL);
			} else if (!value.getAffectedDistricts().isEmpty()) {
				styles = CssStyles.buildVaadinStyle(CssStyles.VAADIN_BUTTON, CssStyles.BUTTON_WARNING);
			} else {
				styles = CssStyles.buildVaadinStyle(CssStyles.VAADIN_BUTTON);
			}

		} else {
			if (moreThanHalfOfDistricts) {
				styles = CssStyles.buildVaadinStyle(CssStyles.VAADIN_LABEL, CssStyles.LABEL_CRITICAL);
			} else if (!value.getAffectedDistricts().isEmpty()) {
				styles = CssStyles.buildVaadinStyle(CssStyles.VAADIN_LABEL, CssStyles.LABEL_WARNING);
			} else {
				styles = CssStyles.buildVaadinStyle(CssStyles.VAADIN_LABEL);
			}
		}
		return LayoutUtil.divCss(styles, value.toString());
	}

	private String getCellDescription(RegionReferenceDto region, Disease disease) {

		Set<DistrictReferenceDto> affectedDistricts = configurations.get(region).get(disease).getAffectedDistricts();

		if (affectedDistricts.isEmpty()) {
			return I18nProperties.getCaption(Captions.outbreakNoOutbreak);
		}

		StringBuilder affectedDistrictsStringBuilder = new StringBuilder();
		affectedDistrictsStringBuilder.append(I18nProperties.getCaption(Captions.outbreakAffectedDistricts)).append(": ");

		int index = 0;
		for (DistrictReferenceDto affectedDistrict : affectedDistricts) {
			affectedDistrictsStringBuilder.append(affectedDistrict.buildCaption());
			if (index < affectedDistricts.size() - 1) {
				affectedDistrictsStringBuilder.append(", ");
			}
			index++;
		}

		return affectedDistrictsStringBuilder.toString();
	}

	public void reload() {

		configurations.clear();

		// Initially set all columns to their default value
		for (RegionReferenceDto region : FacadeProvider.getRegionFacade().getAllActiveByServerCountry()) {
			addItem(region);
		}

		// Alter cells with regions and diseases that actually have an outbreak
		OutbreakCriteria criteria = new OutbreakCriteria().active(true);
		criteria.diseases(UiUtil.getUser().getLimitedDiseases());
		List<OutbreakDto> activeOutbreaks = FacadeProvider.getOutbreakFacade().getActive(criteria);

		for (OutbreakDto outbreak : activeOutbreaks) {
			DistrictReferenceDto outbreakDistrict = outbreak.getDistrict();
			RegionReferenceDto outbreakRegion = FacadeProvider.getDistrictFacade().getByUuid(outbreakDistrict.getUuid()).getRegion();
			Disease outbreakDisease = outbreak.getDisease();

			// Only show the Outbreak if its Disease is active on the system
			if (FacadeProvider.getDiseaseConfigurationFacade().isActiveDisease(outbreakDisease)) {
				Map<Disease, OutbreakRegionConfiguration> item = configurations.get(outbreakRegion);
				if (item != null) { // region may be no longer active
					OutbreakRegionConfiguration configuration = item.get(outbreakDisease);
					configuration.getAffectedDistricts().add(outbreakDistrict);
				}
			}
		}

		setItems(new ArrayList<>(configurations.keySet()));
	}

	private void addItem(RegionReferenceDto region) {

		int totalDistricts = FacadeProvider.getDistrictFacade().getCountByRegion(region.getUuid());
		Map<Disease, OutbreakRegionConfiguration> item = new LinkedHashMap<>();
		configurations.put(region, item);
		for (Disease disease : FacadeProvider.getDiseaseConfigurationFacade().getAllDiseases(true, true, true)) {
			item.put(disease, new OutbreakRegionConfiguration(disease, region, totalDistricts, new HashSet<>()));
		}
	}

	public void itemClick(ItemClickEvent<RegionReferenceDto> event) {

		RegionReferenceDto clickedItem = event.getItem();

		if (event.getColumn() == null || REGION.equals(event.getColumn().getKey())) {
			return;
		}

		Disease disease = Disease.valueOf(event.getColumn().getKey());

		// Open the outbreak configuration window for the clicked row when
		// a) the user is allowed to configure all existing outbreaks or
		// b) the user is allowed to configure outbreaks in his assigned region and has clicked the respective row
		if (UiUtil.hasNoneJurisdictionLevel() || UiUtil.hasNationJurisdictionLevel()) {
			ControllerProvider.getOutbreakController().openOutbreakConfigurationWindow(disease, configurations.get(clickedItem).get(disease));
		} else if (clickedItem.equals(user.getRegion())) {
			ControllerProvider.getOutbreakController().openOutbreakConfigurationWindow(disease, configurations.get(clickedItem).get(disease));
		} else {
			return;
		}
	}
}
