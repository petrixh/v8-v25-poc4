/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2021 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.ui.login;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.CustomLayout;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

/**
 * UI content when the user is not logged in yet.
 */
@SuppressWarnings("serial")
public class LoginScreen extends Div {

	private static final String UTF_8 = "UTF-8";
	private static final int LOGO_WIDTH = 220;
	private static final int CUSTOM_BRANDING_LOGO_WIDTH = 150;
	private static final String THEME_IMG = VaadinUiUtil.THEME_IMAGE_PATH + "img/";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private LoginListener loginListener;
	private boolean isCustomBranding;

	public LoginScreen(LoginListener loginListener) {
		this.loginListener = loginListener;
		this.isCustomBranding = FacadeProvider.getConfigFacade().isCustomBranding();
		buildUI();
	}

	private void buildUI() {
		addClassName(CssStyles.LOGINSCREEN_BACK);

		if (isCustomBranding && FacadeProvider.getConfigFacade().getLoginBackgroundPath() != null) {
			// was a page style rule for .login-screen-back; set on the element itself now
			getStyle().set("background-image", "url(" + FacadeProvider.getConfigFacade().getLoginBackgroundPath() + ")");
		}
		Div layout = new Div();
		layout.addClassName(CssStyles.LOGINSCREEN);
		add(layout);

		layout.add(buildLoginLayout());

		// custom html layout
		if (!isCustomBranding || FacadeProvider.getConfigFacade().isUseLoginSidebar()) {
			Component loginSidebarLayout = buildLoginSidebarLayout();
			layout.add(loginSidebarLayout);
		}
	}

	private Component buildLoginLayout() {

		Path customHtmlDirectory = Paths.get(FacadeProvider.getConfigFacade().getCustomFilesPath());
		Path filePath = customHtmlDirectory.resolve("loginmain.html");

		String html;
		try {
			byte[] encoded = Files.readAllBytes(filePath);
			html = new String(encoded, UTF_8);
		} catch (IOException e) {
			html = "";
		}

		Component loginLayout = buildLogin();

		if (html.contains("location=\"login\"")) {
			CustomLayout loginDetailsLayout = new CustomLayout();
			loginDetailsLayout.addClassName(CssStyles.LOGINDEATILS);
			loginDetailsLayout.setWidthFull();

			loginDetailsLayout.setTemplateContents(html);
			loginDetailsLayout.addComponent(loginLayout, "login");
			return loginDetailsLayout;
		} else {
			logger.warn("Location for login is missing in loginmain.html");
			return loginLayout;
		}
	}

	private Component buildLogin() {

		// login form, centered in the available part of the screen
		VerticalLayout loginFormLayout = new VerticalLayout();
		CssStyles.style(loginFormLayout, CssStyles.LOGINFORM, CssStyles.LAYOUT_SPACIOUS);
		loginFormLayout.setPadding(true);
		loginFormLayout.setSpacing(false);

		// header of the menu
		final HorizontalLayout titleLayout = new HorizontalLayout();
		titleLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		titleLayout.setSpacing(false);
		Span title = new Span(FacadeProvider.getConfigFacade().getSormasInstanceName());
		CssStyles.style(title, CssStyles.H1, CssStyles.VSPACE_NONE, CssStyles.VSPACE_TOP_NONE, CssStyles.HSPACE_LEFT_3);

		Image image;
		if (isCustomBranding && StringUtils.isNotBlank(FacadeProvider.getConfigFacade().getCustomBrandingLogoPath())) {
			Path logoPath = Paths.get(FacadeProvider.getConfigFacade().getCustomBrandingLogoPath());
			image = new Image(DownloadHandler.forFile(logoPath.toFile()), "");
			image.setWidth("50px");
		} else {
			image = new Image(THEME_IMG + "sormas-logo.png", "");
		}

		titleLayout.add(image);
		titleLayout.add(title);
		loginFormLayout.add(titleLayout);

		// the "Login" header (Captions.Login_login) is the title of the login form
		Component loginForm = buildLoginForm();
		loginFormLayout.add(loginForm);
		loginFormLayout.add(buildLoginDetails());

		// layout to center login form when there is sufficient screen space
		// - see the theme for how this is made responsive for various screen
		// sizes
		Div loginLayout = new Div();
		loginLayout.setClassName(CssStyles.LOGINFORM_CONTAINER);
		loginLayout.add(loginFormLayout);

		return loginLayout;
	}

	private Component buildLoginForm() {

		// was the Vaadin 8 LoginForm (a real form, so that browsers offer to save the password)
		LoginI18n i18n = LoginI18n.createDefault();
		i18n.getForm().setTitle(I18nProperties.getCaption(Captions.Login_login));
		i18n.getForm().setUsername(I18nProperties.getCaption(Captions.Login_username));
		i18n.getForm().setPassword(I18nProperties.getCaption(Captions.Login_password));
		i18n.getForm().setSubmit(I18nProperties.getCaption(Captions.Login_doLogIn));

		// the default constructor disables the form on submit on the server too, so setEnabled(true) below reaches the browser
		LoginForm loginForm = new LoginForm();
		loginForm.setI18n(i18n);
		loginForm.setForgotPasswordButtonVisible(false);
		loginForm.setId(Captions.Login_doLogIn);

		loginForm.addLoginListener(event -> {
			login(loginForm, event.getUsername().trim(), event.getPassword());
		});

		loginForm.getElement().getStyle().set("width", "240px");

		return loginForm;
	}

	private Div buildLoginDetails() {

		Div loginDetailsLayout = new Div();
		loginDetailsLayout.setClassName(CssStyles.LOGINDEATILS);
		loginDetailsLayout.setWidthFull();

		Path customHtmlDirectory = Paths.get(FacadeProvider.getConfigFacade().getCustomFilesPath());
		Path filePath = customHtmlDirectory.resolve("logindetails.html");

		String html;
		try {
			byte[] encoded = Files.readAllBytes(filePath);
			html = new String(encoded, UTF_8);
		} catch (IOException e) {
			html = "";
		}

		Span htmlLabel = VaadinUiUtil.createLabel(html, ContentMode.HTML);
		htmlLabel.setWidthFull();

		loginDetailsLayout.add(htmlLabel);
		return loginDetailsLayout;
	}

	private Component buildLoginSidebarLayout() {

		Div loginSidebarLayout = new Div();
		CssStyles.style(loginSidebarLayout, CssStyles.LOGINSIDEBAR);

		VerticalLayout sidebarHeaderLayout = new VerticalLayout();
		CssStyles.style(sidebarHeaderLayout, CssStyles.LAYOUT_SPACIOUS);
		sidebarHeaderLayout.setSizeUndefined();
		sidebarHeaderLayout.setSpacing(false);

		Path customHtmlDirectory = Paths.get(FacadeProvider.getConfigFacade().getCustomFilesPath());

		if (isCustomBranding) {
			Path sidebarHeaderPath = customHtmlDirectory.resolve("loginsidebar-header.html");
			String html;
			CssStyles.style(sidebarHeaderLayout, CssStyles.LOGINSIDEBAR_CUSTOM_HEADER);
			try {
				byte[] encoded = Files.readAllBytes(sidebarHeaderPath);
				html = new String(encoded, StandardCharsets.UTF_8);
			} catch (IOException e) {
				html = "";
			}

			sidebarHeaderLayout.add(VaadinUiUtil.createLabel(html, ContentMode.HTML));
		} else {
			Image img = new Image(THEME_IMG + "sormas-logo-big-text.png", "");
			img.setWidth("200px");
			sidebarHeaderLayout.add(img);
			sidebarHeaderLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, img);

			Span fullNameText =
				VaadinUiUtil.createLabel("Surveillance,<br>Outbreak Response Management<br>and Analysis System<br>", ContentMode.HTML);
			fullNameText.setWidth("320px");
			CssStyles.style(fullNameText, CssStyles.H2, CssStyles.LABEL_PRIMARY, CssStyles.VSPACE_TOP_NONE, CssStyles.ALIGN_CENTER);
			sidebarHeaderLayout.add(fullNameText);
			sidebarHeaderLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, fullNameText);

			Span missionText = VaadinUiUtil.createLabel(
				"• " + I18nProperties.getCaption(Captions.LoginSidebar_diseasePrevention) + "<br>• "
					+ I18nProperties.getCaption(Captions.LoginSidebar_diseaseDetection) + "<br>• "
					+ I18nProperties.getCaption(Captions.LoginSidebar_outbreakResponse),
				ContentMode.HTML);
			missionText.setWidth("320px");
			CssStyles.style(missionText, CssStyles.H3, CssStyles.VSPACE_TOP_NONE, CssStyles.ALIGN_CENTER);
			sidebarHeaderLayout.add(missionText);
			sidebarHeaderLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, missionText);
		}

		loginSidebarLayout.add(sidebarHeaderLayout);

		Span poweredByLabel = new Span(I18nProperties.getCaption(Captions.LoginSidebar_poweredBy));
		CssStyles.style(poweredByLabel, CssStyles.LOGIN_HEADLINELABEL, CssStyles.H2, CssStyles.VSPACE_NONE);
		loginSidebarLayout.add(poweredByLabel);

		// was a GridLayout of one row: one or two logos, centered
		HorizontalLayout poweredByLayout = new HorizontalLayout();
		CssStyles.style(poweredByLayout, CssStyles.LOGIN_LOGOCONTAINER);
		poweredByLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		poweredByLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
		poweredByLayout.setWidthFull();
		poweredByLayout.setSpacing(true);

		if (isCustomBranding) {
			CssStyles.style(poweredByLayout, CssStyles.VSPACE_TOP_3);

			Image imgSormas = new Image(THEME_IMG + "sormas-logo-horizontal.png", "");
			imgSormas.setWidth((isCustomBranding ? CUSTOM_BRANDING_LOGO_WIDTH : LOGO_WIDTH) + "px");
			CssStyles.style(imgSormas, CssStyles.VSPACE_2);
			poweredByLayout.add(imgSormas);
		} else {
			CssStyles.style(poweredByLayout, CssStyles.VSPACE_TOP_1);
		}

		Image imgSormasFoundation = new Image(THEME_IMG + "sormas-foundation-logo.png", "");
		imgSormasFoundation.setWidth((isCustomBranding ? CUSTOM_BRANDING_LOGO_WIDTH : LOGO_WIDTH) + "px");
		poweredByLayout.add(imgSormasFoundation);

		loginSidebarLayout.add(poweredByLayout);

		Path sidebarPath = customHtmlDirectory.resolve("loginsidebar.html");
		String sidebarHtml;
		try {
			byte[] encoded = Files.readAllBytes(sidebarPath);
			sidebarHtml = new String(encoded, StandardCharsets.UTF_8);
		} catch (IOException e) {
			sidebarHtml = "";
		}

		loginSidebarLayout.add(VaadinUiUtil.createLabel(sidebarHtml, ContentMode.HTML));
		return loginSidebarLayout;
	}

	private void login(LoginForm loginForm, String username, String password) {
		if (LoginHelper.login(username, password)) {
			if (LoginHelper.hasApplicationRight()) {
				loginListener.loginSuccessful();
			} else {
				showNotification(I18nProperties.getString(Strings.headingLoginFailed), I18nProperties.getString(Strings.errorNoAccessToWeb));
			}

		} else {
			showNotification(I18nProperties.getString(Strings.headingLoginFailed), I18nProperties.getString(Strings.messageLoginFailed));
		}
		// Flow's login form disables itself on submit; the Vaadin 8 form stayed usable
		loginForm.setEnabled(true);
	}

	private void showNotification(String caption, String description) {
		Notification notification = NotificationHelper.createNotification(caption, description, NotificationHelper.Type.WARNING_MESSAGE, false);
		// keep the notification visible a little while after moving the
		// mouse, or until clicked
		NotificationHelper.setDelayMsec(notification, 2000);
		notification.open();
	}

	public interface LoginListener extends Serializable {

		void loginSuccessful();
	}
}
