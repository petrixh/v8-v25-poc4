package de.symeda.sormas.ui.configuration.linelisting;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

@SuppressWarnings("serial")
public class LineListingDiseaseLayout extends VerticalLayout {

	private final Disease disease;
	private Span lblDisease;
	private Button btnEdit;
	private Button btnDisableAll;

	private Runnable editCallback;
	private Runnable disableAllCallback;

	public LineListingDiseaseLayout(Disease disease) {
		this.disease = disease;

		setSpacing(false);
		setPadding(false);

		buildLayout();
	}

	public void setEditCallback(Runnable editCallback) {
		this.editCallback = editCallback;
	}

	public void setDisableAllCallback(Runnable disableAllCallback) {
		this.disableAllCallback = disableAllCallback;
	}

	private void buildLayout() {

		lblDisease = new Span(disease.toString());
		lblDisease.setWidthFull();
		CssStyles
			.style(lblDisease, CssStyles.LABEL_ROUNDED_CORNERS, CssStyles.LABEL_BOLD, CssStyles.LABEL_BACKGROUND_FOCUS_LIGHT, CssStyles.ALIGN_CENTER);
		add(lblDisease);

		btnEdit = ButtonHelper.createButton(Captions.lineListingEdit, e -> {
			if (editCallback != null) {
				editCallback.run();
			}
		}, CssStyles.BUTTON_PRIMARY, CssStyles.VSPACE_TOP_3);
		btnEdit.setWidthFull();

		add(btnEdit);

		btnDisableAll = ButtonHelper.createButton(Captions.lineListingDisableAll, e -> {
			if (disableAllCallback != null) {
				disableAllCallback.run();
			}
		}, CssStyles.BUTTON_PRIMARY, CssStyles.VSPACE_TOP_4);
		btnDisableAll.setWidthFull();

		add(btnDisableAll);
	}
}
