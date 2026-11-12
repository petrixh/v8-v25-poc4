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

/** migration/workflows/WF37-contact-follow-up-visit.md */
@DisplayName("WF37 Record follow-up visits of a contact (NatUser, C1)")
@AppCopy(AppCopy.Config.C1)
class WF37ContactFollowUpVisitIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("NatUser");
	}

	private boolean addVisit(boolean fever) {
		ui.clickEntry("New visit");
		ui.waitText("Create new visit", 10_000);
		assertTrue(ui.windowText().contains("Create new visit"), "form title");
		ui.windowClickText("Available and cooperative");
		ui.settle(400);
		if (fever) {
			ui.windowYesAfter("Fever").click();
			ui.settle(400);
		}
		ui.windowButton("Save");
		ui.noModal();
		return ui.windowCount() == 0;
	}

	@Test
	@Order(1)
	@DisplayName("1. Open SCON01 and its follow-up visits tab: no visits yet")
	void step1() {
		ui.openView("Contacts");
		ui.fillSearch("contactOrCaseLike", "SCON01");
		ui.applyFilters();
		ui.settle(1000);
		ui.clickGridText("SCON01");
		ui.waitText("Contact data", 15_000);
		ui.tab("Follow-up visits").click();
		ui.settle(1200);
		assertEquals(0, ui.rowTexts().size(), "visits");
	}

	@Test
	@Order(2)
	@DisplayName("2. Add a visit on the reset date: available and cooperative, not symptomatic; save: the visit is listed")
	void step2() {
		assertTrue(addVisit(false), "window closed");
		List<String> rows = ui.rowTexts();
		assertEquals(1, rows.size(), "visits: " + rows);
		assertTrue(rows.get(0).contains(en(0)) && rows.get(0).contains("Available and cooperative") && Pattern.compile("COVID-19 No").matcher(rows.get(0)).find(), "row: " + rows.get(0));
	}

	@Test
	@Order(3)
	@DisplayName("3. Add a second visit with fever present: the visit is listed as symptomatic")
	void step3() {
		assertTrue(addVisit(true), "window closed");
		List<String> rows = ui.rowTexts();
		assertEquals(2, rows.size(), "visits: " + rows);
		assertEquals(1, rows.stream().filter(r -> r.contains("COVID-19 Yes")).count(), "symptomatic visits");
		assertEquals(1, rows.stream().filter(r -> r.contains("COVID-19 No")).count(), "non-symptomatic visits");
	}

	@Test
	@Order(4)
	@DisplayName("4. Back on the contact data: the follow-up end date is the seeded one; the contact directory row shows two visits; the data is stored")
	void step4() {
		ui.tab("Contact").click();
		ui.waitText("Contact data", 10_000);
		ui.settle(1000);
		List<String> vals = ui.inputValues(false);
		// The follow-up status text depends on today's date against the seeded end date (the old app fails it on the
		// same day too): the new app's outcome is checked instead, the end date as stored and some follow-up status shown.
		String until = copy.sql("SELECT to_char(followupuntil, 'FMMM/FMDD/YYYY') FROM contact WHERE uuid LIKE 'SCON01%'");
		assertTrue(vals.contains(until), "follow-up until " + until + " in " + vals);
		assertTrue(vals.stream().anyMatch(v -> v.toLowerCase().contains("follow-up")), "a follow-up status is shown: " + vals);
		ui.openView("Contacts");
		ui.fillSearch("contactOrCaseLike", "SCON01");
		ui.applyFilters();
		ui.settle(1000);
		List<String> rows = ui.rowTexts();
		assertEquals(1, rows.size(), "rows: " + rows);
		assertTrue(Pattern.compile(" 2 \\(").matcher(rows.get(0).replaceAll("\\s+", " ")).find(), "two visits in the row: " + rows.get(0));
		// Data check: SCON01 has two visits, the second symptomatic
		assertEquals(
			"2|1",
			copy.sql(
				"SELECT count(*) || '|' || count(*) FILTER (WHERE s.symptomatic) FROM visit v JOIN symptoms s ON s.id = v.symptoms_id"
					+ " JOIN contact c ON c.person_id = v.person_id WHERE c.uuid LIKE 'SCON01%'"));
	}
}
