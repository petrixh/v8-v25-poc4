package de.symeda.sormas.ui.environment;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.environment.EnvironmentIndexDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentField;

public class EnvironmentListEntry extends SideComponentField {

	private final EnvironmentIndexDto environment;
	private Button editButton;
	private Button unlinkEnvironmentButton;

	public EnvironmentListEntry(EnvironmentIndexDto environment) {
		this.environment = environment;

		HorizontalLayout topLayout = new HorizontalLayout();
		topLayout.setPadding(false);
		topLayout.setSpacing(false);
		topLayout.setWidthFull();
		addComponentToField(topLayout);

		VerticalLayout leftColumnLayout = new VerticalLayout();
		leftColumnLayout.setPadding(false);
		leftColumnLayout.setSpacing(false);

		// TOP LEFT
		VerticalLayout topLeftLayout = new VerticalLayout();
		{
			topLeftLayout.setPadding(false);
			topLeftLayout.setSpacing(false);
			Span environmentNameLabel = new Span(DataHelper.toStringNullable(environment.getEnvironmentName()));
			CssStyles.style(environmentNameLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
			topLeftLayout.add(environmentNameLabel);
			Span environmentMediaLabel = new Span(
				I18nProperties.getCaption(Captions.Event_environmentMedia) + ": " + DataHelper.toStringNullable(environment.getEnvironmentMedia()));
			CssStyles.style(environmentMediaLabel, CssStyles.HSPACE_RIGHT_4);
			topLeftLayout.add(environmentMediaLabel);
			Span investigationStatusLabel = new Span(
				I18nProperties.getCaption(Captions.Event_environment_investigationStatus) + ": "
					+ DataHelper.toStringNullable(environment.getInvestigationStatus()));
			CssStyles.style(investigationStatusLabel);
			topLeftLayout.add(investigationStatusLabel);
			Span envReportDtLabel = new Span(
				I18nProperties.getCaption(Captions.Event_environmentReportDate) + ": "
					+ DataHelper.toStringNullable(DateHelper.formatLocalDate(environment.getReportDate(), I18nProperties.getUserLanguage())));
			CssStyles.style(envReportDtLabel);
			envReportDtLabel.setWidthFull();
			topLeftLayout.add(envReportDtLabel);
			topLayout.add(topLeftLayout);
			topLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, topLeftLayout);
			// TOP RIGHT
			VerticalLayout topRightLayout = new VerticalLayout();
			topRightLayout.addClassName(CssStyles.ALIGN_RIGHT);
			topLayout.add(topRightLayout);
			topLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, topRightLayout);
		}

	}

	public void addEditListener(int rowIndex, ComponentEventListener<ClickEvent<Button>> editClickListener) {
		if (editButton == null) {
			editButton = ButtonHelper.createIconButtonWithCaption(
				"edit-environment-" + rowIndex,
				null,
				VaadinIcon.PENCIL,
				null,
				CssStyles.BUTTON_LINK,
				CssStyles.BUTTON_COMPACT);

			add(editButton);
			setVerticalComponentAlignment(FlexComponent.Alignment.START, editButton);
			setFlexGrow(0, editButton);
		}

		editButton.addClickListener(editClickListener);
	}

	public void addUnlinkEnvironmentListener(int rowIndex, ComponentEventListener<ClickEvent<Button>> unlinkEventClickListener) {
		if (unlinkEnvironmentButton == null) {
			unlinkEnvironmentButton = ButtonHelper.createIconButtonWithCaption(
				"unlink-environment-" + rowIndex,
				null,
				VaadinIcon.UNLINK,
				null,
				CssStyles.BUTTON_LINK,
				CssStyles.BUTTON_COMPACT);
			unlinkEnvironmentButton.setTooltipText(I18nProperties.getCaption(Captions.eventUnlinkEnvironment));

			add(unlinkEnvironmentButton);
			setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, unlinkEnvironmentButton);
			setFlexGrow(0, unlinkEnvironmentButton);
		}

		unlinkEnvironmentButton.addClickListener(unlinkEventClickListener);
	}

	public EnvironmentIndexDto getEnvironment() {
		return environment;
	}
}
