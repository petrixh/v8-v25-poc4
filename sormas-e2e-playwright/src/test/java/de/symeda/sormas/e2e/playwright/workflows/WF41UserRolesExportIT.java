package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Page;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Docx;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF41-user-roles-export.md */
@DisplayName("WF41 Look at the user roles and export them (admin, C1)")
@AppCopy(AppCopy.Config.C1)
class WF41UserRolesExportIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("admin");
	}

	private boolean roleRow(List<String> rows, String regex) {
		return rows.stream().anyMatch(r -> Pattern.compile(regex).matcher(r.trim()).find());
	}

	@Test
	@Order(1)
	@DisplayName("1. Open Users, user roles: the default roles are listed with jurisdiction level")
	void step1() {
		ui.openView("Users");
		ui.tab(Pattern.compile("^user roles$", Pattern.CASE_INSENSITIVE)).click();
		ui.waitText("Surveillance Officer", 10_000);
		ui.settle(1000);
		List<String> rows = ui.rowTexts();
		assertEquals(27, rows.size(), "roles");
		assertTrue(roleRow(rows, "Surveillance Officer District$"), "Surveillance Officer District");
		assertTrue(roleRow(rows, "Surveillance Supervisor Region$"), "Surveillance Supervisor Region");
		assertTrue(roleRow(rows, "National User Nation$"), "National User Nation");
		assertTrue(roleRow(rows, "Admin None$"), "Admin None");
	}

	@Test
	@Order(2)
	@DisplayName("2. Open the Surveillance Officer role: rights grouped by area, with the right to view cases ticked")
	void step2() {
		ui.clickRow(ui.rowIndex("Surveillance Officer"));
		ui.waitMatch("JURISDICTION LEVEL", 15_000);
		ui.waitMatch("Data Protection", 15_000);
		String t = ui.pageText();
		assertTrue(t.contains("Surveillance Officer") && Pattern.compile("JURISDICTION LEVEL", Pattern.CASE_INSENSITIVE).matcher(t).find(), "title");
		for (int i = 0; i < 40 && !ui.inputValues(false).contains("District"); i++) {
			ui.page.waitForTimeout(250);
		}
		assertTrue(ui.inputValues(false).contains("District"), "jurisdiction");
		for (String a : List.of("Data Protection", "Dashboard", "Tasks", "Persons")) {
			assertTrue(t.contains(a), "area " + a);
		}
		ui.checkboxReveal("View existing cases");
		assertTrue(ui.checkboxIsChecked("View existing cases"), "view cases ticked");
	}

	@Test
	@Order(3)
	@DisplayName("3. The role's notifications tab: the e-mail and SMS notification types for the role are shown")
	void step3() {
		ui.tab(Pattern.compile("notifications", Pattern.CASE_INSENSITIVE)).click();
		ui.waitMatch("sms", 10_000);
		String t = ui.pageText();
		assertTrue(Pattern.compile("e-?mail", Pattern.CASE_INSENSITIVE).matcher(t).find() && Pattern.compile("sms", Pattern.CASE_INSENSITIVE).matcher(t).find(), "e-mail and SMS");
	}

	@Test
	@Order(4)
	@DisplayName("4. Back on the role list, export user roles: an XLSX workbook downloads listing roles and their rights")
	void step4() throws IOException {
		try {
			ui.tab(Pattern.compile("user role list", Pattern.CASE_INSENSITIVE)).click(new com.microsoft.playwright.Locator.ClickOptions().setTimeout(3000));
		} catch (RuntimeException ignored) {
			// not a tab here: the fallback below opens the list
		}
		ui.settle(800);
		if (ui.entryCount(Pattern.compile("Export user roles")) == 0) {
			ui.openView("Users");
			ui.tab(Pattern.compile("^user roles$", Pattern.CASE_INSENSITIVE)).click();
			ui.settle(1000);
		}
		Download d = ui.page.waitForDownload(new Page.WaitForDownloadOptions().setTimeout(90_000), () -> ui.clickEntry(Pattern.compile("Export user roles")));
		assertTrue(d.suggestedFilename().toLowerCase().endsWith(".xlsx"), "xlsx: " + d.suggestedFilename());
		Path target = out.resolve("user-roles.xlsx");
		d.saveAs(target);
		List<String> strings = Docx.sharedStrings(target);
		assertTrue(strings.size() > 50, "many cells");
		for (String r : List.of("Admin", "Surveillance Officer", "Surveillance Supervisor", "Lab Officer", "National User")) {
			assertTrue(strings.contains(r), "role " + r);
		}
		assertTrue(strings.stream().anyMatch(s -> s.toLowerCase().contains("case")) && strings.stream().anyMatch(s -> s.toLowerCase().contains("task")), "rights listed");
	}
}
