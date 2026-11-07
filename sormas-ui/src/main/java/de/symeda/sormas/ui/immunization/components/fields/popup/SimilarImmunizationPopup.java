package de.symeda.sormas.ui.immunization.components.fields.popup;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.immunization.ImmunizationDto;
import de.symeda.sormas.ui.immunization.components.fields.info.ImmunizationInfo;
import de.symeda.sormas.ui.immunization.components.fields.info.InfoLayout;
import de.symeda.sormas.ui.utils.CssStyles;

public class SimilarImmunizationPopup extends VerticalLayout {

	public SimilarImmunizationPopup(ImmunizationDto immunization, ImmunizationDto similarImmunization) {

		setSpacing(true);
		setSizeUndefined();
		setWidthFull();

		String infoText = String.format(I18nProperties.getString(Strings.infoSimilarImmunization), immunization.getDisease().toString());
		InfoLayout infoLayout = new InfoLayout(infoText);
		add(infoLayout);
		CssStyles.style(infoLayout, CssStyles.VSPACE_3);

		ImmunizationInfo immunizationInfo = new ImmunizationInfo(I18nProperties.getString(Strings.infoImmunizationPeriod));
		immunizationInfo.add(new ImmunizationInfoLayout(immunization));
		add(immunizationInfo);

		ImmunizationInfo existingImmunization = new ImmunizationInfo(I18nProperties.getString(Strings.infoExistingImmunizationPeriod));
		existingImmunization.add(new ImmunizationInfoLayout(similarImmunization));
		add(existingImmunization);
	}
}
