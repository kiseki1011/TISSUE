package com.tissue.workspace.application.dto.in;

public record JoinViaLinkCommand(String workspaceKey, String token, Long actorMemberId) {}
