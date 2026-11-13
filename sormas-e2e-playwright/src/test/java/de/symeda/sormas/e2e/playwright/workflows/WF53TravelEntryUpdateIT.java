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

/** migration/workflows/WF53-travel-entry-update.md. Captions in German (C2). */
@DisplayName("WF53 Update a travel entry and its person (SurvOff, C2)")
@AppCopy(AppCopy.Config.C2)
class WF53TravelEntryUpdateIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.loginDe("SurvOff");
	}

	private Locator onPage(String regex) {
		return ui.field(Pattern.compile(regex));
	}

	private Locator save() {
		return ui.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Speichern").setExact(true)).locator("visible=true").first();
	}

	private String ticks() {
		return String.join(",", String.valueOf(ui.checkboxChecked("Genesen")), String.valueOf(ui.checkboxChecked("Geimpft")), String.valueOf(ui.checkboxChecked("Negativ getestet")));
	}

	@Test
	@Order(1)
	@DisplayName("1. Open Einreisen and filter by the text \"Hotel\": STRV01 is listed (Hotel Seedperson)")
	void step1() {
		ui.openView("Einreisen");
		ui.fillSearch("nameUuidExternalIDLike", "Hotel");
		ui.applyFilters();
		ui.settle(1200);
		List<String> rows = ui.rowTexts();
		assertEquals(1, rows.size(), "rows: " + rows);
		assertTrue(rows.get(0).contains("STRV01") && rows.get(0).contains("Hotel Seedperson") && rows.get(0).contains("Voreingestellter Landkreis"), "row: " + rows.get(0));
	}

	@Test
	@Order(2)
	@DisplayName("2. Open STRV01: the point of entry (default, active) and the arrival details are shown, the arrival date as 01.11.2026")
	void step2() {
		ui.clickRow(ui.rowIndex("STRV01"));
		assertTrue(ui.waitText("Hotel SEEDPERSON (* 8.4.1972) (STRV01)", 15_000), "title");
		assertEquals("Voreingestellter Einreiseort", onPage("^Einreiseort$").inputValue(), "entry point");
		assertEquals("01.11.2026", onPage("^Einreisedatum").inputValue(), "arrival");
		assertEquals("02.11.2026", onPage("^Meldedatum").inputValue(), "report date");
		assertEquals("COVID-19", onPage("^Krankheit").inputValue(), "disease");
		assertEquals("false,false,false", ticks(), "none ticked");
	}

	@Test
	@Order(3)
	@DisplayName("3. Tick \"Geimpft\" and \"Negativ getestet\"; save: saved")
	void step3() {
		ui.clickCheckbox("Geimpft");
		ui.settle(300);
		ui.clickCheckbox("Negativ getestet");
		ui.settle(300);
		save().click();
		String note = ui.newestNotificationText(10_000);
		assertTrue(note != null && note.contains("Einreisedaten gespeichert"), "saved: " + note);
		ui.settle(800);
		assertEquals("false,true,true", ticks(), "ticked");
		ui.dismissNotifications();
	}

	@Test
	@Order(4)
	@DisplayName("4. Einreiseperson tab: Hotel Seedperson with the Seedville address; add an e-mail address; save: saved; the data is stored")
	void step4() {
		ui.tab("Einreiseperson").click();
		ui.waitMatch("Seedville", 15_000);
		ui.settle(800);
		assertEquals("Hotel", ui.fieldById("firstName", false).inputValue(), "first name");
		assertEquals("Seedperson", ui.fieldById("lastName", false).inputValue(), "last name");
		assertEquals("Seedville", ui.fieldById("city", false).inputValue(), "city");
		assertEquals("Example Street", ui.fieldById("street", false).inputValue(), "street");
		ui.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Neuer Eintrag")).nth(1).click();
		ui.waitText("Personen-Kontaktdetails", 10_000);
		assertTrue(ui.windowText().contains("Personen-Kontaktdetails"), "details window");
		ui.comboToggleChoose(ui.windowFirstCombo(), "E-Mail");
		ui.fieldById("contactInformation", true).fill("hotel.seed@example.org");
		ui.fieldById("contactInformation", true).press("Tab");
		ui.windowButtonLike(Pattern.compile("^Erledigt$"));
		ui.noModal();
		save().click();
		String note = ui.newestNotificationText(10_000);
		assertTrue(note != null && note.contains("Personendaten gespeichert"), "saved: " + note);
		ui.settle(800);
		assertTrue(ui.bodyText().contains("hotel.seed@example.org") || ui.gridHasText("hotel.seed@example.org"), "e-mail listed");
		// Data check: STRV01 is stored as vaccinated and tested negative; Hotel Seedperson has the e-mail address
		assertEquals("true|true", copy.sql("SELECT vaccinated || '|' || testednegative FROM travelentry WHERE uuid LIKE 'STRV01%'"));
		assertEquals("1", copy.sql("SELECT count(*) FROM personcontactdetail d JOIN person p ON p.id = d.person_id WHERE p.firstname = 'Hotel' AND p.lastname = 'Seedperson' AND d.contactinformation = 'hotel.seed@example.org'"));
	}
}
