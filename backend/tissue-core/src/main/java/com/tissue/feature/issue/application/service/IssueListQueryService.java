package com.tissue.feature.issue.application.service;

import com.tissue.feature.activitylog.application.port.repository.ActivityLogQueryRepository;
import com.tissue.feature.issue.application.dto.IssueSearchCursor;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.feature.issue.application.port.repository.IssueListQueryRepository;
import com.tissue.feature.issue.application.port.usecase.IssueListQueryUseCase;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.sprint.application.service.SprintFinder;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.dto.CursorPage;
import com.tissue.shared.dto.PageSizes;
import com.tissue.shared.dto.ProjectIdentifier;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueListQueryService implements IssueListQueryUseCase {

    private static final Set<StateCategory> NON_TERMINAL = Set.of(StateCategory.INITIAL, StateCategory.ACTIVE);
    private static final Set<StateCategory> INITIAL_ONLY = Set.of(StateCategory.INITIAL);

    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final SprintFinder sprintFinder;
    private final IssueListQueryRepository listRepository;
    private final ActivityLogQueryRepository activityLogQueryRepository;

    @Override
    public CursorPage<IssueSummary> getMyWork(
            ProjectIdentifier pid, Long actorMemberId, @Nullable String cursor, int size) {
        Project project = projectFinder.getByProjectKey(pid.projectKey());
        projectMemberFinder.getBy(project, actorMemberId);

        int clamped = PageSizes.clamp(size);
        List<Issue> fetched = listRepository.findAssignedAfter(
                project, Set.of(actorMemberId), NON_TERMINAL, IssueSearchCursor.decode(cursor), clamped);
        return toPage(fetched, clamped);
    }

    @Override
    public CursorPage<IssueSummary> getBacklog(
            ProjectIdentifier pid, Long actorMemberId, @Nullable String cursor, int size) {
        Project project = projectFinder.getByProjectKey(pid.projectKey());
        projectMemberFinder.getBy(project, actorMemberId);

        int clamped = PageSizes.clamp(size);
        List<Issue> fetched =
                listRepository.findBacklogAfter(project, INITIAL_ONLY, IssueSearchCursor.decode(cursor), clamped);
        return toPage(fetched, clamped);
    }

    @Override
    public CursorPage<IssueSummary> getCurrentSprintIssues(
            ProjectIdentifier pid, Long actorMemberId, @Nullable String cursor, int size) {
        Project project = projectFinder.getByProjectKey(pid.projectKey());
        projectMemberFinder.getBy(project, actorMemberId);

        Optional<Sprint> activeSprint = sprintFinder.getActiveOptional(project);
        if (activeSprint.isEmpty()) {
            return CursorPage.empty();
        }

        int clamped = PageSizes.clamp(size);
        List<Issue> fetched = listRepository.findInSprintAfter(
                project, activeSprint.get().getId(), IssueSearchCursor.decode(cursor), clamped);
        return toPage(fetched, clamped);
    }

    private CursorPage<IssueSummary> toPage(List<Issue> fetched, int clamped) {
        boolean hasNext = fetched.size() > clamped;
        List<Issue> page = hasNext ? fetched.subList(0, clamped) : fetched;

        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            Issue last = page.getLast();
            nextCursor = new IssueSearchCursor(last.getPriority(), last.getId()).encode();
        }

        List<String> issueKeys = page.stream().map(Issue::getKey).toList();
        Map<String, Instant> lastActivity = activityLogQueryRepository.findLastActivityAtByIssueKeys(issueKeys);
        return CursorPage.of(
                page.stream()
                        .map(issue -> IssueSummary.from(issue, lastActivity.get(issue.getKey())))
                        .toList(),
                nextCursor);
    }
}
