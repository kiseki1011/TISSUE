package com.tissue.feature.workspace.application.dto.response.query;

import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import java.time.Instant;

public record WorkspaceMemberSummary(
        Long memberId, String username, String displayName, WorkspaceRole role, Instant joinedAt) {

    public static WorkspaceMemberSummary from(WorkspaceMember workspaceMember) {
        return new WorkspaceMemberSummary(
                workspaceMember.getMemberId(),
                workspaceMember.getMember().getUsername(),
                workspaceMember.getDisplayName(),
                workspaceMember.getRole(),
                workspaceMember.getCreatedAt());
    }
}
