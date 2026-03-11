package com.tissue.feature.project.application.service.authorization;

import static com.tissue.feature.project.domain.exception.ProjectErrorCode.PROJECT_JOIN_NOT_ALLOWED;
import static com.tissue.feature.project.domain.exception.ProjectErrorCode.PROJECT_MANAGER_MODIFICATION_NOT_ALLOWED;
import static com.tissue.feature.project.domain.exception.ProjectErrorCode.PROJECT_MANAGER_REQUIRED;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.exception.InsufficientWorkspaceRoleException;
import com.tissue.shared.exception.base.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectAuthorizationService {

    public void requireWorkspaceAdmin(ProjectMember actor) {
        if (actor.getWorkspaceMember().getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        throw new InsufficientWorkspaceRoleException(WorkspaceRole.ADMIN);
    }

    public void requireProjectManager(ProjectMember actor) {
        if (actor.getWorkspaceMember().getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        if (actor.isManager()) {
            return;
        }
        throw new ForbiddenException(PROJECT_MANAGER_REQUIRED);
    }

    public void requireJoinPermission(WorkspaceMember actor, Project project) {
        if (actor.getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        if (project.isPublic()) {
            return;
        }
        throw new ForbiddenException(PROJECT_JOIN_NOT_ALLOWED);
    }

    public void requireHigherRole(ProjectMember actor, ProjectMember target) {
        if (actor.getWorkspaceMember().getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        if (!actor.isManager()) {
            throw new ForbiddenException(PROJECT_MANAGER_REQUIRED);
        }
        if (target.isManager()) {
            throw new ForbiddenException(PROJECT_MANAGER_MODIFICATION_NOT_ALLOWED);
        }
    }
}
