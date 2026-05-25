package com.tissue.shared.dto;

import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Generic keyset-paginated response. Unlike {@code KeysetPageResponse} which
 * exposes the cursor fields directly, this hides them inside an opaque token
 * that the server alone can decode. Used by endpoints whose cursor encoding
 * may evolve.
 */
@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        model = "claude-opus-4-7-max",
        evaluation = Evaluation.ACCEPTABLE,
        evaluationReason = "Used the cursor implementation (before using keyset).",
        reviewedBy = "kiseki1011")
@Schema(description = "Cursor-based paginated response with an opaque next-page token.")
public record CursorPage<T>(
        @Schema(description = "List of items in this page") List<T> content,

        @Schema(description = "Opaque token to pass as `?cursor=` for the next page. Null when no more results.")
        @Nullable
        String nextCursor,

        @Schema(description = "Whether more results are available")
        boolean hasNext) {

    public static <T> CursorPage<T> of(List<T> content, @Nullable String nextCursor) {
        return new CursorPage<>(content, nextCursor, nextCursor != null);
    }

    public static <T> CursorPage<T> empty() {
        return new CursorPage<>(List.of(), null, false);
    }
}
