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

/** migration/workflows/WF55-campaign-data-dashboard.md. Captions in German (C2). */
@DisplayName("WF55 Campaign data, statistics and dashboard (admin, SurvSup; C2)")
@AppCopy(AppCopy.Config.C2)
class WF55CampaignDataDashboardIT extends WorkflowTest {

	Ui admin;
	Ui sup;

	@BeforeAll
	void open() {
		admin = newUi();
		sup = newUi();
	}

	private List<String> combos(Ui ui) {
		@SuppressWarnings("unchecked")
		List<String> v = (List<String>) ui.page.locator("vaadin-combo-box input").evaluateAll("es => es.map(e => e.value)");
		return v;
	}

	@Test
	@Order(1)
	@DisplayName("1. Only the seeded campaign exists: as admin create \"WF campaign\" (reset date to 30 days later); log out")
	void step1() {
		admin.loginDe("admin");
		admin.openView("Kampagnen");
		admin.clickLink("Kampagnen");
		admin.waitRow("Seed campaign", 15_000);
		assertTrue(admin.rowTexts().stream().noneMatch(r -> r.contains("WF campaign")), "no WF campaign yet");
		admin.button("Neue Kampagne").click();
		admin.waitText("Neue Kampagne erstellen", 10_000);
		admin.windowFieldLike(Pattern.compile("^Name")).fill("WF campaign");
		admin.fillDate(admin.windowFieldLike(Pattern.compile("^Startdatum")), de(0), 300);
		admin.fillDate(admin.windowFieldLike(Pattern.compile("^Enddatum")), de(30), 300);
		admin.windowButtonLike(Pattern.compile("^Speichern$"));
		admin.noModal();
		assertTrue(admin.waitRow("WF campaign", 10_000), "listed");
		admin.logout("Abmelden");
		admin.waitLoginForm(20_000);
	}

	@Test
	@Order(2)
	@DisplayName("2. As SurvSup open Kampagnen, campaign data: the campaign choice offers \"WF campaign\"; the data list is empty")
	void step2() {
		sup.loginDe("SurvSup");
		sup.openView("Kampagnen");
		assertTrue(sup.waitText("Kampagnendaten", 15_000), "title");
		sup.comboToggleOpen(sup.filterCombo(0));
		assertTrue(sup.suggestionsText().contains("WF campaign"), "offers the campaign: " + sup.suggestionsText());
		sup.pickSuggestion("WF campaign", 800);
		assertEquals("WF campaign", combos(sup).get(0), "chosen");
		assertEquals(0, sup.rowTexts().size(), "list empty");
	}

	@Test
	@Order(3)
	@DisplayName("3. Campaign statistics for \"WF campaign\": the statistics list is empty, with no error")
	void step3() {
		sup.clickLink("Kampagnenstatistik");
		sup.settle(1500);
		assertEquals("WF campaign", combos(sup).get(0), "campaign chosen");
		assertEquals(0, sup.rowTexts().size(), "list empty");
		assertEquals(0, sup.notificationCount(), "no error shown");
	}

	@Test
	@Order(4)
	@DisplayName("4. Dashboard, campaign dashboard: \"WF campaign\" can be selected; no diagrams are configured, no error")
	void step4() {
		sup.openView("Übersicht");
		sup.selectOption("Kampagnen", false);
		assertTrue(sup.waitText("Kampagnen-Dashboard", 15_000), "title");
		sup.settle(1000);
		if (!"WF campaign".equals(combos(sup).get(0))) {
			sup.comboToggleOpen(sup.filterCombo(0));
			sup.pickSuggestion("WF campaign", 1000);
		}
		assertEquals("WF campaign", combos(sup).get(0), "campaign chosen");
		assertEquals(0, sup.notificationCount(), "no error shown");
		assertEquals(0, sup.chartCount(), "no diagrams");
	}
}
