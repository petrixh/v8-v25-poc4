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

/** migration/workflows/WF15-case-exposures.md */
@DisplayName("WF15 Record symptoms and exposures of a case (SurvOff, C1)")
@AppCopy(AppCopy.Config.C1)
class WF15CaseExposuresIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvOff");
	}

	/** type, start (= end) date offset, description, details (or null). COVID-19 has no "visit" type: Other with details "Visit". */
	private record Exposure(String type, int offset, String description, String details) {
	}

	private static final Exposure WORK = new Exposure("Work", -10, "Office shift", null);
	private static final Exposure TRAVEL = new Exposure("Travel", -8, "Train trip", null);
	private static final Exposure GATHERING = new Exposure("Gathering", -6, "Family party", null);
	private static final Exposure VISIT = new Exposure("Other", -5, "Visit to relatives", "Visit");

	private String line(Exposure e) {
		return e.type() + " " + en(e.offset()) + " " + e.description();
	}

	private void saveForm() {
		ui.savePage();
		assertTrue(ui.waitText("Case saved", 15_000), "Case saved");
		ui.dismissNotifications();
	}

	private void addExposure(Exposure e) {
		ui.clickEntry("New entry");
		ui.waitText("Start of exposure", 10_000);
		for (String l : List.of("Start of exposure", "End of exposure")) {
			ui.fillDate(ui.windowField(l, true), en(e.offset()), 300);
		}
		ui.comboPick(ui.windowField("Type of activity", true), e.type(), e.type(), false, 600, 800);
		if (e.details() != null) {
			ui.windowField("Type of activity details", true).fill(e.details());
			ui.windowField("Type of activity details", true).press("Tab");
		}
		ui.windowField("Description", true).fill(e.description());
		ui.windowField("Description", true).press("Tab");
		ui.windowButton("Done");
		ui.noModal();
		assertEquals(0, ui.windowCount(), "exposure window closed: " + e.type());
	}

	@Test
	@Order(1)
	@DisplayName("1. SCAS02, symptoms tab: fever and headache present, onset date 4 days before the reset date; save: the values show and the date reads as typed")
	void step1() {
		ui.openView("Cases");
		ui.fillSearch("caseLike", "SCAS02");
		ui.applyFilters();
		ui.settle(1000);
		ui.clickGridText("SCAS02");
		assertTrue(ui.waitText("Bravo SEEDPERSON", 15_000), "case opened");
		ui.tab("Symptoms").click();
		ui.waitText("Fever", 15_000);
		ui.settle(800);
		ui.setOptionGroup("Fever", "Yes");
		ui.setOptionGroup("Headache", "Yes");
		var onset = ui.field("Date of symptom onset");
		ui.fillDate(onset, en(-4), 500);
		saveForm();
		assertEquals("Yes", ui.optionGroupValue("Fever"), "fever");
		assertEquals("Yes", ui.optionGroupValue("Headache"), "headache");
		assertEquals("Yes", ui.optionGroupValue("Cough"), "cough");
		assertEquals(en(-4), onset.inputValue(), "onset date");
	}

	@Test
	@Order(2)
	@DisplayName("2. Change cough to No and leave the tab unsaved: the app warns; discard: the earlier saved values remain")
	void step2() {
		ui.setOptionGroup("Cough", "No");
		assertEquals("No", ui.optionGroupValue("Cough"), "cough changed");
		ui.tab("Epidemiological data").click();
		ui.waitText("Unsaved changes", 10_000);
		String wt = ui.windowText();
		assertTrue(Pattern.compile("Unsaved changes", Pattern.CASE_INSENSITIVE).matcher(wt).find()
			&& Pattern.compile("This form contains unsaved changes", Pattern.CASE_INSENSITIVE).matcher(wt).find(), "unsaved warning: " + wt);
		ui.windowButton("Discard changes");
		assertTrue(ui.waitText("Exposure Investigation", 15_000), "left to epidemiological data");
		ui.tab("Symptoms").click();
		ui.waitText("Fever", 15_000);
		ui.settle(800);
		assertEquals("Yes", ui.optionGroupValue("Fever"), "fever");
		assertEquals("Yes", ui.optionGroupValue("Headache"), "headache");
		assertEquals("Yes", ui.optionGroupValue("Cough"), "cough");
		assertEquals(en(-4), ui.field("Date of symptom onset").inputValue(), "onset date");
	}

	@Test
	@Order(3)
	@DisplayName("3. Epidemiological data: exposure details known, three exposures (work, travel, gathering) 10, 8 and 6 days before; save: three listed in that order")
	void step3() {
		ui.tab("Epidemiological data").click();
		ui.waitText("Exposure Investigation", 15_000);
		ui.setOptionGroup("Exposure details known", "Yes");
		for (Exposure e : List.of(WORK, TRAVEL, GATHERING)) {
			addExposure(e);
		}
		saveForm();
		assertEquals(String.join(" | ", line(WORK), line(TRAVEL), line(GATHERING)), String.join(" | ", ui.rowTexts()), "exposures");
	}

	@Test
	@Order(4)
	@DisplayName("4. Fourth exposure, a visit (type Other, details \"Visit\", 5 days before); save: four are listed")
	void step4() {
		addExposure(VISIT);
		saveForm();
		List<String> rows = ui.rowTexts();
		assertEquals(4, rows.size(), "count: " + rows);
		assertEquals(line(VISIT), rows.get(3), "visit");
	}

	@Test
	@Order(5)
	@DisplayName("5. Remove the first exposure, save and reload: three remain (travel, gathering, visit) with dates and descriptions; the data is stored")
	void step5() {
		ui.editTableRowAt(0);
		ui.waitText("Start of exposure", 10_000);
		assertEquals("Office shift", ui.windowField("Description", true).inputValue(), "edits the work exposure");
		ui.windowButton("Delete");
		ui.waitText("Are you sure you want to delete this Exposure", 10_000);
		assertTrue(Pattern.compile("Are you sure you want to delete this Exposure", Pattern.CASE_INSENSITIVE).matcher(ui.windowText()).find(), "confirm deletion");
		ui.windowButtonMatching(Pattern.compile("^yes$", Pattern.CASE_INSENSITIVE));
		ui.noModal();
		saveForm();
		ui.page.reload();
		assertTrue(ui.waitText("Exposure Investigation", 30_000), "still on epidemiological data");
		ui.settle(1000);
		assertEquals(String.join(" | ", line(TRAVEL), line(GATHERING), line(VISIT)), String.join(" | ", ui.rowTexts()), "exposures");
		ui.editTableRowAt(2);
		ui.waitText("Start of exposure", 10_000);
		assertEquals("Other", ui.windowField("Type of activity", true).inputValue(), "visit type");
		assertEquals(en(VISIT.offset()), ui.windowField("Start of exposure", true).inputValue(), "visit start");
		assertEquals(VISIT.description(), ui.windowField("Description", true).inputValue(), "visit description");
		ui.windowButton("Discard");
		ui.noModal();
		assertEquals(0, ui.windowCount(), "window closed");
		// Data check: SCAS02 has fever and headache with the onset date; exactly three exposures with the dates entered
		assertEquals(
			"YES|YES|" + today.plusDays(-4),
			copy.sql(
				"SELECT s.fever || '|' || s.headache || '|' || s.onsetdate::date FROM cases c JOIN symptoms s ON s.id = c.symptoms_id WHERE c.uuid LIKE 'SCAS02%'"));
		assertEquals(
			String.join("|", today.plusDays(-8).toString(), today.plusDays(-6).toString(), today.plusDays(-5).toString()),
			copy.sql(
				"SELECT string_agg(x.startdate::date::text, '|' ORDER BY x.startdate) FROM exposures x JOIN epidata ed ON ed.id = x.epidata_id"
					+ " JOIN cases c ON c.epidata_id = ed.id WHERE c.uuid LIKE 'SCAS02%'"));
	}
}
