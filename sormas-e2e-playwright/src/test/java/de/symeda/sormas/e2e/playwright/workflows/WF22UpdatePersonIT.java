package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF22-update-person.md */
@DisplayName("WF22 Update a person's contact details and addresses (SurvOff, C1)")
@AppCopy(AppCopy.Config.C1)
class WF22UpdatePersonIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvOff");
	}

	private Locator wf(String label) {
		return ui.windowFieldLike(label);
	}

	private void wfill(String label, String value) {
		wf(label).fill(value);
		wf(label).press("Tab");
	}

	private void wcombo(String label, String text) {
		ui.comboPick(wf(label), text, text, false, 500, 600);
	}

	private void newEntry(int n) {
		ui.clickEntry("New entry", n); // 0 addresses, 1 contact details
		ui.settle(800);
	}

	private void done() {
		ui.windowButton("Done");
		ui.noModal();
	}

	private void saveForm() {
		ui.savePage();
		ui.settle(1500);
	}

	private void openAlpha() {
		ui.openView("Persons");
		ui.fillSearch("nameAddressPhoneEmailLike", "Alpha");
		ui.applyFilters();
		ui.settle(1000);
		ui.clickGridText("SPER01");
		ui.waitText("Seedville", 15_000);
		ui.settle(500);
	}

	private void editRow(String text) {
		ui.tableRowClick(text);
		ui.settle(800);
	}

	private String phones() {
		Matcher m = Pattern.compile("\\+358 40 \\d{7}").matcher(ui.pageText());
		TreeSet<String> s = new TreeSet<>();
		while (m.find()) {
			s.add(m.group());
		}
		return String.join(", ", s);
	}

	@Test
	@Order(1)
	@DisplayName("1. Open the person directory and search \"Seedperson\": the seed persons are listed; narrow to Alpha")
	void step1() {
		ui.openView("Persons");
		assertTrue(ui.page.getByText("Person Directory", new com.microsoft.playwright.Page.GetByTextOptions().setExact(true)).first().isVisible(), "directory");
		ui.fillSearch("nameAddressPhoneEmailLike", "Seedperson");
		ui.applyFilters();
		ui.settle(1000);
		List<String> names = ui.rowTexts().stream().map(r -> String.join(" ", java.util.Arrays.asList(r.split(" ")).subList(1, 3))).sorted().toList();
		assertEquals(List.of("Alpha Seedperson", "Bravo Seedperson", "Charlie Seedperson", "Delta Seedperson"), names, "seed persons");
		ui.fillSearch("nameAddressPhoneEmailLike", "Alpha");
		ui.applyFilters();
		ui.settle(1000);
		List<String> r2 = ui.rowTexts();
		assertEquals(1, r2.size(), "narrowed");
		assertTrue(r2.get(0).contains("Alpha Seedperson") && r2.get(0).contains("Female") && r2.get(0).contains("Seedville"), "row: " + r2.get(0));
	}

	@Test
	@Order(2)
	@DisplayName("2. Open Alpha Seedperson: name, sex, birth date and the address in Seedville; the side panel lists case SCAS01")
	void step2() {
		ui.clickGridText("SPER01");
		ui.waitText("Seedville", 15_000);
		ui.settle(500);
		assertEquals("Alpha", ui.field("First name").inputValue(), "first name");
		assertEquals("Seedperson", ui.field("Last name").inputValue(), "last name");
		assertEquals("Female", ui.field("Sex").inputValue(), "sex");
		@SuppressWarnings("unchecked")
		List<String> b = (List<String>) ui.page.locator("vaadin-combo-box input").evaluateAll("es => es.map(e => e.value)");
		assertTrue(b.containsAll(List.of("1980", "March", "14")), "birth date: " + b);
		assertEquals("Example Street", ui.field("Street").inputValue(), "street");
		assertEquals("Seedville", ui.field("City").inputValue(), "city");
		String t = ui.pageText();
		assertTrue(t.contains("SCAS01") && t.contains("COVID-19"), "case panel");
	}

	@Test
	@Order(3)
	@DisplayName("3. Add two phone numbers and two extra addresses (work, holiday) in Seedville; save: both are listed")
	void step3() {
		for (String num : List.of("+358 40 1111111", "+358 40 2222222")) {
			newEntry(1);
			wcombo("Type of contact details", "Phone");
			wfill("Contact information", num);
			done();
		}
		newEntry(0);
		wcombo("Address type", "Place of work");
		wfill("Street", "Work Street");
		wfill("House number", "10");
		wfill("Additional information", "Second floor");
		wfill("Postal code", "00000");
		wfill("City", "Seedville");
		done();
		newEntry(0);
		wcombo("Address type", "Other address");
		wfill("Address name / description", "Holiday address");
		wfill("Street", "Holiday Road");
		wfill("House number", "5");
		wfill("Postal code", "00000");
		wfill("City", "Seedville");
		done();
		saveForm();
		String t = ui.pageText();
		assertEquals("+358 40 1111111, +358 40 2222222", phones(), "phones");
		assertTrue(t.contains("Place of work") && t.contains("Work Street"), "work address");
		assertTrue(t.contains("Holiday Road") || t.contains("Holiday address"), "holiday address");
	}

	@Test
	@Order(4)
	@DisplayName("4. Remove one phone number and one extra address (each confirmed); save and reload: one of each remains")
	void step4() {
		editRow("+358 40 2222222");
		ui.windowButton("Delete");
		ui.settle(500);
		ui.windowButtonMatching(Pattern.compile("^yes$", Pattern.CASE_INSENSITIVE));
		ui.noModal();
		editRow("Holiday Road");
		ui.windowButton("Delete");
		ui.settle(500);
		ui.windowButtonMatching(Pattern.compile("^yes$", Pattern.CASE_INSENSITIVE));
		ui.noModal();
		saveForm();
		openAlpha();
		assertEquals("+358 40 1111111", phones(), "phones");
		assertTrue(ui.pageText().contains("Work Street"), "work address kept");
		assertFalse(ui.pageText().contains("Holiday Road"), "holiday gone");
	}

	@Test
	@Order(5)
	@DisplayName("5. Clear the work address's additional information and save: reopening the address shows the field empty")
	void step5() {
		editRow("Work Street");
		assertEquals("Second floor", wf("Additional information").inputValue(), "before clearing");
		wf("Additional information").fill("");
		wf("Additional information").press("Tab");
		done();
		saveForm();
		openAlpha();
		editRow("Work Street");
		assertEquals("", wf("Additional information").inputValue(), "after clearing");
		assertEquals("Work Street", wf("Street").inputValue(), "street kept");
		done();
		// Data check: one phone number and one extra address; the cleared field is stored empty (null or empty text)
		assertEquals("1", copy.sql("SELECT count(*) FROM personcontactdetail d JOIN person p ON p.id = d.person_id WHERE p.uuid LIKE 'SPER01%' AND d.contactinformation LIKE '+358%'"));
		assertEquals(
			"Work Street|",
			copy.sql(
				"SELECT l.street || '|' || coalesce(l.additionalinformation, '') FROM person p JOIN person_locations pl ON pl.person_id = p.id"
					+ " JOIN location l ON l.id = pl.location_id WHERE p.uuid LIKE 'SPER01%'"));
	}
}
