package com.tissue.api.workspace.domain;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import com.tissue.api.util.Base62Encoder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkspaceKeyGenerator {

	private static final int KEY_SUFFIX_LENGTH = 8;
	private static final String HASH_ALGORITHM = "SHA-256";
	private static final String WORKSPACE_KEY_PREFIX = "WS";

	public static String generateWorkspaceKey() {
		byte[] randomBytes = new byte[9];
		new SecureRandom().nextBytes(randomBytes);
		String code = Base62Encoder.encode(randomBytes);
		return WORKSPACE_KEY_PREFIX + "-" + code.substring(0, KEY_SUFFIX_LENGTH);
	}

	private byte[] hashFunction(String inputString) {
		try {
			MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
			return md.digest(inputString.getBytes());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
