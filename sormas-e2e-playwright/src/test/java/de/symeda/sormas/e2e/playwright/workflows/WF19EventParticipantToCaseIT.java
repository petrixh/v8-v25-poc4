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

/** migration/workflows/WF19-event-participant-to-case.md */
@DisplayName("WF19 Add a participant to an event and make them a case (SurvOff, C1)")
@AppCopy(AppCopy.Config.C1)
class WF19EventParticipantToCaseIT extends WorkflowTest {

	private static final String PHONE = "0401234567";
	private static final Pattern CREATE = Pattern.compile("\\bCreate\\b");
	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvOff");
	}

	private Locator wfield(String label) {
		return ui.windowField(label, true);
	}

	private void wcombo(Locator input, String text) {
		ui.comboPick(input, text, text, false, 500, 700);
	}

	private void wfill(String label, String value) {
		wfield(label).fill(value);
		wfield(label).press("Tab");
		ui.settle(400);
	}

	private void openParticipants() {
		ui.openView("Events");
		ui.fillSearch("freeText", "SEVT01");
		ui.applyFilters();
		ui.settle(1500);
		ui.clickGridText("SEVT01");
		ui.settle(2000);
		ui.tab("Event participants").click();
		ui.settle(2000);
	}

	@Test
	@Order(1)
	@DisplayName("1. Open SEVT01 and its participants tab: Foxtrot Seedperson is listed")
	void step1() {
		openParticipants();
		List<String> rows = ui.rowTexts();
		assertEquals(1, rows.size(), "participants: " + rows);
		assertTrue(rows.get(0).contains("Foxtrot") && rows.get(0).contains("Seedperson"), "Foxtrot");
	}

	@Test
	@Order(2)
	@DisplayName("2. New participant Romeo Participant, Male, involvement Organiser; save: the participant view opens")
	void step2() {
		ui.clickEntry(Pattern.compile("^add participant$", Pattern.CASE_INSENSITIVE));
		ui.settle(1500);
		assertTrue(ui.windows().last().getByText("Add new event participant", new Locator.GetByTextOptions().setExact(true)).isVisible(), "form title");
		wfill("Involvement description", "Organiser");
		wfill("First name", "Romeo");
		wfill("Last name", "Participant");
		wcombo(wfield("Sex"), "Male");
		ui.windowButton("Save");
		ui.settle(2500);
		String t = ui.pageText();
		assertTrue(t.contains("The new event participant was created"), "created");
		assertTrue(t.contains("Romeo PARTICIPANT"), "person");
		assertTrue(t.contains("SEVT01"), "event");
		assertTrue(ui.inputValues(false).contains("Organiser"), "involvement");
	}

	@Test
	@Order(3)
	@DisplayName("3. Participant's person tab: person data editable; add a phone number and save")
	void step3() {
		ui.tab("Event participant person").click();
		ui.settle(2000);
		assertEquals("Romeo", ui.field("First name").inputValue(), "first name");
		assertEquals("Participant", ui.field("Last name").inputValue(), "last name");
		assertEquals("Male", ui.field("Sex").inputValue(), "sex");
		ui.clickEntry("New entry", 1); // the contact details list (the first "New entry" is the address list)
		ui.settle(1500);
		wcombo(ui.windowTextInput(0), "Phone");
		wfield("Contact information").fill(PHONE);
		wfield("Contact information").press("Tab");
		ui.settle(500);
		ui.windowButton("Done");
		ui.settle(1200);
		assertTrue(ui.pageText().contains(PHONE), "phone listed");
		ui.button("Save").click();
		ui.settle(2000);
		String note = ui.notificationTexts();
		assertTrue(Pattern.compile("saved", Pattern.CASE_INSENSITIVE).matcher(note).find(), "saved: " + note);
	}

	@Test
	@Order(4)
	@DisplayName("4. Back on the participants tab: two participants")
	void step4() {
		ui.clickEntry("Event participants");
		ui.settle(2000);
		List<String> rows = ui.rowTexts();
		assertEquals(2, rows.size(), "participants: " + rows);
		assertTrue(rows.stream().anyMatch(r -> r.contains("Romeo") && r.contains("Participant") && r.contains("Organiser")), "Romeo");
	}

	@Test
	@Order(5)
	@DisplayName("5. Create a case from Romeo Participant: prefilled person and COVID-19; report date, jurisdiction, facility; save")
	void step5() {
		ui.clickInRow(ui.rowIndex("Romeo"), "Create", true);
		ui.settle(2000);
		assertTrue(ui.windows().last().getByText("Create new case", new Locator.GetByTextOptions().setExact(true)).isVisible(), "form title");
		List<String> vals = ui.inputValues(true);
		assertTrue(vals.contains("Romeo") && vals.contains("Participant"), "person");
		assertTrue(vals.contains("COVID-19"), "disease");
		boolean preset = vals.contains("Default Region") && vals.contains("Default District");
		wfield("Date of report").fill(en(0));
		wfield("Date of report").press("Tab");
		ui.settle(600);
		if (!preset) {
			wcombo(wfield("Responsible region"), "Default Region");
			wcombo(wfield("Responsible district"), "Default District");
		}
		wcombo(wfield("Responsible community"), "Default Community");
		ui.windowClickText("Facility");
		ui.settle(700);
		wcombo(wfield("Facility"), "Default Facility");
		ui.windowButton("Save");
		assertTrue(ui.waitText("Case saved", 15_000) || ui.waitText("Case data", 15_000), "case opened");
		ui.settle(1500);
		assertTrue(ui.pageText().contains("Romeo PARTICIPANT"), "title");
		assertEquals("COVID-19", ui.field("Disease").inputValue(), "disease");
	}

	@Test
	@Order(6)
	@DisplayName("6. Back on the participants tab: Romeo Participant shows the resulting case")
	void step6() {
		openParticipants();
		List<String> rows = ui.rowTexts();
		assertEquals(2, rows.size(), "participants: " + rows);
		String romeo = rows.stream().filter(r -> r.contains("Romeo")).findFirst().orElse("");
		String foxtrot = rows.stream().filter(r -> r.contains("Foxtrot")).findFirst().orElse("");
		assertTrue(!CREATE.matcher(romeo).find(), "Romeo's case shown instead of Create: " + romeo);
		assertTrue(CREATE.matcher(foxtrot).find(), "Foxtrot still without a case: " + foxtrot);
		// Data check: SEVT01 has two participants; Romeo has a new COVID-19 case linked to the participation
		assertEquals("2", copy.sql("SELECT count(*) FROM eventparticipant ep JOIN events e ON e.id = ep.event_id WHERE e.uuid LIKE 'SEVT01%' AND NOT ep.deleted"));
		assertEquals(
			"CORONAVIRUS",
			copy.sql(
				"SELECT c.disease FROM eventparticipant ep JOIN person p ON p.id = ep.person_id JOIN cases c ON c.id = ep.resultingcase_id"
					+ " WHERE p.firstname = 'Romeo' AND p.lastname = 'Participant'"));
	}
}
