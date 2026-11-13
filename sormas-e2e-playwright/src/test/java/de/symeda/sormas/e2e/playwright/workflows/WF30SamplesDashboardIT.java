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

/** migration/workflows/WF30-samples-dashboard.md */
@DisplayName("WF30 Samples dashboard counts and filters (LabOff, C1)")
@AppCopy(AppCopy.Config.C1)
class WF30SamplesDashboardIT extends WorkflowTest {

	Ui ui;
	int[] before;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("LabOff");
	}

	/** all, positive, pending, shipped, received */
	private int[] counts() {
		return new int[] {
			ui.numberIn("(\\d+) ?All Samples"),
			ui.numberIn("(\\d+) \\(\\d+ %\\) POSITIVE"),
			ui.numberIn("(\\d+) \\(\\d+ %\\) PENDING"),
			ui.numberIn("(\\d+) \\d+% SHIPPED"),
			ui.numberIn("(\\d+) \\d+% RECEIVED") };
	}

	@Test
	@Order(1)
	@DisplayName("1. Open the dashboard and switch to the samples dashboard: counts by result and shipment/receipt state are shown, no error")
	void step1() {
		ui.openView("Dashboard");
		ui.page.getByText("SAMPLES", new com.microsoft.playwright.Page.GetByTextOptions().setExact(true)).first().click();
		assertTrue(ui.waitText("Samples Dashboard", 15_000), "title");
		ui.waitMatch("All Samples", 10_000);
		ui.settle(1000);
		String t = ui.pageText();
		for (String x : List.of("Final Laboratory Results", "Shipment Status", "Specimen Condition", "Purpose of Sample")) {
			assertTrue(t.contains(x), "section " + x);
		}
		assertEquals(0, ui.notificationCount(), "no error notification");
		before = counts();
	}

	@Test
	@Order(2)
	@DisplayName("2. Period: the last 30 days before the reset date (custom, by date), Apply filters: the counts update and include the positive seed sample")
	void step2() {
		ui.setLastDays(today, 30);
		int[] c2 = counts();
		assertTrue(c2[0] > 0 && c2[0] != before[0], "counts updated: " + before[0] + " -> " + c2[0]);
		assertTrue(c2[1] >= 1, "positive seed sample counted");
		before = c2;
	}

	@Test
	@Order(3)
	@DisplayName("3. Disease COVID-19, Apply filters: the counts show the COVID-19 samples of the period and include the positive seed sample")
	void step3() {
		ui.pickDisease("COVID-19");
		int[] c = counts();
		assertEquals("COVID-19", ui.comboByValue("COVID-19").inputValue(), "disease filter set");
		assertTrue(c[0] <= before[0] && c[0] > 0, "not more: " + c[0] + " of " + before[0]);
		assertTrue(c[1] >= 1, "positive seed sample included");
	}
}
