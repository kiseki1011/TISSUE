package com.tissue.workspace.application.dto.info;

import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;

public record WorkspaceMemberInfo(
        Long workspaceMemberId,
        Long memberId,
        String workspaceKey,
        String email,
        String displayName,
        WorkspaceRole role) {

    public static WorkspaceMemberInfo from(WorkspaceMember workspaceMember) {
        return new WorkspaceMemberInfo(
                workspaceMember.getId(),
                workspaceMember.getMemberId(),
                workspaceMember.getWorkspaceKey(),
                workspaceMember.getEmail(),
                workspaceMember.getDisplayName(),
                workspaceMember.getRole());
    }
}
