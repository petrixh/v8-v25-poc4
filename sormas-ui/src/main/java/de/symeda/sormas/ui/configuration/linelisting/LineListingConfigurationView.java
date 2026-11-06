package de.symeda.sormas.ui.configuration.linelisting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.symeda.sormas.ui.navigation.ViewChangeListener.ViewChangeEvent;
import de.symeda.sormas.ui.utils.ContentMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.dialog.Dialog;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.feature.FeatureConfigurationCriteria;
import de.symeda.sormas.api.feature.FeatureConfigurationIndexDto;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.ui.SormasUI;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.configuration.AbstractConfigurationView;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

@SuppressWarnings("serial")
public class LineListingConfigurationView extends AbstractConfigurationView {

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/linelisting";

	private VerticalLayout contentLayout;
	private LineListingAddDiseaseLayout addDiseaseLayout;
	private VerticalLayout lineListingConfigurationsLayout;

	private Map<Disease, List<FeatureConfigurationIndexDto>> configurationMap;

	private RegionReferenceDto region;

	public LineListingConfigurationView() {

		super(VIEW_NAME);
		configurationMap = new TreeMap<>((d1, d2) -> d1.toString().compareTo(d2.toString()));
	}

	private void buildView(Disease enteredDisease) {

		if (region != null && (UiUtil.hasNationJurisdictionLevel() || UiUtil.hasNoneJurisdictionLevel())) {
			Button btnBackToNationView = ButtonHelper.createIconButton(Captions.actionBackToNationOverview, VaadinIcon.ARROW_BACKWARD, e -> {
				SormasUI.get().getNavigator().navigateTo(LineListingConfigurationView.VIEW_NAME);
			}, CssStyles.BUTTON_PRIMARY);

			addHeaderComponent(btnBackToNationView);
		}

		if (region != null) {
			getViewSubTitleLabel().setValue(region.buildCaption());
		}

		Span infoTextLabel;
		if (region != null) {
			infoTextLabel = new Span(
				VaadinIcon.INFO_CIRCLE.getHtml() + " " + I18nProperties.getString(Strings.infoLineListingConfigurationRegion),
				ContentMode.HTML);
		} else {
			infoTextLabel = new Span(
				VaadinIcon.INFO_CIRCLE.getHtml() + " " + I18nProperties.getString(Strings.infoLineListingConfigurationNation),
				ContentMode.HTML);
		}
		CssStyles.style(infoTextLabel, CssStyles.LABEL_MEDIUM);
		contentLayout.add(infoTextLabel);

		lineListingConfigurationsLayout = new VerticalLayout();
		lineListingConfigurationsLayout.setWidthFull();
		lineListingConfigurationsLayout.setSpacing(true);
		lineListingConfigurationsLayout.setPadding(false);
		CssStyles.style(lineListingConfigurationsLayout, CssStyles.VSPACE_TOP_3, CssStyles.VSPACE_3);
		contentLayout.add(lineListingConfigurationsLayout);

		// Retrieve existing line listing configurations from the database
		List<Disease> diseasesWithoutConfigurations = FacadeProvider.getDiseaseConfigurationFacade().getAllDiseases(true, true, true);

		List<FeatureConfigurationIndexDto> lineListingConfigurations = FacadeProvider.getFeatureConfigurationFacade()
			.getFeatureConfigurations(new FeatureConfigurationCriteria().featureType(FeatureType.LINE_LISTING).region(region), false);

		for (FeatureConfigurationIndexDto configuration : lineListingConfigurations) {
			if (!configurationMap.containsKey(configuration.getDisease())) {
				configurationMap.put(configuration.getDisease(), new ArrayList<>());
				diseasesWithoutConfigurations.remove(configuration.getDisease());
			}

			configurationMap.get(configuration.getDisease()).add(configuration);
		}

		configurationMap.keySet().stream().forEach(disease -> {
			lineListingConfigurationsLayout.add(createDiseaseConfigurationLayout(disease, configurationMap.get(disease)));
		});

		if (enteredDisease != null && !configurationMap.containsKey(enteredDisease)) {
			lineListingConfigurationsLayout.add(createDiseaseConfigurationLayout(enteredDisease, null));
		}

		addDiseaseLayout = new LineListingAddDiseaseLayout(diseasesWithoutConfigurations);
		addDiseaseLayout.setWidth(600, Unit.PIXELS);

		addDiseaseLayout.setAddDiseaseCallback(disease -> {
			HorizontalLayout diseaseConfigurationLayout = createDiseaseConfigurationLayout(disease, null);
			lineListingConfigurationsLayout.add(diseaseConfigurationLayout);
			addDiseaseLayout.removeDiseaseFromList(disease);
			if (region != null) {
				openEditWindow(disease);
			}
		});

		contentLayout.add(addDiseaseLayout);
	}

	private void showConfirmDisableAllWindow(HorizontalLayout diseaseAndDistrictLayout, Disease disease) {

		VaadinUiUtil.showConfirmationPopup(
			I18nProperties.getString(Strings.headingDisableLineListing),
			new Span(
				I18nProperties
					.getString(region != null ? Strings.confirmationDisableAllLineListingRegion : Strings.confirmationDisableAllLineListingNational)),
			I18nProperties.getCaption(Captions.actionConfirm),
			I18nProperties.getCaption(Captions.actionCancel),
			480,
			result -> {
				if (Boolean.TRUE.equals(result)) {
					FeatureConfigurationCriteria criteria =
						new FeatureConfigurationCriteria().disease(disease).region(region).featureType(FeatureType.LINE_LISTING);
					FacadeProvider.getFeatureConfigurationFacade().deleteAllFeatureConfigurations(criteria);
					lineListingConfigurationsLayout.removeComponent(diseaseAndDistrictLayout);
					addDiseaseLayout.addDiseaseToList(disease);
					NotificationHelper.show(null, I18nProperties.getString(Strings.messageLineListingDisabled), Type.TRAY_NOTIFICATION);
				}
			});
	}

	private HorizontalLayout createDiseaseConfigurationLayout(Disease disease, List<FeatureConfigurationIndexDto> configurations) {

		HorizontalLayout contentLayout = new HorizontalLayout();
		contentLayout.setWidthFull();

		LineListingDiseaseLayout diseaseLayout = new LineListingDiseaseLayout(disease);
		{
			diseaseLayout.setWidth(300, Unit.PIXELS);
			CssStyles.style(diseaseLayout, CssStyles.VSPACE_4, CssStyles.HSPACE_RIGHT_4);
			diseaseLayout.setEditCallback(() -> openEditWindow(disease));
			diseaseLayout.setDisableAllCallback(() -> showConfirmDisableAllWindow(contentLayout, disease));
		}
		contentLayout.add(diseaseLayout);
		contentLayout.setFlexGrow(0, diseaseLayout);

		if (configurations != null || region == null) {
			Component districtsOrRegionsLayout;
			if (region != null) {
				districtsOrRegionsLayout = new LineListingActiveDistrictsLayout(configurations);
			} else {
				districtsOrRegionsLayout = new LineListingRegionsLayout(configurations, disease);
			}
			districtsOrRegionsLayout.setWidthFull();
			CssStyles.style(districtsOrRegionsLayout, CssStyles.VSPACE_4, CssStyles.HSPACE_LEFT_1);
			contentLayout.add(districtsOrRegionsLayout);
			contentLayout.setFlexGrow(1, districtsOrRegionsLayout);
		}

		return contentLayout;
	}

	@Override
	public void enter(ViewChangeEvent event) {

		super.enter(event);
		Disease disease = null;

		String[] params = event.getParameters().split("\\?");
		if (params.length > 0) {
			String regionUuid = params[0].replaceAll("/", "");
			this.region = FacadeProvider.getRegionFacade().getReferenceByUuid(regionUuid);
			if (params.length > 1) {
				disease = Disease.valueOf(params[1].substring(params[1].indexOf("=") + 1));
			}
		} else if (!(UiUtil.hasNationJurisdictionLevel() || UiUtil.hasNoneJurisdictionLevel())) {
			this.region = UiUtil.getUser().getRegion();
		}

		contentLayout = new VerticalLayout();
		contentLayout.setPadding(true);
		contentLayout.setSpacing(true);
		contentLayout.setWidthFull();
		contentLayout.setClassName("crud-main-layout");

		buildView(disease);
		addComponent(contentLayout);
	}

	private void openEditWindow(Disease disease) {

		Dialog editWindow = VaadinUiUtil.createPopupWindow();

		FeatureConfigurationCriteria criteria =
			new FeatureConfigurationCriteria().disease(disease).region(region).featureType(FeatureType.LINE_LISTING);
		LineListingConfigurationEditLayout editLayout = new LineListingConfigurationEditLayout(
			FacadeProvider.getFeatureConfigurationFacade().getFeatureConfigurations(criteria, true),
			disease,
			region != null ? region.buildCaption() : null);

		editLayout.setSaveCallback(() -> {
			NotificationHelper.show(null, I18nProperties.getString(Strings.messageLineListingSaved), Type.TRAY_NOTIFICATION);
			UI.getCurrent().getPage().reload();
		});
		editLayout.setDiscardCallback(() -> editWindow.close());

		editWindow.setWidth(1024, Unit.PIXELS);
		editWindow.setCaption(I18nProperties.getString(Strings.headingEditLineListing));
		editWindow.setContent(editLayout);
		editWindow.open();
	}
}
