package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF09-event-actions-and-groups.md */
@DisplayName("WF09 Manage an event's actions and event groups (SurvSup, C1)")
@AppCopy(AppCopy.Config.C1)
class WF09EventActionsAndGroupsIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvSup");
	}

	private static boolean find(String regex, String text) {
		return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text).find();
	}

	@Test
	@Order(1)
	@DisplayName("1. Open SEVT01 from the event directory: its data view shows the title, COVID-19 and moderate risk")
	void step1() {
		ui.openView("Events");
		ui.fillSearch("freeText", "SEVT01");
		ui.applyFilters();
		ui.settle(1000);
		int row = ui.rowIndex("SEVT01");
		assertTrue(row >= 0, "listed");
		ui.clickRow(row);
		assertTrue(ui.waitText("Seed event: community gathering", 15_000) || ui.field("Title").isVisible(), "event opened");
		assertEquals("Seed event: community gathering", ui.field("Title").inputValue(), "title");
		assertEquals("COVID-19", ui.field("Disease").inputValue(), "disease");
		assertEquals("Moderate risk", ui.field("Epidemiological risk level").inputValue(), "risk");
	}

	@Test
	@Order(2)
	@DisplayName("2. New action \"Inform the venue\", priority High, status Pending; save: the Actions panel shows one pending action")
	void step2() {
		ui.button("New action").click();
		ui.settle(1000);
		assertTrue(ui.windowText().contains("Create new action"), "form title");
		Locator title = ui.windowField("Title", false);
		title.fill("Inform the venue");
		title.press("Tab");
		ui.comboPick(ui.windowField("Priority", false), "High", "High", true, 600, 700);
		assertTrue(ui.optionChecked("Pending", true), "pending preset");
		ui.windowButton("Save");
		ui.noModal();
		assertEquals(0, ui.windowCount(), "window closed");
		String t = ui.pageText();
		assertFalse(t.contains("There are no actions for this Event"), "no empty text");
		assertTrue(find("Actions\\W*NEW ACTION\\W*PENDING\\W*1(\\W|$)", t), "one pending: " + t);
	}

	@Test
	@Order(3)
	@DisplayName("3. Event actions tab: the action is listed with title, priority High, status Pending; change its status to Done: the list shows Done")
	void step3() {
		ui.tab("Event actions").click();
		ui.waitText("Inform the venue", 10_000);
		String t0 = ui.pageText();
		assertTrue(t0.contains("Inform the venue"), "action listed");
		assertTrue(find("Priority:\\s*High", t0), "priority High");
		assertTrue(find("Action status:\\s*Pending", t0), "status Pending");
		ui.openActionEntry("Inform the venue");
		ui.settle(800);
		assertEquals(1, ui.windowCount(), "edit window");
		ui.selectOption("Done", true);
		ui.windowButton("Save");
		ui.noModal();
		assertEquals(0, ui.windowCount(), "window closed");
		ui.waitText("Done", 10_000);
		String t1 = ui.pageText();
		assertTrue(find("Action status:\\s*Done", t1), "shows Done");
		assertFalse(find("Action status:\\s*Pending", t1), "no Pending");
	}

	@Test
	@Order(4)
	@DisplayName("4. Link the event to a new event group \"Workflow group\": the group is shown on the event with an event count of 1")
	void step4() {
		ui.tab("Event").click();
		ui.settle(1000);
		ui.button("Link event group").click();
		ui.settle(1000);
		ui.windowClickText("New event group");
		ui.settle(500);
		ui.windowButton("Save");
		ui.settle(800);
		assertTrue(ui.windowText().contains("Create new event group"), "form title");
		Locator name = ui.windowField("Group name", false);
		name.fill("Workflow group");
		name.press("Tab");
		ui.windowButton("Save");
		assertTrue(ui.waitText("New event group created", 8000), "created message");
		ui.noModal();
		assertEquals(0, ui.windowCount(), "window closed");
		String t = ui.pageText();
		assertTrue(find("workflow group", t), "group shown");
		assertFalse(find("no event groups", t), "no empty groups text");
		assertTrue(find("successfully linked to this event group", t), "linked note");
		assertTrue(find("event count:\\s*1", t), "event count 1");
	}

	@Test
	@Order(5)
	@DisplayName("5. Open the group's events with its list icon: the event directory, filtered by the group, lists SEVT01 as the one member")
	void step5() {
		ui.page.keyboard().press("Escape");
		ui.page.keyboard().press("Escape");
		ui.settle(500);
		ui.openEventGroupEvents();
		assertTrue(ui.waitText("Event Directory", 15_000), "directory shown");
		ui.settle(800);
		assertTrue(ui.page.url().contains("eventGroup="), "filtered by group");
		List<String> rows = ui.rowTexts();
		assertEquals(1, rows.size(), "rows: " + rows);
		for (String x : List.of("SEVT01", "Seed event: community gathering", "COVID-19")) {
			assertTrue(rows.get(0).contains(x), "member row has " + x);
		}
		// Data check: SEVT01 has one action (Done) and belongs to the group "Workflow group"
		assertEquals("1|DONE", copy.sql("SELECT count(*) || '|' || max(a.actionstatus) FROM action a JOIN events e ON e.id = a.event_id WHERE e.uuid LIKE 'SEVT01%'"));
		assertEquals(
			"Workflow group",
			copy.sql(
				"SELECT g.name FROM eventgroups g JOIN events_eventgroups eg ON eg.eventgroup_id = g.id JOIN events e ON e.id = eg.event_id"
					+ " WHERE e.uuid LIKE 'SEVT01%'"));
	}
}
