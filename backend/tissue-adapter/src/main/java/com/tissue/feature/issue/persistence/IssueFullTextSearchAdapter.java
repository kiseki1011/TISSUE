package com.tissue.feature.issue.persistence;

import com.tissue.feature.issue.application.dto.IssueSearchCursor;
import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.port.repository.IssueFullTextSearchRepository;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.domain.Project;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/**
 * Reuses {@link IssueSearchJpaRepository} (the same {@code JpaSpecificationExecutor}
 * as the LIKE search adapter) but swaps {@link IssueSearchSpecs#keywordMatches} for
 * {@link IssueSearchSpecs#ftsKeywordMatches}. All other filter specs (priority,
 * state, assignee, sprint, tags, date ranges) are reused as-is.
 */
@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        evaluation = Evaluation.NOT_REVIEWED,
        model = "claude-opus-4-7-max")
@Repository
@RequiredArgsConstructor
public class IssueFullTextSearchAdapter implements IssueFullTextSearchRepository {

    private static final Sort CURSOR_SORT = Sort.by(Sort.Order.asc("priority"), Sort.Order.desc("id"));

    private final IssueSearchJpaRepository jpaRepository;

    @Override
    public Page<Issue> ftsByProject(Project project, IssueSearchCondition condition, Pageable pageable) {
        return jpaRepository.findAll(commonSpec(project, condition), pageable);
    }

    @Override
    public List<Issue> ftsByProjectAfter(
            Project project, IssueSearchCondition condition, @Nullable IssueSearchCursor cursor, int limit) {
        Specification<Issue> spec = commonSpec(project, condition).and(IssueSearchSpecs.afterCursor(cursor));

        return jpaRepository.findBy(
                spec, q -> q.sortBy(CURSOR_SORT).limit(limit + 1).all());
    }

    private Specification<Issue> commonSpec(Project project, IssueSearchCondition condition) {
        return Specification.where(IssueSearchSpecs.inProject(project))
                .and(IssueSearchSpecs.hasPriorities(condition.priorities()))
                .and(IssueSearchSpecs.hasStateCategories(condition.stateCategories()))
                .and(IssueSearchSpecs.hasCurrentStateIds(condition.currentStateIds()))
                .and(IssueSearchSpecs.hasAuthors(condition.authorMemberIds()))
                .and(IssueSearchSpecs.hasAssignees(condition.assigneeMemberIds()))
                .and(IssueSearchSpecs.hasReviewers(condition.reviewerMemberIds()))
                .and(IssueSearchSpecs.hasSubscribers(condition.subscriberMemberIds()))
                .and(IssueSearchSpecs.inSprints(condition.sprintIds()))
                .and(IssueSearchSpecs.dueAtBetween(condition.dueAtFrom(), condition.dueAtTo()))
                .and(IssueSearchSpecs.hasAllTags(condition.tagIds()))
                .and(IssueSearchSpecs.ftsKeywordMatches(condition.keyword()));
    }
}
