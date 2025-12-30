package com.tissue.common.util;

import org.jspecify.annotations.Nullable;

public final class EtagUtil {

    private EtagUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // removes quotation marks ("")
    // example: "5" → 5
    public static @Nullable Long parseIfMatch(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String val = raw.trim();
        if (val.startsWith("\"") && val.endsWith("\"")) {
            val = val.substring(1, val.length() - 1);
        }

        return Long.parseLong(val);
    }

    // ETag must be wrapped with quotation marks
    public static String quote(long version) {
        return "\"" + version + "\"";
    }
}
