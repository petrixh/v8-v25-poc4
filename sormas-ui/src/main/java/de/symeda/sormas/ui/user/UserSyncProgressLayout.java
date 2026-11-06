/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.ui.user;

import de.symeda.sormas.ui.utils.ContentMode;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.shared.Registration;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

/**
 * @author Alex Vidrean
 * @since 11-Dec-20
 */
@SuppressWarnings("serial")
public class UserSyncProgressLayout extends VerticalLayout {

	// Components
	private final ProgressBar progressBar;
	private final Span processedImportsLabel;
	private final Span syncSuccessLabel;
	private final Span syncErrorsLabel;
	private final Button closeCancelButton;
	private final HorizontalLayout infoLayout;
	private final Span infoLabel;

	private ProgressBar progressCircle;
	private Image errorIcon;
	private Image successIcon;
	private Image warningIcon;
	private Component currentInfoComponent;

	private final ComponentEventListener<ClickEvent<Button>> cancelListener;
	// Flow buttons remove a listener through its registration (was removeClickListener)
	private Registration cancelListenerRegistration;

	// Counts
	private int processedImportsCount;
	private int syncSuccessCount;
	private int syncErrorCount;
	private final long totalCount;

	private final UI currentUI;

	public UserSyncProgressLayout(long totalCount, UI currentUI, Runnable cancelCallback) {
		this.totalCount = totalCount;
		this.currentUI = currentUI;

		setWidthFull();
		setPadding(true);

		// Info text and icon/progress circle
		infoLayout = new HorizontalLayout();
		infoLayout.setWidthFull();
		infoLayout.setSpacing(true);
		initializeInfoComponents();
		currentInfoComponent = progressCircle;
		infoLayout.add(currentInfoComponent);
		infoLabel = VaadinUiUtil.createLabel(String.format(I18nProperties.getString(Strings.infoUserSyncProcess), totalCount), ContentMode.HTML);
		infoLayout.add(infoLabel);
		infoLayout.setFlexGrow(1, infoLabel);

		add(infoLayout);

		// Progress bar
		progressBar = new ProgressBar(0, 1, 0);
		CssStyles.style(progressBar, CssStyles.VSPACE_TOP_3);
		add(progressBar);
		progressBar.setWidthFull();

		// Progress info
		HorizontalLayout progressInfoLayout = new HorizontalLayout();
		CssStyles.style(progressInfoLayout, CssStyles.VSPACE_TOP_5);
		progressInfoLayout.setSpacing(true);
		processedImportsLabel = new Span(String.format(I18nProperties.getCaption(Captions.syncProcessed), 0, totalCount));
		progressInfoLayout.add(processedImportsLabel);
		syncSuccessLabel = new Span(String.format(I18nProperties.getCaption(Captions.syncSuccessful), 0));
		CssStyles.style(syncSuccessLabel, CssStyles.LABEL_POSITIVE);
		progressInfoLayout.add(syncSuccessLabel);
		syncErrorsLabel = new Span(String.format(I18nProperties.getCaption(Captions.syncErrors), 0));
		CssStyles.style(syncErrorsLabel, CssStyles.LABEL_CRITICAL);
		progressInfoLayout.add(syncErrorsLabel);

		add(progressInfoLayout);
		setHorizontalComponentAlignment(FlexComponent.Alignment.END, progressInfoLayout);

		// Cancel button
		cancelListener = e -> cancelCallback.run();

		closeCancelButton = ButtonHelper.createButton(Captions.actionCancel, null, CssStyles.VSPACE_TOP_2);
		cancelListenerRegistration = closeCancelButton.addClickListener(cancelListener);

		add(closeCancelButton);
		setHorizontalComponentAlignment(FlexComponent.Alignment.END, closeCancelButton);
	}

	private void initializeInfoComponents() {
		progressCircle = new ProgressBar();
		progressCircle.setIndeterminate(true);
		CssStyles.style(progressCircle, "v-progressbar-indeterminate-large");

		// was a ThemeResource of the sormas theme
		errorIcon = new Image(VaadinUiUtil.THEME_IMAGE_PATH + "img/error-icon.png", "");
		errorIcon.setHeight(35, Unit.PIXELS);
		errorIcon.setWidth(35, Unit.PIXELS);
		successIcon = new Image(VaadinUiUtil.THEME_IMAGE_PATH + "img/success-icon.png", "");
		successIcon.setHeight(35, Unit.PIXELS);
		successIcon.setWidth(35, Unit.PIXELS);
		warningIcon = new Image(VaadinUiUtil.THEME_IMAGE_PATH + "img/warning-icon.png", "");
		warningIcon.setHeight(35, Unit.PIXELS);
		warningIcon.setWidth(35, Unit.PIXELS);
	}

	public void updateProgress(SyncResult result) {
		currentUI.access(() -> {
			processedImportsCount++;
			if (result == SyncResult.SUCCESS) {
				syncSuccessLabel.setText(String.format(I18nProperties.getCaption(Captions.syncSuccessful), ++syncSuccessCount));
			} else if (result == SyncResult.ERROR) {
				syncErrorsLabel.setText(String.format(I18nProperties.getCaption(Captions.syncErrors), ++syncErrorCount));
			}
			processedImportsLabel.setText(String.format(I18nProperties.getCaption(Captions.syncProcessed), processedImportsCount, totalCount));
			progressBar.setValue((float) processedImportsCount / (float) totalCount);
		});
	}

	public void makeClosable(Runnable closeCallback) {
		closeCancelButton.setText(I18nProperties.getCaption(Captions.actionClose));
		cancelListenerRegistration.remove();
		closeCancelButton.addClickListener(e -> closeCallback.run());
	}

	public void setInfoLabelText(String text) {
		VaadinUiUtil.setLabelValue(infoLabel, text, ContentMode.HTML);
	}

	public void displayErrorIcon() {
		infoLayout.remove(currentInfoComponent);
		currentInfoComponent = errorIcon;
		infoLayout.addComponentAsFirst(currentInfoComponent);
	}

	public void displaySuccessIcon() {
		infoLayout.remove(currentInfoComponent);
		currentInfoComponent = successIcon;
		infoLayout.addComponentAsFirst(currentInfoComponent);
	}

	public void displayWarningIcon() {
		infoLayout.remove(currentInfoComponent);
		currentInfoComponent = warningIcon;
		infoLayout.addComponentAsFirst(currentInfoComponent);
	}

	public enum SyncResult {
		SUCCESS,
		ERROR
	}
}
