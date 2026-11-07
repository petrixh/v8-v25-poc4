package de.symeda.sormas.ui.reports.aggregate;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.dialog.Dialog;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.report.AggregateReportDto;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

/**
 * @author Christopher Riedel
 */
public class AggregateReportController {

	public AggregateReportController() {

	}

	public void openEditOrCreateWindow(Runnable onClose, boolean edit, AggregateReportDto aggregateReportDto) {
		Dialog window = VaadinUiUtil.createPopupWindow();
		AggregateReportsEditLayout createLayout = new AggregateReportsEditLayout(window, edit, aggregateReportDto);
		window.setHeight(90, Unit.PERCENTAGE);
		// was getWidth() + 64 + 20 pixels (Flow returns the width as a CSS string)
		window.setWidth("calc(" + createLayout.getWidth() + " + 84px)");
		if (edit) {
			window.setHeaderTitle(I18nProperties.getString(Strings.headingEditAggregateReport));
		} else {
			window.setHeaderTitle(I18nProperties.getString(Strings.headingCreateNewAggregateReport));
		}
		window.add(createLayout);
		VaadinUiUtil.addCloseListener(window, e -> onClose.run());
		window.open();
	}

	public void deleteAggregateReport(String uuid, Runnable onClose) {

		VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.setPadding(false);
		Span contentLabel = new Span(I18nProperties.getString(Strings.messageAggregateReportDelete));
		verticalLayout.add(contentLabel);

		VaadinUiUtil.showConfirmationPopup(
			I18nProperties.getString(Strings.headingDeleteConfirmation),
			verticalLayout,
			I18nProperties.getString(Strings.yes),
			I18nProperties.getString(Strings.no),
			640,
			confirmed -> {
				if (Boolean.TRUE.equals(confirmed)) {
					FacadeProvider.getAggregateReportFacade().deleteReport(uuid);
					onClose.run();
				}
				return true;
			});
	}
}
