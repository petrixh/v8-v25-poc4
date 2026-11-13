package de.symeda.sormas.ui.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;

/**
 * The browser's own leave-page warning while a form has unsaved changes. Vaadin 8 kept the whole app on one page
 * ({@code #!} addresses), so the browser's Back to an address the user had typed went through the view change and its
 * "unsaved changes" prompt. With path addresses, Back to a page that was loaded on its own leaves the page without a
 * view change; the browser asks instead (in its own wording). The same warning now also comes on reload, on a typed
 * address and when closing the tab.
 * <p>
 * A form ({@link CommitDiscardWrapperComponent}) has unsaved changes while it is dirty, as for the in-app prompt: a value
 * the user is still typing (focus in the field) does not count yet, as in Vaadin 8.
 */
public final class LeavePageWarning {

	/** Set on the form's element while the form is dirty. */
	static final String UNSAVED_CHANGES_ATTRIBUTE = "sormas-unsaved-changes";

	/** Installed once per page. */
	private static final String INSTALL_JS = "if (!window.__sormasLeavePageWarning) { window.__sormasLeavePageWarning = true;"
		+ " window.addEventListener('beforeunload', (e) => {"
		+ "  if (!window.__sormasLeavePageWarningOff && document.querySelector('[" + UNSAVED_CHANGES_ATTRIBUTE + "]')) {"
		+ "   e.preventDefault(); e.returnValue = ''; } }); }";

	private LeavePageWarning() {
		// Hide Utility Class Constructor
	}

	/**
	 * Installs the warning in the page of the form; call when the form is attached.
	 */
	static void install(Component form) {
		form.getUI().ifPresent(ui -> ui.getPage().executeJs(INSTALL_JS));
	}

	/**
	 * Marks the form as having unsaved changes or not.
	 */
	static void setUnsavedChanges(Component form, boolean unsavedChanges) {
		form.getElement().setAttribute(UNSAVED_CHANGES_ATTRIBUTE, unsavedChanges);
	}

	/**
	 * The app leaves the page on purpose (logout): no warning from then on in this page.
	 */
	public static void leaveWithoutWarning(UI ui) {
		ui.getPage().executeJs("window.__sormasLeavePageWarningOff = true");
	}
}
