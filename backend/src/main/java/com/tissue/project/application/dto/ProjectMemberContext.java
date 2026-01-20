package com.tissue.project.application.dto;

import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.enums.ProjectRole;
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
        ProjectRole projectRole,
        WorkspaceRole workspaceRole) {

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
                projectMember.getRole(),
                workspaceMember.getRole());
    }

    public boolean isProjectAdmin() {
        return projectRole == ProjectRole.ADMIN;
    }

    public boolean isProjectMember() {
        return projectRole.isEqualOrHigherThan(ProjectRole.MEMBER);
    }

    // TODO: 굳이 필요할까? 어차피 ProjectMember로 조회가 가능했다는건 최소한 VIEWER 권한을 가진다는 의미인데?
    public boolean isProjectViewer() {
        return projectRole.isEqualOrHigherThan(ProjectRole.VIEWER);
    }

    public boolean isWorkspaceOwner() {
        return workspaceRole == WorkspaceRole.OWNER;
    }

    public boolean isWorkspaceAdmin() {
        return workspaceRole.isEqualOrHigherThan(WorkspaceRole.ADMIN);
    }

    // TODO: 굳이 필요할까? 어차피 WorkspaceMember로 조회가 가능했다는건 최소한 MEMBER 권한을 가진다는 의미인데?
    public boolean isWorkspaceMember() {
        return workspaceRole.isEqualOrHigherThan(WorkspaceRole.MEMBER);
    }
}
