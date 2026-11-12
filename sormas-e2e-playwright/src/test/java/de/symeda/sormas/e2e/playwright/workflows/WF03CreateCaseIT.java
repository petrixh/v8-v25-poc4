package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF03-create-case.md */
@DisplayName("WF03 Create a new case for a new person (SurvSup, C1)")
@AppCopy(AppCopy.Config.C1)
class WF03CreateCaseIT extends WorkflowTest {

	Ui ui;
	Locator win;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvSup");
		win = ui.windows();
	}

	private boolean formShown() {
		return win.getByText("Create new case", new Locator.GetByTextOptions().setExact(true)).isVisible();
	}

	@Test
	@Order(1)
	@DisplayName("1. Open the case directory and start a new case: the form shows disease, report date and jurisdiction")
	void step1() {
		ui.openView("Cases");
		assertTrue(ui.page.getByText("Case Directory", new com.microsoft.playwright.Page.GetByTextOptions().setExact(true)).first().isVisible(), "directory");
		ui.button("New case").click();
		ui.settle(2000);
		assertTrue(formShown(), "form title");
		for (String f : List.of("Disease", "Date of report", "Responsible region", "Responsible district")) {
			assertTrue(ui.field(f).isVisible(), "field " + f);
		}
	}

	@Test
	@Order(2)
	@DisplayName("2. Save with only the disease Measles: blocked, the missing required fields are flagged")
	void step2() {
		ui.chooseCombo("Disease", "Measles");
		ui.windowButton("Save");
		ui.settle(1200);
		assertTrue(formShown(), "still on the form");
		assertTrue(ui.pageText().contains("Please check the input data"), "blocked message");
		String note = ui.notificationText();
		assertTrue(note != null && note.contains("Place of stay"), "place of stay flagged: " + note);
	}

	@Test
	@Order(3)
	@DisplayName("3. Region, district and community Default; place of stay Facility, type Hospital, Default Facility")
	void step3() {
		ui.dismissNotifications();
		ui.fillField("Date of report", en(0));
		String region = ui.field("Responsible region").inputValue();
		ui.chooseCombo("Responsible district", "Default District");
		ui.chooseCombo("Responsible community", "Default Community");
		ui.chooseOption("Facility");
		String facilityType = ui.field("Facility type").inputValue();
		ui.chooseCombo("Facility", "Default Facility");
		assertEquals("Default Region", region, "region");
		assertEquals("Default District", ui.field("Responsible district").inputValue(), "district");
		assertEquals("Default Community", ui.field("Responsible community").inputValue(), "community");
		assertEquals("Hospital", facilityType, "facility type");
		assertEquals("Default Facility", ui.field("Facility").inputValue(), "facility");
	}

	@Test
	@Order(4)
	@DisplayName("4. Kilo Casecreate, Male, born 15 March 1985; save: the case opens on its case data")
	void step4() {
		ui.fillField("First name", "Kilo");
		ui.fillField("Last name", "Casecreate");
		ui.chooseCombo("Sex", "Male");
		ui.chooseBirthDate(1985, "March", 15);
		ui.windowButton("Save");
		assertTrue(ui.waitText("Case saved", 15000) || ui.waitText("Case data", 15000), "case opened");
		ui.settle(1500);
		String t = ui.pageText();
		assertTrue(t.contains("Kilo CASECREATE"), "title");
		assertEquals("Measles", ui.field("Disease").inputValue(), "disease");
		assertTrue(t.contains("Not yet classified"), "classification");
		assertEquals(en(0), ui.field("Date of report").inputValue(), "report date");
		assertTrue(t.matches("(?is).*Surveillance SUPERVISOR.*"), "reporting user");
	}

	@Test
	@Order(5)
	@DisplayName("5. The case's person tab shows Kilo Casecreate with the birth date and sex entered")
	void step5() {
		ui.tab("Case person").click();
		ui.settle(1500);
		assertEquals("Kilo", ui.field("First name").inputValue(), "first name");
		assertEquals("Casecreate", ui.field("Last name").inputValue(), "last name");
		assertEquals("Male", ui.field("Sex").inputValue(), "sex");
		@SuppressWarnings("unchecked")
		List<String> combos = (List<String>) ui.page.locator("vaadin-combo-box input").evaluateAll("es => es.map(e => e.value)");
		assertEquals(3, combos.stream().filter(v -> List.of("1985", "March", "15").contains(v)).count(), "birth date shown");
	}

	@Test
	@Order(6)
	@DisplayName("6. Search the case directory for Casecreate: one Measles case, Default District, reported on the reset date")
	void step6() {
		ui.openView("Cases");
		ui.fillSearch("personLike", "Casecreate");
		ui.applyFilters();
		ui.settle(1500);
		List<String> rows = ui.rowTexts();
		assertEquals(1, rows.size(), "rows: " + rows);
		for (String x : List.of("Measles", "Kilo Casecreate", "Default District", en(0))) {
			assertTrue(rows.get(0).contains(x), "row has " + x + ": " + rows.get(0));
		}
		// Data check: one new case and person, Measles, Default region/district/community and Default Facility
		assertEquals(
			"MEASLES|Default Region|Default District|Default Community|Default Facility",
			copy.sql(
				"SELECT c.disease, r.name, d.name, cm.name, f.name FROM cases c JOIN person p ON p.id = c.person_id"
					+ " JOIN region r ON r.id = c.responsibleregion_id JOIN district d ON d.id = c.responsibledistrict_id"
					+ " JOIN community cm ON cm.id = c.responsiblecommunity_id JOIN facility f ON f.id = c.healthfacility_id"
					+ " WHERE p.firstname = 'Kilo' AND p.lastname = 'Casecreate'"));
	}
}
