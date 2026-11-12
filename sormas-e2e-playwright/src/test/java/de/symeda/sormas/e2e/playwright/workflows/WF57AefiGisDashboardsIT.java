package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF57-aefi-gis-dashboards.md. German UI. */
@DisplayName("WF57 Adverse events dashboard and the GIS dashboard (NatUser, C2)")
@AppCopy(AppCopy.Config.C2)
class WF57AefiGisDashboardsIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.loginDe("NatUser");
	}

	private String body() {
		return ui.bodyText().replaceAll("\\s+", " ");
	}

	private static String us(LocalDate d) {
		return d.getMonthValue() + "/" + d.getDayOfMonth() + "/" + String.format("%02d", d.getYear() % 100);
	}

	@Test
	@Order(1)
	@DisplayName("1. Dashboard, Adverse Events: counts by seriousness and the charts by vaccine dose and by sex are shown (zeros), no error")
	void step1() {
		ui.openView("Übersicht");
		ui.selectOption("Adverse Events", false);
		assertTrue(ui.waitText("Adverse Events Dashboard", 15_000), "title");
		ui.settle(1500);
		String t = body();
		for (String s : List.of("Serious", "Non-serious", "Adverse events by vaccine dose", "Proportion of AEFI reactions (events) by sex", "Adverse Events Type Chart", "Adverse Events Status Map")) {
			assertTrue(t.contains(s) || ui.page.getByText(s, new Page.GetByTextOptions().setExact(true)).count() > 0, "shows " + s);
		}
		assertTrue(Pattern.compile("0 ?AEFI 0 Serious 0 Non-serious").matcher(t).find(), "zero counts: " + t);
		assertEquals(0, ui.notificationCount(), "no error shown");
	}

	@Test
	@Order(2)
	@DisplayName("2. Change the period to the 30 days up to the reset date: the figures update without error")
	void step2() {
		ui.clickEntry(Pattern.compile("Epi-Woche"));
		ui.settle(600);
		ui.clickEntry("Benutzerdefiniert");
		ui.settle(600);
		ui.popupComboChoose("Nach Datum");
		ui.fillDate(ui.popupDateInput(0), us(today.minusDays(30)), 300);
		assertEquals(us(today), ui.popupDateInput(1).inputValue(), "to date");
		ui.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("Eigenen Filter", Pattern.CASE_INSENSITIVE))).click();
		ui.settle(2000);
		List<String> periods = ui.buttonTexts(Pattern.compile("\\d{4} - \\d"));
		assertTrue(periods.stream().anyMatch(t -> t.contains(de(-30) + " - " + de(0))), "period applied: " + periods);
		String t = body();
		assertTrue(t.contains("Adverse Events Dashboard") && (t.contains("Adverse events by vaccine dose")
			|| ui.page.getByText("Adverse events by vaccine dose", new Page.GetByTextOptions().setExact(true)).count() > 0), "still the dashboard");
		assertEquals(0, ui.notificationCount(), "no error shown");
	}

	@Test
	@Order(3)
	@DisplayName("3. Switch to the GIS dashboard: the map is shown with layers for cases, contacts and events that switch on and off")
	void step3() {
		ui.selectOption("Geographical Analysis", false);
		assertTrue(ui.waitText("Geographical Analysis", 15_000), "title");
		ui.settle(2500);
		assertTrue(ui.mapPresent(), "map shown");
		ui.clickEntry("Ebenen");
		ui.settle(600);
		assertTrue(ui.checkboxChecked("Fälle anzeigen"), "cases on by default");
		for (String name : List.of("Kontakte anzeigen", "Ereignisse anzeigen")) {
			assertFalse(ui.checkboxChecked(name), name + " off");
			ui.clickCheckbox(name);
			ui.settle(800);
			assertTrue(ui.checkboxChecked(name), name + " on");
			ui.clickCheckbox(name);
			ui.settle(800);
			assertFalse(ui.checkboxChecked(name), name + " off again");
		}
		ui.clickCheckbox("Fälle anzeigen");
		ui.settle(800);
		assertFalse(ui.checkboxChecked("Fälle anzeigen"), "cases off");
		ui.clickCheckbox("Fälle anzeigen");
		ui.settle(800);
		assertTrue(ui.checkboxChecked("Fälle anzeigen"), "cases on again");
		assertEquals(0, ui.notificationCount(), "no error shown");
	}
}
