package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.domain.Project;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * PostgreSQL tsvector + GIN backed full-text search.
 * The adapter expects an {@code issue.search_vector} generated column built from
 * issue_key + title + content, indexed with GIN.
 * See {@code tissue-bootstrap/src/main/resources/db/fts.sql} for the DDL.
 * Production should provision it via Flyway.
 *
 * <p>Keyword match uses {@code plainto_tsquery('simple', ...)} via the
 * {@code fts_match} Hibernate function. All non-keyword filters from
 * {@link IssueSearchCondition} are reused (priority, state, assignee, sprint, etc.).
 *
 * <p>Relevance-ranked, offset-paginated: the keyword search reorders results by
 * {@code ts_rank}, so a keyset on stable columns no longer applies.
 */
@LLMGenerated(
        llmInvolvement = LLMInvolvement.ASSISTED,
        model = "claude-opus-4-8",
        evaluation = Evaluation.ACCEPTABLE,
        evaluationReason = "This is just a port, needs to review the implementation.",
        reviewedBy = "kiseki1011")
public interface IssueFullTextSearchRepository {

    /**
     * Relevance-ranked, offset-paginated full-text search. Orders by
     * {@code ts_rank(search_vector, keyword) DESC}, then {@code priority ASC, id DESC}
     * as deterministic tiebreakers.
     */
    Page<Issue> ftsByProjectRanked(Project project, IssueSearchCondition condition, Pageable pageable);
}
