package de.symeda.sormas.ui.immunization.components.fields.pickorcreate;

import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.immunization.ImmunizationDto;
import de.symeda.sormas.ui.immunization.components.fields.info.ImmunizationInfo;
import de.symeda.sormas.ui.immunization.components.fields.info.InfoLayout;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.fields.CustomField;

public class ImmunizationPickOrCreateField extends CustomField<String> {

	public static final String KEEP_IMMUNIZATION = "keepImmunization";
	public static final String OVERWRITE_IMMUNIZATION = "overwriteImmunization";
	public static final String CREATE_NEW_IMMUNIZATION = "createNewImmunization";

	private final ImmunizationDto newImmunization;
	private final ImmunizationDto similarImmunization;

	private String selectedImmunizationUuid;

	private Consumer<Boolean> selectionChangeCallback;

	public ImmunizationPickOrCreateField(ImmunizationDto newImmunization, List<ImmunizationDto> similarImmunizations) {
		this.newImmunization = newImmunization;
		this.similarImmunization = similarImmunizations.isEmpty() ? null : similarImmunizations.get(0);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Component initContent() {

		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setSpacing(true);
		mainLayout.setSizeUndefined();
		mainLayout.setWidthFull();

		String infoText = String.format(I18nProperties.getString(Strings.infoPickOrCreateImmunization), newImmunization.getDisease().toString());
		InfoLayout infoLayout = new InfoLayout(infoText);
		mainLayout.add(infoLayout);
		CssStyles.style(infoLayout, CssStyles.VSPACE_3);

		ImmunizationInfo existingImmunization = new ImmunizationInfo(I18nProperties.getString(Strings.infoPickOrCreateImmunizationExisting));
		existingImmunization.add(new ImmunizationInfoLayout(similarImmunization));
		mainLayout.add(existingImmunization);

		ImmunizationPickOrCreateOption keepImmunization =
			new ImmunizationPickOrCreateOption(KEEP_IMMUNIZATION, I18nProperties.getCaption(Captions.immunizationKeepImmunization));
		mainLayout.add(keepImmunization);

		ImmunizationInfo newImmunizationInfo = new ImmunizationInfo(I18nProperties.getString(Strings.infoPickOrCreateImmunizationNew));
		newImmunizationInfo.add(new ImmunizationInfoLayout(newImmunization));
		mainLayout.add(newImmunizationInfo);

		ImmunizationPickOrCreateOption overwriteImmunization =
			new ImmunizationPickOrCreateOption(OVERWRITE_IMMUNIZATION, I18nProperties.getCaption(Captions.immunizationOverwriteImmunization));
		mainLayout.add(overwriteImmunization);

		ImmunizationPickOrCreateOption createNewImmunization =
			new ImmunizationPickOrCreateOption(CREATE_NEW_IMMUNIZATION, I18nProperties.getCaption(Captions.immunizationCreateNewImmunization));
		mainLayout.add(createNewImmunization);

		keepImmunization.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				overwriteImmunization.setValue(null);
				createNewImmunization.setValue(null);
				setPresentationValue(null);
				if (selectionChangeCallback != null) {
					selectionChangeCallback.accept(true);
				}
			}
		});

		overwriteImmunization.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				keepImmunization.setValue(null);
				createNewImmunization.setValue(null);
				setPresentationValue(similarImmunization.getUuid());
				if (selectionChangeCallback != null) {
					selectionChangeCallback.accept(true);
				}
			}
		});

		createNewImmunization.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				overwriteImmunization.setValue(null);
				keepImmunization.setValue(null);
				setPresentationValue(newImmunization.getUuid());
				if (selectionChangeCallback != null) {
					selectionChangeCallback.accept(true);
				}
			}
		});

		return mainLayout;
	}

	/**
	 * Was Vaadin 8 {@code doSetValue}: stores the value without a value change event.
	 */
	@Override
	protected void setPresentationValue(String selectedImmunizationUuid) {
		this.selectedImmunizationUuid = selectedImmunizationUuid;
	}

	@Override
	public String getValue() {
		return this.selectedImmunizationUuid;
	}

	public void setSelectionChangeCallback(Consumer<Boolean> callback) {
		this.selectionChangeCallback = callback;
	}
}
