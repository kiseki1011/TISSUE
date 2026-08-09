package com.tissue.feature.project.application.service;

import com.tissue.feature.activitylog.application.port.repository.ActivityLogQueryRepository;
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.project.application.dto.response.ProjectDetail;
import com.tissue.feature.project.application.dto.response.ProjectSummary;
import com.tissue.feature.project.application.port.repository.ProjectMemberCountRow;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberRoleRow;
import com.tissue.feature.project.application.port.repository.ProjectQueryRepository;
import com.tissue.feature.project.application.port.usecase.ProjectQueryUseCase;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.shared.dto.ProjectIdentifier;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
public class ProjectQueryService implements ProjectQueryUseCase {

    private final ProjectFinder projectFinder;
    private final MemberFinder memberFinder;
    private final ProjectQueryRepository projectQueryRepository;
    private final ActivityLogQueryRepository activityLogQueryRepository;
    private final ProjectMemberQueryRepository projectMemberQueryRepository;

    @Override
    public Page<ProjectSummary> getProjects(
            boolean includeArchived, @Nullable String keyword, Pageable pageable, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        String normalized = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<Project> page = (normalized == null)
                ? projectQueryRepository.findAllProjects(includeArchived, pageable)
                : projectQueryRepository.findAllByKeyword(includeArchived, normalized, pageable);

        return toSummaries(page, actorMemberId);
    }

    @Override
    public Page<ProjectSummary> getMyProjects(boolean includeArchived, Pageable pageable, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        Page<Project> page = projectQueryRepository.findMemberProjects(includeArchived, actorMemberId, pageable);

        return toSummaries(page, actorMemberId);
    }

    /**
     * Enriches one page of projects with the counts and the caller's role, batching each lookup over the
     * whole page rather than per project.
     */
    private Page<ProjectSummary> toSummaries(Page<Project> page, Long actorMemberId) {
        List<String> projectKeys =
                page.getContent().stream().map(Project::getKey).toList();
        Map<String, Instant> lastActivity = activityLogQueryRepository.findLastActivityAtByProjectKeys(projectKeys);
        Map<String, Long> memberCounts = memberCounts(projectKeys);
        Map<String, ProjectRole> myRoles = myRoles(projectKeys, actorMemberId);
        return page.map(project -> ProjectSummary.from(
                project,
                lastActivity.get(project.getKey()),
                memberCounts.getOrDefault(project.getKey(), 0L),
                myRoles.get(project.getKey())));
    }

    private Map<String, Long> memberCounts(List<String> projectKeys) {
        if (projectKeys.isEmpty()) {
            return Map.of();
        }
        return projectMemberQueryRepository.countByProjectKeys(projectKeys).stream()
                .collect(Collectors.toMap(ProjectMemberCountRow::getProjectKey, ProjectMemberCountRow::getMemberCount));
    }

    private Map<String, ProjectRole> myRoles(List<String> projectKeys, Long actorMemberId) {
        if (projectKeys.isEmpty()) {
            return Map.of();
        }
        return projectMemberQueryRepository.findRolesByProjectKeys(projectKeys, actorMemberId).stream()
                .collect(Collectors.toMap(ProjectMemberRoleRow::getProjectKey, ProjectMemberRoleRow::getRole));
    }

    @Override
    public ProjectDetail getProjectDetail(ProjectIdentifier pid, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);
        Project project = projectFinder.getByProjectKey(pid.projectKey());
        return ProjectDetail.from(project);
    }
}
