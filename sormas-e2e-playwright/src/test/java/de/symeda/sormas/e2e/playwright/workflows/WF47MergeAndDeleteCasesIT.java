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

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF47-merge-and-delete-cases.md */
@DisplayName("WF47 Merge duplicate cases and delete a case (admin, NatUser; C1)")
@AppCopy(AppCopy.Config.C1)
class WF47MergeAndDeleteCasesIT extends WorkflowTest {

	Ui ui;
	/** Short ids of the two new cases (generated: only used to find the older one, never recorded). */
	final List<String> caseIds = new ArrayList<>();
	String olderId;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("admin");
	}

	private void more(Pattern item) {
		ui.clickEntry("More");
		ui.settle(400);
		ui.clickEntry(item);
		ui.settle(1500);
	}

	private void newCase() {
		ui.openView("Cases");
		ui.button("New case").click();
		ui.waitText("Create new case", 10_000);
		ui.chooseCombo("Disease", "Measles");
		ui.fillField("Date of report", en(0));
		ui.chooseCombo("Responsible region", "Default Region");
		ui.chooseCombo("Responsible district", "Default District");
		ui.chooseCombo("Responsible community", "Default Community");
		ui.chooseOption("Facility");
		ui.chooseCombo("Facility", "Default Facility");
		ui.fillField("First name", "Whiskey");
		ui.fillField("Last name", "Mergeperson");
		ui.chooseCombo("Sex", "Female");
		ui.chooseBirthDate(1990, "September", 9);
		ui.windowButton("Save");
	}

	private boolean opened() {
		boolean ok = ui.waitText("Case data", 20_000);
		ui.settle(600);
		Matcher m = Pattern.compile("\\(([A-Z0-9]{6})\\)\\W*CASES LIST", Pattern.CASE_INSENSITIVE).matcher(ui.pageText());
		if (m.find()) {
			caseIds.add(m.group(1));
		}
		ui.dismissNotifications();
		return ok;
	}

	@Test
	@Order(1)
	@DisplayName("1. As admin, two Measles cases for new persons both named Whiskey Mergeperson (the second as a new person when a match is offered): two cases exist")
	void step1() {
		newCase();
		assertTrue(opened(), "first case opened");
		newCase();
		ui.waitText("Pick or create person", 15_000);
		String wt = ui.windowText();
		assertTrue(wt.contains("Pick or create person"), "pick or create person");
		assertTrue(Pattern.compile("Whiskey Mergeperson \\d+ \\(9/9/1990\\) Female").matcher(wt).find(), "match offered: " + wt);
		ui.windowClickText("Create a new person");
		ui.settle(400);
		ui.windowButtonLast("Save");
		assertTrue(opened(), "second case opened");
		assertTrue(caseIds.size() == 2 && !caseIds.get(0).equals(caseIds.get(1)), "two case ids: " + caseIds);
		olderId = caseIds.get(0);
	}

	@Test
	@Order(2)
	@DisplayName("2. Case directory, More, merge duplicates, confirm the filters: the two cases are shown as one potential duplicate pair")
	void step2() {
		ui.openView("Cases");
		more(Pattern.compile("^merge duplicates$", Pattern.CASE_INSENSITIVE));
		assertTrue(ui.waitText("Merge Duplicate Cases", 15_000), "title");
		if (ui.windowCount() > 0) {
			ui.windowButtonMatching(Pattern.compile("^okay$", Pattern.CASE_INSENSITIVE));
			ui.noModal();
		}
		ui.clickEntry(Pattern.compile("confirm filters", Pattern.CASE_INSENSITIVE));
		ui.waitMatch("\\d+ potential duplicates detected", 20_000);
		ui.settle(800);
		Matcher d = Pattern.compile("(\\d+) potential duplicates detected").matcher(ui.pageText());
		assertTrue(d.find(), "detected message");
		assertEquals("1", d.group(1), "detected");
		List<String> rows = ui.rowTexts().stream().map(r -> r.replaceAll("\\s+", " ")).filter(r -> r.contains("Measles")).toList();
		assertEquals(2, rows.size(), "pair: " + rows);
		assertTrue(rows.stream().allMatch(r -> r.contains("Female") && r.contains("1990") && r.contains("Default District")), "pair matches");
		assertTrue(caseIds.stream().allMatch(id -> rows.stream().anyMatch(r -> r.contains(id))), "both new cases in the pair");
	}

	@Test
	@Order(3)
	@DisplayName("3. Merge them, keeping the older one: the pair is gone; as NatUser the case directory lists one Whiskey Mergeperson case, the older one")
	void step3() {
		ui.treeGridRowButton(olderId, Pattern.compile("merge", Pattern.CASE_INSENSITIVE));
		ui.waitText("Are you sure that the case you did not select is a duplicate", 10_000);
		assertTrue(Pattern.compile("Are you sure that the case you did not select is a duplicate of this case", Pattern.CASE_INSENSITIVE).matcher(ui.windowText()).find(), "confirmation");
		ui.windowButtonMatching(Pattern.compile("^confirm$", Pattern.CASE_INSENSITIVE));
		assertTrue(ui.waitText("Cases merged and duplicate case deleted", 15_000) || ui.notificationTexts().contains("Cases merged"), "merged message");
		ui.dismissNotifications();
		ui.settle(800);
		assertEquals(0, ui.rowTexts().stream().filter(r -> r.contains("Measles")).count(), "pair gone");
		ui.logout("Logout");
		ui.waitLoginForm(20_000);
		ui.login("NatUser");
		ui.openView("Cases");
		ui.fillSearch("personLike", "Mergeperson");
		ui.applyFilters();
		ui.settle(1000);
		List<String> rows = ui.rowTexts();
		assertEquals(1, rows.size(), "one case: " + rows);
		assertTrue(rows.get(0).contains(olderId), "the older case is kept");
		for (String x : List.of("Measles", "Whiskey", "Mergeperson", "Default District", "Default Facility", en(0))) {
			assertTrue(rows.get(0).contains(x), "row has " + x);
		}
	}

	private List<String> tasksFor(String q) {
		ui.openView("Tasks");
		ui.fillSearch("freeText", q);
		ui.applyFilters();
		ui.settle(1200);
		return ui.rowTexts();
	}

	@Test
	@Order(4)
	@DisplayName("4. As NatUser, delete SCAS02 with a reason (its task STSK01 is then no longer listed); the data is as expected")
	void step4() {
		List<String> before = tasksFor("SCAS02");
		assertEquals(1, before.stream().filter(r -> r.contains("SCAS02") && r.contains("Synthetic seed task")).count(), "STSK01 listed before");
		ui.openView("Cases");
		ui.resetFilters();
		ui.settle(800);
		ui.fillSearch("caseLike", "SCAS02");
		ui.applyFilters();
		ui.settle(1000);
		ui.clickGridText("SCAS02");
		assertTrue(ui.waitText("Bravo SEEDPERSON", 15_000), "case opened");
		ui.clickEntry(Pattern.compile("^delete$", Pattern.CASE_INSENSITIVE));
		ui.waitText("Reason for deletion", 10_000);
		assertTrue(Pattern.compile("Are you sure you want to delete this Case", Pattern.CASE_INSENSITIVE).matcher(ui.windowText()).find(), "confirm deletion");
		ui.typeInCombo(ui.windowField("Reason for deletion", true), "Entity created", false, 400, true);
		ui.pickSuggestion("Entity created without legal reason", 400);
		ui.windowButtonMatching(Pattern.compile("^yes$", Pattern.CASE_INSENSITIVE));
		ui.settle(1200);
		if (ui.windowCount() > 0 && Pattern.compile("Do you also want to delete all contacts of this case", Pattern.CASE_INSENSITIVE).matcher(ui.windowText()).find()) {
			ui.windowButtonMatching(Pattern.compile("^no$", Pattern.CASE_INSENSITIVE));
		}
		assertTrue(ui.waitText("Case Directory", 20_000), "back in the directory");
		ui.applyFilters();
		ui.settle(1200);
		assertEquals(0, ui.rowTexts().size(), "case gone");
		List<String> after = tasksFor("SCAS02");
		assertEquals(0, after.stream().filter(r -> r.contains("Synthetic seed task")).count(), "STSK01 not listed");
		// Data check: one Whiskey Mergeperson case remains (the other merged); SCAS02 is marked deleted, not removed
		assertEquals(
			"1",
			copy.sql(
				"SELECT count(*) FROM cases c JOIN person p ON p.id = c.person_id WHERE p.lastname = 'Mergeperson' AND c.deleted = false"));
		assertEquals("t", copy.sql("SELECT deleted FROM cases WHERE uuid LIKE 'SCAS02%'"));
	}
}
