package de.symeda.sormas.ui.caze.messaging;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.manualmessagelog.ManualMessageLogDto;
import de.symeda.sormas.api.user.UserReferenceDto;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;

public class SmsListEntry extends HorizontalLayout {

	public SmsListEntry(ManualMessageLogDto manualMessageLogDto) {

		setPadding(false);
		setSpacing(true);
		setWidthFull();
		addClassName(CssStyles.SORMAS_LIST_ENTRY);

		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setWidthFull();
		mainLayout.setPadding(false);
		mainLayout.setSpacing(false);
		add(mainLayout);
		setFlexGrow(1, mainLayout);

		HorizontalLayout topLayout = new HorizontalLayout();
		topLayout.setWidthFull();
		topLayout.setPadding(false);
		topLayout.setSpacing(false);
		mainLayout.add(topLayout);

		VerticalLayout topLeftLayout = new VerticalLayout();
		{
			topLeftLayout.setPadding(false);
			topLeftLayout.setSpacing(false);
			final UserReferenceDto sendingUser = manualMessageLogDto.getSendingUser();
			final Span sendingInfo = new Span(
				I18nProperties.getCaption(Captions.messagesSentBy) + ": " + sendingUser.getFirstName() + " " + sendingUser.getLastName() + " "
					+ DateFormatHelper.formatLocalDateTime(manualMessageLogDto.getSentDate()));
			topLeftLayout.add(sendingInfo);
		}
		topLayout.add(topLeftLayout);
	}
}
