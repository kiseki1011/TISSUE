package com.tissue.project.application.service;

import com.tissue.common.util.Patchers;
import com.tissue.project.application.dto.request.CreateProjectCommand;
import com.tissue.project.application.dto.request.DeleteProjectCommand;
import com.tissue.project.application.dto.request.UpdateProjectCommand;
import com.tissue.project.application.dto.response.ProjectCommandResult;
import com.tissue.project.application.port.in.ProjectCommandUseCase;
import com.tissue.project.application.port.out.ProjectCommandRepository;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.validator.ProjectValidator;
import com.tissue.project.domain.Project;
import com.tissue.security.authentication.application.port.out.CurrentMemberProvider;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.domain.Workspace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectCommandService implements ProjectCommandUseCase {

    private final WorkspaceFinder workspaceFinder;
    private final ProjectFinder projectFinder;
    private final ProjectValidator projectValidator;
    private final ProjectCommandRepository projectRepository;
    private final WorkspaceAuthorizationService workspaceAuthService;
    private final ProjectAuthorizationService projectAuthService;
    private final CurrentMemberProvider currentMemberProvider;

    @Override
    public ProjectCommandResult create(CreateProjectCommand cmd) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        workspaceAuthService.requireWorkspaceMember(cmd.workspaceKey(), actorMemberId);

        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());

        Project project = Project.create(workspace, cmd.projectKey(), cmd.title(), cmd.description());

        projectValidator.ensureUniqueProjectKey(project.getKey(), workspace.getKey());

        projectRepository.save(project);

        // TODO: ProjectCreatedEvent

        return ProjectCommandResult.from(project);
    }

    @Override
    public ProjectCommandResult update(UpdateProjectCommand cmd) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        projectAuthService.requireProjectAdmin(cmd.workspaceKey(), cmd.projectKey(), actorMemberId);

        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());

        Patchers.apply(cmd.title(), project::updateTitle);
        Patchers.apply(cmd.description(), project::updateDescription);
        Patchers.apply(cmd.projectVisibility(), project::updateVisibility);
        Patchers.apply(cmd.defaultJoinRole(), project::updateDefaultJoinRole);

        // TODO: ProjectInfoUpdatedEvent

        return ProjectCommandResult.from(project);
    }

    @Override
    public ProjectCommandResult delete(DeleteProjectCommand cmd) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        workspaceAuthService.requireWorkspaceAdmin(cmd.workspaceKey(), actorMemberId);

        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());

        project.softDelete();

        // TODO: ProjectSoftDeletedEvent

        return ProjectCommandResult.from(project);
    }
}
