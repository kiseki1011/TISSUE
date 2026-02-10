package com.tissue.feature.workspace.application.dto;

import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;

public record WorkspaceMemberContext(
        Long workspaceMemberId,
        Long memberId,
        Long workspaceId,
        String workspaceKey,
        String email,
        String displayName,
        WorkspaceRole workspaceRole) {

    public static WorkspaceMemberContext from(WorkspaceMember workspaceMember) {
        return new WorkspaceMemberContext(
                workspaceMember.getId(),
                workspaceMember.getMemberId(),
                workspaceMember.getWorkspace().getId(),
                workspaceMember.getWorkspaceKey(),
                workspaceMember.getMember().getEmail(),
                workspaceMember.getDisplayName(),
                workspaceMember.getRole());
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
}
