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

/** migration/workflows/WF36-clinical-course-therapy.md */
@DisplayName("WF36 Therapy and clinical course of a case (NatClin, C1)")
@AppCopy(AppCopy.Config.C1)
class WF36ClinicalCourseTherapyIT extends WorkflowTest {

	private static final Pattern DATE = Pattern.compile("^\\d+/\\d+/\\d{4}$");

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("NatClin");
	}

	private void tab(String name) {
		ui.tab(name).click();
		ui.settle(1000);
	}

	private String dateOnOpen() {
		return ui.inputValues(true).stream().filter(v -> DATE.matcher(v).matches()).findFirst().orElse(null);
	}

	@Test
	@Order(1)
	@DisplayName("1. Open SCAS01, therapy tab: prescriptions and treatments are empty")
	void step1() {
		ui.openView("Cases");
		ui.fillSearch("caseLike", "SCAS01");
		ui.applyFilters();
		ui.settle(1000);
		ui.clickGridText("SCAS01");
		ui.waitText("Case data", 15_000);
		tab("Therapy");
		ui.waitText("Executed treatments", 10_000);
		String t = ui.pageText();
		assertTrue(t.contains("Prescriptions") && t.contains("Executed treatments"), "therapy shown");
		assertTrue(t.contains("SCAS01"), "case id");
		assertEquals(0, ui.rowTexts(0).size(), "prescriptions empty");
		assertEquals(0, ui.rowTexts(1).size(), "treatments empty");
	}

	@Test
	@Order(2)
	@DisplayName("2. Add a prescription (Drug intake, \"Paracetamol\", start date 3 days before); save: it is listed")
	void step2() {
		ui.button("New prescription").click();
		ui.waitText("Create new prescription", 10_000);
		assertTrue(ui.windowText().contains("Create new prescription"), "form title");
		ui.chooseCombo("Prescription type", "Drug intake");
		ui.fillField("Drug name", "Paracetamol");
		ui.fillField("Treatment start date", en(-3));
		ui.windowButton("Save");
		ui.noModal();
		List<String> rows = ui.rowTexts(0);
		assertEquals(1, rows.size(), "rows: " + rows);
		assertTrue(rows.get(0).contains("Drug intake"), "listed: " + rows.get(0));
	}

	@Test
	@Order(3)
	@DisplayName("3. Add a treatment (the form opens empty with the date preset): Drug intake, \"Paracetamol\"; save: it is listed")
	void step3() {
		ui.button("New treatment").click();
		ui.waitText("Create new treatment", 10_000);
		assertTrue(ui.windowText().contains("Create new treatment"), "form title");
		assertEquals(en(0), dateOnOpen(), "the date defaults to the reset date");
		ui.chooseCombo("Treatment type", "Drug intake");
		ui.fillField("Drug name", "Paracetamol");
		ui.windowButton("Save");
		ui.noModal();
		List<String> rows = ui.rowTexts(1);
		assertEquals(1, rows.size(), "rows: " + rows);
		assertTrue(rows.get(0).contains("Drug intake") && rows.get(0).contains(en(0)), "listed: " + rows.get(0));
	}

	@Test
	@Order(4)
	@DisplayName("4. Clinical course tab: a clinical assessment with temperature 38.5 on the reset date; save: listed with the temperature as entered")
	void step4() {
		tab("Clinical course");
		ui.waitText("Clinical assessments", 10_000);
		ui.button("New clinical assessment").click();
		ui.waitText("Create new clinical assessment", 10_000);
		assertTrue(ui.windowText().contains("Create new clinical assessment"), "form title");
		assertEquals(en(0), dateOnOpen(), "the visit date defaults to the reset date");
		var temp = ui.windowFieldLike(Pattern.compile("body temperature", Pattern.CASE_INSENSITIVE));
		ui.typeInCombo(temp, "38.5", false, 400, true);
		ui.pickSuggestion(Pattern.compile("^38\\.5"), 500);
		ui.windowButton("Save");
		ui.noModal();
		List<String> rows = ui.rowTexts(0);
		assertEquals(1, rows.size(), "rows: " + rows);
		assertTrue(rows.get(0).contains("38.5"), "temperature listed");
		assertTrue(rows.get(0).contains(en(0)), "visit date listed");
		// Data check: SCAS01 has one prescription, one treatment and one clinical assessment
		assertEquals("1", copy.sql("SELECT count(*) FROM prescription t JOIN cases c ON c.therapy_id = t.therapy_id WHERE c.uuid LIKE 'SCAS01%'"));
		assertEquals("1", copy.sql("SELECT count(*) FROM treatment t JOIN cases c ON c.therapy_id = t.therapy_id WHERE c.uuid LIKE 'SCAS01%'"));
		assertEquals("1", copy.sql("SELECT count(*) FROM clinicalvisit t JOIN cases c ON c.clinicalcourse_id = t.clinicalcourse_id WHERE c.uuid LIKE 'SCAS01%'"));
		assertEquals(
			"38.5",
			copy.sql(
				"SELECT s.temperature FROM clinicalvisit t JOIN symptoms s ON s.id = t.symptoms_id JOIN cases c ON c.clinicalcourse_id = t.clinicalcourse_id WHERE c.uuid LIKE 'SCAS01%'"));
	}
}
