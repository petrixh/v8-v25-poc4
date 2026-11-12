package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

/** migration/workflows/WF04-classify-case.md. Seed case SCAS03 (Charlie Seedperson, Measles). */
@DisplayName("WF04 Classify a case and close its investigation (SurvSup, C1)")
@AppCopy(AppCopy.Config.C1)
class WF04ClassifyCaseIT extends WorkflowTest {

	private static final Pattern NO_ERROR = Pattern.compile("changed by|Please check|error|conflict", Pattern.CASE_INSENSITIVE);
	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvSup");
	}

	private void saveAndWait() {
		ui.save();
		assertTrue(ui.waitText("saved", 8000), "saved confirmation");
		ui.settle(1500);
		ui.page.evaluate("() => document.querySelectorAll('*').forEach(e => { if (e.scrollTop > 0) e.scrollTop = 0; })");
		ui.settle(300);
	}

	@Test
	@Order(1)
	@DisplayName("1. Open SCAS03 from the case directory: Measles, Possible case, investigation pending")
	void step1() {
		ui.openView("Cases");
		ui.fillSearch("caseLike", "SCAS03");
		ui.applyFilters();
		ui.settle(1500);
		int row = ui.rowIndex("SCAS03");
		assertTrue(row >= 0, "listed");
		ui.clickInRow(row, "SCAS03", false);
		ui.settle(2500);
		String t = ui.pageText();
		assertTrue(t.contains("Charlie SEEDPERSON") && t.contains("(SCAS03)"), "title");
		assertEquals("Measles", ui.field("Disease").inputValue(), "disease");
		assertTrue(ui.optionChecked("Possible case", false), "classification possible case");
		assertTrue(ui.optionChecked("Investigation pending", false), "investigation pending");
	}

	@Test
	@Order(2)
	@DisplayName("2. Classification Confirmed, save: saved, the classification shows Confirmed")
	void step2() {
		ui.selectOption("Confirmed case", false);
		ui.settle(800);
		saveAndWait();
		String t = ui.pageText();
		assertTrue(Pattern.compile("Confirmed case\\s+Charlie SEEDPERSON").matcher(t).find(), "header confirmed");
		assertTrue(ui.optionChecked("Confirmed case", false), "confirmed checked");
		assertFalse(Pattern.compile("changed by|Please check|error", Pattern.CASE_INSENSITIVE).matcher(t).find(), "no error");
	}

	@Test
	@Order(3)
	@DisplayName("3. Without reloading, Investigation done, save: saved without an error or a changed-by-someone-else message")
	void step3() {
		ui.dismissNotifications();
		ui.selectOption("Investigation done", false);
		ui.settle(800);
		saveAndWait();
		assertFalse(NO_ERROR.matcher(ui.pageText()).find(), "no error");
		assertTrue(ui.optionChecked("Investigation done", false), "investigation done");
		assertTrue(ui.optionChecked("Confirmed case", false), "classification still confirmed");
	}

	@Test
	@Order(4)
	@DisplayName("4. Without reloading, case person tab, nickname Chuck, save: saved without an error")
	void step4() {
		ui.dismissNotifications();
		ui.tab("Case person").click();
		ui.settle(2000);
		ui.fillField("Nickname", "Chuck");
		saveAndWait();
		assertFalse(NO_ERROR.matcher(ui.pageText()).find(), "no error");
		assertEquals("Chuck", ui.field("Nickname").inputValue(), "nickname");
	}

	@Test
	@Order(5)
	@DisplayName("5. Case directory filtered by classification Confirmed: SCAS03 listed as Confirmed, investigation done")
	void step5() {
		ui.dismissNotifications();
		ui.openView("Cases");
		Locator box = ui.comboByPrompt("Case classificatio");
		assertNotNull(box, "classification filter");
		ui.comboPick(box, "Confirmed", "Confirmed case", false, 600, 700);
		ui.applyFilters();
		ui.settle(1800);
		int row = ui.rowIndex("SCAS03");
		assertTrue(row >= 0, "listed");
		String txt = ui.rowTexts().get(row);
		for (String x : List.of("Measles", "Confirmed case", "Investigation done", "Charlie", "Seedperson")) {
			assertTrue(txt.contains(x), "row has " + x + ": " + txt);
		}
		// Data check: SCAS03 confirmed, investigation done; its person's nickname Chuck
		assertEquals(
			"CONFIRMED|DONE|Chuck",
			copy.sql(
				"SELECT c.caseclassification, c.investigationstatus, p.nickname FROM cases c JOIN person p ON p.id = c.person_id WHERE c.uuid LIKE 'SCAS03%'"));
	}
}
