package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Page;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF46-configuration-lists.md */
@DisplayName("WF46 Browse the configuration lists (admin, C1)")
@AppCopy(AppCopy.Config.C1)
class WF46ConfigurationListsIT extends WorkflowTest {

	Ui ui;
	String countsAtStart;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("admin");
		countsAtStart = counts();
	}

	/** What "nothing changed" compares: row counts of the configuration tables this workflow looks at. */
	private String counts() {
		return copy.sql(
			"SELECT (SELECT count(*) FROM region) || '|' || (SELECT count(*) FROM pointofentry) || '|' || (SELECT count(*) FROM featureconfiguration)"
				+ " || '|' || (SELECT count(*) FROM populationdata) || '|' || (SELECT count(*) FROM customizableenumvalue)");
	}

	private void tab(String name, String title) {
		ui.clickEntry(Pattern.compile("^" + name + "$", Pattern.CASE_INSENSITIVE));
		if (title != null) {
			ui.page.getByText(title, new Page.GetByTextOptions().setExact(true)).first().waitFor();
		}
		ui.settle(800);
	}

	private boolean hasButton(String regex) {
		return ui.entryCount(Pattern.compile(regex, Pattern.CASE_INSENSITIVE)) > 0;
	}

	@Test
	@Order(1)
	@DisplayName("1. Configuration, regions: the Default region is listed; points of entry: the Default Point Of Entry is listed")
	void step1() {
		ui.openView("Configuration");
		tab("Regions", null);
		ui.waitRow("Default Region", 10_000);
		List<String> rows = ui.rowTexts();
		assertEquals(1, rows.size(), "regions");
		assertTrue(rows.get(0).contains("Default Region") && rows.get(0).contains("DEF-REG"), "Default region: " + rows.get(0));
		tab("Points of entry", null);
		ui.waitRow("Default Point Of Entry", 10_000);
		rows = ui.rowTexts();
		assertEquals(1, rows.size(), "points of entry");
		assertTrue(rows.get(0).contains("Default Point Of Entry") && rows.get(0).contains("Airport") && rows.get(0).contains("Default District"), "Default PoE: " + rows.get(0));
	}

	@Test
	@Order(2)
	@DisplayName("2. Population data: the page offers import and export; export: a CSV downloads")
	void step2() throws IOException {
		tab("Population", "Population Data");
		assertTrue(hasButton("import"), "import button");
		assertTrue(hasButton("export"), "export button");
		Download d = ui.page.waitForDownload(new Page.WaitForDownloadOptions().setTimeout(30_000), () -> ui.clickEntry(Pattern.compile("export", Pattern.CASE_INSENSITIVE)));
		assertTrue(d.suggestedFilename().endsWith(".csv"), "csv: " + d.suggestedFilename());
		Path f = out.resolve("population-data.csv");
		d.saveAs(f);
		String head = Files.readAllLines(f, StandardCharsets.UTF_8).get(0);
		assertTrue(!head.isEmpty(), "csv has content");
	}

	@Test
	@Order(3)
	@DisplayName("3. Feature configuration: the features offered (the two pathogen test options) are listed with their state, both on")
	void step3() {
		tab("Features", "Feature Configuration");
		assertEquals(
			"Allow adding pathogen tests from a sample = on | Require a result when entering a pathogen test = on",
			String.join(" | ", ui.checkboxStates()),
			"features");
	}

	@Test
	@Order(4)
	@DisplayName("4. System configuration: configuration values are listed and can be filtered by text")
	void step4() {
		ui.openView("Configuration");
		tab("System configuration", null);
		ui.settle(800);
		List<String> all = ui.rowTexts();
		assertEquals(9, all.size(), "values listed");
		ui.searchEnter("search", "EMAIL");
		for (int i = 0; i < 40 && ui.rowTexts().size() >= all.size(); i++) {
			ui.page.waitForTimeout(250);
		}
		List<String> f = ui.rowTexts();
		assertTrue(f.size() > 0 && f.size() < all.size() && f.stream().allMatch(r -> Pattern.compile("email", Pattern.CASE_INSENSITIVE).matcher(r).find()), "filtered: " + f.size() + " of " + all.size());
	}

	@Test
	@Order(5)
	@DisplayName("5. Customizable fields: the page lists the customizable field entries (none in the default configuration); nothing changed")
	void step5() {
		tab("Customizable fields", null);
		ui.settle(800);
		assertEquals(0, ui.rowTexts().size(), "no entries");
		assertTrue(hasButton("create"), "create button");
		assertEquals(0, ui.windowsAfterClose(), "no window");
		// Data check: nothing changed
		assertEquals(countsAtStart, counts(), "configuration table counts");
	}
}
