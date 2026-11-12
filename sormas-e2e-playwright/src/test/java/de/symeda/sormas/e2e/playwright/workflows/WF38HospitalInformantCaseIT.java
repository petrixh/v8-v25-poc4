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

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF38-hospital-informant-case.md */
@DisplayName("WF38 Hospital informant reports a case at their hospital (HospInf, C1)")
@AppCopy(AppCopy.Config.C1)
class WF38HospitalInformantCaseIT extends WorkflowTest {

	Ui ui;
	Locator win;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("HospInf");
		win = ui.windows();
	}

	private void winCombo(String label, String text) {
		ui.comboPick(ui.windowField(label, true), text, Pattern.compile("^" + text + "$", Pattern.CASE_INSENSITIVE), true, 600, 800);
	}

	@Test
	@Order(1)
	@DisplayName("1. Log in as HospInf: the task directory is the start page; the menu has no contacts entry")
	void step1() {
		assertTrue(ui.pageText().contains("Task Management"), "task directory");
		assertFalse(Pattern.compile("contacts", Pattern.CASE_INSENSITIVE).matcher(ui.menuText()).find(), "no contacts in the menu");
	}

	@Test
	@Order(2)
	@DisplayName("2. New case: facility preset; COVID-19, Tango Hospital, Female, born 1 June 1960; save: the case opens")
	void step2() {
		ui.openView("Cases");
		ui.button("New case").click();
		ui.settle(2000);
		assertTrue(win.getByText("Create new case", new Locator.GetByTextOptions().setExact(true)).isVisible(), "form title");
		assertTrue(ui.inputValues(true).stream().anyMatch(v -> v.startsWith("Default Facility")), "facility preset");
		ui.fillField("Date of report", en(0));
		ui.chooseCombo("Disease", "COVID-19");
		ui.settle(800);
		ui.dismissNotifications();
		ui.fillField("First name", "Tango");
		ui.fillField("Last name", "Hospital");
		ui.chooseCombo("Sex", "Female");
		ui.chooseBirthDate(1960, "June", 1);
		ui.windowButton("Save");
		ui.waitText("Tango HOSPITAL", 15_000);
		ui.settle(1500);
		String t = ui.pageText();
		assertTrue(t.contains("Tango HOSPITAL") && win.count() == 0, "case opened");
		assertTrue(t.contains("COVID-19"), "disease");
	}

	@Test
	@Order(3)
	@DisplayName("3. Hospitalization: admitted today; three previous hospitalizations 300, 200, 100 days back; save: listed in order")
	void step3() {
		ui.tab("Hospitalization").click();
		ui.settle(2000);
		ui.yesAfter("Was patient admitted at the facility as an inpatient?").click();
		ui.settle(800);
		Locator adm = ui.field("Date of visit or admission");
		adm.fill(en(0));
		adm.press("Tab");
		ui.settle(600);
		ui.yesAfter("Was the patient hospitalized or did he/she visit").click();
		ui.settle(1000);
		for (int back : new int[] {
			300,
			200,
			100 }) {
			ui.clickEntry("New entry");
			ui.settle(1500);
			Locator a = ui.windowField("Date of admission", true);
			a.fill(en(-back));
			a.press("Tab");
			ui.settle(500);
			winCombo("Region", "Default Region");
			winCombo("District", "Default District");
			winCombo("Hospital", "Default Facility");
			ui.windowButton("Done");
			ui.noModal();
		}
		ui.savePage();
		ui.settle(2500);
		String t = ui.pageText();
		int i300 = t.indexOf(en(-300)), i200 = t.indexOf(en(-200)), i100 = t.indexOf(en(-100));
		assertTrue(i300 >= 0 && i200 >= 0 && i100 >= 0 && i300 < i200 && i200 < i100, "three listed in order");
	}

	@Test
	@Order(4)
	@DisplayName("4. Remove the first previous hospitalization, save and reload: two remain with the right dates")
	void step4() {
		ui.editFirstTableRow();
		ui.settle(1500);
		assertTrue(win.getByText("Previous hospitalization", new Locator.GetByTextOptions().setExact(true)).first().isVisible(), "edit window");
		ui.windowButton("Delete");
		ui.settle(1000);
		assertTrue(ui.page.getByText("Confirm Deletion", new com.microsoft.playwright.Page.GetByTextOptions().setExact(true)).isVisible(), "confirm deletion");
		ui.windowClickText("Yes");
		ui.settle(1000);
		ui.noModal();
		ui.savePage();
		ui.settle(2500);
		ui.page.reload();
		ui.settle(3000);
		String t = ui.pageText();
		assertFalse(t.contains(en(-300)), "removed one gone");
		assertTrue(t.contains(en(-200)) && t.contains(en(-100)), "two remain");
		assertEquals(2, ui.tableRowCount(), "rows");
	}

	@Test
	@Order(5)
	@DisplayName("5. Case directory: only cases of the Default Facility, including Tango Hospital")
	void step5() {
		ui.openView("Cases");
		ui.fillSearch("personLike", "Tango");
		ui.applyFilters();
		ui.settle(1500);
		List<String> rows = ui.rowTexts();
		assertTrue(rows.size() == 1 && rows.get(0).contains("Tango") && rows.get(0).contains("Hospital"), "Tango: " + rows);
		ui.fillSearch("personLike", "");
		ui.applyFilters();
		ui.settle(1800);
		List<String> all = ui.rowTexts();
		assertTrue(all.size() > 1 && all.stream().allMatch(r -> r.contains("Default Facility")), "all Default Facility: " + all.size());
		// Data check: one new COVID-19 case at the Default Facility reported by HospInf, hospitalized, two previous hospitalizations
		assertEquals(
			"CORONAVIRUS|Default Facility|HospInf|YES|2",
			copy.sql(
				"SELECT c.disease, f.name, u.username, h.admittedtohealthfacility,"
					+ " (SELECT count(*) FROM previoushospitalization ph WHERE ph.hospitalization_id = h.id)"
					+ " FROM cases c JOIN person p ON p.id = c.person_id JOIN facility f ON f.id = c.healthfacility_id"
					+ " JOIN users u ON u.id = c.reportinguser_id JOIN hospitalization h ON h.id = c.hospitalization_id"
					+ " WHERE p.firstname = 'Tango' AND p.lastname = 'Hospital'"));
	}
}
