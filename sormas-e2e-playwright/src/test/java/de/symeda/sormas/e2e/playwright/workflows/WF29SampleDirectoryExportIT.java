package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF29-sample-directory-export.md */
@DisplayName("WF29 Sample directory: filters and CSV export (LabOff, C1)")
@AppCopy(AppCopy.Config.C1)
class WF29SampleDirectoryExportIT extends WorkflowTest {

	Ui ui;
	int total;
	int labCount;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("LabOff");
	}

	private void pick(String label, String text) {
		Locator input = ui.comboByPrompt(label);
		assertTrue(input != null, "no filter " + label);
		ui.comboPick(input, text, text, true, 500, 800);
	}

	private int counter() {
		return ui.numberIn("All\\s*(\\d+)");
	}

	private void apply() {
		ui.applyFilters();
		ui.settle(1200);
	}

	@Test
	@Order(1)
	@DisplayName("1. Open the sample directory: samples are listed with lab, material, received status and result")
	void step1() {
		ui.openView("Samples");
		ui.waitText("Sample Directory", 10_000);
		String t = ui.pageText();
		assertTrue(t.contains("Sample Directory"), "title");
		total = counter();
		assertTrue(total > 0, "listed");
		String r = ui.rowTextWith("SSAM01");
		for (String x : List.of("Default Laboratory", "Nasopharyngeal swab", "Positive")) {
			assertTrue(r.contains(x), "SSAM01 row has " + x + ": " + r);
		}
		for (String x : List.of("LABORATORY", "TYPE OF SAMPLE", "RECEIVED", "FINAL LABORATORY RESULT")) {
			assertTrue(t.contains(x), "column " + x);
		}
	}

	@Test
	@Order(2)
	@DisplayName("2. Filter by result Positive and search for \"Alpha\": SSAM01 remains with result Positive")
	void step2() {
		pick("Test result", "Positive");
		ui.fillSearch("caseCodeIdLike", "Alpha");
		apply();
		List<String> rows = ui.rowTexts();
		assertEquals(1, rows.size(), "rows: " + rows);
		assertTrue(rows.get(0).contains("SSAM01") && rows.get(0).contains("Positive"), "SSAM01 Positive");
		assertEquals(1, counter(), "counter");
	}

	@Test
	@Order(3)
	@DisplayName("3. Clear the search and filter by laboratory Default Laboratory: the count shown matches the rows listed")
	void step3() {
		ui.resetFilters();
		ui.settle(1200);
		assertEquals(total, counter(), "reset");
		pick("Laboratory", "Default Laboratory");
		apply();
		labCount = counter();
		ui.gridScrollEnd();
		ui.settle(800);
		Set<String> seen = new HashSet<>();
		for (String r : ui.rowTexts()) {
			seen.add(r.split(" ")[0]);
		}
		ui.gridScrollTop();
		ui.settle(600);
		for (String r : ui.rowTexts()) {
			seen.add(r.split(" ")[0]);
		}
		assertTrue(ui.rowTexts().stream().allMatch(r -> r.contains("Default Laboratory")), "all in Default Laboratory");
		assertEquals(labCount, seen.size(), "count matches rows");
	}

	@Test
	@Order(4)
	@DisplayName("4. Basic export: a CSV downloads with one row per listed sample")
	void step4() throws IOException {
		ui.button("Export").click();
		ui.settle(400);
		Download d = ui.page.waitForDownload(new Page.WaitForDownloadOptions().setTimeout(90_000), () -> ui.clickEntry("Basic Export"));
		Path target = out.resolve("samples.csv");
		d.saveAs(target);
		ui.settle(500);
		if (ui.windowCount() > 0) {
			ui.closeWindow();
			ui.settle(500);
		}
		List<String> lines = Files.readAllLines(target, StandardCharsets.UTF_8).stream().filter(l -> !l.isBlank()).toList();
		assertEquals(labCount, lines.size() - 2, "one row per sample (two header lines)");
		assertTrue(lines.stream().anyMatch(l -> l.contains("SSAM01")), "SSAM01 in the file");
	}
}
