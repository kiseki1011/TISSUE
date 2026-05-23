package com.tissue.feature.project.application.service;

import com.tissue.feature.project.application.dto.response.ProjectDetail;
import com.tissue.feature.project.application.dto.response.ProjectSummary;
import com.tissue.feature.project.application.port.repository.ProjectQueryRepository;
import com.tissue.feature.project.application.port.usecase.ProjectQueryUseCase;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.shared.dto.ProjectIdentifier;
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
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final ProjectQueryRepository projectQueryRepository;

    @Override
    public Page<ProjectSummary> getProjects(
            String workspaceKey,
            boolean includeArchived,
            @Nullable String keyword,
            Pageable pageable,
            Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        String normalized = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        var page = (normalized == null)
                ? projectQueryRepository.findAllByWorkspaceKey(workspaceKey, includeArchived, pageable)
                : projectQueryRepository.findAllByWorkspaceKeyAndKeyword(
                        workspaceKey, includeArchived, normalized, pageable);
        return page.map(ProjectSummary::from);
    }

    @Override
    public ProjectDetail getProjectDetail(ProjectIdentifier pid, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(pid.workspaceKey(), actorMemberId);
        Project project = projectFinder.getBy(pid.workspaceKey(), pid.projectKey());
        return ProjectDetail.from(project);
    }
}
