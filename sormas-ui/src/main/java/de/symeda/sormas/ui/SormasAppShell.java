/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2026 SORMAS Foundation gGmbH
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

package de.symeda.sormas.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Application shell: page settings that Vaadin 8 read from annotations on the UI class ({@code @Viewport} and
 * {@code @Theme("sormas")} on {@link SormasUI}).
 * <p>
 * The look: the bootstrap grid of the Vaadin 8 theme (form layouts), Lumo, and the SORMAS global theme settings on top
 * of it. The two SORMAS files live in {@code src/main/frontend/themes/sormas/}, next to the theme images; both Vaadin
 * modes serve them under {@code VAADIN/themes/sormas/} (see the war plugin in {@code pom.xml}). Order matters: the
 * grid's normalize rules come first, SORMAS last.
 */
@Viewport("user-scalable=no,initial-scale=1.0")
@StyleSheet("context://VAADIN/themes/sormas/bootstrap-grid.min.css")
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("context://VAADIN/themes/sormas/sormas.css")
public class SormasAppShell implements AppShellConfigurator {

	private static final long serialVersionUID = 1L;
}
