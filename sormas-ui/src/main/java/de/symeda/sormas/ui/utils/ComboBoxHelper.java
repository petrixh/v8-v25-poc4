package de.symeda.sormas.ui.utils;

import de.symeda.sormas.ui.utils.fields.FilteringMode;

public class ComboBoxHelper {

	/**
	 * Create a default V7 combobox which uses FilteringMode.CONTAINS
	 */
	public static ComboBoxWithPlaceholder createComboBoxV7() {
		ComboBoxWithPlaceholder cb = new ComboBoxWithPlaceholder();
		cb.setFilteringMode(FilteringMode.CONTAINS);
		return cb;
	}
}
