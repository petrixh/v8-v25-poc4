package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
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

/** migration/workflows/WF11-case-export-csv.md. Downloads are kept in target/e2e/WF11CaseExportCsvIT/. */
@DisplayName("WF11 Export the case list as CSV (SurvSup, C1)")
@AppCopy(AppCopy.Config.C1)
class WF11CaseExportCsvIT extends WorkflowTest {

	Ui ui;
	int listed;
	String basic;
	String detailed;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvSup");
	}

	/** Export > entry: the download is saved, the "export is being prepared" window closed. */
	private String exportAs(String entry, String file) throws IOException {
		ui.button("Export").click();
		ui.settle(500);
		Download d = ui.page.waitForDownload(new Page.WaitForDownloadOptions().setTimeout(90_000), () -> ui.clickEntry(entry));
		Path target = out.resolve(file);
		d.saveAs(target);
		ui.settle(800);
		ui.closeWindow();
		ui.settle(800);
		return Files.readString(target, StandardCharsets.UTF_8);
	}

	private static List<String> lines(String csv) {
		return Arrays.stream(csv.split("\n")).filter(l -> !l.isBlank()).toList();
	}

	private static String rowOf(String csv, String id) {
		return lines(csv).stream().filter(l -> l.contains("\"" + id + "-")).findFirst().orElse("");
	}

	@Test
	@Order(1)
	@DisplayName("1. Case directory filtered by disease COVID-19: the list shows the COVID-19 cases")
	void step1() {
		ui.openView("Cases");
		var disease = ui.filterCombo(2);
		assertEquals("Disease", Ui.promptOf(disease), "disease filter");
		ui.comboPick(disease, "COVID", "COVID-19");
		ui.applyFilters();
		ui.settle(1500);
		List<String> rows = ui.rowTexts();
		assertTrue(!rows.isEmpty() && rows.stream().allMatch(r -> r.contains("COVID-19")), "rows are COVID-19");
		Matcher m = Pattern.compile("All\\s*(\\d+)").matcher(ui.pageText());
		assertTrue(m.find(), "All counter");
		listed = Integer.parseInt(m.group(1));
		assertEquals(82, listed, "listed cases");
	}

	@Test
	@Order(2)
	@DisplayName("2. Export, Basic Export: a CSV with the basic case columns, one row per listed case")
	void step2() throws IOException {
		basic = exportAs("Basic Export", "basic.csv");
		List<String> ls = lines(basic);
		assertTrue(
			ls.get(0).contains("\"epidNumber\"") && ls.get(0).contains("\"caseClassification\"") && ls.get(0).contains("\"personLastName\""),
			"basic columns");
		assertEquals(listed, ls.size() - 2, "one row per case (two header lines)");
	}

	@Test
	@Order(3)
	@DisplayName("3. Export, Detailed Export: more columns (person, address, symptoms), the same number of rows")
	void step3() throws IOException {
		detailed = exportAs("Detailed Export", "detailed.csv");
		List<String> ls = lines(detailed);
		assertTrue(ls.get(0).split("\",\"").length > lines(basic).get(0).split("\",\"").length, "more columns");
		assertTrue(ls.get(0).contains("\"Symptoms\""), "symptoms columns");
		assertEquals(listed, ls.size() - 3, "same rows (three header lines)");
	}

	@Test
	@Order(4)
	@DisplayName("4. Both files: SCAS01 (Alpha Seedperson, Confirmed) and SCAS02 (Bravo Seedperson, Not a case)")
	void step4() {
		for (String[] f : new String[][] {
			{
				"basic",
				basic },
			{
				"detailed",
				detailed } }) {
			String a = rowOf(f[1], "SCAS01"), b = rowOf(f[1], "SCAS02");
			assertTrue(a.contains("Alpha") && a.contains("Seedperson") && a.contains("Confirmed case"), f[0] + " SCAS01");
			assertTrue(b.contains("Bravo") && b.contains("Seedperson") && b.contains("Not a case"), f[0] + " SCAS02");
		}
	}
}
