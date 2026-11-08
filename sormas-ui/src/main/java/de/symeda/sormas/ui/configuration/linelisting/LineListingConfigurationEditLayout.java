package de.symeda.sormas.ui.configuration.linelisting;

import java.time.LocalDate;
import java.util.List;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.symeda.sormas.ui.utils.ContentMode;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.feature.FeatureConfigurationIndexDto;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.utils.HtmlHelper;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.fields.DefaultDateFormat;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

@SuppressWarnings("serial")
public class LineListingConfigurationEditLayout extends VerticalLayout {

	private Button btnEnableAll;
	private Button btnDisableAll;
	private Button btnSetEndDateForAll;
	private DatePicker dfEndDate;

	private HorizontalLayout controlLayout;
	private LineListingConfigurationsGrid grid;

	private Disease disease;
	private String regionName;
	private List<FeatureConfigurationIndexDto> configurations;

	private Runnable saveCallback;
	private Runnable discardCallback;

	public LineListingConfigurationEditLayout(List<FeatureConfigurationIndexDto> configurations, Disease disease, String regionName) {
		this.configurations = configurations;
		this.disease = disease;
		this.regionName = regionName;
		buildLayout();
	}

	public void setSaveCallback(Runnable saveCallback) {
		this.saveCallback = saveCallback;
	}

	public void setDiscardCallback(Runnable discardCallback) {
		this.discardCallback = discardCallback;
	}

	private void buildLayout() {

		Span lblInfo = VaadinUiUtil.createLabel(
			HtmlRenderer.iconHtml(VaadinIcon.INFO_CIRCLE) + " "
				+ String.format(
					I18nProperties.getString(
						regionName != null ? Strings.infoLineListingConfigurationRegionEdit : Strings.infoLineListingConfigurationNationEdit),
					disease.toString(),
					HtmlHelper.cleanHtml(regionName)),
			ContentMode.HTML);
		CssStyles.style(lblInfo, CssStyles.VSPACE_4);
		add(lblInfo);

		controlLayout = new HorizontalLayout();
		controlLayout.setPadding(false);

		btnEnableAll = ButtonHelper.createButton(Captions.lineListingEnableAll, e -> {
			grid.enableAll();
		}, CssStyles.BUTTON_PRIMARY);

		controlLayout.add(btnEnableAll);

		btnDisableAll = ButtonHelper.createButton(Captions.lineListingDisableAllShort, e -> {
			grid.disableAll();
		}, CssStyles.BUTTON_PRIMARY, CssStyles.HSPACE_RIGHT_2);

		controlLayout.add(btnDisableAll);

		dfEndDate = new DatePicker();
		DefaultDateFormat.applyOnAttach(dfEndDate);
		dfEndDate.setId("endDate");
		dfEndDate.setPlaceholder(I18nProperties.getCaption(Captions.lineListingEndDate));
		dfEndDate.setMin(LocalDate.now());
		controlLayout.add(dfEndDate);

		btnSetEndDateForAll = ButtonHelper.createButton(Captions.lineListingSetEndDateForAll, e -> {
			grid.setEndDateForAll(dfEndDate.getValue());
		}, CssStyles.BUTTON_PRIMARY);

		controlLayout.add(btnSetEndDateForAll);

		add(controlLayout);

		grid = new LineListingConfigurationsGrid(configurations, regionName == null);
		grid.setWidthFull();
		add(grid);

		HorizontalLayout buttonLayout = buildButtonLayout();
		add(buttonLayout);
		setHorizontalComponentAlignment(Alignment.END, buttonLayout);
	}

	private HorizontalLayout buildButtonLayout() {
		HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setPadding(false);

		Button btnDiscard = ButtonHelper.createButton(Captions.actionDiscardChanges, e -> {
			if (discardCallback != null) {
				discardCallback.run();
			}
		});

		buttonLayout.add(btnDiscard);

		Button btnSave = ButtonHelper.createButton(Captions.actionSaveChanges, e -> {
			if (grid.validateDates()) {
				FacadeProvider.getFeatureConfigurationFacade().saveFeatureConfigurations(grid.getChangedConfigurations(), FeatureType.LINE_LISTING);
				if (saveCallback != null) {
					saveCallback.run();
				}
			} else {
				NotificationHelper.show(null, I18nProperties.getString(Strings.messageInvalidDatesLineListing), Type.ERROR_MESSAGE);
			}
		}, CssStyles.BUTTON_PRIMARY);

		buttonLayout.add(btnSave);

		return buttonLayout;
	}
}
