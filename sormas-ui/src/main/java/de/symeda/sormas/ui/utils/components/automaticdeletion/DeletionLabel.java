package de.symeda.sormas.ui.utils.components.automaticdeletion;

import java.util.Date;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.shared.Tooltip;

import de.symeda.sormas.api.deletionconfiguration.DeletionInfoDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class DeletionLabel extends HorizontalLayout {

	public DeletionLabel(DeletionInfoDto deletionInfoDto, String classPrefix) {
		if (deletionInfoDto != null) {
			setDeleteLabel(deletionInfoDto, classPrefix);
		}
	}

	public DeletionLabel(DeletionInfoDto automaticDeletionInfoDto, DeletionInfoDto manuallyDeletionInfoDto, boolean entityDeleted, String classPrefix) {
		if (manuallyDeletionInfoDto != null && entityDeleted) {
			if (automaticDeletionInfoDto != null) {
				if (manuallyDeletionInfoDto.getDeletionDate().before(automaticDeletionInfoDto.getDeletionDate())) {
					setDeleteLabel(manuallyDeletionInfoDto, classPrefix);
				} else {
					setDeleteLabel(automaticDeletionInfoDto, classPrefix);
				}
			} else {
				setDeleteLabel(manuallyDeletionInfoDto, classPrefix);
			}
		} else if (automaticDeletionInfoDto != null) {
			setDeleteLabel(automaticDeletionInfoDto, classPrefix);
		}
	}

	private void setDeleteLabel(DeletionInfoDto deletionInfoDto, String classPrefix) {
		setPadding(false);
		setSpacing(false);

		String infoIconDesciption = String.format(
			I18nProperties.getString(Strings.infoAutomaticDeletionTooltip),
			DateFormatHelper.formatDate(deletionInfoDto.getDeletionDate()),
			I18nProperties.getPrefixCaption(classPrefix, deletionInfoDto.getDeletionReferenceField()),
			DateFormatHelper.formatDate(deletionInfoDto.getReferenceDate()),
			formatDeletionPeriod(deletionInfoDto.getDeletionPeriod()));
		Span infoIcon = new Span(VaadinIcon.INFO_CIRCLE.create());
		// was setDescription(infoIconDesciption, ContentMode.HTML): the tooltip renders the HTML through Markdown
		Tooltip.forComponent(infoIcon).setMarkdown(infoIconDesciption);
		infoIcon.addClassNames(CssStyles.VSPACE_TOP_4, CssStyles.HSPACE_RIGHT_4);
		add(infoIcon);

		String infoText =
			String.format(I18nProperties.getString(Strings.infoAutomaticDeletion), DateFormatHelper.formatDate(deletionInfoDto.getDeletionDate()));
		Span infoTextLabel = VaadinUiUtil.createLabel(infoText, ContentMode.HTML);
		infoTextLabel.addClassName(CssStyles.VSPACE_TOP_4);
		add(infoTextLabel);

		if (DateHelper.getDaysBetween(new Date(), deletionInfoDto.getDeletionDate()) < 181) {
			infoIcon.addClassName(CssStyles.LABEL_CRITICAL);
			infoTextLabel.addClassName(CssStyles.LABEL_CRITICAL);
		} else {
			infoIcon.addClassName(CssStyles.LABEL_MINOR);
			infoTextLabel.addClassName(CssStyles.LABEL_MINOR);
		}
	}

	private String formatDeletionPeriod(int deletionPeriod) {
		if (deletionPeriod < 31) {
			return String.format(I18nProperties.getString(Strings.infoAutomaticDeletionTooltipDays), deletionPeriod);
		} else if (deletionPeriod < 366) {
			return String.format(I18nProperties.getString(Strings.infoAutomaticDeletionTooltipMonths), deletionPeriod / 30);
		} else {
			return String.format(I18nProperties.getString(Strings.infoAutomaticDeletionTooltipYears), deletionPeriod / 365);
		}
	}
}
