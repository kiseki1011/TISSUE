package com.tissue.support.util;

import java.text.Normalizer;
import java.util.Locale;

public class TextNormalizer {

    private TextNormalizer() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String normalizeText(String value) {
        return nfc(value.strip());
    }

    public static String normalizeForUniq(String value) {
        return lower(nfc(value.strip()));
    }

    public static String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static String nfc(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFC);
    }
}
