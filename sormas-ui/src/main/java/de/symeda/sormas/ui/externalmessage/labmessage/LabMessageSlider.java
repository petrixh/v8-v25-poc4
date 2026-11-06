package de.symeda.sormas.ui.externalmessage.labmessage;

import java.util.List;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.externalmessage.ExternalMessageDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.externalmessage.ExternalMessageForm;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class LabMessageSlider {

	private final List<ExternalMessageDto> labMessages;
	private final ExternalMessageForm form;
	private final Button topLeftButton;
	private final Button topRightButton;
	private final Button bottomLeftButton;
	private final Button bottomRightButton;
	private int index;

	public LabMessageSlider(List<ExternalMessageDto> labMessages) {
		VerticalLayout layout = new VerticalLayout();
		layout.setPadding(true);
		VaadinUiUtil.showPopupWindow(layout, I18nProperties.getString(Strings.headingShowExternalMessage));

		this.index = 0;
		this.labMessages = labMessages;

		this.topLeftButton = ButtonHelper.createIconButton("", VaadinIcon.ANGLE_LEFT, (clickEvent) -> this.previous());
		this.topRightButton = ButtonHelper.createIconButton("", VaadinIcon.ANGLE_RIGHT, (clickEvent) -> this.next());

		HorizontalLayout topNavigator = new HorizontalLayout();
		topNavigator.setStyleName(CssStyles.FLOAT_RIGHT);
		topNavigator.addComponent(topLeftButton);
		topNavigator.addComponent(topRightButton);

		layout.addComponent(topNavigator);

		ExternalMessageForm form = new ExternalMessageForm();
		form.setWidth(550, Unit.PIXELS);
		form.setValue(labMessages.get(index));
		this.form = form;
		layout.addComponent(this.form);
		layout.addClassName("lab-message-slider");

		this.bottomLeftButton = ButtonHelper.createIconButton("", VaadinIcon.ANGLE_LEFT, (clickEvent) -> this.previous());
		this.bottomRightButton = ButtonHelper.createIconButton("", VaadinIcon.ANGLE_RIGHT, (clickEvent) -> this.next());
		HorizontalLayout bottomNavigator = new HorizontalLayout();
		bottomNavigator.setStyleName(CssStyles.FLOAT_RIGHT);
		bottomNavigator.addComponent(bottomLeftButton);
		bottomNavigator.addComponent(bottomRightButton);

		layout.addComponent(bottomNavigator);

		this.enableButtons();
	}

	private void next() {
		this.index++;
		this.form.setValue(this.labMessages.get(this.index));
		this.enableButtons();
	}

	private void previous() {
		this.index--;
		this.form.setValue(this.labMessages.get(this.index));
		this.enableButtons();
	}

	private void enableButtons() {
		this.topRightButton.setEnabled(this.index < this.labMessages.size() - 1);
		this.topLeftButton.setEnabled(this.index > 0);

		this.bottomRightButton.setEnabled(this.index < this.labMessages.size() - 1);
		this.bottomLeftButton.setEnabled(this.index > 0);
	}

}
