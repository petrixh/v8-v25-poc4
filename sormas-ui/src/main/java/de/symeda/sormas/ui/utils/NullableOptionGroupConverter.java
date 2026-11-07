package de.symeda.sormas.ui.utils;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

class NullableOptionGroupConverter implements Converter<Object, Object> {

	Object oldValue;

	public NullableOptionGroupConverter(Object initialValue) {
		this.oldValue = initialValue;
	}

	@Override
	public Result<Object> convertToModel(Object ts, ValueContext context) {

		Set newValue = (Set) ts;

		if (newValue != null && !newValue.isEmpty()) {
			switch (newValue.size()) {
			case 1:
				oldValue = newValue.iterator().next();
				break;
			case 2:
				oldValue = oldValue == null
					? newValue.stream().filter(v -> !Objects.equals(v, oldValue)).reduce((first, second) -> second).orElse(null)
					: newValue.stream().filter(v -> !Objects.equals(v, oldValue)).findFirst().get();
				break;

			default:
				throw new IllegalArgumentException(newValue.toString());
			}
		} else {
			oldValue = null;
		}
		return Result.ok(oldValue);
	}

	@Override
	public Object convertToPresentation(Object t, ValueContext context) {
		if (t != null) {
			return Collections.singleton(t);
		} else {
			return null;
		}
	}

}
