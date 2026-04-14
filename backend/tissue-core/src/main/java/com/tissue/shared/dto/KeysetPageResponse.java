package com.tissue.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

@Schema(description = "Keyset-based paginated response.")
public record KeysetPageResponse<T>(
        @Schema(description = "List of items in the current page")
        List<T> content,

        @Schema(description = "Keyset ID for the next page. Null if no more results.") @Nullable
        Long nextKeysetId,

        @Schema(description = "Keyset timestamp for the next page. Used with `nextKeysetId` for composite keyset.")
        @Nullable
        Instant nextKeysetModifiedAt,

        @Schema(description = "Whether more results are available")
        boolean hasNext) {

    public static <T> KeysetPageResponse<T> of(List<T> content, @Nullable Long nextKeysetId) {
        return new KeysetPageResponse<>(content, nextKeysetId, null, nextKeysetId != null);
    }

    public static <T> KeysetPageResponse<T> of(
            List<T> content, @Nullable Long nextKeysetId, @Nullable Instant nextKeysetModifiedAt) {
        boolean hasNext = nextKeysetId != null && nextKeysetModifiedAt != null;
        return new KeysetPageResponse<>(content, nextKeysetId, nextKeysetModifiedAt, hasNext);
    }
}
