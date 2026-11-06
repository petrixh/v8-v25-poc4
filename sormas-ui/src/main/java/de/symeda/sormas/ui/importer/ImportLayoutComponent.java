package de.symeda.sormas.ui.importer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

public class ImportLayoutComponent extends VerticalLayout {

	private static final long serialVersionUID = 3281659031721493105L;

	private Span headlineLabel;
	private Span infoTextLabel;
	private Button button;
	private Checkbox checkbox;

	public ImportLayoutComponent(int step, String headline, String infoText, VaadinIcon buttonIcon, String buttonCaption) {
		this(step, headline, infoText, buttonIcon, buttonCaption, null, null);
	}

	public ImportLayoutComponent(
		int step,
		String headline,
		String infoText,
		VaadinIcon buttonIcon,
		String buttonCaption,
		String checkboxCaption,
		String checkboxDescription) {
		setSpacing(false);
		setPadding(false);

		headlineLabel = new Span(I18nProperties.getString(Strings.step) + " " + step + ": " + headline);
		CssStyles.style(headlineLabel, CssStyles.H3);
		add(headlineLabel);

		if (infoText != null) {
			infoTextLabel = new Span(infoText);
			add(infoTextLabel);
		}

		if (checkboxCaption != null) {
			checkbox = new Checkbox(checkboxCaption);
			checkbox.setValue(false);
			if (checkboxDescription != null) {
				HorizontalLayout checkboxBar = new HorizontalLayout();
				checkboxBar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
				// was setDescription: the tooltip of the bar
				checkboxBar.getElement().setAttribute("title", checkboxDescription);
				CssStyles.style(checkboxBar, CssStyles.VSPACE_TOP_3);
				checkboxBar.add(checkbox);
				checkboxBar.add(VaadinIcon.INFO_CIRCLE.create());
				add(checkboxBar);
			} else {
				CssStyles.style(checkbox, CssStyles.VSPACE_TOP_3);
				add(checkbox);
			}
		}

		if (buttonCaption != null) {
			button = ButtonHelper.createIconButtonWithCaption(
				"import-step-" + step,
				buttonCaption,
				buttonIcon,
				null,
				CssStyles.BUTTON_PRIMARY,
				CssStyles.VSPACE_TOP_3);

			add(button);
		}
	}

	public Button getButton() {
		return button;
	}

	public Checkbox getCheckbox() {
		return checkbox;
	}
}
