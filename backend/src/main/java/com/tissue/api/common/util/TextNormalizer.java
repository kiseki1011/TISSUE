package com.tissue.api.common.util;

import java.text.Normalizer;
import java.util.Locale;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TextNormalizer {

	public static String normalizeLabel(String value) {
		return nfc(stripToEmpty(value));
	}

	public static String stripToEmpty(String value) {
		return value == null ? "" : value.strip();
	}

	public static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value;
	}

	public static String nfc(String value) {
		return value == null ? null : Normalizer.normalize(value, Normalizer.Form.NFC);
	}

	public static String lower(String value) {
		return value == null ? null : value.toLowerCase(Locale.ROOT);
	}
}
