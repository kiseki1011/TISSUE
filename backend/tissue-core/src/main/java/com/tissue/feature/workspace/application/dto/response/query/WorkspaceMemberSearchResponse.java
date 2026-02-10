package com.tissue.feature.workspace.application.dto.response.query;

import com.tissue.feature.workspace.domain.WorkspaceMember;

public record WorkspaceMemberSearchResponse(Long memberId, String username, String displayName, String email) {

    public static WorkspaceMemberSearchResponse from(WorkspaceMember workspaceMember) {
        return new WorkspaceMemberSearchResponse(
                workspaceMember.getMemberId(),
                workspaceMember.getMember().getUsername(),
                workspaceMember.getDisplayName(),
                workspaceMember.getMember().getEmail());
    }
}
