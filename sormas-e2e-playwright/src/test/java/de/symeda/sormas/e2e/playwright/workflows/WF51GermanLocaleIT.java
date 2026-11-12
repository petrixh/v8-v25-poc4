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
import com.microsoft.playwright.options.AriaRole;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/**
 * migration/workflows/WF51-german-locale.md. The typed German date goes into the investigation date (Untersuchungsdatum)
 * on the case data: the onset date is hidden for COVID-19 in C2.
 */
@DisplayName("WF51 German captions and date formats (SurvSup, C2)")
@AppCopy(AppCopy.Config.C2)
class WF51GermanLocaleIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
	}

	private void closeGdpr() {
		if (ui.windowCount() > 0) {
			ui.closeWindow();
			ui.settle(1000);
		}
	}

	private void searchScas01() {
		ui.fillSearch("caseLike", "SCAS01");
		ui.applyFilters();
		ui.settle(1500);
	}

	private String scas01Row() {
		int row = ui.rowIndex("SCAS01");
		assertTrue(row >= 0, "SCAS01 listed");
		return ui.rowTexts().get(row);
	}

	private Locator setLanguage(String typed, String text) {
		ui.comboPick(ui.windowFirstCombo(), typed, text, true, 600, 700);
		return ui.windows();
	}

	@Test
	@Order(1)
	@DisplayName("1. Log in as SurvSup: the main menu reads in German")
	void step1() {
		ui.login("SurvSup");
		ui.settle(1500);
		closeGdpr();
		String m = ui.menuText();
		for (String x : List.of("Übersicht", "Aufgaben", "Personen", "Fälle", "Kontakte", "Ereignisse", "Proben", "Einreisen")) {
			assertTrue(m.contains(x), "menu " + x);
		}
		assertFalse(Pattern.compile("\\bCases\\b|\\bTasks\\b").matcher(m).find(), "no English menu");
	}

	@Test
	@Order(2)
	@DisplayName("2. Fälle: German column headers, report dates day.month.year (SCAS01: 26.10.2026)")
	void step2() {
		ui.openView("Fälle");
		List<String> heads = ui.gridHeaderTexts();
		for (String h : List.of("krankheit", "vorname", "nachname", "meldedatum")) {
			assertTrue(heads.contains(h), "header " + h + ": " + heads);
		}
		searchScas01();
		assertTrue(scas01Row().contains("26.10.2026"), "report date D.M.Y");
	}

	@Test
	@Order(3)
	@DisplayName("3. Open SCAS01: German captions (Meldedatum), report date 26.10.2026, 24-hour time stamps")
	void step3() {
		ui.clickGridText("SCAS01");
		ui.settle(2500);
		String t = ui.pageText();
		assertTrue(t.contains("(SCAS01)"), "title");
		assertTrue(ui.page.locator("label").allInnerTexts().stream().anyMatch(c -> c.trim().toLowerCase().startsWith("meldedatum")), "caption Meldedatum");
		assertEquals("26.10.2026", ui.field(Pattern.compile("^Meldedatum")).inputValue(), "report date");
		assertTrue(t.contains("28.10.2026 11:00"), "sample test time, 24-hour clock");
		assertFalse(Pattern.compile("\\b(AM|PM)\\b").matcher(t).find(), "no AM/PM");
	}

	@Test
	@Order(4)
	@DisplayName("4. Investigation date 11 days back typed as TT.MM.JJJJ, save: German confirmation, the date reads back as typed")
	void step4() {
		Locator f = ui.field("Untersuchungsdatum");
		f.fill(de(-11));
		f.press("Tab");
		ui.settle(800);
		ui.page.getByRole(AriaRole.BUTTON, new com.microsoft.playwright.Page.GetByRoleOptions().setName("Speichern").setExact(true))
			.locator("visible=true")
			.first()
			.click();
		String saved = ui.notificationText(8000);
		assertTrue(saved != null && saved.toLowerCase().contains("gespeichert"), "German confirmation: " + saved);
		ui.settle(2000);
		assertEquals(de(-11), ui.field("Untersuchungsdatum").inputValue(), "date reads back");
		ui.dismissNotifications();
	}

	@Test
	@Order(5)
	@DisplayName("5. User settings, language English, save: English menu and captions, dates month/day/year")
	void step5() {
		ui.openSettings("Benutzereinstellungen");
		setLanguage("Engl", "English").getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Speichern").setExact(true)).click();
		ui.settle(3500);
		closeGdpr();
		String m = ui.menuText();
		for (String x : List.of("Dashboard", "Tasks", "Persons", "Cases", "Contacts", "Events", "Samples")) {
			assertTrue(m.contains(x), "menu " + x);
		}
		ui.openView("Cases");
		searchScas01();
		assertTrue(scas01Row().contains("10/26/2026"), "report date M/D/Y");
		List<String> heads = ui.gridHeaderTexts();
		assertTrue(heads.contains("disease") && heads.contains("first name"), "English headers: " + heads);
	}

	@Test
	@Order(6)
	@DisplayName("6. Language back to Deutsch, save: German captions and day.month.year dates again")
	void step6() {
		ui.openSettings("User settings");
		setLanguage("Deutsch", "Deutsch").getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save").setExact(true)).click();
		ui.settle(3500);
		closeGdpr();
		String m = ui.menuText();
		for (String x : List.of("Übersicht", "Fälle", "Einreisen")) {
			assertTrue(m.contains(x), "menu " + x);
		}
		ui.openView("Fälle");
		searchScas01();
		assertTrue(scas01Row().contains("26.10.2026"), "report date D.M.Y");
		// Data check: SCAS01's investigation date stored as typed (not shifted by a day); SurvSup's language German
		assertEquals(today.minusDays(11).toString(), copy.sql("SELECT to_char(investigateddate, 'YYYY-MM-DD') FROM cases WHERE uuid LIKE 'SCAS01%'"));
		assertEquals("DE", copy.sql("SELECT language FROM users WHERE username = 'SurvSup'"));
	}
}
