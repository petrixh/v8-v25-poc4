package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/**
 * migration/workflows/WF59-survey-tokens.md. The prepared file is src/test/resources/workflows/WF59-survey-tokens.csv.
 * Survey captions are English (B036), the rest German.
 */
@DisplayName("WF59 Create a survey and import survey tokens (SurvSup, C2)")
@AppCopy(AppCopy.Config.C2)
class WF59SurveyTokensIT extends WorkflowTest {

	private static final List<String> TOKENS = List.of("WF-TOKEN-001", "WF-TOKEN-002", "WF-TOKEN-003");

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

	@Test
	@Order(1)
	@DisplayName("1. Open Surveys: only the seeded survey is listed; create \"WF survey\" for COVID-19; save: the survey view opens")
	void step1() {
		ui.openView("Surveys");
		assertTrue(ui.waitText("Surveys directory", 15_000), "title");
		ui.waitRow("Seed survey", 10_000);
		List<String> r0 = ui.rowTexts();
		assertTrue(r0.size() == 1 && r0.get(0).contains("Seed survey: symptom follow-up"), "seed only: " + r0);
		ui.pageButtonLike(Pattern.compile("new survey", Pattern.CASE_INSENSITIVE));
		ui.waitText("Create new survey", 10_000);
		assertTrue(ui.windowText().contains("Create new survey"), "window title");
		inWin("^name").fill("WF survey");
		ui.comboPick(inWin("^krankheit"), "COVID", "COVID-19", false, 500, 600);
		ui.windowButtonLike(Pattern.compile("^Speichern$"));
		String n = ui.newestNotificationText(12_000);
		assertTrue(n != null && n.contains("Survey saved"), "saved: " + n);
		assertTrue(ui.waitMatch("WF survey \\(", 15_000), "view title");
		ui.dismissNotifications();
		assertEquals("WF survey", onPage("^name").inputValue(), "name");
		assertEquals("COVID-19", onPage("^krankheit").inputValue(), "disease");
	}

	@Test
	@Order(2)
	@DisplayName("2. Open the survey's token list tab: no tokens")
	void step2() {
		ui.page.getByText(Pattern.compile("^Survey token list$", Pattern.CASE_INSENSITIVE)).first().click();
		ui.waitText("Import survey tokens", 15_000);
		ui.settle(800);
		assertTrue(ui.button(Pattern.compile("import survey tokens", Pattern.CASE_INSENSITIVE)).isVisible(), "import offered");
		assertEquals(0, ui.rowTexts().size(), "empty");
	}

	@Test
	@Order(3)
	@DisplayName("3. Import the prepared token file: the import window reports 3 of 3 processed, 3 imported, 0 errors")
	void step3() {
		ui.button(Pattern.compile("import survey tokens", Pattern.CASE_INSENSITIVE)).click();
		ui.waitText("Import Survey Tokens", 10_000);
		assertTrue(ui.windowText().contains("Import Survey Tokens"), "window title");
		ui.windowChooseFile(resourceFile("WF59-survey-tokens.csv"));
		String res = ui.importResult(60_000);
		assertTrue(res.contains("Import erfolgreich"), "success: " + res);
		assertTrue(Pattern.compile("3/3 verarbeitet ?3 importiert ?0 Fehler").matcher(res).find(), "counts: " + res);
	}

	@Test
	@Order(4)
	@DisplayName("4. The tokens tab lists 3 tokens; open one: the token view shows the token and its survey; one survey with three tokens is stored")
	void step4() {
		ui.windowButtonLike(Pattern.compile("schlie(ss|ß)en", Pattern.CASE_INSENSITIVE));
		ui.noModal();
		if (ui.windowCount() > 0) {
			ui.closeWindow();
			ui.noModal();
		}
		ui.waitRow("WF-TOKEN-001", 15_000);
		List<String> rows = ui.rowTexts();
		assertEquals(3, rows.size(), "rows: " + rows);
		for (String t : TOKENS) {
			assertTrue(rows.stream().anyMatch(r -> r.contains(t) && Pattern.compile("\\bNein\\b").matcher(r).find()), "token " + t);
		}
		ui.clickRow(ui.rowIndex("WF-TOKEN-001"));
		assertTrue(ui.waitMatch("Survey token \\(", 15_000), "view title");
		assertEquals("WF-TOKEN-001", onPage("^token$").inputValue(), "token");
		assertEquals("WF survey", onPage("^survey$").inputValue(), "survey");
		// Data check: one survey with three tokens
		assertEquals(
			"3",
			copy.sql("SELECT count(*) FROM surveytokens t JOIN surveys s ON s.id = t.survey_id WHERE s.name = 'WF survey'"));
	}
}
