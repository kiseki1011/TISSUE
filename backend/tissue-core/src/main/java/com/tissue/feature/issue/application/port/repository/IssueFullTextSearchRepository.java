package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * PostgreSQL tsvector + GIN backed full-text search.
 * The adapter expects an {@code issue.search_vector} generated column built from
 * issue_key + title + content, indexed with GIN. See {@code loadtest/seed/fts.sql}
 * for the DDL; production should provision it via Flyway.
 *
 * <p>Keyword match uses {@code plainto_tsquery('simple', ...)} via the
 * {@code fts_match} Hibernate function. All non-keyword filters from
 * {@link IssueSearchCondition} are reused (priority, state, assignee, sprint, etc).
 */
public interface IssueFullTextSearchRepository {

    Page<Issue> ftsByProject(Project project, IssueSearchCondition condition, Pageable pageable);
}
