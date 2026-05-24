package com.tissue.feature.workspace.application.dto.response.query;

import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record WorkspaceMemberDetail(
        String workspaceKey,
        Long memberId,
        String username,
        String displayName,
        @Nullable String email,
        WorkspaceRole role,
        Instant joinedAt) {

    public static WorkspaceMemberDetail from(WorkspaceMember workspaceMember) {
        return new WorkspaceMemberDetail(
                workspaceMember.getWorkspaceKey(),
                workspaceMember.getMemberId(),
                workspaceMember.getMember().getUsername(),
                workspaceMember.getDisplayName(),
                workspaceMember.getMember().getEmail(),
                workspaceMember.getRole(),
                workspaceMember.getCreatedAt());
    }
}
