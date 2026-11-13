package de.symeda.sormas.e2e.playwright.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The workflow changes data the app keeps in memory (users, roles and rights, infrastructure, feature or disease
 * configuration, customizable enums, sessions). With e2e.resetMode=mixed the copy gets the restart reset after this
 * class; after other classes the faster database-only reset.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DirtiesAppMemory {

	String value();
}
