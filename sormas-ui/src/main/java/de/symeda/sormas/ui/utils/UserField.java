package de.symeda.sormas.ui.utils;

import static de.symeda.sormas.ui.utils.CssStyles.INACCESSIBLE_LABEL;
import static de.symeda.sormas.ui.utils.CssStyles.LABEL_BOLD;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserReferenceDto;
import de.symeda.sormas.ui.utils.fields.CustomField;

public class UserField extends CustomField<UserReferenceDto> {

	private ComboBoxWithPlaceholder userCombo;
	private boolean readOnly;
	private boolean enabled;
	private List<UserReferenceDto> items = new ArrayList<>();
	private Supplier<Boolean> parentPseudonymizedSupplier;

	public UserField() {
	}

	@Override
	protected Component initContent() {
		HorizontalLayout userLayout = new HorizontalLayout();

		userCombo = ComboBoxHelper.createComboBoxV7();
		userCombo.addItems(items);

		userCombo.setImmediate(true);
		if (getValue() != null) {
			userCombo.addItem(getValue());
		}
		userCombo.setNullSelectionAllowed(true);
		userCombo.setValue(getValue());
		userCombo.setEnabled(enabled);
		userCombo.setReadOnly(readOnly);
		userLayout.add(userCombo);
		final Button userContactButton = createUserContactButton();
		userLayout.add(userContactButton);
		userLayout.setWidthFull();
		userLayout.setFlexGrow(1, userCombo);
		userLayout.setSpacing(false);

		userCombo.setWidthFull();

		userCombo.addValueChangeListener(valueChangeEvent -> {
			setModelValue((UserReferenceDto) userCombo.getValue(), valueChangeEvent.isFromClient());
		});

		return userLayout;
	}

	public void addItems(List<UserReferenceDto> items) {
		this.items.addAll(items);
		if (userCombo == null) {
			userCombo = ComboBoxHelper.createComboBoxV7();
		}
		userCombo.addItems(this.items);
	}

	public void removeAllItems() {
		items.clear();
		if (userCombo != null) {
			userCombo.removeAllItems();
		}
	}

	protected Button createUserContactButton() {
		boolean isReadOnly = userCombo.isReadOnly();
		Button userContactButton = new Button("", VaadinIcon.EYE.create(), e -> {
			triggerUserContactPopUpWindow();
		});
		userContactButton.setId("userContact");
		// V8 Valo styles icon-only, borderless, large are not carried over (no CSS in stage 1a)
		userContactButton.addClassNames(
			isReadOnly ? CssStyles.HSPACE_LEFT_6 : CssStyles.HSPACE_LEFT_NONE,
			isReadOnly ? CssStyles.VSPACE_TOP_6 : CssStyles.VSPACE_NONE);
		return userContactButton;
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		if (userCombo != null) {
			userCombo.setReadOnly(readOnly);
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (userCombo != null) {
			userCombo.setEnabled(enabled);
		}
	}

	@Override
	protected void setPresentationValue(UserReferenceDto newFieldValue) {
		if (userCombo != null) {
			userCombo.setValue(newFieldValue);
		}
	}

	protected void triggerUserContactPopUpWindow() {
		UserDto userDto = null;
		if (getValue() != null) {
			userDto = FacadeProvider.getUserFacade().getByUuid(getValue().getUuid());
		}

		VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.setPadding(false);
		if (userCombo.isReadOnly()) {
			if (userDto == null) {
				verticalLayout.add(createFieldLayout(I18nProperties.getCaption(Captions.User_userName), null, null, true));
				verticalLayout.add(createFieldLayout(I18nProperties.getCaption(Captions.User_phone), null, LinkType.PHONE, true));
				verticalLayout.add(createFieldLayout(I18nProperties.getCaption(Captions.User_userEmail), null, LinkType.EMAIL, true));
			} else {
				verticalLayout.add(createFieldLayout(I18nProperties.getCaption(Captions.User_userName), userDto.getUserName(), null, false));
				verticalLayout
					.add(createFieldLayout(I18nProperties.getCaption(Captions.User_phone), userDto.getPhone(), LinkType.PHONE, false));
				verticalLayout.add(
					createFieldLayout(I18nProperties.getCaption(Captions.User_userEmail), userDto.getUserEmail(), LinkType.EMAIL, false));
			}
		} else {
			boolean parentPseudonymizedFlag = parentPseudonymizedSupplier.get();
			if (userDto == null && !parentPseudonymizedFlag) {
				HorizontalLayout labelLayout = new HorizontalLayout();
				Span noUserMessageLabel = new Span();
				final String noUserMessage;
				noUserMessage = I18nProperties.getString(Strings.messageNoUserSelected);
				noUserMessageLabel.setText(noUserMessage);
				labelLayout.add(noUserMessageLabel);
				verticalLayout.add(labelLayout);
			} else {
				verticalLayout.add(
					createFieldLayout(
						I18nProperties.getCaption(Captions.User_phone),
						userDto != null ? userDto.getPhone() : null,
						LinkType.PHONE,
						parentPseudonymizedFlag));
				verticalLayout.add(
					createFieldLayout(
						I18nProperties.getCaption(Captions.User_userEmail),
						userDto != null ? userDto.getUserEmail() : null,
						LinkType.EMAIL,
						parentPseudonymizedFlag));
			}
		}

		VaadinUiUtil.showConfirmationPopup(
			I18nProperties.getString(Strings.headingContactInformation),
			verticalLayout,
			I18nProperties.getString(Strings.close),
			null,
			640,
			confirmed -> {
				return true;
			});
	}

	private static HorizontalLayout createFieldLayout(String fieldLabelCaption, String fieldValue, LinkType linkType, boolean inaccessible) {
		HorizontalLayout fieldLayout = new HorizontalLayout();
		Span fieldLabel = new Span(fieldLabelCaption + ":");
		fieldLabel.addClassName(LABEL_BOLD);
		fieldLayout.add(fieldLabel);

		if (inaccessible) {
			Span fieldValueLabel = new Span(fieldValue != null ? fieldValue : I18nProperties.getCaption(Captions.inaccessibleValue));
			fieldValueLabel.addClassName(INACCESSIBLE_LABEL);
			fieldLayout.add(fieldValueLabel);
		} else {
			if (fieldValue == null || fieldValue.isEmpty()) {
				Span valueLabel = new Span(I18nProperties.getString(Strings.notSpecified));
				fieldLayout.add(valueLabel);
			} else {
				if (linkType != null) {
					Anchor fieldLink = new Anchor(linkType.getLinkType() + fieldValue, fieldValue);
					fieldLayout.add(fieldLink);
				} else {
					Span valueLabel = new Span(fieldValue);
					fieldLayout.add(valueLabel);
				}
			}
		}
		return fieldLayout;
	}

	public void setParentPseudonymizedSupplier(Supplier<Boolean> parentPseudonymizedSupplier) {
		this.parentPseudonymizedSupplier = parentPseudonymizedSupplier;
	}

	private enum LinkType {

		PHONE("tel:"),
		EMAIL("mailto:");

		private final String linkType;

		LinkType(String linkType) {
			this.linkType = linkType;
		}

		public String getLinkType() {
			return linkType;
		}
	}

	@Override
	public Class<? extends UserReferenceDto> getType() {
		return UserReferenceDto.class;
	}

}
