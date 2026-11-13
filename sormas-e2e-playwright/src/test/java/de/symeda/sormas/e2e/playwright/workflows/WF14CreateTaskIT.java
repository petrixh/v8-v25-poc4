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

/** migration/workflows/WF14-create-task.md */
@DisplayName("WF14 Create a task for a case and assign it to an officer (SurvSup, C1)")
@AppCopy(AppCopy.Config.C1)
class WF14CreateTaskIT extends WorkflowTest {

	private static final String COMMENT = "Check water source";

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvSup");
	}

	private void pick(String label, String typed, String regex) {
		ui.comboPick(ui.field(label), typed, Pattern.compile(regex, Pattern.CASE_INSENSITIVE), true, 600, 700);
	}

	@Test
	@Order(1)
	@DisplayName("1. Open SCAS04 and create a new task from its task list: the task form opens with the case already linked")
	void step1() {
		ui.openView("Cases");
		ui.fillSearch("caseLike", "SCAS04");
		ui.applyFilters();
		ui.settle(1000);
		ui.clickGridText("SCAS04");
		ui.waitText("Cholera", 15_000);
		assertTrue(ui.pageText().contains("SCAS04") && ui.pageText().contains("Cholera"), "case shown");
		ui.clickEntry("New task");
		ui.waitText("Create new task", 10_000);
		assertTrue(ui.windowText().contains("Create new task"), "form title");
		assertTrue(ui.inputValues(true).stream().anyMatch(v -> v.contains("(SCAS04)")), "case linked");
	}

	@Test
	@Order(2)
	@DisplayName("2. Case investigation, due in 3 days (two-digit years 70 and 30 read as 1970 and 2030 first), High, assignee SurvOff, comment (GDPR hint confirmed); save: listed in the case's tasks as pending")
	void step2() {
		pick("Task type", "Case inv", "^case investigation$");
		var due = ui.dateInput(1, true);
		// a typed two-digit year is read as in the old app: at most 10 years ahead, 89 back (ticket 3-07, CR04)
		ui.fillDate(due, "3/14/70", 400);
		assertEquals("3/14/1970", due.inputValue(), "two-digit year 70");
		ui.fillDate(due, "3/14/30", 400);
		assertEquals("3/14/2030", due.inputValue(), "two-digit year 30");
		ui.fillDate(due, en(3), 400);
		pick("Priority", "High", "^high$");
		pick("Assigned to", "Surv", "OFFICER");
		var c = ui.field("Comments on task");
		c.click();
		ui.settle(500);
		assertTrue(ui.page.getByText("GDPR", new com.microsoft.playwright.Page.GetByTextOptions().setExact(true)).count() > 0, "GDPR hint");
		ui.clickEntry("Confirm");
		ui.settle(300);
		c.fill(COMMENT);
		c.press("Tab");
		ui.settle(400);
		assertEquals("case investigation", ui.field("Task type").inputValue(), "type");
		assertEquals(en(3), due.inputValue(), "due");
		assertEquals("High", ui.field("Priority").inputValue(), "priority");
		assertEquals("Surveillance OFFICER", ui.field("Assigned to").inputValue().replaceAll("\\s*\\(\\d+\\)$", ""), "assignee");
		ui.windowButton("Save");
		ui.noModal();
		ui.waitText("Priority: High", 10_000);
		String t = ui.pageText();
		assertTrue(t.contains("Priority: High"), "listed High");
		assertTrue(t.contains("Due date: " + en(3)), "listed due");
		assertTrue(Pattern.compile("CASE INVESTIGATION PENDING Priority: High", Pattern.CASE_INSENSITIVE).matcher(t).find(), "listed pending");
	}

	@Test
	@Order(3)
	@DisplayName("3. Task directory, Officer tasks: the new task is listed with the case, the due date and SurvOff")
	void step3() {
		ui.openView("Tasks");
		ui.clickEntry("Officer tasks");
		ui.waitText(COMMENT, 10_000);
		String t = ui.rowTextWith(COMMENT).replaceAll("\\d+:\\d+ [AP]M", "<time>").replaceAll("\\(\\d+\\)", "");
		assertTrue(!t.isEmpty(), "row found");
		for (String x : List.of("SCAS04", "High", en(3), "Surveillance OFFICER", COMMENT, "pending")) {
			assertTrue(t.contains(x), "row has " + x + ": " + t);
		}
	}

	@Test
	@Order(4)
	@DisplayName("4. Open the task and change the priority to Normal; save: the list shows Normal; the task data is stored")
	void step4() {
		ui.clickRowWith(COMMENT);
		if (!ui.waitText("Edit task", 5000)) {
			ui.editTableRowAt(ui.rowIndex(COMMENT));
		}
		if (!ui.waitText("Edit task", 5000)) {
			ui.page.getByText("Check water source").first().click();
		}
		ui.waitText("Edit task", 10_000);
		assertTrue(ui.windowText().contains("Edit task"), "edit form");
		pick("Priority", "Normal", "^normal$");
		ui.windowButton("Save");
		ui.noModal();
		String t = ui.rowTextWith(COMMENT);
		assertTrue(Pattern.compile("\\bNormal\\b").matcher(t).find(), "priority now Normal: " + t);
		// Data check: one new pending task for SCAS04 (the case has two), assigned to SurvOff by SurvSup, priority Normal
		assertEquals("2", copy.sql("SELECT count(*) FROM task t JOIN cases c ON c.id = t.caze_id WHERE c.uuid LIKE 'SCAS04%'"));
		assertEquals(
			"PENDING|NORMAL|SurvOff|SurvSup",
			copy.sql(
				"SELECT t.taskstatus, t.priority, a.username, b.username FROM task t JOIN users a ON a.id = t.assigneeuser_id"
					+ " JOIN users b ON b.id = t.assignedbyuser_id WHERE t.creatorcomment = '" + COMMENT + "'"));
	}
}
