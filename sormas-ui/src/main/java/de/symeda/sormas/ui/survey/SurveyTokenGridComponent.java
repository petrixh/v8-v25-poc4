package de.symeda.sormas.ui.survey;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.ui.Alignment;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.dialog.Dialog;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.survey.SurveyTokenCriteria;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.configuration.infrastructure.ImportSurveyTokensLayout;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.ViewConfiguration;

public class SurveyTokenGridComponent extends VerticalLayout {

	private SurveyTokenFilterForm filterForm;
	private SurveyTokenGrid grid;

	public SurveyTokenGridComponent(
		SurveyTokenCriteria criteria,
		ViewConfiguration viewConfiguration,
		Runnable filterChangeHandler,
		Runnable filterResetHandler) {

		setSizeFull();
		setSpacing(false);

		grid = new SurveyTokenGrid(criteria, viewConfiguration);

		filterForm = new SurveyTokenFilterForm();
		filterForm.addApplyHandler(e -> filterChangeHandler.run());
		filterForm.addResetHandler(e -> filterResetHandler.run());

		add(filterForm);
		add(createSurveyTokenImportButton(criteria));
		add(grid);
		setFlexGrow(1, grid);
	}

	public void reload() {
		grid.reload();
	}

	public void updateFilterComponents(SurveyTokenCriteria criteria) {
		filterForm.setValue(criteria);
	}

	public HorizontalLayout createSurveyTokenImportButton(SurveyTokenCriteria criteria) {

		HorizontalLayout statusFilterLayout = new HorizontalLayout();
		statusFilterLayout.setSpacing(true);
		statusFilterLayout.setWidth("100%");
		statusFilterLayout.addClassName(CssStyles.VSPACE_3);

		if (UiUtil.permitted(UserRight.SURVEY_TOKEN_IMPORT)) {
			Button importSurveyTokenButton = ButtonHelper.createIconButton(Captions.actionImportSurveyTokens, VaadinIcon.UPLOAD, e -> {
				Dialog window = VaadinUiUtil
					.showPopupWindow(new ImportSurveyTokensLayout(FacadeProvider.getSurveyFacade().getByUuid(criteria.getSurvey().getUuid())));
				window.setCaption(I18nProperties.getString(Strings.headingImportSurveyTokens));
				window.addCloseListener((event) -> reload());
			}, CssStyles.BUTTON_PRIMARY);

			statusFilterLayout.add(importSurveyTokenButton);
			statusFilterLayout.setComponentAlignment(importSurveyTokenButton, Alignment.TOP_RIGHT);
		}
		statusFilterLayout.addClassName("top-bar");
		return statusFilterLayout;
	}

}
