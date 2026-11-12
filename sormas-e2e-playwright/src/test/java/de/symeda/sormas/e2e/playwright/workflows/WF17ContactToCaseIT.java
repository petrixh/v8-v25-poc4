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

/** migration/workflows/WF17-contact-to-case.md */
@DisplayName("WF17 Follow up a contact and turn it into a case (SurvOff, C1)")
@AppCopy(AppCopy.Config.C1)
class WF17ContactToCaseIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvOff");
	}

	/** Inside the open window: the page behind has fields with the same labels. */
	private Locator wfield(String label) {
		return ui.windowField(label, true);
	}

	private void wcombo(String label, String text) {
		ui.comboPick(wfield(label), text, text, false, 500, 700);
	}

	private void openScon01() {
		ui.openView("Contacts");
		ui.fillSearch("contactOrCaseLike", "SCON01");
		ui.applyFilters();
		ui.settle(1500);
		ui.clickGridText("SCON01");
		ui.settle(2000);
	}

	@Test
	@Order(1)
	@DisplayName("1. Open SCON01: source case SCAS01, classification Confirmed, status Active")
	void step1() {
		openScon01();
		String t = ui.pageText();
		assertTrue(t.contains("Echo SEEDPERSON") && t.contains("SCON01") && t.contains("Confirmed contact"), "title");
		assertTrue(
			Pattern.compile("CASE ID\\s+SCAS01\\b", Pattern.CASE_INSENSITIVE).matcher(t).find()
				&& Pattern.compile("DISEASE\\s+COVID-19", Pattern.CASE_INSENSITIVE).matcher(t).find(),
			"source case");
		assertTrue(t.contains("Create a case for this contact person"), "create case link");
	}

	@Test
	@Order(2)
	@DisplayName("2. Epidemiological data: a gathering exposure 7 days back; save: saved")
	void step2() {
		ui.tab("Epidemiological data").click();
		ui.settle(2000);
		ui.selectOption("Yes", false); // exposure details known
		ui.settle(1200);
		ui.clickEntry("New entry");
		ui.settle(1500);
		ui.fillDate(ui.dateInput(0, true), en(-7), 500);
		ui.fillDate(ui.dateInput(1, true), en(-7), 500);
		ui.comboPick(ui.field("Type of activity"), "Gath", Pattern.compile("gathering", Pattern.CASE_INSENSITIVE), false, 600, 800);
		ui.windowButton("Done");
		ui.settle(1500);
		String t = ui.pageText();
		assertTrue(t.contains("Gathering") && t.contains(en(-7)), "exposure listed");
		ui.button("Save").click();
		ui.settle(1500);
		String note = ui.notificationTexts();
		assertTrue(Pattern.compile("saved", Pattern.CASE_INSENSITIVE).matcher(note).find(), "saved: " + note);
	}

	@Test
	@Order(3)
	@DisplayName("3. Create a case for this contact person: the new case form with Echo Seedperson and COVID-19")
	void step3() {
		ui.tab("Contact").click();
		ui.settle(1800);
		ui.clickEntry("Create a case for this contact person");
		ui.settle(2000);
		assertTrue(ui.windows().last().getByText("Create new case", new Locator.GetByTextOptions().setExact(true)).isVisible(), "form title");
		List<String> vals = ui.inputValues(true);
		assertTrue(vals.contains("Echo") && vals.contains("Seedperson"), "person");
		assertTrue(vals.contains("COVID-19"), "disease");
		assertEquals("", wfield("Date of report").inputValue(), "report date empty");
	}

	@Test
	@Order(4)
	@DisplayName("4. Report date, Default community, place of stay Facility, Default Facility; save: the new case opens")
	void step4() {
		wfield("Date of report").fill(en(0));
		wfield("Date of report").press("Tab");
		ui.settle(600);
		assertEquals("Default Region", wfield("Responsible region").inputValue(), "region preset");
		assertEquals("Default District", wfield("Responsible district").inputValue(), "district preset");
		wcombo("Responsible community", "Default Community");
		ui.chooseOption("Facility");
		wcombo("Facility", "Default Facility");
		ui.windowButton("Save");
		assertTrue(ui.waitText("Case saved", 15_000) || ui.waitText("Case data", 15_000), "case opened");
		ui.settle(1500);
		assertTrue(ui.pageText().contains("Echo SEEDPERSON"), "title");
		assertEquals("COVID-19", ui.field("Disease").inputValue(), "disease");
		assertEquals(en(0), ui.field("Date of report").inputValue(), "report date");
	}

	@Test
	@Order(5)
	@DisplayName("5. Open SCON01 again: it shows the resulting case and status Converted")
	void step5() {
		openScon01();
		String t = ui.pageText();
		assertTrue(t.contains("Open case of this contact person") && !t.contains("Create a case for this contact person"), "open case link");
		ui.openView("Contacts");
		ui.fillSearch("contactOrCaseLike", "SCON01");
		ui.applyFilters();
		ui.settle(1500);
		List<String> rows = ui.rowTexts();
		assertTrue(!rows.isEmpty() && rows.get(0).contains("Converted to case"), "row converted: " + rows);
		// Data check: a new COVID-19 case for Echo Seedperson; SCON01 points to it and is converted
		assertEquals(
			"CONVERTED|CORONAVIRUS|Echo|Seedperson",
			copy.sql(
				"SELECT ct.contactstatus, c.disease, p.firstname, p.lastname FROM contact ct JOIN cases c ON c.id = ct.resultingcase_id"
					+ " JOIN person p ON p.id = c.person_id WHERE ct.uuid LIKE 'SCON01%'"));
	}
}
