package de.symeda.sormas.ui.configuration.infrastructure;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.InfrastructureType;
import de.symeda.sormas.ui.configuration.AbstractConfigurationView;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

@SuppressWarnings("serial")
public class PopulationDataView extends AbstractConfigurationView {

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/populationdata";

	public PopulationDataView() {

		super(VIEW_NAME);

		VerticalLayout layout = new VerticalLayout();
		layout.setSpacing(false);
		CssStyles.style(layout, CssStyles.VSPACE_TOP_1);

		Span lblIntroduction = new Span(I18nProperties.getString(Strings.infoPopulationDataView));
		CssStyles.style(lblIntroduction, CssStyles.VSPACE_2);
		layout.add(lblIntroduction);
		layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, lblIntroduction);

		Button btnImport = ButtonHelper.createIconButton(Captions.actionImport, VaadinIcon.UPLOAD, e -> {
			Dialog window = VaadinUiUtil.showPopupWindow(new InfrastructureImportLayout(InfrastructureType.POPULATION_DATA));
			window.setHeaderTitle(I18nProperties.getString(Strings.headingImportPopulationData));
		}, CssStyles.VSPACE_4, CssStyles.BUTTON_PRIMARY);
		layout.add(btnImport);
		layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, btnImport);

		Button btnExport = ButtonHelper.createIconButton(Captions.export, VaadinIcon.DOWNLOAD, null, CssStyles.BUTTON_PRIMARY);
		layout.add(btnExport);

		DownloadHandler populationDataExportResource = DownloadUtil.createPopulationDataExportResource();
		// the download link takes the button's place in the layout, so the link is centred (was the button)
		Anchor exportLink = DownloadUtil.attachDownloader(btnExport, populationDataExportResource);
		layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, exportLink);

		addComponent(layout);
	}
}
