/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2026 SORMAS Foundation gGmbH
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
package de.symeda.sormas.ui.utils.fields;

/**
 * Text fields of the SORMAS field model (V7 {@code AbstractTextField}): {@link TextField}, {@link TextArea},
 * {@link PasswordField}, {@link SormasRichTextArea}.
 */
public interface AbstractTextField extends Field<String> {

	void setMaxLength(int maxLength);

	/**
	 * V7 {@code AbstractTextField.changeVariables} (8.14.3): the value for the text the user entered. Text equal to the
	 * {@link Field#getNullRepresentation() null representation} gives {@code null} when the value was {@code null} or
	 * {@link Field#isNullSettingAllowed() null setting is allowed}; any other text is the value. So an empty field
	 * read from a {@code null} property stays {@code null}, and a field the user cleared gets {@code ""}.
	 *
	 * @param valueWasNull
	 *            whether the field's value was {@code null} before the user's change
	 */
	static String valueFromClient(Field<String> field, String text, boolean valueWasNull) {
		if (text != null && (valueWasNull || field.isNullSettingAllowed()) && text.equals(field.getNullRepresentation())) {
			return null;
		}
		return text;
	}
}
