package com.tissue.project.application.service;

import com.tissue.common.util.Patchers;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.dto.request.CreateProjectCommand;
import com.tissue.project.application.dto.request.DeleteProjectCommand;
import com.tissue.project.application.dto.request.UpdateProjectCommand;
import com.tissue.project.application.dto.response.ProjectCommandResult;
import com.tissue.project.application.port.in.ProjectUseCase;
import com.tissue.project.application.port.out.ProjectCommandRepository;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.validator.ProjectValidator;
import com.tissue.project.domain.Project;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.domain.Workspace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectService implements ProjectUseCase {

    private final WorkspaceFinder workspaceFinder;
    private final ProjectFinder projectFinder;
    private final ProjectValidator projectValidator;
    private final ProjectCommandRepository projectRepository;
    private final WorkspaceAuthorizationService workspaceAuthService;
    private final ProjectAuthorizationService projectAuthService;

    @Override
    public ProjectCommandResult create(CreateProjectCommand cmd) {
        WorkspaceMemberContext actor = cmd.actor();
        workspaceAuthService.requireWorkspaceMember(actor);

        Workspace workspace = workspaceFinder.getModifiableBy(actor.workspaceId());

        projectValidator.ensureUniqueProjectKey(cmd.projectKey(), workspace.getKey());

        Project project = Project.create(workspace, cmd.projectKey(), cmd.title(), cmd.description());
        projectRepository.save(project);

        // TODO: ProjectCreatedEvent

        return ProjectCommandResult.from(project);
    }

    @Override
    public ProjectCommandResult update(UpdateProjectCommand cmd) {
        ProjectMemberContext actor = cmd.actor();
        projectAuthService.requireProjectAdmin(actor);

        Project project = projectFinder.getModifiableBy(actor.projectId());

        Patchers.apply(cmd.title(), project::updateTitle);
        Patchers.apply(cmd.description(), project::updateDescription);
        Patchers.apply(cmd.projectVisibility(), project::updateVisibility);
        Patchers.apply(cmd.defaultJoinRole(), project::updateDefaultJoinRole);

        // TODO: ProjectInfoUpdatedEvent

        return ProjectCommandResult.from(project);
    }

    @Override
    public ProjectCommandResult delete(DeleteProjectCommand cmd) {
        WorkspaceMemberContext actor = cmd.actor();
        workspaceAuthService.requireWorkspaceAdmin(actor);

        Project project = projectFinder.getModifiableBy(cmd.projectKey(), actor.workspaceKey());

        project.softDelete();

        // TODO: ProjectSoftDeletedEvent

        return ProjectCommandResult.from(project);
    }
}
