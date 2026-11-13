package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.DirtiesAppMemory;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF44-geography-archive.md */
@DisplayName("WF44 Continents, subcontinents and countries; archive and de-archive a country (admin, C1)")
@DirtiesAppMemory("archives a country: infrastructure cache")
@AppCopy(AppCopy.Config.C1)
class WF44GeographyArchiveIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("admin");
	}

	private void cfgTab(String name) {
		ui.openView("Configuration");
		ui.clickEntry(name);
		ui.settle(1200);
	}

	private void chooseStatus(Pattern re, String typed) {
		ui.comboPick(ui.lastCombo(), typed, re, true, 600, 1000);
	}

	private void search(String text) {
		ui.searchEnter("search", text);
		ui.settle(1000);
	}

	private List<String> finland() {
		return ui.rowTexts().stream().filter(r -> r.contains("Finland")).toList();
	}

	private void confirmYes() {
		ui.windowButtonMatching(Pattern.compile("^yes$", Pattern.CASE_INSENSITIVE));
		ui.noModal();
		ui.settle(800);
	}

	@Test
	@Order(1)
	@DisplayName("1. Configuration, continents: six listed; subcontinents: 27 listed with their continent")
	void step1() {
		cfgTab("Continents");
		ui.waitRow("Africa", 10_000);
		List<String> c = ui.rowTexts();
		assertEquals(6, c.size(), "continents: " + c);
		assertTrue(c.stream().anyMatch(r -> r.contains("Africa")), "Africa listed");
		cfgTab("Subcontinents");
		ui.waitRow("Northern Europe", 10_000);
		List<String> s = ui.rowTexts();
		assertEquals(27, s.size(), "subcontinents");
		assertTrue(s.stream().anyMatch(r -> r.contains("Northern Europe") && r.replace("Northern Europe", "").contains("Europe")), "Northern Europe with its continent");
	}

	@Test
	@Order(2)
	@DisplayName("2. Countries, search \"Fin\": Finland is listed with its ISO code and subcontinent")
	void step2() {
		cfgTab("Countries");
		search("Fin");
		ui.waitRow("Finland", 10_000);
		List<String> f = finland();
		assertEquals(1, f.size(), "Finland listed");
		assertTrue(f.get(0).contains("FIN"), "ISO code");
		assertTrue(f.get(0).contains("Northern Europe"), "subcontinent");
	}

	@Test
	@Order(3)
	@DisplayName("3. Archive Finland: it leaves the active list; the archived list shows it")
	void step3() {
		ui.clickRow(ui.rowIndex("Finland"));
		ui.waitText("Archive", 10_000);
		ui.windowButton("Archive");
		ui.settle(600);
		confirmYes();
		assertTrue(ui.waitNoRow("Finland", 10_000), "left the active list");
		chooseStatus(Pattern.compile("archived countries", Pattern.CASE_INSENSITIVE), "Archived");
		search("Fin");
		assertTrue(ui.waitRow("Finland", 10_000), "in the archived list");
		assertEquals(1, finland().size(), "Finland archived once");
	}

	@Test
	@Order(4)
	@DisplayName("4. De-archive Finland: it is back in the active list; Finland ends active")
	void step4() {
		ui.clickRow(ui.rowIndex("Finland"));
		ui.waitText("De-Archive", 10_000);
		ui.windowButton("De-Archive");
		ui.settle(600);
		confirmYes();
		assertTrue(ui.waitNoRow("Finland", 10_000), "left the archived list");
		chooseStatus(Pattern.compile("^active countries", Pattern.CASE_INSENSITIVE), "Active");
		search("Fin");
		assertTrue(ui.waitRow("Finland", 10_000), "back in the active list");
		assertEquals(1, finland().size(), "Finland active once");
		// Data check: Finland ends active
		assertEquals("f", copy.sql("SELECT archived FROM country WHERE isocode = 'FIN'"));
	}
}
