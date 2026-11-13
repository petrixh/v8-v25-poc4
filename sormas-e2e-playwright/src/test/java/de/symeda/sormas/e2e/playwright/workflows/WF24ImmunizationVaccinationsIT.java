package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
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

/** migration/workflows/WF24-immunization-vaccinations.md */
@DisplayName("WF24 Record an immunization and its vaccinations (SurvOff, C1)")
@AppCopy(AppCopy.Config.C1)
class WF24ImmunizationVaccinationsIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvOff");
	}

	private Locator wf(String label) {
		return ui.windowFieldLike(label);
	}

	private void combo(String label, String text) {
		ui.comboPick(wf(label), text, text, true, 500, 700);
	}

	private void addVaccination(int daysBefore) {
		ui.clickEntryPrefix("New entry");
		ui.waitText("Vaccination date", 10_000);
		ui.fillDate(wf("Vaccination date"), en(-daysBefore), 400);
		combo("Vaccine name", "Other");
		ui.windowButton("Done");
		ui.noModal();
	}

	private void saveForm() {
		ui.savePage();
		ui.settle(1500);
	}

	/** Dates (M/D/YYYY) of the vaccination table rows, sorted as text like the replay compares them. */
	private List<String> vaccDates() {
		List<String> out = new ArrayList<>();
		for (String r : ui.rowTexts()) {
			Matcher m = Pattern.compile("\\d+/\\d+/\\d{4}").matcher(r);
			if (m.find()) {
				out.add(m.group());
			}
		}
		return out;
	}

	private static List<String> sorted(List<String> l) {
		return l.stream().sorted().toList();
	}

	@Test
	@Order(1)
	@DisplayName("1. New immunization: Measles, Vaccination, report date, Default jurisdiction, person Charlie Seedperson (existing person picked); save: the view opens")
	void step1() {
		ui.openView("Immunizations");
		assertTrue(ui.page.getByText("Immunization Directory", new com.microsoft.playwright.Page.GetByTextOptions().setExact(true)).first().isVisible(), "directory");
		ui.clickEntry(Pattern.compile("New immunization"));
		ui.waitText("Create new immunization", 10_000);
		assertTrue(ui.windowText().contains("Create new immunization"), "form title");
		ui.fillDate(wf("Date of report"), en(0), 400);
		combo("Disease", "Measles");
		combo("Means of immunization", "Vaccination");
		combo("Responsible region", "Default Region");
		combo("Responsible district", "Default District");
		ui.fillField("First name", "Charlie");
		ui.fillField("Last name", "Seedperson");
		ui.chooseCombo("Sex", "Male");
		ui.chooseBirthDate(1990, "November", 23);
		ui.windowButton("Save");
		assertTrue(ui.waitText("Pick or create person", 15_000), "pick window");
		assertTrue(Pattern.compile("Charlie\\s+Seedperson\\s+\\d+ \\(11/23/1990\\)\\s+Male").matcher(ui.windowText()).find(), "match listed");
		ui.windowButtonLast("Save");
		assertTrue(ui.waitText("Vaccinations", 15_000), "view opened");
		String t = ui.pageText();
		assertTrue(t.contains("Charlie SEEDPERSON") && t.contains("Vaccinations"), "title and vaccinations");
		assertEquals("Measles", ui.field("Disease").inputValue(), "disease");
		assertEquals("Vaccination", ui.field("Means of immunization").inputValue(), "means");
		assertEquals(en(0), ui.field("Date of report").inputValue(), "report date");
	}

	@Test
	@Order(2)
	@DisplayName("2. Add three vaccinations (400, 200 and 30 days before); save: three are listed")
	void step2() {
		for (int d : List.of(400, 200, 30)) {
			addVaccination(d);
		}
		saveForm();
		List<String> dates = vaccDates();
		assertEquals(3, dates.size(), "three: " + dates);
		assertEquals(sorted(List.of(en(-400), en(-200), en(-30))), sorted(dates), "dates");
	}

	@Test
	@Order(3)
	@DisplayName("3. Add a fourth vaccination 10 days before; save: four are listed")
	void step3() {
		addVaccination(10);
		saveForm();
		List<String> dates = vaccDates();
		assertEquals(4, dates.size(), "four: " + dates);
		assertEquals(sorted(List.of(en(-400), en(-200), en(-30), en(-10))), sorted(dates), "dates");
	}

	@Test
	@Order(4)
	@DisplayName("4. Remove the first vaccination (reason for deletion), save and reload: three remain with the right dates")
	void step4() {
		ui.tableRowClick(en(-400));
		ui.waitText("Delete", 10_000);
		ui.windowButton("Delete");
		ui.waitText("Reason for deletion", 10_000);
		Locator reason = wf("Reason for deletion");
		reason.click();
		reason.press("ArrowDown");
		ui.settle(400);
		ui.pickSuggestion("Entity created without legal reason", 400);
		ui.windowButtonMatching(Pattern.compile("^yes$", Pattern.CASE_INSENSITIVE));
		ui.noModal();
		saveForm();
		ui.page.reload();
		ui.waitText("Vaccinations", 30_000);
		ui.settle(1000);
		List<String> dates = vaccDates();
		assertEquals(3, dates.size(), "three: " + dates);
		assertEquals(sorted(List.of(en(-200), en(-30), en(-10))), sorted(dates), "dates");
	}

	@Test
	@Order(5)
	@DisplayName("5. Person tab: Charlie Seedperson; the directory lists the immunization for Measles; one measles immunization with three vaccinations is stored")
	void step5() {
		ui.tab(Pattern.compile("^person$", Pattern.CASE_INSENSITIVE)).click();
		ui.waitText("First name", 10_000);
		ui.settle(800);
		assertEquals("Charlie", ui.field("First name").inputValue(), "first name");
		assertEquals("Seedperson", ui.field("Last name").inputValue(), "last name");
		ui.openView("Immunizations");
		List<String> rows = ui.rowTexts().stream().filter(r -> r.contains("Charlie") && r.contains("Seedperson")).toList();
		assertEquals(1, rows.size(), "rows: " + rows);
		for (String x : List.of("Charlie Seedperson", "Measles", "Default District", "Vaccination")) {
			assertTrue(rows.get(0).contains(x), "row has " + x);
		}
		// Data check: one new measles immunization for Charlie Seedperson with exactly three vaccinations
		assertEquals(
			"1|3",
			copy.sql(
				"SELECT count(DISTINCT i.id) || '|' || count(v.id) FROM immunization i JOIN person p ON p.id = i.person_id"
					+ " LEFT JOIN vaccination v ON v.immunization_id = i.id WHERE p.firstname = 'Charlie' AND p.lastname = 'Seedperson' AND i.disease = 'MEASLES'"));
	}
}
