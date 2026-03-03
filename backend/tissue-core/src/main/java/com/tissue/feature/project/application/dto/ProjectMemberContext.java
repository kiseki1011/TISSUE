package com.tissue.feature.project.application.dto;

import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;

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
