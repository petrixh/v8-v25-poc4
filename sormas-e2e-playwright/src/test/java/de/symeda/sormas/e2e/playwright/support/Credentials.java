package de.symeda.sormas.e2e.playwright.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Passwords of the default test users, read at run time from the app's own DefaultEntityHelper.java (the users that
 * createDefaultEntities makes in test environments). Never printed, logged or written anywhere.
 */
public final class Credentials {

	static final String HELPER = "sormas-api/src/main/java/de/symeda/sormas/api/utils/DefaultEntityHelper.java";
	/** Seed users that log in with another default user's password (env/seed/extend.sh copies the hash). */
	private static final Map<String, String> ALIAS = Map.of("CfgUser", "NatUser");
	private static final Pattern PAIR = Pattern.compile("Pair<>\\(\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]*)\"\\s*\\)");

	private Credentials() {
	}

	public static String passwordOf(String user) {
		String name = ALIAS.getOrDefault(user, user);
		Path file = Path.of(System.getProperty("e2e.appSrc", "..")).resolve(HELPER);
		try {
			Matcher m = PAIR.matcher(Files.readString(file, StandardCharsets.UTF_8));
			while (m.find()) {
				if (m.group(1).equals(name)) {
					return m.group(2);
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("cannot read " + file, e);
		}
		throw new IllegalStateException("no default password for " + user + " in " + file);
	}
}
