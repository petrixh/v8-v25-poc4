package de.symeda.sormas.ui.dashboard.surveillance.components.disease.tile;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.utils.CssStyles;

public class StatsItem extends HorizontalLayout {

	public static class Builder {

		private final String label;
		private final String value;

		private boolean isCritical;
		private boolean isSingleColumn;

		public Builder(String label, String value) {
			this.label = label;
			this.value = value;
		}

		public Builder(String label, Long value) {
			this(label, value.toString());
		}

		public Builder critical(boolean isCritical) {
			this.isCritical = isCritical;
			return this;
		}

		public Builder singleColumn(boolean isSingleColumn) {
			this.isSingleColumn = isSingleColumn;
			return this;
		}

		public StatsItem build() {
			return new StatsItem(this);
		}
	}

	private StatsItem(Builder builder) {
		setWidth(100, Unit.PERCENTAGE);
		setPadding(false);
		setSpacing(false);

		Span nameLabel = new Span(I18nProperties.getCaption(builder.label));
		nameLabel.addClassNames(CssStyles.LABEL_PRIMARY, builder.isCritical ? CssStyles.LABEL_CRITICAL : "", CssStyles.HSPACE_LEFT_3);
		addComponent(nameLabel);
		if (!builder.isSingleColumn) {
			setFlexGrow(1, nameLabel);
		}

		Span valueLabel = new Span(builder.value);
		valueLabel.addClassNames(
			CssStyles.LABEL_PRIMARY,
			CssStyles.LABEL_CAPTION_TRUNCATED,
			builder.isCritical ? CssStyles.LABEL_CRITICAL : "",
			builder.isSingleColumn ? CssStyles.HSPACE_LEFT_5 : CssStyles.ALIGN_CENTER);
		addComponent(valueLabel);
		setFlexGrow(builder.isSingleColumn ? 1f : 0.65f, valueLabel);
	}
}
