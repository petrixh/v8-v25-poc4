package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF05-case-list-filters.md */
@DisplayName("WF05 Find cases with filters that survive navigation (SurvSup, C1)")
@AppCopy(AppCopy.Config.C1)
class WF05CaseListFiltersIT extends WorkflowTest {

	Ui ui;
	long firstSortFirstRow;
	boolean firstSortAscending;
	boolean firstSortDescending;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvSup");
	}

	private Locator filterBox(String prompt) {
		return ui.comboByPrompt(prompt);
	}

	private void apply() {
		ui.applyFilters();
		ui.settle(800);
	}

	private int total() {
		return ui.statusCount("All");
	}

	private static long asTime(String d) {
		String[] p = d.split("/");
		return LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[0]), Integer.parseInt(p[1])).toEpochDay();
	}

	private List<Long> reportDates() {
		return ui.gridColumnTexts("^date of report$").stream().map(WF05CaseListFiltersIT::asTime).toList();
	}

	private static boolean ascending(List<Long> d) {
		for (int i = 1; i < d.size(); i++) {
			if (d.get(i - 1) > d.get(i)) {
				return false;
			}
		}
		return true;
	}

	private static boolean descending(List<Long> d) {
		for (int i = 1; i < d.size(); i++) {
			if (d.get(i - 1) < d.get(i)) {
				return false;
			}
		}
		return true;
	}

	@Test
	@Order(1)
	@DisplayName("1. Open the case directory: the case list shows the total number of cases")
	void step1() {
		ui.openView("Cases");
		assertEquals(84, total(), "total");
		assertFalse(ui.rowTexts().isEmpty(), "rows shown");
	}

	@Test
	@Order(2)
	@DisplayName("2. Search \"Seedperson\" and apply: only the four seed cases remain (Alpha, Bravo, Charlie, Delta)")
	void step2() {
		ui.fillSearch("personLike", "Seedperson");
		apply();
		List<String> rows = ui.rowTexts();
		assertEquals(4, rows.size(), "row count: " + rows);
		for (String n : List.of("Alpha", "Bravo", "Charlie", "Delta")) {
			assertTrue(rows.stream().anyMatch(r -> r.contains(n)), "row " + n);
		}
		assertEquals(4, total(), "count");
	}

	@Test
	@Order(3)
	@DisplayName("3. Clear the text, filter by disease COVID-19: only COVID-19 cases remain and the count drops")
	void step3() {
		ui.fillSearch("personLike", "");
		Locator box = filterBox("Disease");
		box.click();
		ui.settle(300);
		ui.typeInCombo(box, "COVID", true, 600, false);
		ui.pickSuggestion("COVID-19", 800);
		apply();
		List<String> rows = ui.rowTexts();
		int covid = total();
		assertFalse(rows.isEmpty(), "some rows");
		assertTrue(rows.stream().allMatch(r -> r.contains("COVID-19")), "all COVID-19");
		assertTrue(covid > 0 && covid < 84, "count dropped: " + covid);
	}

	@Test
	@Order(4)
	@DisplayName("4. Sort by report date both ways, scroll to the end: the order flips and the last rows load without gaps or duplicates")
	void step4() {
		ui.gridSortBy("^Date of report$");
		ui.settle(800);
		List<Long> d1 = reportDates();
		firstSortFirstRow = d1.get(0);
		firstSortAscending = ascending(d1);
		firstSortDescending = descending(d1);
		ui.gridSortBy("^Date of report$");
		ui.settle(800);
		List<Long> d2 = reportDates();
		assertTrue((firstSortAscending && descending(d2)) || (firstSortDescending && ascending(d2)), "order flipped");
		for (int i = 0; i < 6; i++) {
			ui.gridScrollEnd();
			ui.settle(500);
		}
		List<String> rows = ui.rowTexts();
		assertEquals(rows.size(), new HashSet<>(rows).size(), "no duplicates");
		assertTrue(!rows.isEmpty() && rows.stream().allMatch(r -> r.contains("COVID-19")), "rows rendered");
		List<Long> end = reportDates();
		assertEquals(firstSortFirstRow, end.get(end.size() - 1), "the end of the list holds the oldest report date");
		assertEquals(82, total(), "count still");
	}

	private String order() {
		List<Long> d = reportDates();
		return descending(d) ? "descending" : ascending(d) ? "ascending" : "unsorted";
	}

	@Test
	@Order(5)
	@DisplayName("5. Open a case, go back to the case directory: the COVID-19 filter is kept, the sort is back to the default")
	void step5() {
		ui.gridScrollTop();
		ui.settle(500);
		ui.clickRow(0);
		assertTrue(ui.waitText("Case data", 15_000), "case opened");
		ui.openView("Cases");
		assertEquals("COVID-19", filterBox("COVID").inputValue(), "filter kept");
		assertEquals(82, total(), "count kept");
		assertEquals("unsorted", order(), "sort not kept");
	}

	@Test
	@Order(6)
	@DisplayName("6. Reload (F5): the filter is still applied; a second tab opens the list without error and shows all cases")
	void step6() {
		ui.page.reload();
		ui.menu().waitFor();
		ui.settle(1500);
		assertTrue(ui.menuVisible(), "still logged in");
		if (!ui.pageText().contains("Case Directory")) {
			ui.openView("Cases");
		}
		assertEquals("COVID-19", filterBox("COVID").inputValue(), "filter kept after reload");
		assertEquals(82, total(), "count kept after reload");
		Ui second = newTab(ui);
		second.goTo("");
		second.menu().waitFor();
		if (!second.pageText().contains("Case Directory")) {
			second.openView("Cases");
		}
		second.settle(1500);
		assertTrue(second.pageText().contains("Case Directory") && !second.rowTexts().isEmpty(), "second tab list opens");
		assertEquals(84, second.statusCount("All"), "the filter is per tab");
		second.page.close();
	}

	@Test
	@Order(7)
	@DisplayName("7. Reset the filters: the full count is shown again")
	void step7() {
		ui.resetFilters();
		ui.settle(1000);
		assertEquals(84, total(), "full count");
		assertEquals("Disease", Ui.promptOf(filterBox("Disease")), "disease cleared");
	}
}
