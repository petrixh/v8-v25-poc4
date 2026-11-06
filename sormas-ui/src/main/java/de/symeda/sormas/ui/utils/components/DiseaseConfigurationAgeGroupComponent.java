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

package de.symeda.sormas.ui.utils.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import de.symeda.sormas.api.disease.DiseaseConfigurationAgeGroup;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.person.ApproximateAgeType;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.fields.ComboBox;
import de.symeda.sormas.ui.utils.fields.CustomField;

public class DiseaseConfigurationAgeGroupComponent extends CustomField<List<String>> {

	private VerticalLayout rowsLayout;
	private List<AgeGroupRow> rows;
	private Span lblNoAgeGroups;
	private List<DiseaseConfigurationAgeGroup> ageGroups;

	public DiseaseConfigurationAgeGroupComponent() {
		// V7 FieldGroup commits called validate(); the Flow form binding runs the field's validators instead
		addValidator(value -> validateAgeGroups());
	}
	@Override
	protected Component initContent() {

		VerticalLayout layout = new VerticalLayout();
		layout.setWidthFull();
		// was MarginInfo(false, false, true, false): bottom margin only (look, parked)
		layout.setPadding(false);
		layout.setSpacing(false);
		CssStyles.style(layout, CssStyles.VSPACE_TOP_4);

		lblNoAgeGroups = new Span(I18nProperties.getString(Strings.infoNoDiseaseConfigurationAgeGroups));
		layout.add(lblNoAgeGroups);

		rowsLayout = new VerticalLayout();
		rowsLayout.setWidthFull();
		rowsLayout.setPadding(false);
		rowsLayout.setSpacing(false);
		layout.add(rowsLayout);

		if (rows != null) {
			buildRowsLayout();
			updateNoAgeGroupsLabelVisibility();
		}

		Button btnAdd = ButtonHelper
			.createIconButtonWithCaption(null, null, VaadinIcon.PLUS, e -> buildAgeGroupRow(null, null, null, null, true), CssStyles.VSPACE_TOP_5);
		btnAdd.setHeight(25, Unit.PIXELS);
		btnAdd.setWidthFull();
		layout.add(btnAdd);

		return layout;
	}

	@Override
	public Class<? extends List<String>> getType() {
		//noinspection unchecked,InstantiatingObjectToGetClassObject,InstantiatingObjectToGetClassObject
		return (Class<? extends List<String>>) new ArrayList<String>(0).getClass();
	}

	private void buildRowsLayout() {

		if (rowsLayout == null) {
			return;
		}

		rowsLayout.removeAll();
		rows.forEach(r -> rowsLayout.add(r));
	}

	private void buildAgeGroupRows() {

		rows = new ArrayList<>();
		if (ageGroups != null) {
			ageGroups.forEach(
				ageGroup -> buildAgeGroupRow(ageGroup.getStartAge(), ageGroup.getStartAgeType(), ageGroup.getEndAge(), ageGroup.getEndAgeType()));
		}
	}

	private void buildAgeGroupRow(Integer startAge, ApproximateAgeType startAgeType, Integer endAge, ApproximateAgeType endAgeType) {
		buildAgeGroupRow(startAge, startAgeType, endAge, endAgeType, false);
	}

	private void buildAgeGroupRow(Integer startAge, ApproximateAgeType startAgeType, Integer endAge, ApproximateAgeType endAgeType, boolean render) {

		AgeGroupRow row = new AgeGroupRow(startAge, startAgeType, endAge, endAgeType);
		row.setDeleteCallback(() -> {
			rows.remove(row);
			rowsLayout.remove(row);
			updateNoAgeGroupsLabelVisibility();
		});
		rows.add(row);
		updateNoAgeGroupsLabelVisibility();

		if (render) {
			rowsLayout.add(row);
		}
	}

	@Override
	public void setValue(List<String> newFieldValue) {

		super.setValue(newFieldValue);

		List<DiseaseConfigurationAgeGroup> ageGroupList = null;
		if (newFieldValue != null) {
			ageGroupList = new ArrayList<>();
			DiseaseConfigurationAgeGroup diseaseConfigurationAgeGroup = null;

			String startAgeGroupPart;
			Integer startAgeGroupAge = null;
			ApproximateAgeType startAgeGroupCode = null;
			String endAgeGroupPart = null;
			Integer endAgeGroupAge = null;
			ApproximateAgeType endAgeGroupCode = null;

			for (String ageGroupString : newFieldValue) {
				String[] ageGroupSplit = ageGroupString.split("_");

				startAgeGroupPart = ageGroupSplit[0];
				startAgeGroupAge = Integer.valueOf(startAgeGroupPart.substring(0, startAgeGroupPart.length() - 1));
				startAgeGroupCode = getApproximateAgeType(String.valueOf(startAgeGroupPart.charAt(startAgeGroupPart.length() - 1)));

				endAgeGroupAge = null;
				endAgeGroupCode = null;
				if (ageGroupSplit.length == 2) {
					endAgeGroupPart = ageGroupSplit[1];
					endAgeGroupAge = Integer.valueOf(endAgeGroupPart.substring(0, endAgeGroupPart.length() - 1));
					endAgeGroupCode = getApproximateAgeType(String.valueOf(endAgeGroupPart.charAt(endAgeGroupPart.length() - 1)));
				}

				diseaseConfigurationAgeGroup = new DiseaseConfigurationAgeGroup(startAgeGroupAge, startAgeGroupCode, endAgeGroupAge, endAgeGroupCode);
				ageGroupList.add(diseaseConfigurationAgeGroup);
			}
		}

		this.ageGroups = ageGroupList;
		buildAgeGroupRows();
		buildRowsLayout();
	}

	/*
	 * @Override
	 * public String getValue() {
	 * return rows != null ? generateStringValue() : null;
	 * }
	 */

	private ApproximateAgeType getApproximateAgeType(String ageGroupCode) {
		ApproximateAgeType result = null;

		switch (ageGroupCode) {
		case "Y":
			result = ApproximateAgeType.YEARS;
			break;
		case "M":
			result = ApproximateAgeType.MONTHS;
			break;
		case "D":
			result = ApproximateAgeType.DAYS;
			break;
		}

		return result;
	}

	private List<String> generateStringValue() {
		if (rows == null) {
			return null;
		}

		List<String> ageGroupStringsList = new ArrayList<>();
		for (AgeGroupRow ageGroupRow : rows) {
			ageGroupStringsList.add(ageGroupRow.toEntityString());
		}

		return ageGroupStringsList;
	}

	/**
	 * Was the V7 {@code validate()} override (no required check, no other validators).
	 */
	@Override
	public void validateValue() {
		validateAgeGroups();
	}

	private void validateAgeGroups() {
		List<String> errorMessages = new ArrayList<>();
		int rowPosition = 0;
		for (AgeGroupRow ageGroupRow : rows) {
			++rowPosition;

			if (ageGroupRow.getStartAge() == null || ageGroupRow.getStartAgeType() == null) {
				errorMessages.add(I18nProperties.getValidationError(Validations.diseaseConfigurationInvalidStartAge, rowPosition));
			}

			if ((ageGroupRow.getEndAge() != null && ageGroupRow.getEndAgeType() == null)
				|| ageGroupRow.getEndAge() == null && ageGroupRow.getEndAgeType() != null) {
				errorMessages.add(I18nProperties.getValidationError(Validations.diseaseConfigurationInvalidEndAge, rowPosition));
			}

			if (ageGroupRow.getStartAgeType() != null && ageGroupRow.getEndAgeType() != null) {
				if (ageGroupRow.getStartAgeType().getCode().charAt(0) > ageGroupRow.getEndAgeType().getCode().charAt(0)) {
					errorMessages.add(I18nProperties.getValidationError(Validations.diseaseConfigurationInvalidAgeGroup, rowPosition));
				}

				if ((ageGroupRow.getStartAgeType().getCode().charAt(0) == ageGroupRow.getEndAgeType().getCode().charAt(0))
					&& ageGroupRow.getStartAge() != null
					&& ageGroupRow.getEndAge() != null
					&& (ageGroupRow.getStartAge() > ageGroupRow.getEndAge())) {
					errorMessages.add(I18nProperties.getValidationError(Validations.diseaseConfigurationInvalidAgeGroup, rowPosition));
				}

				if ((ageGroupRow.getStartAgeType().getCode().charAt(0) == ageGroupRow.getEndAgeType().getCode().charAt(0))
					&& ageGroupRow.getStartAge() != null
					&& ageGroupRow.getEndAge() != null
					&& ageGroupRow.getStartAge().equals(ageGroupRow.getEndAge())) {
					errorMessages.add(I18nProperties.getValidationError(Validations.diseaseConfigurationInvalidOpenEndedAgeGroup, rowPosition));
				}
			}
		}

		if (!errorMessages.isEmpty()) {
			throw new InvalidValueException(String.join("\n", errorMessages));
		}
	}

	/**
	 * Was V7 {@code getInternalValue()}: the value is built from the rows.
	 */
	@Override
	public List<String> getValue() {
		return rows != null ? generateStringValue() : null;
	}

	private void updateNoAgeGroupsLabelVisibility() {

		if (lblNoAgeGroups == null) {
			return;
		}

		lblNoAgeGroups.setVisible(CollectionUtils.isEmpty(rows));
	}

	private static final class AgeGroupRow extends HorizontalLayout {

		private static final long serialVersionUID = 8787967785281039120L;

		private final TextField tfStartAge;
		private final ComboBox cbStartAgeType;
		private final TextField tfEndAge;
		private final ComboBox cbEndAgeType;
		private Runnable deleteCallback;

		public AgeGroupRow(Integer startAge, ApproximateAgeType startAgeType, Integer endAge, ApproximateAgeType endAgeType) {

			Span fromAgeLabel = new Span(I18nProperties.getString(Strings.promptDiseaseConfigurationAgeFrom));
			fromAgeLabel.setWidthFull();

			tfStartAge = new TextField();
			tfStartAge.setWidthFull();
			if (startAge != null) {
				tfStartAge.setValue(startAge.toString());
			}
			tfStartAge.setPlaceholder(I18nProperties.getString(Strings.promptDiseaseConfigurationStartAge));

			cbStartAgeType = new ComboBox(null, Arrays.asList(ApproximateAgeType.values()));
			cbStartAgeType.setWidth(125, Unit.PIXELS);
			cbStartAgeType.setInputPrompt(I18nProperties.getString(Strings.promptDiseaseConfigurationStartAgeType));
			CssStyles.style(CssStyles.VSPACE_NONE, cbStartAgeType, tfStartAge);
			if (startAgeType != null) {
				cbStartAgeType.setValue(startAgeType);
			}

			Span toAgeLabel = new Span(I18nProperties.getString(Strings.promptDiseaseConfigurationAgeTo));
			CssStyles.style(toAgeLabel, CssStyles.HSPACE_LEFT_3, CssStyles.LABEL_TEXT_ALIGN_RIGHT);
			toAgeLabel.setWidthFull();

			tfEndAge = new TextField();
			tfEndAge.setWidthFull();
			if (endAge != null) {
				tfEndAge.setValue(endAge.toString());
			}
			tfEndAge.setPlaceholder(I18nProperties.getString(Strings.promptDiseaseConfigurationEndAge));

			cbEndAgeType = new ComboBox(null, Arrays.asList(ApproximateAgeType.values()));
			cbEndAgeType.setWidth(125, Unit.PIXELS);
			cbEndAgeType.setInputPrompt(I18nProperties.getString(Strings.promptDiseaseConfigurationEndAgeType));
			CssStyles.style(CssStyles.VSPACE_NONE, cbEndAgeType, tfEndAge);
			if (endAgeType != null) {
				cbEndAgeType.setValue(endAgeType);
			}

			Button btnDelete = ButtonHelper.createIconButtonWithCaption(null, null, VaadinIcon.TRASH, e -> deleteCallback.run());
			CssStyles.style(CssStyles.HSPACE_LEFT_3, btnDelete);

			add(fromAgeLabel);
			add(tfStartAge);
			add(cbStartAgeType);
			add(toAgeLabel);
			add(tfEndAge);
			add(cbEndAgeType);
			add(btnDelete);

			setWidthFull();
			setPadding(false);
			CssStyles.style(this, CssStyles.VSPACE_4);
		}

		public Integer getStartAge() {
			return StringUtils.isNotBlank(tfStartAge.getValue()) ? Integer.parseInt(tfStartAge.getValue()) : null;
		}

		public ApproximateAgeType getStartAgeType() {
			return (ApproximateAgeType) cbStartAgeType.getValue();
		}

		public Integer getEndAge() {
			return StringUtils.isNotBlank(tfEndAge.getValue()) ? Integer.parseInt(tfEndAge.getValue()) : null;
		}

		public ApproximateAgeType getEndAgeType() {
			return (ApproximateAgeType) cbEndAgeType.getValue();
		}

		public String toEntityString() {
			if (getStartAge() == null || getStartAgeType() == null) {
				return null;
			}

			return getStartAge()
				+ getStartAgeType().getCode()
				+ ((getEndAge() != null) ? ("_" + getEndAge()) : "")
				+ (getEndAgeType() != null ? getEndAgeType().getCode() : "");
		}

		public void setDeleteCallback(Runnable deleteCallback) {
			this.deleteCallback = deleteCallback;
		}
	}
}
