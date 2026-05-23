package com.tissue.feature.issue.persistence;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.port.repository.IssueSearchRepository;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.domain.Project;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@LLMGenerated(
        llmInvolvement = LLMInvolvement.ASSISTED,
        evaluation = Evaluation.ACCEPTABLE,
        evaluationReason = """
               This implementation is based on the WikiSearchSpecificationAdapter I implemented.
               Only the pagination is changed to use offset instead of keyset.
               """,
        model = "claude-opus-4-7-max",
        reviewedBy = "kiseki1011")
@Repository
@RequiredArgsConstructor
public class IssueSearchSpecificationAdapter implements IssueSearchRepository {

    private final IssueSearchJpaRepository jpaRepository;

    @Override
    public Page<Issue> searchByProject(Project project, IssueSearchCondition condition, Pageable pageable) {
        Specification<Issue> spec = Specification.where(IssueSearchSpecs.inProject(project))
                .and(IssueSearchSpecs.hasPriorities(condition.priorities()))
                .and(IssueSearchSpecs.hasStateCategories(condition.stateCategories()))
                .and(IssueSearchSpecs.hasCurrentStateIds(condition.currentStateIds()))
                .and(IssueSearchSpecs.hasAssignees(condition.assigneeMemberIds()))
                .and(IssueSearchSpecs.inSprints(condition.sprintIds()))
                .and(IssueSearchSpecs.dueAtBetween(condition.dueAtFrom(), condition.dueAtTo()))
                .and(IssueSearchSpecs.startedAtBetween(condition.startedAtFrom(), condition.startedAtTo()))
                .and(IssueSearchSpecs.resolvedAtBetween(condition.resolvedAtFrom(), condition.resolvedAtTo()))
                .and(IssueSearchSpecs.progressBetween(condition.progressMinPercent(), condition.progressMaxPercent()))
                .and(IssueSearchSpecs.hasAllTags(condition.tagIds()))
                .and(IssueSearchSpecs.keywordMatches(condition.keyword()));

        return jpaRepository.findAll(spec, pageable);
    }
}
