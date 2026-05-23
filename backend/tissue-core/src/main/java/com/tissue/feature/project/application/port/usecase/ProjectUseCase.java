package com.tissue.feature.project.application.port.usecase;

import com.tissue.feature.project.application.dto.request.CreateProjectCommand;
import com.tissue.feature.project.application.dto.request.UpdateProjectCommand;
import com.tissue.feature.project.application.dto.response.ProjectResponse;
import com.tissue.shared.dto.ProjectIdentifier;

public interface ProjectUseCase {

    ProjectResponse create(String workspaceKey, CreateProjectCommand cmd, Long actorMemberId);

    void update(ProjectIdentifier pid, UpdateProjectCommand cmd, Long actorMemberId);

    void delete(ProjectIdentifier pid, Long actorMemberId);

    void archive(ProjectIdentifier pid, Long actorMemberId);

    void restoreArchived(ProjectIdentifier pid, Long actorMemberId);

    void restoreDeleted(ProjectIdentifier pid, Long actorMemberId);

    // TODO: (optional) migrateProjectKey()

    // Project list / detail queries are owned by ProjectQueryUseCase.
}
