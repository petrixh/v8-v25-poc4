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

/** migration/workflows/WF33-case-statistics.md */
@DisplayName("WF33 Case statistics as table, chart and map (NatUser, C1)")
@AppCopy(AppCopy.Config.C1)
class WF33CaseStatisticsIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("NatUser");
	}

	private String flat() {
		return ui.pageText().replaceAll("\\s+", " ");
	}

	private void generate() {
		ui.clickRoleButton("Generate");
		ui.settle(1500);
	}

	@Test
	@Order(1)
	@DisplayName("1. Open Statistics: the filter and visualisation sections are shown")
	void step1() {
		ui.openView("Statistics");
		ui.waitText("Visualization", 10_000);
		String t = flat();
		for (String x : List.of("Filters", "Visualization", "Options", "Results")) {
			assertTrue(t.contains(x), "section " + x);
		}
		for (String x : List.of("Table", "Map", "Chart")) {
			assertTrue(ui.statsOption(x).isVisible(), "type " + x);
		}
	}

	@Test
	@Order(2)
	@DisplayName("2. Add a disease filter, pick COVID-19 and Measles: both are removable entries; remove Measles: only COVID-19 remains")
	void step2() {
		ui.clickRoleButton("Add filter");
		ui.settle(500);
		ui.chooseMenuBarMatching("Select an attribute", Pattern.compile("^Disease$"));
		ui.settle(1000);
		ui.pickToken("COVID-19");
		ui.pickToken("Measles");
		assertEquals("COVID-19,Measles", String.join(",", ui.tokenTexts()), "both entries");
		ui.removeToken("Measles");
		ui.settle(500);
		assertEquals("COVID-19", String.join(",", ui.tokenTexts()), "only COVID-19");
	}

	@Test
	@Order(3)
	@DisplayName("3. Table with rows by case classification, generate: COVID-19 counts per classification, including Confirmed")
	void step3() {
		ui.chooseMenuBarMatching("Don't group rows", Pattern.compile("^Classification$"));
		ui.settle(500);
		generate();
		ui.waitMatch("TOTAL \\d+", 15_000);
		String t = flat();
		for (String c : List.of("CONFIRMED CASE", "NOT A CASE", "NOT YET CLASSIFIED", "POSSIBLE CASE", "PROBABLE CASE", "TOTAL")) {
			assertTrue(t.contains(c), "row " + c);
		}
		assertEquals(82, ui.numberIn("TOTAL (\\d+)"), "total");
		assertEquals(3, ui.numberIn("CONFIRMED CASE (\\d+)"), "confirmed");
	}

	@Test
	@Order(4)
	@DisplayName("4. Switch to a chart, generate: a column chart with the same figures appears")
	void step4() {
		ui.statsOption("Chart").click();
		ui.settle(600);
		generate();
		ui.page.waitForSelector(".highcharts-container");
		ui.settle(800);
		assertTrue(ui.chartDrawn(), "chart drawn");
		String t = flat();
		String legend = String.join(" | ", ui.chartLegendTexts());
		for (String x : List.of("Confirmed case", "Not a case", "Not yet classified", "Possible case", "Probable case")) {
			assertTrue(t.contains(x) || legend.contains(x), "legend " + x);
		}
		List<String> labels = ui.chartDataLabels();
		for (String x : List.of("3", "2", "8", "24", "38", "4")) {
			assertTrue(labels.contains(x), "data label " + x + " in " + labels);
		}
	}

	@Test
	@Order(5)
	@DisplayName("5. Switch to a map by district, generate: a map with a case-count key appears (no district shaded: the seed's Default district has no outline)")
	void step5() {
		ui.statsOption("Map").click();
		ui.settle(600);
		ui.statsOption("Districts").click();
		ui.settle(500);
		generate();
		ui.waitMatch("MAP KEY", 15_000);
		String t = flat();
		assertTrue(ui.statsMapDrawn(), "map shown");
		assertTrue(t.contains("MAP KEY") && Pattern.compile("1 - 82 Cases").matcher(t).find(), "map key");
		assertEquals(0, ui.notificationCount(), "no error notification");
	}
}
