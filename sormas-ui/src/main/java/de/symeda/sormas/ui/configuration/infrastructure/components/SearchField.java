package de.symeda.sormas.ui.configuration.infrastructure.components;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.data.value.ValueChangeMode;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.fields.TextField;

public class SearchField extends TextField {

	public SearchField() {
		setId("search");
		setWidth(200, Unit.PIXELS);
		setInputPrompt(I18nProperties.getString(Strings.promptSearch));
		setNullRepresentation("");
		CssStyles.style(this, CssStyles.FORCE_CAPTION);
		// callers used a V7 text change listener (lazy, 400 ms): the value follows typing
		setValueChangeMode(ValueChangeMode.LAZY);
	}
}
