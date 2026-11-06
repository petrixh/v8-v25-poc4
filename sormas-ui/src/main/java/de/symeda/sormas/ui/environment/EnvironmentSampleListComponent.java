/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2023 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.environment;

import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.environment.EnvironmentDto;
import de.symeda.sormas.api.environment.EnvironmentReferenceDto;
import de.symeda.sormas.api.environment.environmentsample.EnvironmentSampleCriteria;
import de.symeda.sormas.api.environment.environmentsample.EnvironmentSampleIndexDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.sample.SpecimenCondition;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.PaginationList;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponent;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentField;

public class EnvironmentSampleListComponent extends SideComponent {

	private static final long serialVersionUID = -4946315430355463915L;

	private final EnvironmentSampleList environmentSampleList;

	public EnvironmentSampleListComponent(EnvironmentDto environment, boolean isEditAllowed, Consumer<Runnable> actionCallback) {
		super(I18nProperties.getString(Strings.entityEnvironmentSamples), actionCallback);

		if (UiUtil.permitted(isEditAllowed, UserRight.ENVIRONMENT_SAMPLE_CREATE)) {
			addCreateButton(
				I18nProperties.getCaption(Captions.environmentSampleNewSample),
				() -> ControllerProvider.getEnvironmentSampleController().create(environment, this::reload),
				UserRight.ENVIRONMENT_SAMPLE_CREATE);
		}

		environmentSampleList = new EnvironmentSampleList(environment.toReference(), isEditAllowed);
		add(environmentSampleList);
		environmentSampleList.reload();
	}

	private void reload() {
		environmentSampleList.reload();
	}

	class EnvironmentSampleList extends PaginationList<EnvironmentSampleIndexDto> {

		private static final long serialVersionUID = 8811945277567228049L;
		private static final int MAX_DISPLAYED_ENTRIES = 5;

		private final EnvironmentReferenceDto environmentRef;

		private final boolean isEditAllowed;

		public EnvironmentSampleList(EnvironmentReferenceDto environmentRef, boolean isEditAllowed) {
			super(MAX_DISPLAYED_ENTRIES);
			this.environmentRef = environmentRef;
			this.isEditAllowed = isEditAllowed;
		}

		public void reload() {
			List<EnvironmentSampleIndexDto> samples = FacadeProvider.getEnvironmentSampleFacade()
				.getIndexList(new EnvironmentSampleCriteria().withEnvironment(environmentRef), null, null, null);

			setEntries(samples);
			if (!samples.isEmpty()) {
				showPage(1);
			} else {
				listLayout.removeAll();
				updatePaginationLayout();
				Span noSamplesLabel = new Span(I18nProperties.getString(Strings.infoNoEnvironmentSamples));
				listLayout.add(noSamplesLabel);
			}

		}

		@Override
		protected void drawDisplayedEntries() {
			for (EnvironmentSampleIndexDto sample : getDisplayedEntries()) {
				EnvironmentSampleListEntry listEntry = new EnvironmentSampleListEntry(sample);

				String sampleUuid = sample.getUuid();
				if (UiUtil.permitted(isEditAllowed, UserRight.ENVIRONMENT_SAMPLE_EDIT)) {
					listEntry.addEditButton(
						"edit-environment-sample-" + sampleUuid,
						event -> ControllerProvider.getEnvironmentSampleController().navigateToSample(sampleUuid));
				} else {
					listEntry.addViewButton(
						"view-environment-sample" + sampleUuid,
						event -> ControllerProvider.getEnvironmentSampleController().navigateToSample(sampleUuid));
				}

				listEntry.setEnabled(isEditAllowed);

				listLayout.add(listEntry);
			}
		}
	}

	class EnvironmentSampleListEntry extends SideComponentField {

		private static final long serialVersionUID = -7096062233938610947L;

		public EnvironmentSampleListEntry(EnvironmentSampleIndexDto sampleIndex) {
			HorizontalLayout topLayout = new HorizontalLayout();
			topLayout.setWidthFull();
			topLayout.setPadding(false);
			topLayout.setSpacing(false);
			addComponentToField(topLayout);

			VerticalLayout topLeftLayout = new VerticalLayout();

			topLeftLayout.setPadding(false);
			topLeftLayout.setSpacing(false);

			Span materialLabel = new Span(DataHelper.toStringNullable(sampleIndex.getSampleMaterial()));
			CssStyles.style(materialLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
			materialLabel.setWidth("50%");
			topLeftLayout.add(materialLabel);

			if (sampleIndex.getSpecimenCondition() == SpecimenCondition.NOT_ADEQUATE) {
				Span resultLabel = new Span();
				CssStyles.style(resultLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);

				resultLabel.setText(DataHelper.toStringNullable(sampleIndex.getSpecimenCondition()));
				resultLabel.addClassName(CssStyles.LABEL_WARNING);

				topLeftLayout.add(resultLabel);
			}

			Span dispatchReceiveLabel = new Span();
			CssStyles.style(dispatchReceiveLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);

			if (sampleIndex.isReceived()) {
				dispatchReceiveLabel.setText(
					I18nProperties.getCaption(Captions.environmentSampleReceived) + " " + DateFormatHelper.formatDate(sampleIndex.getReceivalDate()));
			} else if (sampleIndex.isDispatched()) {
				dispatchReceiveLabel.setText(
					I18nProperties.getCaption(Captions.environmentSampleShipped) + " "
						+ DateFormatHelper.formatDate((sampleIndex.getDispatchDate())));
			} else {
				dispatchReceiveLabel.setText(I18nProperties.getCaption(Captions.environmentSampleNotShipped));
			}

			topLeftLayout.add(dispatchReceiveLabel);

			Span dateTimeLabel = new Span(
				I18nProperties.getPrefixCaption(EnvironmentSampleIndexDto.I18N_PREFIX, EnvironmentSampleIndexDto.SAMPLE_DATE_TIME) + ": "
					+ DateFormatHelper.formatDate(sampleIndex.getSampleDateTime()));
			topLeftLayout.add(dateTimeLabel);

			Span labLabel = new Span(DataHelper.toStringNullable(sampleIndex.getLaboratory()));
			topLeftLayout.add(labLabel);

			Span testCountLabel = new Span(
				I18nProperties.getPrefixCaption(EnvironmentSampleIndexDto.I18N_PREFIX, EnvironmentSampleIndexDto.NUMBER_OF_TESTS) + ": "
					+ sampleIndex.getNumberOfTests());
			topLeftLayout.add(testCountLabel);

			if (sampleIndex.getNumberOfTests() > 0) {
				VerticalLayout latestTestLayout = new VerticalLayout();
				latestTestLayout.setPadding(false);
				latestTestLayout.setSpacing(false);

				Span heading = new Span(I18nProperties.getCaption(Captions.latestPathogenTest));
				CssStyles.style(heading, CssStyles.LABEL_BOLD);

				Span testDate = new Span(sampleIndex.getLatestPathogenTestResult().toString());
				latestTestLayout.add(heading, testDate);
				topLeftLayout.add(latestTestLayout);
			}

			topLayout.add(topLeftLayout);
			topLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, topLeftLayout);
		}
	}
}
