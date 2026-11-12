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
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF52-travel-entry-to-case.md. Captions in German (C2). */
@DisplayName("WF52 Create a travel entry and convert it to a case (PoeSup, C2)")
@AppCopy(AppCopy.Config.C2)
class WF52TravelEntryToCaseIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.loginDe("PoeSup");
	}

	private Locator inWin(String regex) {
		return ui.windowFieldLike(Pattern.compile(regex));
	}

	private Locator onPage(String regex) {
		return ui.field(Pattern.compile(regex));
	}

	private void pick(Locator input, String typed, String text) {
		ui.comboPick(input, typed, text, true, 500, 600);
	}

	private String body() {
		return ui.bodyText().replaceAll("\\s+", " ");
	}

	private Locator save() {
		return ui.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Speichern").setExact(true)).locator("visible=true").first();
	}

	@Test
	@Order(1)
	@DisplayName("1. Open Einreisen: the list shows the seed travel entry STRV01 (Hotel Seedperson)")
	void step1() {
		ui.openView("Einreisen");
		assertTrue(ui.waitText("Einreiseverzeichnis", 10_000), "title");
		ui.waitRow("STRV01", 10_000);
		String rt = ui.rowTextWith("STRV01");
		assertTrue(!rt.isEmpty(), "seed entry listed");
		assertTrue(rt.contains("Hotel") && rt.contains("Seedperson"), "person: " + rt);
	}

	@Test
	@Order(2)
	@DisplayName("2. New travel entry: COVID-19, dates, Default jurisdiction, entry point \"Anderer Einreiseort\", Xray Reisender, Male, 3 March 1970; save: the view opens")
	void step2() {
		ui.button("Neue Einreise").click();
		ui.waitText("Neue Einreise erstellen", 10_000);
		assertTrue(ui.windowText().contains("Neue Einreise erstellen"), "form title");
		assertEquals(de(0), inWin("^Meldedatum").inputValue(), "report date preset");
		pick(inWin("^Krankheit"), "COVID-19", "COVID-19");
		ui.fillDate(inWin("^Einreisedatum"), de(0), 300);
		pick(inWin("^Zuständiges Bundesland"), "Vor", "Voreingestelltes Bundesland");
		pick(inWin("^Zuständige\\/r Landkreis"), "Vor", "Voreingestellter Landkreis");
		pick(inWin("^Einreiseort$"), "Anderer Einr", "Anderer Einreiseort");
		inWin("^Einreiseort Details").fill("WF Hotel entrance");
		inWin("^Vorname").fill("Xray");
		inWin("^Nachname").fill("Reisender");
		ui.chooseBirthDateLike(1970, "März", 3);
		pick(inWin("^Geschlecht"), "Männlich", "Männlich");
		ui.windowButtonLike(Pattern.compile("^Speichern$"));
		assertTrue(ui.waitText("Xray REISENDER (* 3.3.1970)", 20_000), "view title");
		assertEquals("COVID-19", onPage("^Krankheit").inputValue(), "disease");
		assertEquals(de(0), onPage("^Einreisedatum").inputValue(), "entry date");
		assertEquals("Anderer Einreiseort", onPage("^Einreiseort$").inputValue(), "entry point");
		assertTrue(ui.button("Fall erstellen").isVisible(), "convert button");
	}

	@Test
	@Order(3)
	@DisplayName("3. Quarantine home, ordered in writing, until 10 days after the reset date; save: saved")
	void step3() {
		ui.dismissNotifications();
		ui.lastComboToggleChoose("Häuslich");
		ui.page.getByText("Quarantäne schriftlich verordnet?", new Page.GetByTextOptions().setExact(true)).click();
		ui.settle(300);
		ui.fillDate(onPage("^Quarantäne Ende"), de(10), 500);
		save().click();
		String note = ui.newestNotificationText(10_000);
		assertTrue(note != null && note.contains("Einreisedaten gespeichert"), "saved: " + note);
		ui.settle(800);
		assertEquals(de(10), onPage("^Quarantäne Ende").inputValue(), "quarantine end");
		@SuppressWarnings("unchecked")
		List<String> combos = (List<String>) ui.page.locator("vaadin-combo-box input").evaluateAll("es => es.map(e => e.value)");
		assertEquals("Häuslich", combos.stream().filter(v -> !v.isEmpty()).reduce((a, b) -> b).orElse(""), "quarantine");
	}

	@Test
	@Order(4)
	@DisplayName("4. Create a case from the travel entry: prefilled with the person; complete the district and place of stay (Zuhause); save: the case is created")
	void step4() {
		ui.dismissNotifications();
		ui.button("Fall erstellen").click();
		ui.waitText("Neuen Fall erstellen", 10_000);
		assertTrue(ui.windowText().contains("Neuen Fall erstellen"), "form title");
		assertEquals("Xray", inWin("^Vorname").inputValue(), "first name");
		assertEquals("Reisender", inWin("^Nachname").inputValue(), "last name");
		assertEquals("Männlich", inWin("^Geschlecht").inputValue(), "sex");
		assertEquals("WF Hotel entrance", ui.fieldById("pointOfEntryDetails", true).inputValue(), "entry point details");
		pick(inWin("^Zuständige\\/r Landkreis"), "Vor", "Voreingestellter Landkreis");
		ui.windowClickText("Zuhause");
		ui.settle(400);
		ui.windowButtonLike(Pattern.compile("^Speichern$"));
		assertTrue(ui.waitText("Falldaten", 20_000), "case view");
		assertTrue(body().contains("Xray REISENDER (* 3.3.1970)"), "case person");
		assertEquals(de(0), onPage("^Meldedatum").inputValue(), "report date");
		ui.dismissNotifications();
	}

	@Test
	@Order(5)
	@DisplayName("5. Back on the travel entry: it shows the resulting case; the converted filter lists it; one entry and one case for Xray Reisender are linked")
	void step5() {
		ui.openView("Einreisen");
		ui.waitRow("Reisender", 10_000);
		ui.clickRow(ui.rowIndex("Reisender"));
		assertTrue(ui.waitText("Fall zu dieser Einreise öffnen", 15_000), "case open link");
		assertTrue(body().contains("FALLPERSON Xray REISENDER"), "case person");
		ui.openView("Einreisen");
		ui.page.getByText("Nur in Fälle konvertierte Einreisen", new Page.GetByTextOptions().setExact(true)).click();
		ui.applyFilters();
		ui.settle(1200);
		List<String> rows = ui.rowTexts();
		assertEquals(1, rows.size(), "converted entries: " + rows);
		assertTrue(rows.get(0).contains("Xray") && rows.get(0).contains(de(10)), "row: " + rows.get(0));
		// Data check: one new travel entry and one case for Xray Reisender, linked to each other
		assertEquals(
			"1|1",
			copy.sql(
				"SELECT count(DISTINCT t.id) || '|' || count(DISTINCT c.id) FROM travelentry t JOIN person p ON p.id = t.person_id JOIN cases c ON c.id = t.resultingcase_id"
					+ " WHERE p.firstname = 'Xray' AND p.lastname = 'Reisender' AND c.person_id = p.id"));
	}
}
