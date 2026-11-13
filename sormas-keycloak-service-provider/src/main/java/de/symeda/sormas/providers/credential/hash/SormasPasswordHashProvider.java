/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

import org.keycloak.Config;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.PasswordHashProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.credential.PasswordCredentialModel;

/**
 * Custom Password Hash Provider to be used as a backwards compatible mechanism for users migrated from the SORMAS default Authentication
 * Provider.
 *
 * @author Alex Vidrean
 * @since 14-Dec-20
 * @see <a href="https://www.keycloak.org/docs/latest/server_development/#_providers">Service Provider Interfaces (SPI)</a>
 */
public class SormasPasswordHashProvider implements PasswordHashProviderFactory, PasswordHashProvider {

	public static final String ID = "sormas-sha256";

	@Override
	public boolean policyCheck(PasswordPolicy policy, PasswordCredentialModel credential) {
		return credential.getPasswordCredentialData().getHashIterations() == policy.getHashIterations()
			&& ID.equals(credential.getPasswordCredentialData().getAlgorithm());
	}

	@Override
	public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
		String salt = getSalt();
		String encodedPassword = encodePassword(rawPassword, salt);
		return PasswordCredentialModel.createFromValues(ID, salt.getBytes(), iterations, encodedPassword);
	}

	@Override
	public boolean verify(String rawPassword, PasswordCredentialModel credential) {
		return encodePassword(rawPassword, new String(credential.getPasswordSecretData().getSalt()))
			.equals(credential.getPasswordSecretData().getValue());
	}

	@Override
	public PasswordHashProvider create(KeycloakSession session) {
		return this;
	}

	@Override
	public void init(Config.Scope config) {
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
	}

	public void close() {
	}

	@Override
	public String getId() {
		return ID;
	}

	/**
	 * Characters of a salt, as {@code PasswordHelper.createPass} in sormas-api: a-z without l and v, A-Z without I, O and V,
	 * 2-9.
	 */
	static final String SALT_CHARS = "abcdefghijkmnopqrstuwxyzABCDEFGHJKLMNPQRSTUWXYZ23456789";

	private static final SecureRandom RANDOM = new SecureRandom();

	private String getSalt() {
		StringBuilder salt = new StringBuilder(16);
		for (int i = 0; i < 16; i++) {
			salt.append(SALT_CHARS.charAt(RANDOM.nextInt(SALT_CHARS.length())));
		}
		return salt.toString();
	}

	/**
	 * The SORMAS password hash, as {@code PasswordHelper.encodePassword} in sormas-api (SHA-256 of password + salt, lower
	 * case hex). Not taken from sormas-api: this provider runs inside Keycloak 21 on Java 17, sormas-api is built for Java 21
	 * and needs jakarta.validation (SormasPasswordHashProviderTest keeps the two equal).
	 */
	static String encodePassword(String rawPassword, String salt) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest((rawPassword + salt).getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
