package de.symeda.sormas.e2e.playwright.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The kind of app copy a workflow test class runs on: the test configuration (env/README.md "Configurations") and
 * whether the default-password check is on. Each test class gets a copy of this kind in the seed state (fresh database).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface AppCopy {

	/** C1 (default, English) or C2 (Germany, optional features). */
	Config value() default Config.C1;

	/** true: the copy shows the "Security Alert: default password" window after a default-user login (APP_ENV_PWCHECK=on). */
	boolean passwordCheck() default false;

	enum Config {
		C1,
		C2
	}
}
