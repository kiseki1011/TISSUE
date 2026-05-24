package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.feature.issue.application.port.repository.IssueSearchRepository;
import com.tissue.feature.issue.application.port.usecase.IssueSearchUseCase;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.sprint.application.port.repository.SprintQueryRepository;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.sprint.domain.SprintStatus;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueSearchService implements IssueSearchUseCase {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.asc("priority"))
            .and(Sort.by(Sort.Order.asc("schedule.dueAt")))
            .and(Sort.by(Sort.Order.desc("storyPoint")));

    private static final Map<String, String> SORT_ALIASES = Map.of(
            "dueAt", "schedule.dueAt",
            "startedAt", "schedule.startedAt",
            "resolvedAt", "schedule.resolvedAt",
            "progress", "progress.countBasedProgress");

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "priority",
            "storyPoint",
            "title",
            "createdAt",
            "lastModifiedAt",
            "schedule.dueAt",
            "schedule.startedAt",
            "schedule.resolvedAt",
            "progress.countBasedProgress");

    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final SprintQueryRepository sprintQueryRepository;
    private final IssueSearchRepository issueSearchRepository;

    @Override
    public Page<IssueSummary> searchByProject(
            ProjectIdentifier pid, IssueSearchCondition condition, Pageable pageable, Long actorMemberId) {
        Project project = projectFinder.getBy(pid.workspaceKey(), pid.projectKey());
        projectMemberFinder.getBy(project, actorMemberId);

        IssueSearchCondition resolved = resolveCurrentSprint(condition, project);
        Pageable effective = applyDefaultSort(pageable);

        return issueSearchRepository
                .searchByProject(project, resolved, effective)
                .map(IssueSummary::from);
    }

    @Override
    public Page<IssueSummary> searchByWorkspace(
            String workspaceKey, IssueSearchCondition condition, Pageable pageable, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        // currentSprintOnly is project-scoped; it cannot resolve across multiple projects in a workspace.
        // Clients should set `sprintIds` explicitly when filtering by sprint at the workspace level.
        Pageable effective = applyDefaultSort(pageable);

        return issueSearchRepository
                .searchByWorkspace(workspaceKey, condition, effective, actorMemberId)
                .map(IssueSummary::from);
    }

    private IssueSearchCondition resolveCurrentSprint(IssueSearchCondition c, Project project) {
        if (c.currentSprintOnly() == null || !c.currentSprintOnly()) {
            return c;
        }
        Optional<Sprint> activeSprint = sprintQueryRepository.findByProjectAndStatus(project, SprintStatus.ACTIVE);
        Set<Long> sprintIds = new LinkedHashSet<>(c.sprintIds() == null ? Set.of() : c.sprintIds());
        activeSprint.ifPresent(s -> sprintIds.add(s.getId()));
        if (sprintIds.isEmpty()) {
            sprintIds.add(-1L);
        }
        return new IssueSearchCondition(
                c.priorities(),
                c.stateCategories(),
                c.currentStateIds(),
                c.tagIds(),
                c.assigneeMemberIds(),
                c.reviewerMemberIds(),
                c.subscriberMemberIds(),
                new HashSet<>(sprintIds),
                c.currentSprintOnly(),
                c.dueAtFrom(),
                c.dueAtTo(),
                c.startedAtFrom(),
                c.startedAtTo(),
                c.resolvedAtFrom(),
                c.resolvedAtTo(),
                c.progressMinPercent(),
                c.progressMaxPercent(),
                c.keyword());
    }

    private Pageable applyDefaultSort(Pageable pageable) {
        if (!pageable.getSort().isSorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);
        }
        List<Sort.Order> resolved = pageable.getSort().stream()
                .map(order -> {
                    String prop = SORT_ALIASES.getOrDefault(order.getProperty(), order.getProperty());
                    if (!ALLOWED_SORT_PROPERTIES.contains(prop)) {
                        throw new IllegalArgumentException("Unsupported sort property: " + order.getProperty());
                    }
                    return new Sort.Order(order.getDirection(), prop);
                })
                .toList();
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(resolved));
    }
}
