package com.tissue.feature.issue.application.service;

import com.tissue.feature.activitylog.application.port.repository.ActivityLogQueryRepository;
import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.feature.issue.application.dto.response.MyReviewStatusView;
import com.tissue.feature.issue.application.port.repository.IssueFullTextSearchRepository;
import com.tissue.feature.issue.application.port.repository.IssueReviewerQueryRepository;
import com.tissue.feature.issue.application.port.usecase.IssueFullTextSearchUseCase;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.ReviewStatus;
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.dto.PageSizes;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueFullTextSearchService implements IssueFullTextSearchUseCase {

    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final ProjectMemberQueryRepository projectMemberQueryRepository;
    private final IssueFullTextSearchRepository ftsRepository;
    private final IssueReviewerQueryRepository reviewerQueryRepository;
    private final IssueSearchPolicy policy;
    private final MemberFinder memberFinder;
    private final ActivityLogQueryRepository activityLogQueryRepository;

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

        Pageable pageable = PageSizes.clampedPageRequest(page, size);

        IssueSearchCondition resolved = policy.resolveCurrentSprint(condition, project);

        return enrich(ftsRepository.ftsByProjectRanked(project, resolved, pageable), actorMemberId);
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
        Pageable pageable = PageSizes.clampedPageRequest(page, size);

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

        return enrich(ftsRepository.ftsAllRanked(projectIds, condition, pageable), actorMemberId);
    }

    /**
     * Maps the page of issues to summaries, adding the caller's review status and each assignee's
     * display name - both batched into one query apiece to avoid per-issue (N+1) lookups.
     */
    @LLMGenerated(
            llmInvolvement = LLMInvolvement.ASSISTED,
            evaluation = Evaluation.NOT_REVIEWED,
            evaluationReason = "Integration test passes, but still needs review.",
            model = "claude-opus-4-8")
    private Page<IssueSummary> enrich(Page<Issue> issues, Long actorMemberId) {
        Set<Long> issueIds = issues.getContent().stream().map(Issue::getId).collect(Collectors.toSet());
        Map<Long, ReviewStatus> myStatuses = issueIds.isEmpty()
                ? Map.of()
                : reviewerQueryRepository.findMyReviewStatuses(actorMemberId, issueIds).stream()
                        .collect(Collectors.toMap(MyReviewStatusView::issueId, MyReviewStatusView::status));

        Set<Long> assigneeIds = issues.getContent().stream()
                .map(IssueFullTextSearchService::assigneeIdOf)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> assigneeNames = assigneeIds.isEmpty()
                ? Map.of()
                : memberFinder.getAllActiveByIds(assigneeIds).stream()
                        .collect(Collectors.toMap(Member::getId, Member::getName));

        List<String> issueKeys = issues.getContent().stream().map(Issue::getKey).toList();
        Map<String, Instant> lastActivity = activityLogQueryRepository.findLastActivityAtByIssueKeys(issueKeys);

        return issues.map(issue -> {
            Long assigneeId = assigneeIdOf(issue);
            return IssueSummary.from(
                    issue,
                    myStatuses.get(issue.getId()),
                    assigneeId == null ? null : assigneeNames.get(assigneeId),
                    lastActivity.get(issue.getKey()));
        });
    }

    private static @Nullable Long assigneeIdOf(Issue issue) {
        ProjectMember assignee = issue.getParticipants().getAssignee();
        return assignee != null ? assignee.getMemberId() : null;
    }
}
