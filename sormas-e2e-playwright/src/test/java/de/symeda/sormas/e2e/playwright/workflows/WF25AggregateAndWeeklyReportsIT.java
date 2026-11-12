package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

/** migration/workflows/WF25-aggregate-and-weekly-reports.md */
@DisplayName("WF25 Aggregate reports and the weekly reports overview (SurvOff, C1)")
@AppCopy(AppCopy.Config.C1)
class WF25AggregateAndWeeklyReportsIT extends WorkflowTest {

	Ui ui;
	String weekNo;
	String weekRange;
	String weekYear;

	@BeforeAll
	void open() {
		// Epi weeks run Monday to Sunday: the week before the reset date contains reset - 7 days.
		LocalDate lw = today.minusDays(7);
		LocalDate mon = lw.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate sun = mon.plusDays(6);
		weekRange = "(" + mon.getMonthValue() + "/" + mon.getDayOfMonth() + " - " + sun.getMonthValue() + "/" + sun.getDayOfMonth() + ")";
		weekYear = String.valueOf(sun.getYear());
		ui = newUi();
		ui.login("SurvOff");
	}

	private void newReport() {
		ui.clickId("aggregateReportNewAggregateReport");
		ui.waitText("Create a new aggregated report", 15_000);
		ui.settle(500);
	}

	private void lastWeek() {
		ui.windowClickText(Pattern.compile("^last week$", Pattern.CASE_INSENSITIVE));
		ui.settle(800);
	}

	private void setFromWeek(int idx, String typed) {
		ui.comboPick(ui.filterCombo(idx), typed, Pattern.compile("^" + typed), true, 700, 800);
	}

	private List<String> normRows() {
		List<String> out = new ArrayList<>();
		for (String r : ui.rowTexts()) {
			out.add(r.replaceAll("\\s+", " ").trim());
		}
		return out;
	}

	@Test
	@Order(1)
	@DisplayName("1. Aggregate reporting, new report: the window shows the epi week, region and district Default and a row per reportable disease")
	void step1() {
		ui.openView("Aggregate");
		assertTrue(ui.pageText().contains("Aggregate reporting"), "view shown");
		newReport();
		assertTrue(ui.windowText().contains("Create a new aggregated report"), "title");
		List<String> c = ui.windowComboValues();
		assertEquals("Default Region", c.get(2), "region");
		assertEquals("Default District", c.get(3), "district");
		String t = ui.windowText();
		for (String d : List.of("Malaria", "Pertussis", "Typhoid Fever", "Rubella")) {
			assertTrue(t.contains(d), "disease row " + d);
		}
	}

	@Test
	@Order(2)
	@DisplayName("2. Epi week before the reset date, 3 cases and 1 lab confirmation for Malaria, 2 deaths for Pertussis; save: the summary shows the report")
	void step2() {
		lastWeek();
		List<String> c = ui.windowComboValues();
		Matcher m = Pattern.compile("^Wk (\\d+)").matcher(c.get(1));
		assertTrue(m.find(), "week combo: " + c.get(1));
		weekNo = m.group(1);
		assertEquals(weekYear, c.get(0), "week year");
		assertTrue(c.get(1).contains(weekRange), "week range " + weekRange + " in " + c.get(1));
		ui.aggregateBox("Malaria", 1).fill("3");
		ui.aggregateBox("Malaria", 2).fill("1");
		ui.aggregateBox("Pertussis", 3).fill("2");
		ui.settle(300);
		ui.windowButton("Save");
		ui.noModal();
		assertEquals(0, ui.windowCount(), "window closed");
		setFromWeek(7, "Wk " + weekNo);
		ui.applyFilters();
		ui.settle(1200);
		List<String> rows = ui.rowTexts();
		assertTrue(rows.stream().anyMatch(r -> r.contains("Malaria") && Pattern.compile("\\b3\\s+1\\s+0\\b").matcher(r).find()), "malaria: " + rows);
		assertTrue(rows.stream().anyMatch(r -> r.contains("Pertussis") && Pattern.compile("\\b0\\s+0\\s+2\\b").matcher(r).find()), "pertussis: " + rows);
	}

	@Test
	@Order(3)
	@DisplayName("3. Another report for the same week and district: a duplicate warning appears; cancel")
	void step3() {
		newReport();
		lastWeek();
		String t = ui.windowText();
		Matcher m = Pattern.compile("Attention: Duplicate reports[^.]*\\.[^.]*\\.").matcher(t);
		assertTrue(m.find(), "warning in " + t);
		assertEquals("Attention: Duplicate reports have been found for the above criteria. Diseases marked with red already have reports.", m.group(), "warning");
		ui.windowButton("Discard");
		ui.noModal();
		assertEquals(0, ui.windowCount(), "window closed");
	}

	@Test
	@Order(4)
	@DisplayName("4. Report data tab: the saved report is listed with week, district and figures")
	void step4() {
		ui.clickEntry(Pattern.compile("^report data$", Pattern.CASE_INSENSITIVE));
		ui.settle(1500);
		setFromWeek(6, "Wk " + weekNo);
		ui.applyFilters();
		ui.settle(1200);
		List<String> rows = normRows();
		assertEquals(2, rows.size(), "rows: " + rows);
		assertTrue(rows.stream().anyMatch(r -> r.contains("Surveillance OFFICER Malaria Default Region Default District " + weekYear + " " + weekNo + " 3 1 0")), "malaria: " + rows);
		assertTrue(rows.stream().anyMatch(r -> r.contains("Surveillance OFFICER Pertussis Default Region Default District " + weekYear + " " + weekNo + " 0 0 2")), "pertussis: " + rows);
	}

	@Test
	@Order(5)
	@DisplayName("5. Weekly reports page: the last epi week lists the officer and district with zero informant reports (\"Missing report\"); the report data is stored")
	void step5() {
		ui.openView("Reports");
		ui.waitText("Weekly Reports", 10_000);
		String t = ui.pageText();
		Matcher h = Pattern.compile("LAST EPI WEEK: [^I]*", Pattern.CASE_INSENSITIVE).matcher(t);
		assertTrue(h.find() && h.group().contains(weekRange), "last week header " + weekRange);
		List<String> rows = ui.rowTexts();
		String r = rows.stream().filter(x -> x.contains("Surveillance OFFICER")).findFirst().orElse(t.substring(Math.max(0, t.indexOf("PERCENTAGE")))).replaceAll("\\s+", " ");
		assertTrue(Pattern.compile("Surveillance OFFICER Default District Missing report 0 1 0 0% 0").matcher(r).find(), "row: " + r);
		// Data check: one aggregate report per disease for the Default district and that epi week with the figures entered
		assertEquals(
			"MALARIA|" + weekYear + "|" + weekNo + "|3|1|0\nPERTUSSIS|" + weekYear + "|" + weekNo + "|0|0|2",
			copy.sql(
				"SELECT a.disease || '|' || a.year || '|' || a.epiweek || '|' || a.newcases || '|' || a.labconfirmations || '|' || a.deaths"
					+ " FROM aggregatereport a JOIN district d ON d.id = a.district_id WHERE d.name = 'Default District' ORDER BY a.disease"));
	}
}
