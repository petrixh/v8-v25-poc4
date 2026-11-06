/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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
package de.symeda.sormas.ui.samples.sampleLink;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.Descriptions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.sample.AdditionalTestingStatus;
import de.symeda.sormas.api.sample.PathogenTestDto;
import de.symeda.sormas.api.sample.PathogenTestResultType;
import de.symeda.sormas.api.sample.SampleListEntryDto;
import de.symeda.sormas.api.sample.SamplePurpose;
import de.symeda.sormas.api.sample.SamplingReason;
import de.symeda.sormas.api.sample.SpecimenCondition;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentField;

@SuppressWarnings("serial")
public class SampleListEntry extends SideComponentField {

	private final SampleListEntryDto sampleListEntryDto;
	private Button associatedLabMessagesButton;

	public SampleListEntry(SampleListEntryDto sampleListEntryDto) {

		this.sampleListEntryDto = sampleListEntryDto;

		HorizontalLayout topLayout = new HorizontalLayout();
		topLayout.setWidthFull();
		topLayout.setPadding(false);
		topLayout.setSpacing(false);
		addComponentToField(topLayout);

		VerticalLayout topLeftLayout = new VerticalLayout();
		{
			topLeftLayout.setPadding(false);
			topLeftLayout.setSpacing(false);

			Span materialLabel = new Span(DataHelper.toStringNullable(sampleListEntryDto.getSampleMaterial()));
			CssStyles.style(materialLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
			materialLabel.setWidth(50, Unit.PERCENTAGE);
			topLeftLayout.add(materialLabel);

			Span resultLabel = new Span();
			CssStyles.style(resultLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
			if (sampleListEntryDto.getPathogenTestResult() != null) {
				resultLabel.setText(DataHelper.toStringNullable(sampleListEntryDto.getPathogenTestResult()));
				if (sampleListEntryDto.getPathogenTestResult() == PathogenTestResultType.POSITIVE) {
					resultLabel.addClassName(CssStyles.LABEL_CRITICAL);
				} else if (sampleListEntryDto.getPathogenTestResult() == PathogenTestResultType.INDETERMINATE) {
					resultLabel.addClassName(CssStyles.LABEL_WARNING);
				}
			} else if (sampleListEntryDto.getSpecimenCondition() == SpecimenCondition.NOT_ADEQUATE) {
				resultLabel.setText(DataHelper.toStringNullable(sampleListEntryDto.getSpecimenCondition()));
				resultLabel.addClassName(CssStyles.LABEL_WARNING);
			}
			topLeftLayout.add(resultLabel);

			Span referredLabel = new Span();
			CssStyles.style(referredLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
			if (sampleListEntryDto.isReferred()) {
				referredLabel.setText(I18nProperties.getCaption(Captions.sampleReferredShort));
				referredLabel.addClassName(CssStyles.LABEL_NOT);
			} else if (sampleListEntryDto.getSamplePurpose() != SamplePurpose.INTERNAL) {
				if (sampleListEntryDto.isReceived()) {
					referredLabel.setText(
						I18nProperties.getCaption(Captions.sampleReceived) + " " + DateFormatHelper.formatDate(sampleListEntryDto.getReceivedDate()));
				} else if (sampleListEntryDto.isShipped()) {
					referredLabel.setText(
						I18nProperties.getCaption(Captions.sampleShipped) + " "
							+ DateFormatHelper.formatDate((sampleListEntryDto.getShipmentDate())));
				} else {
					referredLabel.setText(I18nProperties.getCaption(Captions.sampleNotShippedLong));
				}
			}
			topLeftLayout.add(referredLabel);

			Span dateTimeLabel = new Span(
				I18nProperties.getPrefixCaption(SampleListEntryDto.I18N_PREFIX, SampleListEntryDto.SAMPLE_DATE_TIME) + ": "
					+ DateFormatHelper.formatDate(sampleListEntryDto.getSampleDateTime()));
			topLeftLayout.add(dateTimeLabel);

			if (sampleListEntryDto.getSamplePurpose() == SamplePurpose.INTERNAL) {
				Span purposeLabel = new Span(SamplePurpose.INTERNAL.toString());
				topLeftLayout.add(purposeLabel);
			} else {
				Span labLabel = new Span(DataHelper.toStringNullable(sampleListEntryDto.getLab()));
				topLeftLayout.add(labLabel);
			}

			SamplingReason samplingReason = sampleListEntryDto.getSamplingReason();
			if (samplingReason != null) {
				String samplingReasonCaption = samplingReason.toString();
				if (samplingReason == SamplingReason.OTHER_REASON && sampleListEntryDto.getSamplingReasonDetails() != null) {
					samplingReasonCaption = sampleListEntryDto.getSamplingReasonDetails();
				}
				Span samplingReasonLabel = new Span(
					I18nProperties.getPrefixCaption(SampleListEntryDto.I18N_PREFIX, SampleListEntryDto.SAMPLING_REASON) + ": "
						+ samplingReasonCaption);
				topLeftLayout.add(samplingReasonLabel);
			}

			Span testCountLabel = new Span(
				I18nProperties.getPrefixCaption(SampleListEntryDto.I18N_PREFIX, SampleListEntryDto.PATHOGEN_TEST_COUNT) + ": "
					+ sampleListEntryDto.getPathogenTestCount());
			topLeftLayout.add(testCountLabel);

			if (sampleListEntryDto.getPathogenTestCount() > 0) {
				VerticalLayout latestTestLayout = new VerticalLayout();
				latestTestLayout.setPadding(false);
				latestTestLayout.setSpacing(false);

				Span heading = new Span(I18nProperties.getCaption(Captions.latestPathogenTest));
				CssStyles.style(heading, CssStyles.LABEL_BOLD);
				PathogenTestDto latestTest = FacadeProvider.getPathogenTestFacade().getLatestPathogenTest(sampleListEntryDto.getUuid());
				Span testDate = new Span(
					I18nProperties.getPrefixCaption(PathogenTestDto.I18N_PREFIX, PathogenTestDto.TEST_DATE_TIME) + ": "
						+ DateFormatHelper.formatLocalDateTime(latestTest.getTestDateTime()));
				HorizontalLayout bottomLayout = new HorizontalLayout();
				Span testType = new Span(latestTest.getTestType().toString());
				bottomLayout.add(testType);
				if (latestTest.getCqValue() != null) {
					Span cqValue = new Span(
						I18nProperties.getPrefixCaption(PathogenTestDto.I18N_PREFIX, PathogenTestDto.CQ_VALUE) + ": " + latestTest.getCqValue());
					cqValue.addClassName(CssStyles.ALIGN_RIGHT);
					bottomLayout.add(cqValue);
				}
				// Need to include the variants
				String variant = determineSideComponentVariant(latestTest);

				if (StringUtils.isNotBlank(variant)) {
					Span variantLabel = new Span(DataHelper.toStringNullable(variant == null ? latestTest.getTestResult() : variant));
					if (latestTest.getTestResult() == PathogenTestResultType.POSITIVE) {
						CssStyles.style(variantLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_CRITICAL);
					} else {
						CssStyles.style(variantLabel, CssStyles.LABEL_WARNING);
					}
					bottomLayout.add(variantLabel);
				}
				latestTestLayout.add(heading, testDate, bottomLayout);
				topLeftLayout.add(latestTestLayout);
			}
		}

		topLayout.add(topLeftLayout);
		topLayout.setVerticalComponentAlignment(FlexComponent.Alignment.START, topLeftLayout);

		if (sampleListEntryDto.getAdditionalTestingStatus() != AdditionalTestingStatus.NOT_REQUESTED
			&& UiUtil.permitted(FeatureType.ADDITIONAL_TESTS, UserRight.ADDITIONAL_TEST_VIEW)) {
			Span labelAdditionalTests = new Span(
				I18nProperties.getString(Strings.entityAdditionalTests) + " "
					+ sampleListEntryDto.getAdditionalTestingStatus().toString().toLowerCase());
			addComponentToField(labelAdditionalTests);
		}
	}

	public void addAssociatedLabMessagesListener(ComponentEventListener<ClickEvent<Button>> associatedLabMessagesClickListener) {
		if (associatedLabMessagesButton == null) {
			associatedLabMessagesButton = ButtonHelper.createIconButtonWithCaption(
				"see-associated-lab-messages-" + sampleListEntryDto.getUuid(),
				null,
				VaadinIcon.NOTEBOOK,
				associatedLabMessagesClickListener,
				CssStyles.BUTTON_LINK,
				CssStyles.BUTTON_COMPACT);

			add(associatedLabMessagesButton);
			setVerticalComponentAlignment(FlexComponent.Alignment.START, associatedLabMessagesButton);
			setFlexGrow(0, associatedLabMessagesButton);
			associatedLabMessagesButton.setTooltipText(I18nProperties.getDescription(Descriptions.Sample_associatedLabMessages));
		}
	}

	public SampleListEntryDto getSampleListEntryDto() {
		return sampleListEntryDto;
	}
}
