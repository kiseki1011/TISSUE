package com.tissue.workspace.application.dto.in;

import lombok.Builder;

@Builder
public record ManageTeamCommand(String workspaceKey, Long memberId, Long teamId, Long actorMemberId) {}
