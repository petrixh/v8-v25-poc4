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

/** migration/workflows/WF28-lab-result.md. SurvOff and LabOff in their own browser sessions. */
@DisplayName("WF28 Receive a sample and enter a lab result (SurvOff, LabOff; C1)")
@AppCopy(AppCopy.Config.C1)
class WF28LabResultIT extends WorkflowTest {

	Ui surv;
	Ui lab;

	@BeforeAll
	void open() {
		surv = newUi();
		surv.login("SurvOff");
		lab = newUi();
	}

	@Test
	@Order(1)
	@DisplayName("1. As SurvOff add a sample to SCAS03: collected and shipped today, Blood, Default Laboratory; save; log out")
	void step1() {
		Ui ui = surv;
		ui.openView("Cases");
		ui.fillSearch("caseLike", "SCAS03");
		ui.applyFilters();
		ui.settle(1500);
		ui.clickGridText("SCAS03");
		ui.settle(2000);
		ui.clickEntry("New sample");
		ui.settle(1500);
		ui.fillDate(ui.dateInput(0, true), en(0), 600);
		ui.chooseCombo("Type of sample", "Blood");
		ui.chooseCombo("Laboratory", "Default Laboratory");
		ui.windowClickText("Sent/dispatched");
		ui.settle(800);
		ui.fillField("Date sample was sent", en(0));
		ui.windowButton("Save");
		ui.settle(2500);
		assertTrue(Pattern.compile("BLOOD\\s*PENDING\\s*SHIPPED", Pattern.CASE_INSENSITIVE).matcher(ui.pageText()).find(), "listed");
		ui.logout("Logout");
		ui.settle(2000);
		assertTrue(ui.loginVisible(), "logged out");
	}

	@Test
	@Order(2)
	@DisplayName("2. As LabOff, the sample directory: the shipped sample of Charlie Seedperson is not received")
	void step2() {
		lab.login("LabOff");
		lab.openView("Samples");
		List<String> rows = lab.rowTexts().stream().filter(r -> r.contains("Charlie SEEDPERSON (SCAS03)")).toList();
		assertEquals(1, rows.size(), "rows: " + rows);
		for (String x : List.of("Measles", "Default District", "Yes No", "Default Laboratory", "Blood", "External lab testing", "Pending")) {
			assertTrue(rows.get(0).contains(x), "row has " + x + ": " + rows.get(0));
		}
	}

	@Test
	@Order(3)
	@DisplayName("3. Open the sample, received today, specimen condition Adequate; save: received")
	void step3() {
		Ui ui = lab;
		ui.clickRow(0); // the new sample, newest first
		ui.settle(2500);
		String t = ui.pageText();
		assertTrue(t.contains("Laboratory sample") && t.contains("Sample for case (SCAS03)"), "sample view");
		ui.clickCheckbox("Received");
		ui.settle(1000);
		ui.fillField("Date sample received at lab", en(0));
		ui.chooseCombo("Specimen condition", "Adequate");
		ui.savePage();
		ui.settle(2500);
		assertTrue(ui.pageText().contains("Sample data saved"), "saved");
		assertEquals(en(0), ui.field("Date sample received at lab").inputValue(), "received date");
		assertEquals("Adequate", ui.field("Specimen condition").inputValue(), "condition");
	}

	@Test
	@Order(4)
	@DisplayName("4. New test result: ELISA IgM, Measles, Positive, tested today, verified; save")
	void step4() {
		Ui ui = lab;
		ui.clickEntry(Pattern.compile("^new test result$", Pattern.CASE_INSENSITIVE));
		ui.settle(2000);
		assertTrue(ui.windowHasText("Create new pathogen test result"), "form title");
		assertEquals("Measles", ui.windowFieldLike("Tested disease").inputValue(), "disease preset");
		ui.comboPick(ui.windowFieldLike("Test category"), "Serol", Pattern.compile("^Serological"), true, 600, 800);
		ui.comboPick(ui.windowFieldLike("Type of test"), "ELISA IgM", "ELISA IgM", true, 600, 800);
		ui.comboPick(ui.windowFieldLike("Test result"), "Positive", "Positive", true, 600, 800);
		ui.fillDate(ui.dateInput(0, true), en(0), 600);
		ui.windowClickText(Pattern.compile("^yes$", Pattern.CASE_INSENSITIVE)); // result verified by lab supervisor
		ui.settle(500);
		assertEquals("ELISA IgM", ui.windowFieldLike("Type of test").inputValue(), "type");
		assertEquals("Positive", ui.windowFieldLike("Test result").inputValue(), "result");
		ui.windowButton("Save");
		ui.settle(3000);
	}

	@Test
	@Order(5)
	@DisplayName("5. Accept the follow-up questions: the sample's result reads Positive and the case is confirmed")
	void step5() {
		Ui ui = lab;
		String q1 = ui.windowText();
		assertTrue(q1.contains("Change pathogen test result") && q1.contains("update its result to Positive"), "question 1: " + q1);
		ui.windowButton("Yes");
		ui.settle(2500);
		String q2 = ui.windowText();
		assertTrue(q2.contains("Confirm case") && q2.contains("Do you want to set the case classification to confirmed anyway?"), "question 2: " + q2);
		ui.windowButton("Yes");
		ui.settle(3000);
		String t = ui.pageText();
		assertTrue(t.contains("Charlie SEEDPERSON") && t.contains("SCAS03"), "case opened");
		assertTrue(t.contains("Confirmed case"), "confirmed");
		assertTrue(Pattern.compile("BLOOD\\s*POSITIVE\\s*RECEIVED", Pattern.CASE_INSENSITIVE).matcher(t).find() && t.contains("Number of tests: 1"), "sample");
	}

	@Test
	@Order(6)
	@DisplayName("6. SCAS03's lab results tab: the sample with its positive test")
	void step6() {
		Ui ui = lab;
		ui.tab("Laboratory results").click();
		ui.settle(2500);
		String t = ui.pageText();
		for (String x : List.of("ELISA IgM", "Positive", "Default Laboratory", "Blood")) {
			assertTrue(t.contains(x), "shows " + x);
		}
		assertFalse(t.contains("No tests have been performed"), "not empty");
		// Data check: the sample received with result Positive and one verified positive measles test; the case confirmed
		assertEquals(
			"t|POSITIVE|ADEQUATE|1|POSITIVE|t|MEASLES|CONFIRMED",
			copy.sql(
				"SELECT s.received, s.pathogentestresult, s.specimencondition, count(*) OVER (), pt.testresult, pt.testresultverified, pt.testeddisease, c.caseclassification"
					+ " FROM samples s JOIN cases c ON c.id = s.associatedcase_id JOIN pathogentest pt ON pt.sample_id = s.id"
					+ " WHERE c.uuid LIKE 'SCAS03%' AND NOT s.deleted AND NOT pt.deleted"));
	}
}
