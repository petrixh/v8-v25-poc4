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

/** migration/workflows/WF39-poe-case-referral.md */
@DisplayName("WF39 Register a case at the point of entry and refer it to a facility (PoeSup, C1)")
@AppCopy(AppCopy.Config.C1)
class WF39PoeCaseReferralIT extends WorkflowTest {

	private static final Pattern REFER = Pattern.compile("refer case from point of entry", Pattern.CASE_INSENSITIVE);
	Ui ui;
	Locator win;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("PoeSup");
		win = ui.windows();
	}

	/** Page text plus input values: read-only values live in input values. */
	private String text() {
		return Ui.norm(ui.page.locator("body").innerText() + " " + String.join(" ", ui.inputValues(false)));
	}

	private void winCombo(String label, String text) {
		ui.comboPick(ui.windowField(label, true), text, Pattern.compile("^" + text + "$", Pattern.CASE_INSENSITIVE), true, 700, 900);
	}

	@Test
	@Order(1)
	@DisplayName("1. New case at point of entry Other airport (Default Point Of Entry also offered), Uniform Traveller, Male; save")
	void step1() {
		ui.openView("Cases");
		ui.button("New case").click();
		ui.settle(2000);
		assertTrue(win.getByText("Create new case", new Locator.GetByTextOptions().setExact(true)).isVisible(), "form title");
		ui.fillField("Date of report", en(0));
		ui.chooseCombo("Responsible district", "Default District");
		Locator poe = ui.field("Point of entry");
		ui.typeInCombo(poe, "Default", false, 1000, true);
		assertTrue(!ui.suggestionsText().isBlank(), "Default Point Of Entry offered");
		ui.typeInCombo(poe, "Other air", true, 900, false);
		ui.pickSuggestion(Pattern.compile("^other airport$", Pattern.CASE_INSENSITIVE), 900);
		ui.fillField("Point of entry name & description", "Test airport");
		ui.fillField("First name", "Uniform");
		ui.fillField("Last name", "Traveller");
		ui.chooseCombo("Sex", "Male");
		ui.windowButton("Save");
		ui.waitText("Uniform TRAVELLER", 15_000);
		ui.settle(1500);
		String t = text();
		assertTrue(t.contains("Uniform TRAVELLER") && win.count() == 0, "case opened");
		assertTrue(t.contains("Other airport") && t.contains("Test airport"), "point of entry");
		assertTrue(REFER.matcher(t).find(), "refer button");
		assertTrue(ui.page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Port health").setExact(true)).count() > 0, "port health tab");
	}

	@Test
	@Order(2)
	@DisplayName("2. Port health tab: flight number WF123, departure 1 day back; save: saved")
	void step2() {
		ui.tab("Port health").click();
		ui.settle(2000);
		ui.fillField("Flight number", "WF123");
		ui.fillDate(ui.dateInput(0, false), en(-1), 700); // departure; the date fields carry no accessible name
		ui.savePage();
		ui.settle(2500);
		ui.page.reload();
		ui.settle(3000);
		assertEquals("WF123", ui.field("Flight number").inputValue(), "flight");
		assertEquals(en(-1), ui.dateInput(0, false).inputValue(), "departure");
	}

	@Test
	@Order(3)
	@DisplayName("3. Refer case from point of entry: Default region and district, Facility, Medical facility, Hospital, Default Facility")
	void step3() {
		ui.tab("Case").click();
		ui.settle(2000);
		ui.clickEntry("Refer case from point of entry");
		ui.settle(1500);
		assertTrue(win.getByText("Refer case from point of entry", new Locator.GetByTextOptions().setExact(true)).first().isVisible(), "window");
		winCombo("Region", "Default Region");
		winCombo("District", "Default District");
		ui.windowClickText("Facility");
		ui.settle(1000);
		winCombo("Facility category", "Medical facility");
		winCombo("Facility type", "Hospital");
		winCombo("Facility", "Default Facility");
		ui.windowButton("Refer case from point of entry");
		ui.noModal();
		assertEquals(0, win.count(), "window closed");
	}

	@Test
	@Order(4)
	@DisplayName("4. The case shows the Default Facility as place of stay and keeps the point of entry")
	void step4() {
		ui.settle(1500);
		String t = text();
		assertTrue(t.contains("Default Facility"), "facility");
		assertTrue(t.contains("Other airport") && t.contains("Test airport"), "point of entry kept");
	}

	@Test
	@Order(5)
	@DisplayName("5. Case directory filtered by origin Point of entry: Uniform Traveller is listed")
	void step5() {
		ui.openView("Cases");
		Locator origin = ui.page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Case origin").setExact(true)).first();
		if (origin.count() > 0) {
			origin.click();
			ui.settle(600);
			ui.pickSuggestion(Pattern.compile("point of entry", Pattern.CASE_INSENSITIVE), 800);
		}
		ui.applyFilters();
		ui.settle(1800);
		List<String> rows = ui.rowTexts();
		assertTrue(rows.stream().anyMatch(r -> r.contains("Uniform") && r.contains("Traveller")), "listed");
		// Data check: one new case with origin point of entry, port health details, the Default Facility after referral
		assertEquals(
			"POINT_OF_ENTRY|Test airport|WF123|Default Facility",
			copy.sql(
				"SELECT c.caseorigin, c.pointofentrydetails, ph.flightnumber, f.name FROM cases c JOIN person p ON p.id = c.person_id"
					+ " JOIN porthealthinfo ph ON ph.id = c.porthealthinfo_id JOIN facility f ON f.id = c.healthfacility_id"
					+ " WHERE p.firstname = 'Uniform' AND p.lastname = 'Traveller'"));
	}
}
