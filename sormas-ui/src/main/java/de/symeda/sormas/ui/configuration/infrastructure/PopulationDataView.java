package de.symeda.sormas.ui.configuration.infrastructure;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.server.FileDownloader;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.ui.Alignment;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.dialog.Dialog;
import de.symeda.sormas.ui.utils.CssStyles;

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
		layout.addComponent(lblIntroduction);
		layout.setComponentAlignment(lblIntroduction, Alignment.MIDDLE_CENTER);

		Button btnImport = ButtonHelper.createIconButton(Captions.actionImport, VaadinIcon.UPLOAD, e -> {
			Dialog window = VaadinUiUtil.showPopupWindow(new InfrastructureImportLayout(InfrastructureType.POPULATION_DATA));
			window.setCaption(I18nProperties.getString(Strings.headingImportPopulationData));
		}, CssStyles.VSPACE_4, CssStyles.BUTTON_PRIMARY);
		layout.addComponent(btnImport);
		layout.setComponentAlignment(btnImport, Alignment.MIDDLE_CENTER);

		Button btnExport = ButtonHelper.createIconButton(Captions.export, VaadinIcon.DOWNLOAD, null, CssStyles.BUTTON_PRIMARY);
		layout.addComponent(btnExport);
		layout.setComponentAlignment(btnExport, Alignment.MIDDLE_CENTER);

		DownloadHandler populationDataExportResource = DownloadUtil.createPopulationDataExportResource();
		new FileDownloader(populationDataExportResource).extend(btnExport);

		addComponent(layout);
	}
}
