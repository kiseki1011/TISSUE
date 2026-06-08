package com.tissue.feature.issue.adapter.persistence;

import com.tissue.feature.issue.application.dto.IssueSearchCursor;
import com.tissue.feature.issue.application.port.repository.IssueListQueryRepository;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/**
 * Each query uses a fixed, intent specific set of specs.
 * No keyword, no open filter.
 */
@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        model = "claude-opus-4-8",
        evaluation = Evaluation.ACCEPTABLE,
        evaluationReason = "Wraps around specs that were already tested before.",
        reviewedBy = "kiseki1011")
@Repository
@RequiredArgsConstructor
public class IssueListQueryAdapter implements IssueListQueryRepository {

    private static final Sort CURSOR_SORT = Sort.by(Sort.Order.asc("priority"), Sort.Order.desc("id"));

    private final IssueSearchJpaRepository jpaRepository;

    @Override
    public List<Issue> findAssignedAfter(
            Project project,
            Set<Long> memberIds,
            Set<StateCategory> categories,
            @Nullable IssueSearchCursor cursor,
            int limit) {
        Specification<Issue> spec = Specification.where(IssueSearchSpecs.inProject(project))
                .and(IssueSearchSpecs.hasAssignees(memberIds))
                .and(IssueSearchSpecs.hasStateCategories(categories))
                .and(IssueSearchSpecs.afterCursor(cursor));
        return run(spec, limit);
    }

    @Override
    public List<Issue> findBacklogAfter(
            Project project, Set<StateCategory> categories, @Nullable IssueSearchCursor cursor, int limit) {
        Specification<Issue> spec = Specification.where(IssueSearchSpecs.inProject(project))
                .and(IssueSearchSpecs.noSprint())
                .and(IssueSearchSpecs.hasStateCategories(categories))
                .and(IssueSearchSpecs.afterCursor(cursor));
        return run(spec, limit);
    }

    @Override
    public List<Issue> findInSprintAfter(
            Project project, Long sprintId, @Nullable IssueSearchCursor cursor, int limit) {
        Specification<Issue> spec = Specification.where(IssueSearchSpecs.inProject(project))
                .and(IssueSearchSpecs.inSprints(Set.of(sprintId)))
                .and(IssueSearchSpecs.afterCursor(cursor));
        return run(spec, limit);
    }

    private List<Issue> run(Specification<Issue> spec, int limit) {
        return jpaRepository.findBy(
                spec, q -> q.sortBy(CURSOR_SORT).limit(limit + 1).all());
    }
}
