package de.symeda.sormas.ui.utils.components.sidecomponent;

import java.util.Arrays;
import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

public class SideComponent extends VerticalLayout {

	private final HorizontalLayout componentHeader;
	private final Consumer<Runnable> actionCallback;

	protected Button createButton;

	public SideComponent(String heading) {
		this(heading, Runnable::run);
	}

	public SideComponent(String heading, Consumer<Runnable> actionCallback) {
		setWidthFull();
		setPadding(false);
		setSpacing(false);

		componentHeader = new HorizontalLayout();
		componentHeader.setPadding(false);
		componentHeader.setSpacing(false);
		componentHeader.setWidthFull();
		add(componentHeader);

		Span headingLabel = new Span(heading);
		headingLabel.addClassName(CssStyles.H3);
		componentHeader.add(headingLabel);
		componentHeader.setFlexGrow(1, headingLabel);

		this.actionCallback = actionCallback;
	}

	protected void addCreateButton(Button button) {
		componentHeader.add(button);
		componentHeader.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, button);
		componentHeader.setFlexGrow(0, button);
	}

	protected void addCreateButton(String caption, Runnable callback, UserRight... userRights) {
		if (userRights.length == 0 || userHasRight(userRights)) {
			createButton = ButtonHelper.createButton(caption);
			CssStyles.style(createButton, CssStyles.BUTTON_PRIMARY);
			ButtonHelper.setIcon(createButton, VaadinIcon.PLUS_CIRCLE);
			createButton.addClickListener(e -> actionCallback.accept(callback));
			addCreateButton(createButton);
		}
	}

	private boolean userHasRight(UserRight... userRights) {
		return Arrays.stream(userRights).anyMatch(UiUtil::permitted);
	}
}
