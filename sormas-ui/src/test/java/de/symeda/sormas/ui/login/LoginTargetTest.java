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
	public void testRoutesAreKept() {
		assertEquals(
			"contacts?dateFilterOption=DATE&includeContactsFromOtherJurisdictions=false",
			LoginUI.toApplicationPath("contacts?dateFilterOption=DATE&includeContactsFromOtherJurisdictions=false"));
		// deep link of the task mail (WF49)
		assertEquals("cases/data/RJ5ZKL-TZ5UTO-TUKUJL-3JUDKMIO", LoginUI.toApplicationPath("cases/data/RJ5ZKL-TZ5UTO-TUKUJL-3JUDKMIO"));
		assertEquals("cases", LoginUI.toApplicationPath("cases"));
		assertEquals("campaigns/campaigndata?campaign=ABC-123", LoginUI.toApplicationPath("campaigns/campaigndata?campaign=ABC-123"));
		// query values as the login check keeps them (percent-encoded); a query cannot change the path
		assertEquals("cases?searchText=John%20Doe", LoginUI.toApplicationPath("cases?searchText=John%20Doe"));
		assertEquals("cases?x=../../metrics", LoginUI.toApplicationPath("cases?x=../../metrics"));
		// browsers drop tabs and line breaks in addresses
		assertEquals("cases/data/ABC-123", LoginUI.toApplicationPath("cases/\tdata/ABC-123\n"));
	}

	@Test
	public void testNoOtherSite() {
		assertStartPage("//evil.invalid/x");
		assertStartPage("/\\evil.invalid/x");
		assertStartPage("\\\\evil.invalid/x");
		assertStartPage("/\t/evil.invalid/x");
		assertStartPage(" \n//evil.invalid/x");
		assertStartPage("https://evil.invalid/x");
		assertStartPage("https:evil.invalid");
		assertStartPage("javascript:alert(1)");
		assertStartPage("@evil.invalid");
	}

	@Test
	public void testNoOtherPathOnThisServer() {
		assertStartPage("../../metrics");
		assertStartPage("../metrics");
		assertStartPage("..");
		assertStartPage("cases/../../metrics");
		assertStartPage("cases/./data");
		assertStartPage("./cases");
		assertStartPage("cases/../../metrics?x=1");
		assertStartPage("..\\..\\metrics");
		assertStartPage("cases/..;/..;/metrics");
		assertStartPage("cases//data");
		assertStartPage("/cases/data/ABC-123");
		assertStartPage("/metrics");
		assertStartPage(".\t./metrics");
	}

	@Test
	public void testNoEncodedVariants() {
		assertStartPage("%2e%2e/%2e%2e/metrics");
		assertStartPage("%2E%2E%2F%2E%2E%2Fmetrics");
		assertStartPage("..%2f..%2fmetrics");
		assertStartPage("..%5c..%5cmetrics");
		assertStartPage("%252e%252e/metrics");
		assertStartPage("%2f%2fevil.invalid");
		assertStartPage("cases%2f..%2f..%2fmetrics");
		assertStartPage("%c0%ae%c0%ae/metrics");
		// full-width dots and slash
		assertStartPage("\uff0e\uff0e\uff0fmetrics");
		assertStartPage("cases#../../metrics");
	}

	private static void assertStartPage(String target) {
		assertEquals("", LoginUI.toApplicationPath(target), target);
	}
}
