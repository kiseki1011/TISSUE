package com.tissue.api.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TextNormalizer {

	public static String normalizeLabel(String value) {
		Objects.requireNonNull(value);
		return nfc(value.strip());
	}

	public static String normalizeForUniq(String value) {
		Objects.requireNonNull(value);
		return lower(nfc(value.strip()));
	}

	public static String nullToEmpty(String val) {
		return val == null ? "" : val;
	}

	public static String lower(String value) {
		return value.toLowerCase(Locale.ROOT);
	}

	private static String nfc(String value) {
		return Normalizer.normalize(value, Normalizer.Form.NFC);
	}
}
