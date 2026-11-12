package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/**
 * migration/workflows/WF48-case-import.md. The prepared file is src/test/resources/workflows/WF48-cases.csv (built from
 * the app's own import template; synthetic persons Import01..Import09 Csvperson); its report date token is replaced with
 * the reset date before the upload. Admin has no jurisdiction, so the duplicate check only finds persons admin
 * reported: the duplicate is a second row for Import01 Csvperson, whom the first row creates. Admin's case directory is
 * empty, so the directory is checked as NatUser.
 */
@DisplayName("WF48 Import cases from CSV with progress and a duplicate prompt (admin, NatUser; C1)")
@AppCopy(AppCopy.Config.C1)
class WF48CaseImportIT extends WorkflowTest {

	private static final Pattern PROGRESS = Pattern.compile(
		"(\\d+)/(\\d+) Processed\\s*(\\d+) Imported\\s*(\\d+) Error\\(s\\)\\s*(\\d+) Skipped as Duplicates\\s*(\\d+) Skipped");
	Ui ui;
	String prepared;

	@BeforeAll
	void open() throws IOException {
		ui = newUi();
		ui.login("admin");
		try (InputStream in = getClass().getResourceAsStream("/workflows/WF48-cases.csv")) {
			prepared = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private Locator win() {
		return ui.windows().last();
	}

	/** processed, total, imported, errors, duplicates, skipped of a window text, or null. */
	private static int[] progress(String t) {
		Matcher m = PROGRESS.matcher(t);
		if (!m.find()) {
			return null;
		}
		int[] x = new int[6];
		for (int i = 0; i < 6; i++) {
			x[i] = Integer.parseInt(m.group(i + 1));
		}
		return x;
	}

	private String windowStartingWith(String prefix) {
		return ui.windowTexts().stream().filter(t -> t.startsWith(prefix)).findFirst().orElse("");
	}

	/** Polls the open windows' texts until done holds. */
	private boolean until(Predicate<List<String>> done, int ms) {
		long end = System.currentTimeMillis() + ms;
		while (System.currentTimeMillis() < end) {
			if (done.test(ui.windowTexts())) {
				return true;
			}
			ui.page.waitForTimeout(250);
		}
		return false;
	}

	@Test
	@Order(1)
	@DisplayName("1. Case directory, Import, Detailed import: the window offers the import guide, the template and the upload")
	void step1() {
		ui.openView("Cases");
		ui.clickEntry("Import");
		ui.settle(1000);
		ui.clickEntry(Pattern.compile("^detailed import$", Pattern.CASE_INSENSITIVE));
		ui.settle(2000);
		String t = ui.windowText();
		assertTrue(t.startsWith("Import Cases"), "title: " + t);
		for (String x : List.of("DOWNLOAD IMPORT GUIDE", "DOWNLOAD IMPORT TEMPLATE", "START DATA IMPORT")) {
			assertTrue(t.toUpperCase().contains(x), "offers " + x);
		}
		assertEquals(1, ui.windowFileInput().count(), "file input");
	}

	@Test
	@Order(2)
	@DisplayName("2. Download the import template: a CSV whose header is the prepared file's header")
	void step2() throws IOException {
		Download d = ui.page.waitForDownload(
			new Page.WaitForDownloadOptions().setTimeout(60_000),
			() -> win().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(Pattern.compile("download import template", Pattern.CASE_INSENSITIVE)))
				.click());
		assertTrue(d.suggestedFilename().toLowerCase().endsWith(".csv"), "csv: " + d.suggestedFilename());
		Path f = out.resolve(d.suggestedFilename());
		d.saveAs(f);
		String[] lines = Files.readString(f, StandardCharsets.UTF_8).replaceFirst("^﻿", "").split("\r?\n");
		String[] prep = prepared.split("\r?\n");
		assertTrue(
			lines[1].startsWith("\"disease\",\"diseaseVariant\"") && lines[1].contains("\"person.firstName\"") && lines[1].contains("\"reportDate\""),
			"import header");
		for (int i = 0; i < 4; i++) {
			assertEquals(prep[i], lines[i], "header line " + (i + 1) + " as in the prepared file");
		}
		ui.settle(800);
	}

	@Test
	@Order(3)
	@DisplayName("3. Upload the prepared file and start the import: a progress display counts processed rows")
	void step3() throws IOException {
		Path f = out.resolve("WF48-cases.csv");
		Files.writeString(f, prepared.replace("{{REPORT_DATE}}", en(0)), StandardCharsets.UTF_8);
		ui.windowChooseFile(f);
		ui.settle(500);
		long end = System.currentTimeMillis() + 60_000;
		while (System.currentTimeMillis() < end) {
			List<String> ts = ui.windowTexts();
			int[] x = ts.stream().map(WF48CaseImportIT::progress).filter(java.util.Objects::nonNull).findFirst().orElse(null);
			if (ts.stream().anyMatch(t -> t.startsWith("Pick or create person")) || x != null && x[0] == x[1]) {
				break;
			}
			ui.page.waitForTimeout(250);
		}
		String t = windowStartingWith("Data import");
		assertTrue(t.contains("10 rows are being imported"), "progress window: " + t);
		int[] x = progress(t);
		assertTrue(x != null && x[1] == 10, "counter of 10: " + t);
		assertTrue(x[0] < 10, "still running: waits at the duplicate row");
	}

	@Test
	@Order(4)
	@DisplayName("4. At the second Import01 Csvperson row: pick the existing person, then create a new case for her; the import continues")
	void step4() {
		assertTrue(until(ts -> ts.stream().anyMatch(t -> t.startsWith("Pick or create person")), 60_000), "person prompt");
		String t = windowStartingWith("Pick or create person");
		assertTrue(
			Pattern.compile("FIRST NAME\\s+Import01\\s+LAST NAME\\s+Csvperson[\\s\\S]*1/1/1971[\\s\\S]*SEX\\s+Female", Pattern.CASE_INSENSITIVE).matcher(t).find()
				|| Pattern.compile("First name\\s+Last name[\\s\\S]*Sex[\\s\\S]*Import01\\s+Csvperson[\\s\\S]*1/1/1971[\\s\\S]*Female", Pattern.CASE_INSENSITIVE)
					.matcher(t)
					.find(),
			"imported entry: " + t);
		assertTrue(
			Pattern.compile("Select a matching person[\\s\\S]*Import01[\\s\\S]*Csvperson[\\s\\S]*\\(1/1/1971\\)[\\s\\S]*Female", Pattern.CASE_INSENSITIVE)
				.matcher(t)
				.find(),
			"match listed");
		// "Select a matching person" comes preselected with the one match (clicking it again would deselect it)
		assertTrue(ui.windowRowSelected("Import01"), "match preselected");
		win().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save").setExact(true)).click();
		// the picked person already has a Cholera case from row 1: the app offers it as a similar case
		assertTrue(until(ts -> ts.stream().anyMatch(x -> x.startsWith("Pick or Create a Case")), 30_000), "case prompt");
		String ct = windowStartingWith("Pick or Create a Case");
		assertTrue(
			Pattern.compile("Pick an existing case .*Cholera|Pick an existing case .*Default District").matcher(ct).find() && ct.contains("Create a new case"),
			"similar case offered: " + ct);
		ui.windowClickText("Create a new case");
		ui.settle(500);
		win().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Confirm").setExact(true)).click();
		ui.settle(800);
		assertTrue(until(ts -> ts.stream().noneMatch(x -> x.startsWith("Pick or")), 15_000), "import continued");
	}

	@Test
	@Order(5)
	@DisplayName("5. The result: 10 imported, 0 errors, 0 skipped")
	void step5() {
		assertTrue(until(ts -> ts.stream().anyMatch(t -> {
			int[] x = progress(t);
			return x != null && x[0] == x[1] && t.toUpperCase().contains("CLOSE");
		}), 60_000), "finished");
		String t = windowStartingWith("Data import");
		assertTrue(t.contains("Import successful! All rows have been imported."), "successful: " + t);
		int[] x = progress(t);
		assertEquals("10/10 processed, 10 imported, 0 errors, 0 skipped as duplicates, 0 skipped",
			String.format("%d/%d processed, %d imported, %d errors, %d skipped as duplicates, %d skipped", x[0], x[1], x[2], x[3], x[4], x[5]));
		win().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close").setExact(true)).click();
		ui.settle(1200);
		if (ui.windowCount() > 0) {
			ui.closeWindow();
			ui.settle(1200);
		}
		assertEquals(0, ui.windowCount(), "windows closed");
	}

	@Test
	@Order(6)
	@DisplayName("6. As NatUser, search the case directory for Csvperson: ten Cholera cases for nine persons; Import01 has two")
	void step6() {
		ui.logout("Logout");
		ui.settle(2000);
		ui.login("NatUser");
		ui.openView("Cases");
		ui.fillSearch("personLike", "Csvperson");
		ui.applyFilters();
		ui.settle(2000);
		List<String> rows = ui.rowTexts();
		assertEquals(10, rows.size(), "cases");
		assertTrue(
			rows.stream().allMatch(r -> r.contains("Cholera") && r.contains("Default District") && r.contains("Default Facility") && r.contains(en(0))),
			"all Cholera, Default District and Facility, reported today");
		List<String> names = new ArrayList<>();
		for (String r : rows) {
			Matcher m = Pattern.compile("(Import0\\d) Csvperson").matcher(r);
			if (m.find()) {
				names.add(m.group(1));
			}
		}
		assertEquals(9, names.stream().distinct().count(), "nine persons");
		assertEquals(2, names.stream().filter("Import01"::equals).count(), "Import01's cases");
		// Data check: ten new Cholera cases, nine new persons, two cases of Import01; written by admin
		assertEquals(
			"10|9|2|admin",
			copy.sql(
				"SELECT count(*), count(DISTINCT p.id), count(*) FILTER (WHERE p.firstname = 'Import01'), string_agg(DISTINCT u.username, ',')"
					+ " FROM cases c JOIN person p ON p.id = c.person_id JOIN users u ON u.id = c.reportinguser_id"
					+ " WHERE p.lastname = 'Csvperson' AND c.disease = 'CHOLERA'"));
	}
}
