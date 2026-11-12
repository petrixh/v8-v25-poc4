package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/**
 * migration/workflows/WF60-self-report-processing.md. The prepared file is src/test/resources/workflows/WF60-self-reports.csv
 * (report date 05/11/2026, fixed). Self report captions are English (B036), the rest German.
 */
@DisplayName("WF60 Import self reports and process one into a case (SurvSup, C2)")
@AppCopy(AppCopy.Config.C2)
class WF60SelfReportProcessingIT extends WorkflowTest {

	private static final String SR_DATE = "05.11.2026";

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.loginDe("SurvSup");
	}

	private Locator inWin(String regex) {
		return ui.windowFieldLike(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
	}

	private Locator onPage(String regex) {
		return ui.field(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
	}

	private String body() {
		return ui.bodyText().replaceAll("\\s+", " ");
	}

	private Locator save() {
		return ui.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Speichern").setExact(true)).locator("visible=true").first();
	}

	private String rowOf(String name) {
		return ui.rowTextWith(name);
	}

	@Test
	@Order(1)
	@DisplayName("1. Open Self Reports: only the seeded self report (Juliett Seedperson) is listed")
	void step1() {
		ui.openView("Self Reports");
		assertTrue(ui.waitText("Self reports directory", 15_000), "title");
		ui.waitRow("Juliett", 10_000);
		assertTrue(Pattern.compile("Alle ?1 ?In progress").matcher(body()).find(), "all count");
		List<String> r0 = ui.rowTexts();
		assertTrue(r0.size() == 1 && r0.get(0).contains("Juliett"), "seed only: " + r0);
	}

	@Test
	@Order(2)
	@DisplayName("2. Import the prepared file: 2 of 2 processed, 2 imported, 0 errors; both are listed as unprocessed next to the seeded one")
	void step2() {
		ui.clickEntry(Pattern.compile("^import$", Pattern.CASE_INSENSITIVE));
		ui.waitText("Import Self Reports", 10_000);
		assertTrue(ui.windowText().contains("Import Self Reports"), "window title");
		ui.windowChooseFile(resourceFile("WF60-self-reports.csv"));
		String res = ui.importResult(60_000);
		assertTrue(res.contains("Import erfolgreich"), "success: " + res);
		assertTrue(Pattern.compile("2/2 verarbeitet ?2 importiert ?0 Fehler").matcher(res).find(), "counts: " + res);
		ui.windowButtonLike(Pattern.compile("schlie(ss|ß)en", Pattern.CASE_INSENSITIVE));
		ui.noModal();
		if (ui.windowCount() > 0) {
			ui.closeWindow();
			ui.noModal();
		}
		ui.waitRow("Yankee", 15_000);
		assertEquals(3, ui.rowTexts().size(), "rows");
		assertTrue(rowOf("Yankee").contains(SR_DATE + " CASE COVID-19 Yankee Selbst 15.6.1980 Männlich Voreingestellter Landkreis") && rowOf("Yankee").contains("Unprocessed"), "Yankee: " + rowOf("Yankee"));
		assertTrue(rowOf("Zulu").contains(SR_DATE + " CASE COVID-19 Zulu Selbst 3.2.1990 Weiblich Voreingestellter Landkreis") && rowOf("Zulu").contains("Unprocessed"), "Zulu: " + rowOf("Zulu"));
	}

	@Test
	@Order(3)
	@DisplayName("3. Open Yankee Selbst: the self report data are shown (investigation status Pending)")
	void step3() {
		ui.clickRow(ui.rowIndex("Yankee"));
		assertTrue(ui.waitMatch("COVID-19 Pending Yankee SELBST \\(", 20_000), "view title");
		assertEquals("Yankee", onPage("^first name").inputValue(), "first name");
		assertEquals("Selbst", onPage("^last name").inputValue(), "last name");
		assertEquals("COVID-19", onPage("^disease").inputValue(), "disease");
		assertEquals("Männlich", onPage("^sex").inputValue(), "sex");
		assertEquals(SR_DATE, onPage("^report date").inputValue(), "report date");
		assertEquals("Teststrasse", onPage("^stra(ss|ß)e").inputValue(), "street");
		assertEquals("+49 30 1234567", onPage("^phone number").inputValue(), "phone");
	}

	@Test
	@Order(4)
	@DisplayName("4. Set the investigation status Completed, save, process it from the list: the new-case window opens prefilled; choose region and district, save: a case opens for Yankee Selbst")
	void step4() {
		ui.optionAfterCaption(Pattern.compile("^investigation status", Pattern.CASE_INSENSITIVE), "COMPLETED");
		save().click();
		String n = ui.newestNotificationText(12_000);
		assertTrue(n != null && n.contains("Self report saved"), "saved: " + n);
		ui.settle(800);
		ui.dismissNotifications();
		ui.clickEntry(Pattern.compile("self reports list", Pattern.CASE_INSENSITIVE));
		ui.waitRow("Yankee", 15_000);
		int row = ui.rowIndex("Yankee");
		assertTrue(Pattern.compile("PROCESS$", Pattern.CASE_INSENSITIVE).matcher(ui.rowTexts().get(row).trim()).find(), "process offered");
		ui.clickRowButton(row, "Process");
		ui.waitText("Neuen Fall erstellen", 15_000);
		assertTrue(ui.windowText().contains("Neuen Fall erstellen"), "window title");
		assertEquals("Yankee", inWin("^vorname").inputValue(), "first name");
		assertEquals("Selbst", inWin("^nachname").inputValue(), "last name");
		assertEquals("COVID-19", inWin("^krankheit").inputValue(), "disease");
		assertEquals(de(0), inWin("^meldedatum").inputValue(), "case report date is today, not the self report date");
		ui.comboPick(inWin("^zuständiges bundesland"), "Vor", "Voreingestelltes Bundesland", true, 500, 600);
		ui.comboPick(inWin("^zuständige\\/r landkreis"), "Vor", "Voreingestellter Landkreis", true, 500, 600);
		ui.windowButtonLike(Pattern.compile("speichern und fall öffnen", Pattern.CASE_INSENSITIVE));
		String n2 = ui.newestNotificationText(12_000);
		assertTrue(n2 != null && n2.contains("Fall gespeichert"), "case saved: " + n2);
		assertTrue(ui.waitText("Falldaten", 20_000), "case view");
		ui.dismissNotifications();
		assertTrue(Pattern.compile("COVID-19 .*Yankee SELBST \\(\\* 15\\.6\\.1980\\) \\(").matcher(body().substring(0, Math.min(600, body().length()))).find(), "case for Yankee Selbst");
	}

	@Test
	@Order(5)
	@DisplayName("5. Back in the self reports list: Yankee Selbst is shown as processed, Zulu Selbst as not processed; two self reports, one processed into a COVID-19 case")
	void step5() {
		ui.openView("Self Reports");
		ui.waitRow("Yankee", 15_000);
		assertEquals(3, ui.rowTexts().size(), "rows");
		String y = rowOf("Yankee").trim();
		assertTrue(Pattern.compile("Yankee Selbst .*Processed$").matcher(y).find() && !y.contains("Unprocessed"), "Yankee processed: " + y);
		assertTrue(rowOf("Zulu").trim().endsWith("Unprocessed"), "Zulu unprocessed");
		// Data check: two self reports imported, one processed into a new COVID-19 case
		assertEquals("2", copy.sql("SELECT count(*) FROM selfreports WHERE firstname IN ('Yankee', 'Zulu')"));
		assertEquals("1", copy.sql("SELECT count(*) FROM selfreports WHERE firstname = 'Yankee' AND resultingcase_id IS NOT NULL"));
		assertFalse(copy.sql("SELECT count(*) FROM selfreports WHERE firstname = 'Zulu' AND resultingcase_id IS NOT NULL").equals("1"), "Zulu not processed");
	}
}
