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
package de.symeda.sormas.ui.events;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.action.ActionContext;
import de.symeda.sormas.api.action.ActionCriteria;
import de.symeda.sormas.api.event.EventDto;
import de.symeda.sormas.api.event.EventReferenceDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.action.ActionList;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DetailSubComponentWrapper;

public class EventActionsView extends AbstractEventView {

	private static final long serialVersionUID = 597552547876685564L;

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/eventactions";

	private ActionCriteria criteria = new ActionCriteria();

	private ActionList list;
	private Button addButton;
	private DetailSubComponentWrapper listLayout;
	private EventActionFilterForm filterForm;

	public EventActionsView() {
		super(VIEW_NAME);
		setSizeFull();
	}

	/**
	 * Creates the top bar component with header and create button.
	 */
	private HorizontalLayout createTopBar() {
		HorizontalLayout topLayout = new HorizontalLayout();
		topLayout.setSpacing(true);
		topLayout.setWidth(100, Unit.PERCENTAGE);

		Span header = new Span(I18nProperties.getPrefixCaption(EventDto.I18N_PREFIX, EventDto.EVENT_ACTIONS));
		header.setSizeUndefined();
		CssStyles.style(header, CssStyles.H2, CssStyles.VSPACE_NONE);
		topLayout.add(header);

		// add create button if user has role
		if (UiUtil.permitted(UserRight.ACTION_CREATE)) {
			addButton = ButtonHelper.createIconButton(Captions.actionCreate, VaadinIcon.PLUS_CIRCLE, e -> {
				ControllerProvider.getActionController().create(ActionContext.EVENT, this.getEventRef(), this::reload);
			}, CssStyles.BUTTON_PRIMARY);

			topLayout.add(addButton);
			// was Alignment.MIDDLE_RIGHT in its slot: header left, button right
			topLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, addButton);
			topLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		}

		topLayout.addClassName(CssStyles.VSPACE_3);
		return topLayout;
	}

	/**
	 * Create the filter bar.
	 */
	private HorizontalLayout createFilterBar() {
		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.setPadding(false);
		filterLayout.setSpacing(true);
		filterLayout.setSizeUndefined();

		filterForm = new EventActionFilterForm();
		filterForm.addResetHandler(e -> navigateTo(null));
		filterForm.addApplyHandler(e -> navigateTo(criteria));
		filterLayout.add(filterForm);

		return filterLayout;
	}

	@Override
	protected void initView(String params) {

		if (params.contains("?")) {
			criteria.fromUrlParams(params.substring(params.indexOf("?") + 1));
		}
		EventReferenceDto eventRef = getEventRef();
		criteria.event(eventRef);

		if (list == null) {
			list = new ActionList(ActionContext.EVENT, criteria, 20);
			listLayout = new DetailSubComponentWrapper(() -> null);
			listLayout.setSizeFull();
			listLayout.setPadding(true);
			listLayout.setSpacing(false);

			HorizontalLayout topBar = createTopBar();
			listLayout.add(topBar);

			HorizontalLayout filterBar = createFilterBar();
			listLayout.add(filterBar);

			listLayout.add(list);
			listLayout.setFlexGrow(1, list);
			setSubComponent(listLayout);

			boolean hasEventEditRight = UiUtil.permitted(UserRight.EVENT_EDIT);
			if (hasEventEditRight) {
				listLayout.setEnabled(isEditAllowed() && !isEventDeleted());
			} else {
				topBar.setEnabled(false);
				filterBar.setEnabled(false);
			}
		}

		updateFilterComponents();

		reload();
	}

	/**
	 * Update filter form values.
	 */
	private void updateFilterComponents() {
		// disable triggers on value change
		applyingCriteria = true;
		filterForm.setValue(criteria);
		applyingCriteria = false;
	}

	/**
	 * Reload datas in the list.
	 */
	private void reload() {
		list.reload();
	}
}
