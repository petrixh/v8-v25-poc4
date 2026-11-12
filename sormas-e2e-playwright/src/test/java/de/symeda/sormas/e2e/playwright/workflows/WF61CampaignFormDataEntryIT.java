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

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF61-campaign-form-data-entry.md. German UI. */
@DisplayName("WF61 Enter campaign form data (NatUser, C2)")
@AppCopy(AppCopy.Config.C2)
class WF61CampaignFormDataEntryIT extends WorkflowTest {

	private static final String FORM = "Seed form: vaccination round";

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.loginDe("NatUser");
	}

	private Locator tb(String regex) {
		return ui.windowFieldLike(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
	}

	private void choose(String regex, String text) {
		ui.comboToggleChoose(tb(regex), text);
	}

	@Test
	@Order(1)
	@DisplayName("1. Campaign data: the seeded campaign is chosen and its one seeded form entry is listed")
	void step1() {
		ui.openView("Kampagnen");
		assertTrue(ui.waitText("Kampagnendaten", 15_000), "title");
		ui.waitRow("Seed campaign", 10_000);
		List<String> r = ui.rowTexts();
		assertEquals(1, r.size(), "rows: " + r);
		assertTrue(r.get(0).contains("Seed campaign: vaccination drive") && r.get(0).contains(FORM), "seed row: " + r.get(0));
	}

	@Test
	@Order(2)
	@DisplayName("2. \"New form\" and the seeded form: the entry window opens with the campaign, the area/region/district/community choices and the form fields")
	void step2() {
		ui.button(Pattern.compile("Neues Formular", Pattern.CASE_INSENSITIVE)).click();
		ui.settle(500);
		ui.clickLastEntry(FORM);
		ui.waitText("Anzahl geimpfter Kinder", 15_000);
		String t = ui.windowText().toLowerCase();
		String values = String.join("|", ui.inputValues(true));
		for (String s : List.of("Neue " + FORM, "Seed campaign: vaccination drive", "Impfrunde", "Anzahl geimpfter Kinder", "Anzahl nicht erreichter Kinder", "Impfstoffvorrat ausreichend?", "Bemerkungen")) {
			assertTrue(t.contains(s.toLowerCase()) || values.contains(s), "shows " + s);
		}
	}

	@Test
	@Order(3)
	@DisplayName("3. Area \"Seed area\", the default region, district, community; 40 vaccinated, 5 not reached, stock sufficient \"No\", a remark; save: saved")
	void step3() {
		choose("^Gebiet", "Seed area");
		ui.settle(800);
		choose("^Region", "Voreingestelltes Bundesland");
		choose("^District", "Voreingestellter Landkreis");
		choose("^Community", "Voreingestellte Gemeinde");
		for (String[] f : new String[][] { { "Anzahl geimpfter Kinder", "40" }, { "Anzahl nicht erreichter Kinder", "5" } }) {
			Locator l = tb(f[0]);
			l.fill(f[1]);
			l.press("Tab");
		}
		ui.windowClickText(Pattern.compile("^nein$", Pattern.CASE_INSENSITIVE));
		ui.settle(400);
		Locator rem = tb("Bemerkungen");
		rem.fill("Synthetic remark WF61");
		rem.press("Tab");
		ui.windowButtonLike(Pattern.compile("^Speichern$"));
		ui.noModal();
		assertEquals(0, ui.windowCount(), "window closed");
	}

	@Test
	@Order(4)
	@DisplayName("4. The campaign data list holds two entries: the seeded one and the new one (same campaign and form, default region, district, community)")
	void step4() {
		try {
			ui.button(Pattern.compile("Filter anwenden", Pattern.CASE_INSENSITIVE)).click(new Locator.ClickOptions().setTimeout(3000));
		} catch (RuntimeException ignored) {
			// no filter button: the list is already current
		}
		ui.settle(1000);
		List<String> r = ui.rowTexts();
		assertEquals(2, r.size(), "rows: " + r);
		assertTrue(r.stream().allMatch(x -> x.contains("Seed campaign: vaccination drive") && x.contains(FORM) && x.contains("Voreingestelltes Bundesland")
			&& x.contains("Voreingestellter Landkreis") && x.contains("Voreingestellte Gemeinde")), "same campaign, form and place: " + r);
	}

	@Test
	@Order(5)
	@DisplayName("5. Open the new entry: it shows the saved values (40, 5, the remark)")
	void step5() {
		List<String> all = ui.rowTexts();
		int idx = -1;
		for (int i = 0; i < all.size(); i++) {
			if (!all.get(i).contains("03.11.2026")) {
				idx = i;
			}
		}
		assertTrue(idx >= 0, "new row found in " + all);
		ui.clickRow(idx);
		ui.waitMatch("Synthetic remark WF61|Datenformular", 15_000);
		ui.settle(1000);
		List<String> fields = ui.inputValues(false).stream().filter(v -> !v.isEmpty()).toList();
		assertTrue(fields.contains("40"), "vaccinated 40: " + fields);
		assertTrue(fields.contains("5"), "not reached 5: " + fields);
		assertTrue(fields.contains("Synthetic remark WF61"), "remark: " + fields);
	}
}
