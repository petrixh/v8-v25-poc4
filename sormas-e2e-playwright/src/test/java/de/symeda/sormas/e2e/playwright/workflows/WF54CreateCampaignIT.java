package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Page;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF54-create-campaign.md. Captions in German (C2). */
@DisplayName("WF54 Create a campaign (admin, C2)")
@AppCopy(AppCopy.Config.C2)
class WF54CreateCampaignIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.loginDe("admin");
	}

	@Test
	@Order(1)
	@DisplayName("1. Open Kampagnen, the campaigns list: only the seeded campaign \"Seed campaign: vaccination drive\"")
	void step1() {
		ui.openView("Kampagnen");
		ui.clickLink("Kampagnen");
		assertTrue(ui.waitText("Kampagnen-Verzeichnis", 15_000), "title");
		ui.waitRow("Seed campaign", 10_000);
		assertTrue(ui.button("Neue Kampagne").isVisible(), "new campaign button");
		List<String> r0 = ui.rowTexts();
		assertTrue(r0.size() == 1 && r0.get(0).contains("Seed campaign: vaccination drive"), "seed only: " + r0);
	}

	@Test
	@Order(2)
	@DisplayName("2. New campaign \"WF campaign\" (start the reset date, end 30 days later, description); save: \"Neue Kampagne erstellt\" and the window closes")
	void step2() {
		ui.button("Neue Kampagne").click();
		ui.waitText("Neue Kampagne erstellen", 10_000);
		assertTrue(ui.windowText().contains("Neue Kampagne erstellen"), "window title");
		ui.windowFieldLike(Pattern.compile("^Name")).fill("WF campaign");
		ui.fillDate(ui.windowFieldLike(Pattern.compile("^Startdatum")), de(0), 300);
		ui.fillDate(ui.windowFieldLike(Pattern.compile("^Enddatum")), de(30), 300);
		ui.windowFieldLike(Pattern.compile("^Beschreibung")).fill("WF description");
		ui.windowButtonLike(Pattern.compile("^Speichern$"));
		String note = ui.newestNotificationText(10_000);
		assertTrue(note != null && note.contains("Neue Kampagne erstellt"), "created: " + note);
		ui.noModal();
		assertEquals(0, ui.windowCount(), "window closed");
	}

	@Test
	@Order(3)
	@DisplayName("3. Open the campaign from the list: the sections for the campaign's forms and dashboard elements are shown, empty")
	void step3() {
		ui.dismissNotifications();
		ui.waitRow("WF campaign", 10_000);
		ui.clickRow(ui.rowIndex("WF campaign"));
		assertTrue(ui.waitText("Daten der Kampagne", 15_000), "forms section");
		String t = ui.bodyText().replaceAll("\\s+", " ");
		assertTrue(t.contains("Formular hinzufügen"), "add form");
		assertTrue(t.contains("Kampagnenübersicht") && t.contains("Diagramm hinzufügen"), "dashboard section");
		assertEquals("WF campaign", ui.field(Pattern.compile("^Name")).inputValue(), "name");
		assertEquals(de(0) + "-" + de(30), ui.field(Pattern.compile("^Startdatum")).inputValue() + "-" + ui.field(Pattern.compile("^Enddatum")).inputValue(), "dates");
	}

	@Test
	@Order(4)
	@DisplayName("4. Back on the list: the seeded campaign and \"WF campaign\" are listed, the latter with its dates; the data is stored")
	void step4() {
		ui.page.getByText("Alle Kampagnen", new Page.GetByTextOptions().setExact(true)).first().click();
		ui.waitRow("WF campaign", 10_000);
		List<String> rows = ui.rowTexts();
		assertEquals(2, rows.size(), "rows: " + rows);
		String mine = rows.stream().filter(r -> r.contains("WF campaign")).findFirst().orElse("");
		assertTrue(mine.contains(de(0)) && mine.contains(de(30)), "dates: " + mine);
		// Data check: one campaign "WF campaign" with the dates entered
		assertEquals(
			today + "|" + today.plusDays(30),
			copy.sql("SELECT startdate::date || '|' || enddate::date FROM campaigns WHERE name = 'WF campaign'"));
	}
}
