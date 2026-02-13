package com.tissue.feature.project.application.service;

import com.tissue.feature.project.application.dto.request.CreateProjectCommand;
import com.tissue.feature.project.application.dto.request.UpdateProjectCommand;
import com.tissue.feature.project.application.dto.response.ProjectResponse;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.port.usecase.ProjectUseCase;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.application.service.validator.ProjectValidator;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
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
    private final ProjectMemberFinder projectMemberFinder;
    private final ProjectFinder projectFinder;
    private final ProjectValidator projectValidator;
    private final ProjectCommandRepository projectRepository;
    private final ProjectMemberCommandRepository projectMemberRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    @Override
    public ProjectResponse create(String workspaceKey, CreateProjectCommand cmd, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getActiveWithWorkspace(workspaceKey, actorMemberId);

        Workspace workspace = workspaceFinder.getBy(workspaceKey);

        projectValidator.ensureUniqueProjectKey(cmd.projectKey(), workspace.getKey());

        Project project = Project.create(workspace, cmd.projectKey(), cmd.title(), cmd.description());
        projectRepository.save(project);

        ProjectMember projectCreator = ProjectMember.createManager(project, actor);
        projectMemberRepository.save(projectCreator);

        // TODO: ProjectCreatedEvent

        return ProjectResponse.from(project);
    }

    @Override
    public void update(ProjectIdentifier projectIdentifier, UpdateProjectCommand cmd, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        Project project = projectFinder.getBy(projectIdentifier.workspaceKey(), projectIdentifier.projectKey());

        projectAuthorizationService.requireProjectManager(actor);

        Patchers.apply(cmd.title(), project::updateTitle);
        Patchers.apply(cmd.description(), project::updateDescription);
        Patchers.apply(cmd.projectVisibility(), project::updateVisibility);

        // TODO: ProjectInfoUpdatedEvent
    }

    @Override
    public void delete(ProjectIdentifier projectIdentifier, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        Project project = actor.getProject();

        // TODO: workspace admin만 허용하는걸 고려
        //   + archive 상태에서만 삭제 가능하도록 검증하는걸 고려
        projectAuthorizationService.requireProjectManager(actor);

        project.softDelete();

        // TODO: ProjectSoftDeletedEvent
    }
}
