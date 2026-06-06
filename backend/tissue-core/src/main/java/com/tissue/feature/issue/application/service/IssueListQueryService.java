package com.tissue.feature.issue.application.service;

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
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.List;
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

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final Set<StateCategory> NON_TERMINAL = Set.of(StateCategory.INITIAL, StateCategory.ACTIVE);
    private static final Set<StateCategory> INITIAL_ONLY = Set.of(StateCategory.INITIAL);

    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final SprintFinder sprintFinder;
    private final IssueListQueryRepository listRepository;

    @Override
    public CursorPage<IssueSummary> getMyWork(Long actorMemberId, @Nullable String cursor, int size) {
        int clamped = clampSize(size);
        List<Issue> fetched = listRepository.findAssignedAfter(
                Set.of(actorMemberId), NON_TERMINAL, IssueSearchCursor.decode(cursor), clamped);
        return toPage(fetched, clamped);
    }

    @Override
    public CursorPage<IssueSummary> getBacklog(
            ProjectIdentifier pid, Long actorMemberId, @Nullable String cursor, int size) {
        Project project = projectFinder.getByProjectKey(pid.projectKey());
        projectMemberFinder.getBy(project, actorMemberId);

        int clamped = clampSize(size);
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

        int clamped = clampSize(size);
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
        return CursorPage.of(page.stream().map(IssueSummary::from).toList(), nextCursor);
    }

    private static int clampSize(int requested) {
        if (requested <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requested, MAX_PAGE_SIZE);
    }
}
