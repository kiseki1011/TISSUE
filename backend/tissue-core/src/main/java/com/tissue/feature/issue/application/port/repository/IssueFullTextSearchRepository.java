package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.application.dto.IssueSearchCursor;
import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.domain.Project;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * PostgreSQL tsvector + GIN backed full-text search.
 * The adapter expects an {@code issue.search_vector} generated column built from
 * issue_key + title + content, indexed with GIN. See
 * {@code tissue-bootstrap/src/main/resources/db/fts.sql} for the DDL;
 * production should provision it via Flyway.
 *
 * <p>Keyword match uses {@code plainto_tsquery('simple', ...)} via the
 * {@code fts_match} Hibernate function. All non-keyword filters from
 * {@link IssueSearchCondition} are reused (priority, state, assignee, sprint, etc).
 *
 * <p>Cursor (keyset) is the only supported pagination shape.
 */
@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        evaluation = Evaluation.NOT_REVIEWED,
        model = "claude-opus-4-7-max")
public interface IssueFullTextSearchRepository {

    /**
     * Keyset variant. Returns up to {@code limit + 1} issues — the last one
     * (if present) is used by the caller to detect "has next" without paying
     * for a count query. Sort is fixed to {@code priority ASC, id DESC}.
     */
    List<Issue> ftsByProjectAfter(
            Project project, IssueSearchCondition condition, @Nullable IssueSearchCursor cursor, int limit);
}
