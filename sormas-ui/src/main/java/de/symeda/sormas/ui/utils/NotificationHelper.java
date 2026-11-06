package de.symeda.sormas.ui.utils;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/**
 * Notifications with the Vaadin 8 notification types ({@code com.vaadin.ui.Notification.Type}), on Flow
 * {@link Notification}. Callers replace {@code Notification.show(caption, type)} and
 * {@code new Notification(caption, description, type, html).show(Page.getCurrent())} by the methods here.
 */
public final class NotificationHelper {

	/**
	 * The Vaadin 8 notification types and what they map to in Flow.
	 */
	public enum Type {

		/** Middle of the screen, closes on the user's next action. */
		HUMANIZED_MESSAGE(null, Notification.Position.MIDDLE, 0),
		/** Middle of the screen, closes 1.5 s after the user's next action. */
		WARNING_MESSAGE(NotificationVariant.LUMO_WARNING, Notification.Position.MIDDLE, 1500),
		/** Middle of the screen, stays until clicked. */
		ERROR_MESSAGE(NotificationVariant.LUMO_ERROR, Notification.Position.MIDDLE, -1),
		/** Bottom right, closes 3 s after the user's next action. */
		TRAY_NOTIFICATION(null, Notification.Position.BOTTOM_END, 3000),
		/** For screen readers only: not shown, announced through a hidden live region (Vaadin 8 placed it off screen). */
		ASSISTIVE_NOTIFICATION(null, Notification.Position.BOTTOM_START, 3000);

		private final NotificationVariant variant;
		private final Notification.Position position;
		private final int delayMsec;

		Type(NotificationVariant variant, Notification.Position position, int delayMsec) {
			this.variant = variant;
			this.position = position;
			this.delayMsec = delayMsec;
		}

		public int getDelayMsec() {
			return delayMsec;
		}
	}

	private NotificationHelper() {
		// Hide Utility Class Constructor
	}

	public static Notification showNotification(String caption, Type type, int delayMsec) {
		Notification notification = createNotification(caption, "", type, false);
		setDelayMsec(notification, delayMsec);
		open(notification, type, caption, null);
		return notification;
	}

	/**
	 * Was {@code Notification.show(caption, type)}.
	 */
	public static Notification show(String caption, Type type) {
		return show(caption, null, type, false);
	}

	/**
	 * Was {@code Notification.show(caption, description, type)}.
	 */
	public static Notification show(String caption, String description, Type type) {
		return show(caption, description, type, false);
	}

	/**
	 * Was {@code new Notification(caption, description, type, htmlContentAllowed).show(Page.getCurrent())}.
	 */
	public static Notification show(String caption, String description, Type type, boolean htmlContentAllowed) {
		Notification notification = createNotification(caption, description, type, htmlContentAllowed);
		open(notification, type, caption, htmlContentAllowed ? null : description);
		return notification;
	}

	/**
	 * Opens the notification; an assistive one is only announced to screen readers, as in Vaadin 8.
	 */
	private static void open(Notification notification, Type type, String caption, String description) {
		if (type != Type.ASSISTIVE_NOTIFICATION) {
			notification.open();
			return;
		}
		UI ui = UI.getCurrent();
		if (ui == null) {
			return;
		}
		String text = (caption != null ? caption : "") + (description != null && !description.isEmpty() ? " " + description : "");
		ui.getPage()
			.executeJs(
				"let r = document.getElementById('sormas-assistive-notification');"
					+ "if (!r) { r = document.createElement('div'); r.id = 'sormas-assistive-notification';"
					+ " r.setAttribute('aria-live', 'assertive'); r.setAttribute('role', 'alert');"
					+ " r.style.cssText = 'position:absolute;left:-10000px;top:auto;width:1px;height:1px;overflow:hidden';"
					+ " document.body.appendChild(r); }"
					+ "r.textContent = ''; setTimeout(() => { r.textContent = $0; }, 100);",
				text);
	}

	/**
	 * Shows the notification in {@code ui} from a thread that does not hold the session lock (e.g. a download
	 * handler). Without push it appears with the next round trip, as in Vaadin 8.
	 */
	public static void showAsync(UI ui, String caption, String description, Type type) {
		if (ui != null) {
			ui.access(() -> show(caption, description, type));
		}
	}

	/**
	 * Was {@code new Notification(caption, description, type, htmlContentAllowed)}; call {@code open()} to show it.
	 * Like in Vaadin 8, a click on the notification closes it.
	 */
	public static Notification createNotification(String caption, String description, Type type, boolean htmlContentAllowed) {
		Type notificationType = type != null ? type : Type.HUMANIZED_MESSAGE;
		Notification notification = new Notification();
		if (notificationType.variant != null) {
			notification.addThemeVariants(notificationType.variant);
		}
		notification.setPosition(notificationType.position);
		notification.setAssertive(notificationType == Type.ASSISTIVE_NOTIFICATION);
		setDelayMsec(notification, notificationType.delayMsec);
		notification.addAttachListener(e -> notification.getElement().executeJs(CLOSE_ON_USER_ACTION_JS));

		Div content = new Div();
		if (caption != null && !caption.isEmpty()) {
			content.add(createText(new Span(), caption, htmlContentAllowed));
		}
		if (description != null && !description.isEmpty()) {
			content.add(createText(new Div(), description, htmlContentAllowed));
		}
		content.addClickListener(e -> notification.close());
		notification.add(content);
		return notification;
	}

	/**
	 * Vaadin 8 delay: negative = until clicked; otherwise the notification stays until the user's next action (mouse
	 * moved more than a few pixels, mouse button, wheel, scroll or a key) and closes this many milliseconds after it
	 * (0 = at once). This is the timing of the Vaadin 8 client ({@code VNotification}); the browser starts the timer.
	 */
	public static void setDelayMsec(Notification notification, int delayMsec) {
		// Flow's own timer is off; the close timer below starts on the user's next action
		notification.setDuration(0);
		notification.getElement().setProperty(DELAY_PROPERTY, delayMsec);
	}

	private static final String DELAY_PROPERTY = "sormasDelayMsec";

	/**
	 * Installed once per attach: one-shot listeners on the document that close the notification
	 * {@link #DELAY_PROPERTY} ms after the user's next action, as the Vaadin 8 client did.
	 */
	private static final String CLOSE_ON_USER_ACTION_JS = "const n = this;"
		+ "if (n.__sormasCloseOnAction) { return; } n.__sormasCloseOnAction = true;"
		+ "const types = ['mousemove', 'mousedown', 'wheel', 'scroll', 'keydown'];"
		+ "let x0 = -1, y0 = -1;"
		+ "const handler = (e) => {"
		+ " if (!n.opened) { types.forEach(t => document.removeEventListener(t, handler, true)); return; }"
		+ " const delay = n.sormasDelayMsec;"
		+ " if (delay === undefined || delay < 0) { return; }"
		+ " if (e.type === 'mousemove') {"
		+ "  if (x0 < 0) { x0 = e.clientX; y0 = e.clientY; return; }"
		+ "  if (Math.hypot(e.clientX - x0, e.clientY - y0) <= 7) { return; } }"
		+ " if (e.type === 'keydown' && e.repeat) { return; }"
		+ " types.forEach(t => document.removeEventListener(t, handler, true));"
		+ " setTimeout(() => { n.opened = false; }, delay); };"
		+ "types.forEach(t => document.addEventListener(t, handler, true));";

	private static <T extends com.vaadin.flow.component.HtmlContainer> T createText(T component, String text, boolean html) {
		if (html) {
			component.getElement().setProperty("innerHTML", text);
		} else {
			component.setText(text);
		}
		return component;
	}
}
