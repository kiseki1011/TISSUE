package com.tissue.api.common.util;

import java.text.Normalizer;
import java.util.Locale;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TextNormalizer {

	/**
	 * Returns an empty string if the input is null;
	 * otherwise returns the input with leading and trailing Unicode whitespace removed.
	 */
	public static String stripToEmpty(String value) {
		return value == null ? "" : value.strip();
	}

	/**
	 * Returns null if the input is null or consists only of Unicode whitespace;
	 * otherwise returns the original input.
	 */
	public static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value;
	}

	/**
	 * Returns the NFC normalized form;
	 * returns null if input is null.
	 */
	public static String nfc(String value) {
		return value == null ? null : Normalizer.normalize(value, Normalizer.Form.NFC);
	}

	/**
	 * Returns lower-cased value;
	 * returns null if input is null.
	 */
	public static String lower(String value) {
		return value == null ? null : value.toLowerCase(Locale.ENGLISH);
	}
}
