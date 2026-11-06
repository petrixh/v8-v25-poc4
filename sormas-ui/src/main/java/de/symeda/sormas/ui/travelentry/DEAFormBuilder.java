package de.symeda.sormas.ui.travelentry;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;

import de.symeda.sormas.api.travelentry.DeaContentEntry;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.SormasFieldGroupFieldFactory;
import de.symeda.sormas.ui.utils.fields.Field;
import de.symeda.sormas.ui.utils.fields.TextField;

public class DEAFormBuilder {

	private List<DeaContentEntry> deaContentEntries;
	private FormLayout gridLayout;
	private Boolean isCreate;
	private List<Field<?>> fields;

	public DEAFormBuilder(List<DeaContentEntry> deaContentEntries, Boolean isCreate) {
		this.deaContentEntries = deaContentEntries;
		// was a GridLayout of 2 columns, filled row by row
		this.gridLayout = new FormLayout();
		this.gridLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
		this.gridLayout.setWidthFull();
		CssStyles.style(this.gridLayout, CssStyles.VSPACE_3);
		this.isCreate = isCreate;
		this.fields = new ArrayList<>();
	}

	public List<DeaContentEntry> getDeaContentEntries() {
		ArrayList<DeaContentEntry> deaContentValueEntries = new ArrayList<>(this.deaContentEntries);
		for (int i = 0; i < deaContentValueEntries.size(); i++) {
			Field<?> field = fields.get(i);
			deaContentValueEntries.get(i).setValue((String) field.getValue());
		}
		return deaContentValueEntries;
	}

	public void buildForm() {
		SormasFieldGroupFieldFactory fieldFactory = new SormasFieldGroupFieldFactory(new FieldVisibilityCheckers(), UiFieldAccessCheckers.getNoop());

		for (DeaContentEntry deaContentEntry : deaContentEntries) {
			final TextField textField = fieldFactory.createField(String.class, TextField.class);
			textField.setWidthFull();
			final String caption = deaContentEntry.getCaption();
			textField.setId(caption);
			CssStyles.style(textField, CssStyles.TEXTFIELD_ROW);
			textField.setCaption(caption);
			if (!isCreate) {
				textField.setValue(deaContentEntry.getValue());
			}
			gridLayout.add(textField);
			fields.add(textField);
		}
	}

	public Component getLayout() {
		return gridLayout;
	}
}
