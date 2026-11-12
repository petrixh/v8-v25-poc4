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

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF32-contacts-dashboard.md */
@DisplayName("WF32 Contacts dashboard counts and filters (NatUser, C1)")
@AppCopy(AppCopy.Config.C1)
class WF32ContactsDashboardIT extends WorkflowTest {

	Ui ui;
	int base;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("NatUser");
	}

	private String flat() {
		return ui.pageText().replaceAll("\\s+", " ");
	}

	private int allContacts() {
		return ui.numberIn("All Contacts (\\d+)");
	}

	@Test
	@Order(1)
	@DisplayName("1. Open the dashboard and switch to the contacts dashboard: the counts, contacts per case, new cases, follow-up chart and map are shown, no error")
	void step1() {
		ui.openView("Dashboard");
		ui.settle(1000);
		ui.dashboardSwitch(Pattern.compile("^contacts$", Pattern.CASE_INSENSITIVE));
		assertTrue(ui.waitText("Contacts Dashboard", 15_000), "title");
		ui.waitMatch("All Contacts \\d+", 10_000);
		ui.settle(1500);
		String t = flat();
		for (String x : List.of("All Contacts", "Under Follow-up", "Stopped Follow-up", "Visits", "Contacts per Case", "New Cases not Previously Known to Be Contacts")) {
			assertTrue(t.contains(x), "section " + x);
		}
		assertTrue(t.contains("Follow-up Status Chart") && t.contains("Contact Map"), "chart and map");
		assertEquals(0, ui.notificationCount(), "no error notification");
		base = allContacts();
	}

	@Test
	@Order(2)
	@DisplayName("2. Change the period to the last 30 days before the reset date: the figures update")
	void step2() {
		ui.setLastDays(today, 30);
		String t = flat();
		java.time.LocalDate from = today.minusDays(30);
		assertTrue(t.contains(from.getMonthValue() + "/" + from.getDayOfMonth() + "/" + from.getYear() + " - ")
			|| t.contains(from.getMonthValue() + "/" + from.getDayOfMonth() + "/" + String.format("%02d", from.getYear() % 100) + " - "), "period shown");
		base = allContacts();
		assertTrue(t.contains("All Contacts") && base >= 1, "figures shown: " + base);
	}

	@Test
	@Order(3)
	@DisplayName("3. Filter by disease COVID-19: the figures include SCON01")
	void step3() {
		ui.pickDisease("COVID-19");
		int n = allContacts();
		assertEquals("COVID-19", ui.comboByValue("COVID-19").inputValue(), "disease filter set");
		assertTrue(n >= 1 && n <= base, "includes the seed contact: " + n + " of " + base);
	}

	@Test
	@Order(4)
	@DisplayName("4. No contact network diagram tab: the dashboard switch offers surveillance, contacts and samples only")
	void step4() {
		assertFalse(Pattern.compile("network", Pattern.CASE_INSENSITIVE).matcher(flat()).find(), "no network tab");
		assertEquals("surveillance,contacts,samples", String.join(",", ui.dashboardSwitchTexts().stream().map(x -> x.trim().toLowerCase()).toList()), "switch offers");
	}
}
