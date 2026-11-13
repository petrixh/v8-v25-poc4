package de.symeda.sormas.e2e.playwright.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Credentials;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/**
 * Login through Keycloak (authentication.provider=KEYCLOAK) against a local Keycloak 21.1.2 with the repository's SORMAS
 * realm (env/README.md "Keycloak"). The default users reach Keycloak through the app's own user sync at start, with their
 * SORMAS passwords as temporary credentials: Keycloak asks for a new password at the first login. New passwords are
 * generated here and kept in memory only.
 * <p>
 * Opt-in (tag "keycloak", excluded by default): {@code mvn -f sormas-e2e-playwright/pom.xml verify -De2e.excludedGroups=none
 * -Dit.test=KeycloakLoginIT}.
 */
@Tag("keycloak")
@DisplayName("Keycloak single sign-on: redirect, login, return to the app, REST, logout, refused user (SurvSup, PoeInf, C1 with Keycloak)")
@AppCopy(value = AppCopy.Config.C1, keycloak = true)
class KeycloakLoginIT extends WorkflowTest {

	/**
	 * Keycloak 21 with the realm's login theme "sormas": the theme links the stylesheet lib/zocial/zocial.css, which Keycloak
	 * 21 no longer ships, so Chromium refuses the empty answer. Same realm, theme and Keycloak for the old app (not an app
	 * error); any other console error fails the step.
	 */
	private static final Pattern THEME_CSS = Pattern.compile("^console error: Refused to apply style from '.*/login/sormas/lib/zocial/zocial\\.css'.*");

	private static final SecureRandom RANDOM = new SecureRandom();

	private final HttpClient http = HttpClient.newHttpClient();
	private final List<String> requests = Collections.synchronizedList(new ArrayList<>());
	private Ui ui;
	/** SurvSup's password after Keycloak's first-login change; never printed. */
	private String survSupPassword;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.page.onRequest(r -> requests.add(r.url()));
	}

	private String authUrl() {
		return copy.keycloakBase() + "/keycloak/realms/SORMAS/protocol/openid-connect/auth";
	}

	private Locator kcUsername(Page page) {
		return page.getByLabel("Username", new Page.GetByLabelOptions().setExact(true));
	}

	/** A password Keycloak's realm policy accepts (12+ characters, upper, lower, digit, special). */
	private static String newPassword() {
		byte[] b = new byte[12];
		RANDOM.nextBytes(b);
		return "Kc-" + Base64.getUrlEncoder().withoutPadding().encodeToString(b) + "9a!";
	}

	/**
	 * Signs in on Keycloak's login page with the user's SORMAS password and answers Keycloak's "Update password" (temporary
	 * credential from the sync) with a new one. Returns the new password.
	 */
	private String keycloakLogin(Page page, String user) {
		kcUsername(page).fill(user);
		page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true)).fill(Credentials.passwordOf(user));
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")).click();
		Locator newPw = page.getByLabel("New Password", new Page.GetByLabelOptions().setExact(true));
		newPw.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(30_000));
		assertTrue(page.url().startsWith(copy.keycloakBase() + "/keycloak/realms/SORMAS/login-actions/"), "Keycloak asks for a new password: " + page.url());
		String pw = newPassword();
		newPw.fill(pw);
		page.getByLabel("Confirm password", new Page.GetByLabelOptions().setExact(true)).fill(pw);
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();
		return pw;
	}

	/** The browser problems of the step, without the known theme stylesheet error of Keycloak's pages. */
	private List<String> problemsBeyondTheme() {
		List<String> rest = new ArrayList<>();
		for (String p : browser.takeProblems()) {
			if (!THEME_CSS.matcher(p).matches()) {
				rest.add(p);
			}
		}
		return rest;
	}

	private void screenshot(Page page, String name) {
		try {
			Files.createDirectories(out);
			page.screenshot(new Page.ScreenshotOptions().setPath(out.resolve(name)));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private int restStatus(String user, String password) throws IOException, InterruptedException {
		HttpRequest.Builder r = HttpRequest.newBuilder(URI.create(copy.base() + "/sormas-rest/regions/all/0")).GET();
		if (user != null) {
			r.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8)));
		}
		return http.send(r.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
	}

	@Test
	@Order(1)
	@DisplayName("1. Open a view of the app without a session: the browser is sent to Keycloak's login page (SORMAS realm), not the app")
	void step1() {
		ui.page.navigate(copy.base() + "/sormas-ui/cases");
		kcUsername(ui.page).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(60_000));
		assertTrue(ui.page.url().startsWith(authUrl() + "?"), "Keycloak's authorization endpoint: " + ui.page.url());
		assertTrue(ui.page.url().contains("client_id=sormas-ui"), "the sormas-ui client asks: " + ui.page.url());
		assertTrue(ui.waitText("Sign in to your account", 10_000), "Keycloak's sign-in form");
		assertFalse(ui.menuVisible(), "no app menu");
		screenshot(ui.page, "1-keycloak-login.png");
		assertEquals(List.of(), problemsBeyondTheme(), "browser problems");
	}

	@Test
	@Order(2)
	@DisplayName("2. SurvSup signs in at Keycloak with the SORMAS password and sets a new one: the browser returns to the app and the start view (Surveillance Dashboard) renders")
	void step2() {
		survSupPassword = keycloakLogin(ui.page, "SurvSup");
		ui.menu().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(60_000));
		ui.settle(2000);
		ui.closeCommercialBanner();
		assertTrue(ui.page.url().startsWith(copy.base() + "/sormas-ui/"), "back in the app: " + ui.page.url());
		assertTrue(ui.waitText("Surveillance Dashboard", 30_000), "start view");
		assertTrue(ui.menuText().contains("Logout (Surveillance Supervisor)"), "logged in as SurvSup: " + ui.menuText());
		screenshot(ui.page, "2-first-view.png");
		assertEquals(List.of(), problemsBeyondTheme(), "browser problems");
	}

	@Test
	@Order(3)
	@DisplayName("3. REST through Keycloak (basic auth, Keycloak checks the password): SurvSup's new password 200, a wrong password and no credentials 401")
	void step3() throws Exception {
		assertEquals(200, restStatus("SurvSup", survSupPassword), "SurvSup, new password");
		assertEquals(401, restStatus("SurvSup", "Wrong-password-1!"), "wrong password");
		assertEquals(401, restStatus(null, null), "no credentials");
	}

	@Test
	@Order(4)
	@DisplayName("4. Log out: the app ends its session and Keycloak's (logout endpoint); opening the app again shows Keycloak's login page")
	void step4() {
		requests.clear();
		ui.logout("Logout");
		kcUsername(ui.page).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(60_000));
		List<String> seen;
		synchronized (requests) {
			seen = new ArrayList<>(requests);
		}
		assertTrue(seen.stream().anyMatch(u -> u.startsWith(copy.base() + "/sormas-ui/logout")), "the app's logout address: " + seen);
		assertTrue(
			seen.stream().anyMatch(u -> u.startsWith(copy.keycloakBase() + "/keycloak/realms/SORMAS/protocol/openid-connect/logout")),
			"Keycloak's logout endpoint: " + seen);
		assertFalse(ui.menuVisible(), "no app menu");

		ui.page.navigate(copy.base() + "/sormas-ui/");
		kcUsername(ui.page).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(60_000));
		ui.settle(1500);
		assertTrue(ui.page.url().startsWith(authUrl() + "?"), "Keycloak asks again (no single sign-on session left): " + ui.page.url());
		assertFalse(ui.menuVisible(), "no app menu");
		screenshot(ui.page, "4-after-logout.png");
		assertEquals(List.of(), problemsBeyondTheme(), "browser problems");
	}

	@Test
	@Order(5)
	@DisplayName("5. PoeInf (no web UI right) signs in at Keycloak: the app refuses with HTTP 403 and shows no view, as the container check did before")
	void step5() {
		Ui poe = newUi();
		List<Response> refused = Collections.synchronizedList(new ArrayList<>());
		poe.page.onResponse(r -> {
			if (r.status() == 403) {
				refused.add(r);
			}
		});
		poe.page.navigate(copy.base() + "/sormas-ui/");
		kcUsername(poe.page).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(60_000));
		keycloakLogin(poe.page, "PoeInf");
		poe.page.waitForURL(copy.base() + "/sormas-ui/oidc-login", new Page.WaitForURLOptions().setTimeout(60_000));
		poe.settle(1500);
		assertEquals(1, refused.size(), "one refused response");
		assertTrue(refused.get(0).url().startsWith(copy.base() + "/sormas-ui/oidc-login"), "refused by the app: " + refused.get(0).url());
		assertTrue(poe.waitText("Forbidden", 10_000), "403 page");
		assertFalse(poe.menuVisible(), "no app menu");
		screenshot(poe.page, "5-refused.png");

		List<String> problems = problemsBeyondTheme();
		assertEquals(
			List.of(
				"HTTP 403 " + copy.base() + "/sormas-ui/oidc-login",
				"console error: Failed to load resource: the server responded with a status of 403 (Forbidden)"),
			problems.stream().sorted().toList(),
			"only the refusal");
	}
}
