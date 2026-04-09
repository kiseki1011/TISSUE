package com.tissue.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

@Schema(description = "Cursor-based paginated response.")
public record CursorPageResponse<T>(
        @Schema(description = "List of items in the current page")
        List<T> content,

        @Schema(description = "Cursor ID for the next page. Null if no more results.") @Nullable
        Long nextCursorId,

        @Schema(description = "Cursor timestamp for the next page. Used with `nextCursorId` for composite cursor.")
        @Nullable
        Instant nextCursorModifiedAt,

        @Schema(description = "Whether more results are available")
        boolean hasNext) {

    public static <T> CursorPageResponse<T> of(List<T> content, @Nullable Long nextCursorId) {
        return new CursorPageResponse<>(content, nextCursorId, null, nextCursorId != null);
    }

    public static <T> CursorPageResponse<T> of(
            List<T> content, @Nullable Long nextCursorId, @Nullable Instant nextCursorModifiedAt) {
        boolean hasNext = nextCursorId != null && nextCursorModifiedAt != null;
        return new CursorPageResponse<>(content, nextCursorId, nextCursorModifiedAt, hasNext);
    }
}
