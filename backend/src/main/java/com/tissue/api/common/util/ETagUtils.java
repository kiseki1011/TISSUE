package com.tissue.api.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ETagUtils {

	// "5" → 5  (따옴표 있으면 제거)
	public static Long parseIfMatch(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}

		String val = raw.trim();
		if (val.startsWith("\"") && val.endsWith("\"")) {
			val = val.substring(1, val.length() - 1);
		}

		return Long.parseLong(val);
	}

	// ETag는 반드시 큰따옴표로 감싼다
	public static String quote(long version) {
		return "\"" + version + "\"";
	}
}
