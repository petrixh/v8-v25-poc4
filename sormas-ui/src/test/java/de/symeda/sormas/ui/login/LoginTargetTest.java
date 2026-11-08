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
package de.symeda.sormas.ui.login;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * {@link LoginUI#toApplicationPath(String)}: the address asked for before the login comes from the login page's address,
 * so it must stay a path inside the application.
 */
public class LoginTargetTest {

	@Test
	public void testNoTargetIsTheStartPage() {
		assertEquals("", LoginUI.toApplicationPath(null));
		assertEquals("", LoginUI.toApplicationPath(""));
	}

	@Test
	public void testPathAndQueryAreKept() {
		assertEquals(
			"contacts?dateFilterOption=DATE&includeContactsFromOtherJurisdictions=false",
			LoginUI.toApplicationPath("contacts?dateFilterOption=DATE&includeContactsFromOtherJurisdictions=false"));
		assertEquals("cases/data/ABC-123", LoginUI.toApplicationPath("/cases/data/ABC-123"));
	}

	@Test
	public void testNoOtherSite() {
		assertEquals("evil.invalid/x", LoginUI.toApplicationPath("//evil.invalid/x"));
		assertEquals("evil.invalid/x", LoginUI.toApplicationPath("/\\evil.invalid/x"));
		assertEquals("evil.invalid/x", LoginUI.toApplicationPath("\\\\evil.invalid/x"));
		assertEquals("evil.invalid/x", LoginUI.toApplicationPath("/\t/evil.invalid/x"));
		assertEquals("evil.invalid/x", LoginUI.toApplicationPath(" \n//evil.invalid/x"));
	}
}
