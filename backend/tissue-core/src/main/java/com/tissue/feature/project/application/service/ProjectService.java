package com.tissue.feature.project.application.service;

import com.tissue.feature.project.application.dto.request.CreateProjectCommand;
import com.tissue.feature.project.application.dto.request.UpdateProjectCommand;
import com.tissue.feature.project.application.dto.response.ProjectCommandResult;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.port.usecase.ProjectUseCase;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.validator.ProjectValidator;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.feature.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.support.util.Patchers;
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
    public ProjectCommandResult create(String workspaceKey, CreateProjectCommand cmd, Long memberId) {
        WorkspaceMember actor = workspaceMemberFinder.getBy(workspaceKey, memberId);
        workspaceAuthorizationService.requireWorkspaceMember(actor);

        Workspace workspace = workspaceFinder.getBy(workspaceKey);

        projectValidator.ensureUniqueProjectKey(cmd.projectKey(), workspace.getKey());

        Project project = Project.create(workspace, cmd.projectKey(), cmd.title(), cmd.description());
        projectRepository.save(project);

        ProjectMember creator = ProjectMember.createOwner(project, actor);
        projectMemberRepository.save(creator);

        // TODO: ProjectCreatedEvent

        return ProjectCommandResult.from(project);
    }

    @Override
    public ProjectCommandResult update(ProjectIdentifier projectIdentifier, UpdateProjectCommand cmd, Long memberId) {
        Project project = projectFinder.getBy(projectIdentifier.workspaceKey(), projectIdentifier.projectKey());

        WorkspaceMember actor = workspaceMemberFinder.getBy(projectIdentifier.workspaceKey(), memberId);
        projectAuthorizationService.requireProjectEditPermission(actor, project);

        Patchers.apply(cmd.title(), project::updateTitle);
        Patchers.apply(cmd.description(), project::updateDescription);
        Patchers.apply(cmd.projectVisibility(), project::updateVisibility);

        // TODO: ProjectInfoUpdatedEvent

        return ProjectCommandResult.from(project);
    }

    @Override
    public ProjectCommandResult delete(ProjectIdentifier projectIdentifier, Long memberId) {
        Project project =
                projectFinder.getWithWorkspaceBy(projectIdentifier.workspaceKey(), projectIdentifier.projectKey());

        WorkspaceMember actor = workspaceMemberFinder.getBy(projectIdentifier.workspaceKey(), memberId);
        projectAuthorizationService.requireProjectEditPermission(actor, project);

        project.softDelete();

        // TODO: ProjectSoftDeletedEvent

        return ProjectCommandResult.from(project);
    }
}
