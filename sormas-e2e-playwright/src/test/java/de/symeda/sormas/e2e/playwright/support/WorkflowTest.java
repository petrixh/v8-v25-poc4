package de.symeda.sormas.e2e.playwright.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import com.microsoft.playwright.Page;

/**
 * Base of every workflow test: one class per workflow (migration/workflows/WFxx-*.md), one ordered test method per
 * workflow step. The class gets a fresh app copy of its {@link AppCopy} kind and a browser; a failed step ends the
 * workflow (later steps are skipped). After each step, the browser must have logged no console error, no page error
 * and no HTTP response of 400 or more.
 */
@ExtendWith(AppCopyExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class WorkflowTest {

	/** The app's date format in C1 (English): M/D/YYYY. */
	public static final DateTimeFormatter EN = DateTimeFormatter.ofPattern("M/d/yyyy");
	/** The app's date format in C2 (German): DD.MM.YYYY. */
	public static final DateTimeFormatter DE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	protected Copy copy;
	protected BrowserSession browser;
	/** The reset date: the app's "today" (Europe/Berlin) when the copy was reset for this class. */
	protected LocalDate today;
	/** target/e2e/&lt;class&gt;: downloads and failure screenshots. */
	protected Path out;

	void attach(Copy copy, BrowserSession browser, LocalDate today, Path out) {
		this.copy = copy;
		this.browser = browser;
		this.today = today;
		this.out = out;
	}

	/** A new browser session (own context) with the app's helpers on its page. */
	protected Ui newUi() {
		Page page = browser.newPage();
		return new Ui(page, copy.base());
	}

	/** A second tab of the same browser session as the given Ui. */
	protected Ui newTab(Ui other) {
		return new Ui(browser.newTab(other.page), copy.base());
	}

	/** today + days in the app's English format, M/D/YYYY. */
	protected String en(int days) {
		return today.plusDays(days).format(EN);
	}

	/** today + days in the app's German format, DD.MM.YYYY. */
	protected String de(int days) {
		return today.plusDays(days).format(DE);
	}

	@AfterEach
	void noBrowserErrors() {
		List<String> problems = browser.takeProblems();
		assertEquals(List.of(), problems, "console errors, page errors or HTTP errors during the step");
	}
}
