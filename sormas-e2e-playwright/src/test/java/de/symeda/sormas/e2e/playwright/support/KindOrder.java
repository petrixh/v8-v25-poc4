package de.symeda.sormas.e2e.playwright.support;

import java.util.Comparator;

import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;

/**
 * Classes needing a rare copy kind first (Keycloak, the default-password check on, then C2), then C1, each group by name: the
 * rare copies start together with the first C1 copies instead of replacing a C1 copy at the end of the run.
 */
public class KindOrder implements ClassOrderer {

	private static int rank(ClassDescriptor c) {
		AppCopy k = c.findAnnotation(AppCopy.class).orElse(null);
		if (k == null) {
			return 3;
		}
		return k.keycloak() || k.passwordCheck() ? 0 : k.value() == AppCopy.Config.C2 ? 1 : 2;
	}

	@Override
	public void orderClasses(ClassOrdererContext context) {
		context.getClassDescriptors().sort(Comparator.comparingInt(KindOrder::rank).thenComparing(c -> c.getTestClass().getSimpleName()));
	}
}
