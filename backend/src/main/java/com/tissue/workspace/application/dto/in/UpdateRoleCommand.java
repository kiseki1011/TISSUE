package com.tissue.workspace.application.dto.in;

import com.tissue.workspace.domain.enums.WorkspaceRole;
import lombok.Builder;

@Builder
public record UpdateRoleCommand(
        String workspaceKey, Long memberId, WorkspaceRole role, Long actorMemberId) {}
