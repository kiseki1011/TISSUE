package com.tissue.api.common.util;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.lang.Nullable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TextNormalizer {
	// TODO: normalizeText
	public static String normalizeLabel(@NonNull String value) {
		return nfc(value.strip());
	}

	public static String normalizeForUniq(@NonNull String value) {
		return lower(nfc(value.strip()));
	}

	public static String nullToEmpty(@Nullable String val) {
		return val == null ? "" : val;
	}

	public static String lower(String value) {
		return value.toLowerCase(Locale.ROOT);
	}

	private static String nfc(String value) {
		return Normalizer.normalize(value, Normalizer.Form.NFC);
	}
}
