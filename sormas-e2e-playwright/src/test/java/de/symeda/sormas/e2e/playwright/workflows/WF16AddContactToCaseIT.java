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

/** migration/workflows/WF16-add-contact-to-case.md */
@DisplayName("WF16 Add a contact to a case (SurvOff, C1)")
@AppCopy(AppCopy.Config.C1)
class WF16AddContactToCaseIT extends WorkflowTest {

	Ui ui;
	Locator win;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvOff");
		win = ui.windows();
	}

	private boolean formShown() {
		return win.getByText("Create new contact", new Locator.GetByTextOptions().setExact(true)).isVisible();
	}

	@Test
	@Order(1)
	@DisplayName("1. Open SCAS01 and its contacts tab: SCON01 (Echo Seedperson) is listed")
	void step1() {
		ui.openView("Cases");
		ui.fillSearch("caseLike", "SCAS01");
		ui.applyFilters();
		ui.settle(1500);
		ui.clickGridText("SCAS01");
		ui.settle(2000);
		ui.tab("Contacts").click();
		ui.settle(2000);
		List<String> rows = ui.rowTexts();
		assertEquals(2, rows.size(), "SCON01 and one generated contact: " + rows);
		String scon = rows.stream().filter(r -> r.contains("SCON01")).findFirst().orElse("");
		assertTrue(scon.contains("Echo") && scon.contains("Seedperson") && scon.contains("Confirmed contact"), "SCON01: " + scon);
	}

	@Test
	@Order(2)
	@DisplayName("2. New contact, save without names: blocked, the required fields flagged")
	void step2() {
		ui.clickEntry("New contact");
		ui.settle(1500);
		assertTrue(formShown(), "form title");
		ui.windowButton("Save");
		ui.settle(1200);
		assertTrue(formShown(), "still on the form");
		String msg = ui.notificationText();
		assertTrue(msg != null && msg.contains("Please check the input data"), "blocked: " + msg);
		for (String f : List.of("First name", "Last name", "Sex")) {
			assertTrue(msg.contains(f), f + " flagged: " + msg);
		}
	}

	@Test
	@Order(3)
	@DisplayName("3. Oscar Contactnew, Male, work environment, last contact 2 days back; save: the contact opens, linked to SCAS01")
	void step3() {
		ui.dismissNotifications();
		ui.fillField("First name", "Oscar");
		ui.fillField("Last name", "Contactnew");
		ui.chooseCombo("Sex", "Male");
		Locator dl = ui.field("Date of last contact");
		dl.fill(en(-2));
		dl.press("Tab");
		ui.settle(500);
		ui.comboPick(ui.field("Relationship with case"), "Work", "Work in the same environment", false, 600, 600);
		ui.windowButton("Save");
		assertTrue(ui.waitText("New contact created", 15_000), "created");
		ui.settle(1500);
		String t = ui.pageText();
		assertTrue(t.contains("Oscar CONTACTNEW to case Alpha SEEDPERSON"), "title");
		assertTrue(t.contains("Unconfirmed contact"), "classification");
		List<String> inputs = ui.inputValues(false);
		assertTrue(inputs.contains(en(-2)), "last contact");
		assertTrue(inputs.contains("Work in the same environment"), "relationship");
		assertTrue(
			Pattern.compile("CASE ID\\s+SCAS01\\b", Pattern.CASE_INSENSITIVE).matcher(t).find()
				&& Pattern.compile("DISEASE\\s+COVID-19", Pattern.CASE_INSENSITIVE).matcher(t).find(),
			"source case panel SCAS01 COVID-19");
	}

	@Test
	@Order(4)
	@DisplayName("4. Back on the case's contacts tab: three contacts")
	void step4() {
		ui.clickEntry("Case contacts");
		ui.settle(1800);
		List<String> rows = ui.rowTexts();
		assertEquals(3, rows.size(), "contacts: " + rows);
		assertTrue(rows.stream().anyMatch(r -> r.contains("Oscar") && r.contains("Contactnew")), "new contact listed");
	}

	@Test
	@Order(5)
	@DisplayName("5. Contact directory, search Contactnew: listed as Active, Unconfirmed")
	void step5() {
		ui.openView("Contacts");
		ui.fillSearch("personLike", "Contactnew");
		ui.applyFilters();
		ui.settle(1500);
		List<String> rows = ui.rowTexts();
		assertEquals(1, rows.size(), "rows: " + rows);
		for (String x : List.of("COVID-19", "Unconfirmed contact", "Active contact", "Oscar", "Contactnew")) {
			assertTrue(rows.get(0).contains(x), "row has " + x + ": " + rows.get(0));
		}
		// Data check: one new contact of SCAS01 with a new person; follow-up set by the app for the disease
		assertEquals(
			"1|CORONAVIRUS|UNCONFIRMED|ACTIVE|t",
			copy.sql(
				"SELECT count(*) OVER (), ct.disease, ct.contactclassification, ct.contactstatus, ct.followupuntil IS NOT NULL FROM contact ct"
					+ " JOIN person p ON p.id = ct.person_id JOIN cases c ON c.id = ct.caze_id WHERE p.lastname = 'Contactnew' AND c.uuid LIKE 'SCAS01%'"));
	}
}
