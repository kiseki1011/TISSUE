package com.tissue.workspace.application.dto.in;

public record KickWorkspaceMemberCommand(String workspaceKey, Long targetMemberId, Long actorMemberId) {}
