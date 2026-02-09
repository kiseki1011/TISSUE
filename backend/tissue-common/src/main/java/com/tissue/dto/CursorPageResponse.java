package com.tissue.dto;

import java.util.List;
import org.jspecify.annotations.Nullable;

public record CursorPageResponse<T>(
        List<T> content, @Nullable Long nextCursorId, boolean hasNext) {

    public static <T> CursorPageResponse<T> of(List<T> content, @Nullable Long nextCursorId) {
        return new CursorPageResponse<>(content, nextCursorId, nextCursorId != null);
    }
}
