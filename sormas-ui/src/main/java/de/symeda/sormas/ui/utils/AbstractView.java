/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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
 *******************************************************************************/
package de.symeda.sormas.ui.utils;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AttachmentType;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.server.streams.DownloadHandler;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.criteria.BaseCriteria;
import de.symeda.sormas.ui.SormasUI;
import de.symeda.sormas.ui.navigation.Navigator;
import de.symeda.sormas.ui.navigation.View;

public abstract class AbstractView extends VerticalLayout implements View {

	private static final long serialVersionUID = -1L;

	protected final String viewName;
	private final HorizontalLayout viewHeader;
	private final VerticalLayout viewTitleLayout;
	private final Span viewTitleLabel;
	private final Span viewSubTitleLabel;

	protected boolean applyingCriteria;

	protected AbstractView(String viewName) {
		this.viewName = viewName;

		setSizeFull();
		setPadding(false);
		setSpacing(false);

		viewHeader = new HorizontalLayout();
		viewHeader.setWidthFull();
		viewHeader.setHeight(null);
		// was a left and right margin only (MarginInfo); spacing details belong to the theme work
		viewHeader.setPadding(false);
		viewHeader.setSpacing(true);
		CssStyles.style(viewHeader, "view-header");

		viewTitleLayout = new VerticalLayout();
		{
			viewTitleLayout.setSizeUndefined();
			viewTitleLayout.setSpacing(false);
			viewTitleLayout.setPadding(false);

			// note: splitting title and subtitle into labels does not work with the css
			String viewTitle = I18nProperties.getPrefixCaption("View", viewName.replace("/", "."));
			String viewSubTitle = I18nProperties.getPrefixCaption("View", viewName.replace("/", ".") + ".sub", "");
			viewTitleLabel = new Span(viewTitle);
			viewTitleLabel.setSizeUndefined();
			CssStyles.style(viewTitleLabel, CssStyles.H1, CssStyles.VSPACE_NONE);
			viewTitleLayout.add(viewTitleLabel);

			viewSubTitleLabel = new Span(viewSubTitle);
			viewSubTitleLabel.setSizeUndefined();
			CssStyles.style(viewSubTitleLabel, CssStyles.H4, CssStyles.VSPACE_TOP_NONE);
			viewTitleLayout.add(viewSubTitleLabel);
		}

		viewHeader.add(viewTitleLayout);
		viewHeader.setFlexGrow(1, viewTitleLayout);

		addComponent(viewHeader);
		setFlexGrow(0, viewHeader);
	}

	protected void addHeaderComponent(Component c) {
		viewHeader.add(c);
		// was Alignment.MIDDLE_RIGHT: the title layout grows, so header components sit on the right
		viewHeader.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, c);
	}

	protected void setMainHeaderComponent(Component c) {
		viewHeader.remove(viewTitleLayout);
		viewHeader.addComponentAsFirst(c);
		viewHeader.setFlexGrow(1, c);
	}

	/**
	 * Adds a component that takes the remaining height (Vaadin 8: expand ratio 1 by default). Subclasses keep calling
	 * this instead of {@code add(..)} where the old code called {@code addComponent(..)}.
	 */
	public void addComponent(Component c) {
		add(c);
		// set expansion to 1 by default
		setFlexGrow(1, c);
	}

	public Span getViewTitleLabel() {
		return viewTitleLabel;
	}

	public Span getViewSubTitleLabel() {
		return viewSubTitleLabel;
	}

	public boolean navigateTo(BaseCriteria... criteriaList) {
		return navigateTo(true, criteriaList);
	}

	public boolean navigateTo(boolean force, BaseCriteria... criteriaList) {
		if (applyingCriteria) {
			return false;
		}
		applyingCriteria = true;

		Navigator navigator = SormasUI.get().getNavigator();

		String state = navigator.getState();
		String newState = buildNavigationState(state, criteriaList);

		boolean didNavigate = false;
		if (!newState.equals(state) || force) {
			navigator.navigateTo(newState);

			didNavigate = true;
		}
		applyingCriteria = false;

		return didNavigate;
	}

	public boolean navigateTo(BaseCriteria criteria, boolean force) {
		if (applyingCriteria) {
			return false;
		}
		applyingCriteria = true;

		Navigator navigator = SormasUI.get().getNavigator();

		String state = navigator.getState();
		String newState = buildNavigationState(state, criteria);

		boolean didNavigate = false;
		if (!newState.equals(state) || force) {
			navigator.navigateTo(newState);

			didNavigate = true;
		}
		applyingCriteria = false;

		return didNavigate;
	}

	public static String buildNavigationState(String currentState, BaseCriteria... criteriaList) {

		String newState = currentState;
		int paramsIndex = newState.lastIndexOf('?');
		if (paramsIndex >= 0) {
			newState = newState.substring(0, paramsIndex);
		}

		if (criteriaList != null)
		{
			String urlParams = Arrays.stream(criteriaList)
					.filter(Objects::nonNull)
					.map(BaseCriteria::toUrlParams)
					.filter(params -> !DataHelper.isNullOrEmpty(params))
					.collect(Collectors.joining("&"));

			if (urlParams.length() > 0) {
				if (newState.charAt(newState.length() - 1) != '/') {
					newState += "/";
				}
				newState += "?" + urlParams;
			}
		}

		return newState;
	}

	public void setApplyingCriteria(boolean applyingCriteria) {
		this.applyingCriteria = applyingCriteria;
	}

	protected void addExportButton(
		DownloadHandler streamResource,
		Popover exportPopupButton,
		VerticalLayout exportLayout,
		VaadinIcon icon,
		String captionKey,
		String descriptionKey) {

		// was a FileDownloader extending the button: Flow downloads through a link around it
		Anchor exportLink = new Anchor();
		exportLink.setHref(streamResource, AttachmentType.DOWNLOAD);
		exportLink.setWidthFull();

		Button exportButton = ButtonHelper.createIconButton(captionKey, icon, e -> {

			Button button = e.getSource();
			int buttonPos = exportLayout.indexOf(exportLink);

			// the link has to remain in the UI for the download to succeed
			DownloadUtil.showExportWaitDialog(exportLink, ce -> {
				//restore the button
				exportLayout.addComponentAtIndex(buttonPos, exportLink);
				button.setEnabled(true);
			});
			exportPopupButton.close();
		});
		exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		exportButton.setDisableOnClick(true);
		exportButton.setTooltipText(I18nProperties.getDescription(descriptionKey));
		exportButton.setWidthFull();

		exportLink.add(exportButton);
		exportLayout.add(exportLink);
	}

	/**
	 * Iterates through the prefixes to determines the caption for the specified propertyId.
	 *
	 * @return
	 */
	protected static String findPrefixCaption(String propertyId, String... prefixes) {

		return Arrays.stream(prefixes)
			.map(p -> I18nProperties.getPrefixCaption(p, propertyId, null))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(propertyId);
	}

	protected String createFileNameWithCurrentDate(ExportEntityName entityName, String fileExtension) {
		return DownloadUtil.createFileNameWithCurrentDate(entityName, fileExtension);
	}
}
