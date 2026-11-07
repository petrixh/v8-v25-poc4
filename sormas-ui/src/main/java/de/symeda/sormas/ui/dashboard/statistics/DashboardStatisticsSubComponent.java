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
package de.symeda.sormas.ui.dashboard.statistics;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.ui.utils.CssStyles;

@SuppressWarnings("serial")
public class DashboardStatisticsSubComponent extends VerticalLayout {

	// Layouts
	private VerticalLayout contentLayout;

	// Components
	private Span totalCountLabel;

	public DashboardStatisticsSubComponent() {
		// was a left and right margin only (MarginInfo(false, true, false, true)): the layout margin at the sides
		this.setPadding(false);
		this.getStyle().set("padding-left", "var(--lumo-space-l)").set("padding-right", "var(--lumo-space-l)");
		this.setSpacing(false);
	}

	public void addHeader(String headline, Image icon, boolean showTotalCount) {
		HorizontalLayout headerLayout = new HorizontalLayout();
		headerLayout.setWidth(100, Unit.PERCENTAGE);
		headerLayout.setSpacing(true);
		headerLayout.setPadding(false);
		CssStyles.style(headerLayout, CssStyles.VSPACE_4);

		VerticalLayout labelAndTotalLayout = new VerticalLayout();
		{
			labelAndTotalLayout.setPadding(false);
			labelAndTotalLayout.setSpacing(false);
			Span headlineLabel = new Span(headline);
			CssStyles.style(headlineLabel, CssStyles.H2, CssStyles.VSPACE_4, CssStyles.VSPACE_TOP_NONE);
			labelAndTotalLayout.add(headlineLabel);

			if (showTotalCount) {
				totalCountLabel = new Span();
				CssStyles.style(
					totalCountLabel,
					CssStyles.LABEL_PRIMARY,
					CssStyles.LABEL_XXXLARGE,
					CssStyles.LABEL_BOLD,
					CssStyles.VSPACE_4,
					CssStyles.VSPACE_TOP_NONE);
				labelAndTotalLayout.add(totalCountLabel);
			} else {
				CssStyles.style(labelAndTotalLayout, CssStyles.VSPACE_4);
			}

		}
		headerLayout.add(labelAndTotalLayout);
		headerLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, labelAndTotalLayout);
		headerLayout.setHeight(null);
		headerLayout.setFlexGrow(1, labelAndTotalLayout);

		if (icon != null) {
			headerLayout.add(icon);
			headerLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, icon);
		}

		add(headerLayout);
		setFlexGrow(0, headerLayout);
	}

	public FlexLayout createCountLayout(boolean addMarginBottom) {
		// was a CssLayout: the count elements flow side by side and wrap
		FlexLayout countLayout = new FlexLayout();
		countLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
		countLayout.setWidth(null);
		if (addMarginBottom) {
			CssStyles.style(countLayout, CssStyles.VSPACE_3);
		}
		return countLayout;
	}

	public <C extends Component & HasSize & HasStyle> void addComponentToCountLayout(FlexLayout countLayout, C countElement) {
		countElement.setWidth(null);
		countElement.addClassName(CssStyles.HSPACE_RIGHT_3);
		countLayout.add(countElement);
	}

	public void addMainContent() {
		if (contentLayout == null) {
			contentLayout = new VerticalLayout();
			contentLayout.setPadding(false);
			contentLayout.setSpacing(false);
			contentLayout.setWidth(100, Unit.PERCENTAGE);
			contentLayout.setHeight(null);
			add(contentLayout);
			contentLayout.setHeight(null);
			setFlexGrow(1, contentLayout);
		}
	}

	public <C extends Component & HasSize> void addComponentToContent(C component) {
		if (contentLayout != null) {
			component.setWidth(100, Unit.PERCENTAGE);
			contentLayout.add(component);
		}
	}

	public <C extends Component & HasSize> void addComponentToContent(C component, FlexComponent.Alignment alignment) {
		if (contentLayout != null) {
			component.setWidth(null);
			contentLayout.add(component);
			contentLayout.setHorizontalComponentAlignment(alignment, component);
		}
	}

	public void removeAllComponentsFromContent() {
		if (contentLayout != null) {
			contentLayout.removeAll();
		}
	}

	public void removeContent() {
		if (contentLayout != null) {
			remove(contentLayout);
		}

		contentLayout = null;
	}

	public void updateCountLabel(int count) {
		totalCountLabel.setText(Integer.toString(count));
	}
}
