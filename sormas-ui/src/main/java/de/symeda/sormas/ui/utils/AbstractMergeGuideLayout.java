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

package de.symeda.sormas.ui.utils;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;

public abstract class AbstractMergeGuideLayout extends VerticalLayout {

	private static final long serialVersionUID = -4739282529871338153L;

	public AbstractMergeGuideLayout() {

		setPadding(true);
		setSpacing(false);

		Span lblHeadingIntroduction = new Span(I18nProperties.getString(Strings.headingIntroduction));
		CssStyles.style(lblHeadingIntroduction, CssStyles.H2);
		add(lblHeadingIntroduction);
		Span lblIntroduction = VaadinUiUtil.createLabel(I18nProperties.getString(getInfoMergingExplanationMessage()), ContentMode.HTML);
		CssStyles.style(lblIntroduction, CssStyles.VSPACE_4);
		lblIntroduction.setWidthFull();
		add(lblIntroduction);

		Span lblHeadingHowTo = new Span(I18nProperties.getString(getHeadingHowToMergeMessage()));
		CssStyles.style(lblHeadingHowTo, CssStyles.H2);
		add(lblHeadingHowTo);
		Span lblHowTo = new Span(I18nProperties.getString(getInfoHowToMergeMessage()));
		add(lblHowTo);

		Span lblHeadingMerge = new Span(I18nProperties.getCaption(Captions.actionMerge));
		CssStyles.style(lblHeadingMerge, CssStyles.H3);
		add(lblHeadingMerge);
		Span lblMergeDescription = VaadinUiUtil.createLabel(I18nProperties.getString(getInfoMergingMergeDescriptionMessage()), ContentMode.HTML);
		lblMergeDescription.setWidthFull();
		add(lblMergeDescription);

		Span lblHeadingPick = new Span(I18nProperties.getCaption(Captions.actionPick));
		CssStyles.style(lblHeadingPick, CssStyles.H3);
		add(lblHeadingPick);
		Span lblPickDescription = new Span(I18nProperties.getString(getInfoMergingPickDescriptionMessage()));
		CssStyles.style(lblPickDescription, CssStyles.VSPACE_4);
		lblPickDescription.setWidthFull();
		add(lblPickDescription);

		Span lblHeadingHide = new Span(I18nProperties.getCaption(Captions.actionHide));
		CssStyles.style(lblHeadingHide, CssStyles.H3);
		add(lblHeadingHide);
		Span lblHideDescription = VaadinUiUtil.createLabel(I18nProperties.getString(getInfoMergingHideDescriptionMessage()), ContentMode.HTML);
		CssStyles.style(lblHideDescription, CssStyles.VSPACE_4);
		lblHideDescription.setWidthFull();
		add(lblHideDescription);

		Span lblHeadingTermsDefinition = new Span(I18nProperties.getString(Strings.headingExplanationOfTerms));
		CssStyles.style(lblHeadingTermsDefinition, CssStyles.H2);
		add(lblHeadingTermsDefinition);

		Span lblHeadingCompleteness = new Span(I18nProperties.getString(Strings.headingCompleteness));
		CssStyles.style(lblHeadingCompleteness, CssStyles.H3, CssStyles.VSPACE_TOP_5);
		add(lblHeadingCompleteness);
		Span lblCompletenessDescription = VaadinUiUtil.createLabel(
			I18nProperties.getString(getInfoCompletenessMessage()) + I18nProperties.getString(getInfoCompletenessMergeMessage()) + "</br></br>"
				+ I18nProperties.getString(getInfoCalculateCompletenessMessage()), ContentMode.HTML);
		lblCompletenessDescription.setWidthFull();
		add(lblCompletenessDescription);

		Span lblHeadingIgnoreRegion = new Span(I18nProperties.getCaption(Captions.caseFilterWithDifferentRegion));
		CssStyles.style(lblHeadingIgnoreRegion, CssStyles.H3);
		add(lblHeadingIgnoreRegion);
		Span lblIgnoreRegionDescription = VaadinUiUtil.createLabel(I18nProperties.getString(getInfoMergeIgnoreRegionMessage()), ContentMode.HTML);
		lblIgnoreRegionDescription.setWidthFull();
		add(lblIgnoreRegionDescription);
	}

	protected abstract String getInfoMergingExplanationMessage();

	protected abstract String getHeadingHowToMergeMessage();

	protected abstract String getInfoHowToMergeMessage();

	protected abstract String getInfoMergingMergeDescriptionMessage();

	protected abstract String getInfoMergingPickDescriptionMessage();

	protected abstract String getInfoMergingHideDescriptionMessage();

	protected abstract String getInfoCompletenessMessage();

	protected abstract String getInfoCompletenessMergeMessage();

	protected abstract String getInfoCalculateCompletenessMessage();

	protected abstract String getInfoMergeIgnoreRegionMessage();
}
