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

import com.microsoft.playwright.Locator;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF43-infrastructure-and-cache.md */
@DisplayName("WF43 Infrastructure changes show up in forms at once (admin, C1)")
@AppCopy(AppCopy.Config.C1)
class WF43InfrastructureAndCacheIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("admin");
	}

	private Locator wf(String label) {
		return ui.windowField(label, true);
	}

	private void wc(String label, String text) {
		ui.comboPick(wf(label), text, text, true, 600, 700);
	}

	private void cfgTab(String name) {
		ui.openView("Configuration");
		ui.clickEntry(name);
		ui.settle(1200);
	}

	private void openRowExact(String regex) {
		List<String> t = ui.rowTexts();
		int i = -1;
		for (int k = 0; k < t.size(); k++) {
			if (Pattern.compile(regex).matcher(t.get(k).replaceAll("\\s+", " ").trim()).find()) {
				i = k;
				break;
			}
		}
		assertTrue(i >= 0, "row " + regex + " in " + t);
		ui.clickRow(i);
		ui.settle(800);
	}

	private void openCaseForm() {
		ui.openView("Cases");
		ui.button("New case").click();
		ui.waitText("Create new case", 10_000);
	}

	private void discardCaseForm() {
		ui.windowButton("Discard");
		ui.noModal();
	}

	/** The suggestions a window combo box offers for the typed text. */
	private String offered(String label, String typed) {
		ui.typeInCombo(wf(label), typed, true, 400, true);
		for (int i = 0; i < 16 && ui.suggestionsText().isBlank(); i++) {
			ui.page.waitForTimeout(250);
		}
		ui.settle(300);
		return ui.suggestionsText().replaceAll("\\s+", " ").trim();
	}

	private void districtChosen() {
		ui.chooseCombo("Disease", "COVID-19");
		wc("Responsible region", "Default Region");
		wc("Responsible district", "Default District");
	}

	@Test
	@Order(1)
	@DisplayName("1. Configuration, communities: add \"Workflow Village\" in region and district Default; save: listed")
	void step1() {
		cfgTab("Communities");
		ui.clickEntry("New entry");
		ui.waitText("Name", 10_000);
		wf("Name").fill("Workflow Village");
		wc("Region", "Default Region");
		wc("District", "Default District");
		ui.windowButton("Save");
		ui.noModal();
		ui.waitRow("Workflow Village", 10_000);
		assertTrue(ui.rowTexts().stream().anyMatch(r -> r.contains("Workflow Village") && r.contains("Default Region") && r.contains("Default District")), "listed");
	}

	@Test
	@Order(2)
	@DisplayName("2. Facilities: add hospital \"Workflow Clinic\" in Workflow Village; save: listed")
	void step2() {
		cfgTab("Facilities");
		ui.clickEntry("New entry");
		ui.waitText("Name", 10_000);
		wf("Name").fill("Workflow Clinic");
		wc("Facility category", "Medical facility");
		wc("Facility type", "Hospital");
		wc("Region", "Default Region");
		wc("District", "Default District");
		wc("Community", "Workflow Village");
		ui.windowButton("Save");
		ui.noModal();
		ui.waitRow("Workflow Clinic", 10_000);
		String r = ui.rowTextWith("Workflow Clinic");
		assertTrue(r.contains("Hospital") && r.contains("Workflow Village") && r.contains("Default District"), "listed: " + r);
	}

	@Test
	@Order(3)
	@DisplayName("3. New case: choosing district Default offers Workflow Village, and choosing it offers Workflow Clinic; cancel")
	void step3() {
		openCaseForm();
		districtChosen();
		assertTrue(Pattern.compile("Workflow Village").matcher(offered("Responsible community", "Workflow")).find(), "village offered");
		ui.pickSuggestion("Workflow Village", 600);
		ui.windowClickText("Facility");
		ui.settle(500);
		wc("Facility category", "Medical facility");
		wc("Facility type", "Hospital");
		assertTrue(Pattern.compile("Workflow Clinic").matcher(offered("Facility", "Workflow")).find(), "clinic offered");
		ui.page.keyboard().press("Escape");
		ui.settle(300);
		discardCaseForm();
	}

	@Test
	@Order(4)
	@DisplayName("4. Rename the community to \"Workflow Town\"; save; a new case offers \"Workflow Town\", not the old name; cancel")
	void step4() {
		cfgTab("Communities");
		openRowExact("Workflow Village");
		ui.waitText("Name", 10_000);
		wf("Name").fill("Workflow Town");
		wf("Name").press("Tab");
		ui.windowButton("Save");
		ui.noModal();
		ui.waitRow("Workflow Town", 10_000);
		assertTrue(ui.rowTexts().stream().anyMatch(r -> r.contains("Workflow Town")) && ui.rowTexts().stream().noneMatch(r -> r.contains("Workflow Village")), "renamed listed");
		openCaseForm();
		districtChosen();
		String o = offered("Responsible community", "Workflow");
		assertTrue(o.contains("Workflow Town"), "town offered: " + o);
		assertFalse(o.contains("Workflow Village"), "village gone: " + o);
		ui.page.keyboard().press("Escape");
		ui.settle(300);
		discardCaseForm();
	}

	@Test
	@Order(5)
	@DisplayName("5. Archive Workflow Clinic: it leaves the active facility list and is no longer offered in a new case form")
	void step5() {
		cfgTab("Facilities");
		openRowExact("Workflow Clinic");
		ui.waitText("Archive", 10_000);
		ui.windowButton("Archive");
		ui.settle(600);
		boolean confirm = ui.windowButtonTexts().stream().anyMatch(t -> t.equalsIgnoreCase("confirm"));
		ui.windowButtonMatching(confirm ? Pattern.compile("^confirm$", Pattern.CASE_INSENSITIVE) : Pattern.compile("^yes$", Pattern.CASE_INSENSITIVE));
		ui.noModal();
		assertTrue(ui.waitNoRow("Workflow Clinic", 10_000), "gone from the active list");
		openCaseForm();
		districtChosen();
		wc("Responsible community", "Workflow Town");
		ui.windowClickText("Facility");
		ui.settle(500);
		wc("Facility category", "Medical facility");
		wc("Facility type", "Hospital");
		String o = offered("Facility", "Workflow");
		assertFalse(Pattern.compile("Workflow Clinic").matcher(o).find(), "clinic not offered: " + o);
		ui.page.keyboard().press("Escape");
		ui.settle(300);
		discardCaseForm();
	}

	@Test
	@Order(6)
	@DisplayName("6. Archive the Default district: the app asks \"Are you sure you want to archive this district?\"; answer No: the district stays active")
	void step6() {
		cfgTab("Districts");
		openRowExact("Default District");
		ui.waitText("Archive", 10_000);
		ui.windowButton("Archive");
		ui.waitText("Are you sure you want to archive this district", 10_000);
		assertTrue(Pattern.compile("Are you sure you want to archive this district").matcher(ui.windowText()).find(), "confirmation asked");
		ui.windowButtonMatching(Pattern.compile("^no$", Pattern.CASE_INSENSITIVE));
		ui.settle(500);
		if (ui.windowCount() > 0) {
			ui.closeWindow();
		}
		ui.noModal();
		assertTrue(ui.rowTexts().stream().anyMatch(r -> r.contains("Default District")), "district still active");
		// Data check: one community (renamed), one archived facility; the Default district unchanged
		assertEquals("1|0", copy.sql("SELECT count(*) FILTER (WHERE name = 'Workflow Town') || '|' || count(*) FILTER (WHERE name = 'Workflow Village') FROM community"));
		assertEquals("t", copy.sql("SELECT archived FROM facility WHERE name = 'Workflow Clinic'"));
		assertEquals("f", copy.sql("SELECT archived FROM district WHERE name = 'Default District'"));
	}
}
