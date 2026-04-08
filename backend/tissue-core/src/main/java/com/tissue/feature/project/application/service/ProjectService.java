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
import com.tissue.feature.workspace.domain.policy.WorkspacePolicy;
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
    private final WorkspacePolicy workspacePolicy;
    private final ProjectDefaultSetupService projectDefaultSetupService;
    private final ProjectTemplateSetupService projectTemplateSetupService;

    @Override
    public ProjectResponse create(String workspaceKey, CreateProjectCommand cmd, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        Workspace workspace = workspaceFinder.getBy(workspaceKey);

        int currentProjectCount = projectFinder.countByWorkspaceKey(workspaceKey);
        workspacePolicy.ensureCanAddProject(currentProjectCount);

        projectValidator.ensureUniqueProjectKey(cmd.projectKey(), workspace.getKey());

        Project project = Project.create(workspace, cmd.projectKey(), cmd.title(), cmd.description());
        projectRepository.save(project);

        if (cmd.projectTemplateId() != null) {
            projectTemplateSetupService.setupFromTemplate(project, cmd.projectTemplateId());
        } else {
            projectDefaultSetupService.setupDefaultConfiguration(project);
        }

        ProjectMember projectCreator = ProjectMember.createManager(project, actor);
        projectMemberRepository.save(projectCreator);

        return ProjectResponse.from(project);
    }

    @Override
    public void update(ProjectIdentifier pid, UpdateProjectCommand cmd, Long actorMemberId) {
        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);

        Project project = projectFinder.getBy(pid.workspaceKey(), pid.projectKey());

        projectAuthorizationService.requireProjectManager(actor);

        Patchers.apply(cmd.title(), project::updateTitle);
        Patchers.apply(cmd.description(), project::updateDescription);
        Patchers.apply(cmd.projectVisibility(), project::updateVisibility);
    }

    @Override
    public void delete(ProjectIdentifier pid, Long actorMemberId) {
        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);

        Project project = actor.getProject();

        projectAuthorizationService.requireWorkspaceAdmin(actor);

        project.softDelete();
    }

    @Override
    public void archive(ProjectIdentifier pid, Long actorMemberId) {
        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);

        projectAuthorizationService.requireProjectManager(actor);

        Project project = projectFinder.getBy(pid.workspaceKey(), pid.projectKey());
        project.archive();
    }

    @Override
    public void restoreArchived(ProjectIdentifier pid, Long actorMemberId) {
        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);

        projectAuthorizationService.requireProjectManager(actor);

        Project project = projectFinder.getBy(pid.workspaceKey(), pid.projectKey());
        project.restoreArchived();
    }

    @Override
    public void restoreDeleted(ProjectIdentifier pid, Long actorMemberId) {
        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);

        projectAuthorizationService.requireProjectManager(actor);

        Project project = projectFinder.getDeletedBy(pid.workspaceKey(), pid.projectKey());
        project.restoreSoftDeleted();
    }
}
