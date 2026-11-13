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

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF27-jurisdiction-visibility.md */
@DisplayName("WF27 A case outside a user's jurisdiction stays invisible to that user (admin, SurvOff; C1)")
@AppCopy(AppCopy.Config.C1)
class WF27JurisdictionVisibilityIT extends WorkflowTest {

	Ui ui;
	int districtsBefore;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("admin");
	}

	@Test
	@Order(1)
	@DisplayName("1. As admin, add district \"Workflow North\" (region Default, epid code WFN): it is listed")
	void step1() {
		ui.openView("Configuration");
		ui.clickEntry(Pattern.compile("^districts$", Pattern.CASE_INSENSITIVE));
		ui.waitText("No. of districts", 10_000);
		districtsBefore = ui.numberIn("No\\. of districts:\\s*(\\d+)");
		assertFalse(ui.rowTexts().stream().anyMatch(r -> r.contains("Workflow North")), "not listed before");
		ui.clickEntry(Pattern.compile("new entry", Pattern.CASE_INSENSITIVE));
		ui.waitText("Create entry", 10_000);
		assertTrue(ui.windowText().contains("Create entry"), "form title");
		ui.windowField("Name", true).fill("Workflow North");
		ui.windowField("Epid code", true).fill("WFN");
		ui.comboPick(ui.windowField("Region", true), "Default", "Default Region", false, 600, 800);
		ui.windowButton("Save");
		assertTrue(ui.waitText("Entry created", 10_000) || ui.notificationTexts().contains("Entry created"), "created");
		ui.dismissNotifications();
		ui.noModal();
		String row = ui.rowTexts().stream().filter(x -> x.contains("Workflow North")).findFirst().orElse("");
		assertTrue(row.contains("Default Region") && row.contains("WFN"), "listed: " + row);
		assertEquals(districtsBefore + 1, ui.numberIn("No\\. of districts:\\s*(\\d+)"), "districts after");
	}

	@Test
	@Order(2)
	@DisplayName("2. Still as admin, a case for the new person Sierra Outside (female), Measles, region Default, district Workflow North, place of stay Home: the case opens")
	void step2() {
		ui.openView("Cases");
		ui.button("New case").click();
		ui.waitText("Create new case", 10_000);
		assertTrue(ui.windowText().contains("Create new case"), "form title");
		ui.chooseCombo("Disease", "Measles");
		ui.fillField("Date of report", en(0));
		ui.chooseCombo("Responsible region", "Default Region");
		ui.chooseCombo("Responsible district", "Workflow North");
		ui.chooseOption("Home");
		ui.fillField("First name", "Sierra");
		ui.fillField("Last name", "Outside");
		ui.chooseCombo("Sex", "Female");
		ui.windowButton("Save");
		assertTrue(ui.waitText("Case data", 15_000), "case opened");
		ui.settle(800);
		ui.dismissNotifications();
		assertEquals("Measles", ui.field("Disease").inputValue(), "disease");
		assertEquals("Workflow North", ui.field("Responsible district").inputValue(), "district");
		assertEquals(en(0), ui.field("Date of report").inputValue(), "report date");
	}

	@Test
	@Order(3)
	@DisplayName("3. Log out, log in as SurvOff (Default district), search the case directory for \"Outside\": no case is listed")
	void step3() {
		ui.logout("Logout");
		ui.waitLoginForm(20_000);
		assertTrue(ui.loginVisible(), "logged out");
		ui.login("SurvOff");
		ui.openView("Cases");
		assertTrue(ui.numberIn("All\\s*(\\d+)") > 0, "seed cases visible");
		ui.fillSearch("personLike", "Bravo");
		ui.applyFilters();
		ui.settle(1000);
		assertEquals(1, ui.rowTexts().stream().filter(r -> r.contains("Bravo Seedperson")).count(), "control: Bravo found");
		ui.fillSearch("personLike", "Outside");
		ui.applyFilters();
		ui.settle(1000);
		assertEquals(List.of(), ui.rowTexts(), "no rows");
		assertEquals(0, ui.numberIn("All\\s*(\\d+)"), "counter");
	}

	@Test
	@Order(4)
	@DisplayName("4. Person directory, \"Outside\" among case persons and then all association types: the person is not listed; the data is as expected")
	void step4() {
		ui.openView("Persons");
		ui.settle(500);
		ui.fillSearch("nameAddressPhoneEmailLike", "Bravo");
		ui.applyFilters();
		ui.settle(1000);
		assertEquals(1, ui.rowTexts().stream().filter(r -> r.contains("Bravo") && r.contains("Seedperson")).count(), "control: Bravo found");
		ui.fillSearch("nameAddressPhoneEmailLike", "Outside");
		ui.applyFilters();
		ui.settle(1000);
		assertEquals(List.of(), ui.rowTexts(), "case persons");
		assertEquals(0, ui.numberIn("Case\\s*(\\d+)"), "case counter");
		ui.openStatusTab("All");
		ui.settle(1000);
		if (ui.windowCount() > 0) {
			ui.windowClickText("Yes");
			ui.settle(1500);
		}
		assertEquals(List.of(), ui.rowTexts(), "all persons");
		assertEquals(0, ui.numberIn("All\\s*(\\d+)"), "all counter");
		// Data check: one new district and one case in it
		assertEquals("1", copy.sql("SELECT count(*) FROM district WHERE name = 'Workflow North'"));
		assertEquals("1", copy.sql("SELECT count(*) FROM cases c JOIN district d ON d.id = c.responsibledistrict_id WHERE d.name = 'Workflow North'"));
	}
}
