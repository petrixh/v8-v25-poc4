package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.DirtiesAppMemory;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF01-default-password-alert.md. Never generates a password: it would change the test users. */
@DisplayName("WF01 Security alert for default passwords after login (admin, SurvOff; C1, password check on)")
@DirtiesAppMemory("default-password users and their sessions")
@AppCopy(value = AppCopy.Config.C1, passwordCheck = true)
class WF01DefaultPasswordAlertIT extends WorkflowTest {

	Ui ui;
	/** Fingerprint of the two users' password hashes after the reset (compared, never printed). */
	String passwordsBefore;

	@BeforeAll
	void open() {
		ui = newUi();
		passwordsBefore = passwords();
	}

	private String passwords() {
		return copy.sql("SELECT md5(string_agg(username || password, ',' ORDER BY username)) FROM users WHERE username IN ('admin', 'SurvOff')");
	}

	private Locator remind() {
		return ui.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Remind me later"));
	}

	private void signIn(String user) {
		ui.submitLogin(user);
		ui.windows().getByText("Security Alert").first().waitFor(new Locator.WaitForOptions().setTimeout(60_000));
		ui.settle(1000);
	}

	@Test
	@Order(1)
	@DisplayName("1. Log in as admin: a Security Alert window offers Generate new password and Remind me later")
	void step1() {
		signIn("admin");
		String t = ui.pageText();
		assertTrue(t.contains("You have just logged in using the default password for this user"), "own default password text");
		assertTrue(ui.button("Generate new password").isVisible(), "generate offered");
		assertTrue(remind().isVisible(), "remind me later offered");
		assertFalse(t.contains("unchanged"), "no list of other users yet");
	}

	@Test
	@Order(2)
	@DisplayName("2. Remind me later: a second Security Alert lists the other users with default passwords")
	void step2() {
		remind().click();
		ui.settle(2000);
		String t = ui.pageText();
		assertTrue(ui.windows().getByText("Security Alert").first().isVisible(), "second alert");
		assertTrue(t.contains("Your installation of SORMAS has default logins enabled"), "other users text");
		for (String u : new String[] {
			"CaseSup",
			"CommOff",
			"ContSup",
			"HospInf" }) {
			assertTrue(t.contains(u), "lists " + u);
		}
		assertTrue(ui.button("Generate new passwords").isVisible(), "generate many offered");
	}

	@Test
	@Order(3)
	@DisplayName("3. Remind me later again: both windows close, the admin start page with the main menu shows")
	void step3() {
		remind().click();
		ui.settle(2500);
		assertEquals(0, ui.windowsAfterClose(), "windows closed");
		assertTrue(ui.menuVisible(), "menu shown");
		String t = ui.pageText();
		assertTrue(t.contains("Surveillance Dashboard"), "start page");
		assertFalse(t.contains("Security Alert"), "no alert left");
	}

	@Test
	@Order(4)
	@DisplayName("4. Log out, log in as SurvOff: the alert for the own default password, no list of other users")
	void step4() {
		ui.logout("Logout");
		ui.settle(2000);
		signIn("SurvOff");
		String t = ui.pageText();
		assertTrue(t.contains("You have just logged in using the default password for this user"), "own default password text");
		assertFalse(t.contains("unchanged") || t.contains("Your installation of SORMAS has default logins"), "no user list");
	}

	@Test
	@Order(5)
	@DisplayName("5. Remind me later: the surveillance officer's start page (dashboard) shows")
	void step5() {
		remind().click();
		ui.settle(2500);
		assertEquals(0, ui.windowsAfterClose(), "windows closed");
		assertTrue(ui.pageText().contains("Surveillance Dashboard"), "dashboard");
		assertTrue(ui.menuVisible(), "menu shown");
		// Data check: no password changed
		assertTrue(passwords().equals(passwordsBefore), "admin's and SurvOff's passwords unchanged");
	}
}
