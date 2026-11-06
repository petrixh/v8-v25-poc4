package de.symeda.sormas.ui.contact.contactlink;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.contact.ContactCategory;
import de.symeda.sormas.api.contact.ContactDto;
import de.symeda.sormas.api.contact.ContactListEntryDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentField;

public class ContactListEntry extends SideComponentField {

	private static final long serialVersionUID = -122890470232134412L;
	public static final String SEPARATOR = ": ";
	private final ContactListEntryDto contactListEntryDto;

	public ContactListEntry(ContactListEntryDto contactListEntryDto) {
		this.contactListEntryDto = contactListEntryDto;

		HorizontalLayout uuidStatusLayout = new HorizontalLayout();
		uuidStatusLayout.setPadding(false);
		uuidStatusLayout.setSpacing(true);

		Span contactUuidLabel = new Span(DataHelper.toStringNullable(DataHelper.getShortUuid(contactListEntryDto.getUuid())));
		contactUuidLabel.setTitle(contactListEntryDto.getUuid());

		Span statusLabel = new Span(contactListEntryDto.getContactStatus().toString());
		statusLabel.addClassNames(CssStyles.LABEL_BOLD);
		statusLabel.setTitle(contactListEntryDto.getContactStatus().toString());

		uuidStatusLayout.add(contactUuidLabel);
		uuidStatusLayout.add(statusLabel);
		uuidStatusLayout.setWidthFull();
		// was each label aligned left and right in its half
		uuidStatusLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		uuidStatusLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		addComponentToField(uuidStatusLayout);

		HorizontalLayout diseaseClassificationLayout = new HorizontalLayout();
		diseaseClassificationLayout.setPadding(false);
		diseaseClassificationLayout.setSpacing(true);

		Span diseaseLabel = new Span(contactListEntryDto.getDisease().toString());
		diseaseLabel.addClassNames(CssStyles.LABEL_BOLD);
		diseaseLabel.setTitle(contactListEntryDto.getDisease().toString());

		Span classificationLabel = new Span(contactListEntryDto.getContactClassification().toString());
		classificationLabel.addClassNames(CssStyles.LABEL_BOLD);
		classificationLabel.setTitle(contactListEntryDto.getContactClassification().toString());

		diseaseClassificationLayout.add(diseaseLabel);
		diseaseClassificationLayout.add(classificationLabel);
		diseaseClassificationLayout.setWidthFull();
		// was each label aligned left and right in its half
		diseaseClassificationLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		diseaseClassificationLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		addComponentToField(diseaseClassificationLayout);

		if (contactListEntryDto.getLastContactDate() != null) {
			HorizontalLayout lastContactDateLayout = new HorizontalLayout();
			lastContactDateLayout.setPadding(false);
			lastContactDateLayout.setSpacing(true);

			Span lastContactDateLabel = new Span(
				I18nProperties.getPrefixCaption(ContactDto.I18N_PREFIX, ContactDto.LAST_CONTACT_DATE)
					+ SEPARATOR
					+ DateFormatHelper.formatDate(contactListEntryDto.getLastContactDate()));
			lastContactDateLayout.add(lastContactDateLabel);

			lastContactDateLayout.setWidthFull();
			lastContactDateLayout.setAlignItems(FlexComponent.Alignment.CENTER);
			addComponentToField(lastContactDateLayout);
		}

		HorizontalLayout reportDateLayout = new HorizontalLayout();
		reportDateLayout.setPadding(false);
		reportDateLayout.setSpacing(true);

		Span reportDateLabel = new Span(
			I18nProperties.getPrefixCaption(ContactDto.I18N_PREFIX, ContactDto.REPORT_DATE_TIME)
				+ SEPARATOR
				+ DateFormatHelper.formatDate(contactListEntryDto.getReportDate()));
		reportDateLayout.add(reportDateLabel);

		reportDateLayout.setWidthFull();
		reportDateLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		addComponentToField(reportDateLayout);

		final ContactCategory contactCategory = contactListEntryDto.getContactCategory();
		if (contactCategory != null) {
			Span categoryLabel = new Span(contactCategory.toString());
			if (ContactCategory.HIGH_RISK == contactCategory || ContactCategory.HIGH_RISK_MED == contactCategory) {
				categoryLabel.addClassName(CssStyles.LABEL_IMPORTANT);
			} else {
				categoryLabel.addClassName(CssStyles.LABEL_NEUTRAL);
			}
			categoryLabel.setTitle(contactCategory.toString());
			addComponentToField(categoryLabel);
		}
	}

	public ContactListEntryDto getContactListEntryDto() {
		return contactListEntryDto;
	}
}
