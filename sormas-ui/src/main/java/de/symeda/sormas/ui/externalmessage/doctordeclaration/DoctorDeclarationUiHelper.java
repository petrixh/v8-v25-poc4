package de.symeda.sormas.ui.externalmessage.doctordeclaration;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.sample.SampleDto;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.SormasUI;
import de.symeda.sormas.ui.externalmessage.ExternalMessagesView;
import de.symeda.sormas.ui.samples.AbstractSampleForm;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent;

/**
 * Collection of doctor declaration UI related functions
 */
public class DoctorDeclarationUiHelper {

	public static Button addCancelAndUpdateLabels(CommitDiscardWrapperComponent<?> component, String discardCaptionTag) {
		// was expand ratio 0 on discard and 1 on the cancel button aligned bottom right: the free space goes left of cancel
		component.getDiscardButton().getStyle().remove("margin-left");
		Button cancelButton = ButtonHelper.createButton(Captions.actionCancel, null);
		component.getButtonsPanel().addComponentAtIndex(0, cancelButton);
		component.getButtonsPanel().setVerticalComponentAlignment(FlexComponent.Alignment.END, cancelButton);
		cancelButton.getStyle().set("margin-left", "auto");

		component.getCommitButton().setText(I18nProperties.getCaption(Captions.actionSaveAndContinue));
		component.getDiscardButton().setText(I18nProperties.getCaption(discardCaptionTag));

		return cancelButton;
	}

	public static void establishCommitButtons(CommitDiscardWrapperComponent<? extends AbstractSampleForm> sampleComponent, boolean lastSample) {
		if (lastSample) {
			// add option to navigate to related entry after saving
			DoctorDeclarationUiHelper.addSaveAndOpenEntryButton(sampleComponent);
		}
		// add yet another listener just for the save button, it will not be added to the save and open entry button
		sampleComponent.getCommitButton().addClickListener(clickEvent -> SormasUI.get().getNavigator().navigateTo(ExternalMessagesView.VIEW_NAME));
	}

	private static Button addSaveAndOpenEntryButton(CommitDiscardWrapperComponent<? extends AbstractSampleForm> sampleComponent) {
		Button saveAndOpenEntryButton = ButtonHelper.createButton("");

		// Copy every existing listener from the old commit button to the newly added one: Flow buttons do not expose
		// their listeners; besides the double-click guard (which this button has itself) the commit button has only the
		// wrapper's own commit handler at this point
		saveAndOpenEntryButton.addClickListener(clickEvent -> sampleComponent.commitAndHandle());

		SampleDto sample = sampleComponent.getWrappedComponent().getValue();

		if (sample.getAssociatedCase() != null) {
			saveAndOpenEntryButton
				.addClickListener(clickEvent -> ControllerProvider.getCaseController().navigateToCase(sample.getAssociatedCase().getUuid()));
			saveAndOpenEntryButton.setText(I18nProperties.getCaption(Captions.actionSaveAndOpenCase));
		} else if (sample.getAssociatedContact() != null) {
			saveAndOpenEntryButton
				.addClickListener(clickEvent -> ControllerProvider.getContactController().navigateToData(sample.getAssociatedContact().getUuid()));
			saveAndOpenEntryButton.setText(I18nProperties.getCaption(Captions.actionSaveAndOpenContact));
		} else if (sample.getAssociatedEventParticipant() != null) {
			saveAndOpenEntryButton.addClickListener(
				clickEvent -> ControllerProvider.getEventParticipantController().navigateToData(sample.getAssociatedEventParticipant().getUuid()));
			saveAndOpenEntryButton.setText(I18nProperties.getCaption(Captions.actionSaveAndOpenEventParticipant));
		} else {
			throw new UnsupportedOperationException("Could not create saveAndOpenEntryButton: associated entity is not supported.");
		}

		// was setStyleName(commitButton.getStyleName()): the same class names and theme variants
		Button commitButton = sampleComponent.getCommitButton();
		saveAndOpenEntryButton.setClassName(commitButton.getClassName());
		commitButton.getThemeNames().forEach(saveAndOpenEntryButton::addThemeName);

		HorizontalLayout buttonsPanel = sampleComponent.getButtonsPanel();
		buttonsPanel.addComponentAtIndex(buttonsPanel.getComponentCount() - 1, saveAndOpenEntryButton);

		saveAndOpenEntryButton.setId("saveAndOpenEntryButton");

		return saveAndOpenEntryButton;
	}

}
