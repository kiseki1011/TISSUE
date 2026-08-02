package com.tissue.feature.project.application.service;

import com.tissue.feature.activitylog.application.port.repository.ActivityLogQueryRepository;
import com.tissue.feature.issue.application.port.repository.HierarchyCountRow;
import com.tissue.feature.issue.application.port.repository.IssueStatsQueryRepository;
import com.tissue.feature.issue.application.port.repository.PriorityCountRow;
import com.tissue.feature.issue.application.port.repository.ProjectStatsKpiRow;
import com.tissue.feature.issue.application.port.repository.StateCategoryCountRow;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.project.application.dto.response.ProjectDetail;
import com.tissue.feature.project.application.dto.response.ProjectSimpleStats;
import com.tissue.feature.project.application.dto.response.ProjectSummary;
import com.tissue.feature.project.application.port.repository.ProjectMemberCountRow;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberRoleRow;
import com.tissue.feature.project.application.port.repository.ProjectQueryRepository;
import com.tissue.feature.project.application.port.usecase.ProjectQueryUseCase;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.feature.workflow.domain.enums.StateCategory;
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
    private final IssueStatsQueryRepository issueStatsQueryRepository;

    @Override
    public Page<ProjectSummary> getProjects(
            boolean includeArchived, @Nullable String keyword, Pageable pageable, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        String normalized = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<Project> page = (normalized == null)
                ? projectQueryRepository.findAllProjects(includeArchived, pageable)
                : projectQueryRepository.findAllByKeyword(includeArchived, normalized, pageable);

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

    @Override
    public ProjectSimpleStats getProjectSimpleStats(ProjectIdentifier pid, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);
        Project project = projectFinder.getByProjectKey(pid.projectKey());
        Long projectId = project.getId();

        Map<StateCategory, Long> byCategory = issueStatsQueryRepository.countByStateCategory(projectId).stream()
                .collect(Collectors.toMap(StateCategoryCountRow::getCategory, StateCategoryCountRow::getCount));
        Map<IssueHierarchy, Long> byHierarchy = issueStatsQueryRepository.countByHierarchy(projectId).stream()
                .collect(Collectors.toMap(HierarchyCountRow::getHierarchy, HierarchyCountRow::getCount));
        Map<IssuePriority, Long> byPriority = issueStatsQueryRepository.countByPriority(projectId).stream()
                .collect(Collectors.toMap(PriorityCountRow::getPriority, PriorityCountRow::getCount));
        ProjectStatsKpiRow kpi =
                issueStatsQueryRepository.getKpis(projectId, Instant.now(), StateCategory.terminalCategories());

        return ProjectSimpleStats.of(byCategory, byHierarchy, byPriority, kpi.getUnassigned(), kpi.getOverdue());
    }
}
