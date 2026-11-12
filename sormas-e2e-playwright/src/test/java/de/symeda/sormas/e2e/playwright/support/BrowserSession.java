package de.symeda.sormas.e2e.playwright.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * Chromium for one test class. Each {@link #newPage()} is a fresh browser context (own session), so one class can act as
 * two users at once. The browser sends a real Chrome user agent ("HeadlessChrome" replaced with "Chrome"), as
 * env/smoke.cjs and the replay launcher do.
 * <p>
 * Records console errors, page errors and HTTP responses of 400 or more per page; {@link #takeProblems()} returns and
 * clears them.
 */
public final class BrowserSession implements AutoCloseable {

	public static final int WIDTH = 2300;
	public static final int HEIGHT = 1300;

	/** The exact text Chromium logs for a request aborted by a network interface change; any other text fails the step. */
	static final String NETWORK_CHANGED = "Failed to load resource: net::ERR_NETWORK_CHANGED";

	private final Playwright playwright;
	private final Browser browser;
	private final String userAgent;
	private final List<Page> pages = new ArrayList<>();
	private final List<String> problems = Collections.synchronizedList(new ArrayList<>());

	BrowserSession(boolean headless) {
		playwright = Playwright.create();
		browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
		try (BrowserContext probe = browser.newContext()) {
			userAgent = ((String) probe.newPage().evaluate("() => navigator.userAgent")).replace("HeadlessChrome", "Chrome");
		}
	}

	public Page newPage() {
		BrowserContext ctx = browser.newContext(
			new Browser.NewContextOptions().setViewportSize(WIDTH, HEIGHT).setUserAgent(userAgent).setAcceptDownloads(true));
		ctx.setDefaultTimeout(30_000);
		return watch(ctx.newPage());
	}

	/** A second tab in the same browser context (same session) as the given page. */
	public Page newTab(Page sameContext) {
		return watch(sameContext.context().newPage());
	}

	private Page watch(Page page) {
		page.onConsoleMessage(m -> {
			// ERR_NETWORK_CHANGED: Chromium aborts a request when the host's network interfaces change, which they do
			// whenever another app copy's Docker network is created or removed during a parallel run. Not the app's error.
			if ("error".equals(m.type()) && !NETWORK_CHANGED.equals(m.text().trim())) {
				problems.add("console error: " + m.text());
			}
		});
		page.onPageError(e -> problems.add("page error: " + e));
		page.onResponse(r -> {
			if (r.status() >= 400) {
				problems.add("HTTP " + r.status() + " " + r.url());
			}
		});
		pages.add(page);
		return page;
	}

	public List<Page> pages() {
		return pages;
	}

	public List<String> takeProblems() {
		synchronized (problems) {
			List<String> out = new ArrayList<>(problems);
			problems.clear();
			return out;
		}
	}

	@Override
	public void close() {
		playwright.close();
	}
}
