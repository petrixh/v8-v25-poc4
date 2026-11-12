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

/** migration/workflows/WF18-contact-line-listing.md */
@DisplayName("WF18 Enter several contacts at once by line listing (SurvOff, C1)")
@AppCopy(AppCopy.Config.C1)
class WF18ContactLineListingIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvOff");
	}

	private Locator lineField(String label, int i) {
		return ui.windowFieldLikeAt(label, i);
	}

	private void fillLine(int i, String first, String last, String sex) {
		ui.fillDate(ui.lineDateInput(i, 0), en(0), 300);
		ui.fillDate(ui.lineDateInput(i, 1), en(-1), 300);
		lineField("First name", i).fill(first);
		lineField("First name", i).press("Tab");
		lineField("Last name", i).fill(last);
		lineField("Last name", i).press("Tab");
		ui.comboPickVisible(lineField("Sex", i), sex, sex, 400);
	}

	private String line(int i) {
		return String.join(" ", lineField("First name", i).inputValue(), lineField("Last name", i).inputValue(), lineField("Sex", i).inputValue());
	}

	@Test
	@Order(1)
	@DisplayName("1. Start line listing from the contact directory: a window with shared fields and one contact line")
	void step1() {
		ui.openView("Contacts");
		ui.button("Line listing").click();
		ui.waitText("Shared information", 10_000);
		assertTrue(String.valueOf(ui.windowTitle()).contains("Line listing"), "window title");
		String t = ui.windowText().toLowerCase();
		for (String x : List.of("Shared information", "Disease of source case", "Region", "District", "List of new contacts")) {
			assertTrue(t.contains(x.toLowerCase()), "window shows " + x);
		}
		assertEquals(1, ui.windowFieldCount("First name"), "one line");
	}

	@Test
	@Order(2)
	@DisplayName("2. Choose source case SCAS01 (disease COVID-19 follows); first line: Papa Contactline, Male, last contact 1 day before")
	void step2() {
		ui.windowButtonMatching(Pattern.compile("^choose case$", Pattern.CASE_INSENSITIVE));
		ui.windows().last().getByPlaceholder("Search...").last().waitFor();
		ui.windows().last().getByPlaceholder("Search...").last().fill("SCAS01");
		ui.windowButtonMatching(Pattern.compile("^search case$", Pattern.CASE_INSENSITIVE));
		ui.waitText("SCAS01", 10_000);
		ui.settle(500);
		ui.windowGridClickRow(0);
		ui.settle(300);
		ui.windowButtonMatching(Pattern.compile("^confirm$", Pattern.CASE_INSENSITIVE));
		ui.waitText("Alpha Seedperson (SCAS01)", 10_000);
		assertTrue(ui.windowText().contains("Alpha Seedperson (SCAS01)"), "source case");
		assertEquals("COVID-19", ui.windowFieldLike("Disease of source case").inputValue(), "disease");
		fillLine(0, "Papa", "Contactline", "Male");
		assertEquals("Papa Contactline Male", line(0), "line 1");
	}

	@Test
	@Order(3)
	@DisplayName("3. Add a second line: Quebec Contactline, Female, same date")
	void step3() {
		ui.windowButtonMatching(Pattern.compile("^add line$", Pattern.CASE_INSENSITIVE));
		ui.settle(600);
		assertEquals(2, ui.windowFieldCount("First name"), "two lines");
		fillLine(1, "Quebec", "Contactline", "Female");
		assertEquals("Quebec Contactline Female", line(1), "line 2");
		assertEquals(ui.lineDateInput(0, 0).inputValue(), ui.lineDateInput(1, 0).inputValue(), "same report date");
		assertEquals(ui.lineDateInput(0, 1).inputValue(), ui.lineDateInput(1, 1).inputValue(), "same last contact date");
	}

	@Test
	@Order(4)
	@DisplayName("4. Save: the window closes and both contacts appear in the contact directory with source case SCAS01")
	void step4() {
		ui.windowButton("Save");
		ui.noModal();
		assertEquals(0, ui.windowCount(), "window closed");
		ui.fillSearch("personLike", "Contactline");
		ui.applyFilters();
		ui.settle(1000);
		List<String> rows = ui.rowTexts();
		assertEquals(2, rows.size(), "rows: " + rows);
		assertTrue(rows.stream().anyMatch(r -> r.contains("Papa")) && rows.stream().anyMatch(r -> r.contains("Quebec")), "Papa and Quebec");
		assertTrue(rows.stream().allMatch(r -> r.contains("COVID-19") && r.contains("Unconfirmed contact") && r.contains("Active contact") && r.contains("Contactline")),
			"disease, classification, status: " + rows);
		ui.fillSearch("personLike", "");
		ui.fillSearch("contactOrCaseLike", "SCAS01");
		ui.applyFilters();
		ui.settle(1000);
		assertEquals(2, ui.rowTexts().stream().filter(r -> r.contains("Contactline")).count(), "with source case SCAS01");
		// Data check: two new contacts of SCAS01
		assertEquals(
			"2",
			copy.sql(
				"SELECT count(*) FROM contact ct JOIN person p ON p.id = ct.person_id JOIN cases c ON c.id = ct.caze_id WHERE c.uuid LIKE 'SCAS01%' AND p.lastname = 'Contactline'"));
	}
}
