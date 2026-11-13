package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF50-gdpr-consent.md. Captions in German (C2). */
@DisplayName("WF50 GDPR notice after login until the user consents (SurvOff, C2)")
@AppCopy(AppCopy.Config.C2)
class WF50GdprConsentIT extends WorkflowTest {

	private static final String CONSENT = "Ich habe diese Informationen gelesen, bitte dieses Fenster nicht mehr anzeigen";
	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
	}

	private Locator confirm() {
		return ui.windows().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Bestätigen"));
	}

	private void relogin() {
		ui.logout("Abmelden");
		ui.waitLoginForm(20_000);
		ui.settle(800);
		ui.login("SurvOff");
		ui.settle(2000);
	}

	@Test
	@Order(1)
	@DisplayName("1. Log in as SurvOff: a window DSGVO with the data-minimisation notice, a consent box and a confirm button")
	void step1() {
		ui.login("SurvOff");
		ui.settle(2000);
		assertEquals("DSGVO", ui.windowTitle(), "title");
		assertTrue(ui.windowText().contains("Datenminimierung"), "notice");
		assertEquals(1, ui.windowCheckbox(CONSENT).count(), "consent box");
		assertTrue(confirm().isVisible(), "confirm button");
		assertFalse(ui.windowCheckbox(null).isChecked(), "box not ticked");
	}

	@Test
	@Order(2)
	@DisplayName("2. Confirm without ticking the box: the window closes, the start page is usable")
	void step2() {
		confirm().click();
		ui.settle(1500);
		assertEquals(0, ui.windowsAfterClose(), "window closed");
		assertTrue(ui.bodyText().contains("Überwachungsübersicht"), "start page");
		ui.openView("Fälle");
		assertTrue(ui.bodyText().contains("Fallverzeichnis"), "usable");
	}

	@Test
	@Order(3)
	@DisplayName("3. Log out and in again: the GDPR window appears again")
	void step3() {
		relogin();
		assertEquals("DSGVO", ui.windowTitle(), "title");
	}

	@Test
	@Order(4)
	@DisplayName("4. Tick the box and confirm: the window closes")
	void step4() {
		ui.windowClickText("Ich habe diese Informationen gelesen", false);
		ui.settle(500);
		assertTrue(ui.windowCheckbox(null).isChecked(), "ticked");
		confirm().click();
		ui.settle(1500);
		assertEquals(0, ui.windowsAfterClose(), "window closed");
	}

	@Test
	@Order(5)
	@DisplayName("5. Log out and in again: no GDPR window, the start page shows directly")
	void step5() {
		ui.logout("Abmelden");
		ui.waitLoginForm(20_000);
		ui.settle(800);
		ui.login("SurvOff");
		ui.settle(2500);
		assertEquals(0, ui.windowsAfterClose(), "no window");
		assertTrue(ui.bodyText().contains("Überwachungsübersicht"), "start page");
		// Data check: SurvOff stored as having consented
		assertEquals("t", copy.sql("SELECT hasconsentedtogdpr FROM users WHERE username = 'SurvOff'"));
	}
}
