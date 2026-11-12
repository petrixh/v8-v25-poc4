package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Page;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Docx;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF26-about-data-dictionary.md */
@DisplayName("WF26 About page: version, data dictionary and classification rules (SurvOff, C1)")
@AppCopy(AppCopy.Config.C1)
class WF26AboutDataDictionaryIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvOff");
	}

	private Download download(String id) {
		return ui.page.waitForDownload(new Page.WaitForDownloadOptions().setTimeout(30_000), () -> ui.clickId(id));
	}

	@Test
	@Order(1)
	@DisplayName("1. Open About: the page shows the SORMAS version (1.104.0) and the documents section")
	void step1() {
		ui.openView("About");
		ui.waitText("SORMAS Version", 10_000);
		String t = ui.pageText();
		assertTrue(t.contains("SORMAS Version: 1.104.0"), "version");
		assertTrue(t.contains("Documents") && t.contains("Data Dictionary (XLSX)") && t.contains("Case Classification Rules (HTML)"), "documents");
	}

	@Test
	@Order(2)
	@DisplayName("2. Download the data dictionary: an XLSX workbook with one sheet per entity (case, contact, person ...)")
	void step2() throws IOException {
		Download d = download("aboutDataDictionary");
		assertTrue(d.suggestedFilename().endsWith(".xlsx"), "is xlsx: " + d.suggestedFilename());
		Path f = out.resolve("data-dictionary.xlsx");
		d.saveAs(f);
		List<String> sheets = Docx.sheetNames(f);
		for (String s : List.of("Person", "Case", "Contact", "Sample", "Event", "Task", "Immunization", "User")) {
			assertTrue(sheets.contains(s), "sheet " + s + " in " + sheets);
		}
	}

	@Test
	@Order(3)
	@DisplayName("3. Open the case classification rules: an HTML document with the classification criteria per disease downloads")
	void step3() throws IOException {
		Download d = download("aboutCaseClassificationRules");
		assertTrue(d.suggestedFilename().endsWith(".html"), "is html: " + d.suggestedFilename());
		Path f = out.resolve("classification-rules.html");
		d.saveAs(f);
		String html = Files.readString(f, StandardCharsets.UTF_8);
		assertTrue(html.toLowerCase().contains("<html"), "html document");
		for (String x : List.of("Measles", "Cholera", "COVID-19")) {
			assertTrue(html.contains(x), "rules for " + x);
		}
		assertEquals(true, Files.size(f) > 1024, "not empty");
	}
}
