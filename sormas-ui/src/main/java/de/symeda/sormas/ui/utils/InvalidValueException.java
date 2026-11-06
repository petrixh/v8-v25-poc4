package de.symeda.sormas.ui.utils;

/**
 * A value is not valid; replaces the Vaadin 7 {@code com.vaadin.v7.data.Validator.InvalidValueException} that
 * {@link Buffered#commit()} throws and {@link CommitDiscardWrapperComponent#commitAndHandle()} shows. An exception with
 * causes stands for several invalid values; an invisible one (validation not shown on its field) is not listed.
 */
public class InvalidValueException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private static final InvalidValueException[] NO_CAUSES = new InvalidValueException[0];

	private final InvalidValueException[] causes;
	private boolean invisible;

	public InvalidValueException(String message) {
		this(message, NO_CAUSES);
	}

	public InvalidValueException(String message, InvalidValueException... causes) {
		super(message);
		this.causes = causes != null ? causes : NO_CAUSES;
	}

	/**
	 * @return the message escaped for HTML, as in Vaadin 7
	 */
	public String getHtmlMessage() {
		String message = getLocalizedMessage();
		if (message == null) {
			return null;
		}
		return message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}

	public InvalidValueException[] getCauses() {
		return causes;
	}

	public boolean isInvisible() {
		return invisible;
	}

	public void setInvisible(boolean invisible) {
		this.invisible = invisible;
	}
}
