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

package de.symeda.sormas.ui.samples;

import java.util.Optional;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.sample.PathogenTestDto;
import de.symeda.sormas.ui.samples.pathogentestlink.PathogenTestListEntry;
import de.symeda.sormas.ui.utils.AbstractForm.BeanFieldGroup;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent;

public class CollapsiblePathogenTestForm extends VerticalLayout {

	private static final long serialVersionUID = -7352963874627364843L;

	private CommitDiscardWrapperComponent<PathogenTestForm> commitDiscardForm;

	private VerticalLayout expandedLayout;
	private VerticalLayout collapsedLayout;

	private Runnable deleteHandler;

	private boolean deleteOnCancel;

	public CollapsiblePathogenTestForm(PathogenTestForm pathogenTestForm, boolean initialExpanded, boolean deleteOnCancel) {
		this.deleteOnCancel = deleteOnCancel;

		setSpacing(false);
		setPadding(false);

		commitDiscardForm = new CommitDiscardWrapperComponent<>(pathogenTestForm, pathogenTestForm.getFieldGroup());
		commitDiscardForm.setPadding(false);
		commitDiscardForm.setSpacing(false);

		commitDiscardForm.getCommitButton().setText(I18nProperties.getCaption(Captions.actionDone));
		commitDiscardForm.getDiscardButton().setText(I18nProperties.getCaption(Captions.actionCancel));
		commitDiscardForm.getDiscardButton().setId("cancel");
		// was Alignment.BOTTOM_LEFT in the discard button's expanding slot: Cancel at the left, Done at the right
		Button discardButton = commitDiscardForm.getDiscardButton();
		discardButton.getStyle().remove("margin-left");
		discardButton.getStyle().set("margin-right", "auto");
		commitDiscardForm.getButtonsPanel().setVerticalComponentAlignment(FlexComponent.Alignment.END, discardButton);
		commitDiscardForm.addDoneListener(this::collapse);

		expandedLayout = new VerticalLayout(commitDiscardForm);
		expandedLayout.setSpacing(false);
		expandedLayout.setPadding(false);

		collapsedLayout = new VerticalLayout();
		collapsedLayout.setSpacing(false);
		collapsedLayout.setPadding(false);
		collapsedLayout.setWidth(pathogenTestForm.getWidth());

		this.add(collapsedLayout, expandedLayout);

		resetCollapsedLayout();
		pathogenTestForm.getFieldGroup().addCommitHandler(new BeanFieldGroup.CommitHandler() {

			@Override
			public void preCommit(BeanFieldGroup.CommitEvent commitEvent) throws BeanFieldGroup.CommitException {

			}

			@Override
			public void postCommit(BeanFieldGroup.CommitEvent commitEvent) throws BeanFieldGroup.CommitException {
				resetCollapsedLayout();
			}
		});

		if (initialExpanded) {
			expand();
		} else {
			collapse();
		}
	}

	private void resetCollapsedLayout() {
		PathogenTestDto pathogenTest = Optional.ofNullable(getValue()).orElse(new PathogenTestDto());

		collapsedLayout.removeAll();
		PathogenTestListEntry pathogenTestEntry = new PathogenTestListEntry(pathogenTest, false);
		collapsedLayout.add(pathogenTestEntry);

		pathogenTestEntry.addActionButton(pathogenTest.getUuid(), e -> {
			expand();
		}, true);

		if (deleteHandler != null) {
			pathogenTestEntry.addDeleteButton(pathogenTest.getUuid(), (e) -> {
				deleteHandler.run();
			});
		}
	}

	private void expand() {
		collapsedLayout.setVisible(false);
		expandedLayout.setVisible(true);
	}

	private void collapse() {
		collapsedLayout.setVisible(true);
		expandedLayout.setVisible(false);
	}

	public PathogenTestDto getValue() {
		return commitDiscardForm.getWrappedComponent().getValue();
	}

	public void setDeleteHandler(Runnable deleteHandler) {
		this.deleteHandler = deleteHandler;

		resetCollapsedLayout();

		commitDiscardForm.addDiscardListener(() -> {
			if (deleteOnCancel) {
				deleteHandler.run();
			}
		});

		commitDiscardForm.addCommitListener(() -> {
			deleteOnCancel = false;
		});
	}
}
