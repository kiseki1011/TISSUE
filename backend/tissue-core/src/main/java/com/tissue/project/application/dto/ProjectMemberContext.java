package com.tissue.project.application.dto;

import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.ProjectRole;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;

public record ProjectMemberContext(
        Long projectMemberId,
        Long memberId,
        Long workspaceId,
        String workspaceKey,
        Long projectId,
        String projectKey,
        String displayName,
        WorkspaceRole workspaceRole,
        ProjectRole projectRole) {

    public static ProjectMemberContext from(ProjectMember projectMember) {
        WorkspaceMember workspaceMember = projectMember.getWorkspaceMember();
        return new ProjectMemberContext(
                projectMember.getId(),
                projectMember.getMemberId(),
                workspaceMember.getWorkspace().getId(),
                projectMember.getWorkspaceKey(),
                projectMember.getProject().getId(),
                projectMember.getProjectKey(),
                workspaceMember.getDisplayName(),
                workspaceMember.getRole(),
                projectMember.getRole());
    }

    public boolean isWorkspaceOwner() {
        return workspaceRole == WorkspaceRole.OWNER;
    }

    public boolean isWorkspaceAdmin() {
        return workspaceRole.isEqualOrHigherThan(WorkspaceRole.ADMIN);
    }

    public boolean isWorkspaceMember() {
        return workspaceRole.isEqualOrHigherThan(WorkspaceRole.MEMBER);
    }

    public boolean isProjectManager() {
        return projectRole == ProjectRole.MANAGER;
    }
}
