package de.symeda.sormas.e2e.playwright.support;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.FileChooser;
import com.microsoft.playwright.JSHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * The app's UI for workflow tests, the way a user finds things: buttons, links, tabs and menu items by role and name,
 * fields by their label, texts by visible text. Vaadin component tags are used only where Flow gives no role or label:
 * the login form, dialogs (vaadin-dialog), combo-box suggestions, notifications, and grid rows and cells (in the grid's
 * shadow DOM, read with a script). No XPath, no generated ids. Selectors live here only; tests use these helpers.
 * Ported from the replay helpers that drove these workflows on this app (migration replay scripts, Flow helpers).
 */
public final class Ui {

	private static final String INPUT = "input:not([type=radio]):not([type=checkbox]), textarea";

	public final Page page;
	private final String base;

	Ui(Page page, String base) {
		this.page = page;
		this.base = base;
	}

	public String base() {
		return base;
	}

	// ---- Waiting and text ----

	/**
	 * Waits until the page is quiet: loaded, then {@code ms}, then no Vaadin request in flight. Timeouts are not errors.
	 */
	public void settle(int ms) {
		try {
			page.waitForLoadState(LoadState.LOAD);
		} catch (TimeoutError ignored) {
			// not a Vaadin page or still loading: the wait below covers it
		}
		// No fixed sleep of ms: wait until no Vaadin request is in flight, then a short quiet period (at most 200 ms,
		// for client-side rendering that follows a response), then idle again.
		waitVaadinIdle();
		page.waitForTimeout(Math.min(ms, 200));
		waitVaadinIdle();
	}

	private void waitVaadinIdle() {
		try {
			page.waitForFunction(
				"() => { const c = window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients; return !c || !Object.values(c).some(x => x && x.isActive && x.isActive()); }",
				null,
				new Page.WaitForFunctionOptions().setTimeout(10_000).setPollingInterval(50));
		} catch (TimeoutError ignored) {
			// a long request: the next step's own waits decide
		}
	}

	public void settle() {
		settle(400);
	}

	/** Visible text of the page, white space normalised. */
	public String pageText() {
		return norm(page.locator("body").innerText());
	}

	public String bodyText() {
		return page.locator("body").innerText();
	}

	/** Waits up to ms for the text to appear in the page. */
	public boolean waitText(String s, int ms) {
		long end = System.currentTimeMillis() + ms;
		while (System.currentTimeMillis() < end) {
			if (pageText().contains(s)) {
				return true;
			}
			page.waitForTimeout(250);
		}
		return false;
	}

	public static String norm(String s) {
		return s == null ? "" : s.replaceAll("\\s+", " ").trim();
	}

	/** Escapes regular expression characters and slashes: the pattern goes to the browser as a JavaScript regular expression. */
	static String esc(String s) {
		return s.replaceAll("[.*+?^${}()|\\[\\]\\\\/]", "\\\\$0");
	}

	static Pattern exact(String s) {
		return Pattern.compile("^\\s*" + esc(s) + "\\s*$");
	}

	// ---- Login, menu ----

	public void goTo(String route) {
		page.navigate(base + "/sormas-ui/" + route);
	}

	/** Submits the login form without waiting for the main menu (a window may come first). */
	public void submitLogin(String user) {
		page.navigate(base + "/sormas-ui/login");
		page.locator("vaadin-login-form input[name='username']").fill(user);
		page.locator("vaadin-login-form input[name='password']").fill(Credentials.passwordOf(user));
		page.locator("vaadin-login-form vaadin-button[slot='submit']").click();
	}

	/** Logs in and waits for the main menu. */
	public void login(String user) {
		submitLogin(user);
		menu().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(60_000));
		settle(1500);
		closeCommercialBanner();
	}

	/**
	 * CI builds the app with Vaadin's commercial-with-banner option (CI.md, decision E002 C). Flow then adds a fixed
	 * banner at the bottom of every page it loads, which covers buttons there (the dashboard map's Layers button). It
	 * sits in closed shadow roots, so no locator reaches it: it is closed with its own Close button the way a keyboard
	 * user would, as the last focusable element of the page (Shift+Tab from the page start, Enter). Only on pages that
	 * carry the banner's script; licensed builds have none, and nothing else is pressed.
	 */
	public void closeCommercialBanner() {
		boolean banner = (Boolean) page.evaluate("() => [...document.scripts].some(s => s.textContent.includes('vaadin-commercial-banner'))");
		if (!banner) {
			return;
		}
		page.evaluate("() => { if (document.activeElement && document.activeElement.blur) document.activeElement.blur(); }");
		page.keyboard().press("Shift+Tab");
		page.keyboard().press("Enter");
		settle(500);
	}

	public boolean loginVisible() {
		return page.locator("vaadin-login-form input[name='username']").isVisible();
	}

	public void waitLoginForm(int ms) {
		page.locator("vaadin-login-form input[name='username']").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(ms));
	}

	/** The main menu (the app keeps the Valo menu's class name). */
	public Locator menu() {
		return page.locator(".valo-menu");
	}

	public boolean menuVisible() {
		return menu().isVisible();
	}

	public String menuText() {
		return norm(menu().innerText());
	}

	/** Opens a main menu view by its label. */
	public void openView(String label) {
		menu().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(label).setExact(true)).first().click();
		settle(1200);
	}

	public void logout(String label) {
		menu().getByRole(AriaRole.MENUITEM, new Locator.GetByRoleOptions().setName(Pattern.compile("^" + esc(label)))).first().click();
	}

	/** Opens an entry of the user menu bar (user settings) by its text. */
	public void openSettings(String label) {
		page.locator("vaadin-menu-bar-button").filter(new Locator.FilterOptions().setHasText(label)).first().click();
		settle(1500);
	}

	// ---- Buttons, links, tabs ----

	public Locator button(String name) {
		return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true)).first();
	}

	public Locator button(Pattern name) {
		return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name)).first();
	}

	/** A button's name starts with its text: once its tooltip has shown, the tooltip's text is part of the name. */
	public void applyFilters() {
		button(Pattern.compile("^(apply filters|Filter anwenden)", Pattern.CASE_INSENSITIVE)).click();
	}

	public void resetFilters() {
		button(Pattern.compile("^(reset filters|Filter zurücksetzen)", Pattern.CASE_INSENSITIVE)).click();
	}

	/** Sub-menu tabs are links. */
	public Locator tab(String label) {
		return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(label).setExact(true)).first();
	}

	/** Status tabs above a directory (e.g. "Investigation pending"), buttons whose name starts with the label. */
	public void openStatusTab(String label) {
		button(Pattern.compile("^" + esc(label))).click();
	}

	private List<Locator> entries(Object text) {
		List<Locator> out = new ArrayList<>();
		for (AriaRole r : new AriaRole[] {
			AriaRole.BUTTON,
			AriaRole.LINK,
			AriaRole.MENUITEM,
			AriaRole.TAB }) {
			Page.GetByRoleOptions o = new Page.GetByRoleOptions();
			if (text instanceof Pattern p) {
				o.setName(p);
			} else {
				o.setName((String) text).setExact(true);
			}
			out.add(page.getByRole(r, o).locator("visible=true"));
		}
		return out;
	}

	public int entryCount(Object text) {
		return entries(text).stream().mapToInt(Locator::count).sum();
	}

	/** Clicks the nth visible button, link, menu item or tab with that name (exact text, or a pattern). */
	public void clickEntry(Object text, int nth) {
		long end = System.currentTimeMillis() + 10_000;
		do {
			for (Locator l : entries(text)) {
				if (l.count() > nth) {
					l.nth(nth).click();
					return;
				}
			}
			page.waitForTimeout(250);
		} while (System.currentTimeMillis() < end);
		throw new AssertionError("no button, link, menu item or tab: " + text);
	}

	/** Waits until the page text matches the regular expression (case-blind); returns whether it did within the time. */
	public boolean waitMatch(String regex, int ms) {
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		long end = System.currentTimeMillis() + ms;
		do {
			if (p.matcher(pageText()).find()) {
				return true;
			}
			page.waitForTimeout(250);
		} while (System.currentTimeMillis() < end);
		return false;
	}

	public void clickEntry(Object text) {
		clickEntry(text, 0);
	}

	/** Clicks the first visible Save button of the page. */
	public void save() {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save").setExact(true)).locator("visible=true").first().click();
	}

	/** The visible Save button of the page (the last one: the form's own). */
	public void savePage() {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save").setExact(true)).locator("visible=true").last().click();
	}

	/** Chooses an item of a menu bar (e.g. Bulk Actions > Edit...). */
	public void chooseMenuBarItem(String menu, String item) {
		page.locator("vaadin-menu-bar-button").filter(new Locator.FilterOptions().setHasText(menu)).first().click();
		settle(600);
		page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(item).setExact(true)).locator("visible=true").first().click();
	}

	/** Clicks a link by its exact text: an anchor, or the app's link-styled label (.v-link) where Flow gives no link role. */
	public void clickLink(String text) {
		page.locator("a, .v-link").filter(new Locator.FilterOptions().setHasText(exact(text))).first().click();
	}

	// ---- Fields ----

	/** The input of a field found by its label: on the input itself or on its host (date picker, combo box). */
	public Locator field(String label) {
		return inputOf(page.getByLabel(label, new Page.GetByLabelOptions().setExact(true)));
	}

	public Locator field(Pattern label) {
		return inputOf(page.getByLabel(label));
	}

	private Locator inputOf(Locator labelled) {
		return labelled.and(page.locator(INPUT)).or(labelled.locator(INPUT)).first();
	}

	public void fillField(String label, String value) {
		Locator input = field(label);
		input.fill(value);
		input.press("Tab");
		settle(600);
	}

	/** A date input by position (date pickers without an accessible name). */
	public Locator dateInput(int index, boolean inWindow) {
		return (inWindow ? windows().last() : page.locator("body")).locator("vaadin-date-picker input").nth(index);
	}

	/** Enter commits the typed date and closes the calendar pop-up. */
	public void fillDate(Locator input, String value, int wait) {
		input.fill(value);
		input.press("Enter");
		input.press("Tab");
		settle(200);
		settle(wait);
	}

	/** Values of all inputs of the page or of the last open window. */
	public List<String> inputValues(boolean inWindow) {
		Locator scope = inWindow ? windows().last() : page.locator("body");
		@SuppressWarnings("unchecked")
		List<String> v = (List<String>) scope.locator("input").evaluateAll("es => es.map(e => e.value)");
		return v;
	}

	/** A search box of a directory by its id (the app's own ids: caseLike, personLike, freeText, ...). */
	public void fillSearch(String id, String value) {
		page.locator("[id=\"" + id + "\"] input, input[id=\"" + id + "\"]").first().fill(value);
	}

	// ---- Combo boxes ----

	/** The suggestions of the open overlay: only visible ones (a hidden item of an earlier overlay can match first). */
	private Locator suggestions() {
		return page.locator("vaadin-combo-box-item").locator("visible=true");
	}

	/** Types into a combo box and picks the suggestion with exactly that text. */
	public void pickFromCombo(Locator input, String text) {
		input.click();
		input.pressSequentially(text, new Locator.PressSequentiallyOptions().setDelay(40));
		settle(500);
		suggestions().filter(new Locator.FilterOptions().setHasText(exact(text))).first().click();
		settle(700);
	}

	public void chooseCombo(String label, String text) {
		pickFromCombo(field(label), text);
	}

	public void typeInCombo(Locator input, String typed, boolean replace, int wait, boolean click) {
		if (click) {
			input.click();
		}
		if (replace) {
			input.press("Control+a");
		}
		input.pressSequentially(typed, new Locator.PressSequentiallyOptions().setDelay(40));
		settle(wait);
	}

	/** Picks the open suggestion whose text is exactly the given text, or matches the pattern. */
	public void pickSuggestion(Object text, int after) {
		suggestions().filter(new Locator.FilterOptions().setHasText(visible(text))).first().click();
		settle(after);
	}

	/** The texts of the visible suggestions; waits up to 3 s for the list to show (an empty list is read after that). */
	public String suggestionsText() {
		for (int i = 0; i < 12; i++) {
			List<String> t = suggestions().allInnerTexts();
			if (!t.isEmpty()) {
				return String.join(" ", t);
			}
			page.waitForTimeout(250);
		}
		return "";
	}

	/** Type, then pick: the common way to set a combo box. */
	public void comboPick(Locator input, String typed, Object text, boolean replace, int wait, int after) {
		typeInCombo(input, typed, replace, wait, true);
		pickSuggestion(text, after);
	}

	public void comboPick(Locator input, String typed, Object text) {
		comboPick(input, typed, text, false, 600, 800);
	}

	/** Suggestions carry surrounding white space: anchor a pattern's ^ and $ around it. */
	private static Pattern visible(Object t) {
		if (t instanceof Pattern p) {
			return Pattern.compile(p.pattern().replaceAll("^\\^", "^\\\\s*").replaceAll("\\$$", "\\\\s*\\$"), p.flags());
		}
		return exact((String) t);
	}

	/** Filter boxes of a directory in order (their prompt is a placeholder). */
	public Locator filterCombo(int index) {
		return page.locator("vaadin-combo-box input").nth(index);
	}

	/** What an empty filter box shows. */
	public static String promptOf(Locator input) {
		String v = input.inputValue();
		if (v != null && !v.isEmpty()) {
			return v;
		}
		String p = input.getAttribute("placeholder");
		return p == null ? "" : p;
	}

	/** The filter box whose prompt starts with the prefix, or null. */
	public Locator comboByPrompt(String prefix) {
		Locator inputs = page.locator("vaadin-combo-box input");
		for (int i = 0; i < inputs.count(); i++) {
			if (promptOf(inputs.nth(i)).startsWith(prefix)) {
				return inputs.nth(i);
			}
		}
		return null;
	}

	public void chooseFilter(Locator input, String typed, Pattern text) {
		input.click();
		input.pressSequentially(typed, new Locator.PressSequentiallyOptions().setDelay(50));
		settle(800);
		suggestions().filter(new Locator.FilterOptions().setHasText(visible(text))).first().click();
		settle(1800);
	}

	/** Date of birth: three combo boxes with the placeholders Year, Month, Day in the open window. */
	public void chooseBirthDate(int year, String month, int day) {
		Locator win = windows();
		pickFromCombo(win.getByPlaceholder("Year").first(), String.valueOf(year));
		pickFromCombo(win.getByPlaceholder("Month").first(), month);
		pickFromCombo(win.getByPlaceholder("Day").first(), String.valueOf(day));
	}

	// ---- Options (radio buttons, check boxes) ----

	/** Clicks an option (radio button, check box) of the open window by its visible text. */
	public void chooseOption(String text) {
		windows().getByText(text, new Locator.GetByTextOptions().setExact(true)).first().click();
		settle(700);
	}

	private Locator optionBox(String text, boolean inWindow) {
		return (inWindow ? windows() : page.locator("body")).locator("vaadin-radio-button, vaadin-checkbox")
			.filter(new Locator.FilterOptions().setHasText(exact(text)))
			.first();
	}

	public void selectOption(String text, boolean inWindow) {
		optionBox(text, inWindow).locator("label").first().click();
	}

	public boolean optionChecked(String text, boolean inWindow) {
		return optionBox(text, inWindow).locator("input").isChecked();
	}

	/** A check box of the page by its label (the tooltip text is part of the element's text, so the label is matched). */
	public Locator checkbox(String text) {
		return page.locator("vaadin-checkbox")
			.filter(new Locator.FilterOptions().setHas(page.locator("label", new Page.LocatorOptions().setHasText(exact(text)))))
			.first();
	}

	public void clickCheckbox(String text) {
		page.locator("vaadin-checkbox").filter(new Locator.FilterOptions().setHasText(exact(text))).locator("label").first().click();
	}

	/** The "Yes" option of the yes/no field with that caption. */
	public Locator yesAfter(String label) {
		Locator field = page.locator("vaadin-custom-field")
			.filter(new Locator.FilterOptions().setHas(page.locator(":scope > label", new Page.LocatorOptions().setHasText(label))))
			.last();
		return field.locator("vaadin-radio-button, vaadin-checkbox")
			.filter(new Locator.FilterOptions().setHasText(Pattern.compile("^\\s*yes\\s*$", Pattern.CASE_INSENSITIVE)))
			.locator("label")
			.first();
	}

	// ---- Notifications ----

	/** Text of the first notification, or null when none shows within the timeout. */
	public String notificationText(int timeoutMs) {
		try {
			return norm(page.locator("vaadin-notification-card").first().innerText(new Locator.InnerTextOptions().setTimeout(timeoutMs)));
		} catch (TimeoutError e) {
			return null;
		}
	}

	public String notificationText() {
		return notificationText(30_000);
	}

	/** All notification texts, including the hidden live region of assistive notifications ("Entry created"). */
	public String notificationTexts() {
		List<String> all = new ArrayList<>(page.locator("vaadin-notification-card").allInnerTexts());
		all.addAll(page.locator("#sormas-assistive-notification").allTextContents());
		return norm(String.join(" ", all));
	}

	public int notificationCount() {
		return page.locator("vaadin-notification-card").count();
	}

	/**
	 * Clears notifications: the error handler's sticky "A problem has occurred" cards take the clicks over whatever is
	 * under them, so they are clicked away; others close with Escape.
	 */
	public void dismissNotifications() {
		Locator n = page.locator("vaadin-notification-card");
		Locator sticky = n.filter(
			new Locator.FilterOptions().setHasText(
				Pattern.compile("A problem has occurred|An error has occurred|Ein Problem ist aufgetreten|Ein Fehler ist aufgetreten")));
		for (int i = sticky.count() - 1; i >= 0; i--) {
			try {
				sticky.nth(i).click(new Locator.ClickOptions().setForce(true).setTimeout(2000));
			} catch (RuntimeException ignored) {
				// already gone
			}
		}
		if (n.count() > 0) {
			page.waitForTimeout(400);
		}
		if (n.count() > 0) {
			page.keyboard().press("Escape");
			page.waitForTimeout(400);
		}
		page.evaluate("() => { const r = document.getElementById('sormas-assistive-notification'); if (r) r.textContent = ''; }");
		settle(300);
	}

	// ---- Windows (vaadin-dialog) ----

	/** Open windows; a closed dialog can linger in the DOM for a moment, so only open ones count. */
	public Locator windows() {
		return page.locator("vaadin-dialog[opened]");
	}

	public int windowCount() {
		return windows().count();
	}

	/** Waits up to 5 s for every window to close. */
	public void noModal() {
		for (int i = 0; i < 10 && windowCount() > 0; i++) {
			page.waitForTimeout(500);
		}
		settle(800);
	}

	public void windowButton(String text) {
		windows().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(text).setExact(true)).first().click();
	}

	/** Closes the last window with its Close, Discard or Cancel button, else with Escape; a window that is replaced while it is clicked is looked up again. */
	public void closeWindow() {
		for (int attempt = 0; attempt < 4; attempt++) {
			if (windowCount() == 0) {
				return;
			}
			try {
				Locator x = windows().last()
					.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(Pattern.compile("^(close|discard|cancel)$", Pattern.CASE_INSENSITIVE)))
					.first();
				if (x.count() > 0) {
					x.click(new Locator.ClickOptions().setTimeout(5000));
				} else {
					page.keyboard().press("Escape");
				}
				return;
			} catch (RuntimeException e) {
				page.waitForTimeout(300);
			}
		}
	}

	/** A field of the open windows by label: the first or the last one with that label. */
	public Locator windowField(String label, boolean first) {
		Locator labelled = windows().getByLabel(label, new Locator.GetByLabelOptions().setExact(true));
		Locator f = labelled.and(page.locator(INPUT)).or(labelled.locator(INPUT));
		return first ? f.first() : f.last();
	}

	/** A field of the last window whose label contains the text or matches the pattern. */
	public Locator windowFieldLike(Object label) {
		Locator labelled = label instanceof Pattern p ? windows().last().getByLabel(p) : windows().last().getByLabel((String) label);
		return labelled.and(page.locator(INPUT)).or(labelled.locator(INPUT)).first();
	}

	public void windowChooseCombo(String label, String text) {
		Locator i = windowField(label, false);
		i.click();
		i.press("Control+a");
		i.pressSequentially(text, new Locator.PressSequentiallyOptions().setDelay(40));
		settle(700);
		suggestions().filter(new Locator.FilterOptions().setHasText(Pattern.compile("^\\s*" + esc(text) + "\\s*$", Pattern.CASE_INSENSITIVE)))
			.first()
			.click();
		settle(800);
	}

	public boolean windowHasText(String text) {
		return windows().last().getByText(text, new Locator.GetByTextOptions().setExact(true)).first().isVisible();
	}

	public int windowCountText(Pattern text) {
		return windows().last().getByText(text).count();
	}

	/** Title of the first open window (the dialog's header title). */
	public String windowTitle() {
		try {
			return (String) windows().first().evaluate("e => (e.headerTitle || '').trim()", null, new Locator.EvaluateOptions().setTimeout(5000));
		} catch (TimeoutError e) {
			return null;
		}
	}

	/** Text of each open window, starting with its title. */
	@SuppressWarnings("unchecked")
	public List<String> windowTexts() {
		return (List<String>) windows().evaluateAll("es => es.map(e => ((e.headerTitle || '') + ' ' + e.innerText).replace(/\\s+/g, ' ').trim())");
	}

	public String windowText() {
		return (String) windows().last().evaluate("e => ((e.headerTitle || '') + ' ' + e.innerText).replace(/\\s+/g, ' ').trim()");
	}

	public Locator windowWith(String text) {
		return windows().filter(new Locator.FilterOptions().setHasText(text)).last();
	}

	/** Clicks a button of the last window by its name, else a visible text. */
	public void windowClickText(Object text, boolean exact) {
		Locator.GetByRoleOptions o = new Locator.GetByRoleOptions();
		if (text instanceof Pattern p) {
			o.setName(p);
		} else {
			o.setName((String) text).setExact(exact);
		}
		Locator btn = windows().last().getByRole(AriaRole.BUTTON, o).locator("visible=true");
		if (btn.count() > 0) {
			btn.first().click();
			return;
		}
		Locator t = text instanceof Pattern p ? windows().last().getByText(p)
			: windows().last().getByText((String) text, new Locator.GetByTextOptions().setExact(exact));
		t.locator("visible=true").first().click();
	}

	public void windowClickText(Object text) {
		windowClickText(text, true);
	}

	public Locator windowCheckbox(String name) {
		return name == null
			? windows().getByRole(AriaRole.CHECKBOX)
			: windows().getByRole(AriaRole.CHECKBOX, new Locator.GetByRoleOptions().setName(name));
	}

	/** The nth visible text input of the last window. */
	public Locator windowTextInput(int index) {
		return windows().last().locator("input:not([type=checkbox]):not([type=radio]):not([type=file])").locator("visible=true").nth(index);
	}

	public Locator windowFileInput() {
		return windows().last().locator("input[type=file]");
	}

	/** The upload's own button opens the file chooser; the file uploads at once. */
	public void windowChooseFile(Path file) {
		FileChooser fc = page.waitForFileChooser(() -> windows().last().locator("vaadin-upload").getByRole(AriaRole.BUTTON).first().click());
		fc.setFiles(file);
		settle(1000);
	}

	public Locator windowFirstCombo() {
		return windows().last().locator("vaadin-combo-box input").first();
	}

	/** The combo box of the last window whose label matches. */
	public Locator comboAfterCaption(Pattern caption) {
		return windows().last()
			.locator("vaadin-combo-box")
			.filter(new Locator.FilterOptions().setHas(page.getByLabel(caption)))
			.locator("input")
			.first();
	}

	/** Whether the last row of the window's grid showing the text is selected. */
	public boolean windowRowSelected(String text) {
		return (Boolean) windows().last().locator("vaadin-grid").first().evaluate("""
			(g, t) => {
			  const txt = (n) => (n.innerText || n.textContent || '').replace(/\\s+/g, ' ').trim();
			  const rows = [...g.shadowRoot.querySelectorAll('#items tr')].filter((tr) => [...tr.children].some((td) => {
			    const s = td.querySelector('slot'); return s && s.assignedNodes({ flatten: true }).some((nd) => txt(nd).includes(t)); }));
			  const tr = rows[rows.length - 1]; if (!tr) return false;
			  return tr.hasAttribute('selected') || /selected/.test(tr.getAttribute('part') || '') || /selected/.test(tr.className || '');
			}""", text);
	}

	// ---- Grids (vaadin-grid: rows and cells in its shadow DOM, cell texts slotted) ----

	private static final String ROWS_JS = """
		(g) => {
		  const txt = (n) => (n.innerText || n.textContent || '').replace(/\\s+/g, ' ').trim();
		  const out = [];
		  for (const tr of g.shadowRoot.querySelectorAll('#items tr')) {
		    if (tr.hidden || tr.getAttribute('hidden') !== null) continue;
		    const cells = [...tr.children].map((td) => { const s = td.querySelector('slot'); return s ? s.assignedNodes({ flatten: true }).map(txt).join(' ') : ''; }).filter(Boolean);
		    if (cells.length) out.push(cells.join(' '));
		  }
		  return out;
		}""";

	public Locator grid() {
		return page.locator("vaadin-grid").first();
	}

	/** Texts of the visible rows of the first grid (cell texts joined by spaces). */
	@SuppressWarnings("unchecked")
	public List<String> rowTexts() {
		return (List<String>) grid().evaluate(ROWS_JS);
	}

	@SuppressWarnings("unchecked")
	public List<String> rowTexts(Locator grid) {
		return (List<String>) grid.evaluate(ROWS_JS);
	}

	/** Index of the first row of the first grid that contains the text, or -1. */
	public int rowIndex(String text) {
		List<String> rows = rowTexts();
		for (int i = 0; i < rows.size(); i++) {
			if (rows.get(i).contains(text)) {
				return i;
			}
		}
		return -1;
	}

	private ElementHandle element(JSHandle h) {
		ElementHandle e = h.asElement();
		if (e == null) {
			throw new AssertionError("no such element in the grid");
		}
		return e;
	}

	/** Clicks the first cell of the row with that index (opens the row's record in most directories). */
	public void clickRow(int index) {
		clickRow(grid(), index);
	}

	public void clickRow(Locator grid, int index) {
		element(grid.evaluateHandle("""
			(g, i) => {
			  let n = 0;
			  for (const tr of g.shadowRoot.querySelectorAll('#items tr')) {
			    if (tr.hidden || tr.getAttribute('hidden') !== null) continue;
			    const cells = [...tr.children].map((td) => { const s = td.querySelector('slot'); return s ? s.assignedNodes({ flatten: true }) : []; });
			    if (!cells.some(a => a.length)) continue;
			    if (n++ === i) return cells.find(a => a.length)[0];
			  }
			  return null;
			}""", index)).click();
	}

	/** Clicks the element of a row that shows exactly the text (or contains it, when not exact): a link or a button. */
	public void clickInRow(int index, String text, boolean exact) {
		element(grid().evaluateHandle("""
			(g, a) => {
			  const [i, t, ex] = a; let n = 0;
			  const txt = (e) => (e.innerText || e.textContent || '').replace(/\\s+/g, ' ').trim();
			  for (const tr of g.shadowRoot.querySelectorAll('#items tr')) {
			    if (tr.hidden || tr.getAttribute('hidden') !== null) continue;
			    const nodes = [...tr.children].flatMap((td) => { const s = td.querySelector('slot'); return s ? s.assignedNodes({ flatten: true }) : []; });
			    if (!nodes.length) continue;
			    if (n++ !== i) continue;
			    for (const nd of nodes) {
			      const all = [nd, ...nd.querySelectorAll('*')];
			      const hit = all.find(e => ex ? txt(e) === t && ![...e.children].some(c => txt(c) === t) : txt(e).includes(t) && ![...e.children].some(c => txt(c).includes(t)));
			      if (hit) return hit;
			    }
			  }
			  return null;
			}""", Arrays.asList(index, text, exact))).click();
	}

	/** Clicks a text shown in the first grid (e.g. a case id link). */
	public void clickGridText(String text) {
		grid().getByText(text, new Locator.GetByTextOptions().setExact(true)).first().click();
	}

	/** Header texts of the first grid, lower case. */
	@SuppressWarnings("unchecked")
	public List<String> gridHeaderTexts() {
		return (List<String>) grid().evaluate("""
			(g) => {
			  const txt = (n) => (n.innerText || n.textContent || '').replace(/\\s+/g, ' ').trim();
			  const out = [];
			  for (const th of g.shadowRoot.querySelectorAll('#header th')) {
			    const slot = th.querySelector('slot'); if (!slot) continue;
			    const t = slot.assignedNodes({ flatten: true }).map(txt).join(' ').trim(); if (t) out.push(t.toLowerCase());
			  }
			  return out;
			}""");
	}

	private static final String BOXES_JS = """
		(g, sel) => {
		  const out = [];
		  for (const tr of g.shadowRoot.querySelectorAll(sel)) for (const td of tr.children) {
		    const s = td.querySelector('slot'); if (!s) continue;
		    for (const n of s.assignedNodes({ flatten: true })) { if (n.localName === 'vaadin-checkbox') out.push(n); else if (n.querySelector) out.push(...n.querySelectorAll('vaadin-checkbox')); }
		  }
		  return out;
		}""";

	/** Ticks the select-all box in the first grid's header. */
	public void gridSelectAll() {
		JSHandle h = grid().evaluateHandle(BOXES_JS, "#header tr");
		Map<String, JSHandle> props = h.getProperties();
		element(props.get("0")).click();
	}

	/** Number of selection boxes in the first grid's rows. */
	public int gridSelectionBoxes() {
		JSHandle h = grid().evaluateHandle(BOXES_JS, "#items tr");
		return ((Number) h.getProperty("length").jsonValue()).intValue();
	}

	/** Clicks the edit button of the first row of the first grid. */
	public void editFirstTableRow() {
		grid().locator("vaadin-button, vaadin-icon").first().click();
	}

	public int tableRowCount() {
		return rowTexts().size();
	}

	// ---- Dashboards ----

	public boolean mapPresent() {
		return page.locator(".map-component, .leaflet-container, [class*=leaflet]").count() > 0;
	}

	/** The disease tiles' numbers (large white texts). */
	public List<String> tileTexts() {
		return page.locator(".white.xlarge").allInnerTexts();
	}

	/** The selected disease tab of the disease details: the one shown larger (16 px). */
	@SuppressWarnings("unchecked")
	public List<String> selectedDiseaseTab() {
		return (List<String>) page.locator("a.v-caption").evaluateAll("""
			es => es.filter(e => e.innerText && e.innerText.length < 20 && (getComputedStyle(e).fontSize === '16px'
			  || getComputedStyle(e.querySelector('span') || e).fontSize === '16px')).map(e => e.innerText.trim())""");
	}

	// ---- 3-02 batch 1: login attempts, status counts, grid columns and sorting, line listing ----

	public boolean loginFormVisible() {
		return page.locator("vaadin-login-form input[name='username']").isVisible() && page.locator("vaadin-login-form input[name='password']").isVisible();
	}

	/** Fills and submits the login form that is already open (no navigation). */
	public void loginAttempt(String user, String password) {
		page.locator("vaadin-login-form input[name='username']").fill(user);
		page.locator("vaadin-login-form input[name='password']").fill(password);
		page.locator("vaadin-login-form vaadin-button[slot='submit']").click();
	}

	/** The number on a status badge button ("All 84", the badge is not part of the accessible name). */
	public int statusCount(String label) {
		String t = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("^" + esc(label))))
			.filter(new Locator.FilterOptions().setHas(page.locator(".badge")))
			.first()
			.innerText();
		java.util.regex.Matcher m = Pattern.compile("\\d+").matcher(t);
		return m.find() ? Integer.parseInt(m.group()) : -1;
	}

	/** Texts of the column whose header matches the regular expression (JavaScript syntax, case-blind), one per row. */
	@SuppressWarnings("unchecked")
	public List<String> gridColumnTexts(String headerRegex) {
		return (List<String>) grid().evaluate("""
			(g, src) => {
			  const re = new RegExp(src, 'i');
			  const txt = (n) => (n.innerText || n.textContent || '').replace(/\\s+/g, ' ').trim();
			  const cellText = (td) => { const s = td.querySelector('slot'); return s ? s.assignedNodes({ flatten: true }).map(txt).join(' ').trim() : ''; };
			  const ths = [...g.shadowRoot.querySelectorAll('#header tr:last-child th')];
			  const idx = ths.findIndex(th => re.test(cellText(th)));
			  const out = [];
			  for (const tr of g.shadowRoot.querySelectorAll('#items tr')) {
			    if (tr.hidden || tr.getAttribute('hidden') !== null) continue;
			    const tds = [...tr.children]; if (!tds.some(td => cellText(td))) continue;
			    out.push(cellText(tds[idx]));
			  }
			  return out;
			}""", headerRegex);
	}

	/** Clicks the sorter of the column whose caption matches (JavaScript regular expression, case-blind). */
	public void gridSortBy(String headerRegex) {
		Locator sorters = grid().locator("vaadin-grid-sorter");
		@SuppressWarnings("unchecked")
		List<String> texts = (List<String>) sorters.evaluateAll("es => es.map(e => e.textContent.replace(/\\s+/g, ' ').trim())");
		Pattern re = Pattern.compile(headerRegex, Pattern.CASE_INSENSITIVE);
		for (int i = 0; i < texts.size(); i++) {
			if (re.matcher(texts.get(i)).find()) {
				sorters.nth(i).click();
				return;
			}
		}
		throw new AssertionError("no sorter " + headerRegex + " in " + texts);
	}

	public void gridScrollEnd() {
		grid().evaluate("g => g.scrollToIndex(1e9)");
	}

	public void gridScrollTop() {
		grid().evaluate("g => g.scrollToIndex(0)");
	}

	public int buttonCount(Pattern name) {
		return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name)).count();
	}

	public void clickButton(Pattern name) {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name)).first().click();
	}

	/** The value of a date input as M/D/YY (older captures show two-digit years, this app four): used to compare either. */
	public static String dateValue(Locator input) {
		return input.inputValue().replaceAll("/(\\d{2})(\\d{2})$", "/$2");
	}

	/** M/D/YYYY as M/D/YY. */
	public static String shortYear(String date) {
		return date.replaceAll("/(\\d{2})(\\d{2})$", "/$2");
	}

	public Locator windowLastDate() {
		return windows().last().locator("vaadin-date-picker input").last();
	}

	public void windowTickFirst() {
		windows().last().locator("vaadin-checkbox").first().click();
	}

	/** Like comboPick, but the suggestion must be visible (a hidden stale item can match). */
	public void comboPickVisible(Locator input, String typed, String text, int wait) {
		typeInCombo(input, typed, false, wait, true);
		suggestions().filter(new Locator.FilterOptions().setHasText(exact(text))).first().click();
		settle(800);
	}

	private static final java.util.Set<String> LL_SHARED = java.util.Set.of("Disease", "Responsible district", "Facility category", "Facility type");
	private static final Map<String, String> LL_LINE_IDS = Map.of(
		"Date of report", "[id^=\"lineListingDateOfReport_\"]",
		"EPID number", "[id^=\"lineListingEpidNumber_\"]",
		"Responsible community", "[id^=\"lineListingCommunity_\"]",
		"Facility", "[id^=\"lineListingFacility_\"]",
		"First name", "#firstName",
		"Last name", "#lastName",
		"Sex", "#sex",
		"Date of symptom onset", "[id^=\"lineListingDateOfOnSet_\"]");
	private static final String LL_INPUT = "input:not([type=radio]):not([type=checkbox])";

	/** One row (line) of the line listing window: the layout holding the line's delete button. */
	public Locator lineListingRows() {
		return windows().last().locator("vaadin-horizontal-layout:has(> vaadin-button[id^=\"delete_\"])");
	}

	/** An input of the line listing window by caption and line (0 = the first line, which has the captions). */
	public Locator lineListingBox(String label, int k) {
		if (k > 0 && !LL_SHARED.contains(label) && LL_LINE_IDS.containsKey(label)) {
			return lineListingRows().nth(k).locator(LL_LINE_IDS.get(label)).first().locator(LL_INPUT).first();
		}
		Locator labelled = windows().getByLabel(label, new Locator.GetByLabelOptions().setExact(true));
		Locator inputs = labelled.and(page.locator(LL_INPUT)).or(labelled.locator(LL_INPUT));
		return inputs.nth(LL_SHARED.contains(label) ? 0 : k);
	}

	public Locator lineListingBox(String label) {
		return lineListingBox(label, 0);
	}

	public int lineListingLines() {
		int n = lineListingRows().count();
		return n > 0 ? n : windows().getByLabel("First name", new Locator.GetByLabelOptions().setExact(true)).count();
	}

	// ---- 3-02 batch 2: actions, groups, outbreak matrix, option groups, table rows ----

	/** Opens a list entry (action, task) by its title: the pencil/first control in the row holding that text. */
	public void openActionEntry(String title) {
		Locator row = page.locator("vaadin-horizontal-layout")
			.filter(new Locator.FilterOptions().setHas(page.getByText(title, new Page.GetByTextOptions().setExact(true))))
			.last();
		row.locator("vaadin-button, vaadin-icon, .v-slot").first().click();
	}

	/** The list icon of the first event group entry in the event's group panel (opens the group's events). */
	public void openEventGroupEvents() {
		page.locator("vaadin-vertical-layout").filter(new Locator.FilterOptions().setHasText(Pattern.compile("Event groups", Pattern.CASE_INSENSITIVE)))
			.last()
			.locator("vaadin-button")
			.nth(3)
			.click();
	}

	private Locator outbreakButtons() {
		return page.locator("vaadin-grid .v-button").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^(\\d+|\\d+/\\d+)$")));
	}

	/** The outbreak matrix button in the column of that disease (the nearest one horizontally to its header). */
	public Locator outbreakCell(String disease) {
		Locator h = page.getByText(Pattern.compile("^" + disease + "$", Pattern.CASE_INSENSITIVE)).first();
		var hb = h.boundingBox();
		Locator bs = outbreakButtons();
		int n = bs.count();
		Locator best = null;
		double bd = 1e9;
		for (int i = 0; i < n; i++) {
			var b = bs.nth(i).boundingBox();
			if (b == null) {
				continue;
			}
			double d = Math.abs(b.x + b.width / 2 - (hb.x + hb.width / 2));
			if (d < bd) {
				bd = d;
				best = bs.nth(i);
			}
		}
		return best;
	}

	public List<String> outbreakCellTexts() {
		return outbreakButtons().allInnerTexts();
	}

	/** Upper-case texts of all links (the tabs of a case, ...). */
	public List<String> tabTexts() {
		return page.getByRole(AriaRole.LINK).allInnerTexts().stream().map(t -> norm(t).toUpperCase()).filter(t -> !t.isEmpty()).toList();
	}

	private Locator group(String name) {
		return page.getByRole(AriaRole.GROUP, new Page.GetByRoleOptions().setName(name).setExact(true)).first();
	}

	/** The ticked option's text of a radio or check box group by its caption, or null. */
	public String optionGroupValue(String name) {
		return (String) group(name).locator("vaadin-radio-button, vaadin-checkbox")
			.evaluateAll("es => { const e = es.find(x => x.checked); return e ? (e.textContent || '').replace(/\\s+/g, ' ').trim() : null; }");
	}

	public void optionGroupClick(String name, String value) {
		group(name).locator("vaadin-radio-button, vaadin-checkbox").filter(new Locator.FilterOptions().setHasText(exact(value))).locator("label").first().click();
	}

	public void optionGroupReveal(String name) {
		group(name).scrollIntoViewIfNeeded();
	}

	/** Sets an option group to a value: clicking the ticked option again clears it, so click only when it is not ticked. */
	public void setOptionGroup(String name, String value) {
		for (int i = 0; i < 3 && !value.equals(optionGroupValue(name)); i++) {
			optionGroupReveal(name);
			optionGroupClick(name, value);
			settle(300);
		}
	}

	private ElementHandle gridCell(Locator grid, Object pick) {
		return element(grid.evaluateHandle("""
			(g, a) => {
			  const txt = (n) => (n.innerText || n.textContent || '').replace(/\\s+/g, ' ').trim();
			  let n = 0;
			  for (const tr of g.shadowRoot.querySelectorAll('#items tr')) {
			    if (tr.hidden || tr.getAttribute('hidden') !== null) continue;
			    const cells = [...tr.children].map((td) => { const s = td.querySelector('slot'); return s ? s.assignedNodes({ flatten: true }) : []; });
			    if (!cells.some(c => c.length)) continue;
			    const all = cells.map(c => c.map(txt).join(' ')).join(' ');
			    if (typeof a === 'string' ? all.includes(a) : n++ === a) return cells.find(c => c.length)[0];
			  }
			  return null;
			}""", pick));
	}

	/** Clicks the edit button (else the first cell) of the i-th row of the first grid. */
	public void editTableRowAt(int i) {
		ElementHandle el = gridCell(grid(), i);
		ElementHandle b = el.querySelector("vaadin-button");
		(b != null ? b : el).click();
	}

	/** Clicks the first cell of the first row of the first grid that shows the text (the row is found by its text, not by position). */
	public void clickRowWith(String text) {
		gridCell(grid(), text).click();
	}

	/** Clicks the first cell of the i-th row of the first grid of the last window. */
	public void windowGridClickRow(int i) {
		gridCell(windows().last().locator("vaadin-grid").first(), i).click();
	}

	/** Clicks the button (else the cell) of the first row of any grid on the page that shows the text. */
	public void tableRowClick(String text) {
		Locator grids = page.locator("vaadin-grid");
		for (int k = 0; k < grids.count(); k++) {
			ElementHandle el;
			try {
				el = gridCell(grids.nth(k), text);
			} catch (AssertionError e) {
				continue;
			}
			el.scrollIntoViewIfNeeded();
			ElementHandle b = el.querySelector("vaadin-button");
			(b != null ? b : el).click();
			return;
		}
		throw new AssertionError("no table row: " + text);
	}

	public void windowButtonMatching(Pattern name) {
		windows().last().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name)).first().click();
	}

	public void windowButtonLast(String text) {
		windows().last().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(text).setExact(true)).last().click();
	}

	/** The text of the first row of the first grid that shows the text. */
	public String rowTextWith(String text) {
		return rowTexts().stream().filter(r -> r.contains(text)).findFirst().orElse("");
	}

	// ---- 3-02 batch 3: fields of the last window by position, line dates ----

	private Locator windowInputsLike(String label) {
		Locator labelled = windows().last().getByLabel(label);
		return labelled.and(page.locator(INPUT)).or(labelled.locator(INPUT));
	}

	/** The i-th input of the last window whose label contains the text (fields that repeat per line). */
	public Locator windowFieldLikeAt(String label, int i) {
		return windowInputsLike(label).nth(i);
	}

	public int windowFieldCount(String label) {
		return windowInputsLike(label).count();
	}

	/** A date input of the i-th line of a contact line listing: 0 = date of report, 1 = date of last contact. */
	public Locator lineDateInput(int line, int which) {
		Locator labelled = windows().last().getByLabel(which == 0 ? "Date of report" : "Date of last contact", new Locator.GetByLabelOptions().setExact(true)).nth(line);
		return labelled.and(page.locator(INPUT)).or(labelled.locator(INPUT)).first();
	}

	// ---- 3-02 batch 4: ids, aggregate boxes, tab by pattern ----

	/** Clicks the element with that id (the app's own ids of buttons and links without a better handle). */
	public void clickId(String id) {
		page.locator("[id=\"" + id + "\"]").first().click();
	}

	/** Values of all combo boxes of the open windows. */
	@SuppressWarnings("unchecked")
	public List<String> windowComboValues() {
		return (List<String>) windows().locator("vaadin-combo-box input").evaluateAll("es => es.map(e => e.value)");
	}

	/** A sub-menu tab (link) whose name matches the pattern. */
	public Locator tab(Pattern label) {
		return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(label)).first();
	}

	/** Like clickEntry, but the name is matched as a prefix: a button's name carries its tooltip text once shown ("New entry New entry"). */
	public void clickEntryPrefix(String text) {
		clickEntry(Pattern.compile("^" + esc(text)));
	}

	/** The n-th (1-based) input after the text of a disease row in the open window (a row is a label followed by boxes). */
	public ElementHandle aggregateBox(String disease, int n) {
		return element(windows().last().evaluateHandle("""
			(w, a) => {
			  const [d, n] = a;
			  const tw = document.createTreeWalker(w, NodeFilter.SHOW_TEXT);
			  let label = null;
			  while (tw.nextNode()) { if (tw.currentNode.textContent.trim() === d) { label = tw.currentNode.parentElement; break; } }
			  if (!label) return null;
			  const inputs = [...w.querySelectorAll('input')].filter(i => label.compareDocumentPosition(i) & Node.DOCUMENT_POSITION_FOLLOWING);
			  return inputs[n - 1] || null;
			}""", Arrays.asList(disease, n)));
	}

	// ---- 3-02 batch 5: dashboard period and disease filters (ported from _dash-flow.cjs) ----

	/** The combo box whose current value or placeholder is that text. */
	public Locator comboByValue(String value) {
		Locator f = page.locator("vaadin-combo-box input");
		for (int k = 0; k < f.count(); k++) {
			Locator i = f.nth(k);
			if (value.equals(i.inputValue()) || value.equals(i.getAttribute("placeholder"))) {
				return i;
			}
		}
		throw new AssertionError("no combo box " + value);
	}

	private static String mdy(java.time.LocalDate d) {
		return d.getMonthValue() + "/" + d.getDayOfMonth() + "/" + String.format("%02d", d.getYear() % 100);
	}

	/** Sets the dashboard period to the last {@code back} days up to {@code reset}: custom period by date, applied. */
	public void setLastDays(java.time.LocalDate reset, int back) {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
			.setName(Pattern.compile("This epi week|Today|Yesterday|Last epi week|This year|^\\d+\\/\\d+\\/\\d+ - "))).first().click();
		settle(400);
		button("Custom").click();
		settle(400);
		Locator mode = comboByValue("By Epi Week");
		mode.click();
		mode.press("ArrowDown");
		settle(300);
		suggestions().filter(new Locator.FilterOptions().setHasText(Pattern.compile("^\\s*By Date\\s*$"))).first().click();
		settle(400);
		Locator dp = page.locator("vaadin-date-picker input").locator("visible=true");
		Locator from = dp.nth(0);
		Locator to = dp.nth(1);
		from.fill(mdy(reset.minusDays(back)));
		from.press("Enter");
		settle(300);
		to.fill(mdy(reset));
		to.press("Enter");
		settle(300);
		button("Apply custom filter").click();
		settle(1500);
		page.getByText(Pattern.compile("Dashboard$")).locator("visible=true").first().click();
		settle(400);
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("^apply filters$", Pattern.CASE_INSENSITIVE)))
			.click(new Locator.ClickOptions().setForce(true));
		settle(2000);
	}

	/** Chooses the disease filter of a dashboard and applies the filters. */
	public void pickDisease(String name) {
		Locator d = comboByValue("Disease");
		d.click();
		d.press("Control+a");
		d.pressSequentially(name.substring(0, Math.min(5, name.length())), new Locator.PressSequentiallyOptions().setDelay(40));
		settle(400);
		suggestions().filter(new Locator.FilterOptions().setHasText(Pattern.compile("^\\s*" + esc(name) + "\\s*$"))).first().click();
		settle(500);
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("^apply filters$", Pattern.CASE_INSENSITIVE)))
			.click(new Locator.ClickOptions().setForce(true));
		settle(2000);
	}

	/** First number (group 1) of the regular expression in the page text with white space collapsed, or -1. */
	public int numberIn(String regex) {
		java.util.regex.Matcher m = Pattern.compile(regex).matcher(pageText().replaceAll("\\s+", " "));
		return m.find() ? Integer.parseInt(m.group(1)) : -1;
	}

	// ---- 3-02 batch 6: dashboard switch, statistics, charts, tokens, check boxes by label ----

	/** Switches a dashboard (surveillance, contacts, samples) with its radio button. */
	public void dashboardSwitch(Pattern text) {
		page.locator("vaadin-radio-button").filter(new Locator.FilterOptions().setHasText(visible(text))).locator("label").first().click();
	}

	public List<String> dashboardSwitchTexts() {
		return page.locator("vaadin-radio-group").filter(new Locator.FilterOptions().setHasText(Pattern.compile("surveillance", Pattern.CASE_INSENSITIVE)))
			.first().locator("vaadin-radio-button").allInnerTexts();
	}

	/** An option of the statistics page (table, chart, map, districts ...) by its exact text: its label. */
	public Locator statsOption(String name) {
		return page.locator("vaadin-radio-button").filter(new Locator.FilterOptions().setHasText(exact(name))).locator("label").first();
	}

	public void clickRoleButton(String name) {
		button(name).click();
	}

	/** Opens a menu bar by its caption and chooses the visible item matching the pattern (matched on text content and size). */
	public void chooseMenuBarMatching(String menu, Pattern item) {
		page.locator("vaadin-menu-bar-button").filter(new Locator.FilterOptions().setHasText(menu)).first().click();
		settle(400);
		Locator items = page.locator("vaadin-menu-bar-item");
		@SuppressWarnings("unchecked")
		List<Number> hits = (List<Number>) items.evaluateAll("""
			(es, src) => { const re = new RegExp(src);
			  return es.map((e, i) => [i, re.test((e.textContent || '').replace(/\\s+/g, ' ').trim()), e.getClientRects().length > 0]).filter(x => x[1] && x[2]).map(x => x[0]); }""",
			item.pattern());
		if (hits.isEmpty()) {
			throw new AssertionError("no menu bar item " + item + " in " + menu);
		}
		items.nth(hits.get(0).intValue()).click();
	}

	/** Texts (titles) of the chips of the multi-value field. */
	@SuppressWarnings("unchecked")
	public List<String> tokenTexts() {
		return (List<String>) page.locator("vaadin-multi-select-combo-box-chip[slot=chip]")
			.evaluateAll("es => es.map(e => (e.getAttribute('title') || '').trim())");
	}

	public void removeToken(String name) {
		page.locator("vaadin-multi-select-combo-box-chip[slot=chip]").and(page.locator("[title=\"" + name + "\"]")).locator("[part=\"remove-button\"]").click();
	}

	/** Chooses a value of the multi-value field by its exact text (the typed text does not narrow the list: it is searched). */
	public void pickToken(String name) {
		page.locator("vaadin-multi-select-combo-box input").first().click();
		settle(400);
		Locator items = page.locator("vaadin-multi-select-combo-box-item");
		@SuppressWarnings("unchecked")
		List<String> texts = (List<String>) items.evaluateAll("es => es.map(e => e.textContent.trim())");
		int idx = texts.indexOf(name);
		if (idx < 0) {
			throw new AssertionError("no value " + name + " in " + texts);
		}
		items.nth(idx).click(new Locator.ClickOptions().setForce(true));
		settle(500);
		page.keyboard().press("Escape");
		settle(200);
	}

	public boolean chartDrawn() {
		return page.locator(".highcharts-container").count() > 0;
	}

	public List<String> chartDataLabels() {
		@SuppressWarnings("unchecked")
		List<String> l = (List<String>) page.locator(".highcharts-data-label").evaluateAll("es => es.map(e => e.textContent.trim())");
		return l.stream().filter(s -> !s.isEmpty()).toList();
	}

	@SuppressWarnings("unchecked")
	public List<String> chartLegendTexts() {
		return (List<String>) page.locator(".highcharts-legend-item").evaluateAll("es => es.map(e => (e.textContent || '').trim())");
	}

	public boolean statsMapDrawn() {
		return page.locator(".sormas-map-zoom, .leaflet-container").count() > 0;
	}

	/** The check box input of the page or the last window by its label text. */
	public Locator checkboxInput(String text, boolean inWindow) {
		return (inWindow ? windows().last() : page.locator("body")).locator("vaadin-checkbox")
			.filter(new Locator.FilterOptions().setHas(page.locator("label", new Page.LocatorOptions().setHasText(exact(text)))))
			.first()
			.locator("input[type=checkbox]");
	}

	public int checkedCount(boolean inWindow) {
		return (inWindow ? windows().last() : page.locator("body")).locator("input[type=checkbox]:checked").count();
	}

	public void windowGridButton(int n) {
		windows().last().locator("vaadin-grid").first().locator("vaadin-button").nth(n).click();
	}

	/** First column of the rows of the first grid of the last window (empty ones left out). */
	@SuppressWarnings("unchecked")
	public List<String> windowGridFirstColumn() {
		return (List<String>) windows().last().locator("vaadin-grid").first().evaluate("""
			(g) => {
			  const txt = (n) => (n.innerText || n.textContent || '').replace(/\\s+/g, ' ').trim(); const out = [];
			  for (const tr of g.shadowRoot.querySelectorAll('#items tr')) { if (tr.hidden) continue; const td = tr.children[0]; const s = td && td.querySelector('slot'); out.push(s ? s.assignedNodes({ flatten: true }).map(txt).join(' ') : ''); }
			  return out.filter(Boolean);
			}""");
	}

	// ---- 3-02 batch 7: grids by position, yes option of a window group ----

	/** Texts of the rows of the n-th grid of the page. */
	public List<String> rowTexts(int n) {
		return rowTexts(page.locator("vaadin-grid").nth(n));
	}

	/** The "Yes" option of the option group with that caption in the last window. */
	public Locator windowYesAfter(String label) {
		return windows().last().getByRole(AriaRole.GROUP, new Locator.GetByRoleOptions().setName(label).setExact(true)).first()
			.locator("vaadin-radio-button, vaadin-checkbox")
			.filter(new Locator.FilterOptions().setHasText(Pattern.compile("^\\s*yes\\s*$", Pattern.CASE_INSENSITIVE)))
			.locator("label")
			.first();
	}

	// ---- 3-02 batch 8: check boxes by label, window buttons ----

	private Locator checkboxHost(String text) {
		return page.locator("vaadin-checkbox").filter(new Locator.FilterOptions().setHas(page.locator("label", new Page.LocatorOptions().setHasText(exact(text))))).first();
	}

	public boolean checkboxIsChecked(String text) {
		return checkboxHost(text).locator("input").isChecked();
	}

	public void checkboxReveal(String text) {
		checkboxHost(text).scrollIntoViewIfNeeded();
	}

	public void checkboxToggle(String text) {
		checkboxHost(text).locator("label").first().click();
	}

	/** Sets a check box (by its label) to on or off, clicking only when it differs. */
	public void setCheckbox(String text, boolean on) {
		checkboxReveal(text);
		if (checkboxIsChecked(text) != on) {
			checkboxToggle(text);
			settle(300);
		}
	}

	public int checkboxCount() {
		return page.locator("vaadin-checkbox").count();
	}

	public List<String> windowButtonTexts() {
		return windows().last().getByRole(AriaRole.BUTTON).allInnerTexts().stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
	}

	// ---- 3-02 batch 9: configuration pages ----

	public Locator lastCombo() {
		return page.locator("vaadin-combo-box input").last();
	}

	/** Types into a search box by id and presses Enter (configuration lists search on Enter). */
	public void searchEnter(String id, String value) {
		Locator s = page.locator("[id=\"" + id + "\"] input, input[id=\"" + id + "\"]").first();
		s.fill(value);
		s.press("Enter");
	}

	@SuppressWarnings("unchecked")
	public List<String> checkboxStates() {
		return (List<String>) page.locator("vaadin-checkbox")
			.evaluateAll("es => es.map(e => (e.innerText || e.textContent || '').trim() + ' = ' + (e.checked ? 'on' : 'off'))");
	}

	public List<String> windowCaptionTexts() {
		return windows().last().locator("label").allInnerTexts();
	}

	/** Waits until the first grid shows no row (or none left) containing the text, or the time is up; returns whether it is so. */
	public boolean waitNoRow(String text, int ms) {
		long end = System.currentTimeMillis() + ms;
		do {
			if (rowTexts().stream().noneMatch(r -> r.contains(text))) {
				return true;
			}
			page.waitForTimeout(250);
		} while (System.currentTimeMillis() < end);
		return false;
	}

	/** Waits until the first grid shows a row containing the text, or the time is up; returns whether it does. */
	public boolean waitRow(String text, int ms) {
		long end = System.currentTimeMillis() + ms;
		do {
			if (rowTexts().stream().anyMatch(r -> r.contains(text))) {
				return true;
			}
			page.waitForTimeout(250);
		} while (System.currentTimeMillis() < end);
		return false;
	}

	// ---- 3-02 batch 10: C2 (German) helpers, id fields, combo toggle, tree grid ----

	/** The input of the element with that id (a field the app names itself), in the page or the open windows. */
	public Locator fieldById(String id, boolean inWindow) {
		Locator host = (inWindow ? windows() : page.locator("body")).locator("[id=\"" + id + "\"]");
		return host.and(page.locator("input, textarea")).or(host.locator("input, textarea")).first();
	}

	/** Opens the combo box's list with its toggle button. */
	public void comboToggleOpen(Locator input) {
		input.evaluate("e => { const c = e.closest('vaadin-combo-box'); const t = c.querySelector('[part~=toggle-button]') || c.shadowRoot.querySelector('[part~=toggle-button]'); t.click(); }");
		settle(400);
	}

	/** Opens the combo box and picks the entry whose text starts with the given text. */
	public void comboToggleChooseStart(Locator input, String prefix) {
		comboToggleOpen(input);
		pickSuggestion(Pattern.compile("^" + esc(prefix)), 500);
	}

	public void comboToggleChoose(Locator input, String text) {
		comboToggleOpen(input);
		pickSuggestion(text, 500);
	}

	public void lastComboToggleChoose(String text) {
		comboToggleOpen(page.locator("vaadin-combo-box").locator("visible=true").last().locator("input"));
		pickSuggestion(text, 600);
	}

	public boolean checkboxChecked(String label) {
		return page.locator("vaadin-checkbox").filter(new Locator.FilterOptions().setHasText(exact(label))).locator("input").isChecked();
	}

	public boolean gridHasText(String text) {
		return rowTexts().stream().anyMatch(r -> r.contains(text));
	}

	/** Date of birth in English or German: the combo boxes with the placeholders Year/Jahr, Month/Monat, Day/Tag in the open window. */
	public void chooseBirthDateLike(int year, String month, int day) {
		Locator win = windows();
		pickFromCombo(win.getByPlaceholder(Pattern.compile("^(Year|Jahr)$")).first(), String.valueOf(year));
		pickFromCombo(win.getByPlaceholder(Pattern.compile("^(Month|Monat)$")).first(), month);
		pickFromCombo(win.getByPlaceholder(Pattern.compile("^(Day|Tag)$")).first(), String.valueOf(day));
	}

	public void windowButtonLike(Pattern name) {
		windows().last().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name)).click();
	}

	public void pageButtonLike(Pattern name) {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name)).locator("visible=true").first().click();
	}

	public int chartCount() {
		return page.locator(".highcharts-container, canvas, svg.highcharts-root").count();
	}

	/** Clicks a button (by name pattern) inside the first grid row that shows the text (tree grids: merge, ...). */
	public void treeGridRowButton(String rowText, Pattern name) {
		element(grid().evaluateHandle("""
			(g, a) => {
			  const re = new RegExp(a[1], 'i'); const txt = (n) => (n.innerText || n.textContent || '').replace(/\\s+/g, ' ').trim();
			  for (const tr of g.shadowRoot.querySelectorAll('#items tr')) {
			    const nodes = [...tr.children].flatMap((td) => { const s = td.querySelector('slot'); return s ? s.assignedNodes({ flatten: true }) : []; });
			    if (!nodes.map(txt).join(' ').includes(a[0])) continue;
			    for (const n of nodes) for (const b of (n.localName === 'vaadin-button' ? [n] : [...n.querySelectorAll('vaadin-button')])) if (re.test(txt(b) || b.getAttribute('aria-label') || '')) return b;
			  }
			  return null;
			}""", Arrays.asList(rowText, name.pattern()))).click();
	}

	/** The newest notification text: the first open card this page has not been given before, else the last card. */
	private final java.util.Set<String> seenNotifications = new java.util.HashSet<>();

	public String newestNotificationText(int timeoutMs) {
		long end = System.currentTimeMillis() + timeoutMs;
		do {
			for (String x : page.locator("vaadin-notification-card").allInnerTexts()) {
				String n = norm(x);
				if (!n.isEmpty() && seenNotifications.add(n)) {
					return n;
				}
			}
			page.waitForTimeout(250);
		} while (System.currentTimeMillis() < end);
		List<String> all = page.locator("vaadin-notification-card").allInnerTexts();
		return all.isEmpty() ? null : norm(all.get(all.size() - 1));
	}

	/** Logs in on a C2 copy: the GDPR notice that follows the first login of a user is closed without consenting. */
	public void loginDe(String user) {
		login(user);
		settle(800);
		if (windowCount() > 0) {
			closeWindow();
			noModal();
		}
		// the notice is modal: a banner of a CI build could not be closed while it was open
		closeCommercialBanner();
	}

	// ---- 3-02 batch 11: option groups by caption, period pop-up of the C2 dashboards, buttons ----

	/**
	 * Clicks the option (radio button or check box, case-blind exact text) of the option group that follows the caption
	 * matching the pattern: the caption is the label of a custom field around the group, or a text before the group.
	 */
	public void optionAfterCaption(Pattern caption, String value) {
		ElementHandle cap = page.getByText(caption).locator("visible=true").first().elementHandle();
		ElementHandle label = element(cap.evaluateHandle("""
			(c, v) => {
			  const OPTS = 'vaadin-radio-button, vaadin-checkbox';
			  const a = c.closest('vaadin-custom-field'), b = c.closest('vaadin-radio-group');
			  let g = a && b ? (a.contains(b) ? a : b) : (a || b);
			  if (g && !g.querySelector(OPTS)) g = null;
			  if (!g) {
			    g = [...document.querySelectorAll('vaadin-radio-group, vaadin-checkbox-group')].find(x => c.compareDocumentPosition(x) & Node.DOCUMENT_POSITION_FOLLOWING);
			  }
			  const re = new RegExp('^\\\\s*' + v.replace(/[.*+?^${}()|[\\]\\\\]/g, '\\\\$&') + '\\\\s*$', 'i');
			  const o = [...g.querySelectorAll(OPTS)].find(x => re.test((x.textContent || '')));
			  return o ? (o.querySelector('label') || o) : null;
			}""", value));
		label.click();
		settle(400);
	}

	/** Chooses an entry of the period-type combo box in the dashboard's period pop-up ("Nach Datum"): its value starts with "Nach ". */
	public void popupComboChoose(String text) {
		Locator inputs = page.locator("vaadin-combo-box input");
		for (int i = 0; i < inputs.count(); i++) {
			if (inputs.nth(i).inputValue().startsWith("Nach ")) {
				comboToggleChoose(inputs.nth(i), text);
				return;
			}
		}
		throw new AssertionError("no period type combo box");
	}

	public Locator popupDateInput(int index) {
		return page.locator("vaadin-date-picker input").filter(new Locator.FilterOptions().setVisible(true)).nth(index);
	}

	public List<String> buttonTexts(Pattern name) {
		return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name)).allInnerTexts();
	}

	public boolean buttonEnabled(Pattern name) {
		return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name)).first().isEnabled();
	}

	// ---- 3-02 batch 12: uploads, document rows, row buttons, imports ----

	/** The upload's own button reveals the hidden file input of the page; the file goes to it. */
	public void pageUploadFile(Pattern button, Path file) {
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(button)).first().click();
		settle(500);
		page.locator("input[type=file]").last().setInputFiles(file);
	}

	/** A button of the document list row with that file name: the first (download) or the last (delete). */
	public void documentRowButton(String name, boolean first) {
		Locator row = page.locator("vaadin-horizontal-layout").filter(new Locator.FilterOptions().setHas(page.getByText(name, new Page.GetByTextOptions().setExact(true)))).last();
		Locator b = row.getByRole(AriaRole.BUTTON);
		(first ? b.first() : b.last()).click();
	}

	/** A button in the i-th row of the first grid by its text (a button's text holds its tooltip too: start of text, case-blind). */
	public void clickRowButton(int row, String text) {
		element(grid().evaluateHandle("""
			(g, a) => {
			  const [i, t] = a; let n = 0;
			  for (const tr of g.shadowRoot.querySelectorAll('#items tr')) {
			    if (tr.hidden || tr.getAttribute('hidden') !== null) continue;
			    const nodes = [...tr.children].flatMap((td) => { const s = td.querySelector('slot'); return s ? s.assignedNodes({ flatten: true }) : []; });
			    if (!nodes.length) continue;
			    if (n++ !== i) continue;
			    for (const nd of nodes) for (const b of nd.querySelectorAll ? nd.querySelectorAll('vaadin-button, button, a') : []) if ((b.firstChild && b.firstChild.textContent || b.textContent).trim().toLowerCase() === t.toLowerCase()) return b;
			  }
			  return null;
			}""", Arrays.asList(row, text))).click();
	}

	/** The last visible button, link or menu item with that exact name. */
	public void clickLastEntry(String text) {
		for (AriaRole r : new AriaRole[] {
			AriaRole.BUTTON,
			AriaRole.LINK,
			AriaRole.MENUITEM }) {
			Locator l = page.getByRole(r, new Page.GetByRoleOptions().setName(text).setExact(true)).locator("visible=true");
			if (l.count() > 0) {
				l.last().click();
				return;
			}
		}
		throw new AssertionError("no entry: " + text);
	}

	/** The last open window whose text matches the pattern. */
	public Locator windowWith(Pattern text) {
		return windows().filter(new Locator.FilterOptions().setHasText(text)).last();
	}

	/**
	 * Waits for the data import window to report its result ("verarbeitet" ... "Import erfolgreich" or an error) and
	 * returns the window text, white space collapsed.
	 */
	public String importResult(int ms) {
		Locator res = windowWith(Pattern.compile("verarbeitet"));
		try {
			res.getByText(Pattern.compile("Import erfolgreich|Fehler beim Import|nicht importiert")).first().waitFor(new Locator.WaitForOptions().setTimeout(ms));
		} catch (RuntimeException ignored) {
			// the text read below decides
		}
		settle(500);
		return norm(res.innerText());
	}

	/** Number of open windows once they have had up to 5 s to close: for "the window closed" checks. */
	public int windowsAfterClose() {
		for (int i = 0; i < 20 && windowCount() > 0; i++) {
			page.waitForTimeout(250);
		}
		return windowCount();
	}
}
