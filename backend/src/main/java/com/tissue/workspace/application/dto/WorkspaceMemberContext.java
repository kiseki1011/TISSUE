package com.tissue.workspace.application.dto;

import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;

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

    // TODO: 굳이 필요할까? 어차피 WorkspaceMember로 조회가 가능했다는건 최소한 MEMBER 권한을 가진다는 의미인데?
    public boolean isWorkspaceMember() {
        return workspaceRole.isEqualOrHigherThan(WorkspaceRole.MEMBER);
    }
}
