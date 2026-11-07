package de.symeda.sormas.ui.caze.messaging;

import java.util.Arrays;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseReferenceDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.messaging.MessageType;
import de.symeda.sormas.api.person.PersonReferenceDto;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class SmsListComponent extends VerticalLayout {

	private SmsList list;
	private Button sendSmsButton;
	private boolean hasPhoneNumber;
	private final boolean isEditAllowed;

	public SmsListComponent(CaseReferenceDto caseRef, PersonReferenceDto personRef, boolean isEditAllowed) {
		this.isEditAllowed = isEditAllowed;
		long missingPhoneNumbers =
			FacadeProvider.getCaseFacade().countCasesWithMissingContactInformation(Arrays.asList(caseRef.getUuid()), MessageType.SMS);
		hasPhoneNumber = missingPhoneNumbers == 0;
		createSmsListComponent(new SmsList(personRef, hasPhoneNumber), e -> {
			final SmsComponent smsComponent = new SmsComponent(missingPhoneNumbers);
			VaadinUiUtil.showConfirmationPopup(
				I18nProperties.getCaption(Captions.messagesSendingSms),
				smsComponent,
				I18nProperties.getCaption(Captions.actionSend),
				I18nProperties.getCaption(Captions.actionCancel),
				640,
				confirmationEvent -> {
					if (confirmationEvent.booleanValue()) {
						FacadeProvider.getCaseFacade().sendMessage(Arrays.asList(caseRef.getUuid()), "", smsComponent.getValue(), MessageType.SMS);
						NotificationHelper.show(null, I18nProperties.getString(Strings.notificationSmsSent), NotificationHelper.Type.TRAY_NOTIFICATION);
					}
				});
		});

	}

	private void createSmsListComponent(SmsList smsList, ComponentEventListener<ClickEvent<Button>> clickListener) {
		setWidthFull();
		setPadding(false);
		setSpacing(false);

		HorizontalLayout componentHeader = new HorizontalLayout();
		componentHeader.setPadding(false);
		componentHeader.setSpacing(false);
		componentHeader.setWidthFull();
		add(componentHeader);

		list = smsList;
		add(list);
		list.reload();

		Span smsHeader = new Span(I18nProperties.getCaption(Captions.messagesSms));
		smsHeader.addClassName(CssStyles.H3);
		componentHeader.add(smsHeader);

		if (isEditAllowed) {
			sendSmsButton = ButtonHelper.createButton(I18nProperties.getCaption(Captions.messagesSendSMS));
			CssStyles.style(sendSmsButton, CssStyles.BUTTON_PRIMARY);
			sendSmsButton.addClickListener(clickListener);
			componentHeader.add(sendSmsButton);
			// was right-aligned in its half of the header
			componentHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
			componentHeader.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, sendSmsButton);
			if (!hasPhoneNumber) {
				sendSmsButton.setEnabled(false);
			}
		}
	}
}
