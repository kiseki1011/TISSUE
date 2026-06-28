package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.feature.issue.application.dto.response.MyReviewStatusView;
import com.tissue.feature.issue.application.port.repository.IssueFullTextSearchRepository;
import com.tissue.feature.issue.application.port.repository.IssueReviewerQueryRepository;
import com.tissue.feature.issue.application.port.usecase.IssueFullTextSearchUseCase;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.ReviewStatus;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final IssueReviewerQueryRepository reviewerQueryRepository;
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

        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));

        IssueSearchCondition resolved = policy.resolveCurrentSprint(condition, project);

        return withMyReviewStatus(ftsRepository.ftsByProjectRanked(project, resolved, pageable), actorMemberId);
    }

    /**
     * Full text search with rank.
     *
     * <p>A keyword-less request is allowed only when it carries filters. With neither keyword nor filter
     * there is nothing to scope by, so return empty.
     */
    @LLMGenerated(
            llmInvolvement = LLMInvolvement.ASSISTED,
            evaluation = Evaluation.NOT_REVIEWED,
            evaluationReason = "Integration test passes, but still needs review.",
            model = "claude-opus-4-8")
    @Override
    public Page<IssueSummary> ftsAllRanked(IssueSearchCondition condition, int page, int size, Long actorMemberId) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));

        boolean blankKeyword =
                condition.keyword() == null || condition.keyword().isBlank();
        if (blankKeyword && !condition.hasActiveFilters()) {
            return Page.empty(pageable);
        }

        // Authorization scoping
        Set<Long> projectIds = projectMemberQueryRepository.findProjectIdsByMemberId(actorMemberId);
        if (projectIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return withMyReviewStatus(ftsRepository.ftsAllRanked(projectIds, condition, pageable), actorMemberId);
    }

    /**
     * Enriches a page of issues with the caller's own review status per issue, in a single query
     * (keyed by issue id) rather than N+1 lookups. An issue the caller does not review maps to a
     * null status.
     */
    @LLMGenerated(
            llmInvolvement = LLMInvolvement.ASSISTED,
            evaluation = Evaluation.NOT_REVIEWED,
            evaluationReason = "Integration test passes, but still needs review.",
            model = "claude-opus-4-8")
    private Page<IssueSummary> withMyReviewStatus(Page<Issue> issues, Long actorMemberId) {
        Set<Long> issueIds = issues.getContent().stream().map(Issue::getId).collect(Collectors.toSet());
        Map<Long, ReviewStatus> myStatuses = issueIds.isEmpty()
                ? Map.of()
                : reviewerQueryRepository.findMyReviewStatuses(actorMemberId, issueIds).stream()
                        .collect(Collectors.toMap(MyReviewStatusView::issueId, MyReviewStatusView::status));
        return issues.map(issue -> IssueSummary.from(issue, myStatuses.get(issue.getId())));
    }

    private static int clampSize(int requested) {
        if (requested <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requested, MAX_PAGE_SIZE);
    }
}
