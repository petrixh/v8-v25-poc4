package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF23-duplicate-person-check.md */
@DisplayName("WF23 Duplicate person check when creating a case (SurvOff, C1)")
@AppCopy(AppCopy.Config.C1)
class WF23DuplicatePersonCheckIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvOff");
	}

	/** SurvOff's region and district are preset in the form, so a combo box may hold text: select all before typing. */
	private void combo(String label, String text) {
		ui.comboPick(ui.field(label), text, text, true, 500, 700);
	}

	@Test
	@Order(1)
	@DisplayName("1. Start a new case from the case directory: disease Measles, jurisdiction Default, Default Facility")
	void step1() {
		ui.openView("Cases");
		ui.button("New case").click();
		ui.waitText("Create new case", 10_000);
		assertTrue(ui.windowText().contains("Create new case"), "form title");
		combo("Disease", "Measles");
		ui.fillField("Date of report", en(0));
		ui.chooseOption("Facility");
		combo("Facility", "Default Facility");
		assertEquals("Default Region", ui.field("Responsible region").inputValue(), "region");
		assertEquals("Default District", ui.field("Responsible district").inputValue(), "district");
		assertEquals("Default Facility", ui.field("Facility").inputValue(), "facility");
	}

	@Test
	@Order(2)
	@DisplayName("2. Bravo Seedperson, Male, born 2 July 1975; save: \"Pick or create person\" lists the existing Bravo Seedperson as a match")
	void step2() {
		ui.fillField("First name", "Bravo");
		ui.fillField("Last name", "Seedperson");
		ui.chooseCombo("Sex", "Male");
		ui.chooseBirthDate(1975, "July", 2);
		ui.windowButton("Save");
		assertTrue(ui.waitText("Pick or create person", 15_000), "pick window");
		String m = ui.windowText().replaceAll("\\s+", " ");
		assertTrue(m.contains("Bravo Seedperson 51 (7/2/1975) Male Default District") || Pattern.compile("Bravo\\s+Seedperson\\s+\\d+ \\(7/2/1975\\)\\s+Male").matcher(m).find(),
			"match listed: " + m);
		for (String x : List.of("Select a matching person", "Create a new person")) {
			assertTrue(m.contains(x), "option " + x);
		}
	}

	@Test
	@Order(3)
	@DisplayName("3. Pick the existing person and confirm: the new measles case opens for Bravo Seedperson")
	void step3() {
		ui.windowClickText("Select a matching person");
		ui.settle(300);
		ui.windowButtonLast("Save");
		assertTrue(ui.waitText("Case saved", 15_000) || ui.waitText("Case data", 15_000), "case opened");
		ui.waitText("Bravo SEEDPERSON", 10_000);
		String t = ui.pageText();
		assertTrue(t.contains("Bravo SEEDPERSON"), "title");
		assertEquals("Measles", ui.field("Disease").inputValue(), "disease");
		assertEquals(en(0), ui.field("Date of report").inputValue(), "report date");
		assertTrue(t.contains("Not yet classified"), "classification");
	}

	@Test
	@Order(4)
	@DisplayName("4. Person directory, search \"Bravo\": still one Bravo Seedperson, now with two cases (COVID-19 and Measles)")
	void step4() {
		ui.openView("Persons");
		ui.fillSearch("nameAddressPhoneEmailLike", "Bravo");
		ui.applyFilters();
		ui.settle(1000);
		List<String> rows = ui.rowTexts();
		assertEquals(1, rows.size(), "rows: " + rows);
		assertTrue(rows.get(0).contains("SPER02") && rows.get(0).contains("Bravo Seedperson") && rows.get(0).contains("Male"), "row: " + rows.get(0));
		ui.clickGridText("SPER02");
		ui.waitText("SEE CASES FOR THIS PERSON", 15_000);
		java.util.regex.Matcher m = Pattern.compile("NEW CASE(.*?)SEE CASES FOR THIS PERSON", Pattern.DOTALL).matcher(ui.pageText());
		assertTrue(m.find(), "the person's cases panel");
		String cases = m.group(1);
		assertTrue(cases.contains("COVID-19") && cases.contains("Measles"), "cases: " + cases);
		assertTrue(cases.contains("SCAS02"), "old case");
		// Data check: no new person; one new measles case linked to the existing Bravo Seedperson
		assertEquals("1", copy.sql("SELECT count(*) FROM person WHERE firstname = 'Bravo' AND lastname = 'Seedperson'"));
		assertEquals("1", copy.sql("SELECT count(*) FROM cases c JOIN person p ON p.id = c.person_id WHERE p.uuid LIKE 'SPER02%' AND c.disease = 'MEASLES'"));
	}
}
