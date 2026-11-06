package de.symeda.sormas.ui.utils;

import java.util.Arrays;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import de.symeda.sormas.api.common.DeletionDetails;
import de.symeda.sormas.api.common.DeletionReason;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.utils.FieldConstraints;

public class DeletableUtils {

	public static void showDeleteWithReasonPopup(String message, Consumer<DeletionDetails> handler) {
		VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.setPadding(false);

		Span contentLabel = VaadinUiUtil.createLabel(message, ContentMode.HTML);
		contentLabel.addClassName(CssStyles.LABEL_WHITE_SPACE_NORMAL);
		contentLabel.setWidthFull();
		verticalLayout.add(contentLabel);

		ComboBox<DeletionReason> deleteReasonComboBox = new ComboBox<>(null, Arrays.asList(DeletionReason.values()));
		deleteReasonComboBox.setLabel(I18nProperties.getCaption(Captions.deletionReason));
		deleteReasonComboBox.setWidthFull();
		deleteReasonComboBox.setRequiredIndicatorVisible(true);

		verticalLayout.add(deleteReasonComboBox);
		TextArea otherDeletionReason = new TextArea();
		otherDeletionReason.setLabel(I18nProperties.getCaption(Captions.otherDeletionReason));
		verticalLayout.add(otherDeletionReason);
		otherDeletionReason.setVisible(false);
		otherDeletionReason.setWidthFull();
		// was setRows(3)
		otherDeletionReason.setMinRows(3);
		otherDeletionReason.setMaxRows(3);
		otherDeletionReason.setMaxLength(FieldConstraints.CHARACTER_LIMIT_TEXT);

		otherDeletionReason.setRequiredIndicatorVisible(true);

		deleteReasonComboBox.addValueChangeListener(valueChangeEvent -> {
			otherDeletionReason.setVisible(valueChangeEvent.getValue() == (DeletionReason.OTHER_REASON));
		});

		deleteReasonComboBox.addValueChangeListener(valueChangeEvent -> {
			if (deleteReasonComboBox.isEmpty()) {
				deleteReasonComboBox.setErrorMessage(I18nProperties.getString(Strings.messageDeleteReasonNotFilled));
				deleteReasonComboBox.setInvalid(true);
			} else {
				deleteReasonComboBox.setInvalid(false);
			}
		});

		otherDeletionReason.addValueChangeListener(valueChangeEvent -> {
			if (deleteReasonComboBox.getValue() == DeletionReason.OTHER_REASON && StringUtils.isBlank(otherDeletionReason.getValue())) {
				otherDeletionReason.setErrorMessage(I18nProperties.getString(Strings.messageOtherDeleteReasonNotFilled));
				otherDeletionReason.setInvalid(true);
			} else {
				otherDeletionReason.setInvalid(false);
			}
		});

		VaadinUiUtil.showConfirmationPopup(
			I18nProperties.getString(Strings.headingDeleteConfirmation),
			verticalLayout,
			I18nProperties.getString(Strings.yes),
			I18nProperties.getString(Strings.no),
			640,
			confirmed -> {
				if (Boolean.TRUE.equals(confirmed)) {
					if (deleteReasonComboBox.isEmpty()) {
						deleteReasonComboBox.setErrorMessage(I18nProperties.getString(Strings.messageDeleteReasonNotFilled));
						deleteReasonComboBox.setInvalid(true);
						return false;
					} else if (deleteReasonComboBox.getValue() == DeletionReason.OTHER_REASON
						&& StringUtils.isBlank(otherDeletionReason.getValue())) {
						otherDeletionReason.setErrorMessage(I18nProperties.getString(Strings.messageOtherDeleteReasonNotFilled));
						otherDeletionReason.setInvalid(true);
						return false;
					}

					if (deleteReasonComboBox.getValue() != DeletionReason.OTHER_REASON && !StringUtils.isBlank(otherDeletionReason.getValue())) {
						otherDeletionReason.clear();
					}

					handler.accept(new DeletionDetails(deleteReasonComboBox.getValue(), otherDeletionReason.getValue()));
				}

				return true;
			});
	}
}
