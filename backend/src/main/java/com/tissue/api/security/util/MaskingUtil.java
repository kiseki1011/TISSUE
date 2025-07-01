package com.tissue.api.security.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MaskingUtil {

	private static final String MASK_SYMBOL = "*";
	private static final String MASK_ELLIPSIS = "...";

	/**
	 * Masks the middle of a string, keeping a specified number of characters
	 * at the beginning and end visible.
	 * Example: 1234567890 → 12345...7890
	 */
	public static String mask(String input, int unmaskedPrefix, int unmaskedSuffix) {
		if (input == null || input.isBlank()) {
			return "***";
		}

		int length = input.length();
		if (unmaskedPrefix + unmaskedSuffix >= length) {
			return MASK_SYMBOL.repeat(Math.max(length, 3));
		}

		String prefix = input.substring(0, unmaskedPrefix);
		String suffix = input.substring(length - unmaskedSuffix);
		return prefix + MASK_ELLIPSIS + suffix;
	}

	/**
	 * Masks login ID.
	 * Example: loginId = "tiger99" → "t...9"
	 */
	public static String maskLoginId(String loginId) {
		return mask(loginId, 1, 1);
	}

	/**
	 * Masks the local part of an email address.
	 * Example: john.doe@example.com → j...e@example.com
	 */
	public static String maskEmail(String email) {
		if (email == null || !email.contains("@")) {
			return "***";
		}

		String[] parts = email.split("@", 2);
		String localPart = parts[0];
		String domainPart = parts[1];

		String maskedLocal = mask(localPart, 1, 1);
		return maskedLocal + "@" + domainPart;
	}

	/**
	 * Masks all but the first character of a name.
	 * Example: John → J***
	 */
	public static String maskName(String name) {
		if (name == null || name.isBlank()) {
			return "***";
		}
		if (name.length() == 1) {
			return MASK_SYMBOL;
		}
		return name.charAt(0) + MASK_SYMBOL.repeat(name.length() - 1);
	}

	/**
	 * Masks a JWT token, showing only the first and last 5 characters.
	 * Example: abcdefghijklmnopqrstuvwxyz → abcde...vwxyz
	 */
	public static String maskToken(String token) {
		return mask(token, 5, 5);
	}

	/**
	 * Masks numeric IDs (e.g., social security or business IDs),
	 * showing only a defined prefix.
	 * Example: 123456-1234567 → 123456-*******
	 */
	public static String maskNumericId(String id, int unmaskedLength) {
		if (id == null || id.length() <= unmaskedLength) {
			return "***";
		}
		String visible = id.substring(0, unmaskedLength);
		String masked = MASK_SYMBOL.repeat(id.length() - unmaskedLength);
		return visible + masked;
	}
}
