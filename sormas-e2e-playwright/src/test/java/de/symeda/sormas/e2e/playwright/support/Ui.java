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
		page.waitForTimeout(ms);
		try {
			page.waitForFunction(
				"() => { const c = window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients; return !c || !Object.values(c).some(x => x && x.isActive && x.isActive()); }",
				null,
				new Page.WaitForFunctionOptions().setTimeout(10_000).setPollingInterval(100));
		} catch (TimeoutError ignored) {
			// a long request: the next step's own waits decide
		}
	}

	public void settle() {
		settle(800);
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
		for (Locator l : entries(text)) {
			if (l.count() > nth) {
				l.nth(nth).click();
				return;
			}
		}
		throw new AssertionError("no button, link, menu item or tab: " + text);
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

	private Locator suggestions() {
		return page.locator("vaadin-combo-box-item");
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

	public String suggestionsText() {
		return String.join(" ", suggestions().allInnerTexts());
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

	public void closeWindow() {
		Locator w = windows().last();
		Locator x = w.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(Pattern.compile("^(close|discard|cancel)$", Pattern.CASE_INSENSITIVE)))
			.first();
		if (x.count() > 0) {
			x.click();
			return;
		}
		page.keyboard().press("Escape");
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
}
