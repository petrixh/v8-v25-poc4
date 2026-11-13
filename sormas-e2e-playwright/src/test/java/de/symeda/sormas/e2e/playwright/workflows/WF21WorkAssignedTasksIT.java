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

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF21-work-assigned-tasks.md. Seed tasks STSK01 and STSK02; their due dates are fixed by the seed. */
@DisplayName("WF21 Work through assigned tasks (SurvOff, C1)")
@AppCopy(AppCopy.Config.C1)
class WF21WorkAssignedTasksIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvOff");
	}

	/** Task filter boxes in order: context, status, type. */
	private void pickFilter(int i, String typed, String text) {
		ui.comboPick(ui.filterCombo(i), typed, text, true, 600, 800);
	}

	private void apply() {
		ui.applyFilters();
		ui.settle(2000);
	}

	private static void assertRowHas(String row, String... parts) {
		for (String x : parts) {
			assertTrue(row.contains(x), "row has " + x + ": " + row);
		}
	}

	@Test
	@Order(1)
	@DisplayName("1. Task directory, my tasks: the two seed tasks with due dates, priorities and their case or contact")
	void step1() {
		ui.openView("Tasks");
		assertTrue(ui.page.getByText("Task Management", new Page.GetByTextOptions().setExact(true)).first().isVisible(), "directory");
		ui.clickEntry("My tasks");
		ui.settle(2000);
		List<String> rows = ui.rowTexts();
		String s1 = rows.stream().filter(r -> r.contains("Synthetic seed task") && r.contains("SCAS02")).findFirst().orElse("");
		String s2 = rows.stream().filter(r -> r.contains("Synthetic seed task") && r.contains("SCON01")).findFirst().orElse("");
		assertRowHas(s1, "SCAS02 (Bravo SEEDPERSON (SCAS02))", "case investigation", "Normal", "11/7/2026 11:00 AM", "pending");
		assertRowHas(s2, "SCON01 (Echo SEEDPERSON to case Alpha Seedperson (SCON01))", "contact follow up", "High", "11/6/2026 11:00 AM", "pending");
	}

	@Test
	@Order(2)
	@DisplayName("2. Filter to pending contact follow-up tasks: only STSK02 remains")
	void step2() {
		assertEquals("pending", Ui.promptOf(ui.filterCombo(1)), "status filter");
		pickFilter(2, "contact follow", "contact follow up");
		apply();
		List<String> rows = ui.rowTexts();
		assertEquals(1, rows.size(), "rows: " + rows);
		assertRowHas(rows.get(0), "SCON01", "contact follow up", "High", "pending");
	}

	@Test
	@Order(3)
	@DisplayName("3. Open STSK02, comment Called the contact, status Done; save: no longer among pending tasks")
	void step3() {
		ui.clickRow(ui.rowIndex("contact follow up"));
		ui.settle(1500);
		assertTrue(ui.windows().getByText("Edit task", new Locator.GetByTextOptions().setExact(true)).isVisible(), "form title");
		Locator c = ui.field("Comments on execution");
		c.click();
		ui.settle(800);
		if (ui.entryCount("Confirm") > 0) { // personal data hint
			ui.clickEntry("Confirm");
			ui.settle(600);
		}
		c.fill("Called the contact");
		c.press("Tab");
		ui.settle(600);
		ui.windowClickText(Pattern.compile("^done$", Pattern.CASE_INSENSITIVE));
		ui.settle(800);
		ui.windowButton("Save");
		ui.settle(2500);
		assertEquals(0, ui.windowsAfterClose(), "window closed");
		assertEquals(0, ui.rowTexts().size(), "no pending contact follow-up left");
	}

	@Test
	@Order(4)
	@DisplayName("4. Reset the filter, status Done: STSK02 is listed")
	void step4() {
		ui.resetFilters();
		ui.settle(2000);
		pickFilter(1, "done", "done");
		apply();
		List<String> rows = ui.rowTexts();
		assertEquals(1, rows.size(), "rows: " + rows);
		assertRowHas(rows.get(0), "SCON01", "contact follow up", "High", "Called the contact", "done");
	}

	@Test
	@Order(5)
	@DisplayName("5. Open the linked case of STSK01 from its row: SCAS02 opens")
	void step5() {
		ui.resetFilters();
		ui.settle(2000);
		ui.clickEntry("My tasks");
		ui.settle(2000);
		List<String> rows = ui.rowTexts();
		int idx = -1;
		for (int i = 0; i < rows.size(); i++) {
			if (rows.get(i).contains("Synthetic seed task") && rows.get(i).contains("SCAS02")) {
				idx = i;
				break;
			}
		}
		assertTrue(idx >= 0, "STSK01 found");
		ui.clickInRow(idx, "SCAS02", false);
		ui.settle(2500);
		String t = ui.pageText();
		assertTrue(t.contains("Bravo SEEDPERSON") && t.contains("(SCAS02)") && t.contains("COVID-19"), "case opened");
		// Data check: STSK02 done with the comment; STSK01 unchanged
		assertEquals("DONE|Called the contact", copy.sql("SELECT taskstatus, assigneereply FROM task WHERE uuid LIKE 'STSK02%'"));
		assertEquals("PENDING|", copy.sql("SELECT taskstatus, coalesce(assigneereply, '') FROM task WHERE uuid LIKE 'STSK01%'"));
	}
}
