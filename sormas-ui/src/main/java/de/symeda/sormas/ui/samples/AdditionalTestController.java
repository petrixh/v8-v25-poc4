package de.symeda.sormas.ui.samples;

import java.util.List;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.dialog.Dialog;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.sample.AdditionalTestDto;
import de.symeda.sormas.api.sample.AdditionalTestFacade;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent.CommitListener;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.NotificationHelper.Type;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class AdditionalTestController {

	private AdditionalTestFacade facade = FacadeProvider.getAdditionalTestFacade();

	public AdditionalTestController() {

	}

	public List<AdditionalTestDto> getAdditionalTestsBySample(String sampleUuid) {
		return facade.getAllBySample(sampleUuid);
	}

	public void openCreateComponent(String sampleUuid, Runnable callback) {
		AdditionalTestForm form = new AdditionalTestForm(FacadeProvider.getSampleFacade().getSampleByUuid(sampleUuid), true);
		form.setValue(AdditionalTestDto.build(FacadeProvider.getSampleFacade().getReferenceByUuid(sampleUuid)));
		final CommitDiscardWrapperComponent<AdditionalTestForm> component =
			new CommitDiscardWrapperComponent<>(form, UiUtil.permitted(UserRight.ADDITIONAL_TEST_CREATE), form.getFieldGroup());

		Dialog window = VaadinUiUtil.showModalPopupWindow(component, I18nProperties.getString(Strings.headingCreateAdditionalTest));
		window.setWidth("calc(" + form.getWidth() + " + 90px)");
		window.setHeight(80, Unit.PERCENTAGE);

		component.addCommitListener(new CommitListener() {

			@Override
			public void onCommit() {
				if (!form.getFieldGroup().isModified()) {
					FacadeProvider.getAdditionalTestFacade().saveAdditionalTest(form.getValue());
					NotificationHelper.show(I18nProperties.getString(Strings.messageAdditionalTestSaved), Type.TRAY_NOTIFICATION);
					if (callback != null) {
						callback.run();
					}
				}
			}
		});
	}

	public void openEditComponent(AdditionalTestDto dto, Runnable callback, boolean isEditAllowed, boolean isDeleteAllowed) {

		AdditionalTestDto newDto = FacadeProvider.getAdditionalTestFacade().getByUuid(dto.getUuid());
		AdditionalTestForm form = new AdditionalTestForm(FacadeProvider.getSampleFacade().getSampleByUuid(dto.getSample().getUuid()), false);
		form.setValue(newDto);

		boolean isEditOrDeleteAllowed = isEditAllowed || isDeleteAllowed;
		final CommitDiscardWrapperComponent<AdditionalTestForm> component =
			new CommitDiscardWrapperComponent<>(form, isEditOrDeleteAllowed, form.getFieldGroup());

		Dialog window = VaadinUiUtil.showModalPopupWindow(
			component,
			I18nProperties.getString(isEditAllowed ? Strings.headingEditAdditionalTest : Strings.headingViewAdditionalTest));
		window.setWidth("calc(" + form.getWidth() + " + 90px)");
		window.setHeight(80, Unit.PERCENTAGE);

		if (isEditOrDeleteAllowed) {
			component.addCommitListener(new CommitListener() {

				@Override
				public void onCommit() {
					if (!form.getFieldGroup().isModified()) {
						FacadeProvider.getAdditionalTestFacade().saveAdditionalTest(form.getValue());
						NotificationHelper.show(I18nProperties.getString(Strings.messageAdditionalTestSaved), Type.TRAY_NOTIFICATION);
						if (callback != null) {
							callback.run();
						}
					}
				}
			});

			if (isDeleteAllowed) {
				component.addDeleteListener(() -> {
					FacadeProvider.getAdditionalTestFacade().deleteAdditionalTest(dto.getUuid());
					window.close();
					NotificationHelper.show(I18nProperties.getString(Strings.messageAdditionalTestDeleted), Type.TRAY_NOTIFICATION);
					if (callback != null) {
						callback.run();
					}
				}, I18nProperties.getString(Strings.entityAdditionalTest));
			}

			component.restrictEditableComponentsOnEditView(UserRight.ADDITIONAL_TEST_EDIT, null, UserRight.ADDITIONAL_TEST_DELETE, null, true);
		}
		component.getButtonsPanel().setVisible(isEditOrDeleteAllowed);
	}
}
