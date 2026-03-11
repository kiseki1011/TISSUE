package com.tissue.feature.project.application.port.usecase;

import com.tissue.feature.project.application.dto.request.CreateProjectCommand;
import com.tissue.feature.project.application.dto.request.UpdateProjectCommand;
import com.tissue.feature.project.application.dto.response.ProjectResponse;
import com.tissue.shared.dto.ProjectIdentifier;

public interface ProjectUseCase {

    ProjectResponse create(String workspaceKey, CreateProjectCommand cmd, Long actorMemberId);

    void update(ProjectIdentifier projectIdentifier, UpdateProjectCommand cmd, Long actorMemberId);

    void delete(ProjectIdentifier projectIdentifier, Long actorMemberId);

    void archive(ProjectIdentifier projectIdentifier, Long actorMemberId);

    void restoreArchived(ProjectIdentifier projectIdentifier, Long actorMemberId);

    void restoreDeleted(ProjectIdentifier projectIdentifier, Long actorMemberId);

    // TODO: (optional) migrateProjectKey()

    // TODO: Project pagination api
    //  getProjects() vs getProjectPagination() vs getProjectPages() vs searchProjects()
    //  default
    //   - 20 projects
    //   - joinedDate DESC
    //  search by
    //   - createdDate (by scope)
    //   - name
    //   - project key
    //   - show the projects i joined first
    //  sort by
    //   - joinedDate DESC
    //   - createdDate DESC
    //   - total issue numbers (optional)
    //   - total project members (optional)

    // TODO: getProjectDetail()
}
