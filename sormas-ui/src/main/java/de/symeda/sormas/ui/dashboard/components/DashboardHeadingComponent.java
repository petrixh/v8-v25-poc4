/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2023 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.dashboard.components;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.shared.Tooltip;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.utils.CssStyles;

public class DashboardHeadingComponent extends HorizontalLayout {

	private Span totalLabel;
	private Span titleLabel;
	private Span infoIcon;

	private String[] titleStyleNames = new String[] {
		"heading-title-label",
		CssStyles.H2,
		CssStyles.HSPACE_LEFT_4 };

	private String[] totalLabelStyleNames = new String[] {
		CssStyles.LABEL_PRIMARY,
		CssStyles.LABEL_XXXLARGE,
		CssStyles.LABEL_BOLD,
		CssStyles.VSPACE_3,
		CssStyles.VSPACE_TOP_NONE };

	public DashboardHeadingComponent(String titleCaption, String infoIconText) {
		setSpacing(false);

		// count
		totalLabel = new Span();
		CssStyles.style(totalLabel, totalLabelStyleNames);
		add(totalLabel);
		// title
		titleLabel = new Span(I18nProperties.getCaption(titleCaption));
		CssStyles.style(titleLabel, titleStyleNames);
		add(titleLabel);

		if (StringUtils.isNotBlank(infoIconText)) {
			infoIcon = new Span(VaadinIcon.INFO_CIRCLE.create());
			CssStyles.style(infoIcon, CssStyles.LABEL_LARGE, CssStyles.LABEL_SECONDARY, "statistics-info-label", CssStyles.HSPACE_LEFT_4);
			// was setDescription(infoIconText, ContentMode.HTML): the tooltip renders the HTML through Markdown
			Tooltip.forComponent(infoIcon).setMarkdown(infoIconText);
			add(infoIcon);
		}
	}

	public void updateTotalLabel(String value) {
		totalLabel.setText(value);
	}

	public void setTotalLabelDescription(String description) {
		totalLabel.setTitle(I18nProperties.getDescription(description));
	}

	public Span getTitleLabel() {
		return titleLabel;
	}

	public Span getTotalLabel() {
		return totalLabel;
	}

	public void setTitleStyleNames(String[] titleStyleNames) {
		this.titleStyleNames = titleStyleNames;
	}

	public String[] getTitleStyleNames() {
		return titleStyleNames;
	}

	public String[] getTotalLabelStyleNames() {
		return totalLabelStyleNames;
	}
}
