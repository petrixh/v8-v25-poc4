/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2021 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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
package de.symeda.sormas.ui.login;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.opencsv.CSVWriter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.utils.CSVUtils;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.StreamSourceDownloadHandler;

public class ChangeDefaultPasswordsWindow extends Dialog {

	public ChangeDefaultPasswordsWindow(Runnable onContinue, List<UserDto> otherUsersWithDefaultPassword) {
		setHeaderTitle(" " + I18nProperties.getString(Strings.headingSecurityAlert));
		getHeader().add(VaadinIcon.WARNING.create());
		setWidth("40%");
		final VerticalLayout content = new VerticalLayout();
		content.setPadding(true);
		content.setWidthFull();

		Span introductionLabel = new Span(I18nProperties.getString(Strings.DefaultPassword_otherUsersIntroduction));
		introductionLabel.setWidthFull();
		content.add(introductionLabel);

		Span actionLabel = new Span(I18nProperties.getString(Strings.DefaultPassword_otherUsersAction));
		actionLabel.setWidthFull();
		content.add(actionLabel);

		List<MutablePair<UserDto, String>> usersWithNewPassword =
			otherUsersWithDefaultPassword.stream().map(u -> new MutablePair<UserDto, String>(u, null)).collect(Collectors.toList());
		Grid<MutablePair<UserDto, String>> userGrid = new Grid<>();
		userGrid.setWidthFull();
		userGrid.setHeight("250px");
		@SuppressWarnings("unchecked")
		GridMultiSelectionModel<MutablePair<UserDto, String>> selectionModel =
			(GridMultiSelectionModel<MutablePair<UserDto, String>>) userGrid.setSelectionMode(Grid.SelectionMode.MULTI);
		userGrid.setItems(usersWithNewPassword);
		userGrid.addComponentColumn(element -> new Span(element.getLeft().getUserName()))
			.setHeader(I18nProperties.getCaption(Captions.User_userName));
		userGrid.addComponentColumn(element -> new Span(getNewPasswordOrUnchangedText(element.getRight())))
			.setHeader(I18nProperties.getString(Strings.DefaultPassword_newPassword));
		selectionModel.selectAll();
		content.add(userGrid);

		Span newPasswordSetHintsLabel = new Span(I18nProperties.getString(Strings.DefaultPassword_otherUsersNewPasswordSetHints));
		newPasswordSetHintsLabel.setWidthFull();
		CssStyles.style(newPasswordSetHintsLabel, CssStyles.LABEL_POSITIVE);
		newPasswordSetHintsLabel.setVisible(false);

		Button continueButton = ButtonHelper.createButton(Captions.actionRemindMeLater, clickEvent -> {
			close();
			onContinue.run();
		}, CssStyles.BUTTON_PRIMARY);

		// was an on-demand FileDownloader: file name and content are made when the user clicks, not cached
		StreamSourceDownloadHandler csvDownload = new StreamSourceDownloadHandler(() -> {
			StringWriter stringWriter = new StringWriter();
			CSVWriter csvWriter = CSVUtils.createCSVWriter(stringWriter, ',');
			csvWriter.writeNext(
				new String[] {
					I18nProperties.getCaption(Captions.User_userName),
					I18nProperties.getString(Strings.DefaultPassword_newPassword) });
			for (MutablePair<UserDto, String> item : usersWithNewPassword) {
				if (item.getRight() != null) {
					csvWriter.writeNext(
						new String[] {
							item.getLeft().getUserName(),
							getNewPasswordOrUnchangedText(item.getRight()) });
				}

			}
			return new ByteArrayInputStream(stringWriter.toString().getBytes(StandardCharsets.UTF_8));
		}, "sormas-defaultusers.csv");

		Button exportToCsvButton = ButtonHelper.createButton(Captions.export, clickEvent -> {
		});
		exportToCsvButton.setEnabled(false);
		Anchor exportToCsvLink = DownloadUtil.createDownloadLink(exportToCsvButton, csvDownload);
		// a disabled button inside a link would still start the download
		exportToCsvLink.setEnabled(false);

		Button generateNewPasswordsButton = ButtonHelper.createButton(Captions.actionGenerateNewPasswords, clickEvent -> {
			selectionModel.getSelectedItems().forEach(item -> {
				item.setRight(FacadeProvider.getUserFacade().resetPassword(item.getLeft().getUuid()));
				userGrid.getDataProvider().refreshItem(item);
			});
			selectionModel.deselectAll();
			newPasswordSetHintsLabel.setVisible(true);
			continueButton.setText(I18nProperties.getCaption(Captions.actionContinue));
			exportToCsvButton.setEnabled(true);
			exportToCsvLink.setEnabled(true);
		});

		HorizontalLayout buttonBarLayout = new HorizontalLayout();
		buttonBarLayout.add(generateNewPasswordsButton);
		buttonBarLayout.add(exportToCsvLink);
		content.add(buttonBarLayout);
		content.add(newPasswordSetHintsLabel);
		content.add(continueButton);

		setModal(true);
		setResizable(false);
		// not closable, as in Vaadin 8: no close button, Escape and a click outside do nothing
		setCloseOnEsc(false);
		setCloseOnOutsideClick(false);
		add(content);
		setDraggable(false);
	}

	private String getNewPasswordOrUnchangedText(String password) {
		if (password != null) {
			return password;
		} else {
			return I18nProperties.getString(Strings.DefaultPassword_unchanged);
		}
	}

	/**
	 * internal used mutable pair
	 * equals and hashcode method are only dependant on the left value ("key"), but not on the right one
	 * 
	 * @param <L>
	 *            the class of the left value (e.g. key)
	 * @param <R>
	 *            the class of the right value (e.g. value)
	 */
	private static class MutablePair<L, R> {

		private L left;
		private R right;

		public MutablePair(L left, R right) {
			this.left = left;
			this.right = right;
		}

		public L getLeft() {
			return left;
		}

		public void setLeft(L left) {
			this.left = left;
		}

		public R getRight() {
			return right;
		}

		public void setRight(R right) {
			this.right = right;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			MutablePair<?, ?> that = (MutablePair<?, ?>) o;
			return Objects.equals(left, that.left);
		}

		@Override
		public int hashCode() {
			return Objects.hash(left);
		}
	}
}
