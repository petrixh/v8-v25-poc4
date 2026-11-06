package de.symeda.sormas.ui.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import java.util.Locale;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.data.binder.ValueContext;

public class StringToAngularLocationConverterTest {

	@Test
	public void testConvertToModel() {

		StringToAngularLocationConverter c = new StringToAngularLocationConverter();

		assertThat(toModel(c, null, Locale.GERMANY), is((Double) null));
		assertThat(toModel(c, null, Locale.ENGLISH), is((Double) null));

		//@formatter:off
		Stream.of(
			Pair.of("1,21", Locale.GERMANY),
			Pair.of("1,21", Locale.ENGLISH),
			Pair.of("1.21", Locale.GERMANY),
			Pair.of("1.21", Locale.ENGLISH)
		)
		.map(p -> toModel(c, p.getLeft(), p.getRight()))
		.forEach(v -> assertThat(v, closeTo(1.21, 0.00001)));
		//@formatter:on
	}

	@Test
	public void testConvertToPresentation() {

		StringToAngularLocationConverter c = new StringToAngularLocationConverter();

		assertThat(c.convertToPresentation(null, new ValueContext(Locale.GERMANY)), is((String) null));
		assertThat(c.convertToPresentation(null, new ValueContext(Locale.ENGLISH)), is((String) null));

		assertThat(c.convertToPresentation(1.21D, new ValueContext(Locale.GERMANY)), is("1,21"));
		assertThat(c.convertToPresentation(1.21D, new ValueContext(Locale.ENGLISH)), is("1.21"));
	}

	private static Double toModel(StringToAngularLocationConverter c, String value, Locale locale) {
		return c.convertToModel(value, new ValueContext(locale)).getOrThrow(IllegalArgumentException::new);
	}
}
