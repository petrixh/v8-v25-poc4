/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2024 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.dashboard.adverseeventsfollowingimmunization.components;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class AefiCountTilesComponent<T extends Enum<?>> extends VerticalLayout {

	private static final long serialVersionUID = 3342746874277667267L;
	private final Class<T> groupType;
	private final Function<T, String> tileBackgroundFactory;
	private boolean withPercentage;
	private String groupLabelStyle;

	private final Span title;
	private Span infoIcon;
	private final HorizontalLayout countsLayout;
	private String[] titleStyleNames = new String[] {
		CssStyles.H3 };

	public AefiCountTilesComponent(
		Class<T> groupType,
		String titleCaption,
		Function<T, String> tileBackgroundFactory,
		@Nullable String descriptionTag) {
		this.groupType = groupType;
		this.tileBackgroundFactory = tileBackgroundFactory;

		setPadding(false);
		setSpacing(false);
		setWidthFull();

		HorizontalLayout titleLayout = new HorizontalLayout();
		titleLayout.setPadding(false);
		titleLayout.setSpacing(false);
		add(titleLayout);

		title = new Span(I18nProperties.getCaption(titleCaption));
		title.addClassNames(titleStyleNames);

		titleLayout.add(title);

		if (StringUtils.isNotBlank(descriptionTag)) {
			infoIcon = new Span(VaadinIcon.INFO_CIRCLE.create());
			VaadinUiUtil.setDescription(infoIcon, I18nProperties.getDescription(descriptionTag));
			infoIcon.addClassName(CssStyles.HSPACE_LEFT_4);
			infoIcon.addClassNames(titleStyleNames);

			titleLayout.add(infoIcon);
		}

		titleLayout.setVisible(false);

		countsLayout = new HorizontalLayout();
		countsLayout.setWidthFull();
		add(countsLayout);
	}

	public void update(Map<T, Long> counts) {
		countsLayout.removeAll();

		Long total = null;
		if (withPercentage) {
			total = counts.values().stream().reduce(0L, Long::sum);
		}

		addTiles(counts, total);
	}

	private void addTiles(Map<T, Long> counts, @Nullable Long total) {
		for (T group : groupType.getEnumConstants()) {
			addTileForGroup(counts, group, total, String.valueOf(group));
		}

		if (counts.containsKey(null)) {
			addTileForGroup(counts, null, total, I18nProperties.getCaption(Captions.notSpecified));
		}
	}

	private void addTileForGroup(Map<T, Long> counts, T group, Long total, String groupCaption) {
		Long count = counts.getOrDefault(group, 0L);

		BigDecimal percentage = null;
		if (total != null) {
			percentage = total == 0
				? BigDecimal.ZERO
				: BigDecimal.valueOf(count).divide(BigDecimal.valueOf(total), MathContext.DECIMAL32).multiply(new BigDecimal(100));
			percentage = percentage.setScale(0, RoundingMode.HALF_UP);
		}

		TileComponent<T> tile = new TileComponent<>(groupCaption, count, percentage, groupLabelStyle, tileBackgroundFactory.apply(group));
		tile.setWidthFull();

		countsLayout.add(tile);
		countsLayout.setFlexGrow(1, tile);
	}

	public void setTitleStyleNames(String... styleNames) {
		title.removeClassNames(titleStyleNames);
		title.addClassNames(styleNames);

		if (infoIcon != null) {
			infoIcon.removeClassNames(titleStyleNames);
			infoIcon.addClassNames(styleNames);
		}

		this.titleStyleNames = styleNames;
	}

	public void setWithPercentage(boolean withPercentage) {
		this.withPercentage = withPercentage;
	}

	public void setGroupLabelStyle(String groupLabelStyle) {
		this.groupLabelStyle = groupLabelStyle;
	}

	private static class TileComponent<T> extends VerticalLayout {

		private static final long serialVersionUID = 5055236377479070515L;

		TileComponent(String group, Long count, @Nullable BigDecimal percentage, String groupLabelStyle, String backgroundStyle) {
			setSpacing(false);
			setPadding(false);

			VerticalLayout numbersLayout = new VerticalLayout();
			numbersLayout.setWidthFull();
			numbersLayout.setPadding(false);
			numbersLayout.setSpacing(false);
			numbersLayout.addClassNames(backgroundStyle, CssStyles.ROUNDED_BORDER_TOP);
			add(numbersLayout);

			Span countLabel = new Span(count.toString());
			countLabel
				.addClassNames(CssStyles.LABEL_WHITE, CssStyles.LABEL_BOLD, CssStyles.LABEL_LARGE, CssStyles.ALIGN_CENTER, CssStyles.VSPACE_TOP_4);

			numbersLayout.add(countLabel);
			numbersLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, countLabel);

			if (percentage != null) {
				Span percentageLabel = new Span(percentage + "%");
				percentageLabel.addClassNames(CssStyles.LABEL_WHITE, CssStyles.ALIGN_CENTER, CssStyles.VSPACE_4);

				numbersLayout.add(percentageLabel);
				numbersLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, percentageLabel);
			}

			Span groupLabel = new Span(group);
			groupLabel.addClassNames(
				CssStyles.LABEL_WHITE,
				CssStyles.LABEL_BOLD,
				CssStyles.VSPACE_TOP_4,
				CssStyles.VSPACE_4,
				CssStyles.HSPACE_LEFT_4,
				CssStyles.HSPACE_RIGHT_4);

			if (groupLabelStyle != null) {
				// may hold several space-separated names (V8 addStyleName split them; Flow addClassName does not)
				CssStyles.style(groupLabel, groupLabelStyle);
			}

			VerticalLayout groupLabelLayout = new VerticalLayout();
			groupLabelLayout.setPadding(false);
			groupLabelLayout.setSpacing(false);
			groupLabelLayout.addClassNames(backgroundStyle, CssStyles.BACKGROUND_DARKER, CssStyles.ROUNDED_BORDER_BOTTOM);
			groupLabelLayout.add(groupLabel);
			groupLabelLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, groupLabel);

			add(groupLabelLayout);
		}
	}
}
