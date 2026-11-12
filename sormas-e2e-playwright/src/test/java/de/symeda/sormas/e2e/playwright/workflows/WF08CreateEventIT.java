package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF08-create-event.md */
@DisplayName("WF08 Create an event (SurvSup, C1)")
@AppCopy(AppCopy.Config.C1)
class WF08CreateEventIT extends WorkflowTest {

	private static final String TITLE = "Workflow event: school outbreak";
	Ui ui;
	Locator win;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvSup");
		win = ui.windows();
	}

	private void pick(String label, String typed, String text) {
		ui.comboPick(ui.field(label), typed, text, false, 600, 700);
	}

	private boolean formShown() {
		return win.getByText("Create new event", new Locator.GetByTextOptions().setExact(true)).isVisible();
	}

	@Test
	@Order(1)
	@DisplayName("1. Open the event directory and start a new event: the new event form opens")
	void step1() {
		ui.openView("Events");
		assertTrue(ui.page.getByText("Event Directory", new Page.GetByTextOptions().setExact(true)).first().isVisible(), "directory");
		ui.button("New event").click();
		ui.settle(2000);
		assertTrue(formShown(), "form title");
		assertTrue(ui.field("Title").isVisible() && ui.field("Date of report").isVisible(), "fields");
		assertEquals(en(0), ui.field("Date of report").inputValue(), "report date preset");
	}

	@Test
	@Order(2)
	@DisplayName("2. Save with only a title: blocked, the required fields are flagged")
	void step2() {
		ui.fillField("Title", TITLE);
		ui.windowButton("Save");
		ui.settle(1500);
		assertTrue(formShown(), "still on the form");
		String note = ui.notificationText();
		assertTrue(note != null && note.contains("Please check the input data"), "blocked: " + note);
		assertTrue(note.contains("District"), "district flagged: " + note);
	}

	@Test
	@Order(3)
	@DisplayName("3. Status Event, Measles, start 3 days back, High risk, School, Default region, district, community; save: the event opens")
	void step3() {
		ui.dismissNotifications();
		ui.selectOption("Event", true);
		ui.settle(600);
		pick("Disease", "Meas", "Measles");
		// date fields of the window in order: date of report, date of event, signal evolution date
		ui.fillDate(ui.dateInput(1, true), en(-3), 600);
		pick("Epidemiological risk level", "High", "High risk");
		pick("Type of place", "School", "School");
		String region = ui.field("Region").inputValue();
		pick("District", "Default", "Default District");
		pick("Community", "Default", "Default Community");
		assertEquals("Default Region", region, "region");
		ui.windowButton("Save");
		assertTrue(ui.waitText("New event created", 15000), "event created");
		ui.settle(2000);
		assertTrue(ui.pageText().contains(TITLE), "title in header");
		ui.dismissNotifications();
		// the app opens the new event on its participants tab; the event data are on the "Event" tab
		ui.tab("Event").click();
		ui.settle(2000);
		assertEquals(0, win.count(), "no window");
		assertEquals(TITLE, ui.field("Title").inputValue(), "title");
		assertTrue(ui.optionChecked("Event", false), "status event");
		assertEquals("Measles", ui.field("Disease").inputValue(), "disease");
		assertEquals(en(-3), ui.dateInput(1, false).inputValue(), "start date");
		assertEquals("High risk", ui.field("Epidemiological risk level").inputValue(), "risk");
		assertEquals("School", ui.field("Type of place").inputValue(), "type of place");
		assertEquals("Default District", ui.field("District").inputValue(), "district");
		assertEquals("Default Community", ui.field("Community").inputValue(), "community");
	}

	@Test
	@Order(4)
	@DisplayName("4. Investigation status Ongoing, save: saved")
	void step4() {
		ui.dismissNotifications();
		ui.selectOption("Ongoing Investigation", false);
		ui.settle(600);
		ui.save();
		assertTrue(ui.waitText("saved", 8000), "saved");
		ui.settle(1500);
		assertTrue(ui.optionChecked("Ongoing Investigation", false), "ongoing");
	}

	@Test
	@Order(5)
	@DisplayName("5. Event directory, search school outbreak: listed as Event, Measles, High risk")
	void step5() {
		ui.dismissNotifications();
		ui.openView("Events");
		ui.fillSearch("freeText", "school outbreak");
		ui.applyFilters();
		ui.settle(1800);
		List<String> rows = ui.rowTexts();
		assertEquals(1, rows.size(), "rows: " + rows);
		for (String x : List.of(TITLE, "Event", "Measles", "High risk", "Default District")) {
			assertTrue(rows.get(0).contains(x), "row has " + x + ": " + rows.get(0));
		}
		// Data check: one new event with the values entered, located in the Default district
		assertEquals(
			"1|EVENT|MEASLES|HIGH|ONGOING|Default District",
			copy.sql(
				"SELECT count(*) OVER (), e.eventstatus, e.disease, e.risklevel, e.eventinvestigationstatus, d.name FROM events e"
					+ " JOIN location l ON l.id = e.eventlocation_id JOIN district d ON d.id = l.district_id WHERE e.eventtitle = '" + TITLE + "'"));
	}
}
