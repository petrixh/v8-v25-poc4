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
package de.symeda.sormas.ui.samples.pathogentestlink;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.DiseaseHelper;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.sample.PathogenTestDto;
import de.symeda.sormas.api.sample.PathogenTestResultType;
import de.symeda.sormas.api.sample.PathogenTestType;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentField;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

@SuppressWarnings("serial")
public class PathogenTestListEntry extends SideComponentField {

	private final PathogenTestDto pathogenTest;

	public PathogenTestListEntry(PathogenTestDto pathogenTest, boolean showTestResultText) {

		this.pathogenTest = pathogenTest;

		HorizontalLayout topLabelLayout = new HorizontalLayout();
		topLabelLayout.setSpacing(false);
		topLabelLayout.setPadding(false);
		topLabelLayout.setWidthFull();
		addComponentToField(topLabelLayout);
		Span labelTopLeft =
			new Span(PathogenTestType.toString(pathogenTest.getTestType(), pathogenTest.getTestTypeText(), pathogenTest.getTestedDisease()));
		CssStyles.style(labelTopLeft, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
		topLabelLayout.add(labelTopLeft);
		// was equal slots with the icons aligned right in theirs: the label takes the space, the icons sit at the right
		topLabelLayout.setFlexGrow(1, labelTopLeft);

		if (Boolean.TRUE.equals(pathogenTest.getTestResultVerified())) {
			addTopRightIcon(topLabelLayout, VaadinIcon.CHECK_CIRCLE, PathogenTestDto.TEST_RESULT_VERIFIED);
		}

		// At-a-glance lab indicators: reference laboratory, retest, and result details
		// (the latter only when the full result text is not already shown below).
		if (shouldShowRefLabIcon(pathogenTest)) {
			addTopRightIcon(topLabelLayout, VaadinIcon.INSTITUTION, PathogenTestDto.PERFORMED_BY_REFERENCE_LABORATORY);
		}
		if (shouldShowRetestIcon(pathogenTest)) {
			addTopRightIcon(topLabelLayout, VaadinIcon.REFRESH, PathogenTestDto.RETEST_REQUESTED);
		}
		if (shouldShowResultDetailsIcon(pathogenTest, showTestResultText)) {
			addTopRightIcon(topLabelLayout, VaadinIcon.INFO_CIRCLE, PathogenTestDto.TEST_RESULT_TEXT);
		}

		if (showTestResultText && !DataHelper.isNullOrEmpty(pathogenTest.getTestResultText())) {
			Span resultTextLabel = new Span(StringUtils.abbreviate(pathogenTest.getTestResultText(), 125));
			resultTextLabel.setTitle(pathogenTest.getTestResultText());
			resultTextLabel.setWidthFull();
			addComponentToField(resultTextLabel);
		}

		HorizontalLayout middleLabelLayout = new HorizontalLayout();
		middleLabelLayout.setSpacing(false);
		middleLabelLayout.setPadding(false);
		middleLabelLayout.setWidthFull();
		addComponentToField(middleLabelLayout);

		Span labelMiddleLeft = new Span(getDiseaseOrPathogenCaption(pathogenTest));
		middleLabelLayout.add(labelMiddleLeft);

		Span labelMiddleRight = new Span(DateFormatHelper.formatLocalDateTime(pathogenTest.getTestDateTime()));
		labelMiddleRight.addClassName(CssStyles.ALIGN_RIGHT);
		middleLabelLayout.add(labelMiddleRight);
		// was Alignment.TOP_RIGHT for the right label in its half
		middleLabelLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

		if (pathogenTest.getTestedDiseaseVariant() != null || pathogenTest.getCqValue() != null) {
			HorizontalLayout bottomLabelLayout = new HorizontalLayout();
			bottomLabelLayout.setSpacing(false);
			bottomLabelLayout.setPadding(false);
			bottomLabelLayout.setWidthFull();
			addComponentToField(bottomLabelLayout);

			if (pathogenTest.getTestedDiseaseVariant() != null) {
				Span labelBottomLeft = new Span(pathogenTest.getTestedDiseaseVariant().toString());
				CssStyles.style(labelBottomLeft, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE, CssStyles.LABEL_CRITICAL);
				bottomLabelLayout.add(labelBottomLeft);
			}

			if (pathogenTest.getCqValue() != null) {
				Span labelBottomRight = new Span(
					I18nProperties.getPrefixCaption(PathogenTestDto.I18N_PREFIX, PathogenTestDto.CQ_VALUE) + ": " + pathogenTest.getCqValue());
				labelBottomRight.addClassName(CssStyles.ALIGN_RIGHT);
				bottomLabelLayout.add(labelBottomRight);
				// was Alignment.TOP_RIGHT: at the right also when it is the only label in the row
				labelBottomRight.getStyle().set("margin-left", "auto");
			}
		}

		PathogenTestType testType = pathogenTest.getTestType();

		// Quantitative result (issue #13952): combine whichever value-type fields the method populated.
		String quantitativeResult = formatQuantitativeResult(pathogenTest);
		// Antibiotic susceptibility has no qualitative result, so suppress the result label regardless of the stored
		// value — old records may carry a stale POSITIVE.
		// Other methods that have no QUALITATIVE in their @ResultValueTypeRel (Western Blot, Bacterial Culture,
		// typing methods, ...) keep their qualitative cue.
		boolean astWithoutQualitativeResult = testType == PathogenTestType.ANTIBIOTIC_SUSCEPTIBILITY;
		boolean suppressResultLabel =
			astWithoutQualitativeResult || (pathogenTest.getTestResult() == PathogenTestResultType.NOT_APPLICABLE && quantitativeResult != null);
		if (!suppressResultLabel) {
			Object resultText = determineSideComponentVariant(pathogenTest);
			Span labelResult = new Span(DataHelper.toStringNullable(resultText == null ? pathogenTest.getTestResult() : resultText));
			CssStyles.style(labelResult, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE);
			if (pathogenTest.getTestResult() == PathogenTestResultType.POSITIVE) {
				CssStyles.style(labelResult, CssStyles.LABEL_CRITICAL);
			} else {
				CssStyles.style(labelResult, CssStyles.LABEL_WARNING);
			}
			addComponentToField(labelResult);
		}

		if (quantitativeResult != null) {
			Span quantitativeLabel = new Span(StringUtils.abbreviate(quantitativeResult, 125));
			quantitativeLabel.setTitle(quantitativeResult);
			CssStyles.style(quantitativeLabel, CssStyles.LABEL_BOLD, CssStyles.LABEL_UPPERCASE, CssStyles.LABEL_WARNING);
			quantitativeLabel.setWidthFull();
			addComponentToField(quantitativeLabel);
		}

		if (pathogenTest.getTestedDisease() == Disease.TUBERCULOSIS) {
			if (testType == PathogenTestType.PCR_RT_PCR) {
				Span pcrRifampicinTextLabel = new Span(
					I18nProperties.getPrefixCaption(PathogenTestDto.I18N_PREFIX, PathogenTestDto.RIFAMPICIN_RESISTANT) + ": " + StringUtils
						.abbreviate((pathogenTest.getRifampicinResistant() != null ? pathogenTest.getRifampicinResistant().toString() : ""), 125));
				pcrRifampicinTextLabel.setWidthFull();
				addComponentToField(pcrRifampicinTextLabel);

				Span pcrIsoniazidTextLabel = new Span(
					I18nProperties.getPrefixCaption(PathogenTestDto.I18N_PREFIX, PathogenTestDto.ISONIAZID_RESISTANT) + ": " + StringUtils
						.abbreviate((pathogenTest.getIsoniazidResistant() != null ? pathogenTest.getIsoniazidResistant().toString() : ""), 125));
				pcrIsoniazidTextLabel.setWidthFull();
				addComponentToField(pcrIsoniazidTextLabel);
			}
		}
	}

	/**
	 * @return a short label combining every quantitative result field the test recorded (Western Blot
	 *         interpretation, detected flag, smear grade, numeric value with optional unit), or {@code null}
	 *         when none is set. A method can populate several of these at once, so all are shown.
	 */
	@Nullable
	static String formatQuantitativeResult(PathogenTestDto test) {
		List<String> parts = new ArrayList<>();
		if (test.getWesternBlotInterpretation() != null) {
			parts.add(test.getWesternBlotInterpretation().toString());
		}
		if (test.getSmearGrade() != null) {
			parts.add(test.getSmearGrade().toString());
		}
		if (test.getQuantitativeBoolean() != null) {
			parts.add(test.getQuantitativeBoolean().toString());
		}
		if (test.getQuantitativeValue() != null) {
			String unit = test.getQuantitativeUnit();
			parts.add(DataHelper.isNullOrEmpty(unit) ? test.getQuantitativeValue().toString() : test.getQuantitativeValue() + " " + unit);
		}
		return parts.isEmpty() ? null : String.join(": ", parts);
	}

	private static void addTopRightIcon(HorizontalLayout topLabelLayout, VaadinIcon icon, String captionProperty) {
		Span iconLabel = VaadinUiUtil.createLabel(HtmlRenderer.iconHtml(icon), ContentMode.HTML);
		iconLabel.setSizeUndefined();
		iconLabel.addClassNames(CssStyles.LABEL_LARGE, CssStyles.HSPACE_LEFT_4);
		iconLabel.setTitle(I18nProperties.getPrefixCaption(PathogenTestDto.I18N_PREFIX, captionProperty));
		topLabelLayout.add(iconLabel);
	}

	static boolean shouldShowRefLabIcon(PathogenTestDto test) {
		return Boolean.TRUE.equals(test.getPerformedByReferenceLaboratory());
	}

	static boolean shouldShowRetestIcon(PathogenTestDto test) {
		return Boolean.TRUE.equals(test.getRetestRequested());
	}

	/**
	 * The result-details cue is only shown in the compact view (when the full result text is not rendered),
	 * to avoid a redundant icon next to text that is already visible.
	 */
	static boolean shouldShowResultDetailsIcon(PathogenTestDto test, boolean showTestResultText) {
		return !showTestResultText && !DataHelper.isNullOrEmpty(test.getTestResultText());
	}

	@Nullable
	private static String getDiseaseOrPathogenCaption(PathogenTestDto pathogenTest) {
		final String diseaseOrPathogen;
		if (pathogenTest.getTestedDisease() != null) {
			diseaseOrPathogen = DiseaseHelper.toString(pathogenTest.getTestedDisease(), pathogenTest.getTestedDiseaseDetails());
		} else if (pathogenTest.getTestedPathogen() != null) {
			diseaseOrPathogen = DataHelper.getPathogenString(pathogenTest.getTestedPathogen(), pathogenTest.getTestedPathogenDetails());
		} else {
			diseaseOrPathogen = null;
		}
		return diseaseOrPathogen;
	}

	public PathogenTestDto getPathogenTest() {
		return pathogenTest;
	}
}
