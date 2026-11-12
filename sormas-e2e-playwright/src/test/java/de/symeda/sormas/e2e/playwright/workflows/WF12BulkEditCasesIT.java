package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

/** migration/workflows/WF12-bulk-edit-cases.md */
@DisplayName("WF12 Bulk edit 40 or more cases with a progress window (SurvSup, C1)")
@AppCopy(AppCopy.Config.C1)
class WF12BulkEditCasesIT extends WorkflowTest {

	Ui ui;
	Locator progress;
	int pending;
	int discardedBefore;
	String discardedBeforeDb;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvSup");
		progress = ui.windowWith("Bulk operation progress");
		discardedBeforeDb = copy.sql("SELECT count(*) FROM cases WHERE investigationstatus = 'DISCARDED' AND NOT deleted");
	}

	/** The count badge of a status tab. */
	private int count(String tab) {
		Matcher m = Pattern.compile(Pattern.quote(tab) + "\\s*(\\d+)").matcher(ui.pageText());
		return m.find() ? Integer.parseInt(m.group(1)) : 0;
	}

	private void openTab(String tab) {
		ui.openStatusTab(tab);
		ui.settle(1500);
	}

	@Test
	@Order(1)
	@DisplayName("1. Case directory, investigation pending, enter bulk edit mode: selection boxes on every row")
	void step1() {
		ui.openView("Cases");
		openTab("Investigation discarded");
		discardedBefore = count("Investigation discarded");
		openTab("Investigation pending");
		pending = count("Investigation pending");
		assertTrue(pending >= 40, "at least 40 pending: " + pending);
		ui.button("More").click();
		ui.settle(600);
		ui.button(Pattern.compile("^enter bulk edit mode$", Pattern.CASE_INSENSITIVE)).click();
		ui.settle(1500);
		assertTrue(ui.button(Pattern.compile("^leave bulk edit mode$", Pattern.CASE_INSENSITIVE)).isVisible(), "leave button");
		assertTrue(ui.gridSelectionBoxes() > 0, "selection boxes");
	}

	@Test
	@Order(2)
	@DisplayName("2. Select all, Bulk Actions, Edit: the bulk edit window with a box per field group")
	void step2() {
		ui.gridSelectAll();
		ui.settle(1000);
		ui.chooseMenuBarItem("Bulk Actions", "Edit...");
		ui.settle(1500);
		assertTrue(ui.windows().getByText("Edit cases", new Locator.GetByTextOptions().setExact(true)).isVisible(), "title");
		String t = Ui.norm(ui.windows().innerText());
		for (String g : List.of(
			"Change Disease",
			"Change case classification",
			"Change investigation status",
			"Change case outcome",
			"Change surveillance officer",
			"Change facility")) {
			assertTrue(t.contains(g), "field group " + g);
		}
	}

	@Test
	@Order(3)
	@DisplayName("3. Investigation status Investigation discarded, save: a progress window for all selected cases")
	void step3() {
		ui.windowClickText("Change investigation status");
		ui.settle(500);
		ui.windowClickText("Investigation discarded");
		ui.settle(500);
		ui.windowButton("Save");
		progress.getByText("Bulk operation progress", new Locator.GetByTextOptions().setExact(true))
			.waitFor(new Locator.WaitForOptions().setTimeout(20_000));
		Matcher m = Pattern.compile("(\\d+) selected entries|/(\\d+) Completed").matcher(Ui.norm(progress.innerText()));
		assertTrue(m.find(), "total shown");
		assertEquals(pending, Integer.parseInt(m.group(1) != null ? m.group(1) : m.group(2)), "total selected");
	}

	@Test
	@Order(4)
	@DisplayName("4. The progress window reports all cases processed, none skipped or failed, and closes")
	void step4() {
		progress.getByText("All selected entries have been processed!").waitFor(new Locator.WaitForOptions().setTimeout(120_000));
		String t = Ui.norm(progress.innerText());
		assertTrue(t.contains(pending + "/" + pending + " Completed"), "completed: " + t);
		assertTrue(t.contains(pending + " Successful"), "successful: " + t);
		assertTrue(t.contains("0 Skipped"), "none skipped: " + t);
		ui.settle(500);
		ui.windowButton("Close");
		ui.settle(1000);
		assertFalse(progress.isVisible(), "closed");
	}

	@Test
	@Order(5)
	@DisplayName("5. Leave bulk edit mode, filter discarded: the processed cases plus those discarded before; none pending")
	void step5() {
		ui.button(Pattern.compile("^leave bulk edit mode$", Pattern.CASE_INSENSITIVE)).click();
		ui.settle(1500);
		openTab("Investigation discarded");
		assertEquals(discardedBefore + pending, count("Investigation discarded"), "discarded now");
		openTab("Investigation pending");
		assertEquals(0, count("Investigation pending"), "pending now");
		// Data check: every selected case discarded, changed by SurvSup; no other case changed
		assertEquals(
			(Integer.parseInt(discardedBeforeDb) + pending) + "|0|" + pending,
			copy.sql(
				"SELECT count(*) FILTER (WHERE investigationstatus = 'DISCARDED'), count(*) FILTER (WHERE investigationstatus = 'PENDING'),"
					+ " count(*) FILTER (WHERE c.changedate > now() - interval '30 minutes' AND u.username = 'SurvSup')"
					+ " FROM cases c LEFT JOIN users u ON u.id = c.change_user_id WHERE NOT c.deleted"));
	}
}
