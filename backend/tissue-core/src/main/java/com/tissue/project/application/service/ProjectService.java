package com.tissue.project.application.service;

import com.tissue.common.util.Patchers;
import com.tissue.project.application.dto.request.CreateProjectCommand;
import com.tissue.project.application.dto.request.UpdateProjectCommand;
import com.tissue.project.application.dto.response.ProjectCommandResult;
import com.tissue.project.application.port.in.ProjectUseCase;
import com.tissue.project.application.port.out.ProjectCommandRepository;
import com.tissue.project.application.port.out.ProjectMemberCommandRepository;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.validator.ProjectValidator;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectService implements ProjectUseCase {

    private final WorkspaceFinder workspaceFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final ProjectFinder projectFinder;
    private final ProjectValidator projectValidator;
    private final ProjectCommandRepository projectRepository;
    private final ProjectMemberCommandRepository projectMemberRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ProjectAuthorizationService projectAuthorizationService;

    @Override
    public ProjectCommandResult create(CreateProjectCommand cmd, WorkspaceMemberContext actorContext) {
        workspaceAuthorizationService.requireWorkspaceMember(actorContext);

        // TODO: workspaceMemberFinder.getWithWorkspaceBy
        WorkspaceMember actor = workspaceMemberFinder.getBy(actorContext.workspaceKey(), actorContext.memberId());
        Workspace workspace = workspaceFinder.getBy(actorContext.workspaceKey());

        projectValidator.ensureUniqueProjectKey(cmd.projectKey(), workspace.getKey());

        Project project = Project.create(workspace, cmd.projectKey(), cmd.title(), cmd.description());
        projectRepository.save(project);

        ProjectMember creator = ProjectMember.createOwner(project, actor);
        projectMemberRepository.save(creator);

        // TODO: ProjectCreatedEvent

        return ProjectCommandResult.from(project);
    }

    @Override
    public ProjectCommandResult update(
            String projectKey, UpdateProjectCommand cmd, WorkspaceMemberContext actorContext) {
        Project project = projectFinder.getBy(actorContext.workspaceKey(), projectKey);
        projectAuthorizationService.requireProjectEditPermission(actorContext, project);

        Patchers.apply(cmd.title(), project::updateTitle);
        Patchers.apply(cmd.description(), project::updateDescription);
        Patchers.apply(cmd.projectVisibility(), project::updateVisibility);

        // TODO: ProjectInfoUpdatedEvent

        return ProjectCommandResult.from(project);
    }

    @Override
    public ProjectCommandResult delete(String projectKey, WorkspaceMemberContext actorContext) {
        Project project = projectFinder.getWithWorkspaceBy(actorContext.workspaceKey(), projectKey);
        projectAuthorizationService.requireProjectEditPermission(actorContext, project);

        project.softDelete();

        // TODO: ProjectSoftDeletedEvent

        return ProjectCommandResult.from(project);
    }
}
