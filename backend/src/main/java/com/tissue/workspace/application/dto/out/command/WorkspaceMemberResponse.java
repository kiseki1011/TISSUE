package com.tissue.workspace.application.dto.out.command;

import com.tissue.workspace.domain.WorkspaceMember;

public record WorkspaceMemberResponse(String workspaceKey, Long memberId) {
    public static WorkspaceMemberResponse from(WorkspaceMember workspaceMember) {
        return new WorkspaceMemberResponse(workspaceMember.getWorkspaceKey(), workspaceMember.getMemberId());
    }
}
