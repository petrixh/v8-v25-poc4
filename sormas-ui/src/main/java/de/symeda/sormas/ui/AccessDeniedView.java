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
package de.symeda.sormas.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.navigation.View;
import de.symeda.sormas.ui.navigation.ViewChangeListener;

/**
 * View shown when trying to navigate to a view the user does not have access to using
 * {@link de.symeda.sormas.ui.navigation.Navigator}.
 */
@SuppressWarnings("serial")
public class AccessDeniedView extends VerticalLayout implements View {

	private Span explanation;

	public AccessDeniedView() {

		setPadding(true);
		setSpacing(true);

		H1 header = new H1(I18nProperties.getString(Strings.headingAccessDenied));
		add(header);
		add(explanation = new Span());
	}

	@Override
	public void enter(ViewChangeListener.ViewChangeEvent event) {
		explanation.setText(I18nProperties.getString(Strings.errorAccessDenied));
	}
}
