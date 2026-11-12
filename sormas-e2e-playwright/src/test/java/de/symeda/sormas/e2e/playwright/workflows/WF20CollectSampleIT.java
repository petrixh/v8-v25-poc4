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

/** migration/workflows/WF20-collect-sample.md */
@DisplayName("WF20 Record a sample for a case (SurvOff, C1)")
@AppCopy(AppCopy.Config.C1)
class WF20CollectSampleIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvOff");
	}

	@Test
	@Order(1)
	@DisplayName("1. Open SCAS03: its side panel shows no samples")
	void step1() {
		ui.openView("Cases");
		ui.fillSearch("caseLike", "SCAS03");
		ui.applyFilters();
		ui.settle(1500);
		ui.clickGridText("SCAS03");
		ui.settle(2000);
		String t = ui.pageText();
		assertTrue(t.contains("Charlie SEEDPERSON") && t.contains("SCAS03") && t.contains("Measles"), "case shown");
		assertTrue(t.contains("There are no samples for this case"), "no samples");
	}

	@Test
	@Order(2)
	@DisplayName("2. New sample: collected and shipped today, Blood, Default Laboratory; save: listed in the case's samples")
	void step2() {
		ui.clickEntry("New sample");
		ui.settle(1500);
		assertTrue(ui.windows().getByText("Create new sample", new Locator.GetByTextOptions().setExact(true)).isVisible(), "form title");
		ui.fillDate(ui.dateInput(0, true), en(0), 600);
		ui.chooseCombo("Type of sample", "Blood");
		ui.chooseCombo("Laboratory", "Default Laboratory");
		ui.windowClickText("Sent/dispatched");
		ui.settle(800);
		ui.fillField("Date sample was sent", en(0));
		assertEquals("Blood", ui.field("Type of sample").inputValue(), "material");
		assertEquals("Default Laboratory", ui.field("Laboratory").inputValue(), "lab");
		ui.windowButton("Save");
		ui.settle(2500);
		String t = ui.pageText();
		assertTrue(Pattern.compile("BLOOD\\s*PENDING\\s*SHIPPED", Pattern.CASE_INSENSITIVE).matcher(t).find(), "listed");
		assertTrue(t.contains("Date sample was collected: " + en(0)), "collected");
		assertTrue(t.contains("Default Laboratory") && t.contains("Number of tests: 0"), "lab shown");
	}

	@Test
	@Order(3)
	@DisplayName("3. The case's lab results tab: the sample is listed without a result yet")
	void step3() {
		ui.tab("Laboratory results").click();
		ui.settle(2000);
		String t = ui.pageText();
		assertTrue(t.contains("No tests have been performed for this case"), "no tests");
		assertTrue(t.contains("Blood") && t.contains("Default Laboratory") && t.contains(en(0)), "sample listed");
	}

	@Test
	@Order(4)
	@DisplayName("4. Sample directory, search Charlie: shipped, not received, pending result")
	void step4() {
		ui.openView("Samples");
		ui.fillSearch("caseCodeIdLike", "Charlie");
		ui.applyFilters();
		ui.settle(2000);
		List<String> rows = ui.rowTexts();
		assertEquals(1, rows.size(), "rows: " + rows);
		for (String x : List.of(
			"Charlie SEEDPERSON (SCAS03)",
			"Measles",
			"Default District",
			"Yes No " + en(0),
			"Default Laboratory",
			"Blood",
			"External lab testing",
			"Pending")) {
			assertTrue(rows.get(0).contains(x), "row has " + x + ": " + rows.get(0));
		}
		// Data check: one new sample of SCAS03 at the Default Laboratory, shipped, not received
		assertEquals(
			"1|BLOOD|t|f|Default Laboratory",
			copy.sql(
				"SELECT count(*) OVER (), s.samplematerial, s.shipped, s.received, f.name FROM samples s JOIN cases c ON c.id = s.associatedcase_id"
					+ " JOIN facility f ON f.id = s.lab_id WHERE c.uuid LIKE 'SCAS03%' AND NOT s.deleted"));
	}
}
