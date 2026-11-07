package de.symeda.sormas.ui.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;

/**
 * Replaces the Vaadin 8 add-on {@code org.vaadin.hene.popupbutton.PopupButton} (no Vaadin 25 version): a button that
 * opens its content in a Flow {@link Popover} below it. Keeps the add-on methods SORMAS uses, so callers change only
 * the import. The popover opens on click and closes on a click outside or Escape, like the add-on's popup.
 */
public class PopupButton extends Button {

	private static final long serialVersionUID = 1L;

	private final Popover popover;
	private Component content;

	public PopupButton() {
		this(null);
	}

	public PopupButton(String caption) {
		super(caption);
		popover = new Popover();
		popover.setTarget(this);
		popover.setPosition(PopoverPosition.BOTTOM_START);
	}

	public void setContent(Component content) {
		popover.removeAll();
		this.content = content;
		if (content != null) {
			popover.add(content);
		}
	}

	public Component getContent() {
		return content;
	}

	public void setPopupVisible(boolean popupVisible) {
		popover.setOpened(popupVisible);
	}

	public boolean isPopupVisible() {
		return popover.isOpened();
	}

	public void setButtonClickTogglesPopupVisibility(boolean toggles) {
		popover.setOpenOnClick(toggles);
	}

	public void setClosePopupOnOutsideClick(boolean close) {
		popover.setCloseOnOutsideClick(close);
	}

	/**
	 * @return the Flow popover holding the content (e.g. for {@code AbstractView.addExportButton})
	 */
	public Popover getPopover() {
		return popover;
	}
}
