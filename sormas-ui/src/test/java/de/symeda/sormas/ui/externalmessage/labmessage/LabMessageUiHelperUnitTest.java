/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2021 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.externalmessage.labmessage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.dom.Element;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.caze.CaseReferenceDto;
import de.symeda.sormas.api.sample.SampleDto;
import de.symeda.sormas.api.user.UserReferenceDto;
import de.symeda.sormas.ui.samples.humansample.SampleCreateForm;
import de.symeda.sormas.ui.utils.AbstractForm.BeanFieldGroup;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent;

public class LabMessageUiHelperUnitTest {

	@Test
	public void testEstablishFinalCommitButtons() {

		// a mocked Flow component has no element, and the wrapper puts the form in a Scroller: give the mocks an element
		// (a mocked Vaadin 8 component could be added to a layout as it was)
		Mockito.mockConstruction(SampleCreateForm.class, (mock, context) -> when(mock.getElement()).thenReturn(new Element("div")));

		UserReferenceDto user = new UserReferenceDto();
		CaseReferenceDto caze = new CaseReferenceDto();
		SampleDto sample = SampleDto.build(user, caze);

		// Test for last sample
		SampleCreateForm createForm = new SampleCreateForm(Disease.CORONAVIRUS);
		when(createForm.getFieldGroup()).thenReturn(new BeanFieldGroup(SampleDto.class));

		CommitDiscardWrapperComponent<SampleCreateForm> sampleComponent =
			new CommitDiscardWrapperComponent<>(createForm, true, createForm.getFieldGroup());
		when(sampleComponent.getWrappedComponent().getValue()).thenReturn(sample);

		LabMessageUiHelper.establishCommitButtons(sampleComponent, true);

		HorizontalLayout buttonsPanel = sampleComponent.getButtonsPanel();
		Button saveAndOpenEntryButton = (Button) buttonsPanel.getComponentAt(buttonsPanel.getComponentCount() - 2);

		// was getStyleName(): in Flow the style names are the class names and the theme variants
		assertThat(saveAndOpenEntryButton.getClassName(), equalTo(sampleComponent.getCommitButton().getClassName()));
		assertThat(
			new HashSet<>(saveAndOpenEntryButton.getThemeNames()),
			equalTo(new HashSet<>(sampleComponent.getCommitButton().getThemeNames())));
		assertThat(saveAndOpenEntryButton.getId().orElse(null), equalTo("saveAndOpenEntryButton"));

		// Both commit buttons should do the same, except each one should trigger a different navigation 
		assertThat(
			ComponentUtil.getListeners(saveAndOpenEntryButton, ClickEvent.class).size(),
			equalTo(ComponentUtil.getListeners(sampleComponent.getCommitButton(), ClickEvent.class).size()));

		// Test for non-last sample
		createForm = new SampleCreateForm(Disease.CORONAVIRUS);
		when(createForm.getFieldGroup()).thenReturn(new BeanFieldGroup(SampleDto.class));

		sampleComponent = new CommitDiscardWrapperComponent<>(createForm, true, createForm.getFieldGroup());
		when(sampleComponent.getWrappedComponent().getValue()).thenReturn(sample);

		LabMessageUiHelper.establishCommitButtons(sampleComponent, false);
		buttonsPanel = sampleComponent.getButtonsPanel();

		for (int i = 0; i < buttonsPanel.getComponentCount(); i++) {
			assertThat(buttonsPanel.getComponentAt(i).getId().orElse(null), not(equalTo("saveAndOpenEntryButton")));
		}
	}

}
