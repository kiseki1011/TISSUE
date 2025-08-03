package com.tissue.api.global.key;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import com.tissue.api.util.Base62Encoder;

public class WorkspaceKeyGenerator {

	private static final int KEY_SUFFIX_LENGTH = 8;
	private static final String HASH_ALGORITHM = "SHA-256";

	public static String generateWorkspaceKeySuffix() {
		byte[] randomBytes = new byte[9];
		new SecureRandom().nextBytes(randomBytes);
		String code = Base62Encoder.encode(randomBytes);
		return code.substring(0, KEY_SUFFIX_LENGTH);
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
