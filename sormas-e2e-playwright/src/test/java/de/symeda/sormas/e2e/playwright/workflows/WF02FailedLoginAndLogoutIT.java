package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF02-failed-login-and-logout.md */
@DisplayName("WF02 Wrong password is refused; logout ends the session (SurvOff, C1)")
@AppCopy(AppCopy.Config.C1)
class WF02FailedLoginAndLogoutIT extends WorkflowTest {

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
	}

	@Test
	@Order(1)
	@DisplayName("1. Open the app: the login page is shown, not the application")
	void step1() {
		ui.page.navigate(copy.base() + "/sormas-ui/");
		ui.waitLoginForm(30_000);
		assertTrue(ui.loginFormVisible(), "login form");
		assertFalse(ui.menuVisible(), "no menu");
	}

	@Test
	@Order(2)
	@DisplayName("2. SurvOff with a wrong password: the login page stays with the message that the login failed, no server error page")
	void step2() {
		ui.loginAttempt("SurvOff", "not-the-password");
		ui.settle(1500);
		assertTrue(ui.waitText("Login failed", 10_000) || Pattern.compile("login failed|invalid|incorrect|wrong|failed", Pattern.CASE_INSENSITIVE)
			.matcher(ui.pageText()).find(), "failure message");
		String t = ui.pageText();
		assertFalse(ui.menuVisible(), "no menu");
		assertTrue(ui.loginVisible(), "login stays");
		assertFalse(Pattern.compile("exception|internal server error|HTTP Status", Pattern.CASE_INSENSITIVE).matcher(t).find(), "no server error");
	}

	@Test
	@Order(3)
	@DisplayName("3. Log in as SurvOff with the right password: the dashboard is shown")
	void step3() {
		ui.login("SurvOff");
		assertTrue(ui.pageText().contains("Surveillance Dashboard"), "dashboard");
	}

	@Test
	@Order(4)
	@DisplayName("4. Open the case directory: the seeded cases are listed")
	void step4() {
		ui.openView("Cases");
		assertTrue(ui.page.getByText("Case Directory", new com.microsoft.playwright.Page.GetByTextOptions().setExact(true)).first().isVisible(), "directory");
		assertTrue(!ui.rowTexts().isEmpty(), "cases listed");
	}

	@Test
	@Order(5)
	@DisplayName("5. Log out: the login page is shown")
	void step5() {
		ui.logout("Logout");
		ui.waitLoginForm(20_000);
		assertTrue(ui.loginVisible(), "login form");
		assertFalse(ui.menuVisible(), "no menu");
	}

	@Test
	@Order(6)
	@DisplayName("6. The browser's back button: the login page (or a fresh login) is shown, not the case list")
	void step6() {
		ui.page.goBack();
		ui.settle(1500);
		assertFalse(ui.pageText().contains("Case Directory"), "no case list");
		assertFalse(ui.menuVisible(), "no menu");
		assertTrue(ui.loginVisible(), "login form");
	}
}
