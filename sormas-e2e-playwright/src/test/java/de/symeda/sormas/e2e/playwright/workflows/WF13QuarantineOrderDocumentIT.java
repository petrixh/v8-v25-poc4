package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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
import de.symeda.sormas.e2e.playwright.support.Docx;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/**
 * migration/workflows/WF13-quarantine-order-document.md. The example template and the generated quarantine order are
 * kept in target/e2e/WF13QuarantineOrderDocumentIT/.
 */
@DisplayName("WF13 Upload a document template and generate a quarantine order (admin, SurvSup; C1)")
@AppCopy(AppCopy.Config.C1)
class WF13QuarantineOrderDocumentIT extends WorkflowTest {

	private static final String TEMPLATE = "ExampleDocumentTemplateCases.docx";
	Ui ui;
	Path example;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("admin");
	}

	private Locator win() {
		return ui.windows().last();
	}

	private Locator winButton(Pattern name) {
		return win().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name));
	}

	/** The case section of the document templates page: from its heading to the next section's heading. */
	private String caseSectionText() {
		String t = ui.pageText();
		int a = t.indexOf("Document Templates Case"), b = t.indexOf("Document Templates Contact");
		return a < 0 ? "" : t.substring(a, b > a ? b : t.length());
	}

	@Test
	@Order(1)
	@DisplayName("1. As admin, Configuration, Document templates: the sections per document type, no case template")
	void step1() {
		ui.openView("Configuration");
		ui.clickEntry(Pattern.compile("^document templates$", Pattern.CASE_INSENSITIVE));
		ui.settle(2000);
		String t = ui.pageText();
		assertTrue(t.contains("Document Templates Management"), "title");
		for (String s : List.of(
			"Document Templates Case",
			"Document Templates Contact",
			"Document Templates Event Participant",
			"Document Templates Travel Entry",
			"Templates Event Handout")) {
			assertTrue(t.contains(s), "section " + s);
		}
		assertFalse(caseSectionText().toLowerCase().contains(".docx"), "no case template listed");
	}

	@Test
	@Order(2)
	@DisplayName("2. Case templates upload window, download the example template for cases: a .docx file")
	void step2() throws IOException {
		ui.button(Pattern.compile("upload template", Pattern.CASE_INSENSITIVE)).click();
		ui.settle(2000);
		assertTrue(ui.windowHasText("Upload Document Templates Case"), "window title");
		Download d = ui.page.waitForDownload(
			new Page.WaitForDownloadOptions().setTimeout(60_000),
			() -> winButton(Pattern.compile("example template cases", Pattern.CASE_INSENSITIVE)).click());
		assertTrue(d.suggestedFilename().toLowerCase().endsWith(".docx"), "docx: " + d.suggestedFilename());
		example = out.resolve(TEMPLATE);
		d.saveAs(example);
		assertEquals("Example Document Template for Cases", Docx.paragraphs(example).get(0), "example title");
		ui.settle(800);
	}

	@Test
	@Order(3)
	@DisplayName("3. Upload that file as a case quarantine order template: listed under the case templates")
	void step3() {
		ui.windowChooseFile(example);
		ui.settle(3300);
		assertTrue(ui.waitText("Upload successful", 10_000), "upload successful");
		winButton(Pattern.compile("^okay$", Pattern.CASE_INSENSITIVE)).click();
		ui.settle(1000);
		if (ui.windowCount() > 0) {
			ui.closeWindow();
			ui.settle(1500);
		}
		assertEquals(0, ui.windowCount(), "windows closed");
		assertTrue(caseSectionText().contains(TEMPLATE), "listed in the case section");
	}

	@Test
	@Order(4)
	@DisplayName("4. Log out, log in as SurvSup, open SCAS01: the side panel offers to create a quarantine order")
	void step4() {
		ui.logout("Logout");
		ui.settle(2000);
		assertTrue(ui.loginVisible(), "logged out");
		ui.login("SurvSup");
		ui.openView("Cases");
		ui.fillSearch("caseLike", "SCAS01");
		ui.applyFilters();
		ui.settle(1500);
		ui.clickGridText("SCAS01");
		ui.settle(2500);
		String t = ui.pageText();
		assertTrue(t.contains("Alpha SEEDPERSON") && t.contains("(SCAS01)"), "case opened");
		assertTrue(Pattern.compile("Document Templates\\W*Quarantine Order\\W*Create", Pattern.CASE_INSENSITIVE).matcher(t).find(), "quarantine order offered");
	}

	@Test
	@Order(5)
	@DisplayName("5. Create a quarantine order with the uploaded template: a .docx file downloads")
	void step5() {
		ui.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("Create$"))).last().click();
		ui.settle(2000);
		assertTrue(ui.windowHasText("Create Quarantine Order"), "window title");
		Locator tpl = ui.comboAfterCaption(Pattern.compile("^quarantine order$", Pattern.CASE_INSENSITIVE));
		ui.comboPick(tpl, "Example", TEMPLATE, false, 600, 2500);
		assertEquals(TEMPLATE, tpl.inputValue(), "template chosen");
		Download d = ui.page.waitForDownload(new Page.WaitForDownloadOptions().setTimeout(60_000), () -> winButton(Pattern.compile("Create$")).click());
		assertTrue(d.suggestedFilename().toLowerCase().endsWith(".docx"), "docx: " + d.suggestedFilename());
		d.saveAs(out.resolve("quarantine-order.docx"));
		ui.settle(1000);
		if (ui.windowCount() > 0) { // the create window stays open after the download
			winButton(Pattern.compile("^cancel$", Pattern.CASE_INSENSITIVE)).click();
			ui.settle(1000);
		}
	}

	@Test
	@Order(6)
	@DisplayName("6. The document names Alpha Seedperson and shows dates in the app's date format")
	void step6() throws IOException {
		List<String> paras = Docx.paragraphs(out.resolve("quarantine-order.docx"));
		assertTrue(paras.contains("Alpha Seedperson"), "person");
		assertTrue(paras.contains("COVID-19"), "disease");
		assertTrue(paras.contains("10/26/2026"), "SCAS01's seed report date, M/D/YYYY");
		assertTrue(paras.contains("10/27/2026 - Nasopharyngeal swab: Positive"), "sample");
		assertTrue(paras.contains("10/28/2026 - PCR / RT-PCR: Positive"), "pathogen test");
		assertTrue(paras.contains("Surveillance Supervisor"), "current user");
		assertFalse(paras.stream().anyMatch(x -> x.contains("$")), "no placeholder left");
		// Data check: one case quarantine order template stored (a file in the app's custom directory)
		String files = copy.appExec("find", "/opt/sormas/custom/docgeneration", "-name", "*.docx").trim();
		assertEquals(1, files.lines().count(), "templates stored: " + files);
		assertTrue(files.contains("quarantine") && files.endsWith(TEMPLATE), "case quarantine order template: " + files);
	}
}
