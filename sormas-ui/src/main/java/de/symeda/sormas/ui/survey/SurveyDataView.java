package de.symeda.sormas.ui.survey;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.ui.Alignment;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.dialog.Dialog;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.survey.SurveyDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.configuration.infrastructure.ImportSurveyTokenResponsesLayout;
import de.symeda.sormas.ui.samples.HasName;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DetailSubComponentWrapper;
import de.symeda.sormas.ui.utils.LayoutWithSidePanel;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class SurveyDataView extends AbstractSurveyView implements HasName {

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/data";

	private CommitDiscardWrapperComponent<SurveyDataForm> editComponent;
	private SurveyDto survey;

	public SurveyDataView() {
		super(VIEW_NAME);
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	protected String getRootViewName() {
		return SurveysView.VIEW_NAME;
	}

	@Override
	protected void initView(String params) {
		setHeightUndefined();

		survey = FacadeProvider.getSurveyFacade().getByUuid(getReference().getUuid());
		editComponent =
			ControllerProvider.getSurveyController().getSurveyEditComponent(getReference().getUuid(), UserRight.SURVEY_EDIT, isEditAllowed());

		DetailSubComponentWrapper container = new DetailSubComponentWrapper(() -> editComponent);
		container.setWidthFull();
		container.setPadding(true);
		setSubComponent(container);
		container.setEnabled(true);

		LayoutWithSidePanel layout = new LayoutWithSidePanel(editComponent);
		container.add(createSurveyTokenResponsesButton());
		container.add(layout);
	}

	private HorizontalLayout createSurveyTokenResponsesButton() {
		HorizontalLayout statusFilterLayout = new HorizontalLayout();
		statusFilterLayout.setSpacing(true);
		statusFilterLayout.setWidth("100%");
		statusFilterLayout.addClassName(CssStyles.VSPACE_3);

		if (UiUtil.permitted(UserRight.SURVEY_TOKEN_IMPORT)) {
			Button importSurveyTokenResponsesButton = ButtonHelper.createIconButton(Captions.actionImportSurveyTokenResponses, VaadinIcon.UPLOAD, e -> {
				Dialog window = VaadinUiUtil.showPopupWindow(new ImportSurveyTokenResponsesLayout(survey));
				window.setCaption(I18nProperties.getString(Strings.headingImportSurveyTokenResponses));
			}, CssStyles.BUTTON_PRIMARY);

			statusFilterLayout.add(importSurveyTokenResponsesButton);
			statusFilterLayout.setComponentAlignment(importSurveyTokenResponsesButton, Alignment.MIDDLE_CENTER);
		}
		statusFilterLayout.addClassName("top-bar");
		return statusFilterLayout;
	}
}
