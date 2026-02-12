package com.tissue.feature.project.application.service.authorization;

import com.tissue.feature.project.application.dto.ProjectMemberContext;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.exception.ProjectJoinNotAllowedException;
import com.tissue.feature.project.domain.exception.ProjectMemberNotFoundException;
import com.tissue.feature.project.domain.exception.RequireProjectEditPermission;
import com.tissue.feature.project.domain.exception.RequireProjectManagerException;
import com.tissue.feature.project.domain.exception.ResourceOwnershipRequiredException;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectAuthorizationService {

    private final ProjectMemberQueryRepository projectMemberQueryRepository;

    public void requireProjectMember(Project targetProject, Long memberId) {
        boolean hasTargetAccess = projectMemberQueryRepository.existsByProjectAndMemberId(targetProject, memberId);
        if (!hasTargetAccess) {
            throw new ProjectMemberNotFoundException(targetProject.getWorkspaceKey(), targetProject.getKey(), memberId);
        }
    }

    // New style
    public void requireProjectEditPermission(WorkspaceMember actor, Project project) {
        if (actor.getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        if (isProjectCreator(project, actor.getMember().getId())) {
            return;
        }
        throw new RequireProjectEditPermission(actor.getWorkspaceKey(), project.getKey());
    }

    // Compatibility style
    public void requireProjectEditPermission(WorkspaceMemberContext actor, Project project) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (isProjectCreator(project, actor.memberId())) {
            return;
        }
        throw new RequireProjectEditPermission(actor.workspaceKey(), project.getKey());
    }

    // New style
    public void requireProjectManager(ProjectMember actor) {
        if (actor.getWorkspaceMember().getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        if (actor.isManager()) {
            return;
        }
        throw new RequireProjectManagerException(actor.getWorkspaceKey(), actor.getProjectKey());
    }

    // Compatibility style
    public void requireProjectManager(ProjectMemberContext actor) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (actor.isProjectManager()) {
            return;
        }
        throw new RequireProjectManagerException(actor.workspaceKey(), actor.projectKey());
    }

    // New style
    public void requireJoinPermission(WorkspaceMember actor, Project project) {
        if (actor.getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        if (project.isPublic()) {
            return;
        }
        throw new ProjectJoinNotAllowedException(actor.getWorkspaceKey(), project.getKey());
    }

    // Compatibility style
    public void requireJoinPermission(WorkspaceMemberContext actor, Project project) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (project.isPublic()) {
            return;
        }
        throw new ProjectJoinNotAllowedException(actor.workspaceKey(), project.getKey());
    }

    // New style
    public void requireWorkflowEditPermission(ProjectMember actor, Workflow workflow) {
        if (actor.getWorkspaceMember().getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        if (isWorkflowCreator(workflow, actor.getMemberId())) {
            return;
        }
        throw new ResourceOwnershipRequiredException(actor.getWorkspaceKey(), actor.getProjectKey(), "Workflow");
    }

    // Compatibility style
    public void requireWorkflowEditPermission(ProjectMemberContext actor, Workflow workflow) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (isWorkflowCreator(workflow, actor.memberId())) {
            return;
        }
        throw new ResourceOwnershipRequiredException(actor.workspaceKey(), actor.projectKey(), "Workflow");
    }

    private boolean isProjectCreator(Project project, Long actorMemberId) {
        return project.getCreatedBy().equals(actorMemberId);
    }

    private boolean isWorkflowCreator(Workflow workflow, Long actorMemberId) {
        return workflow.getCreatedBy().equals(actorMemberId);
    }
}
