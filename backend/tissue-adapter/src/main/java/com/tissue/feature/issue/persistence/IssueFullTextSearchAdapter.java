package com.tissue.feature.issue.persistence;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.port.repository.IssueFullTextSearchRepository;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.domain.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/**
 * Reuses {@link IssueSearchJpaRepository} (the same {@code JpaSpecificationExecutor}
 * as the LIKE search adapter) but swaps {@link IssueSearchSpecs#keywordMatches} for
 * {@link IssueSearchSpecs#ftsKeywordMatches}. All other filter specs (priority,
 * state, assignee, sprint, tags, date ranges, progress) are reused as-is.
 */
@Repository
@RequiredArgsConstructor
public class IssueFullTextSearchAdapter implements IssueFullTextSearchRepository {

    private final IssueSearchJpaRepository jpaRepository;

    @Override
    public Page<Issue> ftsByProject(Project project, IssueSearchCondition condition, Pageable pageable) {
        Specification<Issue> spec = Specification.where(IssueSearchSpecs.inProject(project))
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
        return jpaRepository.findAll(spec, pageable);
    }
}
