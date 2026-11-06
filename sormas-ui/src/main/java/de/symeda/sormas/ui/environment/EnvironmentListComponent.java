package de.symeda.sormas.ui.environment;

import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.environment.EnvironmentCriteria;
import de.symeda.sormas.api.event.EventDto;
import de.symeda.sormas.api.event.EventReferenceDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

public class EnvironmentListComponent extends VerticalLayout {

	private final Consumer<Runnable> actionCallback;

	public EnvironmentListComponent(EventReferenceDto eventReferenceDto, boolean isEditAllowed, Consumer<Runnable> actionCallback) {
		this.actionCallback = actionCallback;
		createEnvironmentListComponent(
			new EnvironmentList(eventReferenceDto, isEditAllowed),
			I18nProperties.getString(Strings.entityEnvironment),
			false,
			() -> {
				EnvironmentCriteria criteria = new EnvironmentCriteria();
				criteria.setEvent(eventReferenceDto);
				//check if there are active environments in the database
				long events = FacadeProvider.getEnvironmentFacade().count(criteria);
				EventDto eventDto = FacadeProvider.getEventFacade().getByUuid(eventReferenceDto.getUuid());
				if (events > 0) {
					ControllerProvider.getEnvironmentController().selectOrCreateEnvironment(eventDto);
				} else {
					ControllerProvider.getEnvironmentController().create(eventDto);
				}
			},
			isEditAllowed);
	}

	private void createEnvironmentListComponent(
		EnvironmentList environmentList,
		String heading,
		boolean bottomCreateButton,
		Runnable linkEnvironmentCallback,
		boolean isEditAllowed) {

		setWidthFull();
		setPadding(false);
		setSpacing(false);
		Button createButton;

		HorizontalLayout componentHeader = new HorizontalLayout();
		componentHeader.setPadding(false);
		componentHeader.setSpacing(false);
		componentHeader.setWidthFull();
		add(componentHeader);

		add(environmentList);
		environmentList.reload();

		Span environmentLabel = new Span(heading);
		environmentLabel.addClassName(CssStyles.H3);
		componentHeader.add(environmentLabel);
		// Vaadin 8: the heading's slot took the free space, the button sat right in its own slot
		componentHeader.setFlexGrow(1, environmentLabel);

		if (UiUtil.permitted(isEditAllowed, UserRight.ENVIRONMENT_CREATE)) {
			createButton = ButtonHelper.createButton(I18nProperties.getCaption(Captions.linkEnvironment));
			ButtonHelper.setIcon(createButton, VaadinIcon.PLUS_CIRCLE);
			createButton.addClickListener(e -> actionCallback.accept(linkEnvironmentCallback));
			CssStyles.style(createButton, CssStyles.BUTTON_PRIMARY);

			if (bottomCreateButton) {
				HorizontalLayout buttonLayout = new HorizontalLayout();
				buttonLayout.setSpacing(true);
				buttonLayout.setPadding(false);
				buttonLayout.setWidthFull();
				buttonLayout.add(createButton);
				CssStyles.style(buttonLayout, CssStyles.VSPACE_TOP_3);
				add(buttonLayout);
			} else {
				componentHeader.add(createButton);
				componentHeader.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, createButton);
			}
		}
	}
}
