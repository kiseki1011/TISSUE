package com.tissue.feature.project.application.port.usecase;

import com.tissue.feature.project.application.dto.response.ProjectDetail;
import com.tissue.feature.project.application.dto.response.ProjectSummary;
import com.tissue.shared.dto.ProjectIdentifier;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectQueryUseCase {

    /**
     * Paged list of all projects. Visible to any authenticated member regardless of project
     * visibility, so that members can discover PUBLIC projects to join.
     */
    Page<ProjectSummary> getProjects(
            boolean includeArchived, @Nullable String keyword, Pageable pageable, Long actorMemberId);

    ProjectDetail getProjectDetail(ProjectIdentifier pid, Long actorMemberId);
}
