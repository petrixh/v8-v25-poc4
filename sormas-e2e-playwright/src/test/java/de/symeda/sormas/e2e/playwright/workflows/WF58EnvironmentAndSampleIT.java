package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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

/** migration/workflows/WF58-environment-and-sample.md. German UI; some captions English. */
@DisplayName("WF58 Environment and environment sample (NatUser, C2)")
@AppCopy(AppCopy.Config.C2)
class WF58EnvironmentAndSampleIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.loginDe("NatUser");
	}

	private Locator inWin(String regex) {
		return ui.windowFieldLike(Pattern.compile(regex));
	}

	private Locator onPage(String regex) {
		return ui.field(Pattern.compile(regex));
	}

	private void pick(Locator input, String typed, String full) {
		ui.comboPick(input, typed, full, true, 500, 600);
	}

	private String body() {
		return ui.bodyText().replaceAll("\\s+", " ");
	}

	private Locator save() {
		return ui.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Speichern").setExact(true)).locator("visible=true").first();
	}

	@Test
	@Order(1)
	@DisplayName("1. Open Umgebungen: the list shows only the seeded environment")
	void step1() {
		ui.openView("Umgebungen");
		assertTrue(ui.waitText("Umgebungsverzeichnis", 15_000), "title");
		ui.settle(800);
		assertEquals(1, ui.rowTexts().size(), "seed only");
	}

	@Test
	@Order(2)
	@DisplayName("2. Environment \"WF river\", medium Wasser, report date, Default region and district, GPS position; save: the view opens; set the water type surface water, save")
	void step2() {
		ui.button("Neue Umgebungen").click();
		ui.waitText("Umgebungs Name", 10_000);
		assertEquals(de(0), inWin("^Meldedatum").inputValue(), "report date preset");
		ui.comboToggleChoose(inWin("^Umgebungs Medium"), "Wasser");
		inWin("^Umgebungs Name").fill("WF river");
		pick(inWin("^Bundesland"), "Vor", "Voreingestelltes Bundesland");
		pick(inWin("^Landkreis"), "Vor", "Voreingestellter Landkreis");
		inWin("^GPS Breitengrad").fill("52.52");
		inWin("^GPS Längengrad").fill("13.405");
		ui.windowButtonLike(Pattern.compile("^Speichern$"));
		String note = ui.newestNotificationText(10_000);
		assertTrue(note != null && note.contains("Umgebung erstellt"), "created: " + note);
		assertTrue(ui.waitMatch("WF river \\(", 15_000), "view title");
		assertEquals("Wasser", onPage("^Umgebungs Medium").inputValue(), "medium");
		assertEquals(de(0), onPage("^Meldedatum").inputValue(), "report date");
		ui.dismissNotifications();
		ui.comboToggleChooseStart(onPage("^Wasserart"), "Oberflächenwasser");
		save().click();
		String n2 = ui.newestNotificationText(10_000);
		assertTrue(n2 != null && n2.contains("Umgebung gespeichert"), "saved: " + n2);
		assertTrue(onPage("^Wasserart").inputValue().startsWith("Oberflächenwasser"), "water type");
		ui.dismissNotifications();
	}

	@Test
	@Order(3)
	@DisplayName("3. Add an environment sample (sample date, material Water, laboratory Voreingestelltes Labor); save: the sample is listed on the environment")
	void step3() {
		ui.pageButtonLike(Pattern.compile("New sample", Pattern.CASE_INSENSITIVE));
		ui.waitText("Create new environment sample", 10_000);
		assertTrue(ui.windowText().contains("Create new environment sample"), "window title");
		assertEquals(de(0), inWin("^Datum der Probenentnahme").inputValue(), "sample date preset");
		ui.comboToggleChoose(inWin("^Probenmaterial"), "Water");
		pick(inWin("^Labor"), "Vor", "Voreingestelltes Labor");
		ui.windowButtonLike(Pattern.compile("^Speichern$"));
		ui.noModal();
		assertEquals(0, ui.windowCount(), "window closed");
		ui.dismissNotifications();
		ui.waitMatch("nicht versendet", 10_000);
		String t = body();
		assertTrue(Pattern.compile("\\bWATER\\b").matcher(t).find() && Pattern.compile("nicht versendet", Pattern.CASE_INSENSITIVE).matcher(t).find()
			&& t.contains("Datum der Probenentnahme: " + de(0)) && t.contains("Voreingestelltes Labor"), "sample listed");
	}

	@Test
	@Order(4)
	@DisplayName("4. Proben, switch to environment samples (Umgebung): the sample is listed for \"WF river\"; one environment and one new environment sample are stored")
	void step4() {
		ui.openView("Proben");
		ui.waitRow("", 10_000);
		assertTrue(!ui.rowTexts().isEmpty(), "human samples first");
		ui.selectOption("Umgebung", false);
		ui.waitRow("WF river", 15_000);
		List<String> rows = ui.rowTexts();
		assertEquals(2, rows.size(), "rows: " + rows);
		String mine = rows.stream().filter(r -> r.contains("WF river")).findFirst().orElse("");
		assertTrue(mine.contains(de(0)) && mine.contains("Voreingestelltes Labor"), "row: " + mine);
		// Data check: one new environment and one new environment sample (besides the seeded ones)
		assertEquals("1", copy.sql("SELECT count(*) FROM environments WHERE environmentname = 'WF river'"));
		assertEquals("1", copy.sql("SELECT count(*) FROM environmentsamples s JOIN environments e ON e.id = s.environment_id WHERE e.environmentname = 'WF river'"));
	}
}
