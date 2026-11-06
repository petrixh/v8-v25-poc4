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

package de.symeda.sormas.ui.navigation;

/**
 * Marker for views that take path parameters after their view name ({@code cases/data/<uuid>}).
 * <p>
 * Such views are registered with a wildcard route ({@code <viewName>/:params*}); all other views only with their exact
 * view name. An unregistered sub-route such as {@code configuration/devMode} therefore does not open its parent view (the
 * old Navigator's prefix fallback, baseline item B028, is not copied).
 */
public interface HasViewParameters {
}
