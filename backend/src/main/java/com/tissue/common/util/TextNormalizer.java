package com.tissue.common.util;

import java.text.Normalizer;
import java.util.Locale;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TextNormalizer {

	public static String normalizeText(@NonNull String value) {
		return nfc(value.strip());
	}

	public static String normalizeForUniq(@NonNull String value) {
		return lower(nfc(value.strip()));
	}

	public static String lower(String value) {
		return value.toLowerCase(Locale.ROOT);
	}

	private static String nfc(String value) {
		return Normalizer.normalize(value, Normalizer.Form.NFC);
	}
}
