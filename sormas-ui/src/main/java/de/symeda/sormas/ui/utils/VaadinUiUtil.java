/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.ui.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.shared.Registration;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserDto;

/**
 * Popup windows are Flow {@link Dialog}s (were Vaadin 8 {@code Window}s). The helpers keep the Vaadin 8 window
 * behaviour where Flow differs: modal, not resizable, draggable, a close button in the header and Escape closes when
 * closable, a click outside does not close. Use {@link #addCloseListener(Dialog, ComponentEventListener)} where the
 * old code called {@code window.addCloseListener} and {@link #setClosable(Dialog, boolean)} for
 * {@code window.setClosable}.
 */
public final class VaadinUiUtil {

	public static final String FRONT_ELEMENT_STYLE_NAME = "front-element";

	/**
	 * Folder of the old Valo theme images (was {@code ThemeResource("img/...")}); whether Flow serves it is decided
	 * with the theme work (plan R7).
	 */
	public static final String THEME_IMAGE_PATH = "VAADIN/themes/sormas/";

	private static final String CLOSE_BUTTON_KEY = VaadinUiUtil.class.getName() + ".closeButton";

	private VaadinUiUtil() {
		// Hide Utility Class Constructor
	}

	public static Dialog createPopupWindow() {
		Dialog window = new Dialog();
		window.setModal(true);
		window.setResizable(false);
		// Vaadin 8 windows could be moved and were not closed by a click outside
		window.setDraggable(true);
		window.setCloseOnOutsideClick(false);
		setClosable(window, true);

		return window;
	}

	/**
	 * Was {@code Window.setClosable}: a closable window has a close button in its header and closes on Escape.
	 */
	public static void setClosable(Dialog window, boolean closable) {
		window.setCloseOnEsc(closable);
		Button closeButton = (Button) ComponentUtil.getData(window, CLOSE_BUTTON_KEY);
		if (closable && closeButton == null) {
			closeButton = new Button(VaadinIcon.CLOSE_SMALL.create(), e -> window.close());
			closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
			closeButton.setId("closeWindow");
			window.getHeader().add(closeButton);
			ComponentUtil.setData(window, CLOSE_BUTTON_KEY, closeButton);
		}
		if (closeButton != null) {
			closeButton.setVisible(closable);
		}
	}

	public static boolean isClosable(Dialog window) {
		Button closeButton = (Button) ComponentUtil.getData(window, CLOSE_BUTTON_KEY);
		return closeButton != null && closeButton.isVisible();
	}

	/**
	 * Was {@code Window.addCloseListener}: called once the window is closed, by the user or by {@code close()}.
	 */
	public static Registration addCloseListener(Dialog window, ComponentEventListener<Dialog.OpenedChangeEvent> closeListener) {
		return window.addOpenedChangeListener(e -> {
			if (!e.isOpened()) {
				closeListener.onComponentEvent(e);
			}
		});
	}

	/**
	 * Enforces the popup to be in front of other modals.
	 * Internally uses CSS's z-index to make it work.
	 * 
	 * @return Window that will stay at front.
	 */
	public static Dialog createForegroundPopupWindow() {
		Dialog popupWindow = createPopupWindow();

		popupWindow.addClassName(FRONT_ELEMENT_STYLE_NAME);

		return popupWindow;
	}

	public static Dialog showSimplePopupWindow(String caption, String contentText) {
		return showSimplePopupWindow(caption, contentText, ContentMode.TEXT, null);
	}

	public static Dialog showSimplePopupWindow(String caption, String contentText, ContentMode contentMode) {
		return showSimplePopupWindow(caption, contentText, contentMode, null);
	}

	public static Dialog showSimplePopupWindow(String caption, String contentText, ContentMode contentMode, Integer width) {
		Dialog window = createPopupWindow();

		if (width != null) {
			window.setWidth(width + "px");
		}

		VerticalLayout popupLayout = new VerticalLayout();
		popupLayout.setPadding(true);
		popupLayout.setSpacing(true);
		popupLayout.setSizeUndefined();
		Span contentLabel = createLabel(contentText, contentMode);
		contentLabel.setWidthFull();
		popupLayout.add(contentLabel);
		Button okayButton = ButtonHelper.createButton(Captions.actionOkay, e -> {
			window.close();
		}, CssStyles.BUTTON_PRIMARY);

		popupLayout.add(okayButton);
		popupLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, okayButton);

		window.setHeaderTitle(caption);
		window.add(popupLayout);

		window.open();

		return window;
	}

	public static Dialog showPopupWindow(Component content) {

		return showPopupWindow(content, null);
	}

	public static Dialog showPopupWindow(Component content, String caption) {
		Dialog window = createPopupWindow();
		window.setHeaderTitle(caption);
		window.add(content);
		window.open();
		return window;
	}

	public static Dialog showPopupWindowWithWidth(Component content, String caption, Integer width) {
		Dialog window = createPopupWindow();
		window.setHeaderTitle(caption);
		window.add(content);
		if (width != null) {
			window.setWidth(width + "%");
		}

		window.open();
		return window;
	}

	public static Dialog showModalPopupWindow(CommitDiscardWrapperComponent<?> content, String caption) {
		return showModalPopupWindow(content, caption, false);
	}

	public static Dialog showModalPopupWindow(CommitDiscardWrapperComponent<?> content, String caption, boolean discardOnClose) {
		return showModalPopupWindow(content, caption, discardOnClose, null);
	}

	public static Dialog showModalPopupWindow(CommitDiscardWrapperComponent<?> content, String caption, Consumer<Boolean> callback) {
		return showModalPopupWindow(content, caption, false, callback);
	}

	public static Dialog showModalPopupWindow(
		CommitDiscardWrapperComponent<?> content,
		String caption,
		boolean discardOnClose,
		Consumer<Boolean> callback) {
		final Dialog popupWindow = VaadinUiUtil.showPopupWindow(content);
		popupWindow.setHeaderTitle(caption);
		// was setMargin(true): the Vaadin 8 margin is the padding in Flow
		content.setPadding(true);

		final Registration closeListener;
		if (discardOnClose) {
			closeListener = addCloseListener(popupWindow, e -> content.discard());
		} else {
			closeListener = null;
		}

		content.addDoneListener(() -> {
			if (closeListener != null) {
				closeListener.remove();
			}

			popupWindow.close();
		});

		if (callback != null) {
			content.addCommitListener(() -> callback.accept(true));
			content.addDiscardListener(() -> callback.accept(false));
		}

		return popupWindow;
	}

	/**
	 * Was a generated property with the icon's HTML in a Vaadin 7 grid container; together with
	 * {@link #setupActionColumn(Grid.Column)} it gives the narrow action column.
	 */
	public static <T> Grid.Column<T> addIconColumn(Grid<T> grid, String iconPropertyId, VaadinIcon vaadinIconsIcon) {
		return grid.addComponentColumn(item -> vaadinIconsIcon.create()).setKey(iconPropertyId);
	}

	public static void setupActionColumn(Grid.Column<?> column) {
		column.setWidth("20px");
		column.setFlexGrow(0);
		column.setSortable(false);
		column.setHeader("");
	}

	public static Dialog showConfirmationPopup(
		String caption,
		Component content,
		String confirmCaption,
		String cancelCaption,
		Consumer<Boolean> resultConsumer) {

		return showConfirmationPopup(caption, content, confirmCaption, cancelCaption, getEstimatedWidth(caption.length()), resultConsumer);
	}

	public static Integer getEstimatedWidth(int length) {
		BigDecimal width = new BigDecimal(length).multiply(BigDecimal.valueOf(13.6));
		width = width.setScale(0, RoundingMode.HALF_UP);
		return width.intValue();
	}

	public static Dialog showConfirmationPopup(
		String caption,
		Component content,
		String confirmCaption,
		String cancelCaption,
		Integer width,
		Consumer<Boolean> resultConsumer) {

		return showConfirmationPopup(caption, content, popupWindow -> {
			ConfirmationComponent confirmationComponent = new ConfirmationComponent(false) {

				private static final long serialVersionUID = 1L;

				@Override
				protected void onConfirm() {
					resultConsumer.accept(true);
					popupWindow.close();
				}

				@Override
				protected void onCancel() {
					resultConsumer.accept(false);
					popupWindow.close();
				}
			};

			confirmationComponent.getConfirmButton().setText(confirmCaption);
			confirmationComponent.getCancelButton().setText(cancelCaption);

			return confirmationComponent;
		}, width);
	}

	public static CompletionStage<Boolean> showConfirmationPopup(String caption, Component content, String confirmCaption, String cancelCaption) {

		CompletableFuture<Boolean> ret = new CompletableFuture<>();

		VaadinUiUtil.showConfirmationPopup(caption, content, confirmCaption, cancelCaption, null, ret::complete, true);

		return ret;
	}

	public static Dialog showConfirmationPopup(
		String caption,
		Component content,
		String confirmCaption,
		String cancelCaption,
		Integer width,
		UnaryOperator<Boolean> resultConsumer) {
		return showConfirmationPopup(caption, content, confirmCaption, cancelCaption, width, resultConsumer, false);
	}

	public static Dialog showConfirmationPopup(
		String caption,
		Component content,
		String confirmCaption,
		String cancelCaption,
		Integer width,
		UnaryOperator<Boolean> resultConsumer,
		boolean cancelOnClose) {

		return showConfirmationPopup(caption, content, popupWindow -> {

			Registration closeListener = addCloseListener(popupWindow, e -> {
				if (cancelOnClose) {
					resultConsumer.apply(false);
				}
			});

			ConfirmationComponent confirmationComponent = new ConfirmationComponent(false) {

				private static final long serialVersionUID = 1L;

				@Override
				protected void onConfirm() {
					if (Boolean.TRUE.equals(resultConsumer.apply(true))) {
						closeListener.remove();
						popupWindow.close();
					}
				}

				@Override
				protected void onCancel() {
					if (Boolean.TRUE.equals(resultConsumer.apply(false))) {
						closeListener.remove();
						popupWindow.close();
					}
				}
			};

			confirmationComponent.getConfirmButton().setText(confirmCaption);
			if (cancelCaption != null) {
				confirmationComponent.getCancelButton().setText(cancelCaption);
			} else {
				confirmationComponent.getCancelButton().setVisible(false);
			}

			return confirmationComponent;
		}, width);
	}

	public static Dialog showConfirmationPopup(
		String caption,
		Component content,
		Function<Dialog, ConfirmationComponent> confirmationComponentProvider,
		Integer width) {

		Dialog popupWindow = VaadinUiUtil.createPopupWindow();
		if (width != null) {
			popupWindow.setWidth(width + "px");
		} else {
			popupWindow.setWidth(null);
		}
		popupWindow.setHeaderTitle(caption);

		VerticalLayout layout = new VerticalLayout();
		layout.setPadding(true);
		setWidthFull(content);
		layout.add(content);

		ConfirmationComponent confirmationComponent = confirmationComponentProvider.apply(popupWindow);

		layout.add(confirmationComponent);
		layout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, confirmationComponent);
		layout.setWidthFull();
		layout.setSpacing(true);

		popupWindow.add(layout);
		setClosable(popupWindow, false);

		popupWindow.open();

		return popupWindow;
	}

	/**
	 * @param resultConsumer
	 *            TRUE: Option A, FALSE: Option B
	 */
	public static Dialog showChooseOptionPopup(
		String caption,
		Component content,
		String optionACaption,
		String optionBCaption,
		Integer width,
		Consumer<Boolean> resultConsumer) {
		return showChooseOptionPopup(caption, content, optionACaption, optionBCaption, width, resultConsumer, false);
	}

	/**
	 * @param resultConsumer
	 *            TRUE: Option A, FALSE: Option B
	 */
	public static Dialog showChooseOptionPopup(
		String caption,
		Component content,
		String optionACaption,
		String optionBCaption,
		Integer width,
		Consumer<Boolean> resultConsumer,
		boolean closable) {

		Dialog popupWindow = VaadinUiUtil.createPopupWindow();
		if (width != null) {
			popupWindow.setWidth(width + "px");
		} else {
			popupWindow.setWidth(null);
		}
		popupWindow.setHeaderTitle(caption);

		VerticalLayout layout = new VerticalLayout();
		layout.setPadding(true);
		setWidthFull(content);
		layout.add(content);

		ConfirmationComponent confirmationComponent = new ConfirmationComponent(false) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfirm() {
				resultConsumer.accept(true);
				popupWindow.close();
			}

			@Override
			protected void onCancel() {
				resultConsumer.accept(false);
				popupWindow.close();
			}
		};
		confirmationComponent.getConfirmButton().setText(optionACaption);
		Button cancelButton = confirmationComponent.getCancelButton();
		cancelButton.setText(optionBCaption);
		CssStyles.removeStyles(cancelButton, CssStyles.BUTTON_LINK);
		CssStyles.style(cancelButton, CssStyles.BUTTON_PRIMARY);

		layout.add(confirmationComponent);
		layout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, confirmationComponent);
		layout.setWidthFull();
		layout.setSpacing(true);
		popupWindow.add(layout);
		setClosable(popupWindow, closable);

		popupWindow.open();
		return popupWindow;
	}

	public static Dialog showThreeOptionsPopup(
		String caption,
		Component content,
		String optionACaption,
		String optionBCaption,
		String optionCCaption,
		Integer width,
		Consumer<PopupOption> resultConsumer) {

		return showThreeOptionsPopup(caption, content, null, optionACaption, optionBCaption, optionCCaption, width, resultConsumer);
	}

	public static Dialog showThreeOptionsPopup(
		String caption,
		Component content,
		String description,
		String optionACaption,
		String optionBCaption,
		String optionCCaption,
		Integer width,
		Consumer<PopupOption> resultConsumer) {

		Dialog popupWindow = VaadinUiUtil.createPopupWindow();
		if (width != null) {
			popupWindow.setWidth(width + "px");
		} else {
			popupWindow.setWidth(null);
		}
		popupWindow.setHeaderTitle(caption);

		VerticalLayout layout = new VerticalLayout();
		layout.setPadding(true);
		setWidthFull(content);

		if (description != null) {
			layout.add(VaadinUiUtil.createInfoComponent(description));
		}

		layout.add(content);

		ConfirmationComponent confirmationComponent = new ConfirmationComponent(false) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfirm() {
				resultConsumer.accept(PopupOption.OPTION1);
				popupWindow.close();
			}

			@Override
			protected void onCancel() {
				resultConsumer.accept(PopupOption.OPTION2);
				popupWindow.close();
			}
		};
		confirmationComponent.getConfirmButton().setText(optionACaption);
		Button cancelButton = confirmationComponent.getCancelButton();
		cancelButton.setText(optionBCaption);
		CssStyles.removeStyles(cancelButton, CssStyles.BUTTON_LINK);
		CssStyles.style(cancelButton, CssStyles.BUTTON_PRIMARY);

		confirmationComponent
			.addExtraButton(ButtonHelper.createButton(Strings.unsavedChanges_cancel, optionCCaption, null, CssStyles.BUTTON_PRIMARY), buttonEvent -> {
				resultConsumer.accept(PopupOption.OPTION3);
				popupWindow.close();
			});

		layout.add(confirmationComponent);
		layout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, confirmationComponent);
		layout.setWidthFull();
		layout.setSpacing(true);
		popupWindow.add(layout);
		setClosable(popupWindow, false);

		popupWindow.open();
		return popupWindow;
	}

	public enum PopupOption {
		OPTION1,
		OPTION2,
		OPTION3;
	}

	public static Dialog showDeleteConfirmationWindow(String content, Runnable callback) {
		Dialog popupWindow = VaadinUiUtil.createPopupWindow();

		VerticalLayout deleteLayout = new VerticalLayout();
		deleteLayout.setPadding(true);
		deleteLayout.setSizeUndefined();
		deleteLayout.setSpacing(true);

		Span description = createLabel(content, ContentMode.HTML);
		description.setWidthFull();
		deleteLayout.add(description);

		ConfirmationComponent deleteConfirmationComponent = new ConfirmationComponent(false) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfirm() {
				popupWindow.close();
				onDone();
				callback.run();
			}

			@Override
			protected void onCancel() {
				popupWindow.close();
			}
		};
		deleteConfirmationComponent.getConfirmButton().setText(I18nProperties.getString(Strings.yes));
		deleteConfirmationComponent.getCancelButton().setText(I18nProperties.getString(Strings.no));
		deleteLayout.add(deleteConfirmationComponent);
		deleteLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, deleteConfirmationComponent);

		popupWindow.setHeaderTitle(I18nProperties.getString(Strings.headingConfirmDeletion));
		popupWindow.add(deleteLayout);
		popupWindow.open();

		return popupWindow;
	}

	public static ConfirmationComponent buildYesNoConfirmationComponent() {
		ConfirmationComponent requestTaskComponent = new ConfirmationComponent(false) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfirm() {
			}

			@Override
			protected void onCancel() {
			}
		};
		requestTaskComponent.getConfirmButton().setText(I18nProperties.getString(Strings.yes));
		requestTaskComponent.getCancelButton().setText(I18nProperties.getString(Strings.no));
		return requestTaskComponent;
	}

	public static HorizontalLayout createInfoComponent(String htmlContent) {
		return createIconComponent(htmlContent, "img/info-icon.png", 35);

	}

	public static HorizontalLayout createWarningComponent(String htmlContent) {
		return createWarningComponent(htmlContent, 35);
	}

	public static HorizontalLayout createWarningComponent(String htmlContent, int iconSize) {
		return createIconComponent(htmlContent, "img/warning-icon.png", iconSize);

	}

	public static HorizontalLayout createErrorComponent(String htmlContent) {
		return createErrorComponent(htmlContent, 35);
	}

	public static HorizontalLayout createErrorComponent(String htmlContent, int iconSize) {
		return createIconComponent(htmlContent, "img/error-icon.png", iconSize);

	}

	public static HorizontalLayout createIconComponent(String htmlContent, String iconName, int iconSize) {
		HorizontalLayout infoLayout = new HorizontalLayout();
		infoLayout.setWidthFull();
		infoLayout.setSpacing(true);
		Image icon = new Image(THEME_IMAGE_PATH + iconName, "");
		icon.setHeight(iconSize + "px");
		icon.setWidth(iconSize + "px");
		infoLayout.add(icon);
		infoLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, icon);
		Span infoLabel = createLabel(htmlContent, ContentMode.HTML);
		infoLabel.setWidthFull();
		infoLayout.add(infoLabel);
		infoLayout.setFlexGrow(1, infoLabel);
		CssStyles.style(infoLayout, CssStyles.VSPACE_3);
		return infoLayout;
	}

	public static VerticalLayout createWarningLayout() {
		VerticalLayout warningLayout = new VerticalLayout();
		warningLayout.setPadding(true);
		Image warningIcon = new Image(THEME_IMAGE_PATH + "img/warning-icon.png", "");
		warningIcon.setHeight("35px");
		warningIcon.setWidth("35px");
		warningLayout.addComponentAsFirst(warningIcon);
		CssStyles.style(warningLayout, CssStyles.ALIGN_CENTER);
		return warningLayout;
	}

	public static void showWarningPopup(String message) {
		VerticalLayout warningLayout = createWarningLayout();
		Dialog popupWindow = VaadinUiUtil.showPopupWindow(warningLayout);
		Span infoLabel = new Span(message);
		CssStyles.style(infoLabel, CssStyles.LABEL_LARGE, CssStyles.LABEL_WHITE_SPACE_NORMAL);
		warningLayout.add(infoLabel);
		addCloseListener(popupWindow, e -> popupWindow.close());
		popupWindow.setWidth("400px");
	}

	/**
	 * Was a Vaadin 8 {@code Label}: a {@link Span} with the text, or with the HTML for {@link ContentMode#HTML}. The
	 * HTML is set as is, as the Label did; callers clean user input as before.
	 */
	public static Span createLabel(String content, ContentMode contentMode) {
		Span label = new Span();
		setLabelValue(label, content, contentMode);
		return label;
	}

	/**
	 * Was {@code Label.setValue} on a label with the given content mode.
	 */
	public static void setLabelValue(Span label, String content, ContentMode contentMode) {
		if (contentMode == ContentMode.HTML) {
			label.setText(null);
			label.getElement().setProperty("innerHTML", content != null ? content : "");
		} else {
			label.getElement().removeProperty("innerHTML");
			label.setText(content);
			if (contentMode == ContentMode.PREFORMATTED) {
				label.getStyle().set("white-space", "pre");
			}
		}
	}

	private static void setWidthFull(Component component) {
		if (component instanceof HasSize) {
			((HasSize) component).setWidthFull();
		}
	}

	public static void addGdprMessageOnClick(TextArea textArea) {
		AtomicBoolean gdprMessageTriggered = new AtomicBoolean(false);
		Dialog subWindowGdpR = createForegroundPopupWindow();

		textArea.addFocusListener(i -> {
			showGdprWindow(textArea, gdprMessageTriggered, subWindowGdpR);
		});

		textArea.addBlurListener(blurEvent -> {
			if (gdprMessageTriggered.get() && !subWindowGdpR.isOpened()) {
				gdprMessageTriggered.set(false);
			}
		});
	}

	/**
	 * Was {@code addGdprMessageOnClick(RichTextArea)}: the GDPR message shows on the first value change (the Vaadin 7
	 * rich text area had no focus event). For the field that replaces the rich text area (plan Q4).
	 */
	public static <F extends Component & HasValue<?, ?> & Focusable<?>> void addGdprMessageOnValueChange(F richTextArea) {
		AtomicBoolean gdprMessageTriggered = new AtomicBoolean(false);
		Dialog subWindowGdpR = VaadinUiUtil.createForegroundPopupWindow();

		richTextArea.addValueChangeListener(event -> {
			if (!gdprMessageTriggered.get()) {
				showGdprWindow(richTextArea, gdprMessageTriggered, subWindowGdpR);
				gdprMessageTriggered.set(true);
			}
		});
	}

	private static void showGdprWindow(Focusable<?> textArea, AtomicBoolean gdprMessageTriggered, Dialog subWindowGdpR) {
		if (!gdprMessageTriggered.get()) {
			gdprMessageTriggered.set(true);
			subWindowGdpR.setHeaderTitle(I18nProperties.getPrefixCaption(UserDto.I18N_PREFIX, UserDto.HAS_CONSENTED_TO_GDPR));
			VerticalLayout subContentGdpr = new VerticalLayout();
			subWindowGdpR.removeAll();
			subWindowGdpR.add(subContentGdpr);
			subWindowGdpR.setWidth("40%");
			subWindowGdpR.setModal(true);
			setClosable(subWindowGdpR, true);

			Span textGdpr = new Span();
			textGdpr.setWidth("80%");
			textGdpr.setSizeFull();
			textGdpr.setText(I18nProperties.getString(Strings.messageGdpr));
			subContentGdpr.add(textGdpr);

			HorizontalLayout buttonLayout = new HorizontalLayout();
			buttonLayout.setPadding(false);
			buttonLayout.setSpacing(true);
			buttonLayout.setWidthFull();
			Button buttonGdpr = ButtonHelper.createButton(I18nProperties.getCaption(Captions.actionConfirm), event -> {
				subWindowGdpR.close();
				textArea.focus();
			}, CssStyles.BUTTON_PRIMARY);
			buttonLayout.add(buttonGdpr);
			subContentGdpr.add(buttonLayout);
			// was Alignment.BOTTOM_RIGHT with expand ratio 0
			buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
			subWindowGdpR.open();
		}
	}
}
