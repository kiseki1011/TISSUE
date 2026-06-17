package com.tissue.shared.dto;

import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@LLMGenerated(llmInvolvement = LLMInvolvement.VIBE_CODED, model = "claude-opus-4-8", evaluation = Evaluation.ACCEPTABLE)
@Schema(description = "Offset-based paginated response.")
public record PageResponse<T>(
        @Schema(description = "Items in this page") List<T> content,

        @Schema(description = "Zero-based index of this page")
        int page,

        @Schema(description = "Requested page size") int size,

        @Schema(description = "Total number of matching elements across all pages")
        long totalElements,

        @Schema(description = "Total number of pages") int totalPages,

        @Schema(description = "Whether a next page is available")
        boolean hasNext) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext());
    }
}
