package com.tissue.security.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MaskingUtil {

    private static final String MASK = "*";

    /**
     * Masks the local part of an email address.
     *
     * <p>Example: {@code john.doe@example.com → j***e@example.com}
     */
    public static String maskEmail(@Nullable String email) {
        if (email == null || !email.contains("@")) {
            return MASK.repeat(3);
        }

        int atIndex = email.indexOf('@');
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex + 1);

        return mask(localPart, 1, 1) + "@" + domainPart;
    }

    /**
     * Masks all but the first character of a name.
     *
     * <p>Example: {@code John → J***}
     */
    public static String maskName(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return MASK.repeat(3);
        }
        if (name.length() == 1) {
            return MASK;
        }
        return name.charAt(0) + MASK.repeat(name.length() - 1);
    }

    /**
     * Masks a JWT token, showing only the first and last 5 characters.
     *
     * <p>Example: {@code abcdefghijklmnopqrstuvwxyz → abcde***vwxyz}
     */
    public static String maskToken(String token) {
        return mask(token, 5, 5);
    }

    /**
     * Masks the middle of a string, keeping a specified number of characters
     * at the beginning and end visible.
     *
     * <p>Example: {@code 1234567890 → 12345***7890}
     */
    private static String mask(@Nullable String input, int unmaskedPrefix, int unmaskedSuffix) {
        if (input == null || input.isBlank()) {
            return MASK.repeat(3);
        }

        int length = input.length();
        if (unmaskedPrefix + unmaskedSuffix >= length) {
            return MASK.repeat(Math.max(length, 3));
        }

        String prefix = input.substring(0, unmaskedPrefix);
        String suffix = input.substring(length - unmaskedSuffix);
        return prefix + MASK.repeat(3) + suffix;
    }
}
