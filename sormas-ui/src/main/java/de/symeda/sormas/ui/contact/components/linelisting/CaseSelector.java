package de.symeda.sormas.ui.contact.components.linelisting;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.caze.CaseReferenceDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.contact.AdoptAddressLayout;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.fields.CustomField;

public class CaseSelector extends CustomField<CaseReferenceDto> {

	private final CaseReferenceDto initialCaseValue;

	private final Span caseInfoLabel;
	private CaseReferenceDto selectedCaseValue;
	private AdoptAddressLayout adoptAddressLayout;

	public CaseSelector(String caseInfoText) {
		caseInfoLabel = VaadinUiUtil.createLabel(caseInfoText, ContentMode.HTML);
		initialCaseValue = null;
	}

	public CaseSelector(CaseReferenceDto selectedCaseValue) {
		caseInfoLabel = VaadinUiUtil.createLabel("", ContentMode.HTML);
		initialCaseValue = selectedCaseValue;
	}

	/**
	 * Built on first attach, as in Vaadin 8 ({@link CustomField}).
	 */
	@Override
	protected Component initContent() {
		HorizontalLayout layout = new HorizontalLayout();
		VerticalLayout mainLayout = new VerticalLayout(layout);
		mainLayout.setSpacing(false);

		if (initialCaseValue != null) {
			setValue(initialCaseValue);
			displayLabelForCase(initialCaseValue);
			layout.add(caseInfoLabel);
			adoptAddressLayout = new AdoptAddressLayout(initialCaseValue);
			adoptAddressLayout.setAdoptAddress(true);
			mainLayout.add(adoptAddressLayout);
		} else {
			Button chooseCaseButton = ButtonHelper.createButton(Captions.contactChooseCase, null, CssStyles.BUTTON_PRIMARY, CssStyles.VSPACE_2);
			Button removeCaseButton = ButtonHelper.createButton(Captions.contactRemoveCase, null, CssStyles.BUTTON_LINK);

			CssStyles.style(caseInfoLabel, CssStyles.VSPACE_TOP_4);
			layout.add(caseInfoLabel);

			chooseCaseButton.addClickListener(e -> ControllerProvider.getContactController().openSelectCaseForContactWindow(null, selectedCase -> {
				if (selectedCase != null) {
					CaseReferenceDto selectedCaseReference = selectedCase.toReference();
					setValue(selectedCaseReference);
					displayLabelForCase(selectedCaseReference);
					removeCaseButton.setVisible(true);
					chooseCaseButton.setText(I18nProperties.getCaption(Captions.contactChangeCase));
					if (adoptAddressLayout == null) {
						adoptAddressLayout = new AdoptAddressLayout(selectedCase.toReference());
						mainLayout.add(adoptAddressLayout);
					} else {
						adoptAddressLayout.setCaseReference(selectedCase.toReference());
					}
					adoptAddressLayout.setVisible(true);
					adoptAddressLayout.setAdoptAddress(true);
				} else {
					if (adoptAddressLayout != null) {
						adoptAddressLayout.setVisible(false);
						adoptAddressLayout.setAdoptAddress(false);
					}
				}
			}));
			layout.add(chooseCaseButton);

			removeCaseButton.addClickListener(e -> {
				setValue(null);
				VaadinUiUtil.setLabelValue(caseInfoLabel, I18nProperties.getString(Strings.infoNoSourceCaseSelected), ContentMode.HTML);
				caseInfoLabel.addClassName(CssStyles.VSPACE_TOP_4);
				removeCaseButton.setVisible(false);
				chooseCaseButton.setText(I18nProperties.getCaption(Captions.contactChooseCase));
				if (adoptAddressLayout != null) {
					adoptAddressLayout.setVisible(false);
					adoptAddressLayout.setAdoptAddress(false);
				}
			});
			layout.add(removeCaseButton);
			removeCaseButton.setVisible(false);
		}

		return mainLayout;
	}

	@Override
	protected void setPresentationValue(CaseReferenceDto caseReferenceDto) {
		this.selectedCaseValue = caseReferenceDto;

	}

	@Override
	protected CaseReferenceDto generateModelValue() {
		return selectedCaseValue;
	}

	@Override
	public CaseReferenceDto getValue() {
		return selectedCaseValue;
	}

	private void displayLabelForCase(CaseReferenceDto selectedCase) {
		VaadinUiUtil.setLabelValue(
			caseInfoLabel,
			String.format(
				I18nProperties.getString(Strings.infoContactCreationSourceCase),
				selectedCase.getFirstName() + " " + selectedCase.getLastName() + " " + "(" + DataHelper.getShortUuid(selectedCase.getUuid()) + ")"),
			ContentMode.HTML);
		caseInfoLabel.removeClassName(CssStyles.VSPACE_TOP_4);
	}

	public AdoptAddressLayout getAdoptAddressLayout() {
		return adoptAddressLayout;
	}
}
