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

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.DirtiesAppMemory;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/**
 * migration/workflows/WF40-manage-user.md. The new user's generated password is shown once; it is never read or
 * recorded, and no screenshot is taken while it shows.
 */
@DisplayName("WF40 Create, edit and deactivate a user (admin, C1)")
@DirtiesAppMemory("creates, edits and deactivates a user: user cache")
@AppCopy(AppCopy.Config.C1)
class WF40ManageUserIT extends WorkflowTest {

	Ui ui;
	Locator win;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("admin");
		win = ui.windows();
	}

	/** The form has two Region/District pairs; the jurisdiction pair is the last. */
	private Locator wf(String label) {
		return ui.windowField(label, false);
	}

	private void openWfvictor() {
		ui.clickRow(ui.rowIndex("wfvictor"));
		ui.settle(2000);
	}

	@Test
	@Order(1)
	@DisplayName("1. Users: the default users with their roles and active state")
	void step1() {
		ui.openView("Users");
		List<String> rows = ui.rowTexts();
		assertEquals(13, rows.size(), "users");
		for (String u : List.of("admin", "SurvSup", "SurvOff", "NatUser", "LabOff", "HospInf", "PoeSup", "PoeInf")) {
			assertTrue(rows.stream().anyMatch(r -> r.contains(" " + u + " ")), u + " listed");
		}
		assertFalse(rows.stream().anyMatch(r -> r.contains("wfvictor")), "no wfvictor yet");
	}

	@Test
	@Order(2)
	@DisplayName("2. Create Victor Workflow, Surveillance Officer, Default region and district, user name wfvictor; save: listed as active")
	void step2() {
		ui.button("New user").click();
		ui.settle(2000);
		assertTrue(win.getByText("Create new user", new Locator.GetByTextOptions().setExact(true)).isVisible(), "form title");
		wf("First name").fill("Victor");
		wf("Last name").fill("Workflow");
		wf("Email").fill("victor@example.invalid");
		wf("Email").press("Tab");
		ui.settle(1500);
		wf("User name").fill("wfvictor");
		wf("User name").press("Tab");
		ui.settle(800);
		assertEquals("wfvictor", wf("User name").inputValue(), "user name");
		ui.chooseOption("Surveillance Officer");
		ui.windowChooseCombo("Region", "Default Region");
		ui.windowChooseCombo("District", "Default District");
		ui.windowButton("Save");
		ui.settle(3000);
		assertTrue(ui.page.getByText("Please copy this password").isVisible(), "password shown once");
		ui.closeWindow(); // closed without reading the password
		ui.noModal();
		assertEquals(0, win.count(), "windows left");
		ui.settle(1500);
		String r = ui.rowTexts().stream().filter(x -> x.contains("wfvictor")).findFirst().orElse("");
		assertTrue(r.contains("Surveillance Officer") && r.contains("Victor Workflow") && r.contains("Default District"), "listed: " + r);
	}

	@Test
	@Order(3)
	@DisplayName("3. Open wfvictor, add a phone number; save: saved")
	void step3() {
		openWfvictor();
		assertTrue(win.getByText("Edit user").first().isVisible(), "edit form");
		wf("Phone number").fill("+358401234567");
		wf("Phone number").press("Tab");
		ui.settle(500);
		ui.windowButton("Save");
		ui.settle(2500);
		ui.noModal();
		assertEquals(0, win.count(), "windows left");
		openWfvictor();
		assertEquals("+358401234567", wf("Phone number").inputValue(), "phone");
		ui.closeWindow();
		ui.noModal();
	}

	@Test
	@Order(4)
	@DisplayName("4. Deactivate wfvictor and save: the list filtered to inactive users shows wfvictor")
	void step4() {
		openWfvictor();
		ui.chooseOption("Active?");
		ui.windowButton("Save");
		ui.settle(2500);
		ui.noModal();
		Locator active = ui.filterCombo(0);
		assertEquals("Active?", Ui.promptOf(active), "filter prompt");
		ui.chooseFilter(active, "Inact", Pattern.compile("^inactive$", Pattern.CASE_INSENSITIVE));
		List<String> rows = ui.rowTexts();
		assertEquals(1, rows.size(), "inactive users: " + rows);
		assertTrue(rows.get(0).contains("wfvictor") && rows.get(0).contains("Surveillance Officer"), "wfvictor listed");
		// Data check: one new inactive user wfvictor with role Surveillance Officer in the Default district
		assertEquals(
			"f|Default District|+358401234567|Surveillance Officer",
			copy.sql(
				"SELECT u.active, d.name, u.phone, r.caption FROM users u JOIN district d ON d.id = u.district_id"
					+ " JOIN users_userroles ur ON ur.user_id = u.id JOIN userroles r ON r.id = ur.userrole_id WHERE u.username = 'wfvictor'"));
	}
}
