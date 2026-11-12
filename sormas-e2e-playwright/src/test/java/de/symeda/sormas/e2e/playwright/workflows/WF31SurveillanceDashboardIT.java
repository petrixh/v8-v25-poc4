package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/**
 * migration/workflows/WF31-surveillance-dashboard.md. Map tiles come from OpenStreetMap (host blocking off); tile
 * differences are allowed.
 */
@DisplayName("WF31 Surveillance dashboard (NatUser, C1)")
@AppCopy(AppCopy.Config.C1)
class WF31SurveillanceDashboardIT extends WorkflowTest {

	/** The dashboard's date filter fields take two-digit years. */
	private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("M/d/yy");
	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
	}

	private String selectedTab() {
		return String.join(",", ui.selectedDiseaseTab());
	}

	/** Period: custom, by date, from back days before today to today; then apply. */
	private void setLastDays(int back) {
		Page p = ui.page;
		ui.button(Pattern.compile("This epi week|Today|Yesterday|Last epi week|This year|^\\d+\\/\\d+\\/\\d+ - ")).click();
		ui.settle(800);
		ui.button("Custom").click();
		ui.settle(800);
		Locator mode = ui.comboByPrompt("By Epi Week");
		mode.click();
		mode.press("ArrowDown");
		ui.settle(600);
		ui.pickSuggestion("By Date", 800);
		Locator dp = p.locator("vaadin-date-picker input").locator("visible=true");
		dp.nth(0).fill(today.minusDays(back).format(SHORT));
		dp.nth(0).press("Enter");
		ui.settle(500);
		dp.nth(1).fill(today.format(SHORT));
		dp.nth(1).press("Enter");
		ui.settle(500);
		ui.button("Apply custom filter").click();
		ui.settle(2500);
		p.getByText(Pattern.compile("Dashboard$")).locator("visible=true").first().click(); // closes the period pop-up
		ui.settle(800);
		p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("^apply filters$", Pattern.CASE_INSENSITIVE)))
			.first()
			.click(new Locator.ClickOptions().setForce(true));
		ui.settle(3000);
	}

	@Test
	@Order(1)
	@DisplayName("1. Log in as NatUser: the surveillance dashboard with disease tiles, statistics, curve and map")
	void step1() {
		ui.login("NatUser");
		ui.settle(2500);
		String t = ui.pageText();
		assertTrue(t.contains("Surveillance Dashboard"), "title");
		for (String x : List.of("Last report", "Fatalities", "Number of events", "New Cases", "CONFIRMED", "CASE FATALITY RATE", "Epidemiological Curve", "Case Status Map")) {
			assertTrue(t.contains(x), "shows " + x);
		}
		assertTrue(ui.mapPresent(), "map");
	}

	@Test
	@Order(2)
	@DisplayName("2. Period: the last 30 days before the reset date: tiles, curve and map update")
	void step2() {
		List<String> before = ui.tileTexts().subList(0, 3);
		setLastDays(30);
		String t = ui.pageText();
		assertTrue(t.contains(today.minusDays(30).format(WorkflowTest.EN) + " - "), "period shown");
		assertNotEquals(before, ui.tileTexts().subList(0, 3), "tiles updated");
		assertTrue(Pattern.compile("COVID-19\\s*1").matcher(t).find(), "COVID-19 tile");
	}

	@Test
	@Order(3)
	@DisplayName("3. Disease details for COVID-19: case counts by classification including the confirmed seed case")
	void step3() {
		ui.clickLink("COVID-19");
		ui.settle(2500);
		assertEquals("COVID-19", selectedTab(), "selected tab");
		String t = ui.pageText();
		Matcher nc = Pattern.compile("(\\d+) ?New Cases").matcher(t);
		Matcher conf = Pattern.compile("(\\d+) CONFIRMED").matcher(t);
		assertTrue(conf.find(), "confirmed count");
		assertEquals(1, Integer.parseInt(conf.group(1)), "confirmed seed case");
		assertTrue(nc.find() && Integer.parseInt(nc.group(1)) > 0, "new cases");
	}

	@Test
	@Order(4)
	@DisplayName("4. Slide show on, 90 s without input: the disease details advance to the next disease")
	void step4() {
		ui.clickCheckbox("slide show");
		ui.settle(800);
		String first = selectedTab(), now = first;
		for (int i = 0; i < 12 && now.equals(first); i++) {
			ui.page.waitForTimeout(10_000);
			now = selectedTab();
		}
		assertTrue(!now.equals(first) && !now.isEmpty(), "advanced from " + first + " to " + now);
	}

	@Test
	@Order(5)
	@DisplayName("5. Map layers, show regions: the Default region is drawn, no error")
	void step5() {
		ui.clickEntry("Layers");
		ui.settle(800);
		ui.clickCheckbox("Show regions");
		ui.settle(3000);
		assertTrue(ui.page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("Show regions")).isChecked(), "show regions on");
		assertEquals(0, ui.notificationCount(), "no error notification");
	}
}
