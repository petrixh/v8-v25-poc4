package de.symeda.sormas.ui.user;

import static de.symeda.sormas.ui.utils.LayoutUtil.loc;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.validator.RegexpValidator;

import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.fields.TextField;

import de.symeda.sormas.api.AuthProvider;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.utils.AbstractEditForm;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class UserSettingsForm extends AbstractEditForm<UserDto> {

	private static final long serialVersionUID = -928337100277917699L;

	private static final String CROWDIN_INFO_LOC = "crowdinInfoLoc";
	/** The pattern of the Vaadin 7 {@code EmailValidator} (as 1a-45, {@code EventDataForm}). */
	private static final String V7_EMAIL_PATTERN = "^([a-zA-Z0-9_\\.\\-+])+@[a-zA-Z0-9-.]+\\.[a-zA-Z0-9-]{2,}$";

	private static final String HTML_LAYOUT =
		loc(UserDto.LANGUAGE) + loc(UserDto.USER_EMAIL) + loc(UserDto.UUID) + loc(CROWDIN_INFO_LOC);

	private TextField emailTf;

	public UserSettingsForm() {
		super(UserDto.class, UserDto.I18N_PREFIX);
		setWidth(480, Unit.PIXELS);
	}

	@Override
	protected void addFields() {

		ComboBox cbLanguage = addField(UserDto.LANGUAGE, ComboBox.class);
		CssStyles.style(cbLanguage, CssStyles.COMBO_BOX_WITH_FLAG_ICON);
		ControllerProvider.getUserController().setFlagIcons(cbLanguage);

		String authenticationProvider = FacadeProvider.getConfigFacade().getAuthenticationProvider();
		if (AuthProvider.KEYCLOAK.equals(authenticationProvider)) {
			emailTf = addField(UserDto.USER_EMAIL, TextField.class);
			emailTf.setCaption(I18nProperties.getCaption(Captions.User_userEmail));
			// was the Vaadin 7 EmailValidator: same pattern, and an empty value is valid as in Vaadin 7
			emailTf.addValidator(
				new RegexpValidator(I18nProperties.getValidationError(Validations.validEmailAddress, emailTf.getCaption()), V7_EMAIL_PATTERN) {

					@Override
					protected boolean isValid(String value) {
						return value == null || value.isEmpty() || super.isValid(value);
					}
				});
		}

		TextField tfUuid = addField(UserDto.UUID, TextField.class);
		tfUuid.setReadOnly(true);
		tfUuid.setCaption(I18nProperties.getCaption(Captions.userMyUserId));

		Span crowdinInfoLabel = VaadinUiUtil.createLabel(I18nProperties.getString(Strings.infoCrowdinLocalization), ContentMode.HTML);
		crowdinInfoLabel.setWidthFull();
		CssStyles.style(crowdinInfoLabel, CssStyles.VSPACE_TOP_3, CssStyles.VSPACE_3);
		getContent().addComponent(crowdinInfoLabel, CROWDIN_INFO_LOC);
	}

	@Override
	protected String createHtmlLayout() {
		return HTML_LAYOUT;
	}

	@Override
	public void setValue(UserDto userDto) {

		super.setValue(userDto);
		if (emailTf != null) {
			emailTf.setValue(userDto.getUserEmail());
		}
	}
}
