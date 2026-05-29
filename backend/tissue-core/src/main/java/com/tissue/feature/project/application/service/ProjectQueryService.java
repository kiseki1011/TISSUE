package com.tissue.feature.project.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.project.application.dto.response.ProjectDetail;
import com.tissue.feature.project.application.dto.response.ProjectSummary;
import com.tissue.feature.project.application.port.repository.ProjectQueryRepository;
import com.tissue.feature.project.application.port.usecase.ProjectQueryUseCase;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.domain.Project;
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
    private final MemberFinder memberFinder;
    private final ProjectQueryRepository projectQueryRepository;

    @Override
    public Page<ProjectSummary> getProjects(
            boolean includeArchived, @Nullable String keyword, Pageable pageable, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        String normalized = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        var page = (normalized == null)
                ? projectQueryRepository.findAllProjects(includeArchived, pageable)
                : projectQueryRepository.findAllByKeyword(includeArchived, normalized, pageable);
        return page.map(ProjectSummary::from);
    }

    @Override
    public ProjectDetail getProjectDetail(ProjectIdentifier pid, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);
        Project project = projectFinder.getByProjectKey(pid.projectKey());
        return ProjectDetail.from(project);
    }
}
