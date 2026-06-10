package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.feature.issue.application.port.repository.IssueFullTextSearchRepository;
import com.tissue.feature.issue.application.port.usecase.IssueFullTextSearchUseCase;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueFullTextSearchService implements IssueFullTextSearchUseCase {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final ProjectMemberQueryRepository projectMemberQueryRepository;
    private final IssueFullTextSearchRepository ftsRepository;
    private final IssueSearchPolicy policy;

    @LLMGenerated(
            llmInvolvement = LLMInvolvement.ASSISTED,
            evaluation = Evaluation.NOT_REVIEWED,
            evaluationReason = "Integration test passes, but still needs review. Needs review of IssueSearchSpecs.",
            model = "claude-opus-4-8")
    @Override
    public Page<IssueSummary> ftsByProjectRanked(
            ProjectIdentifier pid, IssueSearchCondition condition, int page, int size, Long actorMemberId) {
        Project project = projectFinder.getByProjectKey(pid.projectKey());
        projectMemberFinder.getBy(project, actorMemberId);

        if (condition.keyword() == null || condition.keyword().isBlank()) {
            return Page.empty();
        }

        IssueSearchCondition resolved = policy.resolveCurrentSprint(condition, project);
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));

        return ftsRepository.ftsByProjectRanked(project, resolved, pageable).map(IssueSummary::from);
    }

    @LLMGenerated(
            llmInvolvement = LLMInvolvement.ASSISTED,
            evaluation = Evaluation.NOT_REVIEWED,
            evaluationReason = "Integration test passes, but still needs review.",
            model = "claude-opus-4-8")
    @Override
    public Page<IssueSummary> ftsAllRanked(IssueSearchCondition condition, int page, int size, Long actorMemberId) {
        if (condition.keyword() == null || condition.keyword().isBlank()) {
            return Page.empty();
        }

        // Authz scoping: restrict to projects the caller is a member of. currentSprintOnly is not
        // resolved here — it is a project-specific convenience that has no meaning instance-wide.
        Set<Long> projectIds = projectMemberQueryRepository.findProjectIdsByMemberId(actorMemberId);
        if (projectIds.isEmpty()) {
            return Page.empty();
        }

        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));

        return ftsRepository.ftsAllRanked(projectIds, condition, pageable).map(IssueSummary::from);
    }

    private static int clampSize(int requested) {
        if (requested <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requested, MAX_PAGE_SIZE);
    }
}
