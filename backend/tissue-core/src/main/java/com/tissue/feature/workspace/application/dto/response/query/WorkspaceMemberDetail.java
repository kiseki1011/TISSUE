package com.tissue.feature.workspace.application.dto.response.query;

import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import lombok.Builder;

@Builder
public record WorkspaceMemberDetail(
        String workspaceKey, Long memberId, String displayName, String userName, WorkspaceRole workspaceRole) {

    public static WorkspaceMemberDetail from(WorkspaceMember workspaceMember) {
        return WorkspaceMemberDetail.builder()
                .workspaceKey(workspaceMember.getWorkspaceKey())
                .memberId(workspaceMember.getMemberId())
                .displayName(workspaceMember.getDisplayName())
                .userName(workspaceMember.getMember().getUsername())
                .workspaceRole(workspaceMember.getRole())
                .build();
    }
}
