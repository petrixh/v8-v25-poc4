/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2022 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.ui.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.symeda.sormas.ui.utils.fields.CheckBox;
import de.symeda.sormas.ui.utils.fields.CustomField;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.NotificationType;
import de.symeda.sormas.api.user.NotificationTypeGroup;
import de.symeda.sormas.api.user.UserRoleDto;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.components.CheckboxSetItemDataSource;

public class UserRoleNotificationCheckboxSet extends CustomField<UserRoleDto.NotificationTypes> {

	private static final long serialVersionUID = 4149126643467960889L;

	private VerticalLayout layout;

	private List<CheckboxRow> rows = Collections.emptyList();

	/**
	 * True while {@link #setPresentationValue} sets the check boxes, so their data sources do not set the value again.
	 */
	private boolean updatingCheckboxes;

	@Override
	protected Component initContent() {
		layout = new VerticalLayout();
		layout.setWidthFull();
		// was MarginInfo(true, false): vertical margin only; the spacing is look, left to the theme
		layout.setPadding(false);

		buildCheckboxes();

		return layout;
	}

	@Override
	public Class<? extends UserRoleDto.NotificationTypes> getType() {
		return UserRoleDto.NotificationTypes.class;
	}

	/**
	 * Keeps a copy of the given value, as V7 {@code setInternalValue} did.
	 */
	@Override
	public void setValue(UserRoleDto.NotificationTypes newValue) {
		super.setValue(newValue != null ? copyOf(newValue) : null);
	}

	@Override
	protected void setPresentationValue(UserRoleDto.NotificationTypes newValue) {
		if (newValue == null) {
			return;
		}

		Set<NotificationType> smsNotifications = newValue.getSms();
		Set<NotificationType> emailNotifications = newValue.getEmail();
		updatingCheckboxes = true;
		try {
			rows.stream().map(r -> r.checkboxes).flatMap(Collection::stream).forEach(c -> {
				c.smsCb.setValue(smsNotifications.contains(c.type));
				c.emailCb.setValue(emailNotifications.contains(c.type));
			});
		} finally {
			updatingCheckboxes = false;
		}
	}

	private static UserRoleDto.NotificationTypes copyOf(UserRoleDto.NotificationTypes value) {
		return UserRoleDto.NotificationTypes.of(new HashSet<>(value.getSms()), new HashSet<>(value.getEmail()));
	}

	private void buildCheckboxes() {
		rows = new ArrayList<>();
		for (NotificationTypeGroup group : NotificationTypeGroup.values()) {
			List<CheckboxRow> groupRows = createGroupRows(group);

			layout.add(createGroupHeader(group, groupRows));
			layout.add(groupRows.toArray(new Component[] {}));

			rows.addAll(groupRows);
		}
	}

	private HorizontalLayout createGroupHeader(NotificationTypeGroup group, List<CheckboxRow> rows) {
		Span groupLabel = new Span(group.toString());
		groupLabel.addClassName(CssStyles.H3);

		Span tickForAllLAbel = new Span(I18nProperties.getString(Strings.checkboxSetTickAnAnswerForAll));
		tickForAllLAbel.addClassName(CssStyles.LABEL_ITALIC);

		Button tickAllButton = ButtonHelper.createButton(
			I18nProperties.getCaption(Captions.all),
			(e) -> rows.forEach(CheckboxRow::checkAll),
			CssStyles.BUTTON_LINK,
			CssStyles.BUTTON_COMPACT);
		Button tickSmsButton = ButtonHelper.createButton(
			I18nProperties.getCaption(Captions.userRoleNotificationTypeSms),
			(e) -> rows.forEach(CheckboxRow::checkAllSms),
			CssStyles.BUTTON_LINK,
			CssStyles.BUTTON_COMPACT);
		Button tickEmailButton = ButtonHelper.createButton(
			I18nProperties.getCaption(Captions.userRoleNotificationTypeEmail),
			(e) -> rows.forEach(CheckboxRow::checkAllEmail),
			CssStyles.BUTTON_LINK,
			CssStyles.BUTTON_COMPACT);

		HorizontalLayout buttonsLayout = new HorizontalLayout(tickForAllLAbel, tickAllButton, tickSmsButton, tickEmailButton);
		buttonsLayout.setWidthFull();
		buttonsLayout.setFlexGrow(1, tickForAllLAbel);

		HorizontalLayout headerLayout = new HorizontalLayout(groupLabel, buttonsLayout);
		headerLayout.setWidthFull();

		headerLayout.setFlexGrow(0.5, groupLabel);
		headerLayout.setFlexGrow(0.5, buttonsLayout);
		headerLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, buttonsLayout);

		return headerLayout;
	}

	private List<CheckboxRow> createGroupRows(NotificationTypeGroup group) {
		List<NotificationType> groupItems =
			Stream.of(NotificationType.values()).filter(n -> n.getNotificationTypeGroup() == group).collect(Collectors.toList());

		List<CheckboxRow> rows = new ArrayList<>();
		for (int i = 0, size = groupItems.size(); i < size; i += 2) {
			NotificationType item1 = groupItems.get(i);
			NotificationType item2 = i < size - 1 ? groupItems.get(i + 1) : null;

			rows.add(new CheckboxRow(item1, item2));
		}

		return rows;
	}

	private CheckBox createCheckbox(
		String captionTag,
		NotificationType item,
		Function<NotificationType, CheckboxSetItemDataSource<NotificationType>> dataSourceFactory) {
		CheckBox checkBox = new CheckBox(I18nProperties.getCaption(captionTag));
		dataSourceFactory.apply(item).bind(checkBox);

		return checkBox;
	}

	private CheckboxSetItemDataSource<NotificationType> createSmsCheckboxDataSource(NotificationType item2) {
		return createDataSource(item2, UserRoleDto.NotificationTypes::getSms);
	}

	private CheckboxSetItemDataSource<NotificationType> createEmailCheckboxDataSource(NotificationType item2) {
		return createDataSource(item2, UserRoleDto.NotificationTypes::getEmail);
	}

	private CheckboxSetItemDataSource<NotificationType> createDataSource(
		NotificationType type,
		Function<UserRoleDto.NotificationTypes, Set<NotificationType>> valueSetProvider) {
		return new CheckboxSetItemDataSource<>(type, v -> getValue() != null && valueSetProvider.apply(getValue()).contains(v), (c, v) -> {
			if (updatingCheckboxes) {
				return;
			}

			// Flow fires a value change only for a new value: change a copy and set it (was: the internal value changed
			// in place, then fireValueChange)
			UserRoleDto.NotificationTypes newValue =
				getValue() != null ? copyOf(getValue()) : UserRoleDto.NotificationTypes.of(new HashSet<>(), new HashSet<>());
			Set<NotificationType> valueSet = valueSetProvider.apply(newValue);
			if (c) {
				valueSet.add(v);
			} else {
				valueSet.remove(v);
			}
			setValue(newValue);
		});
	}

	private final class CheckboxRow extends HorizontalLayout {

		private static final long serialVersionUID = -3874252190392021250L;

		private final List<NotificationTypeCheckboxes> checkboxes = new ArrayList<>();

		public CheckboxRow(NotificationType leftGroup, NotificationType rightGroup) {
			setWidthFull();
			setPadding(false);

			NotificationTypeCheckboxes left = new NotificationTypeCheckboxes(leftGroup);
			add(left);
			setFlexGrow(0.45, left);

			HorizontalLayout spacer = new HorizontalLayout();
			add(spacer);
			setFlexGrow(0.01, spacer);

			checkboxes.add(left);

			Component rightComponent;
			if (rightGroup != null) {
				NotificationTypeCheckboxes right = new NotificationTypeCheckboxes(rightGroup);
				rightComponent = right;
				checkboxes.add(right);
			} else {
				// if there is no rightGroup, create an empty container to keep the first checkbox in the first half of tha space
				rightComponent = new HorizontalLayout();
			}

			add(rightComponent);
			setFlexGrow(0.54, rightComponent);
		}

		public void checkAll() {
			checkboxes.forEach(NotificationTypeCheckboxes::checkAll);
		}

		public void checkAllSms() {
			checkboxes.forEach(NotificationTypeCheckboxes::checkSms);
		}

		public void checkAllEmail() {
			checkboxes.forEach(NotificationTypeCheckboxes::checkEmail);
		}
	}

	private final class NotificationTypeCheckboxes extends HorizontalLayout {

		private static final long serialVersionUID = -4965656600618090731L;

		private final NotificationType type;
		private final CheckBox smsCb;
		private final CheckBox emailCb;

		public NotificationTypeCheckboxes(NotificationType type) {
			// was checkBox.setData(type)
			this.type = type;
			Span label = new Span(type.toString());
			label.setTitle(type.getDescription());
			smsCb = createCheckbox(Captions.userRoleNotificationTypeSms, type, UserRoleNotificationCheckboxSet.this::createSmsCheckboxDataSource);
			emailCb =
				createCheckbox(Captions.userRoleNotificationTypeEmail, type, UserRoleNotificationCheckboxSet.this::createEmailCheckboxDataSource);

			add(label, smsCb, emailCb);

			setWidthFull();
			setFlexGrow(1, label);
			setFlexGrow(0, smsCb);
			setFlexGrow(0, emailCb);
		}

		public void checkAll() {
			smsCb.setValue(true);
			emailCb.setValue(true);
		}

		public void checkSms() {
			smsCb.setValue(true);
		}

		public void checkEmail() {
			emailCb.setValue(true);
		}
	}
}
