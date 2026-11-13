/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2026 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.providers.credential.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.keycloak.models.credential.PasswordCredentialModel;

import de.symeda.sormas.api.utils.PasswordHelper;

/**
 * The provider must hash as SORMAS does ({@link PasswordHelper}), so that users synced from SORMAS to Keycloak (their SORMAS
 * hash and salt, algorithm sormas-sha256) can log in with their SORMAS password; and it must load in Keycloak 21 (Java 17).
 */
class SormasPasswordHashProviderTest {

	private final SormasPasswordHashProvider provider = new SormasPasswordHashProvider();

	@Test
	void hashEqualsSormasHash() {
		for (String[] pw : new String[][] {
			{ "Secret1!secret", "AbCdEfGh23456789" },
			{ "", "x" },
			{ "äöü€ with spaces", "k9Mn" } }) {
			assertEquals(PasswordHelper.encodePassword(pw[0], pw[1]), SormasPasswordHashProvider.encodePassword(pw[0], pw[1]), pw[1]);
		}
	}

	/** A credential as KeycloakService (sormas-backend) writes it for a synced user: SORMAS hash, salt = the user's seed. */
	@Test
	void verifiesSyncedSormasCredential() {
		String seed = PasswordHelper.createPass(16);
		String hash = PasswordHelper.encodePassword("Synced-user-pw1", seed);
		PasswordCredentialModel credential = PasswordCredentialModel.createFromValues(SormasPasswordHashProvider.ID, seed.getBytes(), 1, hash);

		assertTrue(provider.verify("Synced-user-pw1", credential));
		assertFalse(provider.verify("Synced-user-pw2", credential));
	}

	@Test
	void encodedCredentialVerifies() {
		PasswordCredentialModel credential = provider.encodedCredential("New-password-1", 1);

		assertEquals(SormasPasswordHashProvider.ID, credential.getPasswordCredentialData().getAlgorithm());
		String salt = new String(credential.getPasswordSecretData().getSalt());
		assertEquals(16, salt.length());
		assertTrue(salt.chars().allMatch(c -> SormasPasswordHashProvider.SALT_CHARS.indexOf(c) >= 0), salt);
		assertTrue(provider.verify("New-password-1", credential));
		assertEquals(PasswordHelper.encodePassword("New-password-1", salt), credential.getPasswordSecretData().getValue());
	}

	@Test
	void saltCharactersEqualSormasPasswordCharacters() throws ReflectiveOperationException {
		Field chars = PasswordHelper.class.getDeclaredField("PASSWORD_CHARS");
		chars.setAccessible(true);
		assertEquals(new String((char[]) chars.get(null)), SormasPasswordHashProvider.SALT_CHARS);
	}

	/** Keycloak 21.1.2 runs on Java 17: the provider's class files must be Java 17 (major version 61) or older. */
	@Test
	void classFileForJava17() throws IOException {
		try (InputStream in = SormasPasswordHashProvider.class.getResourceAsStream("SormasPasswordHashProvider.class")) {
			byte[] header = in.readNBytes(8);
			int major = ((header[6] & 0xff) << 8) | (header[7] & 0xff);
			assertTrue(major <= 61, "class file major version " + major);
		}
	}
}
