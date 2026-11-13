package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Page;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF34-database-export.md */
@DisplayName("WF34 Database export of selected data sets as ZIP (NatUser, C1)")
@AppCopy(AppCopy.Config.C1)
class WF34DatabaseExportIT extends WorkflowTest {

	Ui ui;
	Path zip;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("NatUser");
		zip = out.resolve("export.zip");
	}

	@Test
	@Order(1)
	@DisplayName("1. Open Statistics, database export: the exportable data sets are shown with select-all options")
	void step1() {
		ui.openView("Statistics");
		ui.tab(Pattern.compile("database export", Pattern.CASE_INSENSITIVE)).click();
		assertTrue(ui.waitText("Database export", 15_000), "title");
		ui.settle(800);
		String t = ui.pageText();
		for (String x : List.of("Select all", "Select all SORMAS data", "Deselect all")) {
			assertTrue(t.contains(x), "select option " + x);
		}
		for (String x : List.of("SORMAS DATA", "INFRASTRUCTURE DATA", "CONFIGURATION DATA")) {
			assertTrue(t.toUpperCase().contains(x), "group " + x);
		}
		for (String x : List.of("Cases", "Contacts", "Persons", "Samples", "Events")) {
			assertTrue(t.contains(x), "data set " + x);
		}
		assertEquals(0, ui.checkedCount(false), "none checked at the start");
	}

	@Test
	@Order(2)
	@DisplayName("2. Select cases, contacts and persons only and export: a ZIP downloads")
	void step2() {
		for (String n : List.of("Cases", "Contacts", "Persons")) {
			ui.checkboxInput(n, false).check(new com.microsoft.playwright.Locator.CheckOptions().setForce(true));
		}
		assertEquals(3, ui.checkedCount(false), "three checked");
		Download d = ui.page.waitForDownload(new Page.WaitForDownloadOptions().setTimeout(120_000), () -> ui.button("Export").click());
		assertTrue(d.suggestedFilename().endsWith(".zip"), "zip: " + d.suggestedFilename());
		d.saveAs(zip);
		assertEquals(true, zip.toFile().length() > 100, "not empty");
	}

	@Test
	@Order(3)
	@DisplayName("3. Open the ZIP: one CSV per selected data set; the cases file includes the four seed cases")
	void step3() throws IOException {
		// The archive is only read in memory (names and the cases file), never unpacked to disk.
		Map<String, String> files = new LinkedHashMap<>();
		try (ZipFile z = new ZipFile(zip.toFile())) {
			List<ZipEntry> entries = new ArrayList<>(z.stream().toList());
			for (ZipEntry e : entries) {
				if (e.getName().toLowerCase().matches("^cases\\.csv$")) {
					files.put(e.getName(), new String(z.getInputStream(e).readAllBytes(), StandardCharsets.UTF_8));
				} else {
					files.put(e.getName(), "");
				}
			}
		}
		List<String> names = new ArrayList<>(files.keySet());
		assertTrue(names.stream().anyMatch(f -> f.matches("(?i)^cases\\.csv$")), "cases file: " + names);
		assertTrue(names.stream().anyMatch(f -> f.matches("(?i)^contacts\\.csv$")), "contacts file: " + names);
		assertTrue(names.stream().anyMatch(f -> f.matches("(?i)^person.*\\.csv$")), "persons file: " + names);
		assertEquals(3, names.size(), "only the selected: " + names);
		String cases = files.entrySet().stream().filter(e -> e.getKey().matches("(?i)^cases\\.csv$")).findFirst().get().getValue();
		for (String id : List.of("SCAS01", "SCAS02", "SCAS03", "SCAS04")) {
			assertTrue(cases.contains(id), "seed case " + id);
		}
	}
}
