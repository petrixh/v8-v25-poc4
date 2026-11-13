package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF42-role-right-change.md */
@DisplayName("WF42 Changing a role's right changes what its users see (admin, LabOff; C1)")
@AppCopy(AppCopy.Config.C1)
class WF42RoleRightChangeIT extends WorkflowTest {

	/** The export right requires "Access statistics": the app refuses to save one without the other. */
	private static final String STATS = "Access statistics";
	private static final String EXPORT = "Export detailed statistics from SORMAS";

	Ui adm;
	Ui lab;
	String rightsAtStart;

	@BeforeAll
	void open() {
		adm = newUi();
		lab = newUi();
		rightsAtStart = rights();
	}

	private String rights() {
		return copy.sql("SELECT count(*) FROM userroles_userrights ur JOIN userroles r ON r.id = ur.userrole_id WHERE r.caption = 'Lab Officer'");
	}

	private List<String> menuOf(Ui ui) {
		return Arrays.stream(ui.menuText().split("\n|\\s{2,}")).map(s -> s.replaceAll("[\\ue000-\\uf8ff]", "").replaceAll("\\s+", " ").trim()).filter(s -> !s.isEmpty()).toList();
	}

	private boolean hasStatistics(Ui ui) {
		return ui.menu().getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Statistics").setExact(true)).count() > 0
			|| menuOf(ui).stream().anyMatch(x -> x.equalsIgnoreCase("Statistics"));
	}

	private void logout(Ui ui) {
		ui.logout("Logout");
		ui.waitLoginForm(30_000);
	}

	/** Opens the Lab Officer role in the admin's user role list (exactly "Lab Officer", not "External Lab Officer"). */
	private void openLabRole() {
		adm.openView("Users");
		adm.tab(Pattern.compile("^user roles$", Pattern.CASE_INSENSITIVE)).click();
		adm.waitText("Lab Officer", 10_000);
		adm.settle(800);
		List<String> texts = adm.rowTexts();
		int i = -1;
		for (int k = 0; k < texts.size(); k++) {
			if (Pattern.compile(" Lab Officer Laboratory$").matcher(texts.get(k).trim()).find()) {
				i = k;
				break;
			}
		}
		assertTrue(i >= 0, "Lab Officer row in " + texts);
		adm.clickRow(i);
		adm.waitText(STATS, 15_000);
		adm.settle(800);
	}

	@Test
	@Order(1)
	@DisplayName("1. Log in as LabOff: the main menu shows Statistics; log out")
	void step1() {
		lab.login("LabOff");
		assertTrue(hasStatistics(lab), "Statistics in the menu: " + menuOf(lab));
		logout(lab);
		assertTrue(lab.loginVisible(), "logged out");
	}

	@Test
	@Order(2)
	@DisplayName("2. As admin untick the right to access statistics (refused until the dependent export right is unticked too); save: saved")
	void step2() {
		adm.login("admin");
		openLabRole();
		adm.checkboxReveal(STATS);
		assertTrue(adm.checkboxIsChecked(STATS), "ticked before");
		adm.checkboxToggle(STATS);
		adm.settle(300);
		assertFalse(adm.checkboxIsChecked(STATS), "unticked");
		adm.savePage();
		assertTrue(adm.waitText("required based on other user right", 8000) || Pattern.compile("required based on other user right", Pattern.CASE_INSENSITIVE).matcher(adm.notificationTexts()).find(), "first save refused");
		adm.dismissNotifications();
		adm.setCheckbox(EXPORT, false);
		adm.savePage();
		adm.settle(1500);
		openLabRole();
		adm.checkboxReveal(STATS);
		assertFalse(adm.checkboxIsChecked(STATS), "statistics right after reopen");
		assertFalse(adm.checkboxIsChecked(EXPORT), "export right after reopen");
	}

	@Test
	@Order(3)
	@DisplayName("3. Log in as LabOff: no Statistics in the main menu; opening it by other means gives the access-denied page")
	void step3() {
		lab.login("LabOff");
		assertFalse(hasStatistics(lab), "Statistics gone from the menu: " + menuOf(lab));
		lab.goTo("statistics");
		assertTrue(lab.waitText("Access denied", 15_000), "access denied");
		String t = lab.pageText();
		assertTrue(Pattern.compile("do not have the required rights").matcher(t).find(), "required rights text");
	}

	@Test
	@Order(4)
	@DisplayName("4. As admin tick both rights again and save; LabOff logs in again: Statistics is back; the role ends with its default rights")
	void step4() {
		openLabRole();
		adm.setCheckbox(STATS, true);
		adm.setCheckbox(EXPORT, true);
		adm.savePage();
		adm.settle(1500);
		openLabRole();
		adm.checkboxReveal(STATS);
		assertTrue(adm.checkboxIsChecked(STATS), "statistics right again");
		assertTrue(adm.checkboxIsChecked(EXPORT), "export right again");
		logout(lab);
		lab.login("LabOff");
		assertTrue(hasStatistics(lab), "Statistics back: " + menuOf(lab));
		// Data check: the Lab Officer role ends with its default rights
		assertEquals(rightsAtStart, rights(), "number of rights");
	}
}
