package de.symeda.sormas.ui.caze.messaging;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.value.ValueChangeMode;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.ResizableTextAreaWrapper;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.fields.CustomField;
import de.symeda.sormas.ui.utils.fields.TextArea;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

public class SmsComponent extends CustomField<String> {

	private long missingPhoneNumbers;

	private TextArea smsTextArea;

	public SmsComponent(long missingPhoneNumbers) {
		this.missingPhoneNumbers = missingPhoneNumbers;
	}

	@Override
	protected Component initContent() {
		final VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setSpacing(false);
		mainLayout.setPadding(false);
		mainLayout.setSizeUndefined();
		mainLayout.setWidthFull();

		if (missingPhoneNumbers > 0) {
			mainLayout.add(
				VaadinUiUtil.createLabel(
					HtmlRenderer.iconHtml(VaadinIcon.INFO_CIRCLE) + " "
						+ String.format(I18nProperties.getCaption(Captions.messagesNumberOfMissingPhoneNumbers), missingPhoneNumbers),
					ContentMode.HTML));
			mainLayout.add(new Span());
		}

		mainLayout.add(new Span(I18nProperties.getString(Strings.messageEnterSms)));

		final ResizableTextAreaWrapper<TextArea> resizableTextAreaWrapper = new ResizableTextAreaWrapper<>();

		smsTextArea = new TextArea();
		smsTextArea.setWidthFull();
		smsTextArea.setRows(4);
		mainLayout.add(resizableTextAreaWrapper.wrap(smsTextArea, Captions.messagesCharacters, false));

		final Span numberOfMessagesLabel = new Span(String.format(I18nProperties.getCaption(Captions.messagesNumberOfMessages), 0));
		numberOfMessagesLabel.addClassNames(CssStyles.ALIGN_RIGHT, CssStyles.FIELD_EXTRA_INFO, CssStyles.LABEL_ITALIC);
		mainLayout.add(numberOfMessagesLabel);

		mainLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, numberOfMessagesLabel);

		// was a text change listener: the value follows every keystroke
		smsTextArea.setValueChangeMode(ValueChangeMode.EAGER);
		smsTextArea.addValueChangeListener(e -> setNumberOfMessagesLabel(numberOfMessagesLabel, smsTextArea.getValue().length()));

		return mainLayout;
	}

	private void setNumberOfMessagesLabel(Span numberOfMessagesLabel, int nrOfCharacters) {
		numberOfMessagesLabel.setText(String.format(I18nProperties.getCaption(Captions.messagesNumberOfMessages), (1 + nrOfCharacters / 160)));
	}

	@Override
	public Class<? extends String> getType() {
		return String.class;
	}

	@Override
	public String getValue() {
		return smsTextArea.getValue();
	}
}
