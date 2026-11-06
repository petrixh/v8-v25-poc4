package de.symeda.sormas.ui.person;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.data.validator.RegexpValidator;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.person.PersonDto;
import de.symeda.sormas.api.person.PersonSimilarityCriteria;
import de.symeda.sormas.api.person.SimilarPersonDto;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class PersonFormHelper {

	/**
	 * The pattern of the Vaadin 7 {@code EmailValidator}.
	 */
	private static final String V7_EMAIL_PATTERN = "^([a-zA-Z0-9_\\.\\-+])+@[a-zA-Z0-9-.]+\\.[a-zA-Z0-9-]{2,}$";

	/**
	 * Was the Vaadin 7 {@code EmailValidator}: same pattern, and an empty value is valid as in Vaadin 7 (Flow's own
	 * {@code EmailValidator} rejects it).
	 */
	public static RegexpValidator createEmailValidator(String errorMessage) {
		return new RegexpValidator(errorMessage, V7_EMAIL_PATTERN) {

			private static final long serialVersionUID = 1L;

			@Override
			protected boolean isValid(String value) {
				return value == null || value.isEmpty() || super.isValid(value);
			}
		};
	}

	public static Dialog warningSimilarPersons(String nationalHealthId, String currentPersonUuid, Runnable callback) {
		if (StringUtils.isNoneBlank(nationalHealthId)) {
			PersonDto currentPerson = new PersonDto();
			currentPerson.setNationalHealthId(nationalHealthId);
			List<SimilarPersonDto> similarPersonDtos =
				FacadeProvider.getPersonFacade().getSimilarPersonDtos(PersonSimilarityCriteria.forPerson(currentPerson, true));
			List<SimilarPersonDto> filteredSimilarPersons = similarPersonDtos.stream()
				.filter(similarPersonDto -> !similarPersonDto.getUuid().equals(currentPersonUuid))
				.collect(Collectors.toList());

			if (!filteredSimilarPersons.isEmpty()) {
				PersonSelectionGrid similarPersonGrid = new PersonSelectionGrid();
				similarPersonGrid.loadData(filteredSimilarPersons);

				final CommitDiscardWrapperComponent<PersonSelectionGrid> component = new CommitDiscardWrapperComponent<>(similarPersonGrid);
				component.getCommitButton().setText(I18nProperties.getCaption(Captions.actionDone));
				component.getCommitButton().setTooltipText(I18nProperties.getCaption(Captions.actionOkay));
				component.getDiscardButton().setVisible(false);
				component.getWrappedComponent().setWidth(800, Unit.PIXELS);

				Dialog popupWindow = VaadinUiUtil.showPopupWindow(component);
				component.addDoneListener(() -> {
					popupWindow.close();
					callback.run();
				});
				popupWindow.setHeaderTitle(I18nProperties.getString(Strings.headingSimilarPerson));
				popupWindow.setWidth("900px");
				return popupWindow;
			}
		}
		return null;
	}
}
