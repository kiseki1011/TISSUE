package com.tissue.feature.issue.adapter.persistence;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.port.repository.IssueFullTextSearchRepository;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.domain.Project;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/**
 * Relevance-ranked FTS adapter.
 *
 * <p>Uses {@link IssueSearchSpecs#ftsKeywordMatches} for the keyword predicate and
 * {@link IssueSearchSpecs#orderByRelevance}
 * for the {@code ts_rank} ordering, and reuses the other filter specs (priority, state, assignee, sprint,
 * tags, date ranges) without modification. Both the project-scoped and instance-wide searches share
 * the same {@link #filters} chain, differing only in the project predicate.
 */
@LLMGenerated(
        llmInvolvement = LLMInvolvement.ASSISTED,
        evaluation = Evaluation.NOT_REVIEWED,
        evaluationReason = "Integration test passes, but performance and edge cases have not been tested.",
        model = "claude-opus-4-8")
@Repository
@RequiredArgsConstructor
public class IssueFullTextSearchAdapter implements IssueFullTextSearchRepository {

    private final IssueSearchJpaRepository jpaRepository;

    @Override
    public Page<Issue> ftsByProjectRanked(Project project, IssueSearchCondition condition, Pageable pageable) {
        Specification<Issue> spec = Specification.where(IssueSearchSpecs.inProject(project))
                .and(filters(condition))
                .and(IssueSearchSpecs.orderByRelevance(condition.keyword()));

        return jpaRepository.findAll(spec, pageable);
    }

    @Override
    public Page<Issue> ftsAllRanked(Set<Long> projectIds, IssueSearchCondition condition, Pageable pageable) {
        Specification<Issue> spec = Specification.where(IssueSearchSpecs.inProjectIds(projectIds))
                .and(filters(condition))
                .and(IssueSearchSpecs.orderByRelevance(condition.keyword()));

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<Issue> filters(IssueSearchCondition condition) {
        return Specification.where(IssueSearchSpecs.hasPriorities(condition.priorities()))
                .and(IssueSearchSpecs.hasStateCategories(condition.stateCategories()))
                .and(IssueSearchSpecs.hasCurrentStateIds(condition.currentStateIds()))
                .and(IssueSearchSpecs.hasAuthors(condition.authorMemberIds()))
                .and(IssueSearchSpecs.hasAssignees(condition.assigneeMemberIds()))
                .and(IssueSearchSpecs.hasReviewers(condition.reviewerMemberIds(), condition.reviewerStatuses()))
                .and(IssueSearchSpecs.hasSubscribers(condition.subscriberMemberIds()))
                .and(IssueSearchSpecs.inSprints(condition.sprintIds()))
                .and(IssueSearchSpecs.dueAtBetween(condition.dueAtFrom(), condition.dueAtTo()))
                .and(IssueSearchSpecs.hasAllTags(condition.tagIds()))
                .and(IssueSearchSpecs.ftsKeywordMatches(condition.keyword()));
    }
}
