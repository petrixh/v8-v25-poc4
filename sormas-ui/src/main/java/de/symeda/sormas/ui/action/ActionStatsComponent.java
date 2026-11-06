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
package de.symeda.sormas.ui.action;

import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.action.ActionContext;
import de.symeda.sormas.api.action.ActionCriteria;
import de.symeda.sormas.api.action.ActionStatEntry;
import de.symeda.sormas.api.event.EventReferenceDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

@SuppressWarnings("serial")
public class ActionStatsComponent extends VerticalLayout {

	private final ActionCriteria actionCriteria;
	private final ActionContext context;
	private Button createButton;
	private VerticalLayout listLayout;

	public ActionStatsComponent(ActionContext context, EventReferenceDto entityRef) {
		setWidthFull();
		setPadding(false);
		setSpacing(false);

		this.context = context;
		this.actionCriteria = new ActionCriteria().event(entityRef);

		HorizontalLayout componentHeader = new HorizontalLayout();
		componentHeader.setPadding(false);
		componentHeader.setSpacing(false);
		componentHeader.setWidthFull();
		add(componentHeader);

		listLayout = new VerticalLayout();
		listLayout.setSpacing(true);
		listLayout.setPadding(false);
		add(listLayout);
		reload();

		Span actionsHeader = new Span(I18nProperties.getString(Strings.entityActions));
		actionsHeader.addClassName(CssStyles.H3);
		componentHeader.add(actionsHeader);

		if (UiUtil.permitted(UserRight.ACTION_CREATE)) {
			createButton = ButtonHelper.createIconButton(
				Captions.actionNewAction,
				VaadinIcon.PLUS_CIRCLE,
				e -> ControllerProvider.getActionController().create(context, entityRef, this::reload),
				CssStyles.BUTTON_PRIMARY);

			componentHeader.add(createButton);
			// was Alignment.MIDDLE_RIGHT
			componentHeader.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, createButton);
			createButton.getStyle().set("margin-left", "auto");
		}
	}

	private void reload() {
		List<ActionStatEntry> stats = FacadeProvider.getActionFacade().getActionStats(actionCriteria);
		listLayout.removeAll();
		if (stats.isEmpty()) {
			Span noActionsLabel = new Span(String.format(I18nProperties.getCaption(Captions.actionNoActions), context.toString()));
			setSpacing(false);
			listLayout.add(noActionsLabel);
		} else {
			setSpacing(true);
			listLayout.add(stats.stream().map(this::toComponent).collect(Collectors.toList()).toArray(new Component[0]));
		}
	}

	private Component toComponent(ActionStatEntry stat) {
		HorizontalLayout res = new HorizontalLayout();
		res.setSpacing(false);
		res.setPadding(false);
		res.setWidthFull();

		Span statusLabel = new Span(DataHelper.toStringNullable(stat.getActionStatus()));
		res.add(statusLabel);
		res.setFlexGrow(1, statusLabel);
		CssStyles.style(statusLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
		Span countLabel = new Span();
		if (stat.getCount() == null) {
			countLabel.setText("-");
		} else {
			countLabel.setText(String.valueOf(stat.getCount()));
			countLabel.addClassNames(CssStyles.BADGE, CssStyles.LABEL_LARGE);
		}
		countLabel.addClassName(CssStyles.ALIGN_RIGHT);
		res.add(countLabel);
		res.setFlexGrow(0, countLabel);

		return res;
	}
}
