package de.symeda.sormas.ui.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.button.Button;

/**
 * Valo button styles mapped to Lumo variants by {@link CssStyles}: "borderless" and "link" override "primary", as in Valo.
 */
public class CssStylesButtonVariantTest {

	private static Set<String> themes(Button button) {
		return new TreeSet<>(button.getThemeNames());
	}

	@Test
	public void testBorderlessThenPrimaryStaysTertiary() {
		Button button = new Button();
		CssStyles.style(button, CssStyles.BUTTON_BORDERLESS, CssStyles.VSPACE_TOP_NONE, CssStyles.LABEL_PRIMARY);
		assertEquals(Set.of("tertiary"), themes(button));
		assertEquals(Set.of(CssStyles.VSPACE_TOP_NONE), button.getClassNames());
	}

	@Test
	public void testPrimaryThenLinkStaysTertiaryInline() {
		Button button = new Button();
		CssStyles.style(button, CssStyles.BUTTON_PRIMARY);
		CssStyles.style(button, CssStyles.BUTTON_LINK);
		assertEquals(Set.of("tertiary-inline"), themes(button));
	}

	@Test
	public void testLinkRemovedThenPrimaryIsPrimary() {
		Button button = new Button();
		CssStyles.style(button, CssStyles.BUTTON_LINK);
		CssStyles.removeStyles(button, CssStyles.BUTTON_LINK);
		CssStyles.style(button, CssStyles.BUTTON_PRIMARY);
		assertEquals(Set.of("primary"), themes(button));
	}

	@Test
	public void testPrimaryAloneAndWithOtherVariants() {
		Button button = new Button();
		CssStyles.style(button, CssStyles.BUTTON_PRIMARY, CssStyles.BUTTON_SMALL);
		assertEquals(Set.of("primary", "small"), themes(button));
	}
}
