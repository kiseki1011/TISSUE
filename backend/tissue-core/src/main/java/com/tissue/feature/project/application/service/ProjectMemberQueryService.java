package com.tissue.feature.project.application.service;

import com.tissue.feature.project.application.dto.response.ProjectMemberSummary;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.application.port.usecase.ProjectMemberQueryUseCase;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectRole;
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
public class ProjectMemberQueryService implements ProjectMemberQueryUseCase {

    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final ProjectMemberQueryRepository projectMemberQueryRepository;

    @Override
    public Page<ProjectMemberSummary> getProjectMembers(
            ProjectIdentifier pid,
            @Nullable ProjectRole role,
            @Nullable String keyword,
            Pageable pageable,
            Long actorMemberId) {
        Project project = projectFinder.getBy(pid.workspaceKey(), pid.projectKey());
        projectMemberFinder.getBy(project, actorMemberId);

        String normalized = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        var page = (normalized == null)
                ? projectMemberQueryRepository.findAllByProject(project, role, pageable)
                : projectMemberQueryRepository.findAllByProjectAndKeyword(project, role, normalized, pageable);
        return page.map(ProjectMemberSummary::from);
    }
}
