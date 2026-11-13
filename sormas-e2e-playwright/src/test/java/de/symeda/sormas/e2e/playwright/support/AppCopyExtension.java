package de.symeda.sormas.e2e.playwright.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestWatcher;

import com.microsoft.playwright.Page;

/**
 * Lifecycle of a workflow test class ({@link WorkflowTest}):
 * <ul>
 * <li>before the class: a copy of the class's {@link AppCopy} kind in the seed state from the run's
 * {@link AppCopyPool}, a browser, and the reset date (the app's "today": Europe/Berlin, taken right after the reset);
 * <li>steps run in order; once a step fails, the later steps are skipped as not reached;
 * <li>after a failed step: a full-page screenshot of every open page in target/e2e/&lt;class&gt;/;
 * <li>after the class: browser closed, copy released for the next class. The pool removes the copies at the end of
 * the run.
 * </ul>
 */
public class AppCopyExtension implements BeforeAllCallback, AfterAllCallback, ExecutionCondition, AfterTestExecutionCallback, TestWatcher {

	public static final ZoneId APP_ZONE = ZoneId.of("Europe/Berlin");
	private static final Namespace NS = Namespace.create(AppCopyExtension.class);

	/**
	 * One pool for the JVM's lifetime, closed by a shutdown hook. With several failsafe forks the JUnit launcher is called
	 * once per test class, so a pool kept in the launcher's root store would be closed (copies removed) after every class.
	 */
	private static AppCopyPool shared;

	private static synchronized AppCopyPool pool(ExtensionContext context) {
		if (shared == null) {
			shared = AppCopyPool.fromSystemProperties();
			Runtime.getRuntime().addShutdownHook(new Thread(shared::close, "e2e-pool-close"));
		}
		return shared;
	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		WorkflowTest test = (WorkflowTest) context.getRequiredTestInstance();
		AppCopy kind = context.getRequiredTestClass().getAnnotation(AppCopy.class);
		if (kind == null) {
			throw new IllegalStateException(context.getRequiredTestClass().getSimpleName() + " has no @AppCopy");
		}
		AppCopyPool pool = pool(context);
		Copy copy = pool.acquire(kind);
		context.getStore(NS).put(Copy.class, copy);
		BrowserSession browser = new BrowserSession(Boolean.parseBoolean(System.getProperty("e2e.headless", "true")));
		context.getStore(NS).put(BrowserSession.class, browser);
		test.attach(copy, browser, LocalDate.now(APP_ZONE), outDir(context));
		System.out.printf("[e2e] %s on %s, reset date %s%n", context.getRequiredTestClass().getSimpleName(), copy, test.today);
	}

	@Override
	public void afterAll(ExtensionContext context) {
		BrowserSession browser = context.getStore(NS).remove(BrowserSession.class, BrowserSession.class);
		Copy copy = context.getStore(NS).remove(Copy.class, Copy.class);
		try {
			if (browser != null) {
				browser.close();
			}
		} finally {
			if (copy != null) {
				copy.dirty = context.getRequiredTestClass().isAnnotationPresent(DirtiesAppMemory.class);
				pool(context).release(copy);
			}
		}
	}

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		if (context.getTestMethod().isEmpty()) {
			return ConditionEvaluationResult.enabled("class");
		}
		String failed = context.getParent().map(p -> p.getStore(NS).get("failedStep", String.class)).orElse(null);
		return failed == null ? ConditionEvaluationResult.enabled("earlier steps passed") : ConditionEvaluationResult.disabled("not reached: " + failed + " failed");
	}

	@Override
	public void afterTestExecution(ExtensionContext context) throws Exception {
		if (context.getExecutionException().isEmpty()) {
			return;
		}
		BrowserSession browser = context.getParent().map(p -> p.getStore(NS).get(BrowserSession.class, BrowserSession.class)).orElse(null);
		if (browser == null) {
			return;
		}
		Path dir = outDir(context);
		Files.createDirectories(dir);
		List<Page> pages = browser.pages();
		for (int i = 0; i < pages.size(); i++) {
			Page p = pages.get(i);
			if (p.isClosed()) {
				continue;
			}
			try {
				p.screenshot(new Page.ScreenshotOptions().setFullPage(true).setPath(dir.resolve(context.getRequiredTestMethod().getName() + "-failed-page" + (i + 1) + ".png")));
			} catch (RuntimeException e) {
				System.out.println("[e2e] no failure screenshot: " + e.getMessage());
			}
		}
	}

	/** A failed step (its method or its after-step checks) ends the workflow: later steps are not reached. */
	@Override
	public void testFailed(ExtensionContext context, Throwable cause) {
		context.getParent().ifPresent(p -> p.getStore(NS).put("failedStep", context.getDisplayName()));
	}

	private static Path outDir(ExtensionContext context) {
		return Path.of(System.getProperty("e2e.out", "target/e2e")).resolve(context.getRequiredTestClass().getSimpleName());
	}
}
