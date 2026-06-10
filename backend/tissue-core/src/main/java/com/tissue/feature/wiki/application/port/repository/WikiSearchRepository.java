package com.tissue.feature.wiki.application.port.repository;

import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@LLMGenerated(
        llmInvolvement = LLMInvolvement.ASSISTED,
        model = "claude-opus-4-8",
        evaluation = Evaluation.NOT_REVIEWED,
        evaluationReason = "Test passes, but code not reviewed.")
public interface WikiSearchRepository {

    /**
     * Relevance-ranked, offset-paginated document search. {@code keyword} (tsvector match on
     * title + content) and {@code tagIds} (ANY-of) are both optional; ordering is by
     * {@code ts_rank} when a keyword is present, otherwise by {@code lastModifiedAt} DESC.
     */
    Page<WikiDocument> search(@Nullable String keyword, @Nullable Set<Long> tagIds, Pageable pageable);
}
