package com.tissue.shared.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.shared.exception.InvalidCursorException;
import java.io.IOException;
import java.util.Base64;
import org.jspecify.annotations.Nullable;

/**
 * Opaque pagination cursor codec shared by all keyset-paginated endpoints (paired with
 * {@link CursorPage}). A cursor payload (a small record holding the last row's keyset values) is
 * encoded as Base64url(JSON); clients must treat the token as opaque.
 *
 * <p>The payload record's shape is bound to its query's sort order — if the sort changes, the
 * payload (and thus the token schema) must change with it.
 */
public final class Cursor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Cursor() {}

    public static String encode(Object payload) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(MAPPER.writeValueAsBytes(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to encode cursor", e);
        }
    }

    public static <T> @Nullable T decode(@Nullable String token, Class<T> type) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(Base64.getUrlDecoder().decode(token), type);
        } catch (IllegalArgumentException | IOException e) {
            throw new InvalidCursorException(token, e);
        }
    }
}
