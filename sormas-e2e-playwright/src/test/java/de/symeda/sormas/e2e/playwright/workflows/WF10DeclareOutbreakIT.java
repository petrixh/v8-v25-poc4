package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF10-declare-outbreak.md */
@DisplayName("WF10 Declare and end a disease outbreak in a district (SurvSup, C1)")
@AppCopy(AppCopy.Config.C1)
class WF10DeclareOutbreakIT extends WorkflowTest {

	private static final List<String> FULL_TABS =
		List.of("CASES LIST", "CASE", "CASE PERSON", "HOSPITALIZATION", "SYMPTOMS", "EPIDEMIOLOGICAL DATA", "LABORATORY RESULTS", "CONTACTS");

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvSup");
	}

	private String cellText(String disease) {
		return ui.outbreakCell(disease).innerText().trim();
	}

	/** The cell's text once it shows the value (polled up to 5 s), else the text it shows. */
	private String cellBecomes(String disease, String value) {
		for (int i = 0; i < 25; i++) {
			if (value.equals(cellText(disease))) {
				return value;
			}
			ui.page.waitForTimeout(200);
		}
		return cellText(disease);
	}

	private void openOutbreaks() {
		ui.openView("Configuration");
		ui.waitText("Outbreaks Configuration", 10_000);
	}

	private void setDistrict(boolean outbreak) {
		ui.selectOption(outbreak ? "Outbreak" : "Normal", true);
		ui.windowButton("Save");
		ui.waitText("Outbreak information saved", 10_000);
	}

	private void openScas03() {
		ui.openView("Cases");
		ui.fillSearch("caseLike", "SCAS03");
		ui.applyFilters();
		ui.settle(1000);
		ui.clickGridText("SCAS03");
		ui.waitText("Case data", 15_000);
		ui.settle(800);
	}

	@Test
	@Order(1)
	@DisplayName("1. Open Configuration, outbreaks: a matrix of regions and diseases shows no active outbreak (every cell 0)")
	void step1() {
		openOutbreaks();
		assertTrue(ui.pageText().contains("Outbreaks Configuration"), "title");
		List<String> vals = ui.outbreakCellTexts();
		assertTrue(!vals.isEmpty() && vals.stream().allMatch(v -> v.trim().equals("0")), "no outbreak: " + vals);
	}

	@Test
	@Order(2)
	@DisplayName("2. Mark Measles in the Default district as an outbreak and save: \"Outbreak information saved\", the Measles cell shows 1/1")
	void step2() {
		ui.outbreakCell("Measles").click();
		ui.waitText("Measles Outbreak in Default Region", 10_000);
		assertTrue(ui.windowText().contains("Measles Outbreak in Default Region"), "window title");
		setDistrict(true);
		assertTrue(ui.pageText().contains("Outbreak information saved"), "saved message");
		assertEquals("1/1", cellBecomes("Measles", "1/1"), "Measles cell");
	}

	@Test
	@Order(3)
	@DisplayName("3. Open the dashboard and its Measles tab: the outbreak shows as 1 outbreak district")
	void step3() {
		ui.openView("Dashboard");
		ui.settle(1500);
		ui.clickLink("Measles");
		ui.waitMatch("OUTBREAK DISTRICTS [1-9]", 10_000);
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("OUTBREAK DISTRICTS (\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(ui.pageText());
		assertTrue(m.find(), "outbreak districts shown");
		assertEquals("1", m.group(1), "outbreak districts");
	}

	@Test
	@Order(4)
	@DisplayName("4. Open SCAS03 (measles): the case opens with its full set of tabs (the reduced outbreak view is only for meningitis)")
	void step4() {
		openScas03();
		String t = ui.pageText();
		assertTrue(t.contains("SCAS03") && t.contains("Measles"), "case shown");
		String tabs = String.join(" | ", ui.tabTexts());
		for (String x : FULL_TABS) {
			assertTrue(tabs.contains(x), "tab " + x + " in " + tabs);
		}
	}

	@Test
	@Order(5)
	@DisplayName("5. Set the Default district back to normal: the Measles cell shows 0; SCAS03 still shows the full set of tabs; no active outbreak remains")
	void step5() {
		openOutbreaks();
		ui.outbreakCell("Measles").click();
		ui.settle(800);
		setDistrict(false);
		assertTrue(ui.pageText().contains("Outbreak information saved"), "saved message");
		assertEquals("0", cellBecomes("Measles", "0"), "Measles cell");
		openScas03();
		String tabs = String.join(" | ", ui.tabTexts());
		for (String x : FULL_TABS) {
			assertTrue(tabs.contains(x), "tab " + x + " in " + tabs);
		}
		// Data check: no active outbreak at the end
		assertEquals("0", copy.sql("SELECT count(*) FROM outbreak WHERE enddate IS NULL OR enddate > now()"));
	}
}
