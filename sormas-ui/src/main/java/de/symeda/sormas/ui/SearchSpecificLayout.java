package de.symeda.sormas.ui;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.utils.ConfirmationComponent;
import de.symeda.sormas.ui.utils.CssStyles;

public class SearchSpecificLayout extends VerticalLayout {

	public SearchSpecificLayout(
		Runnable confirmCallback,
		Runnable closePopupCallback,
		TextField searchField,
		String searchDescription,
		String confirmCaption) {

		setPadding(true);
		setSpacing(false);
		setWidthFull();

		// was a Label in HTML content mode
		Span lblSearchDescription = new Span();
		lblSearchDescription.getElement().setProperty("innerHTML", searchDescription);
		lblSearchDescription.setWidthFull();
		CssStyles.style(lblSearchDescription, CssStyles.VSPACE_2);
		add(lblSearchDescription);

		ConfirmationComponent confirmationComponent = new ConfirmationComponent(false) {

			@Override
			protected void onConfirm() {
				confirmCallback.run();
			}

			@Override
			protected void onCancel() {
				closePopupCallback.run();
			}
		};

		searchField.setPlaceholder(I18nProperties.getString(Strings.promptSearch));
		searchField.setWidth("300px");
		searchField.addValueChangeListener(e -> confirmationComponent.getConfirmButton().setEnabled(StringUtils.isNotEmpty(e.getValue())));
		CssStyles.style(searchField, CssStyles.VSPACE_2);
		add(searchField);

		confirmationComponent.getConfirmButton().setText(confirmCaption);
		confirmationComponent.getConfirmButton().setEnabled(false);
		confirmationComponent.getCancelButton().setText(I18nProperties.getCaption(Captions.actionCancel));
		confirmationComponent.setPadding(true);
		add(confirmationComponent);
		setHorizontalComponentAlignment(FlexComponent.Alignment.END, confirmationComponent);
	}
}
