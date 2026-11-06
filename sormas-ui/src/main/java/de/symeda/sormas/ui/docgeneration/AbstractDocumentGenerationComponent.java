package de.symeda.sormas.ui.docgeneration;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.ui.Alignment;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

public abstract class AbstractDocumentGenerationComponent extends VerticalLayout {

	public AbstractDocumentGenerationComponent() {
		super();

		setSpacing(false);

		Span headerDocgeneration = new Span(I18nProperties.getCaption(getComponentLabel()));
		headerDocgeneration.addClassName(CssStyles.H3);
		add(headerDocgeneration);
	}

	protected void addDocumentBar(Runnable createHandler, String documentLabel) {
		HorizontalLayout documentBar = new HorizontalLayout();
		documentBar.setPadding(false);
		documentBar.setSpacing(false);
		documentBar.setWidthFull();
		add(documentBar);

		Span labelDocument = new Span(I18nProperties.getCaption(documentLabel));
		documentBar.add(labelDocument);

		Button createButton = ButtonHelper.createButton(I18nProperties.getCaption(Captions.actionCreate));
		createButton.addClassName(CssStyles.BUTTON_PRIMARY);
		createButton.setIcon(VaadinIcon.FILE_TEXT);
		createButton.addClickListener((Button.ClickListener) clickEvent -> {
			createHandler.run();
		});

		documentBar.add(createButton);
		documentBar.setComponentAlignment(createButton, Alignment.MIDDLE_RIGHT);
	}

	protected String getComponentLabel() {
		return I18nProperties.getCaption(Captions.DocumentTemplate_plural);
	}
}
