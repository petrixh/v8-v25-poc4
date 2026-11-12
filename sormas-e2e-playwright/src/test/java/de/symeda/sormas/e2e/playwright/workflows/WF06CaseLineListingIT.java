package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF06-case-line-listing.md */
@DisplayName("WF06 Enter several cases at once by line listing (SurvSup, C1)")
@AppCopy(AppCopy.Config.C1)
class WF06CaseLineListingIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvSup");
	}

	private void pick(String label, int line, String typed, String text) {
		ui.comboPickVisible(ui.lineListingBox(label, line), typed, text, 600);
	}

	private void type(String label, int line, String value) {
		ui.lineListingBox(label, line).fill(value);
		ui.lineListingBox(label, line).press("Tab");
	}

	@Test
	@Order(1)
	@DisplayName("1. Start line listing from the case directory: a window with shared fields (disease, district, facility) and one case line")
	void step1() {
		ui.openView("Cases");
		ui.button("Line listing").click();
		ui.settle(1500);
		assertTrue(String.valueOf(ui.windowTitle()).contains("Line listing"), "title");
		for (String f : List.of("Disease", "Responsible district", "Facility category", "Facility type")) {
			assertTrue(ui.lineListingBox(f).isVisible(), "shared field " + f);
		}
		assertEquals(1, ui.lineListingLines(), "one line");
	}

	@Test
	@Order(2)
	@DisplayName("2. Cholera, Default District, Medical facility, Hospital; first line: Default Community, Default Facility, report date, Lima Lineone, Female")
	void step2() {
		pick("Disease", 0, "Chol", "Cholera");
		pick("Responsible district", 0, "Default", "Default District");
		pick("Facility category", 0, "Medical", "Medical facility");
		pick("Facility type", 0, "Hosp", "Hospital");
		ui.fillDate(ui.lineListingBox("Date of report"), en(0), 500);
		pick("Responsible community", 0, "Default", "Default Community");
		pick("Facility", 0, "Default F", "Default Facility");
		type("First name", 0, "Lima");
		type("Last name", 0, "Lineone");
		pick("Sex", 0, "Female", "Female");
		assertEquals("Cholera", ui.lineListingBox("Disease").inputValue(), "disease");
		assertEquals("Default District", ui.lineListingBox("Responsible district").inputValue(), "district");
		assertEquals("Default Facility", ui.lineListingBox("Facility").inputValue(), "facility");
		assertEquals("Female", ui.lineListingBox("Sex").inputValue(), "sex");
	}

	@Test
	@Order(3)
	@DisplayName("3. Add two more lines: they take over report date, community and facility; fill Mike Linetwo (Male) and November Linethree (Female)")
	void step3() {
		ui.windowButton("Add line");
		ui.settle(500);
		ui.windowButton("Add line");
		ui.settle(500);
		assertEquals(3, ui.lineListingLines(), "three lines");
		for (int line = 1; line <= 2; line++) {
			assertEquals(Ui.shortYear(en(0)), Ui.dateValue(ui.lineListingBox("Date of report", line)), "report date line " + line);
			assertEquals("Default Community", ui.lineListingBox("Responsible community", line).inputValue(), "community line " + line);
			assertEquals("Default Facility", ui.lineListingBox("Facility", line).inputValue(), "facility line " + line);
		}
		type("First name", 1, "Mike");
		type("Last name", 1, "Linetwo");
		pick("Sex", 1, "Male", "Male");
		type("First name", 2, "November");
		type("Last name", 2, "Linethree");
		pick("Sex", 2, "Female", "Female");
	}

	@Test
	@Order(4)
	@DisplayName("4. Save: the window closes, the case directory shows the three new Cholera cases")
	void step4() {
		ui.windowButton("Save");
		assertTrue(ui.waitText("saved", 10_000) || ui.waitText("created", 3000), "confirmation");
		ui.noModal();
		assertEquals(0, ui.windowsAfterClose(), "window closed");
		ui.openView("Cases");
		ui.fillSearch("personLike", "Line");
		ui.applyFilters();
		ui.settle(1000);
		List<String> rows = ui.rowTexts();
		assertEquals(3, rows.size(), "rows: " + rows);
		for (String n : List.of("Lima Lineone", "Mike Linetwo", "November Linethree")) {
			assertTrue(rows.stream().anyMatch(r -> r.contains(n) && r.contains("Cholera") && r.contains("Default District")), "row " + n);
		}
	}

	@Test
	@Order(5)
	@DisplayName("5. Open one: Cholera, the Default Facility and the report date entered; three Cholera cases and persons in the Default district")
	void step5() {
		ui.clickRow(ui.rowIndex("Lima"));
		assertTrue(ui.waitText("Lima LINEONE", 15_000), "title");
		assertEquals("Cholera", ui.field("Disease").inputValue(), "disease");
		assertEquals("Default Facility", ui.field("Facility").inputValue(), "facility");
		assertEquals(en(0), ui.field("Date of report").inputValue(), "report date");
		// Data check: three new Cholera cases with three new persons, all in the Default district
		assertEquals(
			"Lima Lineone|Mike Linetwo|November Linethree",
			copy.sql(
				"SELECT string_agg(p.firstname || ' ' || p.lastname, '|' ORDER BY p.firstname) FROM cases c JOIN person p ON p.id = c.person_id"
					+ " JOIN district d ON d.id = c.responsibledistrict_id WHERE c.disease = 'CHOLERA' AND d.name = 'Default District'"
					+ " AND p.lastname LIKE 'Line%'"));
	}
}
